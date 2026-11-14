package com.prenota24.backend.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public record CreateAvailabilityExceptionSlotRequest(
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime
) {
    /** Il database lo rifiuterebbe comunque, ma con un 409 generico invece di un errore chiaro. */
    @AssertTrue(message = "l'orario di fine deve essere successivo a quello di inizio")
    @JsonIgnore
    public boolean isEndAfterStart() {
        return startTime == null || endTime == null || endTime.isAfter(startTime);
    }
}
