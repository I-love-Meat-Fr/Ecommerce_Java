package com.ecommerce.cnj70.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "reviews")
public class Review {
    
    @Id
    private String id;
    
    @Indexed
    private String productId;
    
    @Indexed
    private String userId;
    
    private String userName;
    
    private String userAvatar;
    
    private int rating;
    
    private String comment;
    
    @CreatedDate
    private LocalDateTime createdAt;
}
