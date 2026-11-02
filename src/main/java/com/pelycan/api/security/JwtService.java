package com.pelycan.api.security;

import com.pelycan.api.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private final JwtProperties properties;
    private final SecretKey key;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        this.key = hmacKeyFromSecret(properties.secret());
    }

    /**
     * Same semantics as Node jsonwebtoken: UTF-8 secret bytes as HMAC key.
     * jjwt's {@link Keys#hmacShaKeyFor} rejects &lt; 256 bits; {@link SecretKeySpec} matches legacy secrets.
     */
    private static SecretKey hmacKeyFromSecret(String secret) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length >= 32) {
            return Keys.hmacShaKeyFor(bytes);
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    public String createToken(UUID userId, String role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .claim("userId", userId.toString())
                .claim("role", role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(properties.expirationHours(), ChronoUnit.HOURS)))
                .signWith(key)
                .compact();
    }

    public UUID extractUserId(String token) {
        Claims claims = parse(token);
        return UUID.fromString(claims.get("userId", String.class));
    }

    public boolean isValid(String token) {
        try {
            parse(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
