package com.prenota24.backend.repository;

import com.prenota24.backend.domain.WhatsappInboundQuarantine;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WhatsappInboundQuarantineRepository extends JpaRepository<WhatsappInboundQuarantine, UUID> {
    boolean existsByProviderMessageId(String providerMessageId);

    @Modifying
    @Query(value = """
            INSERT INTO whatsapp_inbound_quarantine
                (id, provider_message_id, destination_phone_number_id, sender_phone, text, reason, raw_payload, received_at)
            VALUES
                (gen_random_uuid(), :providerMessageId, :destinationId, :senderPhone, :text, :reason, :rawPayload, now())
            ON CONFLICT (provider_message_id) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("providerMessageId") String providerMessageId,
                       @Param("destinationId") String destinationId,
                       @Param("senderPhone") String senderPhone,
                       @Param("text") String text,
                       @Param("reason") String reason,
                       @Param("rawPayload") String rawPayload);
}
