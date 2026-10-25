package com.driverhire.customer;

public record CustomerProfileResponse(
        Long id,
        String fullName,
        String email,
        String phone,
        String address,
        String city,
        String state) {

    public static CustomerProfileResponse from(CustomerProfile c) {
        return new CustomerProfileResponse(
                c.getId(),
                c.getUser().getFullName(),
                c.getUser().getEmail(),
                c.getUser().getPhone(),
                c.getAddress(),
                c.getCity(),
                c.getState());
    }
}
