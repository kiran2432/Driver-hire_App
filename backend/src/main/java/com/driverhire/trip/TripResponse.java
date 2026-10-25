package com.driverhire.trip;

import com.driverhire.driver.DriverProfile;
import com.driverhire.user.User;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Trip as shown to its participants. Phone numbers are only included once the driver has confirmed
 * (CONFIRMED / COMPLETED) so contact details are not handed out for unaccepted requests.
 */
public record TripResponse(
        Long id,
        TripStatus status,
        TripType tripType,
        String pickupCity,
        String pickupLocation,
        String destination,
        Instant startAt,
        Instant returnAt,
        Car car,
        String notes,
        CustomerInfo customer,
        DriverInfo driver,
        Instant createdAt) {

    public record Car(String make, String model, CarType type, Transmission transmission) {
    }

    public record CustomerInfo(String fullName, String phone) {
    }

    public record DriverInfo(
            Long id,
            String fullName,
            String phone,
            String city,
            String state,
            int yearsOfExperience,
            List<String> languages,
            String bio,
            BigDecimal dailyRate) {
    }

    /** Requires customer, driver and driver.user to be loaded (the repository queries guarantee that). */
    public static TripResponse from(Trip t) {
        boolean shareContacts = t.getStatus() == TripStatus.CONFIRMED || t.getStatus() == TripStatus.COMPLETED;

        User c = t.getCustomer();
        CustomerInfo customer = new CustomerInfo(c.getFullName(), shareContacts ? c.getPhone() : null);

        DriverProfile d = t.getDriver();
        DriverInfo driver = d == null ? null : new DriverInfo(
                d.getId(),
                d.getUser().getFullName(),
                shareContacts ? d.getUser().getPhone() : null,
                d.getCity(),
                d.getState(),
                d.getYearsOfExperience(),
                d.getLanguageList(),
                d.getBio(),
                d.getDailyRate());

        return new TripResponse(
                t.getId(),
                t.getStatus(),
                t.getTripType(),
                t.getPickupCity(),
                t.getPickupLocation(),
                t.getDestination(),
                t.getStartAt(),
                t.getReturnAt(),
                new Car(t.getCarMake(), t.getCarModel(), t.getCarType(), t.getTransmission()),
                t.getNotes(),
                customer,
                driver,
                t.getCreatedAt());
    }
}
