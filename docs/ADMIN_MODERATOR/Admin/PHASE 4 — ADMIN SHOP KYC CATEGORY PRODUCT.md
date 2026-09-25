# PHASE 4 — ADMIN SHOP / KYC / CATEGORY / PRODUCT

## 🎯 Mục tiêu Phase 4

Hoàn thiện và xác minh 4 module Admin: Shop Approval, KYC, Category, Product theo đúng Workbook, business contract và implementation thực tế của source.

Phase 4 không tạo lại toàn bộ 4 module.

Mục tiêu là:

```
Existing Shop / KYC / Category / Product Implementation
        ↓
Audit
        ↓
Chốt 4 module Contract
        ↓
Bổ sung phần thực sự thiếu
        ↓
Admin/Moderator isolation
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
/admin/shops + /admin/kyc + /admin/categories + /admin/products
  ↓
4 MODULE
  ├── List
  ├── View Detail
  ├── Approve
  ├── Reject
  ├── Activate / Deactivate
  └── Edit (nếu contract yêu cầu)
```

---

## 1. 4-MODULE BUSINESS CONTRACT

### 1.1 — Module × Role Matrix

```
Module                ADMIN  MODERATOR  CUSTOMER  VENDOR
/admin/shops          ALLOW  ALLOW      DENY      DENY
/admin/kyc            ALLOW  ALLOW      DENY      DENY
/admin/categories     ALLOW  ALLOW      DENY      DENY
/admin/products       ALLOW  ALLOW      DENY      DENY
```

### 1.2 — Shop Status

```
ShopStatus
├── PENDING       → Shop mới tạo, chờ duyệt
├── APPROVED      → Shop hoạt động
├── REJECTED      → Shop bị từ chối, có lý do
├── SUSPENDED     → Shop tạm khóa có thời hạn
└── RESTRICTED    → Shop bị giới hạn một phần
```

### 1.3 — KYC Status

```
KycStatus
├── PENDING       → Vendor mới submit
├── APPROVED      → Đã duyệt, vendor có thể tạo Shop
└── REJECTED      → Bị từ chối, có lý do
```

### 1.4 — Product Status

```
ProductStatus
├── DRAFT         → Vendor đang soạn
├── PENDING       → Chờ Admin duyệt
├── APPROVED      → Đã duyệt, hiển thị trên Customer
├── REJECTED      → Bị từ chối, có lý do
├── HIDDEN        → Bị ẩn khỏi Customer
└── OUT_OF_STOCK  → Hết hàng
```

### 1.5 — Category

```
Category
├── id
├── name
├── slug
├── parentId (nullable, cho sub-category)
├── active
├── createdAt
└── updatedAt
```

---

## 2. ⚠️ ĐIỀU KIỆN BẮT ĐẦU

Phase 4 chỉ implementation sau khi:

```
PHASE 2 DONE (Security fix)
PHASE 3 DONE (User Management)
        +
SecurityConfig đã fix
ModerationGuard đã fix
AuditAction enum có sẵn (nếu thiếu → DEPENDENCY)
```

---

## 3. SOURCE-FIRST RULE

Đọc source thực tế:

