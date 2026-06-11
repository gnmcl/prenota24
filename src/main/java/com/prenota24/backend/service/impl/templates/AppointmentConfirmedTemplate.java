package com.prenota24.backend.service.impl.templates;

import com.prenota24.backend.domain.Appointment;
import com.prenota24.backend.domain.NotificationChannel;
import com.prenota24.backend.domain.NotificationType;
import com.prenota24.backend.domain.RecipientType;
import com.prenota24.backend.dto.EmailPayload;
import com.prenota24.backend.dto.NotificationPayload;
import com.prenota24.backend.service.NotificationTemplate;
import org.springframework.stereotype.Component;

@Component
public class AppointmentConfirmedTemplate implements NotificationTemplate {

    @Override
    public NotificationType type() {
        return NotificationType.APPOINTMENT_CONFIRMED;
    }

    @Override
    public NotificationPayload build(Appointment apt) {
        var tz = TemplateUtils.timezone(apt);
        var studio = apt.getStudio().getName();
        var clientName = apt.getClient().getFirstName();
        var professionalName = apt.getProfessional().getFirstName() + " " + apt.getProfessional().getLastName();
        var serviceName = apt.getServiceType() != null ? apt.getServiceType().getName() : "Visita";
        var date = TemplateUtils.formatDate(apt.getStartDatetime(), tz);
        var time = TemplateUtils.formatTime(apt.getStartDatetime(), tz);
        var endTime = TemplateUtils.formatTime(apt.getEndDatetime(), tz);

        var subject = "Prenotazione confermata — " + studio;

        var body = """
                Gentile %s,
                
                la sua prenotazione presso %s è stata confermata con successo.
                
                ─────────────────────────────────────
                RIEPILOGO APPUNTAMENTO
                ─────────────────────────────────────
                Professionista : %s
                Prestazione    : %s
                Data           : %s
                Orario         : %s – %s
                ─────────────────────────────────────
                
                La preghiamo di presentarsi qualche minuto prima dell'orario previsto.
                
                Per modificare o cancellare l'appuntamento, contatti direttamente lo studio.
                
                Cordiali saluti,
                %s
                """.formatted(clientName, studio, professionalName, serviceName, date, time, endTime, studio);

        var email = new EmailPayload(apt.getClient().getEmail(), clientName, subject, body);

        return new NotificationPayload(
                NotificationType.APPOINTMENT_CONFIRMED,
                NotificationChannel.EMAIL,
                RecipientType.CLIENT,
                apt.getClient().getId(),
                email
        );
    }
}

