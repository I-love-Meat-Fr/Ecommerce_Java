package com.ecommerce.cnj70.service.impl;

import com.ecommerce.cnj70.document.Category;
import com.ecommerce.cnj70.document.Product;
import com.ecommerce.cnj70.dto.request.ProductFormReq;
import com.ecommerce.cnj70.enums.ProductStatus;
import com.ecommerce.cnj70.exception.BadRequestException;
import com.ecommerce.cnj70.exception.ResourceNotFoundException;
import com.ecommerce.cnj70.repository.CategoryRepository;
import com.ecommerce.cnj70.repository.ProductRepository;
import com.ecommerce.cnj70.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {
    
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    
    @Override
    @Transactional
    public Product createProduct(ProductFormReq request, String shopId, String shopName) {
        if (request.getStock() < 0) {
            throw new BadRequestException("Số lượng tồn kho không được âm");
        }
        
        String categoryName = null;
        if (request.getCategoryId() != null && !request.getCategoryId().isBlank()) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new BadRequestException("Danh mục không tồn tại"));
            categoryName = category.getName();
        }
        
        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .stock(request.getStock())
                .categoryId(request.getCategoryId())
                .categoryName(categoryName)
                .imageUrls(request.getImageUrls())
                .shopId(shopId)
                .shopName(shopName)
                .status(request.getStatus() != null ? request.getStatus() : ProductStatus.ACTIVE)
                .build();
        
        return productRepository.save(product);
    }
    
    @Override
    @Transactional
    public Product updateProduct(String id, ProductFormReq request) {
        Product product = getProductById(id);
        
        if (request.getName() != null && !request.getName().isBlank()) {
            product.setName(request.getName());
        }
        if (request.getDescription() != null) {
            product.setDescription(request.getDescription());
        }
        if (request.getPrice() != null) {
            if (request.getPrice().doubleValue() <= 0) {
                throw new BadRequestException("Giá sản phẩm phải lớn hơn 0");
            }
            product.setPrice(request.getPrice());
        }
        if (request.getStock() != null) {
            if (request.getStock() < 0) {
                throw new BadRequestException("Số lượng tồn kho không được âm");
            }
            product.setStock(request.getStock());
        }
        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new BadRequestException("Danh mục không tồn tại"));
            product.setCategoryId(request.getCategoryId());
            product.setCategoryName(category.getName());
        }
        if (request.getImageUrls() != null) {
            product.setImageUrls(request.getImageUrls());
        }
        if (request.getStatus() != null) {
            product.setStatus(request.getStatus());
        }
        
        return productRepository.save(product);
    }
    
    @Override
    @Transactional
    public void deleteProduct(String id) {
        Product product = getProductById(id);
        
        if (product.getStatus() == ProductStatus.HIDDEN) {
            throw new BadRequestException("Sản phẩm đã bị xóa trước đó");
        }
        
        productRepository.deleteById(id);
    }
    
    @Override
    public Product getProductById(String id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm"));
    }
    
    @Override
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }
    
    @Override
    public List<Product> getProductsByShop(String shopId) {
        return productRepository.findByShopId(shopId);
    }
    
    @Override
    public List<Product> getProductsByCategory(String categoryId) {
        return productRepository.findByCategoryId(categoryId);
    }
    
    @Override
    public List<Product> searchProducts(String keyword) {
        return productRepository.findByNameContainingIgnoreCaseAndStatus(keyword, ProductStatus.ACTIVE);
    }
    
    @Override
    public List<Product> getActiveProducts() {
        return productRepository.findByStatus(ProductStatus.ACTIVE);
    }
    
    @Override
    public List<Product> getNewArrivals(int limit) {
        return productRepository.findTop10ByStatusOrderByCreatedAtDesc(ProductStatus.ACTIVE)
                .stream().limit(limit).toList();
    }
    
    @Override
    public List<Product> getFeaturedProducts(int limit) {
        return productRepository.findByStatus(ProductStatus.ACTIVE).stream()
                .sorted((p1, p2) -> Double.compare(p2.getRating(), p1.getRating()))
                .limit(limit)
                .toList();
    }
    
    @Override
    public void updateProductStatus(String id, ProductStatus status) {
        Product product = getProductById(id);
        product.setStatus(status);
        productRepository.save(product);
    }
}
