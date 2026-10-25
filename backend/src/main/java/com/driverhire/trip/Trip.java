package com.driverhire.trip;

import com.driverhire.common.ApiException;
import com.driverhire.common.BaseEntity;
import com.driverhire.driver.DriverProfile;
import com.driverhire.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

/**
 * A trip a customer wants a driver for. All state changes go through the methods below, which enforce
 * the lifecycle (OPEN -> REQUESTED -> CONFIRMED -> COMPLETED, with CANCELLED / back to OPEN as exits).
 * {@code @Version} makes concurrent changes (e.g. two drivers accepting the same open trip) fail
 * with an optimistic-lock error instead of silently overwriting each other.
 */
@Entity
@Table(name = "trips")
public class Trip extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false, updatable = false)
    private User customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_profile_id")
    private DriverProfile driver;

    @Column(name = "pickup_city", nullable = false)
    private String pickupCity;

    @Column(name = "pickup_location", nullable = false)
    private String pickupLocation;

    @Column(nullable = false)
    private String destination;

    @Enumerated(EnumType.STRING)
    @Column(name = "trip_type", nullable = false)
    private TripType tripType;

    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    @Column(name = "return_at")
    private Instant returnAt;

    @Column(name = "car_make", nullable = false)
    private String carMake;

    @Column(name = "car_model", nullable = false)
    private String carModel;

    @Enumerated(EnumType.STRING)
    @Column(name = "car_type", nullable = false)
    private CarType carType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Transmission transmission;

    @Column
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TripStatus status = TripStatus.OPEN;

    @Version
    private Long version;

    protected Trip() {
        // for JPA
    }

    public Trip(User customer, CreateTripRequest in) {
        this.customer = customer;
        this.pickupCity = in.pickupCity().trim();
        this.pickupLocation = in.pickupLocation().trim();
        this.destination = in.destination().trim();
        this.tripType = in.tripType();
        this.startAt = in.startAt();
        this.returnAt = in.tripType() == TripType.ROUND_TRIP ? in.returnAt() : null;
        this.carMake = in.carMake().trim();
        this.carModel = in.carModel().trim();
        this.carType = in.carType();
        this.transmission = in.transmission();
        this.notes = in.notes() == null || in.notes().isBlank() ? null : in.notes().trim();
    }

    // ---- lifecycle -------------------------------------------------------------------------------

    /** Customer picks a specific driver. */
    public void requestDriver(DriverProfile chosen) {
        requireStatus("Only open trips can be given a driver", TripStatus.OPEN);
        this.driver = chosen;
        this.status = TripStatus.REQUESTED;
    }

    /** The requested driver accepts. */
    public void confirm() {
        requireStatus("Only requested trips can be confirmed", TripStatus.REQUESTED);
        this.status = TripStatus.CONFIRMED;
    }

    /** A driver accepts an open trip directly (nobody was requested). */
    public void claim(DriverProfile claimant) {
        requireStatus("This trip is no longer open", TripStatus.OPEN);
        this.driver = claimant;
        this.status = TripStatus.CONFIRMED;
    }

    /** The driver backs out (declines a request or drops a confirmed trip); the trip goes back to the open pool. */
    public void release() {
        requireStatus("Only requested or confirmed trips can be declined", TripStatus.REQUESTED, TripStatus.CONFIRMED);
        this.driver = null;
        this.status = TripStatus.OPEN;
    }

    public void complete() {
        requireStatus("Only confirmed trips can be completed", TripStatus.CONFIRMED);
        this.status = TripStatus.COMPLETED;
    }

    public void cancel() {
        requireStatus("This trip can no longer be cancelled",
                TripStatus.OPEN, TripStatus.REQUESTED, TripStatus.CONFIRMED);
        this.status = TripStatus.CANCELLED;
    }

    public boolean isAssignedTo(Long driverProfileId) {
        return driver != null && driver.getId().equals(driverProfileId);
    }

    private void requireStatus(String message, TripStatus... allowed) {
        for (TripStatus s : allowed) {
            if (status == s) {
                return;
            }
        }
        throw ApiException.conflict(message + " (current status: " + status + ")");
    }

    // ---- getters ---------------------------------------------------------------------------------

    public User getCustomer() {
        return customer;
    }

    public DriverProfile getDriver() {
        return driver;
    }

    public String getPickupCity() {
        return pickupCity;
    }

    public String getPickupLocation() {
        return pickupLocation;
    }

    public String getDestination() {
        return destination;
    }

    public TripType getTripType() {
        return tripType;
    }

    public Instant getStartAt() {
        return startAt;
    }

    public Instant getReturnAt() {
        return returnAt;
    }

    public String getCarMake() {
        return carMake;
    }

    public String getCarModel() {
        return carModel;
    }

    public CarType getCarType() {
        return carType;
    }

    public Transmission getTransmission() {
        return transmission;
    }

    public String getNotes() {
        return notes;
    }

    public TripStatus getStatus() {
        return status;
    }
}
