package com.prenota24.backend.service.impl;

import com.prenota24.backend.domain.Appointment;
import com.prenota24.backend.domain.AppointmentAction;
import com.prenota24.backend.domain.CancelledBy;
import com.prenota24.backend.domain.NotificationType;
import com.prenota24.backend.dto.NotificationPayload;
import com.prenota24.backend.service.NotificationTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Resolves the correct NotificationTemplate implementation(s) for a given
 * AppointmentAction and builds the corresponding NotificationPayload list.
 *
 * Template implementations are injected by Spring; no manual switch/if needed here
 * beyond the action → type routing (which is pure domain logic, not email content).
 */
@Component
public class AppointmentNotificationFactory {

    private static final Logger logger = LoggerFactory.getLogger(AppointmentNotificationFactory.class);

    private final Map<NotificationType, NotificationTemplate> templates;

    public AppointmentNotificationFactory(List<NotificationTemplate> templateList) {
        this.templates = templateList.stream()
                .collect(Collectors.toMap(NotificationTemplate::type, Function.identity()));
    }

    /**
     * Build all notification payloads that must be sent for the given action.
     * Returns an empty list for actions that require no notification.
     */
    public List<NotificationPayload> buildForAction(Appointment apt, AppointmentAction action) {
        return switch (action) {
            case CONFIRM         -> List.of(build(NotificationType.APPOINTMENT_CONFIRMED, apt));
            case CANCEL          -> buildForCancel(apt);
            case PROPOSE_NEW_TIME -> List.of(build(NotificationType.PROPOSAL_RECEIVED, apt));
            case ACCEPT_PROPOSAL -> buildForAcceptProposal(apt);
            case REJECT_PROPOSAL -> buildForRejectProposal(apt);
            default              -> List.of(); // COMPLETE, NO_SHOW — no notification
        };
    }

    /**
     * Build a single payload directly by notification type.
     * Used by scheduleReminder and any caller that needs a specific type.
     */
    public NotificationPayload build(NotificationType type, Appointment apt) {
        var template = templates.get(type);
        if (template == null) {
            throw new IllegalStateException("No NotificationTemplate registered for type: " + type);
        }
        return template.build(apt);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private List<NotificationPayload> buildForCancel(Appointment apt) {
        if (apt.getCancelledBy() == CancelledBy.PROFESSIONAL) {
            return List.of(build(NotificationType.APPOINTMENT_CANCELLED_BY_PROFESSIONAL, apt));
        }
        if (apt.getCancelledBy() == CancelledBy.CLIENT && apt.getProfessional().getEmail() != null) {
            return List.of(build(NotificationType.APPOINTMENT_CANCELLED_BY_CLIENT, apt));
        }
        logger.warn("Cancellation on appointment {} has no CancelledBy or professional email — skipping notification",
                apt.getId());
        return List.of();
    }

    private List<NotificationPayload> buildForAcceptProposal(Appointment apt) {
        if (apt.getProfessional().getEmail() == null) {
            logger.warn("Professional {} has no email — skipping PROPOSAL_ACCEPTED notification",
                    apt.getProfessional().getId());
            return List.of();
        }
        return List.of(build(NotificationType.PROPOSAL_ACCEPTED, apt));
    }

    private List<NotificationPayload> buildForRejectProposal(Appointment apt) {
        var payloads = new java.util.ArrayList<NotificationPayload>();
        payloads.add(build(NotificationType.PROPOSAL_REJECTED_CLIENT, apt));
        if (apt.getProfessional().getEmail() != null) {
            payloads.add(build(NotificationType.PROPOSAL_REJECTED_STUDIO, apt));
        } else {
            logger.warn("Professional {} has no email — skipping PROPOSAL_REJECTED_STUDIO notification",
                    apt.getProfessional().getId());
        }
        return java.util.Collections.unmodifiableList(payloads);
    }
}

