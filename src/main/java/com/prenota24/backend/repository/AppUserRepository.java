package com.prenota24.backend.repository;


import com.prenota24.backend.domain.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AppUserRepository extends JpaRepository<AppUser, UUID> {

    /**
     * Ricerca per email senza distinzione tra maiuscole e minuscole: gli account creati prima
     * della normalizzazione possono avere maiuscole. Passare l'email già normalizzata
     * ({@link com.prenota24.backend.common.EmailNormalizer}).
     */
    Optional<AppUser> findByEmailIgnoreCase(String email);

    Optional<AppUser> findByEmailAndStudioId(String email, UUID studioId);

    /** Utenti di login collegati a un professionista (di norma al massimo uno). */
    List<AppUser> findByProfessionalId(UUID professionalId);
}
