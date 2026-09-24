# PHASE: AUDIT PROJECT + GAP ANALYSIS + IMPLEMENTATION ROADMAP

> **Mục tiêu:** Đối chiếu project `Ecommerce_Java` hiện tại với
> `SHOPEE_MARKETPLACE_MODEL.md`, xác định rõ **đang có gì -- thiếu gì --
> sai gì -- chưa hoàn chỉnh gì -- cần sửa gì -- sửa theo thứ tự nào**,
> trước khi thay đổi source code.
>
> **Nguyên tắc:** File này là cầu nối giữa **Business Baseline** và
> **Code Implementation**. Không được dùng file này để tự ý sửa code khi
> business decision chưa được chốt.

> **Phiên bản:** Phase Audit #1 (snapshot ngày 14/09/2026, branch
> `feature/admin`). Toàn bộ finding dựa trên đọc trực tiếp source
> trong `src/main/java/**` và `src/main/resources/templates/**`. Không
> suy luận từ snapshot dữ liệu MongoDB.

------------------------------------------------------------------------

## 0. PHASE POSITION

Luồng chính thức:

``` text
SHOPEE_MARKETPLACE_MODEL.md
        │
        ▼
CURRENT PROJECT AUDIT
        │
        ▼
   GAP ANALYSIS
        │
        ├── MISSING
        ├── WRONG
        ├── INCOMPLETE
        ├── RISK
        └── OK
        │
        ▼
IMPLEMENTATION PRIORITY
        │
        ▼
 PHASE 1 → PHASE 2 → ...
        │
        ▼
   CODE CHANGES
        │
        ▼
 TEST / REGRESSION
        │
        ▼
    FINAL AUDIT
```

### Không được làm ngược

``` text
KHÔNG:
Shopee Model
 ↓
Thấy thiếu Shop
 ↓
Tự sửa Shop.java
```

Mà phải:

``` text
Shopee Model
 ↓
Audit source hiện tại
 ↓
Xác định gap
 ↓
Xác định ảnh hưởng
 ↓
Chốt business decision nếu cần
 ↓
Lập implementation phase
 ↓
Mới sửa code
```

------------------------------------------------------------------------

# 1. SOURCE OF TRUTH

## 1.1. Business baseline

`SHOPEE_MARKETPLACE_MODEL.md` là nguồn tham chiếu nghiệp vụ chính.

Các nguyên tắc quan trọng:

-   Marketplace là **multi-vendor**.
-   Có 3 actor chính:
    -   `ADMIN / PLATFORM`
    -   `VENDOR / SELLER`
    -   `CUSTOMER`
-   Phải phân biệt:
    -   User
    -   Seller/Vendor
    -   Shop
    -   Product
-   **Không thêm `SUPPLIER`** (đã verify: enum `UserRole` chỉ có
    `ADMIN, VENDOR, CUSTOMER`).
-   Product thuộc Shop/Seller.
-   Category là dữ liệu platform-managed.
-   Multi-seller checkout phải được thiết kế có chủ đích.
-   Customer Payment không đồng nghĩa Vendor Sales.
-   Vendor Sales không đồng nghĩa Platform Revenue.
-   Platform Fee/Commission phải tách khỏi GMV.
-   Vendor Payout/Settlement là lớp tài chính riêng.
-   Platform Voucher và Seller Voucher phải được phân biệt.
-   Return/Refund/Dispute không được bỏ qua.
-   Seller Violation và Product Violation là hai khái niệm khác nhau.
-   Admin là Platform Operator, không phải chủ của mọi Shop.
-   Không tự đặt Commission Rate nếu chưa có business decision.

## 1.2. Project target

Project hiện tại được đánh giá theo mục tiêu đã xác minh từ
`pom.xml` và `application.yml`:

``` text
Spring Boot 3.2.0
Java 17
Spring Security + JWT (jjwt 0.12.3)
Spring Data MongoDB (MongoDB Atlas — production)
Thymeleaf + thymeleaf-extras-springsecurity6
Lombok
Jakarta Validation
spring-boot-devTools (runtime, optional)
Server port 8081
Mongo URI loaded from .env / MONGODB_URI env var
```

Source structure verified (snapshot 14/09/2026):

``` text
src/main/java/com/ecommerce/cnj70/
├── Cnj70EcommerceApplication.java
├── config/        (AdminBootstrap, SecurityConfig, MongoConfig, ...)
├── controller/
│   ├── admin/     (8 controllers)
│   ├── api/       (AuthApiController)
│   ├── auth/      (AuthController)
│   ├── vendor/    (5 controllers)
│   ├── web/       (Home, Shop, Product, Cart, Order, Review controllers)
│   └── VoucherController.java (mixed public + vendor + admin)
├── document/      (User, Shop, Product, Order, Cart, Voucher, Review,
│                    Category, Banner, FlashSaleStat, ProductSpecification,
│                    ProductVariant)
├── dto/request/   (CheckoutReq, LoginReq, RegisterReq, ProductFormReq,
│                    ReviewReq, ShopFormReq, VoucherFormReq)
├── dto/response/  (AdminDashboardRes, AdminOrderRes, AuthResponse,
│                    OrderHistoryRes, ReviewRes, VendorDashboardRes,
│                    VendorOrderRes, VendorProfileRes)
├── enums/         (AccountStatus, BannerStatus, DiscountType, OrderStatus,
│                    PaymentMethod, ProductStatus, ShopStatus, UserRole,
│                    VoucherType)
├── exception/     (BadRequest, Business, ResourceNotFound, Unauthorized,
│                    GlobalExceptionHandler)
├── interceptor/   (CartCountInterceptor)
├── repository/    (UserRepository, ShopRepository, ProductRepository,
│                    OrderRepository, CartRepository, VoucherRepository,
│                    ReviewRepository, CategoryRepository, BannerRepository,
│                    VendorRepository)
├── security/      (JwtUtils, JwtUserDetailsService, JwtAuthenticationFilter,
│                    JwtAuthenticationEntryPoint, CustomUserDetails,
│                    CustomUserDetailsService)
├── service/       (interfaces — Auth, Cart, Order, Product, Review, Vendor,
│                    Voucher, Admin*)
└── service/impl/  (tất cả implementation)
```

> **VERIFY:** Branch hiện tại là `feature/admin` (theo git status).
> Một số phần chưa từng được chạy runtime trên branch này được đánh dấu
> `VERIFY` thay vì `CONFIRMED`.

------------------------------------------------------------------------

# 2. AUDIT SCOPE

Audit bao phủ:

## 2.1. Backend

-   Entity/Document (MongoDB)
-   DTO (request + response)
-   Enum
-   Repository
-   Service interface + implementation
-   Controller (admin / vendor / web / api / auth)
-   Security (SecurityConfig, JWT, CustomUserDetails)
-   Authentication (login/register/JWT)
-   Authorization (URL-level + service-level ownership)
-   Exception handling (GlobalExceptionHandler)
-   Validation (Bean Validation, manual checks)
-   File upload (StorageService)
-   State transition (Order status, Shop status, Product status)
-   Business calculation (revenue, dashboard, voucher discount)

## 2.2. Frontend (Thymeleaf)

-   Customer pages: `web/*` (index, products, product-detail, shop, cart,
    checkout, order-history)
-   Vendor pages: `vendor/*` (dashboard, shop-create/pending/profile,
    product-list/form, order-list/detail, voucher-list/create/edit, profile)
-   Admin pages: `admin/*` (dashboard, user-list/detail, shop-list/detail,
    category-manage, product-list/detail, order-list/detail, voucher-…,
    review-list/detail, banner-list/form/detail)
-   Layouts: `layout/{web,vendor,admin,dashboard}-layout.html`
-   Fragments: `fragments/{web-header,web-footer,dashboard-sidebar,
    dashboard-topbar,admin-sidebar,admin-topbar}.html`

## 2.3. Data

-   9 collections verified trong `@Document(...)`: `users, shops,
    products, orders, carts, vouchers, reviews, categories, banners`.
-   `flash_sale_stats` cũng tồn tại nhưng không có service/controller
    tham chiếu → **OUT_OF_SCOPE** cho Phase audit này.
-   Snapshot fields: đã verify Order items giữ `productName, price,
    quantity, subtotal, shopId` (BR-10 cơ bản có).
-   Không thấy seed/test data fixture trong source (`VERIFY` với DB
    thực tế).

## 2.4. Tests

-   `src/test/` **KHÔNG tồn tại** (verified: shell `Get-ChildItem -Recurse src\test` trả empty, `Glob "src/test/**/*"` trả 0 file).
-   `spring-boot-starter-test` và `spring-security-test` đã khai báo trong `pom.xml` nhưng không có file test nào được viết.
-   **Status:** `MISSING/P0` — không có test nào.

------------------------------------------------------------------------

# 3. AUDIT CLASSIFICATION

Mọi finding phải dùng một trong các loại sau.

| Status | Ý nghĩa | Hành động |
|---|---|---|
| `OK` | Đã phù hợp | Giữ |
| `MISSING` | Chưa có | Cần implement |
| `WRONG` | Có nhưng sai business/architecture | Cần sửa |
| `INCOMPLETE` | Có nhưng chưa đủ | Cần bổ sung |
| `RISK` | Có nguy cơ gây lỗi/khó mở rộng | Cần review |
| `BLOCKED` | Chưa thể quyết định vì thiếu business decision | Chưa code |
| `VERIFY` | Cần kiểm tra source/runtime thực tế | Không kết luận vội |
| `OUT_OF_SCOPE` | Không nằm trong phạm vi BTL | Không implement |

------------------------------------------------------------------------

# 4. CONFIDENCE LEVEL

| Level | Ý nghĩa |
|---|---|
| `HIGH` | Có bằng chứng trực tiếp từ source/data/config |
| `MEDIUM` | Có bằng chứng từ snapshot hoặc hành vi hiện tại nhưng cần kiểm tra source |
| `LOW` | Chỉ là giả định cần verify |
| `BUSINESS` | Cần nhóm/giảng viên quyết định |

------------------------------------------------------------------------

# 5. CURRENT PROJECT EVIDENCE (HIGH CONFIDENCE — đọc trực tiếp source)

## 5.1. User / Role

Verified từ `document/User.java` + `enums/UserRole.java`:

``` text
ROLE_ADMIN
ROLE_VENDOR
ROLE_CUSTOMER
```

