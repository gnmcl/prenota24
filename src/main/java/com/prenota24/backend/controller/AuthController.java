package com.prenota24.backend.controller;

import com.prenota24.backend.auth.AuthService;
import com.prenota24.backend.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public LoginResponse login(@RequestBody @Valid LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse register(@RequestBody @Valid RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/accept-invitation")
    @ResponseStatus(HttpStatus.CREATED)
    public LoginResponse acceptInvitation(@RequestBody @Valid AcceptInvitationRequest request) {
        return authService.acceptInvitation(request);
    }
}
