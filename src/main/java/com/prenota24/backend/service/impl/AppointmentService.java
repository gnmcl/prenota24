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
import com.prenota24.backend.domain.Studio;
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
    public Page<AppointmentResponse> list(UUID studioId, String status, UUID professionalId,
                                          LocalDate startDate, LocalDate endDate, Pageable pageable) {
        boolean hasStatus = status != null && !status.isBlank();
        boolean hasProfessional = professionalId != null;
        boolean hasDateRange = startDate != null && endDate != null;

        if (hasDateRange) {
            Instant from = startDate.atStartOfDay(ZoneOffset.UTC).toInstant();
            Instant to = endDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

            if (hasStatus) {
                return appointmentRepository
                        .findByStudioIdAndStatusAndDateRange(
                                studioId, AppointmentStatus.valueOf(status), from, to, pageable)
                        .map(this::toResponse);
            }
            return appointmentRepository
                    .findByStudioIdAndDateRange(studioId, from, to, pageable)
                    .map(this::toResponse);
        }

        Page<Appointment> page;
        if (hasStatus && hasProfessional) {
            page = appointmentRepository.findByStudioIdAndStatusAndProfessionalId(
                    studioId, AppointmentStatus.valueOf(status), professionalId, pageable);
        } else if (hasStatus) {
            page = appointmentRepository.findByStudioIdAndStatus(
                    studioId, AppointmentStatus.valueOf(status), pageable);
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
            var serviceType = serviceTypeRepository.findByIdAndStudioId(request.serviceTypeId(), studioId)
                    .orElseThrow(() -> new EntityNotFoundException("Tipo di servizio non trovato"));
            appointment.setServiceType(serviceType);
        }

        if (request.professionalId() != null) {
            var professional = professionalRepository.findByIdAndStudioId(request.professionalId(), studioId)
                    .orElseThrow(() -> new EntityNotFoundException("Professionista non trovato"));
            appointment.setProfessional(professional);
        }

        // Handle reschedule
        if (request.startDatetime() != null && request.endDatetime() != null) {
            if (request.endDatetime().isBefore(request.startDatetime()) ||
                    request.endDatetime().equals(request.startDatetime())) {
                throw new IllegalArgumentException("L'orario di fine deve essere successivo a quello di inizio");
            }

            // Check for overlapping appointments (excluding this one)
            long conflicts = appointmentRepository.countConflictingAppointments(
                    appointment.getProfessional().getId(),
                    request.startDatetime(),
                    request.endDatetime(),
                    appointment.getId()
            );

            if (conflicts > 0) {
                throw new SlotNotAvailableException("L'orario scelto si sovrappone con un altro appuntamento");
            }

            appointment.setStartDatetime(request.startDatetime());
            appointment.setEndDatetime(request.endDatetime());
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
        notificationService.scheduleForTransition(appointment, AppointmentAction.CONFIRM);
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
        notificationService.scheduleForTransition(appointment, AppointmentAction.CANCEL);
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
        notificationService.scheduleForTransition(appointment, AppointmentAction.PROPOSE_NEW_TIME);
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
        // Notifica il professionista che il cliente ha accettato
        notificationService.scheduleForTransition(appointment, AppointmentAction.ACCEPT_PROPOSAL);
        // Invia al cliente la conferma con i dettagli aggiornati
        notificationService.scheduleForTransition(appointment, AppointmentAction.CONFIRM);
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

    @Override
    @Transactional(readOnly = true)
    public List<DayAppointmentCountResponse> getCalendarCounts(UUID studioId, LocalDate startDate, LocalDate endDate) {
        if (ChronoUnit.DAYS.between(startDate, endDate) > 366) {
            throw new IllegalArgumentException("Il range massimo per il calendario è di 366 giorni");
        }

        var studio = studioRepository.findById(studioId)
                .orElseThrow(() -> new EntityNotFoundException("Studio non trovato"));

        ZoneId zone = ZoneId.of(studio.getTimezone() != null ? studio.getTimezone() : "Europe/Rome");
        Instant from = startDate.atStartOfDay(zone).toInstant();
        Instant to = endDate.plusDays(1).atStartOfDay(zone).toInstant();

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

    private Appointment findByIdAndStudio(UUID id, UUID studioId) {
        return appointmentRepository.findByIdAndStudioId(id, studioId)
                .orElseThrow(() -> new EntityNotFoundException("Appuntamento non trovato"));
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
                a.getCancellationReason(),
                a.getCancelledBy(),
                a.getToken(),
                a.getCreatedAt(),
                a.getUpdatedAt()
        );
    }
}
