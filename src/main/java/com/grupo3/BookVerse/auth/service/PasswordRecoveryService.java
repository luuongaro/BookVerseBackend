package com.grupo3.BookVerse.auth.service;

import com.grupo3.BookVerse.auth.domain.PasswordResetToken;
import com.grupo3.BookVerse.auth.events.PasswordChanged;
import com.grupo3.BookVerse.auth.events.PasswordResetRequested;
import com.grupo3.BookVerse.auth.repository.PasswordResetTokenRepository;
import com.grupo3.BookVerse.common.exception.InvalidPasswordResetTokenException;
import com.grupo3.BookVerse.features.users.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class PasswordRecoveryService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final UserRepository users;
    private final PasswordResetTokenRepository tokens;
    private final PasswordEncoder encoder;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;
    @Value("${app.password-reset.expiration-minutes:30}")
    private long expirationMinutes;
    @Value("${app.password-reset.cooldown-seconds:120}")
    private long cooldownSeconds;

    @PostConstruct
    void validateConfiguration() {
        var uri = URI.create(frontendUrl);
        boolean localHttp = "http".equals(uri.getScheme())
                && ("localhost".equals(uri.getHost()) || "127.0.0.1".equals(uri.getHost()));
        if (uri.getHost() == null || uri.getUserInfo() != null || uri.getQuery() != null
                || uri.getFragment() != null || !("https".equals(uri.getScheme()) || localHttp)
                || expirationMinutes < 1 || expirationMinutes > 60 || cooldownSeconds < 30) {
            throw new IllegalStateException("Invalid password recovery configuration");
        }
        frontendUrl = frontendUrl.replaceAll("/+$", "");
    }

    // The HTTP response never waits for account lookup or mail delivery.
    @Async("passwordRecoveryTaskExecutor")
    @Transactional
    public void requestReset(String email) {
        var candidate = users.findForPasswordRecoveryByEmail(email.strip());
        if (candidate.isEmpty()) return;
        var user = candidate.get();
        var now = clock.instant();
        var existing = tokens.findByUserId(user.getId());
        if (existing.isPresent()
                && existing.get().getRequestedAt().plusSeconds(cooldownSeconds).isAfter(now)) return;

        byte[] random = new byte[32];
        RANDOM.nextBytes(random);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        var token = existing.orElseGet(PasswordResetToken::new);
        token.setUser(user);
        token.setTokenHash(hash(raw));
        token.setRequestedAt(now);
        token.setExpiresAt(now.plus(expirationMinutes, ChronoUnit.MINUTES));
        tokens.save(token);
        // A fragment keeps the secret out of frontend access logs and Referer headers.
        String link = frontendUrl + "/reset-password#token=" + raw;
        events.publishEvent(new PasswordResetRequested(user.getEmail(), user.getProfileUsername(), link, expirationMinutes));
    }

    @Transactional
    public void resetPassword(String rawToken, String password) {
        String expectedHash = hash(rawToken);
        Long userId = tokens.findUserIdByTokenHash(expectedHash)
                .orElseThrow(InvalidPasswordResetTokenException::new);
        var user = users.findForPasswordRecoveryById(userId)
                .orElseThrow(InvalidPasswordResetTokenException::new);
        var token = tokens.findByUserId(userId)
                .filter(t -> expectedHash.equals(t.getTokenHash()) && t.getExpiresAt().isAfter(clock.instant()))
                .orElseThrow(InvalidPasswordResetTokenException::new);

        user.setPasswordHash(encoder.encode(password));
        user.setCredentialsVersion(user.getCredentialsVersion() + 1);
        users.save(user);
        tokens.delete(token);
        events.publishEvent(new PasswordChanged(user.getEmail()));
    }

    private static String hash(String raw) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
