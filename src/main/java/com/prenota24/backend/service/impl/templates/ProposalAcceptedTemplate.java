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
 * Notifies the PROFESSIONAL that the client accepted the proposed time.
 */
@Component
public class ProposalAcceptedTemplate implements NotificationTemplate {

    @Override
    public NotificationType type() {
        return NotificationType.PROPOSAL_ACCEPTED;
    }

    @Override
    public NotificationPayload build(Appointment apt) {
        var tz = TemplateUtils.timezone(apt);
        var professionalName = apt.getProfessional().getFirstName();
        var clientFullName = apt.getClient().getFirstName() + " " + apt.getClient().getLastName();
        var serviceName = apt.getServiceType() != null ? apt.getServiceType().getName() : "Visita";
        var date = TemplateUtils.formatDate(apt.getProposedStart(), tz);
        var time = TemplateUtils.formatTime(apt.getProposedStart(), tz);
        var endTime = TemplateUtils.formatTime(apt.getProposedEnd(), tz);

        var subject = "Proposta orario accettata — " + clientFullName;

        var body = """
                Gentile %s,
                
                il cliente %s ha accettato il nuovo orario da lei proposto.
                
                ─────────────────────────────────────
                RIEPILOGO APPUNTAMENTO AGGIORNATO
                ─────────────────────────────────────
                Cliente    : %s
                Prestazione: %s
                Data       : %s
                Orario     : %s – %s
                ─────────────────────────────────────
                
                L'appuntamento è ora confermato al nuovo orario.
                
                Cordiali saluti,
                Prenota24
                """.formatted(professionalName, clientFullName, clientFullName, serviceName, date, time, endTime);

        var email = new EmailPayload(apt.getProfessional().getEmail(), professionalName, subject, body);

        return new NotificationPayload(
                NotificationType.PROPOSAL_ACCEPTED,
                NotificationChannel.EMAIL,
                RecipientType.PROFESSIONAL,
                apt.getProfessional().getId(),
                email
        );
    }
}

