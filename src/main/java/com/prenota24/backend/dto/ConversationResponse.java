package com.prenota24.backend.dto;

import java.time.Instant;
import java.util.UUID;

public record ConversationResponse(
        UUID id,
        UUID clientId,
        String clientName,
        String clientPhone,
        String lastMessagePreview,
        Instant lastMessageAt,
        long unreadCount,
        boolean whatsappConfigured,
        boolean whatsappOptIn,
        boolean canSendText,
        boolean canSendTemplate,
        String sendBlockedReason,
        Instant serviceWindowExpiresAt
) {}
