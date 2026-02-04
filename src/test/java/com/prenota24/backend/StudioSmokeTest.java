package com.prenota24.backend;

import com.prenota24.backend.domain.Studio;
import com.prenota24.backend.repository.StudioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class StudioSmokeTest {

    @Autowired
    private StudioRepository studioRepository;

    @Test
    void shouldSaveStudio() {
        Studio studio = Studio.builder()
                .name("Test Studio")
                .email("studio@prova.com")
                .phone("1234567890")
                .timezone("Europe/Rome")
                .build();

        Studio saved = studioRepository.save(studio);
        assertThat(saved.getId()).isNotNull();
    }
}
