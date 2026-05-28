package com.saving.customer.dto.request;

import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateCustomerRequest {

    @Size(max = 200, message = "Full name must not exceed 200 characters")
    private String fullName;

    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    @Pattern(regexp = "^(MALE|FEMALE|OTHER)$", message = "Gender must be MALE, FEMALE, or OTHER")
    private String gender;

    @Size(max = 10)
    private String nationality;

    @Pattern(regexp = "^(ACTIVE|INACTIVE|BLOCKED)$", message = "Status must be ACTIVE, INACTIVE, or BLOCKED")
    private String status;
}
