package com.prenota24.backend.repository;

import com.prenota24.backend.domain.Studio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface StudioRepository extends JpaRepository<Studio, UUID> {
}
