package com.prenota24.backend.dto;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Strongly-typed representation of an outbound email notification.
 * Converts to a Map for JSON persistence in the Notification entity.
 *
 * <p>The {@code html} field carries the branded HTML version of the email body.
 * When present it is sent as the HTML part; {@code body} is always sent as plain-text fallback.
 * Existing callers that use the 4-arg constructor are unaffected (html defaults to null).
 */
public record EmailPayload(
        String recipientEmail,
        String recipientName,
        String subject,
        String body,
        String html   // nullable — rich HTML; null → body used as fallback
) {
    /** Convenience constructor for backward-compatible 4-arg callers (html = null). */
    public EmailPayload(String recipientEmail, String recipientName, String subject, String body) {
        this(recipientEmail, recipientName, subject, body, null);
    }

    public Map<String, Object> toMap() {
        var map = new HashMap<String, Object>();
        map.put("recipientEmail", recipientEmail);
        map.put("recipientName",  recipientName);
        map.put("subject",        subject);
        map.put("body",           body);
        if (html != null) {
            map.put("html", html);
        }
        return Collections.unmodifiableMap(map);
    }
}

