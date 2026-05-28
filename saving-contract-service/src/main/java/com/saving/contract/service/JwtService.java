package com.saving.contract.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

/**
 * Parses and validates JWTs issued by the Auth Service.
 * This service NEVER issues tokens — it only verifies them.
 * <p>
 * JWT claim layout (set by auth-service):
 *   sub       → username
 *   userId    → UUID string
 *   cif       → customer CIF (null for staff accounts)
 *   roles     → List&lt;String&gt; e.g. ["CUSTOMER"] or ["TELLER"]
 *   type      → "ACCESS" | "REFRESH"
 */
@Service
public class JwtService {

    private final SecretKey signingKey;

    public JwtService(@Value("${jwt.secret}") String secret) {
        byte[] key = secret.getBytes(StandardCharsets.UTF_8);
        // Ensure minimum 32 bytes for HS256
        if (key.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(key, 0, padded, 0, key.length);
            key = padded;
        }
        this.signingKey = Keys.hmacShaKeyFor(key);
    }

    /**
     * Parse and verify the given JWT, returning its claims.
     * Throws a JwtException sub-class on any failure (expired, invalid signature, etc.).
     */
    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    /** Returns the customer CIF embedded in the token, or {@code null} for staff accounts. */
    public String extractCif(String token) {
        return parseClaims(token).get("cif", String.class);
    }

    /**
     * Returns the roles list from the {@code "roles"} claim (e.g. ["CUSTOMER"]).
     * Never returns null — returns an empty list if the claim is absent.
     */
    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        Object roles = parseClaims(token).get("roles");
        if (roles instanceof List<?> list) {
            return (List<String>) list;
        }
        return Collections.emptyList();
    }
}
