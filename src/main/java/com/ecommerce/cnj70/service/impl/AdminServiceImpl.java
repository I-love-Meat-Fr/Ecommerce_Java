package com.ecommerce.cnj70.service.impl;

import com.ecommerce.cnj70.document.Escalation;
import com.ecommerce.cnj70.document.Order;
import com.ecommerce.cnj70.document.Violation;
import com.ecommerce.cnj70.dto.response.AdminDashboardRes;
import com.ecommerce.cnj70.dto.response.AdminDashboardRes.DailyMetric;
import com.ecommerce.cnj70.enums.ModerationStatus;
import com.ecommerce.cnj70.enums.OrderStatus;
import com.ecommerce.cnj70.enums.ShopStatus;
import com.ecommerce.cnj70.repository.EscalationRepository;
import com.ecommerce.cnj70.repository.OrderRepository;
import com.ecommerce.cnj70.repository.ProductRepository;
import com.ecommerce.cnj70.repository.ReviewRepository;
import com.ecommerce.cnj70.repository.ShopRepository;
import com.ecommerce.cnj70.repository.UserRepository;
import com.ecommerce.cnj70.repository.ViolationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements com.ecommerce.cnj70.service.AdminService {

    private final UserRepository userRepository;
    private final ShopRepository shopRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final ReviewRepository reviewRepository;
    private final EscalationRepository escalationRepository;
    private final ViolationRepository violationRepository;
    private final com.ecommerce.cnj70.service.AdminDashboardMetricsGateway metricsGateway;

    /**
     * Phase 4A — pending moderation statuses (Phase 2A/2B contract).
     * Reuses existing {@link ModerationStatus} enum without modifying it.
     */
    private static final Set<ModerationStatus> PENDING_MODERATION_STATUSES = Set.of(
            ModerationStatus.PENDING_MANUAL,
            ModerationStatus.AUTO_PASSED,
            ModerationStatus.AUTO_REJECTED
    );

    /**
     * Phase 4A — open Escalation statuses (Phase 3A inner enum).
     */
    private static final Set<Escalation.Status> OPEN_ESCALATION_STATUSES = Set.of(
            Escalation.Status.PENDING,
            Escalation.Status.IN_REVIEW
    );

    @Override
    public AdminDashboardRes getDashboardStats() {
        return getDashboardStatsByPeriod("WEEK");
    }

    @Override
    public AdminDashboardRes getDashboardStatsByPeriod(String period) {
        long totalUsers = safeCount(userRepository);
        long totalShops = safeCount(shopRepository);
        long totalProducts = safeCount(productRepository);
        long totalOrders = safeCount(orderRepository);

        List<Order> allOrders = safeFindAllOrders();

        List<Order> recentOrders = allOrders.stream()
                .sorted((a, b) -> {
                    LocalDateTime aTime = a.getCreatedAt() != null ? a.getCreatedAt() : LocalDateTime.MIN;
                    LocalDateTime bTime = b.getCreatedAt() != null ? b.getCreatedAt() : LocalDateTime.MIN;
                    return bTime.compareTo(aTime);
                })
                .limit(5)
                .toList();

        String safePeriod = normalizePeriod(period);
        List<DailyMetric> revenueTrend = buildRevenueTrendByPeriod(allOrders, safePeriod);
        BigDecimal periodRevenue = computePeriodRevenue(allOrders, safePeriod);

        List<AdminDashboardRes.RecentActivity> activities = buildRecentActivities(recentOrders);

        // Phase 4A — Financial: GMV (DELIVERED total amount, all-time) distinct from Platform Revenue.
        // Per Phase 4A §10–11: Order Total ≠ Platform Revenue. Platform Revenue must come from
        // a real Finance backend (commission/fee). When unavailable (null from gateway), the
        // metric MUST be null — NEVER fall back to GMV/periodRevenue. The UI uses
        // MetricAvailability.platformRevenue to render N/A.
        BigDecimal gmv = sumDeliveredRevenue(allOrders, o -> true);
        BigDecimal platformRevenue = metricsGateway.getPlatformRevenue(); // null until Finance contract

        // Phase 4A — Compliance / Risk counters
        long pendingModeration = countPendingModeration();
        long openEscalation = safeCountByStatusIn(escalationRepository, OPEN_ESCALATION_STATUSES);
        long openViolation = countActiveViolations();
        long suspendedShops = safeCountByStatus(shopRepository, ShopStatus.SUSPENDED)
                + safeCountByStatus(shopRepository, ShopStatus.RESTRICTED);

        Long pendingKyc = metricsGateway.countPendingKyc();
        BigDecimal vendorSales = metricsGateway.getVendorSales();
        BigDecimal vendorPayable = metricsGateway.getVendorPayable();
        BigDecimal refund = metricsGateway.getRefund();

        AdminDashboardRes.MetricAvailability av = metricsGateway.getAvailability();

        // Phase 4A §43 / §48 — Dashboard values shown to Admin
        // CRITICAL: platformRevenue is null until Finance backend is wired.
        // Do NOT fall back to periodRevenue/GMV — that would conflate Order Sales with Platform Revenue.
        return AdminDashboardRes.builder()
                .totalUsers(totalUsers)
                .totalShops(totalShops)
                .totalProducts(totalProducts)
                .totalOrders(totalOrders)
                .gmv(gmv)
                .platformRevenue(platformRevenue) // null when Finance not wired → UI shows N/A
                .vendorSales(vendorSales)
                .vendorPayable(vendorPayable)
                .refund(refund)
                .pendingKyc(pendingKyc)
                .pendingModeration(pendingModeration)
                .openEscalation(openEscalation)
                .openViolation(openViolation)
                .suspendedShops(suspendedShops)
                .recentActivities(activities)
                .revenueTrend(revenueTrend)
                .availability(av != null
                        ? av
                        : AdminDashboardRes.MetricAvailability.builder()
                                .kyc(false).vendorSales(false)
                                .vendorPayable(false).refund(false).platformRevenue(false).build())
                .build();
    }

    /* === Phase 4A — defensive count helpers (graceful degradation) === */

    private long safeCount(org.springframework.data.repository.CrudRepository<?, ?> repo) {
        try {
            return repo.count();
        } catch (Exception ex) {
            log.warn("Count failed (likely empty / unavailable collection): {}", ex.getMessage());
            return 0L;
        }
    }

    private long safeCountByStatus(ShopRepository repo, ShopStatus status) {
        try {
            return repo.countByStatus(status);
        } catch (Exception ex) {
            log.warn("countByStatus({}) failed: {}", status, ex.getMessage());
            return 0L;
        }
    }

    private long safeCountByStatusIn(EscalationRepository repo, Set<Escalation.Status> statuses) {
        try {
            return repo.countByStatusIn(statuses);
        } catch (Exception ex) {
            log.warn("escalation countByStatusIn failed: {}", ex.getMessage());
            return 0L;
        }
    }

    /**
     * Count active violations: resolvedAt IS NULL.
     * Violation domain model uses resolvedAt == null to represent open/active state.
     * ViolationStatus enum (Phase 3C) is NOT persisted on Violation document.
     *
     * <p>Phase 1: chuyển sang repository count để tối ưu — trước đây dùng
     * {@code findAll().stream().filter(...).count()} gây load toàn bộ collection
     * vào memory.</p>
     */
    private long countActiveViolations() {
        try {
            return violationRepository.countByResolvedAtIsNull();
        } catch (Exception ex) {
            log.warn("Active violations count failed: {}", ex.getMessage());
            return 0L;
        }
    }

    private List<Order> safeFindAllOrders() {
        try {
            return orderRepository.findAll();
        } catch (Exception ex) {
            log.warn("Order findAll failed (likely empty / unavailable collection): {}", ex.getMessage());
            return Collections.emptyList();
        }
    }

    private long countPendingModeration() {
        try {
            return productRepository.countByModerationStatusIn(PENDING_MODERATION_STATUSES)
                    + reviewRepository.countByPipelineModerationStatusIn(PENDING_MODERATION_STATUSES);
        } catch (Exception ex) {
            log.warn("Pending moderation count failed: {}", ex.getMessage());
            return 0L;
        }
    }

    /* === Existing revenue / period / trend logic (preserved) === */

    /**
     * Normalize period to a known uppercase token. Anything unknown → WEEK (default).
     */
    private static String normalizePeriod(String period) {
        if (period == null) return "WEEK";
        String norm = period.trim().toUpperCase(new Locale("en"));
        switch (norm) {
            case "DAY": case "DAILY": return "DAY";
            case "WEEK": case "WEEKLY": return "WEEK";
            case "MONTH": case "MONTHLY": return "MONTH";
            case "QUARTER": case "QUARTERLY": return "QUARTER";
            case "YEAR": case "YEARLY": return "YEAR";
            case "ALL": case "ALL_TIME": case "ALL-TIME": return "ALL";
            default: return "WEEK";
        }
    }

    private List<DailyMetric> buildRevenueTrendByPeriod(List<Order> allOrders, String period) {
        switch (period) {
            case "DAY":     return aggregateDays(allOrders, 14);     // last 14 days
            case "WEEK":    return aggregateWeeks(allOrders, 8);     // last 8 weeks
            case "MONTH":   return aggregateMonths(allOrders, 12);   // last 12 months
            case "QUARTER": return aggregateQuarters(allOrders, 8);  // last 8 quarters
            case "YEAR":    return aggregateYears(allOrders, 6);     // last 6 years
            case "ALL":     return aggregateAllTime(allOrders);
            default:        return aggregateWeeks(allOrders, 8);
        }
    }

    /**
     * Compute total revenue for the same window used by the trend buckets,
     * so the displayed "Total Revenue" matches the selected period instead of
     * always showing all-time.
     */
    private BigDecimal computePeriodRevenue(List<Order> allOrders, String period) {
        if (period == null) return BigDecimal.ZERO;
        switch (period) {
            case "DAY":     return sumDeliveredRevenue(allOrders, daysWindowPredicate(14));
            case "WEEK":    return sumDeliveredRevenue(allOrders, weeksWindowPredicate(8));
            case "MONTH":   return sumDeliveredRevenue(allOrders, monthsWindowPredicate(12));
            case "QUARTER": return sumDeliveredRevenue(allOrders, quartersWindowPredicate(8));
            case "YEAR":    return sumDeliveredRevenue(allOrders, yearsWindowPredicate(6));
            case "ALL":     return sumDeliveredRevenue(allOrders, o -> true);
            default:        return sumDeliveredRevenue(allOrders, weeksWindowPredicate(8));
        }
    }

    private static BigDecimal sumDeliveredRevenue(List<Order> allOrders, java.util.function.Predicate<Order> inWindow) {
        return allOrders.stream()
                .filter(o -> o.getStatus() == OrderStatus.DELIVERED)
                .filter(inWindow)
                .map(Order::getTotalAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static java.util.function.Predicate<Order> weeksWindowPredicate(int weeks) {
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(java.time.DayOfWeek.MONDAY);
        LocalDate from = weekStart.minusWeeks(weeks - 1);
        final LocalDate fFrom = from;
        final LocalDate fWeekStart = weekStart;
        return o -> {
            LocalDateTime t = o.getCreatedAt();
            if (t == null) return false;
            LocalDate ws = t.toLocalDate().with(java.time.DayOfWeek.MONDAY);
            return !ws.isBefore(fFrom) && !ws.isAfter(fWeekStart);
        };
    }

    private static java.util.function.Predicate<Order> monthsWindowPredicate(int months) {
        YearMonth thisMonth = YearMonth.now();
        YearMonth from = thisMonth.minusMonths(months - 1);
        final YearMonth fFrom = from;
        final YearMonth fThisMonth = thisMonth;
        return o -> {
            LocalDateTime t = o.getCreatedAt();
            if (t == null) return false;
            YearMonth ym = YearMonth.from(t);
            return !ym.isBefore(fFrom) && !ym.isAfter(fThisMonth);
        };
    }

    private static java.util.function.Predicate<Order> daysWindowPredicate(int days) {
        LocalDate today = LocalDate.now();
        LocalDate from = today.minusDays(days - 1);
        final LocalDate fFrom = from;
        final LocalDate fToday = today;
        return o -> {
            LocalDateTime t = o.getCreatedAt();
            if (t == null) return false;
            LocalDate d = t.toLocalDate();
            return !d.isBefore(fFrom) && !d.isAfter(fToday);
        };
    }

    private static java.util.function.Predicate<Order> quartersWindowPredicate(int quarters) {
        LocalDate today = LocalDate.now();
        int currentQ = (today.getMonthValue() - 1) / 3 + 1;
        int currentYear = today.getYear();
        int qIdx = currentQ - (quarters - 1);
        int fromYear = currentYear;
        while (qIdx <= 0) { qIdx += 4; fromYear--; }
        final int fCurrentYear = currentYear;
        final int fCurrentQ = currentQ;
        final int fFromYear = fromYear;
        final int fQIdx = qIdx;
        return o -> {
            LocalDateTime t = o.getCreatedAt();
            if (t == null) return false;
            LocalDate ld = t.toLocalDate();
            int y = ld.getYear();
            int qN = (ld.getMonthValue() - 1) / 3 + 1;
            int key = y * 10 + qN;
            int fromKey = fFromYear * 10 + fQIdx;
            int toKey = fCurrentYear * 10 + fCurrentQ;
            return key >= fromKey && key <= toKey;
        };
    }

    private static java.util.function.Predicate<Order> yearsWindowPredicate(int years) {
        int thisYear = LocalDate.now().getYear();
        int fromYear = thisYear - (years - 1);
        final int fThisYear = thisYear;
        final int fFromYear = fromYear;
        return o -> {
            LocalDateTime t = o.getCreatedAt();
            if (t == null) return false;
            int y = t.getYear();
            return y >= fFromYear && y <= fThisYear;
        };
    }

    /**
     * Day buckets — each day is one bucket. Returns labels like "01/09".
     */
    private List<DailyMetric> aggregateDays(List<Order> allOrders, int days) {
        DateTimeFormatter labelFmt = DateTimeFormatter.ofPattern("dd/MM", new Locale("vi"));
        Map<LocalDate, BigDecimal> rev = new LinkedHashMap<>();
        Map<LocalDate, Long> ord = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();
        for (int i = days - 1; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            rev.put(d, BigDecimal.ZERO);
            ord.put(d, 0L);
        }
        applyOrdersToBucket(allOrders, rev, ord, o -> o.getCreatedAt().toLocalDate());
        return buildSeries(rev, ord, labelFmt);
    }

    /**
     * Week buckets — Monday-Sunday, ISO week boundary.
     */
    private List<DailyMetric> aggregateWeeks(List<Order> allOrders, int weeks) {
        Locale vi = new Locale("vi");
        DateTimeFormatter labelFmt = DateTimeFormatter.ofPattern("'W'w", vi);
        Map<LocalDate, BigDecimal> rev = new LinkedHashMap<>();
        Map<LocalDate, Long> ord = new LinkedHashMap<>();
        LocalDate thisWeekStart = LocalDate.now().with(java.time.DayOfWeek.MONDAY);
        for (int i = weeks - 1; i >= 0; i--) {
            LocalDate ws = thisWeekStart.minusWeeks(i);
            rev.put(ws, BigDecimal.ZERO);
            ord.put(ws, 0L);
        }
        applyOrdersToBucket(allOrders, rev, ord, o ->
                o.getCreatedAt().toLocalDate().with(java.time.DayOfWeek.MONDAY));
        return buildSeries(rev, ord, labelFmt);
    }

    /**
     * Month buckets.
     */
    private List<DailyMetric> aggregateMonths(List<Order> allOrders, int months) {
        DateTimeFormatter labelFmt = DateTimeFormatter.ofPattern("MM/yyyy", new Locale("vi"));
        Map<YearMonth, BigDecimal> rev = new LinkedHashMap<>();
        Map<YearMonth, Long> ord = new LinkedHashMap<>();
        YearMonth thisMonth = YearMonth.now();
        for (int i = months - 1; i >= 0; i--) {
            YearMonth ym = thisMonth.minusMonths(i);
            rev.put(ym, BigDecimal.ZERO);
            ord.put(ym, 0L);
        }
        for (Order o : allOrders) {
            if (o.getCreatedAt() == null) continue;
            YearMonth ym = YearMonth.from(o.getCreatedAt());
            if (!rev.containsKey(ym)) continue;
            ord.merge(ym, 1L, Long::sum);
            if (o.getStatus() == OrderStatus.DELIVERED && o.getTotalAmount() != null) {
                rev.merge(ym, o.getTotalAmount(), BigDecimal::add);
            }
        }
        return buildSeriesYM(rev, ord, labelFmt);
    }

    /**
     * Quarter buckets — Q1=Jan-Mar, Q2=Apr-Jun, Q3=Jul-Sep, Q4=Oct-Dec.
     */
    private List<DailyMetric> aggregateQuarters(List<Order> allOrders, int quarters) {
        Map<String, BigDecimal> rev = new LinkedHashMap<>();
        Map<String, Long> ord = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();
        int currentQ = (today.getMonthValue() - 1) / 3 + 1;
        int currentYear = today.getYear();
        for (int i = quarters - 1; i >= 0; i--) {
            int qIdx = currentQ - i;
            int year = currentYear;
            while (qIdx <= 0) { qIdx += 4; year--; }
            String key = year + "-Q" + qIdx;
            rev.put(key, BigDecimal.ZERO);
            ord.put(key, 0L);
        }
        for (Order o : allOrders) {
            if (o.getCreatedAt() == null) continue;
            LocalDate ld = o.getCreatedAt().toLocalDate();
            int qN = (ld.getMonthValue() - 1) / 3 + 1;
            String key = ld.getYear() + "-Q" + qN;
            if (!rev.containsKey(key)) continue;
            ord.merge(key, 1L, Long::sum);
            if (o.getStatus() == OrderStatus.DELIVERED && o.getTotalAmount() != null) {
                rev.merge(key, o.getTotalAmount(), BigDecimal::add);
            }
        }
        List<DailyMetric> out = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> e : rev.entrySet()) {
            QuarterKey qk = QuarterKey.parse(e.getKey());
            String label = "Q" + qk.q + " " + qk.year;
            out.add(DailyMetric.builder()
                    .label(label)
                    .revenue(e.getValue())
                    .orders(ord.getOrDefault(e.getKey(), 0L))
                    .build());
        }
        return out;
    }

    /** Year buckets. */
    private List<DailyMetric> aggregateYears(List<Order> allOrders, int years) {
        DateTimeFormatter labelFmt = DateTimeFormatter.ofPattern("yyyy", new Locale("vi"));
        Map<Integer, BigDecimal> rev = new LinkedHashMap<>();
        Map<Integer, Long> ord = new LinkedHashMap<>();
        int thisYear = LocalDate.now().getYear();
        for (int i = years - 1; i >= 0; i--) {
            int y = thisYear - i;
            rev.put(y, BigDecimal.ZERO);
            ord.put(y, 0L);
        }
        for (Order o : allOrders) {
            if (o.getCreatedAt() == null) continue;
            int y = o.getCreatedAt().getYear();
            if (!rev.containsKey(y)) continue;
            ord.merge(y, 1L, Long::sum);
            if (o.getStatus() == OrderStatus.DELIVERED && o.getTotalAmount() != null) {
                rev.merge(y, o.getTotalAmount(), BigDecimal::add);
            }
        }
        List<DailyMetric> out = new ArrayList<>();
        for (Map.Entry<Integer, BigDecimal> e : rev.entrySet()) {
            out.add(DailyMetric.builder()
                    .label(String.valueOf(e.getKey()))
                    .revenue(e.getValue())
                    .orders(ord.getOrDefault(e.getKey(), 0L))
                    .build());
        }
        return out;
    }

    /**
     * All-time — single bucket. Spec preserves existing revenue definition
     * (DELIVERED only).
     */
    private List<DailyMetric> aggregateAllTime(List<Order> allOrders) {
        BigDecimal total = allOrders.stream()
                .filter(o -> o.getStatus() == OrderStatus.DELIVERED)
                .map(Order::getTotalAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long count = allOrders.stream().filter(o -> o.getStatus() == OrderStatus.DELIVERED).count();
        return List.of(DailyMetric.builder()
                .label("All-time")
                .revenue(total)
                .orders(count)
                .build());
    }

    @FunctionalInterface
    private interface KeyExtractor<T> {
        T extract(Order o);
    }

    private static void applyOrdersToBucket(List<Order> allOrders,
                                            Map<LocalDate, BigDecimal> rev,
                                            Map<LocalDate, Long> ord,
                                            KeyExtractor<LocalDate> keyFn) {
        for (Order o : allOrders) {
            if (o.getCreatedAt() == null) continue;
            LocalDate k = keyFn.extract(o);
            if (!rev.containsKey(k)) continue;
            ord.merge(k, 1L, Long::sum);
            if (o.getStatus() == OrderStatus.DELIVERED && o.getTotalAmount() != null) {
                rev.merge(k, o.getTotalAmount(), BigDecimal::add);
            }
        }
    }

    private static List<DailyMetric> buildSeries(Map<LocalDate, BigDecimal> rev,
                                                  Map<LocalDate, Long> ord,
                                                  DateTimeFormatter labelFmt) {
        List<DailyMetric> out = new ArrayList<>();
        for (Map.Entry<LocalDate, BigDecimal> e : rev.entrySet()) {
            out.add(DailyMetric.builder()
                    .label(labelFmt.format(e.getKey()))
                    .revenue(e.getValue())
                    .orders(ord.getOrDefault(e.getKey(), 0L))
                    .build());
        }
        return out;
    }

    private static List<DailyMetric> buildSeriesYM(Map<YearMonth, BigDecimal> rev,
                                                    Map<YearMonth, Long> ord,
                                                    DateTimeFormatter labelFmt) {
        List<DailyMetric> out = new ArrayList<>();
        for (Map.Entry<YearMonth, BigDecimal> e : rev.entrySet()) {
            out.add(DailyMetric.builder()
                    .label(labelFmt.format(e.getKey()))
                    .revenue(e.getValue())
                    .orders(ord.getOrDefault(e.getKey(), 0L))
                    .build());
        }
        return out;
    }

    private static class QuarterKey {
        final int year;
        final int q;
        QuarterKey(int year, int q) { this.year = year; this.q = q; }
        static QuarterKey parse(String key) {
            String[] parts = key.split("-Q");
            return new QuarterKey(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
        }
    }


    private List<AdminDashboardRes.RecentActivity> buildRecentActivities(List<Order> recentOrders) {
        List<AdminDashboardRes.RecentActivity> activities = new ArrayList<>();
        for (Order order : recentOrders) {
            if (order.getCreatedAt() == null) continue;
            long minutesAgo = ChronoUnit.MINUTES.between(order.getCreatedAt(), LocalDateTime.now());
            String timeAgo;
            if (minutesAgo < 60) {
                timeAgo = minutesAgo + " phút trước";
            } else if (minutesAgo < 1440) {
                timeAgo = (minutesAgo / 60) + " giờ trước";
            } else {
                timeAgo = (minutesAgo / 1440) + " ngày trước";
            }
            String idShort = order.getId() != null
                    ? order.getId().substring(0, Math.min(8, order.getId().length()))
                    : "N/A";
            activities.add(AdminDashboardRes.RecentActivity.builder()
                    .type("ORDER")
                    .description("Đơn hàng mới #" + idShort + " - " + order.getStatus())
                    .time(timeAgo)
                    .orderId(order.getId())
                    .build());
        }
        return activities;
    }
}
