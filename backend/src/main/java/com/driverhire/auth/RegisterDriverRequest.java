package com.driverhire.auth;

import com.driverhire.common.Patterns;
import com.driverhire.driver.DriverProfileInput;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterDriverRequest(
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Pattern(regexp = Patterns.PASSWORD, message = Patterns.PASSWORD_MESSAGE) String password,
        @NotBlank @Size(max = 120) String fullName,
        @NotBlank @Pattern(regexp = Patterns.PHONE, message = Patterns.PHONE_MESSAGE) String phone,
        @NotNull @Valid DriverProfileInput profile) {
}
