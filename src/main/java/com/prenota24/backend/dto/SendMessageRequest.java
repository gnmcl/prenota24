package com.prenota24.backend.dto;

import com.prenota24.backend.domain.MessageKind;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record SendMessageRequest(
        @Size(max = 4000) String text,
        UUID appointmentId,
        @NotNull UUID requestId,
        @NotNull MessageKind kind
) {}
