package com.saving.product.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateTermRequest {

    /** Rename the term label (e.g. "6 tháng" → "6 Tháng cố định") */
    @Size(max = 50, message = "Term label max 50 chars")
    private String termLabel;

    /** Enable / disable this term. Existing contracts are NOT affected. */
    private Boolean isActive;
}
