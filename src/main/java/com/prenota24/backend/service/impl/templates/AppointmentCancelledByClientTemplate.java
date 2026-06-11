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

        var email = new EmailPayload(apt.getProfessional().getEmail(), professionalName, subject, body);

        return new NotificationPayload(
                NotificationType.APPOINTMENT_CANCELLED_BY_CLIENT,
                NotificationChannel.EMAIL,
                RecipientType.PROFESSIONAL,
                apt.getProfessional().getId(),
                email
        );
    }
}

