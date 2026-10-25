package com.driverhire.driver;

import jakarta.validation.constraints.NotNull;

public record AvailabilityRequest(@NotNull Boolean available) {
}
