package com.ecommerce.cnj70.service;

import com.ecommerce.cnj70.document.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminCategoryService {

    Page<Category> listCategories(Pageable pageable, String q);

    Category getCategoryById(String id);

    Category createCategory(String name, String description);

    Category updateCategory(String id, String name, String description);

    void deleteCategory(String id);
}
