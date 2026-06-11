package com.prenota24.backend.service.impl.templates;

import com.prenota24.backend.domain.Appointment;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Shared formatting utilities for notification templates.
 * Formats dates using the Studio's configured timezone.
 */
final class TemplateUtils {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.ITALIAN);

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm", Locale.ITALIAN);

    private TemplateUtils() {}

    static String formatDate(Instant instant, String timezone) {
        var zone = resolveZone(timezone);
        return instant.atZone(zone).format(DATE_FORMATTER);
    }

    static String formatTime(Instant instant, String timezone) {
        var zone = resolveZone(timezone);
        return instant.atZone(zone).format(TIME_FORMATTER);
    }

    static String formatDatetime(Instant instant, String timezone) {
        return formatDate(instant, timezone) + " alle " + formatTime(instant, timezone);
    }

    static String timezone(Appointment apt) {
        var tz = apt.getStudio().getTimezone();
        return tz != null ? tz : "Europe/Rome";
    }

    private static ZoneId resolveZone(String timezone) {
        return ZoneId.of(timezone != null ? timezone : "Europe/Rome");
    }
}

