package com.saving.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
@Schema(description = "Response after creating a new user account")
public class CreateUserResponse {

    @Schema(example = "c0000001-0000-0000-0000-000000000005")
    private String userId;

    @Schema(example = "mailinh92")
    private String username;

    @Schema(example = "CIF0005")
    private String cif;

    @Schema(example = "CUSTOMER")
    private String role;

    @Schema(example = "ACTIVE")
    private String status;

    private Instant createdAt;
}
