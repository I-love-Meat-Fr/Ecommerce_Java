package com.ecommerce.cnj70.service.automation;

import com.ecommerce.cnj70.enums.ViolationSeverity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Kết quả scan của {@link BlacklistWordService#checkText(String)}.
 *
 * <p>Phân cấp nghiêm trọng:</p>
 * <ul>
 *   <li>{@link Decision#CLEAN} — không match keyword nào.</li>
 *   <li>{@link Decision#SUSPICIOUS} — match keyword mức MEDIUM / LOW.
 *       Pipeline sẽ escalate sang MANUAL_REVIEW.</li>
 *   <li>{@link Decision#FATAL} — match keyword mức HIGH / CRITICAL.
 *       Pipeline sẽ HARD_REJECT.</li>
 * </ul>
 */
@Getter
public class BlacklistMatchResult {

    public enum Decision {
        CLEAN,
        SUSPICIOUS,
        FATAL
    }

    private final Decision decision;
    private final String matchedKeyword;
    private final ViolationSeverity severity;
    private final String message;
    private final List<String> allMatches;

    private BlacklistMatchResult(Decision decision, String matchedKeyword,
                                  ViolationSeverity severity, String message,
                                  List<String> allMatches) {
        this.decision = decision;
        this.matchedKeyword = matchedKeyword;
        this.severity = severity;
        this.message = message;
        this.allMatches = allMatches == null ? Collections.emptyList() : List.copyOf(allMatches);
    }

    public static BlacklistMatchResult clean() {
        return new BlacklistMatchResult(Decision.CLEAN, null, null, null, List.of());
    }

    public static BlacklistMatchResult of(Decision decision, String matchedKeyword,
                                          ViolationSeverity severity, String message) {
        return new BlacklistMatchResult(decision, matchedKeyword, severity, message, List.of());
    }

    public static BlacklistMatchResult of(Decision decision, String matchedKeyword,
                                          ViolationSeverity severity, String message,
                                          List<String> allMatches) {
        return new BlacklistMatchResult(decision, matchedKeyword, severity, message, allMatches);
    }

    public boolean isClean() {
        return decision == Decision.CLEAN;
    }

    public boolean isFatal() {
        return decision == Decision.FATAL;
    }

    public boolean isSuspicious() {
        return decision == Decision.SUSPICIOUS;
    }
}
