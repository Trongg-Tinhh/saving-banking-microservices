package com.saving.contract.service;

import com.saving.contract.client.AccountServiceClient;
import com.saving.contract.client.CustomerServiceClient;
import com.saving.contract.client.ProductServiceClient;
import com.saving.contract.client.dto.CustomerValidationResult;
import com.saving.contract.client.dto.AccountValidationResult;
import com.saving.contract.client.dto.ProductRateResult;
import com.saving.contract.common.Constants;
import com.saving.contract.dto.request.CloseContractRequest;
import com.saving.contract.dto.request.OpenContractRequest;
import com.saving.contract.dto.request.UpdateMaturityInstructionRequest;
import com.saving.contract.dto.response.CloseContractResponse;
import com.saving.contract.dto.response.ContractResponse;
import com.saving.contract.dto.response.ContractStatusHistoryResponse;
import com.saving.contract.dto.response.ContractSummaryResponse;
import com.saving.contract.entity.ContractStatusHistory;
import com.saving.contract.entity.MaturityInstruction;
import com.saving.contract.entity.SavingContract;
import com.saving.contract.exception.BusinessException;
import com.saving.contract.exception.ErrorCode;
import com.saving.contract.repository.ContractStatusHistoryRepository;
import com.saving.contract.repository.MaturityInstructionRepository;
import com.saving.contract.repository.SavingContractRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class SavingContractService {

    private final SavingContractRepository      contractRepository;
    private final ContractStatusHistoryRepository historyRepository;
    private final MaturityInstructionRepository  maturityInstructionRepository;

    private final CustomerServiceClient customerClient;
    private final AccountServiceClient  accountClient;
    private final ProductServiceClient  productClient;

    private final ContractNumberGenerator contractNumberGenerator;
    private final InterestCalculator      interestCalculator;
    private final ContractEventPublisher  eventPublisher;

    // ────────────────────────────────────────────────────────────────────────
    // Open Contract
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Full contract opening flow:
     * 1. Validate customer (KYC VERIFIED + ACTIVE)
     * 2. Validate source account (ACTIVE + sufficient balance)
     * 3. Query & lock in interest rate from product service
     * 4. Validate amount against product min/max
     * 5. Persist contract (PENDING) + history + maturity instruction
     * 6. Debit source account via account service
     * 7. Update contract → ACTIVE
     * 8. Publish CONTRACT_OPENED event
     *
     * @param request     the open-contract request body
     * @param bearerToken the caller's JWT (forwarded to account-service for debit)
     * @param openedBy    username extracted from JWT claims
     */
    @Transactional
    public ContractResponse openContract(OpenContractRequest request,
                                         String bearerToken,
                                         String openedBy) {

        String correlationId = MDC.get("correlationId");
        log.info("[{}] Opening contract for CIF={} product={} term={}",
                correlationId, request.getCif(), request.getProductCode(), request.getTermId());

        // ── 1. Validate customer ─────────────────────────────────────────────
        CustomerValidationResult customer =
                customerClient.validateCustomer(request.getCif());
        if (!customer.isValid()) {
            log.warn("[{}] Customer validation failed: {}", correlationId, customer.getInvalidReason());
            throw new BusinessException(ErrorCode.CUSTOMER_NOT_VALID, customer.getInvalidReason());
        }

        // ── 2. Validate source account (sufficient funds) ────────────────────
        AccountValidationResult account =
                accountClient.validateAccount(request.getSourceAccountNo(), request.getPrincipalAmount());
        if (!account.isValid()) {
            log.warn("[{}] Account validation failed: {}", correlationId, account.getInvalidReason());
            String reason = account.getInvalidReason();
            if (reason != null && reason.toLowerCase().contains("insufficient")) {
                throw new BusinessException(ErrorCode.INSUFFICIENT_FUNDS, reason);
            }
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_VALID, reason);
        }

        // ── 3. Lock in interest rate ─────────────────────────────────────────
        LocalDate openDate = request.getOpenDate() != null ? request.getOpenDate() : LocalDate.now();
        ProductRateResult rate =
                productClient.queryRate(request.getProductCode(), request.getTermId(), openDate);

        // ── 4. Validate amount against product limits ────────────────────────
        if (rate.getMinAmount() != null
                && request.getPrincipalAmount().compareTo(rate.getMinAmount()) < 0) {
            throw new BusinessException(ErrorCode.AMOUNT_BELOW_MINIMUM,
                    "Minimum amount is " + rate.getMinAmount() + " " + rate.getCurrency());
        }
        if (rate.getMaxAmount() != null
                && request.getPrincipalAmount().compareTo(rate.getMaxAmount()) > 0) {
            throw new BusinessException(ErrorCode.AMOUNT_ABOVE_MAXIMUM,
                    "Maximum amount is " + rate.getMaxAmount() + " " + rate.getCurrency());
        }

        // ── 5. Derive maturity date ──────────────────────────────────────────
        int termDays = rate.getTermDays() != null ? rate.getTermDays()
                : (rate.getTermMonths() != null ? rate.getTermMonths() * 30 : 0);
        if (termDays <= 0) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_VALID,
                    "Term has no valid duration configured");
        }
        LocalDate maturityDate = openDate.plusDays(termDays);

        // ── 6. Persist contract in PENDING state ─────────────────────────────
        String contractNo = contractNumberGenerator.generate();

        SavingContract contract = SavingContract.builder()
                .contractNo(contractNo)
                .cif(request.getCif())
                .productCode(request.getProductCode())
                .termId(request.getTermId())
                .principalAmount(request.getPrincipalAmount())
                .interestRate(rate.getAnnualRate())
                .currency(rate.getCurrency())
                .openDate(openDate)
                .maturityDate(maturityDate)
                .status(Constants.ContractStatus.PENDING)
                .interestPaymentMethod(rate.getInterestPaymentMethod())
                .sourceAccountNo(request.getSourceAccountNo())
                .branchCode(request.getBranchCode())
                .openedBy(openedBy)
                .build();

        contract = contractRepository.save(contract);   // capture managed instance (merge path)

        // Status history: PENDING entry
        saveHistory(contract, null, Constants.ContractStatus.PENDING, openedBy,
                "Contract created", correlationId);

        // Maturity instruction (optional)
        if (request.getMaturityInstruction() != null) {
            OpenContractRequest.MaturityInstructionDto mid = request.getMaturityInstruction();
            MaturityInstruction instruction = MaturityInstruction.builder()
                    .contract(contract)
                    .instructionType(mid.getInstructionType())
                    .newTermId(mid.getNewTermId())
                    .receivingAccountNo(mid.getReceivingAccountNo())
                    .build();
            maturityInstructionRepository.save(instruction);
            contract.setMaturityInstruction(instruction);
        }

        // ── 7. Debit source account ──────────────────────────────────────────
        String debitRef = "CONTRACT-OPEN-" + contractNo;
        try {
            accountClient.debitAccount(
                    request.getSourceAccountNo(),
                    request.getPrincipalAmount(),
                    debitRef,
                    "Contract opening deposit for " + contractNo,
                    bearerToken);
        } catch (BusinessException ex) {
            log.error("[{}] Debit failed for contract {}; marking FAILED. reason={}",
                    correlationId, contractNo, ex.getMessage());
            contract.setStatus(Constants.ContractStatus.FAILED);
            saveHistory(contract, Constants.ContractStatus.PENDING,
                    Constants.ContractStatus.FAILED, openedBy,
                    "Debit failed: " + ex.getMessage(), correlationId);
            contractRepository.save(contract);
            throw ex;
        }

        // ── 8. Activate contract ─────────────────────────────────────────────
        contract.setStatus(Constants.ContractStatus.ACTIVE);
        saveHistory(contract, Constants.ContractStatus.PENDING,
                Constants.ContractStatus.ACTIVE, openedBy,
                "Debit successful, contract activated", correlationId);
        contractRepository.save(contract);

        // ── 9. Publish event ─────────────────────────────────────────────────
        eventPublisher.publishContractOpened(contract);

        log.info("[{}] Contract {} opened successfully (maturity={})",
                correlationId, contractNo, maturityDate);

        return ContractResponse.from(contract);
    }

    // ────────────────────────────────────────────────────────────────────────
    // Close Contract
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Close a contract — either at maturity or early withdrawal.
     *
     * Flow:
     * 1. Load contract (must be ACTIVE or MATURED)
     * 2. Determine close type (MATURITY vs EARLY_WITHDRAWAL)
     * 3. Calculate interest based on close type
     * 4. Credit total payout (principal + interest) back to source account
     * 5. Update contract status (CLOSED or EARLY_CLOSED)
     * 6. Publish event
     */
    @Transactional
    public CloseContractResponse closeContract(String contractNo,
                                               CloseContractRequest request,
                                               String bearerToken,
                                               String closedBy) {

        String correlationId = MDC.get("correlationId");
        log.info("[{}] Closing contract {} by {}", correlationId, contractNo, closedBy);

        // ── 1. Load contract ──────────────────────────────────────────────────
        SavingContract contract = contractRepository.findByContractNoWithInstruction(contractNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONTRACT_NOT_FOUND, contractNo));

        String currentStatus = contract.getStatus();

        // Terminal states — cannot close again
        if (Constants.ContractStatus.CLOSED.equals(currentStatus)
                || Constants.ContractStatus.EARLY_CLOSED.equals(currentStatus)
                || Constants.ContractStatus.CANCELLED.equals(currentStatus)
                || Constants.ContractStatus.FAILED.equals(currentStatus)) {
            throw new BusinessException(ErrorCode.CONTRACT_ALREADY_CLOSED,
                    "Contract " + contractNo + " is already in terminal state: " + currentStatus);
        }

        // Must be ACTIVE or MATURED to close
        if (!Constants.ContractStatus.ACTIVE.equals(currentStatus)
                && !Constants.ContractStatus.MATURED.equals(currentStatus)) {
            throw new BusinessException(ErrorCode.CONTRACT_NOT_ACTIVE,
                    "Contract " + contractNo + " has status: " + currentStatus);
        }

        // ── 2. Determine close type ───────────────────────────────────────────
        boolean earlyWithdrawal = contract.isEarlyWithdrawal();
        String  newStatus   = earlyWithdrawal
                ? Constants.ContractStatus.EARLY_CLOSED
                : Constants.ContractStatus.CLOSED;
        String  closeType   = earlyWithdrawal
                ? Constants.CloseType.EARLY_WITHDRAWAL
                : Constants.CloseType.MATURITY;

        // ── 3. Calculate interest ─────────────────────────────────────────────
        BigDecimal principal = contract.getPrincipalAmount();
        BigDecimal interest;
        long daysHeld = contract.getDaysHeld();

        if (earlyWithdrawal) {
            // Determine early-withdrawal demand rate from product service
            // (already stored in earlyWithdrawalDemandRate if we had it; use a re-query)
            // For simplicity: we re-query the product service to get the demand rate
            ProductRateResult rateInfo = productClient.queryRate(
                    contract.getProductCode(),
                    contract.getTermId(),
                    contract.getOpenDate());

            BigDecimal demandRate = (rateInfo.getEarlyWithdrawalUseDemandRate() != null
                    && rateInfo.getEarlyWithdrawalUseDemandRate()
                    && rateInfo.getEarlyWithdrawalDemandRate() != null)
                    ? rateInfo.getEarlyWithdrawalDemandRate()
                    : BigDecimal.ZERO;

            interest = interestCalculator.calculateEarlyWithdrawalInterest(
                    principal, demandRate, daysHeld);
        } else {
            // Full-term interest using locked-in rate and contracted term days
            long termDays = contract.getOpenDate().until(contract.getMaturityDate(), ChronoUnit.DAYS);
            interest = interestCalculator.calculateFullTermInterest(
                    principal, contract.getInterestRate(), (int) termDays);
        }

        BigDecimal totalPayout = interestCalculator.totalPayout(principal, interest);
        String creditAccountNo = request.getReceivingAccountNo() != null
                ? request.getReceivingAccountNo()
                : contract.getSourceAccountNo();

        // ── 4. Credit payout ──────────────────────────────────────────────────
        String creditRef = "CONTRACT-CLOSE-" + contractNo;
        accountClient.creditAccount(
                creditAccountNo,
                totalPayout,
                creditRef,
                "Contract payout: " + contractNo + " (" + closeType + ")",
                bearerToken);

        // ── 5. Update contract ────────────────────────────────────────────────
        OffsetDateTime now = OffsetDateTime.now();
        contract.setStatus(newStatus);
        contract.setClosedAt(now);
        contract.setCloseType(closeType);
        saveHistory(contract, currentStatus, newStatus, closedBy,
                request.getReason() != null ? request.getReason() : closeType, correlationId);
        contractRepository.save(contract);

        // ── 6. Publish event ──────────────────────────────────────────────────
        eventPublisher.publishContractClosed(contract, interest, totalPayout);

        log.info("[{}] Contract {} closed. type={} payout={} {}",
                correlationId, contractNo, closeType, totalPayout, contract.getCurrency());

        return CloseContractResponse.builder()
                .contractNo(contractNo)
                .status(newStatus)
                .closeType(closeType)
                .closedDate(LocalDate.now())
                .closedAt(now)
                .principalAmount(principal)
                .interestEarned(interest)
                .totalPayout(totalPayout)
                .creditedToAccountNo(creditAccountNo)
                .daysHeld(daysHeld)
                .interestRate(contract.getInterestRate())
                .earlyWithdrawal(earlyWithdrawal)
                .build();
    }

    // ────────────────────────────────────────────────────────────────────────
    // Query
    // ────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ContractResponse getContract(String contractNo) {
        SavingContract contract = contractRepository.findByContractNoWithInstruction(contractNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONTRACT_NOT_FOUND, contractNo));
        return ContractResponse.from(contract);
    }

    /**
     * Flexible list — both params are optional:
     *   cif=null, status=null  → all contracts (staff view)
     *   cif=X,    status=null  → all contracts for one customer
     *   cif=null, status=X     → all contracts with that status
     *   cif=X,    status=X     → filtered
     * The repository JPQL already handles NULL via "IS NULL OR c.field = :param".
     */
    @Transactional(readOnly = true)
    public Page<ContractSummaryResponse> listContracts(String cif, String status, Pageable pageable) {
        return contractRepository.findByCifAndStatus(cif, status, pageable)
                .map(ContractSummaryResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<ContractSummaryResponse> getContractsByCif(String cif, Pageable pageable) {
        return contractRepository.findByCif(cif, pageable)
                .map(ContractSummaryResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<ContractSummaryResponse> getContractsByStatus(String status, Pageable pageable) {
        return contractRepository.findByStatus(status, pageable)
                .map(ContractSummaryResponse::from);
    }

    // ────────────────────────────────────────────────────────────────────────
    // Maturity Instruction
    // ────────────────────────────────────────────────────────────────────────

    @Transactional
    public ContractResponse updateMaturityInstruction(String contractNo,
                                                       UpdateMaturityInstructionRequest request,
                                                       String updatedBy) {

        SavingContract contract = contractRepository.findByContractNoWithInstruction(contractNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONTRACT_NOT_FOUND, contractNo));

        if (!Constants.ContractStatus.ACTIVE.equals(contract.getStatus())) {
            throw new BusinessException(ErrorCode.CONTRACT_NOT_ACTIVE, contractNo);
        }

        MaturityInstruction instruction = contract.getMaturityInstruction();
        if (instruction == null) {
            instruction = MaturityInstruction.builder()
                    .contract(contract)
                    .build();
        }

        instruction.setInstructionType(request.getInstructionType());
        instruction.setNewTermId(request.getNewTermId());
        instruction.setReceivingAccountNo(request.getReceivingAccountNo());
        maturityInstructionRepository.save(instruction);
        contract.setMaturityInstruction(instruction);

        log.info("[{}] Maturity instruction updated for contract {} by {}",
                MDC.get("correlationId"), contractNo, updatedBy);

        return ContractResponse.from(contract);
    }

    // ────────────────────────────────────────────────────────────────────────
    // Internal (called by Lifecycle Service)
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Mark a contract as MATURED (called by the Saving Lifecycle Service via internal API).
     */
    @Transactional
    public ContractResponse markMatured(String contractNo) {
        SavingContract contract = contractRepository.findByContractNoWithInstruction(contractNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONTRACT_NOT_FOUND, contractNo));

        if (!Constants.ContractStatus.ACTIVE.equals(contract.getStatus())) {
            throw new BusinessException(ErrorCode.CONTRACT_NOT_ACTIVE,
                    "Contract " + contractNo + " is not ACTIVE, current status: " + contract.getStatus());
        }

        contract.setStatus(Constants.ContractStatus.MATURED);
        saveHistory(contract, Constants.ContractStatus.ACTIVE, Constants.ContractStatus.MATURED,
                "SYSTEM", "Contract reached maturity date", MDC.get("correlationId"));
        contractRepository.save(contract);

        eventPublisher.publishContractMatured(contract);

        log.info("[{}] Contract {} marked as MATURED", MDC.get("correlationId"), contractNo);
        return ContractResponse.from(contract);
    }

    // ────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ────────────────────────────────────────────────────────────────────────

    // ────────────────────────────────────────────────────────────────────────
    // Periodic interest (MONTHLY / QUARTERLY) — called by Lifecycle Service
    // ────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<ContractSummaryResponse> getPeriodicInterestContracts(Pageable pageable) {
        return contractRepository
                .findPeriodicInterestContracts(LocalDate.now(), pageable)
                .map(ContractSummaryResponse::from);
    }

    // ────────────────────────────────────────────────────────────────────────
    // Ownership helper (used by controller for CUSTOMER access check)
    // ────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public String getContractCif(String contractNo) {
        return contractRepository.findById(contractNo)
                .map(SavingContract::getCif)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONTRACT_NOT_FOUND, contractNo));
    }

    // ────────────────────────────────────────────────────────────────────────
    // Status History
    // ────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ContractStatusHistoryResponse> getStatusHistory(String contractNo) {
        return historyRepository.findByContractNoOrderByChangedAtAsc(contractNo)
                .stream()
                .map(ContractStatusHistoryResponse::from)
                .toList();
    }

    // ────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ────────────────────────────────────────────────────────────────────────

    private void saveHistory(SavingContract contract,
                              String fromStatus,
                              String toStatus,
                              String changedBy,
                              String reason,
                              String correlationId) {
        ContractStatusHistory history = ContractStatusHistory.builder()
                .contractNo(contract.getContractNo())
                .fromStatus(fromStatus)
                .toStatus(toStatus)
                .changedBy(changedBy)
                .reason(reason)
                .correlationId(correlationId)
                .build();
        historyRepository.save(history);
    }
}
