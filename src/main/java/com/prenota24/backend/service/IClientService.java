package com.prenota24.backend.service;

import com.prenota24.backend.domain.Client;
import com.prenota24.backend.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface IClientService {

    ClientResponse create(CreateClientRequest request, UUID studioId);

    ClientResponse getById(UUID id, UUID studioId);

    Page<ClientSummaryResponse> list(UUID studioId, String search, Pageable pageable);

    ClientResponse update(UUID id, UpdateClientRequest request, UUID studioId);

    void delete(UUID id, UUID studioId);

    // Notes
    java.util.List<ClientNoteResponse> getNotes(UUID clientId, UUID studioId);

    ClientNoteResponse addNote(UUID clientId, CreateClientNoteRequest request, UUID authorId, UUID studioId);

    ClientNoteResponse togglePin(UUID clientId, UUID noteId, UUID studioId);

    void deleteNote(UUID clientId, UUID noteId, UUID studioId);

    // Import from reservation
    Client findOrCreateFromReservation(String email, String name, String phone, UUID studioId);
}