User document có các field:

``` text
id, email, password, fullName, phone, address,
role, status, shopId, avatarUrl,
createdAt, updatedAt
```

`status` là `AccountStatus` enum: `ACTIVE, LOCKED, UNVERIFIED`.

`AuthServiceImpl.register()` đã chặn tạo `ADMIN` qua public
registration — ghi rõ trong comment: "TASK 1.5 — Public registration
MUST NOT create ADMIN."

### Findings — User / Role

| ID | Status | Confidence | Mô tả |
|---|---|---|---|
| USER-01 | `OK` | HIGH | 3 role đúng baseline (BR-01). Không có SUPPLIER. |
| USER-02 | `OK` | HIGH | `AccountStatus` enum rõ ràng (`ACTIVE / LOCKED / UNVERIFIED`). |
| USER-03 | `OK` | HIGH | `AuthServiceImpl.login` đã chặn `!ACTIVE` (TASK 1.6). |
| USER-04 | `OK` | HIGH | `AdminUserServiceImpl.lockUser` chặn admin tự khóa chính mình. |
| USER-05 | `INCOMPLETE` | HIGH | Không phân biệt rõ "Account Verification" và "Seller Verification / KYC" — đều được xử lý gián tiếp qua `AccountStatus` (chưa có `ShopStatus` riêng cho KYC — chỉ PENDING/APPROVED/REJECTED). |
| USER-06 | `RISK` | HIGH | `User.shopId` được dùng làm "nguồn dữ liệu duy nhất" cho quan hệ Vendor → Shop. `GlobalControllerAdvice.addVendorAttributes` và `VendorServiceImpl.getShopIdFromUser` đều dựa vào `user.shopId`. Nếu project muốn 1 Vendor → N Shop, mô hình này phải đổi. |
| USER-07 | `MISSING` | HIGH | Không có cơ chế đăng ký Seller riêng (KYC form, application form, approval flow). Vendor hiện nay chỉ là `User.role = VENDOR` tạo thẳng từ register (không qua flow). |
| USER-08 | `INCOMPLETE` | HIGH | `AuthServiceImpl.register` tạo `status = ACTIVE` ngay (không qua UNVERIFIED → ACTIVE) — bypass flow "verify email" mà tài liệu gợi ý. Cần `VERIFY` nghiệp vụ. |
| USER-09 | `WRONG/RISK` | HIGH | `GlobalControllerAdvice` reference `hasShop`, `availableVoucherCount` (chưa đọc nhưng được grep ra) — cho thấy service-level dựa vào `user.shopId` thay vì query Shop trực tiếp. |

## 5.2. Shop

Verified từ `document/Shop.java` + `enums/ShopStatus.java`:

``` text
collection = "shops"
fields: id, ownerId, shopName (@Indexed unique),
        description, logoUrl, bannerUrl,
        status (ShopStatus), active (boolean),
        createdAt, updatedAt
```

`ShopStatus` enum: `PENDING, APPROVED, REJECTED`.

`Shop` còn có helper methods `isVerified()` (= APPROVED),
`getStatus()` fallback PENDING, `setVerified(boolean)`.

### Findings — Shop

| ID | Status | Confidence | Mô tả |
|---|---|---|---|
| SHOP-01 | `OK` | HIGH | Shop entity tồn tại, unique trên `shopName`, có `ownerId`. |
| SHOP-02 | `INCOMPLETE` | HIGH | `ShopStatus` chỉ có `PENDING/APPROVED/REJECTED`. Thiếu `ACTIVE`, `SUSPENDED`, `LOCKED` theo state model Shopee §22. Tham chiếu BR-02. |
| SHOP-03 | `OK` | HIGH | `Shop.active` (boolean) bổ sung cho `ShopStatus` để có toggle "đang hoạt động / tạm ngưng". |
| SHOP-04 | `OK` | HIGH | `AdminShopServiceImpl` đã có `approveShop / rejectShop / activateShop / deactivateShop`. |
| SHOP-05 | `INCOMPLETE` | HIGH | Chưa có state transition matrix rõ ràng (vd: PENDING → APPROVED → ACTIVE; APPROVED → SUSPENDED). Hiện tại `active=true/false` độc lập với `status` — `activateShop` chỉ set boolean, không đụng `status`. |
| SHOP-06 | `RISK` | HIGH | `Shop.setVerified(boolean)` chỉ toggle `APPROVED/PENDING`. Không ghi nhận `SUSPENDED`. |
| SHOP-07 | `OK` | HIGH | `VendorServiceImpl.validateShopOwnership` enforce Vendor chỉ thao tác Shop của mình (dựa trên `user.shopId == shopId`). |
| SHOP-08 | `MISSING` | HIGH | Không có KYC field (`legalName, taxCode, businessAddress, contactPhone`, ...) trong Shop entity. Cần thiết cho multi-vendor thực sự. |
| SHOP-09 | `MISSING` | HIGH | `Shop.ownerId` chỉ là String. `ShopRepository.findByOwnerId` chỉ trả Optional. Nếu 1 Vendor → N Shop thì phải đổi sang `List<Shop> findByOwnerId`. |
| SHOP-10 | `WRONG` | HIGH | `AdminShopServiceImpl.approveShop` không kiểm tra điều kiện đầu vào (Vendor có KYC chưa?). Nút approve chỉ đổi status; Vendor có thể tạo Shop bao nhiêu lần tùy ý (vì `VendorServiceImpl.createShop` chỉ check "vendor chưa có shop", `existsByShopName`). |

## 5.3. Product

Verified từ `document/Product.java` + `enums/ProductStatus.java`:

``` text
collection = "products"
fields: id, shopId, shopName,
        name (@Indexed), brand, warrantyMonths, manufacturer,
        manufacturerAddress, description, richDescription,
        price (BigDecimal), stock (int),
        categoryId, categoryName,
        imageUrls (List<String>), thumbnailUrl,
        specifications (List<ProductSpecification>),
        variants (List<ProductVariant>),
        status (ProductStatus), rating, reviewCount, sold,
        createdAt, updatedAt
```

`ProductStatus` enum: `DRAFT, ACTIVE, OUT_OF_STOCK, HIDDEN`.

### Findings — Product

| ID | Status | Confidence | Mô tả |
|---|---|---|---|
| PROD-01 | `OK` | HIGH | Product có Category (`categoryId, categoryName`). |
| PROD-02 | `INCOMPLETE` | HIGH | `ProductStatus` thiếu `PENDING_APPROVAL` và `REJECTED` (theo BR-05). Hiện chỉ có `DRAFT/ACTIVE/OUT_OF_STOCK/HIDDEN` — không có moderation gate rõ ràng. |
| PROD-03 | `OK` | HIGH | Product có `shopId` + `shopName` (snapshot field cho display). BR-03 cơ bản đạt vì ownership đi qua Shop. |
| PROD-04 | `OK` | HIGH | `ProductServiceImpl.createProduct` set `shopId = shopId, shopName = shopName` từ caller (VendorController). |
| PROD-05 | `OK` | HIGH | `VendorProductController.editProduct/deleteProduct` gọi `vendorService.validateProductOwnership` trước khi thao tác → BR-03 enforcement. |
| PROD-06 | `OK` | HIGH | `AdminProductServiceImpl.hideProduct/unhideProduct` đổi ACTIVE ↔ HIDDEN (Admin moderation). |
| PROD-07 | `INCOMPLETE` | HIGH | Không có workflow "Submit → Pending_Approval → Approved/Rejected". Vendor có thể set status trực tiếp từ `ProductFormReq.status` (xem `ProductServiceImpl.createProduct`: `.status(request.getStatus() != null ? request.getStatus() : ProductStatus.ACTIVE)`). |
| PROD-08 | `RISK` | HIGH | `AdminProductServiceImpl.deleteProduct` xóa cứng Product (theo `LOCK 7` comment). Order cũ vẫn giữ `productId` dạng String — OK. Tuy nhiên Review có `productId` FK → có thể thành orphan reference. Cần `VERIFY` runtime. |
| PROD-09 | `RISK` | HIGH | `ProductServiceImpl.createProduct` không validate `categoryId != null` (optional) → có thể tạo Product không có Category. |
| PROD-10 | `OK` | HIGH | `SoldCountSeeder` (ngoài scope) tự sinh số "Đã bán" ngẫu nhiên 1 lần (deterministic seed = 42L), idempotent — chỉ chạy 1 lần trong JVM lifetime. Đây là **fixture**, không phải business logic thực. |
| PROD-11 | `OK` | HIGH | `Product` có `specifications, variants, manufacturer` (rich description cho multi-shop marketplace). Đạt chuẩn data model cho product phức tạp. |

## 5.4. Category

Verified từ `document/Category.java` + `enums` không có Category enum:

``` text
collection = "categories"
fields: id, name (@Indexed unique), description,
        iconUrl, parentId, sortOrder, active (boolean),
        createdAt, updatedAt
```

### Findings — Category

| ID | Status | Confidence | Mô tả |
|---|---|---|---|
| CAT-01 | `OK` | HIGH | Category là document riêng, unique trên `name`. |
| CAT-02 | `OK` | HIGH | `AdminCategoryServiceImpl.createCategory/updateCategory/deleteCategory` đã có. |
| CAT-03 | `OK` | HIGH | `AdminCategoryServiceImpl.deleteCategory` chặn xóa nếu đang có Product sử dụng. |
| CAT-04 | `OK` | HIGH | `Category.active` (boolean) — Admin có thể ẩn Category. |
| CAT-05 | `RISK` | MEDIUM | `VendorProductController.createProductForm` lấy Category từ `findByActiveTrueOrderBySortOrderAsc` — Category inactive bị ẩn khỏi form. Tuy nhiên Product cũ đang reference `categoryId` của Category inactive sẽ hiển thị Product có Category "mồ côi". Cần `VERIFY` UI. |
| CAT-06 | `OK` | HIGH | Vendor không có endpoint tạo Category — đúng baseline (Category platform-managed). |

## 5.5. Cart

Verified từ `document/Cart.java`:

``` text
collection = "carts"
fields: id, userId (@Indexed unique), updatedAt,
        items: [ { productId, productName, imageUrl, price, quantity,
                    subtotal, shopId, shopName, stock } ]
```

### Findings — Cart

