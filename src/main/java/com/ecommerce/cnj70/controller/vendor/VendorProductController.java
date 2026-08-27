package com.ecommerce.cnj70.controller.vendor;

import com.ecommerce.cnj70.document.Category;
import com.ecommerce.cnj70.document.CommonSpecs;
import com.ecommerce.cnj70.document.Product;
import com.ecommerce.cnj70.document.Variant;
import com.ecommerce.cnj70.dto.request.ProductFormReq;
import com.ecommerce.cnj70.enums.CategoryType;
import com.ecommerce.cnj70.exception.BadRequestException;
import com.ecommerce.cnj70.exception.ResourceNotFoundException;
import com.ecommerce.cnj70.repository.CategoryRepository;
import com.ecommerce.cnj70.security.CustomUserDetails;
import com.ecommerce.cnj70.service.ProductService;
import com.ecommerce.cnj70.service.VendorService;
import com.ecommerce.cnj70.service.impl.StorageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/vendor/products")
@RequiredArgsConstructor
public class VendorProductController {

    private final ProductService productService;
    private final VendorService vendorService;
    private final CategoryRepository categoryRepository;
    private final StorageService storageService;

    @GetMapping
    public String productList(@AuthenticationPrincipal CustomUserDetails user, Model model) {
        String shopId = vendorService.getShopIdFromUser(user);
        List<Product> products = productService.getProductsByShop(shopId);
        model.addAttribute("products", products);
        return "vendor/product-list";
    }

    @GetMapping("/create")
    public String createProductForm(@AuthenticationPrincipal CustomUserDetails user, Model model) {
        vendorService.getShopIdFromUser(user);

        List<Category> categories = categoryRepository.findByActiveTrueOrderBySortOrderAsc();
        model.addAttribute("categories", categories);
        // Truyền CategoryType enum 10 mục công nghệ cho thẻ phân nhóm
        model.addAttribute("categoryTypes", CategoryType.values());

        // Khởi tạo form trống
        ProductFormReq req = new ProductFormReq();
        model.addAttribute("productFormReq", req);
        return "vendor/product-form";
    }

    @PostMapping(value = "/create", consumes = {"multipart/form-data"})
    public String createProduct(@AuthenticationPrincipal CustomUserDetails user,
                                @ModelAttribute @Valid ProductFormReq request,
                                @RequestParam(value = "mainImageFile", required = false) MultipartFile mainImageFile,
                                RedirectAttributes redirectAttributes,
                                Model model) {
        try {
            vendorService.getShopIdFromUser(user);

            // Upload ảnh đại diện chính
            if (mainImageFile != null && !mainImageFile.isEmpty()) {
                String uploadedUrl = storageService.save(mainImageFile);
                request.setMainImage(uploadedUrl);
                request.setImageUrls(java.util.List.of(uploadedUrl));
            }

            // Bỏ variant rỗng (không có name)
            request.setVariants(filterEmptyVariants(request.getVariants()));

            Product product = productService.createProduct(request, user.getShopId(), user.getFullName());
            redirectAttributes.addFlashAttribute("success", "Tạo sản phẩm thành công!");
            return "redirect:/vendor/products";
        } catch (BadRequestException e) {
            List<Category> categories = categoryRepository.findByActiveTrueOrderBySortOrderAsc();
            model.addAttribute("categories", categories);
            model.addAttribute("categoryTypes", CategoryType.values());
            model.addAttribute("error", e.getMessage());
            model.addAttribute("productFormReq", request);
            return "vendor/product-form";
        }
    }

    @GetMapping("/edit/{id}")
    public String editProductForm(@AuthenticationPrincipal CustomUserDetails user,
                                  @PathVariable String id,
                                  Model model) {
        try {
            vendorService.validateProductOwnership(id, user);

            Product product = productService.getProductById(id);
            List<Category> categories = categoryRepository.findByActiveTrueOrderBySortOrderAsc();

            List<Variant> variants = product.getVariants();
            if (variants == null) {
                variants = new ArrayList<>();
            }

            ProductFormReq form = ProductFormReq.builder()
                    .name(product.getName())
                    .description(product.getDescription())
                    .price(product.getPrice())
                    .stock(product.getStock())
                    .categoryId(product.getCategoryId())
                    .imageUrls(product.getImageUrls())
                    .mainImage(product.getMainImage())
                    .commonSpecs(product.getCommonSpecs() != null ? product.getCommonSpecs() : new CommonSpecs())
                    .customSpecs(product.getCustomSpecs())
                    .variants(variants)
                    .status(product.getStatus())
                    .build();

            model.addAttribute("product", product);
            model.addAttribute("categories", categories);
            model.addAttribute("categoryTypes", CategoryType.values());
            model.addAttribute("productFormReq", form);
            return "vendor/product-form";
        } catch (ResourceNotFoundException | BadRequestException e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/vendor/products";
        }
    }

    @PostMapping(value = "/edit/{id}", consumes = {"multipart/form-data"})
    public String editProduct(@AuthenticationPrincipal CustomUserDetails user,
                              @PathVariable String id,
                              @ModelAttribute @Valid ProductFormReq request,
                              @RequestParam(value = "mainImageFile", required = false) MultipartFile mainImageFile,
                              RedirectAttributes redirectAttributes,
                              Model model) {
        try {
            vendorService.validateProductOwnership(id, user);

            if (mainImageFile != null && !mainImageFile.isEmpty()) {
                String uploadedUrl = storageService.save(mainImageFile);
                request.setMainImage(uploadedUrl);
                request.setImageUrls(java.util.List.of(uploadedUrl));
            }

            request.setVariants(filterEmptyVariants(request.getVariants()));

            productService.updateProduct(id, request);
            redirectAttributes.addFlashAttribute("success", "Cập nhật sản phẩm thành công!");
            return "redirect:/vendor/products";
        } catch (BadRequestException | ResourceNotFoundException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/vendor/products/edit/" + id;
        }
    }

    @PostMapping("/delete/{id}")
    public String deleteProduct(@AuthenticationPrincipal CustomUserDetails user,
                                @PathVariable String id,
                                RedirectAttributes redirectAttributes) {
        try {
            vendorService.validateProductOwnership(id, user);

            productService.deleteProduct(id);
            redirectAttributes.addFlashAttribute("success", "Xóa sản phẩm thành công!");
        } catch (BadRequestException | ResourceNotFoundException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/vendor/products";
    }

    @GetMapping("/categories")
    @ResponseBody
    public List<Category> getCategories() {
        return categoryRepository.findByActiveTrueOrderBySortOrderAsc();
    }

    // Lọc bỏ variant rỗng (không nhập name)
    private List<Variant> filterEmptyVariants(List<Variant> variants) {
        if (variants == null) return new ArrayList<>();
        List<Variant> filtered = new ArrayList<>();
        for (Variant v : variants) {
            if (v.getName() != null && !v.getName().isBlank()) {
                if (v.getId() == null || v.getId().isBlank()) {
                    v.setId(UUID.randomUUID().toString().substring(0, 8));
                }
                filtered.add(v);
            }
        }
        return filtered;
    }
}