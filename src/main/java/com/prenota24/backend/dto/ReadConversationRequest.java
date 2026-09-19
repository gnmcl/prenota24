package com.prenota24.backend.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ReadConversationRequest(@NotNull UUID messageId) {}
