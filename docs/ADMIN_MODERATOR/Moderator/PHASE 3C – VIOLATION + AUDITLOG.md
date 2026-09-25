# PHASE 3C – ADMIN VIOLATION + AUDITLOG QUERY & READ-ONLY
> **Implementation instruction:** Read this Phase 3C file as the single source of truth.
> First inspect only the files/classes directly relevant to this phase.
> Do not scan the entire repository unless required to resolve a dependency.
> Reuse existing architecture and do not re-explain unrelated code.
## 1. MỤC TIÊU

Implement hoàn chỉnh phần:

```text
VIOLATION MANAGEMENT
        +
AUDITLOG QUERY / READ-ONLY
        +
ADMIN SECURITY
        +
DATABASE VERIFICATION
        +
INTEGRATION TEST
        +
REGRESSION TEST
```

Phase 3C là phase tiếp theo sau:

```text
GATE 0
    ↓
PHASE 1
Moderator Access / Layout / Dashboard
    ↓
PHASE 2A
Product Moderation
    ↓
PHASE 2B
Review Moderation
    ↓
PHASE 2C
ReportCase / Escalation Writer / Moderator History
    ↓
PHASE 3A
Admin Escalation + Enforcement + Audit Writer
    ↓
PHASE 3B
KYC Monitoring
    ↓
PHASE 3C
Violation + AuditLog Query
```

Flow Violation:

```text
Detection
    ↓
SellerViolation / Violation Backend
    ↓
Moderator / Backend
    ↓
Violation
    ↓
Severity
    ↓
Admin Monitoring / Review
    ↓
Action / Status
    ↓
AuditLog
```

Flow AuditLog:

```text
Moderator / Admin / Backend Action
              ↓
        AuditLog Writer
              ↓
          AuditLog DB
              ↓
       Admin AuditLog
              ↓
      Filter / Pagination
              ↓
          Detail
              ↓
        READ ONLY
```

### Mục tiêu quan trọng

Phase 3C phải biến Admin thành nơi:

```text
Theo dõi Violation
        +
Tra cứu AuditLog
        +
Xem Evidence / Context
        +
Trace hành động
```

Không biến Admin thành:

```text
Auto Moderation Engine
Moderator thông thường
AuditLog Editor
AuditLog Deleter
KYC Provider
Finance Operator
```

---

# 2. PHẠM VI PHASE 3C

Phase này CHỈ gồm:

### VIOLATION

1. Admin Violation List
2. Violation Detail
3. Violation Filter
4. Violation Search nếu backend hỗ trợ
5. Violation Pagination
6. Violation Severity
7. Violation Action
8. Violation Status
9. Violation Evidence
10. Vendor / Shop / Resource context
11. Escalation reference nếu backend có
12. Moderator decision reference nếu backend có
13. AuditLog reference nếu backend có
14. Admin review / action integration theo backend contract
15. Database verification

### AUDITLOG

16. AuditLog List
17. AuditLog Detail
18. AuditLog Filter
19. AuditLog Search nếu backend hỗ trợ
20. AuditLog Pagination
21. Actor filter
22. Role filter
23. Action filter
24. Resource Type filter
25. Resource ID filter
26. Severity filter
27. Date From / Date To
28. Before / After display
29. Reason display
30. IP display nếu backend hỗ trợ
31. Read-only
32. PII-safe display
33. Database verification

### SECURITY / QUALITY

34. Admin Security
35. Moderator Deny
36. Vendor Deny
37. Customer Deny
38. Anonymous Deny
39. Locked Account Regression
40. Error Handling
41. Concurrency / stale data handling
42. Integration Test
43. Regression Test
44. Build
45. Final Report

---

# 3. KHÔNG THUỘC PHASE 3C

TUYỆT ĐỐI KHÔNG triển khai lại các phần sau.

## 3A – KHÔNG LÀM LẠI

Không làm:

```text
Admin Escalation Queue
Admin Escalation Detail
Suspend Product
Suspend Shop
Ban Vendor
Enforcement Modal
Enforcement Backend
Audit Writer
AuditLog creation logic
```

Phase 3A đã chịu trách nhiệm:

```text
Escalation
    ↓
Enforcement
    ↓
Audit Writer
```

Phase 3C chỉ:

```text
AuditLog DB
    ↓
Admin Query
    ↓
List / Filter / Detail
    ↓
READ ONLY
```

---

# 4. KYC – OUT OF SCOPE

Phase 3C KHÔNG triển khai:

```text
/admin/kyc
KYC List
KYC Detail
KYC Provider
KYC Verification
KYC Status Management
KYC Approval
KYC Rejection
KYC Provider Integration
```

KYC thuộc:

```text
PHASE 3B – KYC MONITORING
```

Nếu trong Violation hoặc AuditLog có reference tới KYC:

```text
Chỉ hiển thị reference nếu backend đã cung cấp.
```

Không xây KYC module mới.

---

# 5. ESCALATION / ENFORCEMENT – OUT OF SCOPE

Phase 3C không triển khai lại:

```text
/admin/escalations
Suspend Product
Suspend Shop
Ban Vendor
```

Nếu Violation Detail có:

```text
Escalation ID
Escalation Status
Escalated At
```

thì chỉ:

```text
DISPLAY / LINK / REFERENCE
```

nếu backend đã hỗ trợ.

Không duplicate Escalation backend.

Không duplicate Enforcement backend.

---

# 6. AUDIT WRITER – OUT OF SCOPE

Phase 3C KHÔNG tạo AuditLog khi người dùng mở trang.

Không tự ghi:

```text
VIEW_AUDIT
VIEW_VIOLATION
FILTER_AUDIT
SEARCH_AUDIT
```

trừ khi backend/business contract đã định nghĩa rõ.

AuditLog của Phase 3C là:

```text
READ ONLY
```

AuditLog Writer thuộc Phase 3A / backend ownership.

Nếu AuditLogService đã có:

```text
REUSE
```

Nếu AuditLogService chưa có:

```text
DEPENDENCY – Anh Tuấn
```

Không tự tạo một AuditLog backend thứ hai.

---

# 7. NGUYÊN TẮC BẮT BUỘC

Được phép:

```text
Đọc source code
Search file
Đọc Git
Chạy build
Chạy test
Chạy application
Tạo / sửa Admin UI
Tạo / sửa Admin Controller nếu cần
Tạo / sửa Admin Service nếu cần
Tạo / sửa DTO / ViewModel
Tạo / sửa Template
Tạo / sửa CSS / JS liên quan
Kết nối backend API
Test Violation
Test AuditLog
Test Security
Test Filter
Test Pagination
Test Error Handling
```

Không được:

```text
Reset
Stash
Merge
Push
Xóa thay đổi của người khác
Overwrite code đang được người khác chỉnh sửa
Refactor lớn ngoài phạm vi
```

Không được:

```text
Viết lại Auto Moderation Engine
Viết lại ReportCase Backend
Viết lại Escalation Backend
Viết lại Enforcement Backend
Viết lại KYC Provider
Viết lại Payment
Viết lại Finance
Viết lại Settlement
Viết lại Payout
Viết lại Refund
Viết lại Order Core
Viết lại Customer UI
Viết lại Vendor UI
```

Nếu backend thuộc Anh Tuấn:

```text
DEPENDENCY – Anh Tuấn
```

Nếu Security Core thuộc Quốc Anh:

```text
DEPENDENCY – Quốc Anh
```

Không tự viết implementation trùng ownership.

---

# 8. KIỂM TRA GIT BAN ĐẦU

Chạy:

```bash
git status
git branch
git log --oneline --decorate -10
```

Ghi:

```text
Current branch:
Current HEAD:
Working tree:
Uncommitted changes:
```

Nếu có thay đổi chưa commit:

```text
KHÔNG RESET
KHÔNG STASH
KHÔNG CHECKOUT
KHÔNG OVERWRITE
```

Không làm mất thay đổi của thành viên khác.

---

# 9. KIỂM TRA PHASE TRƯỚC

Trước khi làm Phase 3C phải kiểm tra:

## Phase 2C

```text
ReportCase
Escalation
Moderator History
Escalation Writer
```

## Phase 3A

```text
Admin Enforcement
Audit Writer
AuditLog persistence
```

## Phase 3B

```text
KYC Monitoring
```

Không yêu cầu Phase 3B để Violation/AuditLog chạy nếu hai module không phụ thuộc trực tiếp.

---

# 10. BACKEND DEPENDENCY AUDIT

