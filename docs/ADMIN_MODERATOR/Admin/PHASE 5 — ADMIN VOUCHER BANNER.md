# PHASE 5 — ADMIN VOUCHER / BANNER

## 🎯 Mục tiêu Phase 5

Hoàn thiện và xác minh Admin WEB Voucher Management + Banner / PR theo đúng Workbook, business contract và implementation thực tế của source.

Phase 5 không tạo lại toàn bộ Voucher / Banner module.

Mục tiêu là:

```
Existing Voucher / Banner Implementation
        ↓
Audit
        ↓
Chốt Voucher / Banner Contract
        ↓
Bổ sung phần thực sự thiếu
        ↓
WEB / SHOP Isolation (cho Voucher)
        ↓
Validation
        ↓
Automated Test
        ↓
Runtime + MongoDB
        ↓
Regression
        ↓
VOUCHER + BANNER hoàn chỉnh
```

Luồng Admin:

```
ADMIN
  ↓
/admin/vouchers + /admin/banners
  ↓
ADMIN VOUCHER / BANNER
  ├── List
  ├── Create
  ├── Edit
  ├── Activate
  ├── Deactivate
  └── Delete theo Contract
```

Phase 5 chỉ tập trung vào:
- Voucher WEB (type=WEB, shopId=null)
- Banner / PR (do Admin tạo và quản lý)

---

## 1. VOUCHER / BANNER BUSINESS CONTRACT

### 1.1 — Voucher Contract

```
WEB Voucher
├── type = WEB
├── shopId = null
└── owner = ADMIN

SHOP Voucher
├── type = SHOP
├── shopId = Vendor Shop
└── owner = VENDOR (OUT OF SCOPE Phase 5)
```

Admin có thể quản lý WEB Voucher theo Workbook/Contract.

Admin không được:
- Edit SHOP Voucher
- Delete SHOP Voucher
- Activate SHOP Voucher
- Deactivate SHOP Voucher

Không được biến:
- WEB → SHOP
- SHOP → WEB

thông qua Admin flow.

### 1.2 — Banner Contract

```
Banner
├── id
├── title
├── imageUrl
├── linkUrl (optional)
├── position (HOME_TOP, HOME_MIDDLE, HOME_BOTTOM, CATEGORY_SIDEBAR, ...)
├── active
├── startDate
├── endDate
├── priority (sắp xếp thứ tự)
├── createdBy
└── createdAt
```

### 1.3 — Role × Module Matrix

```
Module                ADMIN  MODERATOR  CUSTOMER  VENDOR
/admin/vouchers       ALLOW  DENY       DENY      DENY
/admin/banners        ALLOW  DENY       DENY      DENY
```

---

## 2. ⚠️ ĐIỀU KIỆN BẮT ĐẦU

Phase 5 chỉ implementation sau khi:

```
PHASE 4 DONE (Shop/KYC/Category/Product)
        +
AuditAction enum có sẵn VOUCHER_CREATED, VOUCHER_UPDATED,
VOUCHER_ACTIVATED, VOUCHER_DEACTIVATED, VOUCHER_DELETED,
BANNER_CREATED, BANNER_UPDATED, BANNER_ACTIVATED,
BANNER_DEACTIVATED, BANNER_DELETED
        +
Nếu thiếu → [DEPENDENCY — ENUM] → dừng và báo cáo
```

---

## 3. SOURCE-FIRST RULE

Đọc source thực tế:

