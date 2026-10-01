package com.ecommerce.cnj70.enums;

/**
 * Trạng thái của một VendorSubscription.
 *
 * <ul>
 *   <li>{@link #ACTIVE}: đang có hiệu lực, chưa quá endDate</li>
 *   <li>{@link #EXPIRED}: scheduler đã expire vì quá endDate</li>
 *   <li>{@link #CANCELLED}: vendor chủ động hủy</li>
 * </ul>
 */
public enum SubscriptionStatus {
    ACTIVE,
    EXPIRED,
    CANCELLED
}
