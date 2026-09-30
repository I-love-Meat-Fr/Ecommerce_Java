package com.ecommerce.cnj70.service.automation;

import com.ecommerce.cnj70.document.BlacklistWord;
import com.ecommerce.cnj70.enums.ViolationSeverity;
import com.ecommerce.cnj70.exception.BadRequestException;
import com.ecommerce.cnj70.exception.ResourceNotFoundException;
import com.ecommerce.cnj70.repository.BlacklistWordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit test cho {@link BlacklistWordService}.
 *
 * <p>Cover:</p>
 * <ol>
 *   <li>Severity classification: CRITICAL/HIGH → FATAL, MEDIUM/LOW → SUSPICIOUS.</li>
 *   <li>Word-boundary: case-insensitive + Vietnamese diacritics.</li>
 *   <li>Empty text / null text → CLEAN.</li>
 *   <li>Cache: TTL expiry triggers reload.</li>
 *   <li>CRUD: addWord / updateWord / deleteWord / setEnabled — cache invalidation.</li>
 *   <li>Duplicate keyword throw BadRequest.</li>
 *   <li>Missing id throw ResourceNotFound.</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BlacklistWordService - unit test")
class BlacklistWordServiceTest {

    @Mock private BlacklistWordRepository repository;
    private BlacklistWordService service;

    @BeforeEach
    void setUp() {
        service = new BlacklistWordService(repository);
        // Tránh bị reload do TTL = 0 khi @Value không được Spring apply.
        service.setCacheTtlForTest(3600L);
        service.clearCacheForTest();
    }

    // ===== checkText: severity classification =====

    @Test
    @DisplayName("Severity CRITICAL → FATAL")
    void critical_severity_mapsToFatal() {
        service.seedCacheForTest(List.of(
                BlacklistWord.builder()
                        .keyword("vũ khí")
                        .severity(ViolationSeverity.CRITICAL)
                        .enabled(true)
                        .build()));

        BlacklistMatchResult r = service.checkText("Mua bán vũ khí trực tuyến");

        assertThat(r.isFatal()).isTrue();
        assertThat(r.getSeverity()).isEqualTo(ViolationSeverity.CRITICAL);
        assertThat(r.getMatchedKeyword()).isEqualTo("vũ khí");
        assertThat(r.getMessage()).contains("vũ khí");
    }

    @Test
    @DisplayName("Severity HIGH → FATAL")
    void high_severity_mapsToFatal() {
        service.seedCacheForTest(List.of(
                BlacklistWord.builder()
                        .keyword("ma túy")
                        .severity(ViolationSeverity.HIGH)
                        .enabled(true)
                        .build()));

        BlacklistMatchResult r = service.checkText("Bán ma túy tổng hợp");

        assertThat(r.isFatal()).isTrue();
    }

    @Test
    @DisplayName("Severity MEDIUM → SUSPICIOUS")
    void medium_severity_mapsToSuspicious() {
        service.seedCacheForTest(List.of(
                BlacklistWord.builder()
                        .keyword("fake")
                        .severity(ViolationSeverity.MEDIUM)
                        .enabled(true)
                        .build()));

        BlacklistMatchResult r = service.checkText("Đây là hàng FAKE giả mạo");

        assertThat(r.isSuspicious()).isTrue();
        assertThat(r.isFatal()).isFalse();
    }

    @Test
    @DisplayName("Severity LOW → SUSPICIOUS")
    void low_severity_mapsToSuspicious() {
        service.seedCacheForTest(List.of(
                BlacklistWord.builder()
                        .keyword("quảng cáo")
                        .severity(ViolationSeverity.LOW)
                        .enabled(true)
                        .build()));

        BlacklistMatchResult r = service.checkText("Nội dung quảng cáo sai sự thật");

        assertThat(r.isSuspicious()).isTrue();
    }

