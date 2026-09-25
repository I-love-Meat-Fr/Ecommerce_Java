# PHASE 0 – AUDIT TOÀN BỘ ADMIN / MODERATOR

## 1. MỤC TIÊU

Audit toàn bộ project hiện tại trước khi bắt đầu các Phase phát triển Admin/Moderator.

Mục tiêu:

```text
Đọc code hiện tại
        ↓
Audit Backend + UI + Security
        ↓
Kiểm tra nghiệp vụ
        ↓
Phân loại trạng thái
        ↓
Xác định Owner + Dependency
        ↓
Đề xuất Phase tiếp theo
```

**Phase này CHỈ AUDIT, KHÔNG CODE.**

---

# 2. NGUYÊN TẮC BẮT BUỘC

Được phép:

```text
Đọc code
Tìm kiếm file
Đọc Git
Chạy build
Chạy test
Chạy application nếu cần
Kiểm tra route/API/database
```

Không được:

```text
Sửa code
Tạo file
Xóa file
Refactor
Migration
Thay đổi nghiệp vụ
Thay đổi SecurityConfig
Commit
Push
Reset
Stash
Merge
```

Nếu có thay đổi chưa commit:

> Tuyệt đối không xóa, reset hoặc stash thay đổi của người dùng.

---

# 3. KIỂM TRA GIT BAN ĐẦU

Chạy:

```bash
git status
git branch
git log --oneline --decorate -10
```

Ghi nhận:

```text
Current branch:
Current HEAD:
Working tree:
Uncommitted changes:
```

Không được làm mất các thay đổi hiện tại.

---

# 4. AUDIT ROLE

Kiểm tra:

```text
ADMIN
VENDOR
CUSTOMER
MODERATOR
```

Kiểm tra:

```text
UserRole
User
SecurityConfig
Authentication
Authorization
@PreAuthorize
requestMatchers
```

Xác định role nào:

```text
DONE
PARTIAL
MISSING
WRONG
```

Nếu thiếu MODERATOR:

> Ghi nhận `MISSING`, không tự thêm.

---

# 5. AUDIT ADMIN

Kiểm tra toàn bộ nghiệp vụ Admin hiện tại:

```text
Admin Login
Admin Dashboard

User Management
Lock / Unlock User

Vendor Management

Shop Management
Shop Approval / Reject
Activate / Deactivate Shop

Category Management

Product Management

Order Management / View Order

Review Management

WEB Voucher

Banner

PR / Marketing

Violation Management

Vendor Suspension / Ban

Product Hide / Reject

Escalation

KYC Monitoring

AuditLog
```

Với mỗi chức năng kiểm tra:

```text
Backend
UI
Route
Security
Database dependency
Integration
```

---

# 6. AUDIT MODERATOR

Kiểm tra:

```text
Moderator Role
Moderator Login
Moderator Dashboard
Moderation Queue
Product Moderation
Review Moderation
Report Case
Case Detail
Approve
Reject
Escalate
Violation
Complaint Escalation
Moderation History
```

Kiểm tra:

```text
Controller
Service
Repository
DTO
Entity
Template
CSS / JS
Route
Security
```

---

# 7. AUDIT 3-LAYER MODERATION

Kiểm tra flow:

```text
Vendor tạo Product
        ↓
Auto Moderation
        ↓
AUTO_PASSED / AUTO_REJECTED / PENDING_MANUAL
        ↓
Moderator
        ↓
APPROVED / REJECTED
        ↓
Admin nếu Severe
```

Phân biệt rõ:

```text
Auto Moderation
≠
Moderator
≠
Admin
```

Không tự thêm hoặc sửa backend moderation.

Nếu backend thuộc Anh Tuấn:

```text
DEPENDENCY – Anh Tuấn
```

Hoàn chỉ audit phần UI/operation thuộc mình.

---

# 8. AUDIT CÁC MODULE DEPENDENCY

Kiểm tra dependency với:

```text
User
Shop
KYC
Product
Category
Order
Review
Voucher
Banner
ReportCase
Violation
Complaint
AuditLog
Security
```

