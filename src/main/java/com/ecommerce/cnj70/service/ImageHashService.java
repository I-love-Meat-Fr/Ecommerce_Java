package com.ecommerce.cnj70.service;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

/**
 * C3 / C5 — Tối giản service hash image URL thành SHA-256 hex.
 *
 * <p>Hạn chế theo rule "không tải file/image nguy hiểm, không network call":
 * <ul>
 *   <li>Hash dựa trên URL string (deterministic cho cùng URL).</li>
 *   <li>Không download binary để hash nội dung file.</li>
 *   <li>Cùng URL trên nhiều product → cùng hash → phát hiện duplicate ở
 *       mức URL-level (acceptable per spec).</li>
 *   <li>Method {@link #isStockPlaceholder(String)} phát hiện URL thuộc
 *       pattern placeholder/stock-photo (dùng cho ImageHashCheck C5).</li>
 * </ul>
 *
 * <p>Abstraction này mock được dễ dàng — không có network/file IO.
 */
@Service
public class ImageHashService {

    /**
     * Trả về SHA-256 hex (64 ký tự) của URL. Null nếu URL null/rỗng.
     */
    public String hashUrl(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(url.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 luôn có trong JDK — không thể xảy ra.
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    /**
     * Hash một list URL; trả về list cùng thứ tự, hoặc empty list nếu input rỗng.
     * Null trong list → null trong output (để check không crash).
     */
    public List<String> hashUrls(List<String> urls) {
        if (urls == null || urls.isEmpty()) {
            return List.of();
        }
        return urls.stream().map(this::hashUrl).toList();
    }

    /**
     * Heuristic phát hiện URL là ảnh placeholder/stock.
     * <p>Rule đơn giản (extend thêm khi cần):
     * <ul>
     *   <li>URL chứa "placeholder" / "no-image" / "default" / "sample".</li>
     *   <li>Domain stock-photo phổ biến.</li>
     *   <li>URL rỗng/null.</li>
     * </ul>
     */
    public boolean isStockPlaceholder(String url) {
        if (url == null || url.isBlank()) {
            return true;
        }
        String lower = url.toLowerCase();
        if (lower.contains("placeholder")
                || lower.contains("no-image")
                || lower.contains("noimage")
                || lower.contains("default-image")
                || lower.contains("sample")) {
            return true;
        }
        // Pattern phổ biến của stock photo CDN.
        return lower.contains("placehold.co")
                || lower.contains("picsum.photos")
                || lower.contains("dummyimage.com")
                || lower.contains("via.placeholder");
    }
}
