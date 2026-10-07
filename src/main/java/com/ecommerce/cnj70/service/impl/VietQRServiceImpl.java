package com.ecommerce.cnj70.service.impl;

import com.ecommerce.cnj70.config.VietQRProperties;
import com.ecommerce.cnj70.document.Payment;
import com.ecommerce.cnj70.dto.vietqr.VietQRCreateRequest;
import com.ecommerce.cnj70.dto.vietqr.VietQRCreateResponse;
import com.ecommerce.cnj70.dto.vietqr.VietQRPaymentResponse;
import com.ecommerce.cnj70.dto.vietqr.VietQRWebhookPayload;
import com.ecommerce.cnj70.enums.PaymentMethod;
import com.ecommerce.cnj70.enums.PaymentStatus;
import com.ecommerce.cnj70.exception.BusinessException;
import com.ecommerce.cnj70.repository.PaymentRepository;
import com.ecommerce.cnj70.repository.UserRepository;
import com.ecommerce.cnj70.service.VietQRService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

/**
 * Implementation của VietQR Service.
 * 
 * <p>Tích hợp với VietQR API để:
 * <ul>
 *   <li>Tạo QR code thanh toán cho đơn hàng</li>
 *   <li>Xử lý webhook callback khi có thanh toán</li>
 *   <li>Query trạng thái thanh toán</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VietQRServiceImpl implements VietQRService {

    private static final String TRANSFER_PREFIX = "THANH TOAN DH ";

    private final VietQRProperties vietqrProperties;
    private final RestTemplate restTemplate;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Override
    public VietQRPaymentResponse createPaymentQR(String orderId, BigDecimal amount) {
        log.info("Creating VietQR for orderId={}, amount={}", orderId, amount);

        // Validate input
        if (orderId == null || orderId.isBlank()) {
            throw new BusinessException("Order ID không hợp lệ");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Số tiền không hợp lệ");
        }

        VietQRProperties.Merchant merchant = vietqrProperties.getMerchant();
        VietQRProperties.Api api = vietqrProperties.getApi();

        // Build request
        VietQRCreateRequest request = VietQRCreateRequest.builder()
                .accountNumber(merchant.getAccountNumber())
                .accountName(normalizeAccountName(merchant.getAccountName()))
                .amount(amount.longValue())
                .addInfo(TRANSFER_PREFIX + orderId)
                .accountNameBank(merchant.getBankName())
                .build();

        // Build URL: /v2/{accountId}/{bankCode}
        String url = String.format("%s/%s/%s",
                api.getBaseUrl(),
                merchant.getAccountNumber(),
                merchant.getBankCode());

        // Build headers với Basic Auth
        HttpHeaders headers = buildAuthHeaders(api.getClientId(), api.getApiKey());
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<VietQRCreateRequest> entity = new HttpEntity<>(request, headers);

        try {
            log.debug("Calling VietQR API: {}", url);
            log.debug("Request: {}", objectMapper.writeValueAsString(request));
        } catch (Exception debugEx) {
            log.debug("Could not serialize request for logging: {}", debugEx.getMessage());
        }

        try {
            ResponseEntity<VietQRCreateResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    VietQRCreateResponse.class);

            VietQRCreateResponse body = response.getBody();
            if (body == null) {
                throw new BusinessException("VietQR API trả về response rỗng");
            }

            log.debug("VietQR API response: code={}, message={}", body.getCode(), body.getMessage());

            if (!body.isSuccess()) {
                throw new BusinessException("VietQR API lỗi: " + body.getCode() + " - " + body.getMessage());
            }

            VietQRCreateResponse.QRData qrData = body.getData();
            if (qrData == null) {
                throw new BusinessException("VietQR API không trả về dữ liệu QR");
            }

            // Lưu payment record vào DB
            savePaymentRecord(orderId, amount, qrData.getId());

            // Build response
            return VietQRPaymentResponse.builder()
                    .orderId(orderId)
                    .amount(amount.longValue())
                    .amountDisplay(amount)
                    .qrDataURL(qrData.getQrDataURL())
                    .qrCode(qrData.getQrCode())
                    .deepLink(qrData.getDeepLink())
                    .vietqrTransactionId(qrData.getId())
                    .transferDescription(TRANSFER_PREFIX + orderId)
                    .merchantAccount(merchant.getAccountNumber())
                    .merchantBank(merchant.getBankName())
                    .merchantAccountName(merchant.getAccountName())
                    .qrDownloadUrl("/api/vietqr/" + qrData.getId() + "/download")
                    .build();

        } catch (HttpClientErrorException e) {
            log.error("VietQR API HTTP error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new BusinessException("VietQR API lỗi HTTP: " + e.getStatusCode());
        } catch (Exception e) {
            log.error("Error calling VietQR API", e);
            if (e instanceof BusinessException) {
                throw e;
            }
            throw new BusinessException("Không thể tạo QR thanh toán: " + e.getMessage());
        }
    }

    @Override
    public boolean handleWebhook(VietQRWebhookPayload payload) {
        log.info("Received VietQR webhook: code={}, orderId={}",
                payload.getCode(), payload.extractOrderId());

        if (payload == null || !payload.isSuccess()) {
            log.warn("Invalid or failed webhook payload");
            return false;
        }

        VietQRWebhookPayload.TransactionData data = payload.getData();
        if (data == null) {
            log.warn("Webhook data is null");
            return false;
        }

        // Extract order ID từ addInfo
        String orderId = payload.extractOrderId();
        if (orderId == null) {
            log.warn("Cannot extract orderId from webhook addInfo: {}", data.getAddInfo());
            return false;
        }

        // Verify đây là tiền vào tài khoản (credit)
        if (!"C".equals(data.getCrDr())) {
            log.info("Ignoring debit transaction");
            return true;
        }

        // Find and update payment record
        Optional<Payment> paymentOpt = paymentRepository.findByOrderIdAndPaymentMethod(
                orderId, PaymentMethod.BANK_QR);

        if (paymentOpt.isEmpty()) {
            log.warn("No BANK_QR payment found for orderId: {}", orderId);
            // Có thể là thanh toán từ nguồn khác - vẫn return true để không retry
            return true;
        }

        Payment payment = paymentOpt.get();

        // Kiểm tra xem đã xử lý chưa (idempotent)
        if (payment.getStatus() == PaymentStatus.PAID) {
            log.info("Payment already processed for orderId: {}", orderId);
            return true;
        }

        // Verify amount
        Long paidAmount = data.getAmount();
        if (paidAmount != null && payment.getAmount() != null) {
            if (!paidAmount.equals(payment.getAmount().longValue())) {
                log.error("Amount mismatch for orderId={}: expected={}, actual={}",
                        orderId, payment.getAmount().longValue(), paidAmount);
                return false;
            }
        }

        // Update payment status
        payment.setStatus(PaymentStatus.PAID);
        payment.setVietqrTransactionId(data.getId());
        payment.setPaidAt(LocalDateTime.now());
        payment.setPaidBy(data.getAccountNumber());
        payment.setPaidByName(data.getAccountName());
        payment.setBankCode(data.getBankCode());
        payment.setBankName(data.getBankName());

        paymentRepository.save(payment);
        log.info("Payment confirmed for orderId={}, transactionId={}", orderId, data.getId());

        // TODO: Trigger Order status update via Event/AuditLog
        // orderService.confirmPayment(orderId);

        return true;
    }

    @Override
    public boolean queryPaymentStatus(String vietqrTransactionId) {
        if (vietqrTransactionId == null || vietqrTransactionId.isBlank()) {
            return false;
        }

        VietQRProperties.Merchant merchant = vietqrProperties.getMerchant();
        VietQRProperties.Api api = vietqrProperties.getApi();

        // Query transaction: /v2/{accountId}/{bankCode}/{transactionId}
        String url = String.format("%s/%s/%s/%s",
                api.getBaseUrl(),
                merchant.getAccountNumber(),
                merchant.getBankCode(),
                vietqrTransactionId);

        HttpHeaders headers = buildAuthHeaders(api.getClientId(), api.getApiKey());

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            log.debug("Querying VietQR transaction: {}", vietqrTransactionId);
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class);

            // Parse response và kiểm tra trạng thái
            VietQRCreateResponse vietqrResponse = objectMapper.readValue(
                    response.getBody(), VietQRCreateResponse.class);

            return vietqrResponse.isSuccess();

        } catch (Exception e) {
            log.error("Error querying VietQR transaction: {}", vietqrTransactionId, e);
            return false;
        }
    }

    @Override
    public boolean isOrderPaid(String orderId) {
        return paymentRepository.findByOrderIdAndPaymentMethod(orderId, PaymentMethod.BANK_QR)
                .map(payment -> payment.getStatus() == PaymentStatus.PAID)
                .orElse(false);
    }

    /**
     * Lưu payment record vào DB khi tạo QR.
     */
    private void savePaymentRecord(String orderId, BigDecimal amount, String vietqrTransactionId) {
        Payment payment = Payment.builder()
                .orderId(orderId)
                .amount(amount)
                .paymentMethod(PaymentMethod.BANK_QR)
                .status(PaymentStatus.PENDING)
                .vietqrTransactionId(vietqrTransactionId)
                .createdAt(LocalDateTime.now())
                .build();

        paymentRepository.save(payment);
        log.debug("Payment record saved: orderId={}, vietqrTransactionId={}", orderId, vietqrTransactionId);
    }

    /**
     * Build Authorization headers với Basic Auth (clientId:apiKey).
     */
    private HttpHeaders buildAuthHeaders(String clientId, String apiKey) {
        HttpHeaders headers = new HttpHeaders();
        String auth = clientId + ":" + apiKey;
        String encodedAuth = Base64.getEncoder()
                .encodeToString(auth.getBytes(StandardCharsets.UTF_8));
        headers.set("Authorization", "Basic " + encodedAuth);
        return headers;
    }

    /**
     * Normalize account name theo quy tắc VietQR:
     * - Không dấu
     * - Viết hoa
     * - Không có ký tự đặc biệt
     */
    private String normalizeAccountName(String name) {
        if (name == null) return "";
        // Chuẩn hóa Unicode và viết hoa cho VietQR
        // Note: Nên dùng thư viện unaccent như ICU4J để remove dấu tiếng Việt trong production
        try {
            return java.text.Normalizer.normalize(name, java.text.Normalizer.Form.NFKC)
                    .toUpperCase();
        } catch (Exception e) {
            return name.toUpperCase();
        }
    }
}
