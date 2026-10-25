package com.driverhire.customer;

import com.driverhire.common.Patterns;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateCustomerRequest(
        @NotBlank @Size(max = 120) String fullName,
        @NotBlank @Pattern(regexp = Patterns.PHONE, message = Patterns.PHONE_MESSAGE) String phone,
        @NotNull @Valid CustomerProfileInput profile) {
}
