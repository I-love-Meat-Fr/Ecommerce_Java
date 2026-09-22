package com.ecommerce.cnj70.controller.admin;

import com.ecommerce.cnj70.document.AuditLog;
import com.ecommerce.cnj70.enums.AuditAction;
import com.ecommerce.cnj70.enums.AuditSeverity;
import com.ecommerce.cnj70.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * TASK #23/#24 — Admin endpoint xem AuditLog.
 *
 * Endpoint cho Admin xem lịch sử action trên hệ thống.
 * - GET /api/admin/audit-logs: filter theo action/severity/resource/actor
 * - GET /api/admin/audit-logs/resource/{type}/{id}: lịch sử resource cụ thể
 * - GET /api/admin/audit-logs/actor/{actorId}: lịch sử actor
 */
@RestController
@RequestMapping("/api/admin/audit-logs")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminAuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> list(
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) AuditSeverity severity,
            @RequestParam(required = false) String actorId,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) String resourceId,
            Pageable pageable
    ) {
        Page<AuditLog> page;

        boolean hasResource = resourceType != null && resourceId != null;
        boolean hasActor = actorId != null;

        if (action != null) {
            page = auditLogService.findByAction(action, pageable);
        } else if (severity != null) {
            page = auditLogService.findBySeverity(severity, pageable);
        } else if (hasResource) {
            page = auditLogService.findByResource(resourceType, resourceId, pageable);
        } else if (hasActor) {
            page = auditLogService.findByActor(actorId, pageable);
        } else {
            page = Page.empty();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("content", page.getContent());
        response.put("totalElements", page.getTotalElements());
        response.put("totalPages", page.getTotalPages());
        response.put("currentPage", page.getNumber());
        response.put("size", page.getSize());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/resource/{resourceType}/{resourceId}")
    public ResponseEntity<Page<AuditLog>> findByResource(
            @PathVariable String resourceType,
            @PathVariable String resourceId,
            Pageable pageable
    ) {
        return ResponseEntity.ok(auditLogService.findByResource(resourceType, resourceId, pageable));
    }

    @GetMapping("/actor/{actorId}")
    public ResponseEntity<Page<AuditLog>> findByActor(
            @PathVariable String actorId,
            Pageable pageable
    ) {
        return ResponseEntity.ok(auditLogService.findByActor(actorId, pageable));
    }
}
