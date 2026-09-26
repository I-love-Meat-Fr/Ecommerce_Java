# PHASE 1 — AUDIT TỔNG THỂ & SOURCE OF TRUTH

## 🎯 Mục tiêu Phase 1

Khảo sát 100% website `http://localhost:8081` và codebase thực tế trước khi code.

Phase 1 **không tạo code mới**, chỉ khóa:

```
Existing Implementation
        ↓
Audit Codebase (Read-only)
        ↓
Audit Website qua Browser
        ↓
Audit Role Logic
        ↓
Audit Database State
        ↓
Audit Documentation
        ↓
Source of Truth Report
```

Đầu ra Phase 1 là báo cáo `docs/REPORT/PHASE-1-AUDIT.md`. Báo cáo này là **input bắt buộc** cho Phase 2–8.

Luồng Phase 1:

```
DEV / TESTER
  ↓
Read source
  ↓
Browse website
  ↓
Test role logic
  ↓
Đếm lỗi 500/404/403 sai
  ↓
Báo cáo
  ↓
PHASE 1 DONE
```

---

## 1. NGUYÊN TẮC AUDIT

### 1.1 — Source-First Rule

Đọc source thực tế trước khi đánh giá.

Tối thiểu phải đọc:

```
SecurityConfig.java
ModerationGuard.java
CustomUserDetails.java
JwtAuthenticationEntryPoint.java
UserRole.java
AccountStatus.java
ShopStatus.java
User.java
Shop.java
Voucher.java
Product.java
Order.java
Review.java
Violation.java
Escalation.java
AuditLog.java
Banner.java
Category.java
Kyc.java
```

Sau đó đọc:

```
controller/admin/AdminUserController.java
controller/admin/AdminShopController.java
controller/admin/AdminVoucherController.java
controller/admin/AdminCategoryController.java
controller/admin/AdminKycController.java
controller/admin/AdminProductController.java
controller/admin/AdminOrderController.java
controller/admin/AdminReviewController.java
controller/admin/AdminViolationController.java
controller/admin/AdminEscalationController.java
controller/admin/AdminAuditController.java
controller/admin/AdminBannerController.java
```

Phân loại từng file:

```
[EXISTING]
[IMPLEMENTED]
[MISSING]
[BUG]
[DEPENDENCY]
[OUT OF SCOPE]
```

### 1.2 — Website-First Rule

Không tin roadmap. Phải mở web thật và kiểm tra.

Tối thiểu test URL:

```
/admin/users
/admin/categories
/admin/vouchers
/admin/shops
/admin/kyc
/admin/violations
/admin/orders
/admin/products
/admin/reviews
/admin/escalations
/admin/audit
/admin/banners
```

Với 4 role:

```
ADMIN
MODERATOR
CUSTOMER
VENDOR
```

### 1.3 — Không sửa gì trong Phase 1

```
❌ KHÔNG commit, push
❌ KHÔNG insert/update/delete MongoDB
❌ KHÔNG đổi code
❌ KHÔNG tạo file code mới
✅ CHỈ đọc + ghi báo cáo
```

---

## 2. ⚠️ ĐIỀU KIỆN BẮT ĐẦU

Phase 1 chạy được khi:

```
Codebase đã checkout
        +
MongoDB đang chạy
        +
App đang chạy tại localhost:8081
        +
Có ít nhất 1 account ADMIN, MODERATOR, CUSTOMER, VENDOR trong DB
```

Nếu thiếu account:

```
[DEPENDENCY — SEED DATA]
```

Không tự tạo account admin test trong Phase 1. Báo cáo để user tạo.

---

## 3. ⚠️ PHẠM VI PHASE 1

### CHỈ LÀM

```
Đọc source code thực tế
Đọc documentation hiện có (docs/)
Browse website với 4 role
Test CRUD cơ bản
Đếm lỗi 500/404/403 sai
Đếm file thiếu/broken
Đếm template thiếu
Ghi báo cáo vào docs/REPORT/PHASE-1-AUDIT.md
```

### ❌ KHÔNG ĐƯỢC LÀM

```
Sửa code
Tạo file code mới
Commit
Đổi SecurityConfig
Đổi role logic
Refactor
Test tự động
```

---

## TASK 1.1 — AUDIT CODEBASE (BACKEND)

### 🎯 Mục tiêu

Xác định chính xác code nào có, code nào thiếu, code nào broken.

### Cách làm

Đọc từng file trong danh sách ở mục 1.1.

Với mỗi file ghi nhận:

```
File path
Tổng số dòng
Có @Controller / @Service / @Repository không?
Có @PreAuthorize hoặc check role không?
Có method CRUD đầy đủ không?
Có TODO / FIXME / stub không?
```

