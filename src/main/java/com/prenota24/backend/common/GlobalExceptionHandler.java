package com.prenota24.backend.common;

import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.prenota24.backend.dto.ErrorResponse;

import jakarta.servlet.http.HttpServletRequest;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /* ---------- AUTH ---------- */

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    public  ResponseEntity<ErrorResponse> handleEmailAlreadyRegistered(
            EmailAlreadyRegisteredException ex,
            HttpServletRequest request
    ){
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(
                        HttpStatus.CONFLICT.value(),
                        "EMAIL_ALREADY_REGISTERED",
                        "Email già registrata, si prega di modificare l'email",
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(EmailNotVerifiedException.class)
    public  ResponseEntity<ErrorResponse> handleEmailNotVerified(
            EmailNotVerifiedException ex,
            HttpServletRequest request
    ){
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of(
                        HttpStatus.FORBIDDEN.value(),
                        "EMAIL_NOT_VERIFIED",
                        "Email non verificata, si prega di verificare l'email",
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(
            BadCredentialsException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.of(
                        HttpStatus.UNAUTHORIZED.value(),
                        "INVALID_CREDENTIALS",
                        "Invalid email or password",
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of(
                        HttpStatus.FORBIDDEN.value(),
                        "ACCESS_DENIED",
                        "You do not have permission to access this resource",
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(JwtAuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleJwtAuthentication(
            JwtAuthenticationException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.of(
                        HttpStatus.UNAUTHORIZED.value(),
                        "UNAUTHORIZED",
                        "Authentication failed: " + ex.getMessage(),
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(InvalidPasswordResetCodeException.class)
    public ResponseEntity<ErrorResponse> handleInvalidPasswordResetCode(
            InvalidPasswordResetCodeException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT)
                .body(ErrorResponse.of(
                        HttpStatus.UNPROCESSABLE_CONTENT.value(),
                        "INVALID_RESET_CODE",
                        ex.getMessage(),
                        request.getRequestURI()
                ));
    }

    /* ---------- VALIDATION ---------- */

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining(", "));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(
                        HttpStatus.BAD_REQUEST.value(),
                        "VALIDATION_ERROR",
                        message,
                        request.getRequestURI()
                ));
    }

    /* ---------- BUSINESS ERRORS ---------- */

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFound(
            EntityNotFoundException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(
                        HttpStatus.NOT_FOUND.value(),
                        "NOT_FOUND",
                        ex.getMessage(),
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(IllegalStateTransitionException.class)
    public ResponseEntity<ErrorResponse> handleIllegalTransition(
            IllegalStateTransitionException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT)
                .body(ErrorResponse.of(
                        HttpStatus.UNPROCESSABLE_CONTENT.value(),
                        "INVALID_STATE_TRANSITION",
                        ex.getMessage(),
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(SlotNotAvailableException.class)
    public ResponseEntity<ErrorResponse> handleSlotNotAvailable(
            SlotNotAvailableException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(
                        HttpStatus.CONFLICT.value(),
                        "SLOT_NOT_AVAILABLE",
                        ex.getMessage(),
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleConflict(
            DataIntegrityViolationException ex,
            HttpServletRequest request
    ) {
        String message = "Data conflict";
        String code = "DATA_CONFLICT";

        if (ex.getMessage() != null && ex.getMessage().contains("no_overlapping")) {
            message = "L'orario selezionato si sovrappone con un altro appuntamento";
            code = "APPOINTMENT_OVERLAP";
        }

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(
                        HttpStatus.CONFLICT.value(),
                        code,
                        message,
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(
                        400,
                        "BAD_REQUEST",
                        ex.getMessage(),
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(
            IllegalStateException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(
                        400,
                        "BAD_REQUEST",
                        ex.getMessage(),
                        request.getRequestURI()
                ));
    }

    /* ---------- FALLBACK ---------- */

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(
            Exception ex,
            HttpServletRequest request
    ) {
        logger.error("Unhandled exception on {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(
                        500,
                        "INTERNAL_ERROR",
                        "Unexpected error occurred",
                        request.getRequestURI()
                ));
    }
}
