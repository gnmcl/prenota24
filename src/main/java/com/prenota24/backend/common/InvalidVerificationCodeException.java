package com.prenota24.backend.common;

/** Codice di verifica email errato, scaduto o invalidato dopo troppi tentativi. */
public class InvalidVerificationCodeException extends RuntimeException {
    public InvalidVerificationCodeException(String message) {
        super(message);
    }
}
