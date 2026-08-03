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
 * Notifies the CLIENT that the professional proposed up to 3 new appointment times.
 * The email contains a CTA link to the frontend proposal page where the client
 * can select one of the proposed slots or decline all.
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

        var actionPageUrl = frontendUrl + "/booking/confirm/" + apt.getToken();

        var subject = "Nuovo orario proposto per il suo appuntamento — " + studio;

        // Build text-only slot list
        var sbText = new StringBuilder();
        sbText.append("1. ").append(TemplateUtils.formatDate(apt.getProposedStart(), tz))
              .append(" alle ").append(TemplateUtils.formatTime(apt.getProposedStart(), tz))
              .append(" – ").append(TemplateUtils.formatTime(apt.getProposedEnd(), tz));
        if (apt.getProposedStart2() != null) {
            sbText.append("\n2. ").append(TemplateUtils.formatDate(apt.getProposedStart2(), tz))
                  .append(" alle ").append(TemplateUtils.formatTime(apt.getProposedStart2(), tz))
                  .append(" – ").append(TemplateUtils.formatTime(apt.getProposedEnd2(), tz));
        }
        if (apt.getProposedStart3() != null) {
            sbText.append("\n3. ").append(TemplateUtils.formatDate(apt.getProposedStart3(), tz))
                  .append(" alle ").append(TemplateUtils.formatTime(apt.getProposedStart3(), tz))
                  .append(" – ").append(TemplateUtils.formatTime(apt.getProposedEnd3(), tz));
        }

        var body = """
                Gentile %s,

                %s le ha proposto un nuovo orario per il suo appuntamento
                presso %s.

                ─────────────────────────────────────
                ORARI PROPOSTI
                ─────────────────────────────────────
                %s
                ─────────────────────────────────────

                Per rispondere alla proposta clicchi sul link qui sotto:
                %s

                Dalla pagina potrà scegliere uno degli orari proposti
                oppure selezionare "Nessun orario va bene".

                Il link è valido finché l'appuntamento è in stato "proposta in attesa".

                Se non riconosce questa comunicazione, ignori questa email.

                Cordiali saluti,
                %s
                """.formatted(clientName, professionalName, studio,
                sbText.toString(), actionPageUrl, studio);

        // Build HTML slot rows
        var sbHtml = new StringBuilder();
        sbHtml.append("<p style=\"font-size:14px;font-weight:600;color:#111827;margin:0 0 8px 0;\">Orari proposti:</p>\n");
        sbHtml.append("<ol style=\"font-size:14px;color:#374151;margin:0 0 16px 0;padding-left:20px;\">\n");
        sbHtml.append("  <li style=\"margin-bottom:4px;\">")
              .append(BaseEmailLayout.e(TemplateUtils.formatDate(apt.getProposedStart(), tz)))
              .append(" &mdash; ").append(BaseEmailLayout.e(TemplateUtils.formatTime(apt.getProposedStart(), tz)))
              .append(" &ndash; ").append(BaseEmailLayout.e(TemplateUtils.formatTime(apt.getProposedEnd(), tz)))
              .append("</li>\n");
        if (apt.getProposedStart2() != null) {
            sbHtml.append("  <li style=\"margin-bottom:4px;\">")
                  .append(BaseEmailLayout.e(TemplateUtils.formatDate(apt.getProposedStart2(), tz)))
                  .append(" &mdash; ").append(BaseEmailLayout.e(TemplateUtils.formatTime(apt.getProposedStart2(), tz)))
                  .append(" &ndash; ").append(BaseEmailLayout.e(TemplateUtils.formatTime(apt.getProposedEnd2(), tz)))
                  .append("</li>\n");
        }
        if (apt.getProposedStart3() != null) {
            sbHtml.append("  <li>")
                  .append(BaseEmailLayout.e(TemplateUtils.formatDate(apt.getProposedStart3(), tz)))
                  .append(" &mdash; ").append(BaseEmailLayout.e(TemplateUtils.formatTime(apt.getProposedStart3(), tz)))
                  .append(" &ndash; ").append(BaseEmailLayout.e(TemplateUtils.formatTime(apt.getProposedEnd3(), tz)))
                  .append("</li>\n");
        }
        sbHtml.append("</ol>\n");

        var content = "<p style=\"font-size:16px;color:#111827;margin:0 0 12px 0;\">"
            + "Gentile <strong>" + BaseEmailLayout.e(clientName) + "</strong>,</p>\n"
            + "<p style=\"font-size:15px;color:#374151;margin:0 0 16px 0;\">"
            + "<strong>" + BaseEmailLayout.e(professionalName) + "</strong> le ha proposto un "
            + "<strong>nuovo orario</strong> per il suo appuntamento presso <strong>"
            + BaseEmailLayout.e(studio) + "</strong>.</p>\n"
            + sbHtml.toString()
            + BaseEmailLayout.ctaButton("Scegli l'orario", actionPageUrl)
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

