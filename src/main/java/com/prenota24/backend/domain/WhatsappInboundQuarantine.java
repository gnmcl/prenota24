package com.prenota24.backend.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "whatsapp_inbound_quarantine")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WhatsappInboundQuarantine {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "provider_message_id", nullable = false, length = 200)
    private String providerMessageId;
    @Column(name = "destination_phone_number_id", length = 100)
    private String destinationPhoneNumberId;
    @Column(name = "sender_phone", length = 50)
    private String senderPhone;
    @Column(columnDefinition = "TEXT")
    private String text;
    @Column(nullable = false, length = 50)
    private String reason;
    @Column(name = "raw_payload", nullable = false, columnDefinition = "TEXT")
    private String rawPayload;
    @Column(name = "received_at", nullable = false, updatable = false)
    private Instant receivedAt;
    @PrePersist void onCreate() { receivedAt = Instant.now(); }
}
