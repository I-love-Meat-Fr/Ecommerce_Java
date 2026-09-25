# PHASE 3 — ADMIN USER MANAGEMENT

## 🎯 Mục tiêu Phase 3

Hoàn thiện và xác minh Admin User Management theo đúng Workbook, business contract và implementation thực tế của source.

Phase 3 không tạo lại toàn bộ User module.

Mục tiêu là:

```
Existing User Implementation
        ↓
Audit
        ↓
Chốt User Management Contract
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
USER MANAGEMENT hoàn chỉnh
```

Luồng Admin:

```
ADMIN / MODERATOR
  ↓
/admin/users
  ↓
USER MANAGEMENT
  ├── List
  ├── View Detail
  ├── Lock
  ├── Unlock
  ├── Edit Status
  └── View Audit Log
```

Theo source hiện tại:

```
User.role = UserRole.CUSTOMER / VENDOR / MODERATOR / ADMIN
User.status = AccountStatus.ACTIVE / LOCKED / SUSPENDED / BANNED
```

---

## 1. USER BUSINESS CONTRACT

### 1.1 — Role × Action Matrix

```
Action            ADMIN  MODERATOR  CUSTOMER  VENDOR
View user list    YES    YES        NO        NO
View user detail  YES    YES        NO        NO
Lock user         YES    YES        NO        NO
Unlock user       YES    YES        NO        NO
Edit status       YES    YES        NO        NO
Delete user       NO     NO         NO        NO (soft delete only)
```

### 1.2 — Self-Action Rule

```
Admin không được tự khóa chính mình.
Moderator không được tự khóa chính mình.
```

### 1.3 — Account Status

```
ACTIVE     → user hoạt động bình thường
LOCKED     → không login được, có thể unlock
SUSPENDED  → tạm khóa có thời hạn
BANNED     → khóa vĩnh viễn
```

---

## 2. ⚠️ ĐIỀU KIỆN BẮT ĐẦU

Phase 3 chỉ implementation sau khi:

```
PHASE 2 DONE
        +
SecurityConfig đã fix
        +
ModerationGuard đã fix
        +
Role Matrix test PASS
```

---

## 3. SOURCE-FIRST RULE

Đọc source thực tế:

```
User.java
AccountStatus.java
UserRole.java
UserRepository.java
AdminUserService.java
AdminUserServiceImpl.java
AdminUserController.java
user-list.html
user-detail.html
SecurityConfig.java
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

## 4. ⚠️ PHẠM VI PHASE 3

### CHỈ LÀM

```
Admin User Management
Audit implementation hiện tại
Chốt User management contract
List User.
View User Detail.
Lock User.
Unlock User.
Edit User Status.
Validation.
Self-action protection.
Security.
Empty State.
UI consistency.
Automated Test.
Runtime verification.
MongoDB verification.
Regression smoke test.
```

### ❌ KHÔNG ĐƯỢC LÀM

```
Vendor User Management
Customer User Self-Edit
KYC Approval (Phase 4)
Violation Enforcement (Phase 6)
Delete User (chỉ soft delete)
Refactor toàn bộ UserController
Redesign toàn bộ Admin UI
```

---

## TASK 3.1 — AUDIT USER IMPLEMENTATION

### 🎯 Mục tiêu

Xác định chính xác trạng thái User hiện tại.

### Đọc source

```
User.java
UserRepository.java
AdminUserService.java
AdminUserServiceImpl.java
AdminUserController.java
user-list.html
user-detail.html
```

### Kiểm tra

```
List
View Detail
Lock
Unlock
Edit Status
Search
Filter
Pagination
Validation
self-action protection
active / locked state
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

## TASK 3.2 — CHỐT USER MANAGEMENT CONTRACT

### 🎯 Mục tiêu

Xác định chính xác Admin/Moderator được phép làm gì với User.

```
Thành phần       Admin     Moderator
List             Có        Có
View Detail      Có        Có
Lock             Có        Có
Unlock           Có        Có
Edit Status      Có        Có
Self-Lock        DENY      DENY
Delete           DENY      DENY
```

### DONE

User management ownership và scope được xác định rõ.

---

## TASK 3.3 — ADMIN USER LIST

### 🎯 Mục tiêu

Đảm bảo /admin/users hiển thị đúng user.

```
Route: /admin/users
Flow:
  /admin/users
        ↓
  AdminUserController
        ↓
  AdminUserService
        ↓
  findAll()
        ↓
  MongoDB
```

Phải đảm bảo:

```
ACTIVE     → HIỂN THỊ
LOCKED     → HIỂN THỊ
SUSPENDED  → HIỂN THỊ
BANNED     → HIỂN THỊ
```

### DONE

Admin User List hiển thị đúng.

---

## TASK 3.4 — VIEW USER DETAIL

### 🎯 Mục tiêu

Đảm bảo /admin/users/{id} hiển thị đầy đủ thông tin.

