package com.prenota24.backend.repository;

import com.prenota24.backend.domain.ConversationReadCursor;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConversationReadCursorRepository extends JpaRepository<ConversationReadCursor, UUID> {
    Optional<ConversationReadCursor> findByConversationIdAndUserId(UUID conversationId, UUID userId);

    @Modifying
    @Query(value = """
            INSERT INTO conversation_read_cursor
                (id, conversation_id, user_id, last_read_message_id, last_read_message_created_at,
                 last_read_sequence_no, updated_at)
            VALUES
                (gen_random_uuid(), :conversationId, :userId, :messageId, :messageCreatedAt,
                 :sequenceNo, now())
            ON CONFLICT (conversation_id, user_id) DO UPDATE
            SET last_read_message_id = EXCLUDED.last_read_message_id,
                last_read_message_created_at = EXCLUDED.last_read_message_created_at,
                last_read_sequence_no = EXCLUDED.last_read_sequence_no,
                updated_at = now()
            WHERE conversation_read_cursor.last_read_sequence_no < EXCLUDED.last_read_sequence_no
            """, nativeQuery = true)
    int advance(@Param("conversationId") UUID conversationId,
                @Param("userId") UUID userId,
                @Param("messageId") UUID messageId,
                @Param("messageCreatedAt") java.time.Instant messageCreatedAt,
                @Param("sequenceNo") long sequenceNo);
}