### DONE

Có bảng:

```
| File | Status | Lines | Methods | Role Check | Notes |
|------|--------|-------|---------|------------|-------|
| AdminUserController.java | EXISTING | 120 | 6 | YES | OK |
| AdminVoucherController.java | MISSING | 0 | 0 | N/A | Need create |
| ... | | | | | |
```

---

## TASK 1.2 — AUDIT SECURITY CONFIG

### 🎯 Mục tiêu

Xác minh SecurityConfig đang config gì.

### Cách làm

Đọc `SecurityConfig.java`. Ghi nhận:

```
- Tất cả permitAll() hiện có
- Tất cả hasRole / hasAnyRole
- Tất cả requestMatchers theo pattern
- Có AccessDeniedHandler không?
- Có custom AuthenticationEntryPoint không?
- Có CSRF config không?
- Có CORS config không?
```

### DONE

Bảng:

```
| Pattern | Current Role | Should Be | Status |
|---------|--------------|-----------|--------|
| /admin/** | ADMIN | Mixed | NEED FIX |
| /admin/audit/** | (inherits ADMIN) | ADMIN only | OK |
| ... | | | |
```

---

## TASK 1.3 — AUDIT MODERATION GUARD

### 🎯 Mục tiêu

Xác minh logic role check trong service layer.

### Cách làm

Đọc `ModerationGuard.java`. Ghi nhận:

```
- Method name
- Logic check hiện tại
- Có throw exception không?
- Có message rõ ràng không?
```

So sánh với requirement:

```
Admin có được moderate không? → CẦN
Moderator có được moderate không? → CẦN
Customer/Vendor có được moderate không? → KHÔNG
```

### DONE

```
| Logic | Current | Required | Status |
|-------|---------|----------|--------|
| Allow MODERATOR | YES | YES | OK |
| Allow ADMIN | NO | YES | BUG |
| Deny CUSTOMER | YES | YES | OK |
| ... | | | |
```

---

## TASK 1.4 — AUDIT TEMPLATE (FRONTEND)

### 🎯 Mục tiêu

Xác minh template admin có đủ không.

### Cách làm

Liệt kê `templates/admin/*.html`. Với mỗi file ghi nhận:

```
- File có tồn tại không?
- Có dùng Admin Shell không?
- Có link đúng route không?
- Có form post đúng action không?
```

### DONE

```
| Template | Status | Action Forms | Uses Admin Shell |
|----------|--------|--------------|------------------|
| user-list.html | EXISTING | YES | YES |
| user-detail.html | EXISTING | YES | YES |
| voucher-list.html | EXISTING | ? | ? |
| voucher-create.html | EXISTING | YES | YES |
| voucher-edit.html | MISSING | - | - |
| ... | | | |
```

---

## TASK 1.5 — AUDIT DATABASE (MongoDB)

### 🎯 Mục tiêu

Đếm record hiện có trong MongoDB.

### Cách làm

Dùng MongoDB Compass hoặc mongosh:

```
db.users.countDocuments()
db.shops.countDocuments()
db.vouchers.countDocuments({type: "WEB"})
db.vouchers.countDocuments({type: "SHOP"})
db.products.countDocuments()
db.orders.countDocuments()
db.reviews.countDocuments()
db.auditlogs.countDocuments()
db.banners.countDocuments()
db.categories.countDocuments()
db.violations.countDocuments()
db.escalations.countDocuments()
db.kycs.countDocuments()
```

### DONE

```
| Collection | Count | Notes |
|------------|-------|-------|
| users | 25 | OK |
| shops | 8 | OK |
| vouchers (WEB) | 5 | OK |
| vouchers (SHOP) | 12 | OK |
| ... | | |
```

---

## TASK 1.6 — AUDIT WEBSITE (BROWSER)

### 🎯 Mục tiêu

Mở web, test từng URL với từng role.

### Cách làm

Với mỗi role × URL:

```
1. Login với account tương ứng
2. Truy cập URL
3. Ghi nhận:
   - HTTP status (200/404/500/403)
   - Page render OK?
   - Có exception trong console?
   - Có flash message lỗi?
4. Click vào từng element
5. Test 1 action CRUD
```

### DONE

Bảng 4 role × 12 URL = 48 ô test:

```
| URL | ADMIN | MODERATOR | CUSTOMER | VENDOR |
|-----|-------|-----------|----------|--------|
| /admin/users | 200 | 200* | 403 | 403 |
| /admin/audit | 200 | 200 | 403 | 403 |
| /admin/vouchers | 404 | 404 | 403 | 403 |
| ... | | | | |
```

