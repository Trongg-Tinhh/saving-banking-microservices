package com.saving.transaction.controller;

import com.saving.transaction.common.ApiResponse;
import com.saving.transaction.dto.request.RecordTransactionRequest;
import com.saving.transaction.dto.response.TransactionResponse;
import com.saving.transaction.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Tag(name = "Transactions", description = "Transaction ledger")
public class TransactionController {

    private final TransactionService transactionService;

    // ── Health ────────────────────────────────────────────────────────────────

    @GetMapping("/health")
    @Operation(summary = "Health check", security = {})
    public ResponseEntity<ApiResponse<String>> health() {
        return ResponseEntity.ok(ApiResponse.success("UP", "Transaction Service is running"));
    }

    // ── Internal: record transaction (called by other services) ───────────────

    @PostMapping("/internal")
    @Operation(summary = "[INTERNAL] Record a transaction", security = {})
    public ResponseEntity<ApiResponse<TransactionResponse>> recordInternal(
            @Valid @RequestBody RecordTransactionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        transactionService.recordTransaction(request),
                        "Transaction recorded"));
    }

    // ── Public: list with filters ─────────────────────────────────────────────

    @GetMapping
    @Operation(summary = "List transactions with optional filters",
               security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<ApiResponse<Page<TransactionResponse>>> listTransactions(
            @RequestParam(required = false) String contractNo,
            @RequestParam(required = false) String cif,
            @RequestParam(required = false) String txType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                transactionService.listTransactions(
                        contractNo, cif, txType, status, fromDate, toDate, pageable),
                "Transactions retrieved"));
    }

    // ── Public: query by ID/ref/account/cif/contract ──────────────────────────

    @GetMapping("/{transactionId}")
    @Operation(summary = "Get transaction by ID",
               security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<ApiResponse<TransactionResponse>> getById(
            @PathVariable UUID transactionId) {
        return ResponseEntity.ok(ApiResponse.success(
                transactionService.getTransaction(transactionId)));
    }

    @GetMapping("/ref/{transactionRef}")
    @Operation(summary = "Get transaction by reference",
               security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<ApiResponse<TransactionResponse>> getByRef(
            @PathVariable String transactionRef) {
        return ResponseEntity.ok(ApiResponse.success(
                transactionService.getByRef(transactionRef)));
    }

    @GetMapping("/account/{accountNo}")
    @Operation(summary = "List transactions by account",
               security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<ApiResponse<Page<TransactionResponse>>> getByAccount(
            @PathVariable String accountNo,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                transactionService.getByAccount(accountNo, pageable),
                "Transactions retrieved"));
    }

    @GetMapping("/cif/{cif}")
    @Operation(summary = "List transactions by CIF",
               security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<ApiResponse<Page<TransactionResponse>>> getByCif(
            @PathVariable String cif,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                transactionService.getByCif(cif, pageable),
                "Transactions retrieved"));
    }

    @GetMapping("/contract/{contractNo}")
    @Operation(summary = "List transactions by contract",
               security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<ApiResponse<Page<TransactionResponse>>> getByContract(
            @PathVariable String contractNo,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                transactionService.getByContract(contractNo, pageable),
                "Transactions retrieved"));
    }
}
