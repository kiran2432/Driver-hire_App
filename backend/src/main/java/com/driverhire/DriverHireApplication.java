package com.driverhire;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class DriverHireApplication {

    public static void main(String[] args) {
        SpringApplication.run(DriverHireApplication.class, args);
    }
}
