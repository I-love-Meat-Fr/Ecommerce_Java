PHASE 4 — MODERATOR HEADER + 6 QUEUES FIX

0. IMPLEMENTATION INSTRUCTION

Phase 4 sửa 1+6 module Moderator:

Moderator Header — lấy font + CSS từ Admin shell để thống nhất.
6 Queues — import 3-5 data mẫu, test toàn bộ, fix lỗi:
  - Product Queue
  - Shop Queue
  - Report Cases
  - KYC Queue
  - Violations
  - Moderator History

Dựa trực tiếp trên:

Source code hiện tại (ModeratorQueueController.java + templates/moderator/...).
Gate 0 — Source Audit + Scope Lock.
PHASE 2B — MODERATOR WORKFLOW.md (đã có trong docs/Fix_Web).
PHASE 6 — ADMIN ORDER REVIEW VIOLATION ESCALATION.md.
User report: "Phần header của trang kiểm duyệt của moderator thì phải lấy font css html của bên admin để giống nhau thống nhất".

CURRENT SOURCE
        ↓
MODERATOR HEADER CONTRACT
        ↓
PRODUCT QUEUE FIX
        ↓
SHOP QUEUE FIX (CHƯA CÓ NÚT DUYỆT SHOP — Phase 5 sẽ coupling với Admin)
        ↓
REPORT CASES FIX
        ↓
KYC QUEUE FIX
        ↓
VIOLATIONS FIX
        ↓
MODERATOR HISTORY FIX
        ↓
BUILD
        ↓
PHASE 4 READY

1. MỤC TIÊU

Phase 4 phải fix được:

Moderator header: lấy y nguyên admin-shell.html (font, CSS, navigation) → moderator-shell.html. MỌI template moderator phải include fragment này.
Product Queue: 3-5 data, test queue list + approve/reject/hide, fix nếu lỗi.
Shop Queue: 3-5 data, test queue list + view detail (nút duyệt shop — Phase 5 sẽ coupling).
Report Cases: 3-5 data, test mỗi action trong flow kiểm duyệt.
KYC Queue: 3-5 data, test list + approve/reject, fix nếu lỗi.
Violations: 3-5 data, test list + create + resolve.
Moderator History: 3-5 data, test list + filter + detail.

⚠️ Phân biệt:
- Phase 4 = fix existing + import data + test.
- Phase 5 = coupling chặt với Admin (moderator nút duyệt shop cùng logic Admin).

2. KẾT QUẢ ĐỐI CHIẾU SOURCE

Theo source audit:

Moderator files đã có:
ModeratorQueueController.java (class-level @PreAuthorize("hasRole('MODERATOR')"))
Templates trong src/main/resources/templates/moderator/... (cần liệt kê)
Header fragment có thể đã có trong templates/moderator/fragments/

User report: "Header moderator chưa thống nhất Admin"

Theo Phase 2B file (Fix_Web):
- Đã có moderator infrastructure foundation.
- Phase 4 này xây dựng trên đó.

3. NGUYÊN TẮC BẮT BUỘC — KHÔNG XÓA MONGODB

Tuyệt đối KHÔNG:
deleteAll() / deleteMany({}) / drop() / dropDatabase()

4. KHÔNG FAKE

Không fake product/review/shop/KYC/violation để test.

Dữ liệu import PHẢI đánh prefix: `[TEST-PHASE-4]` trong name/code để phân biệt.

Không xóa dữ liệu test.

5. SOURCE OF TRUTH

CURRENT SOURCE > USER REPORT > PHASE 2B (Fix_Web) > PHASE 1-8 AUDIT

6. DISCOVERY RULE

SEARCH → EXISTS? → INSPECT → REUSE/FIX → CREATE

7. PHẠM VI PHASE 4

