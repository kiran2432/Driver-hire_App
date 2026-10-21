package com.driverhire.customer;

import com.driverhire.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customers")
@PreAuthorize("hasRole('CUSTOMER')")
public class CustomerController {

    private final CustomerService service;

    public CustomerController(CustomerService service) {
        this.service = service;
    }

    @GetMapping("/me")
    public CustomerProfileResponse me(@AuthenticationPrincipal Jwt jwt) {
        return service.getOwn(CurrentUser.id(jwt));
    }

    @PutMapping("/me")
    public CustomerProfileResponse update(@AuthenticationPrincipal Jwt jwt,
                                          @Valid @RequestBody UpdateCustomerRequest req) {
        return service.update(CurrentUser.id(jwt), req);
    }
}
