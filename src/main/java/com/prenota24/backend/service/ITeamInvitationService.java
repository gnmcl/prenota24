package com.prenota24.backend.service;

import com.prenota24.backend.dto.CreateInvitationRequest;
import com.prenota24.backend.dto.InvitationInfoResponse;
import com.prenota24.backend.dto.InvitationResponse;

import java.util.List;
import java.util.UUID;

public interface ITeamInvitationService {

    InvitationResponse create(CreateInvitationRequest request, UUID studioId);

    List<InvitationResponse> listByStudio(UUID studioId);

    InvitationInfoResponse getByToken(String token);

    void revoke(UUID invitationId, UUID studioId);
}
