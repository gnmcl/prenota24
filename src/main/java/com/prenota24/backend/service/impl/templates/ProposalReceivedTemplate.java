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
 * Notifies the CLIENT that the professional proposed a new appointment time.
 * The email contains a CTA link to the frontend proposal page where the client
 * can accept the new time or book a different slot.
 */
@Component
public class ProposalReceivedTemplate implements NotificationTemplate {

    /** Base URL of the Angular frontend, e.g. https://prenota24.com */
    private final String frontendUrl;

    public ProposalReceivedTemplate(@Value("${app.frontend-url}") String frontendUrl) {
        this.frontendUrl = frontendUrl;
    }

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

        // FE page that shows the proposal and lets the client accept or book a new time
        var actionPageUrl = frontendUrl + "/booking/confirm/" + apt.getToken();

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
                
                Per rispondere alla proposta clicchi sul link qui sotto:
                %s
                
                Dalla pagina potrà:
                ✅ Accettare il nuovo orario
                🔄 Rifiutare e prenotare un orario diverso
                
                Il link è valido finché l'appuntamento è in stato "proposta in attesa".
                
                Se non riconosce questa comunicazione, ignori questa email.
                
                Cordiali saluti,
                %s
                """.formatted(clientName, professionalName, studio,
                proposedDate, proposedTime, proposedEndTime, actionPageUrl, studio);

        String[][] rows = {
            { "Data",   BaseEmailLayout.e(proposedDate) },
            { "Orario", BaseEmailLayout.e(proposedTime) + " &ndash; " + BaseEmailLayout.e(proposedEndTime) }
        };

        var content = "<p style=\"font-size:16px;color:#111827;margin:0 0 12px 0;\">"
            + "Gentile <strong>" + BaseEmailLayout.e(clientName) + "</strong>,</p>\n"
            + "<p style=\"font-size:15px;color:#374151;margin:0 0 4px 0;\">"
            + "<strong>" + BaseEmailLayout.e(professionalName) + "</strong> le ha proposto un "
            + "<strong>nuovo orario</strong> per il suo appuntamento presso <strong>"
            + BaseEmailLayout.e(studio) + "</strong>.</p>\n"
            + BaseEmailLayout.infoTable(rows)
            + "<p style=\"font-size:14px;color:#6B7280;margin:0 0 4px 0;\">Dalla pagina potr&agrave;:</p>\n"
            + "<ul style=\"font-size:14px;color:#6B7280;margin:0 0 16px 0;padding-left:20px;\">\n"
            + "  <li style=\"margin-bottom:4px;\">Accettare il nuovo orario</li>\n"
            + "  <li>Rifiutare e prenotare un orario diverso</li>\n"
            + "</ul>\n"
            + BaseEmailLayout.ctaButton("Rispondi alla proposta", actionPageUrl)
            + "<p style=\"font-size:12px;color:#9CA3AF;margin:16px 0 0 0;\">"
            + "Il link &egrave; valido finch&eacute; l&rsquo;appuntamento &egrave; in stato &ldquo;proposta in attesa&rdquo;. "
            + "Se non riconosce questa comunicazione, ignori questa email.</p>\n"
            + "<p style=\"font-size:14px;color:#374151;margin:24px 0 0 0;\">Cordiali saluti,<br>"
            + "<strong>" + BaseEmailLayout.e(studio) + "</strong></p>";

        var html = BaseEmailLayout.wrap("Nuovo orario proposto per il tuo appuntamento", content);

        var email = new EmailPayload(apt.getClient().getEmail(), clientName, subject, body, html);

        return new NotificationPayload(
                NotificationType.PROPOSAL_RECEIVED,
                NotificationChannel.EMAIL,
                RecipientType.CLIENT,
                apt.getClient().getId(),
                email
        );
    }
}

