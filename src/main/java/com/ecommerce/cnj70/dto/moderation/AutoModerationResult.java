package com.ecommerce.cnj70.dto.moderation;

import com.ecommerce.cnj70.enums.AutoModerationFlag;
import com.ecommerce.cnj70.enums.AutoModerationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Phase 2A — Auto Moderation Result (integration seam).
 *
 * <p>Returned by {@code AutoModerationResultProvider}. Phase 2A consumes
 * this DTO to display Auto Moderation result on Product Detail.</p>
 *
 * <p>Phase 2A does NOT compute this result — Auto Moderation Engine
 * (owned by another team) is the source of truth. When the backend is
 * unavailable, the provider returns {@code Optional.empty()} (Phase 2A
 * §11).</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutoModerationResult {

    /** Backend result status. Never null when present. */
    private AutoModerationStatus status;

    /** Detected flags. May be empty. */
    @Builder.Default
    private List<AutoModerationFlag> flags = new ArrayList<>();

    /** Severity label (LOW/MEDIUM/HIGH/CRITICAL). Optional. */
    private String severity;

    /** Free-form summary reason. Optional. */
    private String reason;

    /** Supporting evidence. May be empty. */
    @Builder.Default
    private List<AutoModerationEvidence> evidence = new ArrayList<>();

    /** When the backend ran the check. */
    private LocalDateTime checkedAt;
}
