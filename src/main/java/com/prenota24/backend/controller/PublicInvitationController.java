package com.prenota24.backend.controller;

import com.prenota24.backend.dto.InvitationInfoResponse;
import com.prenota24.backend.service.ITeamInvitationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/invitations")
@RequiredArgsConstructor
@Tag(name = "Public Invitations", description = "Info pubblica su un invito team tramite token")
@SecurityRequirements
public class PublicInvitationController {

    private final ITeamInvitationService invitationService;

    @GetMapping("/{token}")
    @Operation(summary = "Dettaglio invito tramite token")
    public InvitationInfoResponse getByToken(@PathVariable String token) {
        return invitationService.getByToken(token);
    }
}
