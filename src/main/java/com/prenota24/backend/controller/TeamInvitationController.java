package com.prenota24.backend.controller;

import com.prenota24.backend.common.AuthHelper;
import com.prenota24.backend.dto.CreateInvitationRequest;
import com.prenota24.backend.dto.InvitationResponse;
import com.prenota24.backend.service.ITeamInvitationService;
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
public class TeamInvitationController {

    private final ITeamInvitationService invitationService;
    private final AuthHelper authHelper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InvitationResponse create(@RequestBody @Valid CreateInvitationRequest request,
                                     Authentication auth) {
        return invitationService.create(request, authHelper.getStudioId(auth));
    }

    @GetMapping
    public List<InvitationResponse> list(Authentication auth) {
        return invitationService.listByStudio(authHelper.getStudioId(auth));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(@PathVariable UUID id, Authentication auth) {
        invitationService.revoke(id, authHelper.getStudioId(auth));
    }
}
