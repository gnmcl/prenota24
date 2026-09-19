package com.prenota24.backend.repository;

import com.prenota24.backend.domain.ConversationConsentAudit;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationConsentAuditRepository extends JpaRepository<ConversationConsentAudit, UUID> {}
