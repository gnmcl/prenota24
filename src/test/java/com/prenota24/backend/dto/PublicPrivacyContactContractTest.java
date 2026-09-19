package com.prenota24.backend.dto;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

class PublicPrivacyContactContractTest {

    @Test
    void publicStudioExposesTheContactForPrivacyRequests() {
        assertTrue(componentNames(StudioPublicResponse.class).contains("privacyContactEmail"));
    }

    @Test
    void publicEventIdentifiesTheStudioAndItsPrivacyContact() {
        Set<String> components = componentNames(EventResponse.class);

        assertTrue(components.contains("studioName"));
        assertTrue(components.contains("studioPrivacyContactEmail"));
    }

    private Set<String> componentNames(Class<?> recordType) {
        return Arrays.stream(recordType.getRecordComponents())
                .map(RecordComponent::getName)
                .collect(Collectors.toSet());
    }
}
