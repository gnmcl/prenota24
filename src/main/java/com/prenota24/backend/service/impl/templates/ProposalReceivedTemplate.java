package com.prenota24.backend.service.impl.templates;

import com.prenota24.backend.domain.Appointment;
import com.prenota24.backend.domain.NotificationChannel;
import com.prenota24.backend.domain.NotificationType;
import com.prenota24.backend.domain.RecipientType;
import com.prenota24.backend.dto.EmailPayload;
import com.prenota24.backend.dto.NotificationPayload;
import com.prenota24.backend.service.NotificationTemplate;
import org.springframework.stereotype.Component;

/**
 * Notifies the CLIENT that the professional proposed a new appointment time.
 */
@Component
public class ProposalReceivedTemplate implements NotificationTemplate {

    @Override
    public NotificationType type() {
        return NotificationType.PROPOSAL_RECEIVED;
    }

    @Override
    public NotificationPayload build(Appointment apt) {
        var tz = TemplateUtils.timezone(apt);
        var studio = apt.getStudio().getName();
        var clientName = apt.getClient().getFirstName();
        var professionalName = apt.getProfessional().getFirstName() + " " + apt.getProfessional().getLastName();
        var proposedDate = TemplateUtils.formatDate(apt.getProposedStart(), tz);
        var proposedTime = TemplateUtils.formatTime(apt.getProposedStart(), tz);
        var proposedEndTime = TemplateUtils.formatTime(apt.getProposedEnd(), tz);
        var actionLink = "/booking/confirm/" + apt.getToken();

        var subject = "Nuovo orario proposto per il suo appuntamento — " + studio;

        var body = """
                Gentile %s,
                
                %s le ha proposto un nuovo orario per il suo appuntamento
                presso %s.
                
                ─────────────────────────────────────
                NUOVO ORARIO PROPOSTO
                ─────────────────────────────────────
                Data   : %s
                Orario : %s – %s
                ─────────────────────────────────────
                
                Per accettare o rifiutare la proposta, visiti il link:
                %s
                
                Se non risponde entro il termine indicato, l'appuntamento
                potrebbe essere cancellato automaticamente.
                
                Cordiali saluti,
                %s
                """.formatted(clientName, professionalName, studio,
                proposedDate, proposedTime, proposedEndTime, actionLink, studio);

        var email = new EmailPayload(apt.getClient().getEmail(), clientName, subject, body);

        return new NotificationPayload(
                NotificationType.PROPOSAL_RECEIVED,
                NotificationChannel.EMAIL,
                RecipientType.CLIENT,
                apt.getClient().getId(),
                email
        );
    }
}

