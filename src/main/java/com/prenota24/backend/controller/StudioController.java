package com.prenota24.backend.controller;

import com.prenota24.backend.dto.CreateStudioRequest;
import com.prenota24.backend.dto.StudioResponse;
import com.prenota24.backend.service.StudioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/studios")
@RequiredArgsConstructor
public class StudioController {

    private final StudioService studioService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StudioResponse create(@RequestBody @Valid CreateStudioRequest request) {
        return studioService.create(request);
    }

    @GetMapping("/{id}")
    public StudioResponse getById(@PathVariable("id") UUID id) {
        return studioService.getById(id);
    }
}
