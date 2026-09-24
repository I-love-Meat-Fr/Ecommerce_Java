# Cart → Checkout → Order → Review Integration

> Tài liệu tích hợp giữa Cart / Checkout / Order / Review — phiên bản cập nhật sau khi hoàn thiện các task #1, #6, #8, #9, #13, #14, #15, #21.

## 1. Tổng quan flow

```
[Customer đăng nhập]
        │
        ▼
[Trang Product] ─── add to cart ──►  CartController POST /api/cart/add
        │                                       │
        │                              cartService.addToCart(userId, ...)
        │                                       │
        ▼                                       ▼
[Trang /cart]  ◄──────── Cart hiển thị theo user ─────  CartRepository.findByUserId(userId)
   │   │
   │   │  áp voucher: POST /api/cart/apply-voucher
   │   │      │
   │   │      ├── validateForCheckout(code, ...)
   │   │      ├── computeDiscount(voucher, cartTotal+shipping)
   │   │      └── lưu HttpSession["appliedVoucher"]
   │   ▼
   │  click "Tiến hành thanh toán" → /checkout
   ▼
[Trang /checkout]
   │   │
   │   │  GET: OrderController.checkoutPage()
   │   │      ├── load User từ UserRepository
   │   │      ├── load Cart từ cartService.getCartByUserId(userId)
   │   │      ├── read AppliedVoucher từ HttpSession
   │   │      └── render items, totals, voucher (nếu có)
   │   ▼
   │  POST /checkout → OrderController.checkout()
   │      │
   │      ├── validate form (@Valid CheckoutReq)
   │      ├── inject voucherCode từ session (nếu thiếu)
   │      ├── orderService.createOrder(userId, request)
   │      │       │
   │      │       ├──  @Transactional  (rollback nếu lỗi)
   │      │       ├──  load Cart, validate items + stock
   │      │       ├──  validate Voucher (nếu có)
   │      │       ├──  tạo Order (PENDING), giảm Product.stock
   │      │       ├──  cartService.removeItems(...)  (partial) HOẶC clearCart (full)
   │      │       └──  voucherService.incrementUsed(voucherId) — chỉ khi Order tạo OK
   │      │
   │      └── redirect → /orders/{id}  (Order detail)
   ▼
[Trang /orders]
   │
   │  list orders của user (PENDING / PREPARING / SHIPPING / DELIVERED / CANCELLED)
   ▼
[Trang Product detail]
   │
   │  POST /products/{id}/reviews → ReviewController.createReview()
   │      │
   │      ├── reviewService.createReview(userId, productId, ...)
   │      │       │
   │      │       ├── rating + comment validation
   │      │       ├── user + product existence check
   │      │       ├── ★ TASK #21: orderService.hasUserPurchasedProduct(userId, productId)
   │      │       ├── duplicate review check
   │      │       └── save Review + update Product.rating/reviewCount
   │      ▼
   │  redirect → /products/{id}
```

---

## 2. Cart flow (TASK #1, #6)

### Files liên quan
| File | Vai trò |
|---|---|
| `document/Cart.java` | MongoDB document: `userId (unique)`, `items[]` |
| `repository/CartRepository.java` | `findByUserId`, `deleteByUserId` |
| `service/CartService.java` | Interface: get/add/update/remove/**removeItems**/clear/calculateTotal |
| `service/impl/CartServiceImpl.java` | Implement. `calculateTotal` sum subtotals |
| `controller/web/CartController.java` | Web + API endpoints. Lưu AppliedVoucher vào HttpSession |
| `templates/web/cart.html` | UI: items theo shop, voucher input, summary |

### TASK #1 — Hiển thị Cart (DONE)
- `GET /cart` lấy `user.getId()` từ `CustomUserDetails` → chỉ Cart của user hiện tại.
- Items render đầy đủ: tên, ảnh (`@image.productImage`), đơn giá (`@money.format`), quantity, thành tiền (`item.subtotal`).
- Items được nhóm theo `shopId` → `itemsByShop: Map<String, List<CartItem>>`.
- Empty state: kiểm `cart.items` rỗng → render SVG minh hoạ + CTA "Khám phá sản phẩm".
- Auth: nếu user null → `redirect:/auth/login` (Spring Security flow hiện có).

### TASK #6 — Tính tổng tiền (DONE)
- `CartServiceImpl.calculateTotal(Cart cart)`:
  - `null cart` hoặc `null items` → `BigDecimal.ZERO`
  - Cộng `item.subtotal` (đã tính sẵn khi add/update) bằng `BigDecimal::add`
