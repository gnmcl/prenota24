package com.prenota24.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * @param name  nuovo nome visualizzato
 * @param email non più modificabile da questo endpoint, perché il cambio email richiede la
 *              password: se valorizzato la richiesta viene rifiutata. Usare
 *              {@code /api/account/change-email}.
 */
public record UpdateAppUserRequest(
        @Size(max = 200) String name,
        @Deprecated @Email String email
) {}
