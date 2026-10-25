package com.driverhire.trip;

/**
 * OPEN      customer posted the trip; no driver yet (drivers nearby can see and accept it)
 * REQUESTED customer picked a specific driver; waiting for that driver to accept
 * CONFIRMED driver accepted; contact details are now shared both ways
 * COMPLETED driver marked the trip done
 * CANCELLED customer cancelled
 */
public enum TripStatus {
    OPEN,
    REQUESTED,
    CONFIRMED,
    COMPLETED,
    CANCELLED
}
