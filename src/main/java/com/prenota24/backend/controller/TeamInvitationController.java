package com.prenota24.backend.controller;

import com.prenota24.backend.common.AuthHelper;
import com.prenota24.backend.dto.CreateInvitationRequest;
import com.prenota24.backend.dto.InvitationResponse;
import com.prenota24.backend.service.ITeamInvitationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/invitations")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Team Invitations", description = "Gestione inviti per professionisti (solo ADMIN)")
public class TeamInvitationController {

    private final ITeamInvitationService invitationService;
    private final AuthHelper authHelper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crea invito", description = "Genera token valido 7 giorni e revoca eventuali inviti PENDING precedenti per lo stesso professionista")
    public InvitationResponse create(@RequestBody @Valid CreateInvitationRequest request,
                                     Authentication auth) {
        return invitationService.create(request, authHelper.getStudioId(auth));
    }

    @GetMapping
    @Operation(summary = "Lista inviti dello studio")
    public List<InvitationResponse> list(Authentication auth) {
        return invitationService.listByStudio(authHelper.getStudioId(auth));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Revoca invito PENDING")
    public void revoke(@PathVariable UUID id, Authentication auth) {
        invitationService.revoke(id, authHelper.getStudioId(auth));
    }
}