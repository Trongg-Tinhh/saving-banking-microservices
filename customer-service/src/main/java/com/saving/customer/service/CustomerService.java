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
        log.info("[CREATE_CUSTOMER] Step 1/5 — check duplicate ID number: idNumber={}, idType={}",
                request.getIdNumber(), request.getIdType());
        if (customerRepository.existsByIdNumber(request.getIdNumber())) {
            log.warn("[CREATE_CUSTOMER] FAILED — ID number already exists: idNumber={}", request.getIdNumber());
            throw new BusinessException(ErrorCode.CUSTOMER_ALREADY_EXISTS,
                    "ID number: " + request.getIdNumber());
        }

        log.info("[CREATE_CUSTOMER] Step 2/5 — generate CIF");
        String cif = generateCif();

        log.info("[CREATE_CUSTOMER] Step 3/5 — build & persist customer: cif={}, name={}, nationality={}",
                cif, request.getFullName(),
                request.getNationality() != null ? request.getNationality() : "VN");
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

        log.info("[CREATE_CUSTOMER] Step 4/5 — create KYC record (NOT_VERIFIED){}: cif={}",
                request.getPrimaryContact() != null ? " + primary contact" : "", cif);
        CustomerKyc kyc = CustomerKyc.builder()
                .customer(customer)
                .kycStatus("NOT_VERIFIED")
                .build();
        customerKycRepository.save(kyc);
        customer.setKyc(kyc);

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

        log.info("[CREATE_CUSTOMER] Step 5/5 — publish customer.created event: cif={}", cif);
        eventPublisher.publishCustomerCreated(cif, request.getFullName());

        log.info("[CREATE_CUSTOMER] SUCCESS — cif={}, name={}", cif, request.getFullName());
        return CustomerResponse.from(customer);
    }

    // ── Get Customer by CIF ────────────────────────────────────────

    @Transactional(readOnly = true)
    public CustomerResponse getCustomerByCif(String cif) {
        log.info("[GET_CUSTOMER] lookup: cif={}", cif);
        Customer customer = customerRepository.findByCifWithKyc(cif)
                .orElseThrow(() -> {
                    log.warn("[GET_CUSTOMER] NOT FOUND: cif={}", cif);
                    return new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND, "cif=" + cif);
                });

        List<CustomerContact> contacts = contactRepository
                .findByCustomer_CifOrderByIsPrimaryDescCreatedAtAsc(cif);
        customer.setContacts(contacts);

        String kycStatus = customer.getKyc() != null ? customer.getKyc().getKycStatus() : "N/A";
        log.info("[GET_CUSTOMER] OK: cif={}, name={}, status={}, kyc={}, contacts={}",
                cif, customer.getFullName(), customer.getStatus(), kycStatus, contacts.size());
        return CustomerResponse.fromWithContacts(customer);
    }

    // ── Search Customers ───────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<CustomerSummaryResponse> searchCustomers(
            String fullName, String idNumber, String phone,
            String email,    String status,   Pageable pageable) {

        boolean byContact = StringUtils.hasText(phone) || StringUtils.hasText(email);
        log.info("[SEARCH_CUSTOMERS] mode={}, fullName={}, idNumber={}, phone={}, email={}, status={}, page={}/size={}",
                byContact ? "contact" : "profile",
                fullName, idNumber, phone, email, status,
                pageable.getPageNumber(), pageable.getPageSize());

        Page<CustomerSummaryResponse> result;
        if (byContact) {
            result = customerRepository.searchByContact(
                    StringUtils.hasText(phone) ? phone : null,
                    StringUtils.hasText(email) ? email : null,
                    pageable
            ).map(CustomerSummaryResponse::from);
        } else {
            result = customerRepository.searchCustomers(
                    StringUtils.hasText(fullName) ? fullName : null,
                    StringUtils.hasText(idNumber) ? idNumber : null,
                    StringUtils.hasText(status)   ? status   : null,
                    pageable
            ).map(CustomerSummaryResponse::from);
        }

        log.info("[SEARCH_CUSTOMERS] OK: found={}, totalPages={}", result.getTotalElements(), result.getTotalPages());
        return result;
    }

    // ── Update Customer ────────────────────────────────────────────

    @Transactional
    public CustomerResponse updateCustomer(String cif, UpdateCustomerRequest request, boolean isStaff) {
        log.info("[UPDATE_CUSTOMER] Step 1/3 — load customer: cif={}, byStaff={}", cif, isStaff);
        Customer customer = customerRepository.findByCifWithKyc(cif)
                .orElseThrow(() -> {
                    log.warn("[UPDATE_CUSTOMER] NOT FOUND: cif={}", cif);
                    return new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND, "cif=" + cif);
                });

        log.info("[UPDATE_CUSTOMER] Step 2/3 — apply changes: cif={}", cif);
        if (StringUtils.hasText(request.getFullName())) {
            log.info("[UPDATE_CUSTOMER]   fullName: {} → {}", customer.getFullName(), request.getFullName());
            customer.setFullName(request.getFullName());
        }
        if (request.getDateOfBirth() != null) {
            log.info("[UPDATE_CUSTOMER]   dateOfBirth: {} → {}", customer.getDateOfBirth(), request.getDateOfBirth());
            customer.setDateOfBirth(request.getDateOfBirth());
        }
        if (StringUtils.hasText(request.getGender())) {
            log.info("[UPDATE_CUSTOMER]   gender: {} → {}", customer.getGender(), request.getGender());
            customer.setGender(request.getGender());
        }
        if (StringUtils.hasText(request.getNationality())) {
            log.info("[UPDATE_CUSTOMER]   nationality: {} → {}", customer.getNationality(), request.getNationality());
            customer.setNationality(request.getNationality());
        }
        if (isStaff && StringUtils.hasText(request.getStatus())) {
            log.info("[UPDATE_CUSTOMER]   status: {} → {} (staff override)", customer.getStatus(), request.getStatus());
            customer.setStatus(request.getStatus());
        }

        log.info("[UPDATE_CUSTOMER] Step 3/3 — persist & publish event: cif={}", cif);
        customer = customerRepository.save(customer);
        eventPublisher.publishCustomerUpdated(cif, customer.getFullName());

        log.info("[UPDATE_CUSTOMER] SUCCESS — cif={}, byStaff={}", cif, isStaff);
        return CustomerResponse.from(customer);
    }

    // ── KYC Management ────────────────────────────────────────────

    @Transactional(readOnly = true)
    public CustomerKycResponse getKyc(String cif) {
        log.info("[GET_KYC] lookup: cif={}", cif);
        if (!customerRepository.existsById(cif)) {
            log.warn("[GET_KYC] customer NOT FOUND: cif={}", cif);
            throw new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND, "cif=" + cif);
        }

        CustomerKyc kyc = customerKycRepository.findByCustomer_Cif(cif)
                .orElseThrow(() -> {
                    log.warn("[GET_KYC] KYC record NOT FOUND: cif={}", cif);
                    return new BusinessException(ErrorCode.KYC_NOT_FOUND, "cif=" + cif);
                });

        log.info("[GET_KYC] OK: cif={}, kycStatus={}", cif, kyc.getKycStatus());
        return CustomerKycResponse.from(kyc);
    }

    @Transactional
    public CustomerKycResponse updateKycStatus(String cif, UpdateKycStatusRequest request,
                                               String actingUsername) {
        log.info("[UPDATE_KYC] Step 1/3 — load customer & KYC: cif={}, requestedStatus={}, by={}",
                cif, request.getKycStatus(), actingUsername);
        if (!customerRepository.existsById(cif)) {
            log.warn("[UPDATE_KYC] customer NOT FOUND: cif={}", cif);
            throw new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND, "cif=" + cif);
        }

        CustomerKyc kyc = customerKycRepository.findByCustomer_Cif(cif)
                .orElseThrow(() -> {
                    log.warn("[UPDATE_KYC] KYC record NOT FOUND: cif={}", cif);
                    return new BusinessException(ErrorCode.KYC_NOT_FOUND, "cif=" + cif);
                });

        String oldStatus = kyc.getKycStatus();

        log.info("[UPDATE_KYC] Step 2/3 — validate business rules: cif={}, {}→{}",
                cif, oldStatus, request.getKycStatus());
        if ("REJECTED".equals(request.getKycStatus()) && !StringUtils.hasText(request.getRejectionReason())) {
            log.warn("[UPDATE_KYC] FAILED — rejection reason required: cif={}", cif);
            throw new BusinessException(ErrorCode.KYC_REJECTION_REASON_REQUIRED);
        }

        OffsetDateTime verifiedAt = "VERIFIED".equals(request.getKycStatus()) ? OffsetDateTime.now() : null;
        String verifiedBy = StringUtils.hasText(request.getVerifiedBy())
                ? request.getVerifiedBy()
                : actingUsername;
        String rejectionReason = "REJECTED".equals(request.getKycStatus())
                ? request.getRejectionReason()
                : null;

        kyc.setKycStatus(request.getKycStatus());
        kyc.setVerifiedAt(verifiedAt);
        kyc.setVerifiedBy(verifiedBy);
        kyc.setRejectionReason(rejectionReason);
        if (StringUtils.hasText(request.getDocType())) kyc.setDocType(request.getDocType());
        if (StringUtils.hasText(request.getDocUrl()))  kyc.setDocUrl(request.getDocUrl());

        log.info("[UPDATE_KYC] Step 3/3 — persist & publish event: cif={}", cif);
        kyc = customerKycRepository.save(kyc);
        eventPublisher.publishKycStatusChanged(cif, oldStatus, request.getKycStatus(), verifiedBy);

        log.info("[UPDATE_KYC] SUCCESS — cif={}, {}→{}, verifiedBy={}", cif, oldStatus, request.getKycStatus(), verifiedBy);
        return CustomerKycResponse.from(kyc);
    }

    // ── Contact Management ────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<CustomerContactResponse> getContacts(String cif) {
        log.info("[GET_CONTACTS] lookup: cif={}", cif);
        if (!customerRepository.existsById(cif)) {
            log.warn("[GET_CONTACTS] customer NOT FOUND: cif={}", cif);
            throw new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND, "cif=" + cif);
        }

        List<CustomerContactResponse> contacts = contactRepository
                .findByCustomer_CifOrderByIsPrimaryDescCreatedAtAsc(cif)
                .stream()
                .map(CustomerContactResponse::from)
                .collect(Collectors.toList());

        log.info("[GET_CONTACTS] OK: cif={}, count={}", cif, contacts.size());
        return contacts;
    }

    @Transactional
    public CustomerContactResponse addContact(String cif, CreateContactRequest request) {
        log.info("[ADD_CONTACT] Step 1/3 — load customer & check limit: cif={}, isPrimary={}",
                cif, request.getIsPrimary());
        Customer customer = customerRepository.findById(cif)
                .orElseThrow(() -> {
                    log.warn("[ADD_CONTACT] customer NOT FOUND: cif={}", cif);
                    return new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND, "cif=" + cif);
                });

        long count = contactRepository.countByCustomer_Cif(cif);
        if (count >= 5) {
            log.warn("[ADD_CONTACT] FAILED — contact limit reached: cif={}, existing={}", cif, count);
            throw new BusinessException(ErrorCode.CONTACT_LIMIT_REACHED);
        }

        log.info("[ADD_CONTACT] Step 2/3 — handle primary flag: cif={}, isPrimary={}, existingCount={}",
                cif, request.getIsPrimary(), count);
        if (Boolean.TRUE.equals(request.getIsPrimary())) {
            contactRepository.unsetPrimaryContacts(cif);
        }

        log.info("[ADD_CONTACT] Step 3/3 — persist contact: cif={}", cif);
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
        log.info("[ADD_CONTACT] SUCCESS — cif={}, contactId={}, isPrimary={}",
                cif, contact.getContactId(), contact.getIsPrimary());
        return CustomerContactResponse.from(contact);
    }

    @Transactional
    public CustomerContactResponse updateContact(String cif, UUID contactId,
                                                 UpdateContactRequest request) {
        log.info("[UPDATE_CONTACT] Step 1/3 — verify customer & load contact: cif={}, contactId={}", cif, contactId);
        if (!customerRepository.existsById(cif)) {
            log.warn("[UPDATE_CONTACT] customer NOT FOUND: cif={}", cif);
            throw new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND, "cif=" + cif);
        }

        CustomerContact contact = contactRepository.findById(contactId)
                .orElseThrow(() -> {
                    log.warn("[UPDATE_CONTACT] contact NOT FOUND: cif={}, contactId={}", cif, contactId);
                    return new BusinessException(ErrorCode.CONTACT_NOT_FOUND, "contactId=" + contactId);
                });

        log.info("[UPDATE_CONTACT] Step 2/3 — validate ownership: cif={}, contactId={}", cif, contactId);
        if (!cif.equals(contact.getCustomer().getCif())) {
            log.warn("[UPDATE_CONTACT] FAILED — contact does not belong to cif: cif={}, contactId={}, ownerCif={}",
                    cif, contactId, contact.getCustomer().getCif());
            throw new BusinessException(ErrorCode.CONTACT_NOT_FOUND,
                    "Contact does not belong to cif=" + cif);
        }

        log.info("[UPDATE_CONTACT] Step 3/3 — apply changes & persist: cif={}, contactId={}", cif, contactId);
        if (StringUtils.hasText(request.getPhoneNumber())) contact.setPhoneNumber(request.getPhoneNumber());
        if (StringUtils.hasText(request.getEmail()))       contact.setEmail(request.getEmail());
        if (StringUtils.hasText(request.getAddress()))     contact.setAddress(request.getAddress());
        if (StringUtils.hasText(request.getDistrict()))    contact.setDistrict(request.getDistrict());
        if (StringUtils.hasText(request.getCity()))        contact.setCity(request.getCity());

        contact = contactRepository.save(contact);
        log.info("[UPDATE_CONTACT] SUCCESS — cif={}, contactId={}", cif, contactId);
        return CustomerContactResponse.from(contact);
    }

    // ── Internal: validate CIF for other services ─────────────────

    @Transactional(readOnly = true)
    public CustomerValidationResponse validateCustomer(String cif) {
        log.info("[VALIDATE_CUSTOMER] check: cif={}", cif);
        Customer customer = customerRepository.findByCifWithKyc(cif).orElse(null);

        if (customer == null) {
            log.warn("[VALIDATE_CUSTOMER] INVALID — not found: cif={}", cif);
            return CustomerValidationResponse.invalid(cif, "Customer not found");
        }
        if (!"ACTIVE".equals(customer.getStatus())) {
            log.warn("[VALIDATE_CUSTOMER] INVALID — status not active: cif={}, status={}",
                    cif, customer.getStatus());
            return CustomerValidationResponse.invalid(cif, "Customer status is " + customer.getStatus());
        }

        String kycStatus = customer.getKyc() != null ? customer.getKyc().getKycStatus() : "NOT_VERIFIED";
        if (!"VERIFIED".equals(kycStatus)) {
            log.warn("[VALIDATE_CUSTOMER] INVALID — KYC not verified: cif={}, kycStatus={}", cif, kycStatus);
            return CustomerValidationResponse.invalid(cif, "KYC status is " + kycStatus + " (VERIFIED required)");
        }

        log.info("[VALIDATE_CUSTOMER] VALID — cif={}, name={}, kycStatus={}", cif, customer.getFullName(), kycStatus);
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