| ID | Status | Confidence | Mô tả |
|---|---|---|---|
| CART-01 | `OK` | HIGH | Cart có `shopId, shopName` ở từng item → đã có nền tảng multi-vendor. |
| CART-02 | `INCOMPLETE` | HIGH | `CartServiceImpl.addToCart/updateCartItem` không validate Product còn `ACTIVE` hoặc Shop còn active. Có thể add Product `HIDDEN` vào cart. |
| CART-03 | `OK` | HIGH | `CartServiceImpl.addToCart` không trừ stock, không reserve — chỉ lưu qty. Stock chỉ giảm khi `OrderServiceImpl.createOrder`. |
| CART-04 | `RISK` | HIGH | Không có concurrency control khi nhiều user add cùng Product cuối cùng. Stock race condition vẫn có thể xảy ra (cần xử lý trong `OrderService.createOrder` qua `MongoTemplate.findAndModify` hoặc transaction — hiện KHÔNG có). |
| CART-05 | `OK` | HIGH | `CartServiceImpl.calculateTotal` chỉ sum các `subtotal` (BigDecimal), an toàn. |
| CART-06 | `MISSING` | HIGH | Cart không có `version`/audit field. Update race có thể ghi đè (MongoDB không có row-level lock). |

## 5.6. Order

Verified từ `document/Order.java`:

``` text
collection = "orders"
fields: id, userId (@Indexed), userName, userEmail, userPhone,
        shippingAddress,
        items: [ { shopId, productId, productName, imageUrl, price,
                    quantity, subtotal } ],
        subtotal, shippingFee, totalAmount,
        status (OrderStatus), paymentMethod (COD/VNPAY),
        paid (boolean),
        shopId, shopName,        // ← top-level, KHÔNG được dùng trong service
        createdAt, updatedAt, deliveredAt
```

`OrderStatus` enum: `PENDING, PREPARING, SHIPPING, DELIVERED, CANCELLED`.

### Findings — Order

| ID | Status | Confidence | Mô tả |
|---|---|---|---|
| ORDER-01 | `OK` | HIGH | Order có `items` với `shopId` snapshot — nền tảng multi-vendor có. |
| ORDER-02 | `OK` | HIGH | `OrderServiceImpl.getOrdersByShopId` filter bằng cách duyệt all orders và check `item.shopId` (đúng về logic, không hiệu quả nhưng đúng). |
| ORDER-03 | `INCOMPLETE` | HIGH | `Order.shopId/shopName` (top-level) **không được dùng** trong service. Đây là field mồ côi, có thể gây hiểu nhầm. Cần `VERIFY` xem có query nào filter theo `Order.shopId` không (xem ORDER-04). |
| ORDER-04 | `INCOMPLETE` | HIGH | `OrderRepository.findByShopId` tồn tại nhưng **không ai gọi** (chỉ `OrderServiceImpl.getOrdersByShopId` dùng filter in-memory thay vì query). Có nguy cơ sai khi scale. |
| ORDER-05 | `MISSING` | HIGH | **KHÔNG có SubOrder / Master Order.** Một Order hiện nay chứa items từ nhiều Shop trong cùng document → nhưng xử lý Order vẫn là single-status toàn Order. |
| ORDER-06 | `WRONG` (RISK) | HIGH | `OrderServiceImpl.createOrder` tạo **1 Order duy nhất** cho cả Cart. Khi Customer có items từ 2 Shop trở lên, cả 2 Shop sẽ chia sẻ 1 status. Vendor A và Vendor B không thể xử lý độc lập. Đây là **WRONG theo BR-06**. |
| ORDER-07 | `INCOMPLETE` | HIGH | `OrderServiceImpl.validateStatusTransition` chỉ cho phép: PENDING → {PREPARING, CANCELLED}; PREPARING → {SHIPPING, CANCELLED}; SHIPPING → {DELIVERED, CANCELLED}. Không có transition DELIVERED → REFUND/RETURN (xem ORDER-12). |
| ORDER-08 | `OK` | HIGH | `OrderServiceImpl.createOrder` validate stock trước khi giảm (có throw `BadRequestException` nếu không đủ). |
| ORDER-09 | `RISK` | HIGH | `OrderServiceImpl.createOrder` dùng `productRepository.save(product)` sau khi giảm stock — KHÔNG atomic, KHÔNG có `findAndModify`/version check. Race condition vẫn có thể xảy ra khi 2 user mua cùng lúc sản phẩm cuối. |
| ORDER-10 | `OK` | HIGH | `OrderServiceImpl.cancelOrder/restoreStock` — khi CANCELLED, stock được cộng lại (logic đúng). |
| ORDER-11 | `RISK` | HIGH | `VendorOrderController.updateOrderStatus` set `status` trực tiếp lên Order mà KHÔNG qua `OrderServiceImpl.updateOrderStatus`. Có nghĩa là Vendor update bypass transition validation. |
| ORDER-12 | `MISSING` | HIGH | `OrderStatus` không có `REFUNDED` / `RETURNED` — không thể hiện được DELIVERED → REFUND/RETURN. |
| ORDER-13 | `INCOMPLETE` | HIGH | `VendorOrderController.orderDetail` map `Order → VendorOrderRes` chỉ lấy items thuộc Shop — nhưng cập nhật status trên cả Order (không phải trên Sub-Order). Nếu có Sub-Order, Vendor A đổi status sẽ ảnh hưởng Vendor B. Hiện tại: status là shared → cả 2 Vendor thấy cùng status. |
| ORDER-14 | `WRONG` | HIGH | **Template inconsistency**: `templates/vendor/order-list.html` và `templates/vendor/order-detail.html` sử dụng `OrderStatus.PROCESSING` và `OrderStatus.SHIPPED`, nhưng `OrderStatus` enum chỉ có `PREPARING` và `SHIPPING`. Filter dropdown và badge render sẽ **không bao giờ match** với status thực → filter rỗng. |
| ORDER-15 | `WRONG` | HIGH | `OrderRepository` không có method `findByItems_shopId` — Vendor service đang scan full table (`orderRepository.findAll()` rồi filter in-memory trong `VendorServiceImpl.getDashboardStats` và `VendorOrderController.orderList`). Performance kém nhưng không sai về authorization. |
| ORDER-16 | `INCOMPLETE` | HIGH | `paid` là boolean — không đủ phân biệt Payment lifecycle theo BR-07 / BR-08. |

## 5.7. Payment / Voucher / Settlement

Verified bằng grep trong toàn bộ source:

-   **Commission / commission:** 0 reference.
-   **Settlement / payout:** 0 reference.
-   **Refund / Return** (business term): 0 reference.
-   `paid` (boolean) là cách duy nhất để theo dõi "đã thanh toán".

### Findings — Payment / Finance

| ID | Status | Confidence | Mô tả |
|---|---|---|---|
| PAY-01 | `OK` | HIGH | `PaymentMethod` enum: `COD, VNPAY`. |
| PAY-02 | `INCOMPLETE` | HIGH | `paid` boolean — không phân biệt `Payment Status, Settlement Status, Payout Status, Refund Status` (BR-07). |
| PAY-03 | `MISSING` | HIGH | **Không có Payment entity** (chỉ có field `paid` + `paymentMethod` trên Order). |
| PAY-04 | `MISSING` | HIGH | **Không có Commission** — không thể tính Platform Fee. |
| PAY-05 | `MISSING` | HIGH | **Không có Settlement document** — không có Vendor Payout. |
| PAY-06 | `MISSING` | HIGH | **Không có Refund flow.** Admin/Vendor không thể xử lý Refund. |
| PAY-07 | `RISK` | HIGH | `AdminServiceImpl.getDashboardStatsByPeriod` tính `totalPlatformRevenue = sum(order.totalAmount WHERE status = DELIVERED)`. **WRONG theo baseline §13:** Đây là **GMV** (Vendor Sales), không phải Platform Revenue. |
| PAY-08 | `WRONG` | HIGH | `templates/admin/dashboard.html` hiển thị `stats.totalPlatformRevenue` với label "Doanh thu nền tảng" và "Tổng doanh thu từ các đơn đã giao thành công" — đây là **terminology sai** (BR-07). Số này thực chất là GMV (Vendor Sales sau khi giao), không phải Platform Revenue. |
| PAY-09 | `WRONG` | HIGH | `VendorServiceImpl.getDashboardStats.totalRevenue` cũng sum `Order.totalAmount WHERE status = DELIVERED` — đúng là Vendor Sales (doanh thu bán hàng của Shop đó), không phải "doanh thu sau Commission". |
| PAY-10 | `MISSING` | HIGH | Không có logic tách `Vendor Payout` (= Vendor Sales − Platform Fee − Voucher discount allocation). |

## 5.8. Voucher / Promotion

Verified từ `document/Voucher.java`:

``` text
collection = "vouchers"
fields: id, code (@Indexed unique), name,
        type (VoucherType: SHOP | WEB),
        shopId, shopName,          // chỉ cho SHOP
        productIds (List<String>), // cho WEB (filter sản phẩm)
        discountType (PERCENT | AMOUNT),
        discountValue (BigDecimal),
        maxDiscountAmount,
        minOrderValue,
        quantity, used,
        startDate, endDate,
        active (boolean),
        createdBy,
        createdAt, updatedAt
```

### Findings — Voucher

