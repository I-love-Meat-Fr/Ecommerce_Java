package com.ecommerce.cnj70.service.impl;

import com.ecommerce.cnj70.document.Product;
import com.ecommerce.cnj70.dto.request.ProductFormReq;
import com.ecommerce.cnj70.enums.ProductStatus;
import com.ecommerce.cnj70.repository.ProductRepository;
import com.ecommerce.cnj70.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {
    
    private final ProductRepository productRepository;
    
    @Override
    public Product createProduct(ProductFormReq request, String shopId, String shopName) {
        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .stock(request.getStock())
                .categoryId(request.getCategoryId())
                .imageUrls(request.getImageUrls())
                .shopId(shopId)
                .shopName(shopName)
                .status(ProductStatus.ACTIVE)
                .build();
        
        return productRepository.save(product);
    }
    
    @Override
    public Product updateProduct(String id, ProductFormReq request) {
        Product product = getProductById(id);
        
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        product.setCategoryId(request.getCategoryId());
        if (request.getImageUrls() != null) {
            product.setImageUrls(request.getImageUrls());
        }
        
        return productRepository.save(product);
    }
    
    @Override
    public void deleteProduct(String id) {
        productRepository.deleteById(id);
    }
    
    @Override
    public Product getProductById(String id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
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
    public void updateProductStatus(String id, ProductStatus status) {
        Product product = getProductById(id);
        product.setStatus(status);
        productRepository.save(product);
    }
}
