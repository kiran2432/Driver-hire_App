package com.driverhire.driver;

import java.math.BigDecimal;
import java.util.List;

/**
 * What a customer may see about a driver before hiring: no phone, street address or licence number.
 * Contact details are only shared once the driver confirms a trip (see the trip package).
 */
public record DriverSummary(
        Long id,
        String fullName,
        String city,
        String state,
        int yearsOfExperience,
        boolean drivesManual,
        boolean drivesAutomatic,
        List<String> languages,
        String bio,
        BigDecimal dailyRate) {

    public static DriverSummary from(DriverProfile d) {
        return new DriverSummary(
                d.getId(),
                d.getUser().getFullName(),
                d.getCity(),
                d.getState(),
                d.getYearsOfExperience(),
                d.isDrivesManual(),
                d.isDrivesAutomatic(),
                d.getLanguageList(),
                d.getBio(),
                d.getDailyRate());
    }
}
