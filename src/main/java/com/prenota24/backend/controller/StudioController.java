package com.prenota24.backend.controller;

import com.prenota24.backend.common.AuthHelper;
import com.prenota24.backend.dto.CreateStudioRequest;
import com.prenota24.backend.dto.EditStudioProfileRequest;
import com.prenota24.backend.dto.StudioResponse;
import com.prenota24.backend.service.IStudioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/studios")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Studios", description = "Gestione studio (solo ADMIN)")
public class StudioController {

    private final IStudioService studioService;
    private final AuthHelper authHelper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crea studio")
    public StudioResponse create(@RequestBody @Valid CreateStudioRequest request) {
        return studioService.create(request);
    }

    @PatchMapping
    @Operation(summary = "Modifica profilo studio", description = "Aggiorna solo i campi non-null (partial update)")
    public StudioResponse editStudioProfile(@RequestBody @Valid EditStudioProfileRequest request,
                                            Authentication auth) {
        return studioService.editStudioProfile(authHelper.getStudioId(auth), request);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Dettaglio studio")
    public StudioResponse getById(@PathVariable("id") UUID id) {
        return studioService.getById(id);
    }
}
