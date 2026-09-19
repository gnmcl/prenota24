package com.prenota24.backend.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "conversation_read_cursor")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ConversationReadCursor {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "last_read_message_id", nullable = false)
    private ConversationMessage lastReadMessage;
    @Column(name = "last_read_message_created_at", nullable = false)
    private Instant lastReadMessageCreatedAt;
    @Column(name = "last_read_sequence_no", nullable = false)
    private Long lastReadSequenceNo;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @PrePersist @PreUpdate void touch() { updatedAt = Instant.now(); }
}
