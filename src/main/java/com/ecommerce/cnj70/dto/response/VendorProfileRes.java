package com.ecommerce.cnj70.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendorProfileRes {
    
    private String id;
    private String email;
    private String fullName;
    private String phone;
    private String address;
    private String avatarUrl;
    private String role;
    private String status;
    private ShopInfo shop;
    private LocalDateTime createdAt;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShopInfo {
        private String id;
        private String shopName;
        private String description;
        private String logoUrl;
        private String bannerUrl;
        private boolean verified;
        private boolean active;
        private LocalDateTime createdAt;
    }
}
