package com.ecommerce.cnj70.config;

import com.ecommerce.cnj70.document.Product;
import com.ecommerce.cnj70.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@RequiredArgsConstructor
public class ImageSeeder implements CommandLineRunner {

    private final ProductRepository productRepository;

    private static final Map<String, String[]> CATEGORY_IMAGES = new HashMap<>();

    static {
        CATEGORY_IMAGES.put("Thời Trang Nữ", new String[]{
            "https://picsum.photos/seed/fashion1/400/400",
            "https://picsum.photos/seed/fashion2/400/400",
            "https://picsum.photos/seed/fashion3/400/400"
        });
        CATEGORY_IMAGES.put("Thời Trang Nam", new String[]{
            "https://picsum.photos/seed/men1/400/400",
            "https://picsum.photos/seed/men2/400/400",
            "https://picsum.photos/seed/men3/400/400"
        });
        CATEGORY_IMAGES.put("Thời Trang Trẻ Em", new String[]{
            "https://picsum.photos/seed/kids1/400/400",
            "https://picsum.photos/seed/kids2/400/400"
        });
        CATEGORY_IMAGES.put("Giày Dép Nữ", new String[]{
            "https://picsum.photos/seed/shoes1/400/400",
            "https://picsum.photos/seed/shoes2/400/400"
        });
        CATEGORY_IMAGES.put("Giày Dép Nam", new String[]{
            "https://picsum.photos/seed/menshoes1/400/400",
            "https://picsum.photos/seed/menshoes2/400/400"
        });
        CATEGORY_IMAGES.put("Túi Sách & Vali", new String[]{
            "https://picsum.photos/seed/bag1/400/400",
            "https://picsum.photos/seed/bag2/400/400"
        });
        CATEGORY_IMAGES.put("Phụ Kiện & Trang Sức", new String[]{
            "https://picsum.photos/seed/jewelry1/400/400",
            "https://picsum.photos/seed/jewelry2/400/400"
        });
        CATEGORY_IMAGES.put("Điện Thoại & Máy Tính Bảng", new String[]{
            "https://picsum.photos/seed/phone1/400/400",
            "https://picsum.photos/seed/phone2/400/400"
        });
        CATEGORY_IMAGES.put("Laptop & Máy Tính", new String[]{
            "https://picsum.photos/seed/laptop1/400/400",
            "https://picsum.photos/seed/laptop2/400/400"
        });
        CATEGORY_IMAGES.put("Máy Ảnh & Máy Quay Phim", new String[]{
            "https://picsum.photos/seed/camera1/400/400",
            "https://picsum.photos/seed/camera2/400/400"
        });
        CATEGORY_IMAGES.put("Tai Nghe & Loa", new String[]{
            "https://picsum.photos/seed/headphone1/400/400",
            "https://picsum.photos/seed/headphone2/400/400"
        });
        CATEGORY_IMAGES.put("Phụ Kiện Điện Tử", new String[]{
            "https://picsum.photos/seed/electronic1/400/400",
            "https://picsum.photos/seed/electronic2/400/400"
        });
        CATEGORY_IMAGES.put("Thiết Bị Đeo Tay Thông Minh", new String[]{
            "https://picsum.photos/seed/watch1/400/400",
            "https://picsum.photos/seed/watch2/400/400"
        });
        CATEGORY_IMAGES.put("Nội Thất", new String[]{
            "https://picsum.photos/seed/furniture1/400/400",
            "https://picsum.photos/seed/furniture2/400/400"
        });
        CATEGORY_IMAGES.put("Trang Trí Nhà Cửa", new String[]{
            "https://picsum.photos/seed/decor1/400/400",
            "https://picsum.photos/seed/decor2/400/400"
        });
        CATEGORY_IMAGES.put("Dụng Cụ Nấu Ăn", new String[]{
            "https://picsum.photos/seed/kitchen1/400/400",
            "https://picsum.photos/seed/kitchen2/400/400"
        });
        CATEGORY_IMAGES.put("Đồ Dùng Gia Đình", new String[]{
            "https://picsum.photos/seed/home1/400/400",
            "https://picsum.photos/seed/home2/400/400"
        });
        CATEGORY_IMAGES.put("Thiết Bị Vệ Sinh", new String[]{
            "https://picsum.photos/seed/clean1/400/400",
            "https://picsum.photos/seed/clean2/400/400"
        });
        CATEGORY_IMAGES.put("Chăm Sóc Da Mặt", new String[]{
            "https://picsum.photos/seed/skincare1/400/400",
            "https://picsum.photos/seed/skincare2/400/400"
        });
        CATEGORY_IMAGES.put("Trang Điểm", new String[]{
            "https://picsum.photos/seed/makeup1/400/400",
            "https://picsum.photos/seed/makeup2/400/400"
        });
        CATEGORY_IMAGES.put("Chăm Sóc Tóc", new String[]{
            "https://picsum.photos/seed/hair1/400/400",
            "https://picsum.photos/seed/hair2/400/400"
        });
        CATEGORY_IMAGES.put("Nước Hoa", new String[]{
            "https://picsum.photos/seed/perfume1/400/400",
            "https://picsum.photos/seed/perfume2/400/400"
        });
        CATEGORY_IMAGES.put("Thực Phẩm Chức Năng", new String[]{
            "https://picsum.photos/seed/supplement1/400/400",
            "https://picsum.photos/seed/supplement2/400/400"
        });
        CATEGORY_IMAGES.put("Thiết Bị Y Tế", new String[]{
            "https://picsum.photos/seed/medical1/400/400",
            "https://picsum.photos/seed/medical2/400/400"
        });
        CATEGORY_IMAGES.put("Đồ Cho Mẹ & Bé", new String[]{
            "https://picsum.photos/seed/baby1/400/400",
            "https://picsum.photos/seed/baby2/400/400"
        });
        CATEGORY_IMAGES.put("Đồ Chơi Trẻ Em", new String[]{
            "https://picsum.photos/seed/toy1/400/400",
            "https://picsum.photos/seed/toy2/400/400"
        });
        CATEGORY_IMAGES.put("Xe Đẩy & Nôi", new String[]{
            "https://picsum.photos/seed/stroller1/400/400",
            "https://picsum.photos/seed/stroller2/400/400"
        });
        CATEGORY_IMAGES.put("Thời Trang Thể Thao", new String[]{
            "https://picsum.photos/seed/sport1/400/400",
            "https://picsum.photos/seed/sport2/400/400"
        });
        CATEGORY_IMAGES.put("Dụng Cụ Thể Thao", new String[]{
            "https://picsum.photos/seed/sportsequip1/400/400",
            "https://picsum.photos/seed/sportsequip2/400/400"
        });
        CATEGORY_IMAGES.put("Vali & Du Lịch", new String[]{
            "https://picsum.photos/seed/luggage1/400/400",
            "https://picsum.photos/seed/luggage2/400/400"
        });
        CATEGORY_IMAGES.put("Xe Đạp & Xe Máy Điện", new String[]{
            "https://picsum.photos/seed/bike1/400/400",
            "https://picsum.photos/seed/bike2/400/400"
        });
        CATEGORY_IMAGES.put("Sách Tiếng Việt", new String[]{
            "https://picsum.photos/seed/book1/400/400",
            "https://picsum.photos/seed/book2/400/400"
        });
        CATEGORY_IMAGES.put("Sách Ngoại Văn", new String[]{
            "https://picsum.photos/seed/ebook1/400/400",
            "https://picsum.photos/seed/ebook2/400/400"
        });
        CATEGORY_IMAGES.put("Văn Phòng Phẩm", new String[]{
            "https://picsum.photos/seed/stationery1/400/400",
            "https://picsum.photos/seed/stationery2/400/400"
        });
        CATEGORY_IMAGES.put("Nhạc Cụ", new String[]{
            "https://picsum.photos/seed/music1/400/400",
            "https://picsum.photos/seed/music2/400/400"
        });
        CATEGORY_IMAGES.put("Phụ Tùng Ô Tô", new String[]{
            "https://picsum.photos/seed/car1/400/400",
            "https://picsum.photos/seed/car2/400/400"
        });
        CATEGORY_IMAGES.put("Phụ Tùng Xe Máy", new String[]{
            "https://picsum.photos/seed/motor1/400/400",
            "https://picsum.photos/seed/motor2/400/400"
        });
        CATEGORY_IMAGES.put("Đồ Chơi Ô Tô & Xe Máy", new String[]{
            "https://picsum.photos/seed/model1/400/400",
            "https://picsum.photos/seed/model2/400/400"
        });
        CATEGORY_IMAGES.put("Bánh Kẹo & Snacks", new String[]{
            "https://picsum.photos/seed/snack1/400/400",
            "https://picsum.photos/seed/snack2/400/400"
        });
        CATEGORY_IMAGES.put("Cà Phê & Trà", new String[]{
            "https://picsum.photos/seed/coffee1/400/400",
            "https://picsum.photos/seed/coffee2/400/400"
        });
        CATEGORY_IMAGES.put("Sữa & Nước Giải Khát", new String[]{
            "https://picsum.photos/seed/drink1/400/400",
            "https://picsum.photos/seed/drink2/400/400"
        });
        CATEGORY_IMAGES.put("Voucher & Dịch Vụ", new String[]{
            "https://picsum.photos/seed/voucher1/400/400",
            "https://picsum.photos/seed/voucher2/400/400"
        });
        CATEGORY_IMAGES.put("Vé Máy Bay & Du Lịch", new String[]{
            "https://picsum.photos/seed/travel1/400/400",
            "https://picsum.photos/seed/travel2/400/400"
        });
        CATEGORY_IMAGES.put("Sim & Thẻ Cào", new String[]{
            "https://picsum.photos/seed/sim1/400/400",
            "https://picsum.photos/seed/sim2/400/400"
        });
    }

