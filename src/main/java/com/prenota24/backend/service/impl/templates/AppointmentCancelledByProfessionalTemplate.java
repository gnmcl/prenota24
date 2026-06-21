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
 * Notifies the CLIENT when a professional cancels their appointment.
 */
@Component
public class AppointmentCancelledByProfessionalTemplate implements NotificationTemplate {

    @Override
    public NotificationType type() {
        return NotificationType.APPOINTMENT_CANCELLED_BY_PROFESSIONAL;
    }

    @Override
    public NotificationPayload build(Appointment apt) {
        var tz = TemplateUtils.timezone(apt);
        var studio = apt.getStudio().getName();
        var clientName = apt.getClient().getFirstName();
        var datetime = TemplateUtils.formatDatetime(apt.getStartDatetime(), tz);
        var reasonLine = apt.getCancellationReason() != null
                ? "\nMotivo comunicato dallo studio: " + apt.getCancellationReason() + "\n"
                : "";

        var subject = "Appuntamento cancellato — " + studio;

        var body = """
                Gentile %s,
                
                la informiamo che il suo appuntamento previsto per il %s
                presso %s è stato cancellato.
                %s
                Ci scusiamo per l'inconveniente.
                Per fissare un nuovo appuntamento o per ulteriori informazioni,
                la invitiamo a contattare direttamente lo studio.
                
                Cordiali saluti,
                %s
                """.formatted(clientName, datetime, studio, reasonLine, studio);

        String reasonHtml = apt.getCancellationReason() != null
                ? "<div style=\"background-color:#FEF2F2;border-left:3px solid #FCA5A5;\n"
                  + "            border-radius:0 6px 6px 0;padding:12px 16px;margin:16px 0;\">\n"
                  + "  <p style=\"margin:0;font-size:13px;color:#B91C1C;font-weight:600;\">Motivo comunicato dallo studio</p>\n"
                  + "  <p style=\"margin:4px 0 0 0;font-size:14px;color:#7F1D1D;\">"
                  + BaseEmailLayout.e(apt.getCancellationReason()) + "</p>\n"
                  + "</div>\n"
                : "";

        var content = "<p style=\"font-size:16px;color:#111827;margin:0 0 12px 0;\">"
            + "Gentile <strong>" + BaseEmailLayout.e(clientName) + "</strong>,</p>\n"
            + "<p style=\"font-size:15px;color:#374151;margin:0 0 4px 0;\">"
            + "La informiamo che il suo appuntamento previsto per il <strong>"
            + BaseEmailLayout.e(datetime) + "</strong> presso <strong>"
            + BaseEmailLayout.e(studio) + "</strong> &egrave; stato "
            + BaseEmailLayout.dangerBadge("cancellato") + ".</p>\n"
            + reasonHtml
            + "<p style=\"font-size:14px;color:#6B7280;margin:16px 0 0 0;\">"
            + "Ci scusiamo per l&rsquo;inconveniente. Per fissare un nuovo appuntamento o per ulteriori informazioni, "
            + "la invitiamo a contattare direttamente lo studio.</p>\n"
            + "<p style=\"font-size:14px;color:#374151;margin:24px 0 0 0;\">Cordiali saluti,<br>"
            + "<strong>" + BaseEmailLayout.e(studio) + "</strong></p>";

        var html = BaseEmailLayout.wrap("Appuntamento cancellato presso " + studio, content);

        var email = new EmailPayload(apt.getClient().getEmail(), clientName, subject, body, html);

        return new NotificationPayload(
                NotificationType.APPOINTMENT_CANCELLED_BY_PROFESSIONAL,
                NotificationChannel.EMAIL,
                RecipientType.CLIENT,
                apt.getClient().getId(),
                email
        );
    }
}

