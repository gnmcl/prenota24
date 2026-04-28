package com.prenota24.backend.service;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.prenota24.backend.domain.CancelledBy;
import com.prenota24.backend.dto.AppointmentResponse;
import com.prenota24.backend.dto.CancelAppointmentRequest;
import com.prenota24.backend.dto.CreateAppointmentRequest;
import com.prenota24.backend.dto.ProposeNewTimeRequest;
import com.prenota24.backend.dto.UpdateAppointmentRequest;

public interface IAppointmentService {

    AppointmentResponse create(CreateAppointmentRequest request, UUID studioId);

    AppointmentResponse getById(UUID id, UUID studioId);

    Page<AppointmentResponse> list(UUID studioId, String status, UUID professionalId, LocalDate startDate, LocalDate endDate, Pageable pageable);

    AppointmentResponse update(UUID id, UpdateAppointmentRequest request, UUID studioId);

    AppointmentResponse confirm(UUID id, UUID studioId);

    AppointmentResponse cancel(UUID id, CancelAppointmentRequest request, CancelledBy cancelledBy, UUID studioId);

    AppointmentResponse complete(UUID id, UUID studioId);

    AppointmentResponse noShow(UUID id, UUID studioId);

    AppointmentResponse proposeNewTime(UUID id, ProposeNewTimeRequest request, UUID studioId);

    AppointmentResponse acceptProposal(String token);

    AppointmentResponse rejectProposal(String token);

    // Public — by token
    AppointmentResponse getByToken(String token);
}
