package com.prenota24.backend.service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.prenota24.backend.dto.AppointmentResponse;
import com.prenota24.backend.dto.PublicBookingRequest;
import com.prenota24.backend.dto.ServiceTypeResponse;
import com.prenota24.backend.dto.StudioPublicResponse;
import com.prenota24.backend.dto.TimeSlotResponse;

public interface IPublicBookingService {

    StudioPublicResponse getStudio(String studioSlug);

    List<ServiceTypeResponse> getServices(String studioSlug);

    List<TimeSlotResponse> getAvailableSlots(
            String studioSlug,
            UUID professionalId,
            LocalDate date,
            UUID serviceTypeId,
            int durationMinutes);

    AppointmentResponse createAppointment(String studioSlug, PublicBookingRequest request);
}
