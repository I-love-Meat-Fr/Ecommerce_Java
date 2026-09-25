package com.ecommerce.cnj70.dto.complaint;

import lombok.Data;

@Data
public class ResolveComplaintReq {
    /** "RESOLVE" | "REJECT" | "ESCALATE" */
    private String action;
    private String reason;
    private String note;
}