| ID | Status | Confidence | Mô tả |
|---|---|---|---|
| VCH-01 | `OK` | HIGH | Phân biệt `VoucherType.SHOP` vs `VoucherType.WEB` (Platform). |
| VCH-02 | `OK` | HIGH | `VoucherServiceImpl.createVoucher` (SHOP) chỉ set `type=SHOP, shopId=…`. |
| VCH-03 | `OK` | HIGH | `VoucherServiceImpl.createWebVoucher` (WEB) chỉ set `type=WEB, shopId=null, shopName=null`. |
| VCH-04 | `OK` | HIGH | `VoucherController.applyVoucher` (POST `/checkout/apply-voucher`) validate active/hạn/còn lượt/đúng shop/đúng product → đúng rule cơ bản. |
| VCH-05 | `OK` | HIGH | `VoucherServiceImpl.validateForCheckout` validate rule 1-5. |
| VCH-06 | `OK` | HIGH | `VoucherServiceImpl.incrementUsed` được gọi qua `VoucherController.placeOrderWithVoucher` (POST `/checkout/place-order`). |
| VCH-07 | `INCOMPLETE` | HIGH | **Voucher chưa được áp dụng vào Order thực tế.** `OrderServiceImpl.createOrder` KHÔNG đọc `voucherCode/voucherId` từ `CheckoutReq`. Discount không được trừ vào `subtotal/totalAmount` của Order. |
| VCH-08 | `MISSING` | HIGH | Không có discount allocation: `Customer Payable / Vendor Amount / Platform Cost` theo BR-07 / §15. |
| VCH-09 | `MISSING` | HIGH | Không có rule chia cost giữa Platform vs Seller cho voucher (cần BUSINESS DECISION — BD-06). |
| VCH-10 | `WRONG` | HIGH | **`VoucherController.placeOrderWithVoucher` (POST `/checkout/place-order`) là orphan route.** Chỉ `incrementUsed` cho voucher rồi redirect về `/web/orders`. KHÔNG tạo Order thật — endpoint này tạo ra "voucher đã dùng" nhưng không có đơn hàng tương ứng. |
| VCH-11 | `INCOMPLETE` | HIGH | Voucher SHOP chỉ validate `shopId == voucher.shopId`. Khi 1 Order có items từ 2 Shop và chỉ dùng 1 SHOP voucher — voucher hiện apply trên `orderTotal` của cả Order (không phải shopSubtotal). Sai về marketplace rule. |
| VCH-12 | `INCOMPLETE` | HIGH | Voucher quantity không giảm tự động khi Order bị Cancel — `VoucherController.placeOrderWithVoucher` đã `incrementUsed` nhưng nếu Order bị cancel sau đó, used không được hoàn lại (overflow cho phép). |

## 5.9. Shipping

Verified từ `document/Order.java`:

``` text
Order có: shippingAddress (String), shippingFee (BigDecimal, hardcode 15.000đ)
```

### Findings — Shipping

| ID | Status | Confidence | Mô tả |
|---|---|---|---|
| SHIP-01 | `INCOMPLETE` | HIGH | `shippingFee` hardcode 15.000đ trong `OrderServiceImpl.createOrder` — không theo logic khoảng cách / carrier. |
| SHIP-02 | `MISSING` | HIGH | Không có carrier/logistics, tracking code, shipment status. |
| SHIP-03 | `MISSING` | HIGH | Không có delivery failure handling, return shipping. |
| SHIP-04 | `INCOMPLETE` | HIGH | Multi-shop checkout không có shipping fee allocation giữa các Shop (toàn bộ 15k gán cho 1 Order). |
| SHIP-05 | `OK` | HIGH | `Order.shippingAddress` lưu được (Customer nhập khi checkout). |

## 5.10. Review

Verified từ `document/Review.java`:

``` text
collection = "reviews"
fields: id, productId (@Indexed), userId (@Indexed),
        userName, userAvatar, rating (int), comment,
        createdAt
```

### Findings — Review

| ID | Status | Confidence | Mô tả |
|---|---|---|---|
| REV-01 | `OK` | HIGH | Review có `productId` và `userId` (FK). |
| REV-02 | `WRONG` | HIGH | **`ReviewServiceImpl.createReview` KHÔNG kiểm tra Customer đã mua Product** (verified purchase). BR-09 cơ bản bị vi phạm — bất kỳ logged-in user nào cũng review được. |
| REV-03 | `INCOMPLETE` | HIGH | `ReviewServiceImpl.createReview` không validate Order đã `DELIVERED/COMPLETED`. |
| REV-04 | `INCOMPLETE` | HIGH | Không giới hạn "1 review / 1 user / 1 product" ngoài duplicate check (`findByProductIdAndUserId`). Nếu productId đã review rồi → throw. Nhưng KHÔNG check theo Order (một user có thể mua 2 lần và review 2 lần nếu logic check duplicate không đúng). |
| REV-05 | `OK` | HIGH | `ReviewServiceImpl.updateProductRating` cập nhật `Product.rating/reviewCount` khi review thay đổi — đúng về consistency. |
| REV-06 | `OK` | HIGH | `AdminReviewServiceImpl.deleteReview` hard delete (đã khóa side-effect theo LOCK 4). |
| REV-07 | `MISSING` | HIGH | Không có Shop review (chỉ Product review). |
| REV-08 | `MISSING` | HIGH | Không có Vendor reply cho Review. |
| REV-09 | `INCOMPLETE` | HIGH | Review chỉ có rating 1-5 integer (không có title, image, helpful vote). |

## 5.18. Banner / Promotion

Verified từ `document/Banner.java`:

``` text
collection = "banners"
fields: id, title, description, tag, tagIcon, imageUrl (@Indexed),
        link, ctaText, ctaIcon, theme,
        status (String: PUBLISHED/UNPUBLISHED — KHÔNG dùng enum),
        position (@Indexed), sortOrder,
        createdAt, updatedAt
```

### Findings — Banner

| ID | Status | Confidence | Mô tả |
|---|---|---|---|
| BANN-01 | `INCOMPLETE` | HIGH | `Banner.status` là **String** chứ không phải enum như các status khác trong project. Hiện tại chỉ 2 giá trị (`PUBLISHED/UNPUBLISHED`) nhưng không có type-safety. |
| BANN-02 | `MISSING` | HIGH | Banner không có `startAt/endAt` scheduling (đúng theo LOCK Phase 17 — chưa có scheduling). |
| BANN-03 | `INCOMPLETE` | HIGH | Banner priority algorithm không tồn tại — chỉ sort theo `sortOrder` thủ công (LOCK 5). |
| BANN-04 | `OUT_OF_SCOPE` | HIGH | Campaign/advertising entity — chưa có (OUT_OF_SCOPE cho BTL). |

---

## 5.19. Bổ sung Security findings (from `SecurityConfig.java`)

`SecurityConfig.java` đã đọc cụ thể:

``` text
.requestMatchers("/", "/auth/**", "/css/**", "/js/**", "/images/**", "/uploads/**", "/error").permitAll()
.requestMatchers("/api/auth/**").permitAll()
.requestMatchers("/api/products/**").permitAll()
.requestMatchers("/api/categories/**").permitAll()
.requestMatchers("/vouchers").permitAll()
.requestMatchers("/checkout/apply-voucher").permitAll()
.requestMatchers("/admin/**").hasRole("ADMIN")
.requestMatchers("/vendor/**").hasRole("VENDOR")
.requestMatchers("/api/**").authenticated()
.anyRequest().permitAll()
```

### Findings — Security (bổ sung)

| ID | Status | Confidence | Mô tả |
|---|---|---|---|
| SEC-11 | `OK` | HIGH | `@EnableMethodSecurity` được bật → có thể dùng `@PreAuthorize`, nhưng **CHƯA AI DÙNG** annotation này trong source. |
| SEC-12 | `RISK` | HIGH | `/api/products/**` permitAll — không có auth. Đúng vì Customer xem public — nhưng cần `VERIFY` xem có endpoint edit/delete qua `/api` không (hiện không thấy controller edit qua `/api`). |
| SEC-13 | `RISK` | HIGH | Route `/web/orders/**`, `/web/cart/**`, `/orders/{id}` không có match ở SecurityConfig → rơi vào `anyRequest().permitAll()`. Một số controller kiểm tra `if (user == null)` ở tầng Controller nhưng không nhất quán. |
| SEC-14 | `MISSING` | HIGH | Không có CSRF bảo vệ cho các form POST (đã `csrf.disable` cho API). Với web flow Thymeleaf có thể cần CSRF token. Hiện tại các form submit có thể bị 403 hoặc bypass tùy Spring Security version — cần `VERIFY`. |
| SEC-15 | `WRONG` | HIGH | `/admin/**` chỉ check `hasRole("ADMIN")` — KHÔNG chặn admin nếu `AccountStatus = LOCKED`. Admin khóa vẫn có thể truy cập `/admin/**` (URL check) nếu Spring Security không load `status` vào `UserDetails`. Cần `VERIFY` runtime. |

---

## 5.20. Address & Data

Verified từ `document/User.java` + `document/Order.java`:

``` text
User.address (String)
Order.shippingAddress (String)
```

### Findings — Address

| ID | Status | Confidence | Mô tả |
|---|---|---|---|
| ADDR-01 | `INCOMPLETE` | HIGH | Address chỉ là String — không có cấu trúc (street/ward/district/city). Cần thiết cho multi-vendor có shipping fee theo region. |
| ADDR-02 | `MISSING` | HIGH | Không có AddressBook entity riêng (Customer có thể có N địa chỉ). Hiện tại mỗi Order có 1 shippingAddress duy nhất, không lưu AddressBook. |
| ADDR-03 | `INCOMPLETE` | HIGH | "Pick-up address" cho Shop không tồn tại trong entity `Shop` (dù baseline §3.3 yêu cầu "địa chỉ lấy hàng"). |

Verified bằng grep trong source: 0 reference cho term "refund"/"return"
ngoài các tên method (vd `aggregateQuarters`).

### Findings — Return / Refund / Dispute

| ID | Status | Confidence | Mô tả |
|---|---|---|---|
| REFUND-01 | `MISSING` | HIGH | **Không có Return flow.** |
| REFUND-02 | `MISSING` | HIGH | **Không có Refund flow.** |
| REFUND-03 | `MISSING` | HIGH | **Không có Dispute flow.** |
| REFUND-04 | `MISSING` | HIGH | Không có entity `ReturnRefundRequest`, `Dispute`. |
| REFUND-05 | `INCOMPLETE` | HIGH | `OrderStatus` enum không có `REFUNDED`. |

## 5.12. Seller Violation / Product Violation

Verified: KHÔNG có entity `Violation`. Comment trong code chỉ dùng
từ "Violation" cho hành động delete của Admin (Product / Review):

-   `AdminProductServiceImpl.deleteProduct` comment: "TASK 14.11 —
    Delete Violation = hard delete theo Contract Phase 14."
-   `AdminReviewServiceImpl.deleteReview` comment: "TASK 15.13 —
    Delete Review Violation."

### Findings — Violation