```
Shop.java
ShopStatus.java
ShopRepository.java

Kyc.java
KycStatus.java
KycRepository.java

Category.java
CategoryRepository.java

Product.java
ProductStatus.java
ProductRepository.java

AdminShopController.java
AdminShopService.java
AdminShopServiceImpl.java
admin/shop-list.html
admin/shop-detail.html

AdminKycController.java
AdminKycService.java
AdminKycServiceImpl.java
admin/kyc-list.html
admin/kyc-detail.html

AdminCategoryController.java
AdminCategoryService.java
AdminCategoryServiceImpl.java
admin/category-list.html
admin/category-form.html

AdminProductController.java
AdminProductService.java
AdminProductServiceImpl.java
admin/product-list.html
admin/product-detail.html

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

## 4. ⚠️ PHẠM VI PHASE 4

### CHỈ LÀM

```
Audit implementation hiện tại của 4 module
Chốt contract cho từng module
List / View Detail
Approve / Reject
Activate / Deactivate
Edit (nếu Workbook yêu cầu)
Validation
WEB / SHOP isolation (cho Shop)
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
Vendor Product CRUD (Phase khác)
Customer Frontend
Checkout
Payment
Order
Review
Violation
Banner / Voucher
Refactor toàn bộ 4 module
Redesign toàn bộ Admin UI
Tự ý thêm enum mới
```

---

## TASK 4.1 — AUDIT SHOP IMPLEMENTATION

### 🎯 Mục tiêu

Xác định chính xác trạng thái Shop hiện tại.

### Đọc source

```
Shop.java
ShopStatus.java
ShopRepository.java
AdminShopController.java
AdminShopService.java
AdminShopServiceImpl.java
shop-list.html
shop-detail.html
```

### Kiểm tra

```
List
View Detail
Approve
Reject
Activate
Deactivate
Search
Filter
Pagination
Validation
state machine (PENDING → APPROVED → SUSPENDED)
```

### DONE

Báo cáo:

```
[CURRENT IMPLEMENTATION]
[MISSING]
[BUG]
[DEPENDENCY]
[PROPOSED CHANGE]
[OUT OF SCOPE]
```

---

## TASK 4.2 — CHỐT SHOP CONTRACT

```
Thành phần       Admin     Moderator
List             Có        Có
View Detail      Có        Có
Approve          Có        Có
Reject           Có        Có
Activate         Có        Có
Deactivate       Có        Có
Edit info        Có        Có
Delete           DENY      DENY (soft delete)
```

State machine:

```
PENDING ──approve──→ APPROVED
PENDING ──reject───→ REJECTED
APPROVED ──suspend─→ SUSPENDED
APPROVED ──restrict→ RESTRICTED
SUSPENDED ──reactivate→ APPROVED
REJECTED ──reconsider→ PENDING (nếu vendor submit lại)
```

### DONE

Shop contract rõ ràng.

---

## TASK 4.3 — ADMIN SHOP LIST

### 🎯 Mục tiêu

Đảm bảo /admin/shops hiển thị đúng shop.

```
Route: /admin/shops
Flow:
  /admin/shops
        ↓
  AdminShopController
        ↓
  AdminShopService
        ↓
  findAll()
        ↓
  MongoDB
```

Phải đảm bảo:

```
Tất cả status hiển thị: PENDING, APPROVED, REJECTED, SUSPENDED, RESTRICTED
```

### DONE

Admin Shop List hiển thị đúng.

---

## TASK 4.4 — APPROVE / REJECT SHOP

### 🎯 Mục tiêu

Verify và hoàn thiện Approve/Reject Shop.

```
Approve Flow:
Admin
 ↓
POST /admin/shops/{id}/approve
 ↓
Validate status = PENDING
 ↓
status = APPROVED
 ↓
active = true
 ↓
AuditLog
 ↓
MongoDB

Reject Flow:
Admin
 ↓
POST /admin/shops/{id}/reject
 ↓
Validate status = PENDING
 ↓
Nhập reason
 ↓
status = REJECTED
 ↓
shop.rejectionReason = reason
 ↓
AuditLog
 ↓
MongoDB
```

Bắt buộc:

```
Chỉ approve shop PENDING.
Reject phải có reason.
AuditLog phải ghi nhận.
```

### DONE

Approve/Reject Shop hoạt động đúng + có AuditLog.

---

## TASK 4.5 — ACTIVATE / DEACTIVATE SHOP

### 🎯 Mục tiêu

Verify và hoàn thiện Activate/Deactivate Shop.

```
Activate Flow:
Admin
 ↓
POST /admin/shops/{id}/activate
 ↓
Validate
 ↓
active = true
 ↓
AuditLog
 ↓
MongoDB

Deactivate Flow:
Admin
 ↓
POST /admin/shops/{id}/deactivate
 ↓
Validate
 ↓
active = false
 ↓
AuditLog
 ↓
