package com.ecommerce.cnj70.service.impl;

import com.ecommerce.cnj70.document.Banner;
import com.ecommerce.cnj70.dto.moderation.AuditEvent;
import com.ecommerce.cnj70.enums.BannerStatus;
import com.ecommerce.cnj70.exception.BadRequestException;
import com.ecommerce.cnj70.exception.ResourceNotFoundException;
import com.ecommerce.cnj70.repository.BannerRepository;
import com.ecommerce.cnj70.service.AuditEventWriter;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MongoConverter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Phase 5 — Unit test cho AdminBannerServiceImpl.
 *
 * Verify rule:
 *  - getBannerById: raw mongoTemplate query; not found → throw.
 *  - createBanner: UNPUBLISHED default; valid title+image; invalid → throw.
 *  - publishBanner: idempotent — re-publish on PUBLISHED → throw.
 *  - unpublishBanner: idempotent — unpublish on non-PUBLISHED → throw.
 *  - deleteBanner: hard delete qua bannerRepository.deleteById.
 *  - URL safety: javascript: scheme rejected.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AdminBannerService - Phase 5 unit test")
class AdminBannerServiceImplTest {

    @Mock private BannerRepository bannerRepository;
    @Mock private MongoTemplate mongoTemplate;
    @Mock private MongoCollection<Document> mongoCollection;
    @Mock private AuditEventWriter auditEventWriter;

    @InjectMocks private AdminBannerServiceImpl adminBannerService;

