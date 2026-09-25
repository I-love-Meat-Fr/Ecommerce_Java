PHASE 2 — ADMIN SHOP + ORDER FIX

0. IMPLEMENTATION INSTRUCTION

Phase 2 sửa 2 module Admin đang lỗi:

Admin Shop — CRUD + xem chi tiết không hoạt động.
Admin Order — không xem chi tiết đơn hàng.

Dựa trực tiếp trên:

Source code hiện tại (AdminShopController 284 lines, AdminOrderController 105 lines).
Gate 0 — Source Audit + Scope Lock.
Phase 1 Audit Report.
PHASE 4 — ADMIN SHOP KYC CATEGORY PRODUCT.md (đã có).
PHASE 6 — ADMIN ORDER REVIEW VIOLATION ESCALATION.md (đã có).
User report: "CRUD và xem được không hoạt động" (Shop), "không xem chi tiết đơn hàng được" (Order).

CURRENT SOURCE
        ↓
SHOP FIX CONTRACT
        ↓
ORDER FIX CONTRACT
        ↓
CONTROLLER FIX
        ↓
TEMPLATE FIX
        ↓
SERVICE PERSISTENCE CHECK
        ↓
SECURITY CHECK
        ↓
BUILD
        ↓
PHASE 2 READY

1. MỤC TIÊU

Phase 2 phải fix được:

Shop: list / detail / approve / reject / activate / deactivate phải lưu MongoDB, redirect flash, hiển thị đúng transition ShopStatus.
Order: list chi tiết (đã có), verify thực sự load được order detail; thêm admin actions nếu có (status update / cancel / refund — Phase 9 note đã yêu cầu).

2. KẾT QUẢ ĐỐI CHIẾU SOURCE

Theo source audit:

Shop:
AdminShopController.java (284 lines) đã có 6 methods (Phase 1 audit §1.1):
- list
- detail
- approve
- reject
- activate
- deactivate
shop-list.html, shop-detail.html đã có (Phase 1 audit §4.1).
AdminShopServiceImpl.java đã có.

User report: "CRUD và xem được không hoạt động" → nghi vấn:
- Template action form URL sai.
- Service save không thực sự persist.
- Template detail không bind Shop document đúng field.

Order:
AdminOrderController.java (105 lines) đã có 2 methods (Phase 1 audit §1.1):
- list
- detail
order-list.html, order-detail.html đã có.

User report: "không xem chi tiết đơn hàng được" → nghi vấn:
- AdminOrderServiceImpl.getOrderById() có thể sai lookup (giống VendorFlowE2ETest String _id bug).
- order-detail.html thiếu flash UI.
- Template detail không đọc đúng `Order.OrderItem`.

Phase 8 FINAL note:
"ORDER admin write endpoints missing (Phase 6 noted)"

3. NGUYÊN TẮC BẮT BUỘC — KHÔNG XÓA MONGODB

Tuyệt đối KHÔNG:
deleteAll() / deleteMany({}) / drop() / dropDatabase()

4. KHÔNG FAKE

Không fake ShopStatus transition chỉ để UI đẹp.

Không fake order detail chỉ để test.

5. KHÔNG PHỤ THUỘC DEVELOPER KHÁC

Phase 2 phải triển khai độc lập.

6. SOURCE OF TRUTH

CURRENT SOURCE > USER REPORT > PHASE 1 AUDIT > PHASE 8 FINAL REPORT

7. DISCOVERY RULE

SEARCH → EXISTS? → INSPECT → REUSE/FIX → CREATE (nếu cần)

8. PHẠM VI PHASE 2

| Module | Hành động | File chính |
|---|---|---|
| Shop | Verify 6 methods persist MongoDB | AdminShopController.java + AdminShopServiceImpl.java |
| Shop | Verify action form trong template | shop-list.html + shop-detail.html |
| Shop | Thêm flash UI nếu thiếu | shop-list.html + shop-detail.html |
| Order | Verify getById với String _id | AdminOrderController.java + AdminOrderServiceImpl.java |
| Order | Verify order-detail.html render đúng Order.OrderItem | order-detail.html |
| Order | Verify flash UI | order-list.html + order-detail.html |
| Order | Cân nhắc thêm endpoint status update | (nếu cần cho Phase 9) |

9. SHOP FIX CONTRACT

9.1 — SHOP DOCUMENT

Đọc Shop.java — fields đã có:
- id
- ownerUserId
- name
- description
- status (ShopStatus: PENDING / APPROVED / REJECTED / SUSPENDED / RESTRICTED)
- createdAt
- updatedAt

