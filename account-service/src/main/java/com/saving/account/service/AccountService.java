package com.saving.account.service;

import com.saving.account.dto.request.*;
import com.saving.account.dto.response.*;
import com.saving.account.entity.Account;
import com.saving.account.entity.AccountBalance;
import com.saving.account.entity.AccountHoldLog;
import com.saving.account.exception.BusinessException;
import com.saving.account.exception.ErrorCode;
import com.saving.account.repository.AccountBalanceRepository;
import com.saving.account.repository.AccountHoldLogRepository;
import com.saving.account.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

    private final AccountRepository        accountRepository;
    private final AccountBalanceRepository balanceRepository;
    private final AccountHoldLogRepository holdLogRepository;
    private final AccountEventPublisher    eventPublisher;

    // ── Create Account ─────────────────────────────────────────────

    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {
        log.info("[CREATE_ACCOUNT] Step 1/5 — check CIF limit: cif={}", request.getCif());
        long existing = accountRepository.countByCif(request.getCif());
        if (existing >= 10) {
            log.warn("[CREATE_ACCOUNT] FAILED — CIF limit reached: cif={}, existing={}", request.getCif(), existing);
            throw new BusinessException(ErrorCode.ACCOUNT_LIMIT_REACHED,
                    "CIF " + request.getCif() + " already has " + existing + " accounts");
        }

        log.info("[CREATE_ACCOUNT] Step 2/5 — generate account number: cif={}", request.getCif());
        String accountNo = generateAccountNo(request.getCif());
        log.info("[CREATE_ACCOUNT] Step 3/5 — build & persist account: accountNo={}, type={}, currency={}",
                accountNo,
                request.getAccountType() != null ? request.getAccountType() : "PAYMENT",
                request.getCurrency() != null ? request.getCurrency() : "VND");

        Account account = Account.builder()
                .accountNo(accountNo)
                .cif(request.getCif())
                .accountType(request.getAccountType() != null ? request.getAccountType() : "PAYMENT")
                .currency(request.getCurrency() != null ? request.getCurrency() : "VND")
                .status("ACTIVE")
                .openDate(request.getOpenDate() != null ? request.getOpenDate() : LocalDate.now())
                .branchCode(request.getBranchCode())
                .build();

        account = accountRepository.save(account);

        log.info("[CREATE_ACCOUNT] Step 4/5 — create zero balance record: accountNo={}", accountNo);
        AccountBalance balance = AccountBalance.builder()
                .account(account)
                .availableBalance(BigDecimal.ZERO)
                .ledgerBalance(BigDecimal.ZERO)
                .holdAmount(BigDecimal.ZERO)
                .currency(account.getCurrency())
                .build();

        balance = balanceRepository.save(balance);
        account.setBalance(balance);

        log.info("[CREATE_ACCOUNT] Step 5/5 — publish account.created event: accountNo={}", accountNo);
        eventPublisher.publishAccountCreated(accountNo, request.getCif(), account.getAccountType());

        log.info("[CREATE_ACCOUNT] SUCCESS — accountNo={}, cif={}, type={}",
                accountNo, request.getCif(), account.getAccountType());
        return AccountResponse.from(account);
    }

    // ── Get Account ────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public AccountResponse getAccount(String accountNo) {
        log.info("[GET_ACCOUNT] lookup: accountNo={}", accountNo);
        Account account = accountRepository.findByAccountNoWithBalance(accountNo)
                .orElseThrow(() -> {
                    log.warn("[GET_ACCOUNT] NOT FOUND: accountNo={}", accountNo);
                    return new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND, "accountNo=" + accountNo);
                });
        log.info("[GET_ACCOUNT] OK: accountNo={}, cif={}, status={}", accountNo, account.getCif(), account.getStatus());
        return AccountResponse.from(account);
    }

    // ── List Accounts by CIF ───────────────────────────────────────

    @Transactional(readOnly = true)
    public List<AccountSummaryResponse> getAccountsByCif(String cif) {
        log.info("[GET_ACCOUNTS_BY_CIF] lookup: cif={}", cif);
        List<Account> accounts = accountRepository.findByCifWithBalance(cif);
        log.info("[GET_ACCOUNTS_BY_CIF] OK: cif={}, count={}", cif, accounts.size());
        return accounts.stream()
                .map(AccountSummaryResponse::from)
                .collect(Collectors.toList());
    }

    // ── Get Balance ────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public AccountBalanceResponse getBalance(String accountNo) {
        log.info("[GET_BALANCE] lookup: accountNo={}", accountNo);
        AccountBalance balance = balanceRepository.findByAccount_AccountNo(accountNo)
                .orElseThrow(() -> {
                    log.warn("[GET_BALANCE] NOT FOUND: accountNo={}", accountNo);
                    return new BusinessException(ErrorCode.BALANCE_NOT_FOUND, "accountNo=" + accountNo);
                });
        log.info("[GET_BALANCE] OK: accountNo={}, available={}, ledger={}, hold={}",
                accountNo, balance.getAvailableBalance(), balance.getLedgerBalance(), balance.getHoldAmount());
        return AccountBalanceResponse.from(balance);
    }

    // ── Update Account Status ──────────────────────────────────────

    @Transactional
    public AccountResponse updateStatus(String accountNo, UpdateAccountStatusRequest request) {
        log.info("[UPDATE_STATUS] Step 1/3 — load account: accountNo={}", accountNo);
        Account account = accountRepository.findByAccountNoWithBalance(accountNo)
                .orElseThrow(() -> {
                    log.warn("[UPDATE_STATUS] NOT FOUND: accountNo={}", accountNo);
                    return new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND, "accountNo=" + accountNo);
                });

        if ("CLOSED".equals(account.getStatus())) {
            log.warn("[UPDATE_STATUS] FAILED — account already closed: accountNo={}", accountNo);
            throw new BusinessException(ErrorCode.ACCOUNT_ALREADY_CLOSED, "accountNo=" + accountNo);
        }

        log.info("[UPDATE_STATUS] Step 2/3 — validate transition: accountNo={}, {}→{}",
                accountNo, account.getStatus(), request.getStatus());
        if ("CLOSED".equals(request.getStatus()) && account.getBalance() != null) {
            BigDecimal ledger = account.getBalance().getLedgerBalance();
            if (ledger.compareTo(BigDecimal.ZERO) != 0) {
                log.warn("[UPDATE_STATUS] FAILED — non-zero balance blocks close: accountNo={}, ledger={}",
                        accountNo, ledger);
                throw new BusinessException(ErrorCode.NON_ZERO_BALANCE,
                        "Ledger balance: " + ledger);
            }
        }

        String oldStatus = account.getStatus();
        account.setStatus(request.getStatus());

        log.info("[UPDATE_STATUS] Step 3/3 — persist & publish event: accountNo={}", accountNo);
        account = accountRepository.save(account);
        eventPublisher.publishStatusChanged(accountNo, account.getCif(), oldStatus, request.getStatus());

        log.info("[UPDATE_STATUS] SUCCESS — accountNo={}, {}→{}", accountNo, oldStatus, request.getStatus());
        return AccountResponse.from(account);
    }

    // ── Debit ──────────────────────────────────────────────────────

    @Transactional
    public BalanceOperationResponse debit(String accountNo, DebitRequest request) {
        log.info("[DEBIT] Step 1/4 — verify active account: accountNo={}, amount={}, useHold={}, ref={}",
                accountNo, request.getAmount(), request.isUseHold(), request.getReference());
        Account account = getActiveAccount(accountNo);

        log.info("[DEBIT] Step 2/4 — acquire pessimistic lock on balance: accountNo={}", accountNo);
        AccountBalance balance = balanceRepository.findByAccountNoForUpdate(accountNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.BALANCE_NOT_FOUND, "accountNo=" + accountNo));

        BigDecimal balanceBefore = balance.getAvailableBalance();

        log.info("[DEBIT] Step 3/4 — check funds & debit: accountNo={}, available={}, hold={}, amount={}, useHold={}",
                accountNo, balance.getAvailableBalance(), balance.getHoldAmount(),
                request.getAmount(), request.isUseHold());

        if (request.isUseHold()) {
            if (balance.getHoldAmount().compareTo(request.getAmount()) < 0) {
                log.warn("[DEBIT] FAILED — insufficient hold: accountNo={}, hold={}, requested={}",
                        accountNo, balance.getHoldAmount(), request.getAmount());
                throw new BusinessException(ErrorCode.INSUFFICIENT_HOLD,
                        "Hold: " + balance.getHoldAmount() + ", requested: " + request.getAmount());
            }
            balance.debitFromHold(request.getAmount());
        } else {
            if (!balance.hasSufficientFunds(request.getAmount())) {
                log.warn("[DEBIT] FAILED — insufficient funds: accountNo={}, available={}, requested={}",
                        accountNo, balance.getAvailableBalance(), request.getAmount());
                throw new BusinessException(ErrorCode.INSUFFICIENT_FUNDS,
                        "Available: " + balance.getAvailableBalance() + ", requested: " + request.getAmount());
            }
            balance.debit(request.getAmount());
        }

        log.info("[DEBIT] Step 4/4 — persist & publish debit event: accountNo={}, ref={}", accountNo, request.getReference());
        balance = balanceRepository.save(balance);
        AccountBalanceResponse balResp = AccountBalanceResponse.from(balance);

        eventPublisher.publishDebit(accountNo, account.getCif(), request.getAmount(),
                balance.getAvailableBalance(), request.getReference());

        log.info("[DEBIT] SUCCESS — accountNo={}, amount={}, ref={}, balanceBefore={}, balanceAfter={}",
                accountNo, request.getAmount(), request.getReference(),
                balanceBefore, balance.getAvailableBalance());

        return BalanceOperationResponse.of(accountNo, "DEBIT", request.getAmount(),
                request.getReference(), balResp);
    }

    // ── Credit ─────────────────────────────────────────────────────

    @Transactional
    public BalanceOperationResponse credit(String accountNo, CreditRequest request) {
        log.info("[CREDIT] Step 1/3 — verify active account: accountNo={}, amount={}, ref={}",
                accountNo, request.getAmount(), request.getReference());
        Account account = getActiveAccount(accountNo);

        log.info("[CREDIT] Step 2/3 — acquire pessimistic lock & credit: accountNo={}", accountNo);
        AccountBalance balance = balanceRepository.findByAccountNoForUpdate(accountNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.BALANCE_NOT_FOUND, "accountNo=" + accountNo));

        BigDecimal balanceBefore = balance.getAvailableBalance();
        balance.credit(request.getAmount());

        log.info("[CREDIT] Step 3/3 — persist & publish credit event: accountNo={}, ref={}", accountNo, request.getReference());
        balance = balanceRepository.save(balance);
        AccountBalanceResponse balResp = AccountBalanceResponse.from(balance);

        eventPublisher.publishCredit(accountNo, account.getCif(), request.getAmount(),
                balance.getAvailableBalance(), request.getReference());

        log.info("[CREDIT] SUCCESS — accountNo={}, amount={}, ref={}, balanceBefore={}, balanceAfter={}",
                accountNo, request.getAmount(), request.getReference(),
                balanceBefore, balance.getAvailableBalance());

        return BalanceOperationResponse.of(accountNo, "CREDIT", request.getAmount(),
                request.getReference(), balResp);
    }

    // ── Place Hold ─────────────────────────────────────────────────

    @Transactional
    public BalanceOperationResponse placeHold(String accountNo, HoldRequest request) {
        log.info("[PLACE_HOLD] Step 1/4 — verify active account: accountNo={}, amount={}, holdRef={}",
                accountNo, request.getAmount(), request.getHoldRef());
        getActiveAccount(accountNo);

        log.info("[PLACE_HOLD] Step 2/4 — idempotency check: holdRef={}", request.getHoldRef());
        if (holdLogRepository.existsByHoldRefAndStatus(request.getHoldRef(), "ACTIVE")) {
            log.warn("[PLACE_HOLD] FAILED — duplicate hold ref: accountNo={}, holdRef={}",
                    accountNo, request.getHoldRef());
            throw new BusinessException(ErrorCode.HOLD_ALREADY_EXISTS,
                    "holdRef=" + request.getHoldRef());
        }

        log.info("[PLACE_HOLD] Step 3/4 — acquire lock & validate funds: accountNo={}", accountNo);
        AccountBalance balance = balanceRepository.findByAccountNoForUpdate(accountNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.BALANCE_NOT_FOUND, "accountNo=" + accountNo));

        BigDecimal balanceBefore = balance.getAvailableBalance();

        if (!balance.hasSufficientFunds(request.getAmount())) {
            log.warn("[PLACE_HOLD] FAILED — insufficient funds: accountNo={}, available={}, requested={}",
                    accountNo, balance.getAvailableBalance(), request.getAmount());
            throw new BusinessException(ErrorCode.INSUFFICIENT_FUNDS,
                    "Available: " + balance.getAvailableBalance() + ", requested: " + request.getAmount());
        }

        balance.placeHold(request.getAmount());
        balance = balanceRepository.save(balance);

        log.info("[PLACE_HOLD] Step 4/4 — save hold log: accountNo={}, holdRef={}", accountNo, request.getHoldRef());
        AccountHoldLog holdLog = AccountHoldLog.builder()
                .accountNo(accountNo)
                .holdAmount(request.getAmount())
                .holdReason(request.getHoldReason())
                .holdRef(request.getHoldRef())
                .status("ACTIVE")
                .build();
        holdLogRepository.save(holdLog);

        log.info("[PLACE_HOLD] SUCCESS — accountNo={}, amount={}, holdRef={}, availableBefore={}, availableAfter={}",
                accountNo, request.getAmount(), request.getHoldRef(),
                balanceBefore, balance.getAvailableBalance());

        return BalanceOperationResponse.of(accountNo, "HOLD", request.getAmount(),
                request.getHoldRef(), AccountBalanceResponse.from(balance));
    }

    // ── Release Hold ───────────────────────────────────────────────

    @Transactional
    public BalanceOperationResponse releaseHold(String accountNo, ReleaseHoldRequest request) {
        log.info("[RELEASE_HOLD] Step 1/4 — verify active account: accountNo={}, holdRef={}",
                accountNo, request.getHoldRef());
        getActiveAccount(accountNo);

        log.info("[RELEASE_HOLD] Step 2/4 — find active hold log: holdRef={}", request.getHoldRef());
        AccountHoldLog holdLog = holdLogRepository.findByHoldRefAndStatus(request.getHoldRef(), "ACTIVE")
                .orElseThrow(() -> {
                    log.warn("[RELEASE_HOLD] FAILED — hold not found: accountNo={}, holdRef={}",
                            accountNo, request.getHoldRef());
                    return new BusinessException(ErrorCode.HOLD_NOT_FOUND,
                            "holdRef=" + request.getHoldRef() + " (not active)");
                });

        log.info("[RELEASE_HOLD] Step 3/4 — acquire lock & release hold: accountNo={}, holdAmount={}",
                accountNo, holdLog.getHoldAmount());
        AccountBalance balance = balanceRepository.findByAccountNoForUpdate(accountNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.BALANCE_NOT_FOUND, "accountNo=" + accountNo));

        BigDecimal balanceBefore = balance.getAvailableBalance();
        balance.releaseHold(holdLog.getHoldAmount());
        balance = balanceRepository.save(balance);

        log.info("[RELEASE_HOLD] Step 4/4 — mark hold log RELEASED: holdRef={}", request.getHoldRef());
        holdLogRepository.releaseByHoldRef(request.getHoldRef());

        log.info("[RELEASE_HOLD] SUCCESS — accountNo={}, releasedAmount={}, holdRef={}, availableBefore={}, availableAfter={}",
                accountNo, holdLog.getHoldAmount(), request.getHoldRef(),
                balanceBefore, balance.getAvailableBalance());

        return BalanceOperationResponse.of(accountNo, "RELEASE_HOLD", holdLog.getHoldAmount(),
                request.getHoldRef(), AccountBalanceResponse.from(balance));
    }

    // ── Internal: Validate Account ─────────────────────────────────

    @Transactional(readOnly = true)
    public AccountValidationResponse validateAccount(String accountNo, BigDecimal requiredAmount) {
        log.info("[VALIDATE_ACCOUNT] check: accountNo={}, requiredAmount={}", accountNo, requiredAmount);
        Account account = accountRepository.findByAccountNoWithBalance(accountNo).orElse(null);

        if (account == null) {
            log.warn("[VALIDATE_ACCOUNT] INVALID — not found: accountNo={}", accountNo);
            return AccountValidationResponse.invalid(accountNo, "Account not found");
        }
        if (!"ACTIVE".equals(account.getStatus())) {
            log.warn("[VALIDATE_ACCOUNT] INVALID — status not active: accountNo={}, status={}",
                    accountNo, account.getStatus());
            return AccountValidationResponse.invalid(accountNo, "Account status is " + account.getStatus());
        }

        BigDecimal available = account.getBalance() != null
                ? account.getBalance().getAvailableBalance() : BigDecimal.ZERO;

        if (requiredAmount != null && available.compareTo(requiredAmount) < 0) {
            log.warn("[VALIDATE_ACCOUNT] INVALID — insufficient funds: accountNo={}, available={}, required={}",
                    accountNo, available, requiredAmount);
            return AccountValidationResponse.invalid(accountNo,
                    "Insufficient funds: available=" + available + ", required=" + requiredAmount);
        }

        log.info("[VALIDATE_ACCOUNT] VALID — accountNo={}, cif={}, available={}", accountNo, account.getCif(), available);
        return AccountValidationResponse.valid(accountNo, account.getCif(),
                account.getAccountType(), account.getCurrency(), available);
    }

    // ── Private helpers ────────────────────────────────────────────

    private Account getActiveAccount(String accountNo) {
        Account account = accountRepository.findById(accountNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND, "accountNo=" + accountNo));

        if ("BLOCKED".equals(account.getStatus())) {
            throw new BusinessException(ErrorCode.ACCOUNT_BLOCKED, "accountNo=" + accountNo);
        }
        if ("CLOSED".equals(account.getStatus())) {
            throw new BusinessException(ErrorCode.ACCOUNT_CLOSED, "accountNo=" + accountNo);
        }
        return account;
    }

    /**
     * Generate account number: ACC + 3-digit CIF-number + 3-digit sequence.
     * E.g. CIF0001 → ACC001001, ACC001002 ...
     */
    private synchronized String generateAccountNo(String cif) {
        // Extract numeric part of CIF (e.g. "0001" from "CIF0001")
        String cifNumPart = cif.replaceAll("[^0-9]", "");
        if (cifNumPart.length() > 3) {
            cifNumPart = cifNumPart.substring(cifNumPart.length() - 3); // Take last 3 digits
        }
        String prefix = "ACC" + String.format("%3s", cifNumPart).replace(' ', '0');

        Integer maxSeq = accountRepository.findMaxAccountSequence(prefix);
        int next = (maxSeq == null ? 0 : maxSeq) + 1;
        if (next > 999) {
            throw new BusinessException(ErrorCode.ACCOUNT_NO_GEN_FAILED,
                    "Account sequence for CIF " + cif + " exceeded 999");
        }
        return prefix + String.format("%03d", next);
    }
}
