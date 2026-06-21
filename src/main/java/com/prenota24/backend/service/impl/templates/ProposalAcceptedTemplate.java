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
        // After ACCEPT_PROPOSAL, proposedStart/proposedEnd are cleared and the new slot
        // is copied into startDatetime/endDatetime.
        var date = TemplateUtils.formatDate(apt.getStartDatetime(), tz);
        var time = TemplateUtils.formatTime(apt.getStartDatetime(), tz);
        var endTime = TemplateUtils.formatTime(apt.getEndDatetime(), tz);

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

        String[][] rows = {
            { "Cliente",    BaseEmailLayout.e(clientFullName) },
            { "Prestazione", BaseEmailLayout.e(serviceName) },
            { "Data",       BaseEmailLayout.e(date) },
            { "Orario",     BaseEmailLayout.e(time) + " &ndash; " + BaseEmailLayout.e(endTime) }
        };

        var content = "<p style=\"font-size:16px;color:#111827;margin:0 0 12px 0;\">"
            + "Gentile <strong>" + BaseEmailLayout.e(professionalName) + "</strong>,</p>\n"
            + "<p style=\"font-size:15px;color:#374151;margin:0 0 4px 0;\">Il cliente <strong>"
            + BaseEmailLayout.e(clientFullName) + "</strong> ha "
            + BaseEmailLayout.successBadge("accettato") + " il nuovo orario da lei proposto.</p>\n"
            + BaseEmailLayout.infoTable(rows)
            + "<p style=\"font-size:14px;color:#6B7280;margin:0 0 24px 0;\">"
            + "L&rsquo;appuntamento &egrave; ora <strong>confermato</strong> al nuovo orario.</p>\n"
            + "<p style=\"font-size:14px;color:#374151;margin:0;\">Cordiali saluti,<br>"
            + "<strong>Prenota24</strong></p>";

        var html = BaseEmailLayout.wrap("Proposta orario accettata da " + clientFullName, content);

        var email = new EmailPayload(apt.getProfessional().getEmail(), professionalName, subject, body, html);

        return new NotificationPayload(
                NotificationType.PROPOSAL_ACCEPTED,
                NotificationChannel.EMAIL,
                RecipientType.PROFESSIONAL,
                apt.getProfessional().getId(),
                email
        );
    }
}