`*` = đáng lẽ phải 200 (BUG) hoặc đáng lẽ phải 403 (BUG) → ghi rõ.

---

## TASK 1.7 — ĐẾM LỖI

### 🎯 Mục tiêu

Tổng hợp số liệu.

### Cách làm

Đếm:

```
Số lỗi 500
Số lỗi 404
Số lỗi 403 sai logic
Số file controller thiếu
Số file template thiếu
Số test broken
Số method bị stub
```

### DONE

```
500 errors: N
404 errors: N
403 wrong: N
Missing controllers: N
Missing templates: N
Broken tests: N
Stub methods: N
Total issues: N
```

---

## TASK 1.8 — GHI BÁO CÁO

### 🎯 Mục tiêu

Tạo `docs/REPORT/PHASE-1-AUDIT.md`.

### DONE

File tồn tại với format:

```markdown
# PHASE 1 AUDIT REPORT — DD/MM/YYYY

## 1. CODE AUDIT
[Bảng từ TASK 1.1]

## 2. SECURITY AUDIT
[Bảng từ TASK 1.2]

## 3. MODERATION GUARD AUDIT
[Bảng từ TASK 1.3]

## 4. TEMPLATE AUDIT
[Bảng từ TASK 1.4]

## 5. DATABASE AUDIT
[Bảng từ TASK 1.5]

## 6. WEBSITE AUDIT (4 ROLES × 12 URLS)
[Bảng từ TASK 1.6]

## 7. ISSUE SUMMARY
[Bảng từ TASK 1.7]

## 8. KẾT LUẬN & ĐỀ XUẤT
- Phase nào cần fix trước?
- Bug nghiêm trọng nhất?
- Ưu tiên thứ tự?
```

---

## TASK 1.9 — PHASE 1 FINAL REVIEW

### 🎯 Mục tiêu

Đóng Phase 1 với checklist.

### DONE Checklist

```
[ ] TASK 1.1 đã đọc hết file backend
[ ] TASK 1.2 đã đọc SecurityConfig
[ ] TASK 1.3 đã đọc ModerationGuard
[ ] TASK 1.4 đã đọc template admin
[ ] TASK 1.5 đã đếm MongoDB
[ ] TASK 1.6 đã test web với 4 role
[ ] TASK 1.7 đã đếm lỗi
[ ] TASK 1.8 đã ghi báo cáo
[ ] TASK 1.9 đã review checklist
```

---

## 🛑 STOP RULE

Sau khi hoàn thành Phase 1:

**DỪNG.**

Không tự chuyển sang Phase 2.

Chỉ khi user xác nhận:

```
PHASE 1 DONE
```

mới tiếp tục:

```
PHASE 2 — SECURITY FIX
```

---

## 📋 TỔNG KẾT PHASE 1

```
PHASE 1 — AUDIT TỔNG THỂ & SOURCE OF TRUTH
│
├── TASK 1.1
│   └── Audit Codebase (Backend)
│
├── TASK 1.2
│   └── Audit Security Config
│
├── TASK 1.3
│   └── Audit Moderation Guard
│
├── TASK 1.4
│   └── Audit Template (Frontend)
│
├── TASK 1.5
│   └── Audit Database (MongoDB)
│
├── TASK 1.6
│   └── Audit Website (Browser)
│
├── TASK 1.7
│   └── Đếm lỗi
│
├── TASK 1.8
│   └── Ghi báo cáo
│
└── TASK 1.9
    └── Phase 1 Final Review
```

---

## THỨ TỰ THỰC THI

```
Đọc Backend Source
      ↓
Đọc SecurityConfig + ModerationGuard
      ↓
Đọc Template Admin
      ↓
Đếm MongoDB
      ↓
Test Web với 4 role
      ↓
Tổng hợp bảng lỗi
      ↓
Ghi báo cáo
      ↓
Review checklist
      ↓
PHASE 1 DONE
```

---

## NGUYÊN TẮC PHASE 1

```
READ SOURCE FIRST
        ↓
READ TEMPLATE SECOND
        ↓
READ DATABASE THIRD
        ↓
TEST WEBSITE FOURTH
        ↓
COUNT ISSUES FIFTH
        ↓
WRITE REPORT SIXTH
        ↓
STOP
```

---

## Điểm quan trọng nhất

Phase 1 **KHÔNG PHẢI** là code Phase.

Phase 1 là **đo lường thực trạng** trước khi đầu tư thời gian vào fix.

Báo cáo Phase 1 là kim chỉ nam cho toàn bộ 7 phase còn lại.
