package com.prenota24.backend.repository;

import com.prenota24.backend.domain.ConversationMessage;
import com.prenota24.backend.domain.MessageDirection;
import com.prenota24.backend.domain.MessageStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConversationMessageRepository extends JpaRepository<ConversationMessage, UUID> {
    @EntityGraph(attributePaths = {"appointment"})
    @Query("""
            SELECT m FROM ConversationMessage m
            WHERE m.conversation.id = :conversationId AND m.conversation.studio.id = :studioId
            ORDER BY m.sequenceNo DESC
            """)
    Page<ConversationMessage> findHistory(@Param("conversationId") UUID conversationId,
                                          @Param("studioId") UUID studioId,
                                          Pageable pageable);

    Optional<ConversationMessage> findByConversationIdAndRequestId(UUID conversationId, UUID requestId);

    Optional<ConversationMessage> findByProviderMessageId(String providerMessageId);

    Optional<ConversationMessage> findByIdAndConversationIdAndConversationStudioId(UUID id, UUID conversationId, UUID studioId);

    boolean existsByProviderMessageId(String providerMessageId);

    @Query("""
            SELECT COUNT(m) FROM ConversationMessage m
            WHERE m.conversation.id = :conversationId
              AND m.direction = :direction
              AND m.sequenceNo > :afterSequence
            """)
    long countUnread(@Param("conversationId") UUID conversationId,
                     @Param("direction") MessageDirection direction,
                     @Param("afterSequence") long afterSequence);

    long countByConversationIdAndDirection(UUID conversationId, MessageDirection direction);

    List<ConversationMessage> findTop50ByStatusOrderByCreatedAtAsc(MessageStatus status);

    @Modifying
    @Query("""
            UPDATE ConversationMessage m
            SET m.status = com.prenota24.backend.domain.MessageStatus.SENDING, m.updatedAt = :claimedAt
            WHERE m.id = :id AND m.status = com.prenota24.backend.domain.MessageStatus.QUEUED
            """)
    int claimForDispatch(@Param("id") UUID id, @Param("claimedAt") Instant claimedAt);

    @Modifying
    @Query(value = """
            INSERT INTO conversation_message
                (id, conversation_id, appointment_id, sender_user_id, request_id, request_fingerprint, recipient_phone, text,
                 direction, kind, status, sender_name, created_at, updated_at)
            VALUES
                (:id, :conversationId, :appointmentId, :senderUserId, :requestId, :fingerprint, :recipientPhone, :text,
                 'OUTBOUND', :kind, 'QUEUED', :senderName, now(), now())
            ON CONFLICT (conversation_id, request_id) DO NOTHING
            """, nativeQuery = true)
    int insertQueuedIfAbsent(@Param("id") UUID id,
                             @Param("conversationId") UUID conversationId,
                             @Param("appointmentId") UUID appointmentId,
                             @Param("senderUserId") UUID senderUserId,
                             @Param("requestId") UUID requestId,
                             @Param("fingerprint") String fingerprint,
                             @Param("recipientPhone") String recipientPhone,
                             @Param("text") String text,
                             @Param("kind") String kind,
                             @Param("senderName") String senderName);

    @Modifying
    @Query("""
            UPDATE ConversationMessage m
            SET m.status = com.prenota24.backend.domain.MessageStatus.UNKNOWN,
                m.errorMessage = 'Dispatch interrotto dopo la presa in carico; esito non verificabile',
                m.updatedAt = :recoveredAt
            WHERE m.status = com.prenota24.backend.domain.MessageStatus.SENDING
              AND m.updatedAt < :cutoff
            """)
    int markStaleSendingUnknown(@Param("cutoff") Instant cutoff, @Param("recoveredAt") Instant recoveredAt);

    @Modifying
    @Query(value = """
            INSERT INTO conversation_message
                (id, conversation_id, provider_message_id, text, direction, kind, status,
                 sender_name, created_at, updated_at)
            VALUES
                (:id, :conversationId, :providerMessageId, :text, 'INBOUND', :kind, 'RECEIVED',
                 :senderName, :createdAt, now())
            ON CONFLICT DO NOTHING
            """, nativeQuery = true)
    int insertInboundIfAbsent(@Param("id") UUID id,
                              @Param("conversationId") UUID conversationId,
                              @Param("providerMessageId") String providerMessageId,
                              @Param("text") String text,
                              @Param("kind") String kind,
                              @Param("senderName") String senderName,
                              @Param("createdAt") Instant createdAt);

    @Modifying
    @Query(value = """
            UPDATE conversation_message
            SET status = CAST(:status AS varchar),
                provider_message_id = COALESCE(provider_message_id, :providerMessageId),
                error_message = :errorMessage,
                updated_at = now()
            WHERE id = :id AND status = 'SENDING'
            """, nativeQuery = true)
    int completeDispatch(@Param("id") UUID id,
                         @Param("status") String status,
                         @Param("providerMessageId") String providerMessageId,
                         @Param("errorMessage") String errorMessage);

    @Modifying
    @Query(value = """
            UPDATE conversation_message
            SET status = CAST(:nextStatus AS varchar),
                provider_message_id = COALESCE(provider_message_id, :providerMessageId),
                error_message = :errorMessage,
                updated_at = now()
            WHERE id = :id
              AND direction = 'OUTBOUND'
              AND (
                    (:nextStatus = 'ACCEPTED' AND status IN ('QUEUED','SENDING','UNKNOWN')) OR
                    (:nextStatus = 'DELIVERED' AND status IN ('QUEUED','SENDING','ACCEPTED','UNKNOWN')) OR
                    (:nextStatus = 'READ' AND status IN ('QUEUED','SENDING','ACCEPTED','DELIVERED','UNKNOWN')) OR
                    (:nextStatus = 'FAILED' AND status IN ('QUEUED','SENDING','ACCEPTED','UNKNOWN'))
              )
            """, nativeQuery = true)
    int advanceProviderStatus(@Param("id") UUID id,
                              @Param("nextStatus") String nextStatus,
                              @Param("providerMessageId") String providerMessageId,
                              @Param("errorMessage") String errorMessage);
}
