PHASE 2C — REPORTCASE + MODERATOR HISTORY + ESCALATION

Implementation instruction: Read this Phase 2C file as the single source of truth for Phase 2C.

Core independence rule:

“Tôi implement được phần của tôi bằng contract/interface hiện tại; backend của Tuấn là integration point, không phải prerequisite để bắt đầu hoặc hoàn thành implementation.”

Phase 2C phải có khả năng được triển khai độc lập với implementation backend của developer khác.

Nếu backend bên ngoài chưa tồn tại:

Xác định contract/interface cần dùng.
Xác định integration seam.
Implement đầy đủ phần thuộc Phase 2C.
Test phần có thể test độc lập.
Đánh dấu INTEGRATION-READY.
Không fake production behavior/data.
Khi backend thật xuất hiện, kết nối tại integration seam và chuyển sang INTEGRATED.

Không được coi developer, branch, commit hoặc backend implementation của người khác là prerequisite để bắt đầu hoặc hoàn thành phần code thuộc Phase 2C.

1. MỤC TIÊU

Implement đầy đủ nghiệp vụ Moderator xử lý:

ReportCase Queue
ReportCase Detail
ReportCase Status
Report Reason
Evidence
Auto Moderation Result
Auto Flags
Violation Display
Vendor History
Previous ReportCases
Previous Violations
Previous Moderation Decisions
Case Approve
Case Reject
Case Escalate
Escalation Reason
Moderator History
Audit integration
Pagination / Filter
Object-level Security
State Transition
Error Handling
Concurrency
Backend ↔ UI Integration
Regression Test Moderator
Regression Test Admin

Flow mục tiêu:

CUSTOMER / SYSTEM
        ↓
REPORT PRODUCT / REVIEW
        ↓
AUTO MODERATION
        ↓
REPORT CASE
        ↓
MODERATOR QUEUE
        ↓
CASE DETAIL
        ↓
Review Evidence + Auto Result + Vendor History
        ↓
APPROVE / REJECT / ESCALATE
        ↓
MODERATOR HISTORY / AUDIT
        ↓
Nếu nghiêm trọng
        ↓
ADMIN ESCALATION

Mục tiêu cuối:

MODERATOR ROLE
        ↓
MODERATOR CÓ THỂ XỬ LÝ REPORT CASE
        ↓
CÓ CONTEXT + EVIDENCE + HISTORY
        ↓
RA CASE DECISION
        ↓
ESCALATE KHI VƯỢT QUYỀN
2. NGUYÊN TẮC ĐỘC LẬP — BẮT BUỘC

Đây là thay đổi quan trọng so với bản Phase 2C cũ.

2.1 Không dùng dependency theo người

Không dùng:

DEPENDENCY – Anh Tuấn
DEPENDENCY – Quốc Anh
DEPENDENCY – Mạnh Quân
DEPENDENCY – Anh Quân

như một lý do để không implement phần của mình.

Thay vào đó phải ghi:

External Integration:
Contract:
Integration Seam:
Current External Implementation:
Integration Required:
Status:

Ví dụ:

External Integration:
ReportCase backend

Contract:
ReportCaseQuery / ReportCaseCommand contract hiện tại

Integration Seam:
Moderator ReportCase Gateway/Service Adapter

Current External Implementation:
Chưa có

Integration Required:
Có

Implementation Status:
IMPLEMENTED

Integration Status:
INTEGRATION-READY
2.2 Nếu ReportCase Backend chưa tồn tại

Không được:

BLOCKED vì Tuấn chưa làm backend

Phải:

ReportCase backend:
NOT IMPLEMENTED

Phase 2C:
IMPLEMENTED

Integration:
INTEGRATION-READY

Miễn là contract/interface đã xác định được và phần thuộc Phase 2C đã implement/test được.

2.3 Không fake production implementation

Không tạo:

FakeReportCaseService
FakeHistoryService
FakeEscalationService
FakeViolationService

để chạy production.

Test double/mock chỉ được dùng trong:

unit test
integration test
component test
contract test

Production phải gọi:

existing backend API
existing service
existing repository
existing contract

hoặc integration seam đã được định nghĩa.

2.4 Không duplicate domain backend

Nếu project đã có:

ReportCase
Violation
AuditLog
Escalation
ModerationResult

thì reuse.

Không tạo:

ModeratorReportCase
ModeratorViolation
ModeratorAuditLog
ModeratorEscalation

chỉ để phục vụ UI.

Moderator layer là consumer của domain contract, không phải domain thứ hai.

3. PHẠM VI PHASE 2C
IN SCOPE
ReportCase Queue
ReportCase Detail
ReportCase Status
Report Reason
Evidence
Auto Moderation Result
Auto Flags
Violation Display
Vendor History
Previous ReportCase
Previous Violation
Previous Moderation Decision
Case Approve
Case Reject
Case Escalate
Escalation Reason
Moderator History
Pagination
Filter
Error Handling
Object-level Security
State Transition
Concurrency
Backend ↔ UI Integration
Regression Test Moderator
Regression Test Admin
Integration Contract
Integration Adapter/Seam
OUT OF SCOPE

Không triển khai lại:

Product Moderation
Product Approve / Reject
Review Moderation
Review Approve / Reject
Review Hide / Unhide
Auto Moderation Engine
KYC Provider
KYC Verification
Payment
Order Core
Checkout
Finance
Commission
Settlement
Payout
Refund
Shop Ownership
Customer UI
Vendor UI
Admin Severe Enforcement Backend
Violation Enforcement Engine

Product/Review chỉ được hiển thị context trong ReportCase nếu contract/backend cung cấp.

4. RANH GIỚI OWNERSHIP

Phase 2C chịu trách nhiệm:

ReportCase Moderator UI
        +
Moderator integration layer
        +
Action invocation
        +
Validation
        +
Security integration
        +
State-aware UI
        +
History consumption
        +
Escalation consumption
        +
Testing

Backend domain bên ngoài là integration point.

