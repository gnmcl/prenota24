package com.prenota24.backend.controller;

import java.util.UUID;

import com.prenota24.backend.auth.AuthService;
import com.prenota24.backend.dto.AuthUserResponse;
import com.prenota24.backend.dto.ChangeEmailRequest;
import com.prenota24.backend.dto.ChangePasswordRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
@Tag(name = "Account", description = "Gestione credenziali dell'utente autenticato")
public class AccountController {

    private final AuthService authService;

    @PatchMapping("/change-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Cambia password", description = "Richiede la password attuale. Revoca tutti i refresh token sugli altri dispositivi.")
    @ApiResponse(responseCode = "204", description = "Password aggiornata")
    @ApiResponse(responseCode = "401", description = "Password attuale non corretta")
    public void changePassword(@RequestBody @Valid ChangePasswordRequest request,
                               Authentication auth) {
        authService.changePassword(UUID.fromString(auth.getName()), request);
    }

    @PatchMapping("/change-email")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Cambia email", description = "Richiede la password attuale come conferma di identità.")
    @ApiResponse(responseCode = "200", description = "Email aggiornata")
    @ApiResponse(responseCode = "401", description = "Password non corretta")
    @ApiResponse(responseCode = "409", description = "Email già in uso")
    public AuthUserResponse changeEmail(@RequestBody @Valid ChangeEmailRequest request,
                                        Authentication auth) {
        return authService.changeEmail(UUID.fromString(auth.getName()), request);
    }
}
