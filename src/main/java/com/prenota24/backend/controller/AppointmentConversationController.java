package com.prenota24.backend.controller;

import com.prenota24.backend.common.AuthHelper;
import com.prenota24.backend.dto.ConversationResponse;
import com.prenota24.backend.service.IConversationService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/appointments")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AppointmentConversationController {
    private final IConversationService conversationService;
    private final AuthHelper authHelper;

    @PostMapping("/{id}/conversation")
    public ConversationResponse getOrCreate(@PathVariable UUID id, Authentication auth) {
        return conversationService.getOrCreateForAppointment(id, authHelper.getStudioId(auth), authHelper.getUserId(auth));
    }
}
