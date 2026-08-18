package com.ecommerce.cnj70.controller.web;

import com.ecommerce.cnj70.document.Product;
import com.ecommerce.cnj70.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class HomeController {
    
    private final ProductService productService;
    
    @GetMapping("/home")
    public String homePage(Model model) {
        List<Product> products = productService.getActiveProducts();
        model.addAttribute("products", products);
        return "web/index";
    }
    
    @GetMapping("/products")
    public String productsPage(@RequestParam(required = false) String category,
                              @RequestParam(required = false) String search,
                              Model model) {
        List<Product> products;
        
        if (search != null && !search.isEmpty()) {
            products = productService.searchProducts(search);
        } else if (category != null && !category.isEmpty()) {
            products = productService.getProductsByCategory(category);
        } else {
            products = productService.getActiveProducts();
        }
        
        model.addAttribute("products", products);
        return "web/index";
    }
}
