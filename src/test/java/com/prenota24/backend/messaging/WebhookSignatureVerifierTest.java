package com.prenota24.backend.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class WebhookSignatureVerifierTest {

    @Test
    void verifiesRawBodyHmacWithoutAcceptingMalformedSignatures() {
        var verifier = new WebhookSignatureVerifier("secret");

        assertThat(verifier.isValid("hello", "sha256=88aab3ede8d3adf94d26ab90d3bafd4a2083070c3bcce9c014ee04a443847c0b"))
                .isTrue();
        assertThat(verifier.isValid("hello ", "sha256=88aab3ede8d3adf94d26ab90d3bafd4a2083070c3bcce9c014ee04a443847c0b"))
                .isFalse();
        assertThat(verifier.isValid("hello", "88aab3ede8d3adf94d26ab90d3bafd4a2083070c3bcce9c014ee04a443847c0b"))
                .isFalse();
        assertThat(verifier.isValid("hello", "sha256=xyz"))
                .isFalse();
    }
}
