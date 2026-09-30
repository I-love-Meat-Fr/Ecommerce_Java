package com.ecommerce.cnj70.controller.admin;

import com.ecommerce.cnj70.document.BlacklistWord;
import com.ecommerce.cnj70.enums.ViolationSeverity;
import com.ecommerce.cnj70.service.automation.BlacklistWordService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Admin UI cho quản lý Blacklist Word (Group 1).
 *
 * <p>Render trang web để Admin thêm / sửa / bật-tắt / xóa từ khóa cấm.
 * REST API tương ứng vẫn nằm ở
 * {@link com.ecommerce.cnj70.controller.web.BlacklistWordController}.</p>
 *
 * <p>Routes:</p>
 * <ul>
 *   <li>{@code GET  /admin/blacklist} - Danh sách + form thêm</li>
 *   <li>{@code POST /admin/blacklist/add} - Thêm keyword</li>
 *   <li>{@code POST /admin/blacklist/{id}/update} - Cập nhật</li>
 *   <li>{@code POST /admin/blacklist/{id}/toggle} - Bật / tắt</li>
 *   <li>{@code POST /admin/blacklist/{id}/delete} - Xóa</li>
 * </ul>
 *
 * Security: /admin/** đã bị SecurityConfig.hasRole("ADMIN") lock.
 */
@Controller
@RequestMapping("/admin/blacklist")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminBlacklistController {

    private final BlacklistWordService service;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("words", service.getAll());
        model.addAttribute("severities", ViolationSeverity.values());
        model.addAttribute("totalEnabled", service.getEnabled().size());
        return "admin/blacklist-list";
    }

    @PostMapping("/add")
    public String add(@RequestParam String keyword,
                      @RequestParam ViolationSeverity severity,
                      @RequestParam(required = false) String description,
                      @org.springframework.security.core.annotation.AuthenticationPrincipal
                      org.springframework.security.core.userdetails.UserDetails user,
                      RedirectAttributes ra) {
        try {
            String actor = user != null ? user.getUsername() : "ADMIN";
            service.addWord(keyword.trim(), severity, description, actor);
            ra.addFlashAttribute("flashSuccess", "Đã thêm từ khóa: " + keyword);
        } catch (Exception e) {
            ra.addFlashAttribute("flashError", "Không thể thêm: " + e.getMessage());
        }
        return "redirect:/admin/blacklist";
    }

    @PostMapping("/{id}/update")
    public String update(@PathVariable String id,
                         @RequestParam(required = false) ViolationSeverity severity,
                         @RequestParam(required = false) String description,
                         @RequestParam(required = false) Boolean enabled,
                         RedirectAttributes ra) {
        try {
            service.updateWord(id,
                    severity,
                    description,
                    Boolean.TRUE.equals(enabled));
            ra.addFlashAttribute("flashSuccess", "Đã cập nhật từ khóa.");
        } catch (Exception e) {
            ra.addFlashAttribute("flashError", "Không thể cập nhật: " + e.getMessage());
        }
        return "redirect:/admin/blacklist";
    }

    @PostMapping("/{id}/toggle")
    public String toggle(@PathVariable String id, RedirectAttributes ra) {
        try {
            List<BlacklistWord> all = service.getAll();
            BlacklistWord target = all.stream()
                    .filter(w -> id.equals(w.getId()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy từ khóa"));
            service.setEnabled(id, !target.isEnabled());
            ra.addFlashAttribute("flashSuccess",
                    "Đã " + (target.isEnabled() ? "tắt" : "bật") + " từ khóa.");
        } catch (Exception e) {
            ra.addFlashAttribute("flashError", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/blacklist";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable String id, RedirectAttributes ra) {
        try {
            service.deleteWord(id);
            ra.addFlashAttribute("flashSuccess", "Đã xóa từ khóa.");
        } catch (Exception e) {
            ra.addFlashAttribute("flashError", "Không thể xóa: " + e.getMessage());
        }
        return "redirect:/admin/blacklist";
    }
}
