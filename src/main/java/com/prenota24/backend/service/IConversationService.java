package com.prenota24.backend.service;

import com.prenota24.backend.dto.*;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IConversationService {
    Page<ConversationResponse> list(UUID studioId, UUID userId, Pageable pageable);
    ConversationResponse get(UUID conversationId, UUID studioId, UUID userId);
    ConversationResponse getOrCreateForAppointment(UUID appointmentId, UUID studioId, UUID userId);
    Page<MessageResponse> listMessages(UUID conversationId, UUID studioId, Pageable pageable);
    MessageResponse enqueue(UUID conversationId, SendMessageRequest request, UUID studioId, UUID userId);
    void markRead(UUID conversationId, UUID messageId, UUID studioId, UUID userId);
    ConversationResponse updateConsent(UUID conversationId, boolean enabled, UUID studioId, UUID userId);
}
