package com.prenota24.backend.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.prenota24.backend.common.EmailAlreadyRegisteredException;
import com.prenota24.backend.common.ResourceInUseException;
import com.prenota24.backend.domain.Appointment;
import com.prenota24.backend.domain.AppointmentStatus;
import com.prenota24.backend.domain.Client;
import com.prenota24.backend.domain.Notification;
import com.prenota24.backend.domain.NotificationType;
import com.prenota24.backend.domain.Studio;
import com.prenota24.backend.dto.CreateClientRequest;
import com.prenota24.backend.repository.AppUserRepository;
import com.prenota24.backend.repository.AppointmentRepository;
import com.prenota24.backend.repository.ClientNoteRepository;
import com.prenota24.backend.repository.ClientRepository;
import com.prenota24.backend.repository.StudioRepository;

class MediumSeverityFixesTest {

    // ── Promemoria ──────────────────────────────────────

    @Test
    void reminderForCancelledAppointmentIsStale() {
        var apt = appointment(AppointmentStatus.CANCELLED);
        assertThat(NotificationDispatcher.isStaleReminder(reminderFor(apt, ReminderScheduler.reminderTimeFor(apt))))
                .isTrue();
    }

    @Test
    void reminderForRescheduledAppointmentIsStale() {
        var apt = appointment(AppointmentStatus.CONFIRMED);
        var oldReminderTime = ReminderScheduler.reminderTimeFor(apt).minus(2, ChronoUnit.HOURS);
        assertThat(NotificationDispatcher.isStaleReminder(reminderFor(apt, oldReminderTime))).isTrue();
    }

    @Test
    void reminderForConfirmedAppointmentIsSent() {
        var apt = appointment(AppointmentStatus.CONFIRMED);
        assertThat(NotificationDispatcher.isStaleReminder(reminderFor(apt, ReminderScheduler.reminderTimeFor(apt))))
                .isFalse();
    }

    @Test
    void otherNotificationsAreNeverStale() {
        var apt = appointment(AppointmentStatus.CANCELLED);
        var notification = reminderFor(apt, Instant.now());
        notification.setType(NotificationType.APPOINTMENT_CANCELLED_BY_PROFESSIONAL.name());
        assertThat(NotificationDispatcher.isStaleReminder(notification)).isFalse();
    }

    // ── Clienti ──────────────────────────────────────

    private final ClientRepository clientRepository = mock(ClientRepository.class);
    private final StudioRepository studioRepository = mock(StudioRepository.class);
    private final AppointmentRepository appointmentRepository = mock(AppointmentRepository.class);
    private final ClientService clientService = new ClientService(
            clientRepository, mock(ClientNoteRepository.class), studioRepository,
            mock(AppUserRepository.class), appointmentRepository);
    private final Studio studio = Studio.builder().id(UUID.randomUUID()).name("Studio").build();

    @Test
    void duplicateClientEmailIsRejected() {
        when(studioRepository.findById(studio.getId())).thenReturn(Optional.of(studio));
        when(clientRepository.existsByStudioIdAndEmailIgnoreCase(studio.getId(), "mario@example.com")).thenReturn(true);

        assertThatThrownBy(() -> clientService.create(new CreateClientRequest(
                "Mario", "Rossi", " Mario@Example.com ", null, null, null), studio.getId()))
                .isInstanceOf(EmailAlreadyRegisteredException.class);
        verify(clientRepository, never()).save(any());
    }

    @Test
    void clientWithAppointmentsCannotBeDeleted() {
        var client = Client.builder().id(UUID.randomUUID()).studio(studio).firstName("Mario").lastName("Rossi").build();
        when(clientRepository.findByIdAndStudioId(client.getId(), studio.getId())).thenReturn(Optional.of(client));
        when(appointmentRepository.existsByClientId(client.getId())).thenReturn(true);

        assertThatThrownBy(() -> clientService.delete(client.getId(), studio.getId()))
                .isInstanceOf(ResourceInUseException.class);
        verify(clientRepository, never()).delete(any());
    }

    // ── Fuso orario ──────────────────────────────────────

    @Test
    void onlyIanaTimezonesAreAccepted() {
        assertThat(StudioService.validTimezone(" Europe/Rome ")).isEqualTo("Europe/Rome");
        assertThatThrownBy(() -> StudioService.validTimezone("Mars/Olympus"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> StudioService.validTimezone("+01:00"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatCode(() -> StudioService.validTimezone("America/New_York")).doesNotThrowAnyException();
    }

    private static Appointment appointment(AppointmentStatus status) {
        var start = Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
        return Appointment.builder()
                .id(UUID.randomUUID())
                .status(status)
                .startDatetime(start)
                .endDatetime(start.plus(1, ChronoUnit.HOURS))
                .build();
    }

    private static Notification reminderFor(Appointment apt, Instant scheduledAt) {
        return Notification.builder()
                .appointment(apt)
                .type(NotificationType.REMINDER_24H.name())
                .scheduledAt(scheduledAt)
                .build();
    }
}
