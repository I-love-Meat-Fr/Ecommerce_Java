package com.ecommerce.cnj70.config;

import com.ecommerce.cnj70.document.PremiumPackage;
import com.ecommerce.cnj70.repository.PremiumPackageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Seed 3 gói Premium mẫu nếu collection rỗng.
 *
 * <p>Chạy idempotent — nếu collection đã có data thì skip để tránh ghi đè.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PremiumPackageSeeder implements CommandLineRunner {

    private final PremiumPackageRepository packageRepository;

    @Override
    public void run(String... args) {
        List<PremiumPackage> existing = packageRepository.findAll();
        if (!existing.isEmpty()) {
            log.info("PremiumPackageSeeder: collection đã có {} gói, skip seed", existing.size());
            return;
        }

        log.info("PremiumPackageSeeder: bắt đầu seed 3 gói Premium mẫu...");

        PremiumPackage p1 = PremiumPackage.builder()
                .name("Premium 1 tháng")
                .description("Hiển thị badge HOT trên sản phẩm ở trang chủ và trang sản phẩm.")
                .price(new BigDecimal("199000"))
                .durationDays(30)
                .priority(0)
                .active(true)
                .build();

        PremiumPackage p2 = PremiumPackage.builder()
                .name("Premium 3 tháng")
                .description("Tiết kiệm 8% so với mua lẻ. Phù hợp shop bán hàng theo mùa vụ.")
                .price(new BigDecimal("549000"))
                .durationDays(90)
                .priority(1)
                .active(true)
                .build();

        PremiumPackage p3 = PremiumPackage.builder()
                .name("Premium 12 tháng")
                .description("Tiết kiệm 17% so với mua lẻ. Dành cho shop cam kết kinh doanh lâu dài.")
                .price(new BigDecimal("1990000"))
                .durationDays(365)
                .priority(0)
                .active(true)
                .build();

        packageRepository.save(p1);
        packageRepository.save(p2);
        packageRepository.save(p3);

        log.info("PremiumPackageSeeder: đã seed 3 gói Premium (1 tháng, 3 tháng, 12 tháng)");
    }
}
