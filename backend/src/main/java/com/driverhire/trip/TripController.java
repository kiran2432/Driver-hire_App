package com.driverhire.trip;

import com.driverhire.common.PageResponse;
import com.driverhire.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/trips")
public class TripController {

    private final TripService service;

    public TripController(TripService service) {
        this.service = service;
    }

    // ---- customer ----------------------------------------------------------------------------------

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CUSTOMER')")
    public TripResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateTripRequest req) {
        return service.create(CurrentUser.id(jwt), req);
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('CUSTOMER')")
    public PageResponse<TripResponse> mine(@AuthenticationPrincipal Jwt jwt,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "10") int size) {
        return service.listForCustomer(CurrentUser.id(jwt), page, size);
    }

    @PostMapping("/{id}/hire")
    @PreAuthorize("hasRole('CUSTOMER')")
    public TripResponse hire(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                             @Valid @RequestBody HireRequest req) {
        return service.hire(CurrentUser.id(jwt), id, req.driverId());
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('CUSTOMER')")
    public TripResponse cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return service.cancel(CurrentUser.id(jwt), id);
    }

    // ---- driver ------------------------------------------------------------------------------------

    @GetMapping("/open")
    @PreAuthorize("hasRole('DRIVER')")
    public PageResponse<TripResponse> open(@AuthenticationPrincipal Jwt jwt,
                                           @RequestParam(required = false) String city,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "10") int size) {
        return service.listOpenForDriver(CurrentUser.id(jwt), city, page, size);
    }

    @GetMapping("/assigned")
    @PreAuthorize("hasRole('DRIVER')")
    public PageResponse<TripResponse> assigned(@AuthenticationPrincipal Jwt jwt,
                                               @RequestParam(required = false) TripStatus status,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "10") int size) {
        return service.listAssigned(CurrentUser.id(jwt), status, page, size);
    }

    @PostMapping("/{id}/accept")
    @PreAuthorize("hasRole('DRIVER')")
    public TripResponse accept(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return service.accept(CurrentUser.id(jwt), id);
    }

    @PostMapping("/{id}/decline")
    @PreAuthorize("hasRole('DRIVER')")
    public TripResponse decline(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return service.decline(CurrentUser.id(jwt), id);
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasRole('DRIVER')")
    public TripResponse complete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return service.complete(CurrentUser.id(jwt), id);
    }

    // ---- both --------------------------------------------------------------------------------------

    @GetMapping("/{id}")
    public TripResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return service.get(CurrentUser.id(jwt), id);
    }
}
