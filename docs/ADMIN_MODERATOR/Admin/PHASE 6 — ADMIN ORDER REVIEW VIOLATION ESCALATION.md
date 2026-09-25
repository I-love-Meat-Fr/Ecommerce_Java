# PHASE 6 — ADMIN ORDER / REVIEW / VIOLATION / ESCALATION

## 🎯 Mục tiêu Phase 6

Hoàn thiện và xác minh 4 module Admin: Order Management, Review Moderation, Violation Enforcement, Escalation theo đúng Workbook, business contract và implementation thực tế của source.

Phase 6 không tạo lại toàn bộ 4 module.

Mục tiêu là:

```
Existing Order / Review / Violation / Escalation Implementation
        ↓
Audit
        ↓
Chốt 4 module Contract
        ↓
Bổ sung phần thực sự thiếu
        ↓
Validation
        ↓
Automated Test
        ↓
Runtime + MongoDB
        ↓
Regression
        ↓
4 MODULE hoàn chỉnh
```

Luồng Admin:

```
ADMIN / MODERATOR
  ↓
/admin/orders + /admin/reviews + /admin/violations + /admin/escalations
  ↓
4 MODULE
  ├── List
  ├── View Detail
  ├── Moderate / Approve / Reject
  └── Audit Trail
```

---

## 1. 4-MODULE BUSINESS CONTRACT

### 1.1 — Module × Role Matrix

```
Module                  ADMIN  MODERATOR  CUSTOMER  VENDOR
/admin/orders           ALLOW  DENY       DENY      DENY
/admin/reviews          ALLOW  ALLOW      DENY      DENY
/admin/violations       ALLOW  ALLOW      DENY      DENY
/admin/escalations      ALLOW  ALLOW      DENY      DENY
```

### 1.2 — Order Status

```
OrderStatus
├── PENDING        → Order mới tạo, chờ thanh toán
├── PAID           → Đã thanh toán
├── PROCESSING     → Vendor đang chuẩn bị
├── SHIPPED        → Đã gửi hàng
├── DELIVERED      → Đã nhận hàng
├── COMPLETED      → Hoàn tất
├── CANCELLED      → Đã hủy
└── REFUNDED       → Đã hoàn tiền
```

### 1.3 — Review Status

```
ReviewStatus
├── PENDING        → Chờ duyệt
├── APPROVED       → Đã duyệt, hiển thị trên Customer
├── REJECTED       → Bị từ chối, có lý do
└── HIDDEN         → Bị ẩn
```

### 1.4 — Violation Severity

```
ViolationSeverity
├── WARNING        → Cảnh báo
├── MINOR          → Nhẹ
├── MAJOR          → Nặng
└── CRITICAL       → Nghiêm trọng
```

### 1.5 — Escalation Status

```
EscalationStatus
├── PENDING        → Moderator chuyển lên Admin
├── APPROVED       → Admin chấp nhận escalation
├── REJECTED       → Admin từ chối
└── RESOLVED       → Đã xử lý xong
```

---

## 2. ⚠️ ĐIỀU KIỆN BẮT ĐẦU

Phase 6 chỉ implementation sau khi:

```
PHASE 5 DONE (Voucher / Banner)
        +
AuditAction enum có sẵn các action cần thiết
```

Nếu thiếu enum → `[DEPENDENCY — ENUM]` → dừng và báo cáo.

---

## 3. SOURCE-FIRST RULE

Đọc source thực tế:

```
Order.java
OrderStatus.java
OrderRepository.java
AdminOrderController.java
AdminOrderService.java
AdminOrderServiceImpl.java
admin/order-list.html
admin/order-detail.html

Review.java
ReviewStatus.java
ReviewRepository.java
AdminReviewController.java
AdminReviewService.java
AdminReviewServiceImpl.java
admin/review-list.html
admin/review-detail.html

Violation.java
ViolationSeverity.java
ViolationRepository.java
AdminViolationController.java
AdminViolationService.java
AdminViolationServiceImpl.java
admin/violation-list.html
admin/violation-detail.html

Escalation.java
EscalationStatus.java
EscalationRepository.java
AdminEscalationController.java
AdminEscalationService.java
AdminEscalationServiceImpl.java
admin/escalation-list.html
admin/escalation-detail.html

AuditLog.java
AuditLogRepository.java
AuditAction.java

SecurityConfig.java
ModerationGuard.java
```

