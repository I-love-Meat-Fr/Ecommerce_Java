# Cart & Review Integration Guide

Master reference for the **Cart** and **Review** modules in the CNJ70 Ecommerce
project. This file complements `docs/CART_INTEGRATION.md` and
`docs/REVIEW_INTEGRATION.md` with cross-module contracts, status of every task
in scope, and a prompt template other developers can paste into Cursor to
continue working on their tasks.

> **Read this file first** before touching Cart or Review code, especially
> before any cross-module change (Cart → Checkout → Order, Review → Order,
> Cart → Voucher).

---

## 1. Architecture

```
┌─────────────────────────── Web Layer ───────────────────────────┐
│  CartController (web)    ReviewController (web)                  │
│  ProductController (web) OrderController (web) [other team]      │
└──────────────────────────────────────────────────────────────────┘
                │                           │
                ▼                           ▼
┌─────────────────────────── Service Layer ───────────────────────┐
│  CartService               ReviewService                         │
│  OrderService [other team] ProductService [other team]           │
│  VoucherService [other team]                                     │
└──────────────────────────────────────────────────────────────────┘
                │                           │
                ▼                           ▼
┌─────────────────────────── Persistence Layer ───────────────────┐
│  CartRepository (MongoDB)   ReviewRepository (MongoDB)           │
│  ProductRepository         OrderRepository [other team]          │
│  UserRepository            VoucherRepository [other team]        │
└──────────────────────────────────────────────────────────────────┘
                │                           │
                ▼                           ▼
┌─────────────────────────── MongoDB ─────────────────────────────┐
│  collection: carts          collection: reviews                 │
│  collection: products       collection: orders                  │
│  collection: users          collection: vouchers                │
└──────────────────────────────────────────────────────────────────┘
```

**Module ownership — DO NOT cross these lines without an integration contract:**

| Module  | Owner (current sprint) | Allowed dependencies                  |
|---------|------------------------|---------------------------------------|
| Cart    | Cart team              | Product (read-only), Voucher (validate) |
| Review  | Review team            | Product (read-only), User (read-only)   |
| Order   | Order team             | Cart, Product, Voucher, User            |
| Checkout| Checkout team          | Order, Cart                            |
| Voucher | Voucher team           | Shop (read-only)                        |

---

## 2. Cart Architecture

`CartController` (web layer) → `CartService` / `CartServiceImpl` → `CartRepository` → MongoDB collection `carts`.

Responsibilities:

