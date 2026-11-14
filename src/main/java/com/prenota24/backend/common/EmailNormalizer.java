package com.prenota24.backend.common;

import java.util.Locale;

/**
 * Forma canonica delle email degli utenti: senza spazi ai lati e in minuscolo.
 * Va usata sia quando si salva sia quando si cerca, altrimenti "Mario@X.it" e "mario@x.it"
 * diventano due account distinti e il login fallisce se si scrive con un'altra capitalizzazione.
 */
public final class EmailNormalizer {

    private EmailNormalizer() {
    }

    public static String normalize(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
