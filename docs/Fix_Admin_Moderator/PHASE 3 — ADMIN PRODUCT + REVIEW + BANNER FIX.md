PHASE 3 — ADMIN PRODUCT + REVIEW + BANNER FIX

0. IMPLEMENTATION INSTRUCTION

Phase 3 sửa 3 module Admin đang lỗi:

Admin Product — không ẩn/xóa/xem chi tiết.
Admin Review — không xóa đánh giá vi phạm.
Admin Banner — CRUD + view + publish đều lỗi.

Dựa trực tiếp trên:

Source code hiện tại (AdminProductController 175 lines, AdminReviewController 198 lines, AdminBannerController 260 lines).
Gate 0 — Source Audit + Scope Lock.
Phase 1 Audit Report.
PHASE 4 — ADMIN SHOP KYC CATEGORY PRODUCT.md (đã có).
PHASE 5 — ADMIN VOUCHER BANNER.md (đã có).
PHASE 6 — ADMIN ORDER REVIEW VIOLATION ESCALATION.md (đã có).
User report: "chỉ xem được chứ không xóa với ẩn sản phẩm đi được", "không xóa đánh giá vi phạm được", "các chức năng trong bảng thao tác đều không hoạt động như xem chi tiết ,sửa, publish, xóa, tạo banner đều không hoạt động đúng cách".

CURRENT SOURCE
        ↓
PRODUCT FIX CONTRACT
        ↓
REVIEW FIX CONTRACT
        ↓
BANNER FIX CONTRACT
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
PHASE 3 READY

1. MỤC TIÊU

Phase 3 phải fix được:

Product: list / detail / hide / unhide / delete phải lưu MongoDB, redirect flash, hiển thị transition ProductStatus.
Review: list / detail / delete / hide / restore phải lưu MongoDB. Đặc biệt xóa review vi phạm phải hoạt động.
Banner: list / create / detail / edit / publish / unpublish / delete phải lưu MongoDB. Banner ở customer home phải sync.

2. KẾT QUẢ ĐỐI CHIẾU SOURCE

Theo source audit:

Product:
AdminProductController.java (175 lines) đã có 5 methods (Phase 1 audit §1.1):
- list
- detail
- hide
- unhide
- delete
product-list.html, product-detail.html đã có.

User report: "xem được nhưng không xóa/ẩn được" → nghi vấn:
- Form action trong product-detail.html có thể sai URL hoặc không có form.
- AdminProductServiceImpl.hide/unhide/delete có thể không persist.

Review:
AdminReviewController.java (198 lines) đã có 5 methods:
- list
- detail
- delete
- hide
- restore
review-list.html, review-detail.html đã có.

User report: "không xóa đánh giá vi phạm được" → nghi vấn:
- Review delete endpoint không hoạt động do URL mismatch.
- Template không có button delete.

Banner:
AdminBannerController.java (260 lines) đã có 8 methods:
- list
- create
- detail
- edit
- publish
- unpublish
- delete
(Tổng 8 theo Phase 1 audit §1.1)
banner-list.html, banner-detail.html, banner-form.html đã có.

User report: "view detail, edit, publish, delete, create đều không hoạt động" → nghi vấn:
- Nhiều bug liên quan.
- BannerServiceImpl transition có thể không persist.
- Template form có thể không bind đúng Banner document.

3. NGUYÊN TẮC BẮT BUỘC — KHÔNG XÓA MONGODB

Tuyệt đối KHÔNG:
deleteAll() / deleteMany({}) / drop() / dropDatabase()

4. KHÔNG FAKE

Không fake transition chỉ để UI đẹp.

5. SOURCE OF TRUTH

CURRENT SOURCE > USER REPORT > PHASE 1 AUDIT > ADMIN_PHASE docs

6. DISCOVERY RULE

SEARCH → EXISTS? → INSPECT → REUSE/FIX → CREATE

7. PHẠM VI PHASE 3

