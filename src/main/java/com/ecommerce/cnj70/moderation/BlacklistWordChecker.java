package com.ecommerce.cnj70.moderation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Yêu cầu #1 — Blacklist Word Checker (task #3.0).
 *
 * <p>Quét văn bản của đối tượng (Product hoặc Review) để phát hiện từ khóa cấm
 * (vũ khí, chất kích thích, ngôn từ thù ghét...). Hoạt động cho cả
 * {@code Product} (name + description) và {@code Review} (comment) thông qua
 * interface {@link ModerationContext} thống nhất.</p>
 *
 * <p>Mức độ:</p>
 * <ul>
 *   <li>Từ khóa nằm trong {@code hardRejectKeywords} → {@link ModerationDecision.Severity#FATAL}.</li>
 *   <li>Từ khóa nằm trong {@code suspiciousKeywords} → {@link ModerationDecision.Severity#SUSPICIOUS}.</li>
 *   <li>Không match → PASS.</li>
 * </ul>
 *
 * <p>Logic:</p>
 * <ul>
 *   <li>Case-insensitive.</li>
 *   <li>Word-boundary để giảm false positive (vd "vũ khí" không match "vũ_khí").</li>
 *   <li>Nếu keyword không hợp lệ regex → fail-safe: trả PASS để không chặn pipeline.</li>
 *   <li>Chỉ match keyword đầu tiên để tránh spam reason; flagCode = {@code "BLACKLIST_HIT"}.</li>
 * </ul>
 *
 * <p>Fail-safe: nếu xảy ra exception khi compile regex → trả PASS để không chặn
 * cả pipeline; log error cho admin biết.</p>
 */
@Slf4j
@Component
public class BlacklistWordChecker implements ModerationChecker {

    /** ID định danh checker. */
    public static final String ID = "BLACKLIST_WORD";

    /** FlagCode cho ReportCase.autoFlags. */
    public static final String FLAG_CODE = "BLACKLIST_HIT";

    /** Từ khóa nặng → auto reject. */
    private final List<String> hardRejectKeywords;

    /** Từ khóa nhẹ → đẩy vào queue review. */
    private final List<String> suspiciousKeywords;

    public BlacklistWordChecker(
            @Value("${moderation.auto.blacklist-keywords:}") List<String> hardRejectKeywords,
            @Value("${moderation.auto.suspicious-keywords:}") List<String> suspiciousKeywords) {
        this.hardRejectKeywords = sanitize(hardRejectKeywords);
        this.suspiciousKeywords = sanitize(suspiciousKeywords);
        log.info("[BlacklistWord] Initialized: hard={} suspicious={}",
                this.hardRejectKeywords.size(), this.suspiciousKeywords.size());
    }

    private static List<String> sanitize(List<String> input) {
        if (input == null) return List.of();
        return input.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(String::trim)
                .toList();
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public int order() {
        // Chạy đầu tiên — rẻ, fail-fast cho FATAL.
        return 10;
    }

    @Override
    public boolean supports(ModerationContext context) {
        // Áp dụng cho cả Product và Review (cả 2 đều có text).
        return context != null && context.getText() != null;
    }

    @Override
    public ModerationDecision check(ModerationContext context) {
        if (context == null || context.getText() == null || context.getText().isBlank()) {
            return ModerationDecision.pass();
        }

        String combined = context.getText();
        String lower = combined.toLowerCase();

        // 1) FATAL trước — nếu match keyword nặng thì short-circuit.
        for (String keyword : hardRejectKeywords) {
            if (matches(lower, keyword)) {
                String msg = "Nội dung chứa từ khóa bị cấm (mức nặng): '" + keyword + "'";
                log.warn("[BlacklistWord] FATAL target={} type={} hit='{}'",
                        context.getTargetId(), context.getTargetType(), keyword);
                return ModerationDecision.fatal(ID, FLAG_CODE, msg);
            }
        }

        // 2) SUSPICIOUS — match keyword nhẹ.
        for (String keyword : suspiciousKeywords) {
            if (matches(lower, keyword)) {
                String msg = "Nội dung chứa từ khóa đáng ngờ: '" + keyword + "'";
                log.info("[BlacklistWord] SUSPICIOUS target={} type={} hit='{}'",
                        context.getTargetId(), context.getTargetType(), keyword);
                return ModerationDecision.suspicious(ID, FLAG_CODE, msg);
            }
        }

        return ModerationDecision.pass();
    }

    /**
     * Word-boundary matching (case-insensitive). Dùng {@code (?<![\wđĐ])} và
     * {@code (?![\wđĐ])} thay cho {@code \b} để xử lý tốt tiếng Việt có dấu.
     */
    private boolean matches(String lowerCombined, String keyword) {
        try {
            String safeKw = Pattern.quote(keyword.toLowerCase());
            Pattern p = Pattern.compile("(?<![\\wđĐ])\\Q" + safeKw + "\\E(?![\\wđĐ])",
                    Pattern.UNICODE_CASE);
            return p.matcher(lowerCombined).find();
        } catch (Exception ex) {
            // Fail-safe: keyword không hợp lệ regex → không chặn.
            log.warn("[BlacklistWord] regex compile failed for '{}': {}", keyword, ex.getMessage());
            return false;
        }
    }

    /**
     * Test-only helper để unit test không cần load Spring context.
     */
    public static BlacklistWordChecker forTest(List<String> hard, List<String> suspicious) {
        return new BlacklistWordChecker(hard, suspicious);
    }
}
