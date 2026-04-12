package com.prenota24.backend.controller;

import com.prenota24.backend.common.AuthHelper;
import com.prenota24.backend.dto.CreateServiceTypeRequest;
import com.prenota24.backend.dto.ServiceTypeResponse;
import com.prenota24.backend.service.IServiceTypeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/service-types")
@RequiredArgsConstructor
public class ServiceTypeController {

    private final IServiceTypeService serviceTypeService;
    private final AuthHelper authHelper;

    @GetMapping
    public List<ServiceTypeResponse> list(Authentication auth) {
        return serviceTypeService.getByStudio(authHelper.getStudioId(auth));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ServiceTypeResponse create(@RequestBody @Valid CreateServiceTypeRequest request,
                                       Authentication auth) {
        return serviceTypeService.create(request, authHelper.getStudioId(auth));
    }

    @GetMapping("/{id}")
    public ServiceTypeResponse getById(@PathVariable UUID id, Authentication auth) {
        return serviceTypeService.getById(id, authHelper.getStudioId(auth));
    }

    @PutMapping("/{id}")
    public ServiceTypeResponse update(@PathVariable UUID id,
                                       @RequestBody @Valid CreateServiceTypeRequest request,
                                       Authentication auth) {
        return serviceTypeService.update(id, request, authHelper.getStudioId(auth));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id, Authentication auth) {
        serviceTypeService.delete(id, authHelper.getStudioId(auth));
    }
}