    private static final String DEFAULT_IMAGE = "https://picsum.photos/seed/product/400/400";

    private static boolean hasRun = false;

    @Override
    public void run(String... args) {
        if (hasRun) {
            return;
        }
        hasRun = true;

        // Find products without images (null or empty)
        List<Product> productsWithoutImages = productRepository.findAll().stream()
                .filter(p -> p.getThumbnailUrl() == null || p.getThumbnailUrl().isEmpty())
                .toList();

        if (productsWithoutImages.isEmpty()) {
            log.info("Tất cả sản phẩm đã có ảnh!");
            return;
        }

        log.info("Đang cập nhật {} sản phẩm chưa có ảnh...", productsWithoutImages.size());

        AtomicInteger counter = new AtomicInteger(0);

        productsWithoutImages.forEach(product -> {
            String categoryName = product.getCategoryName();
            String imageUrl = getImageForCategory(categoryName, product.getId());

            product.setThumbnailUrl(imageUrl);
            product.setImageUrls(java.util.List.of(imageUrl));
            productRepository.save(product);

            counter.incrementAndGet();
        });

        log.info("Đã cập nhật ảnh cho {} sản phẩm!", counter.get());
    }

    private String getImageForCategory(String categoryName, String productId) {
        if (categoryName == null || categoryName.isEmpty()) {
            return DEFAULT_IMAGE;
        }

        String[] images = CATEGORY_IMAGES.get(categoryName);
        if (images == null || images.length == 0) {
            return DEFAULT_IMAGE;
        }

        int index = Math.abs(productId.hashCode()) % images.length;
        return images[index];
    }
}
