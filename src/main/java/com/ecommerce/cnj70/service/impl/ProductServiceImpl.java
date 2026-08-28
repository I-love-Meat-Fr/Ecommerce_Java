package com.ecommerce.cnj70.service.impl;

import com.ecommerce.cnj70.document.Category;
import com.ecommerce.cnj70.document.Product;
import com.ecommerce.cnj70.document.ProductSpecification;
import com.ecommerce.cnj70.document.ProductVariant;
import com.ecommerce.cnj70.dto.request.ProductFormReq;
import com.ecommerce.cnj70.enums.ProductStatus;
import com.ecommerce.cnj70.exception.BadRequestException;
import com.ecommerce.cnj70.exception.ResourceNotFoundException;
import com.ecommerce.cnj70.repository.CategoryRepository;
import com.ecommerce.cnj70.repository.ProductRepository;
import com.ecommerce.cnj70.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
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

        List<ProductSpecification> specs = sanitizeSpecifications(request.getSpecifications());
        List<ProductVariant> variants = sanitizeVariants(request.getVariants());

        Product product = Product.builder()
                .name(request.getName())
                .brand(request.getBrand())
                .warrantyMonths(request.getWarrantyMonths())
                .manufacturer(request.getManufacturer())
                .manufacturerAddress(request.getManufacturerAddress())
                .description(request.getDescription())
                .richDescription(request.getRichDescription())
                .price(request.getPrice())
                .stock(request.getStock())
                .categoryId(request.getCategoryId())
                .categoryName(categoryName)
                .imageUrls(request.getImageUrls())
                .specifications(specs)
                .variants(variants)
                .shopId(shopId)
                .shopName(shopName)
                .status(request.getStatus() != null ? request.getStatus() : ProductStatus.ACTIVE)
                .build();

        if (product.getImageUrls() != null && !product.getImageUrls().isEmpty()) {
            product.setThumbnailUrl(product.getImageUrls().get(0));
        }

        return productRepository.save(product);
    }

    @Override
    @Transactional
    public Product updateProduct(String id, ProductFormReq request) {
        Product product = getProductById(id);

        if (request.getName() != null && !request.getName().isBlank()) {
            product.setName(request.getName());
        }
        if (request.getBrand() != null) {
            product.setBrand(request.getBrand());
        }
        if (request.getWarrantyMonths() != null) {
            product.setWarrantyMonths(request.getWarrantyMonths());
        }
        if (request.getManufacturer() != null) {
            product.setManufacturer(request.getManufacturer());
        }
        if (request.getManufacturerAddress() != null) {
            product.setManufacturerAddress(request.getManufacturerAddress());
        }
        if (request.getDescription() != null) {
            product.setDescription(request.getDescription());
        }
        if (request.getRichDescription() != null) {
            product.setRichDescription(request.getRichDescription());
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
            if (!request.getImageUrls().isEmpty()) {
                product.setThumbnailUrl(request.getImageUrls().get(0));
            }
        }
        if (request.getSpecifications() != null) {
            product.setSpecifications(sanitizeSpecifications(request.getSpecifications()));
        }
        if (request.getVariants() != null) {
            product.setVariants(sanitizeVariants(request.getVariants()));
        }
        if (request.getStatus() != null) {
            product.setStatus(request.getStatus());
        }

        return productRepository.save(product);
    }

    private List<ProductSpecification> sanitizeSpecifications(List<ProductSpecification> specs) {
        if (specs == null) {
            return new ArrayList<>();
        }
        List<ProductSpecification> cleaned = new ArrayList<>();
        for (ProductSpecification spec : specs) {
            if (spec == null) {
                continue;
            }
            String name = spec.getName() == null ? "" : spec.getName().trim();
            String value = spec.getValue() == null ? "" : spec.getValue().trim();
            String unit = spec.getUnit() == null ? "" : spec.getUnit().trim();
            if (name.isEmpty() && value.isEmpty() && unit.isEmpty()) {
                continue;
            }
            cleaned.add(ProductSpecification.builder()
                    .name(name)
                    .value(value)
                    .unit(unit)
                    .build());
        }
        return cleaned;
    }

    private List<ProductVariant> sanitizeVariants(List<ProductVariant> variants) {
        if (variants == null) {
            return new ArrayList<>();
        }
        List<ProductVariant> cleaned = new ArrayList<>();
        for (ProductVariant variant : variants) {
            if (variant == null) {
                continue;
            }
            BigDecimal price = variant.getPrice();
            if (price == null || price.doubleValue() <= 0) {
                continue;
            }
            List<ProductSpecification> specs = sanitizeSpecifications(variant.getSpecifications());
            cleaned.add(ProductVariant.builder()
                    .specifications(specs)
                    .price(price)
                    .stock(Math.max(0, variant.getStock()))
                    .sku(variant.getSku())
                    .build());
        }
        return cleaned;
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
    public Page<Product> getAllProducts(Pageable pageable) {
        return productRepository.findAll(pageable);
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
    public Page<Product> getProductsByCategory(String categoryId, Pageable pageable) {
        return productRepository.findByCategoryId(categoryId, pageable);
    }

    @Override
    public List<Product> searchProducts(String keyword) {
        return productRepository.findByNameContainingIgnoreCaseAndStatus(keyword, ProductStatus.ACTIVE);
    }

    @Override
    public Page<Product> searchProducts(String keyword, Pageable pageable) {
        return productRepository.findByNameContainingIgnoreCase(keyword, pageable);
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
