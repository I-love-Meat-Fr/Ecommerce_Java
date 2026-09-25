# PHASE 7 — AUDIT LOG VIEWER + ROLE LOGIC REGRESSION

## 🎯 Mục tiêu Phase 7

Hoàn thiện Audit Log Viewer và verify toàn bộ Role Logic không bị phá qua 6 phase trước.

Phase 7 là phase **VERIFY**, không tạo logic mới.

Mục tiêu là:

```
Existing AuditLog Implementation
        ↓
Verify coverage
        ↓
Build Audit Log Viewer
        ↓
Role Matrix Regression Test
        ↓
Cascade Logic Regression Test
        ↓
PHASE 7 DONE
```

---

## 1. AUDIT LOG CONTRACT

### 1.1 — AuditLog Structure

```
AuditLog
├── id
├── action (AuditAction enum)
├── actor (username)
├── actorRole (ADMIN / MODERATOR)
├── targetType (USER / SHOP / PRODUCT / REVIEW / VOUCHER / BANNER / ORDER / VIOLATION / ESCALATION / KYC / CATEGORY)
├── targetId
├── before (object before change, optional)
├── after (object after change, optional)
├── note (optional)
├── ipAddress (optional)
├── userAgent (optional)
└── createdAt
```

### 1.2 — AuditAction Enum

```
USER_LOCKED
USER_UNLOCKED
USER_STATUS_CHANGED
SHOP_APPROVED
SHOP_REJECTED
SHOP_ACTIVATED
SHOP_DEACTIVATED
KYC_APPROVED
KYC_REJECTED
CATEGORY_CREATED
CATEGORY_UPDATED
CATEGORY_DELETED
PRODUCT_APPROVED
PRODUCT_REJECTED
PRODUCT_HIDDEN
PRODUCT_SHOWN
VOUCHER_CREATED
VOUCHER_UPDATED
VOUCHER_ACTIVATED
VOUCHER_DEACTIVATED
VOUCHER_DELETED
BANNER_CREATED
BANNER_UPDATED
BANNER_ACTIVATED
BANNER_DEACTIVATED
BANNER_DELETED
ORDER_STATUS_UPDATED
ORDER_CANCELLED
ORDER_REFUNDED
REVIEW_APPROVED
REVIEW_REJECTED
REVIEW_HIDDEN
VIOLATION_CREATED
VIOLATION_RESOLVED
ESCALATION_APPROVED
ESCALATION_REJECTED
ESCALATION_RESOLVED
```

---

## 2. ⚠️ ĐIỀU KIỆN BẮT ĐẦU

Phase 7 chỉ chạy khi:

```
PHASE 6 DONE
        +
AuditAction enum có sẵn đủ action cần thiết
```

---

## 3. SOURCE-FIRST RULE

Đọc source thực tế:

```
AuditLog.java
AuditLogRepository.java
AuditAction.java
AdminAuditController.java
AdminAuditService.java
AdminAuditServiceImpl.java
admin/audit-list.html
admin/audit-detail.html

SecurityConfig.java
ModerationGuard.java
UserRole.java
```

---

## 4. ⚠️ PHẠM VI PHASE 7

### CHỈ LÀM

```
Verify AuditLog coverage cho từng action
Build Audit Log Viewer (nếu chưa có)
List AuditLog với filter
View AuditLog detail
Search theo actor / target / action
Pagination
Role Matrix Regression Test (4 role × 12 module)
Cascade Logic Regression Test
Role logic không bị phá
```

### ❌ KHÔNG ĐƯỢC LÀM

```
Refactor Security
Đổi UserRole enum
Đổi AuditAction enum (chỉ verify)
Đổi logic nghiệp vụ
Refactor 6 module trước
```

---

## TASK 7.1 — AUDIT AUDIT LOG IMPLEMENTATION

### 🎯 Mục tiêu

Xác định chính xác trạng thái Audit Log hiện tại.

### Đọc source

```
AuditLog.java
AuditLogRepository.java
AdminAuditController.java
AdminAuditService.java
AdminAuditServiceImpl.java
audit-list.html
audit-detail.html
```

