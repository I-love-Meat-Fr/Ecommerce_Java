package com.ecommerce.cnj70.service;

import com.ecommerce.cnj70.document.Banner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminBannerService {

    /**
     * Admin Banner List với search + status filter + pagination.
     */
    Page<Banner> listBanners(Pageable pageable, String q, String statusFilter);

    /**
     * Admin Banner Detail.
     */
    Banner getBannerById(String id);

    /**
     * Tạo Banner mới. Mặc định status = UNPUBLISHED.
     * Phase 17: Create → UNPUBLISHED (Admin phải Publish thủ công).
     */
    Banner createBanner(Banner banner);

    /**
     * Cập nhật Banner content. Không thay đổi status ở đây (publish/unpublish riêng).
     */
    Banner updateBanner(String id, Banner banner);

    /**
     * Publish Banner.
     */
    Banner publishBanner(String id);

    /**
     * Unpublish Banner.
     */
    Banner unpublishBanner(String id);

    /**
     * Hard delete Banner theo contract (KHÔNG cascade).
     */
    void deleteBanner(String id);
}
