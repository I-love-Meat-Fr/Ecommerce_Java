package com.ecommerce.cnj70.document;

import com.ecommerce.cnj70.enums.AccountStatus;
import com.ecommerce.cnj70.enums.KycStatus;
import com.ecommerce.cnj70.enums.UserRole;
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
@Document(collection = "users")
public class User {
    
    @Id
    private String id;
    
    @Indexed(unique = true)
    private String email;
    
    private String password;
    
    private String fullName;
    
    private String phone;
    
    private String address;
    
    private UserRole role;
    
    @Builder.Default
    private AccountStatus status = AccountStatus.UNVERIFIED;
    
    private String shopId;
    
    private String avatarUrl;
    
    /** Trạng thái KYC (denormalized từ KycProfile để check nhanh) */
    @Builder.Default
    private KycStatus kycStatus = KycStatus.NOT_SUBMITTED;
    
    @CreatedDate
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    private LocalDateTime updatedAt;

    /** Vendor đã hoàn tất xác minh KYC chưa? */
    public boolean isKycApproved() {
        return kycStatus == KycStatus.APPROVED;
    }
}