Không coi implementation backend của developer khác là prerequisite.

Ví dụ:

ReportCase backend
        ↓
Contract
        ↓
Moderator Adapter
        ↓
Moderator Controller
        ↓
UI

Nếu backend chưa có:

Contract
   ↓
Adapter/Seam
   ↓
UI
   ↓
Contract tests / test doubles

Sau này:

Real Backend
     ↓
same contract
     ↓
same adapter/seam
     ↓
same UI

Không cần viết lại Moderator UI.

5. NGUYÊN TẮC BẮT BUỘC

Được phép:

Đọc source code
Tìm kiếm file
Đọc Git
Chạy build
Chạy test
Chạy application
Tạo/sửa Moderator UI
Tạo/sửa Moderator Controller nếu cần
Tạo/sửa DTO/ViewModel cần thiết
Tạo/sửa Template
Tạo/sửa CSS/JS liên quan Moderator
Kết nối ReportCase contract/API
Tạo integration adapter nếu chưa có seam phù hợp
Kết nối History contract/API
Kết nối Escalation contract/API
Viết contract tests
Viết integration tests
Test Security
Test State Transition
Test Concurrency

Không được:

Reset code
Stash
Merge
Push
Xóa thay đổi của người khác
Overwrite code người khác
Rewrite toàn bộ Admin UI
Tạo Design System mới
Refactor lớn không liên quan
Thay đổi Order
Thay đổi Payment
Thay đổi Finance
Thay đổi KYC
Thay đổi Customer Checkout
Thay đổi Vendor Flow ngoài integration bắt buộc

Đặc biệt:

KHÔNG overwrite code của developer khác.

KHÔNG tự viết lại ReportCase Backend
nếu implementation đã tồn tại.

KHÔNG tự viết lại Violation Backend
nếu implementation đã tồn tại.

KHÔNG tự viết lại Auto Moderation Engine
nếu implementation đã tồn tại.

KHÔNG fake ReportCase / History / Escalation data.

KHÔNG dùng backend chưa tồn tại làm lý do bỏ implementation
của phần Moderator thuộc Phase 2C.
6. ĐỌC SOURCE OF TRUTH

Trước khi code bắt buộc đọc:

Source code hiện tại
Gate 0
Phase 2C
Phase 1
Phase 2A
Phase 2B
File BỘ LUẬT & CHÍNH SÁCH SÀN MARKETPLACE
Git status
Git branch
Git commit hiện tại

Policy phải được dùng làm reference trong quá trình implement.

Đối chiếu:

Report / Complaint Policy
Product Content Policy
Review Policy
Vendor Violation Policy
Complaint Escalation Policy
Prohibited / Restricted Product Policy
Counterfeit & IP Policy
Off-platform Transaction Policy
Order Fraud Policy
KYC Policy nếu chỉ hiển thị context
Audit / enforcement boundary giữa Moderator và Admin

Không tự tạo nghiệp vụ trái policy.

7. KIỂM TRA GIT BAN ĐẦU

Chạy:

git status
git branch
git log --oneline --decorate -10

Ghi nhận:

Current branch:
Current HEAD:
Working tree:
Uncommitted changes:

Nếu có thay đổi chưa commit:

Không reset.
Không stash.
Không checkout làm mất thay đổi.
Không overwrite file người khác đang sửa.

Git baseline chỉ nhằm bảo vệ working tree, không phải điều kiện để chặn implementation.

8. KIỂM TRA PHASE 1 + 2A + 2B
Phase 1

Kiểm tra:

