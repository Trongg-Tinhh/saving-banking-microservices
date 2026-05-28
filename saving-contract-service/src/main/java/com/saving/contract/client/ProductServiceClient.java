package com.saving.contract.client;

import com.saving.contract.client.dto.ProductRateResult;
import com.saving.contract.client.dto.ServiceApiResponse;
import com.saving.contract.exception.BusinessException;
import com.saving.contract.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductServiceClient {

    private final RestTemplate restTemplate;

    @Value("${services.product-url}")
    private String productServiceUrl;

    /**
     * Query product rate locked to a specific effective date.
     * Internal endpoint — no auth required.
     */
    public ProductRateResult queryRate(String productCode, String termId, LocalDate effectiveDate) {
        String url = UriComponentsBuilder
                .fromHttpUrl(productServiceUrl
                        + "/api/v1/products/internal/" + productCode
                        + "/terms/" + termId + "/rate")
                .queryParam("effectiveDate", effectiveDate.toString())
                .toUriString();

        log.debug("Calling product-service rate: product={}, term={}, date={}", productCode, termId, effectiveDate);

        try {
            ResponseEntity<ServiceApiResponse<ProductRateResult>> response = restTemplate.exchange(
                    url, HttpMethod.GET, null,
                    new ParameterizedTypeReference<ServiceApiResponse<ProductRateResult>>() {});

            ServiceApiResponse<ProductRateResult> body = response.getBody();
            if (body == null || !body.isSuccess() || body.getData() == null) {
                throw new BusinessException(ErrorCode.PRODUCT_SERVICE_ERROR,
                        "No rate found for product=" + productCode + ", term=" + termId + ", date=" + effectiveDate);
            }
            return body.getData();
        } catch (RestClientException ex) {
            log.error("product-service unreachable for product={}: {}", productCode, ex.getMessage());
            throw new BusinessException(ErrorCode.PRODUCT_SERVICE_UNAVAILABLE,
                    "product-service call failed: " + ex.getMessage());
        }
    }
}