- Subtotals luôn lấy từ Product backend (không tin frontend): khi `addToCart`/`updateCartItem` set `item.subtotal = price * quantity` từ `product.getPrice()`.
- Logic tính tổng tiền được **tái sử dụng** cho Checkout (`OrderController.checkoutPage`).

---

## 3. Checkout flow (TASK #8, #9)

### Files liên quan
| File | Vai trò |
|---|---|
| `controller/web/OrderController.java` | `GET /checkout`, `POST /checkout` |
| `dto/request/CheckoutReq.java` | Form request: shippingAddress, phone, paymentMethod, items, **voucherCode**, **discount** |
| `templates/web/checkout.html` | UI: items, summary, payment form |

### TASK #8 — Chuẩn bị dữ liệu cho Checkout (DONE)
`OrderController.checkoutPage()`:
1. Load User từ DB (`userRepository.findById`) — dùng cho pre-fill phone/address.
2. Load Cart qua `cartService.getCartByUserId(userId)` — KHÔNG tin client.
3. Tính `cartTotal = cartService.calculateTotal(cart)`.
4. Group items theo shop.
5. Đọc `HttpSession["appliedVoucher"]` → `discount`, `finalTotal`.
6. Validate ngầm: nếu Cart rỗng → vẫn render checkout page với empty state (không 500).

Validation **trước khi tạo Order** (bên trong `OrderServiceImpl.createOrder`):
- Cart tồn tại và không rỗng
- Product còn tồn tại (`productRepository.findById`)
- Product.stock ≥ buyQty
- Voucher (nếu có) còn active, còn hạn, còn lượt (`voucherService.validateForCheckout`)

### TASK #9 — Hiển thị Cart cần Checkout (DONE)
- Hiển thị: product, ảnh (`@image.productImage`), giá (`@money.format`), quantity, item subtotal.
- Subtotal, shipping fee (`SHIPPING_FEE = 15.000đ`), discount (nếu có voucher), final total.
- Empty cart → empty state với CTA "Khám phá sản phẩm".
- KHÔNG cho checkout Cart rỗng — `OrderController.checkout()` kiểm tra và báo lỗi.

---

## 4. Cart → Checkout → Order data contract (TASK #13)

### Data contract
File `dto/request/CheckoutReq.java`:

```java
public class CheckoutReq {
    @NotBlank private String shippingAddress;
    private String phone;
    private PaymentMethod paymentMethod;

    /** null/rỗng = full checkout, không rỗng = partial checkout */
    private List<CheckoutItemReq> items;

    /** Voucher đã áp dụng ở Cart. Server tính lại discount. */
    private String voucherCode;

    /** Server tự tính — client không cần gửi nhưng có thể để double-check */
    private BigDecimal discount;

    public static class CheckoutItemReq {
        private String productId;
        private int quantity;
    }
}
```

### Voucher storage
- `CartController.applyVoucher` → lưu `CartController.AppliedVoucher` vào `HttpSession["appliedVoucher"]`.
- `OrderController.checkoutPage` đọc session → render Checkout summary với discount.
- `OrderController.checkout` (POST):
  - Inject `voucherCode` từ session nếu user quên.
  - Pass `voucherCode` vào `CheckoutReq` → `OrderService.createOrder`.
- `OrderController.checkout` (sau khi Order OK) → `session.removeAttribute("appliedVoucher")`.

### Order document mở rộng
File `document/Order.java` — thêm fields:
```java
private BigDecimal discount;          // snapshot discount server-calculated
private String voucherId;             // voucher đã dùng (null = không có)
private String voucherCode;           // snapshot code
private String voucherName;           // snapshot tên voucher
```

### OrderServiceImpl.createOrder — luồng
1. Load User (404 nếu thiếu).
2. Load Cart (lỗi nếu thiếu/rỗng).
3. Xác định `checkoutProductIds` + `requestedQty`:
   - `request.items` null/rỗng → checkout toàn bộ Cart
   - Có items → partial: chỉ mua các `productId` trong list với quantity chỉ định.
4. Validate Product + stock cho từng item.
5. Tính `subtotal` từ `productPrice * buyQty`.
6. Trừ stock ngay (sẽ rollback nếu lỗi ở bước sau nhờ `@Transactional`).
7. Áp dụng Voucher (nếu có):
   - `voucherService.validateForCheckout(code, firstShopId, null)` → throw `BadRequestException` nếu không hợp lệ
   - Tính `discount = computeDiscount(voucher, subtotal + shippingFee)`
8. `totalAmount = subtotal + shippingFee - discount` (≥ 0).
9. Save Order (PENDING).
10. Cleanup Cart:
    - Full checkout → `cartService.clearCart(userId)`
    - Partial → `cartService.removeItems(userId, checkoutProductIds)`
