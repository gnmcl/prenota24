package com.prenota24.backend.messaging;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class WebhookSignatureVerifier {

    private static final String PREFIX = "sha256=";
    private final byte[] secret;

    public WebhookSignatureVerifier(String secret) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    public boolean isValid(String rawBody, String signature) {
        if (signature == null || !signature.startsWith(PREFIX)) {
            return false;
        }
        try {
            var mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            var expected = mac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8));
            var supplied = HexFormat.of().parseHex(signature.substring(PREFIX.length()));
            return MessageDigest.isEqual(expected, supplied);
        } catch (IllegalArgumentException | java.security.GeneralSecurityException ex) {
            return false;
        }
    }
}