Kiểm tra:

```text
Violation
SellerViolation
AuditLog
AuditLogService
AuditLogRepository
ViolationService
ViolationController
AuditLogController
```

Kiểm tra:

```text
Entity
Repository
Service
Controller
DTO
Enum
Status
Database
API
Security
```

Tìm implementation hiện tại:

```text
ViolationService
ViolationController
SellerViolation
AuditLogService
AuditLogRepository
AuditLogController
```

Phân loại:

```text
[GREEN] Backend đã có
[YELLOW] Backend partial
[RED] Backend missing
[BLUE] Dependency
```

Không đánh dấu:

```text
DONE
```

chỉ vì:

```text
Entity tồn tại.
```

Phải kiểm tra:

```text
API
Service
Query
Authorization
Database
```

---

# 11. OWNERSHIP

## Hoàn

Phụ trách:

```text
Admin Violation UI
Violation List
Violation Detail
Violation Filter
Violation Pagination
Violation Evidence UI
Violation context UI
AuditLog List
AuditLog Detail
AuditLog Filter
AuditLog Pagination
AuditLog Read-only UI
Admin Security Testing
Integration Testing
Regression Testing
```

## Anh Tuấn

Phụ trách:

```text
Violation Backend
SellerViolation
Violation Service
Violation persistence
Violation API
AuditLog Backend
AuditLog Entity
AuditLog Repository
AuditLog Writer
AuditLog Query API nếu backend ownership đã phân công
```

## Quốc Anh

Phụ trách:

```text
Security Core
Authentication
Authorization
Method Security
JWT
Account Lock
```

## Mạnh Quân

```text
Vendor UI
Shop UI
Vendor KYC
```

## Anh Quân

```text
Customer Review
Customer Report
Customer Complaint
```

Không overwrite code của owner khác.

---

# 12. ADMIN / MODERATOR BOUNDARY

Bắt buộc giữ:

```text
MODERATOR
    ↓
Normal Moderation
```

và:

```text
ADMIN
    ↓
Violation Monitoring
AuditLog Query
Severe Enforcement ở Phase 3A
```

Phase 3C không đưa toàn bộ moderation về Admin.

Admin Violation page:

```text
Theo dõi
Tra cứu
Xem context
```

không biến thành:

```text
Moderator Queue
```

Nếu backend cho phép Moderator xử lý violation ở lifecycle bình thường:

```text
Giữ nguyên backend contract.
```

Không đổi ownership chỉ để phù hợp UI.

---

# 13. VIOLATION MODEL

Bắt buộc phân biệt:

```text
ViolationSeverity
```

và:

```text
ViolationAction
```

Severity:

```java
LOW
MEDIUM
HIGH
CRITICAL
```

Action:

```java
WARNING
PRODUCT_HIDDEN
PRODUCT_REJECTED
SHOP_RESTRICTED
SHOP_SUSPENDED
VENDOR_BANNED
FUNDS_FROZEN
```

Không được biểu diễn:

```text
HIGH = BAN
CRITICAL = BAN
```

trừ khi backend/policy hiện tại thực sự định nghĩa mapping đó.

Severity:

```text
MỨC ĐỘ
```

Action:

```text
HÀNH ĐỘNG
```

Status:

```text
TRẠNG THÁI XỬ LÝ
```

Ba field này độc lập.

---

# 14. VIOLATION LIST

Tạo / hoàn thiện route:

```text
/admin/violations
```

Nếu project đã có route khác:

```text
Ưu tiên route hiện tại.
```

Không tạo duplicate route nếu không cần.

List tối thiểu:

```text
Violation ID
Vendor
Shop
Policy Code
Violation Code
Severity
Resource Type
Resource ID
Description
Action
Status
Created At
Resolved At
```

Nếu backend có:

```text
Source
Moderator
Escalation
Case ID
Requires Admin Review
```

thì có thể hiển thị.

Không fake field.

---

# 15. VIOLATION TABLE

Table phải:

```text
Readable
Consistent
Paginated
Sortable nếu backend hỗ trợ
```

Status badge:

```text
LOW
MEDIUM
HIGH
CRITICAL
```

phải phân biệt rõ.

Không dùng:

```text
Severity = Action
```

---

# 16. VIOLATION FILTER

Admin phải filter được nếu backend hỗ trợ:

```text
Severity
Status
Violation Code
Policy Code
Vendor
Shop
Resource Type
Action
Created Date
```

Severity:

```text
LOW
MEDIUM
HIGH
CRITICAL
```

Status sử dụng enum backend thực tế.

Không tự tạo enum mới chỉ để UI có dropdown.

---

# 17. MULTIPLE FILTER

Nếu backend hỗ trợ query nhiều điều kiện:

Ví dụ:

```text
severity=HIGH
status=OPEN
action=SHOP_SUSPENDED
```

UI phải gửi đúng query.

Không:

```text
Filter ở client toàn bộ dataset
```

nếu backend đã hỗ trợ filter server-side.

Ưu tiên:

```text
Backend filtering
```

---

# 18. PAGINATION

Nếu backend có pagination:

```text
Phải sử dụng backend pagination.
```

Không load toàn bộ Violation rồi paginate bằng JavaScript nếu dataset có thể lớn.

Kiểm tra:

```text
Page
Size
Total
Total Pages
Current Page
```

Test:

```text
0 records
1 record
N records
Last page
Empty page
```

---

# 19. VIOLATION SEARCH

Nếu backend có search:

```text
Vendor
Shop
Violation ID
Resource ID
Policy Code
Violation Code
```

thì UI phải reuse API đó.

Nếu backend chưa có:

```text
DEPENDENCY – Anh Tuấn
```

Không tự tạo query database từ UI.

---

# 20. VIOLATION DETAIL

Route:

```text
/admin/violations/{id}
```

Detail phải hiển thị nếu backend hỗ trợ:

```text
Violation ID
Policy Code
Violation Code
Severity
Status
Action
Description
Resource Type
Resource ID
Vendor
Shop
Created At
Resolved At
```

Nếu có:

```text
Moderator Decision
Escalation
ReportCase
Auto Moderation Result
AuditLog
```

thì hiển thị reference.

Không fake.

---

# 21. VIOLATION RESOURCE

Admin phải xác định violation liên quan tới:

```text
PRODUCT
REVIEW
SHOP
VENDOR
ORDER
REPORT_CASE
OTHER
```

nhưng chỉ sử dụng enum/resource type backend thực tế.

Không tự thêm:

```text
RESOURCE_TYPE
```

nếu backend chưa hỗ trợ.

---

# 22. VENDOR / SHOP CONTEXT

Nếu backend có relation:

```text
Vendor
Shop
```

detail nên hiển thị:

```text
Vendor ID
Vendor Name
Shop ID
Shop Name
```

Nếu có link:

```text
Open Vendor
Open Shop
```

thì link phải dùng route hiện tại của project.

Không tạo Vendor module mới.

---

# 23. POLICY / VIOLATION CODE

Hiển thị:

```text
Policy Code
Violation Code
```

Ví dụ:

```text
PROHIBITED_ITEM
COUNTERFEIT_SUSPECT
IP_INFRINGEMENT
MISLEADING_PRODUCT
MISLEADING_PRICE
OFF_PLATFORM_LINK
OFF_PLATFORM_PAYMENT
FAKE_ORDER
VOUCHER_ABUSE
FAKE_REVIEW
HIGH_NFR
HIGH_LSR
KYC_FRAUD
PII_LEAKAGE
UNAUTHORIZED_CONTACT
WARRANTY_VIOLATION
SEVERE_FRAUD
ILLEGAL_ACTIVITY
```

Chỉ hiển thị những code backend thực sự có.

Không hard-code danh sách mới nếu backend đã có enum/source of truth.

---

# 24. VIOLATION EVIDENCE

Nếu backend có evidence:

Admin phải xem được:

```text
Screenshot
Product
Review
Report
Moderation Result
Evidence URL
Metadata
```

tùy backend.

Không được:

```text
Edit Evidence
Delete Evidence
Overwrite Evidence
Fake Evidence
```

Phase 3C chỉ:

```text
READ
```

---

# 25. EVIDENCE SECURITY

Nếu evidence chứa:

```text
Private URL
PII
Internal metadata
```

phải kiểm tra backend authorization.

Không expose trực tiếp private storage URL nếu backend không cho phép.

Nếu backend trả:

```text
403
```

UI phải báo:

```text
Bạn không có quyền xem evidence này.
```