```
Route: /admin/users/{id}
Flow:
  /admin/users/{id}
        ↓
  AdminUserController
        ↓
  AdminUserService.getUser(id)
        ↓
  MongoDB
        ↓
  Template
```

Phải hiển thị:

```
Email
Username
Role
Status
Created At
Last Login
Shop (nếu có)
KYC Status
Violation Count
```

### DONE

User detail hiển thị đủ thông tin.

---

## TASK 3.5 — LOCK USER

### 🎯 Mục tiêu

Verify và hoàn thiện Lock User.

```
Flow:
Admin
 ↓
POST /admin/users/{id}/lock
 ↓
Validate
 ↓
Check self-action
 ↓
status = LOCKED
 ↓
AuditLog
 ↓
MongoDB
```

Bắt buộc:

```
Không cho phép tự khóa chính mình.
status chuyển ACTIVE → LOCKED.
AuditLog phải ghi nhận.
```

### DONE

Lock User hoạt động đúng + có AuditLog.

---

## TASK 3.6 — UNLOCK USER

### 🎯 Mục tiêu

Verify và hoàn thiện Unlock User.

```
Flow:
Admin
 ↓
POST /admin/users/{id}/unlock
 ↓
Validate
 ↓
Check current status
 ↓
status = ACTIVE
 ↓
AuditLog
 ↓
MongoDB
```

Bắt buộc:

```
Chỉ unlock user đang LOCKED.
status chuyển LOCKED → ACTIVE.
AuditLog phải ghi nhận.
```

### DONE

Unlock User hoạt động đúng + có AuditLog.

---

## TASK 3.7 — EDIT USER STATUS

### 🎯 Mục tiêu

Cho phép đổi status giữa các trạng thái ACTIVE / LOCKED / SUSPENDED / BANNED.

```
Flow:
Admin
 ↓
GET /admin/users/{id}/edit
 ↓
Form
 ↓
POST /admin/users/{id}/edit
 ↓
Validate
 ↓
Update status
 ↓
AuditLog
 ↓
MongoDB
```

Bắt buộc:

```
Không cho đổi status của chính mình nếu status hiện tại = ACTIVE.
Ghi AuditLog với status cũ và status mới.
```

### DONE

Edit status hoạt động đúng + có AuditLog.

---

## TASK 3.8 — USER VALIDATION

### 🎯 Mục tiêu

Audit validation của Edit.

Kiểm tra:

```
email format
username
role
status transition
```

Đặc biệt:

```
Không cho ACTIVE → BANNED nếu không có violation
Không cho ACTIVE → ACTIVE (no-op)
```

### DONE

Admin không thể lưu User invalid theo business contract.

---

## TASK 3.9 — SELF-ACTION PROTECTION

### 🎯 Mục tiêu

Đảm bảo Admin/Moderator không tự khóa chính mình.

Test:

```
ADMIN lock ADMIN       → DENY
MODERATOR lock MODERATOR → DENY
ADMIN edit own status   → DENY (nếu status = ACTIVE)
```

### DONE

Self-action protection hoạt động đúng.

---

## TASK 3.10 — USER DATA ACCURACY

### 🎯 Mục tiêu

Đối chiếu Admin User với MongoDB.

Test:

```
Lock
 ↓
MongoDB
Unlock
 ↓
MongoDB
Edit
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

Admin User khớp dữ liệu MongoDB thực tế.

---

## TASK 3.11 — ADMIN USER SECURITY

### 🎯 Mục tiêu

Chỉ ADMIN / MODERATOR được truy cập và thao tác User.

```
ADMIN       → /admin/users → ALLOW
MODERATOR   → /admin/users → ALLOW
CUSTOMER    → /admin/users → DENY
VENDOR      → /admin/users → DENY
ANONYMOUS   → /admin/users → LOGIN / DENIED
```

### DONE

Không có Customer/Vendor/Anonymous bypass Admin User.

---

## TASK 3.12 — ADMIN USER AUTOMATED TEST

### 🎯 Mục tiêu

Bổ sung automated test.

Ưu tiên:

```
JUnit 5
Spring Boot Test
MockMvc
@MockBean
```

Test tối thiểu:

```
Security
[ ] Admin allowed
[ ] Moderator allowed
[ ] Customer denied
[ ] Vendor denied
[ ] Anonymous denied

List
[ ] List user
[ ] Filter by status
[ ] Filter by role

Lock
[ ] Lock ACTIVE user
[ ] Lock ADMIN self → DENY
[ ] Lock not-found user → 404

Unlock
[ ] Unlock LOCKED user
[ ] Unlock ACTIVE user → ERROR

