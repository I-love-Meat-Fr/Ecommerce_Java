package com.ecommerce.cnj70.dto.thirdparty;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KycProviderResponse {

    /** Kết quả từ bên thứ ba: APPROVED / REJECTED */
    private String result;

    /** Lý do từ chối (free text), null nếu APPROVED */
    @JsonProperty("reject_reason")
    private String rejectReason;

    /** Mã reference của bên thứ ba để trace */
    @JsonProperty("reference_id")
    private String referenceId;

    /** Timestamp bên thứ ba trả về */
    private String timestamp;
}
