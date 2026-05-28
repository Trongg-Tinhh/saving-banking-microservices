package com.saving.contract.controller;

import com.saving.contract.common.ApiResponse;
import com.saving.contract.dto.request.CloseContractRequest;
import com.saving.contract.dto.request.OpenContractRequest;
import com.saving.contract.dto.request.UpdateMaturityInstructionRequest;
import com.saving.contract.dto.response.CloseContractResponse;
import com.saving.contract.dto.response.ContractResponse;
import com.saving.contract.dto.response.ContractStatusHistoryResponse;
import com.saving.contract.dto.response.ContractSummaryResponse;
import com.saving.contract.service.JwtService;
import com.saving.contract.service.SavingContractService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/contracts")
@RequiredArgsConstructor
@Tag(name = "Saving Contracts", description = "Contract lifecycle management")
public class SavingContractController {

    private final SavingContractService contractService;
    private final JwtService            jwtService;

    // ────────────────────────────────────────────────────────────────────────
    // Health
    // ────────────────────────────────────────────────────────────────────────

    @GetMapping("/health")
    @Operation(summary = "Health check", security = {})
    public ResponseEntity<ApiResponse<String>> health() {
        return ResponseEntity.ok(ApiResponse.success("UP", "Saving Contract Service is running"));
    }

    // ────────────────────────────────────────────────────────────────────────
    // Open Contract
    // ────────────────────────────────────────────────────────────────────────

