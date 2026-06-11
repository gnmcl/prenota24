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
public class Reminder24hTemplate implements NotificationTemplate {

    @Override
    public NotificationType type() {
        return NotificationType.REMINDER_24H;
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

        var subject = "Promemoria: appuntamento domani — " + studio;

        var body = """
                Gentile %s,
                
                le ricordiamo il suo appuntamento previsto per domani.
                
                ─────────────────────────────────────
                RIEPILOGO APPUNTAMENTO
                ─────────────────────────────────────
                Professionista : %s
                Prestazione    : %s
                Data           : %s
                Orario         : %s
                ─────────────────────────────────────
                
                La preghiamo di presentarsi qualche minuto prima dell'orario.
                Se non può essere presente, la invitiamo a disdire il prima possibile
                contattando lo studio.
                
                Cordiali saluti,
                %s
                """.formatted(clientName, professionalName, serviceName, date, time, studio);

        var email = new EmailPayload(apt.getClient().getEmail(), clientName, subject, body);

        return new NotificationPayload(
                NotificationType.REMINDER_24H,
                NotificationChannel.EMAIL,
                RecipientType.CLIENT,
                apt.getClient().getId(),
                email
        );
    }
}

