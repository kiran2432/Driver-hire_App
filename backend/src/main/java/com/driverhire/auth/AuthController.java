package com.driverhire.auth;

import com.driverhire.common.ApiException;
import com.driverhire.config.AppProperties;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.time.Duration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public authentication endpoints. The access token is returned in the body (kept in memory by the SPA);
 * the refresh token is only ever set as an HttpOnly, SameSite=Strict cookie scoped to /api/auth.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    static final String REFRESH_COOKIE = "dh_refresh";

    private final AuthService auth;
    private final RefreshTokenService refreshTokens;
    private final AppProperties props;

    public AuthController(AuthService auth, RefreshTokenService refreshTokens, AppProperties props) {
        this.auth = auth;
        this.refreshTokens = refreshTokens;
        this.props = props;
    }

    @PostMapping("/register/driver")
    public ResponseEntity<AuthResponse> registerDriver(@Valid @RequestBody RegisterDriverRequest req) {
        return respond(HttpStatus.CREATED, auth.registerDriver(req));
    }

    @PostMapping("/register/customer")
    public ResponseEntity<AuthResponse> registerCustomer(@Valid @RequestBody RegisterCustomerRequest req) {
        return respond(HttpStatus.CREATED, auth.registerCustomer(req));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        return respond(HttpStatus.OK, auth.login(req));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @CookieValue(name = REFRESH_COOKIE, required = false) String token,
            HttpServletResponse response) {
        try {
            if (token == null || token.isBlank()) {
                throw ApiException.unauthorized("Not signed in");
            }
            return respond(HttpStatus.OK, auth.refresh(token));
        } catch (ApiException ex) {
            // Drop the dead cookie so the browser stops sending it.
            response.addHeader(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO).toString());
            throw ex;
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue(name = REFRESH_COOKIE, required = false) String token) {
        auth.logout(token);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO).toString())
                .build();
    }

    private ResponseEntity<AuthResponse> respond(HttpStatus status, AuthResult result) {
        return ResponseEntity.status(status)
                .header(HttpHeaders.SET_COOKIE, cookie(result.refreshToken(), refreshTokens.ttl()).toString())
                .body(result.body());
    }

    private ResponseCookie cookie(String value, Duration maxAge) {
        return ResponseCookie.from(REFRESH_COOKIE, value)
                .httpOnly(true)
                .secure(props.cookie().secure())
                .sameSite("Strict")
                .path("/api/auth")
                .maxAge(maxAge)
                .build();
    }
}
