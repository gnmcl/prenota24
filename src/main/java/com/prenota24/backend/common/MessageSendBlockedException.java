package com.prenota24.backend.common;

public class MessageSendBlockedException extends RuntimeException {
    private final String reason;

    public MessageSendBlockedException(String reason) {
        super("Invio WhatsApp bloccato: " + reason);
        this.reason = reason;
    }

    public String getReason() { return reason; }
}
