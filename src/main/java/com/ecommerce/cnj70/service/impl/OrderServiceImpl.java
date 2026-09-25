package com.ecommerce.cnj70.service.impl;

import com.ecommerce.cnj70.document.*;
import com.ecommerce.cnj70.dto.request.CheckoutReq;
import com.ecommerce.cnj70.enums.DiscountType;
import com.ecommerce.cnj70.enums.OrderStatus;
import com.ecommerce.cnj70.enums.PaymentMethod;
import com.ecommerce.cnj70.enums.ProductStatus;
import com.ecommerce.cnj70.enums.ShippingStatus;
import com.ecommerce.cnj70.enums.ShopStatus;
import com.ecommerce.cnj70.exception.BadRequestException;
import com.ecommerce.cnj70.exception.ResourceNotFoundException;
import com.ecommerce.cnj70.repository.CartRepository;
import com.ecommerce.cnj70.repository.OrderRepository;
import com.ecommerce.cnj70.repository.ProductRepository;
import com.ecommerce.cnj70.repository.ShopRepository;
import com.ecommerce.cnj70.repository.UserRepository;
import com.ecommerce.cnj70.service.CartService;
import com.ecommerce.cnj70.service.OrderService;
import com.ecommerce.cnj70.service.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    /** Phí vận chuyển cố định (đồng bộ với CartController & checkout.html). */
    private static final BigDecimal SHIPPING_FEE = BigDecimal.valueOf(15000);

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final CartRepository cartRepository;
    private final ShopRepository shopRepository;
    private final CartService cartService;
    private final VoucherService voucherService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Order createOrder(String userId, CheckoutReq request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy giỏ hàng"));

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new BadRequestException("Giỏ hàng trống");
        }

        // ===== TASK #14: xác định items sẽ checkout =====
        // Nếu request.items rỗng/null -> checkout toàn bộ Cart
        // Nếu có items -> chỉ checkout các productId trong items (partial)
        Set<String> requestedIds = new HashSet<>();
        Map<String, Integer> requestedQty = new java.util.HashMap<>();
        if (request.getItems() != null && !request.getItems().isEmpty()) {
            for (CheckoutReq.CheckoutItemReq it : request.getItems()) {
                if (it.getProductId() == null || it.getProductId().isBlank()) {
                    throw new BadRequestException("ProductId không được rỗng trong items checkout");
                }
                if (it.getQuantity() <= 0) {
                    throw new BadRequestException("Số lượng phải > 0 cho sản phẩm " + it.getProductId());
                }
                requestedIds.add(it.getProductId());
                requestedQty.put(it.getProductId(), it.getQuantity());
            }
        }
        final boolean isPartial = !requestedIds.isEmpty();
        final Set<String> checkoutProductIds = isPartial ? requestedIds : null;
        final Map<String, Integer> finalRequestedQty = isPartial ? requestedQty : null;

        // Lọc items sẽ được checkout từ Cart hiện tại
        List<Cart.CartItem> itemsToCheckout = cart.getItems().stream()
                .filter(ci -> checkoutProductIds == null || checkoutProductIds.contains(ci.getProductId()))
                .collect(Collectors.toCollection(ArrayList::new));

        if (itemsToCheckout.isEmpty()) {
            throw new BadRequestException("Không có sản phẩm hợp lệ trong giỏ hàng để thanh toán");
        }

        // ===== TASK #8: validate Product + stock =====
        List<Order.OrderItem> orderItems = new ArrayList<>();
        Map<String, String> shopNames = new HashMap<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (Cart.CartItem cartItem : itemsToCheckout) {
            Product product = productRepository.findById(cartItem.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Không tìm thấy sản phẩm: " + cartItem.getProductId()));

            // quantity mua = requestedQty (nếu partial) hoặc cartItem.quantity (full)
            int buyQty = finalRequestedQty != null
                    ? finalRequestedQty.getOrDefault(cartItem.getProductId(), cartItem.getQuantity())
                    : cartItem.getQuantity();

            if (buyQty <= 0) {
                throw new BadRequestException("Số lượng mua phải > 0 cho sản phẩm " + product.getName());
            }

            if (product.getStock() < buyQty) {
                throw new BadRequestException(String.format(
                        "Sản phẩm '%s' không đủ hàng. Chỉ còn %d sản phẩm.",
                        product.getName(), product.getStock()));
            }

            BigDecimal itemSubtotal = cartItem.getPrice().multiply(BigDecimal.valueOf(buyQty));

            Order.OrderItem orderItem = Order.OrderItem.builder()
                    .shopId(product.getShopId())
                    .shopName(product.getShopName())
                    .productId(product.getId())
                    .productName(product.getName())
                    .imageUrl(product.getThumbnailUrl())
                    .price(cartItem.getPrice())
                    .quantity(buyQty)
                    .subtotal(itemSubtotal)
                    .build();

            orderItems.add(orderItem);
            subtotal = subtotal.add(itemSubtotal);

            // Trừ stock (sẽ rollback nếu có lỗi ở bước sau nhờ @Transactional)
            product.setStock(product.getStock() - buyQty);
            if (product.getStock() < 0) {
                product.setStock(0);
            }
            productRepository.save(product);
        }

        BigDecimal shippingFee = SHIPPING_FEE;
        BigDecimal totalBeforeDiscount = subtotal.add(shippingFee);

        // ===== TASK #13: áp dụng voucher (nếu có) =====
        BigDecimal discount = BigDecimal.ZERO;
        Voucher appliedVoucher = null;
        if (request.getVoucherCode() != null && !request.getVoucherCode().isBlank()) {
            try {
                // Lấy shopId đầu tiên để validate voucher SHOP (nếu là SHOP voucher)
                String firstShopId = itemsToCheckout.get(0).getShopId();
                appliedVoucher = voucherService.validateForCheckout(
                        request.getVoucherCode().trim(), firstShopId, null);
                discount = computeDiscount(appliedVoucher, totalBeforeDiscount);
            } catch (BadRequestException e) {
                throw new BadRequestException("Voucher không hợp lệ: " + e.getMessage());
            }
        }

        BigDecimal totalAmount = totalBeforeDiscount.subtract(discount);
        if (totalAmount.compareTo(BigDecimal.ZERO) < 0) {
            totalAmount = BigDecimal.ZERO;
        }

        // Lấy shopId/shopName đầu tiên để Order có thể lọc (giữ nguyên hành vi cũ)
        String primaryShopId = itemsToCheckout.get(0).getShopId();
        String primaryShopName = itemsToCheckout.get(0).getShopName();

        Order order = Order.builder()
                .userId(userId)
                .userName(user.getFullName())
                .userEmail(user.getEmail())
                .userPhone(request.getPhone() != null ? request.getPhone() : user.getPhone())
                .shippingAddress(request.getShippingAddress())
                .items(orderItems)
                .subtotal(subtotal)
                .shippingFee(shippingFee)
                .totalAmount(totalAmount)
                .discount(discount)
                .voucherId(appliedVoucher != null ? appliedVoucher.getId() : null)
                .voucherCode(appliedVoucher != null ? appliedVoucher.getCode() : null)
                .voucherName(appliedVoucher != null ? appliedVoucher.getName() : null)
                .status(OrderStatus.PENDING)
                .paymentMethod(request.getPaymentMethod() != null ? request.getPaymentMethod() : PaymentMethod.COD)
                .paid(false)
                .shopId(primaryShopId)
                .shopName(primaryShopName)
                .build();

        Order savedOrder = orderRepository.save(order);

        // ===== TASK #14: cleanup Cart =====
        // Nếu là full checkout (request.items null/empty) -> xóa toàn bộ Cart
        // Nếu là partial checkout -> chỉ xóa các productId đã mua
        if (checkoutProductIds == null) {
            cartService.clearCart(userId);
        } else {
            cartService.removeItems(userId, checkoutProductIds);
        }

        // ===== TASK #13: tăng voucher used count SAU khi Order tạo thành công =====
        // Nếu tăng used fail, rollback toàn bộ (nhờ @Transactional) -> Cart sẽ giữ nguyên
        if (appliedVoucher != null) {
            voucherService.incrementUsed(appliedVoucher.getId());
        }

        return savedOrder;
    }

    @Override
    public Order getOrderById(String id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng"));
    }

    @Override
    public Order getOrderByIdForCustomer(String orderId, String customerId) {
        Order order = getOrderById(orderId);
        if (customerId == null || !customerId.equals(order.getUserId())) {
            throw new ResourceNotFoundException("Không tìm thấy đơn hàng");
        }
        return order;
    }

    @Override
    public List<Order> getOrdersByUserId(String userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    public List<Order> getOrdersByShopId(String shopId) {
        List<Order> allOrders = orderRepository.findAll();
        return allOrders.stream()
                .filter(order -> order.getItems() != null &&
                        order.getItems().stream()
                                .anyMatch(item -> shopId.equals(item.getShopId())))
                .sorted((o1, o2) -> {
                    if (o1.getCreatedAt() == null || o2.getCreatedAt() == null) return 0;
                    return o2.getCreatedAt().compareTo(o1.getCreatedAt());
                })
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateOrderStatus(String orderId, OrderStatus newStatus) {
        Order order = getOrderById(orderId);
        OrderStatus currentStatus = order.getStatus();

        validateStatusTransition(currentStatus, newStatus);

        order.setStatus(newStatus);

        if (newStatus == OrderStatus.DELIVERED) {
            order.setDeliveredAt(LocalDateTime.now());
        }

        if (newStatus == OrderStatus.CANCELLED && currentStatus != OrderStatus.CANCELLED) {
            restoreStock(order);
        }

        orderRepository.save(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(String orderId) {
        Order order = getOrderById(orderId);

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BadRequestException("Chỉ có thể hủy đơn hàng đang ở trạng thái CHỜ XÁC NHẬN");
        }

        restoreStock(order);

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
    }

    /**
     * TASK #14/#20/#21 — Kiểm tra Customer đã nhận được Product hay chưa.
     * Chỉ Order ở trạng thái DELIVERED mới được coi là "đã nhận hàng".
     * Dùng cho Review validation: chỉ cho phép tạo Review khi đã giao thành công.
     *
     * @return true nếu có Order DELIVERED chứa productId của user
     */
    @Override
    @Transactional(readOnly = true)
    public boolean hasUserReceivedProduct(String userId, String productId) {
        if (userId == null || productId == null || userId.isBlank() || productId.isBlank()) {
            return false;
        }
        List<Order> userOrders = orderRepository.findByUserId(userId);
        return userOrders.stream()
                .filter(o -> o.getStatus() == OrderStatus.DELIVERED)
                .filter(o -> o.getItems() != null)
                .flatMap(o -> o.getItems().stream())
                .anyMatch(item -> productId.equals(item.getProductId()));
    }

    /**
     * TASK #21 (legacy) — Kiểm tra Customer đã mua Product hay chưa.
     * Đếm Order của user có chứa productId và đang ở trạng thái "đã mua thành công".
     *
     * Logic "đã mua thành công" = Order KHÔNG ở trạng thái CANCELLED.
     * PENDING, PREPARING, SHIPPING, DELIVERED đều được tính là đã mua.
     * Lý do: một khi user đã đặt hàng (PENDING), họ đã giao dịch mua bán với shop;
     * chỉ CANCELLED là thực sự không mua nữa.
     * CHÚ Ý: method này dùng cho hiển thị lịch sử mua hàng, KHÔNG dùng cho Review.
     */
    @Override
    @Transactional(readOnly = true)
    public boolean hasUserPurchasedProduct(String userId, String productId) {
        if (userId == null || productId == null || userId.isBlank() || productId.isBlank()) {
            return false;
        }
        List<Order> userOrders = orderRepository.findByUserId(userId);
        return userOrders.stream()
                .filter(o -> o.getStatus() != OrderStatus.CANCELLED)
                .filter(o -> o.getItems() != null)
                .flatMap(o -> o.getItems().stream())
                .anyMatch(item -> productId.equals(item.getProductId()));
    }

    /**
     * Tính discount từ voucher (copy từ CartController.computeDiscount để đảm bảo nhất quán).
     */
    private BigDecimal computeDiscount(Voucher voucher, BigDecimal orderTotal) {
        BigDecimal discount = BigDecimal.ZERO;
        if (voucher.getMinOrderValue() != null
                && orderTotal.compareTo(voucher.getMinOrderValue()) < 0) {
            return BigDecimal.ZERO;
        }
        if (voucher.getDiscountType() == DiscountType.PERCENT) {
            discount = orderTotal.multiply(voucher.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            if (voucher.getMaxDiscountAmount() != null
                    && discount.compareTo(voucher.getMaxDiscountAmount()) > 0) {
                discount = voucher.getMaxDiscountAmount();
            }
        } else if (voucher.getDiscountType() == DiscountType.AMOUNT) {
            discount = voucher.getDiscountValue();
        }
        if (discount.compareTo(orderTotal) > 0) {
            discount = orderTotal;
        }
        return discount.setScale(2, RoundingMode.HALF_UP);
    }

    private void validateStatusTransition(OrderStatus current, OrderStatus next) {
        boolean valid = switch (current) {
            case PENDING -> next == OrderStatus.PREPARING || next == OrderStatus.CANCELLED;
            case PREPARING -> next == OrderStatus.SHIPPING || next == OrderStatus.CANCELLED;
            case SHIPPING -> next == OrderStatus.DELIVERED || next == OrderStatus.CANCELLED;
            case DELIVERED, CANCELLED -> false;
        };

        if (!valid) {
            throw new BadRequestException(String.format(
                    "Không thể chuyển từ trạng thái '%s' sang '%s'",
                    current.name(), next.name()));
        }
    }

    private void restoreStock(Order order) {
        if (order.getItems() == null) return;

        for (Order.OrderItem item : order.getItems()) {
            Product product = productRepository.findById(item.getProductId()).orElse(null);
            if (product != null) {
                product.setStock(product.getStock() + item.getQuantity());
                productRepository.save(product);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Order updateShippingStatus(String orderId, String shopId, ShippingStatus status,
                                     String trackingNumber, String carrier, String note) {
        Order order = getOrderById(orderId);

        if (order.getShippingByShop() == null || !order.getShippingByShop().containsKey(shopId)) {
            throw new BadRequestException("Đơn hàng không chứa sản phẩm của shop này");
        }

        Order.SubOrderShipping shipping = order.getShippingByShop().get(shopId);
        shipping.setStatus(status);
        if (trackingNumber != null && !trackingNumber.isBlank()) {
            shipping.setTrackingNumber(trackingNumber);
        }
        if (carrier != null && !carrier.isBlank()) {
            shipping.setCarrier(carrier);
        }
        if (note != null && !note.isBlank()) {
            shipping.setNote(note);
        }
        shipping.setUpdatedAt(LocalDateTime.now());

        if (status == ShippingStatus.DELIVERED) {
            shipping.setDeliveredAt(LocalDateTime.now());
        }
        if (status == ShippingStatus.PICKED_UP || status == ShippingStatus.IN_TRANSIT) {
            shipping.setShippedAt(LocalDateTime.now());
        }

        return orderRepository.save(order);
    }
}
