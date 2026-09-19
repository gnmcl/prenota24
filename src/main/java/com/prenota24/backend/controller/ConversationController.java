package com.prenota24.backend.controller;

import com.prenota24.backend.common.AuthHelper;
import com.prenota24.backend.dto.*;
import com.prenota24.backend.service.IConversationService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/conversations")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class ConversationController {
    private final IConversationService conversationService;
    private final AuthHelper authHelper;

    @GetMapping
    public Page<ConversationResponse> list(@PageableDefault(size = 20) Pageable pageable, Authentication auth) {
        return conversationService.list(authHelper.getStudioId(auth), authHelper.getUserId(auth), pageable);
    }

    @GetMapping("/{id}")
    public ConversationResponse get(@PathVariable UUID id, Authentication auth) {
        return conversationService.get(id, authHelper.getStudioId(auth), authHelper.getUserId(auth));
    }

    @GetMapping("/{id}/messages")
    public Page<MessageResponse> listMessages(@PathVariable UUID id,
                                              @PageableDefault(size = 50) Pageable pageable,
                                              Authentication auth) {
        return conversationService.listMessages(id, authHelper.getStudioId(auth), pageable);
    }

    @PostMapping("/{id}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public MessageResponse send(@PathVariable UUID id,
                                @RequestBody @Valid SendMessageRequest request,
                                Authentication auth) {
        return conversationService.enqueue(id, request, authHelper.getStudioId(auth), authHelper.getUserId(auth));
    }

    @PostMapping("/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(@PathVariable UUID id,
                         @RequestBody @Valid ReadConversationRequest request,
                         Authentication auth) {
        conversationService.markRead(id, request.messageId(), authHelper.getStudioId(auth), authHelper.getUserId(auth));
    }

    @PostMapping("/{id}/consent")
    public ConversationResponse updateConsent(@PathVariable UUID id,
                                               @RequestBody @Valid UpdateConversationConsentRequest request,
                                               Authentication auth) {
        return conversationService.updateConsent(id, request.enabled(), authHelper.getStudioId(auth), authHelper.getUserId(auth));
    }
}
