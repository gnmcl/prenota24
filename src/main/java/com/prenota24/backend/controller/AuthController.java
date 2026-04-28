package com.prenota24.backend.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.prenota24.backend.auth.AuthService;
import com.prenota24.backend.dto.AcceptInvitationRequest;
import com.prenota24.backend.dto.LoginRequest;
import com.prenota24.backend.dto.LoginResponse;
import com.prenota24.backend.dto.RefreshTokenRequest;
import com.prenota24.backend.dto.RegisterRequest;
import com.prenota24.backend.dto.RegisterResponse;
import com.prenota24.backend.dto.ResendVerificationRequest;
import com.prenota24.backend.dto.VerifyEmailRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Registrazione, login e accettazione inviti")
@SecurityRequirements
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Login", description = "Autentica con email e password, restituisce JWT + refresh token")
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

    @PostMapping("/refresh")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Refresh token", description = "Ruota il refresh token e restituisce nuovo access + refresh token")
    @ApiResponse(responseCode = "200", description = "Token rinnovati")
    @ApiResponse(responseCode = "401", description = "Refresh token non valido o scaduto")
    public LoginResponse refresh(@RequestBody @Valid RefreshTokenRequest request) {
        return authService.refreshToken(request);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Logout", description = "Revoca tutti i refresh token dell'utente")
    @ApiResponse(responseCode = "204", description = "Logout completato")
    public void logout() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof String userId) {
            authService.logout(UUID.fromString(userId));
        }
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
