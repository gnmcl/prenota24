package com.prenota24.backend.dto;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record ProposeNewTimeRequest(
        @NotNull Instant proposedStart,
        @NotNull Instant proposedEnd
) {}
