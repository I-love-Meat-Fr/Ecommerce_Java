package com.ecommerce.cnj70.service.impl;

import com.ecommerce.cnj70.document.Banner;
import com.ecommerce.cnj70.enums.BannerStatus;
import com.ecommerce.cnj70.exception.BadRequestException;
import com.ecommerce.cnj70.exception.ResourceNotFoundException;
import com.ecommerce.cnj70.repository.BannerRepository;
import com.ecommerce.cnj70.service.AdminBannerService;
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

import java.util.List;
import java.util.regex.Pattern;

/**
 * Phase 17 — Admin Banner / PR Service.
 *
 * Đã implement đầy đủ Admin CRUD theo Phase 17 contract:
 *   - List (search + status filter + pagination)
 *   - Detail
 *   - Create (mặc định UNPUBLISHED)
 *   - Edit
 *   - Publish / Unpublish
 *   - Delete (hard delete, không cascade)
 *
 * Phase 17 LOCK nguyên tắc:
 *   - LOCK 4: KHÔNG tự tạo status mới → chỉ dùng BannerStatus.PUBLISHED/UNPUBLISHED
 *   - LOCK 5: KHÔNG tự tạo priority algorithm → chỉ dùng sortOrder int
 *   - LOCK 6: REUSE StorageService
 *   - LOCK 7: KHÔNG redesign Customer Home → chỉ thay đổi phần cần thiết
 *   - LOCK 8: KHÔNG phá Media hiện tại (Product/Vendor image)
 *   - LOCK 9: KHÔNG tự sửa module khác
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminBannerServiceImpl implements AdminBannerService {

    private final BannerRepository bannerRepository;
    private final MongoTemplate mongoTemplate;

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
            Query query = new Query(Criteria.where("status").is(statusFilter)).with(pageable);
            long total = mongoTemplate.count(Query.of(query).limit(-1).skip(-1), Banner.class);
            List<Banner> content = mongoTemplate.find(query, Banner.class);
            return new PageImpl<>(content, pageable, total);
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
        return bannerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy banner với ID: " + id));
    }

    @Override
    @Transactional
    public Banner createBanner(Banner banner) {
        if (banner == null) {
            throw new BadRequestException("Dữ liệu banner không được null");
        }
        if (!StringUtils.hasText(banner.getTitle())) {
            throw new BadRequestException("Tiêu đề banner không được để trống");
        }
        if (!StringUtils.hasText(banner.getImageUrl())) {
            throw new BadRequestException("Ảnh banner không được để trống");
        }
        // Phase 17 Task 17.14: Create → UNPUBLISHED (Admin phải Publish thủ công)
        banner.setId(null);
        banner.setStatus(BannerStatus.UNPUBLISHED);
        if (banner.getSortOrder() == null) {
            banner.setSortOrder(0);
        }
        Banner saved = bannerRepository.save(banner);
        log.info("AdminBannerService.createBanner: created id={} title={}", saved.getId(), saved.getTitle());
        return saved;
    }

    @Override
    @Transactional
    public Banner updateBanner(String id, Banner banner) {
        Banner existing = getBannerById(id);
        if (banner == null) {
            throw new BadRequestException("Dữ liệu banner không được null");
        }

        // KHÔNG thay đổi status ở update — status chỉ thay đổi qua publish/unpublish
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
            existing.setLink(banner.getLink());
        }
        if (banner.getTag() != null) {
            existing.setTag(banner.getTag());
        }
        if (banner.getTagIcon() != null) {
            existing.setTagIcon(banner.getTagIcon());
        }
        if (banner.getCtaText() != null) {
            existing.setCtaText(banner.getCtaText());
        }
        if (banner.getCtaIcon() != null) {
            existing.setCtaIcon(banner.getCtaIcon());
        }
        if (banner.getTheme() != null) {
            existing.setTheme(banner.getTheme());
        }
        if (banner.getPosition() != null) {
            existing.setPosition(banner.getPosition());
        }
        if (banner.getSortOrder() != null) {
            existing.setSortOrder(banner.getSortOrder());
        }

        Banner saved = bannerRepository.save(existing);
        log.info("AdminBannerService.updateBanner: updated id={}", saved.getId());
        return saved;
    }

    @Override
    @Transactional
    public Banner publishBanner(String id) {
        Banner banner = getBannerById(id);

        if (BannerStatus.isPublished(banner.getStatus())) {
            throw new BadRequestException("Banner đã ở trạng thái Published");
        }

        banner.setStatus(BannerStatus.PUBLISHED);
        Banner saved = bannerRepository.save(banner);
        log.info("AdminBannerService.publishBanner: id={} → PUBLISHED", saved.getId());
        return saved;
    }

    @Override
    @Transactional
    public Banner unpublishBanner(String id) {
        Banner banner = getBannerById(id);

        if (!BannerStatus.isPublished(banner.getStatus())) {
            throw new BadRequestException("Banner hiện không ở trạng thái Published");
        }

        banner.setStatus(BannerStatus.UNPUBLISHED);
        Banner saved = bannerRepository.save(banner);
        log.info("AdminBannerService.unpublishBanner: id={} → UNPUBLISHED", saved.getId());
        return saved;
    }

    @Override
    @Transactional
    public void deleteBanner(String id) {
        Banner banner = getBannerById(id);
        // Phase 17 LOCK 9: KHÔNG cascade delete Order/Product/Customer/Review.
        // Banner không có relationship đặc biệt → chỉ xóa document Banner.
        bannerRepository.deleteById(banner.getId());
        log.info("AdminBannerService.deleteBanner: removed id={} title={}",
                banner.getId(), banner.getTitle());
        // KHÔNG xóa file ảnh trong /uploads/ (LOCK 8: không phá media hiện tại).
    }
}
