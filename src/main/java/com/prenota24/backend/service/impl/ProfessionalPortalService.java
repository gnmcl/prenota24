package com.prenota24.backend.service.impl;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.prenota24.backend.common.EntityNotFoundException;
import com.prenota24.backend.domain.Appointment;
import com.prenota24.backend.domain.AppointmentAction;
import com.prenota24.backend.domain.AppointmentStatus;
import com.prenota24.backend.domain.Availability;
import com.prenota24.backend.domain.AvailabilityException;
import com.prenota24.backend.domain.AvailabilityExceptionSlot;
import com.prenota24.backend.domain.CancelledBy;
import com.prenota24.backend.domain.Client;
import com.prenota24.backend.domain.ClientSource;
import com.prenota24.backend.domain.Professional;
import com.prenota24.backend.domain.Studio;
import com.prenota24.backend.dto.AppointmentResponse;
import com.prenota24.backend.dto.AvailabilityExceptionResponse;
import com.prenota24.backend.dto.AvailabilityExceptionSlotResponse;
import com.prenota24.backend.dto.AvailabilityResponse;
import com.prenota24.backend.dto.AvailabilitySlotRequest;
import com.prenota24.backend.dto.CancelAppointmentRequest;
import com.prenota24.backend.dto.ClientSummaryResponse;
import com.prenota24.backend.dto.CreateAppointmentRequest;
import com.prenota24.backend.dto.CreateAvailabilityExceptionRequest;
import com.prenota24.backend.dto.CreateClientRequest;
import com.prenota24.backend.dto.ProfessionalDashboardResponse;
import com.prenota24.backend.dto.ProfessionalResponse;
import com.prenota24.backend.dto.ServiceTypeResponse;
import com.prenota24.backend.dto.StudioResponse;
import com.prenota24.backend.repository.AppointmentRepository;
import com.prenota24.backend.repository.AvailabilityExceptionRepository;
import com.prenota24.backend.repository.AvailabilityRepository;
import com.prenota24.backend.repository.ClientRepository;
import com.prenota24.backend.repository.ProfessionalRepository;
import com.prenota24.backend.repository.ServiceTypeRepository;
import com.prenota24.backend.repository.StudioRepository;
import com.prenota24.backend.service.IProfessionalPortalService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProfessionalPortalService implements IProfessionalPortalService {

    private final AppointmentRepository appointmentRepository;
    private final ProfessionalRepository professionalRepository;
    private final ClientRepository clientRepository;
    private final ServiceTypeRepository serviceTypeRepository;
    private final StudioRepository studioRepository;
    private final AvailabilityRepository availabilityRepository;
    private final AvailabilityExceptionRepository exceptionRepository;
    private final AppointmentStateMachine stateMachine;

    // ── Dashboard ──────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public ProfessionalDashboardResponse getDashboard(UUID professionalId, UUID studioId) {
        var professional = findProfessional(professionalId);
        var studio = studioRepository.findById(studioId)
                .orElseThrow(() -> new EntityNotFoundException("Studio non trovato"));

        // Today's appointments
        var tz = studio.getTimezone();
        var zone = ZoneId.of(tz != null ? tz : "Europe/Rome");
        var todayStart = LocalDate.now(zone).atStartOfDay(zone).toInstant();
        var todayEnd = todayStart.plus(java.time.Duration.ofDays(1));
        long todayCount = appointmentRepository.countTodayAppointments(professionalId, todayStart, todayEnd);

        // Total unique clients
        long totalClients = clientRepository.findClientsByProfessionalId(professionalId).size();

        // Pending appointments
        long pending = appointmentRepository.countByProfessionalIdAndStatusIn(
                professionalId,
                List.of(AppointmentStatus.REQUESTED)
        );

        return new ProfessionalDashboardResponse(
                toProfessionalResponse(professional),
                toStudioResponse(studio),
                todayCount,
                totalClients,
                pending
        );
    }

    // ── Appointments ──────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Page<AppointmentResponse> getMyAppointments(UUID professionalId, String status, Pageable pageable) {
        Page<Appointment> page;
        if (status != null && !status.isBlank()) {
            page = appointmentRepository.findByProfessionalIdAndStatus(
                    professionalId, AppointmentStatus.valueOf(status), pageable);
        } else {
            page = appointmentRepository.findByProfessionalId(professionalId, pageable);
        }
        return page.map(this::toAppointmentResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public AppointmentResponse getMyAppointmentById(UUID appointmentId, UUID professionalId) {
        var appointment = findMyAppointment(appointmentId, professionalId);
        return toAppointmentResponse(appointment);
    }

    @Override
    @Transactional
    public AppointmentResponse createAppointment(CreateAppointmentRequest request, UUID professionalId, UUID studioId) {
        var studio = studioRepository.findById(studioId)
                .orElseThrow(() -> new EntityNotFoundException("Studio non trovato"));

        var professional = findProfessional(professionalId);

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
        return toAppointmentResponse(appointment);
    }

    @Override
    @Transactional
    public AppointmentResponse confirmAppointment(UUID appointmentId, UUID professionalId) {
        var appointment = findMyAppointment(appointmentId, professionalId);
        appointment.setStatus(stateMachine.transition(appointment.getStatus(), AppointmentAction.CONFIRM));
        appointment = appointmentRepository.save(appointment);
        return toAppointmentResponse(appointment);
    }

    @Override
    @Transactional
    public AppointmentResponse cancelAppointment(UUID appointmentId, CancelAppointmentRequest request, UUID professionalId) {
        var appointment = findMyAppointment(appointmentId, professionalId);
        appointment.setStatus(stateMachine.transition(appointment.getStatus(), AppointmentAction.CANCEL));
        appointment.setCancellationReason(request != null ? request.reason() : null);
        appointment.setCancelledBy(CancelledBy.PROFESSIONAL);
        appointment = appointmentRepository.save(appointment);
        return toAppointmentResponse(appointment);
    }

    @Override
    @Transactional
    public AppointmentResponse completeAppointment(UUID appointmentId, UUID professionalId) {
        var appointment = findMyAppointment(appointmentId, professionalId);
        appointment.setStatus(stateMachine.transition(appointment.getStatus(), AppointmentAction.COMPLETE));
        appointment = appointmentRepository.save(appointment);
        return toAppointmentResponse(appointment);
    }

    @Override
    @Transactional
    public AppointmentResponse noShowAppointment(UUID appointmentId, UUID professionalId) {
        var appointment = findMyAppointment(appointmentId, professionalId);
        appointment.setStatus(stateMachine.transition(appointment.getStatus(), AppointmentAction.NO_SHOW));
        appointment = appointmentRepository.save(appointment);
        return toAppointmentResponse(appointment);
    }

    // ── Clients ──────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<ClientSummaryResponse> getMyClients(UUID professionalId) {
        return clientRepository.findClientsByProfessionalId(professionalId)
                .stream()
                .map(c -> new ClientSummaryResponse(
                        c.getId(),
                        c.getFirstName(),
                        c.getLastName(),
                        c.getEmail(),
                        c.getPhone(),
                        c.getCreatedAt()
                ))
                .toList();
    }

    @Override
    @Transactional
    public ClientSummaryResponse createClient(CreateClientRequest request, UUID professionalId, UUID studioId) {
        var studio = studioRepository.findById(studioId)
                .orElseThrow(() -> new EntityNotFoundException("Studio non trovato"));

        var client = Client.builder()
                .studio(studio)
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email(request.email())
                .phone(request.phone())
                .notes(request.notes())
                .tags(request.tags())
                .source(ClientSource.MANUAL)
                .build();

        client = clientRepository.save(client);
        return new ClientSummaryResponse(
                client.getId(), client.getFirstName(), client.getLastName(),
                client.getEmail(), client.getPhone(), client.getCreatedAt()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ServiceTypeResponse> getMyServiceTypes(UUID professionalId, UUID studioId) {
        return serviceTypeRepository.findByStudioIdAndActiveTrue(studioId).stream()
                .filter(st -> st.getProfessionals().isEmpty() || st.getProfessionals().stream().anyMatch(p -> p.getId().equals(professionalId)))
                .map(st -> new ServiceTypeResponse(
                        st.getId(), st.getStudio().getId(),
                        st.getProfessionals().stream().map(p -> p.getId()).toList(),
                        st.getName(), st.getDescription(), st.getDurationMinutes(),
                        st.getPrice(), st.getColor(), st.isActive(), st.getCreatedAt()
                ))
                .toList();
    }

    // ── Availability ──────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<AvailabilityResponse> getMyAvailability(UUID professionalId, UUID studioId) {
        return availabilityRepository.findByProfessionalId(professionalId)
                .stream()
                .map(a -> new AvailabilityResponse(a.getId(), a.getDayOfWeek(), a.getStartTime(), a.getEndTime()))
                .toList();
    }

    @Override
    @Transactional
    public List<AvailabilityResponse> setMyAvailability(UUID professionalId, List<AvailabilitySlotRequest> slots, UUID studioId) {
        var professional = findProfessional(professionalId);

        availabilityRepository.deleteByProfessionalId(professionalId);

        var entities = slots.stream().map(slot -> Availability.builder()
                .professional(professional)
                .dayOfWeek(slot.dayOfWeek())
                .startTime(slot.startTime())
                .endTime(slot.endTime())
                .build()
        ).toList();

        return availabilityRepository.saveAll(entities)
                .stream()
                .map(a -> new AvailabilityResponse(a.getId(), a.getDayOfWeek(), a.getStartTime(), a.getEndTime()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AvailabilityExceptionResponse> getMyExceptions(UUID professionalId, UUID studioId) {
        return exceptionRepository.findByProfessionalIdAndDateBetween(
                        professionalId,
                        LocalDate.now(),
                        LocalDate.now().plusMonths(6)
                ).stream()
                .map(this::toExceptionResponse)
                .toList();
    }

    @Override
    @Transactional
    public AvailabilityExceptionResponse addMyException(UUID professionalId,
                                                         CreateAvailabilityExceptionRequest request,
                                                         UUID studioId) {
        var professional = findProfessional(professionalId);

        // Validazione: se non è giornata intera, almeno uno slot è obbligatorio
        if (!request.isUnavailableAllDay() &&
                (request.slots() == null || request.slots().isEmpty())) {
            throw new IllegalArgumentException(
                    "Se il giorno non è interamente non disponibile, deve essere specificato almeno uno slot");
        }

        var exception = AvailabilityException.builder()
                .professional(professional)
                .date(request.date())
                .isUnavailableAllDay(request.isUnavailableAllDay())
                .reason(request.reason())
                .build();

        // Aggiungo gli slot di indisponibilità
        if (!request.isUnavailableAllDay() && request.slots() != null) {
            for (var slotRequest : request.slots()) {
                var slot = AvailabilityExceptionSlot.builder()
                        .availabilityException(exception)
                        .startTime(slotRequest.startTime())
                        .endTime(slotRequest.endTime())
                        .build();
                exception.getSlots().add(slot);
            }
        }

        exception = exceptionRepository.save(exception);
        return toExceptionResponse(exception);
    }

    @Override
    @Transactional
    public void removeMyException(UUID professionalId, UUID exceptionId, UUID studioId) {
        var exception = exceptionRepository.findById(exceptionId)
                .orElseThrow(() -> new EntityNotFoundException("Eccezione non trovata"));

        if (!exception.getProfessional().getId().equals(professionalId)) {
            throw new EntityNotFoundException("Eccezione non trovata");
        }

        exceptionRepository.delete(exception);
    }

    // ── Helpers ──────────────────────────────────────

    private Professional findProfessional(UUID professionalId) {
        return professionalRepository.findById(professionalId)
                .orElseThrow(() -> new EntityNotFoundException("Professionista non trovato"));
    }

    private Appointment findMyAppointment(UUID appointmentId, UUID professionalId) {
        return appointmentRepository.findByIdAndProfessionalId(appointmentId, professionalId)
                .orElseThrow(() -> new EntityNotFoundException("Appuntamento non trovato"));
    }

    private AppointmentResponse toAppointmentResponse(Appointment a) {
        return new AppointmentResponse(
                a.getId(),
                a.getStudio().getId(),
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

    private ProfessionalResponse toProfessionalResponse(Professional p) {
        return new ProfessionalResponse(
                p.getId(),
                p.getStudio().getId(),
                p.getFirstName(),
                p.getLastName(),
                p.getEmail(),
                p.getPhone(),
                p.isActive(),
                p.getCreatedAt()
        );
    }

    private StudioResponse toStudioResponse(Studio s) {
        return new StudioResponse(s.getId(), s.getName(), s.getEmail(), s.getPhone(), s.getTimezone(),
                s.getMaxAppointmentsPerDay(), s.getWarningThreshold(), s.getCriticalThreshold());
    }

    private AvailabilityExceptionResponse toExceptionResponse(AvailabilityException e) {
        var slotResponses = e.getSlots().stream()
                .map(s -> new AvailabilityExceptionSlotResponse(s.getId(), s.getStartTime(), s.getEndTime()))
                .toList();
        return new AvailabilityExceptionResponse(
                e.getId(), e.getDate(), slotResponses, e.isUnavailableAllDay(), e.getReason()
        );
    }
}
