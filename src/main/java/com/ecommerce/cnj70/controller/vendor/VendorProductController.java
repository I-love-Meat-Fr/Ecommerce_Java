package com.ecommerce.cnj70.controller.vendor;

import com.ecommerce.cnj70.document.Category;
import com.ecommerce.cnj70.document.Product;
import com.ecommerce.cnj70.document.ProductSpecification;
import com.ecommerce.cnj70.document.ProductVariant;
import com.ecommerce.cnj70.dto.request.ProductFormReq;
import com.ecommerce.cnj70.dto.response.VendorProfileRes;
import com.ecommerce.cnj70.exception.BadRequestException;
import com.ecommerce.cnj70.exception.ResourceNotFoundException;
import com.ecommerce.cnj70.repository.CategoryRepository;
import com.ecommerce.cnj70.security.CustomUserDetails;
import com.ecommerce.cnj70.service.ProductService;
import com.ecommerce.cnj70.service.impl.StorageService;
import com.ecommerce.cnj70.service.VendorService;
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
        model.addAttribute("productFormReq", new ProductFormReq());
        return "vendor/product-form";
    }

    @PostMapping(value = "/create", consumes = {"multipart/form-data"})
    public String createProduct(@AuthenticationPrincipal CustomUserDetails user,
                               @ModelAttribute @Valid ProductFormReq request,
                               @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                               RedirectAttributes redirectAttributes,
                               Model model) {
        try {
            vendorService.getShopIdFromUser(user);

            if (imageFile != null && !imageFile.isEmpty()) {
                String uploadedUrl = storageService.save(imageFile);
                request.setImageUrls(java.util.List.of(uploadedUrl));
            }

            Product product = productService.createProduct(request, user.getShopId(), user.getFullName());
            redirectAttributes.addFlashAttribute("success", "Tạo sản phẩm thành công!");
            return "redirect:/vendor/products";
        } catch (BadRequestException e) {
            List<Category> categories = categoryRepository.findByActiveTrueOrderBySortOrderAsc();
            model.addAttribute("categories", categories);
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

            List<ProductSpecification> specs = product.getSpecifications() != null
                    ? product.getSpecifications() : new ArrayList<>();
            List<ProductVariant> variants = product.getVariants() != null
                    ? product.getVariants() : new ArrayList<>();

            model.addAttribute("product", product);
            model.addAttribute("categories", categories);
            model.addAttribute("productFormReq", ProductFormReq.builder()
                    .name(product.getName())
                    .brand(product.getBrand())
                    .warrantyMonths(product.getWarrantyMonths())
                    .manufacturer(product.getManufacturer())
                    .manufacturerAddress(product.getManufacturerAddress())
                    .description(product.getDescription())
                    .richDescription(product.getRichDescription())
                    .price(product.getPrice())
                    .stock(product.getStock())
                    .categoryId(product.getCategoryId())
                    .imageUrls(product.getImageUrls())
                    .specifications(specs)
                    .variants(variants)
                    .status(product.getStatus())
                    .build());

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
                             @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                             RedirectAttributes redirectAttributes,
                             Model model) {
        try {
            vendorService.validateProductOwnership(id, user);

            if (imageFile != null && !imageFile.isEmpty()) {
                String uploadedUrl = storageService.save(imageFile);
                request.setImageUrls(java.util.List.of(uploadedUrl));
            }

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
}
