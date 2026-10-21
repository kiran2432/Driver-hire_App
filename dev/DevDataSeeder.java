package com.driverhire.dev;

import com.driverhire.auth.AuthService;
import com.driverhire.auth.RegisterCustomerRequest;
import com.driverhire.auth.RegisterDriverRequest;
import com.driverhire.customer.CustomerProfileInput;
import com.driverhire.driver.DriverProfileInput;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Demo accounts for local development only (profile "dev"; the bean does not exist in any other profile).
 * Every account uses the password {@value #PASSWORD}.
 */
@Component
@Profile("dev")
public class DevDataSeeder implements CommandLineRunner {

    static final String PASSWORD = "Password123";

    private static final Logger log = LoggerFactory.getLogger(DevDataSeeder.class);

    private final AuthService auth;

    public DevDataSeeder(AuthService auth) {
        this.auth = auth;
    }

    @Override
    public void run(String... args) {
        auth.registerCustomer(new RegisterCustomerRequest(
                "customer@example.com", PASSWORD, "Asha Patil", "9876500001",
                new CustomerProfileInput("12 MG Road", "Vijayapura", "Karnataka")));

        driver("ravi@example.com", "Ravi Kumar", "9876500011", "Vijayapura", 12, true, true, "Kannada,Hindi,English",
                "Calm, punctual driver with long-distance and highway experience.", "1200.00");
        driver("suresh@example.com", "Suresh Naik", "9876500012", "Vijayapura", 5, true, false, "Kannada,Hindi",
                "Comfortable with city and short trips.", "900.00");
        driver("meena@example.com", "Meena Shetty", "9876500013", "Hubballi", 8, true, true, "Kannada,English",
                "Experienced with automatic and luxury cars.", "1500.00");

        log.info("Dev data ready. Sign in with customer@example.com or ravi@example.com (password: {})", PASSWORD);
    }

    private void driver(String email, String name, String phone, String city, int years,
                        boolean manual, boolean automatic, String languages, String bio, String rate) {
        auth.registerDriver(new RegisterDriverRequest(email, PASSWORD, name, phone,
                new DriverProfileInput(
                        "Near Gol Gumbaz", city, "Karnataka", years,
                        "KA28-2015-" + phone.substring(6), LocalDate.now().plusYears(5),
                        manual, automatic, List.of(languages.split(",")), bio, new BigDecimal(rate))));
    }
}
