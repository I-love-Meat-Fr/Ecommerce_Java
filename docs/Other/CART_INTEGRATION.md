# Cart Integration Documentation

## 1. Scope

This document describes how the **Cart module** integrates with the rest of the
CNJ70 Ecommerce application. It is written based on the actual implementation
in this repository (not hypothetical APIs).

It covers:

- Cart architecture and responsibilities
- Stock validation boundaries (Cart vs. Order)
- Checkout preparation
- Voucher integration from the Cart side
- The Cart → Checkout / Order contract
- Security rules and ownership boundaries

This document also explicitly states what Cart **does NOT own**, to prevent
future Cursor sessions from modifying the wrong module.

Cart **does NOT own**:

- Voucher business rules, validity, or persistence
- Order creation, stock decrease, or order transactions
- Product pricing or inventory rules
- Checkout page business logic
- Review functionality

---

## 2. Cart Architecture

The Cart module follows a standard layered structure:

```
HTTP request
   ↓
CartController        (com.ecommerce.cnj70.controller.web.CartController)
   ↓
CartService           (com.ecommerce.cnj70.service.CartService)
   ↓
CartServiceImpl       (com.ecommerce.cnj70.service.impl.CartServiceImpl)
   ↓
CartRepository        (com.ecommerce.cnj70.repository.CartRepository)
   ↓
MongoDB collection: carts
```

Actual class names from the project:

- `com.ecommerce.cnj70.document.Cart` — MongoDB document
- `com.ecommerce.cnj70.repository.CartRepository` — Spring Data repository
- `com.ecommerce.cnj70.service.CartService` — interface
- `com.ecommerce.cnj70.service.impl.CartServiceImpl` — implementation
- `com.ecommerce.cnj70.controller.web.CartController` — web controller (Thymeleaf pages + JSON APIs)
- `com.ecommerce.cnj70.interceptor.CartCountInterceptor` — header cart-count badge

---

## 3. Cart Responsibilities

Cart is responsible for:

- Displaying the Cart page (`/cart`)
- Adding a Product to Cart (server + AJAX API)
- Increasing / decreasing quantity (server-side POST)
- Removing a Product from Cart
- Counting cart items (header badge)
- Calculating the **Cart subtotal** (sum of `price * quantity`)
- **Validating that quantity does not exceed stock** when adding/updating items
- Preparing Checkout data (the items list, prices, quantities — server-side)
- Voucher input (UI only)
- Voucher integration: forwarding `voucherCode` to `VoucherService` and
  displaying the returned discount / total
- Stock re-validation on every cart read (max quantity check)

Cart is **NOT** responsible for:

- Decreasing stock
- Creating Orders
- Voucher rule enforcement
- Discount rule invention
- Payment processing

---

## 4. Stock Validation

The Cart module validates stock at the Cart layer, but never decreases it.

Flow:

```
Add to Cart       → CartService checks product.stock vs. requested qty (max qty cap)
    ↓
Update qty        → CartService caps at product.stock (max qty cap)
    ↓
Customer waits    → stock may change
    ↓
Checkout page     → Cart re-validates current stock
    ↓
Order creation    → OrderService re-checks stock AND decreases it
    ↓
Order cancelled   → OrderService restores stock
```

Implementation details:

- `CartServiceImpl.addToCart` caps quantity to `item.stock`.
- `CartServiceImpl.updateCartItem` caps quantity to `item.stock`.
- `OrderServiceImpl.createOrder` re-checks `product.stock` and decreases it.
  This is **pre-existing Order logic** and is not modified by the Cart task.
- `OrderServiceImpl.restoreStock` runs on order cancellation. This is
  **pre-existing Order logic** and is not modified by the Cart task.

**Cart DOES NOT decrease stock. Order handles stock decrease.**

---

## 5. Voucher Integration

The Cart module reuses the existing `VoucherService` from the Voucher / Vendor
module. Cart does **not** re-implement any voucher business rule.

### Existing service method (Voucher module):

```java
// com.ecommerce.cnj70.service.VoucherService
Voucher validateForCheckout(String code, String shopId, String productId);
```

Parameters used by Cart:

| Parameter  | Value Cart sends | Notes |
|-----------|------------------|-------|
| `code`    | User-entered code | Trimmed, non-blank |
| `shopId`  | `null`            | Cart-level, not shop-scoped |
| `productId` | `null`         | Cart-level, not product-scoped |

