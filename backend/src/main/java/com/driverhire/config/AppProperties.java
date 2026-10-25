package com.driverhire.config;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Typed application settings (see application.yml, prefix "app").
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(Jwt jwt, Cors cors, RateLimit rateLimit, Cookie cookie) {

    /** @param secure value of the "Secure" flag on the refresh-token cookie (true everywhere except local HTTP dev) */
    public record Cookie(boolean secure) {
    }

    /**
     * @param secret     HMAC signing secret, at least 32 characters. Supply via the JWT_SECRET env var.
     * @param issuer     "iss" claim written to and required from every token
     * @param accessTtl  lifetime of access tokens (short)
     * @param refreshTtl lifetime of refresh tokens
     */
    public record Jwt(String secret, String issuer, Duration accessTtl, Duration refreshTtl) {
    }

    /** Browser origins allowed to call the API cross-origin (not needed when served behind one origin). */
    public record Cors(List<String> allowedOrigins) {
    }

    /** Requests per minute, per client IP, for /api/auth/**. */
    public record RateLimit(int authPerMinute) {
    }
}
