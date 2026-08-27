package com.ecommerce.cnj70.controller.admin;

import com.ecommerce.cnj70.document.Category;
import com.ecommerce.cnj70.exception.BusinessException;
import com.ecommerce.cnj70.exception.ResourceNotFoundException;
import com.ecommerce.cnj70.service.AdminCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/admin/categories")
@RequiredArgsConstructor
public class AdminCategoryController {

    private static final int DEFAULT_PAGE_SIZE = 5;

    private final AdminCategoryService adminCategoryService;

    @GetMapping
    public String categoryList(@RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "5") int size,
                               @RequestParam(required = false) String q,
                               Model model) {
        int safeSize = size <= 0 ? DEFAULT_PAGE_SIZE : Math.min(size, 50);
        int safePage = Math.max(page, 0);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.ASC, "name"));

        Page<Category> result = adminCategoryService.listCategories(pageable, q);

        model.addAttribute("categories", result.getContent());
        model.addAttribute("page", result.getNumber());
        model.addAttribute("size", result.getSize());
        model.addAttribute("totalPages", result.getTotalPages());
        model.addAttribute("totalItems", result.getTotalElements());
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("hasNext", result.hasNext());
        model.addAttribute("hasPrev", result.hasPrevious());
        model.addAttribute("isFirst", result.isFirst());
        model.addAttribute("isLast", result.isLast());
        model.addAttribute("pageNumbers", computePageRange(result.getNumber(), result.getTotalPages()));
        return "admin/category-manage";
    }

    @PostMapping("/create")
    public String createCategory(@RequestParam String name,
                                 @RequestParam(required = false) String description,
                                 @RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "5") int size,
                                 @RequestParam(required = false) String q,
                                 RedirectAttributes redirectAttributes) {
        try {
            Category created = adminCategoryService.createCategory(name, description);
            redirectAttributes.addAttribute("flashSuccess",
                    "Đã thêm danh mục \"" + created.getName() + "\"");
        } catch (BusinessException ex) {
            redirectAttributes.addAttribute("flashError", ex.getMessage());
        }
        redirectAttributes.addAttribute("page", page);
        redirectAttributes.addAttribute("size", size);
        if (q != null) redirectAttributes.addAttribute("q", q);
        return "redirect:/admin/categories";
    }

    @GetMapping("/{id}/edit")
    public String editCategoryForm(@PathVariable String id,
                                   @RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "5") int size,
                                   @RequestParam(required = false) String q,
                                   Model model,
                                   RedirectAttributes redirectAttributes) {
        try {
            Category category = adminCategoryService.getCategoryById(id);
            model.addAttribute("category", category);
            model.addAttribute("editMode", true);

            int safeSize = size <= 0 ? DEFAULT_PAGE_SIZE : Math.min(size, 50);
            int safePage = Math.max(page, 0);
            Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.ASC, "name"));
            Page<Category> result = adminCategoryService.listCategories(pageable, q);

            model.addAttribute("categories", result.getContent());
            model.addAttribute("page", result.getNumber());
            model.addAttribute("size", result.getSize());
            model.addAttribute("totalPages", result.getTotalPages());
            model.addAttribute("totalItems", result.getTotalElements());
            model.addAttribute("q", q == null ? "" : q);
            model.addAttribute("hasNext", result.hasNext());
            model.addAttribute("hasPrev", result.hasPrevious());
            model.addAttribute("isFirst", result.isFirst());
            model.addAttribute("isLast", result.isLast());
            model.addAttribute("pageNumbers", computePageRange(result.getNumber(), result.getTotalPages()));
            return "admin/category-manage";
        } catch (BusinessException ex) {
            redirectAttributes.addAttribute("flashError", ex.getMessage());
            redirectAttributes.addAttribute("page", page);
            redirectAttributes.addAttribute("size", size);
            if (q != null) redirectAttributes.addAttribute("q", q);
            return "redirect:/admin/categories";
        }
    }

    @PostMapping("/{id}/edit")
    public String updateCategory(@PathVariable String id,
                                 @RequestParam String name,
                                 @RequestParam(required = false) String description,
                                 @RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "5") int size,
                                 @RequestParam(required = false) String q,
                                 RedirectAttributes redirectAttributes) {
        try {
            Category updated = adminCategoryService.updateCategory(id, name, description);
            redirectAttributes.addAttribute("flashSuccess",
                    "Đã cập nhật danh mục \"" + updated.getName() + "\"");
        } catch (BusinessException ex) {
            redirectAttributes.addAttribute("flashError", ex.getMessage());
        }
        redirectAttributes.addAttribute("page", page);
        redirectAttributes.addAttribute("size", size);
        if (q != null) redirectAttributes.addAttribute("q", q);
        return "redirect:/admin/categories";
    }

    @PostMapping("/{id}/delete")
    public String deleteCategory(@PathVariable String id,
                                 @RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "5") int size,
                                 @RequestParam(required = false) String q,
                                 RedirectAttributes redirectAttributes) {
        try {
            Category category = adminCategoryService.getCategoryById(id);
            String name = category.getName();
            adminCategoryService.deleteCategory(id);
            redirectAttributes.addAttribute("flashSuccess",
                    "Đã xóa danh mục \"" + name + "\"");
        } catch (BusinessException | ResourceNotFoundException ex) {
            redirectAttributes.addAttribute("flashError", ex.getMessage());
        }
        redirectAttributes.addAttribute("page", page);
        redirectAttributes.addAttribute("size", size);
        if (q != null) redirectAttributes.addAttribute("q", q);
        return "redirect:/admin/categories";
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
