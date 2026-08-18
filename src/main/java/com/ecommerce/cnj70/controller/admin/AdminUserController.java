package com.ecommerce.cnj70.controller.admin;

import com.ecommerce.cnj70.document.User;
import com.ecommerce.cnj70.enums.UserRole;
import com.ecommerce.cnj70.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class AdminUserController {
    
    private final UserRepository userRepository;
    
    @GetMapping("/admin/users")
    public String userList(Model model) {
        List<User> users = userRepository.findByRole(UserRole.CUSTOMER);
        model.addAttribute("users", users);
        return "admin/user-list";
    }
    
    @PostMapping("/admin/users/{id}/lock")
    public String lockUser(@PathVariable String id) {
        User user = userRepository.findById(id).orElseThrow();
        user.setStatus(com.ecommerce.cnj70.enums.AccountStatus.LOCKED);
        userRepository.save(user);
        return "redirect:/admin/users";
    }
    
    @PostMapping("/admin/users/{id}/unlock")
    public String unlockUser(@PathVariable String id) {
        User user = userRepository.findById(id).orElseThrow();
        user.setStatus(com.ecommerce.cnj70.enums.AccountStatus.ACTIVE);
        userRepository.save(user);
        return "redirect:/admin/users";
    }
}