Return value: `Voucher` (the validated voucher document) — or an exception is
thrown via `BadRequestException` / `ResourceNotFoundException`.

### Existing supporting methods (already in Voucher module):

- `getVoucherByCode(String code)` — pre-existing
- `incrementUsed(String voucherId)` — pre-existing (Cart does NOT call this)

### What Cart sends

```
POST /api/cart/apply-voucher
Content-Type: application/x-www-form-urlencoded
   code=<voucherCode>
```

The client only sends the voucher code. The Cart server endpoint reads the
current cart and computes the subtotal **from the database**, then calls
`VoucherService.validateForCheckout(...)`.

### What Cart returns

On success (HTTP 200):

```json
{
  "success": true,
  "message": "Áp dụng voucher thành công",
  "voucherId": "...",
  "voucherCode": "ABC123",
  "voucherName": "...",
  "voucherShopName": "...",
  "discount": 50000,
  "cartSubtotal": 500000,
  "shippingFee": 15000,
  "finalTotal": 465000
}
```

On failure (HTTP 200, `success=false`):

```json
{
  "success": false,
  "message": "<error from VoucherService>",
  "cartSubtotal": 500000,
  "shippingFee": 15000,
  "finalTotal": 515000
}
```

### How Cart handles success

- Displays the voucher name next to the input
- Shows the discount row (`Giảm giá: -Xđ`)
- Recalculates the final total: `subtotal + shipping - discount`
- Provides a remove-voucher button (`POST /api/cart/remove-voucher`)

### How Cart handles failure

- Shows the error message returned by `VoucherService`
- Keeps the cart usable: subtotal and final total remain computed without discount
- Does not apply any fallback discount

### Voucher business logic is owned by the Voucher module

The Cart module does not:

- Re-implement validity checks
- Re-implement expiration rules
- Re-implement usage-limit rules
- Re-implement minimum-order rules
- Re-implement discount formulas
- Increment usage counters

`VoucherService.incrementUsed(...)` is called by the Order flow when the order is
successfully placed — this is **not** in Cart's scope.

---

## 6. Voucher Flow

Actual flow:

```
[Cart UI: textbox "coupon-input"]
              ↓ click "Áp dụng"
[fetch POST /api/cart/apply-voucher  with code]
              ↓
[CartController.applyVoucher]
              ↓
[CartService.getCartByUserId]
              ↓
[CartService.calculateTotal]                ← server-side subtotal
              ↓
[VoucherService.validateForCheckout(code, null, null)]   ← existing voucher module
              ↓ (valid)
[Voucher returned, discount computed server-side]
              ↓
[JSON response: {success, discount, finalTotal, ...}]
              ↓
[Cart UI updates: applied voucher badge, discount row, final total]
```

If the user clicks the "×" remove button:

```
[fetch POST /api/cart/remove-voucher]
              ↓
[CartController.removeVoucher]
              ↓
[return {discount: 0, finalTotal: subtotal + shippingFee}]
              ↓
[Cart UI: reset input, hide discount, hide applied badge]
```

---

## 7. Total Calculation

The actual formulas used in Cart display:

```
ItemSubtotal = Product.price × Quantity
CartSubtotal = Σ ItemSubtotal
Shipping     = 15,000 VND       (pre-existing project rule)
Discount     = value from VoucherService validation
FinalTotal   = max(0, CartSubtotal + Shipping − Discount)
```

### Where the shipping rule comes from

`15,000 VND` shipping fee is a **pre-existing project rule**. It has been in
the codebase since the initial Order implementation and is applied by the
`OrderServiceImpl.createOrder(...)` method. Cart reuses this value
(`SHIPPING_FEE = 15,000 VND` constant in `CartController`) **only for Cart-side
display**. The authoritative shipping fee in the order flow lives in the Order
module.

Cart does not invent a second shipping rule.

### What Cart computes

Cart computes:

- `cartSubtotal` — sum of item subtotals (server-side)
- `shippingFee` — `15,000 VND` (pre-existing rule, displayed in Cart for transparency)
- `discount` — result from `VoucherService.validateForCheckout(...)` (server-side)
- `finalTotal` — `cartSubtotal + shippingFee − discount`, floored at 0

---

## 8. Checkout Contract

