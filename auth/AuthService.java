package com.driverhire.auth;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.driverhire.common.ApiException;
import com.driverhire.customer.CustomerProfile;
import com.driverhire.customer.CustomerProfileRepository;
import com.driverhire.driver.DriverProfile;
import com.driverhire.driver.DriverProfileRepository;
import com.driverhire.driver.DriverService;
import com.driverhire.user.Role;
import com.driverhire.user.User;
import com.driverhire.user.UserRepository;
import com.driverhire.user.UserView;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registration, login and token refresh.
 * <p>
 * Note: {@link #login} and {@link #refresh} are deliberately NOT transactional. The refresh path commits
 * session revocation even when it ends in a 401 (see {@link RefreshTokenService#rotate}); an outer
 * transaction would roll that back.
 */
@Service
public class AuthService {

    private static final int BCRYPT_MAX_BYTES = 72;

    private final UserRepository users;
    private final DriverProfileRepository drivers;
    private final CustomerProfileRepository customers;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final RefreshTokenService refreshTokens;

    /** Compared against when the email is unknown so login timing does not reveal which emails exist. */
    private final String dummyHash;

    public AuthService(UserRepository users,
                       DriverProfileRepository drivers,
                       CustomerProfileRepository customers,
                       PasswordEncoder passwordEncoder,
                       TokenService tokenService,
                       RefreshTokenService refreshTokens) {
        this.users = users;
        this.drivers = drivers;
        this.customers = customers;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.refreshTokens = refreshTokens;
        this.dummyHash = passwordEncoder.encode("timing-equaliser-not-a-real-password");
    }

    @Transactional
    public AuthResult registerDriver(RegisterDriverRequest req) {
        DriverService.requireTransmission(req.profile());
        User user = createUser(req.email(), req.password(), req.fullName(), req.phone(), Role.DRIVER);
        drivers.save(new DriverProfile(user, req.profile()));
        return issue(user);
    }

    @Transactional
    public AuthResult registerCustomer(RegisterCustomerRequest req) {
        User user = createUser(req.email(), req.password(), req.fullName(), req.phone(), Role.CUSTOMER);
        customers.save(new CustomerProfile(user, req.profile()));
        return issue(user);
    }

    public AuthResult login(LoginRequest req) {
        User user = users.findByEmail(normalise(req.email())).orElse(null);
        boolean passwordOk = passwordEncoder.matches(req.password(), user == null ? dummyHash : user.getPasswordHash());
        if (user == null || !passwordOk || !user.isEnabled()) {
            throw ApiException.unauthorized("Invalid email or password");
        }
        return issue(user);
    }

    public AuthResult refresh(String rawRefreshToken) {
        RefreshTokenService.Rotation rotation = refreshTokens.rotate(rawRefreshToken);
        return result(rotation.user(), rotation.newToken());
    }

    public void logout(String rawRefreshToken) {
        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            refreshTokens.revoke(rawRefreshToken);
        }
    }

    private User createUser(String email, String password, String fullName, String phone, Role role) {
        if (password.getBytes(UTF_8).length > BCRYPT_MAX_BYTES) {
            throw ApiException.badRequest("Password is too long (maximum 72 bytes)");
        }
        String normalised = normalise(email);
        if (users.existsByEmail(normalised)) {
            throw ApiException.conflict("An account with this email already exists");
        }
        return users.save(new User(normalised, passwordEncoder.encode(password), fullName.trim(), phone.trim(), role));
    }

    private AuthResult issue(User user) {
        return result(user, refreshTokens.issue(user.getId()));
    }

    private AuthResult result(User user, String refreshToken) {
        AuthResponse body = new AuthResponse(
                tokenService.createAccessToken(user), "Bearer", tokenService.accessTtlSeconds(), UserView.from(user));
        return new AuthResult(body, refreshToken);
    }

    private static String normalise(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