MongoDB
```

### DONE

Activate/Deactivate Shop hoạt động đúng + có AuditLog.

---

## TASK 4.6 — AUDIT KYC IMPLEMENTATION

### 🎯 Mục tiêu

Xác định chính xác trạng thái KYC hiện tại.

### Đọc source

```
Kyc.java
KycStatus.java
KycRepository.java
AdminKycController.java
AdminKycService.java
AdminKycServiceImpl.java
kyc-list.html
kyc-detail.html
```

### DONE

Báo cáo `[CURRENT IMPLEMENTATION] / [MISSING] / [BUG] / [DEPENDENCY]`.

---

## TASK 4.7 — CHỐT KYC CONTRACT

```
Thành phần       Admin     Moderator
List             Có        Có
View Detail      Có        Có
Approve          Có        Có
Reject           Có        Có
Edit             DENY      DENY
```

State machine:

```
PENDING ──approve──→ APPROVED
PENDING ──reject───→ REJECTED (có reason)
REJECTED ──resubmit→ PENDING (vendor submit lại)
```

### DONE

KYC contract rõ ràng.

---

## TASK 4.8 — ADMIN KYC APPROVE / REJECT

### 🎯 Mục tiêu

Verify và hoàn thiện Approve/Reject KYC.

```
Approve Flow:
Admin
 ↓
POST /admin/kyc/{id}/approve
 ↓
Validate status = PENDING
 ↓
kyc.status = APPROVED
 ↓
User.role có được promote lên VENDOR?
 ↓
AuditLog
 ↓
MongoDB
```

Bắt buộc:

```
Reject KYC phải có reason.
Sau khi Approve, User.role = VENDOR (nếu cascade rule yêu cầu).
AuditLog phải ghi nhận.
```

### DONE

KYC Approve/Reject hoạt động đúng + có AuditLog.

---

## TASK 4.9 — AUDIT CATEGORY IMPLEMENTATION

### 🎯 Mục tiêu

Xác định chính xác trạng thái Category hiện tại.

### Đọc source

```
Category.java
CategoryRepository.java
AdminCategoryController.java
AdminCategoryService.java
AdminCategoryServiceImpl.java
category-list.html
category-form.html
```

### DONE

Báo cáo `[CURRENT IMPLEMENTATION] / [MISSING] / [BUG] / [DEPENDENCY]`.

---

## TASK 4.10 — CHỐT CATEGORY CONTRACT

```
Thành phần       Admin     Moderator
List             Có        Có
Create           Có        Có
Edit             Có        Có
Delete           Có        Có (cascade product nếu cần)
```

### DONE

Category contract rõ ràng.

---

## TASK 4.11 — ADMIN CATEGORY CRUD

### 🎯 Mục tiêu

Verify và hoàn thiện CRUD Category.

```
Create Flow:
Admin
 ↓
POST /admin/categories/create
 ↓
Validate
 ↓
Save
 ↓
MongoDB

Edit Flow:
Admin
 ↓
POST /admin/categories/{id}/edit
 ↓
Validate
 ↓
Update
 ↓
MongoDB

Delete Flow:
Admin
 ↓
POST /admin/categories/{id}/delete
 ↓
Check cascade product
 ↓
Delete (hoặc soft delete)
 ↓
MongoDB
```

Bắt buộc:

```
Category name unique.
Slug unique.
Cascade product: nếu category bị xóa, product phải chuyển sang category mặc định HOẶC bị ẩn.
```

### DONE

Category CRUD hoạt động đúng.

---

## TASK 4.12 — AUDIT PRODUCT IMPLEMENTATION

### 🎯 Mục tiêu

Xác định chính xác trạng thái Product hiện tại.

### Đọc source

```
Product.java
ProductStatus.java
ProductRepository.java
AdminProductController.java
AdminProductService.java
AdminProductServiceImpl.java
product-list.html
product-detail.html
```

### DONE

Báo cáo `[CURRENT IMPLEMENTATION] / [MISSING] / [BUG] / [DEPENDENCY]`.

---

## TASK 4.13 — CHỐT PRODUCT CONTRACT

```
Thành phần       Admin     Moderator
List             Có        Có
View Detail      Có        Có
Approve          Có        Có
Reject           Có        Có
Hide             Có        Có
Activate         Có        Có
Edit price       DENY      DENY (vendor tự sửa)
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

Product contract rõ ràng.

---