Theo Phase 1 audit §1.4, ShopStatus OK.

9.2 — SHOP CONTROLLER

```java
@GetMapping("/admin/shops")
public String list(@RequestParam(required=false) String status, Model model) {
    List<Shop> shops;
    if (status != null && !status.isEmpty()) {
        shops = shopRepository.findByStatus(ShopStatus.valueOf(status));
    } else {
        shops = shopRepository.findAll();
    }
    model.addAttribute("shops", shops);
    model.addAttribute("currentStatus", status);
    return "admin/shop-list";
}

@GetMapping("/admin/shops/{id}")
public String detail(@PathVariable String id, Model model) {
    Shop shop = shopRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Shop không tồn tại"));
    model.addAttribute("shop", shop);
    return "admin/shop-detail";
}

@PostMapping("/admin/shops/{id}/approve")
public String approve(@PathVariable String id, RedirectAttributes ra) {
    Shop s = shopService.approve(id);
    ra.addFlashAttribute("flashSuccess", "Đã duyệt shop: " + s.getName());
    return "redirect:/admin/shops";
}

@PostMapping("/admin/shops/{id}/reject")
public String reject(@PathVariable String id,
                     @RequestParam String reason,
                     RedirectAttributes ra) {
    Shop s = shopService.reject(id, reason);
    ra.addFlashAttribute("flashSuccess", "Đã từ chối shop: " + s.getName());
    return "redirect:/admin/shops";
}

@PostMapping("/admin/shops/{id}/activate")
public String activate(@PathVariable String id, RedirectAttributes ra) {
    Shop s = shopService.activate(id);
    ra.addFlashAttribute("flashSuccess", "Đã kích hoạt shop");
    return "redirect:/admin/shops";
}

@PostMapping("/admin/shops/{id}/deactivate")
public String deactivate(@PathVariable String id, RedirectAttributes ra) {
    Shop s = shopService.deactivate(id);
    ra.addFlashAttribute("flashSuccess", "Đã vô hiệu shop");
    return "redirect:/admin/shops";
}
```

9.3 — SHOP SERVICE

AdminShopServiceImpl phải:

```java
public Shop approve(String id) {
    Shop s = shopRepository.findById(id).orElseThrow(...);
    s.setStatus(ShopStatus.APPROVED);
    s.setUpdatedAt(LocalDateTime.now());
    return shopRepository.save(s);
}

public Shop reject(String id, String reason) {
    Shop s = shopRepository.findById(id).orElseThrow(...);
    s.setStatus(ShopStatus.REJECTED);
    s.setRejectionReason(reason);
    s.setUpdatedAt(LocalDateTime.now());
    return shopRepository.save(s);
}

public Shop activate(String id) {
    Shop s = shopRepository.findById(id).orElseThrow(...);
    s.setStatus(ShopStatus.APPROVED); // hoặc ACTIVE — xem ShopStatus enum
    s.setUpdatedAt(LocalDateTime.now());
    return shopRepository.save(s);
}

public Shop deactivate(String id) {
    Shop s = shopRepository.findById(id).orElseThrow(...);
    s.setStatus(ShopStatus.SUSPENDED);
    s.setUpdatedAt(LocalDateTime.now());
    return shopRepository.save(s);
}
```

⚠️ Không ép enum nếu ShopStatus hiện không có ACTIVE. Phase 1 audit §1.4 cho biết ShopStatus: PENDING, APPROVED, REJECTED, SUSPENDED, RESTRICTED. → Không có ACTIVE riêng. activate = APPROVED, deactivate = SUSPENDED.

9.4 — SHOP TEMPLATE

shop-list.html phải có flash UI:

```html
<div th:if="${flashSuccess}" class="alert alert-success" th:text="${flashSuccess}"></div>
<div th:if="${flashError}" class="alert alert-danger" th:text="${flashError}"></div>
```

shop-detail.html phải có form action cho approve/reject/activate/deactivate:

```html
<form th:action="@{'/admin/shops/' + ${shop.id} + '/approve'}" method="post">
    <button type="submit" class="btn btn-success">Duyệt</button>
</form>
<form th:action="@{'/admin/shops/' + ${shop.id} + '/reject'}" method="post">
    <input type="text" name="reason" required placeholder="Lý do từ chối"/>
    <button type="submit" class="btn btn-danger">Từ chối</button>
</form>
```

9.5 — SHOP REPOSITORY

Đã có theo Phase 1 audit §5.2. Nếu chưa có `findByStatus`:

```java
List<Shop> findByStatus(ShopStatus status);
```

