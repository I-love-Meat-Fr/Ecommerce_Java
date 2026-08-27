package com.ecommerce.cnj70.controller.admin;

import com.ecommerce.cnj70.document.User;
import com.ecommerce.cnj70.enums.AccountStatus;
import com.ecommerce.cnj70.exception.BusinessException;
import com.ecommerce.cnj70.security.CustomUserDetails;
import com.ecommerce.cnj70.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class AdminUserController {

    private static final int DEFAULT_PAGE_SIZE = 5;

    private final AdminUserService adminUserService;

    @GetMapping("/admin/users")
    public String userList(@RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "5") int size,
                           @RequestParam(required = false) String q,
                           Model model) {
        int safeSize = size <= 0 ? DEFAULT_PAGE_SIZE : Math.min(size, 50);
        int safePage = Math.max(page, 0);
        Pageable pageable = PageRequest.of(safePage, safeSize);

        Page<User> users = adminUserService.listUsers(pageable, q);

        model.addAttribute("users", users.getContent());
        model.addAttribute("page", users.getNumber());
        model.addAttribute("size", users.getSize());
        model.addAttribute("totalPages", users.getTotalPages());
        model.addAttribute("totalItems", users.getTotalElements());
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("hasNext", users.hasNext());
        model.addAttribute("hasPrev", users.hasPrevious());
        model.addAttribute("isFirst", users.isFirst());
        model.addAttribute("isLast", users.isLast());
        model.addAttribute("pageNumbers", computePageRange(users.getNumber(), users.getTotalPages()));
        return "admin/user-list";
    }

    @GetMapping("/admin/users/{id}")
    public String userDetail(@PathVariable String id, Model model) {
        User user = adminUserService.getUserById(id);
        model.addAttribute("user", user);
        return "admin/user-detail";
    }

    @PostMapping("/admin/users/{id}/lock")
    public String lockUser(@PathVariable String id, RedirectAttributes redirectAttributes) {
        String currentUserId = currentUserId();
        AccountStatus before = adminUserService.getCurrentStatus(id);
        try {
            adminUserService.lockUser(id, currentUserId);
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
            return "redirect:/admin/users";
        }
        if (before == AccountStatus.LOCKED) {
            redirectAttributes.addFlashAttribute("flashInfo",
                    "Tài khoản đang ở trạng thái khóa, không thay đổi");
        } else {
            redirectAttributes.addFlashAttribute("flashSuccess",
                    "Đã khóa tài khoản thành công");
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/admin/users/{id}/unlock")
    public String unlockUser(@PathVariable String id, RedirectAttributes redirectAttributes) {
        AccountStatus before = adminUserService.getCurrentStatus(id);
        adminUserService.unlockUser(id);
        if (before == AccountStatus.ACTIVE) {
            redirectAttributes.addFlashAttribute("flashInfo",
                    "Tài khoản đang hoạt động, không thay đổi");
        } else {
            redirectAttributes.addFlashAttribute("flashSuccess",
                    "Đã mở khóa tài khoản thành công");
        }
        return "redirect:/admin/users";
    }

    private String currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails cud) {
            return cud.getId();
        }
        return null;
    }

    private static java.util.List<Integer> computePageRange(int current, int totalPages) {
        java.util.List<Integer> out = new java.util.ArrayList<>();
        if (totalPages <= 0) return out;
        int start = Math.max(0, current - 2);
        int end = Math.min(totalPages - 1, current + 2);
        for (int i = start; i <= end; i++) out.add(i);
        return out;
    }
}