Phân loại:

```
[EXISTING]
[IMPLEMENTED]
[MISSING]
[BUG]
[DEPENDENCY]
[OUT OF SCOPE]
```

---

## 4. ⚠️ PHẠM VI PHASE 6

### CHỈ LÀM

```
Audit implementation hiện tại của 4 module
Chốt contract cho từng module
List / View Detail
Moderate / Approve / Reject
Enforce penalty (Violation)
Resolve Escalation
Audit trail
Validation
Security
Empty State
UI consistency
Automated Test
Runtime verification
MongoDB verification
Regression smoke test
```

### ❌ KHÔNG ĐƯỢC LÀM

```
Customer Order placement
Customer Review submission
Customer Violation report (ngoài Phase scope)
Vendor Order fulfillment
Vendor Review reply (Phase riêng)
Payment Settlement (Phase riêng)
Refund / Return (Phase riêng)
Refactor toàn bộ 4 module
Redesign toàn bộ Admin UI
```

---

## TASK 6.1 — AUDIT ORDER IMPLEMENTATION

### 🎯 Mục tiêu

Xác định chính xác trạng thái Order hiện tại.

### Đọc source

```
Order.java
OrderStatus.java
OrderRepository.java
AdminOrderController.java
AdminOrderService.java
AdminOrderServiceImpl.java
order-list.html
order-detail.html
```

### Kiểm tra

```
List
View Detail
Update Status
Cancel
Refund
Filter (status / date / customer / vendor)
Search
Pagination
Statistics (revenue / order count)
```

### DONE

Báo cáo `[CURRENT IMPLEMENTATION] / [MISSING] / [BUG] / [DEPENDENCY]`.

---

## TASK 6.2 — CHỐT ORDER CONTRACT

```
Thành phần       Admin
List             Có
View Detail      Có
Update Status    Có (có validation)
Cancel           Có (theo rule)
Refund           Có (theo rule)
Delete           DENY
```

State machine:

```
PENDING ──pay──→ PAID
PAID ──process─→ PROCESSING
PROCESSING ──ship──→ SHIPPED
SHIPPED ──deliver──→ DELIVERED
DELIVERED ──complete──→ COMPLETED
PENDING ──cancel──→ CANCELLED
PAID ──cancel──→ REFUNDED (nếu đã trừ tiền)
```

### DONE

Order contract rõ ràng.

---

## TASK 6.3 — ADMIN ORDER LIST & DETAIL

### 🎯 Mục tiêu

Đảm bảo /admin/orders hiển thị đúng order và detail đầy đủ.

```
Route: /admin/orders
Route: /admin/orders/{id}

Phải hiển thị:
- Order ID
- Customer (email / username)
- Vendor / Shop
- Items (product / quantity / price)
- Total amount
- Status
- Created at
- Updated at
- Shipping address
- Payment info
```

### DONE

Admin Order List + Detail hiển thị đúng.

---

## TASK 6.4 — ADMIN ORDER STATUS UPDATE

### 🎯 Mục tiêu

Verify và hoàn thiện Update Order Status.

```
Flow:
Admin
 ↓
POST /admin/orders/{id}/status
 ↓
Validate transition
 ↓
Update status
 ↓
AuditLog
 ↓
MongoDB
```

Bắt buộc:

```
State machine phải được respect.
Không cho phép CANCELLED → COMPLETED.
Phải có lý do nếu CANCELLED / REFUNDED.
AuditLog phải ghi status cũ và mới.
```

### DONE

Update Order Status hoạt động đúng + có AuditLog.

---

## TASK 6.5 — AUDIT REVIEW IMPLEMENTATION

### 🎯 Mục tiêu

Xác định chính xác trạng thái Review hiện tại.

### Đọc source

