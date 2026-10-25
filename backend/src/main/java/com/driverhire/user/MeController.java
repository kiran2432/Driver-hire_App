package com.driverhire.user;

import com.driverhire.common.ApiException;
import com.driverhire.customer.CustomerProfileResponse;
import com.driverhire.customer.CustomerService;
import com.driverhire.driver.DriverProfileResponse;
import com.driverhire.driver.DriverService;
import com.driverhire.security.CurrentUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * "Who am I" for the SPA: the account plus the profile that matches its role.
 */
@RestController
@RequestMapping("/api/me")
public class MeController {

    /** Exactly one of driver / customer is non-null. */
    public record MeResponse(UserView user, DriverProfileResponse driver, CustomerProfileResponse customer) {
    }

    private final UserRepository users;
    private final DriverService drivers;
    private final CustomerService customers;

    public MeController(UserRepository users, DriverService drivers, CustomerService customers) {
        this.users = users;
        this.drivers = drivers;
        this.customers = customers;
    }

    @GetMapping
    public MeResponse me(@AuthenticationPrincipal Jwt jwt) {
        Long id = CurrentUser.id(jwt);
        User user = users.findById(id)
                .filter(User::isEnabled)
                .orElseThrow(() -> ApiException.unauthorized("Account not available"));
        if (user.getRole() == Role.DRIVER) {
            return new MeResponse(UserView.from(user), drivers.getOwn(id), null);
        }
        return new MeResponse(UserView.from(user), null, customers.getOwn(id));
    }
}
