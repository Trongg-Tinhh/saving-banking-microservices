package com.saving.contract.client;

import com.saving.contract.client.dto.CustomerValidationResult;
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

@Component
@RequiredArgsConstructor
@Slf4j
public class CustomerServiceClient {

    private final RestTemplate restTemplate;

    @Value("${services.customer-url}")
    private String customerServiceUrl;

    /**
     * Validate CIF: check customer exists, ACTIVE status, KYC VERIFIED.
     */
    public CustomerValidationResult validateCustomer(String cif) {
        String url = customerServiceUrl + "/api/v1/customers/internal/" + cif + "/validate";
        log.debug("Calling customer-service validate: cif={}", cif);

        try {
            ResponseEntity<ServiceApiResponse<CustomerValidationResult>> response = restTemplate.exchange(
                    url, HttpMethod.GET, null,
                    new ParameterizedTypeReference<ServiceApiResponse<CustomerValidationResult>>() {});

            ServiceApiResponse<CustomerValidationResult> body = response.getBody();
            if (body == null || !body.isSuccess() || body.getData() == null) {
                throw new BusinessException(ErrorCode.CUSTOMER_SERVICE_ERROR,
                        "Invalid response from customer-service for cif=" + cif);
            }
            return body.getData();
        } catch (RestClientException ex) {
            log.error("customer-service unreachable for cif={}: {}", cif, ex.getMessage());
            throw new BusinessException(ErrorCode.CUSTOMER_SERVICE_UNAVAILABLE,
                    "customer-service call failed: " + ex.getMessage());
        }
    }
}
