package com.prenota24.backend.service;

import com.prenota24.backend.dto.CreateServiceTypeRequest;
import com.prenota24.backend.dto.ServiceTypeResponse;

import java.util.List;
import java.util.UUID;

public interface IServiceTypeService {

    ServiceTypeResponse create(CreateServiceTypeRequest request, UUID studioId);

    ServiceTypeResponse getById(UUID id, UUID studioId);

    List<ServiceTypeResponse> getByStudio(UUID studioId);

    ServiceTypeResponse update(UUID id, CreateServiceTypeRequest request, UUID studioId);

    void delete(UUID id, UUID studioId);
}
