package com.prenota24.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AcceptInvitationRequest(
        @NotBlank String token,
        @NotBlank String name,
        @NotBlank @Size(min = 8) String password
) {}
