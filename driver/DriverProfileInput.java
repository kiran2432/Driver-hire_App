package com.driverhire.driver;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Driver details collected at registration and editable later from the profile page.
 */
public record DriverProfileInput(
        @NotBlank @Size(max = 255) String address,
        @NotBlank @Size(max = 100) String city,
        @NotBlank @Size(max = 100) String state,
        @NotNull @Min(0) @Max(60) Integer yearsOfExperience,
        @NotBlank @Size(max = 50) String licenseNumber,
        @NotNull @Future(message = "Driving licence must not be expired") LocalDate licenseExpiry,
        boolean drivesManual,
        boolean drivesAutomatic,
        @Size(max = 10) List<@NotBlank @Size(max = 30) String> languages,
        @Size(max = 1000) String bio,
        @NotNull @DecimalMin("0.00") @DecimalMax("100000.00") @Digits(integer = 8, fraction = 2) BigDecimal dailyRate) {
}
