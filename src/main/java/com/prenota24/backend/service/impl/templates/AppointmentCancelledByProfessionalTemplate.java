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

        var email = new EmailPayload(apt.getClient().getEmail(), clientName, subject, body);

        return new NotificationPayload(
                NotificationType.APPOINTMENT_CANCELLED_BY_PROFESSIONAL,
                NotificationChannel.EMAIL,
                RecipientType.CLIENT,
                apt.getClient().getId(),
                email
        );
    }
}

