package com.saving.product.service;

import com.saving.product.dto.request.*;
import com.saving.product.dto.response.*;
import com.saving.product.entity.*;
import com.saving.product.exception.BusinessException;
import com.saving.product.exception.ErrorCode;
import com.saving.product.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SavingProductService {

    private final SavingProductRepository       productRepository;
    private final SavingTermRepository          termRepository;
    private final InterestRateConfigRepository  rateRepository;
    private final EarlyWithdrawalPolicyRepository policyRepository;

    // ── List Products (all or active-only) ────────────────────────

    @Transactional(readOnly = true)
    public List<SavingProductResponse> listProducts(boolean activeOnly) {
        List<SavingProduct> products = activeOnly
                ? productRepository.findByIsActiveTrueOrderByProductCodeAsc()
                : productRepository.findAllByOrderByProductCodeAsc();

        return products.stream().map(p -> {
            SavingProductResponse resp = SavingProductResponse.from(p);
            // Attach active terms with current rates
            List<SavingTermResponse> terms = termRepository
                    .findByProduct_ProductCodeAndIsActiveTrueOrderByTermMonthsAsc(p.getProductCode())
                    .stream().map(SavingTermResponse::from).collect(Collectors.toList());
            resp.setTerms(terms);
            return resp;
        }).collect(Collectors.toList());
    }

    // ── Get Product Detail ─────────────────────────────────────────

    @Transactional(readOnly = true)
    public SavingProductResponse getProduct(String productCode) {
        SavingProduct product = productRepository.findByProductCodeWithActiveTerms(productCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, "productCode=" + productCode));

        SavingProductResponse resp = SavingProductResponse.from(product);

        // Active terms with current rates
        LocalDate today = LocalDate.now();
        List<SavingTermResponse> terms = termRepository
                .findByProduct_ProductCodeAndIsActiveTrueOrderByTermMonthsAsc(productCode)
                .stream().map(t -> {
                    SavingTermResponse tr = SavingTermResponse.from(t);
                    rateRepository.findEffectiveRate(productCode, t.getTermId(), today)
                            .ifPresent(r -> tr.setAnnualRate(r.getAnnualRate()));
                    return tr;
                }).collect(Collectors.toList());
        resp.setTerms(terms);

        // Current rates
        List<InterestRateResponse> currentRates = rateRepository.findCurrentRates(productCode)
                .stream().map(InterestRateResponse::from).collect(Collectors.toList());
        resp.setCurrentRates(currentRates);

        // Early withdrawal policy
        policyRepository.findByProduct_ProductCode(productCode)
                .ifPresent(p -> resp.setEarlyWithdrawalPolicy(EarlyWithdrawalPolicyResponse.from(p)));

        return resp;
    }

    // ── Create Product ─────────────────────────────────────────────

    @Transactional
    public SavingProductResponse createProduct(CreateProductRequest request) {
        if (productRepository.existsByProductCode(request.getProductCode())) {
            throw new BusinessException(ErrorCode.PRODUCT_ALREADY_EXISTS,
                    "productCode=" + request.getProductCode());
        }

        SavingProduct product = SavingProduct.builder()
                .productCode(request.getProductCode())
                .productName(request.getProductName())
                .currency(request.getCurrency() != null ? request.getCurrency() : "VND")
                .minAmount(request.getMinAmount())
                .maxAmount(request.getMaxAmount())
                .interestPaymentMethod(request.getInterestPaymentMethod())
                .isActive(true)
                .description(request.getDescription())
                .build();

        product = productRepository.save(product);
        log.info("Saving product created: productCode={}", request.getProductCode());
        return SavingProductResponse.from(product);
    }

    // ── Update Product ─────────────────────────────────────────────

    @Transactional
    public SavingProductResponse updateProduct(String productCode, UpdateProductRequest request) {
        SavingProduct product = productRepository.findById(productCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, "productCode=" + productCode));

        if (StringUtils.hasText(request.getProductName()))  product.setProductName(request.getProductName());
        if (request.getMinAmount() != null)                 product.setMinAmount(request.getMinAmount());
        if (request.getMaxAmount() != null)                 product.setMaxAmount(request.getMaxAmount());
        if (request.getIsActive() != null)                  product.setIsActive(request.getIsActive());
        if (request.getDescription() != null)               product.setDescription(request.getDescription());

        product = productRepository.save(product);
        log.info("Saving product updated: productCode={}", productCode);
        return SavingProductResponse.from(product);
    }

    // ── Get Terms ──────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<SavingTermResponse> getTerms(String productCode, boolean activeOnly) {
        ensureProductExists(productCode);
        LocalDate today = LocalDate.now();

        List<SavingTerm> terms = activeOnly
                ? termRepository.findByProduct_ProductCodeAndIsActiveTrueOrderByTermMonthsAsc(productCode)
                : termRepository.findByProduct_ProductCodeOrderByTermMonthsAsc(productCode);

        return terms.stream().map(t -> {
            SavingTermResponse tr = SavingTermResponse.from(t);
            rateRepository.findEffectiveRate(productCode, t.getTermId(), today)
                    .ifPresent(r -> tr.setAnnualRate(r.getAnnualRate()));
            return tr;
        }).collect(Collectors.toList());
    }

    // ── Create Term ────────────────────────────────────────────────

    @Transactional
    public SavingTermResponse createTerm(String productCode, CreateTermRequest request) {
        SavingProduct product = productRepository.findById(productCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, "productCode=" + productCode));

        if (termRepository.existsByTermIdAndProduct_ProductCode(request.getTermId(), productCode)) {
            throw new BusinessException(ErrorCode.TERM_ALREADY_EXISTS, "termId=" + request.getTermId());
        }

        SavingTerm term = SavingTerm.builder()
                .termId(request.getTermId())
                .product(product)
                .termMonths(request.getTermMonths())
                .termDays(request.getTermDays())
                .termLabel(request.getTermLabel())
                .isActive(true)
                .build();

        term = termRepository.save(term);
        log.info("Term created: productCode={}, termId={}", productCode, request.getTermId());
        return SavingTermResponse.from(term);
    }

    // ── Update Term (rename / toggle active) ──────────────────────

    @Transactional
    public SavingTermResponse updateTerm(String productCode, String termId, UpdateTermRequest request) {
        ensureProductExists(productCode);

        SavingTerm term = termRepository.findByTermIdAndProduct_ProductCode(termId, productCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.TERM_NOT_FOUND,
                        "termId=" + termId + " in product=" + productCode));

        if (org.springframework.util.StringUtils.hasText(request.getTermLabel())) {
            term.setTermLabel(request.getTermLabel());
        }
        if (request.getIsActive() != null) {
            term.setIsActive(request.getIsActive());
        }

        term = termRepository.save(term);
        log.info("Term updated: productCode={}, termId={}, isActive={}", productCode, termId, term.getIsActive());
        return SavingTermResponse.from(term);
    }

    // ── Get Rate Configs ───────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<InterestRateResponse> getRateConfigs(String productCode, String termId) {
        ensureProductExists(productCode);
        List<InterestRateConfig> rates = termId != null
                ? rateRepository.findByProduct_ProductCodeAndTerm_TermIdOrderByEffectiveFromDesc(productCode, termId)
                : rateRepository.findByProduct_ProductCodeOrderByEffectiveFromDesc(productCode);
        return rates.stream().map(InterestRateResponse::from).collect(Collectors.toList());
    }

    // ── Add Rate Config ────────────────────────────────────────────

    @Transactional
    public InterestRateResponse addRateConfig(String productCode, CreateRateConfigRequest request) {
        SavingProduct product = productRepository.findById(productCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, "productCode=" + productCode));

        SavingTerm term = termRepository.findByTermIdAndProduct_ProductCode(request.getTermId(), productCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.TERM_NOT_FOUND,
                        "termId=" + request.getTermId() + " in product=" + productCode));

        // Validate dates
        if (request.getEffectiveTo() != null && !request.getEffectiveTo().isAfter(request.getEffectiveFrom())) {
            throw new BusinessException(ErrorCode.RATE_DATE_INVALID,
                    "effectiveTo must be after effectiveFrom");
        }

        // Prevent duplicate: same term + same effectiveFrom → ambiguous rate lookup
        if (rateRepository.existsByProduct_ProductCodeAndTerm_TermIdAndEffectiveFrom(
                productCode, request.getTermId(), request.getEffectiveFrom())) {
            throw new BusinessException(ErrorCode.RATE_DATE_CONFLICT,
                    "A rate for termId=" + request.getTermId()
                    + " already starts on " + request.getEffectiveFrom()
                    + ". Add a new rate with a different effectiveFrom date.");
        }

        InterestRateConfig config = InterestRateConfig.builder()
                .product(product)
                .term(term)
                .annualRate(request.getAnnualRate())
                .effectiveFrom(request.getEffectiveFrom())
                .effectiveTo(request.getEffectiveTo())
                .isActive(true)
                .build();

        config = rateRepository.save(config);
        log.info("Rate config added: productCode={}, termId={}, rate={}%, from={}, to={}",
                productCode, request.getTermId(), request.getAnnualRate(),
                request.getEffectiveFrom(), request.getEffectiveTo());
        return InterestRateResponse.from(config);
    }

    // ── Get Early Withdrawal Policy ────────────────────────────────

    @Transactional(readOnly = true)
    public EarlyWithdrawalPolicyResponse getEarlyWithdrawalPolicy(String productCode) {
        ensureProductExists(productCode);
        EarlyWithdrawalPolicy policy = policyRepository.findByProduct_ProductCode(productCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.POLICY_NOT_FOUND, "productCode=" + productCode));
        return EarlyWithdrawalPolicyResponse.from(policy);
    }

    // ── Upsert Early Withdrawal Policy ────────────────────────────

    @Transactional
    public EarlyWithdrawalPolicyResponse upsertEarlyWithdrawalPolicy(
            String productCode, UpsertEarlyWithdrawalPolicyRequest request) {

        SavingProduct product = productRepository.findById(productCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND,
                        "productCode=" + productCode));

        // Find existing policy or create a new one (upsert)
        EarlyWithdrawalPolicy policy = policyRepository.findByProduct_ProductCode(productCode)
                .orElse(EarlyWithdrawalPolicy.builder().product(product).build());

        policy.setMinDaysHeld(request.getMinDaysHeld());
        policy.setPenaltyRate(request.getPenaltyRate());
        policy.setUseDemandRate(request.getUseDemandRate());
        policy.setDemandRate(request.getDemandRate());

        policy = policyRepository.save(policy);
        log.info("Early withdrawal policy upserted: productCode={}", productCode);
        return EarlyWithdrawalPolicyResponse.from(policy);
    }

    // ── Internal: Validate Amount ──────────────────────────────────

    @Transactional(readOnly = true)
    public void validateAmount(SavingProduct product, BigDecimal amount) {
        if (product.getMinAmount() != null && amount.compareTo(product.getMinAmount()) < 0) {
            throw new BusinessException(ErrorCode.AMOUNT_BELOW_MINIMUM,
                    "minimum=" + product.getMinAmount() + ", requested=" + amount);
        }
        if (product.getMaxAmount() != null && amount.compareTo(product.getMaxAmount()) > 0) {
            throw new BusinessException(ErrorCode.AMOUNT_ABOVE_MAXIMUM,
                    "maximum=" + product.getMaxAmount() + ", requested=" + amount);
        }
    }

    // ── Internal: Rate Query for Saving Contract Service ──────────
    // GET /api/v1/products/internal/{productCode}/terms/{termId}/rate?effectiveDate=YYYY-MM-DD

    @Transactional(readOnly = true)
    public ProductRateQueryResponse queryProductRate(String productCode, String termId,
                                                     LocalDate effectiveDate) {
        // Resolve to today if not provided
        LocalDate resolvedDate = effectiveDate != null ? effectiveDate : LocalDate.now();

        SavingProduct product = productRepository.findById(productCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, "productCode=" + productCode));

        if (!Boolean.TRUE.equals(product.getIsActive())) {
            throw new BusinessException(ErrorCode.PRODUCT_INACTIVE, "productCode=" + productCode);
        }

        SavingTerm term = termRepository.findByTermIdAndProduct_ProductCode(termId, productCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.TERM_NOT_FOUND,
                        "termId=" + termId + " in product=" + productCode));

        if (!Boolean.TRUE.equals(term.getIsActive())) {
            throw new BusinessException(ErrorCode.TERM_INACTIVE, "termId=" + termId);
        }

        InterestRateConfig rate = rateRepository.findEffectiveRate(productCode, termId, resolvedDate)
                .orElseThrow(() -> new BusinessException(ErrorCode.RATE_NOT_FOUND,
                        "No rate for product=" + productCode + ", term=" + termId + ", date=" + resolvedDate));

        // Validate amount if provided
        // (amount validation happens in saving-contract-service, but product limits are checked here)

        // Early withdrawal policy
        EarlyWithdrawalPolicy policy = policyRepository.findByProduct_ProductCode(productCode).orElse(null);

        ProductRateQueryResponse.ProductRateQueryResponseBuilder builder = ProductRateQueryResponse.builder()
                .productCode(product.getProductCode())
                .productName(product.getProductName())
                .interestPaymentMethod(product.getInterestPaymentMethod())
                .currency(product.getCurrency())
                .minAmount(product.getMinAmount())
                .maxAmount(product.getMaxAmount())
                .termId(term.getTermId())
                .termLabel(term.getTermLabel())
                .termMonths(term.getTermMonths())
                .termDays(term.getTermDays())
                .annualRate(rate.getAnnualRate())
                .rateEffectiveFrom(rate.getEffectiveFrom())
                .rateEffectiveTo(rate.getEffectiveTo());

        if (policy != null) {
            builder.earlyWithdrawalDemandRate(policy.getDemandRate())
                   .earlyWithdrawalUseDemandRate(policy.getUseDemandRate())
                   .earlyWithdrawalMinDaysHeld(policy.getMinDaysHeld());
        }

        return builder.build();
    }

    // ── Private helpers ────────────────────────────────────────────

    private void ensureProductExists(String productCode) {
        if (!productRepository.existsByProductCode(productCode)) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, "productCode=" + productCode);
        }
    }
}