### DONE

Báo cáo `[CURRENT IMPLEMENTATION] / [MISSING] / [BUG] / [DEPENDENCY]`.

---

## TASK 7.2 — CHỐT AUDIT LOG CONTRACT

```
Thành phần       Admin
List             Có
View Detail      Có
Filter           Có (action / actor / targetType / date range)
Search           Có
Pagination       Có
Export           Theo Workbook
Delete           DENY (immutable)
```

### DONE

Audit Log contract rõ ràng.

---

## TASK 7.3 — AUDIT LOG COVERAGE CHECK

### 🎯 Mục tiêu

Verify mỗi action quan trọng có ghi AuditLog không.

Test:

```
Lock User → AuditLog (USER_LOCKED)?
Unlock User → AuditLog (USER_UNLOCKED)?
Edit User Status → AuditLog (USER_STATUS_CHANGED)?

Approve Shop → AuditLog (SHOP_APPROVED)?
Reject Shop → AuditLog (SHOP_REJECTED)?
Activate Shop → AuditLog (SHOP_ACTIVATED)?
Deactivate Shop → AuditLog (SHOP_DEACTIVATED)?

Approve KYC → AuditLog (KYC_APPROVED)?
Reject KYC → AuditLog (KYC_REJECTED)?

Create Category → AuditLog (CATEGORY_CREATED)?
Edit Category → AuditLog (CATEGORY_UPDATED)?
Delete Category → AuditLog (CATEGORY_DELETED)?

Approve Product → AuditLog (PRODUCT_APPROVED)?
Reject Product → AuditLog (PRODUCT_REJECTED)?
Hide Product → AuditLog (PRODUCT_HIDDEN)?
Show Product → AuditLog (PRODUCT_SHOWN)?

Create WEB Voucher → AuditLog (VOUCHER_CREATED)?
Edit WEB Voucher → AuditLog (VOUCHER_UPDATED)?
Activate WEB Voucher → AuditLog (VOUCHER_ACTIVATED)?
Deactivate WEB Voucher → AuditLog (VOUCHER_DEACTIVATED)?
Delete WEB Voucher → AuditLog (VOUCHER_DELETED)?

Create Banner → AuditLog (BANNER_CREATED)?
Edit Banner → AuditLog (BANNER_UPDATED)?
Activate Banner → AuditLog (BANNER_ACTIVATED)?
Deactivate Banner → AuditLog (BANNER_DEACTIVATED)?
Delete Banner → AuditLog (BANNER_DELETED)?

Update Order Status → AuditLog (ORDER_STATUS_UPDATED)?
Cancel Order → AuditLog (ORDER_CANCELLED)?
Refund Order → AuditLog (ORDER_REFUNDED)?

Approve Review → AuditLog (REVIEW_APPROVED)?
Reject Review → AuditLog (REVIEW_REJECTED)?
Hide Review → AuditLog (REVIEW_HIDDEN)?

Create Violation → AuditLog (VIOLATION_CREATED)?
Resolve Violation → AuditLog (VIOLATION_RESOLVED)?

Approve Escalation → AuditLog (ESCALATION_APPROVED)?
Reject Escalation → AuditLog (ESCALATION_REJECTED)?
Resolve Escalation → AuditLog (ESCALATION_RESOLVED)?
```

### DONE

Báo cáo coverage:

```
[✓] USER_LOCKED — covered
[✗] SHOP_REJECTED — MISSING (BUG)
...
```

Nếu có BUG → fix ngay trong Phase 7.

---

## TASK 7.4 — ADMIN AUDIT LOG VIEWER

### 🎯 Mục tiêu

Đảm bảo /admin/audit hoạt động đầy đủ.

```
Route: /admin/audit
Route: /admin/audit/{id}
```

Phải có:

```
List:
- Filter by action
- Filter by actor
- Filter by targetType
- Filter by date range
- Search by targetId
- Pagination

Detail:
- AuditLog id
- Action
- Actor + actorRole
- TargetType + targetId
- before (JSON)
- after (JSON)
- IP address (optional)
- User agent (optional)
- Created at
```

### DONE