When the customer proceeds to Checkout from Cart, the Cart page sends:

```
GET /checkout  → Cart → Checkout (form-based)
```

What Cart provides to Checkout:

| Field             | Type     | Source                        | Purpose |
|-------------------|----------|-------------------------------|---------|
| `userId`          | server   | Authentication principal      | Identifies the buyer |
| `cartItems`       | server   | `Cart.items` (Cart document)  | Line items for the order |
| `cartSubtotal`    | server   | `CartService.calculateTotal`  | Subtotal reference |
| `shippingAddress` | form     | Checkout form (user input)    | Shipping destination |
| `phone`           | form     | Checkout form (user input)    | Contact phone |
| `paymentMethod`   | form     | Checkout form (`COD`, `VNPAY`)| Payment method |

The Checkout page (`web/checkout.html`) submits `CheckoutReq`:

```java
public class CheckoutReq {
    private String shippingAddress;
    private String phone;
    private PaymentMethod paymentMethod;
    private List<CheckoutItemReq> items;
}
```

Cart does **not** modify `CheckoutReq`, `Order`, or `OrderService`. The
authoritative pricing, stock, and final-total logic remains in the Order flow.

### Known integration gap (out of Cart scope)

The current `CheckoutReq` does **not** carry `voucherCode`. The Cart-side
voucher integration in this task:

- Validates the voucher against `VoucherService`
- Displays the discount in the Cart UI
- Provides the customer with a visible discount preview

It does **not** persist the voucher code to the order form, because:

- `OrderService` and `Order` entity are owned by another member
- The rule says: "DO NOT modify Order business logic"
- "Create the smallest Cart-side DTO/adapter necessary and document the dependency"

This limitation is documented here so the Order-member task can decide whether
to extend `CheckoutReq` (e.g. add `voucherCode`, `voucherDiscount`) on the
Order side. Until that is done, the voucher discount is **preview-only** at
the Cart layer; the Order flow will continue to apply the existing Order-level
total (`subtotal + shippingFee`).

---

## 9. Security Rules

- `userId` is always taken from the authenticated principal (`@AuthenticationPrincipal CustomUserDetails`). Never from request parameters or body.
- Product price is **always** read from `CartItem.price` or `Product.price` server-side. Never trusted from the browser.
- Product stock is **always** re-checked server-side at Order time. Cart only caps max-quantity locally.
- Discount amount is **always** computed by `VoucherService`. Never trusted from the browser.
- Subtotal passed to voucher validation is the **server-side** cart subtotal.
- Final total displayed in Cart is informational; the Order flow re-computes its own total.
- Voucher rules belong to `VoucherService`. Cart does not implement them.

---

## 10. Ownership Boundaries

| Module         | Responsibility |
|----------------|----------------|
| **Cart**       | Cart data, Cart validation, max-stock check, Cart subtotal, voucher input UI, voucher forwarding to VoucherService, checkout preparation (items list, totals) |
| **Product**    | Product data, product search, product detail display |
| **Inventory**  | Stock decrease / restore (delegated to Order flow); stock is read-only from Cart's perspective |
| **Voucher**    | Voucher validation, expiration, usage limits, min-order rules, discount rules, voucher persistence, `incrementUsed` |
| **Checkout**   | Checkout page, address / phone / payment form, transition to Order |
| **Order**      | Order creation, final stock re-validation, stock decrease, order transaction, cart clearing after successful order |
| **Review**     | Reviews / ratings |

The purpose of this table is to prevent future Cursor sessions from modifying
the wrong module when implementing Cart-side features.

---

## 11. Tasks Status

Using the **original** Cart task numbering:

| # | Task                                              | Status            |
|---|---------------------------------------------------|-------------------|
| #7  | Kiểm tra số lượng tồn kho (stock check)          | **DONE**          |
| #8  | Chuẩn bị dữ liệu cho Checkout                    | **DONE**          |
| #9  | Hiển thị Cart cần Checkout                       | **DONE**          |
| #10 | Hiển thị thông tin Product                        | **DONE**          |
| #11 | Hiển thị số lượng                                 | **DONE**          |
| #12 | Hiển thị tổng tiền                                | **DONE**          |
| #13 | Gửi dữ liệu Checkout cho phần Order              | **PARTIAL** — items, subtotal, shipping to CheckoutReq. Voucher not propagated to CheckoutReq (see §8 "Known integration gap"). |
| #14 | Xóa Product khỏi Cart sau khi mua thành công      | **WAITING FOR ORDER SUCCESS FLOW** |
| #15 | Cập nhật lại Cart sau Checkout                    | **WAITING FOR CHECKOUT + ORDER FLOW** |

