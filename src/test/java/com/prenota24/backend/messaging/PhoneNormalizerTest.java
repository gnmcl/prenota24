package com.prenota24.backend.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PhoneNormalizerTest {
    @Test
    void clientNumbersRequireExplicitInternationalPrefix() {
        assertThat(PhoneNormalizer.normalizeClientPhone("+39 333 123 4567")).contains("393331234567");
        assertThat(PhoneNormalizer.normalizeClientPhone("0039-333-123-4567")).contains("393331234567");
        assertThat(PhoneNormalizer.normalizeClientPhone("3331234567")).isEmpty();
        assertThat(PhoneNormalizer.normalizeClientPhone("+0123456789")).isEmpty();
        assertThat(PhoneNormalizer.normalizeClientPhone("+39 abc")).isEmpty();
    }

    @Test
    void providerNumbersAreAlreadyInternationalDigits() {
        assertThat(PhoneNormalizer.normalizeProviderPhone("393331234567")).contains("393331234567");
        assertThat(PhoneNormalizer.normalizeProviderPhone("3331234567")).contains("3331234567");
        assertThat(PhoneNormalizer.normalizeProviderPhone("+393331234567")).isEmpty();
    }
}
