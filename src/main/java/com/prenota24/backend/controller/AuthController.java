package com.prenota24.backend.controller;

import com.prenota24.backend.auth.AuthService;
import com.prenota24.backend.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Registrazione, login e accettazione inviti")
@SecurityRequirements
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Login", description = "Autentica con email e password, restituisce JWT")
    @ApiResponse(responseCode = "200", description = "Login riuscito")
    @ApiResponse(responseCode = "401", description = "Credenziali non valide")
    public LoginResponse login(@RequestBody @Valid LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registrazione studio", description = "Crea nuovo studio + utente ADMIN e invia codice di verifica email")
    @ApiResponse(responseCode = "201", description = "Registrazione completata, codice inviato")
    public RegisterResponse register(@RequestBody @Valid RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/verify-email")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Verifica email", description = "Verifica il codice inviato via email e restituisce JWT")
    @ApiResponse(responseCode = "200", description = "Email verificata")
    @ApiResponse(responseCode = "400", description = "Codice non valido o scaduto")
    public LoginResponse verifyEmail(@RequestBody @Valid VerifyEmailRequest request) {
        return authService.verifyEmail(request);
    }

    @PostMapping("/resend-verification")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Reinvia codice verifica", description = "Genera e invia un nuovo codice di verifica")
    @ApiResponse(responseCode = "200", description = "Codice reinviato")
    public void resendVerification(@RequestBody @Valid ResendVerificationRequest request) {
        authService.resendVerificationCode(request);
    }

    @PostMapping("/accept-invitation")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Accetta invito team", description = "Crea account PROFESSIONAL tramite token invito")
    @ApiResponse(responseCode = "201", description = "Account creato")
    @ApiResponse(responseCode = "400", description = "Token non valido o scaduto")
    public LoginResponse acceptInvitation(@RequestBody @Valid AcceptInvitationRequest request) {
        return authService.acceptInvitation(request);
    }
}
