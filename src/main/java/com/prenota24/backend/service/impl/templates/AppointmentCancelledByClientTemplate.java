package com.prenota24.backend.service.impl.templates;

import com.prenota24.backend.domain.Appointment;
import com.prenota24.backend.domain.NotificationChannel;
import com.prenota24.backend.domain.NotificationType;
import com.prenota24.backend.domain.RecipientType;
import com.prenota24.backend.dto.EmailPayload;
import com.prenota24.backend.dto.NotificationPayload;
import com.prenota24.backend.email.BaseEmailLayout;
import com.prenota24.backend.service.NotificationTemplate;
import org.springframework.stereotype.Component;

/**
 * Notifies the PROFESSIONAL when a client cancels their appointment.
 */
@Component
public class AppointmentCancelledByClientTemplate implements NotificationTemplate {

    @Override
    public NotificationType type() {
        return NotificationType.APPOINTMENT_CANCELLED_BY_CLIENT;
    }

    @Override
    public NotificationPayload build(Appointment apt) {
        var tz = TemplateUtils.timezone(apt);
        var professionalName = apt.getProfessional().getFirstName();
        var clientFullName = apt.getClient().getFirstName() + " " + apt.getClient().getLastName();
        var datetime = TemplateUtils.formatDatetime(apt.getStartDatetime(), tz);
        var serviceName = apt.getServiceType() != null ? apt.getServiceType().getName() : "Visita";
        var reasonLine = apt.getCancellationReason() != null
                ? "\nMotivo indicato dal cliente: " + apt.getCancellationReason() + "\n"
                : "";

        var subject = "Appuntamento cancellato dal cliente — " + clientFullName;

        var body = """
                Gentile %s,
                
                il cliente %s ha cancellato il suo appuntamento.
                
                ─────────────────────────────────────
                DETTAGLI APPUNTAMENTO CANCELLATO
                ─────────────────────────────────────
                Cliente    : %s
                Prestazione: %s
                Data       : %s
                ─────────────────────────────────────
                %s
                Lo slot è ora nuovamente disponibile per nuove prenotazioni.
                
                Cordiali saluti,
                Prenota24
                """.formatted(professionalName, clientFullName, clientFullName, serviceName, datetime, reasonLine);

        String[][] rows = {
            { "Cliente",    BaseEmailLayout.e(clientFullName) },
            { "Prestazione", BaseEmailLayout.e(serviceName) },
            { "Data",       BaseEmailLayout.e(datetime) }
        };

        String reasonHtml = apt.getCancellationReason() != null
                ? "<div style=\"background-color:#FEF2F2;border-left:3px solid #FCA5A5;\n"
                  + "            border-radius:0 6px 6px 0;padding:12px 16px;margin:0 0 16px 0;\">\n"
                  + "  <p style=\"margin:0;font-size:13px;color:#B91C1C;font-weight:600;\">Motivo indicato dal cliente</p>\n"
                  + "  <p style=\"margin:4px 0 0 0;font-size:14px;color:#7F1D1D;\">"
                  + BaseEmailLayout.e(apt.getCancellationReason()) + "</p>\n"
                  + "</div>\n"
                : "";

        var content = "<p style=\"font-size:16px;color:#111827;margin:0 0 12px 0;\">"
            + "Gentile <strong>" + BaseEmailLayout.e(professionalName) + "</strong>,</p>\n"
            + "<p style=\"font-size:15px;color:#374151;margin:0 0 4px 0;\">Il cliente <strong>"
            + BaseEmailLayout.e(clientFullName) + "</strong> ha "
            + BaseEmailLayout.dangerBadge("cancellato") + " il suo appuntamento.</p>\n"
            + BaseEmailLayout.infoTable(rows)
            + reasonHtml
            + "<p style=\"font-size:14px;color:#6B7280;margin:0 0 24px 0;\">"
            + "Lo slot &egrave; ora nuovamente disponibile per nuove prenotazioni.</p>\n"
            + "<p style=\"font-size:14px;color:#374151;margin:0;\">Cordiali saluti,<br>"
            + "<strong>Prenota24</strong></p>";

        var html = BaseEmailLayout.wrap("Appuntamento cancellato da " + clientFullName, content);

        var email = new EmailPayload(apt.getProfessional().getEmail(), professionalName, subject, body, html);

        return new NotificationPayload(
                NotificationType.APPOINTMENT_CANCELLED_BY_CLIENT,
                NotificationChannel.EMAIL,
                RecipientType.PROFESSIONAL,
                apt.getProfessional().getId(),
                email
        );
    }
}

