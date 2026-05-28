package com.saving.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Response for internal token validation endpoint.
 * Called by other microservices via API Gateway to validate JWT.
 */
@Data
@Builder
@Schema(description = "Token validation result — used by other services for auth check")
public class TokenValidationResponse {

    @Schema(description = "Whether the token is valid", example = "true")
    private boolean valid;

    @Schema(description = "User ID extracted from token")
    private String userId;

    @Schema(description = "Username extracted from token", example = "customer001")
    private String username;

    @Schema(description = "CIF of the customer (null for staff)", example = "CIF0001")
    private String cif;

    @Schema(description = "Roles from token", example = "[\"ROLE_CUSTOMER\"]")
    private List<String> roles;

    @Schema(description = "Token expiry timestamp (epoch seconds)", example = "1735689600")
    private Long expiresAt;

    @Schema(description = "Reason if token is invalid", example = "TOKEN_EXPIRED")
    private String invalidReason;

    // ── Factory methods ──────────────────────────────────────────

    public static TokenValidationResponse invalid(String reason) {
        return TokenValidationResponse.builder()
                .valid(false)
                .invalidReason(reason)
                .build();
    }
}
