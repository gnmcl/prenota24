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

        String[][] rows = {
            { "Professionista", BaseEmailLayout.e(professionalName) },
            { "Prestazione",    BaseEmailLayout.e(serviceName) },
            { "Data",           BaseEmailLayout.e(date) },
            { "Orario",         BaseEmailLayout.e(time) + " &ndash; " + BaseEmailLayout.e(endTime) }
        };

        var content = "<p style=\"font-size:16px;color:#111827;margin:0 0 12px 0;\">"
            + "Gentile <strong>" + BaseEmailLayout.e(clientName) + "</strong>,</p>\n"
            + "<p style=\"font-size:15px;color:#374151;margin:0 0 4px 0;\">"
            + "La sua prenotazione presso <strong>" + BaseEmailLayout.e(studio) + "</strong> &egrave; stata "
            + BaseEmailLayout.successBadge("confermata") + " con successo.</p>\n"
            + BaseEmailLayout.infoTable(rows)
            + "<p style=\"font-size:14px;color:#6B7280;margin:0 0 6px 0;\">"
            + "La preghiamo di presentarsi qualche minuto prima dell&rsquo;orario previsto.</p>\n"
            + "<p style=\"font-size:14px;color:#6B7280;margin:0 0 24px 0;\">"
            + "Per modificare o cancellare l&rsquo;appuntamento, contatti direttamente lo studio.</p>\n"
            + "<p style=\"font-size:14px;color:#374151;margin:0;\">Cordiali saluti,<br>"
            + "<strong>" + BaseEmailLayout.e(studio) + "</strong></p>";

        var html = BaseEmailLayout.wrap("Prenotazione confermata presso " + studio, content);

        var email = new EmailPayload(apt.getClient().getEmail(), clientName, subject, body, html);

        return new NotificationPayload(
                NotificationType.APPOINTMENT_CONFIRMED,
                NotificationChannel.EMAIL,
                RecipientType.CLIENT,
                apt.getClient().getId(),
                email
        );
    }
}

