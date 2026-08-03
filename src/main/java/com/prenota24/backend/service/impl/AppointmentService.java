package com.prenota24.backend.service.impl;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.prenota24.backend.common.EntityNotFoundException;
import com.prenota24.backend.common.SlotNotAvailableException;
import com.prenota24.backend.domain.Appointment;
import com.prenota24.backend.domain.AppointmentAction;
import com.prenota24.backend.domain.AppointmentCapacityLevel;
import com.prenota24.backend.domain.AppointmentStatus;
import com.prenota24.backend.domain.CancelledBy;
import com.prenota24.backend.domain.Client;
import com.prenota24.backend.domain.Professional;
import com.prenota24.backend.domain.ServiceType;
import com.prenota24.backend.domain.Studio;
import com.prenota24.backend.dto.AcceptProposalRequest;
import com.prenota24.backend.dto.AppointmentResponse;
import com.prenota24.backend.dto.CancelAppointmentRequest;
import com.prenota24.backend.dto.CreateAppointmentRequest;
import com.prenota24.backend.dto.DayAppointmentCountResponse;
import com.prenota24.backend.dto.ProposeNewTimeRequest;
import com.prenota24.backend.dto.UpdateAppointmentRequest;
import com.prenota24.backend.repository.AppointmentRepository;
import com.prenota24.backend.repository.ClientRepository;
import com.prenota24.backend.repository.ProfessionalRepository;
import com.prenota24.backend.repository.ServiceTypeRepository;
import com.prenota24.backend.repository.StudioRepository;
import com.prenota24.backend.service.IAppointmentService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AppointmentService implements IAppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final ProfessionalRepository professionalRepository;
    private final ClientRepository clientRepository;
    private final ServiceTypeRepository serviceTypeRepository;
    private final StudioRepository studioRepository;
    private final AppointmentStateMachine stateMachine;

    private final NotificationService notificationService;

    @Override
    @Transactional
    public AppointmentResponse create(CreateAppointmentRequest request, UUID studioId) {
        var studio = findStudio(studioId);
        var professional = findProfessional(request.professionalId(), studioId);
        var client = findClient(request.clientId(), studioId);
        validateTimeRange(request.startDatetime(), request.endDatetime());

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
            builder.serviceType(findServiceType(request.serviceTypeId(), studioId));
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
    public Page<AppointmentResponse> list(UUID studioId, String status, UUID professionalId,
                                          LocalDate startDate, LocalDate endDate, Pageable pageable) {
        boolean hasStatus = status != null && !status.isBlank();
        boolean hasProfessional = professionalId != null;
        boolean hasDateRange = startDate != null && endDate != null;
        var appointmentStatus = hasStatus ? AppointmentStatus.valueOf(status) : null;

        if (hasDateRange) {
            var from = startDate.atStartOfDay(ZoneOffset.UTC).toInstant();
            var to = endDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

            if (hasStatus) {
                return appointmentRepository
                        .findByStudioIdAndStatusAndDateRange(
                                studioId, appointmentStatus, from, to, pageable)
                        .map(this::toResponse);
            }
            return appointmentRepository
                    .findByStudioIdAndDateRange(studioId, from, to, pageable)
                    .map(this::toResponse);
        }

        Page<Appointment> page;
        if (hasStatus && hasProfessional) {
            page = appointmentRepository.findByStudioIdAndStatusAndProfessionalId(
                    studioId, appointmentStatus, professionalId, pageable);
        } else if (hasStatus) {
            page = appointmentRepository.findByStudioIdAndStatus(
                    studioId, appointmentStatus, pageable);
        } else if (hasProfessional) {
            page = appointmentRepository.findByStudioIdAndProfessionalId(studioId, professionalId, pageable);
        } else {
            page = appointmentRepository.findByStudioId(studioId, pageable);
        }
        return page.map(this::toResponse);
    }

    @Override
    @Transactional
    public AppointmentResponse update(UUID id, UpdateAppointmentRequest request, UUID studioId) {
        var appointment = findByIdAndStudio(id, studioId);

        if (request.notes() != null) appointment.setNotes(request.notes());

        if (request.serviceTypeId() != null) {
            appointment.setServiceType(findServiceType(request.serviceTypeId(), studioId));
        }

        if (request.professionalId() != null) {
            appointment.setProfessional(findProfessional(request.professionalId(), studioId));
        }

        rejectDirectTimeUpdate(request, appointment.getStatus());

        appointment = appointmentRepository.save(appointment);
        return toResponse(appointment);
    }

    @Override
    @Transactional
    public AppointmentResponse confirm(UUID id, UUID studioId) {
        var appointment = findByIdAndStudio(id, studioId);
        transition(appointment, AppointmentAction.CONFIRM);
        appointment = appointmentRepository.save(appointment);
        notificationService.scheduleForTransition(appointment, AppointmentAction.CONFIRM);
        return toResponse(appointment);
    }

    @Override
    @Transactional
    public AppointmentResponse cancel(UUID id, CancelAppointmentRequest request, CancelledBy cancelledBy, UUID studioId) {
        var appointment = findByIdAndStudio(id, studioId);
        transition(appointment, AppointmentAction.CANCEL);
        appointment.setCancellationReason(request != null ? request.reason() : null);
        appointment.setCancelledBy(cancelledBy);
        appointment = appointmentRepository.save(appointment);
        notificationService.scheduleForTransition(appointment, AppointmentAction.CANCEL);
        return toResponse(appointment);
    }

    @Override
    @Transactional
    public AppointmentResponse complete(UUID id, UUID studioId) {
        var appointment = findByIdAndStudio(id, studioId);
        transition(appointment, AppointmentAction.COMPLETE);
        appointment = appointmentRepository.save(appointment);
        return toResponse(appointment);
    }

    @Override
    @Transactional
    public AppointmentResponse noShow(UUID id, UUID studioId) {
        var appointment = findByIdAndStudio(id, studioId);
        transition(appointment, AppointmentAction.NO_SHOW);
        appointment = appointmentRepository.save(appointment);
        return toResponse(appointment);
    }

    @Override
    @Transactional
    public AppointmentResponse proposeNewTime(UUID id, ProposeNewTimeRequest request, UUID studioId) {
        var appointment = findByIdAndStudio(id, studioId);

        checkConflict(appointment.getProfessional().getId(), request.proposedStart(), request.proposedEnd(),
                appointment.getId(), "L'orario proposto si sovrappone con un altro appuntamento");

        transition(appointment, AppointmentAction.PROPOSE_NEW_TIME);
        setProposals(appointment, request);
        appointment = appointmentRepository.save(appointment);
        notificationService.scheduleForTransition(appointment, AppointmentAction.PROPOSE_NEW_TIME);
        return toResponse(appointment);
    }

    @Override
    @Transactional
    public AppointmentResponse acceptProposal(String token, AcceptProposalRequest request) {
        var appointment = findByToken(token);

        if (!hasMatchingProposal(appointment, request)) {
            throw new IllegalArgumentException("L'orario selezionato non corrisponde a nessuna delle proposte disponibili");
        }

        checkConflict(appointment.getProfessional().getId(),
                request.selectedStart(), request.selectedEnd(), appointment.getId(), "Slot selezionato");

        transition(appointment, AppointmentAction.ACCEPT_PROPOSAL);
        appointment.setStartDatetime(request.selectedStart());
        appointment.setEndDatetime(request.selectedEnd());
        clearAllProposals(appointment);
        appointment = appointmentRepository.save(appointment);
        notificationService.scheduleForTransition(appointment, AppointmentAction.ACCEPT_PROPOSAL);
        notificationService.scheduleForTransition(appointment, AppointmentAction.CONFIRM);
        return toResponse(appointment);
    }

    @Override
    @Transactional
    public AppointmentResponse rejectProposal(String token) {
        var appointment = findByToken(token);
        transition(appointment, AppointmentAction.REJECT_PROPOSAL);
        appointment.setCancelledBy(CancelledBy.CLIENT);
        clearAllProposals(appointment);
        appointment = appointmentRepository.save(appointment);
        notificationService.scheduleForTransition(appointment, AppointmentAction.REJECT_PROPOSAL);
        return toResponse(appointment);
    }

    @Override
    @Transactional(readOnly = true)
    public AppointmentResponse getByToken(String token) {
        return toResponse(findByToken(token));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DayAppointmentCountResponse> getCalendarCounts(UUID studioId, LocalDate startDate, LocalDate endDate) {
        if (ChronoUnit.DAYS.between(startDate, endDate) > 366) {
            throw new IllegalArgumentException("Il range massimo per il calendario è di 366 giorni");
        }

        var studio = findStudio(studioId);

        var zone = ZoneId.of(studio.getTimezone() != null ? studio.getTimezone() : "Europe/Rome");
        var from = startDate.atStartOfDay(zone).toInstant();
        var to = endDate.plusDays(1).atStartOfDay(zone).toInstant();

        Map<LocalDate, Long> countsMap = appointmentRepository.findActiveInRange(studioId, from, to)
                .stream()
                .collect(Collectors.groupingBy(
                        a -> a.getStartDatetime().atZone(zone).toLocalDate(),
                        Collectors.counting()
                ));

        return startDate.datesUntil(endDate.plusDays(1))
                .map(date -> {
                    long count = countsMap.getOrDefault(date, 0L);
                    return new DayAppointmentCountResponse(date, count, computeCapacityLevel(count, studio));
                })
                .toList();
    }

    // ── Helpers ──────────────────────────────────────

    private void validateTimeRange(Instant start, Instant end) {
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("L'orario di fine deve essere successivo a quello di inizio");
        }
    }

    private void rejectDirectTimeUpdate(UpdateAppointmentRequest request, AppointmentStatus status) {
        if ((request.startDatetime() != null || request.endDatetime() != null)
                && (status == AppointmentStatus.REQUESTED || status == AppointmentStatus.CONFIRMED)) {
            throw new IllegalArgumentException(
                    "La modifica diretta dell'orario non è consentita per un appuntamento in stato " + status
                            + ". Utilizzare il flusso di proposta nuovo orario.");
        }
    }

    private void transition(Appointment appointment, AppointmentAction action) {
        appointment.setStatus(stateMachine.transition(appointment.getStatus(), action));
    }

    private void checkConflict(UUID professionalId, Instant start, Instant end, UUID excludeId, String slotDescription) {
        var conflicts = appointmentRepository.countConflictingAppointments(professionalId, start, end, excludeId);
        if (conflicts > 0) {
            throw new SlotNotAvailableException(slotDescription);
        }
    }

    private boolean hasMatchingProposal(Appointment appointment, AcceptProposalRequest request) {
        return matches(request, appointment.getProposedStart(), appointment.getProposedEnd())
                || matches(request, appointment.getProposedStart2(), appointment.getProposedEnd2())
                || matches(request, appointment.getProposedStart3(), appointment.getProposedEnd3());
    }

    private boolean matches(AcceptProposalRequest request, Instant start, Instant end) {
        return start != null && end != null
                && start.equals(request.selectedStart())
                && end.equals(request.selectedEnd());
    }

    private void setProposals(Appointment appointment, ProposeNewTimeRequest request) {
        appointment.setProposedStart(request.proposedStart());
        appointment.setProposedEnd(request.proposedEnd());
        appointment.setProposedStart2(request.proposedStart2());
        appointment.setProposedEnd2(request.proposedEnd2());
        appointment.setProposedStart3(request.proposedStart3());
        appointment.setProposedEnd3(request.proposedEnd3());
    }

    private void clearAllProposals(Appointment appointment) {
        appointment.setProposedStart(null);
        appointment.setProposedEnd(null);
        appointment.setProposedStart2(null);
        appointment.setProposedEnd2(null);
        appointment.setProposedStart3(null);
        appointment.setProposedEnd3(null);
    }

    private Appointment findByIdAndStudio(UUID id, UUID studioId) {
        return appointmentRepository.findByIdAndStudioId(id, studioId)
                .orElseThrow(() -> new EntityNotFoundException("Appuntamento non trovato"));
    }

    private Studio findStudio(UUID studioId) {
        return studioRepository.findById(studioId)
                .orElseThrow(() -> new EntityNotFoundException("Studio non trovato"));
    }

    private Professional findProfessional(UUID professionalId, UUID studioId) {
        return professionalRepository.findByIdAndStudioId(professionalId, studioId)
                .orElseThrow(() -> new EntityNotFoundException("Professionista non trovato"));
    }

    private Client findClient(UUID clientId, UUID studioId) {
        return clientRepository.findByIdAndStudioId(clientId, studioId)
                .orElseThrow(() -> new EntityNotFoundException("Cliente non trovato"));
    }

    private ServiceType findServiceType(UUID serviceTypeId, UUID studioId) {
        return serviceTypeRepository.findByIdAndStudioId(serviceTypeId, studioId)
                .orElseThrow(() -> new EntityNotFoundException("Tipo di servizio non trovato"));
    }

    private Appointment findByToken(String token) {
        return appointmentRepository.findByToken(token)
                .orElseThrow(() -> new EntityNotFoundException("Appuntamento non trovato"));
    }

    private AppointmentCapacityLevel computeCapacityLevel(long count, Studio studio) {
        if (studio.getMaxAppointmentsPerDay() == null) {
            return AppointmentCapacityLevel.AVAILABLE;
        }
        if (studio.getCriticalThreshold() != null && count >= studio.getCriticalThreshold()) {
            return AppointmentCapacityLevel.CRITICAL;
        }
        if (studio.getWarningThreshold() != null && count >= studio.getWarningThreshold()) {
            return AppointmentCapacityLevel.WARNING;
        }
        return AppointmentCapacityLevel.AVAILABLE;
    }

    private AppointmentResponse toResponse(Appointment a) {
        return new AppointmentResponse(
                a.getId(),
                a.getStudio().getId(),
                a.getStudio().getSlug(),
                a.getProfessional().getId(),
                a.getProfessional().getFirstName() + " " + a.getProfessional().getLastName(),
                a.getClient().getId(),
                a.getClient().getFirstName() + " " + a.getClient().getLastName(),
                a.getServiceType() != null ? a.getServiceType().getId() : null,
                a.getServiceType() != null ? a.getServiceType().getName() : null,
                a.getServiceType() != null ? a.getServiceType().getColor() : null,
                a.getStartDatetime(),
                a.getEndDatetime(),
                a.getStatus(),
                a.getNotes(),
                a.getProposedStart(),
                a.getProposedEnd(),
                a.getProposedStart2(),
                a.getProposedEnd2(),
                a.getProposedStart3(),
                a.getProposedEnd3(),
                a.getCancellationReason(),
                a.getCancelledBy(),
                a.getToken(),
                a.getCreatedAt(),
                a.getUpdatedAt()
        );
    }
}
