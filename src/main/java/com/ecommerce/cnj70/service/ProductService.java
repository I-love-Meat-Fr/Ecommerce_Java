package com.ecommerce.cnj70.service;

import com.ecommerce.cnj70.document.Product;
import com.ecommerce.cnj70.dto.request.ProductFormReq;
import com.ecommerce.cnj70.enums.ProductStatus;

import java.util.List;

public interface ProductService {
    
    Product createProduct(ProductFormReq request, String shopId, String shopName);
    
    Product updateProduct(String id, ProductFormReq request);
    
    void deleteProduct(String id);
    
    Product getProductById(String id);
    
    List<Product> getAllProducts();
    
    List<Product> getProductsByShop(String shopId);
    
    List<Product> getProductsByCategory(String categoryId);
    
    List<Product> searchProducts(String keyword);
    
    List<Product> getActiveProducts();
    
    void updateProductStatus(String id, ProductStatus status);
}