```
Voucher.java
VoucherType.java
VoucherFormReq.java
VoucherRepository.java
VoucherService.java
VoucherServiceImpl.java
VoucherController.java

admin/voucher-list.html
admin/voucher-create.html
admin/voucher-edit.html

vendor/voucher-list.html
vendor/voucher-create.html
vendor/voucher-edit.html

Banner.java
BannerRepository.java
BannerService.java
BannerServiceImpl.java
BannerController.java
admin/banner-list.html
admin/banner-create.html
admin/banner-edit.html

SecurityConfig.java
ModerationGuard.java
AuditLog.java
AuditLogRepository.java
AuditAction.java
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

## 4. ⚠️ PHẠM VI PHASE 5

### CHỈ LÀM

```
Admin WEB Voucher
Admin Banner
Audit implementation hiện tại
Chốt WEB Voucher contract
Chốt Delete / Deactivate contract
List WEB Voucher
Create / Edit WEB Voucher
Activate / Deactivate WEB Voucher
Delete theo Delete Contract
Validation
WEB / SHOP isolation
Banner CRUD
Banner position / priority
Banner active / inactive window (startDate / endDate)
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
Vendor SHOP Voucher Management
Customer Voucher UI
Checkout
Payment
Order
Refund
Product Management
Shop Management
User Management
Category Management
```

Không được:
- Tạo VoucherType mới
- Tạo Voucher status mới
- Đổi discount calculation
- Đổi Checkout Voucher logic
- Đổi Vendor Voucher workflow
- Đổi Customer Voucher workflow

Không:
- Refactor toàn bộ VoucherController
- Redesign toàn bộ Admin UI
- Redesign toàn bộ Voucher architecture

---

## TASK 5.1 — AUDIT VOUCHER IMPLEMENTATION

### 🎯 Mục tiêu

Xác định chính xác trạng thái Voucher hiện tại.

### Đọc source

```
Voucher.java
VoucherType.java
VoucherFormReq.java
VoucherRepository.java
VoucherService.java
VoucherServiceImpl.java
VoucherController.java
Admin Voucher templates
Vendor Voucher templates
SecurityConfig
Voucher tests
```

### Kiểm tra

```
List
Create
Edit
Delete
Activate
Deactivate
Search
Filter
Pagination
Validation
WEB / SHOP separation
active
shopId
productIds
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

## TASK 5.2 — CHỐT WEB VOUCHER CONTRACT

```
Thành phần       WEB Voucher
type             WEB
shopId           null
Admin List       Có
Admin Create     Có
Admin Edit       Theo Workbook
Admin Activate   Theo Workbook
Admin Deactivate Theo Workbook
Admin Delete     Theo Delete Contract
```

Contract:

```
ADMIN
 ↓
WEB Voucher
 ↓
type = WEB
 ↓
shopId = null
```

Admin không được thao tác `type = SHOP`.

### DONE

WEB Voucher ownership và Admin scope được xác định rõ.

---

## TASK 5.3 — CHỐT DELETE / DEACTIVATE CONTRACT

### Trường hợp A

Nếu Contract quy định `Delete = Deactivate`:

```
Delete
 ↓
active = false
```

UI có thể dùng `Deactivate` thay cho Delete nếu Team/Workbook thống nhất.

### Trường hợp B

Nếu Contract quy định `Delete = Hard Delete` và `Deactivate = active=false`:

```
Delete
 ↓
remove document

Deactivate
 ↓
active = false
```

Không tự chọn A/B. Nếu chưa chốt → `[DEPENDENCY — TEAM]`.

### DONE

Delete/Deactivate có definition rõ ràng.

---

## TASK 5.4 — ADMIN WEB VOUCHER LIST

### 🎯 Mục tiêu

Đảm bảo /admin/vouchers chỉ hiển thị WEB Voucher.

```
Route: /admin/vouchers
Flow:
  /admin/vouchers
        ↓
  VoucherController
        ↓
  VoucherService
        ↓
  findByType(WEB)
        ↓
  MongoDB
```

Phải đảm bảo:

```
WEB  → HIỂN THỊ
SHOP → KHÔNG HIỂN THỊ
```

### DONE

Admin Voucher List chỉ hiển thị WEB Voucher đúng contract.

---

## TASK 5.5 — CREATE WEB VOUCHER

### 🎯 Mục tiêu

Verify và hoàn thiện Create WEB Voucher.

```
Flow:
Admin
 ↓
/admin/vouchers/create
 ↓
VoucherFormReq
 ↓
Validation
 ↓
createWebVoucher(...)
 ↓
MongoDB
```

Bắt buộc:

```
type = WEB
shopId = null
```

Không cho Admin tạo SHOP Voucher.

Fields cần audit:

