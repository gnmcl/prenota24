package com.prenota24.backend.repository;

import com.prenota24.backend.domain.Conversation;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {
    @EntityGraph(attributePaths = {"client"})
    @Query("""
            SELECT c FROM Conversation c
            WHERE c.studio.id = :studioId
            ORDER BY c.lastMessageAt DESC NULLS LAST, c.createdAt DESC
            """)
    Page<Conversation> findForStudio(@Param("studioId") UUID studioId, Pageable pageable);

    @EntityGraph(attributePaths = {"client"})
    Optional<Conversation> findByIdAndStudioId(UUID id, UUID studioId);

    @EntityGraph(attributePaths = {"client"})
    Optional<Conversation> findByStudioIdAndClientId(UUID studioId, UUID clientId);

    @Modifying
    @Query(value = """
            INSERT INTO conversation (id, studio_id, client_id, whatsapp_opt_in, created_at, updated_at)
            VALUES (:id, :studioId, :clientId, false, now(), now())
            ON CONFLICT (studio_id, client_id) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("id") UUID id, @Param("studioId") UUID studioId, @Param("clientId") UUID clientId);

    @Modifying
    @Query(value = """
            UPDATE conversation
            SET last_message_at = CASE
                    WHEN last_message_at IS NULL OR last_message_at < :messageAt THEN :messageAt
                    ELSE last_message_at END,
                last_message_preview = :preview,
                last_message_sequence_no = :sequenceNo,
                updated_at = now()
            WHERE id = :conversationId
              AND (last_message_sequence_no IS NULL OR last_message_sequence_no < :sequenceNo)
            """, nativeQuery = true)
    int updateActivityIfNewer(@Param("conversationId") UUID conversationId,
                              @Param("sequenceNo") long sequenceNo,
                              @Param("messageAt") java.time.Instant messageAt,
                              @Param("preview") String preview);

    @Modifying
    @Query(value = """
            UPDATE conversation
            SET last_inbound_at = CASE
                    WHEN last_inbound_at IS NULL OR last_inbound_at < :inboundAt THEN :inboundAt
                    ELSE last_inbound_at END,
                last_inbound_phone = CASE
                    WHEN last_inbound_at IS NULL OR last_inbound_at < :inboundAt THEN :inboundPhone
                    ELSE last_inbound_phone END,
                last_message_at = CASE
                    WHEN last_message_at IS NULL OR last_message_at < :messageAt THEN :messageAt
                    ELSE last_message_at END,
                last_message_preview = CASE
                    WHEN last_message_sequence_no IS NULL OR last_message_sequence_no < :sequenceNo THEN :preview
                    ELSE last_message_preview END,
                last_message_sequence_no = CASE
                    WHEN last_message_sequence_no IS NULL OR last_message_sequence_no < :sequenceNo THEN :sequenceNo
                    ELSE last_message_sequence_no END,
                updated_at = now()
            WHERE id = :conversationId
            """, nativeQuery = true)
    int updateInboundActivity(@Param("conversationId") UUID conversationId,
                              @Param("sequenceNo") long sequenceNo,
                              @Param("inboundAt") java.time.Instant inboundAt,
                              @Param("inboundPhone") String inboundPhone,
                              @Param("messageAt") java.time.Instant messageAt,
                              @Param("preview") String preview);
}
