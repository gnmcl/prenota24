package com.prenota24.backend.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "conversation", uniqueConstraints = @UniqueConstraint(name = "uq_conversation_studio_client", columnNames = {"studio_id", "client_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Conversation {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "studio_id", nullable = false)
    private Studio studio;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Column(name = "whatsapp_opt_in", nullable = false)
    @Builder.Default
    private boolean whatsappOptIn = false;

    @Column(name = "whatsapp_opt_in_phone", length = 50)
    private String whatsappOptInPhone;

    @Column(name = "last_inbound_at")
    private Instant lastInboundAt;

    @Column(name = "last_inbound_phone", length = 50)
    private String lastInboundPhone;

    @Column(name = "last_message_at")
    private Instant lastMessageAt;

    @Column(name = "last_message_sequence_no")
    private Long lastMessageSequenceNo;

    @Column(name = "last_message_preview", length = 240)
    private String lastMessagePreview;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist void onCreate() { var now = Instant.now(); createdAt = now; updatedAt = now; }
    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }
}