Không show raw exception.

---

# 26. VIOLATION STATUS

Sử dụng status backend thực tế.

Ví dụ có thể là:

```text
OPEN
UNDER_REVIEW
RESOLVED
APPEALED
DISMISSED
```

Nhưng:

```text
KHÔNG TỰ THÊM ENUM
```

nếu backend đang dùng enum khác.

Báo cáo:

```text
Current backend status:
Expected business status:
Dependency:
```

---

# 27. VIOLATION ACTION

Hiển thị action backend thực tế:

```text
WARNING
PRODUCT_HIDDEN
PRODUCT_REJECTED
SHOP_RESTRICTED
SHOP_SUSPENDED
VENDOR_BANNED
FUNDS_FROZEN
```

Nếu backend chưa hỗ trợ:

```text
FUNDS_FROZEN
```

không được tạo UI giả.

Đặc biệt:

```text
FUNDS_FROZEN
```

không được tự triển khai vì liên quan Finance.

---

# 28. VIOLATION ACTION – READ / WRITE BOUNDARY

Phase 3C phải kiểm tra backend contract.

Nếu business contract cho phép Admin thực hiện action:

```text
Gọi backend API hiện tại.
```

Không:

```text
UI → MongoDB trực tiếp
```

Không:

```text
UI → tự đổi status
```

Nếu action thuộc Phase 3A enforcement:

```text
Không duplicate.
```

Nếu action chưa có backend:

```text
DEPENDENCY – Anh Tuấn
```

---

# 29. SEVERITY ≠ AUTOMATIC ENFORCEMENT

Bắt buộc:

```text
LOW
MEDIUM
HIGH
CRITICAL
```

không tự động đồng nghĩa:

```text
WARNING
HIDE
SUSPEND
BAN
```

Ví dụ:

```text
Severity = CRITICAL
```

không có nghĩa UI được tự động:

```text
BAN_VENDOR
```

Nếu backend policy có mapping:

```text
Hiển thị mapping hiện tại.
```

Không tự suy diễn.

---

# 30. ESCALATION REFERENCE

Nếu Violation có:

```text
Escalation ID
```

hiển thị:

```text
Escalated
Escalation ID
Escalation Status
```

Nếu có route Phase 3A:

```text
View Escalation
```

có thể link tới:

```text
/admin/escalations/{id}
```

Không duplicate Escalation Detail.

---

# 31. MODERATOR DECISION REFERENCE

Nếu backend có:

```text
Moderator Decision
Moderator ID
Moderator Action
Moderator Reason
Decision At
```

hiển thị dưới dạng:

```text
Moderation History
```

Không cho Admin sửa lịch sử Moderator tại Phase 3C.

---

# 32. AUDITLOG LIST

Tạo / hoàn thiện:

```text
/admin/audit
```

Nếu project có route khác:

```text
Ưu tiên route hiện tại.
```

List:

```text
Timestamp
Actor
Email
Role
Action
Resource Type
Resource ID
Severity
Reason
```

Nếu backend có:

```text
IP
Before
After
```

thì hiển thị phù hợp.

---

# 33. AUDITLOG ACTION

Các action có thể tồn tại:

```text
PRODUCT_APPROVED
PRODUCT_REJECTED
REVIEW_APPROVED
REVIEW_REJECTED
REVIEW_HIDDEN
REVIEW_UNHIDDEN
CASE_APPROVED
CASE_REJECTED
CASE_ESCALATED
PRODUCT_SUSPENDED
SHOP_SUSPENDED
VENDOR_BANNED
KYC_STATUS_UPDATED
VIOLATION_CREATED
VIOLATION_RESOLVED
```

Nhưng:

```text
CHỈ HIỂN THỊ ACTION BACKEND THỰC TẾ CÓ.
```

Không tạo fake AuditLog để làm UI đẹp.

Nếu Phase 3A đã tạo:

```text
PRODUCT_SUSPENDED
SHOP_SUSPENDED
VENDOR_BANNED
```

Phase 3C chỉ query các record đó.

---

# 34. AUDITLOG LIST – TABLE

Table tối thiểu:

```text
Timestamp
Actor
Role
Action
Resource
Severity
Reason
```

Có thể thêm:

```text
Resource ID
IP
```

nếu backend hỗ trợ.

Không hiển thị quá nhiều JSON trong list.

Before / After nên xem ở Detail.

---

# 35. AUDITLOG FILTER

Admin phải filter được nếu backend hỗ trợ:

```text
Actor
Role
Action
Resource Type
Resource ID
Severity
Date From
Date To
```

Ví dụ:

```text
Actor = admin01
Action = SHOP_SUSPENDED
Resource Type = SHOP
Severity = HIGH
Date From = ...
Date To = ...
```

Phải gửi đúng query backend.

---

# 36. AUDITLOG SEARCH

Nếu backend hỗ trợ search:

```text
Actor
Email
Resource ID
Action
```

reuse API.

Không tự query database từ frontend.

Nếu backend chưa hỗ trợ:

```text
DEPENDENCY – Anh Tuấn
```

---

# 37. AUDITLOG PAGINATION

Nếu backend có pagination:

```text
Phải sử dụng.
```

Không load toàn bộ AuditLog.

Test:

```text
0 records
1 record
many records
first page
middle page
last page
```

---

# 38. AUDITLOG DETAIL

Route:

```text
/admin/audit/{id}
```

Hiển thị:

```text
AuditLog ID
Actor ID
Actor Email
Role
Action
Resource Type
Resource ID
Reason
Severity
IP
Before
After
Created At
```

Chỉ hiển thị field backend thực sự cung cấp.

---

# 39. BEFORE / AFTER

Nếu backend có:

```text
before
after
```

UI phải format dễ đọc.

Ví dụ:

```text
Before
status: ACTIVE

After
status: SUSPENDED
```

Không chỉ dump:

```text
{"status":"ACTIVE",...}
```

nếu có thể format thành readable view.

---

# 40. BEFORE / AFTER – SECURITY

Không được expose:

```text
Password
Password Hash
JWT
Refresh Token
Secret
API Key
Sensitive Credential
```

Nếu before/after chứa PII:

```text
Citizen ID
Tax Code
Bank Account
Phone
Address
```

chỉ hiển thị khi backend cho phép và cần thiết.

Nếu không:

```text
MASK
```

---

# 41. AUDITLOG READ-ONLY

Admin AuditLog:

```text
READ ONLY
```

Không có:

```text
Edit
Delete
Update
Restore
```

Không cho Admin sửa:

```text
Actor
Action
Reason
Before
After
Timestamp
```

AuditLog là:

```text
Traceability
Accountability
Forensics
```

không phải master data.

---

# 42. AUDITLOG IMMUTABILITY

UI tuyệt đối không:

```text
DELETE /admin/audit/{id}
PUT /admin/audit/{id}
PATCH /admin/audit/{id}
```

Nếu backend có endpoint mutation:

```text
Không expose vào UI.
```

Nếu business yêu cầu immutable:

```text
Backend phải enforce.
```

---

# 43. AUDITLOG WRITER REGRESSION

Phase 3C không viết lại Writer nhưng phải verify Writer đã hoạt động.

Test các action đã có:

```text
Moderator Approve Product
Moderator Reject Review
Moderator Escalate
Admin Suspend Product
Admin Suspend Shop
Admin Ban Vendor
Violation Created
Violation Resolved
```

Chỉ test những action backend thực tế hỗ trợ.

Nếu action không tạo AuditLog:

```text
DEPENDENCY – Anh Tuấn
```

Không tự fake record.

---

# 44. AUDITLOG ACTOR

AuditLog phải phản ánh:

```text
Actor ID
Actor Email
Role
```

Nếu action do system/backend:

```text
SYSTEM
```

hoặc actor convention backend đang dùng.

Không tự đổi:

```text
SYSTEM
```

thành:

```text
ADMIN
```

---

# 45. AUDITLOG RESOURCE

AuditLog phải xác định:

```text
Resource Type
Resource ID
```

Ví dụ:

```text
PRODUCT / 123
SHOP / 456
VENDOR / 789
REVIEW / 111
VIOLATION / 222
REPORT_CASE / 333
```

Chỉ sử dụng resource type backend thực tế.

---

# 46. AUDITLOG REASON

Nếu AuditLog có:

```text
Reason
```

hiển thị đầy đủ nhưng an toàn.

Không cho UI tự thay đổi Reason.

Nếu Reason null:

