package com.saving.customer.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateCustomerRequest {

    @NotBlank(message = "Full name is required")
    @Size(max = 200, message = "Full name must not exceed 200 characters")
    private String fullName;

    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    @Pattern(regexp = "^(MALE|FEMALE|OTHER)$", message = "Gender must be MALE, FEMALE, or OTHER")
    private String gender;

    @Size(max = 10)
    private String nationality = "VN";

    @NotBlank(message = "ID number is required")
    @Size(max = 50, message = "ID number must not exceed 50 characters")
    private String idNumber;

    @NotBlank(message = "ID type is required")
    @Pattern(regexp = "^(NATIONAL_ID|PASSPORT|MILITARY_ID|DRIVER_LICENSE)$",
             message = "ID type must be one of: NATIONAL_ID, PASSPORT, MILITARY_ID, DRIVER_LICENSE")
    private String idType;

    // Primary contact (required when creating customer)
    @NotNull(message = "Primary contact is required")
    @Valid
    private CreateContactRequest primaryContact;
}
