package com.driverhire.driver;

import com.driverhire.common.BaseEntity;
import com.driverhire.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Entity
@Table(name = "driver_profiles")
public class DriverProfile extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String state;

    @Column(name = "years_of_experience", nullable = false)
    private int yearsOfExperience;

    @Column(name = "license_number", nullable = false)
    private String licenseNumber;

    @Column(name = "license_expiry", nullable = false)
    private LocalDate licenseExpiry;

    @Column(name = "drives_manual", nullable = false)
    private boolean drivesManual;

    @Column(name = "drives_automatic", nullable = false)
    private boolean drivesAutomatic;

    /** Comma-separated; use {@link #getLanguageList()}. */
    @Column
    private String languages;

    @Column
    private String bio;

    @Column(name = "daily_rate", nullable = false, precision = 10, scale = 2)
    private BigDecimal dailyRate;

    @Column(nullable = false)
    private boolean available = true;

    protected DriverProfile() {
        // for JPA
    }

    public DriverProfile(User user, DriverProfileInput input) {
        this.user = user;
        apply(input);
    }

    /** Copies every editable field from the validated request. */
    public void apply(DriverProfileInput in) {
        this.address = in.address().trim();
        this.city = in.city().trim();
        this.state = in.state().trim();
        this.yearsOfExperience = in.yearsOfExperience();
        this.licenseNumber = in.licenseNumber().trim();
        this.licenseExpiry = in.licenseExpiry();
        this.drivesManual = in.drivesManual();
        this.drivesAutomatic = in.drivesAutomatic();
        this.languages = in.languages() == null || in.languages().isEmpty()
                ? null
                : String.join(",", in.languages().stream().map(String::trim).filter(s -> !s.isEmpty()).toList());
        this.bio = in.bio() == null || in.bio().isBlank() ? null : in.bio().trim();
        this.dailyRate = in.dailyRate();
    }

    public List<String> getLanguageList() {
        if (languages == null || languages.isBlank()) {
            return List.of();
        }
        return Arrays.stream(languages.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    public User getUser() {
        return user;
    }

    public String getAddress() {
        return address;
    }

    public String getCity() {
        return city;
    }

    public String getState() {
        return state;
    }

    public int getYearsOfExperience() {
        return yearsOfExperience;
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public LocalDate getLicenseExpiry() {
        return licenseExpiry;
    }

    public boolean isDrivesManual() {
        return drivesManual;
    }

    public boolean isDrivesAutomatic() {
        return drivesAutomatic;
    }

    public String getBio() {
        return bio;
    }

    public BigDecimal getDailyRate() {
        return dailyRate;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }
}