```text
N/A
```

hoặc empty state phù hợp.

Không fake:

```text
System action
```

nếu backend không cung cấp.

---

# 47. AUDITLOG SEVERITY

Nếu AuditLog có:

```text
Severity
```

hiển thị:

```text
LOW
MEDIUM
HIGH
CRITICAL
```

Nếu AuditLog không có Severity:

```text
Không tự suy ra từ Action.
```

Ví dụ:

```text
VENDOR_BANNED
```

không tự gán:

```text
CRITICAL
```

nếu backend không lưu.

---

# 48. DATE FILTER

AuditLog phải hỗ trợ:

```text
Date From
Date To
```

nếu backend API hỗ trợ.

Phải kiểm tra:

```text
timezone
inclusive/exclusive boundary
```

Không để lỗi:

```text
Date From = 2026-01-01
Date To = 2026-01-01
```

làm mất toàn bộ record trong ngày nếu backend dùng timestamp.

Ưu tiên contract backend hiện tại.

---

# 49. FILTER RESET

Có:

```text
Apply
Reset
```

Reset phải:

```text
Clear filters
Return default page
Reload data
```

Không giữ filter cũ ngoài ý muốn.

---

# 50. EMPTY STATE

Khi không có Violation:

```text
Không có violation phù hợp.
```

Khi không có AuditLog:

```text
Không có audit log phù hợp.
```

Không:

```text
NullPointerException
500
Blank screen
```

---

# 51. ERROR HANDLING

Phải test:

```text
400
401
403
404
409
500
```

## 401

```text
Phiên đăng nhập không hợp lệ hoặc đã hết hạn.
```

## 403

```text
Bạn không có quyền truy cập chức năng này.
```

## 404

```text
Không tìm thấy violation / audit log.
```

## 409

```text
Dữ liệu đã thay đổi hoặc trạng thái không còn hợp lệ.
```

## 500

```text
Không thể tải dữ liệu. Vui lòng thử lại.
```

Không show:

```text
Stack trace
SQL exception
Mongo exception
Java exception
Internal path
```

---

# 52. SECURITY – ADMIN VIOLATION

Route:

```text
/admin/violations/**
```

chỉ:

```text
ADMIN
```

được phép.

Test:

```text
ADMIN       → ALLOW
MODERATOR   → DENY
VENDOR      → DENY
CUSTOMER    → DENY
ANONYMOUS   → DENY
```

---

# 53. SECURITY – ADMIN AUDIT

Route:

```text
/admin/audit/**
```

chỉ:

```text
ADMIN
```

được phép.

Test:

```text
ADMIN       → ALLOW
MODERATOR   → DENY
VENDOR      → DENY
CUSTOMER    → DENY
ANONYMOUS   → DENY
```

Nếu architecture hiện tại có route đặc biệt cho ADMIN + MODERATOR:

```text
Không tự thay đổi.
```

Ghi rõ trong báo cáo.

---

# 54. OBJECT-LEVEL SECURITY

Không chỉ test URL.

Test:

```text
Violation ID
AuditLog ID
Vendor ID
Shop ID
Resource ID
```

Admin phải:

```text
Access resource tồn tại → ALLOW
Access resource không tồn tại → 404
```

Không được:

```text
Bypass authorization
```

Không được dùng:

```text
?id=...
```

để truy cập resource ngoài quyền backend.

---

# 55. LOCKED ADMIN

Test:

```text
Locked Admin
    ↓
/admin/violations
/admin/audit
```

Expected:

```text
DENY
```

theo Security Core hiện tại.

Nếu Security Core thuộc Quốc Anh:

```text
DEPENDENCY – Quốc Anh
```

---

# 56. MODERATOR SECURITY REGRESSION

Test:

```text
Moderator
    ↓
/moderator/**
    ↓
ALLOW
```

nhưng:

```text
Moderator
    ↓
/admin/violations
/admin/audit
    ↓
DENY
```

Không được để Phase 3C làm mất quyền Moderator hiện có.

---

# 57. VENDOR / CUSTOMER SECURITY

Test:

```text
Vendor
    ↓
/admin/violations
/admin/audit
    ↓
DENY
```

và:

```text
Customer
    ↓
/admin/violations
/admin/audit
    ↓
DENY
```

Anonymous:

```text
DENY
```

---

# 58. NO DIRECT DATABASE FROM UI

Không được:

```text
Admin UI
    ↓
MongoDB
```

Phải:

```text
Admin UI
    ↓
Controller / API
    ↓
Service
    ↓
Repository
    ↓
MongoDB
```

Không viết:

```text
Mongo query
```

trong:

```text
Thymeleaf
JavaScript
Controller
```

nếu architecture hiện tại yêu cầu Service layer.

---

# 59. DATABASE VERIFICATION – VIOLATION

Sau khi test Violation phải kiểm tra DB.

Kiểm tra:

```text
Violation ID
Vendor
Shop
Policy Code
Violation Code
Severity
Action
Status
Resource Type
Resource ID
CreatedAt
ResolvedAt
```

Nếu backend có:

```text
Escalation ID
Moderator
Evidence
```

kiểm tra relation.

Không chỉ nhìn UI.

---

# 60. DATABASE VERIFICATION – AUDITLOG

Kiểm tra:

```text
AuditLog ID
Actor ID
Actor Email
Role
Action
Resource Type
Resource ID
Reason
Severity
Before
After
IP
CreatedAt
```

Chỉ kiểm tra field thực tế tồn tại.

Không yêu cầu field backend chưa có.

---

# 61. AUDITLOG QUERY CONSISTENCY

Nếu DB có:

```text
100 AuditLog
```

và API page size:

```text
20
```

phải kiểm tra:

```text
Total = 100
Page 1 = 20
Page 2 = 20
...
```

Không duplicate record giữa pages.

Không mất record.

---

# 62. VIOLATION QUERY CONSISTENCY

Tương tự:

```text
Total violations
Page size
Current page
Total pages
```

Kiểm tra:

```text
Filter
Pagination
Sort
```

không làm mất filter.

---

# 63. CONCURRENCY / STALE DATA

Phase 3C chủ yếu read-only.

Tuy nhiên phải test tình huống:

```text
Admin A mở Violation Detail
Admin B xử lý Violation
Admin A refresh
```

Expected:

```text
Dữ liệu mới nhất được tải lại.
```

Không hiển thị dữ liệu cache cũ nếu backend trả dữ liệu mới.

AuditLog:

```text
Admin A mở detail
Backend record mới được tạo
Admin A refresh
```

Expected:

```text
Record mới xuất hiện nếu query phù hợp.
```

---

# 64. NO DUPLICATE AUDITLOG

Phase 3C không được tạo AuditLog khi:

```text
Open Violation
Open AuditLog
Filter
Search
Pagination
Refresh
```

Không được có:

```text
VIEW_VIOLATION
VIEW_AUDIT
```

trừ khi backend business contract đã định nghĩa rõ.

---

# 65. ADMIN UI REUSE

Tiếp tục dùng Admin layout hiện tại.

Reuse:

```text
Sidebar
Header
Table
Card
Modal
Filter
Pagination
Badge
Button
Typography
Spacing
Empty State
Error State
```

Không tạo:

```text
Design System mới
Admin Layout mới
Sidebar mới
Header mới
```

Màn hình:

```text
Violation
AuditLog
```

phải nhìn cùng hệ thống Admin hiện tại.

---

# 66. UI – VIOLATION DETAIL

Ưu tiên bố cục:

```text
Violation Header
        ↓
Status / Severity / Action
        ↓
Resource
        ↓
Vendor / Shop
        ↓
Policy / Violation
        ↓
Description
        ↓
Evidence
        ↓
Moderator / Escalation Context
        ↓
Audit Reference
```

Không nhồi toàn bộ dữ liệu vào một table.

---

# 67. UI – AUDITLOG DETAIL

Ưu tiên:

```text
Audit Header
        ↓
Actor
        ↓
Action
        ↓
Resource
        ↓
Reason
        ↓
Severity
        ↓
Before
        ↓
After
        ↓
Technical Metadata
        ↓
Created At
```

Before / After phải dễ đọc.

---

# 68. AUDITLOG SORT

Nếu backend hỗ trợ:

```text
Newest First
Oldest First
```

mặc định nên dùng thứ tự backend hiện tại.

Không tự đổi contract.

Nếu backend không hỗ trợ sort:

```text
Không fake client-side sort trên toàn dataset.
```

---