| Module | Hành động | File chính |
|---|---|---|
| Header | Reuse admin-shell.html fragment | templates/admin/fragments/admin-shell.html + templates/moderator/fragments/moderator-shell.html (tạo mới nếu chưa có) |
| Product Queue | Import 3-5 data + test | ModeratorQueueController.java + templates/moderator/product-queue.html |
| Shop Queue | Import 3-5 data + test | templates/moderator/shop-queue.html (chưa có nút duyệt — Phase 5) |
| Report Cases | Import 3-5 data + test | templates/moderator/report-cases.html |
| KYC Queue | Import 3-5 data + test | templates/moderator/kyc-queue.html |
| Violations | Import 3-5 data + test | templates/moderator/violations.html |
| Moderator History | Import 3-5 data + test | templates/moderator/history.html |

8. MODERATOR HEADER CONTRACT

8.1 — SOURCE TRUTH

Tìm file fragment admin hiện tại:

src/main/resources/templates/admin/fragments/admin-shell.html (nếu có)
hoặc
src/main/resources/templates/admin/layout.html (nếu có)

Đọc để lấy CSS class:

- admin-shell wrapper
- admin-navbar
- admin-sidebar
- admin-content

8.2 — COPY HEADER PATTERN

Tạo (hoặc cập nhật):

src/main/resources/templates/moderator/fragments/moderator-shell.html

```html
<!DOCTYPE html>
<html lang="vi" xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>Moderator Panel</title>
    <!-- LẤY Y NGUYÊN CSS từ admin -->
    <link rel="stylesheet" th:href="@{/css/admin-shell.css}"/>
    <link rel="stylesheet" th:href="@{/css/moderator-extra.css}"/>
</head>
<body>
<div class="admin-shell">
    <!-- HEADER + NAVBAR -->
    <header class="admin-navbar">
        <a href="/moderator/dashboard" class="admin-logo">CNJ70 Moderator</a>
        <nav class="admin-nav">
            <a href="/moderator/product-queue">Product Queue</a>
            <a href="/moderator/shop-queue">Shop Queue</a>
            <a href="/moderator/report-cases">Report Cases</a>
            <a href="/moderator/kyc-queue">KYC Queue</a>
            <a href="/moderator/violations">Violations</a>
            <a href="/moderator/history">History</a>
            <form action="/auth/logout" method="post">
                <button type="submit">Đăng xuất</button>
            </form>
        </nav>
    </header>

    <div class="admin-content">
        <div th:if="${flashSuccess}" class="alert alert-success" th:text="${flashSuccess}"></div>
        <div th:if="${flashError}" class="alert alert-danger" th:text="${flashError}"></div>
        <th:block th:replace="~{::content}"></th:block>
    </div>
</div>
</body>
</html>
```

8.3 — APPLY CHO TỪNG TEMPLATE

Mỗi template moderator phải:

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<th:block th:replace="fragments/moderator-shell :: layout(~{::content})">
    <th:block th:fragment="content">
        <!-- NỘI DUNG RIÊNG -->
    </th:block>
</th:block>
</html>
```

8.4 — DEPENDENCY

moderator-extra.css có thể tạo mới (khác admin).

9. PRODUCT QUEUE CONTRACT

9.1 — IMPORT DATA

⚠️ KHÔNG xóa dữ liệu cũ. Insert thêm 3-5 Product với status=PENDING + flag TEST-PHASE-4:

```javascript
// Tạo qua Admin UI: /admin/products → create với status=PENDING
// Sau đó sửa status thành PENDING nếu cần

// Hoặc tạo thẳng qua Vendor UI: /vendor/products → create
```

Cách an toàn nhất: tạo qua UI Admin/Vendor.

Nếu cần script MongoDB:
```javascript
db.products.insertMany([
    { _id: "...", name: "[TEST-PHASE-4] Product 1", ..., status: "PENDING" },
    { _id: "...", name: "[TEST-PHASE-4] Product 2", ..., status: "PENDING" }
]);
```

⚠️ Theo QUY TẮC KHÔNG XÓA: KHÔNG drop, KHÔNG deleteMany. Insert thêm OK.

9.2 — PRODUCT QUEUE ENDPOINT

ModeratorQueueController đã có (theo source audit):

```java
@GetMapping("/moderator/product-queue")
public String productQueue(Model model) {
    List<Product> pending = productRepository.findByStatus(ProductStatus.PENDING);
    model.addAttribute("products", pending);
    return "moderator/product-queue";
}
```

9.3 — ACTIONS

Mỗi action phải persist + flash + redirect:

```java
@PostMapping("/moderator/product-queue/{id}/approve")
public String approve(@PathVariable String id, RedirectAttributes ra) {
    Product p = moderatorService.approveProduct(id);
    ra.addFlashAttribute("flashSuccess", "Đã duyệt: " + p.getName());
    return "redirect:/moderator/product-queue";
}