Đặc biệt phân biệt:

```text
KYC Approval
≠
Shop Operational Approval
```

```text
Product Moderation
≠
Admin Product Management
```

```text
WEB Voucher
≠
SHOP Voucher
```

```text
Violation Severity
≠
Violation Action
```

```text
GMV
≠
Platform Revenue
≠
Vendor Sales
```

Không tự sửa nếu phát hiện đang bị gộp sai.

---

# 9. AUDIT SECURITY

Kiểm tra:

```text
/admin/**
/moderator/**
/vendor/**
/cart/**
/checkout/**
/my-orders/**
/api/kyc/**
```

Tìm:

```text
Role bypass
Admin route không bảo vệ
Moderator route không bảo vệ
Vendor truy cập Admin
Customer truy cập Admin
Missing @PreAuthorize
anyRequest().permitAll()
Locked account vẫn login
```

Nếu Security backend thuộc Quốc Anh:

```text
DEPENDENCY – Quốc Anh
```

Không sửa SecurityConfig trong Phase 0.

---

# 10. AUDIT BACKEND ↔ UI

Với từng chức năng kiểm tra:

```text
UI
 ↓
Controller
 ↓
Service
 ↓
Repository
 ↓
Database
```

Phân loại:

```text
[GREEN] DONE
[YELLOW] PARTIAL
[RED] MISSING
[ORANGE] WRONG
[BLUE] DEPENDENCY
[GRAY] OUT OF SCOPE
```

Không được ghi `DONE` nếu chưa kiểm tra thực tế.

---

# 11. OWNERSHIP

### Hoàn

```text
Admin UI
Admin Dashboard
Admin User
Admin Vendor
Admin Shop
Admin Category
Admin Product
Admin Order UI
Admin Review
WEB Voucher
Banner / PR
Moderator UI
Violation UI
Escalation UI
Complaint Escalation UI
KYC Monitoring UI
AuditLog UI
Admin/Moderator testing
```

### Anh Tuấn

```text
KYC Backend
Auto Moderation
ReportCase Backend
Violation Backend
Complaint Backend
Review Validation
Legal Backend
AuditLog Backend
```

### Quốc Anh

```text
Order Core
Checkout Backend
Payment
Commission
Settlement
Payout
Refund
Shipping Core
Cart Backend
Stock Backend
Security Core
```

### Mạnh Quân

```text
Vendor UI
Shop UI
Product Seller UI
Inventory UI
KYC UI
```

### Anh Quân

```text
Customer UI
Cart UI
Checkout UI
Order UI
Review UI
Complaint UI
```

Không được overwrite code thuộc người khác.

---

# 12. AUDIT MASTER TABLE

Tạo bảng:

| #  | Nghiệp vụ      | Backend | UI | Security | Status | Owner | Dependency | Priority | Phase |
| -- | -------------- | ------- | -- | -------- | ------ | ----- | ---------- | -------- | ----- |
| 1  | Admin Login    | ?       | ?  | ?        | ?      | Hoàn  | ?          | ?        | ?     |
| 2  | Dashboard      | ?       | ?  | ?        | ?      | Hoàn  | ?          | ?        | ?     |
| 3  | User           | ?       | ?  | ?        | ?      | Hoàn  | ?          | ?        | ?     |
| 4  | Shop           | ?       | ?  | ?        | ?      | Hoàn  | KYC        | ?        | ?     |
| 5  | Category       | ?       | ?  | ?        | ?      | Hoàn  | ?          | ?        | ?     |
| 6  | Product        | ?       | ?  | ?        | ?      | Hoàn  | Tuấn       | ?        | ?     |
| 7  | Order          | ?       | ?  | ?        | ?      | Hoàn  | Quốc Anh   | ?        | ?     |
| 8  | Review         | ?       | ?  | ?        | ?      | Hoàn  | Tuấn       | ?        | ?     |
| 9  | WEB Voucher    | ?       | ?  | ?        | ?      | Hoàn  | ?          | ?        | ?     |
| 10 | Banner / PR    | ?       | ?  | ?        | ?      | Hoàn  | ?          | ?        | ?     |
| 11 | Moderator      | ?       | ?  | ?        | ?      | Hoàn  | Tuấn       | ?        | ?     |
| 12 | Violation      | ?       | ?  | ?        | ?      | Hoàn  | Tuấn       | ?        | ?     |
| 13 | Escalation     | ?       | ?  | ?        | ?      | Hoàn  | Tuấn       | ?        | ?     |
| 14 | Complaint      | ?       | ?  | ?        | ?      | Hoàn  | Tuấn       | ?        | ?     |
| 15 | KYC Monitoring | ?       | ?  | ?        | ?      | Hoàn  | Tuấn       | ?        | ?     |
| 16 | AuditLog       | ?       | ?  | ?        | ?      | Hoàn  | Tuấn       | ?        | ?     |