Notes:

- #14 and #15 depend on the Order member's success flow + cart cleanup.
- `#13 PARTIAL` is honest: the data handoff works, but voucher code does not
  travel into `CheckoutReq` because that is in the Order member's scope.
- The Cart-side voucher integration adds the voucher preview + total display
  in the Cart layer. It does not modify `CheckoutReq`, `OrderService`, or
  `Order`.

---

## 12. Pending Work for Other Members

### Order member

Needs to handle:

- Order creation (`OrderServiceImpl.createOrder`)
- Final stock re-validation before order commit
- Stock decrease (already pre-existing)
- Successful purchase handling
- Cart cleanup after successful Order (already pre-existing `cartService.clearCart(userId)` call)
- Task #14: remove purchased products from Cart
- Task #15: update Cart after Checkout
- Optional: extend `CheckoutReq` with `voucherCode` and apply voucher discount
  in `OrderServiceImpl` if Cart-side voucher preview should also affect the
  final order total

### Voucher member

Needs to maintain:

- Voucher rules (`VoucherServiceImpl.validateForCheckout`)
- Voucher validity (active, date, usage, min order, shop/product restriction)
- Discount rules (`DiscountType.PERCENT`, `DiscountType.AMOUNT`)
- Usage limits
- `incrementUsed` is called by Order, not Cart

If these already exist (they do), document them as existing functionality —
no changes are required from Cart.

---

## 13. Instructions for Future Cursor Work

1. **Read this document** before modifying Cart.
2. **Inspect actual code** before making assumptions. Run:
   - `git log --oneline -- <file>` to see who last touched a file
   - `git blame` to see line ownership
   - `git show <commit> -- <file>` to see what was added and why
3. **Do not modify another member's module without explicit permission.**
   Especially: `OrderService`, `Order` entity, `VoucherService`,
   `VoucherController` business logic, Product business logic.
4. **Reuse existing services.** Cart reuses `VoucherService.validateForCheckout`
   and `CartService.calculateTotal`. Do not duplicate them.
5. **Do not duplicate business logic.** Voucher rules, stock rules, and order
   rules all live in their own modules.
6. **Do not modify `OrderService` for Cart convenience.** If a Cart-side
   feature needs data from Order, add it to the contract (DTO) and let the
   Order member implement it.
7. **Do not modify `VoucherService` for Cart convenience.** Use
   `VoucherService.validateForCheckout(...)` and `VoucherService.getVoucherByCode(...)`.
8. **Keep changes minimal.** Cart-side features should touch Cart files first.
9. **Check `git diff` before commit.** Verify that only Cart files changed.
10. **Update this document** if Cart's API / contract changes.

---

## Appendix A — Cart Endpoints (current implementation)

| Method | Path                              | Auth   | Purpose |
|--------|-----------------------------------|--------|---------|
| GET    | `/cart`                           | login  | Render cart page |
| POST   | `/api/cart/add`                   | login  | Add product (AJAX JSON) |
| GET    | `/api/cart/count`                 | login  | Header badge JSON |
| POST   | `/cart/update`                    | login  | Update quantity (form) |
| POST   | `/cart/remove`                    | login  | Remove item (form) |
| POST   | `/api/cart/apply-voucher`         | login  | Apply voucher (AJAX JSON) |
| POST   | `/api/cart/remove-voucher`        | login  | Clear applied voucher (AJAX JSON) |

## Appendix B — Voucher Endpoints (pre-existing, not modified)

| Method | Path                              | Owner | Notes |
|--------|-----------------------------------|-------|-------|
| POST   | `/checkout/apply-voucher`         | Voucher module | Generic voucher validation |
| POST   | `/checkout/place-order`           | Voucher module | Order with voucher (increments usage) |

The Cart module's `/api/cart/apply-voucher` is **Cart's own endpoint** that
internally calls `VoucherService.validateForCheckout(...)`. Cart does not
duplicate `calculateDiscount` logic — it computes the same formula used in
`VoucherController.calculateDiscount` for display only.