```
Review.java
ReviewStatus.java
ReviewRepository.java
AdminReviewController.java
AdminReviewService.java
AdminReviewServiceImpl.java
review-list.html
review-detail.html
```

### DONE

Báo cáo `[CURRENT IMPLEMENTATION] / [MISSING] / [BUG] / [DEPENDENCY]`.

---

## TASK 6.6 — CHỐT REVIEW CONTRACT

```
Thành phần       Admin     Moderator
List             Có        Có
View Detail      Có        Có
Approve          Có        Có
Reject           Có        Có (có reason)
Hide             Có        Có
Delete           DENY      DENY
```

State machine:

```
PENDING ──approve──→ APPROVED
PENDING ──reject───→ REJECTED (có reason)
APPROVED ──hide───→ HIDDEN
HIDDEN ──show─────→ APPROVED
```

### DONE

Review contract rõ ràng.

---

## TASK 6.7 — ADMIN REVIEW MODERATION

### 🎯 Mục tiêu

Verify và hoàn thiện Review Moderation.

```
Approve Flow:
Admin/Moderator
 ↓
POST /admin/reviews/{id}/approve
 ↓
Validate status = PENDING
 ↓
status = APPROVED
 ↓
AuditLog
 ↓
MongoDB

Reject Flow:
Admin/Moderator
 ↓
POST /admin/reviews/{id}/reject
 ↓
Validate status = PENDING
 ↓
Nhập reason
 ↓
status = REJECTED
 ↓
review.rejectionReason = reason
 ↓
AuditLog
 ↓
MongoDB

Hide Flow:
Admin/Moderator
 ↓
POST /admin/reviews/{id}/hide
 ↓
Validate status = APPROVED
 ↓
status = HIDDEN
 ↓
AuditLog
 ↓
MongoDB
```

Bắt buộc:

```
Reject phải có reason.
AuditLog phải ghi nhận.
Moderation rules: chứa từ cấm → auto reject (nếu Phase 6 có yêu cầu).
```

### DONE

Review Moderation hoạt động đúng + có AuditLog.

---

## TASK 6.8 — AUDIT VIOLATION IMPLEMENTATION

### 🎯 Mục tiêu

Xác định chính xác trạng thái Violation hiện tại.

### Đọc source

```
Violation.java
ViolationSeverity.java
ViolationRepository.java
AdminViolationController.java
AdminViolationService.java
AdminViolationServiceImpl.java
violation-list.html
violation-detail.html
```

### DONE

Báo cáo `[CURRENT IMPLEMENTATION] / [MISSING] / [BUG] / [DEPENDENCY]`.

---

## TASK 6.9 — CHỐT VIOLATION CONTRACT

```
Thành phần       Admin     Moderator
List             Có        Có
View Detail      Có        Có
Create           Có        Có (cho user/shop)
Enforce penalty  Có        Có (theo rule)
Resolve          Có        Có
```

Severity × Penalty:

```
WARNING  → email warning
MINOR    → temp restrict 1 feature
MAJOR    → suspend account / shop 7 days
CRITICAL → ban account / shop vĩnh viễn
```

### DONE

Violation contract rõ ràng.

---

## TASK 6.10 — ADMIN VIOLATION ENFORCEMENT

### 🎯 Mục tiêu

Verify và hoàn thiện Violation Enforcement.

```
Create Violation Flow:
Admin/Moderator
 ↓
POST /admin/violations/create
 ↓
targetType = USER / SHOP / PRODUCT / REVIEW
targetId = ...
severity = WARNING / MINOR / MAJOR / CRITICAL
reason = ...
 ↓
Save Violation
 ↓
Apply penalty theo severity
 ↓
AuditLog
 ↓
MongoDB
```

Penalty cascade:

```
WARNING  → User.shop warningCount++
MINOR    → User.status = SUSPENDED 1d
MAJOR    → User.status = SUSPENDED 7d
CRITICAL → User.status = BANNED
```

### DONE

Violation Enforcement hoạt động đúng + cascade effect + có AuditLog.

---

## TASK 6.11 — AUDIT ESCALATION IMPLEMENTATION

### 🎯 Mục tiêu

