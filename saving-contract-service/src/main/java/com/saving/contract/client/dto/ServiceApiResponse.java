package com.saving.contract.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Generic wrapper matching the ApiResponse<T> structure returned by all internal services.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ServiceApiResponse<T> {
    private boolean success;
    private String  message;
    private T       data;
    private String  error;
    private String  errorCode;
    private String  details;
}
