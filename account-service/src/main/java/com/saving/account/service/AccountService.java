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
        // Max 10 accounts per CIF
        long existing = accountRepository.countByCif(request.getCif());
        if (existing >= 10) {
            throw new BusinessException(ErrorCode.ACCOUNT_LIMIT_REACHED,
                    "CIF " + request.getCif() + " already has " + existing + " accounts");
        }

        String accountNo = generateAccountNo(request.getCif());

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

        // Create balance record (zero initial balance)
        AccountBalance balance = AccountBalance.builder()
                .account(account)
                .availableBalance(BigDecimal.ZERO)
                .ledgerBalance(BigDecimal.ZERO)
                .holdAmount(BigDecimal.ZERO)
                .currency(account.getCurrency())
                .build();

        balance = balanceRepository.save(balance);
        account.setBalance(balance);

        log.info("Account created: accountNo={}, cif={}, type={}", accountNo, request.getCif(), account.getAccountType());
        eventPublisher.publishAccountCreated(accountNo, request.getCif(), account.getAccountType());

        return AccountResponse.from(account);
    }

    // ── Get Account ────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public AccountResponse getAccount(String accountNo) {
        Account account = accountRepository.findByAccountNoWithBalance(accountNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND, "accountNo=" + accountNo));
        return AccountResponse.from(account);
    }

    // ── List Accounts by CIF ───────────────────────────────────────

    @Transactional(readOnly = true)
    public List<AccountSummaryResponse> getAccountsByCif(String cif) {
        return accountRepository.findByCifWithBalance(cif).stream()
                .map(AccountSummaryResponse::from)
                .collect(Collectors.toList());
    }

    // ── Get Balance ────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public AccountBalanceResponse getBalance(String accountNo) {
        AccountBalance balance = balanceRepository.findByAccount_AccountNo(accountNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.BALANCE_NOT_FOUND, "accountNo=" + accountNo));
        return AccountBalanceResponse.from(balance);
    }

    // ── Update Account Status ──────────────────────────────────────

    @Transactional
    public AccountResponse updateStatus(String accountNo, UpdateAccountStatusRequest request) {
        Account account = accountRepository.findByAccountNoWithBalance(accountNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND, "accountNo=" + accountNo));

        if ("CLOSED".equals(account.getStatus())) {
            throw new BusinessException(ErrorCode.ACCOUNT_ALREADY_CLOSED, "accountNo=" + accountNo);
        }

        // Cannot close account with non-zero balance
        if ("CLOSED".equals(request.getStatus()) && account.getBalance() != null) {
            BigDecimal ledger = account.getBalance().getLedgerBalance();
            if (ledger.compareTo(BigDecimal.ZERO) != 0) {
                throw new BusinessException(ErrorCode.NON_ZERO_BALANCE,
                        "Ledger balance: " + ledger);
            }
        }

        String oldStatus = account.getStatus();
        account.setStatus(request.getStatus());
        account = accountRepository.save(account);

        log.info("Account status changed: accountNo={}, {}→{}", accountNo, oldStatus, request.getStatus());
        eventPublisher.publishStatusChanged(accountNo, account.getCif(), oldStatus, request.getStatus());

        return AccountResponse.from(account);
    }

    // ── Debit ──────────────────────────────────────────────────────

    @Transactional
    public BalanceOperationResponse debit(String accountNo, DebitRequest request) {
        Account account = getActiveAccount(accountNo);

        // Pessimistic lock on balance
        AccountBalance balance = balanceRepository.findByAccountNoForUpdate(accountNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.BALANCE_NOT_FOUND, "accountNo=" + accountNo));

        if (request.isUseHold()) {
            // Debit from pre-placed hold
            if (balance.getHoldAmount().compareTo(request.getAmount()) < 0) {
                throw new BusinessException(ErrorCode.INSUFFICIENT_HOLD,
                        "Hold: " + balance.getHoldAmount() + ", requested: " + request.getAmount());
            }
            balance.debitFromHold(request.getAmount());
        } else {
            // Normal debit from available balance
            if (!balance.hasSufficientFunds(request.getAmount())) {
                throw new BusinessException(ErrorCode.INSUFFICIENT_FUNDS,
                        "Available: " + balance.getAvailableBalance() + ", requested: " + request.getAmount());
            }
            balance.debit(request.getAmount());
        }

        balance = balanceRepository.save(balance);
        AccountBalanceResponse balResp = AccountBalanceResponse.from(balance);

        log.info("Account debited: accountNo={}, amount={}, useHold={}, ref={}",
                accountNo, request.getAmount(), request.isUseHold(), request.getReference());

        eventPublisher.publishDebit(accountNo, account.getCif(), request.getAmount(),
                balance.getAvailableBalance(), request.getReference());

        return BalanceOperationResponse.of(accountNo, "DEBIT", request.getAmount(),
                request.getReference(), balResp);
    }

    // ── Credit ─────────────────────────────────────────────────────

    @Transactional
    public BalanceOperationResponse credit(String accountNo, CreditRequest request) {
        Account account = getActiveAccount(accountNo);

        AccountBalance balance = balanceRepository.findByAccountNoForUpdate(accountNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.BALANCE_NOT_FOUND, "accountNo=" + accountNo));

        balance.credit(request.getAmount());
        balance = balanceRepository.save(balance);
        AccountBalanceResponse balResp = AccountBalanceResponse.from(balance);

        log.info("Account credited: accountNo={}, amount={}, ref={}",
                accountNo, request.getAmount(), request.getReference());

        eventPublisher.publishCredit(accountNo, account.getCif(), request.getAmount(),
                balance.getAvailableBalance(), request.getReference());

        return BalanceOperationResponse.of(accountNo, "CREDIT", request.getAmount(),
                request.getReference(), balResp);
    }

    // ── Place Hold ─────────────────────────────────────────────────

    @Transactional
    public BalanceOperationResponse placeHold(String accountNo, HoldRequest request) {
        getActiveAccount(accountNo);    // Check account status

        // Idempotency: reject if active hold already exists for this ref
        if (holdLogRepository.existsByHoldRefAndStatus(request.getHoldRef(), "ACTIVE")) {
            throw new BusinessException(ErrorCode.HOLD_ALREADY_EXISTS,
                    "holdRef=" + request.getHoldRef());
        }

        AccountBalance balance = balanceRepository.findByAccountNoForUpdate(accountNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.BALANCE_NOT_FOUND, "accountNo=" + accountNo));

        if (!balance.hasSufficientFunds(request.getAmount())) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_FUNDS,
                    "Available: " + balance.getAvailableBalance() + ", requested: " + request.getAmount());
        }

        balance.placeHold(request.getAmount());
        balance = balanceRepository.save(balance);

        // Record hold log
        AccountHoldLog holdLog = AccountHoldLog.builder()
                .accountNo(accountNo)
                .holdAmount(request.getAmount())
                .holdReason(request.getHoldReason())
                .holdRef(request.getHoldRef())
                .status("ACTIVE")
                .build();
        holdLogRepository.save(holdLog);

        log.info("Hold placed: accountNo={}, amount={}, ref={}", accountNo, request.getAmount(), request.getHoldRef());

        return BalanceOperationResponse.of(accountNo, "HOLD", request.getAmount(),
                request.getHoldRef(), AccountBalanceResponse.from(balance));
    }

    // ── Release Hold ───────────────────────────────────────────────

    @Transactional
    public BalanceOperationResponse releaseHold(String accountNo, ReleaseHoldRequest request) {
        getActiveAccount(accountNo);

        AccountHoldLog holdLog = holdLogRepository.findByHoldRefAndStatus(request.getHoldRef(), "ACTIVE")
                .orElseThrow(() -> new BusinessException(ErrorCode.HOLD_NOT_FOUND,
                        "holdRef=" + request.getHoldRef() + " (not active)"));

        AccountBalance balance = balanceRepository.findByAccountNoForUpdate(accountNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.BALANCE_NOT_FOUND, "accountNo=" + accountNo));

        balance.releaseHold(holdLog.getHoldAmount());
        balance = balanceRepository.save(balance);

        holdLogRepository.releaseByHoldRef(request.getHoldRef());

        log.info("Hold released: accountNo={}, amount={}, ref={}", accountNo, holdLog.getHoldAmount(), request.getHoldRef());

        return BalanceOperationResponse.of(accountNo, "RELEASE_HOLD", holdLog.getHoldAmount(),
                request.getHoldRef(), AccountBalanceResponse.from(balance));
    }

    // ── Internal: Validate Account ─────────────────────────────────

    @Transactional(readOnly = true)
    public AccountValidationResponse validateAccount(String accountNo, BigDecimal requiredAmount) {
        Account account = accountRepository.findByAccountNoWithBalance(accountNo).orElse(null);

        if (account == null) {
            return AccountValidationResponse.invalid(accountNo, "Account not found");
        }
        if (!"ACTIVE".equals(account.getStatus())) {
            return AccountValidationResponse.invalid(accountNo, "Account status is " + account.getStatus());
        }

        BigDecimal available = account.getBalance() != null
                ? account.getBalance().getAvailableBalance() : BigDecimal.ZERO;

        if (requiredAmount != null && available.compareTo(requiredAmount) < 0) {
            return AccountValidationResponse.invalid(accountNo,
                    "Insufficient funds: available=" + available + ", required=" + requiredAmount);
        }

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
