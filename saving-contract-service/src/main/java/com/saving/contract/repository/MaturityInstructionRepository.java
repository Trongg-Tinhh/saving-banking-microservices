package com.saving.contract.repository;

import com.saving.contract.entity.MaturityInstruction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MaturityInstructionRepository extends JpaRepository<MaturityInstruction, UUID> {

    Optional<MaturityInstruction> findByContract_ContractNo(String contractNo);

    boolean existsByContract_ContractNo(String contractNo);
}
