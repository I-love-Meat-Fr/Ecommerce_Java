package com.ecommerce.cnj70.service.impl;

import com.ecommerce.cnj70.document.Order;
import com.ecommerce.cnj70.document.Shop;
import com.ecommerce.cnj70.document.User;
import com.ecommerce.cnj70.dto.response.AdminDashboardRes;
import com.ecommerce.cnj70.enums.OrderStatus;
import com.ecommerce.cnj70.enums.ReportFormat;
import com.ecommerce.cnj70.enums.ReportType;
import com.ecommerce.cnj70.repository.OrderRepository;
import com.ecommerce.cnj70.repository.ShopRepository;
import com.ecommerce.cnj70.repository.UserRepository;
import com.ecommerce.cnj70.service.AdminReportService;
import com.ecommerce.cnj70.service.AdminService;
import com.ecommerce.cnj70.service.ReportFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * PHASE 5 — Implementation cho {@link AdminReportService}.
 *
 * <p>Truy vấn trực tiếp từ các {@code Repository} đã có sẵn trong project, kết hợp
 * {@link AdminService#getDashboardStatsByPeriod(String)} để reuse logic Financial metrics
 * đã được audit kỹ ở Phase 4A (GMV ≠ Platform Revenue ≠ Vendor Sales ≠ Vendor Payable ≠ Refund).</p>
 *
 * <h3>Defensive defaults</h3>
 * <ul>
 *   <li>Filter null/blank → ALL.</li>
 *   <li>fromDate sau toDate → swap lại (hoặc rỗng range).</li>
 *   <li>Period không hợp lệ → WEEK.</li>
 *   <li>Repository throw → log warn, trả list rỗng (admin thấy "không có dữ liệu" thay vì crash).</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminReportServiceImpl implements AdminReportService {

    private final OrderRepository orderRepository;
    private final ShopRepository shopRepository;
    private final UserRepository userRepository;
    private final AdminService adminService;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DATE_ONLY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Locale VI = new Locale("vi", "VN");

    /* ===================== Headers ===================== */

    @Override
    public String[] headers(ReportType type) {
        switch (type) {
            case ORDERS:   return new String[]{"Mã đơn", "Khách hàng", "Email", "Số điện thoại",
                                                "Số shop", "Tạm tính", "Phí ship", "Giảm giá", "Tổng cộng",
                                                "Trạng thái", "Thanh toán", "Ngày tạo", "Ngày giao"};
            case REVENUE:  return new String[]{"Mốc thời gian", "Doanh thu (GMV)", "Số đơn đã giao", "Số đơn tổng"};
            case FINANCIAL: return new String[]{"Chỉ số", "Giá trị", "Ghi chú"};
            case USERS:    return new String[]{"Mã", "Email", "Họ tên", "Số điện thoại",
                                                "Vai trò", "Trạng thái", "Ngày đăng ký"};
            case SHOPS:    return new String[]{"Mã shop", "Tên shop", "Chủ shop", "Trạng thái",
                                                "KYC", "Hoạt động", "Ngày tạo"};
            default:       return new String[]{};
        }
    }

    /* ===================== Default Title ===================== */

    @Override
    public String defaultTitle(ReportType type, ReportFilter filter) {
        String dateRange = formatDateRange(filter);
        switch (type) {
            case ORDERS:    return "Báo cáo đơn hàng" + dateRange
                                + (filter != null && notBlank(filter.getStatus())
                                    ? " — Trạng thái: " + filter.getStatus() : "");
            case REVENUE:   return "Báo cáo doanh thu (GMV)"
                                + (filter != null && notBlank(filter.getPeriod())
                                    ? " — " + filter.getPeriod().toUpperCase() : " — WEEK")
                                + dateRange;
            case FINANCIAL: return "Báo cáo tài chính tổng hợp" + dateRange
                                + " (GMV ≠ Platform Revenue ≠ Vendor Sales ≠ Vendor Payable ≠ Refund)";
            case USERS:     return "Báo cáo người dùng" + dateRange
                                + (filter != null && notBlank(filter.getRole())
                                    ? " — Vai trò: " + filter.getRole() : "");
            case SHOPS:     return "Báo cáo cửa hàng" + dateRange
                                + (filter != null && notBlank(filter.getShopStatus())
                                    ? " — Trạng thái: " + filter.getShopStatus() : "");
            default:        return "Báo cáo";
        }
    }

    /* ===================== Suggested Filename ===================== */

    @Override
    public String suggestedFilename(ReportType type, ReportFilter filter) {
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmm"));
        return "report-" + type.name().toLowerCase() + "-" + ts;
    }

    /* ===================== Build Rows ===================== */

    @Override
    public List<String[]> buildRows(ReportType type, ReportFilter rawFilter) {
        ReportFilter filter = normalizeFilter(rawFilter);
        switch (type) {
            case ORDERS:   return buildOrdersRows(filter);
            case REVENUE:  return buildRevenueRows(filter);
            case FINANCIAL: return buildFinancialRows(filter);
            case USERS:    return buildUsersRows(filter);
            case SHOPS:    return buildShopsRows(filter);
            default:       return List.of();
        }
    }

    /* ===================== Filter normalization ===================== */

    @Override
    public ReportFilter normalizeFilter(ReportFilter raw) {
        if (raw == null) return ReportFilter.builder().build();
        LocalDate from = raw.getFromDate();
        LocalDate to = raw.getToDate();
        // Swap nếu from > to.
        if (from != null && to != null && from.isAfter(to)) {
            LocalDate tmp = from;
            from = to;
            to = tmp;
        }
        return ReportFilter.builder()
                .status(trimToNull(raw.getStatus()))
                .period(normalizePeriod(raw.getPeriod()))
                .role(trimToNull(raw.getRole()))
                .shopStatus(trimToNull(raw.getShopStatus()))
                .kycStatus(trimToNull(raw.getKycStatus()))
                .accountStatus(trimToNull(raw.getAccountStatus()))
                .fromDate(from)
                .toDate(to)
                .q(trimToNull(raw.getQ()))
                .build();
    }

    private static String normalizePeriod(String raw) {
        if (raw == null || raw.isBlank()) return "WEEK";
        String norm = raw.trim().toUpperCase(VI);
        Set<String> allowed = Set.of("DAY", "WEEK", "MONTH", "QUARTER", "YEAR", "ALL");
        return allowed.contains(norm) ? norm : "WEEK";
    }

    /* ===================== Orders ===================== */

    private List<String[]> buildOrdersRows(ReportFilter f) {
        List<Order> orders;
        try {
            orders = new ArrayList<>(orderRepository.findAll());
        } catch (Exception ex) {
            log.warn("OrderRepository.findAll failed: {}", ex.getMessage());
            return List.of();
        }

        // Sort: mới nhất trước.
        orders.sort(Comparator.comparing(
                (Order o) -> o.getCreatedAt() != null ? o.getCreatedAt() : LocalDateTime.MIN
        ).reversed());

        List<String[]> rows = new ArrayList<>();
        for (Order o : orders) {
            if (!matchesDate(o.getCreatedAt(), f.getFromDate(), f.getToDate())) continue;
            if (f.getStatus() != null) {
                OrderStatus st = o.getStatus();
                if (st == null || !st.name().equalsIgnoreCase(f.getStatus())) continue;
            }
            if (notBlank(f.getQ())) {
                String q = f.getQ().toLowerCase();
                String userName = o.getUserName() == null ? "" : o.getUserName().toLowerCase();
                String userId = o.getUserId() == null ? "" : o.getUserId().toLowerCase();
                if (!userName.contains(q) && !userId.contains(q)) continue;
            }
            rows.add(toOrderRow(o));
        }
        return rows;
    }

    private String[] toOrderRow(Order o) {
        int shopCount = o.getItems() != null
                ? (int) o.getItems().stream().map(it -> it.getShopId()).distinct().count()
                : 0;
        return new String[]{
                safe(o.getId()),
                safe(o.getUserName()),
                safe(o.getUserEmail()),
                safe(o.getUserPhone()),
                String.valueOf(shopCount),
                money(o.getSubtotal()),
                money(o.getShippingFee()),
                money(o.getDiscount()),
                money(o.getTotalAmount()),
                o.getStatus() == null ? "" : translateOrderStatus(o.getStatus()),
                o.getPaymentStatus() == null ? "" : o.getPaymentStatus().name(),
                o.getCreatedAt() == null ? "" : o.getCreatedAt().format(DATE_FMT),
                o.getDeliveredAt() == null ? "" : o.getDeliveredAt().format(DATE_FMT)
        };
    }

    /* ===================== Revenue ===================== */

    private List<String[]> buildRevenueRows(ReportFilter f) {
        // Reuse AdminServiceImpl.buildRevenueTrendByPeriod (private) bằng cách gọi public API:
        // AdminService.getDashboardStatsByPeriod() trả về AdminDashboardRes với revenueTrend.
        // Tuy nhiên dashboard load toàn bộ orders nên chậm nếu dataset lớn — chấp nhận được cho
        // Admin Report vì không gọi thường xuyên.
        AdminDashboardRes stats = adminService.getDashboardStatsByPeriod(f.getPeriod());
        List<AdminDashboardRes.DailyMetric> trend = stats == null ? null : stats.getRevenueTrend();
        if (trend == null || trend.isEmpty()) {
            String[] emptyRow = new String[]{"(không có dữ liệu)", "0", "0", "0"};
            return java.util.Collections.singletonList(emptyRow);
        }

        List<String[]> rows = new ArrayList<>();
        long totalDelivered = 0, totalAll = 0;
        BigDecimal totalRev = BigDecimal.ZERO;
        for (AdminDashboardRes.DailyMetric m : trend) {
            BigDecimal rev = m.getRevenue() != null ? m.getRevenue() : BigDecimal.ZERO;
            rows.add(new String[]{
                    safe(m.getLabel()),
                    money(rev),
                    String.valueOf(m.getOrders()),
                    String.valueOf(m.getOrders()) // số đơn trong bucket = tổng đơn đếm theo createdAt
            });
            totalRev = totalRev.add(rev);
            totalDelivered += m.getOrders();
            totalAll += m.getOrders();
        }
        // Footer row tổng.
        rows.add(new String[]{
                "TỔNG",
                money(totalRev),
                String.valueOf(totalDelivered),
                String.valueOf(totalAll)
        });
        return rows;
    }

    /* ===================== Financial ===================== */

    private List<String[]> buildFinancialRows(ReportFilter f) {
        AdminDashboardRes stats;
        try {
            stats = adminService.getDashboardStatsByPeriod("ALL");
        } catch (Exception ex) {
            log.warn("Dashboard stats failed: {}", ex.getMessage());
            stats = null;
        }
        if (stats == null) {
            String[] emptyRow = new String[]{"(không có dữ liệu)", "-", "Không thể truy vấn dashboard metrics"};
            return java.util.Collections.singletonList(emptyRow);
        }
        AdminDashboardRes.MetricAvailability av = stats.getAvailability();

        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{
                "GMV (Gross Merchandise Value)",
                stats.getGmv() != null ? money(stats.getGmv()) : "N/A",
                "sum(Order.totalAmount) for DELIVERED — gồm ship, trừ voucher"
        });
        rows.add(new String[]{
                "Platform Revenue",
                stats.getPlatformRevenue() != null ? money(stats.getPlatformRevenue())
                        : (av != null && !av.isPlatformRevenue() ? "N/A (chưa wire Finance)" : "0"),
                "sum(Order.commissionAmount) for DELIVERED — phí sàn nhận"
        });
        rows.add(new String[]{
                "Vendor Sales",
                stats.getVendorSales() != null ? money(stats.getVendorSales()) : "N/A",
                "sum(OrderItem.subtotal) for DELIVERED — doanh số bán hàng (chưa trừ hoa hồng)"
        });
        rows.add(new String[]{
                "Vendor Payable",
                stats.getVendorPayable() != null ? money(stats.getVendorPayable()) : "N/A",
                "sum(Order.vendorNetAmount) for DELIVERED — số tiền sàn nợ vendor"
        });
        rows.add(new String[]{
                "Refund",
                stats.getRefund() != null ? money(stats.getRefund()) : "N/A",
                "sum(RefundRequest.settledAmount) for SUCCEEDED"
        });

        // Tổng số liệu tuân thủ platform.
        rows.add(new String[]{"---", "---", "---"});
        rows.add(new String[]{
                "Tổng Users",
                String.valueOf(stats.getTotalUsers()),
                "UserRepository.count()"
        });
        rows.add(new String[]{
                "Tổng Shops",
                String.valueOf(stats.getTotalShops()),
                "ShopRepository.count()"
        });
        rows.add(new String[]{
                "Tổng Products",
                String.valueOf(stats.getTotalProducts()),
                "ProductRepository.count()"
        });
        rows.add(new String[]{
                "Tổng Orders",
                String.valueOf(stats.getTotalOrders()),
                "OrderRepository.count()"
        });

        return rows;
    }

    /* ===================== Users ===================== */

    private List<String[]> buildUsersRows(ReportFilter f) {
        List<User> users;
        try {
            users = new ArrayList<>(userRepository.findAll());
        } catch (Exception ex) {
            log.warn("UserRepository.findAll failed: {}", ex.getMessage());
            return List.of();
        }
        users.sort(Comparator.comparing(
                (User u) -> u.getCreatedAt() != null ? u.getCreatedAt() : LocalDateTime.MIN
        ).reversed());

        List<String[]> rows = new ArrayList<>();
        for (User u : users) {
            if (!matchesDate(u.getCreatedAt(), f.getFromDate(), f.getToDate())) continue;
            if (notBlank(f.getRole()) && (u.getRole() == null
                    || !u.getRole().name().equalsIgnoreCase(f.getRole()))) continue;
            if (notBlank(f.getAccountStatus()) && (u.getStatus() == null
                    || !u.getStatus().name().equalsIgnoreCase(f.getAccountStatus()))) continue;
            if (notBlank(f.getQ())) {
                String q = f.getQ().toLowerCase();
                String email = u.getEmail() == null ? "" : u.getEmail().toLowerCase();
                String name = u.getFullName() == null ? "" : u.getFullName().toLowerCase();
                if (!email.contains(q) && !name.contains(q)) continue;
            }
            rows.add(new String[]{
                    safe(u.getId()),
                    safe(u.getEmail()),
                    safe(u.getFullName()),
                    safe(u.getPhone()),
                    u.getRole() == null ? "" : u.getRole().name(),
                    u.getStatus() == null ? "" : u.getStatus().name(),
                    u.getCreatedAt() == null ? "" : u.getCreatedAt().format(DATE_FMT)
            });
        }
        return rows;
    }

    /* ===================== Shops ===================== */

    private List<String[]> buildShopsRows(ReportFilter f) {
        List<Shop> shops;
        try {
            shops = new ArrayList<>(shopRepository.findAll());
        } catch (Exception ex) {
            log.warn("ShopRepository.findAll failed: {}", ex.getMessage());
            return List.of();
        }
        shops.sort(Comparator.comparing(
                (Shop s) -> s.getCreatedAt() != null ? s.getCreatedAt() : LocalDateTime.MIN
        ).reversed());

        List<String[]> rows = new ArrayList<>();
        for (Shop s : shops) {
            if (!matchesDate(s.getCreatedAt(), f.getFromDate(), f.getToDate())) continue;
            if (notBlank(f.getShopStatus()) && (s.getStatus() == null
                    || !s.getStatus().name().equalsIgnoreCase(f.getShopStatus()))) continue;
            if (notBlank(f.getKycStatus()) && (s.getKycStatus() == null
                    || !s.getKycStatus().name().equalsIgnoreCase(f.getKycStatus()))) continue;
            if (notBlank(f.getQ())) {
                String q = f.getQ().toLowerCase();
                String shopName = s.getShopName() == null ? "" : s.getShopName().toLowerCase();
                if (!shopName.contains(q)) continue;
            }
            rows.add(new String[]{
                    safe(s.getId()),
                    safe(s.getShopName()),
                    safe(s.getOwnerId()),
                    s.getStatus() == null ? "" : s.getStatus().name(),
                    s.getKycStatus() == null ? "" : s.getKycStatus().name(),
                    s.isActive() ? "Có" : "Không",
                    s.getCreatedAt() == null ? "" : s.getCreatedAt().format(DATE_FMT)
            });
        }
        return rows;
    }

    /* ===================== Helpers ===================== */

    private static boolean matchesDate(LocalDateTime t, LocalDate from, LocalDate to) {
        if (t == null) return from == null && to == null;
        LocalDate d = t.toLocalDate();
        if (from != null && d.isBefore(from)) return false;
        if (to != null && d.isAfter(to)) return false;
        return true;
    }

    private static String formatDateRange(ReportFilter f) {
        if (f == null) return "";
        boolean hasFrom = f.getFromDate() != null;
        boolean hasTo = f.getToDate() != null;
        if (!hasFrom && !hasTo) return "";
        if (hasFrom && hasTo) {
            return " (" + f.getFromDate().format(DATE_ONLY) + " — " + f.getToDate().format(DATE_ONLY) + ")";
        }
        if (hasFrom) return " (từ " + f.getFromDate().format(DATE_ONLY) + ")";
        return " (đến " + f.getToDate().format(DATE_ONLY) + ")";
    }

    private static String translateOrderStatus(OrderStatus st) {
        switch (st) {
            case PENDING:    return "Chờ xác nhận";
            case PREPARING:  return "Đang chuẩn bị";
            case SHIPPING:   return "Đang giao";
            case DELIVERED:  return "Đã giao";
            case CANCELLED:  return "Đã hủy";
            default:         return st.name();
        }
    }

    private static String money(BigDecimal value) {
        if (value == null) return "0";
        // Dùng format VND với dấu phân cách hàng nghìn + " đ" — phù hợp tiếng Việt.
        return String.format("%,.0f đ", value.doubleValue());
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}