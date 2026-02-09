package com.prenota24.backend.controller;


import com.prenota24.backend.dto.AppUserResponse;
import com.prenota24.backend.dto.CreateAppUserRequest;
import com.prenota24.backend.service.IAppUserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class AppUserController {

    private final IAppUserService appUserService;

    @PostMapping("/create")
    @ResponseStatus(HttpStatus.CREATED)
    public AppUserResponse create(@RequestBody @Valid CreateAppUserRequest request) {
        return appUserService.create(request);
    }

    @GetMapping("/{id}")
    public AppUserResponse getById(@PathVariable @NotNull UUID id) {
        return appUserService.getById(id);
    }
}
