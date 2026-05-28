package com.saving.account.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReleaseHoldRequest {

    /**
     * The hold reference to release. Must match an ACTIVE hold for this account.
     */
    @NotBlank(message = "Hold reference is required")
    @Size(max = 100)
    private String holdRef;

    private String reason;
}
