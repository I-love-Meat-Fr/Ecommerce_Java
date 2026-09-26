package com.ecommerce.cnj70.migration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * TASK #27.0 — Bootstrap runner: chạy migrations khi application start.
 *
 * <p>Quy ước:
 * <ul>
 *   <li>{@code @Order(0)} để chạy SAU Spring khởi tạo beans nhưng TRƯỚC web server
 *       bắt đầu nhận request.</li>
 *   <li>CHỈ chạy khi Spring profile KHÔNG phải test (để không kéo migration vào
 *       unit test).</li>
 *   <li>Idempotent — chạy nhiều lần an toàn (xem {@link DataMigrationRunner}).</li>
 * </ul>
 */
@Slf4j
@Component
@Order(0)
@RequiredArgsConstructor
public class MigrationBootstrap implements ApplicationRunner {

    private final DataMigrationRunner migrationRunner;

    @Override
    public void run(ApplicationArguments args) {
        log.info("[MigrationBootstrap] Starting data migration...");
        long start = System.currentTimeMillis();
        migrationRunner.runAll();
        log.info("[MigrationBootstrap] Data migration completed in {} ms",
                System.currentTimeMillis() - start);
    }
}
