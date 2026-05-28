package com.saving.product.dto.response;

import com.saving.product.entity.EarlyWithdrawalPolicy;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class EarlyWithdrawalPolicyResponse {

    private UUID       policyId;
    private String     productCode;
    private Integer    minDaysHeld;
    private BigDecimal penaltyRate;
    private Boolean    useDemandRate;
    private BigDecimal demandRate;

    public static EarlyWithdrawalPolicyResponse from(EarlyWithdrawalPolicy p) {
        return EarlyWithdrawalPolicyResponse.builder()
                .policyId(p.getPolicyId())
                .productCode(p.getProduct() != null ? p.getProduct().getProductCode() : null)
                .minDaysHeld(p.getMinDaysHeld())
                .penaltyRate(p.getPenaltyRate())
                .useDemandRate(p.getUseDemandRate())
                .demandRate(p.getDemandRate())
                .build();
    }
}
