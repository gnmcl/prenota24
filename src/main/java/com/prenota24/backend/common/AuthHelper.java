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

    public UUID getProfessionalId(Authentication authentication) {
        var user = getUser(authentication);
        if (user.getProfessional() == null) {
            throw new IllegalStateException("L'utente non è associato a un professionista");
        }
        return user.getProfessional().getId();
    }

    public AppUser getUser(Authentication authentication) {
        UUID userId = getUserId(authentication);
        var user = appUserRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Utente non trovato"));
        // L'access token resta valido fino alla scadenza anche dopo la disattivazione:
        // il controllo qui chiude subito l'accesso (401, e il refresh poi fallisce).
        if (!user.isActive()) {
            throw new JwtAuthenticationException("Account disattivato");
        }
        return user;
    }
}
