package com.ecommerce.cnj70.controller.admin;

import com.ecommerce.cnj70.document.Category;
import com.ecommerce.cnj70.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/admin/categories")
@RequiredArgsConstructor
public class AdminCategoryController {
    
    private final CategoryRepository categoryRepository;
    
    @GetMapping
    public String categoryList(Model model) {
        List<Category> categories = categoryRepository.findAll();
        model.addAttribute("categories", categories);
        return "admin/category-manage";
    }
    
    @PostMapping("/create")
    public String createCategory(@RequestParam String name, @RequestParam(required = false) String description) {
        Category category = Category.builder()
                .name(name)
                .description(description)
                .active(true)
                .build();
        categoryRepository.save(category);
        return "redirect:/admin/categories";
    }
    
    @PostMapping("/{id}/delete")
    public String deleteCategory(@PathVariable String id) {
        categoryRepository.deleteById(id);
        return "redirect:/admin/categories";
    }
}