@PostMapping("/moderator/product-queue/{id}/reject")
public String reject(@PathVariable String id,
                     @RequestParam String reason,
                     RedirectAttributes ra) {
    Product p = moderatorService.rejectProduct(id, reason);
    ra.addFlashAttribute("flashSuccess", "Đã từ chối: " + p.getName());
    return "redirect:/moderator/product-queue";
}

@PostMapping("/moderator/product-queue/{id}/hide")
public String hide(@PathVariable String id, RedirectAttributes ra) {
    Product p = moderatorService.hideProduct(id);
    ra.addFlashAttribute("flashSuccess", "Đã ẩn");
    return "redirect:/moderator/product-queue";
}
```

10. SHOP QUEUE CONTRACT

⚠️ Phase 4 chỉ test list + view detail.
⚠️ Phase 5 mới coupling nút duyệt với Admin.

10.1 — IMPORT 3-5 SHOP DATA

```javascript
db.shops.insertMany([
    { _id: "...", name: "[TEST-PHASE-4] Shop 1", status: "PENDING", ... },
    ...
]);
```

10.2 — SHOP QUEUE ENDPOINT

```java
@GetMapping("/moderator/shop-queue")
public String shopQueue(Model model) {
    List<Shop> pending = shopRepository.findByStatus(ShopStatus.PENDING);
    model.addAttribute("shops", pending);
    return "moderator/shop-queue";
}

@GetMapping("/moderator/shop-queue/{id}")
public String shopDetail(@PathVariable String id, Model model) {
    Shop s = shopRepository.findById(id).orElseThrow(...);
    model.addAttribute("shop", s);
    return "moderator/shop-detail";  // ← có thể chưa có template, cần tạo
}
```

Template shop-detail dùng tạm admin/shop-detail.html (sửa URL action về admin path) cho Phase 4. Phase 5 sẽ tạo moderator có action riêng.

11. REPORT CASES CONTRACT

11.1 — REPORT CASE DOCUMENT

Đọc ReportCase.java. Fields:
- id
- reporterId
- resourceType (PRODUCT, REVIEW, SHOP)
- resourceId
- reason
- description
- status (OPEN, IN_REVIEW, RESOLVED, REJECTED)
- createdAt
- updatedAt

11.2 — IMPORT 3-5 DATA

```javascript
db.report_cases.insertMany([
    { _id: "...", reporterId: "...", resourceType: "PRODUCT", resourceId: "...", reason: "...", status: "OPEN", ... },
    ...
]);
```

⚠️ Trước khi insert, kiểm tra collection name. Theo source: `report_cases` hay `reportcases`? Đọc @Document annotation trong ReportCase.java.

Nếu chưa có collection → tạo thêm ở Phase 4 thông qua admin/moderator UI.

11.3 — REPORT CASES ENDPOINT

```java
@GetMapping("/moderator/report-cases")
public String reportCases(@RequestParam(required=false) String status, Model model) {
    List<ReportCase> cases;
    if (status != null) {
        cases = reportCaseRepository.findByStatus(ReportCaseStatus.valueOf(status));
    } else {
        cases = reportCaseRepository.findAll();
    }
    model.addAttribute("reportCases", cases);
    return "moderator/report-cases";
}

@PostMapping("/moderator/report-cases/{id}/claim")
public String claim(@PathVariable String id, RedirectAttributes ra) {
    ReportCase r = reportCaseService.claim(id);
    ra.addFlashAttribute("flashSuccess", "Đã nhận case");
    return "redirect:/moderator/report-cases/" + id;
}

