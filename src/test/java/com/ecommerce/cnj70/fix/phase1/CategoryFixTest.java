package com.ecommerce.cnj70.fix.phase1;

import com.ecommerce.cnj70.document.Category;
import com.ecommerce.cnj70.exception.BusinessException;
import com.ecommerce.cnj70.exception.ResourceNotFoundException;
import com.ecommerce.cnj70.repository.CategoryRepository;
import com.ecommerce.cnj70.repository.ProductRepository;
import com.ecommerce.cnj70.service.impl.AdminCategoryServiceImpl;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test cho AdminCategoryServiceImpl — Phase 1 (Category Fix Contract §10.4).
 *
 * <p>Verify rules:</p>
 * <ul>
 *   <li>{@code createCategory}: persist via {@code categoryRepository.save} (MongoDB).</li>
 *   <li>{@code updateCategory}: persist via {@code categoryRepository.save}; reject duplicate name.</li>
 *   <li>{@code deleteCategory}: persist via {@code categoryRepository.deleteById}; reject nếu còn product.</li>
 *   <li>{@code createCategory}: blank/empty name → throw BusinessException.</li>
 *   <li>{@code getCategoryById}: raw Document query pattern (Phase 4 fix String _id).</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AdminCategoryService - Phase 1 fix verification")
class CategoryFixTest {

    @Mock private CategoryRepository categoryRepository;
    @Mock private ProductRepository productRepository;
    @Mock private MongoTemplate mongoTemplate;
    @Mock private MongoCollection<Document> mongoCollection;

    @InjectMocks private AdminCategoryServiceImpl adminCategoryService;

    @BeforeEach
    void setUp() {
        // Default for save: echo back argument so we can verify what was saved.
        lenient().when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> {
            Category c = inv.getArgument(0);
            if (c.getId() == null) {
                c.setId("cat-stub-id");
            }
            return c;
        });

        // Default for deleteById: no-op.
        lenient().doNothing().when(categoryRepository).deleteById(any(String.class));

