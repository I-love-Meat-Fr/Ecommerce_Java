package com.ecommerce.cnj70.repository;

import com.ecommerce.cnj70.document.Payment;
import com.ecommerce.cnj70.enums.PaymentMethod;
import com.ecommerce.cnj70.enums.PaymentStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository cho Payment document.
 */
@Repository
public interface PaymentRepository extends MongoRepository<Payment, String> {

    /**
     * Tìm payment theo order ID.
     */
    Optional<Payment> findByOrderId(String orderId);

    /**
     * Tìm payment theo order ID và phương thức thanh toán.
     */
    Optional<Payment> findByOrderIdAndPaymentMethod(String orderId, PaymentMethod paymentMethod);

    /**
     * Tìm payment theo VietQR transaction ID.
     */
    Optional<Payment> findByVietqrTransactionId(String vietqrTransactionId);

    /**
     * Tìm payment theo VNPAY transaction ID.
     */
    Optional<Payment> findByVnpTransactionId(String vnpTransactionId);

    /**
     * Tìm tất cả payments theo order ID.
     */
    List<Payment> findAllByOrderId(String orderId);

    /**
     * Tìm payments theo trạng thái.
     */
    List<Payment> findByStatus(PaymentStatus status);

    /**
     * Tìm payments theo trạng thái và phương thức.
     */
    List<Payment> findByStatusAndPaymentMethod(PaymentStatus status, PaymentMethod paymentMethod);

    /**
     * Kiểm tra order đã được thanh toán chưa.
     */
    boolean existsByOrderIdAndStatus(String orderId, PaymentStatus status);
}
