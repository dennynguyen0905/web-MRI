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
 * Khung JWT toi thieu (IN-13) - chi sinh/doc token HS256 bang khoa bi mat chung. CHUA day du RBAC
 * (vai tro lay tho tu claim "roles" trong token, chua tra DB app_user/role/user_role) - xem
 * doc/15-kien-truc-backend.md muc 7 diem 4 va README IN-14.
 *
 * secret dang doc tu app.security.jwt.secret (application.yml) - gia tri demo trong repo, PHAI
 * chuyen sang Kubernetes Secret truoc khi dung ngoai moi truong dev (IN-17).
 */
@Component
public class JwtService {

    private final SecretKey key;
    private final long expirationMinutes;

    public JwtService(@Value("${app.security.jwt.secret}") String secret,
                       @Value("${app.security.jwt.expiration-minutes:60}") long expirationMinutes) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMinutes = expirationMinutes;
    }

    public String generateToken(String username, List<String> roles) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setSubject(username)
                .claim("roles", roles)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plus(expirationMinutes, ChronoUnit.MINUTES)))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
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
}