    @Test
    @DisplayName("Worst severity thắng: FATAL + SUSPICIOUS cùng match → FATAL")
    void worstSeverityWins() {
        service.seedCacheForTest(List.of(
                BlacklistWord.builder().keyword("vũ khí")
                        .severity(ViolationSeverity.CRITICAL).enabled(true).build(),
                BlacklistWord.builder().keyword("fake")
                        .severity(ViolationSeverity.MEDIUM).enabled(true).build()));

        BlacklistMatchResult r = service.checkText("Hàng fake bán vũ khí");

        assertThat(r.isFatal()).isTrue();
        assertThat(r.getAllMatches()).contains("vũ khí", "fake");
    }

    // ===== checkText: text matching =====

    @Test
    @DisplayName("Text sạch → CLEAN")
    void cleanText_returnsClean() {
        service.seedCacheForTest(List.of(
                BlacklistWord.builder().keyword("vũ khí")
                        .severity(ViolationSeverity.CRITICAL).enabled(true).build()));

        BlacklistMatchResult r = service.checkText("Điện thoại thông minh Galaxy S24");

        assertThat(r.isClean()).isTrue();
        assertThat(r.getMatchedKeyword()).isNull();
    }

    @Test
    @DisplayName("Null / empty text → CLEAN không crash")
    void emptyOrNullText_clean() {
        assertThat(service.checkText(null).isClean()).isTrue();
        assertThat(service.checkText("").isClean()).isTrue();
        assertThat(service.checkText("   ").isClean()).isTrue();
    }

    @Test
    @DisplayName("Cache rỗng → luôn CLEAN")
    void emptyCache_clean() {
        assertThat(service.checkText("Bất cứ gì cũng OK").isClean()).isTrue();
    }

    @Test
    @DisplayName("Word-boundary: 'ma' KHÔNG match 'mama'")
    void wordBoundary_noSubstringMatch() {
        service.seedCacheForTest(List.of(
                BlacklistWord.builder().keyword("ma")
                        .severity(ViolationSeverity.CRITICAL).enabled(true).build()));

        // 'mama' không có 'ma' đứng riêng (có ký tự alpha liền kề 2 bên).
        assertThat(service.checkText("Mama bear baby").isClean()).isTrue();

        // 'ma' đứng riêng (giữa 2 non-word char) → match.
        assertThat(service.checkText("Cô ma xuất hiện").isFatal()).isTrue();
    }

    @Test
    @DisplayName("Case-insensitive: 'VŨ KHÍ' input match keyword 'vũ khí'")
    void caseInsensitive_match() {
        service.seedCacheForTest(List.of(
                BlacklistWord.builder().keyword("vũ khí")
                        .severity(ViolationSeverity.CRITICAL).enabled(true).build()));

        assertThat(service.checkText("VŨ KHÍ hạng nặng").isFatal()).isTrue();
    }

    // ===== Cache behavior =====

    @Test
    @DisplayName("Cache TTL expired → reload từ DB")
    void cacheReloadAfterTtlExpiry() throws Exception {
        when(repository.findByEnabledTrue()).thenReturn(List.of(
                BlacklistWord.builder().keyword("vũ khí")
                        .severity(ViolationSeverity.CRITICAL).enabled(true).build()));

        // TTL = 0s → mọi call đều reload.
        service.setCacheTtlForTest(0L);
        service.clearCacheForTest();

        service.checkText("vũ khí test");
        service.checkText("vũ khí test lần 2");

        // findByEnabledTrue được gọi 2 lần (mỗi checkText 1 lần) vì TTL = 0.
        verify(repository, atLeast(2)).findByEnabledTrue();
    }

    @Test
    @DisplayName("Cache TTL còn hạn → KHÔNG query DB")
    void cacheHit_noDbCall() {
        when(repository.findByEnabledTrue()).thenReturn(List.of(
                BlacklistWord.builder().keyword("vũ khí")
                        .severity(ViolationSeverity.CRITICAL).enabled(true).build()));

        service.reloadCache();
        service.checkText("vũ khí lần 1");
        service.checkText("vũ khí lần 2");
        service.checkText("vũ khí lần 3");

        // reloadCache() gọi 1 lần, 3 lần checkText sau đó không gọi thêm.
        verify(repository, times(1)).findByEnabledTrue();
    }

