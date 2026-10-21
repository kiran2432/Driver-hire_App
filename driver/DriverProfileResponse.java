package com.driverhire.driver;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** The driver's own full profile (includes private fields such as address and licence number). */
public record DriverProfileResponse(
        Long id,
        String fullName,
        String email,
        String phone,
        String address,
        String city,
        String state,
        int yearsOfExperience,
        String licenseNumber,
        LocalDate licenseExpiry,
        boolean drivesManual,
        boolean drivesAutomatic,
        List<String> languages,
        String bio,
        BigDecimal dailyRate,
        boolean available) {

    public static DriverProfileResponse from(DriverProfile d) {
        return new DriverProfileResponse(
                d.getId(),
                d.getUser().getFullName(),
                d.getUser().getEmail(),
                d.getUser().getPhone(),
                d.getAddress(),
                d.getCity(),
                d.getState(),
                d.getYearsOfExperience(),
                d.getLicenseNumber(),
                d.getLicenseExpiry(),
                d.isDrivesManual(),
                d.isDrivesAutomatic(),
                d.getLanguageList(),
                d.getBio(),
                d.getDailyRate(),
                d.isAvailable());
    }
}