# 69. VIOLATION SORT

Nếu backend hỗ trợ:

```text
Created At
Severity
Status
```

thì sử dụng API.

Không tự sort toàn dataset nếu backend có pagination.

---

# 70. PERFORMANCE

Không:

```text
Load toàn bộ Violation
Load toàn bộ AuditLog
```

nếu dataset lớn.

Phải ưu tiên:

```text
Server-side pagination
Server-side filter
Server-side search
Server-side sort
```

nếu backend hỗ trợ.

---

# 71. PII SAFETY

Violation/AuditLog có thể chứa:

```text
Email
Phone
IP
Address
Citizen ID
Tax Code
Bank Account
```

Không hiển thị PII không cần thiết.

Đặc biệt không expose:

```text
Password
Token
Secret
Credential
```

Nếu before/after có sensitive field:

```text
MASK
```

hoặc backend phải loại bỏ.

---

# 72. AUDITLOG DATA PRIVACY

Không log / render:

```text
JWT
Refresh Token
Password
Password Hash
API Secret
Payment Credential
```

Nếu phát hiện backend đang lưu:

```text
Sensitive secret
```

không tự sửa rộng ngoài scope.

Báo:

```text
SECURITY ISSUE
Owner:
Dependency:
```

---

# 73. VIOLATION → AUDITLOG RELATION

Nếu backend có relation:

```text
Violation
    ↓
AuditLog
```

Detail có thể hiển thị:

```text
Audit history
```

Ví dụ:

```text
VIOLATION_CREATED
VIOLATION_RESOLVED
PRODUCT_HIDDEN
SHOP_SUSPENDED
```

Nhưng chỉ query backend.

Không tạo relation giả từ:

```text
resourceId
```

nếu backend không đảm bảo.

---

# 74. VIOLATION → ESCALATION RELATION

Nếu có:

```text
Violation
    ↓
Escalation
```

hiển thị:

```text
Escalation ID
Status
Created At
```

Nếu click:

```text
View Escalation
```

chuyển sang module Phase 3A.

Phase 3C không sửa Escalation.

---

# 75. VIOLATION → MODERATION RELATION

Nếu có:

```text
Violation
    ↓
Moderator Decision
```

hiển thị:

```text
Moderator
Action
Reason
Created At
```

Read-only.

Không sửa moderation history.

---

# 76. TEST – VIOLATION LIST

Test:

```text
Admin Login
    ↓
/admin/violations
```

Expected:

```text
Page loads
Data loads
No exception
Correct columns
Correct status
Correct severity
```

---

# 77. TEST – VIOLATION FILTER

Test từng filter:

```text
Severity
Status
Violation Code
Policy Code
Vendor
Shop
Resource Type
Action
Date
```

Test:

```text
1 filter
2 filters
multiple filters
reset
```

Expected:

```text
Correct result
No duplicate
No stale data
Pagination reset correctly
```

---

# 78. TEST – VIOLATION DETAIL

Test:

```text
Open valid violation
Open invalid violation
Refresh detail
Back to list
```

Expected:

```text
Valid → detail
Invalid → 404 / proper message
Refresh → stable
Back → list
```

---

# 79. TEST – VIOLATION EVIDENCE

Test:

```text
Evidence exists
Evidence missing
Evidence inaccessible
Evidence URL invalid
```

Expected:

```text
Render correctly
Empty state
403 message
No raw exception
```

---

# 80. TEST – AUDITLOG LIST

Test:

```text
Admin Login
    ↓
/admin/audit
```

Expected:

```text
List loads
Timestamp correct
Actor correct
Role correct
Action correct
Resource correct
```

---

# 81. TEST – AUDITLOG FILTER

Test:

```text
Actor
Role
Action
Resource Type
Resource ID
Severity
Date From
Date To
```

Expected:

```text
Correct query
Correct results
Correct pagination
Reset works
No duplicate
```

---

# 82. TEST – AUDITLOG DETAIL

Test:

```text
Valid ID
Invalid ID
Missing ID
```

Expected:

```text
Valid → detail
Invalid → 404
Missing → proper route handling
```

---

# 83. TEST – AUDITLOG READ ONLY

Attempt:

```text
Edit
Delete
Update
```

Expected:

```text
No UI control
No mutation route exposed
No mutation performed
```

Nếu backend có mutation endpoint:

```text
Không expose trong Admin Audit UI.
```

---

# 84. TEST – AUDITLOG WRITER REGRESSION

Không implement Writer mới.

Chỉ verify existing records.

Test nếu backend hỗ trợ:

```text
Moderator Approve Product
        ↓
AuditLog

Moderator Reject Review
        ↓
AuditLog

Moderator Escalate
        ↓
AuditLog

Admin Suspend Product
        ↓
AuditLog

Admin Suspend Shop
        ↓
AuditLog

Admin Ban Vendor
        ↓
AuditLog

Violation Created
        ↓
AuditLog

Violation Resolved
        ↓
AuditLog
```

Mỗi record kiểm tra:

```text
Actor
Role
Action
Resource
Reason
Timestamp
```

Nếu backend không tạo record:

```text
DEPENDENCY – Anh Tuấn
```

Không fake record.

---

# 85. SECURITY E2E

Test:

## ADMIN

```text
/admin/violations → ALLOW
/admin/audit → ALLOW
```

## MODERATOR

```text
/admin/violations → DENY
/admin/audit → DENY
```

## VENDOR

```text
DENY
```

## CUSTOMER

```text
DENY
```

## ANONYMOUS

```text
DENY
```

## LOCKED ADMIN

```text
DENY
```

---

# 86. FILTER / PAGINATION TEST MATRIX

Test Violation:

```text
No data
1 record
Many records
Pagination
Severity filter
Status filter
Code filter
Vendor filter
Shop filter
Date filter
Multiple filters
Reset
```

Test AuditLog:

```text
No data
1 record
Many records
Pagination
Actor filter
Action filter
Resource filter
Severity filter
Date filter
Multiple filters
Reset
```

---

# 87. DATABASE TEST MATRIX

## Violation

```text
Violation ID
Vendor
Shop
Policy
Code
Severity
Action
Status
Resource
CreatedAt
ResolvedAt
```

## AuditLog

```text
AuditLog ID
Actor
Role
Action
Resource
Severity
Reason
Before
After
IP
CreatedAt
```

Không đánh dấu PASS chỉ dựa trên UI.

---

# 88. REGRESSION ADMIN

Sau Phase 3C test lại:

```text
Admin Login
Dashboard
User Management
Lock / Unlock
Role Management
Category
Shop
Product
Order
Review
WEB Voucher
Banner
Escalation
KYC
```

Phase 3C không được phá:

```text
Admin Security
Admin Layout
Existing CRUD
Existing Templates
Existing API
```

---

# 89. REGRESSION MODERATOR

Test:

```text
Moderator Login
Moderator Dashboard
Product Moderation
Review Moderation
ReportCase
Escalation
Moderator History
```

Đặc biệt:

```text
Moderator → /admin/violations = DENY
Moderator → /admin/audit = DENY
```

---

# 90. FILE SCOPE

Chỉ thay đổi file cần thiết cho:

```text
Admin Violation
Admin AuditLog
Admin Security integration
DTO/ViewModel
Templates
CSS/JS liên quan
Tests
```

Nếu sửa file ngoài scope:

```text
File:
Reason:
Impact:
Owner:
```

Không refactor toàn bộ Admin.

---

# 91. MASTER TABLE PHASE 3C

Tạo bảng:

| #  | Nghiệp vụ                    | Backend | UI | Security | DB  | Test | Status | Owner | Dependency      |
| -- | ---------------------------- | ------- | -- | -------- | --- | ---- | ------ | ----- | --------------- |
| 1  | Violation List               | ?       | ?  | ?        | ?   | ?    | ?      | Hoàn  | Tuấn            |
| 2  | Violation Detail             | ?       | ?  | ?        | ?   | ?    | ?      | Hoàn  | Tuấn            |
| 3  | Violation Filter             | ?       | ?  | ?        | ?   | ?    | ?      | Hoàn  | Tuấn            |
| 4  | Violation Search             | ?       | ?  | ?        | ?   | ?    | ?      | Hoàn  | Tuấn            |
| 5  | Violation Pagination         | ?       | ?  | ?        | ?   | ?    | ?      | Hoàn  | Tuấn            |
| 6  | Violation Severity           | ?       | ?  | ?        | ?   | ?    | ?      | Hoàn  | Tuấn            |
| 7  | Violation Action             | ?       | ?  | ?        | ?   | ?    | ?      | Hoàn  | Tuấn            |
| 8  | Violation Status             | ?       | ?  | ?        | ?   | ?    | ?      | Hoàn  | Tuấn            |
| 9  | Violation Evidence           | ?       | ?  | ?        | ?   | ?    | ?      | Hoàn  | Tuấn            |
| 10 | Vendor / Shop Context        | ?       | ?  | ?        | ?   | ?    | ?      | Hoàn  | Tuấn            |
| 11 | Escalation Reference         | ?       | ?  | ?        | ?   | ?    | ?      | Hoàn  | Tuấn            |
| 12 | Moderator Decision Reference | ?       | ?  | ?        | ?   | ?    | ?      | Hoàn  | Tuấn            |
| 13 | AuditLog List                | ?       | ?  | ?        | ?   | ?    | ?      | Hoàn  | Tuấn            |
| 14 | AuditLog Detail              | ?       | ?  | ?        | ?   | ?    | ?      | Hoàn  | Tuấn            |
| 15 | AuditLog Filter              | ?       | ?  | ?        | ?   | ?    | ?      | Hoàn  | Tuấn            |
| 16 | AuditLog Search              | ?       | ?  | ?        | ?   | ?    | ?      | Hoàn  | Tuấn            |
| 17 | AuditLog Pagination          | ?       | ?  | ?        | ?   | ?    | ?      | Hoàn  | Tuấn            |
| 18 | Actor Filter                 | ?       | ?  | ?        | ?   | ?    | ?      | Hoàn  | Tuấn            |
| 19 | Action Filter                | ?       | ?  | ?        | ?   | ?    | ?      | Hoàn  | Tuấn            |
| 20 | Resource Filter              | ?       | ?  | ?        | ?   | ?    | ?      | Hoàn  | Tuấn            |
| 21 | Severity Filter              | ?       | ?  | ?        | ?   | ?    | ?      | Hoàn  | Tuấn            |
| 22 | Date Filter                  | ?       | ?  | ?        | ?   | ?    | ?      | Hoàn  | Tuấn            |
| 23 | Before / After               | ?       | ?  | ?        | ?   | ?    | ?      | Hoàn  | Tuấn            |
| 24 | Read-only                    | ?       | ?  | ?        | ?   | ?    | ?      | Hoàn  | Tuấn            |
| 25 | PII Safety                   | ?       | ?  | ?        | ?   | ?    | ?      | Hoàn  | Tuấn            |
| 26 | Admin Security               | ?       | ?  | ?        | ?   | ?    | ?      | Hoàn  | Quốc Anh        |
| 27 | Moderator Deny               | ?       | ?  | ?        | ?   | ?    | ?      | Hoàn  | Quốc Anh        |
| 28 | Locked Account               | ?       | ?  | ?        | ?   | ?    | ?      | Hoàn  | Quốc Anh        |
| 29 | Integration Test             | N/A     | ?  | ?        | ?   | ?    | ?      | Hoàn  | Tuấn / Quốc Anh |
| 30 | Admin Regression             | N/A     | ?  | ?        | N/A | ?    | ?      | Hoàn  | Quốc Anh        |

Không tự điền:

```text
DONE
```

nếu chưa test thực tế.

---

# 92. TEST CHECKLIST BẮT BUỘC

## A. VIOLATION

```text
[ ] Admin Login

[ ] Violation List

[ ] Violation Detail

[ ] Filter Severity

[ ] Filter Status

[ ] Filter Violation Code

[ ] Filter Policy Code

[ ] Filter Vendor

[ ] Filter Shop

[ ] Filter Resource

[ ] Filter Action

[ ] Date Filter

[ ] Multiple Filters

[ ] Reset Filter

[ ] Pagination

[ ] Empty State

[ ] Evidence

[ ] Vendor Context

[ ] Shop Context

[ ] Resource Context

[ ] Moderator Decision Reference

[ ] Escalation Reference

[ ] Severity

[ ] Action

[ ] Status

[ ] Resolved At
```

## B. AUDITLOG

```text
[ ] AuditLog List

[ ] AuditLog Detail

[ ] Actor

[ ] Role

[ ] Action

[ ] Resource Type

[ ] Resource ID

[ ] Severity

[ ] Reason

[ ] IP

[ ] Before

[ ] After

[ ] Date From

[ ] Date To

[ ] Pagination

[ ] Filter

[ ] Multiple Filters

[ ] Reset Filter

[ ] Empty State

[ ] Read-only
```

## C. SECURITY

```text
[ ] ADMIN → /admin/violations = ALLOW

[ ] ADMIN → /admin/audit = ALLOW

[ ] MODERATOR → /admin/violations = DENY

[ ] MODERATOR → /admin/audit = DENY

[ ] VENDOR → /admin/violations = DENY

[ ] VENDOR → /admin/audit = DENY

[ ] CUSTOMER → /admin/violations = DENY

[ ] CUSTOMER → /admin/audit = DENY

[ ] Anonymous → /admin/violations = DENY

[ ] Anonymous → /admin/audit = DENY

[ ] Locked Admin = DENY
```

---

# 93. END-TO-END TEST 1 – VIOLATION QUERY

Flow:

```text
Violation Backend
        ↓
Violation DB
        ↓
Admin Login
        ↓
/admin/violations
        ↓
Violation List
        ↓
Filter
        ↓
Detail
```

Expected:

```text
Violation hiển thị đúng
Severity đúng
Action đúng
Status đúng
Resource đúng
Vendor đúng
Shop đúng
Evidence đúng nếu có
```

Không tạo violation giả chỉ để test UI nếu backend test data đã có.

---

# 94. END-TO-END TEST 2 – VIOLATION → AUDIT

Flow:

```text
Violation
    ↓
Action / Status change
    ↓
Backend
    ↓
AuditLog Writer
    ↓
AuditLog DB
    ↓
Admin AuditLog
```

Expected:

```text
Violation state đúng
AuditLog tồn tại nếu backend contract yêu cầu
Actor đúng
Action đúng
Resource đúng
Reason đúng
Timestamp đúng
```

---

# 95. END-TO-END TEST 3 – ADMIN AUDIT QUERY

Flow:

```text
Admin Login
    ↓
/admin/audit
    ↓
Filter Action
    ↓
Filter Resource
    ↓
Filter Date
    ↓
Open Detail
```

Expected:

```text
Correct records
Correct pagination
Correct detail
No mutation
```

---

# 96. END-TO-END TEST 4 – MODERATOR SECURITY

Flow:

```text
Moderator Login
      ↓
/moderator/**
      ↓
ALLOW
```

nhưng:

```text
/admin/violations
/admin/audit
```

Expected:

```text
DENY
```

Không được redirect vòng tránh authorization.

---

# 97. END-TO-END TEST 5 – AUDIT WRITER REGRESSION

Không triển khai Writer mới.

Chỉ kiểm tra:

```text
Existing Action
      ↓
AuditLog
      ↓
Admin Query
```

Test các action backend thực tế có:

```text
PRODUCT_APPROVED
PRODUCT_REJECTED
REVIEW_APPROVED
REVIEW_REJECTED
REVIEW_HIDDEN
REVIEW_UNHIDDEN
CASE_ESCALATED
PRODUCT_SUSPENDED
SHOP_SUSPENDED
VENDOR_BANNED
VIOLATION_CREATED
VIOLATION_RESOLVED
```

Không bắt buộc tất cả nếu backend hiện tại chưa hỗ trợ.

Báo rõ:

```text
SUPPORTED
NOT IMPLEMENTED
DEPENDENCY
```

---

# 98. BUILD / TEST

Chạy:

```bash
mvn clean package
```

Sau đó:

```bash
mvn test
```

Nếu có integration test:

```text
Chạy integration test phù hợp.
```

Ghi:

```text
BUILD: PASS / FAIL

UNIT TEST: PASS / FAIL

INTEGRATION TEST: PASS / FAIL

MANUAL TEST: PASS / FAIL
```

Nếu FAIL:

```text
Command:
Error:
Root Cause:
File:
Function:
Owner:
Dependency:
```

Không ghi:

```text
PASS
```

nếu chưa chạy.

---

# 99. RESULT CLASSIFICATION

Mỗi chức năng phải phân loại:

```text
DONE
PARTIAL
MISSING
WRONG
DEPENDENCY
OUT OF SCOPE
```

