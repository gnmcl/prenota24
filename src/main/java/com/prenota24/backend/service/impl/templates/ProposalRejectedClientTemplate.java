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
 * Notifica il CLIENTE che ha rifiutato le proposte di un nuovo orario.
 */
@Component
public class ProposalRejectedClientTemplate implements NotificationTemplate {

    @Override
    public NotificationType type() {
        return NotificationType.PROPOSAL_REJECTED_CLIENT;
    }

    @Override
    public NotificationPayload build(Appointment apt) {
        var studio = apt.getStudio().getName();
        var clientName = apt.getClient().getFirstName();
        var subject = "Proposte di orario rifiutate — " + studio;

        var body = """
                Gentile %s,

                Ha rifiutato le proposte di nuovo orario ricevute.

                L'appuntamento è di nuovo in attesa di conferma.

                Cordiali saluti,
                %s
                """.formatted(clientName, studio);

        var content = "<p style=\"font-size:16px;color:#111827;margin:0 0 12px 0;\">"
            + "Gentile <strong>" + BaseEmailLayout.e(clientName) + "</strong>,</p>\n"
            + "<p style=\"font-size:15px;color:#374151;margin:0 0 16px 0;\">"
            + "Ha rifiutato le proposte di nuovo orario ricevute.</p>\n"
            + "<p style=\"font-size:15px;color:#374151;margin:0 0 4px 0;\">"
            + "L&rsquo;appuntamento &egrave; di nuovo <strong>in attesa di conferma</strong>.</p>\n"
            + "<p style=\"font-size:14px;color:#374151;margin:24px 0 0 0;\">Cordiali saluti,<br>"
            + "<strong>" + BaseEmailLayout.e(studio) + "</strong></p>";

        var html = BaseEmailLayout.wrap("Proposte di orario rifiutate", content);

        var email = new EmailPayload(apt.getClient().getEmail(), clientName, subject, body, html);

        return new NotificationPayload(
                NotificationType.PROPOSAL_REJECTED_CLIENT,
                NotificationChannel.EMAIL,
                RecipientType.CLIENT,
                apt.getClient().getId(),
                email
        );
    }
}
