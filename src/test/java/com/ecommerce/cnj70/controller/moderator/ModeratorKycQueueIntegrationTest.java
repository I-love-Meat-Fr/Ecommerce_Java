package com.ecommerce.cnj70.controller.moderator;

import com.ecommerce.cnj70.document.KycProfile;
import com.ecommerce.cnj70.document.User;
import com.ecommerce.cnj70.enums.AccountStatus;
import com.ecommerce.cnj70.enums.KycStatus;
import com.ecommerce.cnj70.enums.UserRole;
import com.ecommerce.cnj70.repository.KycProfileRepository;
import com.ecommerce.cnj70.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration test #26.1 — verify moderator /moderator/kyc endpoint shows
 * KycProfile của vendor MỚI (chưa tạo Shop). Trước đây queue query ShopRepository
 * bỏ sót các vendor mới.
 *
 * <p>Không cần Mongo embedded — dùng live MongoDB Atlas (test cluster) mà
 * project đang kết nối. Cleanup các record test được tạo ở AfterEach.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Moderator KYC queue visibility fix")
class ModeratorKycQueueIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private KycProfileRepository kycProfileRepository;

    private String testUserId;
    private String testProfileId;

    @AfterEach
    void cleanup() {
        if (testProfileId != null) kycProfileRepository.deleteById(testProfileId);
        if (testUserId != null) userRepository.deleteById(testUserId);
    }

    @Test
    @WithMockUser(username = "moderator2@gmail.com", roles = {"MODERATOR"})
    @DisplayName("GET /moderator/kyc — vendor mới submit KYC (không có Shop) hiện trong queue")
    void newVendorWithoutShop_appearsInKycQueue() throws Exception {
        // Arrange: tạo vendor user + KycProfile (KHÔNG có Shop)
        String uniqueSuffix = "TEST-" + System.currentTimeMillis();
        User newVendor = User.builder()
                .email("vendor-" + uniqueSuffix + "@test.local")
                .password("ignored")
                .fullName("Vendor " + uniqueSuffix)
                .phone("0900000000")
                .role(UserRole.VENDOR)
                .status(AccountStatus.ACTIVE)
                .kycStatus(KycStatus.PENDING_THIRD_PARTY)
                .shopId(null)  // KEY: chưa tạo shop
                .build();
        newVendor = userRepository.save(newVendor);
        testUserId = newVendor.getId();

        KycProfile profile = KycProfile.builder()
                .userId(testUserId)
                .ownerFullName("Vendor " + uniqueSuffix)
                .businessName("Biz " + uniqueSuffix)
                .taxCode("1234567890")
                .idNumber("012345678901")
                .status(KycStatus.PENDING_THIRD_PARTY)
                .submittedAt(LocalDateTime.now())
                .lastResubmittedAt(LocalDateTime.now())
                .submitCount(1)
                .build();
        profile = kycProfileRepository.save(profile);
        testProfileId = profile.getId();

        // Act: moderator truy cập /moderator/kyc
        mockMvc.perform(get("/moderator/kyc"))
                .andExpect(status().isOk())
                .andExpect(view().name("moderator/kyc-queue"))
                // Expect queue chứa email của vendor mới (lookup từ User)
                .andExpect(content().string(org.hamcrest.Matchers.containsString(newVendor.getEmail())))
                // Expect chứa tên doanh nghiệp (từ KycProfile.businessName)
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Biz " + uniqueSuffix)))
                // Expect badge "Chưa tạo" shop (vì vendor chưa tạo Shop document)
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Chưa tạo")))
                // Expect không hiện empty state
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Không có KYC nào"))));
    }

    @Test
    @WithMockUser(username = "moderator2@gmail.com", roles = {"MODERATOR"})
    @DisplayName("GET /moderator/kyc/{profileId} — detail page load cho vendor mới (no shop)")
    void newVendorKycDetail_loadsWithoutShop() throws Exception {
        // Arrange
        String uniqueSuffix = "DETAIL-" + System.currentTimeMillis();
        User newVendor = User.builder()
                .email("vendordetail-" + uniqueSuffix + "@test.local")
                .password("ignored")
                .fullName("Vendor Detail " + uniqueSuffix)
                .phone("0900000001")
                .role(UserRole.VENDOR)
                .status(AccountStatus.ACTIVE)
                .kycStatus(KycStatus.PENDING_THIRD_PARTY)
                .shopId(null)
                .build();
        newVendor = userRepository.save(newVendor);
        testUserId = newVendor.getId();

        KycProfile profile = KycProfile.builder()
                .userId(testUserId)
                .ownerFullName("Vendor Detail " + uniqueSuffix)
                .businessName("Detail Biz " + uniqueSuffix)
                .taxCode("9999888877")
                .idNumber("987654321012")
                .status(KycStatus.PENDING_THIRD_PARTY)
                .submittedAt(LocalDateTime.now())
                .lastResubmittedAt(LocalDateTime.now())
                .submitCount(1)
                .build();
        profile = kycProfileRepository.save(profile);
        testProfileId = profile.getId();

        // Act + Assert
        mockMvc.perform(get("/moderator/kyc/" + testProfileId))
                .andExpect(status().isOk())
                .andExpect(view().name("moderator/kyc-detail"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Detail Biz " + uniqueSuffix)))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Vendor Detail " + uniqueSuffix)))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(newVendor.getEmail())))
                // Show "Chưa tạo Shop" badge
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Chưa tạo")))
                // Show approve/reject form
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Duyệt KYC")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Từ chối")));
    }

    @Test
    @WithMockUser(username = "moderator2@gmail.com", roles = {"MODERATOR"})
    @DisplayName("POST /moderator/kyc/{id}/approve — moderator approves, status changes to APPROVED, User synced")
    void moderatorApproveKycProfile_succeeds() throws Exception {
        // Arrange: vendor + KycProfile ở PENDING_ADMIN
        String uniqueSuffix = "APPR-" + System.currentTimeMillis();
        User newVendor = User.builder()
                .email("vendorappr-" + uniqueSuffix + "@test.local")
                .password("ignored")
                .fullName("Vendor Approve " + uniqueSuffix)
                .phone("0900000002")
                .role(UserRole.VENDOR)
                .status(AccountStatus.ACTIVE)
                .kycStatus(KycStatus.PENDING_ADMIN)
                .shopId(null)
                .build();
        newVendor = userRepository.save(newVendor);
        testUserId = newVendor.getId();

        KycProfile profile = KycProfile.builder()
                .userId(testUserId)
                .ownerFullName("Vendor Approve " + uniqueSuffix)
                .businessName("Approve Biz " + uniqueSuffix)
                .taxCode("111222333")
                .idNumber("321098765432")
                .status(KycStatus.PENDING_ADMIN)
                .submittedAt(LocalDateTime.now())
                .lastResubmittedAt(LocalDateTime.now())
                .submitCount(1)
                .build();
        profile = kycProfileRepository.save(profile);
        testProfileId = profile.getId();

        // Act: moderator POSTs approve
        mockMvc.perform(post("/moderator/kyc/{id}/approve", testProfileId)
                        .param("note", "Đã xác minh CCCD + MST"))
        // Should redirect back to detail page (302)
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/moderator/kyc/" + testProfileId));

        // Assert: DB state changed
        KycProfile after = kycProfileRepository.findById(testProfileId).orElseThrow();
        assertThat(after.getStatus()).isEqualTo(KycStatus.APPROVED);
        assertThat(after.getAdminReviewerName()).isEqualTo("moderator2@gmail.com");
        assertThat(after.getAdminNote()).isEqualTo("Đã xác minh CCCD + MST");
        assertThat(after.getAdminReviewedAt()).isNotNull();

        // User.kycStatus must be synced
        User vendorAfter = userRepository.findById(testUserId).orElseThrow();
        assertThat(vendorAfter.getKycStatus()).isEqualTo(KycStatus.APPROVED);
    }

    @Test
    @WithMockUser(username = "moderator2@gmail.com", roles = {"MODERATOR"})
    @DisplayName("POST /moderator/kyc/{id}/reject — moderator rejects với note, status ADMIN_REJECTED")
    void moderatorRejectKycProfile_succeeds() throws Exception {
        // Arrange
        String uniqueSuffix = "REJ-" + System.currentTimeMillis();
        User newVendor = User.builder()
                .email("vendorrej-" + uniqueSuffix + "@test.local")
                .password("ignored")
                .fullName("Vendor Reject " + uniqueSuffix)
                .phone("0900000003")
                .role(UserRole.VENDOR)
                .status(AccountStatus.ACTIVE)
                .kycStatus(KycStatus.PENDING_ADMIN)
                .shopId(null)
                .build();
        newVendor = userRepository.save(newVendor);
        testUserId = newVendor.getId();

        KycProfile profile = KycProfile.builder()
                .userId(testUserId)
                .ownerFullName("Vendor Reject " + uniqueSuffix)
                .businessName("Reject Biz " + uniqueSuffix)
                .status(KycStatus.PENDING_ADMIN)
                .submittedAt(LocalDateTime.now())
                .lastResubmittedAt(LocalDateTime.now())
                .submitCount(1)
                .build();
        profile = kycProfileRepository.save(profile);
        testProfileId = profile.getId();

        // Act
        mockMvc.perform(post("/moderator/kyc/{id}/reject", testProfileId)
                        .param("note", "Ảnh CCCD mờ, vui lòng chụp lại"))
                .andExpect(status().is3xxRedirection());

        // Assert
        KycProfile after = kycProfileRepository.findById(testProfileId).orElseThrow();
        assertThat(after.getStatus()).isEqualTo(KycStatus.ADMIN_REJECTED);
        assertThat(after.getAdminNote()).isEqualTo("Ảnh CCCD mờ, vui lòng chụp lại");
        assertThat(after.getAdminReviewerName()).isEqualTo("moderator2@gmail.com");

        User vendorAfter = userRepository.findById(testUserId).orElseThrow();
        assertThat(vendorAfter.getKycStatus()).isEqualTo(KycStatus.ADMIN_REJECTED);
    }
}