Xác định chính xác trạng thái Escalation hiện tại.

### Đọc source

```
Escalation.java
EscalationStatus.java
EscalationRepository.java
AdminEscalationController.java
AdminEscalationService.java
AdminEscalationServiceImpl.java
escalation-list.html
escalation-detail.html
```

### DONE

Báo cáo `[CURRENT IMPLEMENTATION] / [MISSING] / [BUG] / [DEPENDENCY]`.

---

## TASK 6.12 — CHỐT ESCALATION CONTRACT

```
Thành phần       Admin     Moderator
List             Có        Có (chỉ của mình)
View Detail      Có        Có
Approve          Có        DENY (chỉ Admin mới có quyền)
Reject           Có        DENY
Resolve          Có        Có
Create           DENY      Có (Moderator tạo)
```

State machine:

```
PENDING ──approve──→ APPROVED → resolved
PENDING ──reject───→ REJECTED
APPROVED ──resolve─→ RESOLVED
```

### DONE

Escalation contract rõ ràng.

---

## TASK 6.13 — ADMIN ESCALATION APPROVAL

### 🎯 Mục tiêu

Verify và hoàn thiện Escalation Approval.

```
Flow:
Admin
 ↓
GET /admin/escalations
 ↓
List PENDING
 ↓
POST /admin/escalations/{id}/approve
 ↓
Validate status = PENDING
 ↓
status = APPROVED
 ↓
Apply action (lock user / ban shop / hide product / reject review)
 ↓
AuditLog
 ↓
MongoDB
 ↓
Resolve
```

Bắt buộc:

```
AuditLog phải ghi nhận cả Escalation và action cascade.
Resolved phải có note từ Admin.
```

### DONE

Escalation Approval hoạt động đúng + cascade effect + có AuditLog.

---

## TASK 6.14 — 4-MODULE DATA ACCURACY

### 🎯 Mục tiêu

Đối chiếu 4 module với MongoDB.

Test:

```
Order Update Status
 ↓
MongoDB
Review Approve / Reject / Hide
 ↓
MongoDB
Violation Create / Enforce
 ↓
MongoDB
Escalation Approve / Resolve
 ↓
MongoDB
```

Đối chiếu:

```
MongoDB
   ↕
Repository
   ↕
Service
   ↕
Controller
   ↕
Template
```

### DONE

4 module khớp dữ liệu MongoDB thực tế.

---

## TASK 6.15 — 4-MODULE SECURITY

### 🎯 Mục tiêu

Chỉ ADMIN / MODERATOR được truy cập.

```
ADMIN       → /admin/orders         → ALLOW
MODERATOR   → /admin/orders         → DENY
CUSTOMER    → /admin/orders         → DENY
VENDOR      → /admin/orders         → DENY
ANONYMOUS   → /admin/orders         → LOGIN / DENIED

ADMIN       → /admin/reviews        → ALLOW
MODERATOR   → /admin/reviews        → ALLOW
CUSTOMER    → /admin/reviews        → DENY
VENDOR      → /admin/reviews        → DENY

ADMIN       → /admin/violations     → ALLOW
MODERATOR   → /admin/violations     → ALLOW

ADMIN       → /admin/escalations    → ALLOW
MODERATOR   → /admin/escalations    → ALLOW
```

### DONE

Không có Customer/Vendor/Anonymous bypass 4 module.

---

## TASK 6.16 — 4-MODULE AUTOMATED TEST

### 🎯 Mục tiêu

Bổ sung automated test.

Test tối thiểu:

```
Order
[ ] List order
[ ] View detail
[ ] Update status PENDING → PAID
[ ] Update status COMPLETED → PENDING → ERROR
[ ] Filter by status

Review
[ ] Approve PENDING → APPROVED
[ ] Reject PENDING → REJECTED + reason
[ ] Hide APPROVED → HIDDEN

Violation
[ ] Create WARNING
[ ] Create CRITICAL → cascade User BANNED
[ ] Resolve

Escalation
[ ] List PENDING
[ ] Approve → cascade action
[ ] Reject
[ ] Resolve
```

### DONE

