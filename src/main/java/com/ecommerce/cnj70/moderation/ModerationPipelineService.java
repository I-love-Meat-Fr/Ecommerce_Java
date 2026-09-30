package com.ecommerce.cnj70.moderation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/**
 * Yêu cầu #1 — Moderation Pipeline Service.
 *
 * <p>Chạy lần lượt các {@link ModerationChecker} theo thứ tự {@link ModerationChecker#order()}
 * (tăng dần). Nếu checker nào trả về FATAL → short-circuit, không chạy tiếp.
 * Nếu không có FATAL nhưng có SUSPICIOUS → tiếp tục chạy hết chain để thu thập
 * đủ reasons trước khi tổng hợp.</p>
 *
 * <p>Quy tắc tổng hợp (xem {@link ModerationPipelineResult}):</p>
 * <ul>
 *   <li>Có ≥ 1 FATAL → {@code severity = FATAL}.</li>
 *   <li>Không FATAL, có ≥ 1 SUSPICIOUS → {@code severity = SUSPICIOUS}.</li>
 *   <li>Tất cả PASS → {@code severity = PASS}.</li>
 * </ul>
 *
 * <p>Service này KHÔNG tự save document. Việc apply kết quả (set status, save,
 * tạo ReportCase, audit log) là trách nhiệm của caller — vd
 * {@code ReviewServiceImpl.createReview()} cho Review,
 * {@code ProductServiceImpl.createProduct()} cho Product.</p>
 *
 * <p>Thêm checker mới chỉ cần tạo bean {@code @Component} implement
 * {@link ModerationChecker}, Spring tự inject — không cần sửa service này.</p>
 */
@Slf4j
@Service
public class ModerationPipelineService {

    private final List<ModerationChecker> checkers;

    /**
     * Constructor injection — Spring tự động truyền toàn bộ bean implement
     * {@link ModerationChecker}. Sắp xếp theo {@code order()} ASC.
     */
    public ModerationPipelineService(List<ModerationChecker> checkers) {
        this.checkers = checkers == null
                ? List.of()
                : checkers.stream()
                    .sorted(Comparator.comparingInt(ModerationChecker::order))
                    .toList();
        log.info("[ModerationPipeline] Initialized with {} checker(s): {}",
                this.checkers.size(),
                this.checkers.stream().map(ModerationChecker::id).toList());
    }

    /**
     * Chạy toàn bộ pipeline và trả về kết quả tổng hợp.
     *
     * <p>Pipeline là fail-safe: nếu 1 checker throw exception, nó bị bỏ qua
     * (coi như PASS) và pipeline tiếp tục với checker kế tiếp. Lý do: không
     * nên để 1 checker lỗi chặn cả quá trình duyệt.</p>
     *
     * @param context ngữ cảnh cần kiểm duyệt (Product hoặc Review)
     * @return kết quả tổng hợp
     */
    public ModerationPipelineResult run(ModerationContext context) {
        if (context == null) {
            log.warn("[ModerationPipeline] run() called with null context — returning PASS");
            return ModerationPipelineResult.pass();
        }

        ModerationPipelineResult.ModerationPipelineResultBuilder builder =
                ModerationPipelineResult.builder();

        boolean hasFatal = false;
        boolean hasSuspicious = false;

        for (ModerationChecker checker : checkers) {
            // Filter theo supports() — checker có thể chỉ áp dụng cho Product hoặc Review.
            if (!checker.supports(context)) {
                continue;
            }

            ModerationDecision decision;
            try {
                decision = checker.check(context);
            } catch (Exception ex) {
                // Fail-safe: checker lỗi → bỏ qua (coi như PASS).
                log.error("[ModerationPipeline] Checker '{}' threw exception for target={} type={}: {}",
                        checker.id(), context.getTargetId(), context.getTargetType(),
                        ex.getMessage(), ex);
                continue;
            }

            if (decision == null) {
                log.warn("[ModerationPipeline] Checker '{}' returned null — treating as PASS",
                        checker.id());
                continue;
            }

            builder.decisions(java.util.Collections.singletonList(decision));

            switch (decision.getSeverity()) {
                case PASS -> { /* no-op */ }
                case SUSPICIOUS -> {
                    hasSuspicious = true;
                    if (decision.getFlagCode() != null) {
                        builder.autoFlags(java.util.Collections.singletonList(decision.getFlagCode()));
                    }
                    if (decision.getMessage() != null) {
                        builder.reasons(java.util.Collections.singletonList(decision.getMessage()));
                    }
                }
                case FATAL -> {
                    hasFatal = true;
                    if (decision.getFlagCode() != null) {
                        builder.autoFlags(java.util.Collections.singletonList(decision.getFlagCode()));
                    }
                    if (decision.getMessage() != null) {
                        builder.reasons(java.util.Collections.singletonList(decision.getMessage()));
                    }
                    // Short-circuit: FATAL → dừng chain ngay.
                    log.info("[ModerationPipeline] FATAL short-circuit at checker='{}' target={} type={}",
                            checker.id(), context.getTargetId(), context.getTargetType());
                    return finalizeResult(builder, hasFatal, hasSuspicious);
                }
            }
        }

        return finalizeResult(builder, hasFatal, hasSuspicious);
    }

    private ModerationPipelineResult finalizeResult(
            ModerationPipelineResult.ModerationPipelineResultBuilder builder,
            boolean hasFatal,
            boolean hasSuspicious) {
        ModerationDecision.Severity severity;
        if (hasFatal) {
            severity = ModerationDecision.Severity.FATAL;
        } else if (hasSuspicious) {
            severity = ModerationDecision.Severity.SUSPICIOUS;
        } else {
            severity = ModerationDecision.Severity.PASS;
        }

        // Đảm bảo lists không null
        if (builder.build().getAutoFlags() == null) {
            builder.autoFlags(new java.util.ArrayList<>());
        }
        if (builder.build().getReasons() == null) {
            builder.reasons(new java.util.ArrayList<>());
        }
        if (builder.build().getDecisions() == null) {
            builder.decisions(new java.util.ArrayList<>());
        }

        ModerationPipelineResult result = builder.severity(severity).build();
        log.debug("[ModerationPipeline] result: severity={} flags={} reasons={}",
                severity, result.getAutoFlags().size(), result.getReasons().size());
        return result;
    }

    /**
     * Trả về danh sách checker hiện đang đăng ký (cho debug / admin endpoint).
     */
    public List<String> getRegisteredCheckerIds() {
        return checkers.stream().map(ModerationChecker::id).toList();
    }
}
