package com.ecommerce.cnj70.service;

import com.ecommerce.cnj70.document.Product;
import com.ecommerce.cnj70.dto.request.ProductFormReq;
import com.ecommerce.cnj70.enums.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProductService {
    
    Product createProduct(ProductFormReq request, String shopId, String shopName);
    
    Product updateProduct(String id, ProductFormReq request);

    /**
     * Phase 4 — Vendor gửi lại Product đã bị Moderator REJECTED vào queue
     * để Moderator xem xét lại. Đặt moderationStatus về PENDING_AUTO
     * (chờ Auto Moderation engine chạy lại), clear moderationReason/Actor/At.
     *
     * <p>Pre-condition: product.moderationStatus == REJECTED và product
     * thuộc shop của vendor hiện tại. Nếu không, throw {@code BadRequestException}.</p>
     *
     * @return Product đã được cập nhật.
     */
    Product resubmitProduct(String id);

    void deleteProduct(String id);
    
    Product getProductById(String id);
    
    List<Product> getAllProducts();
    
    Page<Product> getAllProducts(Pageable pageable);
    
    List<Product> getProductsByShop(String shopId);
    
    List<Product> getProductsByCategory(String categoryId);
    
    Page<Product> getProductsByCategory(String categoryId, Pageable pageable);
    
    List<Product> searchProducts(String keyword);
    
    Page<Product> searchProducts(String keyword, Pageable pageable);
    
    List<Product> getActiveProducts();
    
    List<Product> getNewArrivals(int limit);
    
    List<Product> getFeaturedProducts(int limit);
    
    void updateProductStatus(String id, ProductStatus status);
}
