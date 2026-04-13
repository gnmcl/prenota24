package com.prenota24.backend.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.prenota24.backend.common.AuthHelper;
import com.prenota24.backend.dto.AppointmentResponse;
import com.prenota24.backend.dto.ClientNoteResponse;
import com.prenota24.backend.dto.ClientResponse;
import com.prenota24.backend.dto.ClientSummaryResponse;
import com.prenota24.backend.dto.CreateClientNoteRequest;
import com.prenota24.backend.dto.CreateClientRequest;
import com.prenota24.backend.dto.UpdateClientRequest;
import com.prenota24.backend.service.IClientService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/clients")
@RequiredArgsConstructor
public class ClientController {

    private final IClientService clientService;
    private final AuthHelper authHelper;

    @GetMapping
    public Page<ClientSummaryResponse> list(@RequestParam(required = false) String search,
                                             @PageableDefault(size = 20) Pageable pageable,
                                             Authentication auth) {
        return clientService.list(authHelper.getStudioId(auth), search, pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClientResponse create(@RequestBody @Valid CreateClientRequest request, Authentication auth) {
        return clientService.create(request, authHelper.getStudioId(auth));
    }

    @GetMapping("/{id}")
    public ClientResponse getById(@PathVariable UUID id, Authentication auth) {
        return clientService.getById(id, authHelper.getStudioId(auth));
    }

    @PutMapping("/{id}")
    public ClientResponse update(@PathVariable UUID id,
                                 @RequestBody @Valid UpdateClientRequest request,
                                 Authentication auth) {
        return clientService.update(id, request, authHelper.getStudioId(auth));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id, Authentication auth) {
        clientService.delete(id, authHelper.getStudioId(auth));
    }

    // ── Notes ──────────────────────────────────

    @GetMapping("/{id}/notes")
    public List<ClientNoteResponse> getNotes(@PathVariable UUID id, Authentication auth) {
        return clientService.getNotes(id, authHelper.getStudioId(auth));
    }

    @PostMapping("/{id}/notes")
    @ResponseStatus(HttpStatus.CREATED)
    public ClientNoteResponse addNote(@PathVariable UUID id,
                                       @RequestBody @Valid CreateClientNoteRequest request,
                                       Authentication auth) {
        return clientService.addNote(id, request, authHelper.getUserId(auth), authHelper.getStudioId(auth));
    }

    @PatchMapping("/{id}/notes/{noteId}/pin")
    public ClientNoteResponse togglePin(@PathVariable UUID id,
                                        @PathVariable UUID noteId,
                                        Authentication auth) {
        return clientService.togglePin(id, noteId, authHelper.getStudioId(auth));
    }

    @DeleteMapping("/{id}/notes/{noteId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteNote(@PathVariable UUID id,
                           @PathVariable UUID noteId,
                           Authentication auth) {
        clientService.deleteNote(id, noteId, authHelper.getStudioId(auth));
    }

    @GetMapping("/{id}/appointments")
    public List<AppointmentResponse> getAppointments(@PathVariable UUID id, Authentication auth) {
        return clientService.getAppointments(id, authHelper.getStudioId(auth));
    }
}
