package com.grupo3.BookVerse.auth;

import com.grupo3.BookVerse.auth.domain.PasswordResetToken;
import com.grupo3.BookVerse.auth.jwt.JwtService;
import com.grupo3.BookVerse.auth.repository.PasswordResetTokenRepository;
import com.grupo3.BookVerse.auth.service.PasswordRecoveryService;
import com.grupo3.BookVerse.common.exception.InvalidPasswordResetTokenException;
import com.grupo3.BookVerse.features.subscriptions.domain.*;
import com.grupo3.BookVerse.features.users.domain.*;
import com.grupo3.BookVerse.features.users.repository.UserRepository;
import com.grupo3.BookVerse.infrastructure.EmailService;
import com.grupo3.BookVerse.infrastructure.notifications.PasswordRecoveryMailListener;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.persistence.*;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.*;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.*;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.*;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;
import javax.sql.DataSource;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringJUnitConfig(PasswordRecoveryTest.Config.class)
class PasswordRecoveryTest {
    @PersistenceContext EntityManager em;
    @Autowired PasswordRecoveryService recovery;
    @Autowired UserRepository users;
    @Autowired PasswordResetTokenRepository tokens;
    @Autowired PasswordEncoder encoder;
    @Autowired EmailService emails;
    @Autowired PlatformTransactionManager manager;
    @Autowired Clock clock;
    final String email = "reader@example.com";
    final String oldPassword = "Previous1!";
    final String newPassword = "Updated123!";
    static final String SECRET = "password-recovery-test-secret-at-least-32-bytes";
    TransactionTemplate tx;

    @BeforeEach void setup() {
        reset(emails);
        tx = new TransactionTemplate(manager);
        tx.executeWithoutResult(status -> {
            tokens.deleteAllInBatch();
            em.createQuery("delete from UserEntity").executeUpdate();
            em.createQuery("delete from SubscriptionEntity").executeUpdate();
            var plan = SubscriptionEntity.builder().type(SubscriptionType.FREE)
                    .maxStoriesPublished(10).maxActiveStoriesReading(3).build();
            em.persist(plan);
            var user = UserEntity.builder().username("Reader").email(email)
                    .passwordHash(encoder.encode(oldPassword)).subscription(plan).build();
            em.persist(user);
        });
    }

    String issue() {
        recovery.requestReset(email);
        var link = ArgumentCaptor.forClass(String.class);
        verify(emails).sendPasswordResetEmail(eq(email), nullable(String.class), link.capture(), eq(30L));
        assertTrue(link.getValue().startsWith("http://localhost:4200/reset-password#token="));
        assertNull(URI.create(link.getValue()).getQuery());
        return URI.create(link.getValue()).getFragment().substring("token=".length());
    }

    UserEntity user() { return users.findByEmailWithRoles(email).orElseThrow(); }
    JwtService jwt() {
        var jwt = new JwtService();
        ReflectionTestUtils.setField(jwt, "jwtSecretKey", SECRET);
        ReflectionTestUtils.setField(jwt, "jwtExpiration", 3_600_000L);
        return jwt;
    }

    @Test void onlyStoresADigestAndDoesNotChangeCredentialsBeforeConfirmation() {
        String raw = issue();
        assertEquals(32, Base64.getUrlDecoder().decode(raw).length);
        var saved = tokens.findAll().getFirst();
        assertEquals(64, saved.getTokenHash().length());
        assertNotEquals(raw, saved.getTokenHash());
        assertEquals(clock.instant().plusSeconds(1800), saved.getExpiresAt());
        assertTrue(encoder.matches(oldPassword, user().getPasswordHash()));
        assertEquals(0, user().getCredentialsVersion());
        verify(emails, never()).sendPasswordChangedEmail(anyString());
    }

    @Test void missingAndInactiveAccountsDoNotCreateTokensOrSendMail() {
        recovery.requestReset("missing@example.com");
        tx.executeWithoutResult(status -> user().setStatus(UserStatus.SUSPENDED));
        recovery.requestReset(email);
        assertEquals(0, tokens.count());
        verifyNoInteractions(emails);
    }

    @Test void acceptsTheAccountEmailWithoutCaseSensitivity() {
        recovery.requestReset("  READER@EXAMPLE.COM  ");
        verify(emails).sendPasswordResetEmail(eq(email), nullable(String.class), anyString(), eq(30L));
        assertEquals(1, tokens.count());
    }

    @Test void rateLimitsResendsAndANewLinkInvalidatesThePreviousLink() {
        String first = issue();
        recovery.requestReset(email);
        verify(emails, times(1)).sendPasswordResetEmail(anyString(), nullable(String.class), anyString(), anyLong());
        tx.executeWithoutResult(status -> tokens.findAll().getFirst()
                .setRequestedAt(clock.instant().minusSeconds(121)));
        recovery.requestReset(email);
        var links = ArgumentCaptor.forClass(String.class);
        verify(emails, times(2)).sendPasswordResetEmail(eq(email), nullable(String.class), links.capture(), eq(30L));
        String second = URI.create(links.getAllValues().getLast()).getFragment().substring(6);
        assertNotEquals(first, second);
        assertEquals(1, tokens.count());
        assertThrows(InvalidPasswordResetTokenException.class, () -> recovery.resetPassword(first, newPassword));
        recovery.resetPassword(second, newPassword);
        assertTrue(encoder.matches(newPassword, user().getPasswordHash()));
    }

