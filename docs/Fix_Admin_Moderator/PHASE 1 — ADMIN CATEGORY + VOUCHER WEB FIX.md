PHASE 1 — ADMIN CATEGORY + VOUCHER WEB FIX

0. IMPLEMENTATION INSTRUCTION

Phase 1 sửa 2 module Admin đang lỗi:

Admin Category — nút sửa/xóa không hoạt động (4 chỗ addAttribute → addFlashAttribute theo Phase 1 audit §4.3 bug #1).
Admin Voucher WEB — toàn bộ CRUD không lưu / không hiển thị (theo user report).

Dựa trực tiếp trên:

Source code hiện tại (AdminCategoryController, VoucherController, các template tương ứng).
Gate 0 — Source Audit + Scope Lock.
Phase 1 Audit Report (docs/REPORT/PHASE-1-AUDIT.md).
Phase 2 Security Report.

Phase 1 không coi audit report cũ là bằng chứng implementation đã đúng.

CURRENT SOURCE
        ↓
DOMAIN GAP
        ↓
FIX CONTRACT
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
PHASE 1 READY

1. MỤC TIÊU

Phase 1 phải fix được:

Category: thêm/sửa/xóa category phải thực sự lưu vào MongoDB `categories`, redirect kèm flashSuccess, hiển thị lại sau reload.
Voucher WEB: tạo/sửa/xóa/kích hoạt/khóa voucher WEB phải lưu MongoDB `vouchers` với type=WEB, hiển thị đầy đủ trên list, filter đúng (type=WEB only).

2. KẾT QUẢ ĐỐI CHIẾU SOURCE

Theo source audit:

Category:
AdminCategoryController.java (189 lines) đã có 5 methods (list, create, update, delete, ?)
AdminCategoryServiceImpl.java (đã có) — đã có save(), deleteById()
category-manage.html (đã có) — đã có form action
Template đã có thẻ `<th:block th:if="${flashSuccess}">`? — Phase 1 audit cho biết THIẾU

Đã rõ:
Bug #1 (Phase 1 audit §4.3): `redirectAttributes.addAttribute()` 4 chỗ thay vì `addFlashAttribute()` → URL rối, flash message không hiển thị.
Bug #2 (Phase 1 audit §4.3): category-manage.html thiếu flash UI div.

Voucher WEB:
VoucherController.java đã có admin endpoints (line 233-455 theo Phase 1 audit).
VoucherServiceImpl.java đã có create/update/activate/deactivate.
voucher-list.html (267 lines), voucher-create.html (291 lines), voucher-edit.html (252 lines) đã có.

Đã rõ:
User report: "tạo voucher rồi lưu ra mất dữ liệu, không hiển thị dữ liệu" → nghi vấn template form không truyền `type=WEB`, hoặc Voucher document không có field `type`.
Phase 1 audit: AdminVoucherController MISSING → controller chung `VoucherController` đang xử lý cả admin + customer, có nguy cơ trộn logic.

3. NGUYÊN TẮC BẮT BUỘC — KHÔNG XÓA MONGODB

TUYỆT ĐỐI KHÔNG:

deleteAll()
deleteMany({})
drop()
dropDatabase()
collection reset

4. KHÔNG FAKE PRODUCTION DATA

Không tạo category giả / voucher giả chỉ để UI đẹp.

Không thêm `type=WEB` mặc định để "fix nhanh" — phải đúng template form.

Nếu cần test data:

TEST-PHASE-1

và phải báo rõ số lượng.

Không xóa dữ liệu test sau đó.

5. KHÔNG PHỤ THUỘC DEVELOPER KHÁC

Phase 1 phải triển khai độc lập. Nếu thiếu:

DEFINE CONTRACT
      ↓
FIX OWNED FOUNDATION
      ↓
INTEGRATION-READY

6. KHÔNG THAY ĐỔI GIT STATE

Không commit / push / merge / checkout / stash.

7. SOURCE OF TRUTH

CURRENT SOURCE CODE > PHASE 1 AUDIT > USER REPORT

8. DISCOVERY RULE

Trước khi tạo file mới:

SEARCH → EXISTS? → INSPECT → REUSE/FIX → (NO) CREATE

9. PHẠM VI PHASE 1

| Module | Hành động | File chính |
|---|---|---|
| Category | Fix addAttribute → addFlashAttribute (4 chỗ) | AdminCategoryController.java |
| Category | Thêm flash UI div | category-manage.html |
| Category | Verify AdminCategoryServiceImpl lưu MongoDB | AdminCategoryServiceImpl.java |
| Voucher WEB | Fix form binding type=WEB | voucher-create.html + voucher-edit.html |
| Voucher WEB | Verify VoucherServiceImpl.create() có set type=WEB | VoucherServiceImpl.java |
| Voucher WEB | Verify list filter type=WEB | VoucherController.java (admin endpoint) |
| Voucher WEB | Verify thêm flashSuccess sau save | voucher-list.html |
| Voucher WEB | Cân nhắc tách AdminVoucherController | (nếu cần) |

10. CATEGORY FIX CONTRACT

10.1 — ADMIN CATEGORY CONTROLLER

Trước khi fix, đọc AdminCategoryController.java dòng 71-79, 116-120, 136-140, 159-167.

Bug cụ thể:

```java
// ❌ SAI (4 chỗ)
redirectAttributes.addAttribute("flashSuccess", "Thành công");
redirectAttributes.addAttribute("flashError", "Lỗi");

// ✅ ĐÚNG
redirectAttributes.addFlashAttribute("flashSuccess", "Thành công");
redirectAttributes.addFlashAttribute("flashError", "Lỗi");
```

Sau khi sửa, URL sẽ sạch, flash attribute lưu trong session, hiển thị 1 lần sau redirect.

10.2 — CATEGORY TEMPLATE

category-manage.html phải có:

```html
<div th:if="${flashSuccess}" class="alert alert-success" th:text="${flashSuccess}"></div>
<div th:if="${flashError}" class="alert alert-danger" th:text="${flashError}"></div>
```

10.3 — CATEGORY SERVICE PERSISTENCE

Xác minh AdminCategoryServiceImpl có method `save(Category)` gọi `categoryRepository.save()`.
Có method `deleteById(String)` gọi `categoryRepository.deleteById()`.
Không có hard-coded mock data.

10.4 — TEST CATEGORY

```java
@Test void createCategory_validInput_persistedToMongoDB() { ... }
@Test void updateCategory_validInput_changedInMongoDB() { ... }
@Test void deleteCategory_validId_removedFromMongoDB() { ... }
@Test void listCategory_afterCreate_appearsInList() { ... }
@Test void createCategory_invalidInput_throwValidation() { ... }
```

11. VOUCHER WEB FIX CONTRACT

11.1 — VOUCHER DOCUMENT

Đọc Voucher.java — kiểm tra có field `type` (enum VoucherType.WEB / SHOP) không.
Nếu chưa có field `type`:

⚠️ KHÔNG tự ý thêm field nếu chưa chắc business rule.

Theo audit Phase 1 §1.3:
"Voucher | `vouchers` | OK (type=WEB/SHOP, used/quantity)"

→ Có field `type` rồi.

11.2 — VOUCHER CONTROLLER ADMIN ENDPOINTS

Đọc VoucherController.java (line 233-455):

Endpoint POST /admin/vouchers/create → gọi service.create() set type=WEB
Endpoint POST /admin/vouchers/{id}/edit → gọi service.update()
Endpoint POST /admin/vouchers/{id}/delete → gọi service.delete()
Endpoint POST /admin/vouchers/{id}/activate → status=ACTIVE
Endpoint POST /admin/vouchers/{id}/deactivate → status=INACTIVE
GET /admin/vouchers → list filter type=WEB

11.3 — VOUCHER SERVICE CREATE

VoucherServiceImpl.create() phải:

```java
public Voucher create(Voucher v) {
    if (v.getType() == null) v.setType(VoucherType.WEB);  // ← ép type=WEB cho admin
    return voucherRepository.save(v);
}
```

⚠️ Không ép nếu admin có thể tạo SHOP voucher — nhưng theo user, "Voucher WEB" là admin-only WEB.

Theo Phase 6 doc đã có:

11.4 — VOUCHER TEMPLATE BIND

voucher-create.html:

```html
<form th:action="@{/admin/vouchers/create}" th:object="${voucher}" method="post">
    <input type="hidden" name="type" value="WEB"/>  <!-- ← field quan trọng -->
    <input type="text" th:field="*{code}" required/>
    <input type="number" th:field="*{discountValue}" required/>
    <input type="number" th:field="*{quantity}" required/>
    <input type="datetime-local" th:field="*{startDate}" required/>
    <input type="datetime-local" th:field="*{endDate}" required/>
    <select th:field="*{discountType}">
        <option value="PERCENT">Phần trăm</option>
        <option value="FIXED">Cố định</option>
    </select>
    <button type="submit">Tạo</button>
</form>
```

11.5 — VOUCHER LIST FILTER

VoucherController admin list:

```java
@GetMapping("/admin/vouchers")
public String list(Model model) {
    List<Voucher> webVouchers = voucherRepository.findByType(VoucherType.WEB);
    model.addAttribute("vouchers", webVouchers);
    return "admin/voucher-list";
}
```

11.6 — VOUCHER LIST FLASH UI

voucher-list.html (đã có theo Phase 1 audit):

```html
<div th:if="${flashSuccess}" class="alert alert-success" th:text="${flashSuccess}"></div>
<div th:if="${flashError}" class="alert alert-danger" th:text="${flashError}"></div>
```

Phase 1 audit §4.1 cho biết voucher-list.html đã có flash UI. Verify lại.

11.7 — REPOSITORY

Nếu VoucherRepository chưa có method `findByType(VoucherType)`, thêm:

```java
List<Voucher> findByType(VoucherType type);
```

11.8 — TEST VOUCHER WEB

```java
@Test void createVoucher_webType_persistedToMongoDB() { ... }
@Test void createVoucher_formMissingTypeType_serviceAssignsWEB() { ... }
@Test void listVouchers_adminOnlyReturnsWEB() { ... }
@Test void activateVoucher_changesStatus() { ... }
@Test void deactivateVoucher_changesStatus() { ... }
@Test void deleteVoucher_removesFromMongoDB() { ... }
@Test void createVoucher_endDateInPast_throwValidation() { ... }
@Test void createVoucher_quantityZero_throwValidation() { ... }
```

12. SCHEMA EVOLUTION

Nếu cần thêm field vào Voucher document:

Phải backward-compatible.

Document cũ vẫn đọc được.

Không yêu cầu reset MongoDB.

13. SECURITY

Giữ nguyên Phase 2:

/admin/vouchers/**   ADMIN-only (Phase 2 đã verify)
ADMIN+MODERATOR không còn truy cập /admin/vouchers/** (sửa ở session trước).

14. EXISTING DATA REGRESSION

Sau Phase 1 phải đảm bảo dữ liệu hiện tại còn nguyên:

categories
vouchers
users
shops
products
orders

Không bị xóa.

15. MONGODB SAFETY

Tuyệt đối KHÔNG drop collection.

Migration (nếu cần thêm field): NON-DESTRUCTIVE.

16. TEST DATA

Nếu cần test Phase 1:

[TEST-PHASE-1]

Chỉ insert lượng nhỏ:

1 Category "TEST_PHASE_1"
1 Voucher "TESTWEB10", type=WEB

Không tạo hàng loạt.

Không xóa sau test.

17. KHÔNG FAKE

Không tạo category / voucher chỉ để dashboard đẹp.

18. KHÔNG LÀM MẤT CHỨC NĂNG HIỆN TẠI

Không được làm hỏng:

Authentication
Authorization
Admin Voucher WEB hiện có (nếu đang chạy)
Customer /vouchers list (Public route — Phase 2 đã verify)
Admin các module khác (User/Shop/KYC/Product/Order/Review/Banner)

19. FILE CHANGE REPORT

Cuối Phase 1:

Files Modified:

- src/main/java/com/ecommerce/cnj70/controller/admin/AdminCategoryController.java
- src/main/resources/templates/admin/category-manage.html
- (nếu cần) src/main/java/com/ecommerce/cnj70/controller/admin/VoucherController.java
- src/main/resources/templates/admin/voucher-create.html
- src/main/resources/templates/admin/voucher-edit.html
- src/main/resources/templates/admin/voucher-list.html
- (nếu cần) src/main/java/com/ecommerce/cnj70/document/Voucher.java
- (nếu cần) src/main/java/com/ecommerce/cnj70/repository/VoucherRepository.java

Files Created:

- src/test/java/com/ecommerce/cnj70/fix/phase1/CategoryFixTest.java
- src/test/java/com/ecommerce/cnj70/fix/phase1/VoucherWebFixTest.java

Files Deleted:
NONE

20. DATA CHANGE REPORT

MongoDB:
Existing data deleted: NONE
Database reset: NO
Collection dropped: NO
deleteAll: NO
deleteMany({}): NO

Test records inserted:
1 Category TEST_PHASE_1
1 Voucher TESTWEB10 type=WEB

Test records deleted:
NO

Existing data preserved:
YES / NO / NOT VERIFIED

21. BUILD REPORT

Command:
mvn clean package -DskipTests

Result:
PASS / FAIL

Nếu fail:

Root Cause: ...
Affected File: ...
Dependency: ...
Status: ...

22. MANUAL TEST REPORT

Test case 1 — Category CRUD:
B1: Login ADMIN
B2: GET /admin/categories → hiển thị list
B3: POST /admin/categories/create (name="NewCat") → redirect có flashSuccess
B4: GET /admin/categories → NewCat xuất hiện trong list
B5: POST /admin/categories/{id}/edit (name="NewCat2") → redirect có flashSuccess
B6: GET /admin/categories → NewCat2 (không phải NewCat)
B7: POST /admin/categories/{id}/delete → redirect có flashSuccess
B8: GET /admin/categories → category đã xóa
B9: MongoDB categories collection: kiểm tra thực sự lưu/sửa/xóa

Test case 2 — Voucher WEB CRUD:
C1: Login ADMIN
C2: GET /admin/vouchers → chỉ hiển thị voucher type=WEB
C3: POST /admin/vouchers/create (code="NEWVOUCHER", type=WEB) → redirect có flashSuccess
C4: GET /admin/vouchers → NEWVOUCHER xuất hiện, type=WEB
C5: POST /admin/vouchers/{id}/edit → redirect flashSuccess
C6: POST /admin/vouchers/{id}/activate → status=ACTIVE
C7: POST /admin/vouchers/{id}/deactivate → status=INACTIVE
C8: POST /admin/vouchers/{id}/delete → redirect flashSuccess
C9: GET /admin/vouchers → NEWVOUCHER không còn
C10: MongoDB vouchers collection: verify

Test case 3 — Customer side regression:
D1: GET /vouchers → vẫn hiển thị public vouchers
D2: Public voucher WEB vẫn hiển thị đúng (số lượng, ngày)

23. KNOWN ISSUES

Mỗi issue:

Issue: ...
Current Behavior: ...
Expected Behavior: ...
Root Cause: ...
Source: ...
Severity: ...
Phase 1 Status: ...

24. STATUS CLASSIFICATION

EXISTING / MISSING / FIXED / PARTIAL / WRONG / OUT OF SCOPE

25. INTEGRATION STATUS

Integration Point: Voucher document

Existing Contract: Voucher (id, code, type, discountValue, startDate, endDate, quantity, used, status)

Phase 1 Implementation: ensure type=WEB for admin endpoints

External Implementation: VoucherServiceImpl

Status: INTEGRATED / INTEGRATION-READY / PARTIAL / MISSING

Notes: ...

26. ACCEPTANCE CRITERIA

Phase 1 hoàn thành khi:

Category: 4 chỗ addAttribute → addFlashAttribute đã sửa
Category: template có flash UI
Category: CRUD thực sự lưu MongoDB
Category: thêm 1 category mới, sửa, xóa → verify MongoDB thay đổi
Voucher WEB: form binding type=WEB
Voucher WEB: list filter type=WEB
Voucher WEB: CRUD lưu MongoDB thật
Voucher WEB: hiển thị đúng data (không mất sau save)
Customer side /vouchers không bị hỏng
MongoDB không xóa dữ liệu cũ
Build PASS
Manual test PASS
Automated test PASS

27. ĐIỀU KIỆN KẾT THÚC

Phase 1 PASS khi:

Category CRUD + flash message hoạt động
Voucher WEB CRUD + list filter + flash message hoạt động
Build PASS
MongoDB data preserved
Manual test 23.1, 23.2 PASS

Nếu partial:
- PARTIAL — chỉ fix được 1 module → ghi rõ module nào
