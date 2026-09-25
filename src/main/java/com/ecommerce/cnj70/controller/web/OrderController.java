package com.ecommerce.cnj70.controller.web;

import com.ecommerce.cnj70.document.Cart;
import com.ecommerce.cnj70.document.Order;
import com.ecommerce.cnj70.document.User;
import com.ecommerce.cnj70.dto.checkout.CheckoutValidationRes;
import com.ecommerce.cnj70.dto.request.CheckoutReq;
import com.ecommerce.cnj70.dto.response.OrderHistoryRes;
import com.ecommerce.cnj70.enums.PaymentStatus;
import com.ecommerce.cnj70.enums.ShippingStatus;
import com.ecommerce.cnj70.repository.UserRepository;
import com.ecommerce.cnj70.security.CustomUserDetails;
import com.ecommerce.cnj70.service.CartService;
import com.ecommerce.cnj70.service.OrderService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class OrderController {

    /** Đồng bộ với CartController.SESSION_APPLIED_VOUCHER. */
    public static final String SESSION_APPLIED_VOUCHER = "appliedVoucher";

    private static final BigDecimal SHIPPING_FEE = new BigDecimal("15000");

    private final OrderService orderService;
    private final UserRepository userRepository;
    private final CartService cartService;

    @GetMapping("/checkout")
    public String checkoutPage(@AuthenticationPrincipal CustomUserDetails user,
                               HttpSession session,
                               Model model) {
        if (user == null) {
            return "redirect:/auth/login";
        }

        User dbUser = userRepository.findById(user.getId()).orElse(null);
        // Defensive: nếu JWT còn valid nhưng user đã bị xóa khỏi DB (vd: DB reset,
        // admin xóa user), buộc logout + redirect về login với thông báo rõ ràng.
        // Trước đây method này tiếp tục render với user=null khiến lỗi "Không tìm thấy
        // người dùng" chỉ bùng ra ở POST /checkout — UX rất tệ.
        if (dbUser == null) {
            invalidateSession(session);
            return "redirect:/auth/login?expired=true";
        }

        Cart cart = cartService.getCartByUserId(user.getId());

        // ===== Validate cart trước khi cho Checkout =====
        // Nếu có bất kỳ item nào không còn hợp lệ (Product không ACTIVE,
        // Shop bị suspend, hết hàng, ...) — redirect về /cart để user xử lý.
        java.util.Map<String, com.ecommerce.cnj70.dto.cart.CartItemValidation> invalidItems =
                cartService.validateCartItems(cart);
        if (!invalidItems.isEmpty()) {
            session.setAttribute("checkoutBlockedReason",
                    "Giỏ hàng có " + invalidItems.size()
                            + " sản phẩm không hợp lệ. Vui lòng xóa hoặc cập nhật trước khi thanh toán.");
            return "redirect:/cart";
        }

        BigDecimal cartTotal = cartService.calculateTotal(cart);
        int totalQuantity = cart.getItems().stream().mapToInt(Cart.CartItem::getQuantity).sum();

        Map<String, List<Cart.CartItem>> itemsByShop = cart.getItems().stream()
                .collect(Collectors.groupingBy(
                        item -> item.getShopId() != null ? item.getShopId() : "default",
                        LinkedHashMap::new,
                        Collectors.toList()));

        // Đọc voucher từ session (đã được CartController lưu khi user apply)
        CartController.AppliedVoucher appliedVoucher = readAppliedVoucher(session);
        BigDecimal discount = appliedVoucher != null ? appliedVoucher.getDiscount() : BigDecimal.ZERO;
        BigDecimal finalTotal = cartTotal.add(SHIPPING_FEE).subtract(discount);
        if (finalTotal.compareTo(BigDecimal.ZERO) < 0) {
            finalTotal = BigDecimal.ZERO;
        }

        // Pre-fill form with user data + applied voucher (TASK #13)
        CheckoutReq.CheckoutReqBuilder builder = CheckoutReq.builder()
                .phone(dbUser != null ? dbUser.getPhone() : null)
                .shippingAddress(dbUser != null ? dbUser.getAddress() : null);
        if (appliedVoucher != null) {
            builder.voucherCode(appliedVoucher.getCode());
            builder.discount(discount);
        }
        CheckoutReq checkoutReq = builder.build();

        model.addAttribute("checkoutReq", checkoutReq);
        model.addAttribute("cart", cart);
        model.addAttribute("cartTotal", cartTotal);
        model.addAttribute("totalQuantity", totalQuantity);
        model.addAttribute("itemsByShop", itemsByShop);
        model.addAttribute("user", dbUser);
        model.addAttribute("shippingFee", SHIPPING_FEE);
        model.addAttribute("appliedVoucher", appliedVoucher);
        model.addAttribute("discount", discount);
        model.addAttribute("finalTotal", finalTotal);

        return "web/checkout";
    }

    @PostMapping("/checkout")
    public String checkout(@AuthenticationPrincipal CustomUserDetails user,
                          @ModelAttribute @Valid CheckoutReq request,
                          BindingResult bindingResult,
                          HttpSession session,
                          Model model) {
        if (user == null) {
            return "redirect:/auth/login";
        }

        // Defensive: trước khi gọi OrderService.createOrder (vốn load lại user từ DB),
        // check trực tiếp để fail-fast với thông báo thân thiện.
        // Trường hợp phổ biến: JWT cookie còn hạn nhưng user đã bị xóa khỏi DB
        // (vd: DB reset, admin xóa user). Trước đây OrderServiceImpl throw
        // ResourceNotFoundException → GlobalExceptionHandler → 404 page với message
        // "Không tìm thấy người dùng" — gây hiểu nhầm cho user rằng checkout bị lỗi.
        User dbUser = userRepository.findById(user.getId()).orElse(null);
        if (dbUser == null) {
            invalidateSession(session);
            return "redirect:/auth/login?expired=true";
        }
        Cart cart = cartService.getCartByUserId(user.getId());

        // ===== Validate cart trước khi tạo Order (defense-in-depth) =====
        java.util.Map<String, com.ecommerce.cnj70.dto.cart.CartItemValidation> invalidItems =
                cartService.validateCartItems(cart);
        if (!invalidItems.isEmpty()) {
            return "redirect:/cart";
        }

        BigDecimal cartTotal = cartService.calculateTotal(cart);
        int totalQuantity = cart.getItems().stream().mapToInt(Cart.CartItem::getQuantity).sum();

        Map<String, List<Cart.CartItem>> itemsByShop = cart.getItems().stream()
                .collect(Collectors.groupingBy(
                        item -> item.getShopId() != null ? item.getShopId() : "default",
                        LinkedHashMap::new,
                        Collectors.toList()));

        CartController.AppliedVoucher appliedVoucher = readAppliedVoucher(session);
        BigDecimal discount = appliedVoucher != null ? appliedVoucher.getDiscount() : BigDecimal.ZERO;
        BigDecimal finalTotal = cartTotal.add(SHIPPING_FEE).subtract(discount);
        if (finalTotal.compareTo(BigDecimal.ZERO) < 0) {
            finalTotal = BigDecimal.ZERO;
        }

        model.addAttribute("cart", cart);
        model.addAttribute("cartTotal", cartTotal);
        model.addAttribute("totalQuantity", totalQuantity);
        model.addAttribute("itemsByShop", itemsByShop);
        model.addAttribute("user", dbUser);
        model.addAttribute("shippingFee", SHIPPING_FEE);
        model.addAttribute("appliedVoucher", appliedVoucher);
        model.addAttribute("discount", discount);
        model.addAttribute("finalTotal", finalTotal);

        // If validation errors, return to checkout with errors
        if (bindingResult.hasErrors()) {
            model.addAttribute("error", "Vui lòng điền đầy đủ thông tin bắt buộc.");
            return "web/checkout";
        }

        // If cart is empty, redirect back
        if (cart.getItems().isEmpty()) {
            model.addAttribute("error", "Giỏ hàng của bạn đang trống. Vui lòng thêm sản phẩm trước khi thanh thanh toán.");
            return "web/checkout";
        }

        try {
            // Đảm bảo CheckoutReq mang theo voucherCode từ session (nếu user quên)
            if ((request.getVoucherCode() == null || request.getVoucherCode().isBlank())
                    && appliedVoucher != null) {
                request.setVoucherCode(appliedVoucher.getCode());
                request.setDiscount(discount);
            }

            Order order = orderService.createOrder(user.getId(), request);

            // Xóa voucher đã dùng khỏi session (đã được Order lưu vào Order.voucherCode)
            session.removeAttribute(SESSION_APPLIED_VOUCHER);

            return "redirect:/orders/" + order.getId();
        } catch (Exception e) {
            String errorMessage = e.getMessage();

            // ===== Xử lý lỗi Voucher =====
            // OrderServiceImpl.createOrder luôn wrap voucher errors với prefix
            // "Voucher không hợp lệ:" (cả BadRequestException + ResourceNotFoundException
            // từ VoucherService.validateForCheckout).
            //
            // Khi backend từ chối voucher, ta cần:
            //   1) Clear SESSION_APPLIED_VOUCHER — tránh user retry với cùng voucher
            //   2) Override model attrs để UI không còn hiển thị discount row cũ
            //   3) Clear voucherCode/discount trên CheckoutReq request — form re-submit
            //      sẽ không gửi lại voucher bị reject
            //   4) Set voucherError để template hiển thị inline + error để banner trên đầu
            boolean isVoucherError = errorMessage != null
                    && errorMessage.startsWith("Voucher không hợp lệ");

            if (isVoucherError) {
                session.removeAttribute(SESSION_APPLIED_VOUCHER);

                // Tính lại finalTotal không có discount
                BigDecimal recomputedFinal = cartTotal.add(SHIPPING_FEE);

                // Override model attrs (đã add trước đó với stale voucher)
                model.addAttribute("appliedVoucher", null);
                model.addAttribute("discount", BigDecimal.ZERO);
                model.addAttribute("finalTotal", recomputedFinal);

                // Inline voucher error cho template (render cạnh discount row)
                model.addAttribute("voucherError", errorMessage);
                // Top-of-form error banner
                model.addAttribute("error", errorMessage);

                // Clear voucherCode/discount trên request để form re-submit KHÔNG
                // gửi lại mã bị reject. Spring dùng cùng instance cho @ModelAttribute
                // nên update này cũng phản ánh lên model attribute "checkoutReq".
                request.setVoucherCode(null);
                request.setDiscount(BigDecimal.ZERO);

                return "web/checkout";
            }

            model.addAttribute("error", errorMessage);
            return "web/checkout";
        }
    }

    /**
     * Pre-submit validation endpoint — UI gọi trước khi gửi CheckoutReq để:
     * <ul>
     *   <li>Phát hiện price/stock/shop/voucher đã đổi từ lúc user mở trang</li>
     *   <li>Auto-refresh UI với dữ liệu mới nhất</li>
     *   <li>Chặn submit nếu có lỗi nghiêm trọng</li>
     * </ul>
     *
     * <p>Endpoint này là read-only — KHÔNG tạo Order, KHÔNG trừ stock, KHÔNG tăng used.</p>
     *
     * <p>Flow:</p>
     * <pre>
     *   UI Checkout button click
     *     → POST /api/checkout/validate { voucherCode }
     *     → server: read cart + product + shop + voucher từ DB
     *     → server: trả về CheckoutValidationRes (valid, changed, items, summary, voucher, errors, warnings)
     *     → UI: nếu valid=true → submit form CheckoutReq
     *           nếu valid=false → render banner lỗi + refresh giá/tổng từ response
     * </pre>
     */
    @PostMapping("/api/checkout/validate")
    @ResponseBody
    public ResponseEntity<CheckoutValidationRes> validateCheckout(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam(required = false) String voucherCode) {
        if (user == null) {
            // 401 với response có valid=false để UI xử lý thống nhất
            return ResponseEntity.status(401).body(
                    CheckoutValidationRes.builder()
                            .valid(false)
                            .changed(false)
                            .errors(List.of("Vui lòng đăng nhập để tiếp tục thanh toán."))
                            .build());
        }
        // Defensive: nếu JWT cookie còn hạn nhưng user bị xóa khỏi DB (admin xóa, DB reset),
        // JwtAuthenticationFilter vẫn pass vì load user qua email... nhưng có thể fail
        // trong một số race condition. Pre-check tại đây để fail-fast với response rõ ràng
        // thay vì để OrderServiceImpl ném 404.
        if (!userRepository.existsById(user.getId())) {
            return ResponseEntity.status(401).body(
                    CheckoutValidationRes.builder()
                            .valid(false)
                            .changed(false)
                            .errors(List.of("Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại."))
                            .build());
        }
        // voucherCode từ query param có thể null → service tự xử lý
        CheckoutValidationRes result = orderService.validateCheckout(user.getId(), voucherCode);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/orders")
    public String orderHistoryPage(@AuthenticationPrincipal CustomUserDetails user,
                                   HttpSession session,
                                   Model model) {
        String redirect = requireValidUser(user, session);
        if (redirect != null) return redirect;

        List<Order> orders = orderService.getOrdersByUserId(user.getId());
        List<OrderHistoryRes> orderHistory = orders.stream()
                .map(this::buildOrderHistoryRes)
                .collect(Collectors.toList());

        model.addAttribute("orders", orderHistory);
        return "web/order-history";
    }

    /**
     * GET /orders/{id} — Chi tiết 1 Order của Customer.
     *
     * <p>Hiển thị rõ ràng:</p>
     * <ul>
     *   <li><b>Master Order status</b> (OrderStatus) — trạng thái tổng của đơn hàng</li>
     *   <li><b>SubOrder status</b> (ShippingStatus) — trạng thái vận chuyển của từng shop</li>
     * </ul>
     *
     * <p><b>Rule</b>: trạng thái phải rõ ràng — Master Order và SubOrder là 2 khái niệm
     * độc lập, KHÔNG tự suy diễn Master Order status từ SubOrder statuses (hoặc ngược lại).
     * Controller trả về status thô từ DB, template chỉ hiển thị.</p>
     */
    @GetMapping("/orders/{id}")
    public String orderDetailPage(@AuthenticationPrincipal CustomUserDetails user,
                                  HttpSession session,
                                  @PathVariable String id,
                                  Model model) {
        String redirect = requireValidUser(user, session);
        if (redirect != null) return redirect;

        // Service.getOrderByIdForCustomer đã enforce ownership tại service layer
        // (throws UnauthorizedException nếu order không thuộc customer).
        Order order = orderService.getOrderByIdForCustomer(id, user.getId());
        OrderHistoryRes orderRes = buildOrderHistoryRes(order);

        model.addAttribute("order", orderRes);
        return "web/order-detail";
    }

    /**
     * Build OrderHistoryRes với SubOrders được nhóm theo Shop.
     */
    private OrderHistoryRes buildOrderHistoryRes(Order order) {
        // Group items by shopId
        Map<String, List<Order.OrderItem>> itemsByShop = order.getItems().stream()
                .collect(Collectors.groupingBy(
                        item -> item.getShopId() != null ? item.getShopId() : "default",
                        LinkedHashMap::new,
                        Collectors.toList()));

        // Build SubOrderRes for each shop
        List<OrderHistoryRes.SubOrderRes> subOrders = new ArrayList<>();
        for (Map.Entry<String, List<Order.OrderItem>> entry : itemsByShop.entrySet()) {
            String shopId = entry.getKey();
            List<Order.OrderItem> shopItems = entry.getValue();

            // Get first item's shopName (should be consistent per shop)
            String shopName = shopItems.isEmpty() ? "Unknown Shop" :
                    (shopItems.get(0).getShopName() != null ? shopItems.get(0).getShopName() : "Unknown Shop");

            // Build OrderItemRes list
            List<OrderHistoryRes.OrderItemRes> itemResList = shopItems.stream()
                    .map(item -> OrderHistoryRes.OrderItemRes.builder()
                            .shopId(item.getShopId())
                            .productId(item.getProductId())
                            .productName(item.getProductName())
                            .imageUrl(item.getImageUrl())
                            .price(item.getPrice())
                            .quantity(item.getQuantity())
                            .subtotal(item.getSubtotal())
                            .build())
                    .collect(Collectors.toList());

            // Calculate shop subtotal
            BigDecimal shopSubtotal = shopItems.stream()
                    .map(Order.OrderItem::getSubtotal)
                    .filter(s -> s != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Get shipping status for this shop
            Order.SubOrderShipping shopShipping = order.getShippingByShop() != null ?
                    order.getShippingByShop().get(shopId) : null;
            ShippingStatus shippingStatus = shopShipping != null ? shopShipping.getStatus() : ShippingStatus.PENDING;
            String trackingNumber = shopShipping != null ? shopShipping.getTrackingNumber() : null;
            String carrier = shopShipping != null ? shopShipping.getCarrier() : null;
            String note = shopShipping != null ? shopShipping.getNote() : null;
            LocalDateTime shippedAt = shopShipping != null ? shopShipping.getShippedAt() : null;
            LocalDateTime deliveredAt = shopShipping != null ? shopShipping.getDeliveredAt() : null;
            // Phí vận chuyển được phân bổ theo shop khi tạo Order. Fallback nếu SubOrder không có
            // (vd: order cũ trước khi triển khai per-shop shippingFee) — lấy từ master order.
            BigDecimal subShippingFee = BigDecimal.ZERO;
            if (shopShipping != null && shopShipping.getShippingFee() != null) {
                subShippingFee = shopShipping.getShippingFee();
            } else if (order.getShippingFee() != null && subOrders.size() + 1 > 0 && itemsByShop.size() > 0) {
                // Phân bổ đều từ master shippingFee cho hiển thị (chỉ dùng khi sub-order chưa có fee)
                subShippingFee = order.getShippingFee().divide(
                        BigDecimal.valueOf(itemsByShop.size()), 2, RoundingMode.HALF_UP);
            }

            subOrders.add(OrderHistoryRes.SubOrderRes.builder()
                    .shopId(shopId)
                    .shopName(shopName)
                    .items(itemResList)
                    .itemCount(shopItems.size())
                    .subtotal(shopSubtotal)
                    .shippingFee(subShippingFee)
                    .shippingStatus(shippingStatus)
                    .trackingNumber(trackingNumber)
                    .carrier(carrier)
                    .note(note)
                    .shippedAt(shippedAt)
                    .deliveredAt(deliveredAt)
                    .build());
        }

        // Build product images list (all images from this order)
        List<String> productImages = order.getItems().stream()
                .map(Order.OrderItem::getImageUrl)
                .collect(Collectors.toList());

        return OrderHistoryRes.builder()
                .orderId(order.getId())
                .orderDate(order.getCreatedAt())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .totalItemCount(order.getItems().size())
                .shopName(order.getShopName())
                .userName(order.getUserName())
                .userPhone(order.getUserPhone())
                .shippingAddress(order.getShippingAddress())
                .subtotal(order.getSubtotal())
                .shippingFee(order.getShippingFee())
                .discount(order.getDiscount())
                .voucherCode(order.getVoucherCode())
                .voucherName(order.getVoucherName())
                .paymentMethod(order.getPaymentMethod() != null ? order.getPaymentMethod().name() : null)
                // Map paymentStatus (PENDING/PAID/FAILED/REFUNDED) ra String để UI render.
                // Fallback an toàn: nếu Order cũ trong DB chưa có field này → PENDING.
                .paymentStatus(order.getPaymentStatus() != null
                        ? order.getPaymentStatus().name()
                        : PaymentStatus.PENDING.name())
                .paidAt(order.getPaidAt())
                .refundedAt(order.getRefundedAt())
                .paid(order.isPaid())
                .deliveredAt(order.getDeliveredAt())
                .subOrders(subOrders)
                .productImages(productImages)
                .build();
    }

    private CartController.AppliedVoucher readAppliedVoucher(HttpSession session) {
        if (session == null) return null;
        Object attr = session.getAttribute(SESSION_APPLIED_VOUCHER);
        return (attr instanceof CartController.AppliedVoucher av) ? av : null;
    }

    /**
     * Invalidate HTTP session + clear JWT cookie khi phát hiện user không còn tồn tại
     * trong database (vd: DB reset, admin xóa user trong khi JWT cookie còn hạn).
     * Buộc browser phải đăng nhập lại để tránh các lỗi "Không tìm thấy người dùng"
     * lặp lại ở các flow khác.
     */
    private void invalidateSession(HttpSession session) {
        if (session != null) {
            try {
                session.invalidate();
            } catch (IllegalStateException ignored) {
                // session đã bị invalidate trước đó — bỏ qua
            }
        }
    }

    /**
     * Kiểm tra user còn hợp lệ không (có trong DB). Trả về redirect URL nếu KHÔNG hợp lệ,
     * hoặc {@code null} nếu OK. Dùng ở đầu mỗi handler để fail-fast với thông báo thân thiện.
     *
     * <p>Tình huống bảo vệ:</p>
     * <ul>
     *   <li>JWT cookie còn hạn nhưng user bị xóa khỏi DB (admin xóa, DB reset, ...)</li>
     *   <li>User có status != ACTIVE nhưng JwtAuthenticationFilter vẫn pass (race)</li>
     * </ul>
     *
     * <p>Khi phát hiện không hợp lệ: invalidate session để Spring Security dọn dẹp,
     * redirect về {@code /auth/login?expired=true} để trang login hiển thị banner cảnh báo.</p>
     */
    private String requireValidUser(CustomUserDetails user, HttpSession session) {
        if (user == null) {
            return "redirect:/auth/login";
        }
        User dbUser = userRepository.findById(user.getId()).orElse(null);
        if (dbUser == null) {
            invalidateSession(session);
            return "redirect:/auth/login?expired=true";
        }
        return null;
    }
}
