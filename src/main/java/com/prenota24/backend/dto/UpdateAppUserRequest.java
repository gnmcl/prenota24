package com.prenota24.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateAppUserRequest(
        @Size(max = 200) String name,
        @Email String email
) {}
