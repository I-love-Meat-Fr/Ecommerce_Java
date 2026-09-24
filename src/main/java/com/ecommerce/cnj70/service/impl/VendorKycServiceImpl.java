package com.ecommerce.cnj70.service.impl;

import com.ecommerce.cnj70.document.KycProfile;
import com.ecommerce.cnj70.document.Shop;
import com.ecommerce.cnj70.document.User;
import com.ecommerce.cnj70.dto.request.KycFormReq;
import com.ecommerce.cnj70.dto.response.KycStatusRes;
import com.ecommerce.cnj70.enums.KycStatus;
import com.ecommerce.cnj70.exception.BadRequestException;
import com.ecommerce.cnj70.exception.UnauthorizedException;
import com.ecommerce.cnj70.repository.KycProfileRepository;
import com.ecommerce.cnj70.repository.ShopRepository;
import com.ecommerce.cnj70.repository.UserRepository;
import com.ecommerce.cnj70.service.VendorKycService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class VendorKycServiceImpl implements VendorKycService {

    private final UserRepository userRepository;
    private final KycProfileRepository kycProfileRepository;
    private final ThirdPartyKycVerifier thirdPartyKycVerifier;
    private final StorageService storageService;
    private final ShopRepository shopRepository;

    @Override
    public User getCurrentVendor(UserDetails userDetails) {
        if (userDetails == null) {
            throw new UnauthorizedException("Vui lòng đăng nhập để tiếp tục");
        }
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new UnauthorizedException("Không tìm thấy thông tin người dùng"));
    }

    @Override
    public KycStatusRes getKycStatus(UserDetails userDetails) {
        User vendor = getCurrentVendor(userDetails);
        KycProfile profile = kycProfileRepository.findByUserId(vendor.getId()).orElse(null);
        return KycStatusRes.fromProfile(profile);
    }

    @Override
    public KycFormReq getEditableForm(UserDetails userDetails) {
        User vendor = getCurrentVendor(userDetails);
        KycProfile profile = kycProfileRepository.findByUserId(vendor.getId()).orElse(null);

        // Chưa có profile → form rỗng cho lần submit đầu
        if (profile == null) {
            return KycFormReq.builder().build();
        }

        KycStatus current = profile.getStatus() != null ? profile.getStatus() : KycStatus.NOT_SUBMITTED;
        if (!current.canSubmit()) {
            throw new BadRequestException(
                    "Không thể chỉnh sửa hồ sơ ở trạng thái " + current.name()
                  + ". Vui lòng chờ kết quả hoặc liên hệ hỗ trợ.");
        }

        // Pre-fill từ profile hiện tại để vendor không phải nhập lại
        return KycFormReq.builder()
                .ownerFullName(profile.getOwnerFullName())
                .idNumber(profile.getIdNumber())
                .idFrontImageUrl(profile.getIdFrontImageUrl())
                .idBackImageUrl(profile.getIdBackImageUrl())
                .businessName(profile.getBusinessName())
                .taxCode(profile.getTaxCode())
                .businessLicenseUrl(profile.getBusinessLicenseUrl())
                .bankAccount(profile.getBankAccount())
                .bankName(profile.getBankName())
                .bankBranch(profile.getBankBranch())
                .build();
    }

    @Override
    public KycProfile submitKyc(UserDetails userDetails, KycFormReq request) {
        User vendor = getCurrentVendor(userDetails);

        // Upsert profile theo userId
        KycProfile profile = kycProfileRepository.findByUserId(vendor.getId())
                .orElseGet(() -> KycProfile.builder()
                        .userId(vendor.getId())
                        .submitCount(0)
                        .build());

        // Chỉ cho phép submit khi: NOT_SUBMITTED hoặc THIRD_PARTY_REJECTED
        KycStatus current = profile.getStatus() != null ? profile.getStatus() : KycStatus.NOT_SUBMITTED;
        if (!current.canSubmit()) {
            throw new BadRequestException(
                    "Không thể gửi hồ sơ ở trạng thái " + current.name()
                    + ". Vui lòng chờ kết quả hoặc liên hệ hỗ trợ.");
        }

        // Map dữ liệu từ request
        profile.setOwnerFullName(request.getOwnerFullName());
        profile.setIdNumber(request.getIdNumber());
        profile.setIdFrontImageUrl(request.getIdFrontImageUrl());
        profile.setIdBackImageUrl(request.getIdBackImageUrl());
        profile.setBusinessName(request.getBusinessName());
        profile.setTaxCode(request.getTaxCode());
        profile.setBusinessLicenseUrl(request.getBusinessLicenseUrl());
        profile.setBankAccount(request.getBankAccount());
        profile.setBankName(request.getBankName());
        profile.setBankBranch(request.getBankBranch());
        profile.setStatus(KycStatus.PENDING_THIRD_PARTY);

        // Reset các trường 3rd-party + admin
        profile.setThirdPartyRejectionReason(null);
        profile.setAdminNote(null);

        // Audit
        if (profile.getSubmittedAt() == null) {
            profile.setSubmittedAt(LocalDateTime.now());
        }
        profile.setLastResubmittedAt(LocalDateTime.now());
        profile.setSubmitCount((profile.getSubmitCount() == null ? 0 : profile.getSubmitCount()) + 1);

        KycProfile saved = kycProfileRepository.save(profile);

        // Đồng bộ kycStatus trên User (denormalized)
        vendor.setKycStatus(KycStatus.PENDING_THIRD_PARTY);
        userRepository.save(vendor);

        // Phase 1 — Đồng bộ Shop.kycStatus (denormalized mirror cho Moderator queue)
        // Source of truth là KycProfile.status; Shop.kycStatus giữ mirror để
        // ShopRepository.findByKycStatus() (dùng bởi ModeratorServiceImpl.getShopsByKycStatusPaged)
        // không trả về dữ liệu stale.
        if (vendor.getShopId() != null) {
            shopRepository.findById(vendor.getShopId()).ifPresent(shop -> {
                shop.setKycStatus(KycStatus.PENDING_THIRD_PARTY);
                shop.setKycSubmittedAt(LocalDateTime.now());
                shopRepository.save(shop);
            });
        }

        // Gọi 3rd-party để xác minh
        try {
            thirdPartyKycVerifier.verifyAndUpdate(saved.getId(), request);
        } catch (Exception e) {
            log.error("Error calling 3rd-party KYC verifier: {}", e.getMessage());
        }

        // Trả về profile mới nhất sau khi 3rd-party xử lý
        return kycProfileRepository.findById(saved.getId()).orElse(saved);
    }

    @Override
    public String uploadKycImage(UserDetails userDetails, MultipartFile file) {
        // Validate vendor tồn tại
        getCurrentVendor(userDetails);

        // StorageService đã validate size + content type
        return storageService.save(file);
    }

    @Override
    public boolean isVendorKycApproved(String userId) {
        if (userId == null) return false;
        return kycProfileRepository.findByUserId(userId)
                .map(p -> p.getStatus() == KycStatus.APPROVED)
                .orElse(false);
    }

    @Override
    public void requireKycApproved(UserDetails userDetails) {
        User vendor = getCurrentVendor(userDetails);
        if (!isVendorKycApproved(vendor.getId())) {
            throw new BadRequestException(
                    "Bạn cần hoàn tất xác minh danh tính (KYC) trước khi thực hiện thao tác này. "
                  + "Truy cập /vendor/kyc để hoàn tất xác minh.");
        }
    }
}
