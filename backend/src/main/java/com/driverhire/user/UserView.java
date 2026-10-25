package com.driverhire.user;

/** Public view of the signed-in account (never includes the password hash). */
public record UserView(Long id, String email, String fullName, String phone, Role role) {

    public static UserView from(User u) {
        return new UserView(u.getId(), u.getEmail(), u.getFullName(), u.getPhone(), u.getRole());
    }
}