| ID | Status | Confidence | Mô tả |
|---|---|---|---|
| VIOL-01 | `MISSING` | HIGH | **Không có Violation entity.** |
| VIOL-02 | `MISSING` | HIGH | Không có Seller Violation workflow (warning, penalty, restriction). |
| VIOL-03 | `MISSING` | HIGH | Không có Product Violation workflow (chỉ có "HIDDEN" status rời rạc). |
| VIOL-04 | `MISSING` | HIGH | Không có strike/score logic (đúng baseline — không tự tạo nếu chưa có business decision). |

## 5.13. Admin Dashboard

Verified từ `service/impl/AdminServiceImpl.java` +
`dto/response/AdminDashboardRes.java` +
`templates/admin/dashboard.html`:

``` text
metrics hiển thị:
  - totalUsers, totalShops, totalProducts, totalOrders
  - totalPlatformRevenue (sum order.totalAmount WHERE DELIVERED)
  - recentActivities (top 5 orders)
  - revenueTrend (DAY/WEEK/MONTH/QUARTER/YEAR/ALL)
```

### Findings — Admin Dashboard

| ID | Status | Confidence | Mô tả |
|---|---|---|---|
| ADMIN-01 | `OK` | HIGH | Có các KPI tổng quan (users, shops, products, orders). |
| ADMIN-02 | `WRONG` | HIGH | `totalPlatformRevenue` thực chất là **GMV / Vendor Sales** (sum order total). Sai terminology theo §13 / §25 baseline. |
| ADMIN-03 | `MISSING` | HIGH | Thiếu tách `GMV / Platform Revenue / Vendor Sales / Vendor Payable / Escrow / Refund`. |
| ADMIN-04 | `OK` | HIGH | Có period selector (DAY/WEEK/MONTH/QUARTER/YEAR/ALL). |
| ADMIN-05 | `INCOMPLETE` | HIGH | `AdminServiceImpl.aggregateAllTime` sum toàn bộ DELIVERED orders — performance kém khi data lớn. |
| ADMIN-06 | `INCOMPLETE` | HIGH | Admin chỉ có READ-only view Order (không có POST endpoint đổi status). Đúng theo ADMIN-01 (Admin = READ-ONLY / monitor). |

## 5.14. Security / RBAC

Verified từ `config/SecurityConfig.java`:

``` text
.requestMatchers("/admin/**").hasRole("ADMIN")
.requestMatchers("/vendor/**").hasRole("VENDOR")
.requestMatchers("/api/**").authenticated()
.anyRequest().permitAll()
```

`CustomUserDetailsService` implements `UserDetailsService`. Role lấy từ
`User.role` → authority `ROLE_<role>`.

`@EnableMethodSecurity` có bật → có thể dùng `@PreAuthorize`.

### Findings — Security / RBAC

| ID | Status | Confidence | Mô tả |
|---|---|---|---|
| SEC-01 | `OK` | HIGH | URL-level RBAC cho `/admin/**` và `/vendor/**` đúng. |
| SEC-02 | `INCOMPLETE` | HIGH | Không thấy `@PreAuthorize` trên Controller/Service → service-level ownership chỉ dựa vào manual check trong `VendorServiceImpl.validateShopOwnership/validateProductOwnership`. |
| SEC-03 | `OK` | HIGH | `VendorOrderController` manual check ownership: chỉ cho phép Vendor update Order có `item.shopId == shopId`. |
| SEC-04 | `OK` | HIGH | `VendorProductController.edit/delete` gọi `validateProductOwnership`. |
| SEC-05 | `RISK` | HIGH | `VendorOrderController.updateOrderStatus` check ownership bằng filter `item.shopId` — ĐÚNG cho việc kiểm tra "được xem", nhưng **SAI** cho việc update: status hiện nay shared trên Order, nên Vendor A có thể update status của Order có cả Vendor B. |
| SEC-06 | `OK` | HIGH | `AdminUserServiceImpl.lockUser` chặn admin tự khóa. |
| SEC-07 | `INCOMPLETE` | HIGH | `OrderController` (`/checkout`, `/orders`) chỉ check `user != null` — không check ownership của orderId trả về từ `getOrderById`. Nếu route `/orders/{id}` tồn tại, cần audit. |
| SEC-08 | `OK` | HIGH | `/api/cart/**` yêu cầu `authenticated()` — bảo vệ cross-user cart. |
| SEC-09 | `RISK` | HIGH | `/api/products/**` permitAll — không có auth. (Đúng vì Customer xem public.) Nhưng `/api/products/{id}/edit` (nếu có) cần `VERIFY`. Hiện không thấy controller cho product edit qua `/api`. |
| SEC-10 | `RISK` | HIGH | `SecurityConfig.anyRequest().permitAll()` → route không match các matcher ở trên (vd `/web/orders/{id}`, `/products/{id}`) **không bị ép auth**. Một số trang có check `user == null` ở controller (vd `/cart`, `/checkout`), nhưng không nhất quán. |

------------------------------------------------------------------------

# 6. CONFIRMED GAP MATRIX (HIGH CONFIDENCE)

