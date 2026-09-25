package com.ecommerce.cnj70.dto.moderation;

import com.ecommerce.cnj70.enums.AutoModerationFlag;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Phase 2A — Evidence piece from Auto Moderation Engine.
 *
 * <p>Display-only DTO. Phase 2A never creates fake evidence; it only
 * displays what the integration seam returns.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutoModerationEvidence {
    private AutoModerationFlag flagType;
    private String evidenceType;
    private String evidenceValue;
    private String evidenceSource;
    private LocalDateTime createdAt;
}
