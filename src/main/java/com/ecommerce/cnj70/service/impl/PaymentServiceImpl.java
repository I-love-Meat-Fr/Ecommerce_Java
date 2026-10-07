package com.ecommerce.cnj70.service.impl;

import com.ecommerce.cnj70.document.Payment;
import com.ecommerce.cnj70.dto.vietqr.VietQRPaymentResponse;
import com.ecommerce.cnj70.enums.PaymentMethod;
import com.ecommerce.cnj70.enums.PaymentStatus;
import com.ecommerce.cnj70.repository.PaymentRepository;
import com.ecommerce.cnj70.service.PaymentService;
import com.ecommerce.cnj70.service.VietQRService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Implementation của PaymentService.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final VietQRService vietQRService;

    @Override
    public Payment createPayment(String orderId, BigDecimal amount, PaymentMethod paymentMethod) {
        log.info("Creating payment for orderId={}, amount={}, method={}",
                orderId, amount, paymentMethod);

        // Kiểm tra đã có payment chưa
        Optional<Payment> existing = paymentRepository.findByOrderIdAndPaymentMethod(
                orderId, paymentMethod);
        if (existing.isPresent()) {
            log.warn("Payment already exists for orderId={}, method={}",
                    orderId, paymentMethod);
            return existing.get();
        }

        Payment payment = Payment.builder()
                .orderId(orderId)
                .amount(amount)
                .paymentMethod(paymentMethod)
                .status(PaymentStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        return paymentRepository.save(payment);
    }

    @Override
    public VietQRPaymentResponse createVietQRPayment(String orderId, BigDecimal amount) {
        log.info("Creating VietQR payment for orderId={}, amount={}", orderId, amount);

        // Tạo payment record trước
        createPayment(orderId, amount, PaymentMethod.BANK_QR);

        // Gọi VietQR API để tạo QR
        return vietQRService.createPaymentQR(orderId, amount);
    }

    @Override
    public boolean confirmPayment(String orderId) {
        log.info("Confirming payment for orderId={}", orderId);

        Optional<Payment> paymentOpt = paymentRepository.findByOrderId(orderId);
        if (paymentOpt.isEmpty()) {
            log.error("Payment not found for orderId={}", orderId);
            return false;
        }

        Payment payment = paymentOpt.get();
        if (payment.getStatus() == PaymentStatus.PAID) {
            log.info("Payment already confirmed for orderId={}", orderId);
            return true;
        }

        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(LocalDateTime.now());
        paymentRepository.save(payment);

        log.info("Payment confirmed for orderId={}", orderId);
        return true;
    }

    @Override
    public void failPayment(String orderId, String message) {
        log.warn("Payment failed for orderId={}, reason={}", orderId, message);

        paymentRepository.findByOrderId(orderId).ifPresent(payment -> {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setErrorMessage(message);
            paymentRepository.save(payment);
        });
    }

    @Override
    public void updatePaymentStatus(String orderId, PaymentStatus newStatus) {
        log.info("Updating payment status for orderId={} to {}", orderId, newStatus);

        paymentRepository.findByOrderId(orderId).ifPresent(payment -> {
            payment.setStatus(newStatus);
            
            if (newStatus == PaymentStatus.PAID) {
                payment.setPaidAt(LocalDateTime.now());
            } else if (newStatus == PaymentStatus.REFUNDED) {
                payment.setRefundedAt(LocalDateTime.now());
            }
            
            paymentRepository.save(payment);
        });
    }

    @Override
    public Optional<Payment> getPaymentByOrderId(String orderId) {
        return paymentRepository.findByOrderId(orderId);
    }

    @Override
    public Optional<Payment> getPaymentByOrderIdAndMethod(String orderId, PaymentMethod paymentMethod) {
        return paymentRepository.findByOrderIdAndPaymentMethod(orderId, paymentMethod);
    }

    @Override
    public boolean isPaid(String orderId) {
        return paymentRepository.existsByOrderIdAndStatus(orderId, PaymentStatus.PAID);
    }
}
