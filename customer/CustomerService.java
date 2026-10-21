package com.driverhire.customer;

import com.driverhire.common.ApiException;
import com.driverhire.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerService {

    private final CustomerProfileRepository customers;

    public CustomerService(CustomerProfileRepository customers) {
        this.customers = customers;
    }

    @Transactional(readOnly = true)
    public CustomerProfileResponse getOwn(Long userId) {
        return CustomerProfileResponse.from(requireByUser(userId));
    }

    @Transactional
    public CustomerProfileResponse update(Long userId, UpdateCustomerRequest req) {
        CustomerProfile profile = requireByUser(userId);
        User user = profile.getUser();
        user.setFullName(req.fullName().trim());
        user.setPhone(req.phone().trim());
        profile.apply(req.profile());
        return CustomerProfileResponse.from(profile);
    }

    private CustomerProfile requireByUser(Long userId) {
        return customers.findByUserId(userId).orElseThrow(() -> ApiException.notFound("Customer profile not found"));
    }
}
