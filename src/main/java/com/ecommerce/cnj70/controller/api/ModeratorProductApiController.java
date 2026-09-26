package com.ecommerce.cnj70.controller.api;

import com.ecommerce.cnj70.dto.moderation.EscalateProductReq;
import com.ecommerce.cnj70.dto.moderation.ModerationDecision;
import com.ecommerce.cnj70.dto.moderation.ProductModerationContext;
import com.ecommerce.cnj70.dto.moderation.ProductModerationQueueItem;
import com.ecommerce.cnj70.dto.moderation.RejectProductReq;
import com.ecommerce.cnj70.enums.ModerationStatus;
import com.ecommerce.cnj70.exception.BadRequestException;
import com.ecommerce.cnj70.exception.ConflictException;
import com.ecommerce.cnj70.exception.ResourceNotFoundException;
import com.ecommerce.cnj70.exception.UnauthorizedException;
import com.ecommerce.cnj70.security.CustomUserDetails;
import com.ecommerce.cnj70.service.ModeratorProductService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Phase 4 — REST API cho Moderator Product Moderation.
 *
 * <p>Cung cấp JSON endpoints song song với các {@code @Controller} MVC đã có,
 * phục vụ SPA admin tool / mobile app / integration test.</p>
 *
 * <p>Endpoints:</p>
 * <ul>
 *     <li>{@code GET  /api/moderator/products/queue} — list queue items (paginated, filter by status/keyword)</li>
 *     <li>{@code GET  /api/moderator/products/{id}} — product detail + auto moderation context</li>
 *     <li>{@code POST /api/moderator/products/{id}/approve} — approve product</li>
 *     <li>{@code POST /api/moderator/products/{id}/reject}  — reject product (reason required)</li>
 *     <li>{@code POST /api/moderator/products/{id}/escalate} — escalate to admin (reason required)</li>
 *     <li>{@code GET  /api/moderator/products/statuses} — enum list (for filter dropdown)</li>
 * </ul>
 *
 * <p>Security: tất cả endpoints yêu cầu role {@code MODERATOR} — đã được
 * {@code SecurityConfig} bảo vệ qua matcher {@code /api/**}.authenticated()}.
 * Object-level guard (account-status, locked) được enforce trong
 * {@code ModeratorProductServiceImpl.ModeratorGuard}.</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/moderator/products")
@RequiredArgsConstructor
public class ModeratorProductApiController {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final ModeratorProductService moderatorProductService;

    // ======================== QUEUE ========================

    @GetMapping("/queue")
    public ResponseEntity<?> listQueue(@RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "20") int size,
                                       @RequestParam(required = false) String q,
                                       @RequestParam(required = false) String status) {
        int safeSize = (size <= 0) ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
        int safePage = Math.max(page, 0);
        Pageable pageable = PageRequest.of(safePage, safeSize,
                Sort.by(Sort.Direction.DESC, "createdAt"));

        ModerationStatus filter = parseStatus(status);
        Page<ProductModerationQueueItem> result =
                moderatorProductService.listQueue(q, filter, pageable);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("items", result.getContent());
        body.put("page", result.getNumber());
        body.put("size", result.getSize());
        body.put("totalItems", result.getTotalElements());
        body.put("totalPages", result.getTotalPages());
        body.put("hasNext", result.hasNext());
        body.put("hasPrev", result.hasPrevious());
        return ResponseEntity.ok(body);
    }

    // ======================== STATUSES (enum metadata) ========================

    @GetMapping("/statuses")
    public ResponseEntity<List<Map<String, String>>> listStatuses() {
        List<Map<String, String>> list = new java.util.ArrayList<>();
        for (ModerationStatus s : ModerationStatus.values()) {
            Map<String, String> m = new LinkedHashMap<>();
            m.put("name", s.name());
            m.put("label", labelFor(s));
            list.add(m);
        }
        return ResponseEntity.ok(list);
    }

    // ======================== DETAIL ========================

    @GetMapping("/{id}")
    public ResponseEntity<?> getDetail(@PathVariable String id) {
        try {
            ProductModerationContext ctx = moderatorProductService.getContext(id);
            return ResponseEntity.ok(ctx);
        } catch (ResourceNotFoundException ex) {
            return error(404, "NOT_FOUND", ex.getMessage());
        } catch (BadRequestException ex) {
            return error(400, "BAD_REQUEST", ex.getMessage());
        }
    }

    // ======================== APPROVE ========================

    @PostMapping("/{id}/approve")
    public ResponseEntity<?> approve(@PathVariable String id,
                                     @AuthenticationPrincipal CustomUserDetails moderator,
                                     HttpServletRequest request) {
        return executeDecision(() -> moderatorProductService.approve(id, moderator, clientIp(request)),
                "approve", id);
    }

    // ======================== REJECT ========================

    @PostMapping("/{id}/reject")
    public ResponseEntity<?> reject(@PathVariable String id,
                                    @Valid @RequestBody RejectProductReq body,
                                    @AuthenticationPrincipal CustomUserDetails moderator,
                                    HttpServletRequest request) {
        String reason = body != null ? body.getReason() : null;
        return executeDecision(() -> moderatorProductService.reject(id, reason, moderator, clientIp(request)),
                "reject", id);
    }

    // ======================== ESCALATE ========================

    @PostMapping("/{id}/escalate")
    public ResponseEntity<?> escalate(@PathVariable String id,
                                      @Valid @RequestBody EscalateProductReq body,
                                      @AuthenticationPrincipal CustomUserDetails moderator,
                                      HttpServletRequest request) {
        String reason = body != null ? body.getReason() : null;
        return executeDecision(() -> moderatorProductService.escalate(id, reason, moderator, clientIp(request)),
                "escalate", id);
    }

    // ======================== HELPERS ========================

    private ResponseEntity<?> executeDecision(java.util.function.Supplier<ModerationDecision> action,
                                             String actionName, String productId) {
        try {
            ModerationDecision decision = action.get();
            return ResponseEntity.ok(decision);
        } catch (ConflictException ex) {
            return error(409, "CONFLICT", ex.getMessage());
        } catch (BadRequestException ex) {
            return error(400, "BAD_REQUEST", ex.getMessage());
        } catch (ResourceNotFoundException ex) {
            return error(404, "NOT_FOUND", ex.getMessage());
        } catch (UnauthorizedException ex) {
            return error(401, "UNAUTHORIZED", ex.getMessage());
        } catch (RuntimeException ex) {
            log.error("Moderator {} failed for productId={}: {}", actionName, productId, ex.getMessage(), ex);
            return error(500, "INTERNAL_ERROR", "Đã xảy ra lỗi khi xử lý yêu cầu");
        }
    }

    private static ResponseEntity<Map<String, Object>> error(int status, String code, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", status);
        body.put("code", code);
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }

    private static ModerationStatus parseStatus(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String norm = raw.trim().toUpperCase();
        if ("ALL".equals(norm)) return null;
        try {
            return ModerationStatus.valueOf(norm);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static String labelFor(ModerationStatus s) {
        return switch (s) {
            case PENDING_AUTO -> "Chờ Auto";
            case PENDING_MANUAL -> "Chờ Moderator";
            case AUTO_PASSED -> "Auto: Đạt";
            case AUTO_REJECTED -> "Auto: Từ chối";
            case APPROVED -> "Đã duyệt";
            case REJECTED -> "Bị từ chối";
            case ESCALATED -> "Đã leo thang";
        };
    }

    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
