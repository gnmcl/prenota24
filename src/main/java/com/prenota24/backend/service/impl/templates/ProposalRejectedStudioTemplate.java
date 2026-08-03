package com.prenota24.backend.service.impl.templates;

import org.springframework.stereotype.Component;

import com.prenota24.backend.domain.Appointment;
import com.prenota24.backend.domain.NotificationChannel;
import com.prenota24.backend.domain.NotificationType;
import com.prenota24.backend.domain.RecipientType;
import com.prenota24.backend.dto.EmailPayload;
import com.prenota24.backend.dto.NotificationPayload;
import com.prenota24.backend.email.BaseEmailLayout;
import com.prenota24.backend.service.NotificationTemplate;

/**
 * Notifica il PROFESSIONISTA che il cliente ha rifiutato tutte le proposte
 * e l'appuntamento è stato annullato.
 */
@Component
public class ProposalRejectedStudioTemplate implements NotificationTemplate {

    @Override
    public NotificationType type() {
        return NotificationType.PROPOSAL_REJECTED_STUDIO;
    }

    @Override
    public NotificationPayload build(Appointment apt) {
        var professionalName = apt.getProfessional().getFirstName();
        var clientFullName = apt.getClient().getFirstName() + " " + apt.getClient().getLastName();
        var studio = apt.getStudio().getName();

        var subject = "Nessuna proposta accettata — " + clientFullName;

        var body = """
                Gentile %s,

                Il cliente %s ha rifiutato tutte le proposte di orario.

                L'appuntamento è stato annullato.

                Cordiali saluti,
                Prenota24
                """.formatted(professionalName, clientFullName);

        var content = "<p style=\"font-size:16px;color:#111827;margin:0 0 12px 0;\">"
            + "Gentile <strong>" + BaseEmailLayout.e(professionalName) + "</strong>,</p>\n"
            + "<p style=\"font-size:15px;color:#374151;margin:0 0 4px 0;\">Il cliente <strong>"
            + BaseEmailLayout.e(clientFullName) + "</strong> ha rifiutato tutte le proposte di orario.</p>\n"
            + "<p style=\"font-size:15px;color:#374151;margin:0 0 24px 0;\">"
            + "L&rsquo;appuntamento &egrave; stato <strong>annullato</strong>.</p>\n"
            + "<p style=\"font-size:14px;color:#374151;margin:0;\">Cordiali saluti,<br>"
            + "<strong>Prenota24</strong></p>";

        var html = BaseEmailLayout.wrap("Nessuna proposta accettata da " + clientFullName, content);

        var email = new EmailPayload(apt.getProfessional().getEmail(), professionalName, subject, body, html);

        return new NotificationPayload(
                NotificationType.PROPOSAL_REJECTED_STUDIO,
                NotificationChannel.EMAIL,
                RecipientType.PROFESSIONAL,
                apt.getProfessional().getId(),
                email
        );
    }
}
