package com.prenota24.backend.repository;

import com.prenota24.backend.domain.ServiceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ServiceTypeRepository extends JpaRepository<ServiceType, UUID> {

    List<ServiceType> findByStudioIdAndActiveTrue(UUID studioId);

    Optional<ServiceType> findByIdAndStudioId(UUID id, UUID studioId);
}
