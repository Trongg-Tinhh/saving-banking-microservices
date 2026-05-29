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
        log.info("[LIST_PRODUCTS] activeOnly={}", activeOnly);

        List<SavingProduct> products = activeOnly
                ? productRepository.findByIsActiveTrueOrderByProductCodeAsc()
                : productRepository.findAllByOrderByProductCodeAsc();

        log.info("[LIST_PRODUCTS] found {} product(s)", products.size());

        return products.stream().map(p -> {
            SavingProductResponse resp = SavingProductResponse.from(p);
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
        log.info("[GET_PRODUCT] productCode={}", productCode);

        SavingProduct product = productRepository.findByProductCodeWithActiveTerms(productCode)
                .orElseThrow(() -> {
                    log.warn("[GET_PRODUCT] NOT FOUND productCode={}", productCode);
                    return new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, "productCode=" + productCode);
                });

        SavingProductResponse resp = SavingProductResponse.from(product);

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

        List<InterestRateResponse> currentRates = rateRepository.findCurrentRates(productCode)
                .stream().map(InterestRateResponse::from).collect(Collectors.toList());
        resp.setCurrentRates(currentRates);

        policyRepository.findByProduct_ProductCode(productCode)
                .ifPresent(p -> resp.setEarlyWithdrawalPolicy(EarlyWithdrawalPolicyResponse.from(p)));

        log.info("[GET_PRODUCT] OK productCode={} name='{}' terms={} rates={}",
                productCode, product.getProductName(), terms.size(), currentRates.size());
        return resp;
    }

    // ── Create Product ─────────────────────────────────────────────

    @Transactional
    public SavingProductResponse createProduct(CreateProductRequest request) {
        log.info("[CREATE_PRODUCT] Step 1/3 — checking uniqueness: productCode='{}'", request.getProductCode());

        if (productRepository.existsByProductCode(request.getProductCode())) {
            log.warn("[CREATE_PRODUCT] FAILED — productCode='{}' already exists", request.getProductCode());
            throw new BusinessException(ErrorCode.PRODUCT_ALREADY_EXISTS,
                    "productCode=" + request.getProductCode());
        }

        log.info("[CREATE_PRODUCT] Step 2/3 — building product: name='{}' currency={} ipm={}",
                request.getProductName(), request.getCurrency(), request.getInterestPaymentMethod());

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
        log.info("[CREATE_PRODUCT] Step 3/3 — SUCCESS: productCode='{}' id={}",
                product.getProductCode(), product.getProductCode());
        return SavingProductResponse.from(product);
    }

    // ── Update Product ─────────────────────────────────────────────

    @Transactional
    public SavingProductResponse updateProduct(String productCode, UpdateProductRequest request) {
        log.info("[UPDATE_PRODUCT] Step 1/2 — loading productCode='{}'", productCode);

        SavingProduct product = productRepository.findById(productCode)
                .orElseThrow(() -> {
                    log.warn("[UPDATE_PRODUCT] FAILED — productCode='{}' not found", productCode);
                    return new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, "productCode=" + productCode);
                });

        // Apply partial updates and log what changed
        if (StringUtils.hasText(request.getProductName())) {
            log.info("[UPDATE_PRODUCT] name: '{}' → '{}'", product.getProductName(), request.getProductName());
            product.setProductName(request.getProductName());
        }
        if (request.getMinAmount() != null) {
            log.info("[UPDATE_PRODUCT] minAmount: {} → {}", product.getMinAmount(), request.getMinAmount());
            product.setMinAmount(request.getMinAmount());
        }
        if (request.getMaxAmount() != null) {
            log.info("[UPDATE_PRODUCT] maxAmount: {} → {}", product.getMaxAmount(), request.getMaxAmount());
            product.setMaxAmount(request.getMaxAmount());
        }
        if (request.getIsActive() != null) {
            log.info("[UPDATE_PRODUCT] isActive: {} → {}", product.getIsActive(), request.getIsActive());
            product.setIsActive(request.getIsActive());
        }
        if (request.getDescription() != null) product.setDescription(request.getDescription());

        product = productRepository.save(product);
        log.info("[UPDATE_PRODUCT] Step 2/2 — SUCCESS: productCode='{}'", productCode);
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
        log.info("[CREATE_TERM] Step 1/3 — productCode='{}' termId='{}' months={} days={}",
                productCode, request.getTermId(), request.getTermMonths(), request.getTermDays());

        SavingProduct product = productRepository.findById(productCode)
                .orElseThrow(() -> {
                    log.warn("[CREATE_TERM] FAILED — productCode='{}' not found", productCode);
                    return new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, "productCode=" + productCode);
                });

        if (termRepository.existsByTermIdAndProduct_ProductCode(request.getTermId(), productCode)) {
            log.warn("[CREATE_TERM] FAILED — termId='{}' already exists in productCode='{}'",
                    request.getTermId(), productCode);
            throw new BusinessException(ErrorCode.TERM_ALREADY_EXISTS, "termId=" + request.getTermId());
        }

        log.info("[CREATE_TERM] Step 2/3 — persisting term '{}'", request.getTermLabel());
        SavingTerm term = SavingTerm.builder()
                .termId(request.getTermId())
                .product(product)
                .termMonths(request.getTermMonths())
                .termDays(request.getTermDays())
                .termLabel(request.getTermLabel())
                .isActive(true)
                .build();

        term = termRepository.save(term);
        log.info("[CREATE_TERM] Step 3/3 — SUCCESS: productCode='{}' termId='{}'", productCode, term.getTermId());
        return SavingTermResponse.from(term);
    }

    // ── Update Term (rename / toggle active) ──────────────────────

    @Transactional
    public SavingTermResponse updateTerm(String productCode, String termId, UpdateTermRequest request) {
        log.info("[UPDATE_TERM] productCode='{}' termId='{}'", productCode, termId);
        ensureProductExists(productCode);

        SavingTerm term = termRepository.findByTermIdAndProduct_ProductCode(termId, productCode)
                .orElseThrow(() -> {
                    log.warn("[UPDATE_TERM] FAILED — termId='{}' not found in productCode='{}'", termId, productCode);
                    return new BusinessException(ErrorCode.TERM_NOT_FOUND,
                            "termId=" + termId + " in product=" + productCode);
                });

        if (org.springframework.util.StringUtils.hasText(request.getTermLabel())) {
            log.info("[UPDATE_TERM] label: '{}' → '{}'", term.getTermLabel(), request.getTermLabel());
            term.setTermLabel(request.getTermLabel());
        }
        if (request.getIsActive() != null) {
            log.info("[UPDATE_TERM] isActive: {} → {}", term.getIsActive(), request.getIsActive());
            term.setIsActive(request.getIsActive());
        }

        term = termRepository.save(term);
        log.info("[UPDATE_TERM] SUCCESS: productCode='{}' termId='{}' isActive={}", productCode, termId, term.getIsActive());
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
        log.info("[ADD_RATE] Step 1/3 — productCode='{}' termId='{}' rate={}% from={} to={}",
                productCode, request.getTermId(), request.getAnnualRate(),
                request.getEffectiveFrom(), request.getEffectiveTo());

        SavingProduct product = productRepository.findById(productCode)
                .orElseThrow(() -> {
                    log.warn("[ADD_RATE] FAILED — productCode='{}' not found", productCode);
                    return new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, "productCode=" + productCode);
                });

        SavingTerm term = termRepository.findByTermIdAndProduct_ProductCode(request.getTermId(), productCode)
                .orElseThrow(() -> {
                    log.warn("[ADD_RATE] FAILED — termId='{}' not found in productCode='{}'",
                            request.getTermId(), productCode);
                    return new BusinessException(ErrorCode.TERM_NOT_FOUND,
                            "termId=" + request.getTermId() + " in product=" + productCode);
                });

        log.info("[ADD_RATE] Step 2/3 — validating date range and duplicate check");

        if (request.getEffectiveTo() != null && !request.getEffectiveTo().isAfter(request.getEffectiveFrom())) {
            log.warn("[ADD_RATE] FAILED — effectiveTo {} is not after effectiveFrom {}",
                    request.getEffectiveTo(), request.getEffectiveFrom());
            throw new BusinessException(ErrorCode.RATE_DATE_INVALID, "effectiveTo must be after effectiveFrom");
        }

        if (rateRepository.existsByProduct_ProductCodeAndTerm_TermIdAndEffectiveFrom(
                productCode, request.getTermId(), request.getEffectiveFrom())) {
            log.warn("[ADD_RATE] FAILED — duplicate rate for termId='{}' on effectiveFrom={}",
                    request.getTermId(), request.getEffectiveFrom());
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
        log.info("[ADD_RATE] Step 3/3 — SUCCESS: productCode='{}' termId='{}' rate={}% from={} to={}",
                productCode, request.getTermId(), config.getAnnualRate(),
                config.getEffectiveFrom(), config.getEffectiveTo());
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

        log.info("[UPSERT_POLICY] productCode='{}' minDays={} penaltyRate={}% useDemandRate={}",
                productCode, request.getMinDaysHeld(), request.getPenaltyRate(), request.getUseDemandRate());

        SavingProduct product = productRepository.findById(productCode)
                .orElseThrow(() -> {
                    log.warn("[UPSERT_POLICY] FAILED — productCode='{}' not found", productCode);
                    return new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, "productCode=" + productCode);
                });

        boolean isNew = policyRepository.findByProduct_ProductCode(productCode).isEmpty();
        EarlyWithdrawalPolicy policy = policyRepository.findByProduct_ProductCode(productCode)
                .orElse(EarlyWithdrawalPolicy.builder().product(product).build());

        policy.setMinDaysHeld(request.getMinDaysHeld());
        policy.setPenaltyRate(request.getPenaltyRate());
        policy.setUseDemandRate(request.getUseDemandRate());
        policy.setDemandRate(request.getDemandRate());

        policy = policyRepository.save(policy);
        log.info("[UPSERT_POLICY] SUCCESS ({}) productCode='{}' penaltyRate={}%",
                isNew ? "created" : "updated", productCode, policy.getPenaltyRate());
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
        LocalDate resolvedDate = effectiveDate != null ? effectiveDate : LocalDate.now();
        log.info("[INTERNAL_RATE] productCode='{}' termId='{}' effectiveDate={}", productCode, termId, resolvedDate);

        SavingProduct product = productRepository.findById(productCode)
                .orElseThrow(() -> {
                    log.warn("[INTERNAL_RATE] FAILED — productCode='{}' not found", productCode);
                    return new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, "productCode=" + productCode);
                });

        if (!Boolean.TRUE.equals(product.getIsActive())) {
            log.warn("[INTERNAL_RATE] FAILED — productCode='{}' is inactive", productCode);
            throw new BusinessException(ErrorCode.PRODUCT_INACTIVE, "productCode=" + productCode);
        }

        SavingTerm term = termRepository.findByTermIdAndProduct_ProductCode(termId, productCode)
                .orElseThrow(() -> {
                    log.warn("[INTERNAL_RATE] FAILED — termId='{}' not found in productCode='{}'", termId, productCode);
                    return new BusinessException(ErrorCode.TERM_NOT_FOUND,
                            "termId=" + termId + " in product=" + productCode);
                });

        if (!Boolean.TRUE.equals(term.getIsActive())) {
            log.warn("[INTERNAL_RATE] FAILED — termId='{}' is inactive", termId);
            throw new BusinessException(ErrorCode.TERM_INACTIVE, "termId=" + termId);
        }

        InterestRateConfig rate = rateRepository.findEffectiveRate(productCode, termId, resolvedDate)
                .orElseThrow(() -> {
                    log.warn("[INTERNAL_RATE] FAILED — no effective rate for productCode='{}' termId='{}' date={}",
                            productCode, termId, resolvedDate);
                    return new BusinessException(ErrorCode.RATE_NOT_FOUND,
                            "No rate for product=" + productCode + ", term=" + termId + ", date=" + resolvedDate);
                });

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

        ProductRateQueryResponse resp = builder.build();
        log.info("[INTERNAL_RATE] OK productCode='{}' termId='{}' rate={}% from={} to={}",
                productCode, termId, rate.getAnnualRate(), rate.getEffectiveFrom(), rate.getEffectiveTo());
        return resp;
    }

    // ── Private helpers ────────────────────────────────────────────

    private void ensureProductExists(String productCode) {
        if (!productRepository.existsByProductCode(productCode)) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, "productCode=" + productCode);
        }
    }
}
