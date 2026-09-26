package com.ecommerce.cnj70.config;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Helper tập trung các hằng số + pool dữ liệu (tên Việt, brand, sản phẩm, địa chỉ...)
 * + các tiện ích sinh số/chuỗi ngẫu nhiên deterministic.
 *
 * <p>Dùng chung cho {@link ProductionDataSeeder}. Seed cố định = 42 để dữ liệu
 * reproducible. Khoảng thời gian mặc định: 2021-09-01 → 2026-09-26 (5 năm).</p>
 */
public final class SeedPools {

    private SeedPools() {}

    public static final long SEED = 42L;
    public static final int BATCH_SIZE = 100;

    public static final LocalDateTime T_START = LocalDateTime.of(2021, 9, 1, 0, 0);
    public static final LocalDateTime T_END   = LocalDateTime.of(2026, 9, 26, 23, 59);
    public static final long T_START_EPOCH = T_START.toEpochSecond(ZoneOffset.UTC);
    public static final long T_END_EPOCH   = T_END.toEpochSecond(ZoneOffset.UTC);

    // ===== Họ tên Việt =====
    public static final String[] HO = {
        "Nguyễn","Trần","Lê","Phạm","Hoàng","Vũ","Đặng","Bùi","Đỗ","Hồ","Ngô","Dương","Lý"
    };
    public static final String[] TEN_NAM = {
        "An","Anh","Bảo","Bình","Châu","Chiến","Cường","Đạt","Đức","Hải","Hùng","Khang","Khánh",
        "Lâm","Long","Minh","Nam","Phong","Phú","Phúc","Quân","Quốc","Sơn","Tâm","Tân","Thắng",
        "Thiện","Trung","Tú","Tùng","Việt","Vũ","Xuân","Bách","Bảo","Cao","Đăng","Gia","Hào","Hiếu"
    };
    public static final String[] TEN_NU = {
        "An","Ánh","Bích","Châu","Diệu","Hà","Hạnh","Hoa","Hồng","Huyền","Hương","Khanh","Lan",
        "Linh","Mai","My","Ngân","Ngọc","Nhi","Như","Phương","Quỳnh","Thảo","Thu","Thư","Trang",
        "Trinh","Tú","Uyên","Vân","Yến","Diễm","Giang","Hằng","Khánh","Kim","Lam","Ly","Mỹ","Na"
    };

    public static final String[] CITIES = {
        "Hà Nội","TP.HCM","Đà Nẵng","Hải Phòng","Cần Thơ","Biên Hoà","Nha Trang","Huế",
        "Bình Dương","Đồng Nai","Quảng Ninh","Bắc Ninh","Hải Dương","Nam Định","Thái Bình",
        "Vinh","Buôn Ma Thuột","Pleiku","Quy Nhơn","Long Xuyên","Mỹ Tho","Rạch Giá","Hạ Long",
        "Thanh Hoá","Hà Nam"
    };
    public static final String[] DISTRICTS = {
        "Quận 1","Quận 3","Quận Bình Thạnh","Quận Gò Vấp","Quận Tân Bình","Quận Phú Nhuận",
        "Quận Hai Bà Trưng","Quận Đống Đa","Quận Cầu Giấy","Quận Thanh Xuân","Quận Hoàng Mai",
        "Quận Long Biên","Quận Ba Đình","Quận Hoàn Kiếm","Quận Tây Hồ","Quận Nam Từ Liêm",
        "Quận Bắc Từ Liêm","Huyện Gia Lâm","Huyện Đông Anh","Huyện Sóc Sơn"
    };
    public static final String[] STREETS = {
        "Nguyễn Huệ","Lê Lợi","Trần Hưng Đạo","Nguyễn Trãi","Lý Tự Trọng","Hai Bà Trưng",
        "Phan Đình Phùng","Điện Biên Phủ","Cách Mạng Tháng 8","Nguyễn Văn Cừ","Lê Hồng Phong",
        "Trường Chinh","Phạm Văn Đồng","Võ Văn Tần","Nguyễn Thái Học","Hoàng Diệu",
        "Phan Bội Châu","Lý Thường Kiệt","Tôn Đức Thắng","Trần Phú","Nguyễn Du","Bạch Đằng"
    };

    // ===== Brand / Sản phẩm =====
    public static final String[] BRANDS = {
        "Samsung","Apple","Sony","LG","Xiaomi","Oppo","Vivo","Realme","Dell","HP","Asus","Acer",
        "Lenovo","MSI","JBL","Logitech","Razer","Canon","Nikon","GoPro","Nike","Adidas","Puma",
        "Uniqlo","Zara","H&M","Levis","L'Oreal","Maybelline","Innisfree","The Ordinary","IKEA",
        "Panasonic","Philips","Sharp","Toshiba","Electrolux","Lock&Lock","Elmich","Kangaroo",
        "Sunhouse","Bosch","Midea","TCL","Aukey","Anker","Baseus","Remax","HyperX","SteelSeries"
    };