## TASK 4.14 — ADMIN PRODUCT APPROVE / REJECT / HIDE

### 🎯 Mục tiêu

Verify và hoàn thiện Product Moderation.

```
Approve Flow:
Admin/Moderator
 ↓
POST /admin/products/{id}/approve
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
POST /admin/products/{id}/reject
 ↓
Validate status = PENDING
 ↓
Nhập reason
 ↓
status = REJECTED
 ↓
product.rejectionReason = reason
 ↓
AuditLog
 ↓
MongoDB

Hide Flow:
Admin/Moderator
 ↓
POST /admin/products/{id}/hide
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
Sau Reject, vendor có thể edit và resubmit (chuyển về PENDING).
AuditLog phải ghi nhận.
```

### DONE

Product Moderation hoạt động đúng + có AuditLog.

---

## TASK 4.15 — 4-MODULE DATA ACCURACY

### 🎯 Mục tiêu

Đối chiếu 4 module với MongoDB.

Test:

```
Shop Approve / Reject
 ↓
MongoDB
KYC Approve / Reject
 ↓
MongoDB
Category Create / Edit / Delete
 ↓
MongoDB
Product Approve / Reject / Hide
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

## TASK 4.16 — 4-MODULE SECURITY

### 🎯 Mục tiêu

Chỉ ADMIN / MODERATOR được truy cập.

```
ADMIN       → /admin/shops         → ALLOW
MODERATOR   → /admin/shops         → ALLOW
CUSTOMER    → /admin/shops         → DENY
VENDOR      → /admin/shops         → DENY
ANONYMOUS   → /admin/shops         → LOGIN / DENIED

(Tương tự cho KYC, CATEGORY, PRODUCT)
```

### DONE

Không có Customer/Vendor/Anonymous bypass 4 module.

---

## TASK 4.17 — 4-MODULE AUTOMATED TEST

### 🎯 Mục tiêu

Bổ sung automated test.

Test tối thiểu:

```
Shop
[ ] Approve PENDING → APPROVED
[ ] Reject PENDING → REJECTED + reason
[ ] Approve APPROVED → ERROR
[ ] Activate / Deactivate
[ ] List filter by status

KYC
[ ] Approve PENDING → APPROVED
[ ] Reject PENDING → REJECTED + reason
[ ] Cascade User.role = VENDOR

Category
[ ] Create
[ ] Edit
[ ] Delete (cascade product?)

Product
[ ] Approve PENDING → APPROVED
[ ] Reject PENDING → REJECTED + reason
[ ] Hide APPROVED → HIDDEN
[ ] List filter by status / shop / category
```

### DONE

4 module có automated test.

---

## TASK 4.18 — 4-MODULE UI CONSISTENCY

### 🎯 Mục tiêu

Đảm bảo UI sử dụng đúng Admin Shell.

Kiểm tra:

```
Header / Sidebar / Navigation / Breadcrumb / Page title
Table / Form / Action buttons
Flash message / Error message / Empty State
```

### DONE

4 module UI nhất quán với Admin Core.

---

## TASK 4.19 — RUNTIME VERIFICATION

### 🎯 Mục tiêu

Kiểm tra application thực tế.

Flow:

```
Admin Login
    ↓
/admin/shops → List / Approve / Reject
    ↓
/admin/kyc → List / Approve / Reject
    ↓
/admin/categories → List / Create / Edit / Delete
    ↓
/admin/products → List / Approve / Reject / Hide
    ↓
MongoDB
```

### DONE

4 module đã được kiểm tra trên application thực tế.

---

## TASK 4.20 — MONGODB VERIFICATION

### 🎯 Mục tiêu

Xác minh dữ liệu 4 module trực tiếp trên MongoDB.

Kiểm tra:

```
Shop.status / Shop.active / Shop.rejectionReason
Kyc.status / Kyc.rejectionReason
Category.name / Category.slug / Category.active
Product.status / Product.rejectionReason / Product.hidden
```

### DONE

MongoDB phản ánh đúng kết quả của 4 module operations.

---

## TASK 4.21 — REGRESSION SMOKE TEST

### 🎯 Mục tiêu

Đảm bảo Phase 4 không phá các flow đang tồn tại.

```
ADMIN
[ ] Admin User (Phase 3 không bị phá)

