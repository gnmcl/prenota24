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

        String[][] rows = {
            { "Professionista", BaseEmailLayout.e(professionalName) },
            { "Prestazione",    BaseEmailLayout.e(serviceName) },
            { "Data",           BaseEmailLayout.e(date) },
            { "Orario",         BaseEmailLayout.e(time) }
        };

        var content = "<p style=\"font-size:16px;color:#111827;margin:0 0 12px 0;\">"
            + "Gentile <strong>" + BaseEmailLayout.e(clientName) + "</strong>,</p>\n"
            + "<p style=\"font-size:15px;color:#374151;margin:0 0 4px 0;\">"
            + "Le ricordiamo il suo appuntamento previsto per <strong>domani</strong>"
            + " presso <strong>" + BaseEmailLayout.e(studio) + "</strong>.</p>\n"
            + BaseEmailLayout.infoTable(rows)
            + "<p style=\"font-size:14px;color:#6B7280;margin:0 0 6px 0;\">"
            + "La preghiamo di presentarsi qualche minuto prima dell&rsquo;orario.</p>\n"
            + "<p style=\"font-size:14px;color:#6B7280;margin:0 0 24px 0;\">"
            + "Se non pu&ograve; essere presente, la invitiamo a disdire il prima possibile contattando lo studio.</p>\n"
            + "<p style=\"font-size:14px;color:#374151;margin:0;\">Cordiali saluti,<br>"
            + "<strong>" + BaseEmailLayout.e(studio) + "</strong></p>";

        var html = BaseEmailLayout.wrap("Promemoria appuntamento domani", content);

        var email = new EmailPayload(apt.getClient().getEmail(), clientName, subject, body, html);

        return new NotificationPayload(
                NotificationType.REMINDER_24H,
                NotificationChannel.EMAIL,
                RecipientType.CLIENT,
                apt.getClient().getId(),
                email
        );
    }
}

