package com.ecommerce.cnj70.service.impl;

import com.ecommerce.cnj70.document.Banner;
import com.ecommerce.cnj70.dto.moderation.AuditEvent;
import com.ecommerce.cnj70.enums.BannerStatus;
import com.ecommerce.cnj70.enums.UserRole;
import com.ecommerce.cnj70.exception.BadRequestException;
import com.ecommerce.cnj70.exception.ResourceNotFoundException;
import com.ecommerce.cnj70.repository.BannerRepository;
import com.ecommerce.cnj70.service.AdminBannerService;
import com.ecommerce.cnj70.service.AuditEventWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Phase 17 + Phase 4C — Admin Banner / PR Service.
 *
 * <p>Phase 17 LOCKs preserved:</p>
 * <ul>
 *     <li>LOCK 4: Only {@link BannerStatus#PUBLISHED} / {@link BannerStatus#UNPUBLISHED}.</li>
 *     <li>LOCK 5: No priority algorithm; only {@code sortOrder} integer sort.</li>
 *     <li>LOCK 6: Reuse {@code StorageService} for image upload.</li>
 *     <li>LOCK 7: No Customer Home redesign.</li>
 *     <li>LOCK 8: No modification to existing Product/Vendor media.</li>
 *     <li>LOCK 9: No modification to other modules.</li>
 * </ul>
 *
 * <p>Phase 4C hardening (additive, no new fields):</p>
 * <ul>
 *     <li>URL safety — reject {@code javascript:}, {@code data:}, {@code vbscript:}
 *         schemes on banner {@code link} (target URL).</li>
 *     <li>Length validation — title ≤ 200 chars, description ≤ 1000, link ≤ 2048,
 *         tag/ctaText ≤ 100, tagIcon/ctaIcon ≤ 60.</li>
 *     <li>sortOrder integer guard — reject decimals, non-numeric, out-of-bounds.</li>
 *     <li>Audit emission on create/update/publish/unpublish/delete via
 *         {@link AuditEventWriter} (INTEGRATION-READY seam).</li>
 *     <li>Idempotent state transitions — explicit guard so re-publish / unpublish
 *         don't silently overwrite.</li>
 *     <li>Concurrency-safe delete — pre-check existence then delete (404 vs 500).</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminBannerServiceImpl implements AdminBannerService {

    /** Phase 4C §31 — banned URL schemes for banner link (target URL). */
    private static final Set<String> BANNED_URL_SCHEMES = Set.of("javascript", "data", "vbscript");

    /** Phase 4C §29 — sortOrder bounds (admin UX). Source field exists; business rule is sanity only. */
    private static final int SORT_ORDER_MIN = -1_000;
    private static final int SORT_ORDER_MAX = 1_000;

    /** Length caps — soft caps consistent with template maxlength attributes. */
    private static final int TITLE_MAX = 200;
    private static final int DESCRIPTION_MAX = 1000;
    private static final int LINK_MAX = 2048;
    private static final int SHORT_TEXT_MAX = 100;
    private static final int ICON_MAX = 60;

    /** Title regex — no leading/trailing whitespace; reject all-whitespace. */
    private static final Pattern SAFE_TITLE = Pattern.compile(".*\\S.*");

    private final BannerRepository bannerRepository;
    private final MongoTemplate mongoTemplate;
    private final AuditEventWriter auditEventWriter;

    /* ==== LIST / VIEW ==== */

    @Override
    public Page<Banner> listBanners(Pageable pageable, String q, String statusFilter) {
        boolean hasQ = StringUtils.hasText(q);
        boolean hasStatus = StringUtils.hasText(statusFilter);

        if (!hasQ && !hasStatus) {
            Query qAll = new Query().with(pageable);
            long total = mongoTemplate.count(new Query(), Banner.class);
            List<Banner> content = mongoTemplate.find(qAll, Banner.class);
            return new PageImpl<>(content, pageable, total);
        }

        if (hasQ && !hasStatus) {
            String trimmed = q.trim();
            Pattern regex = Pattern.compile(Pattern.quote(trimmed), Pattern.CASE_INSENSITIVE);
            Query query = new Query(
                    new Criteria().orOperator(
                            Criteria.where("title").regex(regex),
                            Criteria.where("description").regex(regex)
                    )
            ).with(pageable);
            long total = mongoTemplate.count(Query.of(query).limit(-1).skip(-1), Banner.class);
            List<Banner> content = mongoTemplate.find(query, Banner.class);
            return new PageImpl<>(content, pageable, total);
        }

        if (!hasQ && hasStatus) {
            // Phase 4C §25 — only accept the two contract statuses.
            if (!isKnownStatus(statusFilter)) {
                throw new BadRequestException("Trạng thái banner không hợp lệ: " + statusFilter);
            }
            Query query = new Query(Criteria.where("status").is(statusFilter)).with(pageable);
            long total = mongoTemplate.count(Query.of(query).limit(-1).skip(-1), Banner.class);
            List<Banner> content = mongoTemplate.find(query, Banner.class);
            return new PageImpl<>(content, pageable, total);
        }

        if (!isKnownStatus(statusFilter)) {
            throw new BadRequestException("Trạng thái banner không hợp lệ: " + statusFilter);
        }
        String trimmed = q.trim();
        Pattern regex = Pattern.compile(Pattern.quote(trimmed), Pattern.CASE_INSENSITIVE);
        Query query = new Query(
                new Criteria().andOperator(
                        new Criteria().orOperator(
                                Criteria.where("title").regex(regex),
                                Criteria.where("description").regex(regex)
                        ),
                        Criteria.where("status").is(statusFilter)
                )
        ).with(pageable);
        long total = mongoTemplate.count(Query.of(query).limit(-1).skip(-1), Banner.class);
        List<Banner> content = mongoTemplate.find(query, Banner.class);
        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public Banner getBannerById(String id) {
        if (!StringUtils.hasText(id)) {
            throw new BadRequestException("ID banner không hợp lệ");
        }
        // Phase 5 + Phase 3 fix: Hỗ trợ CẢ 2 kiểu _id (String lẫn ObjectId).
        // KHÔNG ép raw.put("_id", id) vì sẽ làm save() insert document mới
        // khi DB lưu _id là ObjectId → duplicate key error (E11000).
        org.bson.Document raw = mongoTemplate.getCollection("banners")
                .find(new org.bson.Document("_id", id))
                .first();
        if (raw == null && id.length() == 24 && id.matches("[0-9a-fA-F]+")) {
            org.bson.types.ObjectId oid = new org.bson.types.ObjectId(id);
            raw = mongoTemplate.getCollection("banners")
                    .find(new org.bson.Document("_id", oid))
                    .first();
        }
        if (raw == null) {
            throw new ResourceNotFoundException("Không tìm thấy banner với ID: " + id);
        }
        if (!raw.containsKey("_class")) {
            raw.put("_class", Banner.class.getName());
        }
        Banner banner = mongoTemplate.getConverter().read(Banner.class, raw);
        if (banner.getId() == null) {
            banner.setId(id);
        }
        return banner;
    }

    /* ==== CREATE ==== */

    @Override
    @Transactional
    public Banner createBanner(Banner banner) {
        if (banner == null) {
            throw new BadRequestException("Dữ liệu banner không được null");
        }
        validateForCreateOrUpdate(banner);

        // Phase 17 Task 17.14: Create → UNPUBLISHED (Admin phải Publish thủ công).
        banner.setId(null);
        banner.setStatus(BannerStatus.UNPUBLISHED);
        if (banner.getSortOrder() == null) {
            banner.setSortOrder(0);
        }
        Banner saved = bannerRepository.save(banner);
        log.info("AdminBannerService.createBanner: created id={} title={}", saved.getId(), saved.getTitle());
        emitAudit("BANNER_CREATED", null, saved, "Admin created Banner");
        return saved;
    }

    /* ==== UPDATE ==== */

    @Override
    @Transactional
    public Banner updateBanner(String id, Banner banner) {
        Banner existing = getBannerById(id);
        if (banner == null) {
            throw new BadRequestException("Dữ liệu banner không được null");
        }
        // On update, image is optional in the patch — existing is preserved if absent.
        validateForUpdate(banner);

        // Phase 17: status is NOT updated here. It only changes via publish/unpublish.
        if (StringUtils.hasText(banner.getTitle())) {
            existing.setTitle(banner.getTitle());
        }
        if (banner.getDescription() != null) {
            existing.setDescription(banner.getDescription());
        }
        if (StringUtils.hasText(banner.getImageUrl())) {
            existing.setImageUrl(banner.getImageUrl());
        }
        if (banner.getLink() != null) {
            // Phase 4C §31 — explicit link normalization (allow empty to clear).
            existing.setLink(banner.getLink().isBlank() ? null : banner.getLink().trim());
        }
        if (banner.getTag() != null) {
            existing.setTag(banner.getTag().isBlank() ? null : banner.getTag().trim());
        }
        if (banner.getTagIcon() != null) {
            existing.setTagIcon(banner.getTagIcon().isBlank() ? null : banner.getTagIcon().trim());
        }
        if (banner.getCtaText() != null) {
            existing.setCtaText(banner.getCtaText().isBlank() ? null : banner.getCtaText().trim());
        }
        if (banner.getCtaIcon() != null) {
            existing.setCtaIcon(banner.getCtaIcon().isBlank() ? null : banner.getCtaIcon().trim());
        }
        if (banner.getTheme() != null) {
            existing.setTheme(banner.getTheme().isBlank() ? null : banner.getTheme().trim());
        }
        if (banner.getPosition() != null) {
            existing.setPosition(banner.getPosition().isBlank() ? null : banner.getPosition().trim());
        }
        if (banner.getSortOrder() != null) {
            existing.setSortOrder(banner.getSortOrder());
        }

        Banner saved = bannerRepository.save(existing);
        log.info("AdminBannerService.updateBanner: updated id={}", saved.getId());
        emitAudit("BANNER_UPDATED", existing, saved, "Admin updated Banner");
        return saved;
    }

    /* ==== PUBLISH / UNPUBLISH ==== */

    @Override
    @Transactional
    public Banner publishBanner(String id) {
        Banner banner = getBannerById(id);

        if (BannerStatus.isPublished(banner.getStatus())) {
            // Phase 4C §33 — idempotent rejection so re-publish doesn't silently succeed.
            throw new BadRequestException("Banner đã ở trạng thái Published");
        }

        // Phase 3 critical fix: atomic update để tránh E11000 duplicate key khi DB
        // lưu _id là ObjectId (save() sẽ insert document mới với _id String hex).
        Banner before = snapshotBanner(banner);
        Object nativeId = resolveIdForQuery(id);
        org.bson.Document update = new org.bson.Document("$set",
                new org.bson.Document("status", BannerStatus.PUBLISHED)
                        .append("updatedAt", java.util.Date.from(java.time.LocalDateTime.now()
                                .atZone(java.time.ZoneId.systemDefault()).toInstant())));
        long matched = mongoTemplate.getCollection("banners")
                .updateOne(new org.bson.Document("_id", nativeId), update)
                .getMatchedCount();
        Banner saved;
        if (matched == 0) {
            log.warn("AdminBannerService.publishBanner: atomic update not matched, falling back");
            banner.setStatus(BannerStatus.PUBLISHED);
            saved = bannerRepository.save(banner);
        } else {
            saved = getBannerById(id);
        }
        log.info("AdminBannerService.publishBanner: id={} → PUBLISHED", saved.getId());
        emitAudit("BANNER_PUBLISHED", before, saved, "Admin published Banner");
        return saved;
    }

    @Override
    @Transactional
    public Banner unpublishBanner(String id) {
        Banner banner = getBannerById(id);

        if (!BannerStatus.isPublished(banner.getStatus())) {
            throw new BadRequestException("Banner hiện không ở trạng thái Published");
        }

        // Phase 3 critical fix: atomic update (xem publishBanner).
        Banner before = snapshotBanner(banner);
        Object nativeId = resolveIdForQuery(id);
        org.bson.Document update = new org.bson.Document("$set",
                new org.bson.Document("status", BannerStatus.UNPUBLISHED)
                        .append("updatedAt", java.util.Date.from(java.time.LocalDateTime.now()
                                .atZone(java.time.ZoneId.systemDefault()).toInstant())));
        long matched = mongoTemplate.getCollection("banners")
                .updateOne(new org.bson.Document("_id", nativeId), update)
                .getMatchedCount();
        Banner saved;
        if (matched == 0) {
            log.warn("AdminBannerService.unpublishBanner: atomic update not matched, falling back");
            banner.setStatus(BannerStatus.UNPUBLISHED);
            saved = bannerRepository.save(banner);
        } else {
            saved = getBannerById(id);
        }
        log.info("AdminBannerService.unpublishBanner: id={} → UNPUBLISHED", saved.getId());
        emitAudit("BANNER_UNPUBLISHED", before, saved, "Admin unpublished Banner");
        return saved;
    }

    /* ==== DELETE ==== */

    @Override
    @Transactional
    public void deleteBanner(String id) {
        Banner banner = getBannerById(id);
        // Phase 17 LOCK 9: KHÔNG cascade delete Order/Product/Customer/Review.
        // Phase 17 LOCK 8: KHÔNG xóa file ảnh trong /uploads/.
        // Phase 3 critical fix: atomic delete với native _id.
        Object nativeId = resolveIdForQuery(id);
        long deleted = mongoTemplate.getCollection("banners")
                .deleteOne(new org.bson.Document("_id", nativeId))
                .getDeletedCount();
        if (deleted == 0) {
            log.warn("AdminBannerService.deleteBanner: atomic delete not matched, falling back to repo");
            bannerRepository.deleteById(banner.getId());
        }
        log.info("AdminBannerService.deleteBanner: removed id={} title={}",
                banner.getId(), banner.getTitle());
        emitAudit("BANNER_DELETED", banner, null, "Admin deleted Banner");
    }

    /**
     * Phase 3 critical fix: trả về _id dạng native (ObjectId hoặc String) để
     * Mongo query match đúng document trong DB, tránh E11000 duplicate key.
     */
    private Object resolveIdForQuery(String id) {
        if (id.length() == 24 && id.matches("[0-9a-fA-F]+")) {
            org.bson.Document raw = mongoTemplate.getCollection("banners")
                    .find(new org.bson.Document("_id", new org.bson.types.ObjectId(id)))
                    .first();
            if (raw != null) {
                return new org.bson.types.ObjectId(id);
            }
        }
        return id;
    }

    /* ==== Validation helpers ==== */

    private void validateForCreateOrUpdate(Banner banner) {
        // Title — required, non-whitespace, ≤ TITLE_MAX
        if (!StringUtils.hasText(banner.getTitle()) || !SAFE_TITLE.matcher(banner.getTitle()).matches()) {
            throw new BadRequestException("Tiêu đề banner không được để trống hoặc chỉ chứa khoảng trắng");
        }
        if (banner.getTitle().length() > TITLE_MAX) {
            throw new BadRequestException("Tiêu đề banner tối đa " + TITLE_MAX + " ký tự");
        }
        // Image required on create
        if (!StringUtils.hasText(banner.getImageUrl())) {
            throw new BadRequestException("Ảnh banner không được để trống");
        }
        if (banner.getImageUrl().length() > 2048) {
            throw new BadRequestException("Đường dẫn ảnh tối đa 2048 ký tự");
        }
        validateCommon(banner);
    }

    private void validateForUpdate(Banner banner) {
        // Title optional on update, but if provided must be valid
        if (banner.getTitle() != null) {
            if (banner.getTitle().isBlank() || !SAFE_TITLE.matcher(banner.getTitle()).matches()) {
                throw new BadRequestException("Tiêu đề banner không được để trống hoặc chỉ chứa khoảng trắng");
            }
            if (banner.getTitle().length() > TITLE_MAX) {
                throw new BadRequestException("Tiêu đề banner tối đa " + TITLE_MAX + " ký tự");
            }
        }
        // Image optional on update — only validate length if provided
        if (StringUtils.hasText(banner.getImageUrl()) && banner.getImageUrl().length() > 2048) {
            throw new BadRequestException("Đường dẫn ảnh tối đa 2048 ký tự");
        }
        validateCommon(banner);
    }

    private void validateCommon(Banner banner) {
        // Link / target URL safety — Phase 4C §31
        if (StringUtils.hasText(banner.getLink())) {
            String link = banner.getLink().trim();
            if (link.length() > LINK_MAX) {
                throw new BadRequestException("Link banner tối đa " + LINK_MAX + " ký tự");
            }
            String scheme = extractScheme(link);
            if (scheme != null && BANNED_URL_SCHEMES.contains(scheme.toLowerCase(Locale.ROOT))) {
                throw new BadRequestException(
                        "Link banner không được dùng scheme nguy hiểm: " + scheme);
            }
        }
        // Description
        if (banner.getDescription() != null && banner.getDescription().length() > DESCRIPTION_MAX) {
            throw new BadRequestException("Mô tả tối đa " + DESCRIPTION_MAX + " ký tự");
        }
        // Tag / CTA
        if (banner.getTag() != null && banner.getTag().length() > SHORT_TEXT_MAX) {
            throw new BadRequestException("Tag tối đa " + SHORT_TEXT_MAX + " ký tự");
        }
        if (banner.getCtaText() != null && banner.getCtaText().length() > SHORT_TEXT_MAX) {
            throw new BadRequestException("CTA text tối đa " + SHORT_TEXT_MAX + " ký tự");
        }
        if (banner.getTagIcon() != null && banner.getTagIcon().length() > ICON_MAX) {
            throw new BadRequestException("Tag icon tối đa " + ICON_MAX + " ký tự");
        }
        if (banner.getCtaIcon() != null && banner.getCtaIcon().length() > ICON_MAX) {
            throw new BadRequestException("CTA icon tối đa " + ICON_MAX + " ký tự");
        }
        // Theme whitelist — must be one of the rendered CSS themes
        if (StringUtils.hasText(banner.getTheme())) {
            Set<String> allowedThemes = Set.of("primary", "teal", "purple", "orange", "red", "slate");
            if (!allowedThemes.contains(banner.getTheme().toLowerCase(Locale.ROOT))) {
                throw new BadRequestException("Theme không hợp lệ: " + banner.getTheme());
            }
        }
        // sortOrder — integer guard
        if (banner.getSortOrder() != null) {
            int so = banner.getSortOrder();
            if (so < SORT_ORDER_MIN || so > SORT_ORDER_MAX) {
                throw new BadRequestException(
                        "sortOrder phải nằm trong [" + SORT_ORDER_MIN + ", " + SORT_ORDER_MAX + "]");
            }
        }
    }

    private static String extractScheme(String link) {
        try {
            // Treat as URI for scheme extraction only (don't fully parse — mustn't break
            // legitimate relative URLs like "/promo/xyz").
            String lower = link.toLowerCase(Locale.ROOT);
            int colonIdx = lower.indexOf(':');
            int slashIdx = lower.indexOf('/');
            int questionIdx = lower.indexOf('?');
            int hashIdx = lower.indexOf('#');
            int firstNonScheme = minPositive(slashIdx, questionIdx, hashIdx, lower.length());
            if (colonIdx > 0 && colonIdx < firstNonScheme) {
                return lower.substring(0, colonIdx);
            }
            return null;
        } catch (Exception ex) {
            return null;
        }
    }

    private static int minPositive(int... values) {
        int m = Integer.MAX_VALUE;
        for (int v : values) if (v >= 0 && v < m) m = v;
        return m == Integer.MAX_VALUE ? Integer.MAX_VALUE : m;
    }

    private static boolean isKnownStatus(String status) {
        return BannerStatus.PUBLISHED.equalsIgnoreCase(status)
                || BannerStatus.UNPUBLISHED.equalsIgnoreCase(status);
    }

    /* ==== Audit ==== */

    private void emitAudit(String action, Banner before, Banner after, String reason) {
        try {
            String beforeStr = before == null ? null : bannerFingerprint(before);
            String afterStr = after == null ? null : bannerFingerprint(after);
            AuditEvent ev = AuditEvent.builder()
                    .role(UserRole.ADMIN)
                    .action(action)
                    .resourceType("BANNER")
                    .resourceId(after != null ? after.getId() : (before != null ? before.getId() : null))
                    .reason(reason)
                    .before(beforeStr)
                    .after(afterStr)
                    .createdAt(LocalDateTime.now())
                    .build();
            auditEventWriter.write(ev);
        } catch (Exception ex) {
            log.warn("Banner audit emission failed for action {}: {}", action, ex.getMessage());
        }
    }

    private static String bannerFingerprint(Banner b) {
        return "title=" + safe(b.getTitle())
                + ";status=" + safe(b.getStatus())
                + ";sortOrder=" + (b.getSortOrder() == null ? "0" : b.getSortOrder())
                + ";hasImage=" + StringUtils.hasText(b.getImageUrl())
                + ";hasLink=" + StringUtils.hasText(b.getLink());
    }

    private static String safe(String s) {
        return s == null ? "" : s.replace('\n', ' ').replace('\r', ' ');
    }

    /** Snapshot for audit "before" field — avoids leaking full entity into audit. */
    private static Banner snapshotBanner(Banner src) {
        return Banner.builder()
                .id(src.getId())
                .title(src.getTitle())
                .status(src.getStatus())
                .sortOrder(src.getSortOrder())
                .build();
    }
}
