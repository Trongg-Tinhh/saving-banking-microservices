package com.saving.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Login request payload")
public class LoginRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 100, message = "Username must be 3-100 characters")
    @Schema(description = "Username", example = "customer001")
    private String username;

    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 128, message = "Password must be 6-128 characters")
    @Schema(description = "Password", example = "Test@123")
    private String password;

    @Schema(description = "Client IP address (optional, set by gateway)", example = "192.168.1.1")
    private String clientIp;

    @Schema(description = "Device/User-Agent info", example = "Mozilla/5.0...")
    private String deviceInfo;
}
