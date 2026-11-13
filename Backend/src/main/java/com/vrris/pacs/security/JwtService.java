package com.vrris.pacs.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Dich vu JWT (IN-13): sinh va kiem tra token HS256.
 * Payload: sub = app_user.id, username, roles[], iat, exp.
 * Thoi han mac dinh: access token 15 phut.
 * Secret dai >= 32 byte lay tu cau hinh/Secret (APP_SECURITY_JWT_SECRET).
 */
@Component
public class JwtService {

    private final SecretKey key;
    private final long expirationMinutes;

    public JwtService(@Value("${app.security.jwt.secret}") String secret,
                      @Value("${app.security.jwt.expiration-minutes:15}") long expirationMinutes) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("JWT secret must be at least 32 bytes for HS256");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMinutes = expirationMinutes;
    }

    public String generateToken(Long userId, String username, List<String> roles) {
        Instant now = Instant.now();
        var builder = Jwts.builder()
                .setSubject(userId != null ? String.valueOf(userId) : username)
                .claim("username", username)
                .claim("roles", roles)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plus(expirationMinutes, ChronoUnit.MINUTES)))
                .signWith(key, SignatureAlgorithm.HS256);
        return builder.compact();
    }

    public String generateToken(String username, List<String> roles) {
        return generateToken(null, username, roles);
    }

    public Optional<Claims> parse(String token) {
        try {
            Jws<Claims> jws = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);
            return Optional.of(jws.getBody());
        } catch (JwtException | IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    public long getExpirationMinutes() {
        return expirationMinutes;
    }

    public long getExpirationSeconds() {
        return expirationMinutes * 60;
    }
}
