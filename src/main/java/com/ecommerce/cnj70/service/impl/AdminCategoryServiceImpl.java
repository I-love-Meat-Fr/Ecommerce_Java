package com.ecommerce.cnj70.service.impl;

import com.ecommerce.cnj70.document.Category;
import com.ecommerce.cnj70.exception.BusinessException;
import com.ecommerce.cnj70.exception.ResourceNotFoundException;
import com.ecommerce.cnj70.repository.CategoryRepository;
import com.ecommerce.cnj70.repository.ProductRepository;
import com.ecommerce.cnj70.service.AdminCategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminCategoryServiceImpl implements AdminCategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final MongoTemplate mongoTemplate;

    @Override
    public Page<Category> listCategories(Pageable pageable, String q) {
        if (!StringUtils.hasText(q)) {
            Query empty = new Query().with(pageable);
            long total = mongoTemplate.count(new Query(), Category.class);
            List<Category> all = mongoTemplate.find(empty, Category.class);
            return new PageImpl<>(all, pageable, total);
        }

        String safe = Pattern.quote(q.trim());
        Pattern namePattern = Pattern.compile(safe, Pattern.CASE_INSENSITIVE);
        Criteria criteria = Criteria.where("name").regex(namePattern);

        Query query = Query.query(criteria).with(pageable);
        long total = mongoTemplate.count(Query.query(criteria), Category.class);
        List<Category> content = mongoTemplate.find(query, Category.class);
        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public Category getCategoryById(String id) {
        if (!StringUtils.hasText(id)) {
            throw new BusinessException("ID danh mục không hợp lệ");
        }
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));
    }

    @Override
    public Category createCategory(String name, String description) {
        String normalizedName = normalizeName(name);

        if (normalizedName == null) {
            throw new BusinessException("Tên danh mục không được để trống");
        }

        if (categoryRepository.existsByName(normalizedName)) {
            log.info("AdminCategoryService.createCategory: duplicate name={}", normalizedName);
            throw new BusinessException(
                    "Tên danh mục \"" + normalizedName + "\" đã tồn tại trong hệ thống");
        }

        Category category = Category.builder()
                .name(normalizedName)
                .description(StringUtils.hasText(description) ? description.trim() : null)
                .active(true)
                .build();

        try {
            Category saved = categoryRepository.save(category);
            log.info("AdminCategoryService.createCategory: created category id={} name={}",
                    saved.getId(), saved.getName());
            return saved;
        } catch (org.springframework.dao.DuplicateKeyException ex) {
            log.warn("AdminCategoryService.createCategory: duplicate key for name={}", normalizedName);
            throw new BusinessException(
                    "Tên danh mục \"" + normalizedName + "\" đã tồn tại trong hệ thống");
        }
    }

    @Override
    public Category updateCategory(String id, String name, String description) {
        Category category = getCategoryById(id);

        String normalizedName = normalizeName(name);
        if (normalizedName == null) {
            throw new BusinessException("Tên danh mục không được để trống");
        }

        if (!category.getName().equalsIgnoreCase(normalizedName)
                && categoryRepository.existsByName(normalizedName)) {
            throw new BusinessException(
                    "Tên danh mục \"" + normalizedName + "\" đã tồn tại trong hệ thống");
        }

        category.setName(normalizedName);
        category.setDescription(StringUtils.hasText(description) ? description.trim() : null);

        try {
            Category saved = categoryRepository.save(category);
            log.info("AdminCategoryService.updateCategory: updated category id={} name={}",
                    saved.getId(), saved.getName());
            return saved;
        } catch (org.springframework.dao.DuplicateKeyException ex) {
            log.warn("AdminCategoryService.updateCategory: duplicate key for name={}", normalizedName);
            throw new BusinessException(
                    "Tên danh mục \"" + normalizedName + "\" đã tồn tại trong hệ thống");
        }
    }

    @Override
    public void deleteCategory(String id) {
        Category category = getCategoryById(id);

        long productCount = productRepository.findByCategoryId(category.getId()).size();
        if (productCount > 0) {
            throw new BusinessException(
                    "Không thể xóa danh mục \"" + category.getName()
                            + "\" vì đang được " + productCount
                            + " sản phẩm sử dụng");
        }

        categoryRepository.deleteById(category.getId());
        log.info("AdminCategoryService.deleteCategory: deleted category id={} name={}",
                category.getId(), category.getName());
    }

    private String normalizeName(String name) {
        if (name == null) {
            return null;
        }
        String trimmed = name.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
