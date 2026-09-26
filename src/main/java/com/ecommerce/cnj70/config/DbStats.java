package com.ecommerce.cnj70.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Quick CLI tool — in số bản ghi của từng collection trong MongoDB Atlas.
 * <p>Cách dùng:
 * <pre>
 * mvn -q exec:java -Dexec.mainClass="com.ecommerce.cnj70.config.DbStats"
 * </pre>
 * Hoặc sau khi compile:
 * <pre>
 * java -cp "target/classes;target/dependency/*" com.ecommerce.cnj70.config.DbStats
 * </pre>
 * </p>
 */
public final class DbStats {

    public static void main(String[] args) {
        // Đọc URI từ env / system property
        String uri = System.getenv("SPRING_DATA_MONGODB_URI");
        if (uri == null || uri.isBlank()) {
            uri = System.getProperty("spring.data.mongodb.uri");
        }
        if (uri == null || uri.isBlank()) {
            uri = System.getenv("MONGODB_URI");
        }
        if (uri == null || uri.isBlank()) {
            uri = "mongodb://127.0.0.1:27017/cnj70_ecommerce";
        }

        String dbName = System.getenv("MONGO_DB");
        if (dbName == null || dbName.isBlank()) {
            dbName = System.getProperty("spring.data.mongodb.database", "cnj70_ecommerce");
        }

        List<String> cols = Arrays.asList(
            "users","shops","products","categories","orders","reviews","carts","vouchers",
            "banners","kyc_profiles","complaints","returns","refunds","report_cases",
            "moderation_history","violations","escalations","legal_documents","audit_logs",
            "scheduler_idempotency"
        );

        System.out.println("MongoDB URI: " + uri.replaceAll(":[^@/]+@", ":***@"));
        System.out.println("Database:    " + dbName);
        System.out.println("----- DB SUMMARY -----");

        try (MongoClient client = MongoClients.create(uri)) {
            MongoDatabase db = client.getDatabase(dbName);
            for (String name : cols) {
                try {
                    long n = db.getCollection(name).countDocuments();
                    System.out.printf("  %-22s = %d%n", name, n);
                } catch (Exception e) {
                    System.out.printf("  %-22s = (n/a: %s)%n", name, e.getMessage());
                }
            }
            System.out.println("----- TOTAL -----");
            long total = 0;
            for (String name : cols) {
                try { total += db.getCollection(name).countDocuments(); } catch (Exception ignored) {}
            }
            System.out.printf("  %d bản ghi / %d collection%n", total, cols.size());
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private DbStats() {}
}
