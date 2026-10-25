package com.driverhire.customer;

import com.driverhire.common.BaseEntity;
import com.driverhire.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "customer_profiles")
public class CustomerProfile extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String state;

    protected CustomerProfile() {
        // for JPA
    }

    public CustomerProfile(User user, CustomerProfileInput input) {
        this.user = user;
        apply(input);
    }

    public void apply(CustomerProfileInput in) {
        this.address = in.address().trim();
        this.city = in.city().trim();
        this.state = in.state().trim();
    }

    public User getUser() {
        return user;
    }

    public String getAddress() {
        return address;
    }

    public String getCity() {
        return city;
    }

    public String getState() {
        return state;
    }
}
