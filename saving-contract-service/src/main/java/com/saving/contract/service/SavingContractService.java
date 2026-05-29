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

    private final SavingContractRepository        contractRepository;
    private final ContractStatusHistoryRepository historyRepository;
    private final MaturityInstructionRepository   maturityInstructionRepository;

    private final CustomerServiceClient customerClient;
    private final AccountServiceClient  accountClient;
    private final ProductServiceClient  productClient;

    private final ContractNumberGenerator contractNumberGenerator;
    private final InterestCalculator      interestCalculator;
    private final ContractEventPublisher  eventPublisher;

    // ────────────────────────────────────────────────────────────────────────
    // Open Contract
    // ────────────────────────────────────────────────────────────────────────

    @Transactional
    public ContractResponse openContract(OpenContractRequest request,
                                         String bearerToken,
                                         String openedBy) {

        String correlationId = MDC.get("correlationId");
        log.info("[OPEN_CONTRACT] Step 1/9 — validate customer: cif={} product={} term={} amount={} by={}",
                request.getCif(), request.getProductCode(), request.getTermId(),
                request.getPrincipalAmount(), openedBy);

        // ── 1. Validate customer ─────────────────────────────────────────────
        CustomerValidationResult customer = customerClient.validateCustomer(request.getCif());
        if (!customer.isValid()) {
            log.warn("[OPEN_CONTRACT] FAILED — customer validation: cif={}, reason={}",
                    request.getCif(), customer.getInvalidReason());
            throw new BusinessException(ErrorCode.CUSTOMER_NOT_VALID, customer.getInvalidReason());
        }
        log.info("[OPEN_CONTRACT] Step 2/9 — validate source account: accountNo={} amount={}",
                request.getSourceAccountNo(), request.getPrincipalAmount());

        // ── 2. Validate source account (sufficient funds) ────────────────────
        AccountValidationResult account =
                accountClient.validateAccount(request.getSourceAccountNo(), request.getPrincipalAmount());
        if (!account.isValid()) {
            log.warn("[OPEN_CONTRACT] FAILED — account validation: accountNo={}, reason={}",
                    request.getSourceAccountNo(), account.getInvalidReason());
            String reason = account.getInvalidReason();
            if (reason != null && reason.toLowerCase().contains("insufficient")) {
                throw new BusinessException(ErrorCode.INSUFFICIENT_FUNDS, reason);
            }
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_VALID, reason);
        }

        // ── 3. Lock in interest rate ─────────────────────────────────────────
        LocalDate openDate = request.getOpenDate() != null ? request.getOpenDate() : LocalDate.now();
        log.info("[OPEN_CONTRACT] Step 3/9 — query interest rate: product={} term={} date={}",
                request.getProductCode(), request.getTermId(), openDate);
        ProductRateResult rate =
                productClient.queryRate(request.getProductCode(), request.getTermId(), openDate);
        log.info("[OPEN_CONTRACT]   rate={}% currency={} interestMethod={}",
                rate.getAnnualRate(), rate.getCurrency(), rate.getInterestPaymentMethod());

        // ── 4. Validate amount against product limits ────────────────────────
        log.info("[OPEN_CONTRACT] Step 4/9 — validate principal: amount={} min={} max={}",
                request.getPrincipalAmount(), rate.getMinAmount(), rate.getMaxAmount());
        if (rate.getMinAmount() != null
                && request.getPrincipalAmount().compareTo(rate.getMinAmount()) < 0) {
            log.warn("[OPEN_CONTRACT] FAILED — amount below minimum: amount={} min={}",
                    request.getPrincipalAmount(), rate.getMinAmount());
            throw new BusinessException(ErrorCode.AMOUNT_BELOW_MINIMUM,
                    "Minimum amount is " + rate.getMinAmount() + " " + rate.getCurrency());
        }
        if (rate.getMaxAmount() != null
                && request.getPrincipalAmount().compareTo(rate.getMaxAmount()) > 0) {
            log.warn("[OPEN_CONTRACT] FAILED — amount above maximum: amount={} max={}",
                    request.getPrincipalAmount(), rate.getMaxAmount());
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
        log.info("[OPEN_CONTRACT] Step 5/9 — maturity date: openDate={} termDays={} maturity={}",
                openDate, termDays, maturityDate);

        // ── 6. Persist contract in PENDING state ─────────────────────────────
        String contractNo = contractNumberGenerator.generate();
        log.info("[OPEN_CONTRACT] Step 6/9 — persist PENDING contract: contractNo={} cif={} maturity={}",
                contractNo, request.getCif(), maturityDate);

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

        contract = contractRepository.save(contract);

        saveHistory(contract, null, Constants.ContractStatus.PENDING, openedBy,
                "Contract created", correlationId);

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
            log.info("[OPEN_CONTRACT]   maturity instruction saved: type={}", mid.getInstructionType());
        }

        // ── 7. Debit source account ──────────────────────────────────────────
        String debitRef = "CONTRACT-OPEN-" + contractNo;
        log.info("[OPEN_CONTRACT] Step 7/9 — debit source account: accountNo={} amount={} ref={}",
                request.getSourceAccountNo(), request.getPrincipalAmount(), debitRef);
        try {
            accountClient.debitAccount(
                    request.getSourceAccountNo(),
                    request.getPrincipalAmount(),
                    debitRef,
                    "Contract opening deposit for " + contractNo,
                    bearerToken);
        } catch (BusinessException ex) {
            log.error("[OPEN_CONTRACT] FAILED — debit failed: contractNo={} reason={}", contractNo, ex.getMessage());
            contract.setStatus(Constants.ContractStatus.FAILED);
            saveHistory(contract, Constants.ContractStatus.PENDING,
                    Constants.ContractStatus.FAILED, openedBy,
                    "Debit failed: " + ex.getMessage(), correlationId);
            contractRepository.save(contract);
            throw ex;
        }

        // ── 8. Activate contract ─────────────────────────────────────────────
        log.info("[OPEN_CONTRACT] Step 8/9 — activate contract PENDING→ACTIVE: contractNo={}", contractNo);
        contract.setStatus(Constants.ContractStatus.ACTIVE);
        saveHistory(contract, Constants.ContractStatus.PENDING,
                Constants.ContractStatus.ACTIVE, openedBy,
                "Debit successful, contract activated", correlationId);
        contractRepository.save(contract);

        // ── 9. Publish event ─────────────────────────────────────────────────
        log.info("[OPEN_CONTRACT] Step 9/9 — publish CONTRACT_OPENED event: contractNo={}", contractNo);
        eventPublisher.publishContractOpened(contract);

        log.info("[OPEN_CONTRACT] SUCCESS — contractNo={} cif={} principal={} {} rate={}% maturity={}",
                contractNo, request.getCif(), request.getPrincipalAmount(),
                rate.getCurrency(), rate.getAnnualRate(), maturityDate);

        return ContractResponse.from(contract);
    }

    // ────────────────────────────────────────────────────────────────────────
    // Close Contract
    // ────────────────────────────────────────────────────────────────────────

    @Transactional
    public CloseContractResponse closeContract(String contractNo,
                                               CloseContractRequest request,
                                               String bearerToken,
                                               String closedBy) {

        String correlationId = MDC.get("correlationId");
        log.info("[CLOSE_CONTRACT] Step 1/6 — load & validate contract state: contractNo={} by={}",
                contractNo, closedBy);

        // ── 1. Load contract ──────────────────────────────────────────────────
        SavingContract contract = contractRepository.findByContractNoWithInstruction(contractNo)
                .orElseThrow(() -> {
                    log.warn("[CLOSE_CONTRACT] NOT FOUND: contractNo={}", contractNo);
                    return new BusinessException(ErrorCode.CONTRACT_NOT_FOUND, contractNo);
                });

        String currentStatus = contract.getStatus();

        if (Constants.ContractStatus.CLOSED.equals(currentStatus)
                || Constants.ContractStatus.EARLY_CLOSED.equals(currentStatus)
                || Constants.ContractStatus.CANCELLED.equals(currentStatus)
                || Constants.ContractStatus.FAILED.equals(currentStatus)) {
            log.warn("[CLOSE_CONTRACT] FAILED — already terminal: contractNo={} status={}",
                    contractNo, currentStatus);
            throw new BusinessException(ErrorCode.CONTRACT_ALREADY_CLOSED,
                    "Contract " + contractNo + " is already in terminal state: " + currentStatus);
        }

        if (!Constants.ContractStatus.ACTIVE.equals(currentStatus)
                && !Constants.ContractStatus.MATURED.equals(currentStatus)) {
            log.warn("[CLOSE_CONTRACT] FAILED — not closeable: contractNo={} status={}",
                    contractNo, currentStatus);
            throw new BusinessException(ErrorCode.CONTRACT_NOT_ACTIVE,
                    "Contract " + contractNo + " has status: " + currentStatus);
        }

        // ── 2. Determine close type ───────────────────────────────────────────
        boolean earlyWithdrawal = contract.isEarlyWithdrawal();
        String  newStatus       = earlyWithdrawal
                ? Constants.ContractStatus.EARLY_CLOSED
                : Constants.ContractStatus.CLOSED;
        String  closeType       = earlyWithdrawal
                ? Constants.CloseType.EARLY_WITHDRAWAL
                : Constants.CloseType.MATURITY;
        long    daysHeld        = contract.getDaysHeld();

        log.info("[CLOSE_CONTRACT] Step 2/6 — determine close type: contractNo={} earlyWithdrawal={} daysHeld={} {}→{}",
                contractNo, earlyWithdrawal, daysHeld, currentStatus, newStatus);

        // ── 3. Calculate interest ─────────────────────────────────────────────
        BigDecimal principal = contract.getPrincipalAmount();
        BigDecimal interest;

        log.info("[CLOSE_CONTRACT] Step 3/6 — calculate interest: contractNo={} principal={} rate={}% closeType={}",
                contractNo, principal, contract.getInterestRate(), closeType);

        if (earlyWithdrawal) {
            ProductRateResult rateInfo = productClient.queryRate(
                    contract.getProductCode(),
                    contract.getTermId(),
                    contract.getOpenDate());

            BigDecimal demandRate = (rateInfo.getEarlyWithdrawalUseDemandRate() != null
                    && rateInfo.getEarlyWithdrawalUseDemandRate()
                    && rateInfo.getEarlyWithdrawalDemandRate() != null)
                    ? rateInfo.getEarlyWithdrawalDemandRate()
                    : BigDecimal.ZERO;

            log.info("[CLOSE_CONTRACT]   early withdrawal demand rate={}%", demandRate);
            interest = interestCalculator.calculateEarlyWithdrawalInterest(
                    principal, demandRate, daysHeld);
        } else {
            long termDays = contract.getOpenDate().until(contract.getMaturityDate(), ChronoUnit.DAYS);
            interest = interestCalculator.calculateFullTermInterest(
                    principal, contract.getInterestRate(), (int) termDays);
        }

        BigDecimal totalPayout    = interestCalculator.totalPayout(principal, interest);
        String     creditAccountNo = request.getReceivingAccountNo() != null
                ? request.getReceivingAccountNo()
                : contract.getSourceAccountNo();

        log.info("[CLOSE_CONTRACT]   interest={} totalPayout={} creditTo={}",
                interest, totalPayout, creditAccountNo);

        // ── 4. Credit payout ──────────────────────────────────────────────────
        String creditRef = "CONTRACT-CLOSE-" + contractNo;
        log.info("[CLOSE_CONTRACT] Step 4/6 — credit payout: accountNo={} amount={} ref={}",
                creditAccountNo, totalPayout, creditRef);
        accountClient.creditAccount(
                creditAccountNo,
                totalPayout,
                creditRef,
                "Contract payout: " + contractNo + " (" + closeType + ")",
                bearerToken);

        // ── 5. Update contract ────────────────────────────────────────────────
        log.info("[CLOSE_CONTRACT] Step 5/6 — update contract: contractNo={} {}→{}",
                contractNo, currentStatus, newStatus);
        OffsetDateTime now = OffsetDateTime.now();
        contract.setStatus(newStatus);
        contract.setClosedAt(now);
        contract.setCloseType(closeType);
        saveHistory(contract, currentStatus, newStatus, closedBy,
                request.getReason() != null ? request.getReason() : closeType, correlationId);
        contractRepository.save(contract);

        // ── 6. Publish event ──────────────────────────────────────────────────
        log.info("[CLOSE_CONTRACT] Step 6/6 — publish CONTRACT_CLOSED event: contractNo={}", contractNo);
        eventPublisher.publishContractClosed(contract, interest, totalPayout);

        log.info("[CLOSE_CONTRACT] SUCCESS — contractNo={} closeType={} principal={} interest={} payout={} {}",
                contractNo, closeType, principal, interest, totalPayout, contract.getCurrency());

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
        log.info("[GET_CONTRACT] lookup: contractNo={}", contractNo);
        SavingContract contract = contractRepository.findByContractNoWithInstruction(contractNo)
                .orElseThrow(() -> {
                    log.warn("[GET_CONTRACT] NOT FOUND: contractNo={}", contractNo);
                    return new BusinessException(ErrorCode.CONTRACT_NOT_FOUND, contractNo);
                });
        log.info("[GET_CONTRACT] OK: contractNo={} cif={} status={} maturity={}",
                contractNo, contract.getCif(), contract.getStatus(), contract.getMaturityDate());
        return ContractResponse.from(contract);
    }

    @Transactional(readOnly = true)
    public Page<ContractSummaryResponse> listContracts(String cif, String status, Pageable pageable) {
        log.info("[LIST_CONTRACTS] query: cif={} status={} page={}/size={}",
                cif, status, pageable.getPageNumber(), pageable.getPageSize());
        Page<ContractSummaryResponse> result = contractRepository
                .findByCifAndStatus(cif, status, pageable)
                .map(ContractSummaryResponse::from);
        log.info("[LIST_CONTRACTS] OK: total={}", result.getTotalElements());
        return result;
    }

    @Transactional(readOnly = true)
    public Page<ContractSummaryResponse> getContractsByCif(String cif, Pageable pageable) {
        log.info("[GET_CONTRACTS_BY_CIF] cif={} page={}/size={}",
                cif, pageable.getPageNumber(), pageable.getPageSize());
        Page<ContractSummaryResponse> result = contractRepository
                .findByCif(cif, pageable)
                .map(ContractSummaryResponse::from);
        log.info("[GET_CONTRACTS_BY_CIF] OK: cif={} total={}", cif, result.getTotalElements());
        return result;
    }

    @Transactional(readOnly = true)
    public Page<ContractSummaryResponse> getContractsByStatus(String status, Pageable pageable) {
        log.info("[GET_CONTRACTS_BY_STATUS] status={} page={}/size={}",
                status, pageable.getPageNumber(), pageable.getPageSize());
        Page<ContractSummaryResponse> result = contractRepository
                .findByStatus(status, pageable)
                .map(ContractSummaryResponse::from);
        log.info("[GET_CONTRACTS_BY_STATUS] OK: status={} total={}", status, result.getTotalElements());
        return result;
    }

    // ────────────────────────────────────────────────────────────────────────
    // Maturity Instruction
    // ────────────────────────────────────────────────────────────────────────

    @Transactional
    public ContractResponse updateMaturityInstruction(String contractNo,
                                                       UpdateMaturityInstructionRequest request,
                                                       String updatedBy) {

        log.info("[UPDATE_MATURITY_INSTRUCTION] Step 1/2 — load & validate ACTIVE contract: contractNo={}",
                contractNo);
        SavingContract contract = contractRepository.findByContractNoWithInstruction(contractNo)
                .orElseThrow(() -> {
                    log.warn("[UPDATE_MATURITY_INSTRUCTION] NOT FOUND: contractNo={}", contractNo);
                    return new BusinessException(ErrorCode.CONTRACT_NOT_FOUND, contractNo);
                });

        if (!Constants.ContractStatus.ACTIVE.equals(contract.getStatus())) {
            log.warn("[UPDATE_MATURITY_INSTRUCTION] FAILED — not active: contractNo={} status={}",
                    contractNo, contract.getStatus());
            throw new BusinessException(ErrorCode.CONTRACT_NOT_ACTIVE, contractNo);
        }

        log.info("[UPDATE_MATURITY_INSTRUCTION] Step 2/2 — upsert instruction: contractNo={} type={} receivingAccount={}",
                contractNo, request.getInstructionType(), request.getReceivingAccountNo());
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

        log.info("[UPDATE_MATURITY_INSTRUCTION] SUCCESS — contractNo={} by={}", contractNo, updatedBy);
        return ContractResponse.from(contract);
    }

    // ────────────────────────────────────────────────────────────────────────
    // Internal (called by Lifecycle Service)
    // ────────────────────────────────────────────────────────────────────────

    @Transactional
    public ContractResponse markMatured(String contractNo) {
        String correlationId = MDC.get("correlationId");
        log.info("[MARK_MATURED] Step 1/2 — load & validate ACTIVE contract: contractNo={}", contractNo);
        SavingContract contract = contractRepository.findByContractNoWithInstruction(contractNo)
                .orElseThrow(() -> {
                    log.warn("[MARK_MATURED] NOT FOUND: contractNo={}", contractNo);
                    return new BusinessException(ErrorCode.CONTRACT_NOT_FOUND, contractNo);
                });

        if (!Constants.ContractStatus.ACTIVE.equals(contract.getStatus())) {
            log.warn("[MARK_MATURED] FAILED — not ACTIVE: contractNo={} status={}",
                    contractNo, contract.getStatus());
            throw new BusinessException(ErrorCode.CONTRACT_NOT_ACTIVE,
                    "Contract " + contractNo + " is not ACTIVE, current status: " + contract.getStatus());
        }

        log.info("[MARK_MATURED] Step 2/2 — persist MATURED + publish event: contractNo={}", contractNo);
        contract.setStatus(Constants.ContractStatus.MATURED);
        saveHistory(contract, Constants.ContractStatus.ACTIVE, Constants.ContractStatus.MATURED,
                "SYSTEM", "Contract reached maturity date", correlationId);
        contractRepository.save(contract);
        eventPublisher.publishContractMatured(contract);

        log.info("[MARK_MATURED] SUCCESS — contractNo={} maturityDate={}", contractNo, contract.getMaturityDate());
        return ContractResponse.from(contract);
    }

    // ────────────────────────────────────────────────────────────────────────
    // Periodic interest (MONTHLY / QUARTERLY) — called by Lifecycle Service
    // ────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<ContractSummaryResponse> getPeriodicInterestContracts(Pageable pageable) {
        log.info("[GET_PERIODIC_INTEREST] query: page={}/size={}",
                pageable.getPageNumber(), pageable.getPageSize());
        Page<ContractSummaryResponse> result = contractRepository
                .findPeriodicInterestContracts(LocalDate.now(), pageable)
                .map(ContractSummaryResponse::from);
        log.info("[GET_PERIODIC_INTEREST] OK: total={}", result.getTotalElements());
        return result;
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
        log.info("[GET_STATUS_HISTORY] contractNo={}", contractNo);
        List<ContractStatusHistoryResponse> history = historyRepository
                .findByContractNoOrderByChangedAtAsc(contractNo)
                .stream()
                .map(ContractStatusHistoryResponse::from)
                .toList();
        log.info("[GET_STATUS_HISTORY] OK: contractNo={} entries={}", contractNo, history.size());
        return history;
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
