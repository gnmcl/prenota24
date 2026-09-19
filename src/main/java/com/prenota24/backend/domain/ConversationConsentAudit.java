package com.prenota24.backend.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "conversation_consent_audit")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ConversationConsentAudit {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;
    @Column(nullable = false)
    private boolean enabled;
    @Column(name = "recipient_phone", length = 50)
    private String recipientPhone;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @PrePersist void onCreate() { createdAt = Instant.now(); }
}
