package com.prenota24.backend.common;

/**
 * Lanciata quando una prenotazione pubblica supera il numero massimo di richieste
 * in attesa consentite per lo stesso cliente.
 */
public class BookingLimitExceededException extends RuntimeException {
    public BookingLimitExceededException(String message) {
        super(message);
    }
}
