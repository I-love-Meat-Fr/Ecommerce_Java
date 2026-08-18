package com.ecommerce.cnj70.controller.vendor;

import com.ecommerce.cnj70.document.Product;
import com.ecommerce.cnj70.dto.request.ProductFormReq;
import com.ecommerce.cnj70.security.CustomUserDetails;
import com.ecommerce.cnj70.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/vendor/products")
@RequiredArgsConstructor
public class VendorProductController {
    
    private final ProductService productService;
    
    @GetMapping
    public String productList(@AuthenticationPrincipal CustomUserDetails user, Model model) {
        List<Product> products = productService.getProductsByShop(user.getShopId());
        model.addAttribute("products", products);
        return "vendor/product-list";
    }
    
    @GetMapping("/create")
    public String createProductForm(Model model) {
        model.addAttribute("productFormReq", new ProductFormReq());
        return "vendor/product-form";
    }
    
    @PostMapping("/create")
    public String createProduct(@AuthenticationPrincipal CustomUserDetails user,
                               @ModelAttribute @Valid ProductFormReq request,
                               Model model) {
        try {
            productService.createProduct(request, user.getShopId(), user.getFullName());
            return "redirect:/vendor/products";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "vendor/product-form";
        }
    }
    
    @GetMapping("/edit/{id}")
    public String editProductForm(@PathVariable String id, Model model) {
        Product product = productService.getProductById(id);
        model.addAttribute("product", product);
        model.addAttribute("productFormReq", new ProductFormReq());
        return "vendor/product-form";
    }
    
    @PostMapping("/edit/{id}")
    public String editProduct(@PathVariable String id,
                             @ModelAttribute @Valid ProductFormReq request,
                             Model model) {
        try {
            productService.updateProduct(id, request);
            return "redirect:/vendor/products";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "vendor/product-form";
        }
    }
    
    @PostMapping("/delete/{id}")
    public String deleteProduct(@PathVariable String id) {
        productService.deleteProduct(id);
        return "redirect:/vendor/products";
    }
}
