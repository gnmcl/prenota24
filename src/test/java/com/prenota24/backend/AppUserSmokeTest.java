package com.prenota24.backend;

import com.prenota24.backend.domain.*;
import com.prenota24.backend.repository.AppUserRepository;
import com.prenota24.backend.repository.StudioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class AppUserSmokeTest {

    @Autowired
    private StudioRepository studioRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Test
    void shouldSaveUser() {
        Studio studio = studioRepository.save(
                Studio.builder().name("Studio Test").timezone("Europe/Rome").build()
        );

        AppUser user = AppUser.builder().studio(studio)
                .email("admin@studio.it")
                .passwordHash("hashed")
                .role(UserRole.ADMIN)
                .build();

        AppUser saved = appUserRepository.save(user);

        assertThat(saved.getId()).isNotNull();
    }
}
