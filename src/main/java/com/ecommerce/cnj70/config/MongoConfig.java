package com.ecommerce.cnj70.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.mongo.MongoClientSettingsBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * PERFORMANCE #8 — Custom MongoClient tuning cho Spring Data Mongo.
 *
 * <p>Mặc định Spring Boot dùng connection pool size = 100, không có
 * timeout cho socket read/write và server selection. Với MongoDB Atlas
 * remote (cluster0.2pu37z6.mongodb.net), điều này dẫn đến:</p>
 * <ul>
 *   <li>Mỗi request home mở connection mới nếu pool cạn</li>
 *   <li>Socket read timeout mặc định 0 → treo vô hạn nếu Mongo
 *       chậm hoặc mạng chập chờn</li>
 *   <li>Server selection timeout mặc định 30s → user thấy "loading"
 *       quá lâu trước khi nhận error</li>
 * </ul>
 *
 * <p>Tune:</p>
 * <ul>
 *   <li>maxSize=50 (giảm từ 100, đủ cho ~50 concurrent request home + admin)</li>
 *   <li>minSize=5 (giữ warm pool để request đầu không phải open connection)</li>
 *   <li>maxWaitTime=3s (khi pool cạn, fail fast thay vì block thread)</li>
 *   <li>serverSelectionTimeout=3s</li>
 *   <li>connectTimeout=5s, readTimeout=10s</li>
 * </ul>
 */
@Configuration
public class MongoConfig {

    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;

    /**
     * PERFORMANCE #8 — Custom MongoClientSettings thay thế bean mặc định
     * của spring-boot-starter-data-mongodb. Spring sẽ pick up bean này
     * và apply các setting bên dưới.
     */
    @Bean
    public MongoClient mongoClient() {
        ConnectionString connectionString = new ConnectionString(mongoUri);

        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(connectionString)
                .applyToConnectionPoolSettings(pool -> pool
                                .maxSize(50)
                                .minSize(5)
                                .maxWaitTime(3000, TimeUnit.MILLISECONDS)
                                .maxConnectionIdleTime(60, TimeUnit.SECONDS)
                                .maxConnectionLifeTime(10, TimeUnit.MINUTES)
                        )
                .applyToSocketSettings(socket -> socket
                                .connectTimeout(5000, TimeUnit.MILLISECONDS)
                                .readTimeout(10000, TimeUnit.MILLISECONDS)
                        )
                .applyToClusterSettings(cluster -> cluster
                                .serverSelectionTimeout(3000, TimeUnit.MILLISECONDS)
                        )
                .applyToServerSettings(server -> server
                                .heartbeatFrequency(10, TimeUnit.SECONDS)
                                .minHeartbeatFrequency(500, TimeUnit.MILLISECONDS)
                        )
                .build();

        return MongoClients.create(settings);
    }

    /**
     * PERFORMANCE #8 — backup bean: thêm builder customizer để nếu user
     * override mongoClient bằng bean khác, vẫn có timeout áp dụng.
     * Spring Boot picks up cả hai cùng lúc.
     */
    @Bean
    public MongoClientSettingsBuilderCustomizer mongoSettingsCustomizer() {
        return builder -> builder
                .applyToConnectionPoolSettings(pool -> pool
                        .maxSize(50)
                        .minSize(5)
                        .maxWaitTime(3000, TimeUnit.MILLISECONDS))
                .applyToSocketSettings(socket -> socket
                        .connectTimeout(5000, TimeUnit.MILLISECONDS)
                        .readTimeout(10000, TimeUnit.MILLISECONDS))
                .applyToClusterSettings(cluster -> cluster
                        .serverSelectionTimeout(3000, TimeUnit.MILLISECONDS));
    }
}