11. `voucherService.incrementUsed(voucherId)` — chỉ chạy khi Order tạo OK. Nếu lỗi → rollback toàn bộ.

---

## 5. Cart cleanup sau Checkout (TASK #14, #15)

### TASK #14 — Xóa Product sau khi mua thành công (DONE)
- **Chỉ xóa SAU khi Order tạo thành công** (bước 9 ở trên). Nếu `createOrder` throw → `@Transactional` rollback → Cart vẫn nguyên, stock không bị trừ.
- **Full checkout** (`request.items` null/empty) → `cartService.clearCart(userId)` → xóa toàn bộ.
- **Partial checkout** (`request.items` không rỗng) → `cartService.removeItems(userId, checkoutProductIds)` → chỉ xóa `productId` đã mua.
- File mới: method `removeItems` trong `CartService` / `CartServiceImpl`.

### TASK #15 — Cập nhật Cart sau Checkout (DONE)
- Sau Checkout thành công → redirect `/orders/{id}`. Cart đã được clear/removeItems ngay trong transaction.
- Cart count hiển thị trên header → `CartCountInterceptor.postHandle()` load lại từ DB mỗi request → **luôn phản ánh DB mới nhất**, không bị cache.
- Khi user refresh /cart sau checkout → `CartService.getCartByUserId` đọc từ `cartRepository.findByUserId` → thấy Cart đã trống → render empty state.

Các case đã cover:
- Mua thành công → Cart clear (full) hoặc removeItems (partial).
- Order fail (hết stock, voucher invalid, …) → rollback → Cart nguyên.
- Cart rỗng sau checkout → empty state đúng.
- Product hết stock sau checkout → throw `BadRequestException` → rollback.

---

## 6. Review purchase verification (TASK #21)

### Quy tắc "đã mua"
Trong `OrderServiceImpl.hasUserPurchasedProduct(userId, productId)`:
- User phải có **ít nhất 1 Order** chứa `productId`.
- Order KHÔNG ở trạng thái `CANCELLED`.
- Các status PENDING, PREPARING, SHIPPING, DELIVERED đều tính là đã mua (vì giao dịch đã được khởi tạo).

### Áp dụng trong Review
File `service/impl/ReviewServiceImpl.java` — `createReview()`:
```java
// Trước đây: chỉ check user tồn tại + product tồn tại + chưa review → lỏng lẻo
// Bây giờ:
if (!orderService.hasUserPurchasedProduct(userId, productId)) {
    throw new BadRequestException(
        "Bạn chỉ có thể đánh giá sản phẩm sau khi đã mua và đơn hàng không bị hủy");
}
```

### File mới / chỉnh sửa
- `OrderService.hasUserPurchasedProduct(String userId, String productId)` — method mới.
- `OrderServiceImpl.hasUserPurchasedProduct` — query `orderRepository.findByUserId(userId)` → filter status ≠ CANCELLED → match productId.
- `ReviewService.canUserReviewProduct(userId, productId)` — combined check (đã mua + chưa review).
- `ReviewController` — endpoint mới `GET /api/reviews/can-review?productId=...` cho frontend check trước khi show form review.

### Rule "1 user review 1 lần / Product"
Đã có sẵn trong code cũ: `reviewRepository.findByProductIdAndUserId(productId, userId).isPresent()` → throw "Bạn đã đánh giá sản phẩm này rồi".

### User mua nhiều lần
- Mỗi Order được check riêng (cộng dồn). Nhưng vì rule "1 user 1 review / Product" vẫn giữ → chỉ review được 1 lần sau lần mua đầu tiên thành công.
- Nếu sau này muốn cho review sau mỗi lần mua → cần thay đổi rule duplicate check (key = orderId).

---

## 7. Các class / file chính

### Service
| Service | Method chính |
|---|---|
| `CartService` | `getCartByUserId`, `addToCart`, `updateCartItem`, `removeFromCart`, **`removeItems` (NEW)**, `clearCart`, `countItems`, `calculateTotal` |
| `OrderService` | `createOrder`, `getOrderById`, `getOrdersByUserId`, `getOrdersByShopId`, `updateOrderStatus`, `cancelOrder`, **`hasUserPurchasedProduct` (NEW)** |
| `ReviewService` | `createReview`, `updateReview`, `deleteReview`, `getReviewsByProductId`, `hasUserReviewedProduct`, **`canUserReviewProduct` (NEW)** |
| `VoucherService` | `validateForCheckout`, `incrementUsed`, `getVoucherByCode` |