10. ORDER FIX CONTRACT

10.1 — ORDER DOCUMENT

Đọc Order.java. Fields đã có:
- id
- userId
- shopId
- items (OrderItem: productId, productName, quantity, price)
- subtotal
- shippingFee
- discount
- voucherId
- total
- status (OrderStatus)
- shippingAddress
- paymentMethod
- createdAt
- updatedAt

Theo Phase 1 audit §1.3 Order EXISTING.

10.2 — ORDER CONTROLLER

⚠️ Bug nghi vấn: VendorFlowE2ETest String _id shop lookup error (Phase 8 §2.2 note).

```java
@GetMapping("/admin/orders")
public String list(@RequestParam(required=false) String status, Model model) {
    List<Order> orders;
    if (status != null && !status.isEmpty()) {
        orders = orderRepository.findByStatus(OrderStatus.valueOf(status));
    } else {
        orders = orderRepository.findAll();
    }
    model.addAttribute("orders", orders);
    return "admin/order-list";
}

@GetMapping("/admin/orders/{id}")
public String detail(@PathVariable String id, Model model) {
    Order order = orderRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Đơn hàng không tồn tại"));
    model.addAttribute("order", order);
    // Lấy thêm user + shop info để hiển thị
    return "admin/order-detail";
}
```

10.3 — ORDER DETAIL TEMPLATE

order-detail.html phải bind:

```html
<h2>Đơn hàng #<span th:text="${order.id}"></span></h2>

<div>
    <p>Khách: <span th:text="${order.userId}"></span></p>
    <p>Shop: <span th:text="${order.shopId}"></span></p>
    <p>Trạng thái: <span th:text="${order.status}"></span></p>
    <p>Thanh toán: <span th:text="${order.paymentMethod}"></span></p>
    <p>Tổng tiền: <span th:text="${order.total}"></span></p>
</div>

<table>
    <thead>
        <tr><th>Sản phẩm</th><th>SL</th><th>Giá</th></tr>
    </thead>
    <tbody>
        <tr th:each="item : ${order.items}">
            <td th:text="${item.productName}"></td>
            <td th:text="${item.quantity}"></td>
            <td th:text="${item.price}"></td>
        </tr>
    </tbody>
</table>
```

10.4 — ORDER ADMIN WRITE ENDPOINTS

Theo Phase 8 §10 Recommendation #5:
"Add Order admin write endpoints: ORDER_STATUS_UPDATED/CANCELLED/REFUNDED"

Có thể thêm trong Phase 2 (hoặc Phase 9 nếu tách):

```java
@PostMapping("/admin/orders/{id}/status")
public String updateStatus(@PathVariable String id,
                          @RequestParam String status,
                          RedirectAttributes ra) {
    Order o = adminOrderService.updateStatus(id, OrderStatus.valueOf(status));
    ra.addFlashAttribute("flashSuccess", "Đã cập nhật trạng thái đơn hàng");
    return "redirect:/admin/orders/" + id;
}

@PostMapping("/admin/orders/{id}/cancel")
public String cancel(@PathVariable String id, RedirectAttributes ra) {
    Order o = adminOrderService.cancel(id);
    ra.addFlashAttribute("flashSuccess", "Đã hủy đơn hàng");
    return "redirect:/admin/orders/" + id;
}
```

⚠️ Phase 2 chỉ fix "không xem chi tiết được". Admin write endpoints (status / cancel / refund) có thể OUT OF SCOPE Phase 2 — để Phase 3+ hoặc Phase 9.

Quyết định: Phase 2 chỉ fix READ-ONLY detail. Nếu user muốn write, tạo Phase 9 riêng.

11. SHOP + ORDER TEST

```java
@Test void listShops_admin_returnsAllShops() { ... }
@Test void approveShop_validId_statusSetAPPROVED() { ... }
@Test void approveShop_invalidId_throwNotFound() { ... }
@Test void rejectShop_validIdAndReason_statusSetREJECTED() { ... }
@Test void rejectShop_missingReason_throwValidation() { ... }
@Test void activateShop_validId_statusSetAPPROVED() { ... }
@Test void deactivateShop_validId_statusSetSUSPENDED() { ... }
@Test void listShops_filterByStatus_returnsFiltered() { ... }

@Test void listOrders_admin_returnsAllOrders() { ... }
@Test void viewOrderDetail_validId_returnsOrder() { ... }
@Test void viewOrderDetail_invalidId_throwNotFound() { ... }
```

12. SHOP + ORDER REGRESSION

