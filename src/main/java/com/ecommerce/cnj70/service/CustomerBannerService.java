package com.ecommerce.cnj70.service;

import com.ecommerce.cnj70.document.Banner;

import java.util.List;

public interface CustomerBannerService {

    /**
     * Customer hiển thị banner PUBLISHED theo position (nếu có).
     * Nếu position null → lấy tất cả PUBLISHED.
     */
    List<Banner> getVisibleBanners(String position);
}
