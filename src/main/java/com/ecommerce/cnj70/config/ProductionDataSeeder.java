package com.ecommerce.cnj70.config;

import com.ecommerce.cnj70.document.*;
import com.ecommerce.cnj70.document.Order.OrderItem;
import com.ecommerce.cnj70.document.Order.SubOrderShipping;
import com.ecommerce.cnj70.enums.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.ecommerce.cnj70.config.SeedPools.*;

/**
 * <h2>ProductionDataSeeder</h2>
 *
 * <p>Seeder dữ liệu thật vào MongoDB Atlas cho hệ thống CNJ70 Ecommerce.
 * Chạy được qua Spring Boot CLI:</p>
 *
 * <pre>
 * mvn spring-boot:run -Dspring-boot.run.profiles=seed ^
 *     -Dspring-boot.run.arguments="--app.seed.enabled=true --seed.target=users --seed.batch=0"
 * </pre>
 *
 * <p>Các tham số:</p>
 * <ul>
 *     <li><b>--app.seed.enabled=true</b> — bắt buộc kích hoạt bean.</li>
 *     <li><b>--seed.target=X</b> — một trong:
 *         <code>categories|users|shops|products|vouchers|banners|orders|reviews|carts|kyc|
 *         complaints|returns|refunds|reportcases|moderation|violations|escalations|legal|
 *         auditlogs|auditentries|scheduler</code>.
 *         Bỏ trống = chạy tất cả (full).</li>
 *     <li><b>--seed.batch=N</b> — chỉ số batch (0‑based). Mỗi batch = 100 bản ghi.
 *         Bỏ trống = chạy hết các batch của target.</li>
 *     <li><b>--seed.wipe</b> — wipe toàn bộ collections trước khi seed
 *         (chỉ áp dụng khi target=all).</li>
 * </ul>
 *
 * <p>Quy ước:</p>
 * <ul>
 *     <li>Seed cố định = 42, dữ liệu reproducible.</li>
 *     <li>Khoảng thời gian: 2021-09-01 → 2026-09-26 (5 năm).</li>
 *     <li>Mỗi lần chèn batch mới sẽ <i>không trùng</i> ID với batch trước
 *         (UUID v4).</li>
 *     <li>Các collection có FK (Order/Review/Complaint/...) tự load
 *         collection cha từ DB để tham chiếu.</li>
 * </ul>
 *
 * @author CNJ70 Seed Tooling
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class ProductionDataSeeder implements ApplicationRunner {

    private final MongoTemplate mongo;

    // ============================================================================
    // ============================== MAIN DISPATCH =============================
    // ============================================================================

    @Override
    public void run(ApplicationArguments args) {
        long t0 = System.currentTimeMillis();
        // Đọc từ cả ApplicationArguments lẫn System properties (để chạy được
        // qua cả `mvn spring-boot:run -- --arg` lẫn `mvn spring-boot:run -Darg`.
        String target = firstNonBlank(
                argVal(args, "seed.target"),
                System.getProperty("seed.target"),
                "all");
        Integer batch = parseIntOrNull(firstNonBlank(
                argVal(args, "seed.batch"),
                System.getProperty("seed.batch"),
                null));
        boolean wipe = args.containsOption("seed.wipe")
                || "true".equalsIgnoreCase(System.getProperty("seed.wipe"));

        log.info("=== ProductionDataSeeder START: target={}, batch={}, wipe={} ===", target, batch, wipe);

        // Nếu user chạy 1 target cụ thể ở batch=0:
        //   - Nếu --seed.wipe=true → FORCE drop collection target rồi seed lại từ đầu.
        //     (Dùng khi muốn reseed sạch dữ liệu cũ, ví dụ đổi logic shop chỉ bán Tech.)
        //   - Nếu collection rỗng / rất ít (< BATCH_SIZE) → an toàn để drop & re-seed.
        //   - Nếu collection đã có nhiều data → không drop, để giữ data; các batch sau
        //     (>= current/100) vẫn append được.
        if (!"all".equals(target) && batch != null && batch == 0) {
            if (wipe) {
                dropTargetCollection(target);
                log.info("  [wipe=true] Force-dropped collection '{}' before re-seed.", target);
            } else {
                wipeTargetIfEmpty(target);
            }
        }


        if (wipe && "all".equals(target)) {
            wipeAll();
        }

        // Phase 1: Independent (no FK hoặc FK tự tham chiếu)
        switchOrAll(target, "categories", batch, this::seedCategoriesBatch);
        switchOrAll(target, "users",      batch, this::seedUsersBatch);
        switchOrAll(target, "shops",      batch, this::seedShopsBatch);
        switchOrAll(target, "products",   batch, this::seedProductsBatch);
        switchOrAll(target, "vouchers",   batch, this::seedVouchersBatch);
        switchOrAll(target, "banners",    batch, this::seedBannersBatch);
        switchOrAll(target, "legal",      batch, this::seedLegalDocumentsBatch);

        // Phase 2: Dependent
        switchOrAll(target, "orders",       batch, this::seedOrdersBatch);
        switchOrAll(target, "reviews",      batch, this::seedReviewsBatch);
        switchOrAll(target, "carts",        batch, this::seedCartsBatch);
        switchOrAll(target, "kyc",          batch, this::seedKycProfilesBatch);
        switchOrAll(target, "complaints",   batch, this::seedComplaintsBatch);
        switchOrAll(target, "returns",      batch, this::seedReturnsBatch);
        switchOrAll(target, "refunds",      batch, this::seedRefundsBatch);
        switchOrAll(target, "reportcases",  batch, this::seedReportCasesBatch);
        switchOrAll(target, "moderation",   batch, this::seedModerationHistoryBatch);
        switchOrAll(target, "violations",   batch, this::seedViolationsBatch);
        switchOrAll(target, "escalations",  batch, this::seedEscalationsBatch);
        switchOrAll(target, "auditlogs",    batch, this::seedAuditLogsBatch);
        switchOrAll(target, "auditentries", batch, this::seedAuditLogEntriesBatch);
        switchOrAll(target, "scheduler",    batch, this::seedSchedulerRecordsBatch);

        long dt = System.currentTimeMillis() - t0;
        log.info("=== ProductionDataSeeder DONE in {} ms ===", dt);
        printSummary();
    }

    @FunctionalInterface
    private interface SeedFn { void run(Integer batch); }

    private void switchOrAll(String target, String name, Integer batch, SeedFn fn) {
        if ("all".equals(target) || name.equals(target)) {
            fn.run(batch);
        }
    }

    private void dropTargetCollection(String target) {
        String col = collectionNameForTarget(target);
        if (col != null) {
            try { mongo.dropCollection(col); log.info("  Dropped collection: {}", col); } catch (Exception e) { /* ignore */ }
        }
    }

    /** Trả về tên collection tương ứng với target seeder. */
    private String collectionNameForTarget(String target) {
        return switch (target) {
            case "categories"    -> "categories";
            case "users"         -> "users";
            case "shops"         -> "shops";
            case "products"      -> "products";
            case "vouchers"      -> "vouchers";
            case "banners"       -> "banners";
            case "legal"         -> "legal_documents";
            case "orders"        -> "orders";
            case "reviews"       -> "reviews";
            case "carts"         -> "carts";
            case "kyc"           -> "kyc_profiles";
            case "complaints"    -> "complaints";
            case "returns"       -> "returns";
            case "refunds"       -> "refunds";
            case "reportcases"  -> "report_cases";
            case "moderation"   -> "moderation_history";
            case "violations"   -> "violations";
            case "escalations"   -> "escalations";
            case "auditlogs"    -> "audit_logs";
            case "auditentries" -> "audit_logs";
            case "scheduler"    -> "scheduler_idempotency";
            default -> null;
        };
    }

    /** Drop collection nếu rỗng / rất ít để idempotent. KHÔNG drop khi đã có nhiều data. */
    private void wipeTargetIfEmpty(String target) {
        String col = collectionNameForTarget(target);
        if (col == null) return;
        try {
            long current = mongo.getCollection(col).countDocuments();
            if (current < BATCH_SIZE) {
                mongo.dropCollection(col);
                log.info("  Dropped collection '{}' (was {} records, < {} threshold) → re-seed.",
                    col, current, BATCH_SIZE);
            } else {
                log.info("  KEEP collection '{}' (has {} records >= {} threshold) → append only.",
                    col, current, BATCH_SIZE);
            }
        } catch (Exception e) { /* ignore */ }
    }

    private Integer parseBatch(List<String> values) {
        if (values == null || values.isEmpty()) return null;
        try { return Integer.parseInt(values.get(0)); } catch (NumberFormatException e) { return null; }
    }

    private Integer parseIntOrNull(String s) {
        if (s == null || s.isBlank()) return null;
        try { return Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return null; }
    }

    private String argVal(ApplicationArguments args, String name) {
        List<String> v = args.getOptionValues(name);
        return v == null || v.isEmpty() ? null : v.get(0);
    }

    private String firstNonBlank(String... vals) {
        for (String v : vals) if (v != null && !v.isBlank()) return v;
        return null;
    }

    private void wipeAll() {
        String[] collections = {
            "users","shops","products","categories","orders","reviews","carts","vouchers",
            "banners","kyc_profiles","complaints","returns","refunds","report_cases",
            "moderation_history","violations","escalations","legal_documents","audit_logs",
            "scheduler_idempotency"
        };
        for (String c : collections) {
            try { mongo.dropCollection(c); } catch (Exception ignored) { }
        }
        log.info("Wiped {} collections", collections.length);
    }

    private void printSummary() {
        String[] collections = {
            "users","shops","products","categories","orders","reviews","carts","vouchers",
            "banners","kyc_profiles","complaints","returns","refunds","report_cases",
            "moderation_history","violations","escalations","legal_documents","audit_logs",
            "scheduler_idempotency"
        };
        log.info("----- DB SUMMARY -----");
        for (String c : collections) {
            try {
                long n = mongo.getCollection(c).countDocuments();
                log.info("  {:>22} = {}", c, n);
            } catch (Exception e) {
                log.info("  {:>22} = (n/a)", c);
            }
        }
    }

    // ============================================================================
    // ============================== CATEGORIES =================================
    // ============================================================================
    // Target: 10. Single batch (chạy 1 lần là đủ).
    // CNJ70 là marketplace Đồ công nghệ — danh sách khớp với bộ lọc ở
    // trang web (template product-list, sidebar trái).

    private void seedCategoriesBatch(Integer batch) {
        if (batch != null && batch != 0) return;
        String[][] data = {
            {"PC & Máy tính bàn",     "fa-desktop"},
            {"Laptop",                "fa-laptop"},
            {"Điện thoại",            "fa-mobile-screen"},
            {"Máy tính bảng",         "fa-tablet-screen-button"},
            {"Linh kiện máy tính",    "fa-microchip"},
            {"Màn hình",              "fa-display"},
            {"Thiết bị âm thanh",     "fa-headphones"},
            {"Phụ kiện công nghệ",    "fa-plug"},
            {"Thiết bị mạng",         "fa-wifi"},
            {"Đồng hồ thông minh",    "fa-clock"}
        };
        Random rng = new Random(SEED);
        List<Category> list = new ArrayList<>();
        for (int i = 0; i < data.length; i++) {
            list.add(Category.builder()
                .id(uuid())
                .name(data[i][0])
                .description("Danh mục " + data[i][0] + " — sản phẩm đa dạng, chất lượng từ nhiều shop uy tín trên CNJ70.")
                .iconUrl(null)
                .parentId(null)
                .sortOrder(i)
                .active(true)
                .createdAt(randomDateTime(rng))
                .updatedAt(LocalDateTime.now())
                .build());
        }
        mongo.insertAll(list);
        log.info("[categories] +{} records (total = {})", list.size(), mongo.getCollection("categories").countDocuments());
    }

    // ============================================================================
    // ================================ USERS ====================================
    // ============================================================================
    // Target: 1500. Batches: 15. Phân bổ:
    //   8 admin, 22 moderator, 370 vendor, 1100 customer.
    // Mỗi user có email duy nhất: user%05d@cnj70.vn.

    private void seedUsersBatch(Integer batch) {
        int target = 1500;
        int from = batch == null ? 0 : batch * BATCH_SIZE;
        int to   = batch == null ? target : Math.min(from + BATCH_SIZE, target);
        if (from >= target) return;

        Random rng = new Random(SEED + from);
        List<User> list = new ArrayList<>(to - from);
        for (int i = from; i < to; i++) {
            UserRole role;
            if (i < 8)            role = UserRole.ADMIN;
            else if (i < 30)      role = UserRole.MODERATOR;
            else if (i < 400)     role = UserRole.VENDOR;
            else                  role = UserRole.CUSTOMER;

            boolean female = rng.nextBoolean();
            String ho  = HO[rng.nextInt(HO.length)];
            String ten = female ? TEN_NU[rng.nextInt(TEN_NU.length)] : TEN_NAM[rng.nextInt(TEN_NAM.length)];
            String fullName = ho + " " + ten + (i % 7 == 0 ? " " + (1 + rng.nextInt(99)) : "");
            String email = String.format("user%05d@cnj70.vn", i);
            String phone = phoneVN(rng);
            String address = vietnameseAddress(rng);

            AccountStatus accStatus;
            KycStatus kycStatus;
            if (role == UserRole.VENDOR) {
                double r = rng.nextDouble();
                accStatus = r < 0.05 ? AccountStatus.LOCKED : (r < 0.15 ? AccountStatus.UNVERIFIED : AccountStatus.ACTIVE);
                double k = rng.nextDouble();
                if (k < 0.70)      kycStatus = KycStatus.APPROVED;
                else if (k < 0.85) kycStatus = KycStatus.PENDING_ADMIN;
                else if (k < 0.92) kycStatus = KycStatus.PENDING_THIRD_PARTY;
                else if (k < 0.95) kycStatus = KycStatus.NOT_SUBMITTED;
                else if (k < 0.98) kycStatus = KycStatus.ADMIN_REJECTED;
                else               kycStatus = KycStatus.SUSPENDED;
            } else if (role == UserRole.MODERATOR || role == UserRole.ADMIN) {
                accStatus = AccountStatus.ACTIVE;
                kycStatus = KycStatus.NOT_SUBMITTED;
            } else {
                accStatus = rng.nextDouble() < 0.02 ? AccountStatus.LOCKED : AccountStatus.ACTIVE;
                kycStatus = KycStatus.NOT_SUBMITTED;
            }

            LocalDateTime created = randomDateTime(rng);

            list.add(User.builder()
                .id(uuid())
                .email(email)
                .password("$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy") // BCrypt placeholder ("password")
                .fullName(fullName)
                .phone(phone)
                .address(address)
                .role(role)
                .status(accStatus)
                .avatarUrl(null)
                .encryptedCitizenId(null)
                .encryptedTaxCode(null)
                .encryptedBankAccount(null)
                .acceptedTermsAt(created.plusDays(rng.nextInt(3)))
                .acceptedTermsVersion(1)
                .acceptedPrivacyAt(created.plusDays(rng.nextInt(3)))
                .acceptedPrivacyVersion(1)
                .marketingOptIn(rng.nextDouble() < 0.40)
                .kycStatus(kycStatus)
                .createdAt(created)
                .updatedAt(created.plusDays(rng.nextInt(365)))
                .build());
        }
        mongo.insertAll(list);
        log.info("[users] batch={}, +{} records (total = {})", batch, list.size(), mongo.getCollection("users").countDocuments());
    }

    // ============================================================================
    // ================================ SHOPS ====================================
    // ============================================================================
    // Target: 300. Batches: 3. Mỗi shop gắn với 1 VENDOR.

    private void seedShopsBatch(Integer batch) {
        int target = 300;
        int from = batch == null ? 0 : batch * BATCH_SIZE;
        int to   = batch == null ? target : Math.min(from + BATCH_SIZE, target);
        if (from >= target) return;

        // Lấy các user VENDOR có kycStatus=APPROVED (để gắn shop)
        List<User> vendors = mongo.find(
            org.springframework.data.mongodb.core.query.Query.query(
                org.springframework.data.mongodb.core.query.Criteria.where("role").is(UserRole.VENDOR.name())
                    .and("kycStatus").is(KycStatus.APPROVED.name())
            ), User.class);

        if (vendors.isEmpty()) {
            log.warn("[shops] No VENDOR/APPROVED users found, run users seed first.");
            return;
        }

        Random rng = new Random(SEED + 7 + from);
        // CNJ70 chỉ là marketplace Đồ công nghệ → mọi shop đều thuộc nhóm "Tech".
        String[] shopTypes = { "Tech" };
        List<Shop> list = new ArrayList<>(to - from);
        Set<String> usedNames = new HashSet<>();
        for (int i = from; i < to; i++) {
            User vendor = vendors.get(i % vendors.size());
            String type = shopTypes[rng.nextInt(shopTypes.length)];
            String name;
            int suffix = 1;
            do {
                name = String.format("%s %s %s", pick(rng, HO), type, (i + suffix));
                suffix++;
            } while (usedNames.contains(name));
            usedNames.add(name);

            ShopStatus shopStatus;
            double r = rng.nextDouble();
            if (r < 0.75) shopStatus = ShopStatus.APPROVED;
            else if (r < 0.85) shopStatus = ShopStatus.PENDING;
            else if (r < 0.92) shopStatus = ShopStatus.RESTRICTED;
            else if (r < 0.97) shopStatus = ShopStatus.SUSPENDED;
            else shopStatus = ShopStatus.REJECTED;

            LocalDateTime created = randomDateTime(rng);

            list.add(Shop.builder()
                .id(uuid())
                .ownerId(vendor.getId())
                .shopName(name)
                .description(String.format("Chuyên %s chính hãng, giá tốt, giao hàng nhanh toàn quốc.", type))
                .logoUrl(null)
                .bannerUrl(null)
                .status(shopStatus)
                .active(shopStatus == ShopStatus.APPROVED)
                .kycStatus(KycStatus.APPROVED)
                .kycReferenceId("kycref-" + (1000 + i))
                .kycApprovedAt(created.plusDays(2 + rng.nextInt(10)))
                .kycSubmittedAt(created.plusDays(rng.nextInt(2)))
                .kycDocumentVersion(1)
                .rejectionReason(shopStatus == ShopStatus.REJECTED ? "Thiếu giấy tờ hợp lệ" : null)
                .deactivationReason(shopStatus == ShopStatus.SUSPENDED ? "Vi phạm chính sách" : null)
                .actionBy(null)
                .actionAt(null)
                .createdAt(created)
                .updatedAt(created.plusDays(rng.nextInt(180)))
                .build());

            // Set shopId ngược lại cho vendor
            vendor.setShopId(list.get(list.size() - 1).getId());
        }
        mongo.insertAll(list);
        // Update vendor users với shopId
        for (User u : vendors) {
            if (u.getShopId() != null) mongo.save(u);
        }
        log.info("[shops] batch={}, +{} records (total = {})", batch, list.size(), mongo.getCollection("shops").countDocuments());
    }

    // ============================================================================
    // =============================== PRODUCTS ==================================
    // ============================================================================
    // Target: 2000. Batches: 20.

    private void seedProductsBatch(Integer batch) {
        int target = 2000;
        int from = batch == null ? 0 : batch * BATCH_SIZE;
        int to   = batch == null ? target : Math.min(from + BATCH_SIZE, target);
        if (from >= target) return;

        List<Shop> shops = mongo.findAll(Shop.class);
        List<Category> categories = mongo.findAll(Category.class);
        if (shops.isEmpty()) {
            log.warn("[products] No shops found, run shops seed first.");
            return;
        }
        if (categories.isEmpty()) {
            log.warn("[products] No categories found, run categories seed first.");
            return;
        }

        // CNJ70 chỉ bán Đồ công nghệ → lọc category thuộc whitelist TECH_CATEGORY_NAMES
        // trước khi random. Nếu whitelist rỗng (lỗi cấu hình) → fallback toàn bộ categories.
        Set<String> techSet = new HashSet<>(TECH_CATEGORY_NAMES);
        List<Category> techCats = categories.stream()
            .filter(c -> c.getName() != null && techSet.contains(c.getName()))
            .collect(Collectors.toList());
        List<Category> pool = techCats.isEmpty() ? categories : techCats;
        log.info("[products] Using {}/{} categories (Tech-only whitelist).", pool.size(), categories.size());

        Random rng = new Random(SEED + 11 + from);
        List<Product> list = new ArrayList<>(to - from);
        for (int i = from; i < to; i++) {
            Shop shop = shops.get(i % shops.size());
            Category cat = pool.get(rng.nextInt(pool.size()));
            LocalDateTime created = randomDateTime(rng);

            ProductStatus ps;
            double r = rng.nextDouble();
            if (r < 0.78)      ps = ProductStatus.ACTIVE;
            else if (r < 0.90) ps = ProductStatus.OUT_OF_STOCK;
            else if (r < 0.97) ps = ProductStatus.DRAFT;
            else               ps = ProductStatus.HIDDEN;

            ModerationStatus ms;
            double r2 = rng.nextDouble();
            if (r2 < 0.70)      ms = ModerationStatus.APPROVED;
            else if (r2 < 0.82) ms = ModerationStatus.AUTO_PASSED;
            else if (r2 < 0.90) ms = ModerationStatus.PENDING_MANUAL;
            else if (r2 < 0.95) ms = ModerationStatus.AUTO_REJECTED;
            else if (r2 < 0.98) ms = ModerationStatus.REJECTED;
            else                ms = ModerationStatus.ESCALATED;

            long priceVnd = 50000L + (long) (rng.nextDouble() * 29_950_000L); // 50k–30tr

            int reviewCount = rng.nextInt(200);
            double rating = reviewCount > 0 ? Math.round((3.0 + rng.nextDouble() * 2.0) * 10.0) / 10.0 : 0.0;

            list.add(Product.builder()
                .id(uuid())
                .shopId(shop.getId())
                .shopName(shop.getShopName())
                .name(productName(rng))
                .brand(pick(rng, BRANDS))
                .warrantyMonths(rng.nextInt(24) + 1)
                .manufacturer(pick(rng, BRANDS) + " Vietnam")
                .manufacturerAddress(vietnameseAddress(rng))
                .description("Sản phẩm chính hãng, bảo hành đầy đủ. Giao hàng nhanh toàn quốc. Đổi trả trong 7 ngày.")
                .richDescription("<p>Thông tin chi tiết sản phẩm:</p><ul><li>Thương hiệu uy tín</li><li>Chất lượng cao</li><li>Giá cả hợp lý</li></ul>")
                .price(BigDecimal.valueOf(priceVnd).setScale(0, RoundingMode.HALF_UP))
                .stock(ps == ProductStatus.OUT_OF_STOCK ? 0 : 1 + rng.nextInt(500))
                .categoryId(cat.getId())
                .categoryName(cat.getName())
                .imageUrls(List.of(
                    "https://picsum.photos/seed/prod" + i + "/600/600",
                    "https://picsum.photos/seed/prod" + i + "b/600/600"))
                .thumbnailUrl("https://picsum.photos/seed/prod" + i + "/200/200")
                .specifications(List.of(
                    ProductSpecification.builder().name("Xuất xứ").value("Việt Nam").unit(null).build(),
                    ProductSpecification.builder().name("Bảo hành").value(String.valueOf(12)).unit("tháng").build()))
                .variants(List.of(
                    ProductVariant.builder()
                        .specifications(List.of(ProductSpecification.builder().name("Màu").value("Đen").build()))
                        .price(BigDecimal.valueOf(priceVnd).setScale(0, RoundingMode.HALF_UP))
                        .stock(1 + rng.nextInt(50))
                        .sku("SKU-" + i + "-BLK")
                        .build()))
                .status(ps)
                .moderationStatus(ms)
                .moderationActorId(ms == ModerationStatus.APPROVED || ms == ModerationStatus.REJECTED ? null : null)
                .moderationReason(ms == ModerationStatus.REJECTED ? "Sai danh mục" : null)
                .moderationAt(ms == ModerationStatus.APPROVED || ms == ModerationStatus.REJECTED ? created.plusDays(1) : null)
                .rating(rating)
                .reviewCount(reviewCount)
                .sold(rng.nextInt(800))
                .createdAt(created)
                .updatedAt(created.plusDays(rng.nextInt(180)))
                .build());
        }
        mongo.insertAll(list);
        log.info("[products] batch={}, +{} records (total = {})", batch, list.size(), mongo.getCollection("products").countDocuments());
    }

    // ============================================================================
    // ================================ VOUCHERS ==================================
    // ============================================================================
    // Target: 200. Batches: 2.

    private void seedVouchersBatch(Integer batch) {
        int target = 200;
        int from = batch == null ? 0 : batch * BATCH_SIZE;
        int to   = batch == null ? target : Math.min(from + BATCH_SIZE, target);
        if (from >= target) return;

        List<Shop> shops = mongo.findAll(Shop.class);
        Random rng = new Random(SEED + 19 + from);
        List<Voucher> list = new ArrayList<>(to - from);
        Set<String> usedCodes = new HashSet<>();
        for (int i = from; i < to; i++) {
            Shop shop = shops.isEmpty() ? null : shops.get(i % shops.size());
            VoucherType vt = (i % 3 == 0 && shop != null) ? VoucherType.SHOP : VoucherType.WEB;
            DiscountType dt = rng.nextDouble() < 0.6 ? DiscountType.PERCENT : DiscountType.AMOUNT;
            BigDecimal value = dt == DiscountType.PERCENT
                ? BigDecimal.valueOf(5 + rng.nextInt(46)).setScale(0, RoundingMode.HALF_UP)
                : randomVnd(rng, 10000, 200000);
            BigDecimal max = dt == DiscountType.PERCENT
                ? randomVnd(rng, 50000, 500000)
                : null;
            BigDecimal min = randomVnd(rng, 100000, 1000000);
            int quantity = 10 + rng.nextInt(990);
            int used = rng.nextInt(quantity + 1);

            LocalDateTime start = randomDateTime(rng);
            LocalDateTime end = start.plusDays(7 + rng.nextInt(120));
            if (end.isAfter(T_END)) end = T_END;

            String code;
            do {
                code = String.format("VC%05d", i);
            } while (usedCodes.contains(code));
            usedCodes.add(code);

            list.add(Voucher.builder()
                .id(uuid())
                .code(code)
                .name(String.format("Voucher %s giảm %s", vt == VoucherType.SHOP ? "shop" : "sàn",
                    dt == DiscountType.PERCENT ? value + "%" : value.toPlainString() + "đ"))
                .type(vt)
                .shopId(vt == VoucherType.SHOP && shop != null ? shop.getId() : null)
                .shopName(vt == VoucherType.SHOP && shop != null ? shop.getShopName() : null)
                .productIds(new ArrayList<>())
                .discountType(dt)
                .discountValue(value)
                .maxDiscountAmount(max)
                .minOrderValue(min)
                .quantity(quantity)
                .used(used)
                .startDate(start)
                .endDate(end)
                .active(true)
                .createdBy(shop != null ? shop.getOwnerId() : "system")
                .createdAt(start.minusDays(rng.nextInt(5)))
                .updatedAt(start.plusDays(rng.nextInt(30)))
                .build());
        }
        mongo.insertAll(list);
        log.info("[vouchers] batch={}, +{} records (total = {})", batch, list.size(), mongo.getCollection("vouchers").countDocuments());
    }

    // ============================================================================
    // ================================ BANNERS ===================================
    // ============================================================================
    // Target: 100. Single batch.

    private void seedBannersBatch(Integer batch) {
        if (batch != null && batch != 0) return;

        Random rng = new Random(SEED + 23);
        String[] themes = {"primary","teal","purple","orange","red"};
        String[] positions = {"HOME_HERO","HOME_PROMO","CATEGORY_TOP","CHECKOUT_BANNER","FLASH_SALE"};
        List<Banner> list = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            String status = (i < 80) ? BannerStatus.PUBLISHED : BannerStatus.UNPUBLISHED;
            LocalDateTime created = randomDateTime(rng);
            list.add(Banner.builder()
                .id(uuid())
                .title(String.format("Khuyến mãi %s #%02d", pick(rng, new String[]{"cực sốc","siêu hot","giá rẻ","cuối năm","tựu trường","Tết","Black Friday","11.11"}), i + 1))
                .description("Giảm giá đến 50% cho hàng ngàn sản phẩm trên toàn sàn CNJ70.")
                .tag("HOT")
                .tagIcon("fa-fire")
                .imageUrl("https://picsum.photos/seed/banner" + i + "/1200/400")
                .link("/search?q=khuyen-mai")
                .ctaText("Mua ngay")
                .ctaIcon("fa-arrow-right")
                .theme(themes[i % themes.length])
                .status(status)
                .position(positions[i % positions.length])
                .sortOrder(i)
                .createdAt(created)
                .updatedAt(created.plusDays(rng.nextInt(30)))
                .build());
        }
        mongo.insertAll(list);
        log.info("[banners] +{} records (total = {})", list.size(), mongo.getCollection("banners").countDocuments());
    }

    // ============================================================================
    // ============================ LEGAL DOCUMENTS ==============================
    // ============================================================================
    // Target: 100. Single batch (mỗi loại có nhiều versions qua 5 năm).

    private void seedLegalDocumentsBatch(Integer batch) {
        if (batch != null && batch != 0) return;

        Random rng = new Random(SEED + 31);
        LegalDocumentType[] types = LegalDocumentType.values();
        List<LegalDocument> list = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            LegalDocumentType t = types[i % types.length];
            int version = 1 + (i / types.length);
            LocalDateTime eff = LocalDateTime.of(2021, 9, 1, 0, 0).plusMonths(i);
            list.add(LegalDocument.builder()
                .id(uuid())
                .type(t)
                .title(String.format("%s — Phiên bản %d", labelFor(t), version))
                .content("<h1>" + labelFor(t) + "</h1><p>Điều khoản phiên bản " + version
                    + " được ban hành ngày " + eff.toLocalDate()
                    + " bởi CNJ70 Ecommerce. Vui lòng đọc kỹ trước khi sử dụng dịch vụ.</p>")
                .version(version)
                .effectiveDate(eff)
                .updatedBy("admin@cnj70.vn")
                .renderedContent("<h1>" + labelFor(t) + "</h1>")
                .metaDescription(labelFor(t) + " của CNJ70 Ecommerce")
                .createdAt(eff.minusDays(7))
                .updatedAt(eff)
                .build());
        }
        mongo.insertAll(list);
        log.info("[legal_documents] +{} records (total = {})", list.size(), mongo.getCollection("legal_documents").countDocuments());
    }

    private String labelFor(LegalDocumentType t) {
        return switch (t) {
            case TERMS -> "Điều khoản sử dụng";
            case PRIVACY -> "Chính sách bảo mật";
            case RETURN -> "Chính sách đổi trả";
            case SHIPPING -> "Chính sách vận chuyển";
            case WARRANTY -> "Chính sách bảo hành";
            case COMPLAINT -> "Quy trình khiếu nại";
            case PAYMENT -> "Điều khoản thanh toán";
            case SITEMAP -> "Sơ đồ trang";
        };
    }

    // ============================================================================
    // ================================ ORDERS ====================================
    // ============================================================================
    // Target: 5000. Batches: 50.

    private void seedOrdersBatch(Integer batch) {
        int target = 5000;
        int from = batch == null ? 0 : batch * BATCH_SIZE;
        int to   = batch == null ? target : Math.min(from + BATCH_SIZE, target);
        if (from >= target) return;

        List<User> customers = mongo.find(
            org.springframework.data.mongodb.core.query.Query.query(
                org.springframework.data.mongodb.core.query.Criteria.where("role").is(UserRole.CUSTOMER.name())
            ), User.class);
        List<Product> products = mongo.findAll(Product.class);
        List<Voucher> vouchers = mongo.findAll(Voucher.class);
        if (customers.isEmpty() || products.isEmpty()) {
            log.warn("[orders] customers={}, products={} — run prerequisites first", customers.size(), products.size());
            return;
        }

        Random rng = new Random(SEED + 41 + from);
        List<Order> list = new ArrayList<>(to - from);
        for (int i = from; i < to; i++) {
            User customer = customers.get(rng.nextInt(customers.size()));
            int itemCount = 1 + rng.nextInt(3);
            List<OrderItem> items = new ArrayList<>();
            BigDecimal subtotal = BigDecimal.ZERO;
            Map<String, SubOrderShipping> shippingByShop = new HashMap<>();
            Set<String> shopIdsInOrder = new HashSet<>();

            for (int k = 0; k < itemCount; k++) {
                Product p = products.get(rng.nextInt(products.size()));
                int qty = 1 + rng.nextInt(3);
                BigDecimal price = p.getPrice();
                BigDecimal itemSub = price.multiply(BigDecimal.valueOf(qty));
                items.add(OrderItem.builder()
                    .shopId(p.getShopId())
                    .shopName(p.getShopName())
                    .productId(p.getId())
                    .productName(p.getName())
                    .imageUrl(p.getThumbnailUrl())
                    .price(price)
                    .quantity(qty)
                    .subtotal(itemSub)
                    .build());
                subtotal = subtotal.add(itemSub);
                shopIdsInOrder.add(p.getShopId());
            }

            // Voucher (10% có dùng)
            BigDecimal discount = BigDecimal.ZERO;
            String voucherId = null;
            String voucherCode = null;
            String voucherName = null;
            if (rng.nextDouble() < 0.30 && !vouchers.isEmpty()) {
                Voucher v = vouchers.get(rng.nextInt(vouchers.size()));
                if (v.getDiscountType() == DiscountType.PERCENT) {
                    discount = subtotal.multiply(v.getDiscountValue()).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
                    if (v.getMaxDiscountAmount() != null && discount.compareTo(v.getMaxDiscountAmount()) > 0) {
                        discount = v.getMaxDiscountAmount();
                    }
                } else {
                    discount = v.getDiscountValue();
                }
                if (discount.compareTo(subtotal) > 0) discount = subtotal;
                voucherId = v.getId();
                voucherCode = v.getCode();
                voucherName = v.getName();
            }

            BigDecimal shippingFee = BigDecimal.valueOf(15000L + rng.nextInt(5) * 5000L);
            BigDecimal total = subtotal.subtract(discount).add(shippingFee);

            OrderStatus os;
            double r = rng.nextDouble();
            if (r < 0.70)       os = OrderStatus.DELIVERED;
            else if (r < 0.80)  os = OrderStatus.SHIPPING;
            else if (r < 0.88)  os = OrderStatus.PREPARING;
            else if (r < 0.96)  os = OrderStatus.PENDING;
            else                os = OrderStatus.CANCELLED;

            PaymentMethod pm = pick(rng, new PaymentMethod[]{
                PaymentMethod.COD, PaymentMethod.VNPAY, PaymentMethod.VNPAY, PaymentMethod.BANK_QR
            });
            PaymentStatus ps = (os == OrderStatus.CANCELLED && rng.nextDouble() < 0.5)
                ? PaymentStatus.FAILED
                : (os == OrderStatus.PENDING ? PaymentStatus.PENDING : PaymentStatus.PAID);

            LocalDateTime created = randomDateTime(rng);
            LocalDateTime paidAt = (ps == PaymentStatus.PAID) ? created.plusHours(rng.nextInt(2)) : null;
            LocalDateTime deliveredAt = (os == OrderStatus.DELIVERED) ? created.plusDays(2 + rng.nextInt(7)) : null;
            LocalDateTime refundedAt = (os == OrderStatus.CANCELLED && ps == PaymentStatus.PAID && rng.nextDouble() < 0.5)
                ? created.plusDays(7 + rng.nextInt(7)) : null;
            if (refundedAt != null) ps = PaymentStatus.REFUNDED;

            // Shipping per shop
            for (String sid : shopIdsInOrder) {
                ShippingStatus ss;
                double r2 = rng.nextDouble();
                if (os == OrderStatus.DELIVERED) ss = ShippingStatus.DELIVERED;
                else if (os == OrderStatus.SHIPPING) ss = rng.nextDouble() < 0.5 ? ShippingStatus.IN_TRANSIT : ShippingStatus.PICKED_UP;
                else if (os == OrderStatus.PREPARING) ss = ShippingStatus.PACKED;
                else if (os == OrderStatus.CANCELLED) ss = ShippingStatus.FAILED;
                else ss = ShippingStatus.PENDING;
                String shopName = items.stream().filter(it -> sid.equals(it.getShopId())).findFirst().map(OrderItem::getShopName).orElse("");
                shippingByShop.put(sid, SubOrderShipping.builder()
                    .shopId(sid)
                    .shopName(shopName)
                    .status(ss)
                    .shippingFee(shippingFee.divide(BigDecimal.valueOf(Math.max(1, shopIdsInOrder.size())), 0, RoundingMode.HALF_UP))
                    .trackingNumber(os == OrderStatus.DELIVERED || os == OrderStatus.SHIPPING
                        ? "TRK" + (1000000 + i * 10 + shippingByShop.size()) : null)
                    .carrier(pick(rng, new String[]{"GHTK","GHN","Viettel Post","J&T Express","Best Express"}))
                    .shippedAt(ss == ShippingStatus.PENDING || ss == ShippingStatus.PACKED ? null : created.plusDays(1))
                    .deliveredAt(ss == ShippingStatus.DELIVERED ? deliveredAt : null)
                    .updatedAt(created.plusDays(1 + rng.nextInt(5)))
                    .build());
            }

            list.add(Order.builder()
                .id(uuid())
                .userId(customer.getId())
                .userName(customer.getFullName())
                .userEmail(customer.getEmail())
                .userPhone(customer.getPhone())
                .shippingAddress(customer.getAddress())
                .items(items)
                .subtotal(subtotal)
                .shippingFee(shippingFee)
                .totalAmount(total)
                .discount(discount)
                .voucherId(voucherId)
                .voucherCode(voucherCode)
                .voucherName(voucherName)
                .status(os)
                .paymentMethod(pm)
                .paymentStatus(ps)
                .paid(ps == PaymentStatus.PAID || ps == PaymentStatus.REFUNDED)
                .paidAt(paidAt)
                .refundedAt(refundedAt)
                .deliveredAt(deliveredAt)
                .shippingByShop(shippingByShop)
                .createdAt(created)
                .updatedAt(created.plusDays(rng.nextInt(10)))
                .build());
        }
        mongo.insertAll(list);
        log.info("[orders] batch={}, +{} records (total = {})", batch, list.size(), mongo.getCollection("orders").countDocuments());
    }

    // ============================================================================
    // ================================ REVIEWS ===================================
    // ============================================================================
    // Target: 3000. Batches: 30. Mỗi review gắn 1 customer + 1 product, ưu tiên
    // chọn product mà customer đã mua (orders).

    private void seedReviewsBatch(Integer batch) {
        int target = 3000;
        int from = batch == null ? 0 : batch * BATCH_SIZE;
        int to   = batch == null ? target : Math.min(from + BATCH_SIZE, target);
        if (from >= target) return;

        List<User> customers = mongo.find(
            org.springframework.data.mongodb.core.query.Query.query(
                org.springframework.data.mongodb.core.query.Criteria.where("role").is(UserRole.CUSTOMER.name())
            ), User.class);
        List<Product> products = mongo.findAll(Product.class);
        if (customers.isEmpty() || products.isEmpty()) {
            log.warn("[reviews] Missing customers/products");
            return;
        }

        Random rng = new Random(SEED + 47 + from);
        List<Review> list = new ArrayList<>(to - from);
        String[] comments = {
            "Sản phẩm tốt, đóng gói cẩn thận, giao hàng nhanh.",
            "Chất lượng ổn trong tầm giá. Sẽ ủng hộ shop lần sau.",
            "Đúng mô tả, dùng ổn. Shop tư vấn nhiệt tình.",
            "Hơi nhỏ so với mong đợi nhưng OK.",
            "Tuyệt vời! Recommend cho mọi người.",
            "Giao hàng hơi chậm nhưng hàng ok.",
            "Sản phẩm không giống hình. Hơi thất vọng.",
            "Đóng gói kỹ, hàng đẹp, sẽ mua lại.",
            "Shop phục vụ tốt, hỏi gì trả lời nấy.",
            "Giá hơi cao nhưng xứng đáng."
        };
        for (int i = from; i < to; i++) {
            User customer = customers.get(rng.nextInt(customers.size()));
            Product p = products.get(rng.nextInt(products.size()));
            int rating = 3 + rng.nextInt(3); // 3-5 chủ yếu; đôi khi 1-2
            if (rng.nextDouble() < 0.10) rating = 1 + rng.nextInt(2);

            ReviewModerationStatus rms;
            double r = rng.nextDouble();
            if (r < 0.85)      rms = ReviewModerationStatus.VISIBLE;
            else if (r < 0.93) rms = ReviewModerationStatus.REPORTED;
            else if (r < 0.98) rms = ReviewModerationStatus.HIDDEN;
            else               rms = ReviewModerationStatus.DELETED;

            ModerationStatus ms;
            double r2 = rng.nextDouble();
            if (r2 < 0.85)      ms = ModerationStatus.APPROVED;
            else if (r2 < 0.95) ms = ModerationStatus.AUTO_PASSED;
            else                ms = ModerationStatus.PENDING_MANUAL;

            LocalDateTime created = randomDateTime(rng);

            list.add(Review.builder()
                .id(uuid())
                .productId(p.getId())
                .userId(customer.getId())
                .userName(customer.getFullName())
                .userAvatar(customer.getAvatarUrl())
                .rating(rating)
                .comment(comments[rng.nextInt(comments.length)])
                .images(rng.nextDouble() < 0.2
                    ? List.of("https://picsum.photos/seed/rv" + i + "/300/300") : new ArrayList<>())
                .moderationStatus(rms)
                .moderationReason(rms == ReviewModerationStatus.HIDDEN || rms == ReviewModerationStatus.DELETED ? "Vi phạm ngôn từ" : null)
                .moderatedBy(rms == ReviewModerationStatus.HIDDEN ? "moderator@cnj70.vn" : null)
                .moderatedAt(rms == ReviewModerationStatus.HIDDEN ? created.plusDays(rng.nextInt(7)) : null)
                .reportCount(rng.nextInt(15))
                .hidden(rms == ReviewModerationStatus.HIDDEN || rms == ReviewModerationStatus.DELETED)
                .hiddenReason(rms == ReviewModerationStatus.HIDDEN ? "Ngôn từ không phù hợp" : null)
                .pipelineModerationStatus(ms)
                .createdAt(created)
                .updatedAt(created.plusDays(rng.nextInt(60)))
                .build());
        }
        mongo.insertAll(list);
        log.info("[reviews] batch={}, +{} records (total = {})", batch, list.size(), mongo.getCollection("reviews").countDocuments());
    }

    // ============================================================================
    // ================================= CARTS ===================================
    // ============================================================================
    // Target: 1500. Batches: 15. 1 cart / customer (giả lập giỏ hiện tại).

    private void seedCartsBatch(Integer batch) {
        int target = 1500;
        int from = batch == null ? 0 : batch * BATCH_SIZE;
        int to   = batch == null ? target : Math.min(from + BATCH_SIZE, target);
        if (from >= target) return;

        List<User> customers = mongo.find(
            org.springframework.data.mongodb.core.query.Query.query(
                org.springframework.data.mongodb.core.query.Criteria.where("role").is(UserRole.CUSTOMER.name())
            ), User.class);
        List<Product> products = mongo.findAll(Product.class);
        if (customers.isEmpty() || products.isEmpty()) return;

        // Tránh trùng userId unique index
        Set<String> alreadyHasCart = mongo.findAll(Cart.class).stream()
            .map(Cart::getUserId).collect(Collectors.toSet());

        Random rng = new Random(SEED + 53 + from);
        List<Cart> list = new ArrayList<>();
        int safety = 0;
        for (int i = from; i < to && safety++ < target * 2; i++) {
            User customer = customers.get(rng.nextInt(customers.size()));
            if (alreadyHasCart.contains(customer.getId())) continue;

            int itemCount = 1 + rng.nextInt(4);
            List<Cart.CartItem> items = new ArrayList<>();
            BigDecimal subtotal = BigDecimal.ZERO;
            for (int k = 0; k < itemCount; k++) {
                Product p = products.get(rng.nextInt(products.size()));
                int qty = 1 + rng.nextInt(3);
                BigDecimal price = p.getPrice();
                items.add(Cart.CartItem.builder()
                    .productId(p.getId())
                    .productName(p.getName())
                    .imageUrl(p.getThumbnailUrl())
                    .price(price)
                    .quantity(qty)
                    .subtotal(price.multiply(BigDecimal.valueOf(qty)))
                    .shopId(p.getShopId())
                    .shopName(p.getShopName())
                    .stock(p.getStock())
                    .build());
                subtotal = subtotal.add(price.multiply(BigDecimal.valueOf(qty)));
            }

            list.add(Cart.builder()
                .id(uuid())
                .userId(customer.getId())
                .items(items)
                .appliedVoucherCode(rng.nextDouble() < 0.10 ? "VC00001" : null)
                .updatedAt(randomDateTime(rng))
                .build());
            alreadyHasCart.add(customer.getId());
        }
        if (!list.isEmpty()) mongo.insertAll(list);
        log.info("[carts] batch={}, +{} records (total = {})", batch, list.size(), mongo.getCollection("carts").countDocuments());
    }

    // ============================================================================
    // ============================== KYC PROFILES ===============================
    // ============================================================================
    // Target: 300. Batches: 3. 1 KYC / vendor.

    private void seedKycProfilesBatch(Integer batch) {
        int target = 300;
        int from = batch == null ? 0 : batch * BATCH_SIZE;
        int to   = batch == null ? target : Math.min(from + BATCH_SIZE, target);
        if (from >= target) return;

        List<User> vendors = mongo.find(
            org.springframework.data.mongodb.core.query.Query.query(
                org.springframework.data.mongodb.core.query.Criteria.where("role").is(UserRole.VENDOR.name())
            ), User.class);
        if (vendors.isEmpty()) return;

        Set<String> already = mongo.findAll(KycProfile.class).stream()
            .map(KycProfile::getUserId).collect(Collectors.toSet());

        Random rng = new Random(SEED + 59 + from);
        List<KycProfile> list = new ArrayList<>();
        for (int i = from; i < to; i++) {
            User vendor = vendors.get(i % vendors.size());
            if (already.contains(vendor.getId())) continue;

            KycStatus st;
            double r = rng.nextDouble();
            if (r < 0.75)      st = KycStatus.APPROVED;
            else if (r < 0.88) st = KycStatus.PENDING_ADMIN;
            else if (r < 0.95) st = KycStatus.PENDING_THIRD_PARTY;
            else               st = KycStatus.ADMIN_REJECTED;

            LocalDateTime submitted = randomDateTime(rng);
            list.add(KycProfile.builder()
                .id(uuid())
                .userId(vendor.getId())
                .status(st)
                .ownerFullName(vendor.getFullName())
                .idNumber(String.format("0%02d%08d", 1 + rng.nextInt(99), rng.nextInt(100000000)))
                .idFrontImageUrl("https://picsum.photos/seed/idfront" + i + "/600/400")
                .idBackImageUrl("https://picsum.photos/seed/idback" + i + "/600/400")
                .businessName(vendor.getFullName() + " Trading")
                .taxCode(String.format("%02d%09d", 1 + rng.nextInt(99), rng.nextInt(100_000_000)))
                .businessLicenseUrl("https://picsum.photos/seed/bl" + i + "/600/400")
                .bankAccount(String.valueOf(100_000_000_000L + (long)(rng.nextDouble() * 9_000_000_000L)))
                .bankName(pick(rng, BANKS))
                .bankBranch(pick(rng, new String[]{"CN Hà Nội","CN TP.HCM","CN Đà Nẵng","CN Hải Phòng"}))
                .thirdPartyResult(st == KycStatus.APPROVED || st == KycStatus.PENDING_ADMIN ? "APPROVED" : null)
                .thirdPartyRejectionReason(st == KycStatus.ADMIN_REJECTED ? "Ảnh CCCD không rõ" : null)
                .thirdPartyReferenceId("kyc3p-" + (10000 + i))
                .thirdPartyVerifiedAt(st == KycStatus.APPROVED || st == KycStatus.PENDING_ADMIN ? submitted.plusHours(rng.nextInt(24)) : null)
                .adminReviewerId(st == KycStatus.APPROVED ? "admin@cnj70.vn" : null)
                .adminReviewerName(st == KycStatus.APPROVED ? "Admin CNJ70" : null)
                .adminNote(st == KycStatus.APPROVED ? "Đạt yêu cầu" : null)
                .adminReviewedAt(st == KycStatus.APPROVED ? submitted.plusDays(1) : null)
                .submittedAt(submitted)
                .lastResubmittedAt(rng.nextDouble() < 0.3 ? submitted.plusDays(rng.nextInt(30)) : null)
                .submitCount(1 + rng.nextInt(3))
                .createdAt(submitted)
                .updatedAt(submitted.plusDays(rng.nextInt(30)))
                .build());
        }
        if (!list.isEmpty()) mongo.insertAll(list);
        log.info("[kyc_profiles] batch={}, +{} records (total = {})", batch, list.size(), mongo.getCollection("kyc_profiles").countDocuments());
    }

    // ============================================================================
    // ============================== COMPLAINTS ==================================
    // ============================================================================
    // Target: 1500. Batches: 15.

    private void seedComplaintsBatch(Integer batch) {
        int target = 1500;
        int from = batch == null ? 0 : batch * BATCH_SIZE;
        int to   = batch == null ? target : Math.min(from + BATCH_SIZE, target);
        if (from >= target) return;

        List<User> customers = mongo.find(
            org.springframework.data.mongodb.core.query.Query.query(
                org.springframework.data.mongodb.core.query.Criteria.where("role").is(UserRole.CUSTOMER.name())
            ), User.class);
        List<Order> orders = mongo.findAll(Order.class);
        List<User> moderators = mongo.find(
            org.springframework.data.mongodb.core.query.Query.query(
                org.springframework.data.mongodb.core.query.Criteria.where("role").is(UserRole.MODERATOR.name())
            ), User.class);
        List<User> admins = mongo.find(
            org.springframework.data.mongodb.core.query.Query.query(
                org.springframework.data.mongodb.core.query.Criteria.where("role").is(UserRole.ADMIN.name())
            ), User.class);
        if (customers.isEmpty() || orders.isEmpty()) {
            log.warn("[complaints] Missing customers/orders");
            return;
        }

        Random rng = new Random(SEED + 61 + from);
        List<Complaint> list = new ArrayList<>(to - from);
        ComplaintReason[] reasons = ComplaintReason.values();
        for (int i = from; i < to; i++) {
            Order o = orders.get(rng.nextInt(orders.size()));
            CustomerIdHolder custHolder = new CustomerIdHolder();
            User c = customers.stream().filter(u -> u.getId().equals(o.getUserId())).findFirst().orElseGet(() -> {
                custHolder.fallback = true; return customers.get(rng.nextInt(customers.size()));
            });

            ComplaintReason reason = reasons[rng.nextInt(reasons.length)];
            ComplaintStatus st;
            double r = rng.nextDouble();
            if (r < 0.40)      st = ComplaintStatus.RESOLVED;
            else if (r < 0.55) st = ComplaintStatus.VENDOR_RESPONDED;
            else if (r < 0.70) st = ComplaintStatus.OPEN;
            else if (r < 0.80) st = ComplaintStatus.MODERATOR_REVIEW;
            else if (r < 0.88) st = ComplaintStatus.ADMIN_REVIEW;
            else if (r < 0.94) st = ComplaintStatus.ESCALATED;
            else               st = ComplaintStatus.CLOSED;

            ComplaintLevel lv;
            if (st == ComplaintStatus.ADMIN_REVIEW || st == ComplaintStatus.ESCALATED) lv = ComplaintLevel.LEVEL_2;
            else if (st == ComplaintStatus.MODERATOR_REVIEW) lv = ComplaintLevel.LEVEL_1;
            else lv = ComplaintLevel.LEVEL_0;

            LocalDateTime created = randomDateTime(rng);
            int replyCount = st.ordinal() >= ComplaintStatus.VENDOR_RESPONDED.ordinal() ? 1 + rng.nextInt(3) : 0;
            User assignedMod = moderators.isEmpty() ? null : moderators.get(rng.nextInt(moderators.size()));
            User assignedAdmin = admins.isEmpty() ? null : admins.get(rng.nextInt(admins.size()));

            list.add(Complaint.builder()
                .id(uuid())
                .customerId(o.getUserId())
                .customerName(o.getUserName())
                .customerEmail(o.getUserEmail())
                .shopId(o.getShopId() != null ? o.getShopId()
                    : (o.getItems().isEmpty() ? null : o.getItems().get(0).getShopId()))
                .shopName(o.getShopName() != null ? o.getShopName()
                    : (o.getItems().isEmpty() ? null : o.getItems().get(0).getShopName()))
                .vendorId(o.getItems().isEmpty() ? null : o.getItems().get(0).getShopId())
                .orderId(o.getId())
                .orderItemIds(o.getItems().stream().map(it -> uuid()).limit(2).collect(Collectors.toList()))
                .reason(reason)
                .description(reasonVi(reason))
                .evidence(new ArrayList<>())
                .status(st)
                .level(lv)
                .vendorResponse(replyCount > 0 ? "Shop đã liên hệ và xin lỗi khách. Đang xử lý đổi/trả." : null)
                .vendorRespondedAt(replyCount > 0 ? created.plusHours(rng.nextInt(48)) : null)
                .vendorRespondedByEmail(replyCount > 0 ? "vendor" + rng.nextInt(370) + "@cnj70.vn" : null)
                .vendorReplyCount(replyCount)
                .assignedModeratorId(lv.ordinal() >= 1 ? (assignedMod != null ? assignedMod.getId() : null) : null)
                .assignedModeratorEmail(lv.ordinal() >= 1 ? (assignedMod != null ? assignedMod.getEmail() : null) : null)
                .moderatorAssignedAt(lv.ordinal() >= 1 ? created.plusHours(rng.nextInt(48)) : null)
                .assignedAdminId(lv == ComplaintLevel.LEVEL_2 && assignedAdmin != null ? assignedAdmin.getId() : null)
                .assignedAdminEmail(lv == ComplaintLevel.LEVEL_2 && assignedAdmin != null ? assignedAdmin.getEmail() : null)
                .adminAssignedAt(lv == ComplaintLevel.LEVEL_2 ? created.plusHours(rng.nextInt(72)) : null)
                .decision(st == ComplaintStatus.RESOLVED ? "REFUND_FULL" : null)
                .decisionReason(st == ComplaintStatus.RESOLVED ? "Đồng ý hoàn tiền theo chính sách" : null)
                .resolvedAt(st == ComplaintStatus.RESOLVED || st == ComplaintStatus.CLOSED ? created.plusDays(2 + rng.nextInt(10)) : null)
                .resolvedByRole(st == ComplaintStatus.RESOLVED ? (lv == ComplaintLevel.LEVEL_2 ? "ADMIN" : "VENDOR") : null)
                .vendorResponseDeadline(created.plusDays(2))
                .moderatorResolutionDeadline(lv.ordinal() >= 1 ? created.plusDays(5) : null)
                .escalationCount(lv.ordinal())
                .createdAt(created)
                .updatedAt(created.plusDays(rng.nextInt(15)))
                .build());
        }
        mongo.insertAll(list);
        log.info("[complaints] batch={}, +{} records (total = {})", batch, list.size(), mongo.getCollection("complaints").countDocuments());
    }

    private String reasonVi(ComplaintReason r) {
        return switch (r) {
            case NON_DELIVERY -> "Đơn hàng chưa được giao sau 7 ngày làm việc.";
            case WRONG_PRODUCT -> "Sản phẩm nhận được khác với đơn đặt.";
            case MISSING_ITEM -> "Thiếu sản phẩm trong đơn hàng.";
            case DAMAGED_PRODUCT -> "Sản phẩm bị hư hỏng khi vận chuyển.";
            case NOT_AS_DESCRIBED -> "Sản phẩm không đúng mô tả.";
            case WARRANTY_ISSUE -> "Yêu cầu bảo hành sản phẩm.";
            case OTHER -> "Vấn đề khác cần hỗ trợ.";
        };
    }

    private static class CustomerIdHolder { boolean fallback; }

    // ============================================================================
    // =============================== RETURNS ====================================
    // ============================================================================
    // Target: 1000. Batches: 10.

    private void seedReturnsBatch(Integer batch) {
        int target = 1000;
        int from = batch == null ? 0 : batch * BATCH_SIZE;
        int to   = batch == null ? target : Math.min(from + BATCH_SIZE, target);
        if (from >= target) return;

        List<Order> orders = mongo.findAll(Order.class);
        List<Complaint> complaints = mongo.findAll(Complaint.class);
        if (orders.isEmpty()) return;

        Random rng = new Random(SEED + 71 + from);
        List<ReturnRequest> list = new ArrayList<>(to - from);
        String[] reasons = {"Sản phẩm lỗi","Không đúng size","Không đúng mô tả","Hàng hư hỏng","Đổi ý trong 7 ngày"};
        ReturnStatus[] statuses = ReturnStatus.values();
        for (int i = from; i < to; i++) {
            Order o = orders.get(rng.nextInt(orders.size()));
            ReturnStatus st = statuses[rng.nextInt(statuses.length)];
            LocalDateTime created = randomDateTime(rng);
            Complaint c = complaints.isEmpty() ? null : complaints.get(rng.nextInt(complaints.size()));
            list.add(ReturnRequest.builder()
                .id(uuid())
                .complaintId(c != null ? c.getId() : null)
                .orderId(o.getId())
                .orderItemIds(o.getItems().stream().map(it -> uuid()).limit(2).collect(Collectors.toList()))
                .customerId(o.getUserId())
                .customerName(o.getUserName())
                .shopId(o.getItems().isEmpty() ? null : o.getItems().get(0).getShopId())
                .shopName(o.getItems().isEmpty() ? null : o.getItems().get(0).getShopName())
                .vendorId(o.getItems().isEmpty() ? null : o.getItems().get(0).getShopId())
                .reason(reasons[rng.nextInt(reasons.length)])
                .evidence(new ArrayList<>())
                .status(st)
                .declaredAmount(o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO)
                .createdAt(created)
                .updatedAt(created.plusDays(rng.nextInt(15)))
                .build());
        }
        mongo.insertAll(list);
        log.info("[returns] batch={}, +{} records (total = {})", batch, list.size(), mongo.getCollection("returns").countDocuments());
    }

    // ============================================================================
    // =============================== REFUNDS ====================================
    // ============================================================================
    // Target: 1000. Batches: 10.

    private void seedRefundsBatch(Integer batch) {
        int target = 1000;
        int from = batch == null ? 0 : batch * BATCH_SIZE;
        int to   = batch == null ? target : Math.min(from + BATCH_SIZE, target);
        if (from >= target) return;

        List<Order> orders = mongo.findAll(Order.class);
        List<ReturnRequest> returns = mongo.findAll(ReturnRequest.class);
        if (orders.isEmpty()) return;

        Random rng = new Random(SEED + 73 + from);
        List<RefundRequest> list = new ArrayList<>(to - from);
        RefundStatus[] statuses = RefundStatus.values();
        for (int i = from; i < to; i++) {
            Order o = orders.get(rng.nextInt(orders.size()));
            RefundStatus st = statuses[rng.nextInt(statuses.length)];
            LocalDateTime created = randomDateTime(rng);
            ReturnRequest ret = returns.isEmpty() ? null : returns.get(rng.nextInt(returns.size()));
            BigDecimal declared = o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO;
            boolean settled = (st == RefundStatus.SUCCEEDED);
            list.add(RefundRequest.builder()
                .id(uuid())
                .complaintId(null)
                .returnId(ret != null && rng.nextDouble() < 0.5 ? ret.getId() : null)
                .orderId(o.getId())
                .customerId(o.getUserId())
                .shopId(o.getItems().isEmpty() ? null : o.getItems().get(0).getShopId())
                .vendorId(o.getItems().isEmpty() ? null : o.getItems().get(0).getShopId())
                .declaredAmount(declared)
                .settledAmount(settled ? declared : null)
                .provider(pick(rng, new String[]{"internal-ledger","stripe","momo"}))
                .providerTransactionId(settled ? "TX-" + (100000 + i) : null)
                .status(st)
                .providerMessage(st == RefundStatus.FAILED ? "Bank declined" : null)
                .requestedByEmail(o.getUserEmail())
                .requestedByRole("CUSTOMER")
                .settledAt(settled ? created.plusDays(1 + rng.nextInt(5)) : null)
                .createdAt(created)
                .updatedAt(created.plusDays(rng.nextInt(10)))
                .build());
        }
        mongo.insertAll(list);
        log.info("[refunds] batch={}, +{} records (total = {})", batch, list.size(), mongo.getCollection("refunds").countDocuments());
    }

    // ============================================================================
    // ============================== REPORT CASES ===============================
    // ============================================================================
    // Target: 1500. Batches: 15.

    private void seedReportCasesBatch(Integer batch) {
        int target = 1500;
        int from = batch == null ? 0 : batch * BATCH_SIZE;
        int to   = batch == null ? target : Math.min(from + BATCH_SIZE, target);
        if (from >= target) return;

        List<User> customers = mongo.find(
            org.springframework.data.mongodb.core.query.Query.query(
                org.springframework.data.mongodb.core.query.Criteria.where("role").is(UserRole.CUSTOMER.name())
            ), User.class);
        List<Product> products = mongo.findAll(Product.class);
        List<Review> reviews = mongo.findAll(Review.class);
        List<User> moderators = mongo.find(
            org.springframework.data.mongodb.core.query.Query.query(
                org.springframework.data.mongodb.core.query.Criteria.where("role").is(UserRole.MODERATOR.name())
            ), User.class);
        if (customers.isEmpty() || (products.isEmpty() && reviews.isEmpty())) return;

        Random rng = new Random(SEED + 79 + from);
        List<ReportCase> list = new ArrayList<>(to - from);
        ReportReason[] reasons = ReportReason.values();
        ReportCaseStatus[] statuses = ReportCaseStatus.values();
        for (int i = from; i < to; i++) {
            User reporter = customers.get(rng.nextInt(customers.size()));
            ReportCaseResourceType rt;
            String resourceId;
            String resourceName;
            if ((i % 3) != 0 && !reviews.isEmpty()) {
                rt = ReportCaseResourceType.REVIEW;
                Review r = reviews.get(rng.nextInt(reviews.size()));
                resourceId = r.getId();
                resourceName = r.getComment() != null && r.getComment().length() > 30
                    ? r.getComment().substring(0, 30) + "..." : r.getComment();
            } else {
                rt = ReportCaseResourceType.PRODUCT;
                Product p = products.get(rng.nextInt(products.size()));
                resourceId = p.getId();
                resourceName = p.getName();
            }
            ReportCaseStatus st = statuses[rng.nextInt(statuses.length)];
            ReportCaseDecision decision;
            if (st == ReportCaseStatus.RESOLVED || st == ReportCaseStatus.APPROVED) decision = ReportCaseDecision.APPROVE;
            else if (st == ReportCaseStatus.REJECTED) decision = ReportCaseDecision.REJECT;
            else if (st == ReportCaseStatus.ESCALATED) decision = ReportCaseDecision.ESCALATE;
            else decision = null;

            User mod = moderators.isEmpty() ? null : moderators.get(rng.nextInt(moderators.size()));
            EscalationSeverity sev = null;
            if (st == ReportCaseStatus.ESCALATED) {
                sev = EscalationSeverity.values()[rng.nextInt(EscalationSeverity.values().length)];
            }

            LocalDateTime created = randomDateTime(rng);

            list.add(ReportCase.builder()
                .id(uuid())
                .resourceType(rt)
                .resourceId(resourceId)
                .resourceName(resourceName)
                .targetType(rt == ReportCaseResourceType.REVIEW ? ReportTargetType.REVIEW : ReportTargetType.PRODUCT)
                .targetId(resourceId)
                .targetSnapshot(resourceName)
                .reporterId(reporter.getId())
                .reporterName(reporter.getFullName())
                .reporterEmail(reporter.getEmail())
                .autoModerationStatus("AUTO_PASSED")
                .source("USER")
                .reportReason(reasons[rng.nextInt(reasons.length)])
                .description("Báo cáo vi phạm chính sách sàn CNJ70.")
                .evidence(new ArrayList<>())
                .autoFlags(new ArrayList<>())
                .status(st)
                .assignedModeratorId(mod != null ? mod.getId() : null)
                .assignedModeratorEmail(mod != null ? mod.getEmail() : null)
                .decision(decision)
                .decisionReason(decision != null ? "Đã xử lý theo chính sách" : null)
                .priority(rng.nextInt(5))
                .createdAt(created)
                .assignedAt(mod != null ? created.plusHours(rng.nextInt(24)) : null)
                .resolvedAt(st == ReportCaseStatus.RESOLVED || st == ReportCaseStatus.REJECTED || st == ReportCaseStatus.APPROVED ? created.plusDays(1 + rng.nextInt(7)) : null)
                .escalationSeverity(sev)
                .escalationReason(sev != null ? "Vi phạm nghiêm trọng" : null)
                .escalationReviewed(sev != null)
                .decisionModeratorEmail(mod != null ? mod.getEmail() : null)
                .decisionModeratorId(mod != null ? mod.getId() : null)
                .decidedAt(st == ReportCaseStatus.RESOLVED || st == ReportCaseStatus.REJECTED || st == ReportCaseStatus.APPROVED ? created.plusDays(rng.nextInt(7)) : null)
                .updatedAt(created.plusDays(rng.nextInt(10)))
                .build());
        }
        mongo.insertAll(list);
        log.info("[report_cases] batch={}, +{} records (total = {})", batch, list.size(), mongo.getCollection("report_cases").countDocuments());
    }

    // ============================================================================
    // ========================== MODERATION HISTORY =============================
    // ============================================================================
    // Target: 2000. Batches: 20.

    private void seedModerationHistoryBatch(Integer batch) {
        int target = 2000;
        int from = batch == null ? 0 : batch * BATCH_SIZE;
        int to   = batch == null ? target : Math.min(from + BATCH_SIZE, target);
        if (from >= target) return;

        List<Product> products = mongo.findAll(Product.class);
        List<Review> reviews = mongo.findAll(Review.class);
        List<User> moderators = mongo.find(
            org.springframework.data.mongodb.core.query.Query.query(
                org.springframework.data.mongodb.core.query.Criteria.where("role").in(UserRole.MODERATOR.name(), UserRole.ADMIN.name())
            ), User.class);
        if (moderators.isEmpty() || (products.isEmpty() && reviews.isEmpty())) return;

        Random rng = new Random(SEED + 83 + from);
        List<ModerationHistory> list = new ArrayList<>(to - from);
        ModerationAction[] actions = ModerationAction.values();
        ModerationStatus[] statuses = ModerationStatus.values();
        for (int i = from; i < to; i++) {
            String resourceType;
            String resourceId;
            String resourceName;
            if ((i % 3) != 0 && !reviews.isEmpty()) {
                Review rv = reviews.get(rng.nextInt(reviews.size()));
                resourceType = "REVIEW";
                resourceId = rv.getId();
                resourceName = "Review " + (rv.getComment() != null ? rv.getComment().substring(0, Math.min(20, rv.getComment().length())) : "");
            } else {
                Product p = products.get(rng.nextInt(products.size()));
                resourceType = "PRODUCT";
                resourceId = p.getId();
                resourceName = p.getName();
            }
            ModerationAction action = actions[rng.nextInt(actions.length)];
            ModerationStatus after = switch (action) {
                case APPROVE -> ModerationStatus.APPROVED;
                case REJECT -> ModerationStatus.REJECTED;
                case ESCALATE -> ModerationStatus.ESCALATED;
                case HIDE -> ModerationStatus.REJECTED;
                case UNHIDE -> ModerationStatus.APPROVED;
                default -> ModerationStatus.APPROVED;
            };
            ModerationStatus before = statuses[rng.nextInt(statuses.length)];
            User mod = moderators.get(rng.nextInt(moderators.size()));
            LocalDateTime created = randomDateTime(rng);

            list.add(ModerationHistory.builder()
                .id(uuid())
                .resourceType(resourceType)
                .resourceId(resourceId)
                .resourceName(resourceName)
                .action(action)
                .beforeStatus(before)
                .afterStatus(after)
                .reason("Đánh giá theo chính sách moderation.")
                .moderatorId(mod.getId())
                .moderatorEmail(mod.getEmail())
                .moderatorRole(mod.getRole())
                .createdAt(created)
                .build());
        }
        mongo.insertAll(list);
        log.info("[moderation_history] batch={}, +{} records (total = {})", batch, list.size(), mongo.getCollection("moderation_history").countDocuments());
    }

    // ============================================================================
    // ============================== VIOLATIONS ==================================
    // ============================================================================
    // Target: 300. Batches: 3.

    private void seedViolationsBatch(Integer batch) {
        int target = 300;
        int from = batch == null ? 0 : batch * BATCH_SIZE;
        int to   = batch == null ? target : Math.min(from + BATCH_SIZE, target);
        if (from >= target) return;

        List<Shop> shops = mongo.findAll(Shop.class);
        List<Product> products = mongo.findAll(Product.class);
        if (shops.isEmpty()) return;

        Random rng = new Random(SEED + 89 + from);
        List<Violation> list = new ArrayList<>(to - from);
        ViolationType[] types = ViolationType.values();
        ViolationSeverity[] sevs = ViolationSeverity.values();
        for (int i = from; i < to; i++) {
            Shop s = shops.get(rng.nextInt(shops.size()));
            Product p = products.isEmpty() ? null : products.get(rng.nextInt(products.size()));
            LocalDateTime created = randomDateTime(rng);
            list.add(Violation.builder()
                .id(uuid())
                .shopId(s.getId())
                .productId(p != null ? p.getId() : null)
                .productName(p != null ? p.getName() : null)
                .orderId(null)
                .type(types[rng.nextInt(types.length)])
                .severity(sevs[rng.nextInt(sevs.length)])
                .reason("Vi phạm chính sách sàn CNJ70.")
                .adminNote("Đã xem xét và ghi nhận.")
                .createdBy("admin@cnj70.vn")
                .createdAt(created)
                .resolvedAt(rng.nextDouble() < 0.6 ? created.plusDays(rng.nextInt(15)) : null)
                .resolvedBy(rng.nextDouble() < 0.6 ? "admin@cnj70.vn" : null)
                .resolutionNote("Đã xử lý theo chính sách.")
                .build());
        }
        mongo.insertAll(list);
        log.info("[violations] batch={}, +{} records (total = {})", batch, list.size(), mongo.getCollection("violations").countDocuments());
    }

    // ============================================================================
    // ============================== ESCALATIONS =================================
    // ============================================================================
    // Target: 300. Batches: 3.

    private void seedEscalationsBatch(Integer batch) {
        int target = 300;
        int from = batch == null ? 0 : batch * BATCH_SIZE;
        int to   = batch == null ? target : Math.min(from + BATCH_SIZE, target);
        if (from >= target) return;

        List<ReportCase> reportCases = mongo.findAll(ReportCase.class);
        if (reportCases.isEmpty()) return;
        List<ReportCase> escalated = reportCases.stream()
            .filter(rc -> rc.getStatus() == ReportCaseStatus.ESCALATED).collect(Collectors.toList());
        if (escalated.isEmpty()) escalated = reportCases;

        List<User> admins = mongo.find(
            org.springframework.data.mongodb.core.query.Query.query(
                org.springframework.data.mongodb.core.query.Criteria.where("role").is(UserRole.ADMIN.name())
            ), User.class);
        List<User> moderators = mongo.find(
            org.springframework.data.mongodb.core.query.Query.query(
                org.springframework.data.mongodb.core.query.Criteria.where("role").is(UserRole.MODERATOR.name())
            ), User.class);
        if (admins.isEmpty() || moderators.isEmpty()) return;

        Random rng = new Random(SEED + 97 + from);
        List<Escalation> list = new ArrayList<>(to - from);
        ViolationAction[] actions = { ViolationAction.WARNING, ViolationAction.PRODUCT_HIDDEN, ViolationAction.SHOP_RESTRICTED };
        EscalationSeverity[] sevs = EscalationSeverity.values();
        for (int i = from; i < to; i++) {
            ReportCase rc = escalated.get(rng.nextInt(escalated.size()));
            User mod = moderators.get(rng.nextInt(moderators.size()));
            User admin = admins.get(rng.nextInt(admins.size()));
            LocalDateTime created = randomDateTime(rng);
            Escalation.Status st;
            double r = rng.nextDouble();
            if (r < 0.7) st = Escalation.Status.RESOLVED;
            else if (r < 0.9) st = Escalation.Status.IN_REVIEW;
            else st = Escalation.Status.PENDING;

            list.add(Escalation.builder()
                .id(uuid())
                .reportCaseId(rc.getId())
                .resourceType(rc.getResourceType())
                .resourceId(rc.getResourceId())
                .vendorId(rc.getShopId())
                .shopId(rc.getShopId())
                .reason(rc.getEscalationReason() != null ? rc.getEscalationReason() : "Vi phạm nghiêm trọng")
                .severity(sevs[rng.nextInt(sevs.length)])
                .moderatorId(mod.getId())
                .moderatorEmail(mod.getEmail())
                .moderatorRole("MODERATOR")
                .sourceStatus(rc.getStatus())
                .status(st)
                .assignedAdminId(st == Escalation.Status.IN_REVIEW || st == Escalation.Status.RESOLVED ? admin.getId() : null)
                .assignedAdminEmail(st == Escalation.Status.IN_REVIEW || st == Escalation.Status.RESOLVED ? admin.getEmail() : null)
                .decisionAdminId(st == Escalation.Status.RESOLVED ? admin.getId() : null)
                .decisionAdminEmail(st == Escalation.Status.RESOLVED ? admin.getEmail() : null)
                .decisionAction(st == Escalation.Status.RESOLVED ? actions[rng.nextInt(actions.length)] : null)
                .decisionNote(st == Escalation.Status.RESOLVED ? "Đã xử lý theo policy" : null)
                .resolvedAt(st == Escalation.Status.RESOLVED ? created.plusDays(1 + rng.nextInt(5)) : null)
                .createdAt(created)
                .updatedAt(created.plusDays(rng.nextInt(10)))
                .build());
        }
        mongo.insertAll(list);
        log.info("[escalations] batch={}, +{} records (total = {})", batch, list.size(), mongo.getCollection("escalations").countDocuments());
    }

    // ============================================================================
    // =============================== AUDIT LOGS =================================
    // ============================================================================
    // Target: 5000. Batches: 50.

    private void seedAuditLogsBatch(Integer batch) {
        int target = 5000;
        int from = batch == null ? 0 : batch * BATCH_SIZE;
        int to   = batch == null ? target : Math.min(from + BATCH_SIZE, target);
        if (from >= target) return;

        List<User> actors = mongo.findAll(User.class);
        if (actors.isEmpty()) return;

        Random rng = new Random(SEED + 101 + from);
        List<AuditLog> list = new ArrayList<>(to - from);
        AuditAction[] actions = AuditAction.values();
        AuditSeverity[] sevs = AuditSeverity.values();
        String[] resources = {"USER","SHOP","PRODUCT","REVIEW","COMPLAINT","REPORT_CASE","VIOLATION","ESCALATION","KYC","LEGAL"};
        for (int i = from; i < to; i++) {
            User actor = actors.get(rng.nextInt(actors.size()));
            AuditAction action = actions[rng.nextInt(actions.length)];
            String resourceType = resources[rng.nextInt(resources.length)];
            LocalDateTime created = randomDateTime(rng);

            list.add(AuditLog.builder()
                .id(uuid())
                .actorId(actor.getId())
                .actorUsername(actor.getEmail())
                .actorRole(actor.getRole() != null ? actor.getRole().name() : "CUSTOMER")
                .action(action)
                .resourceType(resourceType)
                .resourceId(uuid())
                .beforeState("{\"status\":\"OLD\"}")
                .afterState("{\"status\":\"NEW\"}")
                .ipAddress(String.format("10.0.%d.%d", rng.nextInt(255), rng.nextInt(255)))
                .userAgent("Mozilla/5.0 (Seeder)")
                .reason("Seeded audit")
                .severity(sevs[rng.nextInt(sevs.length)])
                .metadata(Map.of("seed", true, "batch", String.valueOf(batch == null ? -1 : batch)))
                .createdAt(created)
                .build());
        }
        mongo.insertAll(list);
        log.info("[audit_logs] batch={}, +{} records (total = {})", batch, list.size(), mongo.getCollection("audit_logs").countDocuments());
    }

    // ============================================================================
    // ============================ AUDIT LOG ENTRIES =============================
    // ============================================================================
    // Target: 5000. Batches: 50. Dùng chung collection audit_logs.

    private void seedAuditLogEntriesBatch(Integer batch) {
        int target = 5000;
        int from = batch == null ? 0 : batch * BATCH_SIZE;
        int to   = batch == null ? target : Math.min(from + BATCH_SIZE, target);
        if (from >= target) return;

        List<User> actors = mongo.findAll(User.class);
        if (actors.isEmpty()) return;

        Random rng = new Random(SEED + 103 + from);
        List<AuditLogEntry> list = new ArrayList<>(to - from);
        AuditAction[] actions = AuditAction.values();
        String[] resources = {"USER","SHOP","PRODUCT","REVIEW","COMPLAINT","REPORT_CASE"};
        String[] sevs = {"INFO","WARNING","CRITICAL"};
        for (int i = from; i < to; i++) {
            User actor = actors.get(rng.nextInt(actors.size()));
            AuditAction action = actions[rng.nextInt(actions.length)];
            String resourceType = resources[rng.nextInt(resources.length)];
            LocalDateTime created = randomDateTime(rng);

            list.add(AuditLogEntry.builder()
                .id(uuid())
                .actorId(actor.getId())
                .actorEmail(actor.getEmail())
                .role(actor.getRole())
                .action(action.name())
                .resourceType(resourceType)
                .resourceId(uuid())
                .reason("Seeded entry")
                .severity(sevs[rng.nextInt(sevs.length)])
                .before("OLD")
                .after("NEW")
                .ip(String.format("192.168.%d.%d", rng.nextInt(255), rng.nextInt(255)))
                .createdAt(created)
                .build());
        }
        mongo.insertAll(list);
        log.info("[audit_log_entries] batch={}, +{} records (total = {})", batch, list.size(), mongo.getCollection("audit_logs").countDocuments());
    }

    // ============================================================================
    // ========================== SCHEDULER IDEMPOTENCY ===========================
    // ============================================================================
    // Target: 200. Batches: 2.

    private void seedSchedulerRecordsBatch(Integer batch) {
        int target = 200;
        int from = batch == null ? 0 : batch * BATCH_SIZE;
        int to   = batch == null ? target : Math.min(from + BATCH_SIZE, target);
        if (from >= target) return;

        List<Complaint> complaints = mongo.findAll(Complaint.class);
        if (complaints.isEmpty()) return;

        Random rng = new Random(SEED + 107 + from);
        List<SchedulerIdempotencyRecord> list = new ArrayList<>(to - from);
        String[] statuses = {"SUCCESS","FAILED","SKIPPED"};
        String[] ops = {"ESCALATE_L1","ESCALATE_L2"};
        for (int i = from; i < to; i++) {
            Complaint c = complaints.get(rng.nextInt(complaints.size()));
            LocalDateTime created = randomDateTime(rng);
            list.add(SchedulerIdempotencyRecord.builder()
                .id(uuid())
                .operationKey("complaint-escalation:" + c.getId() + ":" + ops[rng.nextInt(ops.length)])
                .jobName("ComplaintEscalationScheduler")
                .entityType("complaint")
                .entityId(c.getId())
                .operation(ops[rng.nextInt(ops.length)])
                .status(statuses[rng.nextInt(statuses.length)])
                .detail("Auto-processed by scheduler")
                .createdAt(created)
                .expiresAt(created.plusDays(7))
                .build());
        }
        mongo.insertAll(list);
        log.info("[scheduler_idempotency] batch={}, +{} records (total = {})", batch, list.size(), mongo.getCollection("scheduler_idempotency").countDocuments());
    }
}
