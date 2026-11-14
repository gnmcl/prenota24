package com.prenota24.backend.service.impl;

import com.prenota24.backend.common.EmailNormalizer;
import com.prenota24.backend.common.EntityNotFoundException;
import com.prenota24.backend.domain.InvitationStatus;
import com.prenota24.backend.domain.TeamInvitation;
import com.prenota24.backend.dto.CreateInvitationRequest;
import com.prenota24.backend.dto.InvitationInfoResponse;
import com.prenota24.backend.dto.InvitationResponse;
import com.prenota24.backend.repository.ProfessionalRepository;
import com.prenota24.backend.repository.StudioRepository;
import com.prenota24.backend.repository.TeamInvitationRepository;
import com.prenota24.backend.service.ITeamInvitationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class TeamInvitationService implements ITeamInvitationService {

    private static final int EXPIRATION_DAYS = 7;

    private final TeamInvitationRepository invitationRepository;
    private final ProfessionalRepository professionalRepository;
    private final StudioRepository studioRepository;
    /** URL del frontend (app.frontend-url): prima il link era fisso a "prenota24.com", senza https. */
    private final String frontendUrl;

    public TeamInvitationService(TeamInvitationRepository invitationRepository,
                                 ProfessionalRepository professionalRepository,
                                 StudioRepository studioRepository,
                                 @Value("${app.frontend-url}") String frontendUrl) {
        this.invitationRepository = invitationRepository;
        this.professionalRepository = professionalRepository;
        this.studioRepository = studioRepository;
        this.frontendUrl = frontendUrl.endsWith("/")
                ? frontendUrl.substring(0, frontendUrl.length() - 1)
                : frontendUrl;
    }

    @Override
    @Transactional
    public InvitationResponse create(CreateInvitationRequest request, UUID studioId) {
        var studio = studioRepository.findById(studioId)
                .orElseThrow(() -> new EntityNotFoundException("Studio non trovato"));

        var professional = professionalRepository.findByIdAndStudioId(request.professionalId(), studioId)
                .orElseThrow(() -> new EntityNotFoundException("Professionista non trovato nello studio"));

        // Revoke any existing pending invitation for this professional
        invitationRepository.findByProfessionalIdAndStatus(professional.getId(), InvitationStatus.PENDING)
                .ifPresent(existing -> {
                    existing.setStatus(InvitationStatus.REVOKED);
                    invitationRepository.save(existing);
                });

        var invitation = TeamInvitation.builder()
                .studio(studio)
                .professional(professional)
                .email(EmailNormalizer.normalize(request.email()))
                .token(UUID.randomUUID().toString().replace("-", ""))
                .status(InvitationStatus.PENDING)
                .expiresAt(Instant.now().plus(EXPIRATION_DAYS, ChronoUnit.DAYS))
                .build();

        invitation = invitationRepository.save(invitation);
        return toResponse(invitation);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvitationResponse> listByStudio(UUID studioId) {
        return invitationRepository.findByStudioIdOrderByCreatedAtDesc(studioId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public InvitationInfoResponse getByToken(String token) {
        var invitation = invitationRepository.findByToken(token)
                .orElseThrow(() -> new EntityNotFoundException("Invito non trovato"));

        // Check expiration
        String status = invitation.getStatus().name();
        if (invitation.getStatus() == InvitationStatus.PENDING
                && invitation.getExpiresAt().isBefore(Instant.now())) {
            status = "EXPIRED";
        }

        var pro = invitation.getProfessional();
        return new InvitationInfoResponse(
                pro.getFirstName() + " " + pro.getLastName(),
                invitation.getStudio().getName(),
                invitation.getEmail(),
                status
        );
    }

    @Override
    @Transactional
    public void revoke(UUID invitationId, UUID studioId) {
        var invitation = invitationRepository.findByIdAndStudioId(invitationId, studioId)
                .orElseThrow(() -> new EntityNotFoundException("Invito non trovato"));

        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new IllegalStateException("Solo gli inviti in attesa possono essere revocati");
        }

        invitation.setStatus(InvitationStatus.REVOKED);
        invitationRepository.save(invitation);
    }

    // ── Helpers ──────────────────────────────────────

    private InvitationResponse toResponse(TeamInvitation inv) {
        var pro = inv.getProfessional();
        return new InvitationResponse(
                inv.getId(),
                pro.getId(),
                pro.getFirstName() + " " + pro.getLastName(),
                inv.getEmail(),
                inv.getStatus().name(),
                frontendUrl + "/invito/" + inv.getToken(),
                inv.getExpiresAt(),
                inv.getCreatedAt()
        );
    }
}
