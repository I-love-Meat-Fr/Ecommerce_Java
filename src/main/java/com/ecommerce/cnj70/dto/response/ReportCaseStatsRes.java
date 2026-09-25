package com.ecommerce.cnj70.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * TASK #26 — Response cho thống kê ReportCase (cho Dashboard).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportCaseStatsRes {

    /** Tổng số case đang PENDING */
    private long pendingTotal;

    /** Case pending theo targetType */
    private Map<String, Long> pendingByTargetType;

    /** Tổng số case đã RESOLVED */
    private long resolvedTotal;

    /** Tổng số case ESCALATED */
    private long escalatedTotal;

    /** Case resolved hôm nay */
    private long resolvedToday;
}
