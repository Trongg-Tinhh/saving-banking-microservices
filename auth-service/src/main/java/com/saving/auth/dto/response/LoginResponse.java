package com.saving.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@Schema(description = "Login response containing JWT tokens and user info")
public class LoginResponse {

    @Schema(description = "JWT Access Token", example = "eyJhbGciOiJIUzI1NiJ9...")
    private String accessToken;

    @Schema(description = "JWT Refresh Token", example = "eyJhbGciOiJIUzI1NiJ9...")
    private String refreshToken;

    @Schema(description = "Token type", example = "Bearer")
    private String tokenType;

    @Schema(description = "Access token expiry in seconds", example = "3600")
    private long expiresIn;

    @Schema(description = "User ID", example = "c0000001-0000-0000-0000-000000000003")
    private String userId;

    @Schema(description = "Username", example = "customer001")
    private String username;

    @Schema(description = "Customer CIF (null for staff accounts)", example = "CIF0001")
    private String cif;

    @Schema(description = "Assigned roles", example = "[\"ROLE_CUSTOMER\"]")
    private List<String> roles;
}
