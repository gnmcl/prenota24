package com.prenota24.backend.messaging;

import com.prenota24.backend.domain.MessageKind;

public interface WhatsappGateway {
    SendResult send(DispatchCommand command);

    record DispatchCommand(java.util.UUID messageId, String recipientPhone, MessageKind kind, String text) {}

    record SendResult(Outcome outcome, String providerMessageId, String errorMessage) {
        public static SendResult accepted(String providerMessageId) {
            return new SendResult(Outcome.ACCEPTED, providerMessageId, null);
        }
        public static SendResult failed(String errorMessage) {
            return new SendResult(Outcome.FAILED, null, errorMessage);
        }
        public static SendResult unknown(String errorMessage) {
            return new SendResult(Outcome.UNKNOWN, null, errorMessage);
        }
    }

    enum Outcome { ACCEPTED, FAILED, UNKNOWN }
}