```
code
name
discountType
discountValue
maxDiscountAmount
minOrderValue
quantity
startDate
endDate
productIds
```

### Validation

```
Code không rỗng.
Code format.
Code duplicate.
Discount type.
Discount value.
Percent limit theo contract.
Quantity.
Date.
endDate >= startDate.
```

### DONE

Create WEB Voucher lưu đúng `type = WEB, shopId = null` và validation đúng contract.

---

## TASK 5.6 — EDIT WEB VOUCHER

### 🎯 Mục tiêu

Bổ sung Edit nếu Workbook yêu cầu và source thực tế chưa có Admin flow phù hợp.

```
Flow:
Admin
 ↓
GET /admin/vouchers/edit/{id}
 ↓
Load Voucher
 ↓
Check WEB
 ↓
Validate
 ↓
Update
 ↓
MongoDB
```

POST: `/admin/vouchers/edit/{id}`

Phải guard:

```
voucher.type != WEB
        ↓
DENY
```

Không cho:

```
SHOP → Edit bởi Admin
WEB → SHOP
```

Không cho thay đổi ownership: `shopId = null` thành Shop của Vendor.

### DONE

Admin chỉ Edit được WEB Voucher.

---

## TASK 5.7 — DELETE WEB VOUCHER

### 🎯 Mục tiêu

Bảo vệ Delete theo WEB-only contract.

```
Endpoint: POST /admin/vouchers/delete/{id}
Flow:
  Load Voucher
      ↓
  Check type
      ↓
  WEB?
   ├── YES → Delete theo Contract
   └── NO  → DENY
```

Nếu source hiện tại: `active = false` thì giữ đúng contract đã chốt ở TASK 5.3.

### DONE

Admin Delete không thể tác động SHOP Voucher.

---

## TASK 5.8 — ACTIVATE / DEACTIVATE WEB VOUCHER

### 🎯 Mục tiêu

Bổ sung Activate / Deactivate nếu Workbook/Contract yêu cầu.

```
Activate Flow:
Admin
 ↓
POST /admin/vouchers/activate/{id}
 ↓
Load Voucher
 ↓
Check WEB
 ↓
active = true
 ↓
save

Deactivate Flow:
Admin
 ↓
POST /admin/vouchers/deactivate/{id}
 ↓
Load Voucher
 ↓
Check WEB
 ↓
active = false
 ↓
save
```

Chỉ thay đổi `active`. Không tự thay đổi `code, type, shopId, discount, quantity, productIds, dates`.

### DONE

Activate / Deactivate WEB Voucher hoạt động đúng contract.

---

## TASK 5.9 — VOUCHER VALIDATION

### 🎯 Mục tiêu

Audit validation của Create/Edit.

Kiểm tra:

```
code / name / discountType / discountValue / maxDiscountAmount
minOrderValue / quantity / startDate / endDate / productIds
existsByCode() cho Create/Edit nếu source/business rule yêu cầu
```

Đặc biệt: `endDate >= startDate` phải được đảm bảo ở WEB Voucher.

### DONE

Admin không thể lưu WEB Voucher invalid theo business contract.

---

## TASK 5.10 — WEB / SHOP VOUCHER ISOLATION

### 🎯 Mục tiêu

Đảm bảo mọi Admin Voucher operation đều kiểm tra ownership.

```
Test quan trọng:
SHOP Voucher ID
        ↓
Admin Edit → DENIED
Admin Delete → DENIED
Admin Activate → DENIED
Admin Deactivate → DENIED

WEB Voucher
        ↓
Admin → ALLOW
```

### DONE

Không có Admin operation nào tác động nhầm SHOP Voucher.

---

## TASK 5.11 — VOUCHER DATA ACCURACY

### 🎯 Mục tiêu

Đối chiếu Admin Voucher với MongoDB.

Ví dụ:

```
MongoDB: WEB = 5, SHOP = 8
/admin/vouchers phải chỉ hiển thị WEB = 5
Không được: WEB + SHOP = 13
```

### DONE

Admin WEB Voucher khớp dữ liệu MongoDB thực tế.

---