| Module | Hành động | File chính |
|---|---|---|
| Product | Verify 5 methods persist | AdminProductController.java + AdminProductServiceImpl.java |
| Product | Verify form action | product-list.html + product-detail.html |
| Product | Flash UI | product-list.html (thiếu theo Phase 1 §4.3) |
| Review | Verify 5 methods persist | AdminReviewController.java + AdminReviewServiceImpl.java |
| Review | Form action delete | review-detail.html |
| Review | Flash UI | review-list.html (thiếu theo Phase 1 §4.3) |
| Banner | Verify 8 methods persist | AdminBannerController.java + AdminBannerServiceImpl.java |
| Banner | Form action view/edit/publish/delete | banner-list.html + banner-detail.html + banner-form.html |
| Banner | Flash UI | banner-list.html (thiếu theo Phase 1 §4.3) |

8. PRODUCT FIX CONTRACT

8.1 — PRODUCT DOCUMENT

Đọc Product.java. Fields đã có:
- id
- shopId
- name
- description
- price
- quantity
- status (ProductStatus: DRAFT / ACTIVE / OUT_OF_STOCK / HIDDEN theo Phase 2A §10)
- ...

⚠️ ProductLifecycle ≠ ModerationState (theo Phase 2A §10):
- Nếu Product.lifecycle (status) cho Product moderation chung, Phase 3 fix dựa trên đó.
- Nếu cần moderationStatus riêng → fix trong Phase 4 hoặc Phase 9.

Phase 3 chỉ fix Admin CRUD: hide / unhide / delete dựa trên status hiện tại.

8.2 — PRODUCT CONTROLLER

```java
@GetMapping("/admin/products")
public String list(@RequestParam(required=false) String status, Model model) {
    List<Product> products;
    if (status != null && !status.isEmpty()) {
        products = productRepository.findByStatus(ProductStatus.valueOf(status));
    } else {
        products = productRepository.findAll();
    }
    model.addAttribute("products", products);
    return "admin/product-list";
}

@GetMapping("/admin/products/{id}")
public String detail(@PathVariable String id, Model model) {
    Product p = productRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm không tồn tại"));
    model.addAttribute("product", p);
    return "admin/product-detail";
}

@PostMapping("/admin/products/{id}/hide")
public String hide(@PathVariable String id, RedirectAttributes ra) {
    Product p = adminProductService.hide(id);
    ra.addFlashAttribute("flashSuccess", "Đã ẩn sản phẩm: " + p.getName());
    return "redirect:/admin/products";
}

@PostMapping("/admin/products/{id}/unhide")
public String unhide(@PathVariable String id, RedirectAttributes ra) {
    Product p = adminProductService.unhide(id);
    ra.addFlashAttribute("flashSuccess", "Đã hiện sản phẩm: " + p.getName());
    return "redirect:/admin/products";
}

@PostMapping("/admin/products/{id}/delete")
public String delete(@PathVariable String id, RedirectAttributes ra) {
    adminProductService.delete(id);
    ra.addFlashAttribute("flashSuccess", "Đã xóa sản phẩm");
    return "redirect:/admin/products";
}
```

8.3 — PRODUCT SERVICE

```java
public Product hide(String id) {
    Product p = productRepository.findById(id).orElseThrow(...);
    p.setStatus(ProductStatus.HIDDEN);
    p.setUpdatedAt(LocalDateTime.now());
    return productRepository.save(p);
}

public Product unhide(String id) {
    Product p = productRepository.findById(id).orElseThrow(...);
    p.setStatus(ProductStatus.ACTIVE);
    p.setUpdatedAt(LocalDateTime.now());
    return productRepository.save(p);
}

public void delete(String id) {
    productRepository.deleteById(id);
}
```

8.4 — PRODUCT TEMPLATE