Admin Audit Log Viewer hoạt động đầy đủ.

---

## TASK 7.5 — ROLE MATRIX REGRESSION TEST

### 🎯 Mục tiêu

Verify toàn bộ role logic vẫn đúng sau 6 phase trước.

```
                    ADMIN  MODERATOR  CUSTOMER  VENDOR
/admin/users        ALLOW  ALLOW      DENY      DENY
/admin/categories   ALLOW  ALLOW      DENY      DENY
/admin/vouchers     ALLOW  DENY       DENY      DENY
/admin/shops        ALLOW  ALLOW      DENY      DENY
/admin/kyc          ALLOW  ALLOW      DENY      DENY
/admin/violations   ALLOW  ALLOW      DENY      DENY
/admin/orders       ALLOW  DENY       DENY      DENY
/admin/products     ALLOW  ALLOW      DENY      DENY
/admin/reviews      ALLOW  ALLOW      DENY      DENY
/admin/escalations  ALLOW  ALLOW      DENY      DENY
/admin/audit        ALLOW  DENY       DENY      DENY
/admin/banners      ALLOW  DENY       DENY      DENY
```

### Cách test

Với mỗi ô:

```
1. Login với role tương ứng
2. Truy cập URL
3. Ghi nhận HTTP status:
   - 200 → nếu matrix yêu cầu ALLOW → OK
   - 200 → nếu matrix yêu cầu DENY → BUG
   - 403 / redirect login → nếu DENY → OK
   - 403 / redirect login → nếu ALLOW → BUG
```

### DONE

Bảng 48 ô phải đúng 100%. Nếu sai → fix SecurityConfig ngay.

---

## TASK 7.6 — CASCADE LOGIC REGRESSION TEST

### 🎯 Mục tiêu

Verify các cascade rules vẫn hoạt động.

```
Shop SUSPEND
 ↓
Product của shop đó có bị HIDDEN không?

User LOCKED
 ↓
User có còn login được không?
 ↓
Nếu có → BUG

Violation BAN (CRITICAL)
 ↓
User.status = BANNED
 ↓
User có bị force logout không?
 ↓
Nếu không → BUG

KYC APPROVED
 ↓
User.role = VENDOR (nếu cascade rule yêu cầu)

Review REJECTED
 ↓
Product rating có được tính lại không?

Shop REJECTED
 ↓
Vendor có thể tạo Shop mới không?
```

### DONE

Tất cả cascade rules phải hoạt động đúng.

---

## TASK 7.7 — ROLE LOGIC SPOT CHECK

### 🎯 Mục tiêu

Spot check các rule nghiệp vụ quan trọng không bị phá.

```
[ ] Admin không tự khóa chính mình (Phase 3)
[ ] Moderator không edit SHOP Voucher (Phase 5)
[ ] Admin không edit SHOP Voucher (Phase 5)
[ ] Customer không truy cập /admin/** (Phase 2)
[ ] Vendor không truy cập /admin/users (Phase 3)
[ ] Vendor Product CRUD chỉ trong Shop của mình (Phase 4)
[ ] Moderator chỉ Escalate từ PENDING (Phase 6)
[ ] Review chỉ HIDDEN chứ không DELETE (Phase 6)
```

### DONE

Tất cả spot check pass.

---

## TASK 7.8 — AUDIT LOG IMMUTABILITY CHECK

### 🎯 Mục tiêu

Verify AuditLog không thể bị sửa hoặc xóa qua API.

```
Test:
- Có endpoint DELETE /admin/audit/{id}? → KHÔNG
- Có endpoint PUT /admin/audit/{id}? → KHÔNG
- Có endpoint POST /admin/audit/{id}/edit? → KHÔNG
- Có endpoint POST /admin/audit/{id}/delete? → KHÔNG
```

### DONE

AuditLog immutable từ API.

---

## TASK 7.9 — AUDIT LOG SECURITY

### 🎯 Mục tiêu

Chỉ ADMIN được xem Audit Log.

```
ADMIN       → /admin/audit         → ALLOW
MODERATOR   → /admin/audit         → DENY
CUSTOMER    → /admin/audit         → DENY
VENDOR      → /admin/audit         → DENY
ANONYMOUS   → /admin/audit         → LOGIN / DENIED
```

