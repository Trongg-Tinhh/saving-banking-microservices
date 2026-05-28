package com.saving.contract.client;

import com.saving.contract.client.dto.AccountValidationResult;
import com.saving.contract.client.dto.ServiceApiResponse;
import com.saving.contract.exception.BusinessException;
import com.saving.contract.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class AccountServiceClient {

    private final RestTemplate restTemplate;

    @Value("${services.account-url}")
    private String accountServiceUrl;

    /**
     * Validate account: exists, ACTIVE, sufficient balance.
     * Internal endpoint — no auth required.
     */
    public AccountValidationResult validateAccount(String accountNo, BigDecimal requiredAmount) {
        String url = UriComponentsBuilder
                .fromHttpUrl(accountServiceUrl + "/api/v1/accounts/internal/" + accountNo + "/validate")
                .queryParam("requiredAmount", requiredAmount)
                .toUriString();

        log.debug("Calling account-service validate: accountNo={}, required={}", accountNo, requiredAmount);

        try {
            ResponseEntity<ServiceApiResponse<AccountValidationResult>> response = restTemplate.exchange(
                    url, HttpMethod.GET, null,
                    new ParameterizedTypeReference<ServiceApiResponse<AccountValidationResult>>() {});

            ServiceApiResponse<AccountValidationResult> body = response.getBody();
            if (body == null || body.getData() == null) {
                throw new BusinessException(ErrorCode.ACCOUNT_SERVICE_ERROR,
                        "Invalid response for accountNo=" + accountNo);
            }
            return body.getData();
        } catch (RestClientException ex) {
            log.error("account-service unreachable for accountNo={}: {}", accountNo, ex.getMessage());
            throw new BusinessException(ErrorCode.ACCOUNT_SERVICE_UNAVAILABLE,
                    "account-service call failed: " + ex.getMessage());
        }
    }

    /**
     * Debit source account (called with Bearer token to authorize).
     */
    public void debitAccount(String accountNo, BigDecimal amount, String reference,
                             String description, String bearerToken) {
        String url = accountServiceUrl + "/api/v1/accounts/" + accountNo + "/debit";

        Map<String, Object> body = Map.of(
                "amount",      amount,
                "description", description,
                "reference",   reference,
                "useHold",     false
        );

        log.info("Debiting account: accountNo={}, amount={}, ref={}", accountNo, amount, reference);
        executePost(url, body, bearerToken, "debit account=" + accountNo);
    }

    /**
     * Credit receiving account (called with Bearer token).
     */
    public void creditAccount(String accountNo, BigDecimal amount, String reference,
                              String description, String bearerToken) {
        String url = accountServiceUrl + "/api/v1/accounts/" + accountNo + "/credit";

        Map<String, Object> body = Map.of(
                "amount",      amount,
                "description", description,
                "reference",   reference
        );

        log.info("Crediting account: accountNo={}, amount={}, ref={}", accountNo, amount, reference);
        executePost(url, body, bearerToken, "credit account=" + accountNo);
    }

    private void executePost(String url, Object body, String bearerToken, String context) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (bearerToken != null) {
            headers.set("Authorization", "Bearer " + bearerToken);
        }

        HttpEntity<Object> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new BusinessException(ErrorCode.ACCOUNT_SERVICE_ERROR,
                        context + " returned " + response.getStatusCode());
            }
        } catch (HttpClientErrorException ex) {
            log.error("account-service client error [{}]: {} - {}", context, ex.getStatusCode(), ex.getResponseBodyAsString());
            if (ex.getStatusCode() == HttpStatus.UNPROCESSABLE_ENTITY) {
                throw new BusinessException(ErrorCode.INSUFFICIENT_FUNDS,
                        "Insufficient funds for " + context);
            }
            throw new BusinessException(ErrorCode.ACCOUNT_SERVICE_ERROR,
                    context + " failed: " + ex.getResponseBodyAsString());
        } catch (RestClientException ex) {
            log.error("account-service unreachable [{}]: {}", context, ex.getMessage());
            throw new BusinessException(ErrorCode.ACCOUNT_SERVICE_UNAVAILABLE,
                    "account-service unreachable: " + ex.getMessage());
        }
    }
}