product-list.html (thiếu flash UI theo Phase 1 audit §4.3 bug #2):

```html
<div th:if="${flashSuccess}" class="alert alert-success" th:text="${flashSuccess}"></div>
<div th:if="${flashError}" class="alert alert-danger" th:text="${flashError}"></div>
```

product-detail.html (cần form action):

```html
<form th:action="@{'/admin/products/' + ${product.id} + '/hide'}" method="post">
    <button type="submit" class="btn btn-warning">Ẩn</button>
</form>
<form th:action="@{'/admin/products/' + ${product.id} + '/unhide'}" method="post">
    <button type="submit" class="btn btn-success">Hiện</button>
</form>
<form th:action="@{'/admin/products/' + ${product.id} + '/delete'}" method="post"
      onsubmit="return confirm('Xóa sản phẩm này?')">
    <button type="submit" class="btn btn-danger">Xóa</button>
</form>
```

9. REVIEW FIX CONTRACT

9.1 — REVIEW DOCUMENT

Đọc Review.java. Fields:
- id
- productId
- userId
- rating
- comment
- status (active / hidden / deleted)
- createdAt

Theo Phase 2A §14, Review moderation state chưa đầy đủ. Phase 3 chỉ fix:
- admin delete (set status=DELETED hoặc xóa cứng — xem AdminReviewServiceImpl hiện tại)
- hide (status=HIDDEN)
- restore (status=ACTIVE)

9.2 — REVIEW CONTROLLER

```java
@PostMapping("/admin/reviews/{id}/delete")
public String delete(@PathVariable String id, RedirectAttributes ra) {
    adminReviewService.delete(id);
    ra.addFlashAttribute("flashSuccess", "Đã xóa đánh giá vi phạm");
    return "redirect:/admin/reviews";
}

@PostMapping("/admin/reviews/{id}/hide")
public String hide(@PathVariable String id, RedirectAttributes ra) {
    Review r = adminReviewService.hide(id);
    ra.addFlashAttribute("flashSuccess", "Đã ẩn đánh giá");
    return "redirect:/admin/reviews";
}

@PostMapping("/admin/reviews/{id}/restore")
public String restore(@PathVariable String id, RedirectAttributes ra) {
    Review r = adminReviewService.restore(id);
    ra.addFlashAttribute("flashSuccess", "Đã khôi phục đánh giá");
    return "redirect:/admin/reviews";
}
```

9.3 — REVIEW SERVICE

```java
public void delete(String id) {
    reviewRepository.deleteById(id);
}

public Review hide(String id) {
    Review r = reviewRepository.findById(id).orElseThrow(...);
    r.setStatus(ReviewStatus.HIDDEN);
    return reviewRepository.save(r);
}

public Review restore(String id) {
    Review r = reviewRepository.findById(id).orElseThrow(...);
    r.setStatus(ReviewStatus.ACTIVE);
    return reviewRepository.save(r);
}
```

9.4 — REVIEW TEMPLATE

review-list.html (thiếu flash UI theo Phase 1 §4.3 bug #2):

```html
<div th:if="${flashSuccess}" class="alert alert-success" th:text="${flashSuccess}"></div>
<div th:if="${flashError}" class="alert alert-danger" th:text="${flashError}"></div>
```

review-detail.html (cần form delete):

```html
<form th:action="@{'/admin/reviews/' + ${review.id} + '/delete'}" method="post"
      onsubmit="return confirm('Xóa đánh giá này?')">
    <button type="submit" class="btn btn-danger">Xóa vĩnh viễn</button>
</form>
```

10. BANNER FIX CONTRACT

10.1 — BANNER DOCUMENT

Đọc Banner.java. Fields:
- id
- title
- imageUrl
- linkUrl
- position (TOP, MIDDLE, BOTTOM, ...)
- priority (int)
- startDate
- endDate
- status (DRAFT / ACTIVE / INACTIVE)
- createdAt
- updatedAt

10.2 — BANNER CONTROLLER

⚠️ 8 methods theo Phase 1 audit §1.1.

```java
@GetMapping("/admin/banners") → list
@PostMapping("/admin/banners/create") → create
@GetMapping("/admin/banners/{id}") → detail
@PostMapping("/admin/banners/{id}/edit") → edit
@PostMapping("/admin/banners/{id}/publish") → publish (status=ACTIVE)
@PostMapping("/admin/banners/{id}/unpublish") → unpublish (status=INACTIVE)
@PostMapping("/admin/banners/{id}/delete") → delete
```

10.3 — BANNER SERVICE

```java
public Banner create(Banner b) {
    b.setId(null);
    if (b.getStatus() == null) b.setStatus(BannerStatus.DRAFT);
    b.setCreatedAt(LocalDateTime.now());
    return bannerRepository.save(b);
}

public Banner update(String id, Banner b) {
    Banner existing = bannerRepository.findById(id).orElseThrow(...);
    existing.setTitle(b.getTitle());
    existing.setImageUrl(b.getImageUrl());
    existing.setLinkUrl(b.getLinkUrl());
    existing.setPosition(b.getPosition());
    existing.setPriority(b.getPriority());
    existing.setStartDate(b.getStartDate());
    existing.setEndDate(b.getEndDate());
    existing.setUpdatedAt(LocalDateTime.now());
    return bannerRepository.save(existing);
}

public Banner publish(String id) {
    Banner b = bannerRepository.findById(id).orElseThrow(...);
    b.setStatus(BannerStatus.ACTIVE);
    b.setUpdatedAt(LocalDateTime.now());
    return bannerRepository.save(b);
}

public Banner unpublish(String id) {
    Banner b = bannerRepository.findById(id).orElseThrow(...);
    b.setStatus(BannerStatus.INACTIVE);
    b.setUpdatedAt(LocalDateTime.now());
    return bannerRepository.save(b);
}

public void delete(String id) {
    bannerRepository.deleteById(id);
}
```

10.4 — BANNER TEMPLATE

banner-list.html (thiếu flash UI theo Phase 1 §4.3 bug #2):

```html
<div th:if="${flashSuccess}" class="alert alert-success" th:text="${flashSuccess}"></div>
<div th:if="${flashError}" class="alert alert-danger" th:text="${flashError}"></div>

<table>
    <thead>
        <tr><th>Tiêu đề</th><th>Vị trí</th><th>Ưu tiên</th><th>Trạng thái</th><th>Thao tác</th></tr>
    </thead>
    <tbody>
        <tr th:each="banner : ${banners}">
            <td th:text="${banner.title}"></td>
            <td th:text="${banner.position}"></td>
            <td th:text="${banner.priority}"></td>
            <td>
                <span th:if="${banner.status.name() == 'ACTIVE'}" class="badge bg-success">ACTIVE</span>
                <span th:if="${banner.status.name() == 'INACTIVE'}" class="badge bg-secondary">INACTIVE</span>
                <span th:if="${banner.status.name() == 'DRAFT'}" class="badge bg-warning">DRAFT</span>
            </td>
            <td>
                <a th:href="@{'/admin/banners/' + ${banner.id}}" class="btn btn-sm btn-info">Xem</a>
                <a th:href="@{'/admin/banners/' + ${banner.id} + '/edit'}" class="btn btn-sm btn-primary">Sửa</a>
                <form th:action="@{'/admin/banners/' + ${banner.id} + '/publish'}" method="post" style="display:inline">
                    <button type="submit" class="btn btn-sm btn-success">Publish</button>
                </form>
                <form th:action="@{'/admin/banners/' + ${banner.id} + '/unpublish'}" method="post" style="display:inline">
                    <button type="submit" class="btn btn-sm btn-warning">Unpublish</button>
                </form>
                <form th:action="@{'/admin/banners/' + ${banner.id} + '/delete'}" method="post" style="display:inline"
                      onsubmit="return confirm('Xóa banner này?')">
                    <button type="submit" class="btn btn-sm btn-danger">Xóa</button>
                </form>
            </td>
        </tr>
    </tbody>
</table>

<a href="/admin/banners/create" class="btn btn-primary">Tạo banner</a>
```

banner-detail.html:

```html
<div th:if="${flashSuccess}" class="alert alert-success" th:text="${flashSuccess}"></div>

<h2 th:text="${banner.title}"></h2>
<p>Position: <span th:text="${banner.position}"></span></p>
<p>Image: <img th:src="${banner.imageUrl}" alt="banner"/></p>
<p>Trạng thái: <span th:text="${banner.status}"></span></p>

<a th:href="@{'/admin/banners/' + ${banner.id} + '/edit'}">Sửa</a>
```

banner-form.html (dùng chung cho create + edit):

```html
<div th:if="${flashError}" class="alert alert-danger" th:text="${flashError}"></div>

<form th:action="${banner.id == null ? '/admin/banners/create' : '/admin/banners/' + banner.id + '/edit'}"
      th:object="${banner}" method="post" enctype="multipart/form-data">
    <input type="text" th:field="*{title}" required/>
    <input type="url" th:field="*{linkUrl}"/>
    <input type="text" th:field="*{imageUrl}" required/>
    <select th:field="*{position}">
        <option value="TOP">Trên</option>
        <option value="MIDDLE">Giữa</option>
        <option value="BOTTOM">Dưới</option>
    </select>
    <input type="number" th:field="*{priority}" min="0"/>
    <input type="datetime-local" th:field="*{startDate}"/>
    <input type="datetime-local" th:field="*{endDate}"/>
    <button type="submit">Lưu</button>
</form>
```

11. PRODUCT + REVIEW + BANNER TEST

```java
@Test void hideProduct_validId_statusSetHIDDEN() { ... }
@Test void unhideProduct_validId_statusSetACTIVE() { ... }
@Test void deleteProduct_validId_removedFromMongoDB() { ... }

@Test void deleteReview_validId_removedFromMongoDB() { ... }
@Test void hideReview_validId_statusSetHIDDEN() { ... }
@Test void restoreReview_validId_statusSetACTIVE() { ... }

@Test void createBanner_validInput_persistedToMongoDB() { ... }
@Test void publishBanner_validId_statusSetACTIVE() { ... }
@Test void unpublishBanner_validId_statusSetINACTIVE() { ... }
@Test void deleteBanner_validId_removedFromMongoDB() { ... }
@Test void createBanner_invalidDateRange_throwValidation() { ... }
```

12. PRODUCT + REVIEW + BANNER REGRESSION

Verify không hỏng:
- Customer product list/detail (public)
- Vendor product CRUD
- Customer review create
- Customer home page (banner sync)
- Vendor order management

13. PRODUCT + REVIEW + BANNER SECURITY

Đã có theo Phase 2:
/admin/products/**  ADMIN + MODERATOR
/admin/reviews/**   ADMIN + MODERATOR
/admin/banners/**   ADMIN-only

14. EXISTING DATA

Sau Phase 3:
- products: PRESERVED
- reviews: PRESERVED
- banners: PRESERVED

15. MONGODB SAFETY

Không drop collection.
Không deleteMany.

16. TEST DATA

Nếu cần:
[TEST-PHASE-3]
- 1 Product status=ACTIVE → test hide
- 1 Review → test delete
- 1 Banner → test publish

Không xóa sau test.

17. KHÔNG LÀM MẤT CHỨC NĂNG HIỆN TẠI

Không được làm hỏng:
- Customer /products
- Vendor product CRUD
- Moderator product queue (Phase 4 mới fix)
- Customer home banner display

18. FILE CHANGE REPORT

Files Modified:
- src/main/java/com/ecommerce/cnj70/controller/admin/AdminProductController.java
- src/main/java/com/ecommerce/cnj70/service/impl/AdminProductServiceImpl.java (nếu thiếu)
- src/main/resources/templates/admin/product-list.html (flash UI)
- src/main/resources/templates/admin/product-detail.html (form action)
- src/main/java/com/ecommerce/cnj70/controller/admin/AdminReviewController.java
- src/main/java/com/ecommerce/cnj70/service/impl/AdminReviewServiceImpl.java (nếu thiếu)
- src/main/resources/templates/admin/review-list.html (flash UI)
- src/main/resources/templates/admin/review-detail.html (form action)
- src/main/java/com/ecommerce/cnj70/controller/admin/AdminBannerController.java
- src/main/java/com/ecommerce/cnj70/service/impl/AdminBannerServiceImpl.java (nếu thiếu)
- src/main/resources/templates/admin/banner-list.html (flash UI + form actions)
- src/main/resources/templates/admin/banner-detail.html (binding đúng)
- src/main/resources/templates/admin/banner-form.html (form binding)

Files Created:
- src/test/java/com/ecommerce/cnj70/fix/phase3/ProductFixTest.java
- src/test/java/com/ecommerce/cnj70/fix/phase3/ReviewFixTest.java
- src/test/java/com/commerce/cnj70/fix/phase3/BannerFixTest.java

Files Deleted:
NONE

19. DATA CHANGE REPORT

MongoDB:
Existing data deleted: NONE
Database reset: NO
Collection dropped: NO

Test records inserted (nếu cần):
- 1 Product test
- 1 Review test
- 1 Banner test

Test records deleted:
NO

20. BUILD REPORT

Command: mvn clean package -DskipTests
Result: PASS / FAIL

21. MANUAL TEST REPORT

Test 1 — Product:
G1: Login ADMIN
G2: GET /admin/products → list
G3: Click vào product → /admin/products/{id} → chi tiết
G4: Bấm "Ẩn" → status HIDDEN, flashSuccess
G5: Bấm "Hiện" → status ACTIVE
G6: Bấm "Xóa" → confirm → redirect flashSuccess, product biến mất

Test 2 — Review:
H1: Login ADMIN
H2: GET /admin/reviews → list
H3: Click review vi phạm → /admin/reviews/{id}
H4: Bấm "Xóa" → confirm → flashSuccess, review biến mất
H5: Verify MongoDB reviews collection

Test 3 — Banner:
I1: Login ADMIN
I2: GET /admin/banners → list hiển thị banner có nhãn trạng thái
I3: Bấm "Tạo banner" → /admin/banners/create → form
I4: Submit form → flashSuccess → list hiển thị banner mới
I5: Bấm "Publish" → status ACTIVE, banner hiển thị ngoài home customer
I6: Bấm "Sửa" → form binding đúng → save → flashSuccess
I7: Bấm "Unpublish" → status INACTIVE
I8: Bấm "Xóa" → confirm → banner biến mất

22. KNOWN ISSUES

Mỗi issue:
Issue: ...
Root Cause: ...
Phase 3 Status: ...

23. STATUS CLASSIFICATION

EXISTING / FIXED / PARTIAL / MISSING / WRONG / OUT OF SCOPE

24. ACCEPTANCE CRITERIA

Phase 3 PASS khi:
- Product hide/unhide/delete lưu MongoDB, flash UI
- Review delete/hide/restore lưu MongoDB
- Banner CRUD + publish + unpublish lưu MongoDB
- Banner sync với customer home
- Manual test 21.1, 21.2, 21.3 PASS
- Build PASS
- Existing flows không bị hỏng
- MongoDB data preserved

25. ĐIỀU KIỆN KẾT THÚC

Phase 3 PASS khi:
- 3 module Admin (Product, Review, Banner) đã hoạt động đầy đủ
- 8 methods banner persist MongoDB
- Banner đồng bộ với customer home
- Manual test PASS
- Build PASS
- MongoDB preserved
