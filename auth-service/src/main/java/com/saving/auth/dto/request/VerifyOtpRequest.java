package com.saving.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "OTP verification request")
public class VerifyOtpRequest {

    @NotBlank(message = "User ID is required")
    @Schema(description = "User ID", example = "c0000001-0000-0000-0000-000000000003")
    private String userId;

    @NotBlank(message = "OTP code is required")
    @Size(min = 6, max = 6, message = "OTP must be 6 digits")
    @Pattern(regexp = "\\d{6}", message = "OTP must contain only digits")
    @Schema(description = "6-digit OTP code", example = "123456")
    private String otpCode;

    @NotBlank(message = "Purpose is required")
    @Schema(description = "OTP purpose", example = "LOGIN", allowableValues = {"LOGIN", "TRANSACTION", "RESET_PASSWORD"})
    private String purpose;
}
