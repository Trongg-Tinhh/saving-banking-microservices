package com.saving.contract.dto.response;

import com.saving.contract.entity.ContractStatusHistory;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Builder
public class ContractStatusHistoryResponse {

    private String         fromStatus;
    private String         toStatus;
    private String         changedBy;
    private String         reason;
    private OffsetDateTime changedAt;
    private String         correlationId;

    public static ContractStatusHistoryResponse from(ContractStatusHistory h) {
        return ContractStatusHistoryResponse.builder()
                .fromStatus(h.getFromStatus())
                .toStatus(h.getToStatus())
                .changedBy(h.getChangedBy())
                .reason(h.getReason())
                .changedAt(h.getChangedAt())
                .correlationId(h.getCorrelationId())
                .build();
    }
}
