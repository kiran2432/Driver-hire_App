package com.driverhire.driver;

import com.driverhire.common.ApiException;
import com.driverhire.common.PageResponse;
import com.driverhire.user.User;
import com.driverhire.user.UserRepository;
import java.util.Locale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DriverService {

    private final DriverProfileRepository drivers;
    private final UserRepository users;

    public DriverService(DriverProfileRepository drivers, UserRepository users) {
        this.drivers = drivers;
        this.users = users;
    }

    /** Business rule shared by registration and profile updates. */
    public static void requireTransmission(DriverProfileInput in) {
        if (!in.drivesManual() && !in.drivesAutomatic()) {
            throw ApiException.badRequest("Select at least one transmission type you can drive (manual or automatic)");
        }
    }

    @Transactional(readOnly = true)
    public DriverProfileResponse getOwn(Long userId) {
        return DriverProfileResponse.from(requireByUser(userId));
    }

    @Transactional
    public DriverProfileResponse update(Long userId, UpdateDriverRequest req) {
        requireTransmission(req.profile());
        DriverProfile profile = requireByUser(userId);
        User user = profile.getUser();
        user.setFullName(req.fullName().trim());
        user.setPhone(req.phone().trim());
        profile.apply(req.profile());
        return DriverProfileResponse.from(profile);
    }

    @Transactional
    public DriverProfileResponse setAvailability(Long userId, boolean available) {
        DriverProfile profile = requireByUser(userId);
        profile.setAvailable(available);
        return DriverProfileResponse.from(profile);
    }

    /**
     * Customer-facing search. Only available, enabled drivers are returned.
     *
     * @param city          optional, case-insensitive exact match
     * @param transmission  optional MANUAL / AUTOMATIC: the driver must be able to drive that gearbox
     * @param minExperience minimum years of experience (0 = any)
     */
    @Transactional(readOnly = true)
    public PageResponse<DriverSummary> search(String city, String transmission, int minExperience, int page, int size) {
        String normalisedCity = city == null ? "" : city.trim().toLowerCase(Locale.ROOT);
        boolean needManual = "MANUAL".equalsIgnoreCase(transmission);
        boolean needAutomatic = "AUTOMATIC".equalsIgnoreCase(transmission);
        Page<DriverProfile> result = drivers.search(
                normalisedCity, needManual, needAutomatic, Math.max(minExperience, 0),
                PageResponse.pageable(page, size, Sort.unsorted()));
        return PageResponse.of(result, DriverSummary::from);
    }

    @Transactional(readOnly = true)
    public DriverSummary getPublic(Long driverProfileId) {
        DriverProfile profile = drivers.findDetailedById(driverProfileId)
                .filter(d -> d.getUser().isEnabled())
                .orElseThrow(() -> ApiException.notFound("Driver not found"));
        return DriverSummary.from(profile);
    }

    private DriverProfile requireByUser(Long userId) {
        return drivers.findByUserId(userId).orElseThrow(() -> ApiException.notFound("Driver profile not found"));
    }
}
