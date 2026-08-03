package com.prenota24.backend.service.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.prenota24.backend.common.EntityNotFoundException;
import com.prenota24.backend.domain.Client;
import com.prenota24.backend.domain.ClientNote;
import com.prenota24.backend.domain.ClientSource;
import com.prenota24.backend.dto.AppointmentResponse;
import com.prenota24.backend.dto.ClientNoteResponse;
import com.prenota24.backend.dto.ClientResponse;
import com.prenota24.backend.dto.ClientSummaryResponse;
import com.prenota24.backend.dto.CreateClientNoteRequest;
import com.prenota24.backend.dto.CreateClientRequest;
import com.prenota24.backend.dto.UpdateClientRequest;
import com.prenota24.backend.repository.AppUserRepository;
import com.prenota24.backend.repository.AppointmentRepository;
import com.prenota24.backend.repository.ClientNoteRepository;
import com.prenota24.backend.repository.ClientRepository;
import com.prenota24.backend.repository.StudioRepository;
import com.prenota24.backend.service.IClientService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ClientService implements IClientService {

    private final ClientRepository clientRepository;
    private final ClientNoteRepository noteRepository;
    private final StudioRepository studioRepository;
    private final AppUserRepository appUserRepository;
    private final AppointmentRepository appointmentRepository;

    @Override
    @Transactional
    public ClientResponse create(CreateClientRequest request, UUID studioId) {
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
        return toResponse(client);
    }

    @Override
    @Transactional(readOnly = true)
    public ClientResponse getById(UUID id, UUID studioId) {
        return toResponse(findByIdAndStudio(id, studioId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ClientSummaryResponse> list(UUID studioId, String search, Pageable pageable) {
        if (search != null && !search.isBlank()) {
            return clientRepository.search(studioId, search.trim(), pageable)
                    .map(this::toSummary);
        }
        return clientRepository.findByStudioId(studioId, pageable)
                .map(this::toSummary);
    }

    @Override
    @Transactional
    public ClientResponse update(UUID id, UpdateClientRequest request, UUID studioId) {
        var client = findByIdAndStudio(id, studioId);

        if (request.firstName() != null) client.setFirstName(request.firstName());
        if (request.lastName() != null) client.setLastName(request.lastName());
        if (request.email() != null) client.setEmail(request.email());
        if (request.phone() != null) client.setPhone(request.phone());
        if (request.notes() != null) client.setNotes(request.notes());
        if (request.tags() != null) client.setTags(request.tags());

        client = clientRepository.save(client);
        return toResponse(client);
    }

    @Override
    @Transactional
    public void delete(UUID id, UUID studioId) {
        var client = findByIdAndStudio(id, studioId);
        clientRepository.delete(client);
    }

    // ── Notes ──────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<ClientNoteResponse> getNotes(UUID clientId, UUID studioId) {
        findByIdAndStudio(clientId, studioId);
        return noteRepository.findByClientIdOrderByPinnedDescCreatedAtDesc(clientId)
                .stream()
                .map(this::toNoteResponse)
                .toList();
    }

    @Override
    @Transactional
    public ClientNoteResponse addNote(UUID clientId, CreateClientNoteRequest request, UUID authorId, UUID studioId) {
        var client = findByIdAndStudio(clientId, studioId);
        var author = appUserRepository.findById(authorId)
                .orElseThrow(() -> new RuntimeException("Utente non trovato"));

        var noteBuilder = ClientNote.builder()
                .client(client)
                .studio(client.getStudio())
                .author(author)
                .content(request.content())
                .pinned(Boolean.TRUE.equals(request.pinned()));

        if (request.appointmentId() != null) {
            var appointment = appointmentRepository.findByIdAndStudioId(request.appointmentId(), studioId)
                    .orElseThrow(() -> new EntityNotFoundException("Appuntamento non trovato"));
            noteBuilder.appointment(appointment);
        }

        var note = noteRepository.save(noteBuilder.build());
        return toNoteResponse(note);
    }

    @Override
    @Transactional
    public ClientNoteResponse togglePin(UUID clientId, UUID noteId, UUID studioId) {
        findByIdAndStudio(clientId, studioId);
        var note = noteRepository.findByIdAndStudioId(noteId, studioId)
                .orElseThrow(() -> new EntityNotFoundException("Nota non trovata"));

        note.setPinned(!note.isPinned());
        note = noteRepository.save(note);
        return toNoteResponse(note);
    }

    @Override
    @Transactional
    public void deleteNote(UUID clientId, UUID noteId, UUID studioId) {
        findByIdAndStudio(clientId, studioId);
        var note = noteRepository.findByIdAndStudioId(noteId, studioId)
                .orElseThrow(() -> new EntityNotFoundException("Nota non trovata"));
        noteRepository.delete(note);
    }

    // ── Reservation Import ──────────────────────────────────────

    @Override
    @Transactional
    public Client findOrCreateFromReservation(String email, String name, String phone, UUID studioId) {
        if (email == null || email.isBlank()) {
            return createMinimalClient(name, phone, studioId);
        }

        return clientRepository.findByStudioIdAndEmailIgnoreCase(studioId, email)
                .orElseGet(() -> createMinimalClient(name, phone, email, studioId, ClientSource.PUBLIC_BOOKING));
    }

    // ── Helpers ──────────────────────────────────────

    private Client createMinimalClient(String name, String phone, UUID studioId) {
        return createMinimalClient(name, phone, null, studioId, ClientSource.RESERVATION_IMPORT);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getAppointments(UUID clientId, UUID studioId) {
        findByIdAndStudio(clientId, studioId);
        return appointmentRepository.findByClientIdAndStudioIdOrderByStartDatetimeDesc(clientId, studioId)
                .stream()
                .map(this::toAppointmentResponse)
                .toList();
    }

    private AppointmentResponse toAppointmentResponse(com.prenota24.backend.domain.Appointment a) {
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

    private Client createMinimalClient(String name, String phone, String email, UUID studioId, ClientSource source) {
        var studio = studioRepository.findById(studioId)
                .orElseThrow(() -> new EntityNotFoundException("Studio non trovato"));

        String[] parts = (name != null ? name : "").split("\\s+", 2);
        String firstName = parts.length > 0 ? parts[0] : "N/A";
        String lastName = parts.length > 1 ? parts[1] : "";

        var client = Client.builder()
                .studio(studio)
                .firstName(firstName)
                .lastName(lastName)
                .email(email)
                .phone(phone)
                .source(source)
                .build();

        return clientRepository.save(client);
    }

    private Client findByIdAndStudio(UUID id, UUID studioId) {
        return clientRepository.findByIdAndStudioId(id, studioId)
                .orElseThrow(() -> new EntityNotFoundException("Cliente non trovato"));
    }

    private ClientResponse toResponse(Client c) {
        return new ClientResponse(
                c.getId(), c.getStudio().getId(),
                c.getFirstName(), c.getLastName(),
                c.getEmail(), c.getPhone(), c.getNotes(),
                c.getSource(), c.getTags(),
                c.getCreatedAt(), c.getUpdatedAt()
        );
    }

    private ClientSummaryResponse toSummary(Client c) {
        return new ClientSummaryResponse(
                c.getId(), c.getFirstName(), c.getLastName(),
                c.getEmail(), c.getPhone(), c.getCreatedAt()
        );
    }

    private ClientNoteResponse toNoteResponse(ClientNote n) {
        return new ClientNoteResponse(
                n.getId(), n.getClient().getId(),
                n.getAuthor().getId(),
                n.getAuthor().getName(),
                n.getAppointment() != null ? n.getAppointment().getId() : null,
                n.getContent(), n.isPinned(),
                n.getCreatedAt()
        );
    }
}
