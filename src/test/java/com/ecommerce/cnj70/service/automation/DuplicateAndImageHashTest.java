package com.ecommerce.cnj70.service.automation;

import com.ecommerce.cnj70.document.Product;
import com.ecommerce.cnj70.repository.ProductRepository;
import com.ecommerce.cnj70.service.ImageHashService;
import com.ecommerce.cnj70.service.automation.AutoCheckStrategy.AutoCheckContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test cho {@link DuplicateProductCheck} + {@link ImageHashCheck}.
 *
 * <p>Covers DuplicateProductCheck (task #4.0):
 * <ol>
 *   <li>Không trùng image → PASS.</li>
 *   <li>Trùng image với product khác → SOFT_FLAG "DUPLICATE_IMAGE".</li>
 *   <li>Product tự match chính nó → loại trừ (PASS).</li>
 *   <li>URL placeholder → skip duplicate check.</li>
 *   <li>Null imageUrls → PASS.</li>
 * </ol>
 *
 * <p>Covers ImageHashCheck (task #6.0):
 * <ol>
 *   <li>URL placeholder → SOFT_FLAG "STOCK_IMAGE".</li>
 *   <li>URL extension hợp lệ → PASS.</li>
 *   <li>URL extension không whitelist → SOFT_FLAG "BAD_IMAGE_EXT".</li>
 *   <li>Null imageUrls → PASS.</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DuplicateProductCheck + ImageHashCheck - unit test")
class DuplicateAndImageHashTest {

    @Mock private ProductRepository productRepository;
    private ImageHashService imageHashService;
    private DuplicateProductCheck duplicateCheck;
    private ImageHashCheck imageHashCheck;

    @BeforeEach
    void setUp() {
        imageHashService = new ImageHashService();
        duplicateCheck = new DuplicateProductCheck(productRepository, imageHashService);
        imageHashCheck = ImageHashCheck.forTest(imageHashService, List.of("jpg", "png", "webp"));
    }

    private AutoCheckContext ctxFor(Product product) {
        return AutoCheckContext.builder()
                .product(product)
                .sharedState(new HashMap<>())
                .build();
    }

    private Product productWithImages(String id, List<String> imageUrls) {
        return Product.builder()
                .id(id)
                .name("Test " + id)
                .price(new BigDecimal("100"))
                .imageUrls(imageUrls)
                .shopId("s-1")
                .build();
    }

    // ============ DuplicateProductCheck ============

    @Test
    @DisplayName("[Duplicate] Không duplicate (no match in DB) → PASS")
    void duplicate_noMatch_pass() {
        when(productRepository.findFirstByImageUrlsContainingAndIdNot(
                any(String.class), any(String.class))).thenReturn(Optional.empty());
        Product p = productWithImages("p-new",
                List.of("https://cdn.example.com/new-image.jpg"));
        AutoCheckVerdict v = duplicateCheck.check(ctxFor(p));
        assertThat(v.isPass()).isTrue();
    }

    @Test
    @DisplayName("[Duplicate] Có match với product khác → SOFT_FLAG DUPLICATE_IMAGE")
    void duplicate_match_softFlag() {
        Product other = productWithImages("p-other",
                List.of("https://cdn.example.com/img-x.jpg"));
        when(productRepository.findFirstByImageUrlsContainingAndIdNot(
                eq("https://cdn.example.com/img-x.jpg"), eq("p-new")))
                .thenReturn(Optional.of(other));

        Product p = productWithImages("p-new",
                List.of("https://cdn.example.com/img-x.jpg"));
        AutoCheckVerdict v = duplicateCheck.check(ctxFor(p));
        assertThat(v.isSoftFlag()).isTrue();
        assertThat(v.getFlagCode()).isEqualTo("DUPLICATE_IMAGE");
        assertThat(v.getMessage()).contains("p-other");
    }

    @Test
    @DisplayName("[Duplicate] Loại trừ chính product (id=current)")
    void duplicate_excludesSelf() {
        // Query chỉ được gọi với excludeId chính là id của product hiện tại.
        when(productRepository.findFirstByImageUrlsContainingAndIdNot(
                any(String.class), any(String.class))).thenReturn(Optional.empty());

        Product p = productWithImages("p-100",
                List.of("https://cdn.example.com/img-x.jpg"));
        duplicateCheck.check(ctxFor(p));
        verify(productRepository).findFirstByImageUrlsContainingAndIdNot(
                "https://cdn.example.com/img-x.jpg", "p-100");
    }

    @Test
    @DisplayName("[Duplicate] URL placeholder → skip DB query")
    void duplicate_placeholderSkipped() {
        Product p = productWithImages("p-new",
                List.of("https://placehold.co/600x400"));
        AutoCheckVerdict v = duplicateCheck.check(ctxFor(p));
        assertThat(v.isPass()).isTrue();
        verify(productRepository, never())
                .findFirstByImageUrlsContainingAndIdNot(any(), any());
    }

    @Test
    @DisplayName("[Duplicate] Null imageUrls → PASS, không query DB")
    void duplicate_nullImageUrls_pass() {
        Product p = productWithImages("p-new", null);
        AutoCheckVerdict v = duplicateCheck.check(ctxFor(p));
        assertThat(v.isPass()).isTrue();
        verify(productRepository, never())
                .findFirstByImageUrlsContainingAndIdNot(any(), any());
    }

    @Test
    @DisplayName("[Duplicate] Null Product → PASS")
    void duplicate_nullProduct_pass() {
        AutoCheckVerdict v = duplicateCheck.check(ctxFor(null));
        assertThat(v.isPass()).isTrue();
    }

    // ============ ImageHashCheck ============

    @Test
    @DisplayName("[ImageHash] URL placeholder → SOFT_FLAG STOCK_IMAGE")
    void imageHash_placeholder_softFlag() {
        Product p = productWithImages("p-1",
                List.of("https://placehold.co/600x400"));
        AutoCheckVerdict v = imageHashCheck.check(ctxFor(p));
        assertThat(v.isSoftFlag()).isTrue();
        assertThat(v.getFlagCode()).isEqualTo("STOCK_IMAGE");
    }

    @Test
    @DisplayName("[ImageHash] URL real, extension hợp lệ → PASS")
    void imageHash_validExtension_pass() {
        Product p = productWithImages("p-1",
                List.of("https://cdn.cnj70.vn/products/abc.jpg",
                        "https://cdn.cnj70.vn/products/def.png"));
        AutoCheckVerdict v = imageHashCheck.check(ctxFor(p));
        assertThat(v.isPass()).isTrue();
    }

    @Test
    @DisplayName("[ImageHash] URL với extension không whitelist → SOFT_FLAG BAD_IMAGE_EXT")
    void imageHash_badExtension_softFlag() {
        Product p = productWithImages("p-1",
                List.of("https://cdn.example.com/img.gif"));
        AutoCheckVerdict v = imageHashCheck.check(ctxFor(p));
        assertThat(v.isSoftFlag()).isTrue();
        assertThat(v.getFlagCode()).isEqualTo("BAD_IMAGE_EXT");
        assertThat(v.getMessage()).contains("gif");
    }

    @Test
    @DisplayName("[ImageHash] Null imageUrls → PASS")
    void imageHash_nullImageUrls_pass() {
        Product p = productWithImages("p-1", null);
        AutoCheckVerdict v = imageHashCheck.check(ctxFor(p));
        assertThat(v.isPass()).isTrue();
    }

    @Test
    @DisplayName("[ImageHash] Query string được bỏ qua khi check extension")
    void imageHash_queryStringIgnored() {
        // ?v=123 → extension thực sự là jpg, không phải phần query string.
        Product p = productWithImages("p-1",
                List.of("https://cdn.example.com/img.jpg?v=123"));
        AutoCheckVerdict v = imageHashCheck.check(ctxFor(p));
        assertThat(v.isPass()).isTrue();
    }

    @Test
    @DisplayName("[ImageHash] id() = 'IMAGE_CONTENT'")
    void imageHash_id_returnsConstant() {
        assertThat(imageHashCheck.id()).isEqualTo("IMAGE_CONTENT");
    }

    @Test
    @DisplayName("[Duplicate] id() = 'DUPLICATE_PRODUCT'")
    void duplicate_id_returnsConstant() {
        assertThat(duplicateCheck.id()).isEqualTo("DUPLICATE_PRODUCT");
    }
}
