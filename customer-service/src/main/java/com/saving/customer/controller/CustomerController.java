package com.saving.customer.controller;

import com.saving.customer.common.ApiResponse;
import com.saving.customer.dto.request.CreateContactRequest;
import com.saving.customer.dto.request.CreateCustomerRequest;
import com.saving.customer.dto.request.UpdateContactRequest;
import com.saving.customer.dto.request.UpdateCustomerRequest;
import com.saving.customer.dto.request.UpdateKycStatusRequest;
import com.saving.customer.dto.response.*;
import com.saving.customer.exception.BusinessException;
import com.saving.customer.exception.ErrorCode;
import com.saving.customer.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Customer Management", description = "CIF management, KYC, contacts")
public class CustomerController {

    private final CustomerService customerService;

    // ── POST /api/v1/customers ────────────────────────────────────

    @PostMapping
    @Operation(summary = "Create customer",
               description = "Create a new customer with primary contact. Auto-generates CIF.",
               security = @SecurityRequirement(name = "Bearer Authentication"))
    @PreAuthorize("hasAnyAuthority('TELLER','ADMIN')")
    public ResponseEntity<ApiResponse<CustomerResponse>> createCustomer(
            @Valid @RequestBody CreateCustomerRequest request) {

        CustomerResponse response = customerService.createCustomer(request);
        log.info("Customer created: cif={}", response.getCif());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Customer created successfully"));
    }

    // ── GET /api/v1/customers ─────────────────────────────────────