### DONE

Không có role nào khác bypass Audit Log.

---

## TASK 7.10 — PHASE 7 FINAL REVIEW

### Báo cáo

```
[AUDIT LOG]
Coverage → % (X/Y actions ghi log)
Viewer List → PASS/FAIL
Viewer Detail → PASS/FAIL
Filter → PASS/FAIL
Search → PASS/FAIL
Pagination → PASS/FAIL

[ROLE MATRIX]
48 ô test → PASS/FAIL (số ô đúng / 48)

[CASCADE LOGIC]
Shop SUSPEND → Product HIDDEN → PASS/FAIL
User LOCKED → cannot login → PASS/FAIL
Violation BAN → force logout → PASS/FAIL
KYC APPROVED → User.role = VENDOR → PASS/FAIL

[ROLE LOGIC SPOT CHECK]
Admin self-lock → DENY → PASS/FAIL
Moderator edit SHOP Voucher → DENY → PASS/FAIL
Customer /admin/** → DENY → PASS/FAIL
...

[AUDIT LOG IMMUTABILITY]
No DELETE endpoint → PASS/FAIL
No PUT endpoint → PASS/FAIL

[SECURITY]
Admin → PASS/FAIL
Moderator → PASS/FAIL
Customer → PASS/FAIL
Vendor → PASS/FAIL
Anonymous → PASS/FAIL

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
[ROLE MATRIX RESULT]
[CASCADE RESULT]
[TEST RESULT]
```

---

## 🛑 STOP RULE

Sau khi hoàn thành Phase 7:

**DỪNG.**

Không tự triển khai:

```
E2E Test (Phase 8)
Final Report (Phase 8)
Performance Test (Phase 8)
```

Chỉ khi user xác nhận:

```
PHASE 7 DONE
```

mới chuyển sang:

```
PHASE 8 — E2E TESTING + FINAL REPORT
```

---

## 📋 TỔNG KẾT PHASE 7

```
PHASE 7 — AUDIT LOG VIEWER + ROLE LOGIC REGRESSION
│
├── TASK 7.1
│   └── Audit Audit Log Implementation
│
├── TASK 7.2
│   └── Chốt Audit Log Contract
│
├── TASK 7.3
│   └── Audit Log Coverage Check
│
├── TASK 7.4
│   └── Admin Audit Log Viewer
│
├── TASK 7.5
│   └── Role Matrix Regression Test
│
├── TASK 7.6
│   └── Cascade Logic Regression Test
│
├── TASK 7.7
│   └── Role Logic Spot Check
│
├── TASK 7.8
│   └── Audit Log Immutability Check
│
├── TASK 7.9
│   └── Audit Log Security
│
└── TASK 7.10
    └── Phase 7 Final Review
```

---

## THỨ TỰ THỰC THI

```
Audit AuditLog Source
      ↓
Chốt AuditLog Contract
      ↓
Coverage Check
      ↓
Build Viewer (nếu thiếu)
      ↓
Role Matrix Test (4 × 12 = 48 ô)
      ↓
Cascade Logic Test
      ↓
Role Logic Spot Check
      ↓
Immutability Check
      ↓
Security Check
      ↓
Final Review
      ↓
PHASE 7 DONE
```

---

## NGUYÊN TẮC PHASE 7

```
READ SOURCE FIRST
        ↓
VERIFY COVERAGE
        ↓
TEST ROLE MATRIX
        ↓
TEST CASCADE LOGIC
        ↓
TEST IMMUTABILITY
        ↓
NO NEW BUSINESS LOGIC
        ↓
NO REFACTOR
        ↓
VERIFY ONLY
        ↓
STOP
```

---

## Điểm quan trọng nhất

Phase 7

```
≠
Refactor Security
```

mà là:

```
Existing 6 phase implementation
      ↓
Verify Role Matrix
      ↓
Verify Cascade Logic
      ↓
Verify Audit Coverage
      ↓
Spot check
      ↓
PASS/FAIL
```
