package com.ecommerce.cnj70.service.impl;

import com.ecommerce.cnj70.document.Banner;
import com.ecommerce.cnj70.enums.BannerStatus;
import com.ecommerce.cnj70.repository.BannerRepository;
import com.ecommerce.cnj70.service.CustomerBannerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Phase 17 — Customer Banner Service (Dynamic Display).
 *
 * Customer chỉ thấy banner có status = PUBLISHED.
 * Phase 17 Customer Display Contract (Task 17.5):
 *   - Filter: status = PUBLISHED
 *   - Sort: sortOrder ASC, createdAt ASC
 *   - Có thể filter theo position (HERO_SLIDER, PROMO_GRID)
 *   - KHÔNG áp dụng Schedule (LOCK 4 — chờ Contract)
 *   - KHÔNG áp dụng Priority algorithm (LOCK 5)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerBannerServiceImpl implements CustomerBannerService {

    private final BannerRepository bannerRepository;

    @Override
    public List<Banner> getVisibleBanners(String position) {
        if (StringUtils.hasText(position)) {
            return bannerRepository
                    .findByStatusAndPositionOrderBySortOrderAscCreatedAtAsc(
                            BannerStatus.PUBLISHED, position);
        }
        return bannerRepository
                .findByStatusOrderBySortOrderAscCreatedAtAsc(BannerStatus.PUBLISHED);
    }
}