- Display Cart for an authenticated user.
- Add / update quantity / remove items.
- Validate quantity `>= 1` and `<= Product.stock`.
- Calculate subtotal from server-side `Product.price` (never trust client price).
- Apply voucher (display only — final price is recomputed by Voucher/Order modules).
- Provide data to Checkout via existing `Cart` document fields.
- `clearCart(userId)` is called by `OrderServiceImpl.createOrder()` after a successful order (Task #14 / #15 already implemented).

---

## 3. Cart Data Model

`com.ecommerce.cnj70.document.Cart` (MongoDB collection `carts`):

```java
@Document(collection = "carts")
class Cart {
    @Id String id;
    @Indexed(unique = true) String userId;        // 1 cart per user
    List<CartItem> items;
    LocalDateTime updatedAt;

    static class CartItem {
        String productId;
        String productName;
        String imageUrl;                          // thumbnail from Product.thumbnailUrl
        BigDecimal price;                         // server-side price snapshot
        int quantity;                             // 1..stock
        BigDecimal subtotal;                      // = price * quantity, computed server-side
        String shopId;                            // for grouping in UI
        String shopName;
        Integer stock;                            // cached Product.stock at add time
    }
}
```

Key fields every other module may depend on:

- `userId` — Cart belongs to exactly one user (`@Indexed(unique = true)`).
- `items[].productId`, `items[].shopId` — referenced by Order and Checkout.
- `items[].price` and `items[].subtotal` — captured at add/update time. The
  authoritative price still lives on `Product.price`; OrderService recomputes
  from Product when creating the order.

---

## 4. Cart APIs

All endpoints are owned by `CartController`.

| Method | Path                          | Auth   | Purpose                                                  |
|--------|-------------------------------|--------|----------------------------------------------------------|
| GET    | `/cart`                       | User   | Render `web/cart` template                               |
| POST   | `/api/cart/add`               | User   | Add product to cart (form params: `productId`, `quantity`)|
| GET    | `/api/cart/count`             | any    | Header badge count (`itemCount`, `totalQuantity`)        |
| POST   | `/cart/update`                | User   | Update quantity (form params: `productId`, `quantity`)    |
| POST   | `/cart/remove`                | User   | Remove item (form param: `productId`)                    |
| POST   | `/api/cart/apply-voucher`     | User   | Apply voucher code; returns discount + finalTotal preview |
| POST   | `/api/cart/remove-voucher`    | User   | Reset voucher preview                                    |

Stock handling:

- `addToCart` clamps new quantity to `Math.min(current + add, item.stock)`.
- `updateCartItem` clamps to `Math.min(requested, item.stock)`. Quantity `<= 0`
  removes the item.
- `calculateTotal(cart)` sums `items[].subtotal` server-side. Client never
  sends totals.

---

## 5. Cart Quantity Logic

```
addToCart(userId, productId, qty):
    cart = getCartByUserId(userId)
    product = productRepository.findById(productId)
    existing = cart.items.find { it.productId == productId }
    if existing:
        newQty = min(existing.quantity + qty, existing.stock or MAX_VALUE)
        existing.quantity = newQty
        existing.subtotal = existing.price * newQty
    else:
        cart.items.add new CartItem(... product snapshot ...)

updateCartItem(userId, productId, qty):
    if qty <= 0: removeFromCart(...)
    else: clamp qty to stock, recompute subtotal

removeFromCart(userId, productId): drop item, save cart

calculateTotal(cart): sum(items.subtotal) -- SERVER ONLY
```

Hard rules:

- `quantity >= 1` always.
- `quantity <= product.stock` always.
- Cart **never** mutates `product.stock`.
- Cart **never** trusts `price` / `subtotal` / `total` from the client.

---

## 6. Cart Stock Validation

Cart reads `product.stock` at add time and caches it on the item.
`CartServiceImpl.addToCart` and `updateCartItem` clamp to that cached value.

The **authoritative** stock check still happens in
`OrderServiceImpl.createOrder` (Task #7, owned by Order team):

```java
if (product.getStock() < cartItem.getQuantity()) {
    throw new BadRequestException(...);
}
```

Cart does **not** re-decrease stock — that is exclusively Order's job.

---

## 7. Cart Total Calculation

`CartService.calculateTotal(cart)`:

```java
return cart.items.stream()
        .map(Cart.CartItem::getSubtotal)   // subtotal is server-computed
        .filter(Objects::nonNull)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
```

The UI also adds a fixed shipping fee (`SHIPPING_FEE = 15_000đ` in
`CartController`). Voucher discount is layered on top inside
`apply-voucher` (display-only) and inside `OrderService` at order time.

---

## 8. Cart → Checkout Contract

The Checkout module does **not** need a new endpoint or DTO from Cart.
`OrderServiceImpl.createOrder` reads the Cart directly via
`cartRepository.findByUserId(userId)` — this is the current contract.

What Cart guarantees when `OrderServiceImpl` calls it:

- A `Cart` document exists for `userId` (Cart auto-creates an empty one on
  first `getCartByUserId` call, so `findByUserId` may return empty).
- `cart.items` is never `null` (initialized as empty list).
- `cart.items[i].price`, `.subtotal`, `.quantity`, `.shopId`, `.productId` are
  populated from `Product` server-side.
- `cart.items[i].imageUrl` is `Product.thumbnailUrl` snapshot.

What Cart needs from Checkout / Order:

- A successful order path that calls `CartService.clearCart(userId)`.
  This is already done in `OrderServiceImpl.createOrder()`.

---

## 9. Checkout → Order Contract

`OrderService` interface:

```java
Order createOrder(String userId, CheckoutReq request);
```

`CheckoutReq`:

```java
class CheckoutReq {
    @NotBlank String shippingAddress;
    String phone;
    PaymentMethod paymentMethod;          // COD or VNPAY (see PaymentMethod enum)
    List<CheckoutItemReq> items;
}
class CheckoutItemReq {
    String productId;
    int quantity;
}
```

`Order` (MongoDB collection `orders`):

```java
class Order {
    String id, userId, userName, userEmail, userPhone, shippingAddress;
    List<OrderItem> items;
    BigDecimal subtotal, shippingFee, totalAmount;
    OrderStatus status;                   // PENDING by default
    PaymentMethod paymentMethod;
    boolean paid;
    String shopId, shopName;              // single shop per order in current impl
    LocalDateTime createdAt, updatedAt, deliveredAt;

    static class OrderItem {
        String shopId, productId, productName, imageUrl;
        BigDecimal price;
        int quantity;
        BigDecimal subtotal;
    }
}
```

`OrderServiceImpl.createOrder` flow:

1. Load `User`, `Cart` for `userId`. Throw if cart empty.
2. For every `CartItem`: load `Product`, re-check stock, build `OrderItem`.
3. Subtract from `Product.stock` and save (the only place stock decreases).
4. Build `Order` with `subtotal + shippingFee = totalAmount` (currently fixed
   `15_000đ`; no voucher discount in current OrderService).
5. `cartService.clearCart(userId)`.
6. Save order.

---

## 10. Cart Cleanup

Cart is cleared automatically by `OrderServiceImpl.createOrder()` (line 95):

```java
Order savedOrder = orderRepository.save(order);
cartService.clearCart(userId);
return savedOrder;
```

`CartService.clearCart(userId)` empties `items`, updates `updatedAt`, saves.

`CartRepository.deleteByUserId(userId)` also exists but is currently unused.

**Task #14 (Remove Product from Cart after success)** — DONE by Order team.
**Task #15 (Refresh Cart after Checkout)** — DONE; `clearCart` removes items
and the next `/cart` GET rebuilds via `getCartByUserId`.

---

## 11. Voucher Integration

Cart only **displays** voucher impact; it does not own voucher logic.

Flow used in `CartController.applyVoucher`:

1. Client POSTs `code`.
2. Cart reloads subtotal via `cartService.calculateTotal(cart)`.
3. `VoucherService.validateForCheckout(code, null, null)` — owned by Voucher team.
4. `CartController.computeDiscount(voucher, subtotal)` mirrors the Voucher
   module's discount rule (PERCENT / AMOUNT with min-order + max-discount).
5. Returns `{ discount, cartSubtotal, shippingFee, finalTotal }` for the UI.

> ⚠️ Final discount is recalculated inside Voucher / Order at order creation.
> The Cart preview is **display only**. Order/Voucher team owns the
> authoritative computation.

---

## 12. Review Architecture

`ReviewController` (web layer) → `ReviewService` / `ReviewServiceImpl` → `ReviewRepository` → MongoDB collection `reviews`.

Responsibilities:

- Create / read / update / delete reviews.
- Validate rating (1-5) and non-empty comment.
- Ensure ownership (only the author can edit / delete).
- Recompute `Product.rating` and `Product.reviewCount` after every mutation.

`ProductController.productDetail` renders the reviews list for each product
page (used by `templates/web/product-detail.html`).

---

## 13. Review MongoDB Schema

`com.ecommerce.cnj70.document.Review` (MongoDB collection `reviews`):

```java
@Document(collection = "reviews")
class Review {
    @Id String id;
    @Indexed String productId;
    @Indexed String userId;
    String userName;          // snapshot of User.fullName at write time
    String userAvatar;        // snapshot of User.avatarUrl at write time
    int rating;               // 1..5
    String comment;           // trimmed, non-blank
    @CreatedDate LocalDateTime createdAt;
}
```

Indexes (created automatically via `MongoConfig` + `auto-index-creation: true`):

- `_id` (default)
- `productId` (non-unique, for `findByProductId` queries)
- `userId` (non-unique, for `findByUserId` queries)

---

## 14. Review APIs

| Method | Path                                          | Auth | Purpose                                        |
|--------|-----------------------------------------------|------|------------------------------------------------|
| GET    | `/products/{productId}/reviews`               | any  | List reviews for a product (JSON)              |
| POST   | `/products/{productId}/reviews`               | User | Create review (form: `rating`, `comment`)      |
| GET    | `/reviews/{reviewId}`                         | any  | Get a single review                            |
| POST   | `/reviews/{reviewId}/edit`                    | User | Update own review (form: `rating`, `comment`)  |
| POST   | `/reviews/{reviewId}/delete`                  | User | Delete own review                              |
| GET    | `/my-reviews`                                 | User | List current user's reviews (page)             |
| GET    | `/api/reviews/check?productId=...`            | any  | `true` if current user already reviewed product|

---

## 15. Review Create Flow

```
Product detail page
    → user submits #pdp-review-form (rating, comment)
    → POST /products/{productId}/reviews (form binding @ModelAttribute ReviewReq)
    → ReviewController.createReview()
        - userId from @AuthenticationPrincipal CustomUserDetails (NEVER from client)
        - reviewService.createReview(userId, productId, rating, comment)
            * validate 1 <= rating <= 5
            * validate comment not blank
            * ensure User + Product exist
            * ensure user has NOT already reviewed (unique by productId+userId)
            * save Review
            * updateProductRating(productId) → recompute avg + count, save Product
        - redirect back to /products/{productId}
```

---

## 16. Review Edit Flow

```
My reviews or product detail edit button
    → POST /reviews/{reviewId}/edit (rating, comment)
    → ReviewController.updateReview()
        - userId from session
        - reviewService.updateReview(reviewId, userId, rating, comment)
            * load review; throw if missing
            * ownership check: review.userId == userId, else BadRequestException
            * revalidate rating + comment
            * save Review
            * updateProductRating(...)
        - redirect to product detail page
```

---

## 17. Review Delete Flow

```
Owner clicks delete
    → POST /reviews/{reviewId}/delete
    → ReviewController.deleteReview()
        - loads review to know which productId to redirect to
        - reviewService.deleteReview(reviewId, userId)
            * ownership check (same as edit)
            * delete Review
            * updateProductRating(...)
        - redirect to product detail page
```

---

## 18. Review Display Flow

`ProductController.productDetail` loads reviews via
`reviewService.getReviewsByProductId(id)` (sorted `createdAt DESC`) and passes
them to `web/product-detail.html` as the `reviews` model attribute.

UI tabs (`Mô tả`, `Thông số`, `Đánh giá`) are switched by JS in
`product-detail.html`. Average rating + count are also passed (via
`Product.rating` and `Product.reviewCount`) for the header summary.

---

## 19. Review Security

| Rule                                | Enforced where                                   |
|-------------------------------------|--------------------------------------------------|
| `userId` from authenticated session | `ReviewController` via `@AuthenticationPrincipal` |
| Owner-only edit                     | `ReviewServiceImpl.updateReview`                 |
| Owner-only delete                   | `ReviewServiceImpl.deleteReview`                  |
| Rating 1..5                         | `ReviewReq` `@Min`/`@Max` + service-layer guard  |
| Comment not blank                   | `ReviewReq` `@NotBlank` + service-layer guard    |
| Product must exist                  | `ReviewServiceImpl.createReview`                 |
| One review per (product, user)      | `ReviewServiceImpl.createReview` + index         |

`ReviewReq` Bean Validation runs on form binding via `@ModelAttribute @Valid`.
Service-layer guards re-validate because the service is also reachable from
JSON/REST callers in the future.

---

## 20. Purchase Verification

`hasPurchased(userId, productId)` is **NOT implemented** anywhere in the
project (`grep hasPurchased` returns 0 matches).

This is a cross-module dependency:

- Review wants to require "user must have bought this product before reviewing".
- That requires reading `Order` (owned by Order team).

Recommendation for the Order team (do **not** edit Order yourself):

```java
// Suggested addition to OrderService — owned by Order team
boolean hasUserPurchasedProduct(String userId, String productId);
```

Implementation outline:

```java
return orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
        .filter(o -> o.getStatus() == OrderStatus.DELIVERED)  // or PAID
        .flatMap(o -> o.getItems().stream())
        .anyMatch(i -> i.getProductId().equals(productId));
```

When Order team delivers this method, Review should call it inside
`createReview()` **before** saving (throwing `BadRequestException` if false).
Until then, `Task #21` status is **BLOCKED** on Order team.

---

## 21. Important Files

### Cart

| File                                                                       | Purpose                                                                 |
|----------------------------------------------------------------------------|-------------------------------------------------------------------------|
| `src/main/java/com/ecommerce/cnj70/document/Cart.java`                     | MongoDB `@Document(collection = "carts")` entity + nested `CartItem`.    |
| `src/main/java/com/ecommerce/cnj70/repository/CartRepository.java`         | `findByUserId`, `deleteByUserId`, standard `MongoRepository` ops.        |
| `src/main/java/com/ecommerce/cnj70/service/CartService.java`                | Interface — add/update/remove/calculate/clear.                          |
| `src/main/java/com/ecommerce/cnj70/service/impl/CartServiceImpl.java`      | All cart ops; clamps to stock; auto-creates cart for new user.          |
| `src/main/java/com/ecommerce/cnj70/controller/web/CartController.java`     | `/cart`, `/api/cart/*` endpoints; voucher preview.                      |
| `src/main/resources/templates/web/cart.html`                               | UI: grouped by shop, qty stepper, voucher input, summary, checkout CTA. |
| `src/main/resources/static/css/cart.css`                                   | Cart page styles (reuses `--teal`/`--teal-deep` from home.css).         |
| `src/main/java/com/ecommerce/cnj70/interceptor/CartCountInterceptor.java`  | Header cart-count badge interceptor.                                    |

### Review

| File                                                                       | Purpose                                                                 |
|----------------------------------------------------------------------------|-------------------------------------------------------------------------|
| `src/main/java/com/ecommerce/cnj70/document/Review.java`                    | MongoDB `@Document(collection = "reviews")`.                            |
| `src/main/java/com/ecommerce/cnj70/repository/ReviewRepository.java`        | `findByProductId`, `findByUserId`, `findByProductIdAndUserId`, etc.     |
| `src/main/java/com/ecommerce/cnj70/service/ReviewService.java`              | Interface — CRUD + ownership + rating auto-update.                     |
| `src/main/java/com/ecommerce/cnj70/service/impl/ReviewServiceImpl.java`     | Validations, ownership, `updateProductRating()` after every mutation.   |
| `src/main/java/com/ecommerce/cnj70/controller/web/ReviewController.java`    | `/products/{id}/reviews`, `/reviews/{id}/edit|delete`, `/api/reviews/check`. |
| `src/main/java/com/ecommerce/cnj70/dto/request/ReviewReq.java`              | `@Min(1) @Max(5) rating`, `@NotBlank comment`.                          |
| `src/main/java/com/ecommerce/cnj70/dto/response/ReviewRes.java`             | Wire DTO for review list / my-reviews.                                  |
| `src/main/resources/templates/web/product-detail.html`                      | Review tab UI (rating input, form, list, edit/delete buttons).          |
| `src/main/resources/static/css/product-detail.css`                          | `.pdp-btn-submit-review`, `.pdp-review-*` styles.                       |
| `src/main/java/com/ecommerce/cnj70/controller/web/ProductController.java`   | Loads reviews + `hasReviewed` flag for product detail page.              |
| `src/main/java/com/ecommerce/cnj70/config/MongoConfig.java`                  | `@EnableMongoAuditing` for `@CreatedDate`.                              |

---

## 22. Current Task Status

| Task | Description                                            | Status     | Owner         | Notes                                                                 |
|------|--------------------------------------------------------|------------|---------------|-----------------------------------------------------------------------|
| #7   | Check stock                                            | DONE       | Order team    | Implemented in `OrderServiceImpl.createOrder`. Cart only clamps.      |
| #8   | Prepare checkout data                                  | DONE       | Cart team     | `Cart.items` already carries all required fields (see §8).            |
| #9   | Show Cart needing checkout                             | DONE       | Cart team     | Summary card in `cart.html` shows subtotal + shipping + CTA.          |
| #10  | Show Product info in Cart                              | DONE       | Cart team     | `cart.html` thumbnail + name + meta.                                  |
| #11  | Show quantity                                          | DONE       | Cart team     | `.qty-stepper` in `cart.html` + `CartServiceImpl.addToCart`.          |
| #12  | Show total                                             | DONE       | Cart team     | `CartService.calculateTotal` + summary card.                         |
| #13  | Send checkout data to Order                            | DONE       | Order team    | `OrderController` POST `/checkout` → `OrderService.createOrder`.      |
| #14  | Remove Product from Cart after success                 | DONE       | Order team    | `cartService.clearCart(userId)` inside `OrderServiceImpl.createOrder`.|
| #15  | Refresh Cart after checkout                            | DONE       | Order team    | `clearCart` + next `getCartByUserId` rebuilds the document.           |
| #16  | Let Customer write Review                              | DONE       | Review team   | Form in `product-detail.html`.                                        |
| #17  | Save Review                                            | DONE       | Review team   | `ReviewServiceImpl.createReview` writes to MongoDB `reviews`.         |
| #18  | Display Review                                         | DONE       | Review team   | `ProductController.productDetail` + review tab in template.           |
| #19  | Let Customer delete Review                             | DONE       | Review team   | Owner-only; `deleteReview` updates Product rating.                    |
| #20  | Let Customer edit Review                               | DONE       | Review team   | Owner-only; `updateReview` updates Product rating.                    |
| #21  | Verify Customer purchased Product before Review        | BLOCKED    | Order team    | Requires `OrderService.hasUserPurchasedProduct(userId, productId)`.   |

---

## 23. Remaining Dependencies

| Dependency                                 | Direction         | Owner       | Status     |
|--------------------------------------------|-------------------|-------------|------------|
| `OrderService.hasUserPurchasedProduct`     | Review → Order    | Order team  | NOT STARTED |
| Voucher discount applied at order time     | Order → Voucher   | Voucher team| PARTIAL (Cart preview only) |
| Stock clamp on add (already done)          | Cart → Product    | Cart team   | DONE        |
| Cart → Checkout data handover              | Order reads Cart  | Order team  | DONE        |

---

## 24. Testing Checklist

### Cart

- [ ] Display Cart when authenticated
- [ ] Display empty-state when cart is empty
- [ ] Add Product (new item)
- [ ] Add Product (existing item — increments quantity)
- [ ] Increase quantity via stepper
- [ ] Decrease quantity via stepper
- [ ] Decrease to 0 → item is removed
- [ ] Remove Product manually
- [ ] Quantity cannot exceed `Product.stock`
- [ ] Quantity cannot be < 1
- [ ] Subtotal recomputed after quantity change
- [ ] Total recomputed server-side
- [ ] Voucher apply (happy path)
- [ ] Voucher apply (invalid code) shows error
- [ ] Voucher remove resets final total
- [ ] Cart header count badge updates after add/remove
- [ ] Logout / unauthenticated user redirected to `/auth/login`

### Review

- [ ] Create Review (logged-in user, rating 1-5, non-blank comment)
- [ ] Save Review to MongoDB `reviews`
- [ ] Display Review list on product detail
- [ ] Edit own Review
- [ ] Cannot edit another user's Review (BadRequestException)
- [ ] Delete own Review
- [ ] Cannot delete another user's Review
- [ ] Invalid rating (0 or 6) rejected
- [ ] Empty / whitespace comment rejected
- [ ] Invalid Product rejected
- [ ] Duplicate Review (same user, same product) rejected
- [ ] `Product.rating` and `Product.reviewCount` recomputed after create / edit / delete
- [ ] (Future) `hasUserPurchasedProduct` enforced before create

---

## 25. How Future Developers Should Continue

### Prompt Template For Future Developers

```
Read docs/CART_REVIEW_INTEGRATION.md completely before coding.

I am responsible for Task #XX (or feature YYY).

Follow the architecture and ownership boundaries documented in the file.
Specifically:
  - Cart owns CartRepository, CartService(Impl), CartController, cart.html, cart.css.
  - Review owns ReviewRepository, ReviewService(Impl), ReviewController,
    product-detail.html (review section), ReviewReq/Res DTOs.
  - Do NOT edit OrderService, OrderController, VoucherService,
    ProductService, AuthService, or ShopService unless explicitly required
    by your task AND documented in this file.

First, inspect the existing implementation. Read:
  - CartController + CartServiceImpl
  - ReviewController + ReviewServiceImpl
  - Cart and Review MongoDB documents
  - The current templates (cart.html, product-detail.html)

Implement only Task #XX. Reuse existing services/APIs.
Do not duplicate business logic. Do not invent new fields if the document
already has what you need.

Do not commit. Do not push.

When done:
  - List changed files (full paths).
  - Show the key changes (function-level diff summary).
  - Run the build (mvn compile / mvn test).
  - Update this file (CART_REVIEW_INTEGRATION.md):
      * §22 Task Status table if a task status changed.
      * §23 Remaining Dependencies if you added or resolved a dependency.
  - Report remaining dependencies / blockers.
```

### Common pitfalls to avoid

1. **Trusting client-supplied price / total** — always recompute on server.
2. **Mutating `Product.stock` from Cart** — only `OrderServiceImpl.createOrder`
   decreases stock; `cancelOrder` / `updateOrderStatus(CANCELLED)` restore it.
3. **Adding review ownership checks via client-side flag** — ownership is
   always `Review.userId.equals(currentUserId)` server-side.
4. **Modifying another team's controller/service** — open an integration
   contract in this file instead.
5. **Bypassing `cartService.clearCart(userId)`** — Order team relies on it.

---

## Appendix A — Quick reference: method signatures

```java
// CartService
Cart getCartByUserId(String userId);
Cart addToCart(String userId, String productId, int quantity);
Cart updateCartItem(String userId, String productId, int quantity);
Cart removeFromCart(String userId, String productId);
void clearCart(String userId);
int countItems(String userId);
BigDecimal calculateTotal(Cart cart);

// ReviewService
Review createReview(String userId, String productId, int rating, String comment);
Review updateReview(String reviewId, String userId, int rating, String comment);
void deleteReview(String reviewId, String userId);
Review getReviewById(String reviewId);
List<Review> getReviewsByProductId(String productId);
List<Review> getReviewsByUserId(String userId);
boolean hasUserReviewedProduct(String userId, String productId);
double getAverageRatingByProductId(String productId);
int getReviewCountByProductId(String productId);
```

```java
// Cross-module (read-only references from Cart / Review)
Product productRepository.findById(productId)
User    userRepository.findById(userId)
Voucher voucherService.validateForCheckout(code, shopId, productId)
Cart    cartRepository.findByUserId(userId)            // read by OrderService
```