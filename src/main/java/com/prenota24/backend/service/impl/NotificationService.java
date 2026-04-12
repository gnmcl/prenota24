package com.prenota24.backend.service.impl;

import com.prenota24.backend.domain.*;
import com.prenota24.backend.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Transactional
    public Notification schedule(Appointment apt, NotificationChannel channel, String type,
                                  UUID recipientId, RecipientType recipientType, Map<String, Object> payload) {
        var notification = Notification.builder()
                .studio(apt.getStudio())
                .appointment(apt)
                .channel(channel)
                .type(type)
                .recipientId(recipientId)
                .recipientType(recipientType)
                .scheduledAt(Instant.now())
                .payload(payload)
                .build();

        return notificationRepository.save(notification);
    }

    @Transactional
    public Notification scheduleReminder(Appointment apt, NotificationChannel channel) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("recipientEmail", apt.getClient().getEmail());
        payload.put("recipientName", apt.getClient().getFirstName() + " " + apt.getClient().getLastName());
        payload.put("subject", "Promemoria: appuntamento domani");
        payload.put("body", String.format(
                "Ciao %s,\n\nTi ricordiamo il tuo appuntamento con %s %s previsto per domani.\n\nPrenota24",
                apt.getClient().getFirstName(),
                apt.getProfessional().getFirstName(),
                apt.getProfessional().getLastName()
        ));

        var notification = Notification.builder()
                .studio(apt.getStudio())
                .appointment(apt)
                .channel(channel)
                .type("REMINDER_24H")
                .recipientId(apt.getClient().getId())
                .recipientType(RecipientType.CLIENT)
                .scheduledAt(apt.getStartDatetime().minus(24, ChronoUnit.HOURS))
                .payload(payload)
                .build();

        return notificationRepository.save(notification);
    }

    @Transactional
    public void scheduleForTransition(Appointment apt, AppointmentAction action) {
        Map<String, Object> payload = new HashMap<>();

        switch (action) {
            case CONFIRM -> {
                payload.put("recipientEmail", apt.getClient().getEmail());
                payload.put("recipientName", apt.getClient().getFirstName());
                payload.put("subject", "Appuntamento confermato");
                payload.put("body", String.format(
                        "Ciao %s,\n\nIl tuo appuntamento con %s %s è stato confermato.\n\nPrenota24",
                        apt.getClient().getFirstName(),
                        apt.getProfessional().getFirstName(),
                        apt.getProfessional().getLastName()
                ));
                schedule(apt, NotificationChannel.EMAIL, "APPOINTMENT_CONFIRMED",
                        apt.getClient().getId(), RecipientType.CLIENT, payload);
            }
            case CANCEL -> {
                // Notify the other party
                if (apt.getCancelledBy() == CancelledBy.PROFESSIONAL) {
                    payload.put("recipientEmail", apt.getClient().getEmail());
                    payload.put("recipientName", apt.getClient().getFirstName());
                    payload.put("subject", "Appuntamento cancellato");
                    payload.put("body", "Il tuo appuntamento è stato cancellato." +
                            (apt.getCancellationReason() != null ? " Motivo: " + apt.getCancellationReason() : ""));
                    schedule(apt, NotificationChannel.EMAIL, "APPOINTMENT_CANCELLED",
                            apt.getClient().getId(), RecipientType.CLIENT, payload);
                }
                // If cancelled by client, notify professional (if they have email)
                if (apt.getCancelledBy() == CancelledBy.CLIENT && apt.getProfessional().getEmail() != null) {
                    payload.put("recipientEmail", apt.getProfessional().getEmail());
                    payload.put("recipientName", apt.getProfessional().getFirstName());
                    payload.put("subject", "Appuntamento cancellato dal cliente");
                    payload.put("body", String.format(
                            "Il cliente %s %s ha cancellato il suo appuntamento.",
                            apt.getClient().getFirstName(), apt.getClient().getLastName()
                    ));
                    schedule(apt, NotificationChannel.EMAIL, "APPOINTMENT_CANCELLED",
                            apt.getProfessional().getId(), RecipientType.PROFESSIONAL, payload);
                }
            }
            case PROPOSE_NEW_TIME -> {
                payload.put("recipientEmail", apt.getClient().getEmail());
                payload.put("recipientName", apt.getClient().getFirstName());
                payload.put("subject", "Nuovo orario proposto per il tuo appuntamento");
                payload.put("body", String.format(
                        "Ciao %s,\n\n%s %s ti ha proposto un nuovo orario per il tuo appuntamento.\n\n" +
                                "Per accettare o rifiutare, visita: /booking/confirm/%s\n\nPrenota24",
                        apt.getClient().getFirstName(),
                        apt.getProfessional().getFirstName(),
                        apt.getProfessional().getLastName(),
                        apt.getToken()
                ));
                payload.put("tokenLink", "/booking/confirm/" + apt.getToken());
                schedule(apt, NotificationChannel.EMAIL, "PROPOSAL_RECEIVED",
                        apt.getClient().getId(), RecipientType.CLIENT, payload);
            }
            case ACCEPT_PROPOSAL -> {
                if (apt.getProfessional().getEmail() != null) {
                    payload.put("recipientEmail", apt.getProfessional().getEmail());
                    payload.put("recipientName", apt.getProfessional().getFirstName());
                    payload.put("subject", "Proposta orario accettata");
                    payload.put("body", String.format(
                            "%s %s ha accettato il nuovo orario proposto.",
                            apt.getClient().getFirstName(), apt.getClient().getLastName()
                    ));
                    schedule(apt, NotificationChannel.EMAIL, "PROPOSAL_ACCEPTED",
                            apt.getProfessional().getId(), RecipientType.PROFESSIONAL, payload);
                }
            }
            default -> {} // No notification for COMPLETE, NO_SHOW, REJECT_PROPOSAL
        }
    }
}
