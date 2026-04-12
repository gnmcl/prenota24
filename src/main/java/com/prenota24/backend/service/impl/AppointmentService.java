package com.prenota24.backend.service.impl;

import com.prenota24.backend.common.EntityNotFoundException;
import com.prenota24.backend.common.SlotNotAvailableException;
import com.prenota24.backend.domain.*;
import com.prenota24.backend.dto.*;
import com.prenota24.backend.repository.*;
import com.prenota24.backend.service.IAppointmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AppointmentService implements IAppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final ProfessionalRepository professionalRepository;
    private final ClientRepository clientRepository;
    private final ServiceTypeRepository serviceTypeRepository;
    private final StudioRepository studioRepository;
    private final AppointmentStateMachine stateMachine;

    @Override
    @Transactional
    public AppointmentResponse create(CreateAppointmentRequest request, UUID studioId) {
        var studio = studioRepository.findById(studioId)
                .orElseThrow(() -> new EntityNotFoundException("Studio non trovato"));

        var professional = professionalRepository.findByIdAndStudioId(request.professionalId(), studioId)
                .orElseThrow(() -> new EntityNotFoundException("Professionista non trovato"));

        var client = clientRepository.findByIdAndStudioId(request.clientId(), studioId)
                .orElseThrow(() -> new EntityNotFoundException("Cliente non trovato"));

        if (request.endDatetime().isBefore(request.startDatetime()) ||
                request.endDatetime().equals(request.startDatetime())) {
            throw new IllegalArgumentException("L'orario di fine deve essere successivo a quello di inizio");
        }

        var builder = Appointment.builder()
                .studio(studio)
                .professional(professional)
                .client(client)
                .startDatetime(request.startDatetime())
                .endDatetime(request.endDatetime())
                .notes(request.notes())
                .token(UUID.randomUUID().toString().replace("-", ""))
                .status(request.confirmImmediately() ? AppointmentStatus.CONFIRMED : AppointmentStatus.REQUESTED);

        if (request.serviceTypeId() != null) {
            var serviceType = serviceTypeRepository.findByIdAndStudioId(request.serviceTypeId(), studioId)
                    .orElseThrow(() -> new EntityNotFoundException("Tipo di servizio non trovato"));
            builder.serviceType(serviceType);
        }

        var appointment = appointmentRepository.save(builder.build());
        return toResponse(appointment);
    }

    @Override
    @Transactional(readOnly = true)
    public AppointmentResponse getById(UUID id, UUID studioId) {
        return toResponse(findByIdAndStudio(id, studioId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AppointmentResponse> list(UUID studioId, Pageable pageable) {
        return appointmentRepository.findByStudioId(studioId, pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional
    public AppointmentResponse update(UUID id, UpdateAppointmentRequest request, UUID studioId) {
        var appointment = findByIdAndStudio(id, studioId);

        if (request.notes() != null) appointment.setNotes(request.notes());

        if (request.serviceTypeId() != null) {
            var serviceType = serviceTypeRepository.findByIdAndStudioId(request.serviceTypeId(), studioId)
                    .orElseThrow(() -> new EntityNotFoundException("Tipo di servizio non trovato"));
            appointment.setServiceType(serviceType);
        }

        appointment = appointmentRepository.save(appointment);
        return toResponse(appointment);
    }

    @Override
    @Transactional
    public AppointmentResponse confirm(UUID id, UUID studioId) {
        var appointment = findByIdAndStudio(id, studioId);
        appointment.setStatus(stateMachine.transition(appointment.getStatus(), AppointmentAction.CONFIRM));
        appointment = appointmentRepository.save(appointment);
        return toResponse(appointment);
    }

    @Override
    @Transactional
    public AppointmentResponse cancel(UUID id, CancelAppointmentRequest request, CancelledBy cancelledBy, UUID studioId) {
        var appointment = findByIdAndStudio(id, studioId);
        appointment.setStatus(stateMachine.transition(appointment.getStatus(), AppointmentAction.CANCEL));
        appointment.setCancellationReason(request != null ? request.reason() : null);
        appointment.setCancelledBy(cancelledBy);
        appointment = appointmentRepository.save(appointment);
        return toResponse(appointment);
    }

    @Override
    @Transactional
    public AppointmentResponse complete(UUID id, UUID studioId) {
        var appointment = findByIdAndStudio(id, studioId);
        appointment.setStatus(stateMachine.transition(appointment.getStatus(), AppointmentAction.COMPLETE));
        appointment = appointmentRepository.save(appointment);
        return toResponse(appointment);
    }

    @Override
    @Transactional
    public AppointmentResponse noShow(UUID id, UUID studioId) {
        var appointment = findByIdAndStudio(id, studioId);
        appointment.setStatus(stateMachine.transition(appointment.getStatus(), AppointmentAction.NO_SHOW));
        appointment = appointmentRepository.save(appointment);
        return toResponse(appointment);
    }

    @Override
    @Transactional
    public AppointmentResponse proposeNewTime(UUID id, ProposeNewTimeRequest request, UUID studioId) {
        var appointment = findByIdAndStudio(id, studioId);

        // Check conflicts on proposed time
        long conflicts = appointmentRepository.countConflictingAppointments(
                appointment.getProfessional().getId(),
                request.proposedStart(),
                request.proposedEnd(),
                appointment.getId()
        );

        if (conflicts > 0) {
            throw new SlotNotAvailableException("L'orario proposto si sovrappone con un altro appuntamento");
        }

        appointment.setStatus(stateMachine.transition(appointment.getStatus(), AppointmentAction.PROPOSE_NEW_TIME));
        appointment.setProposedStart(request.proposedStart());
        appointment.setProposedEnd(request.proposedEnd());
        appointment = appointmentRepository.save(appointment);
        return toResponse(appointment);
    }

    @Override
    @Transactional
    public AppointmentResponse acceptProposal(String token) {
        var appointment = findByToken(token);
        appointment.setStatus(stateMachine.transition(appointment.getStatus(), AppointmentAction.ACCEPT_PROPOSAL));

        // Swap proposed times into actual times
        appointment.setStartDatetime(appointment.getProposedStart());
        appointment.setEndDatetime(appointment.getProposedEnd());
        appointment.setProposedStart(null);
        appointment.setProposedEnd(null);

        appointment = appointmentRepository.save(appointment);
        return toResponse(appointment);
    }

    @Override
    @Transactional
    public AppointmentResponse rejectProposal(String token) {
        var appointment = findByToken(token);
        appointment.setStatus(stateMachine.transition(appointment.getStatus(), AppointmentAction.REJECT_PROPOSAL));
        appointment.setProposedStart(null);
        appointment.setProposedEnd(null);
        appointment = appointmentRepository.save(appointment);
        return toResponse(appointment);
    }

    @Override
    @Transactional(readOnly = true)
    public AppointmentResponse getByToken(String token) {
        return toResponse(findByToken(token));
    }

    // ── Helpers ──────────────────────────────────────

    private Appointment findByIdAndStudio(UUID id, UUID studioId) {
        return appointmentRepository.findByIdAndStudioId(id, studioId)
                .orElseThrow(() -> new EntityNotFoundException("Appuntamento non trovato"));
    }

    private Appointment findByToken(String token) {
        return appointmentRepository.findByToken(token)
                .orElseThrow(() -> new EntityNotFoundException("Appuntamento non trovato"));
    }

    private AppointmentResponse toResponse(Appointment a) {
        return new AppointmentResponse(
                a.getId(),
                a.getStudio().getId(),
                a.getProfessional().getId(),
                a.getProfessional().getFirstName() + " " + a.getProfessional().getLastName(),
                a.getClient().getId(),
                a.getClient().getFirstName() + " " + a.getClient().getLastName(),
                a.getServiceType() != null ? a.getServiceType().getId() : null,
                a.getServiceType() != null ? a.getServiceType().getName() : null,
                a.getStartDatetime(),
                a.getEndDatetime(),
                a.getStatus(),
                a.getNotes(),
                a.getProposedStart(),
                a.getProposedEnd(),
                a.getCancellationReason(),
                a.getCancelledBy(),
                a.getToken(),
                a.getCreatedAt(),
                a.getUpdatedAt()
        );
    }
}
