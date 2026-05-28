package com.saving.account.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UpdateAccountStatusRequest {

    @NotBlank(message = "Status is required")
    @Pattern(regexp = "^(ACTIVE|BLOCKED|CLOSED)$",
             message = "Status must be ACTIVE, BLOCKED, or CLOSED")
    private String status;

    private String reason;
}
