package com.prenota24.backend.service.impl;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.prenota24.backend.domain.ServiceType;

@Component
public class ServiceTypeAssignmentValidator {

    public void validate(ServiceType serviceType, UUID professionalId) {
        var assignedProfessionals = serviceType.getProfessionals();
        if (!assignedProfessionals.isEmpty()
                && assignedProfessionals.stream().noneMatch(p -> p.getId().equals(professionalId))) {
            throw new IllegalArgumentException("Il professionista selezionato non offre il servizio richiesto");
        }
    }
}
