package com.ecommerce.cnj70.document;

import com.ecommerce.cnj70.enums.SubscriptionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Bản ghi subscription Premium của một Shop cụ thể.
 *
 * <p>Mỗi lần vendor "Mua" / "Gia hạn" sẽ tạo (hoặc cập nhật) 1 record.
 * Nếu shop đã có sub ACTIVE thì cộng dồn {@code endDate} thay vì tạo mới.</p>
 *
 * <p>NOTE về payment: Hiện tại dùng {@code paymentMethod = "MOCK_WALLET"} —
 * chưa tích hợp gateway thật. Khi chuyển production cần thay bằng VNPay/MoMo
 * và thêm trạng thái PENDING_PAYMENT.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "vendor_subscriptions")
public class VendorSubscription {

    @Id
    private String id;

    @Indexed
    private String shopId;

    @Indexed
    private String packageId;

    /** Snapshot tên gói tại thời điểm mua — tránh phụ thuộc vào PremiumPackage sau này. */
    private String packageName;

    /** Snapshot số ngày của gói (để log/audit). */
    private int durationDays;

    /** Số tiền đã thanh toán (snapshot). */
    private BigDecimal amountPaid;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    @Builder.Default
    private SubscriptionStatus status = SubscriptionStatus.ACTIVE;

    /**
     * Phương thức thanh toán. Hiện tại chỉ hỗ trợ "MOCK_WALLET" (mock nội bộ).
     * Sau này sẽ thêm "VNPAY", "MOMO".
     */
    private String paymentMethod;

    /** UUID của transaction. */
    private String paymentTransactionId;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    /**
     * Subscription còn hiệu lực không (status ACTIVE và chưa quá endDate).
     */
    public boolean isActive() {
        return status == SubscriptionStatus.ACTIVE
                && endDate != null
                && LocalDateTime.now().isBefore(endDate);
    }
}
