package com.driverhire.trip;

import jakarta.validation.constraints.NotNull;

/** @param driverId the driver profile id (as returned by the driver search) */
public record HireRequest(@NotNull Long driverId) {
}
