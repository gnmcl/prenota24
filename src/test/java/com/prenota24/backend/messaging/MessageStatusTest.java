package com.prenota24.backend.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import com.prenota24.backend.domain.MessageStatus;
import org.junit.jupiter.api.Test;

class MessageStatusTest {

    @Test
    void deliveryCallbacksAdvanceWithoutDowngrading() {
        assertThat(MessageStatus.ACCEPTED.canAdvanceTo(MessageStatus.DELIVERED)).isTrue();
        assertThat(MessageStatus.DELIVERED.canAdvanceTo(MessageStatus.READ)).isTrue();
        assertThat(MessageStatus.READ.canAdvanceTo(MessageStatus.ACCEPTED)).isFalse();
        assertThat(MessageStatus.DELIVERED.canAdvanceTo(MessageStatus.ACCEPTED)).isFalse();
        assertThat(MessageStatus.UNKNOWN.canAdvanceTo(MessageStatus.DELIVERED)).isTrue();
        assertThat(MessageStatus.FAILED.canAdvanceTo(MessageStatus.ACCEPTED)).isFalse();
    }
}
