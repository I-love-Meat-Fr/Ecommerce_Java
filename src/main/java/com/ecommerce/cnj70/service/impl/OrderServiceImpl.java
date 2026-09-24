package com.ecommerce.cnj70.service.impl;

import com.ecommerce.cnj70.document.*;
import com.ecommerce.cnj70.dto.checkout.CheckoutValidationRes;
import com.ecommerce.cnj70.dto.request.CheckoutReq;
import com.ecommerce.cnj70.enums.DiscountType;
import com.ecommerce.cnj70.enums.OrderStatus;
import com.ecommerce.cnj70.enums.PaymentMethod;
import com.ecommerce.cnj70.enums.PaymentStatus;
import com.ecommerce.cnj70.enums.ProductStatus;
import com.ecommerce.cnj70.enums.ShippingStatus;
import com.ecommerce.cnj70.enums.ShopStatus;
import com.ecommerce.cnj70.exception.BadRequestException;
import com.ecommerce.cnj70.exception.ResourceNotFoundException;
import com.ecommerce.cnj70.exception.UnauthorizedException;
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
import java.util.Optional;
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
        Map<String, Integer> requestedQty = new HashMap<>();
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

            // ===== TASK #16 (vendors-module): validate product status + shop active/verified =====
            if (product.getStatus() != null && product.getStatus() != ProductStatus.ACTIVE) {
                throw new BadRequestException(String.format(
                        "Sản phẩm '%s' không thể mua (trạng thái: %s).",
                        product.getName(), product.getStatus()));
            }

            Shop shop = shopRepository.findById(product.getShopId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy cửa hàng: " + product.getShopId()));
            if (!shop.isActive()) {
                throw new BadRequestException(String.format(
                        "Cửa hàng '%s' hiện không hoạt động. Không thể đặt hàng.",
                        shop.getShopName()));
            }
            if (!shop.isVerified() || shop.getStatus() != ShopStatus.APPROVED) {
                throw new BadRequestException(String.format(
                        "Cửa hàng '%s' chưa được xác minh. Không thể đặt hàng.",
                        shop.getShopName()));
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
            shopNames.put(product.getShopId(), product.getShopName());

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
        // Lưu ý: bất kỳ lỗi nào từ voucherService.validateForCheckout() đều phải
        // được wrap với prefix "Voucher không hợp lệ:" để OrderController dễ dàng
        // phát hiện và:
        //   1) Clear appliedVoucher khỏi session
        //   2) Hiển thị lỗi inline trên UI
        //   3) KHÔNG tạo Order (đã được đảm bảo bởi @Transactional + throw ở đây)
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
                // Voucher tồn tại nhưng không thỏa điều kiện (hết hạn / hết lượt /
                // không áp dụng cho shop này / sản phẩm này / bị vô hiệu hóa)
                throw new BadRequestException("Voucher không hợp lệ: " + e.getMessage());
            } catch (ResourceNotFoundException e) {
                // Voucher không tồn tại trong hệ thống
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

        // ===== vendors-module: shippingByShop khởi tạo cho mỗi shop (sub-order) =====
        // Mỗi shop có shippingFee riêng — phân bổ đều từ SHIPPING_FEE tổng của Order.
        // Shop cuối nhận phần dư (remainder) để tổng các sub-order luôn khớp với Order.shippingFee.
        Map<String, Order.SubOrderShipping> shippingByShop = new HashMap<>();
        List<Map.Entry<String, String>> shopEntries = new ArrayList<>(shopNames.entrySet());
        int shopCount = shopEntries.size();
        BigDecimal perShopFee = shopCount > 0
                ? shippingFee.divide(BigDecimal.valueOf(shopCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal allocatedSoFar = BigDecimal.ZERO;
        for (int i = 0; i < shopEntries.size(); i++) {
            Map.Entry<String, String> entry = shopEntries.get(i);
            // Shop cuối: lấy remainder để đảm bảo tổng bằng shippingFee
            BigDecimal subFee;
            if (i == shopEntries.size() - 1) {
                subFee = shippingFee.subtract(allocatedSoFar).max(BigDecimal.ZERO);
            } else {
                subFee = perShopFee;
                allocatedSoFar = allocatedSoFar.add(perShopFee);
            }
            shippingByShop.put(entry.getKey(), Order.SubOrderShipping.builder()
                    .shopId(entry.getKey())
                    .shopName(entry.getValue())
                    .status(ShippingStatus.PENDING)
                    .shippingFee(subFee)
                    .build());
        }

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
                // PENDING: mọi Order mới đều ở trạng thái chờ thanh toán.
                // - COD: vendor sẽ set PAID khi thu tiền khi giao hàng
                // - VNPAY: VNPAY callback sẽ set PAID/FAILED (Phase 3B+)
                // Field paid được giữ đồng bộ với paymentStatus (paid == paymentStatus == PAID)
                .paymentStatus(PaymentStatus.PENDING)
                .paid(false)
                .shopId(primaryShopId)
                .shopName(primaryShopName)
                .shippingByShop(shippingByShop)
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

    /**
     * Pre-submit validation — đọc lại toàn bộ trạng thái (price, stock, shop, voucher)
     * từ DB và so sánh với Cart snapshot. Trả về:
     *
     * <ul>
     *   <li>{@code valid = false} nếu có lỗi nghiêm trọng (stock=0, shop suspended, product removed,
     *       voucher exhausted/expired) — UI phải chặn submit</li>
     *   <li>{@code changed = true} nếu bất kỳ trường nào (price, stock, shop status, voucher)
     *       đã đổi so với Cart/session — UI cần refresh dữ liệu hiển thị</li>
     *   <li>warnings: cảnh báo không chặn submit (giá tăng/giảm, stock sắp hết, voucher sắp hết lượt)</li>
     * </ul>
     *
     * <p>Method này KHÔNG tạo Order, KHÔNG trừ stock, KHÔNG tăng used count.</p>
     */
    @Override
    @Transactional(readOnly = true)
    public CheckoutValidationRes validateCheckout(String userId, String voucherCode) {
        CheckoutValidationRes.CheckoutValidationResBuilder response = CheckoutValidationRes.builder()
                .valid(true)
                .changed(false);

        List<CheckoutValidationRes.ValidatedItem> validatedItems = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        Cart cart = cartRepository.findByUserId(userId).orElse(null);
        if (cart == null || cart.getItems() == null || cart.getItems().isEmpty()) {
            // Cart trống → không thể checkout
            response.valid(false);
            response.errors(List.of("Giỏ hàng của bạn đang trống. Vui lòng thêm sản phẩm trước khi thanh toán."));
            response.items(validatedItems);
            response.warnings(warnings);
            response.summary(CheckoutValidationRes.Summary.builder()
                    .subtotal(BigDecimal.ZERO)
                    .shippingFee(SHIPPING_FEE)
                    .discount(BigDecimal.ZERO)
                    .finalTotal(SHIPPING_FEE)
                    .deltaFromPrevious(BigDecimal.ZERO)
                    .build());
            response.voucher(null);
            return response.build();
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        boolean anyChanged = false;

        for (Cart.CartItem cartItem : cart.getItems()) {
            CheckoutValidationRes.ValidatedItem.ValidatedItemBuilder itemBuilder =
                    CheckoutValidationRes.ValidatedItem.builder()
                            .productId(cartItem.getProductId())
                            .productName(cartItem.getProductName())
                            .imageUrl(cartItem.getImageUrl())
                            .shopId(cartItem.getShopId())
                            .shopName(cartItem.getShopName())
                            .oldPrice(cartItem.getPrice())
                            .currentPrice(cartItem.getPrice())
                            .priceChanged(false)
                            .requestedQuantity(cartItem.getQuantity())
                            .currentStock(cartItem.getStock() != null ? cartItem.getStock() : 0)
                            .stockOk(true)
                            .productStatus("ACTIVE")
                            .shopActive(true)
                            .shopCurrentName(cartItem.getShopName())
                            .subtotal(cartItem.getSubtotal() != null ? cartItem.getSubtotal() : BigDecimal.ZERO)
                            .valid(true);

            // 1) Product tồn tại?
            Optional<Product> productOpt = productRepository.findById(cartItem.getProductId());
            if (productOpt.isEmpty()) {
                itemBuilder.valid(false)
                        .productStatus("NOT_FOUND")
                        .reasonCode("PRODUCT_NOT_FOUND")
                        .reasonMessage("Sản phẩm đã ngừng bán hoặc bị xóa khỏi hệ thống.");
                errors.add(String.format("Sản phẩm \"%s\" đã ngừng bán.", cartItem.getProductName()));
                validatedItems.add(itemBuilder.build());
                response.valid(false);
                continue;
            }

            Product product = productOpt.get();

            // 2) Product phải ACTIVE
            ProductStatus status = product.getStatus();
            String statusStr = status != null ? status.name() : "UNKNOWN";
            itemBuilder.productStatus(statusStr);
            if (status == null || status != ProductStatus.ACTIVE) {
                itemBuilder.valid(false)
                        .reasonCode("INACTIVE_PRODUCT")
                        .reasonMessage(String.format(
                                "Sản phẩm không còn khả dụng (trạng thái: %s).",
                                statusStr));
                errors.add(String.format("Sản phẩm \"%s\" hiện không khả dụng (%s).",
                        product.getName(), statusStr));
                validatedItems.add(itemBuilder.build());
                response.valid(false);
                continue;
            }

            // 3) Shop phải active & APPROVED
            boolean shopOk = false;
            if (product.getShopId() != null) {
                Optional<Shop> shopOpt = shopRepository.findById(product.getShopId());
                if (shopOpt.isPresent()) {
                    Shop shop = shopOpt.get();
                    itemBuilder.shopCurrentName(shop.getShopName());
                    if (shop.isActive() && shop.getStatus() == ShopStatus.APPROVED) {
                        shopOk = true;
                        itemBuilder.shopActive(true);
                    } else {
                        itemBuilder.shopActive(false);
                        String reasonCode = !shop.isActive() ? "SHOP_INACTIVE"
                                : (shop.getStatus() == ShopStatus.SUSPENDED ? "SHOP_SUSPENDED"
                                : "SHOP_NOT_APPROVED");
                        itemBuilder.reasonCode(reasonCode)
                                .valid(false)
                                .reasonMessage(String.format(
                                        "Cửa hàng \"%s\" hiện không hoạt động.",
                                        shop.getShopName()));
                        errors.add(String.format("Cửa hàng \"%s\" hiện không hoạt động. "
                                + "Không thể đặt hàng.", shop.getShopName()));
                    }
                } else {
                    itemBuilder.shopActive(false)
                            .valid(false)
                            .reasonCode("SHOP_NOT_FOUND")
                            .reasonMessage("Cửa hàng đã ngừng hoạt động hoặc bị xóa.");
                    errors.add(String.format("Cửa hàng bán \"%s\" đã ngừng hoạt động.",
                            product.getName()));
                }
            } else {
                itemBuilder.shopActive(false)
                        .valid(false)
                        .reasonCode("SHOP_NOT_FOUND")
                        .reasonMessage("Không xác định được cửa hàng bán sản phẩm này.");
                errors.add(String.format("Không xác định được cửa hàng bán \"%s\".",
                        product.getName()));
            }
            if (!shopOk) {
                validatedItems.add(itemBuilder.build());
                response.valid(false);
                continue;
            }

            // 4) Price drift detection — so sánh price cũ (cart snapshot) vs hiện tại (DB)
            BigDecimal currentPrice = product.getPrice() != null ? product.getPrice() : BigDecimal.ZERO;
            BigDecimal oldPrice = cartItem.getPrice() != null ? cartItem.getPrice() : BigDecimal.ZERO;
            itemBuilder.currentPrice(currentPrice);
            if (currentPrice.compareTo(oldPrice) != 0) {
                itemBuilder.priceChanged(true);
                anyChanged = true;
                if (currentPrice.compareTo(oldPrice) > 0) {
                    warnings.add(String.format(
                            "Giá sản phẩm \"%s\" đã tăng từ %s₫ lên %s₫.",
                            product.getName(),
                            formatVnd(oldPrice), formatVnd(currentPrice)));
                } else {
                    warnings.add(String.format(
                            "Giá sản phẩm \"%s\" đã giảm từ %s₫ xuống %s₫.",
                            product.getName(),
                            formatVnd(oldPrice), formatVnd(currentPrice)));
                }
            }

            // 5) Stock check
            int currentStock = Math.max(0, product.getStock());
            int requested = cartItem.getQuantity();
            itemBuilder.currentStock(currentStock);
            if (currentStock <= 0) {
                itemBuilder.stockOk(false)
                        .valid(false)
                        .reasonCode("OUT_OF_STOCK")
                        .reasonMessage("Sản phẩm đã hết hàng.");
                errors.add(String.format("Sản phẩm \"%s\" đã hết hàng.", product.getName()));
                validatedItems.add(itemBuilder.build());
                response.valid(false);
                continue;
            }
            if (currentStock < requested) {
                itemBuilder.stockOk(false)
                        .valid(false)
                        .reasonCode("INSUFFICIENT_STOCK")
                        .reasonMessage(String.format(
                                "Chỉ còn %d sản phẩm trong kho (giỏ hàng: %d).",
                                currentStock, requested));
                errors.add(String.format(
                        "Sản phẩm \"%s\" chỉ còn %d (giỏ: %d).",
                        product.getName(), currentStock, requested));
                validatedItems.add(itemBuilder.build());
                response.valid(false);
                continue;
            }
            if (currentStock - requested <= 3) {
                warnings.add(String.format(
                        "Sản phẩm \"%s\" chỉ còn %d trong kho sau khi đặt.",
                        product.getName(), currentStock - requested));
            }

            // 6) Subtotal theo currentPrice
            BigDecimal itemSubtotal = currentPrice.multiply(BigDecimal.valueOf(requested));
            itemBuilder.subtotal(itemSubtotal);
            subtotal = subtotal.add(itemSubtotal);
            validatedItems.add(itemBuilder.build());
        }

        BigDecimal shippingFee = SHIPPING_FEE;
        BigDecimal totalBeforeDiscount = subtotal.add(shippingFee);

        // ===== Voucher validation (nếu có) =====
        CheckoutValidationRes.VoucherState voucherState = null;
        BigDecimal discount = BigDecimal.ZERO;
        if (voucherCode != null && !voucherCode.isBlank()) {
            try {
                // Lấy shopId đầu tiên của cart (nếu có) để validate SHOP voucher
                String firstShopId = cart.getItems().isEmpty() ? null
                        : cart.getItems().get(0).getShopId();
                Voucher voucher = voucherService.validateForCheckout(
                        voucherCode.trim(), firstShopId, null);
                discount = computeDiscount(voucher, totalBeforeDiscount);
                if (discount.signum() <= 0) {
                    // Voucher tồn tại nhưng subtotal < minOrderValue → không được giảm
                    voucherState = CheckoutValidationRes.VoucherState.builder()
                            .code(voucher.getCode())
                            .voucherId(voucher.getId())
                            .voucherName(voucher.getName())
                            .valid(false)
                            .discount(BigDecimal.ZERO)
                            .reasonCode("MIN_ORDER_NOT_MET")
                            .reasonMessage(String.format(
                                    "Đơn hàng chưa đạt giá trị tối thiểu %s₫ để áp dụng voucher.",
                                    formatVnd(voucher.getMinOrderValue())))
                            .build();
                    warnings.add(voucherState.getReasonMessage());
                    anyChanged = true;
                } else {
                    voucherState = CheckoutValidationRes.VoucherState.builder()
                            .code(voucher.getCode())
                            .voucherId(voucher.getId())
                            .voucherName(voucher.getName())
                            .valid(true)
                            .discount(discount)
                            .reasonCode(null)
                            .reasonMessage(null)
                            .build();
                }
            } catch (BadRequestException e) {
                voucherState = CheckoutValidationRes.VoucherState.builder()
                        .code(voucherCode)
                        .valid(false)
                        .discount(BigDecimal.ZERO)
                        .reasonCode("VOUCHER_INVALID")
                        .reasonMessage(e.getMessage())
                        .build();
                errors.add("Voucher không hợp lệ: " + e.getMessage());
                response.valid(false);
                anyChanged = true;
            } catch (Exception e) {
                voucherState = CheckoutValidationRes.VoucherState.builder()
                        .code(voucherCode)
                        .valid(false)
                        .discount(BigDecimal.ZERO)
                        .reasonCode("VOUCHER_NOT_FOUND")
                        .reasonMessage("Voucher không tồn tại hoặc đã bị xóa.")
                        .build();
                errors.add("Voucher không tồn tại hoặc đã bị xóa.");
                response.valid(false);
                anyChanged = true;
            }
        }

        BigDecimal finalTotal = totalBeforeDiscount.subtract(discount);
        if (finalTotal.compareTo(BigDecimal.ZERO) < 0) {
            finalTotal = BigDecimal.ZERO;
        }

        // deltaFromPrevious: so sánh finalTotal mới với tổng từ Cart snapshot (price × qty)
        BigDecimal previousSubtotal = cart.getItems().stream()
                .map(Cart.CartItem::getSubtotal)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal previousFinalTotal = previousSubtotal.add(shippingFee)
                .subtract(BigDecimal.ZERO) // không có voucher cũ để so sánh
                .max(BigDecimal.ZERO);
        BigDecimal deltaFromPrevious = previousFinalTotal.subtract(finalTotal);

        // Nếu có bất kỳ thay đổi nào về price/voucher → mark changed = true
        if (!warnings.isEmpty() || voucherState != null || !errors.isEmpty()) {
            anyChanged = true;
        }

        response.items(validatedItems)
                .errors(errors)
                .warnings(warnings)
                .changed(anyChanged)
                .summary(CheckoutValidationRes.Summary.builder()
                        .subtotal(subtotal)
                        .shippingFee(shippingFee)
                        .discount(discount)
                        .finalTotal(finalTotal)
                        .deltaFromPrevious(deltaFromPrevious)
                        .build())
                .voucher(voucherState);

        return response.build();
    }

    private static String formatVnd(BigDecimal amount) {
        if (amount == null) return "0";
        return new java.text.DecimalFormat("#,###").format(amount);
    }

    @Override
    public Order getOrderById(String id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng"));
    }

    /**
     * Customer-facing variant — chỉ trả về Order nếu thuộc về {@code userId}.
     *
     * <p><b>Quan trọng (IDOR fix):</b> Method {@link #getOrderById(String)} gốc trả về bất kỳ
     * Order nào theo ID. Trong các flow Customer-facing (vd: ComplaintController load Order
     * để hiển thị sản phẩm khiếu nại), nếu Controller quên check ownership thì service sẽ
     * lộ dữ liệu. Method này enforce ownership ngay tại service layer — nếu customer A
     * cố tình request orderId của customer B, throw UnauthorizedException.</p>
     *
     * <p><b>Phòng thủ nhiều lớp (defense-in-depth):</b> vẫn khuyến khích Controller cũng
     * check ownership, nhưng service check đảm bảo không có flow nào có thể leak qua
     * service-layer.</p>
     *
     * @throws ResourceNotFoundException nếu order không tồn tại
     * @throws UnauthorizedException     nếu order.userId != userId
     */
    @Override
    public Order getOrderByIdForCustomer(String orderId, String userId) {
        if (orderId == null || orderId.isBlank()) {
            throw new BadRequestException("orderId không hợp lệ");
        }
        if (userId == null || userId.isBlank()) {
            throw new UnauthorizedException("Không xác định được Customer");
        }
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng"));
        if (order.getUserId() == null || !order.getUserId().equals(userId)) {
            throw new UnauthorizedException("Đơn hàng không thuộc về Customer");
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
