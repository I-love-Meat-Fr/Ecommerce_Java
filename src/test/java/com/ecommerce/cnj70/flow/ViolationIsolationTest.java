package com.ecommerce.cnj70.flow;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ViolationIsolationTest - Verify isolation giữa violation của các shop khác nhau.
 *
 * Hiện tại: KHÔNG có Violation document/service (xem task Vendor Violation).
 * Test này document requirement + cung cấp logic test mẫu để khi implement sẽ chuyển sang unit test thật.
 *
 * Rule:
 *  - Violation có field shopId
 *  - Vendor A chỉ xem violation có shopId = currentShopId
 *  - Vendor A KHÔNG BAO GIỜ thấy violation của shop khác
 */
@DisplayName("Violation Isolation Test (mock - Violation chưa implement)")
class ViolationIsolationTest {

    /**
     * Mock Violation class để test logic isolation
     * (sẽ thay bằng Violation document thật khi implement)
     */
    static class MockViolation {
        String id;
        String shopId;
        String type;        // WARNING / VIOLATION / BAN
        String reason;
        String createdBy;   // admin id
        boolean acknowledged;
        String severity;    // LOW / MEDIUM / HIGH

        MockViolation(String id, String shopId, String type, String reason) {
            this.id = id;
            this.shopId = shopId;
            this.type = type;
            this.reason = reason;
        }
    }

    /**
     * Mock service - mô phỏng behavior MONG MUỐN của VendorViolationService.findByCurrentVendor
     */
    static class MockVendorViolationService {
        List<MockViolation> store = new ArrayList<>();

        List<MockViolation> findByCurrentVendor(String currentShopId) {
            return store.stream()
                    .filter(v -> v.shopId.equals(currentShopId))
                    .collect(Collectors.toList());
        }
    }

    @Test
    @DisplayName("Vendor A chỉ thấy violation của shop-A, không thấy shop-B")
    void vendorA_onlySeesOwnViolations() {
        MockVendorViolationService service = new MockVendorViolationService();
        service.store.add(new MockViolation("v-1", "shop-A", "WARNING", "Sản phẩm mô tả sai"));
        service.store.add(new MockViolation("v-2", "shop-B", "VIOLATION", "Bán hàng giả"));
        service.store.add(new MockViolation("v-3", "shop-A", "BAN", "Vi phạm nghiêm trọng"));
        service.store.add(new MockViolation("v-4", "shop-C", "WARNING", "Chậm giao hàng"));

        List<MockViolation> shopAViolations = service.findByCurrentVendor("shop-A");
        List<MockViolation> shopBViolations = service.findByCurrentVendor("shop-B");

        assertThat(shopAViolations).hasSize(2);
        assertThat(shopAViolations).extracting("shopId").containsOnly("shop-A");

        assertThat(shopBViolations).hasSize(1);
        assertThat(shopBViolations.get(0).reason).isEqualTo("Bán hàng giả");
    }

    @Test
    @DisplayName("Vendor không có shop → trả empty list (không throw)")
    void vendorNoShop_returnsEmpty() {
        MockVendorViolationService service = new MockVendorViolationService();
        service.store.add(new MockViolation("v-1", "shop-A", "WARNING", "Test"));

        List<MockViolation> result = service.findByCurrentVendor(null);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Vendor A có 5 violations, filter theo severity HIGH → chỉ thấy 1")
    void filterBySeverity() {
        MockVendorViolationService service = new MockVendorViolationService();
        for (int i = 0; i < 5; i++) {
            MockViolation v = new MockViolation("v-" + i, "shop-A", "WARNING", "Reason " + i);
            v.severity = (i == 0) ? "HIGH" : "LOW";
            service.store.add(v);
        }

        List<MockViolation> all = service.findByCurrentVendor("shop-A");
        List<MockViolation> highSeverity = all.stream()
                .filter(v -> "HIGH".equals(v.severity))
                .collect(Collectors.toList());

        assertThat(all).hasSize(5);
        assertThat(highSeverity).hasSize(1);
    }

    @Test
    @DisplayName("Document: Violation entity cần các trường tối thiểu")
    void documentRequiredFields() {
        // Đây là test design document các trường cần có ở Violation document khi implement
        Map<String, String> requiredFields = new HashMap<>();
        requiredFields.put("id", "primary key");
        requiredFields.put("shopId", "FK tới Shop - dùng để filter");
        requiredFields.put("type", "WARNING / VIOLATION / BAN");
        requiredFields.put("severity", "LOW / MEDIUM / HIGH");
        requiredFields.put("reason", "Lý do vi phạm (required, không rỗng)");
        requiredFields.put("createdBy", "Admin id tạo violation");
        requiredFields.put("createdAt", "Audit timestamp");
        requiredFields.put("acknowledged", "Vendor đã đọc chưa");

        // Sanity check
        assertThat(requiredFields).containsKeys(
                "id", "shopId", "type", "severity", "reason", "createdBy", "createdAt", "acknowledged");
        assertThat(requiredFields).hasSize(8);
    }
}
