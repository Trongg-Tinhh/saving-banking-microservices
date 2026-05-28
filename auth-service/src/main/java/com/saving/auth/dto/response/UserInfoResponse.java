package com.saving.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@Schema(description = "Current authenticated user information")
public class UserInfoResponse {

    @Schema(example = "c0000001-0000-0000-0000-000000000003")
    private String userId;

    @Schema(example = "customer001")
    private String username;

    @Schema(example = "CIF0001")
    private String cif;

    @Schema(example = "ACTIVE")
    private String status;

    @Schema(example = "[\"ROLE_CUSTOMER\"]")
    private List<String> roles;

    @Schema(example = "2025-05-20T08:30:00Z")
    private Instant lastLoginAt;
}
