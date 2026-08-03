package com.prenota24.backend.dto;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * Body inviato dal cliente quando accetta uno degli orari proposti.
 * selectedStart/End devono corrispondere esattamente a una delle
 * proposte memorizzate sull'appuntamento.
 */
public record AcceptProposalRequest(
        @NotNull Instant selectedStart,
        @NotNull Instant selectedEnd
) {}
