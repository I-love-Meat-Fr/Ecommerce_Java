package com.ecommerce.cnj70.document;

import com.ecommerce.cnj70.enums.ModerationStatus;
import com.ecommerce.cnj70.enums.ProductStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "products")
public class Product {

    @Id
    private String id;

    private String shopId;

    private String shopName;

    @Indexed
    private String name;

    private String brand;

    private Integer warrantyMonths;

    private String manufacturer;

    private String manufacturerAddress;

    private String description;

    private String richDescription;

    private BigDecimal price;

    private int stock;

    private String categoryId;

    private String categoryName;

    private List<String> imageUrls;

    private String thumbnailUrl;

    @Builder.Default
    private List<ProductSpecification> specifications = new ArrayList<>();

    @Builder.Default
    private List<ProductVariant> variants = new ArrayList<>();

    @Builder.Default
    private ProductStatus status = ProductStatus.DRAFT;

    /**
     * Phase 2A — Moderation workflow status. Orthogonal to {@link #status}.
     * <p>
     * Existing documents created before Phase 2A will have {@code null},
     * which means "not in moderation pipeline" (treated as no-op for
     * Moderator queue / actions). This preserves backward compatibility
     * — Admin Product Management and the public-facing ProductStatus
     * code paths are not affected.
     */
    @Indexed
    private ModerationStatus moderationStatus;

    /**
     * Phase 2A — Moderator who most recently processed this product
     * (Approve / Reject / Escalate). Set together with {@link #moderationStatus}.
     */
    private String moderationActorId;

    /**
     * Phase 2A — Reason supplied with the most recent moderation action.
     * Required for REJECT and ESCALATE; null for APPROVE.
     */
    private String moderationReason;

    /**
     * Phase 2A — Timestamp of the most recent moderation action.
     */
    private java.time.LocalDateTime moderationAt;

    /**
     * Phase 3A — Admin who most recently enforced on this product
     * (Suspend / Unhide). Set when Admin enforcement changes {@link #status}
     * (e.g. ACTIVE → HIDDEN).
     */
    private String enforcementActorId;

    /**
     * Phase 3A — Reason supplied with the most recent enforcement action.
     */
    private String enforcementReason;

    /**
     * Phase 3A — Timestamp of the most recent enforcement action.
     */
    private java.time.LocalDateTime enforcementAt;

    @Builder.Default
    private double rating = 0.0;

    @Builder.Default
    private int reviewCount = 0;

    @Builder.Default
    private int sold = 0;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
