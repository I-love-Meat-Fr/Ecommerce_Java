package com.ecommerce.cnj70.config;

import org.springframework.stereotype.Component;

/**
 * Utility helper for image URLs in Thymeleaf templates.
 * Normalizes image URLs and provides fallbacks.
 * 
 * Usage in templates: th:src="${@image.url(item.imageUrl)}"
 *                     th:src="${@image.urlOrPlaceholder(item.imageUrl, '/uploads/placeholder.jpg')}"
 */
@Component("image")
public class ImageHelper {

    private static final String PLACEHOLDER_COLOR = "f0f7f6";
    private static final String PLACEHOLDER_TEXT_COLOR = "94a3b8";
    private static final String UPLOADS_PREFIX = "/uploads/";

    /**
     * Returns a normalized, safe image URL. If the input is null, empty,
     * or doesn't start with "/" or "http", prepends /uploads/.
     */
    public String url(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return null;
        }
        String trimmed = imageUrl.trim();

        // Already a full URL or an absolute path
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed;
        }
        if (trimmed.startsWith("/")) {
            return trimmed;
        }

        // Bare filename — prepend /uploads/
        return UPLOADS_PREFIX + trimmed;
    }

    /**
     * Returns a normalized image URL, falling back to a placeholder
     * if the input is null/empty.
     */
    public String urlOrPlaceholder(String imageUrl) {
        return urlOrPlaceholder(imageUrl, "placeholder.jpg");
    }

    /**
     * Returns a normalized image URL, falling back to a placeholder
     * constructed from the given base filename.
     */
    public String urlOrPlaceholder(String imageUrl, String placeholderFile) {
        String normalized = url(imageUrl);
        if (normalized == null || normalized.isBlank()) {
            return url(placeholderFile);
        }
        return normalized;
    }

    /**
     * Returns a data URI for a colored placeholder image using placehold.co.
     * @param text Optional text overlay (e.g. "No Img")
     */
    public String placeholder(String text) {
        return placeholder(120, 120, text);
    }

    /**
     * Returns a data URI for a colored placeholder image.
     * @param width  Image width in pixels
     * @param height Image height in pixels
     * @param text   Optional text overlay
     */
    public String placeholder(int width, int height, String text) {
        String encoded = (text != null && !text.isBlank())
                ? java.net.URLEncoder.encode(text, java.nio.charset.StandardCharsets.UTF_8)
                : "No+Img";
        return String.format(
                "https://placehold.co/%dx%d/%s/%s?text=%s",
                width, height, PLACEHOLDER_COLOR, PLACEHOLDER_TEXT_COLOR, encoded
        );
    }

    /**
     * Convenience: returns the correct image src for a cart item or product image.
     * Handles null, empty, bare filenames, and full URLs uniformly.
     */
    public String productImage(String imageUrl) {
        String normalized = url(imageUrl);
        if (normalized == null || normalized.isBlank()) {
            return placeholder(120, 120, null);
        }
        return normalized;
    }

    /**
     * Convenience: thumbnail with custom size for placehold.co.
     */
    public String productImage(String imageUrl, int size) {
        String normalized = url(imageUrl);
        if (normalized == null || normalized.isBlank()) {
            return placeholder(size, size, null);
        }
        return normalized;
    }

    /**
     * Returns the background color for a placeholder based on image URL hash.
     * This gives each product a slightly different placeholder color for visual variety.
     */
    public String placeholderBg(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return PLACEHOLDER_COLOR;
        }
        int hash = Math.abs(imageUrl.hashCode());
        String[] colors = {
                "f0f7f6", "f0f4f7", "f4f0f7", "f0f7f4", "f7f4f0",
                "e8f5f6", "f0faf5", "faf5e8", "eaf0fa", "faf0ea"
        };
        return colors[hash % colors.length];
    }
}
