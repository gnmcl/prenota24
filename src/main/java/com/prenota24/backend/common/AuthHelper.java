package com.prenota24.backend.common;

import com.prenota24.backend.domain.AppUser;
import com.prenota24.backend.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AuthHelper {

    private final AppUserRepository appUserRepository;

    public UUID getUserId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }

    public UUID getStudioId(Authentication authentication) {
        return getUser(authentication).getStudio().getId();
    }

    public AppUser getUser(Authentication authentication) {
        UUID userId = getUserId(authentication);
        return appUserRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Utente non trovato"));
    }
}
