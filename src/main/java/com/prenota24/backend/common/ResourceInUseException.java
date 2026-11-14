package com.prenota24.backend.common;

/**
 * Lanciata quando una risorsa non può essere eliminata perché altri dati dipendono da lei
 * (es. un cliente con appuntamenti).
 */
public class ResourceInUseException extends RuntimeException {
    public ResourceInUseException(String message) {
        super(message);
    }
}