## TASK 5.12 — ADMIN VOUCHER SECURITY

### 🎯 Mục tiêu

Chỉ ADMIN được truy cập và thao tác Admin Voucher.

```
ADMIN       → /admin/vouchers → ALLOW
CUSTOMER    → /admin/vouchers → DENIED
VENDOR      → /admin/vouchers → DENIED
ANONYMOUS   → /admin/vouchers → LOGIN / DENIED
```

### DONE

Không có Customer/Vendor/Anonymous bypass Admin Voucher.

---

## TASK 5.13 — AUDIT BANNER IMPLEMENTATION

### 🎯 Mục tiêu

Xác định chính xác trạng thái Banner hiện tại.

### Đọc source

```
Banner.java
BannerRepository.java
BannerService.java
BannerServiceImpl.java
BannerController.java
banner-list.html
banner-create.html
banner-edit.html
```

### DONE

Báo cáo `[CURRENT IMPLEMENTATION] / [MISSING] / [BUG] / [DEPENDENCY]`.

---

## TASK 5.14 — CHỐT BANNER CONTRACT

```
Thành phần       Admin
List             Có
Create           Có
Edit             Có
Activate         Có
Deactivate       Có
Delete           Có (soft delete)
```

### DONE

Banner contract rõ ràng.

---

## TASK 5.15 — ADMIN BANNER CRUD

### 🎯 Mục tiêu

Verify và hoàn thiện Banner CRUD.

```
Create Flow:
Admin
 ↓
POST /admin/banners/create
 ↓
Validate title, imageUrl, position
 ↓
Save
 ↓
MongoDB

Edit Flow:
Admin
 ↓
POST /admin/banners/{id}/edit
 ↓
Validate
 ↓
Update
 ↓
MongoDB

Activate / Deactivate:
Admin
 ↓
POST /admin/banners/{id}/activate
 ↓
active = true
 ↓
AuditLog
 ↓
MongoDB

Delete:
Admin
 ↓
POST /admin/banners/{id}/delete
 ↓
Soft delete (active = false HOẶC xóa document)
 ↓
MongoDB
```

Bắt buộc:

```
Banner chỉ hiển thị ở Customer khi: active=true, currentDate in [startDate, endDate], priority order.
Position unique hoặc priority xác định thứ tự.
```

### DONE

Banner CRUD hoạt động đúng + có AuditLog.

---

## TASK 5.16 — BANNER DISPLAY LOGIC

### 🎯 Mục tiêu

Verify banner chỉ hiển thị khi đúng điều kiện.

```
Customer truy cập trang chủ:
 ↓
Load banner active
 ↓
Filter:
   active = true
   currentDate >= startDate
   currentDate <= endDate
 ↓
Sort theo priority
 ↓
Hiển thị
```

### DONE

Banner chỉ hiển thị đúng điều kiện.

---

## TASK 5.17 — VOUCHER + BANNER AUTOMATED TEST

### 🎯 Mục tiêu

Bổ sung automated test.

Test tối thiểu:

```
Voucher Security
[ ] Admin allowed
[ ] Customer denied
[ ] Vendor denied
[ ] Anonymous denied

Voucher List
[ ] List WEB Voucher
[ ] SHOP Voucher không xuất hiện

Voucher Create
[ ] Create WEB Voucher
[ ] type = WEB
[ ] shopId = null
[ ] Validation

Voucher Edit
[ ] Edit WEB Voucher
[ ] Edit SHOP Voucher denied

Voucher Delete
[ ] Delete WEB Voucher
[ ] Delete SHOP Voucher denied

Voucher Activate / Deactivate
[ ] Activate WEB Voucher
[ ] Deactivate WEB Voucher
[ ] SHOP Voucher denied

Voucher Empty State
[ ] Empty WEB Voucher

Banner
[ ] Create Banner
[ ] Edit Banner
[ ] Activate / Deactivate
[ ] Delete
[ ] Position / priority
[ ] Display window (startDate / endDate)
```

### DONE

Voucher + Banner có automated test.

---

## TASK 5.18 — VOUCHER + BANNER UI CONSISTENCY

