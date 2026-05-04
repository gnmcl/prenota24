package com.prenota24.backend.common;

public class InvalidPasswordResetCodeException extends RuntimeException {
    public InvalidPasswordResetCodeException(String message) {
        super(message);
    }
}

