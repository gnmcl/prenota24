package com.prenota24.backend.service.impl;

import com.prenota24.backend.common.EntityNotFoundException;
import com.prenota24.backend.domain.Professional;
import com.prenota24.backend.domain.ServiceType;
import com.prenota24.backend.dto.CreateServiceTypeRequest;
import com.prenota24.backend.dto.ServiceTypeResponse;
import com.prenota24.backend.repository.ProfessionalRepository;
import com.prenota24.backend.repository.ServiceTypeRepository;
import com.prenota24.backend.repository.StudioRepository;
import com.prenota24.backend.service.IServiceTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ServiceTypeService implements IServiceTypeService {

    private final ServiceTypeRepository serviceTypeRepository;
    private final StudioRepository studioRepository;
    private final ProfessionalRepository professionalRepository;

    @Override
    @Transactional
    public ServiceTypeResponse create(CreateServiceTypeRequest request, UUID studioId) {
        var studio = studioRepository.findById(studioId)
                .orElseThrow(() -> new EntityNotFoundException("Studio non trovato"));

        var serviceType = ServiceType.builder()
                .studio(studio)
                .name(request.name())
                .description(request.description())
                .durationMinutes(request.durationMinutes())
                .price(request.price())
                .color(request.color())
                .active(true)
                .professionals(resolveProfessionals(request.professionalIds(), studioId))
                .build();

        serviceType = serviceTypeRepository.save(serviceType);
        return toResponse(serviceType);
    }

    @Override
    @Transactional(readOnly = true)
    public ServiceTypeResponse getById(UUID id, UUID studioId) {
        return toResponse(findByIdAndStudio(id, studioId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ServiceTypeResponse> getByStudio(UUID studioId) {
        return serviceTypeRepository.findByStudioIdAndActiveTrue(studioId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public ServiceTypeResponse update(UUID id, CreateServiceTypeRequest request, UUID studioId) {
        var serviceType = findByIdAndStudio(id, studioId);

        serviceType.setName(request.name());
        serviceType.setDescription(request.description());
        serviceType.setDurationMinutes(request.durationMinutes());
        serviceType.setPrice(request.price());
        serviceType.setColor(request.color());
        serviceType.setProfessionals(resolveProfessionals(request.professionalIds(), studioId));

        serviceType = serviceTypeRepository.save(serviceType);
        return toResponse(serviceType);
    }

    @Override
    @Transactional
    public void delete(UUID id, UUID studioId) {
        var serviceType = findByIdAndStudio(id, studioId);
        serviceType.setActive(false);
        serviceTypeRepository.save(serviceType);
    }

    private ServiceType findByIdAndStudio(UUID id, UUID studioId) {
        return serviceTypeRepository.findByIdAndStudioId(id, studioId)
                .orElseThrow(() -> new EntityNotFoundException("Tipo di servizio non trovato"));
    }

    private Set<Professional> resolveProfessionals(List<UUID> ids, UUID studioId) {
        if (ids == null || ids.isEmpty()) return new HashSet<>();
        var set = new HashSet<Professional>();
        for (UUID pid : ids) {
            set.add(professionalRepository.findByIdAndStudioId(pid, studioId)
                    .orElseThrow(() -> new EntityNotFoundException("Professionista non trovato: " + pid)));
        }
        return set;
    }

    private ServiceTypeResponse toResponse(ServiceType s) {
        return new ServiceTypeResponse(
                s.getId(), s.getStudio().getId(),
                s.getProfessionals().stream().map(Professional::getId).toList(),
                s.getName(), s.getDescription(),
                s.getDurationMinutes(), s.getPrice(),
                s.getColor(), s.isActive(),
                s.getCreatedAt()
        );
    }
}
