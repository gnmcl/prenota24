package com.prenota24.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateReservationRequest(
        @NotBlank @Size(max = 200) String guestName,
        @Email @NotBlank String guestEmail,
        @Size(max = 50) String guestPhone,
        String notes
) {
}
