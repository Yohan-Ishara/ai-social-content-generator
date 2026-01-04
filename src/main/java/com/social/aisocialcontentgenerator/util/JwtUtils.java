package com.social.aisocialcontentgenerator.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Component
public class JwtUtils {

    @Value("${app.jwtSecret:change_this_secret_change_this_secret_change_this}")
    private String jwtSecret;

    @Value("${app.jwtExpirationMs:86400000}") // 24 hours
    private long jwtExpirationMs;

    // Key object created from the secret
    private Key signingKey;

    @PostConstruct
    public void init() {
        // Ensure secret is long enough for HS256 (>= 32 bytes). If your secret is short,
        // repeat/expand or use a securely generated key.
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);

        // If someone passed a base64-encoded secret, detect and decode it:
        try {
            // Quick heuristic: if it looks like base64 (only base64 chars and length % 4 == 0)
            if ((jwtSecret.matches("^[A-Za-z0-9+/=]+$") && (jwtSecret.length() % 4 == 0))) {
                // decode and use decoded bytes
                byte[] decoded = java.util.Base64.getDecoder().decode(jwtSecret);
                signingKey = Keys.hmacShaKeyFor(decoded);
                return;
            }
        } catch (IllegalArgumentException ignored) {
            // not valid base64 -> fall back to using raw bytes below
        }

        // Use raw UTF-8 bytes (recommended: set a strong secret of length >= 32 bytes)
        if (keyBytes.length < 32) {
            // if the secret is too short, pad it deterministically (not ideal for prod).
            // Recommended: set a secure random 32+ byte secret in env.
            byte[] expanded = new byte[32];
            System.arraycopy(keyBytes, 0, expanded, 0, keyBytes.length);
            for (int i = keyBytes.length; i < expanded.length; i++) {
                expanded[i] = (byte) ('A' + (i % 26)); // simple padding; replace in prod
            }
            signingKey = Keys.hmacShaKeyFor(expanded);
        } else {
            signingKey = Keys.hmacShaKeyFor(keyBytes);
        }
    }

    // Generate token (subject is email in your flow)
    public String generateToken(String subject) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    // Extract subject
    public String getSubjectFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    // Validate token
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(signingKey).build().parseClaimsJws(token);
            return true;
        } catch (JwtException e) {
            // log if you want in dev
            System.out.println("JWT validation failed: " + e.getMessage());
        }
        return false;
    }
}
