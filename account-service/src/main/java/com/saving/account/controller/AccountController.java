package com.saving.account.controller;

import com.saving.account.common.ApiResponse;
import com.saving.account.dto.request.*;
import com.saving.account.dto.response.*;
import com.saving.account.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Account Management", description = "Account lifecycle, balance operations, hold/release")
public class AccountController {

    private final AccountService accountService;

    // ── POST /api/v1/accounts ─────────────────────────────────────

    @PostMapping
    @Operation(summary = "Create account",
               description = "Create a new PAYMENT/SAVING/LOAN account for a CIF. Auto-generates account number.",
               security = @SecurityRequirement(name = "Bearer Authentication"))
    @PreAuthorize("hasAnyAuthority('TELLER','ADMIN')")
    public ResponseEntity<ApiResponse<AccountResponse>> createAccount(
            @Valid @RequestBody CreateAccountRequest request) {

        AccountResponse response = accountService.createAccount(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Account created successfully"));
    }

    // ── GET /api/v1/accounts/{accountNo} ──────────────────────────

    @GetMapping("/{accountNo}")
    @Operation(summary = "Get account",
               description = "Returns account info with current balance.",
               security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<AccountResponse>> getAccount(
            @PathVariable String accountNo) {

        return ResponseEntity.ok(ApiResponse.success(accountService.getAccount(accountNo)));
    }

    // ── GET /api/v1/accounts?cif=xxx ─────────────────────────────

    @GetMapping
    @Operation(summary = "List accounts by CIF",
               description = "Returns all accounts for a CIF with balance summaries.",
               security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<List<AccountSummaryResponse>>> getAccountsByCif(
            @RequestParam String cif) {

        List<AccountSummaryResponse> result = accountService.getAccountsByCif(cif);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // ── GET /api/v1/accounts/{accountNo}/balance ──────────────────

    @GetMapping("/{accountNo}/balance")
    @Operation(summary = "Get balance",
               description = "Returns available, ledger, and hold amounts.",
               security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<AccountBalanceResponse>> getBalance(
            @PathVariable String accountNo) {

        return ResponseEntity.ok(ApiResponse.success(accountService.getBalance(accountNo)));
    }

    // ── PUT /api/v1/accounts/{accountNo}/status ───────────────────

    @PutMapping("/{accountNo}/status")
    @Operation(summary = "Update account status",
               description = "Set account to ACTIVE, BLOCKED, or CLOSED. Closing requires zero balance.",
               security = @SecurityRequirement(name = "Bearer Authentication"))
    @PreAuthorize("hasAnyAuthority('TELLER','ADMIN')")
    public ResponseEntity<ApiResponse<AccountResponse>> updateStatus(
            @PathVariable String accountNo,
            @Valid @RequestBody UpdateAccountStatusRequest request) {

        AccountResponse response = accountService.updateStatus(accountNo, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Account status updated"));
    }

    // ── POST /api/v1/accounts/{accountNo}/debit ───────────────────

    @PostMapping("/{accountNo}/debit")
    @Operation(summary = "Debit account",
               description = "Debit an amount from available balance. Provide unique `reference` for idempotency. " +
                             "Set `useHold=true` to debit against pre-placed hold.",
               security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<BalanceOperationResponse>> debit(
            @PathVariable String accountNo,
            @Valid @RequestBody DebitRequest request) {

        BalanceOperationResponse response = accountService.debit(accountNo, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Debit successful"));
    }

    // ── POST /api/v1/accounts/{accountNo}/credit ──────────────────

    @PostMapping("/{accountNo}/credit")
    @Operation(summary = "Credit account",
               description = "Credit an amount to the account. Provide unique `reference` for idempotency.",
               security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<BalanceOperationResponse>> credit(
            @PathVariable String accountNo,
            @Valid @RequestBody CreditRequest request) {

        BalanceOperationResponse response = accountService.credit(accountNo, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Credit successful"));
    }

    // ── POST /api/v1/accounts/{accountNo}/hold ────────────────────

    @PostMapping("/{accountNo}/hold")
    @Operation(summary = "Place fund hold",
               description = "Reserve funds by placing a hold. Reduces available balance. " +
                             "Use `holdRef` to identify and later release the hold.",
               security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<BalanceOperationResponse>> placeHold(
            @PathVariable String accountNo,
            @Valid @RequestBody HoldRequest request) {

        BalanceOperationResponse response = accountService.placeHold(accountNo, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Hold placed successfully"));
    }

    // ── POST /api/v1/accounts/{accountNo}/release-hold ────────────

    @PostMapping("/{accountNo}/release-hold")
    @Operation(summary = "Release fund hold",
               description = "Release a previously placed hold, returning funds to available balance.",
               security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<BalanceOperationResponse>> releaseHold(
            @PathVariable String accountNo,
            @Valid @RequestBody ReleaseHoldRequest request) {

        BalanceOperationResponse response = accountService.releaseHold(accountNo, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Hold released successfully"));
    }

    // ── Internal endpoints — no JWT required ─────────────────────

    @PostMapping("/internal/{accountNo}/credit")
    @Operation(summary = "[INTERNAL] Credit account (no JWT)",
               description = "Called by lifecycle/contract services for automated interest disbursement.")
    public ResponseEntity<ApiResponse<BalanceOperationResponse>> creditInternal(
            @PathVariable String accountNo,
            @Valid @RequestBody CreditRequest request) {

        BalanceOperationResponse response = accountService.credit(accountNo, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Credit successful"));
    }

    @GetMapping("/internal/{accountNo}/validate")
    @Operation(summary = "[INTERNAL] Validate account",
               description = "Called by other microservices to check account status and available balance. " +
                             "Pass `requiredAmount` to verify sufficient funds.")
    public ResponseEntity<ApiResponse<AccountValidationResponse>> validateAccount(
            @PathVariable String accountNo,
            @RequestParam(required = false) BigDecimal requiredAmount) {

        AccountValidationResponse response = accountService.validateAccount(accountNo, requiredAmount);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ── GET /api/v1/accounts/health ───────────────────────────────

    @GetMapping("/health")
    @Operation(summary = "Service health check")
    public ResponseEntity<ApiResponse<String>> health() {
        return ResponseEntity.ok(ApiResponse.success("Account Service is UP", "OK"));
    }
}
