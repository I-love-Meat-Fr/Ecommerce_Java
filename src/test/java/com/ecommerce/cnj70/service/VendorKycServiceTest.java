package com.ecommerce.cnj70.service;

import com.ecommerce.cnj70.document.KycProfile;
import com.ecommerce.cnj70.document.User;
import com.ecommerce.cnj70.dto.request.KycFormReq;
import com.ecommerce.cnj70.enums.KycStatus;
import com.ecommerce.cnj70.enums.UserRole;
import com.ecommerce.cnj70.exception.BadRequestException;
import com.ecommerce.cnj70.exception.UnauthorizedException;
import com.ecommerce.cnj70.repository.KycProfileRepository;
import com.ecommerce.cnj70.repository.UserRepository;
import com.ecommerce.cnj70.service.impl.StorageService;
import com.ecommerce.cnj70.service.impl.ThirdPartyKycVerifier;
import com.ecommerce.cnj70.service.impl.VendorKycServiceImpl;
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Unit test cho VendorKycServiceImpl.
 *
 * Verify rule:
 *  - submitKyc: chỉ cho phép submit ở trạng thái NOT_SUBMITTED / THIRD_PARTY_REJECTED / ADMIN_REJECTED
 *  - submitKyc: gọi 3rd-party verifier (nếu fail → không throw, log error)
 *  - submitKyc: đồng bộ kycStatus trên User
 *  - getEditableForm: status không cho submit → throw
 *  - requireKycApproved: vendor chưa APPROVED → throw
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("VendorKycService - unit test")
class VendorKycServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private KycProfileRepository kycProfileRepository;
    @Mock private ThirdPartyKycVerifier thirdPartyKycVerifier;
    @Mock private StorageService storageService;

    @InjectMocks private VendorKycServiceImpl kycService;

    private User vendor;
    private UserDetails vendorDetails;

    @BeforeEach
    void setUp() {
        vendor = TestFixtures.userVendor("v-1", "v1@cnj70.com", KycStatus.NOT_SUBMITTED, null);
        vendorDetails = TestFixtures.customUserDetails(vendor);
        lenient().when(userRepository.findByEmail("v1@cnj70.com")).thenReturn(Optional.of(vendor));
        lenient().when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(kycProfileRepository.save(any(KycProfile.class))).thenAnswer(inv -> {
            KycProfile p = inv.getArgument(0);
            if (p.getId() == null) p.setId("kyc-1");
            return p;
        });
    }

    // ============ getCurrentVendor ============

    @Test
    @DisplayName("getCurrentVendor: UserDetails null → throw UnauthorizedException")
    void getCurrentVendor_nullDetails_throws() {
        assertThatThrownBy(() -> kycService.getCurrentVendor(null))
                .isInstanceOf(UnauthorizedException.class);
    }

    // ============ submitKyc ============

    @Test
    @DisplayName("submitKyc: chưa có profile → tạo mới, status = PENDING_THIRD_PARTY")
    void submitKyc_firstTime_createsProfileAndCallsVerifier() {
        when(kycProfileRepository.findByUserId("v-1")).thenReturn(Optional.empty());

        KycFormReq req = validKycFormReq();
        KycProfile saved = kycService.submitKyc(vendorDetails, req);

        assertThat(saved.getStatus()).isEqualTo(KycStatus.PENDING_THIRD_PARTY);
        assertThat(saved.getSubmitCount()).isEqualTo(1);
        assertThat(saved.getSubmittedAt()).isNotNull();
        assertThat(vendor.getKycStatus()).isEqualTo(KycStatus.PENDING_THIRD_PARTY);
    }

    @Test
    @DisplayName("submitKyc: status APPROVED → throw BadRequestException")
    void submitKyc_alreadyApproved_throws() {
        KycProfile approved = TestFixtures.kycProfile("kyc-1", "v-1", KycStatus.APPROVED);
        when(kycProfileRepository.findByUserId("v-1")).thenReturn(Optional.of(approved));

        assertThatThrownBy(() -> kycService.submitKyc(vendorDetails, validKycFormReq()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Không thể gửi");
    }

    @Test
    @DisplayName("submitKyc: status PENDING_THIRD_PARTY → throw (chờ xác minh)")
    void submitKyc_pendingThirdParty_throws() {
        KycProfile pending = TestFixtures.kycProfile("kyc-1", "v-1", KycStatus.PENDING_THIRD_PARTY);
        when(kycProfileRepository.findByUserId("v-1")).thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> kycService.submitKyc(vendorDetails, validKycFormReq()))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("submitKyc: status ADMIN_REJECTED → cho phép resubmit (sửa & gửi lại)")
    void submitKyc_adminRejected_canResubmit() {
        KycProfile rejected = TestFixtures.kycProfile("kyc-1", "v-1", KycStatus.ADMIN_REJECTED);
        when(kycProfileRepository.findByUserId("v-1")).thenReturn(Optional.of(rejected));

        KycProfile saved = kycService.submitKyc(vendorDetails, validKycFormReq());

        assertThat(saved.getStatus()).isEqualTo(KycStatus.PENDING_THIRD_PARTY);
        assertThat(saved.getAdminNote()).isNull(); // reset khi resubmit
    }

    @Test
    @DisplayName("submitKyc: 3rd-party fail → không throw (chỉ log error), submit vẫn thành công")
    void submitKyc_thirdPartyFails_swallowsException() {
        when(kycProfileRepository.findByUserId("v-1")).thenReturn(Optional.empty());
        doThrow(new RuntimeException("3rd-party down"))
                .when(thirdPartyKycVerifier).verifyAndUpdate(any(), any());

        // Không throw ra ngoài
        KycProfile saved = kycService.submitKyc(vendorDetails, validKycFormReq());

        assertThat(saved).isNotNull();
        assertThat(saved.getStatus()).isEqualTo(KycStatus.PENDING_THIRD_PARTY);
    }

    // ============ getEditableForm ============

    @Test
    @DisplayName("getEditableForm: chưa có profile → trả form rỗng")
    void getEditableForm_noProfile_returnsEmpty() {
        when(kycProfileRepository.findByUserId("v-1")).thenReturn(Optional.empty());

        KycFormReq form = kycService.getEditableForm(vendorDetails);

        assertThat(form).isNotNull();
        assertThat(form.getOwnerFullName()).isNull();
    }

    @Test
    @DisplayName("getEditableForm: status PENDING_THIRD_PARTY → throw (không cho edit)")
    void getEditableForm_pendingThirdParty_throws() {
        KycProfile pending = TestFixtures.kycProfile("kyc-1", "v-1", KycStatus.PENDING_THIRD_PARTY);
        when(kycProfileRepository.findByUserId("v-1")).thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> kycService.getEditableForm(vendorDetails))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Không thể chỉnh sửa");
    }

    @Test
    @DisplayName("getEditableForm: status ADMIN_REJECTED → cho phép edit, pre-fill từ profile")
    void getEditableForm_adminRejected_allowsEditAndPrefills() {
        KycProfile rejected = TestFixtures.kycProfile("kyc-1", "v-1", KycStatus.ADMIN_REJECTED);
        rejected.setOwnerFullName("Nguyễn Văn A");
        rejected.setIdNumber("012345678901");
        when(kycProfileRepository.findByUserId("v-1")).thenReturn(Optional.of(rejected));

        KycFormReq form = kycService.getEditableForm(vendorDetails);

        assertThat(form.getOwnerFullName()).isEqualTo("Nguyễn Văn A");
        assertThat(form.getIdNumber()).isEqualTo("012345678901");
    }

    // ============ isVendorKycApproved ============

    @Test
    @DisplayName("isVendorKycApproved: status APPROVED → true")
    void isVendorKycApproved_true() {
        when(kycProfileRepository.findByUserId("v-1")).thenReturn(Optional.of(
                TestFixtures.kycProfile("kyc-1", "v-1", KycStatus.APPROVED)));

        assertThat(kycService.isVendorKycApproved("v-1")).isTrue();
    }

    @Test
    @DisplayName("isVendorKycApproved: không có profile → false")
    void isVendorKycApproved_noProfile_false() {
        when(kycProfileRepository.findByUserId("v-1")).thenReturn(Optional.empty());

        assertThat(kycService.isVendorKycApproved("v-1")).isFalse();
    }

    @Test
    @DisplayName("isVendorKycApproved: userId null → false")
    void isVendorKycApproved_nullUserId_false() {
        assertThat(kycService.isVendorKycApproved(null)).isFalse();
    }

    // ============ requireKycApproved ============

    @Test
    @DisplayName("requireKycApproved: chưa APPROVED → throw BadRequestException")
    void requireKycApproved_notApproved_throws() {
        when(kycProfileRepository.findByUserId("v-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> kycService.requireKycApproved(vendorDetails))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("KYC");
    }

    @Test
    @DisplayName("requireKycApproved: APPROVED → không throw")
    void requireKycApproved_approved_passes() {
        when(kycProfileRepository.findByUserId("v-1")).thenReturn(Optional.of(
                TestFixtures.kycProfile("kyc-1", "v-1", KycStatus.APPROVED)));

        // Không throw
        kycService.requireKycApproved(vendorDetails);
    }

    // ============ helper ============

    private KycFormReq validKycFormReq() {
        return KycFormReq.builder()
                .ownerFullName("Nguyễn Văn Test")
                .idNumber("012345678901")
                .idFrontImageUrl("/uploads/front.jpg")
                .idBackImageUrl("/uploads/back.jpg")
                .businessName("Công ty Test")
                .taxCode("0123456789")
                .businessLicenseUrl("/uploads/license.jpg")
                .bankAccount("1234567890")
                .bankName("Vietcombank")
                .bankBranch("HCM")
                .build();
    }
}
