package com.prenota24.backend.repository;

import com.prenota24.backend.domain.Client;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClientRepository extends JpaRepository<Client, UUID> {

    Page<Client> findByStudioId(UUID studioId, Pageable pageable);

    Optional<Client> findByStudioIdAndEmailIgnoreCase(UUID studioId, String email);

    Optional<Client> findByIdAndStudioId(UUID id, UUID studioId);

    @Query("""
            SELECT c FROM Client c
            WHERE c.studio.id = :studioId
            AND (
                LOWER(c.firstName) LIKE LOWER(CONCAT('%', :q, '%')) OR
                LOWER(c.lastName)  LIKE LOWER(CONCAT('%', :q, '%')) OR
                LOWER(c.email)     LIKE LOWER(CONCAT('%', :q, '%')) OR
                c.phone            LIKE CONCAT('%', :q, '%')
            )
            ORDER BY c.lastName ASC, c.firstName ASC
            """)
    Page<Client> search(@Param("studioId") UUID studioId, @Param("q") String q, Pageable pageable);

    @Query("SELECT DISTINCT a.client FROM Appointment a WHERE a.professional.id = :professionalId ORDER BY a.client.lastName ASC")
    List<Client> findClientsByProfessionalId(@Param("professionalId") UUID professionalId);
}