````

Không được tự điền kết quả khi chưa kiểm tra.

---

# 13. BUILD / RUNTIME BASELINE

Nếu có thể, chạy:

```bash
mvn clean package
````

Ghi:

```text
BUILD: PASS / FAIL
```

Nếu FAIL:

```text
Error:
Root cause:
Affected module:
Owner:
```

Nếu chạy được application, kiểm tra tối thiểu:

```text
Admin Login
Admin Dashboard
Moderator Route
```

Không sửa lỗi trong Phase 0.

---

# 14. PHÂN LOẠI KẾT QUẢ

Báo cáo riêng:

```text
DONE:
...

PARTIAL:
...

MISSING:
...

WRONG:
...

DEPENDENCY:
...

OUT OF SCOPE:
...
```

Mỗi lỗi phải ghi:

```text
File:
Function:
Current behavior:
Expected behavior:
Owner:
Dependency:
Priority:
```

---

# 15. ĐỀ XUẤT PHASE SAU AUDIT

Sau khi audit xong mới đề xuất:

```text
PHASE 1
Admin Core
(User / Vendor / Shop / Lock)

PHASE 2
Category / WEB Voucher / Banner / PR

PHASE 3
Order / Product / Review

PHASE 4
Moderator

PHASE 5
Violation / Escalation / Complaint

PHASE 6
Dashboard / Finance / KYC Monitoring / AuditLog

PHASE 7
Integration Test
```

Nếu dependency thực tế khác:

> Phải điều chỉnh thứ tự theo kết quả audit.

Không được bắt đầu code Phase tiếp theo trong Phase 0.

---

# 16. BÁO CÁO CUỐI PHASE 0

Báo cáo đúng format:

```text
## 1. Git Baseline

Current branch:
HEAD:
Working tree:
Uncommitted changes:

## 2. Existing

...

## 3. Partial

...

## 4. Missing

...

## 5. Wrong

...

## 6. Dependency

...

## 7. Out of Scope

...

## 8. Ownership

...

## 9. Priority

P0:
P1:
P2:
P3:

## 10. Phase đề xuất

Phase 1:
Phase 2:
Phase 3:
...

## 11. Build

PASS / FAIL

## 12. File thay đổi

Phải là:

NONE
```

---

# 17. ĐIỀU KIỆN KẾT THÚC

Phase 0 chỉ hoàn thành khi:

```text
1. Audit Git
2. Audit Role
3. Audit Admin
4. Audit Moderator
5. Audit Backend/UI
6. Audit Security
7. Audit Database dependency
8. Xác định Missing
9. Xác định Partial
10. Xác định Wrong
11. Xác định Dependency
12. Xác định Ownership
13. Xác định Priority
14. Đề xuất Phase tiếp theo
15. Build baseline nếu có thể
16. Không sửa source code
17. Không commit
18. Không push
```

### MỤC TIÊU CUỐI CÙNG

Sau Phase 0 phải biết chính xác:

```text
CÁI GÌ ĐÃ CÓ
CÁI GÌ THIẾU
CÁI GÌ SAI
CÁI GÌ ĐANG LÀM DỞ
AI PHỤ TRÁCH
PHỤ THUỘC AI
CẦN LÀM GÌ TIẾP THEO
```

**Phase 0 = AUDIT ONLY. Không code.**
