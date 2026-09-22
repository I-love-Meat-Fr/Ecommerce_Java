package com.ecommerce.cnj70.service.impl;

import com.ecommerce.cnj70.document.Shop;
import com.ecommerce.cnj70.document.User;
import com.ecommerce.cnj70.dto.request.KycSubmitRequest;
import com.ecommerce.cnj70.dto.response.KycStatusResponse;
import com.ecommerce.cnj70.enums.AuditAction;
import com.ecommerce.cnj70.enums.AuditSeverity;
import com.ecommerce.cnj70.enums.KycStatus;
import com.ecommerce.cnj70.exception.BadRequestException;
import com.ecommerce.cnj70.exception.ResourceNotFoundException;
import com.ecommerce.cnj70.kyc.KycProvider;
import com.ecommerce.cnj70.repository.ShopRepository;
import com.ecommerce.cnj70.repository.UserRepository;
import com.ecommerce.cnj70.service.AuditLogService;
import com.ecommerce.cnj70.service.KycService;
import com.ecommerce.cnj70.util.CryptoUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * TASK #16 — KycService implementation.
 *
 * Flow:
 * 1. Vendor gọi submitKyc() → mã hóa PII → gửi provider → lưu referenceId + PENDING_PROVIDER
 * 2. (Trong demo/mock) provider xử lý sync → gọi callback → cập nhật kycStatus
 * 3. getKycStatus() / getVendorKycStatus() → trả response KHÔNG có PII plaintext
 *
 * PII được mã hóa:
 * - encryptPii() → Base64(iv || ciphertext || authTag) → lưu vào Shop/User encrypted* field
 * - decryptPii() → chỉ gọi khi cần hiển thị (trong admin service hoặc callback)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KycServiceImpl implements KycService {

    private final ShopRepository shopRepository;
    private final UserRepository userRepository;
    private final KycProvider kycProvider;
    private final CryptoUtil cryptoUtil;
    private final AuditLogService auditLogService;

    @Override
    public KycStatusResponse submitKyc(String shopId, KycSubmitRequest request) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Shop"));

        // ===== TASK #16: validate shop có thể nộp KYC =====
        if (shop.getKycStatus() == KycStatus.PENDING_PROVIDER) {
            throw new BadRequestException(
                    "KYC đang được xử lý. Vui lòng chờ kết quả trước khi nộp lại.");
        }

        // ===== TASK #19: mã hóa PII trước khi gửi provider và lưu =====
        String encryptedCitizenId = cryptoUtil.encrypt(request.getCitizenId());
        String encryptedTaxCode = request.getTaxCode() != null && !request.getTaxCode().isBlank()
                ? cryptoUtil.encrypt(request.getTaxCode())
                : null;
        String encryptedBankAccount = request.getBankAccount() != null && !request.getBankAccount().isBlank()
                ? cryptoUtil.encrypt(request.getBankAccount())
                : null;

        // ===== TASK #16: gửi sang provider =====
        String referenceId = kycProvider.submit(
                encryptedCitizenId,
                encryptedTaxCode,
                encryptedBankAccount,
                shopId
        );

        // ===== TASK #16: cập nhật Shop =====
        shop.setKycReferenceId(referenceId);
        shop.setKycStatus(KycStatus.PENDING_PROVIDER);
        shop.setKycSubmittedAt(LocalDateTime.now());
        shop.setEncryptedCitizenId(encryptedCitizenId);
        shop.setEncryptedTaxCode(encryptedTaxCode);
        shop.setEncryptedBankAccount(encryptedBankAccount);
        shopRepository.save(shop);

        // ===== TASK #16: đồng bộ encrypted PII vào User =====
        User owner = userRepository.findById(shop.getOwnerId()).orElse(null);
        if (owner != null) {
            owner.setEncryptedCitizenId(encryptedCitizenId);
            owner.setEncryptedTaxCode(encryptedTaxCode);
            owner.setEncryptedBankAccount(encryptedBankAccount);
            userRepository.save(owner);
        }

        log.info("[KycService] Shop {} submitted KYC, refId={}", shopId, referenceId);

        // ===== TASK #24: AuditLog =====
        auditLogService.logInfo(
                AuditAction.KYC_SUBMITTED,
                "SHOP",
                shopId,
                shop.getOwnerId(),
                shop.getShopName(),
                "VENDOR",
                "Vendor nộp KYC, provider=" + kycProvider.getProviderName() +
                        ", referenceId=" + referenceId
        );

        return toResponse(shop);
    }

    @Override
    public KycStatusResponse getKycStatus(String shopId) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Shop"));
        return toResponse(shop);
    }

    @Override
    public KycStatusResponse getVendorKycStatus(String vendorUserId) {
        User user = userRepository.findById(vendorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        if (user.getShopId() == null) {
            throw new BadRequestException("Người dùng này chưa có Shop");
        }

        return getKycStatus(user.getShopId());
    }

    @Override
    public boolean isKycApproved(String shopId) {
        return shopRepository.findById(shopId)
                .map(shop -> shop.getKycStatus() == KycStatus.APPROVED)
                .orElse(false);
    }

    /**
     * Cập nhật KYC result từ provider callback (TASK #18).
     * Được gọi bởi KycController.
     */
    @Override
    public void processKycCallback(String referenceId, boolean approved, String rejectionReason) {
        Shop shop = shopRepository.findByKycReferenceId(referenceId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy Shop với referenceId: " + referenceId));

        if (approved) {
            shop.setKycStatus(KycStatus.APPROVED);
            shop.setKycApprovedAt(LocalDateTime.now());
            shop.setKycRejectionReason(null);
            // Nâng cấp Shop status nếu đang PENDING
            if (shop.getStatus() == com.ecommerce.cnj70.enums.ShopStatus.PENDING) {
                shop.setStatus(com.ecommerce.cnj70.enums.ShopStatus.APPROVED);
            }
        } else {
            shop.setKycStatus(KycStatus.KYC_REJECTED);
            shop.setKycRejectionReason(rejectionReason);
        }

        shopRepository.save(shop);

        // ===== TASK #24: AuditLog cho KYC callback =====
        if (approved) {
            auditLogService.logInfo(
                    AuditAction.KYC_APPROVED,
                    "SHOP",
                    shop.getId(),
                    shop.getOwnerId(),
                    shop.getShopName(),
                    "SYSTEM",
                    "KYC APPROVED từ provider, referenceId=" + referenceId
            );
        } else {
            auditLogService.logWarning(
                    AuditAction.KYC_REJECTED,
                    "SHOP",
                    shop.getId(),
                    shop.getOwnerId(),
                    shop.getShopName(),
                    "SYSTEM",
                    "KYC REJECTED từ provider, referenceId=" + referenceId +
                            (rejectionReason != null ? ", lý do: " + rejectionReason : "")
            );
        }

        log.info("[KycService] Callback processed for refId={}, approved={}", referenceId, approved);
    }

    private KycStatusResponse toResponse(Shop shop) {
        String message = switch (shop.getKycStatus()) {
            case PENDING_KYC -> "Chưa nộp KYC";
            case PENDING_PROVIDER -> "Đang xác minh, vui lòng chờ";
            case APPROVED -> "KYC đã được xác minh thành công";
            case KYC_REJECTED -> "KYC bị từ chối: " +
                    (shop.getKycRejectionReason() != null ? shop.getKycRejectionReason() : "Không rõ lý do");
            case SUSPENDED -> "KYC đã bị tạm ngưng";
        };

        return KycStatusResponse.builder()
                .shopId(shop.getId())
                .kycStatus(shop.getKycStatus())
                .kycReferenceId(shop.getKycReferenceId())
                .kycApprovedAt(shop.getKycApprovedAt())
                .kycRejectionReason(shop.getKycRejectionReason())
                .kycSubmittedAt(shop.getKycSubmittedAt())
                .statusMessage(message)
                .build();
    }
}
