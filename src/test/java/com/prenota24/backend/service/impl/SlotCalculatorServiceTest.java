package com.prenota24.backend.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyShort;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.prenota24.backend.domain.Availability;
import com.prenota24.backend.domain.Studio;
import com.prenota24.backend.dto.TimeSlotResponse;
import com.prenota24.backend.repository.AppointmentRepository;
import com.prenota24.backend.repository.AvailabilityExceptionRepository;
import com.prenota24.backend.repository.AvailabilityRepository;

class SlotCalculatorServiceTest {

    private static final ZoneId ROME = ZoneId.of("Europe/Rome");
    private static final LocalDate MONDAY = LocalDate.of(2027, 3, 1);

    private final AvailabilityRepository availabilityRepository = mock(AvailabilityRepository.class);
    private final AvailabilityExceptionRepository exceptionRepository = mock(AvailabilityExceptionRepository.class);
    private final AppointmentRepository appointmentRepository = mock(AppointmentRepository.class);
    private final SlotCalculatorService calculator =
            new SlotCalculatorService(availabilityRepository, exceptionRepository, appointmentRepository);

    private final UUID professionalId = UUID.randomUUID();
    private final Studio studio = Studio.builder().id(UUID.randomUUID()).timezone("Europe/Rome").build();

    @Test
    void usesEveryWindowOfTheDay() {
        windows(window("09:00", "11:00"), window("15:00", "17:00"));

        var starts = startTimes(calculator.calculateSlots(professionalId, MONDAY, 60, studio));

        assertThat(starts).containsExactly(
                LocalTime.of(9, 0), LocalTime.of(10, 0), LocalTime.of(15, 0), LocalTime.of(16, 0));
    }

    @Test
    void overlappingWindowsDoNotDuplicateSlots() {
        windows(window("09:00", "11:00"), window("09:00", "10:00"));

        var starts = startTimes(calculator.calculateSlots(professionalId, MONDAY, 60, studio));

        assertThat(starts).containsExactly(LocalTime.of(9, 0), LocalTime.of(10, 0));
    }

    @Test
    @Timeout(5)
    void windowEndingNearMidnightTerminates() {
        // Prima: 23:30 + 30 minuti = 00:00, che è "prima" di 23:59 → ciclo infinito.
        windows(window("22:30", "23:59"));

        var starts = startTimes(calculator.calculateSlots(professionalId, MONDAY, 30, studio));

        assertThat(starts).containsExactly(LocalTime.of(22, 30), LocalTime.of(23, 0));
    }

    private void windows(Availability... availabilities) {
        when(availabilityRepository.findByProfessionalIdAndDayOfWeek(eq(professionalId), anyShort()))
                .thenReturn(List.of(availabilities));
        when(exceptionRepository.findByProfessionalIdAndDate(professionalId, MONDAY)).thenReturn(Optional.empty());
        when(appointmentRepository.findActiveForProfessionalInRange(eq(professionalId), any(), any()))
                .thenReturn(List.of());
    }

    private static Availability window(String start, String end) {
        return Availability.builder()
                .dayOfWeek((short) 1)
                .startTime(LocalTime.parse(start))
                .endTime(LocalTime.parse(end))
                .build();
    }

    private static List<LocalTime> startTimes(List<TimeSlotResponse> slots) {
        return slots.stream().map(slot -> slot.start().atZone(ROME).toLocalTime()).toList();
    }
}
