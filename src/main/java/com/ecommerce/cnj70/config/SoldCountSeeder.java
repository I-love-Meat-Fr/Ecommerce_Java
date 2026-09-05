package com.ecommerce.cnj70.config;

import com.ecommerce.cnj70.document.Product;
import com.ecommerce.cnj70.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * One-shot seeder that fills a realistic Shopee-style "Đã bán" (sold) value
 * for any product that does not yet have one. Runs idempotently: products
 * with sold > 0 are skipped, so it is safe to restart the application.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SoldCountSeeder implements CommandLineRunner {

    private final ProductRepository productRepository;

    private static boolean hasRun = false;

    @Override
    public void run(String... args) {
        if (hasRun) {
            return;
        }
        hasRun = true;

        List<Product> all = productRepository.findAll();
        if (all.isEmpty()) {
            log.info("Không có sản phẩm nào, bỏ qua SoldCountSeeder");
            return;
        }

        // Deterministic seed so sold counts stay stable between restarts.
        Random random = new Random(42L);
        AtomicInteger updated = new AtomicInteger(0);

        for (Product product : all) {
            if (product.getSold() > 0) {
                continue;
            }
            // Bias towards realistic values: 80% under 1k, 18% 1k-9.9k, 2% 10k+.
            int bucket = random.nextInt(100);
            int sold;
            if (bucket < 80) {
                sold = 10 + random.nextInt(990);          // 10 .. 999
            } else if (bucket < 98) {
                sold = 1_000 + random.nextInt(9_000);      // 1.0k .. 9.9k
            } else {
                sold = 10_000 + random.nextInt(40_000);    // 10k+ .. 49k+
            }
            product.setSold(sold);
            productRepository.save(product);
            updated.incrementAndGet();
        }

        if (updated.get() > 0) {
            log.info("Đã seed số 'Đã bán' cho {} sản phẩm.", updated.get());
        } else {
            log.info("Tất cả sản phẩm đã có số 'Đã bán', bỏ qua.");
        }
    }
}
