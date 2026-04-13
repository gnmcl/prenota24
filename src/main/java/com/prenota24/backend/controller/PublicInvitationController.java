package com.prenota24.backend.controller;

import com.prenota24.backend.dto.InvitationInfoResponse;
import com.prenota24.backend.service.ITeamInvitationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/invitations")
@RequiredArgsConstructor
public class PublicInvitationController {

    private final ITeamInvitationService invitationService;

    @GetMapping("/{token}")
    public InvitationInfoResponse getByToken(@PathVariable String token) {
        return invitationService.getByToken(token);
    }
}
