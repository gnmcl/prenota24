package com.prenota24.backend.domain;

public enum AppointmentCapacityLevel {
    /** Count is below the warning threshold (or no threshold configured). */
    AVAILABLE,
    /** Count has reached or exceeded the warning threshold. */
    WARNING,
    /** Count has reached or exceeded the critical threshold. */
    CRITICAL
}
