package com.ecommerce.cnj70.service;

import com.ecommerce.cnj70.document.Product;
import com.ecommerce.cnj70.enums.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminProductService {

    /**
     * TASK 14.7 — Admin Product List (ALL Vendor Products).
     * Search by name + filter by status. Pagination theo Pageable.
     */
    Page<Product> listProducts(Pageable pageable, String q, ProductStatus statusFilter);

    /**
     * TASK 14.8 — Admin Product Detail.
     */
    Product getProductById(String id);

    /**
     * TASK 14.9 — Hide Product.
     * Set status = HIDDEN. Document vẫn tồn tại trong MongoDB.
     */
    Product hideProduct(String id);

    /**
     * TASK 14.10 — Unhide Product.
     * Set status = ACTIVE nếu trước đó là HIDDEN.
     */
    Product unhideProduct(String id);

    /**
     * TASK 14.11 — Delete Product Violation.
     * Hard delete. Chỉ dành cho Admin moderation khi Contract cho phép.
     * KHÔNG cascade Order/Review/Cart theo LOCK 7.
     */
    void deleteProduct(String id);
}
