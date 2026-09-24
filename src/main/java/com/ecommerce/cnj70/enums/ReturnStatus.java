package com.ecommerce.cnj70.enums;

/**
 * Phase 3A — Return Request Status.
 *
 * <p>Bám sát Phase 3A §23-§24: chỉ define state contract, không tự phát minh workflow.
 * Nếu Policy chưa quy định state chi tiết, các giá trị dưới đây là <i>đề xuất tối thiểu</i>
 * cho integration seam; phase sau sẽ thu hẹp theo Policy thực tế.</p>
 *
 * <p>Phân biệt với {@link ShippingStatus.RETURNED} (đó là trạng thái đơn vị vận chuyển
 * hoàn hàng về shop). ReturnRequest là yêu cầu trả hàng từ Customer phát sinh từ
 * Complaint; lifecycle ở đây tách biệt.</p>
 */
public enum ReturnStatus {
    /** Customer / Vendor / Moderator yêu cầu trả hàng; chờ xử lý. */
    REQUESTED,
    /** Đã chấp nhận yêu cầu trả hàng; chờ khách gửi hàng về. */
    APPROVED,
    /** Khách đã gửi hàng về shop (vendor ack). */
    IN_TRANSIT,
    /** Shop đã nhận lại hàng. */
    RECEIVED,
    /** Yêu cầu bị từ chối (vendor reject / moderator reject). */
    REJECTED,
    /** Terminal — hàng đã hoàn tất luồng (chuyển Refund hoặc đóng). */
    COMPLETED,
    /** Terminal — hủy bỏ. */
    CANCELLED
}
