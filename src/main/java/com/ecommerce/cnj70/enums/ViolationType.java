package com.ecommerce.cnj70.enums;

public enum ViolationType {
    /** Cảnh báo nhẹ, shop vẫn hoạt động bình thường */
    WARNING,
    /** Vi phạm nghiêm trọng hơn, có thể bị giới hạn */
    VIOLATION,
    /** Vi phạm nghiêm trọng → shop bị đình chỉ */
    BAN
}
