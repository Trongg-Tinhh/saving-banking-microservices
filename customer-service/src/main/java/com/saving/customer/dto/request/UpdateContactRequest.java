package com.saving.customer.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateContactRequest {

    @Pattern(regexp = "^(0|\\+84)[0-9]{8,10}$", message = "Invalid Vietnamese phone number")
    private String phoneNumber;

    @Email(message = "Invalid email format")
    @Size(max = 100)
    private String email;

    @Size(max = 500, message = "Address must not exceed 500 characters")
    private String address;

    @Size(max = 100)
    private String district;

    @Size(max = 100)
    private String city;
}