Verify không hỏng:
- Vendor shop CRUD (customer-facing)
- Vendor order management
- Customer order history
- Shop owner → chỉ thấy shop của mình (Phase 2 đã verify)

13. SHOP + ORDER SECURITY

Đã có theo Phase 2:
/admin/shops/**   ADMIN + MODERATOR (Phase 2 verified)
/admin/orders/**  ADMIN-only (Phase 2 verified)

Không cần sửa SecurityConfig ở Phase 2.

14. EXISTING DATA

Sau Phase 2:
- shops: PRESERVED
- orders: PRESERVED
- users: PRESERVED

15. MONGODB SAFETY

Không drop collection.
Không deleteMany.

16. TEST DATA

Nếu cần:
[TEST-PHASE-2]
- 1 Shop mới status=PENDING
- 1 Shop mới status=APPROVED
- 1 Shop mới status=REJECTED

Không xóa sau test.

17. KHÔNG LÀM MẤT CHỨC NĂNG HIỆN TẠI

Không được làm hỏng:
- Authentication
- Shop owner register / create shop flow
- Vendor product CRUD (của shop)
- Cart / Checkout (đã có)

18. FILE CHANGE REPORT

Files Modified:
- src/main/java/com/ecommerce/cnj70/controller/admin/AdminShopController.java
- src/main/java/com/ecommerce/cnj70/service/impl/AdminShopServiceImpl.java (nếu thiếu method)
- src/main/resources/templates/admin/shop-list.html (thêm flash UI nếu thiếu)
- src/main/resources/templates/admin/shop-detail.html (form action nếu sai)
- src/main/java/com/ecommerce/cnj70/controller/admin/AdminOrderController.java (nếu cần fix getById)
- src/main/java/com/ecommerce/cnj70/service/impl/AdminOrderServiceImpl.java (nếu cần fix lookup)
- src/main/resources/templates/admin/order-list.html (flash UI)
- src/main/resources/templates/admin/order-detail.html (binding đúng)

Files Created:
- src/test/java/com/ecommerce/cnj70/fix/phase2/ShopFixTest.java
- src/test/java/com/ecommerce/cnj70/fix/phase2/OrderFixTest.java

Files Deleted:
NONE

19. DATA CHANGE REPORT

MongoDB:
Existing data deleted: NONE
Database reset: NO
Collection dropped: NO

Test records inserted (nếu cần):
- 3 Shop test
- 0 Order test (order đã có sẵn)

Test records deleted:
NO

20. BUILD REPORT

Command: mvn clean package -DskipTests
Result: PASS / FAIL

21. MANUAL TEST REPORT

Test 1 — Shop CRUD:
E1: Login ADMIN
E2: GET /admin/shops → list
E3: Click vào shop → /admin/shops/{id} → hiển thị detail
E4: Click "Duyệt" → status PENDING → APPROVED, flashSuccess
E5: Click "Từ chối" + lý do → status REJECTED, flashSuccess
E6: Click "Activate" → status APPROVED (nếu đang REJECTED/SUSPENDED)
E7: Click "Deactivate" → status SUSPENDED
E8: Verify MongoDB shops collection thực sự thay đổi status

Test 2 — Order Detail:
F1: Login ADMIN
F2: GET /admin/orders → list
F3: Click vào order → /admin/orders/{id} → hiển thị detail đầy đủ (items, address, total)
F4: Reload → vẫn hiển thị đúng

22. KNOWN ISSUES

Mỗi issue:
Issue: ...
Current Behavior: ...
Expected Behavior: ...
Root Cause: ...
Source: ...
Severity: ...
Phase 2 Status: ...

23. STATUS CLASSIFICATION

EXISTING / FIXED / PARTIAL / MISSING / WRONG / OUT OF SCOPE

24. ACCEPTANCE CRITERIA

Phase 2 PASS khi:
- Shop: 6 methods persist MongoDB đầy đủ
- Shop: flash UI hiển thị sau mỗi action
- Shop: manual test PASS
- Order: detail view load đúng
- Order: hiển thị items, address, total
- Order: manual test PASS
- Build PASS
- MongoDB data preserved
- Existing flows (Vendor / Customer) không bị hỏng

25. ĐIỀU KIỆN KẾT THÚC

Phase 2 PASS khi:
- Shop approve/reject/activate/deactivate thực sự lưu MongoDB
- Shop detail binding đúng
- Order detail binding đúng
- Tất cả flash message hiển thị
- Manual test 21.1, 21.2 PASS

Admin write order endpoints (status update / cancel / refund) — Phase 9 hoặc ghi rõ OUT OF SCOPE Phase 2.