### 🎯 Mục tiêu

Đảm bảo UI sử dụng đúng Admin Shell.

Kiểm tra:

```
Header / Sidebar / Navigation / Breadcrumb / Page title
Table / Form / Action buttons
Flash message / Error message / Empty State
```

Sidebar:

```
Voucher WEB → /admin/vouchers
Banner / PR → /admin/banners
```

### DONE

Voucher + Banner UI nhất quán với Admin Core.

---

## TASK 5.19 — EMPTY STATE

### 🎯 Mục tiêu

Xác minh Empty State.

```
MongoDB: WEB Voucher = 0
Admin /admin/vouchers → Empty State
MongoDB: Banner = 0
Admin /admin/banners → Empty State
```

Không:

```
Hiển thị SHOP Voucher.
Hiển thị fake data.
Crash template.
```

### DONE

Empty State đúng.

---

## TASK 5.20 — RUNTIME VERIFICATION

### 🎯 Mục tiêu

Kiểm tra application thực tế.

Flow:

```
Admin Login
    ↓
/admin/vouchers
    ↓
List / Create / Edit / Activate / Deactivate / Delete
    ↓
MongoDB

/admin/banners
    ↓
List / Create / Edit / Activate / Deactivate / Delete
    ↓
MongoDB

Customer Home
    ↓
Banner hiển thị đúng
```

### DONE

Voucher + Banner đã được kiểm tra trên application thực tế.

---

## TASK 5.21 — MONGODB VERIFICATION

### 🎯 Mục tiêu

Xác minh dữ liệu Voucher + Banner trực tiếp trên MongoDB.

Kiểm tra:

```
Voucher.type / Voucher.shopId / Voucher.active
Voucher.quantity / Voucher.used
Voucher.startDate / Voucher.endDate

Banner.active / Banner.startDate / Banner.endDate
Banner.position / Banner.priority
```

### DONE

MongoDB phản ánh đúng kết quả của Voucher + Banner operations.

---

## TASK 5.22 — REGRESSION SMOKE TEST

### 🎯 Mục tiêu

Đảm bảo Phase 5 không phá các flow đang tồn tại.

```
ADMIN
[ ] Admin User (Phase 3)
[ ] Admin Shop (Phase 4)
[ ] Admin Product (Phase 4)

CUSTOMER
[ ] Customer Home (Banner hiển thị)
[ ] Customer Voucher / Checkout (WEB Voucher hoạt động)

VENDOR
[ ] Vendor SHOP Voucher (không bị ảnh hưởng)
```

### DONE

Không có regression nghiêm trọng do Phase 5 gây ra.

---

## TASK 5.23 — PHASE 5 FINAL REVIEW

### Báo cáo

```
[VOUCHER CONTRACT]
WEB Voucher Ownership → PASS/FAIL
SHOP Voucher Ownership → PASS/FAIL
type = WEB → PASS/FAIL
shopId = null → PASS/FAIL

[VOUCHER LIST]
List WEB → PASS/FAIL
SHOP Isolation → PASS/FAIL

[VOUCHER CREATE]
Create → PASS/FAIL
Validation → PASS/FAIL
type → PASS/FAIL
shopId → PASS/FAIL

[VOUCHER EDIT]
Edit WEB → PASS/FAIL
Edit SHOP denied → PASS/FAIL

[VOUCHER DELETE]
Delete → PASS/FAIL
Delete Contract → PASS/FAIL
SHOP isolation → PASS/FAIL

[VOUCHER ACTIVATE / DEACTIVATE]
Activate → PASS/FAIL
Deactivate → PASS/FAIL
active state → PASS/FAIL

[VOUCHER SECURITY]
Admin → PASS/FAIL
Customer → PASS/FAIL
Vendor → PASS/FAIL
Anonymous → PASS/FAIL

[BANNER]
Create → PASS/FAIL
Edit → PASS/FAIL
Activate → PASS/FAIL
Deactivate → PASS/FAIL
Delete → PASS/FAIL
Display window → PASS/FAIL
Position / priority → PASS/FAIL

[BANNER SECURITY]
Admin → PASS/FAIL
Customer → PASS/FAIL
Vendor → PASS/FAIL

[DATA]
MongoDB → PASS/FAIL
Admin UI → PASS/FAIL
Customer Sync (Banner) → PASS/FAIL

[TEST]
Automated Test → PASS/FAIL/SKIPPED
Maven Test → PASS/FAIL

[RUNTIME]
Runtime → PASS/FAIL

[REGRESSION]
Admin → PASS/FAIL
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
[WEB/SHOP ISOLATION]
[TEST RESULT]
[RUNTIME RESULT]
[MONGODB RESULT]
[REGRESSION RESULT]
```