Edit Status
[ ] Edit status ACTIVE → SUSPENDED
[ ] Edit status không hợp lệ → ERROR
```

### DONE

Admin User có automated test cho các flow quan trọng.

---

## TASK 3.13 — ADMIN USER UI CONSISTENCY

### 🎯 Mục tiêu

Đảm bảo User UI sử dụng đúng Admin Shell.

Kiểm tra:

```
Header.
Sidebar.
Navigation.
Breadcrumb.
Page title.
User table.
Action buttons.
Lock/Unlock state.
Flash message.
Error message.
Empty State.
```

### DONE

User UI nhất quán với Admin Core.

---

## TASK 3.14 — USER EMPTY STATE

### 🎯 Mục tiêu

Xác minh Empty State khi không có User.

```
MongoDB
User = 0

Admin:
/admin/users
        ↓
Empty State
```

Không:

```
Hiển thị fake data.
Crash template.
```

### DONE

User = 0 được hiển thị đúng Empty State.

---

## TASK 3.15 — RUNTIME VERIFICATION

### 🎯 Mục tiêu

Kiểm tra application thực tế.

Chạy:

```
mvn spring-boot:run
```

Flow:

```
Admin Login
    ↓
/admin/users
    ↓
List
    ↓
View Detail
    ↓
Lock
    ↓
Unlock
    ↓
Edit Status
    ↓
MongoDB
```

### DONE

Admin User đã được kiểm tra trên application thực tế.

---

## TASK 3.16 — MONGODB VERIFICATION

### 🎯 Mục tiêu

Xác minh dữ liệu User trực tiếp trên MongoDB.

Kiểm tra:

```
User.status
User.role
User.createdAt
User.lastLoginAt
```

### DONE

MongoDB phản ánh đúng kết quả của Admin User operations.

---

## TASK 3.17 — REGRESSION SMOKE TEST

### 🎯 Mục tiêu

Đảm bảo Phase 3 không phá các flow đang tồn tại.

```
ADMIN
[ ] Admin Login
[ ] Admin Dashboard
[ ] Admin Navigation
[ ] Admin User

CUSTOMER
[ ] Customer Login
[ ] Customer Home
[ ] Customer Profile (không bị ảnh hưởng)

VENDOR
[ ] Vendor Login
[ ] Vendor Dashboard
```

### DONE

Không có regression nghiêm trọng do Phase 3 gây ra.

---

## TASK 3.18 — PHASE 3 FINAL REVIEW

### Báo cáo

```
[USER CONTRACT]
List → PASS/FAIL
View Detail → PASS/FAIL
Lock → PASS/FAIL
Unlock → PASS/FAIL
Edit Status → PASS/FAIL
Self-Action Protection → PASS/FAIL

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
[TEST RESULT]
[RUNTIME RESULT]
[MONGODB RESULT]
[REGRESSION RESULT]
```

---

## 🛑 STOP RULE

Sau khi hoàn thành Phase 3:

**DỪNG.**

Không tự triển khai:

```
KYC Management (Phase 4)
Violation Management (Phase 6)
Shop Management (Phase 4)
```

Chỉ khi user xác nhận:

```
PHASE 3 DONE
```

mới chuyển sang:

```
PHASE 4 — ADMIN SHOP / KYC / CATEGORY / PRODUCT
```

---

## 📋 TỔNG KẾT PHASE 3

```
PHASE 3 — ADMIN USER MANAGEMENT
│
├── TASK 3.1
│   └── Audit User Implementation
│
├── TASK 3.2
│   └── Chốt User Management Contract
│
├── TASK 3.3
│   └── Admin User List
│
├── TASK 3.4
│   └── View User Detail
│
├── TASK 3.5
│   └── Lock User
│
├── TASK 3.6
│   └── Unlock User
│
├── TASK 3.7
│   └── Edit User Status
│
├── TASK 3.8
│   └── User Validation
│
├── TASK 3.9
│   └── Self-Action Protection
│
├── TASK 3.10
│   └── User Data Accuracy
│
├── TASK 3.11
│   └── Admin User Security
│
├── TASK 3.12
│   └── Automated Test
│
├── TASK 3.13
│   └── Admin User UI Consistency
│
├── TASK 3.14
│   └── User Empty State
│
├── TASK 3.15
│   └── Runtime Verification
│
├── TASK 3.16
│   └── MongoDB Verification
│
├── TASK 3.17
│   └── Regression Smoke Test
│
└── TASK 3.18
    └── Phase 3 Final Review
```

---

## THỨ TỰ THỰC THI

```
Audit Source
      ↓
Chốt User Contract
      ↓
List
      ↓
View Detail
      ↓
Lock
      ↓
Unlock
      ↓
Edit Status
      ↓
Validation
      ↓
Self-Action Protection
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
PHASE 3 DONE
```

---

## NGUYÊN TẮC PHASE 3

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
SELF-ACTION DENIED
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

Phase 3

```
≠
Xây lại User từ đầu
```

mà là:

```
Existing User
      ↓
Audit
      ↓
Giữ phần đúng
      ↓
Sửa phần sai
      ↓
Bổ sung phần Workbook thực sự yêu cầu
      ↓
Self-action protection
        ↓
Test
        ↓
Runtime
```
