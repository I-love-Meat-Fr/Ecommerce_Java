package com.ecommerce.cnj70.migration;

import com.ecommerce.cnj70.document.AuditLog;
import com.ecommerce.cnj70.enums.AuditAction;
import com.ecommerce.cnj70.enums.AuditSeverity;
import com.ecommerce.cnj70.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * TASK #27.0 — Migration utility tối thiểu cho các field mới thêm vào
 * {@link com.ecommerce.cnj70.document.User}, {@link com.ecommerce.cnj70.document.Shop},
 * {@link com.ecommerce.cnj70.document.Product}, {@link com.ecommerce.cnj70.document.Order}.
 *
 * <p>Idempotency: mỗi migration có {@code id} (vd {@code "MIG-001"}). Runner
 * dùng {@link AuditLog} với {@code resourceId = migration id} làm marker đã
 * chạy. Mỗi lần startup, query audit để check — nếu marker tồn tại thì skip.
 *
 * <p>An toàn: KHÔNG bao giờ overwrite field có giá trị — chỉ {@code setOnInsert}
 * cho missing fields (vd default role nếu null).
 *
 * <p>Hiện tại các migration:
 * <ul>
 *   <li>{@code MIG-001}: User có role null → role = CUSTOMER (mặc định).</li>
 *   <li>{@code MIG-002}: Shop có status null → status = PENDING.</li>
 *   <li>{@code MIG-003}: Product có status null → status = DRAFT.</li>
 *   <li>{@code MIG-004}: Product có moderationStatus null — KHÔNG set (null = legacy, intentional).</li>
 *   <li>{@code MIG-005}: User có emailVerified null → emailVerified = false (chỉ thông tin, không enforce).</li>
 * </ul>
 *
 * <p>Không dùng {@code @PostConstruct} cho migration — chỉ chạy khi
 * {@link #runAll()} được gọi explicit (từ {@code ApplicationRunner} hoặc CLI).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataMigrationRunner {

    private final MongoTemplate mongoTemplate;
    private final AuditLogRepository auditLogRepository;

    /** Tên collection giữ marker migration; mặc định dùng audit_logs luôn. */
    private static final String MIGRATION_AUDIT_RESOURCE_TYPE = "MIGRATION";

    /**
     * Chạy tất cả migration định nghĩa sẵn theo thứ tự. Mỗi migration check
     * marker trước khi chạy → idempotent.
     */
    public void runAll() {
        log.info("[Migration] Running all data migrations...");
        try {
            runIfNotApplied("MIG-001", () -> backfillNullUserRole());
            runIfNotApplied("MIG-002", () -> backfillNullShopStatus());
            runIfNotApplied("MIG-003", () -> backfillNullProductStatus());
            // MIG-004 là no-op, không cần backfill (null = legacy intentional).
            runIfNotApplied("MIG-005", () -> backfillNullUserEmailVerified());
            log.info("[Migration] Data migrations complete.");
        } catch (Exception e) {
            log.error("[Migration] Error during migrations", e);
            // KHÔNG throw — application vẫn phải boot được dù migration fail.
        }
    }

    private void runIfNotApplied(String migId, Runnable migrate) {
        try {
            // Idempotency: check marker audit log đã ghi với resourceId = migId.
            // Dùng findByResourceId (Page<AuditLog>) và lấy trang 1.
            boolean alreadyApplied = !auditLogRepository
                    .findByResourceId(migId, PageRequest.of(0, 1))
                    .isEmpty();
            if (alreadyApplied) {
                log.info("[Migration] {} already applied, skipping", migId);
                return;
            }
            log.info("[Migration] Applying {} ...", migId);
            migrate.run();
            recordMigrationApplied(migId);
            log.info("[Migration] {} applied successfully", migId);
        } catch (Exception ex) {
            log.error("[Migration] {} failed: {}", migId, ex.getMessage(), ex);
        }
    }

    private void backfillNullUserRole() {
        // Update chỉ áp dụng cho document có role = null, KHÔNG đụng vào role đã set.
        // (Mongo $set sẽ ghi đè nếu target. Khắc phục: dùng $setOnInsert hoặc query trước.)
        // Đơn giản: nếu role null → set role = CUSTOMER.
        long updated = mongoTemplate.updateMulti(
                Query.query(Criteria.where("role").is(null)),
                new Update().set("role", "CUSTOMER"),
                "users").getModifiedCount();
        log.info("[Migration] MIG-001 User.role backfilled for {} document(s)", updated);
    }

    private void backfillNullShopStatus() {
        long updated = mongoTemplate.updateMulti(
                Query.query(Criteria.where("status").is(null)),
                new Update().set("status", "PENDING"),
                "shops").getModifiedCount();
        log.info("[Migration] MIG-002 Shop.status backfilled for {} document(s)", updated);
    }

    private void backfillNullProductStatus() {
        long updated = mongoTemplate.updateMulti(
                Query.query(Criteria.where("status").is(null)),
                new Update().set("status", "DRAFT"),
                "products").getModifiedCount();
        log.info("[Migration] MIG-003 Product.status backfilled for {} document(s)", updated);
    }

    private void backfillNullUserEmailVerified() {
        long updated = mongoTemplate.updateMulti(
                Query.query(Criteria.where("emailVerified").is(null)),
                new Update().set("emailVerified", false),
                "users").getModifiedCount();
        log.info("[Migration] MIG-005 User.emailVerified backfilled for {} document(s)", updated);
    }

    private void recordMigrationApplied(String migId) {
        try {
            AuditLog marker = AuditLog.builder()
                    .actorId("SYSTEM")
                    .actorUsername("migration")
                    .actorRole("SYSTEM")
                    .action(AuditAction.OTHER)
                    .resourceType(MIGRATION_AUDIT_RESOURCE_TYPE)
                    .resourceId(migId)
                    .reason("Data migration applied: " + migId)
                    .severity(AuditSeverity.INFO)
                    .metadata(Map.of(
                            "migrationId", migId,
                            "appliedAt", LocalDateTime.now().toString()))
                    .createdAt(LocalDateTime.now())
                    .build();
            auditLogRepository.save(marker);
        } catch (Exception ex) {
            log.warn("[Migration] Failed to record marker for {}: {}", migId, ex.getMessage());
        }
    }

    /**
     * Trả về danh sách migrations đã chạy (cho admin UI).
     */
    public List<AuditLog> getAppliedMigrations() {
        return auditLogRepository
                .findByResourceTypeAndResourceId(
                        MIGRATION_AUDIT_RESOURCE_TYPE, "*",
                        PageRequest.of(0, 1000))
                .getContent();
    }
}
