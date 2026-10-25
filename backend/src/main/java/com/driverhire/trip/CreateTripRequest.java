package com.driverhire.trip;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/**
 * Trip and car details a customer enters. Instants are ISO-8601 (the browser sends UTC).
 * returnAt is required for ROUND_TRIP and ignored for ONE_WAY.
 */
public record CreateTripRequest(
        @NotBlank @Size(max = 100) String pickupCity,
        @NotBlank @Size(max = 255) String pickupLocation,
        @NotBlank @Size(max = 255) String destination,
        @NotNull TripType tripType,
        @NotNull @Future(message = "Start time must be in the future") Instant startAt,
        Instant returnAt,
        @NotBlank @Size(max = 60) String carMake,
        @NotBlank @Size(max = 60) String carModel,
        @NotNull CarType carType,
        @NotNull Transmission transmission,
        @Size(max = 1000) String notes) {
}
