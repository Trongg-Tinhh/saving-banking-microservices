package com.saving.auth.service;

import com.saving.auth.common.Constants;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * JWT Service — handles generation, validation, and claim extraction.
 * Uses JJWT 0.12.x API.
 */
@Service
@Slf4j
public class JwtService {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration-ms}")
    private long accessExpirationMs;

    @Value("${jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    // ── Key ──────────────────────────────────────────────────────

    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        // Ensure minimum 32 bytes (256 bits) for HS256
        if (keyBytes.length < 32) {
            byte[] paddedKey = new byte[32];
            System.arraycopy(keyBytes, 0, paddedKey, 0, keyBytes.length);
            return Keys.hmacShaKeyFor(paddedKey);
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // ── Generate Tokens ───────────────────────────────────────────

    public String generateAccessToken(String userId, String username, String cif, List<String> roles) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessExpirationMs);

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(username)
                .claim(Constants.CLAIM_USER_ID, userId)
                .claim(Constants.CLAIM_CIF, cif)
                .claim(Constants.CLAIM_ROLES, roles)
                .claim(Constants.CLAIM_TYPE, Constants.TOKEN_TYPE_ACCESS)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSigningKey())
                .compact();
    }

    public String generateRefreshToken(String userId, String username) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + refreshExpirationMs);

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(username)
                .claim(Constants.CLAIM_USER_ID, userId)
                .claim(Constants.CLAIM_TYPE, Constants.TOKEN_TYPE_REFRESH)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSigningKey())
                .compact();
    }

    // ── Extract Claims ────────────────────────────────────────────

    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        return claimsResolver.apply(extractAllClaims(token));
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String extractUserId(String token) {
        return extractClaim(token, claims -> claims.get(Constants.CLAIM_USER_ID, String.class));
    }

    public String extractCif(String token) {
        return extractClaim(token, claims -> claims.get(Constants.CLAIM_CIF, String.class));
    }

    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        return extractClaim(token, claims -> (List<String>) claims.get(Constants.CLAIM_ROLES));
    }

    public String extractTokenType(String token) {
        return extractClaim(token, claims -> claims.get(Constants.CLAIM_TYPE, String.class));
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    // ── Validation ────────────────────────────────────────────────

    public boolean isTokenValid(String token) {
        try {
            extractAllClaims(token);
            return true;
        } catch (ExpiredJwtException ex) {
            log.debug("Token expired: {}", ex.getMessage());
            return false;
        } catch (JwtException ex) {
            log.warn("Invalid JWT token: {}", ex.getMessage());
            return false;
        }
    }

    public boolean isAccessToken(String token) {
        try {
            String type = extractTokenType(token);
            return Constants.TOKEN_TYPE_ACCESS.equals(type);
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isRefreshToken(String token) {
        try {
            String type = extractTokenType(token);
            return Constants.TOKEN_TYPE_REFRESH.equals(type);
        } catch (Exception e) {
            return false;
        }
    }

    public long getAccessExpirationMs() { return accessExpirationMs; }

    /**
     * Validate token and return structured result for internal use.
     */
    public Map<String, Object> validateAndExtract(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return Map.of(
                "valid", true,
                "userId", claims.get(Constants.CLAIM_USER_ID, String.class),
                "username", claims.getSubject(),
                "cif", claims.getOrDefault(Constants.CLAIM_CIF, ""),
                "roles", claims.getOrDefault(Constants.CLAIM_ROLES, List.of()),
                "expiresAt", claims.getExpiration().toInstant().getEpochSecond()
            );
        } catch (ExpiredJwtException ex) {
            return Map.of("valid", false, "reason", "TOKEN_EXPIRED");
        } catch (JwtException ex) {
            return Map.of("valid", false, "reason", "TOKEN_INVALID");
        } catch (Exception ex) {
            return Map.of("valid", false, "reason", "TOKEN_PARSE_ERROR");
        }
    }
}