    @Test void aSuccessfulResetIsSingleUseAndRevokesExistingJwtSessions() {
        var jwt = jwt();
        String previousSession = jwt.generateToken(user());
        String legacySession = Jwts.builder().subject(email).issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3_600_000))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8))).compact();
        assertTrue(jwt.isTokenValid(legacySession, user()));
        String raw = issue();
        recovery.resetPassword(raw, newPassword);
        var changed = user();
        assertTrue(encoder.matches(newPassword, changed.getPasswordHash()));
        assertFalse(encoder.matches(oldPassword, changed.getPasswordHash()));
        assertEquals(1, changed.getCredentialsVersion());
        assertEquals(0, tokens.count());
        assertFalse(jwt.isTokenValid(previousSession, changed));
        assertFalse(jwt.isTokenValid(legacySession, changed));
        assertTrue(jwt.isTokenValid(jwt.generateToken(changed), changed));
        verify(emails).sendPasswordChangedEmail(email);
        assertThrows(InvalidPasswordResetTokenException.class, () -> recovery.resetPassword(raw, "Different1!"));
    }

    @Test void expiredAndUnknownLinksCannotChangeThePassword() {
        String raw = issue();
        tx.executeWithoutResult(status -> tokens.findAll().getFirst().setExpiresAt(clock.instant()));
        for (String candidate : List.of(raw, "x".repeat(43))) {
            assertThrows(InvalidPasswordResetTokenException.class, () -> recovery.resetPassword(candidate, newPassword));
        }
        assertTrue(encoder.matches(oldPassword, user().getPasswordHash()));
        verify(emails, never()).sendPasswordChangedEmail(anyString());
    }

    @Test void aLinkCannotReactivateASuspendedAccount() {
        String raw = issue();
        tx.executeWithoutResult(status -> user().setStatus(UserStatus.SUSPENDED));
        assertThrows(InvalidPasswordResetTokenException.class, () -> recovery.resetPassword(raw, newPassword));
        assertTrue(encoder.matches(oldPassword, user().getPasswordHash()));
    }

    @Test void anInactiveAccountIsReactivatedOnlyAfterUsingTheEmailedLink() {
        var before = user();
        var id = before.getId();
        var externalId = before.getIdExternal();
        var subscriptionId = before.getSubscription().getId();
        tx.executeWithoutResult(status -> user().setStatus(UserStatus.INACTIVE));
        String raw = issue();
        assertEquals(UserStatus.INACTIVE, user().getStatus());
        assertTrue(encoder.matches(oldPassword, user().getPasswordHash()));

        recovery.resetPassword(raw, newPassword);
        var restored = user();
        assertEquals(UserStatus.ACTIVE, restored.getStatus());
        assertEquals(id, restored.getId());
        assertEquals(externalId, restored.getIdExternal());
        assertEquals(subscriptionId, restored.getSubscription().getId());
        assertEquals("Reader", restored.getProfileUsername());
        assertEquals(1, restored.getCredentialsVersion());
        assertTrue(encoder.matches(newPassword, restored.getPasswordHash()));
        assertThrows(InvalidPasswordResetTokenException.class, () -> recovery.resetPassword(raw, newPassword));
    }

    @Test void bannedAccountsCannotRequestOrUseARecoveryLink() {
        String raw = issue();
        tx.executeWithoutResult(status -> user().setStatus(UserStatus.BANNED));
        assertThrows(InvalidPasswordResetTokenException.class, () -> recovery.resetPassword(raw, newPassword));
        assertEquals(UserStatus.BANNED, user().getStatus());
    }

    @Test void rollbackNeverSendsAnUnusableLink() {
        tx.executeWithoutResult(status -> {
            recovery.requestReset(email);
            verifyNoInteractions(emails);
            status.setRollbackOnly();
        });
        assertEquals(0, tokens.count());
        verifyNoInteractions(emails);
    }

    @Test void concurrentRequestsCanOnlyConsumeTheTokenOnce() throws Exception {
        String raw = issue();
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            Callable<Boolean> reset = () -> {
                start.await();
                try {
                    recovery.resetPassword(raw, newPassword);
                    return true;
                } catch (InvalidPasswordResetTokenException expected) {
                    return false;
                }
            };
            var first = executor.submit(reset);
            var second = executor.submit(reset);
            start.countDown();
            int successes = (first.get(10, TimeUnit.SECONDS) ? 1 : 0)
                    + (second.get(10, TimeUnit.SECONDS) ? 1 : 0);
            assertEquals(1, successes);
        }
        assertEquals(1, user().getCredentialsVersion());
        verify(emails, times(1)).sendPasswordChangedEmail(email);
    }

    @Configuration
    @EnableTransactionManagement
    @EnableJpaRepositories(basePackageClasses = {UserRepository.class, PasswordResetTokenRepository.class})
    @Import({PasswordRecoveryService.class, PasswordRecoveryMailListener.class})
    static class Config {
        @Bean DataSource dataSource() { return new DriverManagerDataSource("jdbc:h2:mem:password-recovery;MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", ""); }
        @Bean LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource source) {
            var bean = new LocalContainerEntityManagerFactoryBean();
            bean.setDataSource(source);
            bean.setPackagesToScan("com.grupo3.BookVerse.features", "com.grupo3.BookVerse.auth.domain");
            bean.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
            var properties = new Properties();
            properties.setProperty("hibernate.hbm2ddl.auto", "create-drop");
            bean.setJpaProperties(properties);
            return bean;
        }
        @Bean PlatformTransactionManager transactionManager(EntityManagerFactory factory) { return new JpaTransactionManager(factory); }
        @Bean PasswordEncoder encoder() { return new BCryptPasswordEncoder(); }
        @Bean EmailService emails() { return mock(EmailService.class); }
        @Bean Clock clock() { return Clock.fixed(Instant.parse("2026-11-04T12:00:00Z"), ZoneOffset.UTC); }
    }
}
