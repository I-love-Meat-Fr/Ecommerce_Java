package com.ecommerce.cnj70.service;

import com.ecommerce.cnj70.document.KycProfile;
import com.ecommerce.cnj70.document.User;
import com.ecommerce.cnj70.enums.KycStatus;
import com.ecommerce.cnj70.exception.BadRequestException;
import com.ecommerce.cnj70.exception.ResourceNotFoundException;
import com.ecommerce.cnj70.repository.KycProfileRepository;
import com.ecommerce.cnj70.repository.UserRepository;
import com.ecommerce.cnj70.service.impl.AdminKycServiceImpl;
import com.ecommerce.cnj70.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit test cho AdminKycServiceImpl.
 *
 * Verify rule:
 *  - approve: chỉ duyệt được khi PENDING_ADMIN / PENDING_THIRD_PARTY / THIRD_PARTY_REJECTED
 *  - reject: yêu cầu note (lý do) không được rỗng
 *  - suspend: chỉ suspend được khi APPROVED
 *  - reject: đồng bộ kycStatus trên User → ADMIN_REJECTED
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AdminKycService - unit test")
class AdminKycServiceTest {

    @Mock private KycProfileRepository kycProfileRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private AdminKycServiceImpl adminKycService;

    private User admin;
    private UserDetails adminDetails;

    @BeforeEach
    void setUp() {
        admin = TestFixtures.userAdmin();
        adminDetails = TestFixtures.customUserDetails(admin);
        when(kycProfileRepository.save(any(KycProfile.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // ============ approve ============

    @Test
    @DisplayName("approve: PENDING_ADMIN → APPROVED + sync User.kycStatus")
    void approve_pendingAdmin_setsApproved() {
        KycProfile profile = TestFixtures.kycProfile("kyc-1", "v-1", KycStatus.PENDING_ADMIN);
        User vendor = TestFixtures.userVendor("v-1", "v1@cnj70.com", KycStatus.PENDING_ADMIN, null);
        when(kycProfileRepository.findById("kyc-1")).thenReturn(Optional.of(profile));
        when(userRepository.findById("v-1")).thenReturn(Optional.of(vendor));

        KycProfile result = adminKycService.approve("kyc-1", adminDetails, "OK");

        assertThat(result.getStatus()).isEqualTo(KycStatus.APPROVED);
        assertThat(result.getAdminNote()).isEqualTo("OK");
        assertThat(result.getAdminReviewedAt()).isNotNull();
        assertThat(vendor.getKycStatus()).isEqualTo(KycStatus.APPROVED);
    }

    @Test
    @DisplayName("approve: status SUSPENDED → throw (chỉ duyệt được từ PENDING)")
    void approve_suspended_throws() {
        KycProfile profile = TestFixtures.kycProfile("kyc-1", "v-1", KycStatus.SUSPENDED);
        when(kycProfileRepository.findById("kyc-1")).thenReturn(Optional.of(profile));

        assertThatThrownBy(() -> adminKycService.approve("kyc-1", adminDetails, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Chỉ có thể duyệt");
    }

    @Test
    @DisplayName("approve: profile không tồn tại → ResourceNotFoundException")
    void approve_notFound_throws() {
        when(kycProfileRepository.findById("kyc-x")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminKycService.approve("kyc-x", adminDetails, "OK"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ============ reject ============

    @Test
    @DisplayName("reject: note rỗng → throw BadRequestException (bắt buộc lý do)")
    void reject_emptyNote_throws() {
        assertThatThrownBy(() -> adminKycService.reject("kyc-1", adminDetails, ""))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("lý do");
    }

    @Test
    @DisplayName("reject: note null → throw BadRequestException")
    void reject_nullNote_throws() {
        assertThatThrownBy(() -> adminKycService.reject("kyc-1", adminDetails, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("lý do");
    }

    @Test
    @DisplayName("reject: happy path → status = ADMIN_REJECTED + lưu note")
    void reject_withNote_setsAdminRejected() {
        KycProfile profile = TestFixtures.kycProfile("kyc-1", "v-1", KycStatus.PENDING_ADMIN);
        User vendor = TestFixtures.userVendor("v-1", "v1@cnj70.com", KycStatus.PENDING_ADMIN, null);
        when(kycProfileRepository.findById("kyc-1")).thenReturn(Optional.of(profile));
        when(userRepository.findById("v-1")).thenReturn(Optional.of(vendor));

        KycProfile result = adminKycService.reject("kyc-1", adminDetails, "Sai thông tin CCCD");

        assertThat(result.getStatus()).isEqualTo(KycStatus.ADMIN_REJECTED);
        assertThat(result.getAdminNote()).isEqualTo("Sai thông tin CCCD");
        assertThat(vendor.getKycStatus()).isEqualTo(KycStatus.ADMIN_REJECTED);
    }

    // ============ suspend ============

    @Test
    @DisplayName("suspend: APPROVED → SUSPENDED")
    void suspend_approved_setsSuspended() {
        KycProfile profile = TestFixtures.kycProfile("kyc-1", "v-1", KycStatus.APPROVED);
        User vendor = TestFixtures.userVendor("v-1", "v1@cnj70.com", KycStatus.APPROVED, "shop-1");
        when(kycProfileRepository.findById("kyc-1")).thenReturn(Optional.of(profile));
        when(userRepository.findById("v-1")).thenReturn(Optional.of(vendor));

        KycProfile result = adminKycService.suspend("kyc-1", adminDetails, "Vi phạm điều khoản");

        assertThat(result.getStatus()).isEqualTo(KycStatus.SUSPENDED);
        assertThat(result.getAdminNote()).isEqualTo("Vi phạm điều khoản");
    }

    @Test
    @DisplayName("suspend: PENDING_ADMIN → throw (chỉ suspend được khi APPROVED)")
    void suspend_pendingAdmin_throws() {
        KycProfile profile = TestFixtures.kycProfile("kyc-1", "v-1", KycStatus.PENDING_ADMIN);
        when(kycProfileRepository.findById("kyc-1")).thenReturn(Optional.of(profile));

        assertThatThrownBy(() -> adminKycService.suspend("kyc-1", adminDetails, "x"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("đã được duyệt");
    }
}
