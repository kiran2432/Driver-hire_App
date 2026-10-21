package com.driverhire.driver;

import com.driverhire.common.PageResponse;
import com.driverhire.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/drivers")
public class DriverController {

    private final DriverService service;

    public DriverController(DriverService service) {
        this.service = service;
    }

    /** Customers browse available drivers. */
    @GetMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public PageResponse<DriverSummary> search(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String transmission,
            @RequestParam(defaultValue = "0") int minExperience,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        return service.search(city, transmission, minExperience, page, size);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public DriverSummary get(@PathVariable Long id) {
        return service.getPublic(id);
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('DRIVER')")
    public DriverProfileResponse me(@AuthenticationPrincipal Jwt jwt) {
        return service.getOwn(CurrentUser.id(jwt));
    }

    @PutMapping("/me")
    @PreAuthorize("hasRole('DRIVER')")
    public DriverProfileResponse update(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody UpdateDriverRequest req) {
        return service.update(CurrentUser.id(jwt), req);
    }

    @PatchMapping("/me/availability")
    @PreAuthorize("hasRole('DRIVER')")
    public DriverProfileResponse availability(@AuthenticationPrincipal Jwt jwt,
                                              @Valid @RequestBody AvailabilityRequest req) {
        return service.setAvailability(CurrentUser.id(jwt), req.available());
    }
}
