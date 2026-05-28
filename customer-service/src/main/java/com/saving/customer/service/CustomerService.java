package com.saving.customer.service;

import com.saving.customer.dto.request.CreateContactRequest;
import com.saving.customer.dto.request.CreateCustomerRequest;
import com.saving.customer.dto.request.UpdateContactRequest;
import com.saving.customer.dto.request.UpdateCustomerRequest;
import com.saving.customer.dto.request.UpdateKycStatusRequest;
import com.saving.customer.dto.response.*;
import com.saving.customer.entity.Customer;
import com.saving.customer.entity.CustomerContact;
import com.saving.customer.entity.CustomerKyc;
import com.saving.customer.exception.BusinessException;
import com.saving.customer.exception.ErrorCode;
import com.saving.customer.repository.CustomerContactRepository;
import com.saving.customer.repository.CustomerKycRepository;
import com.saving.customer.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerService {

    private final CustomerRepository        customerRepository;
    private final CustomerKycRepository     customerKycRepository;
    private final CustomerContactRepository contactRepository;
    private final CustomerEventPublisher    eventPublisher;

    // ── Create Customer ────────────────────────────────────────────

    @Transactional
    public CustomerResponse createCustomer(CreateCustomerRequest request) {

        // Duplicate ID number check
        if (customerRepository.existsByIdNumber(request.getIdNumber())) {
            throw new BusinessException(ErrorCode.CUSTOMER_ALREADY_EXISTS,
                    "ID number: " + request.getIdNumber());
        }

        // Generate CIF: CIF + zero-padded 4-digit sequence
        String cif = generateCif();

        // Build Customer entity
        Customer customer = Customer.builder()
                .cif(cif)
                .fullName(request.getFullName())
                .dateOfBirth(request.getDateOfBirth())
                .gender(request.getGender())
                .nationality(request.getNationality() != null ? request.getNationality() : "VN")
                .idNumber(request.getIdNumber())
                .idType(request.getIdType())
                .status("ACTIVE")
                .build();

        customer = customerRepository.save(customer);

        // Create default KYC record (NOT_VERIFIED)
        CustomerKyc kyc = CustomerKyc.builder()
                .customer(customer)
                .kycStatus("NOT_VERIFIED")
                .build();
        customerKycRepository.save(kyc);
        customer.setKyc(kyc);

        // Create primary contact
        if (request.getPrimaryContact() != null) {
            CreateContactRequest contactReq = request.getPrimaryContact();
            CustomerContact contact = CustomerContact.builder()
                    .customer(customer)
                    .phoneNumber(contactReq.getPhoneNumber())
                    .email(contactReq.getEmail())
                    .address(contactReq.getAddress())
                    .district(contactReq.getDistrict())
                    .city(contactReq.getCity())
                    .isPrimary(true)
                    .build();
            contactRepository.save(contact);
        }

        log.info("Customer created: cif={}, name={}", cif, request.getFullName());
        eventPublisher.publishCustomerCreated(cif, request.getFullName());

        return CustomerResponse.from(customer);
    }

    // ── Get Customer by CIF ────────────────────────────────────────

    @Transactional(readOnly = true)
    public CustomerResponse getCustomerByCif(String cif) {
        Customer customer = customerRepository.findByCifWithKyc(cif)
                .orElseThrow(() -> new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND, "cif=" + cif));

        // Load contacts separately (lazy)
        List<CustomerContact> contacts = contactRepository
                .findByCustomer_CifOrderByIsPrimaryDescCreatedAtAsc(cif);
        customer.setContacts(contacts);

        return CustomerResponse.fromWithContacts(customer);
    }

    // ── Search Customers ───────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<CustomerSummaryResponse> searchCustomers(
            String fullName, String idNumber, String phone,
            String email,    String status,   Pageable pageable) {

        // If searching by phone/email — use contact join
        if (StringUtils.hasText(phone) || StringUtils.hasText(email)) {
            return customerRepository.searchByContact(
                    StringUtils.hasText(phone) ? phone : null,
                    StringUtils.hasText(email) ? email : null,
                    pageable
            ).map(CustomerSummaryResponse::from);
        }

        return customerRepository.searchCustomers(
                StringUtils.hasText(fullName) ? fullName : null,
                StringUtils.hasText(idNumber) ? idNumber : null,
                StringUtils.hasText(status)   ? status   : null,
                pageable
        ).map(CustomerSummaryResponse::from);
    }

    // ── Update Customer ────────────────────────────────────────────

    /**
     * @param isStaff true nếu người gọi là TELLER/ADMIN/MANAGER.
     *                CUSTOMER chỉ được cập nhật fullName, dateOfBirth, gender, nationality;
     *                không được tự thay đổi status.
     */
    @Transactional
    public CustomerResponse updateCustomer(String cif, UpdateCustomerRequest request, boolean isStaff) {
        Customer customer = customerRepository.findByCifWithKyc(cif)
                .orElseThrow(() -> new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND, "cif=" + cif));

        if (StringUtils.hasText(request.getFullName()))    customer.setFullName(request.getFullName());
        if (request.getDateOfBirth() != null)              customer.setDateOfBirth(request.getDateOfBirth());
        if (StringUtils.hasText(request.getGender()))      customer.setGender(request.getGender());
        if (StringUtils.hasText(request.getNationality())) customer.setNationality(request.getNationality());
        // Chỉ staff mới được thay đổi trạng thái tài khoản khách hàng
        if (isStaff && StringUtils.hasText(request.getStatus())) {
            customer.setStatus(request.getStatus());
        }

        customer = customerRepository.save(customer);

        log.info("Customer updated: cif={}, byStaff={}", cif, isStaff);
        eventPublisher.publishCustomerUpdated(cif, customer.getFullName());

        return CustomerResponse.from(customer);
    }

    // ── KYC Management ────────────────────────────────────────────

    @Transactional(readOnly = true)
    public CustomerKycResponse getKyc(String cif) {
        // Ensure customer exists first
        if (!customerRepository.existsById(cif)) {
            throw new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND, "cif=" + cif);
        }

        CustomerKyc kyc = customerKycRepository.findByCustomer_Cif(cif)
                .orElseThrow(() -> new BusinessException(ErrorCode.KYC_NOT_FOUND, "cif=" + cif));

        return CustomerKycResponse.from(kyc);
    }

    @Transactional
    public CustomerKycResponse updateKycStatus(String cif, UpdateKycStatusRequest request,
                                               String actingUsername) {
        // Ensure customer exists
        if (!customerRepository.existsById(cif)) {
            throw new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND, "cif=" + cif);
        }

        CustomerKyc kyc = customerKycRepository.findByCustomer_Cif(cif)
                .orElseThrow(() -> new BusinessException(ErrorCode.KYC_NOT_FOUND, "cif=" + cif));

        String oldStatus = kyc.getKycStatus();

        // Business rules
        if ("REJECTED".equals(request.getKycStatus()) && !StringUtils.hasText(request.getRejectionReason())) {
            throw new BusinessException(ErrorCode.KYC_REJECTION_REASON_REQUIRED);
        }

        OffsetDateTime verifiedAt = "VERIFIED".equals(request.getKycStatus()) ? OffsetDateTime.now() : null;
        String verifiedBy = StringUtils.hasText(request.getVerifiedBy())
                ? request.getVerifiedBy()
                : actingUsername;
        String rejectionReason = "REJECTED".equals(request.getKycStatus())
                ? request.getRejectionReason()
                : null;

        // Update KYC fields
        kyc.setKycStatus(request.getKycStatus());
        kyc.setVerifiedAt(verifiedAt);
        kyc.setVerifiedBy(verifiedBy);
        kyc.setRejectionReason(rejectionReason);
        if (StringUtils.hasText(request.getDocType())) kyc.setDocType(request.getDocType());
        if (StringUtils.hasText(request.getDocUrl()))  kyc.setDocUrl(request.getDocUrl());

        kyc = customerKycRepository.save(kyc);

        log.info("KYC updated: cif={}, {}→{}, by={}", cif, oldStatus, request.getKycStatus(), verifiedBy);
        eventPublisher.publishKycStatusChanged(cif, oldStatus, request.getKycStatus(), verifiedBy);

        return CustomerKycResponse.from(kyc);
    }

    // ── Contact Management ────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<CustomerContactResponse> getContacts(String cif) {
        if (!customerRepository.existsById(cif)) {
            throw new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND, "cif=" + cif);
        }

        return contactRepository.findByCustomer_CifOrderByIsPrimaryDescCreatedAtAsc(cif)
                .stream()
                .map(CustomerContactResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public CustomerContactResponse addContact(String cif, CreateContactRequest request) {
        Customer customer = customerRepository.findById(cif)
                .orElseThrow(() -> new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND, "cif=" + cif));

        // Max 5 contacts per customer
        long count = contactRepository.countByCustomer_Cif(cif);
        if (count >= 5) {
            throw new BusinessException(ErrorCode.CONTACT_LIMIT_REACHED);
        }

        // If new contact is primary, unset existing primaries
        if (Boolean.TRUE.equals(request.getIsPrimary())) {
            contactRepository.unsetPrimaryContacts(cif);
        }

        CustomerContact contact = CustomerContact.builder()
                .customer(customer)
                .phoneNumber(request.getPhoneNumber())
                .email(request.getEmail())
                .address(request.getAddress())
                .district(request.getDistrict())
                .city(request.getCity())
                .isPrimary(Boolean.TRUE.equals(request.getIsPrimary()))
                .build();

        contact = contactRepository.save(contact);
        log.info("Contact added: cif={}, contactId={}", cif, contact.getContactId());

        return CustomerContactResponse.from(contact);
    }

    @Transactional
    public CustomerContactResponse updateContact(String cif, UUID contactId,
                                                 UpdateContactRequest request) {
        // Ensure customer exists
        if (!customerRepository.existsById(cif)) {
            throw new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND, "cif=" + cif);
        }

        CustomerContact contact = contactRepository.findById(contactId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONTACT_NOT_FOUND,
                        "contactId=" + contactId));

        // Safety: ensure the contact belongs to this CIF
        if (!cif.equals(contact.getCustomer().getCif())) {
            throw new BusinessException(ErrorCode.CONTACT_NOT_FOUND,
                    "Contact does not belong to cif=" + cif);
        }

        if (StringUtils.hasText(request.getPhoneNumber())) contact.setPhoneNumber(request.getPhoneNumber());
        if (StringUtils.hasText(request.getEmail()))       contact.setEmail(request.getEmail());
        if (StringUtils.hasText(request.getAddress()))     contact.setAddress(request.getAddress());
        if (StringUtils.hasText(request.getDistrict()))    contact.setDistrict(request.getDistrict());
        if (StringUtils.hasText(request.getCity()))        contact.setCity(request.getCity());

        contact = contactRepository.save(contact);
        log.info("Contact updated: cif={}, contactId={}", cif, contactId);

        return CustomerContactResponse.from(contact);
    }

    // ── Internal: validate CIF for other services ─────────────────

    @Transactional(readOnly = true)
    public CustomerValidationResponse validateCustomer(String cif) {
        Customer customer = customerRepository.findByCifWithKyc(cif).orElse(null);

        if (customer == null) {
            return CustomerValidationResponse.invalid(cif, "Customer not found");
        }
        if (!"ACTIVE".equals(customer.getStatus())) {
            return CustomerValidationResponse.invalid(cif,
                    "Customer status is " + customer.getStatus());
        }

        String kycStatus = customer.getKyc() != null ? customer.getKyc().getKycStatus() : "NOT_VERIFIED";

        if (!"VERIFIED".equals(kycStatus)) {
            return CustomerValidationResponse.invalid(cif,
                    "KYC status is " + kycStatus + " (VERIFIED required)");
        }

        return CustomerValidationResponse.valid(cif, customer.getFullName(), kycStatus);
    }

    // ── CIF generation ────────────────────────────────────────────

    private synchronized String generateCif() {
        Integer maxNum = customerRepository.findMaxCifNumber();
        int next = (maxNum == null ? 0 : maxNum) + 1;
        if (next > 9999) {
            throw new BusinessException(ErrorCode.CIF_GENERATION_FAILED,
                    "CIF sequence exceeded 9999");
        }
        return String.format("CIF%04d", next);
    }
}
