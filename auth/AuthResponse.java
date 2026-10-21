package com.driverhire.auth;

import com.driverhire.user.UserView;

/**
 * Body returned by register / login / refresh. The refresh token is NOT in the body:
 * it travels only in an HttpOnly cookie so page scripts can never read it.
 *
 * @param expiresIn access token lifetime in seconds
 */
public record AuthResponse(String accessToken, String tokenType, long expiresIn, UserView user) {
}