    @GetMapping
    @Operation(summary = "Search customers",
               description = "Search by name, ID number, phone, email or status. Paginated.",
               security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<Page<CustomerSummaryResponse>>> searchCustomers(
            @RequestParam(required = false) String fullName,
            @RequestParam(required = false) String idNumber,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0")   int page,
            @RequestParam(defaultValue = "20")  int size) {

        Pageable pageable = PageRequest.of(page, Math.min(size, 100),
                Sort.by(Sort.Direction.ASC, "fullName"));

        Page<CustomerSummaryResponse> result =
                customerService.searchCustomers(fullName, idNumber, phone, email, status, pageable);

        return ResponseEntity.ok(ApiResponse.success(result, "Customers retrieved"));
    }

    // ── GET /api/v1/customers/{cif} ───────────────────────────────

    @GetMapping("/{cif}")
    @Operation(summary = "Get customer by CIF",
               description = "Returns full customer profile including KYC status and contacts.",
               security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<CustomerResponse>> getCustomer(
            @PathVariable String cif) {

        CustomerResponse response = customerService.getCustomerByCif(cif);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ── PUT /api/v1/customers/{cif} ───────────────────────────────
    // Staff (TELLER/ADMIN/MANAGER): update any customer, including status field.
    // CUSTOMER: can only update their OWN profile (CIF must match JWT claim);
    //           status field is silently ignored even if sent.

    @PutMapping("/{cif}")
    @Operation(summary = "Update customer",
               description = "Update basic customer information. " +
                             "Staff can update any customer including status. " +
                             "Customers can only update their own profile (name, DOB, gender, nationality).",
               security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<CustomerResponse>> updateCustomer(
            @PathVariable String cif,
            @Valid @RequestBody UpdateCustomerRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        requireOwnershipOrStaff(cif, authentication, httpRequest);

        boolean isStaff = authentication.getAuthorities().stream()
                .anyMatch(a -> List.of("TELLER", "ADMIN", "MANAGER").contains(a.getAuthority()));

        CustomerResponse response = customerService.updateCustomer(cif, request, isStaff);
        return ResponseEntity.ok(ApiResponse.success(response, "Customer updated successfully"));
    }

    // ── GET /api/v1/customers/{cif}/kyc ──────────────────────────

    @GetMapping("/{cif}/kyc")
    @Operation(summary = "Get KYC details",
               description = "Returns KYC status and verification details for a customer.",
               security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<CustomerKycResponse>> getKyc(
            @PathVariable String cif) {

        CustomerKycResponse response = customerService.getKyc(cif);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ── PUT /api/v1/customers/{cif}/kyc/status ────────────────────

    @PutMapping("/{cif}/kyc/status")
    @Operation(summary = "Update KYC status",
               description = "Update KYC verification status. VERIFIED/REJECTED requires acting user.",
               security = @SecurityRequirement(name = "Bearer Authentication"))
    @PreAuthorize("hasAnyAuthority('TELLER','ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<CustomerKycResponse>> updateKycStatus(
            @PathVariable String cif,
            @Valid @RequestBody UpdateKycStatusRequest request,
            @AuthenticationPrincipal String username) {

        CustomerKycResponse response = customerService.updateKycStatus(cif, request, username);
        return ResponseEntity.ok(ApiResponse.success(response, "KYC status updated successfully"));
    }

    // ── GET /api/v1/customers/{cif}/contacts ─────────────────────

    @GetMapping("/{cif}/contacts")
    @Operation(summary = "Get customer contacts",
               description = "Returns all contact records for a customer.",
               security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<List<CustomerContactResponse>>> getContacts(
            @PathVariable String cif) {

        List<CustomerContactResponse> contacts = customerService.getContacts(cif);
        return ResponseEntity.ok(ApiResponse.success(contacts));
    }

    // ── POST /api/v1/customers/{cif}/contacts ────────────────────

    @PostMapping("/{cif}/contacts")
    @Operation(summary = "Add contact",
               description = "Add a new contact (phone/email/address) for a customer. Max 5 contacts. " +
                             "CUSTOMER role can add contacts for their own CIF; staff can add for any CIF.",
               security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<CustomerContactResponse>> addContact(
            @PathVariable String cif,
            @Valid @RequestBody CreateContactRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        requireOwnershipOrStaff(cif, authentication, httpRequest);

        CustomerContactResponse response = customerService.addContact(cif, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Contact added successfully"));
    }

    // ── PUT /api/v1/customers/{cif}/contacts/{contactId} ─────────

    @PutMapping("/{cif}/contacts/{contactId}")
    @Operation(summary = "Update contact",
               description = "Update an existing contact record (phone, email, address). " +
                             "Customers can update their own contacts; staff can update any.",
               security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<CustomerContactResponse>> updateContact(
            @PathVariable String cif,
            @PathVariable UUID   contactId,
            @Valid @RequestBody UpdateContactRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        requireOwnershipOrStaff(cif, authentication, httpRequest);

        CustomerContactResponse response = customerService.updateContact(cif, contactId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Contact updated successfully"));
    }

    // ── GET /api/v1/customers/internal/{cif}/validate ─────────────
    // Internal endpoint — called by saving-contract-service, account-service
    // No authentication required (network-level security via Docker internal network)

    @GetMapping("/internal/{cif}/validate")
    @Operation(summary = "[INTERNAL] Validate customer",
               description = "Checks if CIF exists, customer is ACTIVE, and KYC is VERIFIED. " +
                             "Called internally by other microservices. No auth required.")
    public ResponseEntity<ApiResponse<CustomerValidationResponse>> validateCustomer(
            @PathVariable String cif) {

        CustomerValidationResponse response = customerService.validateCustomer(cif);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ── GET /api/v1/customers/health ──────────────────────────────

    @GetMapping("/health")
    @Operation(summary = "Service health check")
    public ResponseEntity<ApiResponse<String>> health() {
        return ResponseEntity.ok(ApiResponse.success("Customer Service is UP", "OK"));
    }

    // ── Helpers ───────────────────────────────────────────────────

    /**
     * Throws FORBIDDEN if the calling user is not a staff member AND
     * the CIF in their JWT does not match the requested CIF.
     * Staff (TELLER / ADMIN / MANAGER) always passes.
     */
    private void requireOwnershipOrStaff(String cif,
                                         Authentication authentication,
                                         HttpServletRequest httpRequest) {
        boolean isStaff = authentication.getAuthorities().stream()
                .anyMatch(a -> List.of("TELLER", "ADMIN", "MANAGER").contains(a.getAuthority()));
        if (!isStaff) {
            String userCif = (String) httpRequest.getAttribute("cif");
            if (userCif == null || !userCif.equals(cif)) {
                throw new BusinessException(
                        ErrorCode.FORBIDDEN,
                        "Khách hàng chỉ được thao tác trên dữ liệu của chính mình");
            }
        }
    }
}