    /**
     * Whitelist các category thuộc nhóm "Đồ công nghệ" — Shop trên CNJ70 chỉ bán
     * sản phẩm thuộc nhóm này (điện thoại, laptop, máy ảnh, gaming, ...).
     * Dùng trong {@code ProductionDataSeeder} để lọc category khi seed product.
     *
     * <p>Tên trùng với các entry trong {@code seedCategoriesBatch} của
     * ProductionDataSeeder (do đó khi đổi tên category phải đổi cả ở đây).</p>
     */
    public static final java.util.List<String> TECH_CATEGORY_NAMES = java.util.List.of(
        "PC & Máy tính bàn",
        "Laptop",
        "Điện thoại",
        "Máy tính bảng",
        "Linh kiện máy tính",
        "Màn hình",
        "Thiết bị âm thanh",
        "Phụ kiện công nghệ",
        "Thiết bị mạng",
        "Đồng hồ thông minh"
    );
    public static final String[] PRODUCT_NOUNS = {
        "Điện thoại","Laptop","Tai nghe","Loa","Chuột","Bàn phím","Màn hình","Máy ảnh","Ốp lưng",
        "Sạc dự phòng","Cáp sạc","Đồng hồ","Kính","Túi xách","Ví","Giày","Dép","Áo","Quần","Váy",
        "Đầm","Son","Phấn","Kem","Sữa rửa","Dầu gội","Nước hoa","Ghế","Bàn","Giường","Tủ","Chăn",
        "Gối","Đèn","Quạt","Nồi","Chảo","Bình","Ly","Đũa","Bát","Đồ chơi","Sách","Vở","Bút",
        "Balo","Vali","Mũ","Khẩu trang","Kem đánh răng","Bàn chải","Máy sấy","Máy giặt",
        "Tủ lạnh","Điều hòa","Tivi","Nồi cơm","Ấm","Bếp","Lò vi sóng","Máy xay","Máy ép","Bình đun"
    };
    public static final String[] PRODUCT_ADJ = {
        "thông minh","cao cấp","tiện lợi","sang trọng","hiện đại","chính hãng","mini","đa năng",
        "không dây","chống nước","giá rẻ","bền bỉ","nhẹ","thời trang","trẻ trung","năng động"
    };

    public static final String[] BANKS = {
        "Vietcombank","Techcombank","BIDV","VietinBank","ACB","MBBank","Sacombank","TPBank",
        "VPBank","HDBank","Agribank","SHB"
    };

    // ===== Helpers =====

    public static String uuid() {
        return UUID.randomUUID().toString();
    }

    /** Random LocalDateTime trong [T_START, T_END]. */
    public static LocalDateTime randomDateTime(java.util.Random rng) {
        long span = T_END_EPOCH - T_START_EPOCH;
        long ts = T_START_EPOCH + (long) (rng.nextDouble() * span);
        return LocalDateTime.ofEpochSecond(ts, 0, ZoneOffset.UTC);
    }

    /** Random LocalDateTime trong khoảng [from, to]. */
    public static LocalDateTime randomDateTimeBetween(java.util.Random rng, LocalDateTime from, LocalDateTime to) {
        long a = from.toEpochSecond(ZoneOffset.UTC);
        long b = to.toEpochSecond(ZoneOffset.UTC);
        if (b <= a) return from;
        long ts = a + (long) (rng.nextDouble() * (b - a));
        return LocalDateTime.ofEpochSecond(ts, 0, ZoneOffset.UTC);
    }

    /** Random BigDecimal trong [min, max] với 2 chữ số thập phân (VND). */
    public static BigDecimal randomVnd(java.util.Random rng, long min, long max) {
        long v = min + (long) (rng.nextDouble() * (max - min));
        return BigDecimal.valueOf(v).setScale(0, RoundingMode.HALF_UP);
    }

    /** Random BigDecimal có 2 chữ số thập phân (%). */
    public static BigDecimal randomPct(java.util.Random rng, double min, double max) {
        double v = min + rng.nextDouble() * (max - min);
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP);
    }

    public static String pick(java.util.Random rng, String[] arr) {
        return arr[rng.nextInt(arr.length)];
    }

    public static <T> T pick(java.util.Random rng, java.util.List<T> list) {
        return list.get(rng.nextInt(list.size()));
    }

    public static <T> T pick(java.util.Random rng, T[] arr) {
        return arr[rng.nextInt(arr.length)];
    }

    public static String phoneVN(java.util.Random rng) {
        String[] prefixes = {"09","08","07","03","05"};
        StringBuilder sb = new StringBuilder(prefixes[rng.nextInt(prefixes.length)]);
        for (int i = 0; i < 8; i++) sb.append(rng.nextInt(10));
        return sb.toString();
    }

    public static String vietnameseAddress(java.util.Random rng) {
        return String.format("%d %s, %s, %s",
            1 + rng.nextInt(500),
            pick(rng, STREETS),
            pick(rng, DISTRICTS),
            pick(rng, CITIES));
    }

    public static String productName(java.util.Random rng) {
        return pick(rng, BRANDS) + " " + pick(rng, PRODUCT_NOUNS) + " " + pick(rng, PRODUCT_ADJ);
    }
}
