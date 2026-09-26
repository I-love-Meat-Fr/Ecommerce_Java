package com.ecommerce.cnj70.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test cho {@link ImageHashService}.
 */
@DisplayName("ImageHashService - unit test")
class ImageHashServiceTest {

    private ImageHashService service;

    @BeforeEach
    void setUp() {
        service = new ImageHashService();
    }

    @Test
    @DisplayName("hashUrl: same URL → same hash, deterministic")
    void sameUrl_sameHash() {
        String h1 = service.hashUrl("https://cdn.example.com/img/product-1.jpg");
        String h2 = service.hashUrl("https://cdn.example.com/img/product-1.jpg");
        assertThat(h1).isEqualTo(h2);
        assertThat(h1).hasSize(64); // SHA-256 hex length
    }

    @Test
    @DisplayName("hashUrl: different URL → different hash")
    void differentUrl_differentHash() {
        String h1 = service.hashUrl("https://cdn.example.com/img/product-1.jpg");
        String h2 = service.hashUrl("https://cdn.example.com/img/product-2.jpg");
        assertThat(h1).isNotEqualTo(h2);
    }

    @Test
    @DisplayName("hashUrl: null/blank → null")
    void nullOrBlank_returnsNull() {
        assertThat(service.hashUrl(null)).isNull();
        assertThat(service.hashUrl("")).isNull();
        assertThat(service.hashUrl("   ")).isNull();
    }

    @Test
    @DisplayName("isStockPlaceholder: phát hiện placeholder / stock URL phổ biến")
    void isStockPlaceholder_detectsKnownPatterns() {
        assertThat(service.isStockPlaceholder(null)).isTrue();
        assertThat(service.isStockPlaceholder("")).isTrue();
        assertThat(service.isStockPlaceholder("https://placehold.co/600x400")).isTrue();
        assertThat(service.isStockPlaceholder("https://picsum.photos/200/300")).isTrue();
        assertThat(service.isStockPlaceholder("https://dummyimage.com/600x400/fff/000"))
                .isTrue();
        assertThat(service.isStockPlaceholder("https://cdn.example.com/no-image.jpg"))
                .isTrue();
        assertThat(service.isStockPlaceholder("https://cdn.example.com/placeholder.png"))
                .isTrue();
    }

    @Test
    @DisplayName("isStockPlaceholder: ảnh thật (URL có nghĩa) → false")
    void isStockPlaceholder_realImageFalse() {
        assertThat(service.isStockPlaceholder(
                "https://cdn.cnj70.vn/products/iphone-15-pro/photo-1.jpg")).isFalse();
        assertThat(service.isStockPlaceholder(
                "https://shop.domain.com/images/abc-product.png")).isFalse();
    }

    @Test
    @DisplayName("hashUrls: trả về cùng thứ tự với input")
    void hashUrls_preservesOrder() {
        var hashes = service.hashUrls(java.util.List.of(
                "https://a.com/1.jpg",
                "https://a.com/2.jpg"));
        assertThat(hashes).hasSize(2);
        assertThat(hashes.get(0)).isNotEqualTo(hashes.get(1));
    }
}