@PostMapping("/moderator/report-cases/{id}/resolve")
public String resolve(@PathVariable String id,
                      @RequestParam String resolution,
                      RedirectAttributes ra) {
    ReportCase r = reportCaseService.resolve(id, resolution);
    ra.addFlashAttribute("flashSuccess", "Đã đóng case");
    return "redirect:/moderator/report-cases";
}

@PostMapping("/moderator/report-cases/{id}/reject")
public String reject(@PathVariable String id,
                     @RequestParam String reason,
                     RedirectAttributes ra) {
    ReportCase r = reportCaseService.reject(id, reason);
    ra.addFlashAttribute("flashSuccess", "Đã từ chối report");
    return "redirect:/moderator/report-cases";
}
```

12. KYC QUEUE CONTRACT

12.1 — IMPORT 3-5 DATA

```javascript
db.kyc_profiles.insertMany([
    { _id: "...", userId: "...", status: "PENDING", fullName: "[TEST-PHASE-4] Kyc 1", ... },
    ...
]);
```

12.2 — KYC QUEUE ENDPOINT

```java
@GetMapping("/moderator/kyc-queue")
public String kycQueue(Model model) {
    List<KycProfile> pending = kycProfileRepository.findByStatus(KycStatus.PENDING);
    model.addAttribute("kycProfiles", pending);
    return "moderator/kyc-queue";
}

@PostMapping("/moderator/kyc-queue/{id}/approve")
public String approveKyc(@PathVariable String id, RedirectAttributes ra) {
    KycProfile k = moderatorService.approveKyc(id);
    ra.addFlashAttribute("flashSuccess", "Đã duyệt KYC");
    return "redirect:/moderator/kyc-queue";
}

@PostMapping("/moderator/kyc-queue/{id}/reject")
public String rejectKyc(@PathVariable String id,
                        @RequestParam String reason,
                        RedirectAttributes ra) {
    KycProfile k = moderatorService.rejectKyc(id, reason);
    ra.addFlashAttribute("flashSuccess", "Đã từ chối KYC");
    return "redirect:/moderator/kyc-queue";
}
```

13. VIOLATIONS CONTRACT

13.1 — IMPORT 3-5 DATA

```javascript
db.violations.insertMany([
    { _id: "...", actorId: "...", code: "VIOLATION_001", severity: "WARNING", status: "PENDING", ... },
    ...
]);
```

13.2 — VIOLATIONS ENDPOINT

```java
@GetMapping("/moderator/violations")
public String violations(@RequestParam(required=false) String severity, Model model) {
    List<Violation> violations;
    if (severity != null) {
        violations = violationRepository.findBySeverity(ViolationSeverity.valueOf(severity));
    } else {
        violations = violationRepository.findAll();
    }
    model.addAttribute("violations", violations);
    return "moderator/violations";
}

@PostMapping("/moderator/violations/{id}/resolve")
public String resolveViolation(@PathVariable String id,
                               @RequestParam String note,
                               RedirectAttributes ra) {
    Violation v = moderatorService.resolveViolation(id, note);
    ra.addFlashAttribute("flashSuccess", "Đã xử lý violation");
    return "redirect:/moderator/violations";
}
```

14. MODERATOR HISTORY CONTRACT

14.1 — SOURCE

ModeratorHistory có thể là AuditLog filter theo actor có role=MODERATOR.

Theo Phase 2A §26:
"Nếu cần history riêng: ModerationHistory"

Nếu source chưa có ModerationHistory riêng → Phase 4 dùng AuditLog filter.

⚠️ Phase 2B đã có ModerationHistory contract? → Verify.

14.2 — IMPORT 3-5 DATA

AuditLog test:

```javascript
db.audit_logs.insertMany([
    { _id: "...", actorId: "...", actorRole: "MODERATOR", action: "PRODUCT_APPROVED", resourceType: "Product", resourceId: "...", timestamp: ..., metadata: {...} },
    ...
]);
```

14.3 — HISTORY ENDPOINT

```java
@GetMapping("/moderator/history")
public String history(@RequestParam(required=false) String action, Model model) {
    List<AuditLog> history;
    if (action != null) {
        history = auditLogRepository.findByActionOrderByTimestampDesc(action);
    } else {
        // Filter by actor role MODERATOR
        history = auditLogRepository.findByActorRoleOrderByTimestampDesc("MODERATOR");
    }
    model.addAttribute("history", history);
    return "moderator/history";
}

