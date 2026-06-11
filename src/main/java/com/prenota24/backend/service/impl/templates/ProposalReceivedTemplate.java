package com.prenota24.backend.service.impl.templates;

import com.prenota24.backend.domain.Appointment;
import com.prenota24.backend.domain.NotificationChannel;
import com.prenota24.backend.domain.NotificationType;
import com.prenota24.backend.domain.RecipientType;
import com.prenota24.backend.dto.EmailPayload;
import com.prenota24.backend.dto.NotificationPayload;
import com.prenota24.backend.service.NotificationTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Notifies the CLIENT that the professional proposed a new appointment time.
 * The email contains two action links:
 *  - Accept  → FE page calls POST /api/public/appointments/{token}/accept
 *  - Reject  → FE redirects to the studio public booking page
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

