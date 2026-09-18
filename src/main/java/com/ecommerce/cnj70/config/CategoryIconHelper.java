package com.ecommerce.cnj70.config;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Maps a category name to a FontAwesome 6 icon class and a colour token so
 * the home page can render a meaningful icon and gradient for each category.
 *
 * Matching is done case-insensitively on the first keyword that appears in
 * the category name. If nothing matches a sensible generic icon is returned.
 */
@Component("categoryIcon")
public class CategoryIconHelper {

    /** Visual theme for a category card. {@code gradient} uses two CSS colours. */
    public static final class Theme {
        private final String iconClass;
        private final String gradientFrom;
        private final String gradientTo;
        private final String soft;

        public Theme(String iconClass, String gradientFrom, String gradientTo, String soft) {
            this.iconClass = iconClass;
            this.gradientFrom = gradientFrom;
            this.gradientTo = gradientTo;
            this.soft = soft;
        }

        public String getIconClass() { return iconClass; }
        public String getGradientFrom() { return gradientFrom; }
        public String getGradientTo() { return gradientTo; }
        public String getSoft() { return soft; }
    }

    /** Ordered list — first match wins. */
    private static final Map<String, Theme> KEYWORDS = new LinkedHashMap<>();

    static {
        // ===== Thời trang & phụ kiện =====
        KEYWORDS.put("thời trang nữ",       new Theme("fa-person-dress",        "#ec4899", "#f472b6", "#fce7f3"));
        KEYWORDS.put("thời trang nam",       new Theme("fa-person",              "#3b82f6", "#60a5fa", "#dbeafe"));
        KEYWORDS.put("thời trang",           new Theme("fa-shirt",              "#ec4899", "#f472b6", "#fce7f3"));
        KEYWORDS.put("váy",                  new Theme("fa-person-dress",        "#ec4899", "#f472b6", "#fce7f3"));
        KEYWORDS.put("áo",                   new Theme("fa-shirt",              "#a855f7", "#c084fc", "#f3e8ff"));
        KEYWORDS.put("quần",                 new Theme("fa-shirt",              "#0ea5e9", "#38bdf8", "#e0f2fe"));
        KEYWORDS.put("giày",                 new Theme("fa-shoe-prints",        "#f97316", "#fb923c", "#ffedd5"));
        KEYWORDS.put("dép",                  new Theme("fa-shoe-prints",        "#f59e0b", "#fbbf24", "#fef3c7"));
        KEYWORDS.put("túi",                  new Theme("fa-bag-shopping",       "#d97706", "#f59e0b", "#fef3c7"));
        KEYWORDS.put("balo",                 new Theme("fa-bag-shopping",       "#0891b2", "#06b6d4", "#cffafe"));
        KEYWORDS.put("phụ kiện",             new Theme("fa-gem",                "#8b5cf6", "#a78bfa", "#ede9fe"));
        KEYWORDS.put("trang sức",            new Theme("fa-ring",               "#eab308", "#facc15", "#fef9c3"));
        KEYWORDS.put("đồng hồ",              new Theme("fa-clock",              "#475569", "#64748b", "#f1f5f9"));
        KEYWORDS.put("mắt kính",             new Theme("fa-glasses",            "#0f172a", "#334155", "#e2e8f0"));

        // ===== Công nghệ =====
        KEYWORDS.put("điện thoại",           new Theme("fa-mobile-screen",      "#0ea5e9", "#38bdf8", "#e0f2fe"));
        KEYWORDS.put("smartphone",           new Theme("fa-mobile-screen",      "#0ea5e9", "#38bdf8", "#e0f2fe"));
        KEYWORDS.put("laptop",               new Theme("fa-laptop",             "#1e293b", "#475569", "#e2e8f0"));
        KEYWORDS.put("máy tính",             new Theme("fa-laptop",             "#1e293b", "#475569", "#e2e8f0"));
        KEYWORDS.put("pc",                   new Theme("fa-desktop",            "#1e293b", "#475569", "#e2e8f0"));
        KEYWORDS.put("máy ảnh",              new Theme("fa-camera",             "#0f172a", "#1e293b", "#e2e8f0"));
        KEYWORDS.put("tai nghe",             new Theme("fa-headphones",         "#7c3aed", "#a78bfa", "#ede9fe"));
        KEYWORDS.put("loa",                  new Theme("fa-volume-high",        "#7c3aed", "#a78bfa", "#ede9fe"));
        KEYWORDS.put("tivi",                 new Theme("fa-tv",                 "#0891b2", "#06b6d4", "#cffafe"));
        KEYWORDS.put("máy tính bảng",        new Theme("fa-tablet-screen-button","#0ea5e9", "#38bdf8", "#e0f2fe"));
        KEYWORDS.put("đồ công nghệ",         new Theme("fa-microchip",          "#0891b2", "#06b6d4", "#cffafe"));
        KEYWORDS.put("công nghệ",            new Theme("fa-microchip",          "#0fb5b0", "#14b8a6", "#ccfbf1"));
        KEYWORDS.put("phụ kiện công nghệ",   new Theme("fa-plug",               "#14b8a6", "#2dd4bf", "#ccfbf1"));
        KEYWORDS.put("linh kiện",            new Theme("fa-memory",             "#0d9488", "#14b8a6", "#ccfbf1"));

        // ===== Nhà cửa & đời sống =====
        KEYWORDS.put("nội thất",             new Theme("fa-couch",              "#92400e", "#b45309", "#fef3c7"));
        KEYWORDS.put("trang trí",            new Theme("fa-paint-roller",       "#db2777", "#ec4899", "#fce7f3"));
        KEYWORDS.put("nhà cửa",              new Theme("fa-house",              "#0d9488", "#14b8a6", "#ccfbf1"));
        KEYWORDS.put("đời sống",             new Theme("fa-house-laptop",       "#0d9488", "#14b8a6", "#ccfbf1"));
        KEYWORDS.put("gia dụng",             new Theme("fa-blender",            "#0d9488", "#14b8a6", "#ccfbf1"));
        KEYWORDS.put("bếp",                  new Theme("fa-kitchen-set",        "#ea580c", "#f97316", "#ffedd5"));
        KEYWORDS.put("phòng tắm",            new Theme("fa-shower",             "#0891b2", "#06b6d4", "#cffafe"));
        KEYWORDS.put("chăn ga",              new Theme("fa-bed",                "#a855f7", "#c084fc", "#f3e8ff"));
        KEYWORDS.put("đèn",                  new Theme("fa-lightbulb",          "#eab308", "#facc15", "#fef9c3"));
        KEYWORDS.put("cây cảnh",             new Theme("fa-seedling",           "#16a34a", "#22c55e", "#dcfce7"));
        KEYWORDS.put("hoa",                  new Theme("fa-seedling",           "#16a34a", "#22c55e", "#dcfce7"));

        // ===== Sức khỏe & làm đẹp =====
        KEYWORDS.put("làm đẹp",              new Theme("fa-wand-magic-sparkles","#db2777", "#ec4899", "#fce7f3"));
        KEYWORDS.put("mỹ phẩm",              new Theme("fa-spray-can",          "#db2777", "#ec4899", "#fce7f3"));
        KEYWORDS.put("son",                  new Theme("fa-spray-can",          "#e11d48", "#f43f5e", "#ffe4e6"));
        KEYWORDS.put("chăm sóc da",          new Theme("fa-pump-soap",          "#f472b6", "#f9a8d4", "#fce7f3"));
        KEYWORDS.put("dưỡng",                new Theme("fa-pump-soap",          "#f472b6", "#f9a8d4", "#fce7f3"));
        KEYWORDS.put("nước hoa",             new Theme("fa-spray-can-sparkles", "#a855f7", "#c084fc", "#f3e8ff"));
        KEYWORDS.put("sức khỏe",             new Theme("fa-heart-pulse",        "#dc2626", "#ef4444", "#fee2e2"));
        KEYWORDS.put("sức khoẻ",             new Theme("fa-heart-pulse",        "#dc2626", "#ef4444", "#fee2e2"));
        KEYWORDS.put("thực phẩm chức năng",  new Theme("fa-capsules",           "#16a34a", "#22c55e", "#dcfce7"));
        KEYWORDS.put("y tế",                 new Theme("fa-briefcase-medical",  "#0ea5e9", "#38bdf8", "#e0f2fe"));
        KEYWORDS.put("thuốc",                new Theme("fa-briefcase-medical",  "#0ea5e9", "#38bdf8", "#e0f2fe"));

        // ===== Sách & văn phòng phẩm =====
        KEYWORDS.put("sách",                 new Theme("fa-book",               "#0891b2", "#06b6d4", "#cffafe"));
        KEYWORDS.put("truyện",               new Theme("fa-book-open",          "#0891b2", "#06b6d4", "#cffafe"));
        KEYWORDS.put("văn phòng phẩm",       new Theme("fa-pen",                "#475569", "#64748b", "#f1f5f9"));
        KEYWORDS.put("dụng cụ học tập",      new Theme("fa-pen-ruler",          "#475569", "#64748b", "#f1f5f9"));

        // ===== Thể thao & du lịch =====
        KEYWORDS.put("thể thao",             new Theme("fa-dumbbell",           "#f97316", "#fb923c", "#ffedd5"));
        KEYWORDS.put("gym",                  new Theme("fa-dumbbell",           "#dc2626", "#ef4444", "#fee2e2"));
        KEYWORDS.put("yoga",                 new Theme("fa-spa",                "#a855f7", "#c084fc", "#f3e8ff"));
        KEYWORDS.put("chạy bộ",              new Theme("fa-person-running",     "#f97316", "#fb923c", "#ffedd5"));
        KEYWORDS.put("đạp xe",               new Theme("fa-bicycle",            "#16a34a", "#22c55e", "#dcfce7"));
        KEYWORDS.put("bơi",                  new Theme("fa-person-swimming",    "#0891b2", "#06b6d4", "#cffafe"));
        KEYWORDS.put("bóng đá",              new Theme("fa-futbol",             "#16a34a", "#22c55e", "#dcfce7"));
        KEYWORDS.put("bóng rổ",              new Theme("fa-basketball",         "#ea580c", "#f97316", "#ffedd5"));
        KEYWORDS.put("câu cá",               new Theme("fa-fish",               "#0891b2", "#06b6d4", "#cffafe"));
        KEYWORDS.put("du lịch",              new Theme("fa-plane",              "#0ea5e9", "#38bdf8", "#e0f2fe"));
        KEYWORDS.put("cắm trại",             new Theme("fa-campground",         "#16a34a", "#22c55e", "#dcfce7"));

        // ===== Mẹ & bé =====
        KEYWORDS.put("mẹ và bé",             new Theme("fa-baby",               "#ec4899", "#f472b6", "#fce7f3"));
        KEYWORDS.put("đồ chơi",              new Theme("fa-gamepad",            "#a855f7", "#c084fc", "#f3e8ff"));
        KEYWORDS.put("sữa",                  new Theme("fa-baby",               "#ec4899", "#f472b6", "#fce7f3"));
        KEYWORDS.put("tã",                   new Theme("fa-baby",               "#ec4899", "#f472b6", "#fce7f3"));

        // ===== Thú cưng =====
        KEYWORDS.put("thú cưng",             new Theme("fa-paw",                "#92400e", "#b45309", "#fef3c7"));
        KEYWORDS.put("chó",                  new Theme("fa-dog",                "#92400e", "#b45309", "#fef3c7"));
        KEYWORDS.put("mèo",                  new Theme("fa-cat",                "#92400e", "#b45309", "#fef3c7"));

        // ===== Đồ ăn & đồ uống =====
        KEYWORDS.put("đồ ăn",                new Theme("fa-utensils",           "#ea580c", "#f97316", "#ffedd5"));
        KEYWORDS.put("thực phẩm",            new Theme("fa-utensils",           "#ea580c", "#f97316", "#ffedd5"));
        KEYWORDS.put("đồ uống",              new Theme("fa-mug-hot",            "#92400e", "#b45309", "#fef3c7"));
        KEYWORDS.put("cafe",                 new Theme("fa-mug-hot",            "#92400e", "#b45309", "#fef3c7"));
        KEYWORDS.put("cà phê",               new Theme("fa-mug-hot",            "#92400e", "#b45309", "#fef3c7"));
        KEYWORDS.put("trà",                  new Theme("fa-mug-hot",            "#16a34a", "#22c55e", "#dcfce7"));
        KEYWORDS.put("bánh",                 new Theme("fa-cookie-bite",        "#d97706", "#f59e0b", "#fef3c7"));
        KEYWORDS.put("trái cây",             new Theme("fa-apple-whole",        "#dc2626", "#ef4444", "#fee2e2"));

        // ===== Xe cộ =====
        KEYWORDS.put("ô tô",                 new Theme("fa-car",                "#1e293b", "#475569", "#e2e8f0"));
        KEYWORDS.put("xe máy",               new Theme("fa-motorcycle",         "#dc2626", "#ef4444", "#fee2e2"));
        KEYWORDS.put("xe đạp",               new Theme("fa-bicycle",            "#16a34a", "#22c55e", "#dcfce7"));
        KEYWORDS.put("phụ tùng xe",          new Theme("fa-gauge-high",         "#475569", "#64748b", "#f1f5f9"));

        // ===== Nhạc cụ =====
        KEYWORDS.put("nhạc cụ",              new Theme("fa-guitar",             "#a855f7", "#c084fc", "#f3e8ff"));
        KEYWORDS.put("guitar",               new Theme("fa-guitar",             "#a855f7", "#c084fc", "#f3e8ff"));
        KEYWORDS.put("piano",                new Theme("fa-music",              "#1e293b", "#475569", "#e2e8f0"));

        // ===== Khác =====
        KEYWORDS.put("quà tặng",             new Theme("fa-gift",               "#dc2626", "#ef4444", "#fee2e2"));
        KEYWORDS.put("dịch vụ",              new Theme("fa-handshake",          "#0ea5e9", "#38bdf8", "#e0f2fe"));
        KEYWORDS.put("voucher",              new Theme("fa-ticket",             "#eab308", "#facc15", "#fef9c3"));
    }

