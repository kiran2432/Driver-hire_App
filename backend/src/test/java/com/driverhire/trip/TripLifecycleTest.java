package com.driverhire.trip;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.driverhire.common.ApiException;
import com.driverhire.driver.DriverProfile;
import com.driverhire.driver.DriverProfileInput;
import com.driverhire.user.Role;
import com.driverhire.user.User;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/** Pure unit tests for the trip state machine (no Spring context). */
class TripLifecycleTest {

    private static Trip newTrip() {
        User customer = new User("c@example.com", "hash", "Customer", "9876543210", Role.CUSTOMER);
        return new Trip(customer, new CreateTripRequest(
                "Pune", "Station", "Goa", TripType.ONE_WAY, Instant.now().plus(1, ChronoUnit.DAYS), null,
                "Maruti", "Swift", CarType.HATCHBACK, Transmission.MANUAL, null));
    }

    private static DriverProfile newDriver() {
        User user = new User("d@example.com", "hash", "Driver", "9123456789", Role.DRIVER);
        return new DriverProfile(user, new DriverProfileInput(
                "Addr", "Pune", "MH", 5, "LIC-1", LocalDate.now().plusYears(2), true, true,
                List.of("English"), null, new BigDecimal("1000.00")));
    }

    @Test
    void newTripIsOpen() {
        assertThat(newTrip().getStatus()).isEqualTo(TripStatus.OPEN);
    }

    @Test
    void requestThenConfirmThenComplete() {
        Trip trip = newTrip();
        trip.requestDriver(newDriver());
        assertThat(trip.getStatus()).isEqualTo(TripStatus.REQUESTED);
        trip.confirm();
        assertThat(trip.getStatus()).isEqualTo(TripStatus.CONFIRMED);
        trip.complete();
        assertThat(trip.getStatus()).isEqualTo(TripStatus.COMPLETED);
    }

    @Test
    void releaseClearsTheDriverAndReopensTheTrip() {
        Trip trip = newTrip();
        trip.requestDriver(newDriver());
        trip.release();
        assertThat(trip.getStatus()).isEqualTo(TripStatus.OPEN);
        assertThat(trip.getDriver()).isNull();
    }

    @Test
    void illegalTransitionsAreConflicts() {
        Trip trip = newTrip();
        assertThatThrownBy(trip::confirm).isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThatThrownBy(trip::complete).isInstanceOf(ApiException.class);
        assertThatThrownBy(trip::release).isInstanceOf(ApiException.class);

        trip.cancel();
        assertThat(trip.getStatus()).isEqualTo(TripStatus.CANCELLED);
        assertThatThrownBy(trip::cancel).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> trip.requestDriver(newDriver())).isInstanceOf(ApiException.class);
    }

    @Test
    void oneWayTripsIgnoreAReturnTime() {
        User customer = new User("c@example.com", "hash", "Customer", "9876543210", Role.CUSTOMER);
        Instant start = Instant.now().plus(1, ChronoUnit.DAYS);
        Trip trip = new Trip(customer, new CreateTripRequest(
                "Pune", "Station", "Goa", TripType.ONE_WAY, start, start.plus(2, ChronoUnit.DAYS),
                "Maruti", "Swift", CarType.HATCHBACK, Transmission.MANUAL, null));
        assertThat(trip.getReturnAt()).isNull();
    }
}
