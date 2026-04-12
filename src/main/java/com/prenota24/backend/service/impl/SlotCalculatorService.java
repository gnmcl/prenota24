package com.prenota24.backend.service.impl;

import com.prenota24.backend.domain.AppointmentStatus;
import com.prenota24.backend.domain.Studio;
import com.prenota24.backend.dto.TimeSlotResponse;
import com.prenota24.backend.repository.AppointmentRepository;
import com.prenota24.backend.repository.AvailabilityExceptionRepository;
import com.prenota24.backend.repository.AvailabilityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SlotCalculatorService {

    private final AvailabilityRepository availabilityRepository;
    private final AvailabilityExceptionRepository exceptionRepository;
    private final AppointmentRepository appointmentRepository;

    /**
     * Calculate available time slots for a professional on a given date.
     * Takes into account weekly availability, exceptions, and existing appointments.
     */
    public List<TimeSlotResponse> calculateSlots(UUID professionalId, LocalDate date,
                                                  int durationMinutes, Studio studio) {
        ZoneId zone = ZoneId.of(studio.getTimezone() != null ? studio.getTimezone() : "Europe/Rome");

        // 1. Get weekly availability for this day of week
        short dayOfWeek = (short) date.getDayOfWeek().getValue();
        var availabilities = availabilityRepository.findByProfessionalIdAndDayOfWeek(professionalId, dayOfWeek);

        if (availabilities.isEmpty()) {
            return List.of();
        }

        // 2. Check exceptions for this specific date
        var exceptionOpt = exceptionRepository.findByProfessionalIdAndDate(professionalId, date);

        LocalTime workStart;
        LocalTime workEnd;

        if (exceptionOpt.isPresent()) {
            var exception = exceptionOpt.get();
            if (exception.isUnavailable()) {
                return List.of(); // Completely unavailable this day
            }
            // Use custom hours from exception
            workStart = exception.getStartTime();
            workEnd = exception.getEndTime();
        } else {
            // Use regular availability (take first slot — can be extended to support multiple)
            var availability = availabilities.getFirst();
            workStart = availability.getStartTime();
            workEnd = availability.getEndTime();
        }

        // 3. Get existing appointments for this professional on this date
        Instant dayStart = date.atStartOfDay(zone).toInstant();
        Instant dayEnd = date.plusDays(1).atStartOfDay(zone).toInstant();

        // Use a nil UUID as excludeId since we're not excluding any appointment
        UUID nilId = new UUID(0, 0);

        // Get all conflicting appointments for the entire day
        var existingAppointments = appointmentRepository.findByStudioId(
                studio.getId(),
                org.springframework.data.domain.Pageable.unpaged()
        ).getContent().stream()
                .filter(a -> a.getProfessional().getId().equals(professionalId))
                .filter(a -> a.getStatus() == AppointmentStatus.REQUESTED
                        || a.getStatus() == AppointmentStatus.CONFIRMED
                        || a.getStatus() == AppointmentStatus.PROPOSED_NEW_TIME)
                .filter(a -> a.getStartDatetime().isBefore(dayEnd) && a.getEndDatetime().isAfter(dayStart))
                .toList();

        // 4. Generate slots
        List<TimeSlotResponse> slots = new ArrayList<>();
        LocalTime cursor = workStart;
        Duration slotDuration = Duration.ofMinutes(durationMinutes);

        while (cursor.plus(slotDuration).compareTo(workEnd) <= 0) {
            Instant slotStart = date.atTime(cursor).atZone(zone).toInstant();
            Instant slotEnd = slotStart.plus(slotDuration);

            // Check if this slot overlaps with any existing appointment
            boolean hasConflict = existingAppointments.stream().anyMatch(a ->
                    a.getStartDatetime().isBefore(slotEnd) && a.getEndDatetime().isAfter(slotStart)
            );

            if (!hasConflict) {
                slots.add(new TimeSlotResponse(slotStart, slotEnd));
            }

            cursor = cursor.plusMinutes(durationMinutes);
        }

        return slots;
    }
}
