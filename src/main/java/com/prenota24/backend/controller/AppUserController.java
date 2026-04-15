package com.prenota24.backend.controller;

import com.prenota24.backend.dto.AppUserResponse;
import com.prenota24.backend.dto.CreateAppUserRequest;
import com.prenota24.backend.service.IAppUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Gestione utenti applicativi")
public class AppUserController {

    private final IAppUserService appUserService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crea utente (solo ADMIN)")
    public AppUserResponse create(@RequestBody @Valid CreateAppUserRequest request) {
        return appUserService.create(request);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Dettaglio utente")
    public AppUserResponse getById(@PathVariable @NotNull UUID id) {
        return appUserService.getById(id);
    }
}