    @BeforeEach
    void setUp() {
        lenient().when(bannerRepository.save(any(Banner.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        // Default "not found" — raw collection query returns null.
        FindIterable<Document> emptyFind = mock(FindIterable.class);
        lenient().when(emptyFind.first()).thenReturn(null);
        lenient().when(mongoCollection.find(any(Document.class))).thenReturn(emptyFind);
        lenient().when(mongoTemplate.getCollection("banners")).thenReturn(mongoCollection);
    }

    private void stubRawGetBannerById(Banner b) {
        Document raw = new Document("_id", b.getId())
                .append("title", b.getTitle())
                .append("description", b.getDescription())
                .append("imageUrl", b.getImageUrl())
                .append("link", b.getLink())
                .append("status", b.getStatus())
                .append("position", b.getPosition())
                .append("sortOrder", b.getSortOrder());
        FindIterable<Document> findIterable = mock(FindIterable.class);
        lenient().when(mongoCollection.find(any(Document.class))).thenReturn(findIterable);
        lenient().when(findIterable.first()).thenReturn(raw);
        MongoConverter converter = mock(MongoConverter.class);
        lenient().when(mongoTemplate.getConverter()).thenReturn(converter);
        lenient().when(converter.read(eq(Banner.class), eq(raw))).thenReturn(b);
    }

    private Banner validNewBanner() {
        return Banner.builder()
                .id("b-1")
                .title("Summer Sale")
                .imageUrl("/uploads/banner.jpg")
                .status(BannerStatus.UNPUBLISHED)
                .sortOrder(1)
                .build();
    }

    // ===== getBannerById =====

    @Test
    @DisplayName("getBannerById: found → return Banner")
    void getBannerById_found() {
        Banner b = validNewBanner();
        stubRawGetBannerById(b);
        // Override raw store to also handle "banners" collection specifically.
        // (Stub above already targeted mongoCollection.find(Document.class) regardless of Bson type.)

        Banner result = adminBannerService.getBannerById("b-1");
        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Summer Sale");
    }

    @Test
    @DisplayName("getBannerById: not found → throw ResourceNotFoundException")
    void getBannerById_notFound() {
        assertThatThrownBy(() -> adminBannerService.getBannerById("nope"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getBannerById: empty id → throw BadRequestException")
    void getBannerById_emptyId() {
        assertThatThrownBy(() -> adminBannerService.getBannerById(""))
                .isInstanceOf(BadRequestException.class);
    }

    // ===== createBanner =====

    @Test
    @DisplayName("createBanner: valid → UNPUBLISHED default + audit")
    void createBanner_valid() {
        Banner saved = adminBannerService.createBanner(validNewBanner());

        assertThat(saved.getStatus()).isEqualTo(BannerStatus.UNPUBLISHED);
        assertThat(saved.getSortOrder()).isEqualTo(1);
        verify(bannerRepository).save(any(Banner.class));
        verify(auditEventWriter).write(any(AuditEvent.class));
    }

    @Test
    @DisplayName("createBanner: blank title → BadRequest")
    void createBanner_blankTitle() {
        Banner b = validNewBanner();
        b.setTitle("   ");
        assertThatThrownBy(() -> adminBannerService.createBanner(b))
                .isInstanceOf(BadRequestException.class);
        verify(bannerRepository, never()).save(any());
    }

    @Test
    @DisplayName("createBanner: missing image → BadRequest")
    void createBanner_noImage() {
        Banner b = validNewBanner();
        b.setImageUrl(null);
        assertThatThrownBy(() -> adminBannerService.createBanner(b))
                .isInstanceOf(BadRequestException.class);
        verify(bannerRepository, never()).save(any());
    }

    @Test
    @DisplayName("createBanner: javascript URL scheme → BadRequest")
    void createBanner_javascriptLink() {
        Banner b = validNewBanner();
        b.setLink("javascript:alert(1)");
        assertThatThrownBy(() -> adminBannerService.createBanner(b))
                .isInstanceOf(BadRequestException.class);
    }

    // ===== publishBanner / unpublishBanner idempotency =====

    @Test
    @DisplayName("publishBanner: UNPUBLISHED → PUBLISHED")
    void publishBanner_unpublishedToPublished() {
        Banner b = validNewBanner();
        b.setStatus(BannerStatus.UNPUBLISHED);
        stubRawGetBannerById(b);

        Banner result = adminBannerService.publishBanner("b-1");

        assertThat(result.getStatus()).isEqualTo(BannerStatus.PUBLISHED);
        verify(auditEventWriter).write(any(AuditEvent.class));
    }

    @Test
    @DisplayName("publishBanner: re-publish PUBLISHED → throw (idempotent rejection)")
    void publishBanner_rePublish_throws() {
        Banner b = validNewBanner();
        b.setStatus(BannerStatus.PUBLISHED);
        stubRawGetBannerById(b);

        assertThatThrownBy(() -> adminBannerService.publishBanner("b-1"))
                .isInstanceOf(BadRequestException.class);
        verify(bannerRepository, never()).save(any());
    }

    @Test
    @DisplayName("unpublishBanner: PUBLISHED → UNPUBLISHED")
    void unpublishBanner_publishedToUnpublished() {
        Banner b = validNewBanner();
        b.setStatus(BannerStatus.PUBLISHED);
        stubRawGetBannerById(b);

        Banner result = adminBannerService.unpublishBanner("b-1");

        assertThat(result.getStatus()).isEqualTo(BannerStatus.UNPUBLISHED);
    }

    @Test
    @DisplayName("unpublishBanner: re-unpublish UNPUBLISHED → throw")
    void unpublishBanner_alreadyUnpublished_throws() {
        Banner b = validNewBanner();
        b.setStatus(BannerStatus.UNPUBLISHED);
        stubRawGetBannerById(b);

        assertThatThrownBy(() -> adminBannerService.unpublishBanner("b-1"))
                .isInstanceOf(BadRequestException.class);
        verify(bannerRepository, never()).save(any());
    }

    // ===== deleteBanner =====

    @Test
    @DisplayName("deleteBanner: existing → bannerRepository.deleteById")
    void deleteBanner_existing() {
        Banner b = validNewBanner();
        stubRawGetBannerById(b);

        adminBannerService.deleteBanner("b-1");

        verify(bannerRepository).deleteById("b-1");
        verify(auditEventWriter).write(any(AuditEvent.class));
    }

    @Test
    @DisplayName("deleteBanner: not found → throw ResourceNotFoundException")
    void deleteBanner_notFound() {
        assertThatThrownBy(() -> adminBannerService.deleteBanner("nope"))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(bannerRepository, never()).deleteById(any());
    }
}
