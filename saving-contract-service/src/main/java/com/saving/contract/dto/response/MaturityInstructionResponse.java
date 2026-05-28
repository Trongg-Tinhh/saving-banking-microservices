package com.saving.contract.dto.response;

import com.saving.contract.entity.MaturityInstruction;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class MaturityInstructionResponse {
    private UUID   instructionId;
    private String contractNo;
    private String instructionType;
    private String newTermId;
    private String receivingAccountNo;

    public static MaturityInstructionResponse from(MaturityInstruction m) {
        return MaturityInstructionResponse.builder()
                .instructionId(m.getInstructionId())
                .contractNo(m.getContract() != null ? m.getContract().getContractNo() : null)
                .instructionType(m.getInstructionType())
                .newTermId(m.getNewTermId())
                .receivingAccountNo(m.getReceivingAccountNo())
                .build();
    }
}
