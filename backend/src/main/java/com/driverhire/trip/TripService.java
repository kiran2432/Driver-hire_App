package com.driverhire.trip;

import com.driverhire.common.ApiException;
import com.driverhire.common.PageResponse;
import com.driverhire.driver.DriverProfile;
import com.driverhire.driver.DriverProfileRepository;
import com.driverhire.user.User;
import com.driverhire.user.UserRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The hiring workflow. Authorisation rules live here (not only in controllers) so every path is covered:
 * customers only touch their own trips, drivers only trips assigned to them or open ones.
 * Anything a user may not see is reported as "not found" to avoid confirming that it exists.
 */
@Service
@Transactional
public class TripService {

    private static final Set<TripStatus> ALL_STATUSES = EnumSet.allOf(TripStatus.class);

    private final TripRepository trips;
    private final UserRepository users;
    private final DriverProfileRepository drivers;

    public TripService(TripRepository trips, UserRepository users, DriverProfileRepository drivers) {
        this.trips = trips;
        this.users = users;
        this.drivers = drivers;
    }

    // ---- customer side -----------------------------------------------------------------------------

    public TripResponse create(Long customerId, CreateTripRequest req) {
        if (req.tripType() == TripType.ROUND_TRIP) {
            if (req.returnAt() == null) {
                throw ApiException.badRequest("Return date and time are required for a round trip");
            }
            if (!req.returnAt().isAfter(req.startAt())) {
                throw ApiException.badRequest("Return time must be after the start time");
            }
        }
        User customer = users.findById(customerId)
                .filter(User::isEnabled)
                .orElseThrow(() -> ApiException.unauthorized("Account not available"));
        Trip trip = trips.save(new Trip(customer, req));
        return TripResponse.from(trip);
    }

    @Transactional(readOnly = true)
    public PageResponse<TripResponse> listForCustomer(Long customerId, int page, int size) {
        return PageResponse.of(
                trips.findForCustomer(customerId, PageResponse.pageable(page, size, Sort.unsorted())),
                TripResponse::from);
    }

    /** Customer picks a driver: OPEN -> REQUESTED. */
    public TripResponse hire(Long customerId, Long tripId, Long driverProfileId) {
        Trip trip = requireOwned(customerId, tripId);
        DriverProfile driver = drivers.findDetailedById(driverProfileId)
                .filter(d -> d.getUser().isEnabled())
                .orElseThrow(() -> ApiException.notFound("Driver not found"));

        if (!driver.isAvailable()) {
            throw ApiException.conflict("This driver is not available right now");
        }
        requireCanTake(driver, trip, "This driver");
        trip.requestDriver(driver);
        return TripResponse.from(trip);
    }

    /** Customer cancels: OPEN / REQUESTED / CONFIRMED -> CANCELLED. */
    public TripResponse cancel(Long customerId, Long tripId) {
        Trip trip = requireOwned(customerId, tripId);
        trip.cancel();
        return TripResponse.from(trip);
    }

    // ---- driver side -------------------------------------------------------------------------------

    /** Open trips in the driver's own city (or an explicitly requested one). */
    @Transactional(readOnly = true)
    public PageResponse<TripResponse> listOpenForDriver(Long userId, String city, int page, int size) {
        DriverProfile me = requireDriver(userId);
        String wanted = (city == null || city.isBlank() ? me.getCity() : city).trim().toLowerCase(Locale.ROOT);
        return PageResponse.of(
                trips.findOpenByCity(TripStatus.OPEN, wanted, Instant.now(),
                        PageResponse.pageable(page, size, Sort.unsorted())),
                TripResponse::from);
    }

    /** Trips assigned to this driver (requests, confirmed, finished, cancelled), optionally one status only. */
    @Transactional(readOnly = true)
    public PageResponse<TripResponse> listAssigned(Long userId, TripStatus status, int page, int size) {
        DriverProfile me = requireDriver(userId);
        Set<TripStatus> statuses = status == null ? ALL_STATUSES : EnumSet.of(status);
        return PageResponse.of(
                trips.findForDriver(me.getId(), statuses, PageResponse.pageable(page, size, Sort.unsorted())),
                TripResponse::from);
    }

