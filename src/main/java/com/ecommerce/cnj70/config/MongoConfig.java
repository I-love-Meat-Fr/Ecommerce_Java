package com.ecommerce.cnj70.config;

import com.ecommerce.cnj70.enums.DiscountType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

import java.util.List;

@Slf4j
@Configuration
@EnableMongoAuditing
public class MongoConfig {

    /**
     * Ánh xạ giá trị cũ "FIXED" (và các giá trị không xác định) sang AMOUNT
     * khi đọc document Voucher từ MongoDB. Tránh ném IllegalArgumentException
     * "No enum constant DiscountType.FIXED" khi có dữ liệu legacy.
     */
    @Bean
    public MongoCustomConversions mongoCustomConversions() {
        return new MongoCustomConversions(List.of(new DiscountTypeReadingConverter()));
    }

    @ReadingConverter
    static class DiscountTypeReadingConverter implements Converter<String, DiscountType> {
        @Override
        public DiscountType convert(String source) {
            if (source == null || source.isBlank()) {
                return null;
            }
            try {
                return DiscountType.valueOf(source);
            } catch (IllegalArgumentException ex) {
                // FIXED là tên cũ trước khi enum đổi sang AMOUNT -> map sang AMOUNT
                if ("FIXED".equalsIgnoreCase(source)) {
                    log.warn("Mapping legacy discountType '{}' -> AMOUNT", source);
                    return DiscountType.AMOUNT;
                }
                log.warn("Unknown discountType '{}', defaulting to AMOUNT", source);
                return DiscountType.AMOUNT;
            }
        }
    }
}