@GetMapping("/moderator/history/{id}")
public String historyDetail(@PathVariable String id, Model model) {
    AuditLog log = auditLogRepository.findById(id).orElseThrow(...);
    model.addAttribute("log", log);
    return "moderator/history-detail";
}
```

15. 6 QUEUES TEST

```java
@Test void productQueue_pendingProducts_listedCorrectly() { ... }
@Test void approveProduct_validId_statusSetACTIVE() { ... }
@Test void rejectProduct_validReason_statusSetREJECTED() { ... }
@Test void hideProduct_validId_statusSetHIDDEN() { ... }

@Test void shopQueue_pendingShops_listedCorrectly() { ... }

@Test void reportCases_open_listedCorrectly() { ... }
@Test void claimReportCase_validId_statusSetIN_REVIEW() { ... }
@Test void resolveReportCase_validId_statusSetRESOLVED() { ... }
@Test void rejectReportCase_validReason_statusSetREJECTED() { ... }

@Test void kycQueue_pending_listedCorrectly() { ... }
@Test void approveKyc_validId_statusSetAPPROVED() { ... }
@Test void rejectKyc_validReason_statusSetREJECTED() { ... }

@Test void violations_listedCorrectly() { ... }
@Test void resolveViolation_validId_statusSetRESOLVED() { ... }

