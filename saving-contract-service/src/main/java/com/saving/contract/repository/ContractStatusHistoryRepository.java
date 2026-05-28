package com.saving.contract.repository;

import com.saving.contract.entity.ContractStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ContractStatusHistoryRepository extends JpaRepository<ContractStatusHistory, UUID> {

    List<ContractStatusHistory> findByContractNoOrderByChangedAtAsc(String contractNo);
}
