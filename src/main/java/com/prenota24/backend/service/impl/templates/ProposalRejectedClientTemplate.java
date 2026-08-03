package com.prenota24.backend.service.impl.templates;

import org.springframework.beans.factory.annotation.Value;
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
 * Notifica il CLIENTE che ha rifiutato tutte le proposte:
 * l'appuntamento è stato annullato e dovrà effettuare una nuova prenotazione.
 */
@Component
public class ProposalRejectedClientTemplate implements NotificationTemplate {

    private final String frontendUrl;

    public ProposalRejectedClientTemplate(@Value("${app.frontend-url}") String frontendUrl) {
        this.frontendUrl = frontendUrl;
    }

    @Override
    public NotificationType type() {
        return NotificationType.PROPOSAL_REJECTED_CLIENT;
    }

    @Override
    public NotificationPayload build(Appointment apt) {
        var studio = apt.getStudio().getName();
        var studioSlug = apt.getStudio().getSlug();
        var clientName = apt.getClient().getFirstName();

        var bookingUrl = frontendUrl + "/prenota/" + studioSlug;

        var subject = "Appuntamento annullato — " + studio;

        var body = """
                Gentile %s,

                Nessuna delle proposte disponibili è stata accettata.

                L'appuntamento è stato annullato.

                Per prenotare un nuovo appuntamento sarà necessario effettuare una nuova richiesta:
                %s

                Cordiali saluti,
                %s
                """.formatted(clientName, bookingUrl, studio);

        var content = "<p style=\"font-size:16px;color:#111827;margin:0 0 12px 0;\">"
            + "Gentile <strong>" + BaseEmailLayout.e(clientName) + "</strong>,</p>\n"
            + "<p style=\"font-size:15px;color:#374151;margin:0 0 16px 0;\">"
            + "Nessuna delle proposte disponibili &egrave; stata accettata.</p>\n"
            + "<p style=\"font-size:15px;color:#374151;margin:0 0 4px 0;\">"
            + "L&rsquo;appuntamento &egrave; stato <strong>annullato</strong>.</p>\n"
            + "<p style=\"font-size:14px;color:#6B7280;margin:0 0 24px 0;\">"
            + "Per prenotare un nuovo appuntamento sar&agrave; necessario effettuare una nuova richiesta.</p>\n"
            + BaseEmailLayout.ctaButton("Prenota un nuovo appuntamento", bookingUrl)
            + "<p style=\"font-size:14px;color:#374151;margin:24px 0 0 0;\">Cordiali saluti,<br>"
            + "<strong>" + BaseEmailLayout.e(studio) + "</strong></p>";

        var html = BaseEmailLayout.wrap("Appuntamento annullato", content);

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
