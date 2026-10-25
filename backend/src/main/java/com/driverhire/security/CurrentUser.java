package com.driverhire.security;

import org.springframework.security.oauth2.jwt.Jwt;

/** Reads the authenticated user's id (the JWT "sub" claim). */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static Long id(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