4 module có automated test.

---

## TASK 6.17 — 4-MODULE UI CONSISTENCY

### 🎯 Mục tiêu

Đảm bảo UI sử dụng đúng Admin Shell.

Kiểm tra:

```
Header / Sidebar / Navigation / Breadcrumb / Page title
Table / Form / Action buttons
Flash message / Error message / Empty State
Filter / Search / Pagination
```

### DONE

4 module UI nhất quán với Admin Core.

---

## TASK 6.18 — 4-MODULE EMPTY STATE

### 🎯 Mục tiêu

Xác minh Empty State khi không có data.

```
MongoDB: Order = 0 → Empty State
MongoDB: Review = 0 → Empty State
MongoDB: Violation = 0 → Empty State
MongoDB: Escalation = 0 → Empty State
```

Không:

```
Hiển thị fake data.
Crash template.
```

### DONE

Empty State đúng cho 4 module.

---

## TASK 6.19 — RUNTIME VERIFICATION

### 🎯 Mục tiêu

Kiểm tra application thực tế.

Flow:

```
Admin Login
    ↓
/admin/orders → List / View Detail / Update Status
    ↓
/admin/reviews → List / Approve / Reject / Hide
    ↓
/admin/violations → List / Create / Resolve
    ↓
/admin/escalations → List / Approve / Resolve
    ↓
MongoDB
```

### DONE

4 module đã được kiểm tra trên application thực tế.

---

## TASK 6.20 — MONGODB VERIFICATION

### 🎯 Mục tiêu

Xác minh dữ liệu 4 module trực tiếp trên MongoDB.

Kiểm tra:

```
Order.status / Order.updatedAt
Review.status / Review.rejectionReason
Violation.severity / Violation.resolved
Escalation.status / Escalation.resolvedNote
```

### DONE

MongoDB phản ánh đúng kết quả của 4 module operations.

---

## TASK 6.21 — REGRESSION SMOKE TEST

### 🎯 Mục tiêu

Đảm bảo Phase 6 không phá các flow đang tồn tại.

```
ADMIN
[ ] Admin User (Phase 3)
[ ] Admin Shop (Phase 4)
[ ] Admin Product (Phase 4)
[ ] Admin Voucher (Phase 5)
[ ] Admin Banner (Phase 5)

CUSTOMER
[ ] Customer Order (không bị ảnh hưởng bởi Admin status change)
[ ] Customer Review (HIDDEN thì không hiển thị)
[ ] Customer Home

VENDOR
[ ] Vendor Order (status PROCESSING/SHIPPED hoạt động)
[ ] Vendor Dashboard
```

### DONE

Không có regression nghiêm trọng do Phase 6 gây ra.

---

## TASK 6.22 — PHASE 6 FINAL REVIEW

### Báo cáo

```
[ORDER]
List → PASS/FAIL
View Detail → PASS/FAIL
Update Status → PASS/FAIL
State Machine → PASS/FAIL

[REVIEW]
Approve → PASS/FAIL
Reject → PASS/FAIL
Hide → PASS/FAIL

[VIOLATION]
Create → PASS/FAIL
Enforce penalty → PASS/FAIL
Cascade User/Shop → PASS/FAIL

[ESCALATION]
List → PASS/FAIL
Approve → PASS/FAIL
Resolve → PASS/FAIL

[SECURITY]
Admin → PASS/FAIL
Moderator → PASS/FAIL
Customer → PASS/FAIL
Vendor → PASS/FAIL
Anonymous → PASS/FAIL

[DATA]
MongoDB → PASS/FAIL
Admin UI → PASS/FAIL

[TEST]
Automated Test → PASS/FAIL/SKIPPED
Maven Test → PASS/FAIL

[RUNTIME]
Runtime → PASS/FAIL

[REGRESSION]
Customer → PASS/FAIL
Vendor → PASS/FAIL

[IMPLEMENTATION SUMMARY]
[CURRENT IMPLEMENTATION]
[MISSING FEATURES]
[BUGS FOUND]
[BACKEND CHANGES]
[CONTROLLER CHANGES]
[SERVICE CHANGES]
[REPOSITORY CHANGES]
[FRONTEND CHANGES]
[SECURITY RESULT]
[VALIDATION RESULT]
[TEST RESULT]
[RUNTIME RESULT]
[MONGODB RESULT]
[REGRESSION RESULT]
```

