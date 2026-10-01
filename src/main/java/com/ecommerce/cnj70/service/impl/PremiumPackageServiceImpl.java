package com.ecommerce.cnj70.service.impl;

import com.ecommerce.cnj70.document.PremiumPackage;
import com.ecommerce.cnj70.document.Shop;
import com.ecommerce.cnj70.document.User;
import com.ecommerce.cnj70.document.VendorSubscription;
import com.ecommerce.cnj70.dto.request.PremiumPackageFormReq;
import com.ecommerce.cnj70.enums.AuditAction;
import com.ecommerce.cnj70.enums.ShopStatus;
import com.ecommerce.cnj70.enums.SubscriptionStatus;
import com.ecommerce.cnj70.exception.BadRequestException;
import com.ecommerce.cnj70.exception.ResourceNotFoundException;
import com.ecommerce.cnj70.exception.UnauthorizedException;
import com.ecommerce.cnj70.repository.PremiumPackageRepository;
import com.ecommerce.cnj70.repository.ShopRepository;
import com.ecommerce.cnj70.repository.UserRepository;
import com.ecommerce.cnj70.repository.VendorSubscriptionRepository;
import com.ecommerce.cnj70.service.AuditLogService;
import com.ecommerce.cnj70.service.PremiumPackageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PremiumPackageServiceImpl implements PremiumPackageService {

    private final PremiumPackageRepository packageRepository;
    private final VendorSubscriptionRepository subscriptionRepository;
    private final ShopRepository shopRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    // ========== Admin operations ==========

    @Override
    @Transactional
    public PremiumPackage createPackage(PremiumPackageFormReq request) {
        PremiumPackage pkg = PremiumPackage.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .durationDays(request.getDurationDays())
                .priority(request.getPriority() != null ? request.getPriority() : 0)
                .active(request.getActive() == null || request.getActive())
                .build();
        PremiumPackage saved = packageRepository.save(pkg);

        auditLogService.logInfo(AuditAction.PREMIUM_PACKAGE_CREATED,
                "PremiumPackage", saved.getId(),
                "SYSTEM", "system", "ADMIN",
                "Created premium package: " + saved.getName() + " (price=" + saved.getPrice()
                        + ", durationDays=" + saved.getDurationDays() + ")",
                Map.of("name", saved.getName(),
                        "price", saved.getPrice(),
                        "durationDays", saved.getDurationDays()));

        return saved;
    }

    @Override
    @Transactional
    public PremiumPackage updatePackage(String packageId, PremiumPackageFormReq request) {
        PremiumPackage pkg = getPackageById(packageId);
        pkg.setName(request.getName());
        pkg.setDescription(request.getDescription());
        pkg.setPrice(request.getPrice());
        pkg.setDurationDays(request.getDurationDays());
        if (request.getPriority() != null) pkg.setPriority(request.getPriority());
        if (request.getActive() != null) pkg.setActive(request.getActive());

        PremiumPackage saved = packageRepository.save(pkg);

        auditLogService.logInfo(AuditAction.PREMIUM_PACKAGE_UPDATED,
                "PremiumPackage", saved.getId(),
                "SYSTEM", "system", "ADMIN",
                "Updated premium package: " + saved.getName(),
                Map.of("name", saved.getName()));

        return saved;
    }

    @Override
    @Transactional
    public void deletePackage(String packageId) {
        PremiumPackage pkg = getPackageById(packageId);
        pkg.setActive(false);
        packageRepository.save(pkg);

        auditLogService.logWarning(AuditAction.PREMIUM_PACKAGE_DELETED,
                "PremiumPackage", pkg.getId(),
                "SYSTEM", "system", "ADMIN",
                "Soft-deleted premium package: " + pkg.getName());
    }

    @Override
    public List<PremiumPackage> getAllPackages() {
        return packageRepository.findAllByOrderByPriceAsc();
    }

    @Override
    public List<PremiumPackage> getAllPackages(Boolean activeFilter) {
        if (activeFilter == null) return getAllPackages();
        return packageRepository.findByActiveOrderByPriceAsc(activeFilter);
    }

    @Override
    public PremiumPackage getPackageById(String packageId) {
        return packageRepository.findById(packageId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy Premium Package: " + packageId));
    }

    // ========== Vendor operations ==========

    @Override
    public List<PremiumPackage> getActivePackages() {
        return packageRepository.findByActiveTrueOrderByPriceAsc();
    }

    @Override
    @Transactional
    public VendorSubscription purchasePackage(String packageId, UserDetails userDetails) {
        User vendor = getCurrentVendor(userDetails);
        Shop shop = shopRepository.findByOwnerId(vendor.getId())
                .orElseThrow(() -> new BadRequestException(
                        "Bạn cần tạo shop trước khi đăng ký Premium."));

        // Validate shop đang hoạt động
        if (shop.getStatus() != ShopStatus.APPROVED) {
            throw new BadRequestException(
                    "Shop của bạn chưa được duyệt. Không thể đăng ký Premium.");
        }

        PremiumPackage pkg = getPackageById(packageId);
        if (!pkg.isActive()) {
            throw new BadRequestException("Gói Premium này hiện không còn bán.");
        }

        LocalDateTime now = LocalDateTime.now();
        boolean isRenewal = false;
        LocalDateTime newStart;
        LocalDateTime newEnd;

        Optional<VendorSubscription> existingSub =
                subscriptionRepository.findFirstByShopIdAndStatusOrderByEndDateDesc(
                        shop.getId(), SubscriptionStatus.ACTIVE);

        if (existingSub.isPresent() && existingSub.get().getEndDate().isAfter(now)) {
            // Gia hạn: cộng dồn từ endDate hiện tại
            isRenewal = true;
            newStart = existingSub.get().getStartDate();
            newEnd = existingSub.get().getEndDate().plusDays(pkg.getDurationDays());
        } else {
            // Mua mới
            newStart = now;
            newEnd = now.plusDays(pkg.getDurationDays());
        }

        // ===== Mock payment =====
        // Hiện tại không tích hợp gateway thật. Khi cần production-ready,
        // thay bằng VNPay/MoMo SDK và thêm trạng thái PENDING_PAYMENT.
        String paymentTxnId = "MOCK-" + UUID.randomUUID();
        log.info("[MOCK PAYMENT] Vendor {} purchased package {} for shop {}, txnId={}",
                vendor.getId(), pkg.getId(), shop.getId(), paymentTxnId);

        VendorSubscription sub = VendorSubscription.builder()
                .shopId(shop.getId())
                .packageId(pkg.getId())
                .packageName(pkg.getName())
                .durationDays(pkg.getDurationDays())
                .amountPaid(pkg.getPrice())
                .startDate(newStart)
                .endDate(newEnd)
                .status(SubscriptionStatus.ACTIVE)
                .paymentMethod("MOCK_WALLET")
                .paymentTransactionId(paymentTxnId)
                .build();
        VendorSubscription saved = subscriptionRepository.save(sub);

        // Sync shop flags
        shop.setPremiumActive(true);
        shop.setPremiumExpiresAt(newEnd);
        shopRepository.save(shop);

        AuditAction action = isRenewal
                ? AuditAction.PREMIUM_PACKAGE_RENEWED
                : AuditAction.PREMIUM_PACKAGE_PURCHASED;
        auditLogService.logInfo(action,
                "VendorSubscription", saved.getId(),
                vendor.getId(), vendor.getEmail(), "VENDOR",
                (isRenewal ? "Renewed" : "Purchased")
                        + " Premium package: " + pkg.getName()
                        + " | shopId=" + shop.getId()
                        + " | endDate=" + newEnd,
                Map.of(
                        "shopId", shop.getId(),
                        "packageId", pkg.getId(),
                        "packageName", pkg.getName(),
                        "amountPaid", pkg.getPrice(),
                        "endDate", newEnd.toString(),
                        "paymentMethod", "MOCK_WALLET",
                        "paymentTxnId", paymentTxnId,
                        "isRenewal", isRenewal
                ));

        return saved;
    }

    @Override
    @Transactional
    public VendorSubscription cancelSubscription(UserDetails userDetails) {
        User vendor = getCurrentVendor(userDetails);
        Shop shop = shopRepository.findByOwnerId(vendor.getId())
                .orElseThrow(() -> new BadRequestException(
                        "Bạn chưa có shop."));

        VendorSubscription sub = subscriptionRepository
                .findFirstByShopIdAndStatusOrderByEndDateDesc(shop.getId(), SubscriptionStatus.ACTIVE)
                .orElseThrow(() -> new BadRequestException(
                        "Shop của bạn hiện không có Premium nào đang hoạt động."));

        sub.setStatus(SubscriptionStatus.CANCELLED);
        VendorSubscription saved = subscriptionRepository.save(sub);

        // Clear shop flag
        shop.setPremiumActive(false);
        shop.setPremiumExpiresAt(null);
        shopRepository.save(shop);

        auditLogService.logWarning(AuditAction.PREMIUM_PACKAGE_CANCELLED,
                "VendorSubscription", sub.getId(),
                vendor.getId(), vendor.getEmail(), "VENDOR",
                "Vendor cancelled Premium subscription | shopId=" + shop.getId(),
                Map.of("shopId", shop.getId(),
                        "packageName", sub.getPackageName() != null ? sub.getPackageName() : ""));

        return saved;
    }

    @Override
    public Optional<VendorSubscription> getCurrentSubscription(UserDetails userDetails) {
        try {
            User vendor = getCurrentVendor(userDetails);
            Optional<Shop> shopOpt = shopRepository.findByOwnerId(vendor.getId());
            if (shopOpt.isEmpty()) return Optional.empty();
            String shopId = shopOpt.get().getId();

            return subscriptionRepository
                    .findFirstByShopIdAndStatusOrderByEndDateDesc(shopId, SubscriptionStatus.ACTIVE)
                    .filter(s -> s.getEndDate() != null && s.getEndDate().isAfter(LocalDateTime.now()));
        } catch (UnauthorizedException ex) {
            return Optional.empty();
        }
    }

    @Override
    public List<VendorSubscription> getSubscriptionHistory(UserDetails userDetails) {
        User vendor = getCurrentVendor(userDetails);
        Optional<Shop> shopOpt = shopRepository.findByOwnerId(vendor.getId());
        if (shopOpt.isEmpty()) return List.of();
        return subscriptionRepository.findByShopIdOrderByCreatedAtDesc(shopOpt.get().getId());
    }

    // ========== Internal — Scheduler ==========

    @Override
    public List<VendorSubscription> findExpiredSubscriptions(LocalDateTime cutoff) {
        return subscriptionRepository.findByStatusAndEndDateBefore(
                SubscriptionStatus.ACTIVE, cutoff);
    }

    @Override
    @Transactional
    public void markExpired(String subscriptionId) {
        VendorSubscription sub = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "VendorSubscription not found: " + subscriptionId));

        // Defensive: chỉ expire nếu vẫn đang ACTIVE và đã quá hạn
        if (sub.getStatus() != SubscriptionStatus.ACTIVE) {
            log.debug("Subscription {} already in terminal state, skip", subscriptionId);
            return;
        }
        if (sub.getEndDate() != null && sub.getEndDate().isAfter(LocalDateTime.now())) {
            log.debug("Subscription {} not yet past endDate, skip", subscriptionId);
            return;
        }

        sub.setStatus(SubscriptionStatus.EXPIRED);
        subscriptionRepository.save(sub);

        // Clear shop flag — chỉ khi shop đang reference đúng sub này
        // (tránh over-write khi vendor vừa gia hạn tạo sub mới)
        shopRepository.findById(sub.getShopId()).ifPresent(shop -> {
            if (shop.isPremiumActive()
                    && shop.getPremiumExpiresAt() != null
                    && shop.getPremiumExpiresAt().equals(sub.getEndDate())) {
                shop.setPremiumActive(false);
                shop.setPremiumExpiresAt(null);
                shopRepository.save(shop);
                log.info("Cleared premiumActive flag on shop {} after expiring subscription {}",
                        shop.getId(), sub.getId());
            } else {
                log.debug("Shop {} already has newer subscription, skip flag clearing",
                        shop.getId());
            }
        });

        auditLogService.logInfo(AuditAction.PREMIUM_PACKAGE_EXPIRED,
                "VendorSubscription", sub.getId(),
                "SYSTEM", "scheduler", "SYSTEM",
                "Premium subscription expired | shopId=" + sub.getShopId()
                        + " | endDate=" + sub.getEndDate(),
                Map.of("shopId", sub.getShopId(),
                        "packageName", sub.getPackageName() != null ? sub.getPackageName() : ""));
    }

    // ========== Helper ==========

    private User getCurrentVendor(UserDetails userDetails) {
        if (userDetails == null) {
            throw new UnauthorizedException("Vui lòng đăng nhập để tiếp tục");
        }
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new UnauthorizedException(
                        "Không tìm thấy thông tin người dùng"));
    }
}
