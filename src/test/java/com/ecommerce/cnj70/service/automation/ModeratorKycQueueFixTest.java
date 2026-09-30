package com.ecommerce.cnj70.service.automation;

import com.ecommerce.cnj70.document.KycProfile;
import com.ecommerce.cnj70.document.Shop;
import com.ecommerce.cnj70.document.User;
import com.ecommerce.cnj70.enums.KycStatus;
import com.ecommerce.cnj70.repository.KycProfileRepository;
import com.ecommerce.cnj70.repository.ReviewRepository;
import com.ecommerce.cnj70.repository.ShopRepository;
import com.ecommerce.cnj70.repository.UserRepository;
import com.ecommerce.cnj70.service.ModeratorService;
import com.ecommerce.cnj70.service.impl.ModeratorServiceImpl;
import org.bson.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Verify fix: ModeratorServiceImpl.getKycProfilesByStatusPaged() trả về profile
 * của vendor MỚI (chưa tạo Shop). Trước đây queue query ShopRepository.findByKycStatus
 * nên bỏ sót vendor mới.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ModeratorKycQueueFixTest {

    @Mock private ReviewRepository reviewRepository;
    @Mock private ShopRepository shopRepository;
    @Mock private KycProfileRepository kycProfileRepository;
    @Mock private UserRepository userRepository;
    @Mock private MongoTemplate mongoTemplate;

    @InjectMocks
    private ModeratorServiceImpl moderatorService;

    @Test
    @DisplayName("Vendor mới submit KYC nhưng chưa tạo Shop → vẫn hiện trong moderator queue")
    void vendorWithoutShop_appearsInQueue() {
        // Vendor user mới đăng ký
        User newVendor = User.builder()
                .id("user-new-vendor")
                .email("newvendor@test.com")
                .fullName("Nguyễn Văn Mới")
                .phone("0901234567")
                .shopId(null)  // <-- KEY: chưa có shop
                .kycStatus(KycStatus.PENDING_THIRD_PARTY)
                .build();

        // KycProfile họ vừa submit
        KycProfile newProfile = KycProfile.builder()
                .id("kyc-new-001")
                .userId("user-new-vendor")
                .ownerFullName("Nguyễn Văn Mới")
                .businessName("Shop Mới Mở")
                .taxCode("1234567890")
                .status(KycStatus.PENDING_THIRD_PARTY)
                .submittedAt(LocalDateTime.now().minusHours(1))
                .lastResubmittedAt(LocalDateTime.now())
                .submitCount(1)
                .build();

        Pageable pageable = PageRequest.of(0, 10);
        Page<KycProfile> mockPage = new PageImpl<>(List.of(newProfile), pageable, 1);

        when(kycProfileRepository.findByStatusIn(
                eq(List.of(KycStatus.PENDING_THIRD_PARTY, KycStatus.PENDING_ADMIN,
                        KycStatus.THIRD_PARTY_REJECTED)),
                eq(pageable)))
                .thenReturn(mockPage);

        // Act: gọi method mới
        Page<KycProfile> result = moderatorService.getKycProfilesByStatusPaged(null, pageable);

        // Assert: profile của vendor mới (không có shop) HIỆN trong queue
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo("kyc-new-001");
        assertThat(result.getContent().get(0).getUserId()).isEqualTo("user-new-vendor");
        assertThat(result.getContent().get(0).getStatus()).isEqualTo(KycStatus.PENDING_THIRD_PARTY);
    }

    @Test
    @DisplayName("Filter status cụ thể → chỉ trả về profile khớp")
    void specificStatusFilter_returnsMatchingOnly() {
        KycProfile pending = KycProfile.builder()
                .id("kyc-pending").userId("u1")
                .status(KycStatus.PENDING_THIRD_PARTY).build();
        KycProfile rejected = KycProfile.builder()
                .id("kyc-rej").userId("u2")
                .status(KycStatus.THIRD_PARTY_REJECTED).build();
        Pageable pageable = PageRequest.of(0, 10);

        when(kycProfileRepository.findByStatus(eq(KycStatus.PENDING_THIRD_PARTY), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(pending), pageable, 1));
        when(kycProfileRepository.findByStatus(eq(KycStatus.THIRD_PARTY_REJECTED), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(rejected), pageable, 1));

        Page<KycProfile> pendingResult = moderatorService.getKycProfilesByStatusPaged(KycStatus.PENDING_THIRD_PARTY, pageable);
        Page<KycProfile> rejectedResult = moderatorService.getKycProfilesByStatusPaged(KycStatus.THIRD_PARTY_REJECTED, pageable);

        assertThat(pendingResult.getContent()).extracting(KycProfile::getId).containsExactly("kyc-pending");
        assertThat(rejectedResult.getContent()).extracting(KycProfile::getId).containsExactly("kyc-rej");
    }

    @Test
    @DisplayName("approveKycProfile: cập nhật KycProfile + User.kycStatus + (nếu có Shop)")
    void approveKycProfile_syncsAllDenormalizedFields() {
        User vendor = User.builder()
                .id("u-vendor").email("v@t.com").fullName("V")
                .shopId(null)  // chưa tạo shop
                .kycStatus(KycStatus.PENDING_THIRD_PARTY)
                .build();
        KycProfile profile = KycProfile.builder()
                .id("kyc-approve-test").userId("u-vendor")
                .ownerFullName("V").businessName("B")
                .status(KycStatus.PENDING_THIRD_PARTY)
                .submitCount(1)
                .build();

        when(kycProfileRepository.findById("kyc-approve-test"))
                .thenReturn(Optional.of(profile));
        when(userRepository.findById("u-vendor"))
                .thenReturn(Optional.of(vendor));
        when(kycProfileRepository.save(any(KycProfile.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        KycProfile result = moderatorService.approveKycProfile("kyc-approve-test", "mod@x.com", "ok");

        assertThat(result.getStatus()).isEqualTo(KycStatus.APPROVED);
        assertThat(result.getAdminReviewedAt()).isNotNull();
        assertThat(result.getAdminNote()).isEqualTo("ok");
        assertThat(result.getAdminReviewerName()).isEqualTo("mod@x.com");
        assertThat(vendor.getKycStatus()).isEqualTo(KycStatus.APPROVED);
    }

    @Test
    @DisplayName("approveKycProfile với vendor đã có Shop → sync cả Shop.kycStatus")
    void approveKycProfile_syncsShopIfExists() {
        User vendor = User.builder()
                .id("u-with-shop").shopId("shop-1")
                .kycStatus(KycStatus.PENDING_THIRD_PARTY).build();
        Shop shop = new Shop();
        shop.setId("shop-1");
        shop.setKycStatus(KycStatus.PENDING_THIRD_PARTY);
        KycProfile profile = KycProfile.builder()
                .id("kyc-with-shop").userId("u-with-shop")
                .status(KycStatus.PENDING_THIRD_PARTY).build();

        when(kycProfileRepository.findById("kyc-with-shop")).thenReturn(Optional.of(profile));
        when(userRepository.findById("u-with-shop")).thenReturn(Optional.of(vendor));
        when(shopRepository.findById("shop-1")).thenReturn(Optional.of(shop));
        when(kycProfileRepository.save(any(KycProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        moderatorService.approveKycProfile("kyc-with-shop", "mod@x.com", "good");

        assertThat(shop.getKycStatus()).isEqualTo(KycStatus.APPROVED);
        assertThat(shop.getKycApprovedAt()).isNotNull();
        assertThat(shop.getActionBy()).isEqualTo("mod@x.com");
    }

    @Test
    @DisplayName("rejectKycProfile: yêu cầu note, set ADMIN_REJECTED + sync User + Shop")
    void rejectKycProfile_requiresNoteAndSyncsAll() {
        User vendor = User.builder().id("u-rej").kycStatus(KycStatus.PENDING_THIRD_PARTY).build();
        KycProfile profile = KycProfile.builder()
                .id("kyc-rej-test").userId("u-rej")
                .status(KycStatus.PENDING_THIRD_PARTY).build();

        when(kycProfileRepository.findById("kyc-rej-test")).thenReturn(Optional.of(profile));
        when(userRepository.findById("u-rej")).thenReturn(Optional.of(vendor));
        when(kycProfileRepository.save(any(KycProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        // reject without note → throw
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> moderatorService.rejectKycProfile("kyc-rej-test", "mod@x.com", ""));

        // reject with note → success
        KycProfile result = moderatorService.rejectKycProfile("kyc-rej-test", "mod@x.com", "Ảnh CCCD mờ");
        assertThat(result.getStatus()).isEqualTo(KycStatus.ADMIN_REJECTED);
        assertThat(result.getAdminNote()).isEqualTo("Ảnh CCCD mờ");
        assertThat(vendor.getKycStatus()).isEqualTo(KycStatus.ADMIN_REJECTED);
    }
}
