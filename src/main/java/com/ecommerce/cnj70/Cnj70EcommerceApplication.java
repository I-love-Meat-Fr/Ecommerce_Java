package com.ecommerce.cnj70;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Cnj70EcommerceApplication.
 *
 * Note: @EnableMongoAuditing đã được khai báo ở MongoConfig (để có thể cấu hình
 * custom conversion chung với auditing). KHÔNG đặt @EnableMongoAuditing ở đây
 * để tránh bean definition override conflict.
 */
@SpringBootApplication
public class Cnj70EcommerceApplication {

    public static void main(String[] args) {
        SpringApplication.run(Cnj70EcommerceApplication.class, args);
    }
}
