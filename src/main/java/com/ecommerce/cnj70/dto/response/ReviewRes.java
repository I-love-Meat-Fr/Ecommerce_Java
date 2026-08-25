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
public class ReviewRes {
    
    private String id;
    private String productId;
    private String userId;
    private String userName;
    private String userAvatar;
    private int rating;
    private String comment;
    private LocalDateTime createdAt;
}
