package com.prenota24.backend.dto;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * Richiesta di proposta nuovo orario: una proposta principale obbligatoria
 * e fino a due proposte alternative opzionali.
 */
public record ProposeNewTimeRequest(
        @NotNull Instant proposedStart,
        @NotNull Instant proposedEnd,
        Instant proposedStart2,
        Instant proposedEnd2,
        Instant proposedStart3,
        Instant proposedEnd3
) {}
