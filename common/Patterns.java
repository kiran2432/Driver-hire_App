package com.driverhire.common;

/**
 * Shared validation rules for request DTOs.
 */
public final class Patterns {

    /** Optional leading +, then 10 to 15 digits. */
    public static final String PHONE = "^\\+?[0-9]{10,15}$";

    /** 8-72 characters (bcrypt limit) with at least one letter and one digit. */
    public static final String PASSWORD = "^(?=.*[A-Za-z])(?=.*\\d).{8,72}$";

    public static final String PHONE_MESSAGE = "Phone must be 10-15 digits, optionally starting with +";
    public static final String PASSWORD_MESSAGE =
            "Password must be 8-72 characters and include at least one letter and one digit";

    private Patterns() {
    }
}
