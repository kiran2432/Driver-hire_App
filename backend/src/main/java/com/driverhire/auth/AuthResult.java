package com.driverhire.auth;

/** Service-level result: the JSON body plus the raw refresh token the controller puts in a cookie. */
public record AuthResult(AuthResponse body, String refreshToken) {
}
