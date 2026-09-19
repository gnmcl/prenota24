package com.prenota24.backend.messaging;

import com.prenota24.backend.dto.SendMessageRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class MessageRequestFingerprint {
    private MessageRequestFingerprint() {}

    public static String calculate(SendMessageRequest request) {
        String canonical = request.kind().name() + "\n"
                + (request.appointmentId() == null ? "" : request.appointmentId()) + "\n"
                + (request.text() == null ? "" : request.text());
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 non disponibile", ex);
        }
    }
}