### Repository
| Repository | Method chính |
|---|---|
| `CartRepository` | `findByUserId`, `deleteByUserId` |
| `OrderRepository` | `findByUserId`, `findByUserIdOrderByCreatedAtDesc` |
| `ProductRepository` | `findById` |
| `ReviewRepository` | `findByProductIdAndUserId` (cho rule 1 review/user/product) |

### DTO
| DTO | Vai trò |
|---|---|
| `CheckoutReq` | Form checkout. **Mở rộng**: `voucherCode`, `discount`, `items` (partial) |
| `CheckoutItemReq` | { productId, quantity } cho partial checkout |
| `ReviewReq` | { rating, comment } |
| `OrderHistoryRes` | Hiển thị order history cho customer |

### Controller / Route
| Route | Method | Vai trò |
|---|---|---|
| `GET /cart` | CartController.cartPage | TASK #1 — hiển thị Cart |
| `POST /api/cart/add` | CartController | thêm item |
| `POST /api/cart/apply-voucher` | CartController | áp voucher, lưu session |
| `POST /api/cart/remove-voucher` | CartController | hủy voucher, xóa session |
| `POST /cart/update` | CartController | update qty |
| `POST /cart/remove` | CartController | remove 1 item |
| `GET /checkout` | OrderController.checkoutPage | TASK #8 #9 — render |
| `POST /checkout` | OrderController.checkout | tạo Order, cleanup Cart |
| `GET /orders` | OrderController | order history |
| `GET /orders/{id}` | (đã có) | order detail |
| `POST /products/{id}/reviews` | ReviewController | TASK #21 — verify purchase |
| `GET /api/reviews/can-review` | ReviewController | check quyền review (NEW) |

---

## 8. Validation quan trọng

### Cart
- `addToCart`: product tồn tại; quantity > 0; không vượt stock.
- `updateCartItem`: quantity > 0; không vượt stock.
- `calculateTotal`: chịu null cart / null items.

### Checkout
- `shippingAddress` không rỗng (`@NotBlank`).
- Cart phải có items.
- Từng `productId` trong Cart phải tồn tại + đủ stock.

### Order (trong `OrderServiceImpl.createOrder`)
- User tồn tại.
- Cart tồn tại + không rỗng.
- Từng item: product tồn tại, stock ≥ buyQty, quantity > 0.
- Nếu partial: tất cả `productId` trong `request.items` phải nằm trong Cart.
- Nếu có `voucherCode`: voucher phải active, còn hạn, còn lượt, áp dụng cho shop/product.
- Subtotal, shipping fee, discount, final total đều do server tính.
- Toàn bộ thực hiện trong `@Transactional(rollbackFor = Exception.class)`.

### Review
- `rating` 1..5.
- `comment` không rỗng.
- User tồn tại.
- Product tồn tại.
- **★ User phải đã mua Product** (`orderService.hasUserPurchasedProduct`).
- Chưa review Product này (rule 1 review/user/product).

---

## 9. Tích hợp với phần Order của Quốc Anh

- `OrderServiceImpl.createOrder` đã handle:
  - Trừ `Product.stock` (rollback nếu lỗi)
  - Tạo `Order` với status `PENDING`
  - Cleanup Cart (full hoặc partial)
  - Increment Voucher used count (sau khi Order OK)
- Sau khi Order thành công → redirect `/orders/{id}` (hiển thị order detail).
- Quốc Anh có thể mở rộng:
  - Order status transition (PENDING → PREPARING → SHIPPING → DELIVERED) — đã có sẵn trong `updateOrderStatus`.
  - Stock restore khi cancel — đã có sẵn trong `cancelOrder`.
  - Voucher used tracking — đã integrate sẵn trong `createOrder`.

### Những phần Order/Review mà thành viên khác cần biết
- **Order document** mới có `discount`, `voucherId`, `voucherCode`, `voucherName` → cần cập nhật UI admin/vendor order detail nếu muốn hiển thị.
- **Review endpoint** `/api/reviews/can-review` cần được frontend gọi trước khi show form review (vd: trang `/products/{id}` → chỉ show "Đánh giá" nếu API trả `true`).
- **Partial checkout** — hiện `CheckoutReq.items` đã được nhận nhưng frontend chưa gửi lên (chỉ checkout full). Nếu muốn dùng partial, frontend cần build `items` list từ Cart items user chọn (vd: checkbox).
- **Voucher session** — `CartController.SESSION_APPLIED_VOUCHER = "appliedVoucher"` — nếu thêm bước mới giữa Cart và Checkout cần nhớ key này.
