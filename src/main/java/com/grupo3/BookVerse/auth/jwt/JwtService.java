package com.grupo3.BookVerse.auth.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class JwtService {

    private static final String ROLES_CLAIM = "roles";

    @Value("${jwt.secret}")
    private String jwtSecretKey;

    @Value("${jwt.expiration}")
    private Long jwtExpiration;

    // In BookVerse, the JWT subject is the user's email
    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }


    public String generateToken(UserDetails userDetails) {

        Map<String, Object> claims = new HashMap<>();

        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        claims.put(ROLES_CLAIM, roles);
        if (userDetails instanceof com.grupo3.BookVerse.features.users.domain.UserEntity user) {
            claims.put("credentialsVersion", user.getCredentialsVersion());
        }

        return buildToken(claims, userDetails, jwtExpiration);
    }


    public List<GrantedAuthority> extractAuthorities(String token) {

        Claims claims = extractAllClaims(token);

        List<?> rawRoles = claims.get(ROLES_CLAIM, List.class);

        if (rawRoles == null) {
            return Collections.emptyList();
        }

        return rawRoles.stream()
                .map(Object::toString)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {

        final String email = extractEmail(token);

        return email.equals(userDetails.getUsername())
                && !isTokenExpired(token)
                && credentialsMatch(token, userDetails)
                && userDetails.isAccountNonExpired()
                && userDetails.isAccountNonLocked()
                && userDetails.isCredentialsNonExpired()
                && userDetails.isEnabled();
    }


    private boolean credentialsMatch(String token, UserDetails details) {
        if (!(details instanceof com.grupo3.BookVerse.features.users.domain.UserEntity user)) return true;
        Number version = extractClaim(token, claims -> claims.get("credentialsVersion", Number.class));
        return (version == null ? 0 : version.longValue()) == user.getCredentialsVersion();
    }

    private String buildToken(
            Map<String, Object> extraClaims,
            UserDetails userDetails,
            long expiration
    ) {

        return Jwts.builder()
                .claims(extraClaims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSignInKey())
                .compact();
    }


    private <T> T extractClaim(
            String token,
            Function<Claims, T> claimsResolver
    ) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }


    private Claims extractAllClaims(String token) {

        return Jwts.parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSignInKey() {

        byte[] keyBytes = jwtSecretKey.getBytes(StandardCharsets.UTF_8);

        return Keys.hmacShaKeyFor(keyBytes);
    }


    private boolean isTokenExpired(String token) {

        Date expiration = extractClaim(
                token,
                Claims::getExpiration
        );

        return expiration.before(new Date());
    }
}