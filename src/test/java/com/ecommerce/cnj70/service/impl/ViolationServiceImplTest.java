package com.ecommerce.cnj70.service.impl;

import com.ecommerce.cnj70.document.Violation;
import com.ecommerce.cnj70.enums.ViolationSeverity;
import com.ecommerce.cnj70.enums.ViolationType;
import com.ecommerce.cnj70.exception.ResourceNotFoundException;
import com.ecommerce.cnj70.repository.ViolationRepository;
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

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Phase 6 — Unit test cho ViolationServiceImpl.
 *
 * Verify rule:
 *  - getById: raw mongoTemplate query; not found → throw.
 *  - getById(blank) → ResourceNotFoundException.
 *  - createViolation: shopId required; defaults applied.
 *  - resolveViolation: idempotent (already resolved → skip).
 *  - countActiveViolations / countCriticalActiveViolations.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ViolationService - Phase 6 unit test")
class ViolationServiceImplTest {

    @Mock private ViolationRepository violationRepository;
    @Mock private MongoTemplate mongoTemplate;
    @Mock private MongoCollection<Document> mongoCollection;
    @Mock private MongoConverter mongoConverter;

    @InjectMocks private ViolationServiceImpl violationService;

    @BeforeEach
    void setUp() {
        FindIterable<Document> emptyFind = mock(FindIterable.class);
        lenient().when(emptyFind.first()).thenReturn(null);
        lenient().when(mongoCollection.find(any(Document.class))).thenReturn(emptyFind);
        lenient().when(mongoTemplate.getCollection("violations")).thenReturn(mongoCollection);
        lenient().when(mongoTemplate.getConverter()).thenReturn(mongoConverter);
        lenient().when(violationRepository.save(any(Violation.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    private Violation validViolation() {
        return Violation.builder()
                .id("v-1")
                .shopId("s-1")
                .type(ViolationType.VIOLATION)
                .severity(ViolationSeverity.HIGH)
                .reason("Test reason")
                .createdBy("admin")
                .createdAt(LocalDateTime.now())
                .build();
    }

    private void stubRawGetViolationById(Violation v) {
        Document raw = new Document("_id", v.getId())
                .append("shopId", v.getShopId())
                .append("type", v.getType() != null ? v.getType().name() : null)
                .append("severity", v.getSeverity() != null ? v.getSeverity().name() : null)
                .append("reason", v.getReason())
                .append("createdBy", v.getCreatedBy());
        FindIterable<Document> findIterable = mock(FindIterable.class);
        lenient().when(findIterable.first()).thenReturn(raw);
        lenient().when(mongoCollection.find(any(Document.class))).thenReturn(findIterable);
        lenient().when(mongoConverter.read(eq(Violation.class), eq(raw))).thenReturn(v);
    }

    @Test
    void getById_existing_returnsViolation() {
        Violation v = validViolation();
        stubRawGetViolationById(v);

        Violation result = violationService.getById("v-1");

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("v-1");
        assertThat(result.getShopId()).isEqualTo("s-1");
        assertThat(result.getSeverity()).isEqualTo(ViolationSeverity.HIGH);
    }

    @Test
    void getById_notFound_throws() {
        assertThatThrownBy(() -> violationService.getById("nope"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getById_blank_throws() {
        assertThatThrownBy(() -> violationService.getById(""))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> violationService.getById(null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createViolation_valid_returnsSaved() {
        Violation v = validViolation();

        Violation result = violationService.createViolation(v);

        assertThat(result).isNotNull();
        assertThat(result.getShopId()).isEqualTo("s-1");
        verify(violationRepository).save(v);
    }

    @Test
    void createViolation_blankShopId_throws() {
        Violation v = validViolation();
        v.setShopId("");
        assertThatThrownBy(() -> violationService.createViolation(v))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void resolveViolation_existing_setsResolvedFields() {
        Violation v = validViolation();
        stubRawGetViolationById(v);

        Violation result = violationService.resolveViolation("v-1", "admin2", "Done");

        assertThat(result.getResolvedAt()).isNotNull();
        assertThat(result.getResolvedBy()).isEqualTo("admin2");
        assertThat(result.getResolutionNote()).isEqualTo("Done");
    }

    @Test
    void resolveViolation_alreadyResolved_skips() {
        Violation v = validViolation();
        v.setResolvedAt(LocalDateTime.now().minusDays(1));
        v.setResolvedBy("admin-old");
        stubRawGetViolationById(v);

        Violation result = violationService.resolveViolation("v-1", "admin2", "Done");

        // resolvedAt/resolvedBy phải được giữ nguyên, không update.
        assertThat(result.getResolvedBy()).isEqualTo("admin-old");
    }

    @Test
    void countActiveViolations_delegatesToRepository() {
        when(violationRepository.countByShopIdAndResolvedAtIsNull("s-1")).thenReturn(3L);

        long count = violationService.countActiveViolations("s-1");

        assertThat(count).isEqualTo(3);
    }

    @Test
    void countCriticalActiveViolations_delegatesToRepository() {
        when(violationRepository.countByShopIdAndSeverityInAndResolvedAtIsNull(
                any(String.class), any(java.util.List.class))).thenReturn(2L);

        long count = violationService.countCriticalActiveViolations("s-1");

        assertThat(count).isEqualTo(2);
    }
}
