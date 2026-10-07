package com.ecommerce.cnj70.controller.api;

import com.ecommerce.cnj70.dto.vietqr.VietQRPaymentResponse;
import com.ecommerce.cnj70.dto.vietqr.VietQRWebhookPayload;
import com.ecommerce.cnj70.exception.BadRequestException;
import com.ecommerce.cnj70.service.VietQRService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Controller xử lý VietQR API endpoints.
 * 
 * <p>Endpoints:
 * <ul>
 *   <li>POST /api/vietqr/create - Tạo QR code thanh toán</li>
 *   <li>POST /api/vietqr/webhook - Nhận callback từ VietQR</li>
 *   <li>GET /api/vietqr/status/{orderId} - Kiểm tra trạng thái thanh toán</li>
 *   <li>GET /api/vietqr/{transactionId}/download - Download QR image</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/vietqr")
@RequiredArgsConstructor
public class VietQRController {

    private final VietQRService vietQRService;
    private final ObjectMapper objectMapper;

    /**
     * Tạo QR code thanh toán cho đơn hàng.
     * 
     * <p>POST /api/vietqr/create
     * Body: { "orderId": "...", "amount": 150000 }
     */
    @PostMapping("/create")
    public ResponseEntity<?> createPaymentQR(@RequestBody Map<String, Object> request) {
        try {
            String orderId = (String) request.get("orderId");
            Object amountObj = request.get("amount");
            
            if (orderId == null || orderId.isBlank()) {
                throw new BadRequestException("Order ID không hợp lệ");
            }
            
            BigDecimal amount;
            if (amountObj instanceof Number) {
                amount = BigDecimal.valueOf(((Number) amountObj).longValue());
            } else if (amountObj instanceof String) {
                amount = new BigDecimal((String) amountObj);
            } else {
                throw new BadRequestException("Số tiền không hợp lệ");
            }
            
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestException("Số tiền phải lớn hơn 0");
            }
            
            VietQRPaymentResponse response = vietQRService.createPaymentQR(orderId, amount);
            return ResponseEntity.ok(response);
            
        } catch (BadRequestException e) {
            log.warn("Bad request creating VietQR: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Error creating VietQR", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Không thể tạo QR thanh toán: " + e.getMessage()
            ));
        }
    }

    /**
     * Webhook endpoint nhận callback từ VietQR.
     * 
     * <p>POST /api/vietqr/webhook
     * 
     * <p>VietQR sẽ gọi endpoint này khi có thanh toán vào tài khoản.
     * Cần đăng ký webhook URL trong VietQR Developer Portal.
     * 
     * <p><b>Lưu ý:</b> Endpoint này nên được verify signature nếu VietQR hỗ trợ.
     */
    @PostMapping(value = "/webhook", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> handleWebhook(@RequestBody String payload) {
        try {
            log.info("Received VietQR webhook: {}", payload);
            
            VietQRWebhookPayload webhookPayload = objectMapper.readValue(
                    payload, VietQRWebhookPayload.class);
            
            boolean success = vietQRService.handleWebhook(webhookPayload);
            
            if (success) {
                return ResponseEntity.ok(Map.of(
                        "code", "00",
                        "message", "Webhook processed successfully"
                ));
            } else {
                return ResponseEntity.ok(Map.of(
                        "code", "01",
                        "message", "Webhook processing failed"
                ));
            }
            
        } catch (Exception e) {
            log.error("Error processing VietQR webhook", e);
            // Vẫn trả 200 để VietQR không retry
            return ResponseEntity.ok(Map.of(
                    "code", "01",
                    "message", "Error: " + e.getMessage()
            ));
        }
    }

    /**
     * Kiểm tra trạng thái thanh toán của đơn hàng.
     * 
     * <p>GET /api/vietqr/status/{orderId}
     */
    @GetMapping("/status/{orderId}")
    public ResponseEntity<?> checkPaymentStatus(@PathVariable String orderId) {
        try {
            boolean paid = vietQRService.isOrderPaid(orderId);
            return ResponseEntity.ok(Map.of(
                    "orderId", orderId,
                    "paid", paid
            ));
        } catch (Exception e) {
            log.error("Error checking payment status for orderId: {}", orderId, e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Không thể kiểm tra trạng thái thanh toán"
            ));
        }
    }

    /**
     * Query trạng thái từ VietQR API.
     * 
     * <p>GET /api/vietqr/query/{transactionId}
     */
    @GetMapping("/query/{transactionId}")
    public ResponseEntity<?> queryTransaction(@PathVariable String transactionId) {
        try {
            boolean paid = vietQRService.queryPaymentStatus(transactionId);
            return ResponseEntity.ok(Map.of(
                    "transactionId", transactionId,
                    "paid", paid
            ));
        } catch (Exception e) {
            log.error("Error querying VietQR transaction: {}", transactionId, e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Không thể query giao dịch"
            ));
        }
    }

    /**
     * Health check endpoint.
     */
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(Map.of(
                "status", "OK",
                "service", "VietQR API"
        ));
    }
}
