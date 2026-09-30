package com.ecommerce.cnj70.service.automation;

import com.ecommerce.cnj70.document.BlacklistWord;
import com.ecommerce.cnj70.enums.ViolationSeverity;
import com.ecommerce.cnj70.exception.BadRequestException;
import com.ecommerce.cnj70.exception.ResourceNotFoundException;
import com.ecommerce.cnj70.repository.BlacklistWordRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Group 1 — Centralized service quản lý Blacklist Word.
 *
 * <p>Thay thế cơ chế config-only (đọc từ {@code application.yml}) của
 * {@link BlacklistKeywordCheck} cũ. Cho phép Admin CRUD keyword qua DB
 * mà không cần deploy lại.</p>
 *
 * <p>Cấu trúc:</p>
 * <ul>
 *   <li>DB: {@link BlacklistWordRepository} (collection {@code blacklist_words}).</li>
 *   <li>Cache: in-memory TTL cache trong {@link #cache} — mặc định 5 phút.</li>
 *   <li>API: {@link #checkText(String)} → {@link BlacklistMatchResult}.</li>
 *   <li>Admin CRUD: {@link #addWord}, {@link #updateWord}, {@link #deleteWord},
 *       {@link #setEnabled}. Mỗi thao tác CUD sẽ invalidate cache.</li>
 * </ul>
 *
 * <p>Logic scan:</p>
 * <ol>
 *   <li>Load danh sách {@code BlacklistWord} (đã enabled) từ cache.</li>
 *   <li>Với mỗi keyword, dùng regex word-boundary để match (case-insensitive).</li>
 *   <li>Tổng hợp severity cao nhất → trả {@link BlacklistMatchResult}.</li>
 * </ul>
 *
 * <p>Fail-safe: nếu regex compile fail → skip keyword đó, log warning, tiếp tục
 * keyword kế tiếp. Không bao giờ throw exception ra caller.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BlacklistWordService {

    private final BlacklistWordRepository repository;

    /** TTL cache mặc định 5 phút — override qua config. */
    @Value("${moderation.blacklist.cache-ttl-seconds:300}")
    private long cacheTtlSeconds = 300L;

    /** Cache in-memory: keyword lowercase → severity. */
    private final Map<String, CachedEntry> cache = new ConcurrentHashMap<>();
    /** Marker để invalidate nhanh toàn bộ cache khi CRUD. */
    private volatile Instant cacheLoadedAt = Instant.EPOCH;

    @PostConstruct
    void init() {
        log.info("[BlacklistWord] Initialized with cache TTL = {}s", cacheTtlSeconds);
        reloadCache();
    }

    // ============ Cache management ============

    /**
     * Force reload cache từ DB. Dùng khi Admin thêm/sửa/xóa keyword.
     */
    public synchronized void reloadCache() {
        List<BlacklistWord> all = repository.findByEnabledTrue();
        cache.clear();
        for (BlacklistWord bw : all) {
            if (bw.getKeyword() == null || bw.getKeyword().isBlank()) continue;
            cache.put(bw.getKeyword().toLowerCase(),
                    new CachedEntry(bw.getKeyword().toLowerCase(), bw.getSeverity()));
        }
        cacheLoadedAt = Instant.now();
        log.info("[BlacklistWord] Cache reloaded: {} keyword(s) loaded", cache.size());
    }

    private boolean isCacheExpired() {
        return Duration.between(cacheLoadedAt, Instant.now()).getSeconds() >= cacheTtlSeconds;
    }

    private void ensureFreshCache() {
        if (isCacheExpired()) {
            reloadCache();
        }
    }

    // ============ Scan API ============

    /**
     * Scan text chống lại danh sách blacklist.
     *
     * @param text văn bản cần scan (Product name + description, Review comment,...)
     * @return kết quả match — không bao giờ null
     */
    public BlacklistMatchResult checkText(String text) {
        if (text == null || text.isBlank()) {
            return BlacklistMatchResult.clean();
        }
        ensureFreshCache();

        if (cache.isEmpty()) {
            return BlacklistMatchResult.clean();
        }

        String lower = text.toLowerCase();
        BlacklistMatchResult.Decision worstDecision = BlacklistMatchResult.Decision.CLEAN;
        ViolationSeverity worstSeverity = null;
        String firstMatch = null;
        String firstMessage = null;
        List<String> allMatches = new ArrayList<>();

        for (CachedEntry entry : cache.values()) {
            try {
                if (matchesKeyword(lower, entry.keyword)) {
                    allMatches.add(entry.keyword);
                    if (firstMatch == null) {
                        firstMatch = entry.keyword;
                    }
                    BlacklistMatchResult.Decision mapped = mapDecision(entry.severity);
                    if (compareDecision(mapped, worstDecision) > 0) {
                        worstDecision = mapped;
                        worstSeverity = entry.severity;
                    }
                }
            } catch (Exception ex) {
                log.warn("[BlacklistWord] scan failed for keyword '{}': {}",
                        entry.keyword, ex.getMessage());
            }
        }

        if (worstSeverity == null) {
            return BlacklistMatchResult.clean();
        }

        firstMessage = String.format("Nội dung chứa từ khóa bị cấm: '%s' (mức %s)",
                firstMatch, worstSeverity);
        return BlacklistMatchResult.of(worstDecision, firstMatch, worstSeverity,
                firstMessage, allMatches);
    }

    /**
     * Kiểm tra {@code keyword} xuất hiện trong {@code lowerText} với word-boundary
     * an toàn Unicode — dùng {@link Character#isLetterOrDigit(char)} thay cho
     * regex {@code \w} (regex {@code \w} chỉ match ASCII, không bao gồm ký tự
     * có dấu tiếng Việt).
     *
     * <p>Cho phép match khi cả 2 phía ranh giới là non-word char hoặc đầu/cuối chuỗi.</p>
     */
    static boolean matchesKeyword(String lowerText, String lowerKeyword) {
        if (lowerText == null || lowerKeyword == null || lowerKeyword.isEmpty()) {
            return false;
        }
        int idx = 0;
        while ((idx = lowerText.indexOf(lowerKeyword, idx)) >= 0) {
            boolean beforeOk = (idx == 0) || !isWordChar(lowerText.charAt(idx - 1));
            int afterIdx = idx + lowerKeyword.length();
            boolean afterOk = (afterIdx >= lowerText.length())
                    || !isWordChar(lowerText.charAt(afterIdx));
            if (beforeOk && afterOk) {
                return true;
            }
            idx++;
        }
        return false;
    }

    private static boolean isWordChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }

    /**
     * Map {@link ViolationSeverity} của keyword sang {@link BlacklistMatchResult.Decision}.
     * CRITICAL / HIGH → FATAL; MEDIUM / LOW → SUSPICIOUS.
     */
    private BlacklistMatchResult.Decision mapDecision(ViolationSeverity severity) {
        if (severity == null) return BlacklistMatchResult.Decision.SUSPICIOUS;
        return switch (severity) {
            case CRITICAL, HIGH -> BlacklistMatchResult.Decision.FATAL;
            case MEDIUM, LOW -> BlacklistMatchResult.Decision.SUSPICIOUS;
        };
    }

    /**
     * So sánh 2 decision theo thứ tự CLEAN < SUSPICIOUS < FATAL.
     * Trả về số dương nếu {@code a} nghiêm trọng hơn {@code b}.
     */
    private static int compareDecision(BlacklistMatchResult.Decision a,
                                       BlacklistMatchResult.Decision b) {
        return rank(a) - rank(b);
    }

    private static int rank(BlacklistMatchResult.Decision d) {
        return switch (d) {
            case CLEAN -> 0;
            case SUSPICIOUS -> 1;
            case FATAL -> 2;
        };
    }

    /**
     * Build word-boundary pattern. Dùng {@code (?<![\wđĐ])} và
     * {@code (?![\wđĐ])} thay {@code \b} để xử lý tốt tiếng Việt có dấu.
     * Keyword được wrap trong {@code \Q...\E} để escape regex meta chars;
     * literal {@code \E} trong keyword phải escape thành {@code \\E} để
     * không kết thúc quote sớm.
     */
    private String compilePattern(String keyword) {
        String escaped = keyword.toLowerCase().replace("\\E", "\\\\E");
        return "(?<![\\wđĐ])\\Q" + escaped + "\\E(?![\\wđĐ])";
    }

    // ============ Admin CRUD ============

    @Transactional
    public BlacklistWord addWord(String keyword, ViolationSeverity severity,
                                  String description, String createdBy) {
        if (keyword == null || keyword.isBlank()) {
            throw new BadRequestException("Từ khóa không được để trống");
        }
        if (severity == null) {
            throw new BadRequestException("Mức độ không được null");
        }
        String trimmed = keyword.trim();
        if (repository.existsByKeywordIgnoreCase(trimmed)) {
            throw new BadRequestException("Từ khóa đã tồn tại: " + trimmed);
        }
        BlacklistWord entity = BlacklistWord.builder()
                .keyword(trimmed)
                .severity(severity)
                .description(description)
                .enabled(true)
                .createdBy(createdBy)
                .build();
        BlacklistWord saved = repository.save(entity);
        reloadCache();
        log.info("[BlacklistWord] Added keyword '{}' severity={} by={}", trimmed, severity, createdBy);
        return saved;
    }

    @Transactional
    public BlacklistWord updateWord(String id, ViolationSeverity severity,
                                     String description, Boolean enabled) {
        BlacklistWord existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy blacklist word id=" + id));
        if (severity != null) existing.setSeverity(severity);
        if (description != null) existing.setDescription(description);
        if (enabled != null) existing.setEnabled(enabled);
        BlacklistWord saved = repository.save(existing);
        reloadCache();
        log.info("[BlacklistWord] Updated keyword id={} severity={} enabled={}",
                id, saved.getSeverity(), saved.isEnabled());
        return saved;
    }

    @Transactional
    public void deleteWord(String id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Không tìm thấy blacklist word id=" + id);
        }
        repository.deleteById(id);
        reloadCache();
        log.info("[BlacklistWord] Deleted keyword id={}", id);
    }

    @Transactional
    public BlacklistWord setEnabled(String id, boolean enabled) {
        return updateWord(id, null, null, enabled);
    }

    public List<BlacklistWord> getAll() {
        return repository.findAll();
    }

    public List<BlacklistWord> getEnabled() {
        return repository.findByEnabledTrue();
    }

    public BlacklistWord getById(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy blacklist word id=" + id));
    }

    /** Test-only: reset cache (cho unit test inject keyword trực tiếp). */
    public void clearCacheForTest() {
        cache.clear();
        cacheLoadedAt = Instant.EPOCH;
    }

    /** Test-only: set cache TTL (vì @Value không apply khi new manual). */
    public void setCacheTtlForTest(long ttlSeconds) {
        this.cacheTtlSeconds = ttlSeconds;
    }

    /** Test-only: seed cache (cho unit test không cần MongoDB). */
    public void seedCacheForTest(List<BlacklistWord> words) {
        cache.clear();
        for (BlacklistWord bw : words) {
            cache.put(bw.getKeyword().toLowerCase(),
                    new CachedEntry(bw.getKeyword().toLowerCase(), bw.getSeverity()));
        }
        cacheLoadedAt = Instant.now();
    }

    public int getCacheSize() {
        return cache.size();
    }

    // ============ Internal ============

    private record CachedEntry(String keyword, ViolationSeverity severity) {}
}
