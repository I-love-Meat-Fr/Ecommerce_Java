package com.ecommerce.cnj70.dto.complaint;

import com.ecommerce.cnj70.enums.ComplaintReason;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Phase 3A §11/§13 — Customer tạo Complaint.
 *
 * <p>customerId/shopId/orderId được suy ra từ authenticated user (Phase 3 §14:
 * "Không tin customerId/shopId/orderId do frontend gửi lên nếu có thể xác định
 * từ authenticated user").</p>
 *
 * <p>Phase 3A §13 — {@code orderItemIds} là danh sách productId của các item
 * trong Order mà complaint đang trỏ tới. Backend verify từng id có nằm trong
 * Order.items (đồng thời Order phải thuộc Customer).</p>
 */
@Data
public class CreateComplaintReq {
    private String orderId;
    private ComplaintReason reason;

    @Size(max = 4000, message = "description tối đa 4000 ký tự")
    private String description;

    /**
     * Phase 3A §11/§13 — evidence URLs / file metadata.
     * Backend không lưu file; chỉ giữ reference theo architecture hiện có.
     */
    private List<String> evidence = new ArrayList<>();

    /**
     * Phase 3A §11/§13 — optional OrderItem references (productId).
     * Nếu rỗng → complaint gắn với cả Order.
     * Nếu có → mỗi id phải tồn tại trong Order.items (verify tại service).
     */
    private List<String> orderItemIds = new ArrayList<>();
}