---

## 🛑 STOP RULE

Sau khi hoàn thành Phase 6:

**DỪNG.**

Không tự triển khai:

```
Audit Log Viewer (Phase 7)
Role Logic Regression (Phase 7)
E2E Test (Phase 8)
```

Chỉ khi user xác nhận:

```
PHASE 6 DONE
```

mới chuyển sang:

```
PHASE 7 — AUDIT LOG + ROLE LOGIC REGRESSION
```

---

## 📋 TỔNG KẾT PHASE 6

```
PHASE 6 — ADMIN ORDER / REVIEW / VIOLATION / ESCALATION
│
├── TASK 6.1
│   └── Audit Order Implementation
│
├── TASK 6.2
│   └── Chốt Order Contract
│
├── TASK 6.3
│   └── Admin Order List & Detail
│
├── TASK 6.4
│   └── Admin Order Status Update
│
├── TASK 6.5
│   └── Audit Review Implementation
│
├── TASK 6.6
│   └── Chốt Review Contract
│
├── TASK 6.7
│   └── Admin Review Moderation
│
├── TASK 6.8
│   └── Audit Violation Implementation
│
├── TASK 6.9
│   └── Chốt Violation Contract
│
├── TASK 6.10
│   └── Admin Violation Enforcement
│
├── TASK 6.11
│   └── Audit Escalation Implementation
│
├── TASK 6.12
│   └── Chốt Escalation Contract
│
├── TASK 6.13
│   └── Admin Escalation Approval
│
├── TASK 6.14
│   └── 4-Module Data Accuracy
│
├── TASK 6.15
│   └── 4-Module Security
│
├── TASK 6.16
│   └── 4-Module Automated Test
│
├── TASK 6.17
│   └── 4-Module UI Consistency
│
├── TASK 6.18
│   └── 4-Module Empty State
│
├── TASK 6.19
│   └── Runtime Verification
│
├── TASK 6.20
│   └── MongoDB Verification
│
├── TASK 6.21
│   └── Regression Smoke Test
│
└── TASK 6.22
    └── Phase 6 Final Review
```

---

## THỨ TỰ THỰC THI

```
Audit Order Source
      ↓
Chốt Order Contract
      ↓
Audit Review Source
      ↓
Chốt Review Contract
      ↓
Audit Violation Source
      ↓
Chốt Violation Contract
      ↓
Audit Escalation Source
      ↓
Chốt Escalation Contract
      ↓
Implement từng module
      ↓
Validation + Security
      ↓
Automated Test
      ↓
UI Consistency
      ↓
Empty State
      ↓
Runtime
      ↓
MongoDB
      ↓
Regression
      ↓
Final Review
      ↓
PHASE 6 DONE
```

---

## NGUYÊN TẮC PHASE 6

```
READ SOURCE FIRST
        ↓
IDENTIFY EXISTING IMPLEMENTATION
        ↓
DO NOT REBUILD EXISTING FEATURES
        ↓
ONLY IMPLEMENT WHAT IS ACTUALLY MISSING
        ↓
FOLLOW WORKBOOK + TEAM CONTRACT
        ↓
ADMIN + MODERATOR ALLOWED (tuỳ module)
        ↓
STATE MACHINE RESPECTED
        ↓
CASCADE EFFECT CLEAR
        ↓
TEST
        ↓
RUNTIME
        ↓
MONGODB
        ↓
REGRESSION
        ↓
STOP
```

---

## Điểm quan trọng nhất

Phase 6

```
≠
Xây lại 4 module từ đầu
```

mà là:

```
Existing 4 module
      ↓
Audit
      ↓
Giữ phần đúng
      ↓
Sửa phần sai
      ↓
Bổ sung phần Workbook thực sự yêu cầu
      ↓
State machine + cascade
        ↓
Test
        ↓
Runtime
```