    private static final Theme DEFAULT_THEME =
            new Theme("fa-tag", "#0fb5b0", "#14b8a6", "#ccfbf1");

    /**
     * Return the theme that best matches the given category name (case
     * insensitive, longest match wins because the map preserves insertion
     * order and we iterate from longest to shortest keyword below).
     */
    public Theme themeFor(Object categoryName) {
        if (categoryName == null) {
            return DEFAULT_THEME;
        }
        String name = String.valueOf(categoryName).toLowerCase(Locale.ROOT).trim();

        // First try the longest keyword that appears as a substring.
        String best = null;
        for (String keyword : KEYWORDS.keySet()) {
            if (name.contains(keyword) && (best == null || keyword.length() > best.length())) {
                best = keyword;
            }
        }
        return best != null ? KEYWORDS.get(best) : DEFAULT_THEME;
    }

    /** Convenience accessor for templates. */
    public String iconFor(Object categoryName) {
        return themeFor(categoryName).getIconClass();
    }

    /** Convenience accessor — CSS background value (e.g. linear-gradient(...)). */
    public String gradientFor(Object categoryName) {
        Theme t = themeFor(categoryName);
        return "linear-gradient(135deg, " + t.getGradientFrom() + " 0%, " + t.getGradientTo() + " 100%)";
    }

    /** Convenience accessor — soft tinted background (used as card backdrop). */
    public String softFor(Object categoryName) {
        return themeFor(categoryName).getSoft();
    }
}