        // Default for raw getCategoryById: empty first().
        FindIterable<Document> emptyFind = mock(FindIterable.class);
        lenient().when(emptyFind.first()).thenReturn(null);
        lenient().when(mongoCollection.find(any(Bson.class))).thenReturn(emptyFind);
        lenient().when(mongoTemplate.getCollection("categories")).thenReturn(mongoCollection);
    }

    /**
     * Stub raw getCategoryById(): mongoTemplate.getCollection("categories").find(filter).first()
     * → Document. Phase 4 — pattern giống AdminShopServiceTest.stubRawGetShopById.
     */
    @SuppressWarnings("unchecked")
    private void stubRawGetCategoryById(Category category) {
        Document raw = new Document("_id", category.getId())
                .append("name", category.getName() != null ? category.getName() : "")
                .append("description", category.getDescription() != null ? category.getDescription() : "")
                .append("active", category.isActive());

        FindIterable<Document> findIterable = mock(FindIterable.class);
        lenient().when(mongoCollection.find(any(Bson.class))).thenReturn(findIterable);
        lenient().when(findIterable.first()).thenReturn(raw);
        lenient().when(mongoTemplate.getConverter()).thenReturn(
                mock(org.springframework.data.mongodb.core.convert.MongoConverter.class));
        lenient().when(mongoTemplate.getConverter().read(eq(Category.class), eq(raw))).thenReturn(category);
    }

    // ============ §10.4 - createCategory ============

    @Test
    @DisplayName("createCategory: valid input → persisted to MongoDB via repository.save")
    void createCategory_validInput_persistedToMongoDB() {
        // Stub: name does not exist yet
        when(categoryRepository.existsByName("TEST_PHASE_1")).thenReturn(false);

        Category created = adminCategoryService.createCategory("TEST_PHASE_1", "Phase 1 test category");

        // Verify categoryRepository.save was called
        ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository).save(captor.capture());

        Category saved = captor.getValue();
        assertThat(saved.getName()).isEqualTo("TEST_PHASE_1");
        assertThat(saved.getDescription()).isEqualTo("Phase 1 test category");
        assertThat(saved.isActive()).isTrue();
        assertThat(created.getName()).isEqualTo("TEST_PHASE_1");
    }

    @Test
    @DisplayName("createCategory: blank name → throw BusinessException (validation)")
    void createCategory_invalidInput_throwValidation() {
        assertThatThrownBy(() -> adminCategoryService.createCategory("   ", "desc"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("không được để trống");

        // Verify NO save call
        verify(categoryRepository, org.mockito.Mockito.never()).save(any(Category.class));
    }

    @Test
    @DisplayName("createCategory: duplicate name → throw BusinessException")
    void createCategory_duplicateName_throwValidation() {
        when(categoryRepository.existsByName("Duplicate")).thenReturn(true);

        assertThatThrownBy(() -> adminCategoryService.createCategory("Duplicate", "x"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("đã tồn tại");

        verify(categoryRepository, org.mockito.Mockito.never()).save(any(Category.class));
    }

    // ============ §10.4 - updateCategory ============

    @Test
    @DisplayName("updateCategory: valid input → changed in MongoDB via repository.save")
    void updateCategory_validInput_changedInMongoDB() {
        Category existing = Category.builder()
                .id("cat-1")
                .name("OLD_NAME")
                .description("old desc")
                .active(true)
                .build();
        stubRawGetCategoryById(existing);
        when(categoryRepository.existsByName("NEW_NAME")).thenReturn(false);

        Category updated = adminCategoryService.updateCategory("cat-1", "NEW_NAME", "new desc");

        ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository).save(captor.capture());

        Category saved = captor.getValue();
        assertThat(saved.getId()).isEqualTo("cat-1");
        assertThat(saved.getName()).isEqualTo("NEW_NAME");
        assertThat(saved.getDescription()).isEqualTo("new desc");
        assertThat(updated.getName()).isEqualTo("NEW_NAME");
    }

    @Test
    @DisplayName("updateCategory: same name (no rename) → save without existsByName check")
    void updateCategory_sameName_persisted() {
        Category existing = Category.builder()
                .id("cat-1")
                .name("SAME")
                .active(true)
                .build();
        stubRawGetCategoryById(existing);

        adminCategoryService.updateCategory("cat-1", "SAME", "desc");

        verify(categoryRepository).save(any(Category.class));
        // existsByName should NOT be called when name is unchanged
        verify(categoryRepository, org.mockito.Mockito.never()).existsByName("SAME");
    }

    // ============ §10.4 - deleteCategory ============

    @Test
    @DisplayName("deleteCategory: no products → removed from MongoDB via deleteById")
    void deleteCategory_validId_removedFromMongoDB() {
        Category existing = Category.builder()
                .id("cat-del")
                .name("TO_DELETE")
                .active(true)
                .build();
        stubRawGetCategoryById(existing);
        when(productRepository.findByCategoryId("cat-del")).thenReturn(Collections.emptyList());

        adminCategoryService.deleteCategory("cat-del");

        verify(categoryRepository).deleteById("cat-del");
    }

    @Test
    @DisplayName("deleteCategory: có products → throw BusinessException, no delete")
    void deleteCategory_withProducts_throws() {
        Category existing = Category.builder()
                .id("cat-busy")
                .name("BUSY")
                .active(true)
                .build();
        stubRawGetCategoryById(existing);
        // Stub Product mock with reflection-friendly stub
        com.ecommerce.cnj70.document.Product stubProduct =
                com.ecommerce.cnj70.document.Product.builder().build();
        when(productRepository.findByCategoryId("cat-busy")).thenReturn(List.of(stubProduct));

        assertThatThrownBy(() -> adminCategoryService.deleteCategory("cat-busy"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Không thể xóa");

        verify(categoryRepository, org.mockito.Mockito.never()).deleteById(any(String.class));
    }

    // ============ §10.4 - listCategory afterCreate ============

    @Test
    @DisplayName("listCategory: afterCreate_appearsInList — list returns the saved category")
    void listCategory_afterCreate_appearsInList() {
        Category saved = Category.builder()
                .id("cat-1")
                .name("TEST_PHASE_1")
                .active(true)
                .build();
        org.springframework.data.domain.Page<Category> page =
                new org.springframework.data.domain.PageImpl<>(List.of(saved));
        when(categoryRepository.findByNameContainingIgnoreCaseAndActive(
                any(String.class), any(Boolean.class), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(page);

        org.springframework.data.domain.Page<Category> result = adminCategoryService.listCategories(
                org.springframework.data.domain.PageRequest.of(0, 10),
                "TEST_PHASE_1",
                true);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("TEST_PHASE_1");
    }

    // ============ String _id retrieval (Phase 4 fix verification) ============

    @Test
    @DisplayName("getCategoryById: exists → raw Document query returns category")
    void getCategoryById_exists_returnsCategory() {
        Category existing = Category.builder()
                .id("cat-raw")
                .name("RAW")
                .active(true)
                .build();
        stubRawGetCategoryById(existing);

        Category found = adminCategoryService.getCategoryById("cat-raw");

        assertThat(found.getId()).isEqualTo("cat-raw");
        assertThat(found.getName()).isEqualTo("RAW");
    }

    @Test
    @DisplayName("getCategoryById: not found → throw ResourceNotFoundException")
    void getCategoryById_notFound_throws() {
        // Default: emptyFind.first() → null (setUp)

        assertThatThrownBy(() -> adminCategoryService.getCategoryById("nonexistent"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getCategoryById: blank id → throw BusinessException")
    void getCategoryById_blankId_throws() {
        assertThatThrownBy(() -> adminCategoryService.getCategoryById(""))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("không hợp lệ");
    }
}