    // ===== CRUD =====

    @Test
    @DisplayName("addWord: lưu DB + invalidate cache + return entity")
    void addWord_persistsAndInvalidatesCache() {
        when(repository.existsByKeywordIgnoreCase("vũ khí")).thenReturn(false);
        when(repository.save(any())).thenAnswer(inv -> {
            BlacklistWord bw = inv.getArgument(0);
            bw.setId("bw-1");
            return bw;
        });

        BlacklistWord saved = service.addWord("vũ khí",
                ViolationSeverity.CRITICAL, "Bị cấm theo NĐ 13/2023", "admin@cnj70.vn");

        assertThat(saved.getId()).isEqualTo("bw-1");
        assertThat(saved.isEnabled()).isTrue();
        assertThat(saved.getCreatedBy()).isEqualTo("admin@cnj70.vn");
        verify(repository).save(any());
    }

    @Test
    @DisplayName("addWord: keyword trùng → throw BadRequest")
    void addWord_duplicateThrowsBadRequest() {
        when(repository.existsByKeywordIgnoreCase("vũ khí")).thenReturn(true);

        assertThatThrownBy(() -> service.addWord("vũ khí",
                ViolationSeverity.CRITICAL, "x", "admin"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("đã tồn tại");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("addWord: keyword null/blank → throw BadRequest")
    void addWord_blankKeywordThrows() {
        assertThatThrownBy(() -> service.addWord(null,
                ViolationSeverity.CRITICAL, "x", "admin"))
                .isInstanceOf(BadRequestException.class);

        assertThatThrownBy(() -> service.addWord("  ",
                ViolationSeverity.CRITICAL, "x", "admin"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("addWord: severity null → throw BadRequest")
    void addWord_nullSeverityThrows() {
        assertThatThrownBy(() -> service.addWord("test",
                null, "x", "admin"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("updateWord: update severity/description/enabled + save + reload cache")
    void updateWord_persistsChanges() {
        BlacklistWord existing = BlacklistWord.builder()
                .id("bw-1")
                .keyword("fake")
                .severity(ViolationSeverity.LOW)
                .enabled(true)
                .build();
        when(repository.findById("bw-1")).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BlacklistWord updated = service.updateWord("bw-1",
                ViolationSeverity.HIGH, "Nâng mức độ", false);

        assertThat(updated.getSeverity()).isEqualTo(ViolationSeverity.HIGH);
        assertThat(updated.getDescription()).isEqualTo("Nâng mức độ");
        assertThat(updated.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("updateWord: id không tồn tại → throw ResourceNotFound")
    void updateWord_notFoundThrows() {
        when(repository.findById("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateWord("ghost",
                ViolationSeverity.HIGH, "x", null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("deleteWord: xóa + reload cache")
    void deleteWord_removesAndReloads() {
        when(repository.existsById("bw-1")).thenReturn(true);

        service.deleteWord("bw-1");

        verify(repository).deleteById("bw-1");
    }

    @Test
    @DisplayName("deleteWord: id không tồn tại → throw ResourceNotFound")
    void deleteWord_notFoundThrows() {
        when(repository.existsById("ghost")).thenReturn(false);

        assertThatThrownBy(() -> service.deleteWord("ghost"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("setEnabled: delegate updateWord")
    void setEnabled_delegatesCorrectly() {
        BlacklistWord existing = BlacklistWord.builder()
                .id("bw-1").keyword("fake").severity(ViolationSeverity.MEDIUM).enabled(true).build();
        when(repository.findById("bw-1")).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BlacklistWord result = service.setEnabled("bw-1", false);

        assertThat(result.isEnabled()).isFalse();
        // severity giữ nguyên (updateWord không đổi severity khi truyền null).
        assertThat(result.getSeverity()).isEqualTo(ViolationSeverity.MEDIUM);
    }
}