    /**
     * Driver accepts. If the customer requested this driver (REQUESTED) the trip is confirmed;
     * if the trip is OPEN the driver claims it directly. Two drivers racing for the same open trip:
     * the loser gets 409 from the optimistic lock.
     */
    public TripResponse accept(Long userId, Long tripId) {
        DriverProfile me = requireDriver(userId);
        Trip trip = requireDetailed(tripId);

        switch (trip.getStatus()) {
            case REQUESTED -> {
                requireAssigned(trip, me);
                requireLicenceValidFor(me, trip, "Your");
                trip.confirm();
            }
            case OPEN -> {
                requireCanTake(me, trip, "You");
                trip.claim(me);
            }
            default -> throw ApiException.conflict("This trip can no longer be accepted (current status: "
                    + trip.getStatus() + ")");
        }
        return TripResponse.from(trip);
    }

    /** Driver declines a request or drops a confirmed trip: back to OPEN so the customer can pick someone else. */
    public TripResponse decline(Long userId, Long tripId) {
        DriverProfile me = requireDriver(userId);
        Trip trip = requireDetailed(tripId);
        requireAssigned(trip, me);
        trip.release();
        return TripResponse.from(trip);
    }

    /** Driver marks a confirmed trip as done. */
    public TripResponse complete(Long userId, Long tripId) {
        DriverProfile me = requireDriver(userId);
        Trip trip = requireDetailed(tripId);
        requireAssigned(trip, me);
        trip.complete();
        return TripResponse.from(trip);
    }

    // ---- shared ------------------------------------------------------------------------------------

    /** Visible to: the owning customer, the assigned driver, or any driver while the trip is still open. */
    @Transactional(readOnly = true)
    public TripResponse get(Long userId, Long tripId) {
        Trip trip = requireDetailed(tripId);
        boolean owner = trip.getCustomer().getId().equals(userId);
        boolean assignedDriver = trip.getDriver() != null && trip.getDriver().getUser().getId().equals(userId);
        boolean browsingDriver = trip.getStatus() == TripStatus.OPEN && drivers.findByUserId(userId).isPresent();
        if (!owner && !assignedDriver && !browsingDriver) {
            throw ApiException.notFound("Trip not found");
        }
        return TripResponse.from(trip);
    }

    // ---- helpers -----------------------------------------------------------------------------------

    private Trip requireDetailed(Long tripId) {
        return trips.findDetailedById(tripId).orElseThrow(() -> ApiException.notFound("Trip not found"));
    }

    private Trip requireOwned(Long customerId, Long tripId) {
        Trip trip = requireDetailed(tripId);
        if (!trip.getCustomer().getId().equals(customerId)) {
            throw ApiException.notFound("Trip not found");
        }
        return trip;
    }

    private DriverProfile requireDriver(Long userId) {
        return drivers.findByUserId(userId).orElseThrow(() -> ApiException.forbidden("Driver profile required"));
    }

    private static void requireAssigned(Trip trip, DriverProfile driver) {
        if (!trip.isAssignedTo(driver.getId())) {
            throw ApiException.notFound("Trip not found");
        }
    }

    /** A driver may take a trip only if it is in the future, matches their gearbox skills and licence validity. */
    private static void requireCanTake(DriverProfile driver, Trip trip, String subject) {
        if (!trip.getStartAt().isAfter(Instant.now())) {
            throw ApiException.conflict("The trip start time has already passed");
        }
        boolean canDrive = trip.getTransmission() == Transmission.MANUAL
                ? driver.isDrivesManual()
                : driver.isDrivesAutomatic();
        if (!canDrive) {
            throw ApiException.badRequest(subject + " cannot drive a "
                    + trip.getTransmission().name().toLowerCase(Locale.ROOT) + " car");
        }
        requireLicenceValidFor(driver, trip, subject.equals("You") ? "Your" : subject + "'s");
    }

    private static void requireLicenceValidFor(DriverProfile driver, Trip trip, String possessive) {
        LocalDate tripDate = trip.getStartAt().atZone(ZoneOffset.UTC).toLocalDate();
        if (driver.getLicenseExpiry().isBefore(tripDate)) {
            throw ApiException.badRequest(possessive + " driving licence expires before this trip");
        }
    }
}