    @PostMapping
    @Operation(summary = "Open a new saving contract",
               description = "Staff (TELLER/ADMIN) can open contracts for any CIF. " +
                             "CUSTOMER can only open contracts for their own CIF.",
               security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<ApiResponse<ContractResponse>> openContract(
            @Valid @RequestBody OpenContractRequest request,
            @RequestHeader("Authorization") String authHeader,
            HttpServletRequest httpRequest) {

        String       bearerToken = stripBearer(authHeader);
        String       openedBy    = jwtService.extractUsername(bearerToken);
        List<String> roles       = jwtService.extractRoles(bearerToken);

        boolean isStaff = roles.stream()
                .anyMatch(r -> r.equals("TELLER") || r.equals("ADMIN") || r.equals("MANAGER"));

        // CUSTOMER: may only open a contract for their own CIF
        if (!isStaff) {
            String userCif = (String) httpRequest.getAttribute("cif");
            if (userCif == null || !userCif.equals(request.getCif())) {
                throw new AccessDeniedException(
                        "Khách hàng chỉ được mở sổ tiết kiệm cho CIF của chính mình");
            }
        }

        ContractResponse response = contractService.openContract(request, bearerToken, openedBy);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Contract opened successfully"));
    }

    // ────────────────────────────────────────────────────────────────────────
    // Get Contract
    // ────────────────────────────────────────────────────────────────────────

    @GetMapping("/{contractNo}")
    @Operation(summary = "Get contract details",
               security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<ApiResponse<ContractResponse>> getContract(
            @PathVariable String contractNo) {

        return ResponseEntity.ok(ApiResponse.success(contractService.getContract(contractNo),
                "Contract retrieved"));
    }

    // ────────────────────────────────────────────────────────────────────────
    // Status History
    // ────────────────────────────────────────────────────────────────────────

    @GetMapping("/{contractNo}/status-history")
    @Operation(summary = "Get status change history of a contract",
               security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<ApiResponse<List<ContractStatusHistoryResponse>>> getStatusHistory(
            @PathVariable String contractNo) {

        return ResponseEntity.ok(ApiResponse.success(
                contractService.getStatusHistory(contractNo), "Status history retrieved"));
    }

    // ────────────────────────────────────────────────────────────────────────
    // List Contracts
    // ────────────────────────────────────────────────────────────────────────

    @GetMapping
    @Operation(summary = "List saving contracts",
               description = "Staff: list any CIF or all when cif is omitted. " +
                             "CUSTOMER: always scoped to their own CIF (JWT claim) regardless of query param.",
               security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<ApiResponse<Page<ContractSummaryResponse>>> listContracts(
            @Parameter(description = "Filter by customer CIF (optional for staff, ignored for CUSTOMER)")
            @RequestParam(required = false) String cif,
            @Parameter(description = "Filter by contract status (optional)")
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20, sort = "openDate,desc") Pageable pageable,
            @RequestHeader("Authorization") String authHeader,
            HttpServletRequest httpRequest) {

        List<String> roles  = jwtService.extractRoles(stripBearer(authHeader));
        boolean isStaff     = roles.stream()
                .anyMatch(r -> r.equals("TELLER") || r.equals("ADMIN") || r.equals("MANAGER"));

        String effectiveCif;
        if (isStaff) {
            // Staff may pass an optional CIF filter, or leave it null to list all
            effectiveCif = (cif != null && !cif.isBlank()) ? cif : null;
        } else {
            // CUSTOMER: always use the CIF stored in their JWT — ignore any request param
            effectiveCif = (String) httpRequest.getAttribute("cif");
        }

        return ResponseEntity.ok(ApiResponse.success(
                contractService.listContracts(effectiveCif, status, pageable),
                "Contracts retrieved"));
    }

    // ────────────────────────────────────────────────────────────────────────
    // List by Status
    // ────────────────────────────────────────────────────────────────────────

    @GetMapping("/status/{status}")
    @Operation(summary = "List contracts by status",
               security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<ApiResponse<Page<ContractSummaryResponse>>> listByStatus(
            @PathVariable String status,
            @PageableDefault(size = 20, sort = "openDate") Pageable pageable) {

        return ResponseEntity.ok(ApiResponse.success(
                contractService.getContractsByStatus(status, pageable), "Contracts retrieved"));
    }

    // ────────────────────────────────────────────────────────────────────────
    // Close Contract
    // ────────────────────────────────────────────────────────────────────────

    @PostMapping("/{contractNo}/close")
    @Operation(summary = "Close a contract (maturity or early withdrawal)",
               description = "Staff (TELLER/ADMIN) có thể đóng bất kỳ hợp đồng nào. " +
                             "CUSTOMER chỉ được đóng hợp đồng thuộc CIF của chính mình.",
               security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<ApiResponse<CloseContractResponse>> closeContract(
            @PathVariable String contractNo,
            @Valid @RequestBody CloseContractRequest request,
            @RequestHeader("Authorization") String authHeader,
            HttpServletRequest httpRequest) {

        String       bearerToken = stripBearer(authHeader);
        String       closedBy    = jwtService.extractUsername(bearerToken);
        List<String> roles       = jwtService.extractRoles(bearerToken);

        boolean isStaff = roles.stream()
                .anyMatch(r -> r.equals("TELLER") || r.equals("ADMIN") || r.equals("MANAGER"));

        // CUSTOMER: chỉ được tất toán hợp đồng của chính mình
        if (!isStaff) {
            String userCif       = (String) httpRequest.getAttribute("cif");
            String contractOwner = contractService.getContractCif(contractNo);
            if (userCif == null || !userCif.equals(contractOwner)) {
                throw new AccessDeniedException(
                        "Khách hàng chỉ được tất toán hợp đồng của chính mình");
            }
        }

        CloseContractResponse response =
                contractService.closeContract(contractNo, request, bearerToken, closedBy);

        return ResponseEntity.ok(ApiResponse.success(response, "Contract closed successfully"));
    }

    // ────────────────────────────────────────────────────────────────────────
    // Update Maturity Instruction
    // ────────────────────────────────────────────────────────────────────────

    @PutMapping("/{contractNo}/maturity-instruction")
    @Operation(summary = "Set or update maturity instruction",
               security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<ApiResponse<ContractResponse>> updateMaturityInstruction(
            @PathVariable String contractNo,
            @Valid @RequestBody UpdateMaturityInstructionRequest request,
            @RequestHeader("Authorization") String authHeader) {

        String bearerToken = stripBearer(authHeader);
        String updatedBy   = jwtService.extractUsername(bearerToken);

        ContractResponse response =
                contractService.updateMaturityInstruction(contractNo, request, updatedBy);

        return ResponseEntity.ok(ApiResponse.success(response, "Maturity instruction updated"));
    }

    // ────────────────────────────────────────────────────────────────────────
    // Internal endpoints (service-to-service, no JWT required)
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Called daily by the Saving Lifecycle Service to get all ACTIVE contracts
     * with MONTHLY or QUARTERLY interest payment that haven't matured yet.
     * The lifecycle service will filter which ones are due today.
     */
    @GetMapping("/internal/periodic-interest-due")
    @Operation(summary = "[INTERNAL] List ACTIVE contracts with periodic interest payment", security = {})
    public ResponseEntity<ApiResponse<Page<ContractSummaryResponse>>> listPeriodicInterestContracts(
            @PageableDefault(size = 500) Pageable pageable) {

        return ResponseEntity.ok(ApiResponse.success(
                contractService.getPeriodicInterestContracts(pageable),
                "Periodic interest contracts retrieved"));
    }

    /**
     * Called by the Saving Lifecycle Service (Python APScheduler) to mark
     * a contract as MATURED when its maturity date is reached.
     */
    @PostMapping("/internal/{contractNo}/mark-matured")
    @Operation(summary = "[INTERNAL] Mark contract as MATURED", security = {})
    public ResponseEntity<ApiResponse<ContractResponse>> markMatured(
            @PathVariable String contractNo) {

        ContractResponse response = contractService.markMatured(contractNo);
        return ResponseEntity.ok(ApiResponse.success(response, "Contract marked as MATURED"));
    }

    /**
     * Called by the Saving Lifecycle Service to fetch contracts that have
     * reached maturity as of today (for batch processing).
     */
    @GetMapping("/internal/matured")
    @Operation(summary = "[INTERNAL] List ACTIVE contracts that have reached maturity", security = {})
    public ResponseEntity<ApiResponse<Page<ContractSummaryResponse>>> listMaturedContracts(
            @PageableDefault(size = 100) Pageable pageable) {

        // Returns MATURED status contracts for the lifecycle service to process
        Page<ContractSummaryResponse> matured =
                contractService.getContractsByStatus("MATURED", pageable);

        return ResponseEntity.ok(ApiResponse.success(matured, "Matured contracts retrieved"));
    }

    // ────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ────────────────────────────────────────────────────────────────────────

    private String stripBearer(String authHeader) {
        return (authHeader != null && authHeader.startsWith("Bearer "))
                ? authHeader.substring(7)
                : authHeader;
    }
}
