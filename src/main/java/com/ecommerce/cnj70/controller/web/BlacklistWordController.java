package com.ecommerce.cnj70.controller.web;

import com.ecommerce.cnj70.document.BlacklistWord;
import com.ecommerce.cnj70.enums.ViolationSeverity;
import com.ecommerce.cnj70.service.automation.BlacklistWordService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Group 1 — Admin REST API cho BlacklistWord.
 *
 * <p>Tất cả endpoints yêu cầu role ADMIN. Cho phép Admin:</p>
 * <ul>
 *   <li>{@code GET /api/admin/blacklist} — xem tất cả keyword.</li>
 *   <li>{@code POST /api/admin/blacklist} — thêm keyword mới.</li>
 *   <li>{@code PUT /api/admin/blacklist/{id}} — cập nhật severity/description.</li>
 *   <li>{@code PATCH /api/admin/blacklist/{id}/enabled} — bật/tắt keyword.</li>
 *   <li>{@code DELETE /api/admin/blacklist/{id}} — xóa vĩnh viễn.</li>
 * </ul>
 *
 * <p>Mọi thao tác CUD sẽ invalidate cache của {@link BlacklistWordService},
 * đảm bảo lần scan tiếp theo dùng data mới nhất.</p>
 */
@RestController
@RequestMapping("/api/admin/blacklist")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class BlacklistWordController {

    private final BlacklistWordService service;

    @GetMapping
    public ResponseEntity<List<BlacklistWord>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @GetMapping("/enabled")
    public ResponseEntity<List<BlacklistWord>> getEnabled() {
        return ResponseEntity.ok(service.getEnabled());
    }

    @PostMapping
    public ResponseEntity<BlacklistWord> add(@RequestBody AddRequest req,
                                              @AuthenticationPrincipal UserDetails user) {
        String actor = user != null ? user.getUsername() : "ADMIN";
        return ResponseEntity.ok(service.addWord(
                req.keyword(), req.severity(), req.description(), actor));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BlacklistWord> update(@PathVariable String id,
                                                 @RequestBody UpdateRequest req) {
        return ResponseEntity.ok(service.updateWord(id, req.severity(),
                req.description(), req.enabled()));
    }

    @PatchMapping("/{id}/enabled")
    public ResponseEntity<BlacklistWord> setEnabled(@PathVariable String id,
                                                     @RequestParam boolean value) {
        return ResponseEntity.ok(service.setEnabled(id, value));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.deleteWord(id);
        return ResponseEntity.noContent().build();
    }

    // ===== DTO =====

    public record AddRequest(
            @NotBlank String keyword,
            @NotNull ViolationSeverity severity,
            String description) {}

    public record UpdateRequest(
            ViolationSeverity severity,
            String description,
            Boolean enabled) {}
}