Moderator Login
Moderator Role
Moderator Layout
Moderator Sidebar
/moderator/**
Moderator Security
Phase 2A

Kiểm tra Product Moderation contract/UI hiện tại.

Phase 2B

Kiểm tra Review Moderation contract/UI hiện tại.

Không assume 2A/2B đã hoàn thành.

Nếu 2A/2B chưa hoàn thành:

Phase 2C vẫn implement phần ReportCase của mình.

Chỉ đánh dấu integration status phù hợp.

Ví dụ:

Phase 2A:
PARTIAL

Impact:
Product context integration chưa available.

Phase 2C:
IMPLEMENTED

Integration:
INTEGRATION-READY

Không đánh dấu toàn bộ Phase 2C BLOCKED.

9. XÁC ĐỊNH BACKEND CONTRACT

Kiểm tra implementation thực tế của:

ReportCase
ReportCaseStatus
ReportCaseType
ReportCaseRepository
ReportCaseService
ReportCaseController

Violation
ViolationSeverity
ViolationAction
ViolationService
ViolationController

ModerationResult
AutoModerationResult
AuditLog
AuditLogService
Escalation
EscalationService

Kiểm tra:

Entity
Repository
Service
Controller
DTO
Enum
API
Status
Database
Validation
Authorization

Phân loại:

[GREEN] Existing implementation
[YELLOW] Partial implementation
[RED] Missing implementation
[BLUE] Integration seam available but external implementation missing

Không dùng [BLUE] để biểu thị blocker.

Ví dụ:

ReportCaseService      [RED]
ReportCase contract    [GREEN]
Moderator adapter      [GREEN]
Integration            [BLUE]

Kết luận:

Moderator implementation có thể hoàn thành.
External integration = INTEGRATION-READY.
10. CONTRACT-FIRST WORKFLOW

Trước khi implement mỗi integration:

1. Xác định domain contract
2. Xác định request/response
3. Xác định error contract
4. Xác định authorization contract
5. Xác định state transition
6. Xác định integration seam
7. Implement Moderator side
8. Test bằng contract/test double nếu backend thật chưa có
9. Mark INTEGRATION-READY
10. Khi backend thật có → integration test

Không làm:

Chờ backend
↓
mới bắt đầu UI

Phải làm:

Contract
↓
Moderator implementation
↓
Tests
↓
INTEGRATION-READY
↓
Real backend integration
11. PHÂN BIỆT REPORTCASE VỚI RESOURCE MODERATION

Bắt buộc giữ:

REPORTCASE
    ≠
PRODUCT
    ≠
REVIEW
    ≠
VIOLATION
    ≠
ESCALATION

ReportCase là case được tạo từ report.

Customer Report
       ↓
ReportCase
       ↓
Moderator xử lý Case
       ↓
Case Decision
       ↓
Resource Action / Violation / Escalation

Không được hiểu:

Approve Case
=
Approve Product

hoặc:

Reject Case
=
Reject Product

Nếu backend tách decision thì UI gọi đúng contract tương ứng.

12. REPORTCASE QUEUE

Route:

/moderator/cases

hoặc route tương đương hiện có.

Queue tối thiểu:

Case ID
Case Type
Reporter
Reported Resource
Product / Review
Vendor
Shop
Reason
Severity
Status
Created At
Assigned Moderator nếu có
Auto Moderation Status nếu có

Có thể hiển thị:

Auto Flag
Priority

nếu contract cung cấp.

Filter:

Case Type
Product / Review
Severity
Status
Vendor
Shop
Date
Auto Flag

Nếu backend hỗ trợ pagination:

BẮT BUỘC sử dụng backend pagination.

Không load toàn bộ database.

13. REPORTCASE DETAIL

Route:

/moderator/cases/{id}
Case
Case ID
Case Type
Status
Severity
Created At
Updated At
Assigned Moderator
Reporter
Reporter
Reporter ID nếu policy/backend cho phép
Report Reason
Resource
Product hoặc Review
Resource ID
Product Name
Review Content
Shop
Vendor

Chỉ hiển thị dữ liệu contract thực sự cung cấp.

Không tạo placeholder mang tính production fact.

14. EVIDENCE

Hiển thị nếu contract/backend có:

Image evidence
Text evidence
Report attachment
Screenshot
Moderation evidence
Related resource

Không:

Tự tạo evidence
Tự sửa evidence
Tự kết luận evidence là thật
Tự thay đổi evidence URL

Nếu evidence backend chưa tồn tại:

Contract:
Evidence field/schema cần thiết

Integration Seam:
ReportCaseDetailResponse.evidence

Status:
INTEGRATION-READY

Không fake evidence.

15. AUTO MODERATION RESULT

Nếu ReportCase có liên quan, Moderator xem kết quả Auto Moderation.

Hiển thị nếu contract cung cấp:

Auto Moderation Status
Flags
Rule Code
Score
Reason
Severity
Checked At

Ví dụ flag:

BLACKLIST_KEYWORD
DUPLICATE_PRODUCT
IMAGE_HASH
PRICE_ANOMALY
FORBIDDEN_CATEGORY
IMAGE_CONTENT

Không fake:

Score
Risk
Flag
Reason
Severity

Moderator chỉ xem kết quả.

Không tạo:

Run Auto Moderation

trừ khi backend contract/policy định nghĩa rõ.

Nếu Auto Moderation backend chưa tồn tại:

AutoModerationResult contract
        ↓
Moderator read-only adapter
        ↓
INTEGRATION-READY
16. VIOLATION DISPLAY

Nếu ReportCase có SellerViolation/Violation:

Hiển thị:

Violation Code
Policy Code
Severity
Evidence
Resource Type
Resource ID
Description
Action
Status
Created At
Resolved At nếu có

Phân biệt:

ViolationSeverity
LOW
MEDIUM
HIGH
CRITICAL
ViolationAction
WARNING
PRODUCT_HIDDEN
PRODUCT_REJECTED
SHOP_RESTRICTED
SHOP_SUSPENDED
VENDOR_BANNED
FUNDS_FROZEN

Không dùng:

HIGH

như một Action.

Moderator không tự thực hiện Admin enforcement nếu không có quyền.

17. VENDOR HISTORY

Case Detail hiển thị Vendor History nếu contract cung cấp:

Vendor
Shop
Previous ReportCases
Previous Violations
Previous Rejected Products
Previous Hidden Reviews
Previous Escalations
Previous Suspensions nếu backend có

Mục đích:

Moderator
    ↓
Current Case
    +
Historical Context

Không tự tính severity từ history nếu backend đã có rule/severity.

Không tự tạo historical data.

Nếu backend chưa cung cấp:

VendorHistory contract
        ↓
Moderator adapter
        ↓
INTEGRATION-READY
18. PREVIOUS CASES

Nếu backend contract hỗ trợ:

Previous Cases

Hiển thị:

Case ID
Type
Reason
Severity
Status
Decision
Created At

Có thể link tới Case Detail nếu route hỗ trợ.

Không tự tạo query/backend endpoint mới nếu không thuộc Phase.

Nếu chưa có endpoint:

Integration Status:
INTEGRATION-READY
19. PREVIOUS VIOLATIONS / MODERATION DECISIONS

Nếu backend cung cấp:

Previous Violations
Previous Moderation Decisions
Previous Escalations

thì hiển thị read-only.

Không duplicate domain logic.

Không tự suy luận:

Nhiều violation
=
Case hiện tại chắc chắn vi phạm

Decision hiện tại vẫn dựa trên evidence/policy/backend contract.

20. CASE APPROVE

Phải xác định chính xác backend contract định nghĩa:

Approve Case

Không tự suy diễn rằng Approve Case đồng nghĩa với approve resource.

Flow:

Case Queue
    ↓
Case Detail
    ↓
Approve Case
    ↓
Moderator Case API
    ↓
Case status updated
    ↓
History / Audit

Trước action:

Case tồn tại
Moderator authenticated
Moderator authorized
Case chưa final
Transition hợp lệ

Không:

Direct DB update từ UI
21. CASE REJECT

Reject Case cần reason nếu policy/contract yêu cầu.

UI:

Reject Case

Reason:
[________________________]

[Cancel] [Confirm Reject]

Không submit:

empty
spaces only

Backend phải validate lại.

Flow:

Case Detail
    ↓
Reject
    ↓
Reason
    ↓
Backend
    ↓
REJECTED
    ↓
History / Audit
22. CASE ESCALATE

Moderator có thể:

Escalate Case

Khi contract/policy xác định case cần Admin review.

Các điều kiện có thể bao gồm nếu policy/backend định nghĩa:

CRITICAL
SEVERE_FRAUD
ILLEGAL_ACTIVITY
Serious counterfeit
Serious IP infringement
Serious vendor violation
requiresAdminReview = true
Case vượt quyền Moderator

Không tự tạo thêm rule ngoài policy.

UI:

Escalation Reason

Flow:

Moderator
    ↓
Escalate
    ↓
Reason
    ↓
Escalation Contract
    ↓
Admin Queue

Moderator không tự:

Ban Vendor
Suspend Shop
Freeze Funds

nếu đây là quyền Admin.

23. ESCALATION CONTRACT

Trước khi làm UI xác định:

Endpoint
HTTP Method
Request DTO
Response DTO
Status Code
Reason Field
Case ID
Actor
Created At
Target Admin Queue
Authorization

Ví dụ tham khảo:

POST /api/moderator/cases/{id}/escalate

Nhưng:

Đây chỉ là ví dụ contract. Ưu tiên API thực tế hoặc interface hiện tại của project.

Nếu backend chưa có endpoint:

Không fake POST endpoint production.

Tạo/chuẩn hóa integration seam thuộc Moderator scope nếu cần.

Status:
INTEGRATION-READY

Khi backend thật có endpoint:

Adapter → real endpoint

không cần rewrite UI.

24. MODERATOR HISTORY

Route:

/moderator/history

History lấy từ backend contract.

Không dùng:

localStorage
session
JavaScript array
frontend fake history

Hiển thị:

Action
Resource
Case ID
Moderator
Role
Reason
Status
Created At

Các action tối thiểu nếu backend hỗ trợ:

CASE_APPROVED
CASE_REJECTED
CASE_ESCALATED

Có thể có:

PRODUCT_APPROVED
PRODUCT_REJECTED
REVIEW_APPROVED
REVIEW_REJECTED
REVIEW_HIDDEN
REVIEW_UNHIDDEN

nếu dùng chung History API.

Không duplicate History logic của Phase 2A/2B.

25. HISTORY SOURCE OF TRUTH

Nếu backend có:

AuditLog

Moderator History phải consume AuditLog/API backend.

Không tạo frontend history.

Tối thiểu:

Actor
Role
Action
Resource
Reason nếu có
CreatedAt

Nếu AuditLog backend chưa có:

Contract:
Audit event contract

Integration Seam:
History/Audit adapter

Status:
INTEGRATION-READY

Không fake AuditLog.

26. ACTION BUTTON RULE

Button phải phụ thuộc vào trạng thái backend.

Ví dụ:

CASE_PENDING
    → Approve
    → Reject
    → Escalate

Sau:

CASE_APPROVED

không hiển thị action không hợp lệ.

Tương tự:

CASE_REJECTED

Backend vẫn là source of truth.

UI chỉ hỗ trợ UX.

Backend phải kiểm tra:

Authorization
State transition
Object existence
Final state
27. MANDATORY REASON

Các action:

Reject Case
Escalate Case

phải yêu cầu reason nếu policy/contract yêu cầu.

Validation:

null
empty
spaces

đều phải bị reject nếu reason bắt buộc.

Test:

Empty
Spaces
Valid reason

Backend cũng phải validate, không chỉ UI.

28. OBJECT-LEVEL SECURITY

Không chỉ kiểm tra URL.

Phải test:

Case ID
Resource ID
Vendor ID

Moderator không được:

Xử lý Case không tồn tại
Xử lý Case đã final
Bypass state transition
Đổi ID để bypass UI
Escalate Case ngoài quyền
Truy cập resource không thuộc permission

Nếu backend có:

@PreAuthorize

hoặc service-level authorization thì phải kiểm tra thực tế.

Nếu backend security implementation chưa có:

Contract:
Moderator authorization requirement

Integration Status:
INTEGRATION-READY

Không tự mở quyền ngoài architecture.

29. SECURITY

Kiểm tra:

/moderator/**

Expected:

MODERATOR  → ALLOW
CUSTOMER   → DENY
VENDOR     → DENY
Anonymous  → DENY

Moderator:

/moderator/** → ALLOW
/admin/**     → DENY

theo architecture Phase 1.

ADMIN access tới Moderator nếu architecture hiện tại cho phép thì giữ nguyên.

Locked Moderator:

DENY

Không tự mở thêm permission.

30. BACKEND ↔ UI CONTRACT

Trước khi sửa UI xác định:

Endpoint
HTTP Method
Request DTO
Response DTO
Status Code
Error Response
Authentication
Authorization
Reason Field
Pagination
Filter
State
Allowed Actions

Ví dụ:

GET  /api/moderator/cases
GET  /api/moderator/cases/{id}
POST /api/moderator/cases/{id}/approve
POST /api/moderator/cases/{id}/reject
POST /api/moderator/cases/{id}/escalate
GET  /api/moderator/history

Đây là contract tham khảo, không phải yêu cầu tạo đúng các endpoint này.

Ưu tiên:

Existing API
Existing Service Interface
Existing DTO
Existing Enum
Existing Authorization
31. INTEGRATION SEAM

Nếu đã có service/interface tương đương:

REUSE.

Nếu chưa có seam phù hợp và việc tạo seam thuộc Phase 2C:

Create minimal integration abstraction.

Ví dụ về ý tưởng:

Moderator
   ↓
ReportCaseGateway / existing equivalent
   ↓
ReportCase Contract

Không nhất thiết phải tạo tên class này.

Không được tạo abstraction trùng với service/domain layer đã tồn tại.

Mục tiêu là:

UI không phụ thuộc trực tiếp vào implementation cụ thể

và:

Real backend có thể attach vào cùng seam.
32. TRẠNG THÁI KHI BACKEND CHƯA CÓ

Ví dụ ReportCase backend chưa implement:

ReportCase Queue UI:
IMPLEMENTED

ReportCase Detail UI:
IMPLEMENTED

Moderator Action Contract:
IMPLEMENTED

Security:
IMPLEMENTED

Tests:
PASS

External Backend:
MISSING

Integration:
INTEGRATION-READY

Không ghi:

BLOCKED – Tuấn

và cũng không ghi:

INTEGRATED
33. ERROR HANDLING

UI phải xử lý:

400 Bad Request
401 Unauthorized
403 Forbidden
404 Not Found
409 Conflict
500 Internal Server Error

Đặc biệt:

Case đã được Moderator khác xử lý.

hoặc:

Case không còn ở trạng thái có thể xử lý.

Nếu backend trả:

409 Conflict

UI phải thông báo phù hợp.

Không để:

White screen
Raw stack trace
Raw JSON
Java exception

cho end user nếu project đã có error handling.

34. PAGINATION / FILTER
Case Queue

Test:

No data
1 record
Many records
Pagination
Filter
Filter + Pagination
Reset Filter
History

Test:

No data
1 record
Many records
Pagination
Filter nếu backend hỗ trợ

Expected:

Không duplicate
Không crash
Không sai page
Không mất filter
Empty state rõ ràng
35. CONCURRENCY / DOUBLE ACTION

Test:

Moderator A mở Case
Moderator B mở Case

A → Approve
B → Reject

Expected:

Backend state transition bảo vệ dữ liệu
Không overwrite silently
Không tạo trạng thái mâu thuẫn

Nếu backend trả:

409 Conflict

UI hiển thị thông báo phù hợp.

Tương tự:

A → Escalate
B → Reject

Nếu backend chưa có concurrency implementation:

Moderator implementation:
READY

Concurrency integration:
INTEGRATION-READY

Không fake PASS.
36. CUSTOMER REPORT → REPORTCASE FLOW

Nếu backend đã hỗ trợ:

Customer
    ↓
Report Product / Review
    ↓
Auto Moderation
    ↓
ReportCase
    ↓
Moderator Queue
    ↓
Case Detail

Moderator phải nhìn được:

Reporter
Report reason
Evidence
Resource
Auto flags
Vendor
Vendor history
Previous cases
Violation nếu có

Nếu ReportCase generation backend chưa có:

ReportCase creation:
EXTERNAL INTEGRATION MISSING

Moderator ReportCase consumption:
IMPLEMENTED

Integration:
INTEGRATION-READY

Không tạo fake case.

37. REPORTCASE APPROVE TEST

Test contract:

ReportCase
    ↓
Queue
    ↓
Detail
    ↓
Approve

Expected khi real backend available:

API success
Case status cập nhật
Database cập nhật
History/Audit có record nếu contract hỗ trợ
Case không thể xử lý lại nếu final

Nếu backend chưa available:

Contract test PASS
Moderator implementation = IMPLEMENTED
Real integration = INTEGRATION-READY

Không gọi đó là full E2E PASS.

38. REPORTCASE REJECT TEST

Test:

Reject
    ↓
Empty reason

Expected:

Validation failed

Sau đó:

Reject
    ↓
Valid reason

Expected khi backend available:

Success
Case status updated
History recorded nếu backend hỗ trợ

Contract-level test có thể chạy độc lập.

39. ESCALATION TEST

Case có điều kiện escalation theo policy/backend contract.

Test:

Moderator
    ↓
Escalate
    ↓
Reason

Expected khi backend available:

Escalation success
Reason được lưu
Case status đúng
Admin nhìn thấy escalation nếu backend hỗ trợ
Moderator History có record
AuditLog có record nếu backend hỗ trợ

Moderator không tự:

Ban Vendor
Suspend Shop
Freeze Funds
40. HISTORY TEST

Test:

Case Approve
Case Reject
Case Escalate

Sau mỗi action kiểm tra contract:

Action
Actor
Role
Resource
Reason
Timestamp

Nếu backend AuditLog chưa tồn tại:

Contract test:
PASS

Production Audit Integration:
INTEGRATION-READY

Không fake history.

41. DATABASE CHECK

Khi backend thực tế tồn tại, sau:

Approve
Reject
Escalate

kiểm tra field thực tế:

Status
Decision
Reason
Moderator
Timestamp
Escalation
Violation
AuditLog

Chỉ kiểm tra field thực sự tồn tại.

Không yêu cầu thêm field vào backend ngoài Phase 2C nếu không có contract.

42. REUSE ADMIN UI

Reuse:

Sidebar
Header
Card
Table
Button
Modal
Filter
Pagination
Typography
Spacing
Status Badge

Không tạo Design System mới.

Visual language:

ADMIN UI
    +
MODERATOR UI
    ↓
CÙNG VISUAL LANGUAGE

nhưng:

Menu
Content
Permission
Operation

phải khác theo role.

43. KHÔNG REFACTOR NGOÀI PHẠM VI

Không tự sửa:

Admin Dashboard
Admin User
Admin Shop
Admin Category
Admin Voucher
Admin Banner
Order
Checkout
Payment
Finance
Vendor UI
Customer UI

trừ khi integration trực tiếp với Phase 2C bắt buộc.

Nếu bắt buộc:

File:
Reason:
Why necessary:
Impact:
44. REGRESSION MODERATOR

Sau Phase 2C test:

Moderator Login
Moderator Dashboard
Moderator Sidebar
Moderator Security
Product Queue
Product Detail
Review Queue
Review Detail
ReportCase Queue
ReportCase Detail
Moderator History

Phase 2C không được làm hỏng Phase 1/2A/2B.

45. REGRESSION ADMIN

Test tối thiểu:

Admin Login
Admin Dashboard
User Management
Shop Management
Category
WEB Voucher
Banner
Admin Product
Admin Review

ReportCase integration không được làm hỏng Admin.

46. FILE SCOPE

Chỉ thay đổi file cần thiết cho:

Moderator Controller
Moderator DTO/ViewModel
Moderator Template
Moderator CSS
Moderator JS
Moderator History UI
Moderator ReportCase UI
Moderator integration
Security integration nếu thật sự cần
Contract/Adapter nếu thuộc Phase 2C
Tests liên quan Moderator

Nếu backend dependency đã tồn tại:

KHÔNG sửa backend của developer khác

nếu không thuộc ownership Phase 2C.

Nếu backend thiếu:

Xác định contract
Xác định seam
Implement phần Moderator
Mark INTEGRATION-READY
47. PHÂN LOẠI KẾT QUẢ — THAY CHO DONE/DEPENDENCY CŨ

Phase 2C dùng các trạng thái:

IMPLEMENTED
INTEGRATION-READY
INTEGRATED
PARTIAL
MISSING
WRONG
OUT OF SCOPE
BLOCKED
IMPLEMENTED

Phần thuộc Phase 2C đã implement đầy đủ và test độc lập.

Ví dụ:

UI
Controller
Adapter
Validation
Security
Contract tests

đã hoàn thành.

INTEGRATION-READY

Phần Phase 2C đã implement nhưng implementation bên ngoài chưa kết nối.

Ví dụ:

ReportCase Moderator UI = IMPLEMENTED
ReportCase Contract = READY
External ReportCase Backend = MISSING
Integration = INTEGRATION-READY

Đây không phải blocker.

INTEGRATED

Chỉ dùng khi:

Real external backend
        ↓
Real API/service
        ↓
Moderator implementation
        ↓
Integration test

đã chạy thành công.

PARTIAL

Phần thuộc Phase 2C còn thiếu implementation.

MISSING

Component cần thiết chưa tồn tại và chưa có contract/seam đủ để hoàn thành integration.

WRONG

Implementation hiện tại có nhưng vi phạm:

Policy
Contract
Security
State transition
Business boundary
OUT OF SCOPE

Không thuộc Phase 2C.

BLOCKED

Chỉ dùng cho hard technical/environmental blocker thực sự, ví dụ:

Repository không build được do baseline corruption
Database không thể khởi động
Environment không thể chạy
Required toolchain unavailable

Không dùng:

Tuấn chưa code
Quốc Anh chưa code
Mạnh Quân chưa code
Branch khác chưa merge
Commit khác chưa có
48. MASTER TABLE PHASE 2C

Tạo bảng thực tế:

#	Nghiệp vụ	Contract	External Implementation	UI	Security	DB	Test	Implementation Status	Integration Status	Integration Seam	Notes
1	ReportCase Queue	?	?	?	?	?	?	?	?	?	
2	ReportCase Detail	?	?	?	?	?	?	?	?	?	
3	Report Reason	?	?	?	?	N/A	?	?	?	?	
4	Evidence	?	?	?	?	?	?	?	?	?	
5	Auto Moderation Result	?	?	?	?	?	?	?	?	?	
6	Violation Display	?	?	?	?	?	?	?	?	?	
7	Vendor History	?	?	?	?	?	?	?	?	?	
8	Previous Cases	?	?	?	?	?	?	?	?	?	
9	Case Approve	?	?	?	?	?	?	?	?	?	
10	Case Reject	?	?	?	?	?	?	?	?	?	
11	Mandatory Reason	?	?	?	?	N/A	?	?	?	?	
12	Case Escalate	?	?	?	?	?	?	?	?	?	
13	Escalation Reason	?	?	?	?	?	?	?	?	?	
14	Moderator History	?	?	?	?	?	?	?	?	?	
15	Pagination / Filter	?	?	?	?	N/A	?	?	?	?	
16	Error Handling	?	?	?	?	N/A	?	?	?	?	
17	Object Security	?	?	?	?	N/A	?	?	?	?	
18	Concurrency	?	?	?	?	?	?	?	?	?	
19	Admin Escalation Visibility	?	?	?	?	?	?	?	?	?	
20	Admin Regression	N/A	N/A	?	?	N/A	?	?	?	N/A	
21	Moderator Regression	N/A	N/A	?	?	?	?	?	?	N/A	

Không tự điền IMPLEMENTED, INTEGRATED hoặc PASS nếu chưa kiểm tra thực tế.

49. TEST CHECKLIST BẮT BUỘC
A. ReportCase
[ ] Case Queue
[ ] Filter Case
[ ] Pagination
[ ] Case Detail
[ ] Reporter
[ ] Report Reason
[ ] Evidence
[ ] Product / Review Resource
[ ] Auto Moderation Result
[ ] Auto Flags
[ ] Violation
[ ] Vendor History
[ ] Previous Cases
[ ] Previous Violations
[ ] Previous Decisions
B. Decision
[ ] Approve Case
[ ] Reject Case + reason
[ ] Reject Case + empty reason
[ ] Reject Case + spaces
[ ] Escalate Case
[ ] Escalate + empty reason
[ ] Escalate + valid reason
[ ] Final Case cannot be processed again
C. History
[ ] Case Approved appears in History
[ ] Case Rejected appears in History
[ ] Case Escalated appears in History
[ ] Actor correct
[ ] Role correct
[ ] Reason correct
[ ] Timestamp correct
D. Security
[ ] Moderator → /moderator/** = ALLOW
[ ] Moderator → /admin/** = DENY
[ ] Customer → /moderator/** = DENY
[ ] Vendor → /moderator/** = DENY
[ ] Anonymous → /moderator/** = DENY
[ ] Locked Moderator = DENY
[ ] Object-level authorization
[ ] State transition authorization
E. Regression
[ ] Phase 1 Moderator
[ ] Phase 2A Product
[ ] Phase 2B Review
[ ] Admin Login
[ ] Admin Dashboard
[ ] Admin User
[ ] Admin Shop
[ ] Admin Category
[ ] WEB Voucher
[ ] Banner
[ ] Admin Product
[ ] Admin Review
50. MANUAL END-TO-END TEST

Có hai mức test phải phân biệt.

Level 1 — Independent Contract Test

Có thể chạy ngay cả khi external backend chưa tồn tại:

Moderator UI
    ↓
Contract
    ↓
Test Double

Chỉ dùng test double trong test.

Kết quả:

IMPLEMENTATION TEST = PASS

Không gọi đây là production E2E.

Level 2 — Real Integration E2E

Chỉ chạy khi backend thật tồn tại:

Real Report
    ↓
Real Auto Moderation
    ↓
Real ReportCase
    ↓
Moderator Queue
    ↓
Case Detail
    ↓
Real Decision
    ↓
Real History/Audit

Kết quả:

INTEGRATED
51. FLOW 1 — REPORTCASE APPROVE

Khi backend thật có:

Customer Report
        ↓
Auto Moderation
        ↓
ReportCase
        ↓
Moderator Login
        ↓
Case Queue
        ↓
Case Detail
        ↓
Reporter
        ↓
Evidence
        ↓
Auto Result
        ↓
Vendor History
        ↓
Approve
        ↓
Database
        ↓
History / Audit

Expected:

Case được xử lý
Status đúng
Decision đúng
History có record nếu contract hỗ trợ

Nếu backend chưa có:

Contract test:
PASS

Phase 2C:
IMPLEMENTED

Integration:
INTEGRATION-READY
52. FLOW 2 — REPORTCASE REJECT
ReportCase
    ↓
Moderator
    ↓
Reject
    ↓
Empty Reason

Expected:

Validation Error

Sau đó:

Reject
    ↓
Valid Reason
    ↓
Backend contract
    ↓
Case Rejected
    ↓
History
53. FLOW 3 — REPORTCASE ESCALATION
Customer Report
        ↓
ReportCase
        ↓
CRITICAL / policy-defined severe case
        ↓
Moderator Queue
        ↓
Case Detail
        ↓
Vendor History
        ↓
Escalate
        ↓
Reason
        ↓
Admin
        ↓
History / Audit

Expected khi integration tồn tại:

Case được escalation
Reason được lưu
Admin nhận escalation nếu backend hỗ trợ
Moderator không tự ban/suspend Vendor
History có record

Nếu Admin escalation backend chưa tồn tại:

Escalation Contract:
READY

Moderator Escalation:
IMPLEMENTED

Admin Integration:
INTEGRATION-READY
54. KHÔNG ĐƯỢC FAKE DATA

Không tạo fake production data cho:

ReportCase Queue
Evidence
Vendor History
Violation
Auto Moderation Result
Moderator History
Escalation

Nếu cần test:

Test account:
Test ReportCase:
Test Vendor:
Test Product/Review:

Phải ghi rõ test data.

Test double chỉ nằm trong test environment.

Không đưa test data/fake service vào production logic.

55. DATABASE CHECK

Nếu real backend available, sau mỗi action:

Case Approve
Case Reject
Case Escalate

kiểm tra:

Status
Decision
Reason
Moderator
Timestamp
Escalation
Violation
AuditLog

chỉ với field thực tế tồn tại.

Nếu backend chưa có:

Không fake DB verification.

Contract test + integration-ready status.
56. BUILD

Chạy:

mvn clean package

và:

mvn test

nếu project có Maven/test suite.

Ghi:

BUILD: PASS / FAIL
TEST: PASS / FAIL

Nếu FAIL:

Command:
Error:
Root cause:
File:
Function:

Nếu lỗi thuộc baseline:

BASELINE ISSUE

Không gán thành dependency của developer khác nếu chưa có bằng chứng.

57. BÁO CÁO FILE THAY ĐỔI

Sau khi hoàn thành:

Created:
Modified:
Deleted:

Với file quan trọng:

File:
Purpose:
Why changed:
Integration role:

Chỉ ghi file thực tế đã thay đổi.

Không liệt kê file không đụng tới.

58. BÁO CÁO KẾT QUẢ PHASE 2C

Dùng format:

# PHASE 2C RESULT

## 1. Git Baseline

Branch:
HEAD:
Working tree:
Uncommitted changes:

## 2. Phase Preconditions

Phase 1:
Phase 2A:
Phase 2B:

## 3. External Contracts

ReportCase:
Violation:
Auto Moderation:
AuditLog:
Escalation:

## 4. Implementation Status

IMPLEMENTED:
...

PARTIAL:
...

MISSING:
...

WRONG:
...

OUT OF SCOPE:
...

## 5. Integration Status

INTEGRATED:
...

INTEGRATION-READY:
...

External implementations missing:
...

## 6. REPORTCASE

Queue:
Detail:
Reason:
Evidence:
Auto Result:
Violation:
Vendor History:
Previous Cases:
Approve:
Reject:
Escalate:

## 7. MODERATOR HISTORY

Status:
Source of truth:
Audit integration:

## 8. SECURITY TEST

...

## 9. OBJECT / STATE SECURITY

...

## 10. CONCURRENCY TEST

...

## 11. REGRESSION TEST

...

## 12. BUILD

PASS / FAIL

## 13. TEST

PASS / FAIL

## 14. FILES CHANGED

Created:
Modified:
Deleted:

## 15. KNOWN ISSUES

...

## 16. HARD BLOCKERS

...

## 17. INTEGRATION POINTS

...

## 18. PHASE 3 DEPENDENCY

...

## 19. FINAL STATUS

IMPLEMENTED
INTEGRATION-READY
INTEGRATED
PARTIAL

Không dùng DEPENDENCY – Anh Tuấn làm status.

Nếu backend Tuấn chưa tồn tại:

External Implementation:
MISSING

Phase 2C Implementation:
IMPLEMENTED

Integration:
INTEGRATION-READY
59. ĐIỀU KIỆN ĐỂ GHI IMPLEMENTED

Phần thuộc Phase 2C được ghi IMPLEMENTED khi:

Queue UI tồn tại
Detail UI tồn tại
Contract được xác định
Integration seam tồn tại
Security đúng ở phần Phase 2C
State-aware UI đúng
Validation đúng
Error handling đúng
Contract/integration tests pass
Không fake production behavior
Không phụ thuộc implementation chưa tồn tại của developer khác

Không yêu cầu external backend phải hoàn thành để ghi IMPLEMENTED.

60. ĐIỀU KIỆN ĐỂ GHI INTEGRATED

Chỉ ghi INTEGRATED khi:

Real backend
    ↓
Real API / service
    ↓
Moderator integration seam
    ↓
UI
    ↓
Real integration test

đã chạy thành công.

Ví dụ:

ReportCase UI:
IMPLEMENTED

ReportCase Backend:
IMPLEMENTED

Moderator Integration:
INTEGRATED

E2E:
PASS
61. ĐIỀU KIỆN KẾT THÚC PHASE 2C

Phase 2C hoàn thành phần implementation của mình khi:

ReportCase Queue hoạt động
ReportCase Detail hoạt động
Report Reason hiển thị
Evidence được consume đúng contract
Auto Moderation Result được consume đúng contract
Violation được consume đúng contract
Vendor History được consume đúng contract
Previous Cases được consume đúng contract
Case Approve contract được implement
Case Reject contract được implement
Reject validation đúng
Case Escalate contract được implement
Escalation Reason validation đúng
Moderator History contract được implement
Audit integration seam được xác định
Security contract đúng
Object-level security được test
State transition được test
Concurrency behavior được test/contract-test
Phase 2A không bị hỏng
Phase 2B không bị hỏng
Admin regression pass
Build pass
Test pass nếu test suite tồn tại
Không overwrite code người khác
Không fake production data
Không triển khai nghiệp vụ Phase 3
Integration seams được ghi rõ
External backend status được ghi rõ
Known issues được báo cáo
Phase 3 integration contract được xác định

External backend chưa tồn tại không làm Phase 2C bị BLOCKED.

Khi đó:

FINAL STATUS:
IMPLEMENTED + INTEGRATION-READY
62. RANH GIỚI PHASE 2C VỚI PHASE 3

Phase 2C:

REPORTCASE
    ↓
MODERATOR DECISION
    ↓
HISTORY
    ↓
ESCALATION

Không triển khai sâu:

ADMIN SEVERE ENFORCEMENT
SHOP SUSPENSION
VENDOR BAN
FUNDS FREEZE
KYC
AUDIT ADMIN MANAGEMENT
VIOLATION ENFORCEMENT ENGINE

Phase 2C chỉ:

create/use escalation contract
        ↓
chuyển case cho Admin

Không biến Phase 2C thành Phase 3.

63. PHASE 3 INTEGRATION CONTRACT

Trước khi kết thúc Phase 2C phải ghi rõ seam cho Phase 3:

Moderator
    ↓
Escalation
    ↓
Admin Queue
    ↓
Admin Enforcement

Phase 2C chịu trách nhiệm:

Case
Escalation reason
Actor
Timestamp
Escalation state
Integration contract

Phase 3 chịu trách nhiệm:

Severe Enforcement
Vendor Ban
Shop Suspension
Funds Freeze
Admin-level actions

Không implement sâu Phase 3 trong Phase 2C.

64. MỤC TIÊU CUỐI CÙNG

Sau Phase 2C, architecture phải đạt:

CUSTOMER
    ↓
REPORT PRODUCT / REVIEW
    ↓
AUTO MODERATION
    ↓
REPORT CASE
    ↓
MODERATOR QUEUE
    ↓
CASE DETAIL
    ├── Reporter
    ├── Evidence
    ├── Auto Result
    ├── Violation
    ├── Vendor History
    └── Previous Cases
            ↓
     ┌──────┼──────┐
     ↓      ↓      ↓
  APPROVE REJECT ESCALATE
     ↓      ↓      ↓
  HISTORY HISTORY ADMIN

Ranh giới:

AUTO MODERATION
= Máy kiểm tra

REPORTCASE
= Case được tạo từ Report

MODERATOR
= Xử lý moderation thông thường

MODERATOR HISTORY
= Lịch sử thao tác Moderator

ESCALATION
= Chuyển case vượt quyền cho Admin

ADMIN
= Severe Enforcement / quyết định cấp cao
65. NGUYÊN TẮC CHỐT CỦA PHASE 2C
Tôi implement được phần của tôi bằng contract/interface hiện tại;
backend của Tuấn là integration point,
không phải prerequisite để bắt đầu hoặc hoàn thành implementation.

Cụ thể:

BACKEND CÓ
    ↓
Reuse contract/API
    ↓
Implement Moderator
    ↓
Test
    ↓
INTEGRATED

hoặc:

BACKEND CHƯA CÓ
    ↓
Xác định contract
    ↓
Xác định integration seam
    ↓
Implement Moderator
    ↓
Contract/Test-double tests
    ↓
IMPLEMENTED
    +
INTEGRATION-READY
    ↓
Backend thật xuất hiện
    ↓
Connect tại seam
    ↓
INTEGRATED

Không có bước:

Chờ Tuấn làm xong
↓
mới được bắt đầu Phase 2C

Và cũng không có bước:

Fake backend
↓
UI giả
↓
đánh dấu DONE
66. PHASE 2C FINAL BOUNDARY
PHASE 2C
=
REPORTCASE
+
MODERATOR HISTORY
+
ESCALATION
+
CONTRACT / INTEGRATION SEAM
+
SECURITY
+
TEST

Không triển khai lại:

Product Moderation
Review Moderation
Auto Moderation Engine
KYC
Payment
Finance
Settlement
Payout
Refund
Admin Enforcement

Không overwrite code người khác.

Không dùng developer khác làm blocker.

Không fake dữ liệu production.

Không fake backend.

Không đánh dấu INTEGRATED khi backend thật chưa được kết nối.

Trạng thái hợp lệ khi phần của bạn đã hoàn thành nhưng backend ngoài chưa có là:

IMPLEMENTED
+
INTEGRATION-READY

Đó là trạng thái hoàn thành độc lập đúng với nguyên tắc Phase 2C.