---

## 🛑 STOP RULE

Sau khi hoàn thành Phase 5:

**DỪNG.**

Không tự triển khai:

```
Order Management (Phase 6)
Review Moderation (Phase 6)
Violation Enforcement (Phase 6)
Escalation (Phase 6)
```

Chỉ khi user xác nhận:

```
PHASE 5 DONE
```

mới chuyển sang:

```
PHASE 6 — ADMIN ORDER / REVIEW / VIOLATION / ESCALATION
```

---

## 📋 TỔNG KẾT PHASE 5

```
PHASE 5 — ADMIN VOUCHER / BANNER
│
├── TASK 5.1
│   └── Audit Voucher Implementation
│
├── TASK 5.2
│   └── Chốt WEB Voucher Contract
│
├── TASK 5.3
│   └── Chốt Delete / Deactivate Contract
│
├── TASK 5.4
│   └── Admin WEB Voucher List
│
├── TASK 5.5
│   └── Create WEB Voucher
│
├── TASK 5.6
│   └── Edit WEB Voucher
│
├── TASK 5.7
│   └── Delete WEB Voucher
│
├── TASK 5.8
│   └── Activate / Deactivate WEB Voucher
│
├── TASK 5.9
│   └── Voucher Validation
│
├── TASK 5.10
│   └── WEB / SHOP Voucher Isolation
│
├── TASK 5.11
│   └── Voucher Data Accuracy
│
├── TASK 5.12
│   └── Admin Voucher Security
│
├── TASK 5.13
│   └── Audit Banner Implementation
│
├── TASK 5.14
│   └── Chốt Banner Contract
│
├── TASK 5.15
│   └── Admin Banner CRUD
│
├── TASK 5.16
│   └── Banner Display Logic
│
├── TASK 5.17
│   └── Voucher + Banner Automated Test
│
├── TASK 5.18
│   └── Voucher + Banner UI Consistency
│
├── TASK 5.19
│   └── Empty State
│
├── TASK 5.20
│   └── Runtime Verification
│
├── TASK 5.21
│   └── MongoDB Verification
│
├── TASK 5.22
│   └── Regression Smoke Test
│
└── TASK 5.23
    └── Phase 5 Final Review
```

---

## THỨ TỰ THỰC THI

```
Audit Voucher Source
      ↓
Chốt WEB Voucher Contract
      ↓
Chốt Delete / Deactivate Contract
      ↓
Audit Banner Source
      ↓
Chốt Banner Contract
      ↓
List WEB Voucher
      ↓
Create / Edit / Delete
      ↓
Activate / Deactivate
      ↓
Validation
      ↓
WEB / SHOP Isolation
      ↓
Security
      ↓
Banner CRUD
      ↓
Banner Display Logic
      ↓
Data Accuracy
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
PHASE 5 DONE
```

---

## NGUYÊN TẮC PHASE 5

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
WEB ONLY (Voucher)
        ↓
NEVER TOUCH SHOP OWNERSHIP
        ↓
NO UNAUTHORIZED BUSINESS RULE CHANGES
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

Phase 5

```
≠
Xây lại Voucher / Banner từ đầu
```

mà là:

```
Existing Voucher / Banner
      ↓
Audit
      ↓
Giữ phần đúng
      ↓
Sửa phần sai
      ↓
Bổ sung phần Workbook thực sự yêu cầu
      ↓
WEB / SHOP isolation
        ↓
Test
        ↓
Runtime
```
