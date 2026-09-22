package com.ecommerce.cnj70.dto.request;

import com.ecommerce.cnj70.enums.ReportTargetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * TASK #26 — Request tạo ReportCase mới.
 *
 * Dùng cho cả 2 trường hợp:
 *   - Customer report:  reporterId lấy từ authenticated user
 *   - Auto Moderation:  reporterId = null, có autoFlags
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportCaseCreateReq {

    @NotNull(message = "Loại đối tượng không được để trống")
    private ReportTargetType targetType;

    @NotBlank(message = "ID đối tượng không được để trống")
    private String targetId;

    /** Lý do report ngắn gọn */
    @NotBlank(message = "Lý do report không được để trống")
    private String reason;

    /** Mô tả chi tiết thêm */
    private String description;

    /** Bằng chứng đính kèm (URL hình ảnh, text...) */
    private List<String> evidence;

    /** Cờ cảnh báo từ Auto Moderation */
    private List<String> autoFlags;

    /** Nguồn: USER hoặc AUTO (mặc định USER) */
    private String source;

    /** Priority (0 = bình thường, càng cao càng ưu tiên) */
    private Integer priority;
}
