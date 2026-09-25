package com.ecommerce.cnj70.controller.admin;

import com.ecommerce.cnj70.document.AuditLogEntry;
import com.ecommerce.cnj70.enums.UserRole;
import com.ecommerce.cnj70.service.AdminAuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Phase 3C — Admin Audit Log HTML Controller.
 *
 * <p>Serves Thymeleaf pages for Admin to browse audit logs.
 * Backend query delegate: {@link AdminAuditLogService}</p>
 *
 * <h3>Routes</h3>
 * <ul>
 *     <li>{@code GET /admin/audit}             — List audit log entries</li>
 *     <li>{@code GET /admin/audit/{id}}      — Audit log detail</li>
 * </ul>
 *
 * <h3>Security</h3>
 * <p>{@code /admin/**} is protected by SecurityConfig.hasRole("ADMIN").</p>
 */
@Controller
@RequestMapping("/admin/audit")
@RequiredArgsConstructor
public class AdminAuditLogController {

    private static final int DEFAULT_PAGE_SIZE = 20;

    private final AdminAuditLogService adminAuditLogService;

    @GetMapping
    public String list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String actorId,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) String resourceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            Model model) {

        int safeSize = (size <= 0) ? DEFAULT_PAGE_SIZE : Math.min(size, 100);
        int safePage = Math.max(page, 0);
        Pageable pageable = PageRequest.of(safePage, safeSize);

        Page<AuditLogEntry> entries;

        if (from != null && to != null) {
            entries = adminAuditLogService.listByDateRange(from, to, null, null, java.util.Collections.emptyList(), pageable);
        } else if (isNonEmpty(resourceType) && isNonEmpty(resourceId)) {
            entries = adminAuditLogService.listByResource(resourceType, resourceId, pageable);
        } else if (isNonEmpty(actorId)) {
            entries = adminAuditLogService.listByActor(actorId, pageable);
        } else if (isNonEmpty(role)) {
            try {
                UserRole roleEnum = UserRole.valueOf(role.toUpperCase());
                entries = adminAuditLogService.listByRole(roleEnum, pageable);
            } catch (IllegalArgumentException e) {
                entries = adminAuditLogService.listAll(pageable);
            }
        } else {
            entries = adminAuditLogService.listAll(pageable);
        }

        model.addAttribute("entries", entries.getContent());
        model.addAttribute("page", entries.getNumber());
        model.addAttribute("size", entries.getSize());
        model.addAttribute("totalPages", entries.getTotalPages());
        model.addAttribute("totalItems", entries.getTotalElements());
        model.addAttribute("hasNext", entries.hasNext());
        model.addAttribute("hasPrev", entries.hasPrevious());
        model.addAttribute("isFirst", entries.isFirst());
        model.addAttribute("isLast", entries.isLast());
        model.addAttribute("pageNumbers", computePageRange(entries.getNumber(), entries.getTotalPages()));

        model.addAttribute("roleFilter", isNonEmpty(role) ? role : "");
        model.addAttribute("actorId", isNonEmpty(actorId) ? actorId : "");
        model.addAttribute("resourceType", isNonEmpty(resourceType) ? resourceType : "");
        model.addAttribute("resourceId", isNonEmpty(resourceId) ? resourceId : "");
        model.addAttribute("from", from != null ? from.toString().replace("T", " ") : "");
        model.addAttribute("to", to != null ? to.toString().replace("T", " ") : "");
        model.addAttribute("roleChoices", UserRole.values());

        return "admin/audit-list";
    }

    @GetMapping("/{id}")
    public String detail(
            @PathVariable String id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String actorId,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) String resourceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            Model model) {

        AuditLogEntry entry = adminAuditLogService.getDetail(id);
        model.addAttribute("entry", entry);
        model.addAttribute("page", page);
        model.addAttribute("size", size);
        model.addAttribute("roleFilter", isNonEmpty(role) ? role : "");
        model.addAttribute("actorId", isNonEmpty(actorId) ? actorId : "");
        model.addAttribute("resourceType", isNonEmpty(resourceType) ? resourceType : "");
        model.addAttribute("resourceId", isNonEmpty(resourceId) ? resourceId : "");
        model.addAttribute("from", from != null ? from.toString().replace("T", " ") : "");
        model.addAttribute("to", to != null ? to.toString().replace("T", " ") : "");

        return "admin/audit-detail";
    }

    private static boolean isNonEmpty(String s) {
        return s != null && !s.isBlank();
    }

    private static List<Integer> computePageRange(int current, int totalPages) {
        List<Integer> out = new ArrayList<>();
        if (totalPages <= 0) return out;
        int start = Math.max(0, current - 2);
        int end = Math.min(totalPages - 1, current + 2);
        for (int i = start; i <= end; i++) out.add(i);
        return out;
    }
}
