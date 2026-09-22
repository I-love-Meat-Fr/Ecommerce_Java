package com.ecommerce.cnj70.repository;

import com.ecommerce.cnj70.document.AuditLog;
import com.ecommerce.cnj70.enums.AuditAction;
import com.ecommerce.cnj70.enums.AuditSeverity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends MongoRepository<AuditLog, String> {

    Page<AuditLog> findByActorId(String actorId, Pageable pageable);

    Page<AuditLog> findByAction(AuditAction action, Pageable pageable);

    Page<AuditLog> findByResourceTypeAndResourceId(String resourceType, String resourceId, Pageable pageable);

    Page<AuditLog> findByResourceId(String resourceId, Pageable pageable);

    Page<AuditLog> findBySeverity(AuditSeverity severity, Pageable pageable);

    List<AuditLog> findByCreatedAtBetween(LocalDateTime from, LocalDateTime to);

    Page<AuditLog> findByActorRole(String actorRole, Pageable pageable);

    /** Filter theo actor + action */
    Page<AuditLog> findByActorIdAndAction(String actorId, AuditAction action, Pageable pageable);

    /** Filter theo resource type + action */
    Page<AuditLog> findByResourceTypeAndAction(String resourceType, AuditAction action, Pageable pageable);
}
