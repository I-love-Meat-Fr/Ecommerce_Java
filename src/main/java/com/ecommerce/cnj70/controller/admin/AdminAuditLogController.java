package com.ecommerce.cnj70.controller.admin;

import com.ecommerce.cnj70.document.AuditLogEntry;
import com.ecommerce.cnj70.enums.AuditAction;
import com.ecommerce.cnj70.enums.AuditSeverity;
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
import java.util.stream.Collectors;

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
 * <h3>Filters</h3>
 * <p>All filters are optional and compose with AND. They are evaluated
 * at the query layer (MongoTemplate criteria) — the underlying data
 * source (collection {@code audit_logs}) is unchanged.</p>
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
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) String resourceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            Model model) {

        int safeSize = (size <= 0) ? DEFAULT_PAGE_SIZE : Math.min(size, 100);
        int safePage = Math.max(page, 0);
        Pageable pageable = PageRequest.of(safePage, safeSize);

        // Parse filter values (null when blank / unparseable).
        UserRole roleEnum = parseRole(role);
        AuditSeverity severityEnum = parseSeverity(severity);
        List<String> actionList = parseActions(action);

        // Compose ALL supplied filters via the search() query method.
        // Falls back to listAll() only when no filter is supplied.
        Page<AuditLogEntry> entries = adminAuditLogService.search(
                from, to,
                roleEnum, actorId,
                actionList,
                severityEnum,
                resourceType, resourceId,
                pageable);

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

        // Echo filter values back to the form (preserve user input).
        model.addAttribute("roleFilter", isNonEmpty(role) ? role : "");
        model.addAttribute("actorId", isNonEmpty(actorId) ? actorId : "");
        model.addAttribute("actionFilter", isNonEmpty(action) ? action : "");
        model.addAttribute("severityFilter", isNonEmpty(severity) ? severity : "");
        model.addAttribute("resourceType", isNonEmpty(resourceType) ? resourceType : "");
        model.addAttribute("resourceId", isNonEmpty(resourceId) ? resourceId : "");
        model.addAttribute("from", from != null ? from.toString().replace("T", " ") : "");
        model.addAttribute("to", to != null ? to.toString().replace("T", " ") : "");

        // Dropdown options.
        model.addAttribute("roleChoices", UserRole.values());
        model.addAttribute("severityChoices", AuditSeverity.values());
        model.addAttribute("actionChoices", AuditAction.values());
        model.addAttribute("actionGroups", groupedActions());

        return "admin/audit-list";
    }

    @GetMapping("/{id}")
    public String detail(
            @PathVariable String id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String actorId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) String resourceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            Model model) {

        AuditLogEntry entry = adminAuditLogService.getDetail(id);
        model.addAttribute("entry", entry);
        model.addAttribute("page", page);
        model.addAttribute("size", size);
        // Echo every filter so "Quay lại danh sách" preserves the
        // user's filter context.
        model.addAttribute("roleFilter", isNonEmpty(role) ? role : "");
        model.addAttribute("actorId", isNonEmpty(actorId) ? actorId : "");
        model.addAttribute("actionFilter", isNonEmpty(action) ? action : "");
        model.addAttribute("severityFilter", isNonEmpty(severity) ? severity : "");
        model.addAttribute("resourceType", isNonEmpty(resourceType) ? resourceType : "");
        model.addAttribute("resourceId", isNonEmpty(resourceId) ? resourceId : "");
        model.addAttribute("from", from != null ? from.toString().replace("T", " ") : "");
        model.addAttribute("to", to != null ? to.toString().replace("T", " ") : "");

        return "admin/audit-detail";
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private static boolean isNonEmpty(String s) {
        return s != null && !s.isBlank();
    }

    private static UserRole parseRole(String role) {
        if (!isNonEmpty(role)) return null;
        try {
            return UserRole.valueOf(role.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static AuditSeverity parseSeverity(String severity) {
        if (!isNonEmpty(severity)) return null;
        try {
            return AuditSeverity.valueOf(severity.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    /**
     * Parse the action filter — comma-separated values are supported so
     * the UI can pass multiple action names in one parameter.
     */
    private static List<String> parseActions(String action) {
        if (!isNonEmpty(action)) return List.of();
        return java.util.Arrays.stream(action.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    private static List<Integer> computePageRange(int current, int totalPages) {
        List<Integer> out = new ArrayList<>();
        if (totalPages <= 0) return out;
        int start = Math.max(0, current - 2);
        int end = Math.min(totalPages - 1, current + 2);
        for (int i = start; i <= end; i++) out.add(i);
        return out;
    }

    /**
     * Group {@link AuditAction} values by their semantic prefix so the
     * dropdown in the UI can render {@code <optgroup>} sections
     * (User, Shop, KYC, Product, ...). Keeps the dropdown scannable
     * even with 50+ actions.
     */
    private static java.util.Map<String, List<AuditAction>> groupedActions() {
        java.util.LinkedHashMap<String, List<AuditAction>> groups = new java.util.LinkedHashMap<>();
        for (AuditAction a : AuditAction.values()) {
            String prefix = a.name().contains("_")
                    ? a.name().substring(0, a.name().indexOf('_'))
                    : "OTHER";
            groups.computeIfAbsent(prefix, k -> new ArrayList<>()).add(a);
        }
        return groups;
    }
}