package com.prenota24.backend.dto;

import java.time.Instant;

public record TimeSlotResponse(
        Instant start,
        Instant end
) {}