@Test void history_listedCorrectly() { ... }
@Test void history_filterByAction_returnsCorrect() { ... }
```

16. 6 QUEUES REGRESSION

Verify không hỏng:
- Admin vẫn thấy queue liên quan (vd Admin Review list)
- Vendor không bị ảnh hưởng (KYC reject có thể block vendor, cẩn thận)
- Customer không bị ảnh hưởng (report của customer phải hiển thị ở report-cases moderator)

17. 6 QUEUES SECURITY

Đã có theo Phase 2:
@PreAuthorize("hasRole('MODERATOR')") ở class-level — KHÔNG touch.

18. EXISTING DATA

Sau Phase 4:
- products: PRESERVED + thêm 3-5 test
- shops: PRESERVED + thêm 3-5 test
- kyc_profiles: PRESERVED + thêm 3-5 test
- violations: PRESERVED + thêm 3-5 test
- report_cases: PRESERVED + thêm 3-5 test
- audit_logs: PRESERVED + thêm 3-5 test

19. MONGODB SAFETY

Không drop collection.
Không deleteMany.
Insert thêm OK nếu đánh dấu [TEST-PHASE-4].

20. TEST DATA (3-5 MỖI LOẠI)

| Collection | Count | Prefix |
|---|---|---|
| products | 3-5 | [TEST-PHASE-4] |
| shops | 3-5 | [TEST-PHASE-4] |
| report_cases | 3-5 | [TEST-PHASE-4] |
| kyc_profiles | 3-5 | [TEST-PHASE-4] |
| violations | 3-5 | [TEST-PHASE-4] |
| audit_logs (moderator) | 3-5 | [TEST-PHASE-4] |

Không xóa sau test.

21. KHÔNG LÀM MẤT CHỨC NĂNG HIỆN TẠI

Không được làm hỏng:
- Admin Product/Review (đã fix Phase 3)
- Vendor product/KYC
- Customer UI
- Moderator dashboard (nếu có)

22. FILE CHANGE REPORT

Files Modified:
- src/main/resources/templates/moderator/** (cập nhật include fragment moderator-shell)
- src/main/java/com/ecommerce/cnj70/controller/moderator/ModeratorQueueController.java (nếu cần thêm endpoint)
- src/main/resources/templates/moderator/product-queue.html (form action)
- src/main/resources/templates/moderator/shop-queue.html (form action)
- src/main/resources/templates/moderator/report-cases.html
- src/main/resources/templates/moderator/kyc-queue.html
- src/main/resources/templates/moderator/violations.html
- src/main/resources/templates/moderator/history.html

Files Created:
- src/main/resources/templates/moderator/fragments/moderator-shell.html
- src/main/resources/static/css/moderator-extra.css (nếu cần)
- src/main/resources/templates/moderator/shop-detail.html (nếu chưa có)
- src/main/resources/templates/moderator/history-detail.html (nếu chưa có)
- src/test/java/com/ecommerce/cnj70/fix/phase4/ModeratorQueueFixTest.java

Files Deleted:
NONE

23. DATA CHANGE REPORT

MongoDB:
Existing data deleted: NONE
Database reset: NO
Collection dropped: NO

Test records inserted:
- 3-5 Product
- 3-5 Shop
- 3-5 ReportCase
- 3-5 KycProfile
- 3-5 Violation
- 3-5 AuditLog (moderator)

Tất cả prefix [TEST-PHASE-4]

Test records deleted:
NO

24. BUILD REPORT

Command: mvn clean package -DskipTests
Result: PASS / FAIL

25. MANUAL TEST REPORT

Test 1 — Header consistency:
J1: Login ADMIN → xem dashboard → ghi nhận font + CSS class
J2: Login MODERATOR → /moderator/dashboard → so sánh font, navbar, sidebar
J3: Nếu khác → fix fragment moderator-shell.html
J4: Verify mọi template moderator include moderator-shell

Test 2 — Product Queue:
K1: Login MODERATOR
K2: /moderator/product-queue → hiển thị [TEST-PHASE-4] products
K3: Bấm "Approve" → status ACTIVE, flash
K4: Bấm "Reject" + reason → status REJECTED
K5: Bấm "Hide" → status HIDDEN

Test 3 — Shop Queue:
L1: Login MODERATOR
L2: /moderator/shop-queue → hiển thị [TEST-PHASE-4] shops
L3: Click shop → view detail (Phase 4 chưa có nút duyệt)
L4: Phase 5 sẽ thêm nút duyệt + coupling Admin

Test 4 — Report Cases:
M1: Login MODERATOR
M2: /moderator/report-cases → hiển thị [TEST-PHASE-4] cases
M3: Bấm "Claim" → status IN_REVIEW
M4: Bấm "Resolve" + note → status RESOLVED
M5: Bấm "Reject" + reason → status REJECTED

Test 5 — KYC Queue:
N1: Login MODERATOR
N2: /moderator/kyc-queue → hiển thị [TEST-PHASE-4] KYC
N3: Bấm "Approve" → status APPROVED
N4: Bấm "Reject" + reason → status REJECTED

Test 6 — Violations:
O1: Login MODERATOR
O2: /moderator/violations → hiển thị [TEST-PHASE-4] violations
O3: Bấm "Resolve" + note → status RESOLVED

Test 7 — History:
P1: Login MODERATOR
P2: /moderator/history → hiển thị [TEST-PHASE-4] audit logs của moderator
P3: Filter theo action → hiển thị đúng
P4: Click log detail → hiển thị đầy đủ metadata

26. KNOWN ISSUES

Mỗi issue:
Issue: ...
Root Cause: ...
Phase 4 Status: ...

27. STATUS CLASSIFICATION

EXISTING / FIXED / PARTIAL / MISSING / WRONG / OUT OF SCOPE

28. ACCEPTANCE CRITERIA

Phase 4 PASS khi:
- Moderator header đồng nhất Admin (font + CSS + layout)
- 6 queues đều list đúng [TEST-PHASE-4] data
- Action trên mỗi queue hoạt động (approve/reject/hide/claim/resolve/...)
- Manual test 25.1-25.7 PASS
- Build PASS
- MongoDB data preserved
- Existing flows không bị hỏng

29. ĐIỀU KIỆN KẾT THÚC

Phase 4 PASS khi:
- Header moderator = header admin (font, CSS, layout)
- 6 queues đều hoạt động
- 18-30 records test đã insert (3-5 mỗi loại)
- Manual test PASS
- Build PASS
- MongoDB preserved
