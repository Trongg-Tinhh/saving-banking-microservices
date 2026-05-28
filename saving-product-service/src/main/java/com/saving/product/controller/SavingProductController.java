package com.saving.product.controller;

import com.saving.product.common.ApiResponse;
import com.saving.product.dto.request.*;
import com.saving.product.dto.response.*;
import com.saving.product.service.SavingProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Saving Product Catalog", description = "Products, terms, interest rates, early-withdrawal policies")
public class SavingProductController {

    private final SavingProductService productService;

    // ── GET /api/v1/products ──────────────────────────────────────
    @GetMapping
    @Operation(summary = "List saving products",
               description = "ADMIN: activeOnly=false returns all products including inactive. " +
                             "Non-admin: always returns active products only regardless of param.",
               security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<List<SavingProductResponse>>> listProducts(
            @RequestParam(defaultValue = "false") boolean activeOnly) {
        // Non-admin users may only see active products
        boolean effectiveActiveOnly = activeOnly || !isAdmin();
        return ResponseEntity.ok(ApiResponse.success(productService.listProducts(effectiveActiveOnly)));
    }

    private boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> "ADMIN".equals(a.getAuthority()));
    }

    // ── GET /api/v1/products/{productCode} ────────────────────────
    @GetMapping("/{productCode}")
    @Operation(summary = "Get product detail",
               description = "Returns full product detail: terms with current rates + early withdrawal policy.",
               security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<SavingProductResponse>> getProduct(
            @PathVariable String productCode) {
        return ResponseEntity.ok(ApiResponse.success(productService.getProduct(productCode)));
    }

    // ── POST /api/v1/products ─────────────────────────────────────
    @PostMapping
    @Operation(summary = "Create product (ADMIN)",
               description = "Create a new saving product definition.",
               security = @SecurityRequirement(name = "Bearer Authentication"))
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<SavingProductResponse>> createProduct(
            @Valid @RequestBody CreateProductRequest request) {
        SavingProductResponse resp = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(resp, "Product created"));
    }

    // ── PUT /api/v1/products/{productCode} ────────────────────────
    @PutMapping("/{productCode}")
    @Operation(summary = "Update product (ADMIN)",
               description = "Update product name, limits, active status, or description.",
               security = @SecurityRequirement(name = "Bearer Authentication"))
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<SavingProductResponse>> updateProduct(
            @PathVariable String productCode,
            @Valid @RequestBody UpdateProductRequest request) {
        return ResponseEntity.ok(ApiResponse.success(productService.updateProduct(productCode, request),
                "Product updated"));
    }

    // ── GET /api/v1/products/{productCode}/terms ──────────────────
    @GetMapping("/{productCode}/terms")
    @Operation(summary = "Get terms for a product",
               description = "Returns all terms with current annual rates.",
               security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<List<SavingTermResponse>>> getTerms(
            @PathVariable String productCode,
            @RequestParam(defaultValue = "true") boolean activeOnly) {
        return ResponseEntity.ok(ApiResponse.success(productService.getTerms(productCode, activeOnly)));
    }

    // ── POST /api/v1/products/{productCode}/terms ─────────────────
    @PostMapping("/{productCode}/terms")
    @Operation(summary = "Add term to product (ADMIN)",
               description = "Add a new tenor (e.g. 18-month) to an existing product.",
               security = @SecurityRequirement(name = "Bearer Authentication"))
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<SavingTermResponse>> createTerm(
            @PathVariable String productCode,
            @Valid @RequestBody CreateTermRequest request) {
        SavingTermResponse resp = productService.createTerm(productCode, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(resp, "Term created"));
    }

    // ── PUT /api/v1/products/{productCode}/terms/{termId} ────────
    @PutMapping("/{productCode}/terms/{termId}")
    @Operation(summary = "Update term (ADMIN)",
               description = "Rename the term label or toggle its active status. " +
                             "Disabling a term does NOT affect existing contracts.",
               security = @SecurityRequirement(name = "Bearer Authentication"))
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<SavingTermResponse>> updateTerm(
            @PathVariable String productCode,
            @PathVariable String termId,
            @Valid @RequestBody UpdateTermRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                productService.updateTerm(productCode, termId, request), "Term updated"));
    }

    // ── GET /api/v1/products/{productCode}/rates ──────────────────
    @GetMapping("/{productCode}/rates")
    @Operation(summary = "Get rate history",
               description = "Returns all interest rate configurations for a product (optionally filtered by term).",
               security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<List<InterestRateResponse>>> getRates(
            @PathVariable String productCode,
            @RequestParam(required = false) String termId) {
        return ResponseEntity.ok(ApiResponse.success(productService.getRateConfigs(productCode, termId)));
    }

    // ── POST /api/v1/products/{productCode}/rates ─────────────────
    @PostMapping("/{productCode}/rates")
    @Operation(summary = "Add interest rate config (ADMIN)",
               description = "Add a new rate for a term with an effective date range. " +
                             "effectiveTo=null means the rate is current (no end date).",
               security = @SecurityRequirement(name = "Bearer Authentication"))
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<InterestRateResponse>> addRate(
            @PathVariable String productCode,
            @Valid @RequestBody CreateRateConfigRequest request) {
        InterestRateResponse resp = productService.addRateConfig(productCode, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(resp, "Rate config added"));
    }

    // ── GET /api/v1/products/{productCode}/early-withdrawal ───────
    @GetMapping("/{productCode}/early-withdrawal")
    @Operation(summary = "Get early withdrawal policy",
               description = "Returns the early withdrawal penalty policy for a product.",
               security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<EarlyWithdrawalPolicyResponse>> getEarlyWithdrawalPolicy(
            @PathVariable String productCode) {
        return ResponseEntity.ok(ApiResponse.success(productService.getEarlyWithdrawalPolicy(productCode)));
    }

    // ── POST /api/v1/products/{productCode}/early-withdrawal ─────
    @PostMapping("/{productCode}/early-withdrawal")
    @Operation(summary = "Upsert early withdrawal policy (ADMIN)",
               description = "Create or update the early-withdrawal penalty policy for a product.",
               security = @SecurityRequirement(name = "Bearer Authentication"))
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<EarlyWithdrawalPolicyResponse>> upsertEarlyWithdrawalPolicy(
            @PathVariable String productCode,
            @Valid @RequestBody UpsertEarlyWithdrawalPolicyRequest request) {
        EarlyWithdrawalPolicyResponse resp = productService.upsertEarlyWithdrawalPolicy(productCode, request);
        return ResponseEntity.ok(ApiResponse.success(resp, "Policy saved"));
    }

    // ── GET /api/v1/products/internal/{productCode}/terms/{termId}/rate ──
    // INTERNAL — No JWT required. Called by saving-contract-service to lock in rate at contract open.

    @GetMapping("/internal/{productCode}/terms/{termId}/rate")
    @Operation(summary = "[INTERNAL] Query product rate",
               description = "Returns product + term + effective interest rate for a given date. " +
                             "Called by saving-contract-service when opening a new contract. No auth required.")
    public ResponseEntity<ApiResponse<ProductRateQueryResponse>> queryRate(
            @PathVariable String productCode,
            @PathVariable String termId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate effectiveDate) {

        ProductRateQueryResponse resp = productService.queryProductRate(productCode, termId, effectiveDate);
        return ResponseEntity.ok(ApiResponse.success(resp));
    }

    // ── GET /api/v1/products/health ───────────────────────────────
    @GetMapping("/health")
    @Operation(summary = "Service health check")
    public ResponseEntity<ApiResponse<String>> health() {
        return ResponseEntity.ok(ApiResponse.success("Saving Product Service is UP", "OK"));
    }
}