Bảng tổng hợp dựa trên source audit (Phase Audit #1, 14/09/2026).

| Area | Status | Priority | Finding IDs |
|---|---|---|---|
| 3 Roles (ADMIN/VENDOR/CUSTOMER) | OK | P0 | USER-01 |
| User document | OK | P0 | USER |
| Account lock + login guard | OK | P0 | USER-03, USER-04 |
| Shop entity | OK | P0 | SHOP-01 |
| Shop active toggle | OK | P0 | SHOP-03, SHOP-04 |
| Shop lifecycle state machine (PENDING/APPROVED/SUSPENDED/LOCKED/ACTIVE) | INCOMPLETE | P0 | SHOP-02, SHOP-05, SHOP-06 |
| 1 Vendor → 1 Shop (current) vs N Shop (BD-05) | RISK | P1 | USER-06, SHOP-09 |
| Seller onboarding / KYC flow | MISSING | P0 | USER-07, SHOP-08 |
| Product entity + ownership | OK | P0 | PROD-01, PROD-03, PROD-04 |
| Product moderation workflow (PENDING_APPROVAL) | MISSING / INCOMPLETE | P0 | PROD-02, PROD-07 |
| Admin Product Hide/Unhide/Delete | OK | P0 | PROD-06 |
| Product violation workflow (separate from Product status) | MISSING | P1 | VIOL-03 |
| Category entity + Admin CRUD | OK | P1 | CAT-01..04 |
| Category platform-managed (no Vendor create) | OK | P1 | CAT-06 |
| Cart multi-shop | OK | P0 | CART-01 |
| Cart stock race / atomic checkout | RISK | P0 | CART-04, CART-06, ORDER-09 |
| Multi-vendor Checkout (single Order) | WRONG | P0 | ORDER-06 |
| Sub-Order / Master Order | MISSING | P0 | ORDER-05 |
| Order status enum incomplete (no REFUNDED) | INCOMPLETE | P0 | ORDER-07, ORDER-12 |
| Vendor-specific Order access (filter by shopId) | OK (manual) | P0 | SEC-03 |
| Vendor update Order bypasses transition validation | WRONG / RISK | P0 | ORDER-11 |
| Admin Order READ-ONLY | OK | P0 | ADMIN-06 |
| Template enum mismatch (PROCESSING/SHIPPED vs PREPARING/SHIPPING) | WRONG | P0 | ORDER-14 |
| Payment (COD/VNPAY) | OK | P0 | PAY-01 |
| Payment lifecycle (PENDING/PAID/REFUND/...) | MISSING / INCOMPLETE | P0 | PAY-02, PAY-03 |
| Platform Fee / Commission | MISSING / BLOCKED | P0 | PAY-04, FIN-03 |
| Settlement / Payout | MISSING | P0 | PAY-05, FIN-04 |
| Refund / Return / Dispute | MISSING | P0 | REFUND-01..04 |
| Historical transaction snapshot | OK (Order items) | P0 | (BR-10 cơ bản đạt) |
| Financial terminology (Admin Dashboard) | WRONG | P0 | PAY-07, PAY-08, ADMIN-02 |
| Voucher SHOP / WEB distinction | OK | P1 | VCH-01..03 |
| Voucher validation rules | OK | P1 | VCH-04, VCH-05 |
| Voucher thực sự áp dụng vào Order total | MISSING / WRONG | P1 | VCH-07 |
| Voucher discount allocation (Platform vs Seller cost) | MISSING / BLOCKED | P1 | VCH-08, VCH-09 |
| Multi-shop voucher allocation | WRONG | P1 | VCH-11 |
| Shipping (address, fee) | INCOMPLETE | P1 | SHIP-01, SHIP-05 |
| Carrier / tracking / delivery failure | MISSING | P2 | SHIP-02, SHIP-03 |
| Multi-shop shipping fee allocation | MISSING | P1 | SHIP-04 |
| Review create (no purchase check) | WRONG | P1 | REV-02 |
| Review eligibility theo Order | MISSING / INCOMPLETE | P1 | REV-03, REV-04 |
| Review chỉ có rating/comment | INCOMPLETE | P3 | REV-09 |
| Shop review | MISSING | P2 | REV-07 |
| Vendor reply review | MISSING | P2 | REV-08 |
| Seller Violation entity / workflow | MISSING | P1 | VIOL-01, VIOL-02 |
| Product Violation workflow | MISSING | P1 | VIOL-03 |
| RBAC URL-level | OK | P0 | SEC-01 |
| RBAC service-level (method security) | INCOMPLETE | P0 | SEC-02, SEC-11 |
| `/anyRequest().permitAll()` quá rộng | WRONG / RISK | P0 | SEC-10, SEC-13 |
| Admin LOCKED vẫn truy cập /admin | WRONG / RISK | P0 | SEC-15 |
| Cart count / ownership at interceptor | OK | P0 | CartCountInterceptor |
| Admin self-lock protection | OK | P0 | SEC-06 |
| Banner (status dùng String) | INCOMPLETE | P3 | BANN-01..03 |
| Address (User, Order đều là String thuần) | INCOMPLETE / MISSING | P1 | ADDR-01..03 |
| Tests (unit/integration/security) | MISSING | P0 | §2.4 |
| Voucher used không restore khi Order Cancel | RISK | P1 | VCH-12 |
| Shop approve không kiểm tra KYC | RISK | P0 | SHOP-10 |
| Auth register set ACTIVE ngay (bypass UNVERIFIED) | INCOMPLETE | P2 | USER-08 |

------------------------------------------------------------------------

# 7. PRIORITY DEFINITION (giữ nguyên từ baseline)

## P0 — BLOCKER

Không thể coi Marketplace implementation hoàn chỉnh nếu còn:

-   Multi-vendor order chưa rõ (`ORDER-05`, `ORDER-06`).
-   Seller ownership/security chưa rõ (`USER-06`, `SEC-02`, `SEC-05`).
-   Payment model chưa rõ (`PAY-02`, `PAY-03`).
-   Platform Fee/Commission chưa rõ (`PAY-04`, `FIN-03`).
-   Settlement/Payout chưa có nếu finance nằm trong scope (`PAY-05`,
    `FIN-04`).
-   Admin Revenue sai nghĩa (`ADMIN-02`, `PAY-07`, `PAY-08`).
-   Customer có thể truy cập Order người khác (`SEC-07`, `SEC-10`).
-   Vendor có thể truy cập Order Vendor khác (`ORDER-13`, `SEC-05`).
-   Vendor update Order bypass transition (`ORDER-11`).
-   Product ownership sai (`PROD-03`, `PROD-04` — đang OK).
-   Checkout multi-shop sai (`ORDER-06`).
-   Template OrderStatus mismatch (`ORDER-14` — runtime bug).

## P1 — CORE MARKETPLACE

-   Seller onboarding (`USER-07`, `SHOP-08`).
-   Shop lifecycle (`SHOP-02`, `SHOP-05`, `SHOP-06`).
-   Product moderation workflow (`PROD-02`, `PROD-07`).
-   Product violation (`VIOL-03`).
-   Voucher funding / allocation (`VCH-07`, `VCH-08`, `VCH-09`,
    `VCH-11`).
-   Shipping (`SHIP-*`).
-   Review eligibility (`REV-02..04`).
-   Return/Refund (`REFUND-01..04`).
-   Seller violation (`VIOL-01`, `VIOL-02`).
-   Dashboard metrics (`ADMIN-03`).

## P2 — ENHANCEMENT

-   Advanced tracking (`SHIP-02`, `SHIP-03`).
-   Advanced dispute (`REFUND-03`).
-   Risk/Fraud (chưa có — OUT_OF_SCOPE cho Phase audit này).
-   Notification (chưa có — OUT_OF_SCOPE).
-   Analytics nâng cao (`ADMIN-05`).
-   Performance optimization (`ADMIN-05`, `ORDER-15`).

------------------------------------------------------------------------

# 8. PHASE 0 — BUSINESS FREEZE

### Mục tiêu

Chốt các business decision **trước khi** thay đổi domain.

### Cần chốt

``` text
[BUSINESS DECISION]

BD-01. Commission Rate = ? (cố định / theo Category / theo Seller / cấu hình)
       → liên quan: PAY-04, FIN-03.

BD-02. Settlement Timing = ? (Delivered / Customer xác nhận / sau N ngày)
       → liên quan: PAY-05, FIN-04, ORDER-07.

BD-03. Product Moderation = Manual admin duyệt / Auto-approve?
       → liên quan: PROD-02, PROD-07.

BD-04. Seller KYC mức nào? (Basic info / Full Legal)
       → liên quan: USER-07, SHOP-08.

BD-05. 1 Vendor → 1 Shop hay N Shop?
       → liên quan: USER-06, SHOP-09, Order.multi-vendor.

BD-06. Voucher Funding = Platform chịu 100% / Seller chia sẻ / Cơ chế?
       → liên quan: VCH-08, VCH-09.

BD-07. Refund/Return quyết định cuối = Seller / Platform / Phối hợp?
       → liên quan: REFUND-01..04.

BD-08. Order multi-vendor: Master Order + Sub-Order hay giữ 1 Order
       (items nhóm theo Shop)? → liên quan: ORDER-05, ORDER-06.

BD-09. Payment lifecycle state machine cần những status nào?
       (PENDING / PAID / HOLD / REFUNDED / ...?). → PAY-02, PAY-03.
```

### PASS

Tất cả decision cần thiết đã được ghi rõ trong file
`MARKETPLACE_AUDIT_DECISIONS.md` (TODO: tạo file này nếu chưa có).

### STOP

Không được tự suy đoán. Đặc biệt KHÔNG tự đặt `5%` Commission hay
`7 ngày` Settlement nếu nhóm chưa chốt.

------------------------------------------------------------------------

# 9. PHASE 1 — DOMAIN / OWNERSHIP AUDIT

### Scope

``` text
User
Seller/Vendor
Shop
Product
Category
```

### Tasks (sau khi đã có BD-04, BD-05)

-   Xác nhận User role và account lifecycle.
-   Chuẩn hóa Vendor ownership của Shop (1-1 hay 1-N tùy BD-05).
-   Đảm bảo Product → Shop → Vendor → User chain đúng.
-   Đảm bảo Category là platform-managed.
-   Không thêm SUPPLIER.
-   Nếu 1-N: thêm `List<Shop> findByOwnerId(...)` vào ShopRepository.

### PASS

``` text
User (role=ADMIN/VENDOR/CUSTOMER)
 ↓
Seller capability (BD-05 quyết định 1-1 hay 1-N)
 ↓
Shop (status: PENDING/APPROVED/REJECTED + active boolean)
 ↓
Product (status: DRAFT/PENDING_APPROVAL/ACTIVE/HIDDEN/...)
 ↓
Category (platform-managed)
```

### Files affected (nếu triển khai)

-   `document/User.java` (giữ nguyên shopId — chưa cần đổi nếu BD-05 =
    1-1).
-   `repository/ShopRepository.java` (chỉ thêm method nếu BD-05 = 1-N).
-   `service/impl/VendorServiceImpl.java` (chỉnh `getShopIdFromUser`
    nếu BD-05 = 1-N — có thể cần `selectActiveShop`).
-   `interceptor/CartCountInterceptor.java` (không đổi).

------------------------------------------------------------------------

# 10. PHASE 2 — MULTI-VENDOR CART + ORDER

### Scope

``` text
Cart
Checkout
Order
OrderItem
Sub-Order (nếu BD-08 chốt Master/Sub)
```

### Required scenario (BR-06)

``` text
Customer Cart
 ├── Shop A: Product A, Product C
 └── Shop B: Product B

Checkout
 ↓
Master transaction (Order hoặc Master Order + Sub-Order)
 ├── Sub-Order A → Shop A → OrderItem(s)
 └── Sub-Order B → Shop B → OrderItem(s)
```

### Tasks (sau BD-08)

-   **P0:** Sửa `ORDER-14` (template enum mismatch) — đổi `PROCESSING`
    → `PREPARING`, `SHIPPED` → `SHIPPING`. Đây là runtime bug.
-   **P0:** Chuẩn hóa state machine `OrderStatus`. Thêm `REFUNDED`
    (nếu BD-09 chốt) hoặc tối thiểu document.
-   **P0:** Sửa `ORDER-11` — Vendor update status phải qua
    `OrderService.updateOrderStatus` (validation).
-   **P0:** Nếu BD-08 = Master/Sub → tạo entity `SubOrder` (hoặc đổi
    Order thành embed Sub-Order collection).
-   **P0:** Nếu BD-08 = giữ 1 Order → enforce "Vendor chỉ thấy items
    Shop mình" + "Status là shared toàn Order, Vendor không đổi được
    nếu Order có items từ Shop khác".
-   **P0:** Atomic stock giảm (dùng `MongoTemplate.findAndModify` với
    `findAndModify(..., returnNew)` hoặc phiên bản `stock`).
-   **P0:** Cart cleanup sau Order success — đã OK.

### PASS

-   Multi-shop cart hoạt động.
-   Shop grouping đúng trong checkout UI (`templates/web/checkout.html`
    đã group).
-   Vendor A không thấy Shop B order.
-   Vendor A không đổi được status Order có items Shop B.
-   Admin monitor toàn platform.
-   Historical order snapshot không phụ thuộc Product hiện tại (đã
    đạt cơ bản).

### Files affected

-   `document/Order.java` (thêm status enum / SubOrder nếu cần).
-   `enums/OrderStatus.java` (bổ sung status theo BD-09).
-   `service/impl/OrderServiceImpl.java` (atomic stock, transition).
-   `controller/vendor/VendorOrderController.java` (route qua
    `OrderService.updateOrderStatus`).
-   `templates/vendor/order-list.html` + `templates/vendor/order-detail.html`
    (sửa PROCESSING/SHIPPED → PREPARING/SHIPPING).
-   `repository/OrderRepository.java` (thêm query theo `item.shopId`).

------------------------------------------------------------------------

# 11. PHASE 3 — PAYMENT + FINANCE + SETTLEMENT

### Scope

``` text
Payment
GMV
Platform Fee
Commission (BD-01)
Settlement (BD-02)
Vendor Payout
Refund impact
```

### Tasks (sau BD-01, BD-02, BD-09)

-   **P0:** Sửa `PAY-07` / `ADMIN-02` — `AdminDashboardRes.totalPlatformRevenue`
    đổi tên thành `gmv` (Vendor Sales sau DELIVERED) — hoặc giữ
    `totalPlatformRevenue` nhưng document đây là GMV (KHÔNG phải
    Platform Revenue).
-   **P0:** Tạo entity `Payment` (hoặc tách `OrderPayment` collection)
    với status: `PENDING / PAID / HOLD / REFUNDED / FAILED`.
-   **P0:** Tạo entity `Settlement` (per Sub-Order hoặc per Shop Order):
    `grossAmount, discountAllocation, platformFee, vendorPayout`.
-   **P0:** Áp Commission vào Settlement (theo BD-01).
-   **P0:** Refund flow cơ bản (BD-07).

### Target

``` text
Customer Payment
 ↓
Payment (entity, status)
 ↓
Order / Sub-Order
 ↓
Settlement
 ├── Gross Amount
 ├── Discount Allocation
 ├── Platform Fee (= gross × commission rate)
 └── Vendor Payout (= gross − Platform Fee − Seller-borne discount)
```

### PASS

-   `Order.totalAmount ≠ Admin Revenue`.
-   Dashboard có `GMV / Platform Revenue / Vendor Sales / Vendor
    Payable / Escrow / Refund` riêng biệt.

### Files affected (nếu triển khai)

-   MỚI: `document/Payment.java`, `document/Settlement.java`,
    `document/Refund.java` (tùy scope).
-   MỚI: `repository/PaymentRepository.java`,
    `repository/SettlementRepository.java`.
-   MỚI: `service/PaymentService.java`, `service/SettlementService.java`.
-   `service/impl/AdminServiceImpl.java` (đổi `totalPlatformRevenue`
    semantics hoặc tách metric mới).
-   `dto/response/AdminDashboardRes.java` (thêm field `gmv, platformRevenue,
    vendorSales, vendorPayable, refund`).
-   `templates/admin/dashboard.html` (label rõ từng metric).

------------------------------------------------------------------------

# 12. PHASE 4 — SELLER LIFECYCLE

### Scope

``` text
Seller registration
Verification (BD-04)
Approval
Rejection
Restriction
Suspension
Shop lifecycle
```

### Tasks (sau BD-04)

-   **P0:** Tạo Seller onboarding flow (form riêng sau khi
    `User.role=VENDOR`). Form lưu `Shop` với các field KYC (legalName,
    taxCode, ...).
-   **P0:** Phân biệt state: User `ACTIVE/LOCKED/UNVERIFIED` ≠ Shop
    `PENDING/APPROVED/REJECTED/SUSPENDED` ≠ Product
    `DRAFT/PENDING_APPROVAL/ACTIVE/...`.
-   **P0:** Vendor không được tạo Product khi Shop chưa `APPROVED`
    (hoặc theo rule business).

### PASS

-   Vendor có Shop chưa `APPROVED` không thể:
    -   Tạo Product.
    -   Nhận Order.
    -   Tạo Voucher.
-   Admin có thể transition Shop status theo state machine rõ ràng.

### Files affected

-   `enums/ShopStatus.java` (thêm `SUSPENDED, LOCKED` nếu cần — `VERIFY`).
-   `document/Shop.java` (thêm field KYC).
-   `controller/vendor/VendorProductController.java` (gate theo Shop
    status).
-   `service/impl/ProductServiceImpl.java` (gate theo Shop status).
-   `templates/vendor/shop-create.html` + form KYC mới.

------------------------------------------------------------------------

# 13. PHASE 5 — PRODUCT + MODERATION

### Scope

``` text
Product
Category
Moderation (BD-03)
Violation
Visibility
```

### Tasks (sau BD-03)

-   **P0:** Tách `ProductStatus.PENDING_APPROVAL` (nếu BD-03 = manual)
    hoặc bỏ qua (nếu BD-03 = auto-approve).
-   **P0:** Vendor tạo Product → status = `DRAFT` (không publish ngay).
-   **P0:** Vendor submit → `PENDING_APPROVAL`.
-   **P0:** Admin approve → `ACTIVE` / reject → `REJECTED`.
-   **P0:** Sửa `PROD-07` — `ProductServiceImpl.createProduct` không
    nhận `status` từ Vendor mà force `DRAFT`.

### PASS

``` text
Vendor → Create → DRAFT → Submit → PENDING_APPROVAL
                                ↓
                              Admin
                          ├── APPROVED → ACTIVE
                          └── REJECTED → REJECTED (visible cho Vendor)
```

-   Product bị HIDDEN không mất Order history (đã đạt qua
    `AdminProductServiceImpl.deleteProduct` comment LOCK 7).

### Files affected

-   `enums/ProductStatus.java` (thêm `PENDING_APPROVAL, REJECTED`).
-   `service/impl/ProductServiceImpl.java` (gate status).
-   `service/impl/AdminProductServiceImpl.java` (thêm
    `approveProduct/rejectProduct`).
-   `controller/admin/AdminProductController.java` (thêm route).
-   `templates/admin/product-list.html` + `templates/admin/product-detail.html`.

------------------------------------------------------------------------

# 14. PHASE 6 — VOUCHER + PROMOTION

### Scope

``` text
Platform Voucher (WEB) — đã có
Seller Voucher (SHOP) — đã có
Promotion — MISSING
Discount allocation — MISSING
```

### Tasks (sau BD-06)

-   **P1:** Sửa `VCH-07` — `OrderServiceImpl.createOrder` đọc
    `voucherCode/voucherId` từ `CheckoutReq`, áp discount vào
    `totalAmount`, lưu `discountAmount` vào Order.
-   **P1:** Sửa `VCH-10` — bỏ `placeOrderWithVoucher` orphan route
    (hoặc tích hợp vào createOrder).
-   **P1:** Sửa `VCH-11` — khi Cart có items nhiều Shop và dùng SHOP
    Voucher, voucher chỉ áp lên shopSubtotal, không phải orderTotal.
-   **P1:** Allocation: `Customer Payable / Vendor Amount / Platform Cost`
    (theo BD-06).
-   **P2:** Promotion entity (campaign, banner-to-product) — có thể
    dùng `Banner` mở rộng (hiện Banner đã có).

### PASS

-   Checkout tính đúng `Customer Payable` (orderTotal − voucher
    discount).
-   Settlement tách được cost bên nào chịu.

### Files affected

-   `dto/request/CheckoutReq.java` (thêm `voucherCode`).
-   `service/impl/OrderServiceImpl.java` (áp discount).
-   `document/Order.java` (thêm `voucherId, voucherDiscount,
    voucherCostBearer`).
-   `controller/VoucherController.java` (bỏ route orphan).

------------------------------------------------------------------------

# 15. PHASE 7 — SHIPPING

### Scope

``` text
Shipping
Tracking
Delivery
Delivery failure
Return shipping
```

### Tasks

-   **P1:** Shipping fee calculation thực (không hardcode 15.000đ) —
    có thể làm theo `shopId` hoặc `region`.
-   **P1:** Shipping fee allocation khi multi-shop Order.
-   **P2:** Tracking entity (carrier + tracking code).
-   **P2:** Delivery failure handling.

### PASS

-   Order lifecycle không dùng Shipping fee như substitute cho shipment
    state.

### Files affected

-   `service/impl/OrderServiceImpl.java` (shippingFee logic).
-   `document/Order.java` (thêm tracking field nếu P2).

------------------------------------------------------------------------

# 16. PHASE 8 — REVIEW + RETURN + REFUND + DISPUTE

### Scope

``` text
Review
Return
Refund
Dispute
```

### Tasks (sau BD-07)

-   **P1:** Sửa `REV-02` — `ReviewServiceImpl.createReview` phải check
    Customer đã mua Product (Order có item `productId`, Order status
    `DELIVERED`).
-   **P1:** Sửa `REV-04` — giới hạn 1 review / 1 Order Item.
-   **P1:** Tạo `ReturnRefundRequest` entity + flow:
    `Customer → Request → Seller Response → Platform (optional) →
    Decision → Refund`.
-   **P2:** Tạo `Dispute` entity + escalation.

### PASS

-   Review chỉ khi Customer đã mua và Order đã DELIVERED.
-   Refund có financial effect (giảm `Vendor Payout`, tăng `Refund`).
-   Return không phá historical order.
-   Dispute có ownership rõ.

### Files affected

-   `service/impl/ReviewServiceImpl.java` (gate theo Order).
-   MỚI: `document/ReturnRefundRequest.java`,
    `service/ReturnRefundService.java`.
-   `enums/OrderStatus.java` (thêm `RETURNING, REFUNDED` nếu BD-09).

------------------------------------------------------------------------

# 17. PHASE 9 — SELLER VIOLATION + PLATFORM MODERATION

### Scope

``` text
Seller Violation
Product Violation
Penalty
Restriction
Suspension
```

### Tasks (sau khi có BD liên quan)

-   **P1:** Tạo `Violation` entity với `targetType: SELLER | PRODUCT`,
    `severity`, `action`, `createdBy (Admin)`.
-   **P1:** Tạo flow: Admin record Violation → Seller bị hạn chế (cấm
    tạo Product / nhận Order) → appeal (optional).
-   **P1:** Phân biệt Seller vs Product Violation (BR-05 + §5).

### PASS

-   Seller Violation ≠ Product Violation.
-   Cả 2 đều giữ lịch sử (BR-10).

### Files affected

-   MỚI: `document/Violation.java`, `service/ViolationService.java`,
    `controller/admin/AdminViolationController.java`.
-   `service/impl/VendorServiceImpl.java` (gate theo Violation).

------------------------------------------------------------------------

# 18. PHASE 10 — SECURITY / RBAC HARDENING

### Scope

``` text
Controller
Service
Repository query
SecurityConfig
```

### Critical tests (cần viết)

``` text
Customer A → Order B       = DENY
Vendor A → Shop B Product  = DENY
Vendor A → Shop B Order    = DENY
Customer → Admin page      = DENY
Vendor → Admin API         = DENY
```

### Tasks

-   **P0:** Sửa `SEC-02` — bật `@PreAuthorize` trên các endpoint nhạy
    cảm (Admin moderation, Vendor Product/Order edit).
-   **P0:** Sửa `SEC-05` — Vendor update status không được phép khi
    Order có items thuộc Shop khác (hoặc enforce theo Sub-Order).
-   **P0:** Sửa `SEC-10` — siết `anyRequest().permitAll()` thành các
    rule rõ ràng (vd: `/web/orders/**` cần authenticated).
-   **P0:** Thêm method-level security cho `OrderService` (chỉ Owner
    xem Order của mình).

### PASS

-   Authorization không phụ thuộc UI hiding.
-   `Order.getOrderById` enforce ownership.

### Files affected

-   `config/SecurityConfig.java` (matcher chi tiết).
-   `service/impl/OrderServiceImpl.java` (ownership check).
-   `service/impl/VendorServiceImpl.java` (`@PreAuthorize`).

------------------------------------------------------------------------

# 19. PHASE 11 — ADMIN DASHBOARD (metrics theo baseline)

### Metrics bắt buộc (sau Phase 3)

``` text
GMV (gross merchandise value)
Platform Revenue (= Σ commission)
Vendor Sales (= GMV theo Vendor)
Vendor Payable (= Vendor Sales − Platform Fee − Seller-borne discount)
Refund (tổng)
Escrow / Hold (nếu có)
Order Count
Customer Count
Vendor Count
Shop Count
```

### Tasks

-   **P0:** Đổi label và terminology (`templates/admin/dashboard.html`).
-   **P0:** Bổ sung các metric thiếu sau Phase 3.

### PASS

-   Terminology đúng business.

### Files affected

-   `service/impl/AdminServiceImpl.java`.
-   `dto/response/AdminDashboardRes.java`.
-   `templates/admin/dashboard.html`.

------------------------------------------------------------------------

# 20. PHASE 12 — TESTING

### Status hiện tại: `MISSING` (không có file test nào).

### Cần viết

#### Unit

-   Commission calculation.
-   Voucher calculation.
-   Order total (multi-shop).
-   Refund calculation.
-   Settlement calculation.
-   Stock update.
-   Status transition matrix.

#### Integration

-   Customer checkout (single-shop).
-   Customer checkout (multi-shop).
-   Vendor order processing.
-   Admin moderation.
-   Seller approval flow.
-   Refund flow.

#### Security

-   Horizontal privilege escalation (Customer A xem Order B → DENY).
-   Vertical privilege escalation (Customer → Admin API → DENY).
-   Object ownership (Vendor A edit Product Shop B → DENY).

#### Regression

-   Existing Customer flow (login, browse, cart, checkout).
-   Existing Vendor flow (shop create, product CRUD, order processing).
-   Existing Admin flow (CRUD User/Shop/Product/Order/Review/Voucher).
-   COD payment.
-   VNPAY payment.
-   Voucher apply.

### Files affected

-   MỚI: `src/test/java/**`.

------------------------------------------------------------------------

# 21. PHASE 13 — FINAL AUDIT

Chạy lại toàn bộ matrix §6. Target:

``` text
MISSING     = 0 P0 items
WRONG       = 0 P0 items
INCOMPLETE  = 0 P0 items
RISK        = reviewed và mitigated
BLOCKED     = documented và chốt decision
BUSINESS    = documented
```

------------------------------------------------------------------------

# 22. END-TO-END ACCEPTANCE SCENARIOS

## Scenario A — Single Shop

``` text
Customer
 ↓
Product (Shop A)
 ↓
Cart
 ↓
Checkout
 ↓
Order
 ↓
Seller (Shop A)
 ↓
Shipping
 ↓
Delivered
 ↓
Review
```

Status hiện tại: **Cơ bản OK** (với caveat `ORDER-14` template bug).

## Scenario B — Multi Shop

``` text
Customer
 ↓
Cart
 ├── Shop A: Product A, Product C
 └── Shop B: Product B
 ↓
Checkout
 ↓
Master transaction / Order
 ├── Seller A (items A, C)
 └── Seller B (item B)
```

Status hiện tại: **WRONG** — Order single, status shared
(`ORDER-06`). Cần Phase 2 + BD-08.

## Scenario C — Seller Suspension

``` text
Seller
 ↓
Suspended (BD cần chốt)
 ↓
New Product       = policy-defined
New Order         = policy-defined
Existing Order    = policy-defined
Refund            = policy-defined
Payout            = policy-defined
```

Status: **MISSING** — không có Suspended workflow. Cần Phase 4.

## Scenario D — Product Violation

``` text
Product
 ↓
Violation
 ↓
Hidden / Rejected
```

Existing historical Order vẫn phải truy xuất được — **OK**
(snapshot field trong Order items).

## Scenario E — Refund

``` text
Customer
 ↓
Refund Request
 ↓
Decision
 ↓
Refund
 ↓
Settlement Adjustment
```

Status: **MISSING**. Cần Phase 8 + BD-07.

------------------------------------------------------------------------

# 23. FILE / CODE CHANGE RULE

Mỗi implementation task phải có format:

``` text
TASK ID:
AREA:
CURRENT:
PROBLEM:
TARGET:
FILES AFFECTED:
DEPENDENCIES:
BUSINESS DECISION:
TESTS:
PASS CRITERIA:
ROLLBACK / RISK:
```

Không yêu cầu Coding Agent một câu chung chung.

------------------------------------------------------------------------

# 24. CODING AGENT HANDOFF TEMPLATE

``` text
You are implementing one approved phase of the CNJ70 Ecommerce marketplace.

READ FIRST:
1. SHOPEE_MARKETPLACE_MODEL.md
2. MARKETPLACE_AUDIT_AND_IMPLEMENTATION_PHASES.md (this file, Phase Audit #1)

RULES:
- Do not redesign unrelated modules.
- Do not add SUPPLIER.
- Do not invent business rules.
- Do not hard-code an unapproved commission rate.
- Preserve existing working flows unless the approved phase requires change.
- Enforce ownership at service level.
- Keep financial concepts separate.
- Do not silently change public behavior outside the phase.

BEFORE CODING:
1. Inspect current source.
2. Confirm affected files/classes.
3. Report current implementation.
4. Report exact gap.
5. Report proposed changes.
6. Stop if a required business decision is missing.

IMPLEMENT ONLY:
[INSERT APPROVED PHASE]

AFTER CODING:
1. Compile.
2. Run relevant tests.
3. Run regression tests.
4. Report changed files.
5. Report remaining gaps.
6. Report PASS/FAIL for each acceptance criterion.
```

------------------------------------------------------------------------

# 25. DO NOT DO LIST

Không được:

-   Thêm `SUPPLIER`.
-   Tự tạo commission rate.
-   Tự thay đổi role.
-   Tự chuyển toàn bộ Order thành SubOrder nếu chưa audit.
-   Tự xóa Order model cũ.
-   Tự xóa dữ liệu lịch sử.
-   Dùng Product hiện tại để tính lại historical transaction.
-   Cho Vendor xem toàn bộ platform orders.
-   Cho Customer xem Order của Customer khác.
-   Cho Vendor sửa Product của Shop khác.
-   Gọi GMV là Platform Revenue.
-   Dùng `paid=true` để đại diện toàn bộ settlement.
-   Dùng `Order.status` để đại diện Payment/Refund/Settlement.
-   Đưa business logic lớn vào Controller.
-   Sửa UI trước khi domain/backend contract ổn định.
-   Refactor không liên quan chỉ vì "clean code".
-   **Sửa template enum sai (PROCESSING/SHIPPED) mà không fix enum
    gốc** — phải sửa `OrderStatus` enum hoặc sửa template, không
    patch một phía.

------------------------------------------------------------------------

# 26. IMPLEMENTATION ORDER

Thứ tự ưu tiên chính thức (dựa trên dependency thực tế giữa các
finding):

``` text
PHASE 0
Business Freeze (BD-01..09)
      ↓
PHASE 1
Domain / Ownership
      ↓
PHASE 2
Multi-Vendor Cart + Order
      ↓
PHASE 3
Payment + Finance + Settlement
      ↓
PHASE 10
Security / RBAC (gate song song với các phase trên nếu cần)
      ↓
PHASE 4
Seller Lifecycle
      ↓
PHASE 5
Product + Moderation
      ↓
PHASE 6
Voucher / Promotion
      ↓
PHASE 7
Shipping
      ↓
PHASE 8
Review / Return / Refund / Dispute
      ↓
PHASE 9
Violation / Platform Moderation
      ↓
PHASE 11
Admin Dashboard
      ↓
PHASE 12
Testing
      ↓
PHASE 13
Final Audit
```

------------------------------------------------------------------------

# 27. DEFINITION OF DONE

Phase Marketplace chỉ được coi là hoàn thành khi:

``` text
[✓] Business baseline approved (BD-01..09)
[✓] User / Seller / Shop distinction
[✓] Product ownership
[✓] Category ownership
[✓] Seller onboarding (BD-04)
[✓] Shop lifecycle (PENDING/APPROVED/SUSPENDED/...)
[✓] Multi-shop Cart
[✓] Multi-vendor Order (BD-08)
[✓] Seller-specific Order access (SEC-03 + Sub-Order enforcement)
[✓] Payment lifecycle (BD-09)
[✓] GMV
[✓] Platform Revenue
[✓] Platform Fee / Commission (BD-01)
[✓] Settlement (BD-02)
[✓] Vendor Payout
[✓] Voucher distinction (SHOP / WEB)
[✓] Discount allocation (BD-06)
[✓] Shipping flow
[✓] Review eligibility (BR-09)
[✓] Return
[✓] Refund (BD-07)
[✓] Dispute
[✓] Seller Violation
[✓] Product Violation
[✓] Admin RBAC
[✓] Vendor ownership security
[✓] Customer ownership security
[✓] Historical transaction integrity (BR-10)
[✓] Unit tests
[✓] Integration tests
[✓] Security tests
[✓] Regression tests
[✓] Final audit
```

------------------------------------------------------------------------

# 28. FINAL PRINCIPLE

> **Không sửa code chỉ vì thấy một Entity còn thiếu.**
>
> Phải xác định:
>
> ``` text
> BUSINESS REQUIREMENT
>        ↓
> CURRENT IMPLEMENTATION
>        ↓
> GAP
>        ↓
> IMPACT
>        ↓
> BUSINESS DECISION
>        ↓
> IMPLEMENTATION
>        ↓
> TEST
>        ↓
> FINAL AUDIT
> ```
>
> `SHOPEE_MARKETPLACE_MODEL.md` trả lời:
>
> **"Marketplace cần vận hành như thế nào?"**
>
> `MARKETPLACE_AUDIT_AND_IMPLEMENTATION_PHASES.md` trả lời:
>
> **"Project hiện tại đang ở đâu, lệch ở đâu, thiếu gì, và phải đưa
> nó tới target bằng những phase nào?"**

------------------------------------------------------------------------

# 29. PHASE STATUS

Current document status (sau Phase Audit #1 — 14/09/2026):

``` text
[✓] Business baseline linked
[✓] Audit scope defined
[✓] Audit classification defined
[✓] Current source evidence reviewed (HIGH confidence trên toàn bộ
    document/service/controller)
[✓] Full source-code audit (Phase 1)
[✓] Gap matrix (theo HIGH confidence)
[✓] P0/P1/P2 prioritization
[✓] Implementation phases
[✓] Acceptance scenarios
[✓] Coding-agent handoff
[✓] Definition of Done
[✓] Do-not-do list

[ ] Business decisions finalized (BD-01..09)
[ ] Implementation (Phase 1 → Phase 13)
[ ] Regression
[ ] Final audit
```

### IMPORTANT

This document is the **audit and implementation roadmap**, not
permission to modify source code automatically.

Before each coding phase:

``` text
READ
 ↓
AUDIT
 ↓
CONFIRM
 ↓
IMPLEMENT
 ↓
TEST
 ↓
PASS
```

Only then move to the next phase.
