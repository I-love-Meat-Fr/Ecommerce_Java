package com.ecommerce.cnj70.service.impl;

import com.ecommerce.cnj70.document.KycProfile;
import com.ecommerce.cnj70.document.User;
import com.ecommerce.cnj70.dto.request.KycFormReq;
import com.ecommerce.cnj70.dto.thirdparty.KycProviderResponse;
import com.ecommerce.cnj70.enums.KycStatus;
import com.ecommerce.cnj70.repository.KycProfileRepository;
import com.ecommerce.cnj70.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Gọi 3rd-party để verify CCCD/MST.
 * Kết quả từ 3rd-party chỉ là bước 1 - sau đó admin phải duyệt lần 2.
 *
 * Luồng:
 *   3rd-party APPROVED  → KycProfile.status = PENDING_ADMIN (chờ admin)
 *   3rd-party REJECTED  → KycProfile.status = THIRD_PARTY_REJECTED (vendor sửa lại)
 *   3rd-party timeout   → fallback theo config (mặc định PENDING_THIRD_PARTY)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ThirdPartyKycVerifier {

    private final RestTemplate restTemplate;
    private final KycProfileRepository kycProfileRepository;
    private final UserRepository userRepository;

    @Value("${kyc.provider.url:https://kyc-provider.example.com/api/verify}")
    private String kycProviderUrl;

    @Value("${kyc.provider.api-key:}")
    private String apiKey;

    @Value("${kyc.fallback.on-error:PENDING_THIRD_PARTY}")
    private String fallbackOnError;

    /**
     * Gọi 3rd-party verify. Chỉ update các trường thirdParty* và status (nếu APPROVED → PENDING_ADMIN).
     */
    public void verifyAndUpdate(String profileId, KycFormReq request) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
            if (apiKey != null && !apiKey.isBlank()) {
                headers.set("X-API-Key", apiKey);
            }

            Map<String, Object> body = Map.of(
                    "id_number", request.getIdNumber() != null ? request.getIdNumber() : "",
                    "full_name", request.getOwnerFullName() != null ? request.getOwnerFullName() : "",
                    "tax_code", request.getTaxCode() != null ? request.getTaxCode() : "",
                    "bank_account", request.getBankAccount() != null ? request.getBankAccount() : "",
                    "reference_id", profileId
            );

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<KycProviderResponse> response = restTemplate.exchange(
                    kycProviderUrl,
                    HttpMethod.POST,
                    entity,
                    KycProviderResponse.class
            );

            KycProviderResponse resp = response.getBody();
            if (resp == null) {
                throw new RuntimeException("Empty response from KYC provider");
            }

            applyThirdPartyResult(profileId, resp);
            log.info("KYC 3rd-party completed for profile {}: {}", profileId, resp.getResult());

        } catch (Exception e) {
            log.error("KYC 3rd-party error for profile {}: {}", profileId, e.getMessage());
            fallbackOnError(profileId, e.getMessage());
        }
    }

    /**
     * Apply kết quả từ 3rd-party
     */
    private void applyThirdPartyResult(String profileId, KycProviderResponse resp) {
        KycProfile profile = kycProfileRepository.findById(profileId).orElse(null);
        if (profile == null) {
            log.warn("KycProfile not found: {}", profileId);
            return;
        }

        profile.setThirdPartyResult(resp.getResult());
        profile.setThirdPartyRejectionReason(resp.getRejectReason());
        profile.setThirdPartyReferenceId(resp.getReferenceId());
        profile.setThirdPartyVerifiedAt(LocalDateTime.now());

        if ("APPROVED".equalsIgnoreCase(resp.getResult())) {
            // 3rd-party OK → chuyển sang chờ admin duyệt
            profile.setStatus(KycStatus.PENDING_ADMIN);
        } else {
            // REJECTED → vendor sửa lại
            profile.setStatus(KycStatus.THIRD_PARTY_REJECTED);
        }

        kycProfileRepository.save(profile);
        syncUserKycStatus(profile);
    }

    /**
     * Fallback khi 3rd-party không phản hồi
     */
    private void fallbackOnError(String profileId, String errorMsg) {
        KycProfile profile = kycProfileRepository.findById(profileId).orElse(null);
        if (profile == null) return;

        if ("APPROVE".equalsIgnoreCase(fallbackOnError)) {
            // Dev mode: tự động coi như 3rd-party OK → chờ admin
            profile.setThirdPartyResult("APPROVED (fallback)");
            profile.setThirdPartyRejectionReason(null);
            profile.setThirdPartyVerifiedAt(LocalDateTime.now());
            profile.setStatus(KycStatus.PENDING_ADMIN);
            log.info("KYC fallback: auto-approved 3rd-party for profile {} due to error: {}", profileId, errorMsg);
        } else {
            // Production: giữ nguyên PENDING_THIRD_PARTY, vendor chờ admin duyệt tay (admin có thể override)
            profile.setThirdPartyResult("ERROR");
            profile.setThirdPartyRejectionReason("3rd-party timeout: " + errorMsg);
            profile.setThirdPartyVerifiedAt(LocalDateTime.now());
            log.info("KYC fallback: keeping PENDING_THIRD_PARTY for profile {} (admin can override)", profileId);
        }

        kycProfileRepository.save(profile);
        syncUserKycStatus(profile);
    }

    /**
     * Đồng bộ kycStatus trên User (denormalized)
     */
    private void syncUserKycStatus(KycProfile profile) {
        if (profile.getUserId() == null) return;
        userRepository.findById(profile.getUserId()).ifPresent(user -> {
            user.setKycStatus(profile.getStatus());
            userRepository.save(user);
        });
    }
}
