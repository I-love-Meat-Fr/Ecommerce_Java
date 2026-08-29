package com.ecommerce.cnj70.document;

import com.ecommerce.cnj70.enums.ShopStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "shops")
public class Shop {

    @Id
    private String id;

    private String ownerId;

    @Indexed(unique = true)
    private String shopName;

    private String description;

    private String logoUrl;

    private String bannerUrl;

    @Builder.Default
    private ShopStatus status = ShopStatus.PENDING;

    @Builder.Default
    private boolean active = true;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public boolean isVerified() {
        return status == ShopStatus.APPROVED;
    }

    public ShopStatus getStatus() {
        return status != null ? status : ShopStatus.PENDING;
    }

    public void setVerified(boolean verified) {
        this.status = verified ? ShopStatus.APPROVED : ShopStatus.PENDING;
    }
}