Mỗi issue:

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

# 100. FILE CHANGE REPORT

Báo cáo:

```text
Created:
Modified:
Deleted:
```

Với mỗi file:

```text
File:
Purpose:
Why changed:
Owner:
```

Nếu không xóa:

```text
Deleted:
NONE
```

Không ghi file không thực sự thay đổi.

---

# 101. SECURITY REPORT

Bắt buộc báo cáo:

```text
Admin → /admin/violations:
Admin → /admin/audit:

Moderator → /admin/violations:
Moderator → /admin/audit:

Vendor → Admin:
Customer → Admin:
Anonymous → Admin:

Locked Admin:
```

Kết quả:

```text
PASS
FAIL
DEPENDENCY
```

Nếu Security Core bị block:

```text
DEPENDENCY – Quốc Anh
```

---

# 102. DATABASE REPORT

Báo cáo:

## Violation

```text
List query:
Filter query:
Detail query:
Pagination:
DB consistency:
```

## AuditLog

```text
List query:
Filter query:
Detail query:
Pagination:
Read-only:
DB consistency:
```

Nếu không có DB access để verify:

```text
DEPENDENCY / NOT VERIFIED
```

Không ghi PASS.

---

# 103. DEPENDENCY REPORT

## Anh Tuấn

```text
Violation Backend:
SellerViolation:
Violation API:
Violation Query:
AuditLog Backend:
AuditLog Query API:
AuditLog Entity:
AuditLog Repository:
AuditLog Writer:
```

## Quốc Anh

```text
Authentication:
Authorization:
Method Security:
Locked Account:
```

## Mạnh Quân

```text
Vendor:
Shop:
KYC:
```

## Anh Quân

```text
Customer:
Review:
Report:
Complaint:
```

Không đánh dấu Phase 3C BLOCKED nếu dependency không ảnh hưởng trực tiếp phần đang triển khai.

---

# 104. KHÔNG TỰ TẠO MOCK PRODUCTION DATA

Nếu backend chưa có:

```text
Violation API
AuditLog API
```

không được tạo:

```text
Hard-coded violation
Hard-coded audit log
fake JSON
fake DB record
```

để làm cho UI trông như DONE.

Phải báo:

```text
MISSING
```

hoặc:

```text
DEPENDENCY – Anh Tuấn
```

Có thể dùng test fixture/test data riêng nếu test framework hiện tại đã có và phải ghi rõ đó là:

```text
TEST DATA
```

---

# 105. KHÔNG DUPLICATE BACKEND

Nếu đã có:

```text
ViolationController
ViolationService
AuditLogController
AuditLogService
```

phải:

```text
REUSE
```

Không tạo:

```text
AdminViolationService2
AdminAuditLogService2
```

chỉ để phục vụ UI.

Nếu cần Adapter/DTO cho Admin:

```text
chỉ tạo lớp mỏng cần thiết.
```

---

# 106. API CONTRACT

Trước khi viết UI phải xác định:

```text
GET /admin/violations
GET /admin/violations/{id}

GET /admin/audit
GET /admin/audit/{id}
```

hoặc route/API tương đương đang tồn tại.

Ghi:

```text
Current API:
HTTP Method:
Request:
Response:
Pagination:
Filter:
Authorization:
```

Không tự giả định API.

---

# 107. API RESPONSE VALIDATION

Kiểm tra response:

```text
HTTP status
JSON structure
null handling
enum
pagination metadata
error structure
```

Nếu API trả:

```text
data
content
items
page
total
```

phải dùng đúng contract.

Không tự map sai field.

---

# 108. ENUM CONTRACT

Các enum:

```text
ViolationSeverity
ViolationAction
ViolationStatus
AuditAction
ResourceType
Role
```

phải lấy từ backend hiện tại.

Nếu backend enum khác tên UI:

```text
Mapping ở ViewModel/formatter.
```

Không đổi enum backend chỉ vì UI.

---

# 109. NULL / MISSING FIELD

Nếu backend không có:

```text
ResolvedAt
IP
Before
After
Severity
Evidence
Escalation
```

UI phải xử lý:

```text
N/A
Not available
```

tùy field.

Không:

```text
NullPointerException
```

Không fake giá trị.

---

# 110. AUDITLOG DETAIL – JSON FORMAT

Nếu:

```text
Before
After
```

là JSON:

Phải format:

```text
field
value
```

hoặc:

```text
Before
status: ACTIVE
reason: ...

After
status: SUSPENDED
reason: ...
```

Nếu JSON invalid:

```text
Hiển thị raw value an toàn.
```

Không làm crash page.

---

# 111. DATE / TIME

AuditLog và Violation phải hiển thị timestamp nhất quán với Admin hiện tại.

Không tự đổi timezone backend.

Nếu backend lưu UTC:

```text
hiển thị theo convention project hiện tại.
```

Không tạo một timezone convention riêng cho Phase 3C.

---

# 112. UI ACCESSIBILITY / USABILITY

Không cần tạo design system mới nhưng phải đảm bảo:

```text
Button có label
Filter có label
Status dễ đọc
Table header rõ
Error message rõ
Empty state rõ
Detail navigation rõ
```

Không dùng màu sắc là thông tin duy nhất.

---

# 113. PHASE 3C REGRESSION – PHASE 3A

Đảm bảo Phase 3C không phá:

```text
Admin Escalation
Admin Enforcement
Suspend Product
Suspend Shop
Ban Vendor
Audit Writer
```

Test tối thiểu:

```text
Existing AuditLog records
Existing Enforcement records
Existing Escalation links
```

---

# 114. PHASE 3C REGRESSION – PHASE 3B

Không được phá:

```text
/admin/kyc
KYC Detail
KYC Filter
KYC Monitoring
```

Phase 3C không sửa KYC trừ khi có dependency bắt buộc và phải báo cáo.

---

# 115. OUT-OF-SCOPE CHECK

Agent phải tự kiểm tra trước khi kết thúc:

```text
[ ] Không làm KYC
[ ] Không làm Escalation Queue
[ ] Không làm Enforcement
[ ] Không làm Audit Writer
[ ] Không làm Finance
[ ] Không làm Payment
[ ] Không làm Settlement
[ ] Không làm Payout
[ ] Không làm Refund
[ ] Không làm Dashboard tài chính
[ ] Không rewrite Auto Moderation
[ ] Không rewrite ReportCase
[ ] Không rewrite Security Core
```

---

# 116. DONE CRITERIA – VIOLATION

Một chức năng Violation chỉ được:

```text
DONE
```

khi đã kiểm tra:

```text
1. UI
2. Route
3. Backend API
4. Authentication
5. Authorization
6. Query
7. Filter
8. Pagination
9. Validation
10. Error Handling
11. Database
12. Manual Test
```

Nếu có Evidence:

```text
13. Evidence access
```

Nếu có Audit relation:

```text
14. AuditLog reference
```

---

# 117. DONE CRITERIA – AUDITLOG

AuditLog chỉ được:

```text
DONE
```

khi đã kiểm tra:

```text
1. List
2. Detail
3. Filter
4. Pagination
5. Search nếu backend hỗ trợ
6. Actor
7. Role
8. Action
9. Resource
10. Reason
11. Severity nếu có
12. Before
13. After
14. Date
15. Read-only
16. PII safety
17. Database
18. Manual Test
19. Security
```

---

# 118. KHÔNG ĐƯỢC GHI DONE CHỈ VÌ UI HIỆN

Không ghi:

```text
DONE
```

chỉ vì:

```text
Trang mở được
Table hiện
Button hiện
Mock data hiện
```

Phải xác minh:

```text
UI
+
API
+
Security
+
Database
+
Behavior
```

---

# 119. FINAL REPORT FORMAT

Báo cáo cuối Phase 3C đúng format:

```text
# PHASE 3C RESULT

## 1. Git Baseline

Current branch:

HEAD:

Working tree:

Uncommitted changes:


## 2. Previous Phase Dependency

Phase 2C:

Escalation:

Phase 3A:

Audit Writer:

Phase 3B:

KYC:


## 3. BACKEND AUDIT

Violation Backend:

AuditLog Backend:

Violation API:

AuditLog API:

Status:


## 4. VIOLATION

List:

Detail:

Filter:

Search:

Pagination:

Severity:

Action:

Status:

Evidence:

Vendor:

Shop:

Resource:

Escalation Reference:

Moderator Decision:

Status:


## 5. AUDITLOG

List:

Detail:

Filter:

Search:

Pagination:

Actor:

Role:

Action:

Resource:

Severity:

Reason:

Before:

After:

IP:

Read-only:

PII Safety:

Status:


## 6. SECURITY

ADMIN:

MODERATOR:

VENDOR:

CUSTOMER:

ANONYMOUS:

LOCKED ADMIN:

Status:


## 7. DATABASE

Violation:

AuditLog:

Status:


## 8. TEST

Unit Test:

Integration Test:

Manual Test:

E2E Test:

Status:


## 9. REGRESSION

Admin:

Moderator:

Phase 3A:

Phase 3B:

Status:


## 10. DONE

...


## 11. PARTIAL

...


## 12. MISSING

...


## 13. WRONG

...


## 14. DEPENDENCY

Anh Tuấn:

Quốc Anh:

Mạnh Quân:

Anh Quân:


## 15. FILES CHANGED

Created:

Modified:

Deleted:


## 16. SECURITY ISSUES

...


## 17. KNOWN ISSUES

...


## 18. BLOCKERS

...


## 19. OUT OF SCOPE

...


## 20. PHASE 4 DEPENDENCY

...


## 21. FINAL STATUS

READY FOR PHASE 4

hoặc

BLOCKED
```

---

# 120. PHASE 3C FINAL CHECKLIST

## VIOLATION

```text
[ ] Violation List
[ ] Violation Detail
[ ] Filter
[ ] Search
[ ] Pagination
[ ] Severity
[ ] Action
[ ] Status
[ ] Evidence
[ ] Vendor
[ ] Shop
[ ] Resource
[ ] Moderator Decision Reference
[ ] Escalation Reference
[ ] Audit Reference
[ ] Error Handling
[ ] Database Verification
```

## AUDITLOG

```text
[ ] AuditLog List
[ ] AuditLog Detail
[ ] Actor
[ ] Role
[ ] Action
[ ] Resource Type
[ ] Resource ID
[ ] Severity
[ ] Reason
[ ] Before
[ ] After
[ ] IP
[ ] Date Filter
[ ] Pagination
[ ] Search
[ ] Read-only
[ ] PII Safety
[ ] Database Verification
```

## SECURITY

```text
[ ] Admin Allow
[ ] Moderator Deny
[ ] Vendor Deny
[ ] Customer Deny
[ ] Anonymous Deny
[ ] Locked Admin Deny
[ ] Object-level Security
```

## QUALITY

```text
[ ] Error Handling
[ ] Empty State
[ ] Loading State
[ ] Pagination
[ ] Filter Reset
[ ] Regression
[ ] Build
[ ] Unit Test
[ ] Integration Test
[ ] Manual Test
```

---

# 121. MỤC TIÊU CUỐI CÙNG

Sau Phase 3C phải đạt flow:

```text
                VIOLATION BACKEND
                       │
                       ↓
                  VIOLATION DB
                       │
                       ↓
                    ADMIN
                       │
              ┌────────┴────────┐
              ↓                 ↓
       VIOLATION LIST       AUDITLOG LIST
              │                 │
       Filter / Search     Filter / Search
              │                 │
              ↓                 ↓
       VIOLATION DETAIL    AUDITLOG DETAIL
              │                 │
              ↓                 ↓
          Evidence          Before / After
              │                 │
              ↓                 ↓
       Context / History       READ ONLY
```

Audit flow:

```text
ACTION
   ↓
AUDIT WRITER
   ↓
AUDITLOG DB
   ↓
ADMIN QUERY
   ↓
FILTER
   ↓
DETAIL
   ↓
READ ONLY
```

Violation flow:

```text
DETECTION
    ↓
SELLER VIOLATION
    ↓
VIOLATION
    ↓
SEVERITY
    ↓
STATUS / ACTION
    ↓
ADMIN MONITORING
    ↓
AUDITLOG
```

---

# 122. RANH GIỚI CUỐI CÙNG

```text
AUTO MODERATION
= Máy kiểm tra / flag
```

```text
MODERATOR
= Normal moderation
```

```text
ADMIN
= Violation monitoring
  + AuditLog query
  + Severe enforcement ở Phase 3A
```

```text
VIOLATION BACKEND
= Tạo / xử lý / persistence theo ownership
```

```text
AUDIT WRITER
= Ghi AuditLog
```

```text
PHASE 3C
= Query / Filter / Detail / Read-only
```

Đặc biệt:

```text
PHASE 3A
= Escalation + Enforcement + Audit Writer

PHASE 3B
= KYC Monitoring

PHASE 3C
= Violation + AuditLog Query
```

Không được nhập lại ba phase thành một phase khổng lồ.

---

# 123. ĐIỀU KIỆN KẾT THÚC PHASE 3C

Phase 3C chỉ được coi là hoàn thành khi:

```text
1. Violation List hoạt động

2. Violation Detail hoạt động

3. Violation Filter hoạt động

4. Violation Search hoạt động nếu backend hỗ trợ

5. Violation Pagination hoạt động

6. Severity hiển thị đúng

7. Action hiển thị đúng

8. Status hiển thị đúng

9. Severity ≠ Action

10. Evidence hiển thị đúng nếu backend hỗ trợ

11. Vendor / Shop / Resource context đúng

12. Escalation reference đúng nếu có

13. Moderator decision reference đúng nếu có

14. AuditLog List hoạt động

15. AuditLog Detail hoạt động

16. AuditLog Filter hoạt động

17. AuditLog Pagination hoạt động

18. Actor / Role hiển thị đúng

19. Action hiển thị đúng

20. Resource hiển thị đúng

21. Reason hiển thị đúng

22. Before / After hiển thị đúng nếu backend có

23. Date filter hoạt động nếu backend hỗ trợ

24. AuditLog Read-only

25. Không expose sensitive credential

26. Admin Security test pass

27. Moderator không truy cập được Admin Violation

28. Moderator không truy cập được Admin AuditLog

29. Vendor không truy cập được

30. Customer không truy cập được

31. Anonymous không truy cập được

32. Locked Admin test pass

33. Database verification pass

34. Violation E2E pass

35. AuditLog E2E pass

36. Audit Writer regression pass nếu backend testable

37. Admin Regression pass

38. Moderator Regression pass

39. Phase 3A Regression pass

40. Phase 3B Regression pass

41. Build pass

42. Test pass nếu test suite tồn tại

43. File changes được báo cáo

44. Dependency được báo cáo

45. Known issues được báo cáo

46. Out-of-scope changes được báo cáo

47. Phase 4 dependency được xác định
```

---

# 124. PHASE 3C KHÔNG ĐƯỢC KẾT THÚC BẰNG VIỆC "TRANG ĐÃ CHẠY"

Agent phải chứng minh:

```text
Backend contract
      +
UI
      +
Security
      +
Database
      +
Filter
      +
Pagination
      +
Detail
      +
Read-only
      +
Regression
```

đã được kiểm tra.

Nếu thiếu backend:

```text
DEPENDENCY
```

Nếu backend có nhưng UI thiếu:

```text
MISSING
```

Nếu UI có nhưng API sai:

```text
WRONG
```

Nếu chỉ có mock:

```text
PARTIAL
```

Nếu ngoài phạm vi:

```text
OUT OF SCOPE
```

Không được dùng:

```text
DONE
```

để che dependency hoặc mock data.

---

# 125. PHASE 3C END STATE

Trạng thái cuối cùng phải là:

```text
                    ADMIN
                      │
          ┌───────────┴───────────┐
          ↓                       ↓
      VIOLATION                 AUDITLOG
          │                       │
    ┌─────┼─────┐           ┌────┼────┐
    ↓     ↓     ↓           ↓    ↓    ↓
 Filter Detail Evidence   Filter Detail History
    │     │     │           │    │    │
    └─────┴─────┘           └────┴────┘
          │                       │
          ↓                       ↓
      READ / REVIEW           READ ONLY
          │                       │
          └───────────┬───────────┘
                      ↓
                 TRACEABILITY
```

Và toàn bộ phase boundary phải giữ:

```text
PHASE 3A
Admin Escalation
+
Enforcement
+
Audit Writer

        ↓

PHASE 3B
KYC Monitoring

        ↓

PHASE 3C
Violation
+
AuditLog Query / Read-only

        ↓

PHASE 4
Dashboard / Voucher / Banner Hardening
```

**Không triển khai Finance, Payment, Settlement, Payout, Refund hoặc Dashboard tài chính tổng thể trong Phase 3C.**