CUSTOMER
[ ] Customer Home
[ ] Customer Product List (không bị ảnh hưởng bởi HIDDEN)

VENDOR
[ ] Vendor Shop (APPROVED thì hiển thị)
[ ] Vendor Product (APPROVED thì customer thấy)
[ ] Vendor KYC (chỉ vendor)
```

### DONE

Không có regression nghiêm trọng do Phase 4 gây ra.

---

## TASK 4.22 — PHASE 4 FINAL REVIEW

### Báo cáo

```
[SHOP]
Approve → PASS/FAIL
Reject → PASS/FAIL
Activate → PASS/FAIL
Deactivate → PASS/FAIL
State Machine → PASS/FAIL

[KYC]
Approve → PASS/FAIL
Reject → PASS/FAIL
Cascade User.role → PASS/FAIL

[CATEGORY]
Create → PASS/FAIL
Edit → PASS/FAIL
Delete → PASS/FAIL
Cascade Product → PASS/FAIL/SKIPPED

[PRODUCT]
Approve → PASS/FAIL
Reject → PASS/FAIL
Hide → PASS/FAIL

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

Sau khi hoàn thành Phase 4:

**DỪNG.**

Không tự triển khai:

```
Voucher / Banner (Phase 5)
Order / Review / Violation / Escalation (Phase 6)
```

Chỉ khi user xác nhận:

```
PHASE 4 DONE
```

mới chuyển sang:

```
PHASE 5 — ADMIN VOUCHER / BANNER
```

---

## 📋 TỔNG KẾT PHASE 4

```
PHASE 4 — ADMIN SHOP / KYC / CATEGORY / PRODUCT
│
├── TASK 4.1
│   └── Audit Shop Implementation
│
├── TASK 4.2
│   └── Chốt Shop Contract
│
├── TASK 4.3
│   └── Admin Shop List
│
├── TASK 4.4
│   └── Approve / Reject Shop
│
├── TASK 4.5
│   └── Activate / Deactivate Shop
│
├── TASK 4.6
│   └── Audit KYC Implementation
│
├── TASK 4.7
│   └── Chốt KYC Contract
│
├── TASK 4.8
│   └── Admin KYC Approve / Reject
│
├── TASK 4.9
│   └── Audit Category Implementation
│
├── TASK 4.10
│   └── Chốt Category Contract
│
├── TASK 4.11
│   └── Admin Category CRUD
│
├── TASK 4.12
│   └── Audit Product Implementation
│
├── TASK 4.13
│   └── Chốt Product Contract
│
├── TASK 4.14
│   └── Admin Product Approve / Reject / Hide
│
├── TASK 4.15
│   └── 4-Module Data Accuracy
│
├── TASK 4.16
│   └── 4-Module Security
│
├── TASK 4.17
│   └── 4-Module Automated Test
│
├── TASK 4.18
│   └── 4-Module UI Consistency
│
├── TASK 4.19
│   └── Runtime Verification
│
├── TASK 4.20
│   └── MongoDB Verification
│
├── TASK 4.21
│   └── Regression Smoke Test
│
└── TASK 4.22
    └── Phase 4 Final Review
```

---

## THỨ TỰ THỰC THI

```
Audit Shop Source
      ↓
Chốt Shop Contract
      ↓
Audit KYC Source
      ↓
Chốt KYC Contract
      ↓
Audit Category Source
      ↓
Chốt Category Contract
      ↓
Audit Product Source
      ↓
Chốt Product Contract
      ↓
Implement từng module
      ↓
Validation + Security
      ↓
Automated Test
      ↓
UI Consistency
      ↓
Runtime
      ↓
MongoDB
      ↓
Regression
      ↓
Final Review
      ↓
PHASE 4 DONE
```

---

## NGUYÊN TẮC PHASE 4

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
ADMIN + MODERATOR ALLOWED
        ↓
CUSTOMER + VENDOR DENIED
        ↓
STATE MACHINE RESPECTED
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

Phase 4

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
State machine protection
        ↓
Test
        ↓
Runtime
```
