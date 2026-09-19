package com.prenota24.backend.messaging;

import java.util.Optional;
import java.util.regex.Pattern;

public final class PhoneNormalizer {
    private static final Pattern CLIENT_FORMAT = Pattern.compile("^(?:\\+|00)[0-9 .()/-]+$");
    private static final Pattern E164_DIGITS = Pattern.compile("^[1-9][0-9]{7,14}$");

    private PhoneNormalizer() {}

    public static Optional<String> normalizeClientPhone(String phone) {
        if (phone == null) return Optional.empty();
        String trimmed = phone.trim();
        if (!CLIENT_FORMAT.matcher(trimmed).matches()) return Optional.empty();
        String digits = trimmed.replaceAll("[^0-9]", "");
        if (trimmed.startsWith("00")) digits = digits.substring(2);
        return E164_DIGITS.matcher(digits).matches() ? Optional.of(digits) : Optional.empty();
    }

    public static Optional<String> normalizeProviderPhone(String phone) {
        if (phone == null || !phone.chars().allMatch(Character::isDigit)) return Optional.empty();
        return E164_DIGITS.matcher(phone).matches() ? Optional.of(phone) : Optional.empty();
    }
}
