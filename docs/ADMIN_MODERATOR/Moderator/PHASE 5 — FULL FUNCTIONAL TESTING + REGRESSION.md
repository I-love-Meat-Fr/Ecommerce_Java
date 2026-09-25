PHASE 5 — FULL FUNCTIONAL TESTING + REGRESSION

Implementation instruction: Read this Phase 5 file as the single source of truth for Phase 5.

First inspect the actual implementation and the Phase 1 → Phase 4C specifications/files.

Phase 5 is a testing, verification, defect-fixing and regression phase. It is not a new feature-development phase.

Test the real implementation and real integration wherever available. Do not manufacture success.

Independence rule:

“Tôi implement được phần của tôi bằng contract/interface hiện tại; backend của Tuấn là integration point, không phải prerequisite để bắt đầu hoặc hoàn thành implementation.”

Phase 5 must be independently executable.

Do not wait for another developer, branch, commit, MR, or backend implementation to begin testing or to complete testing of the user-owned scope.

If an external implementation is unavailable:

Identify the existing contract/interface.
Test the user-owned implementation against that contract.
Use test doubles/fixtures only inside tests when appropriate.
Never fake production behavior or production data.
Mark the integration as INTEGRATION-READY / NOT-INTEGRATED.
Do not mark it BLOCKED merely because another implementation is missing.
Integrate and rerun the affected tests when the real implementation becomes available.
1. MỤC TIÊU

Phase 5 là phase:

FULL FUNCTIONAL TESTING
        +
REGRESSION
        +
SECURITY VERIFICATION
        +
INTEGRATION VERIFICATION
        +
E2E VERIFICATION
        +
DEFECT FIX
        +
BUILD VERIFICATION

Kiểm tra toàn bộ phạm vi đã triển khai từ:

Phase 1
   ↓
Phase 2A
   ↓
Phase 2B
   ↓
Phase 2C
   ↓
Phase 3A
   ↓
Phase 3B
   ↓
Phase 3C
   ↓
Phase 4A
   ↓
Phase 4B
   ↓
Phase 4C
   ↓
Phase 5

Mục tiêu kiểm tra toàn bộ flow:

UI
 ↓
API
 ↓
Authentication
 ↓
Authorization
 ↓
Validation
 ↓
Business Logic
 ↓
Persistence
 ↓
Database
 ↓
Response
 ↓
UI

Phase 5 phải xác định chính xác:

PASS
FAIL
PARTIAL
MISSING
WRONG
IMPLEMENTED
TESTED
INTEGRATION-READY
NOT-INTEGRATED
OUT OF SCOPE
BLOCKED

Không được đánh dấu PASS chỉ vì:

UI hiển thị

hoặc:

Maven build thành công
2. PHASE 5 KHÔNG PHẢI FEATURE DEVELOPMENT

Phase 5 chỉ được:

TEST
 ↓
FIND DEFECT
 ↓
CLASSIFY
 ↓
FIX CONFIRMED DEFECT
 ↓
RETEST
 ↓
REGRESSION

Không được biến Phase 5 thành:

NEW FEATURE DEVELOPMENT

Không tự thêm:

business feature mới;
API mới không có trong Phase 1 → 4C;
workflow mới;
lifecycle mới;
permission mới;
database architecture mới;
CMS mới;
moderation engine mới;
KYC provider mới;
Finance Core mới.

Nếu phát hiện requirement chưa được implement:

MISSING

Nếu requirement đó thuộc Phase 1 → 4C:

Defect / Missing implementation

Nếu ngoài scope:

OUT OF SCOPE

Không tự mở rộng scope để làm cho đủ.

3. PHẠM VI PHASE 5
3.1 IN SCOPE
Testing
Unit Test
Controller Test
Service Test
Repository Test nếu có
Integration Test
API Test
Functional Test
Security Test
Authentication Test
Authorization Test
Object-level Security
IDOR Test
State Transition Test
Validation Test
Error Handling Test
Database Persistence Test
Concurrency Test nếu applicable
Contract Test
Integration Test
End-to-End Test
Regression Test
Regression
Moderator
Admin
Vendor
Customer
Product Moderation
Review Moderation
ReportCase
Escalation
Enforcement
KYC Monitoring
Violation
AuditLog Query
Dashboard
Voucher
Banner
Verification
Security
API
Database
UI
Business flow
Integration seam
Existing test suite
Build
Regression
4. OUT OF SCOPE

Không triển khai mới:

Auto Moderation Engine
Security Core
Order Core
Finance Core
Payment Core
KYC Provider
AuditLog backend core
ReportCase backend mới
Violation backend mới
Complaint backend mới
Vendor UI mới
Customer UI mới
Dashboard feature mới
Voucher feature mới
Banner feature mới

Các module trên chỉ được:

TEST
VERIFY
FIX CONFIRMED BUG

nếu đã thuộc Phase 1 → Phase 4C.

Không rewrite architecture chỉ vì test phát hiện vấn đề.

5. NGUYÊN TẮC ĐỘC LẬP

Phase 5 phải có khả năng bắt đầu và thực hiện độc lập.

Không được viết:

DEPENDENCY – Anh Tuấn
DEPENDENCY – Quốc Anh
DEPENDENCY – Mạnh Quân
DEPENDENCY – Anh Quân

như một lý do mặc định để dừng Phase 5.

Thay vào đó phải ghi:

External Integration:
Contract:
Integration Seam:
Current External Implementation:
User-Owned Test:
External Integration Test:
Status:

Ví dụ:

External Integration:
AuditLog

Contract:
Existing AuditLog contract

Integration Seam:
Moderation action → AuditLog writer

Current External Implementation:
AVAILABLE / MISSING / UNKNOWN

User-Owned Test:
Moderation action creates expected integration request

External Integration Test:
NOT-INTEGRATED

Status:
INTEGRATION-READY
6. PHÂN LOẠI TRẠNG THÁI
6.1 PASS

PASS khi behavior thực tế đúng với contract/requirement và test đã chạy thành công.

6.2 FAIL

FAIL khi implementation thực tế không đáp ứng requirement/contract.

Ví dụ:

Expected: 403
Actual: 200
6.3 PARTIAL

Một phần behavior hoạt động nhưng chưa đầy đủ.

6.4 MISSING

Requirement bắt buộc thuộc scope nhưng implementation chưa tồn tại.

6.5 WRONG

Implementation tồn tại nhưng sai:

business rule;
contract;
security;
state transition;
validation;
persistence;
response.
6.6 IMPLEMENTED

Phần implementation thuộc scope đã hoàn thành.

6.7 TESTED

Phần implementation đã được test thực tế.

6.8 INTEGRATION-READY

Phần user-owned đã hoàn thành và có integration seam/contract rõ ràng, nhưng external implementation chưa được kết nối.

6.9 NOT-INTEGRATED

Không thể thực hiện integration/E2E thật vì implementation bên ngoài chưa tồn tại hoặc chưa được kết nối.

Điều này không tự động có nghĩa là FAIL hoặc BLOCKED.

6.10 OUT OF SCOPE

Không thuộc Phase 5 hoặc không thuộc Phase 1 → 4C.

6.11 BLOCKED

Chỉ sử dụng khi có blocker thực sự khiến test không thể tiếp tục, ví dụ:

Repository không build được do môi trường
Database infrastructure không khởi động được
Test environment unavailable
Required runtime unavailable
Build tooling bị hỏng
Không thể execute test environment

Không dùng BLOCKED chỉ vì:

external developer chưa implement backend
7. SOURCE OF TRUTH

Trước khi test phải đọc:

Source code hiện tại.
Gate 0.
Phase 1.
Phase 2A.
Phase 2B.
Phase 2C.
Phase 3A.
Phase 3B.
Phase 3C.
Phase 4A.
Phase 4B.
Phase 4C.
BỘ LUẬT & CHÍNH SÁCH SÀN MARKETPLACE.
Test hiện tại.
Git state hiện tại.

Các Phase là:

SOURCE OF TRUTH

Không thay đổi business rule chỉ vì implementation hiện tại đang khác specification.

Nếu specification và implementation khác nhau:

Classify:
WRONG
MISSING
PARTIAL

sau đó mới quyết định fix.

8. GIT SAFETY — TRƯỚC KHI TEST

Chạy:

git status
git branch
git log --oneline --decorate -10

Ghi nhận:

Current branch:
Current HEAD:
Working tree:
Uncommitted changes:

Nếu có uncommitted changes:

DO NOT RESET
DO NOT STASH
DO NOT OVERWRITE
DO NOT DELETE
DO NOT MERGE
DO NOT PUSH

Không commit nếu chưa được yêu cầu.

Không reset code của developer khác.

9. KIỂM TRA TEST HIỆN TẠI

Kiểm tra:

src/test/**

và toàn bộ test hiện có.

Phân loại:

Unit
Controller
Service
Repository
Integration
Security
API
Functional
E2E
Regression

Không assume:

Test PASS = implementation đúng

Phải kiểm tra test có thực sự kiểm tra behavior hay chỉ kiểm tra mock.

Đặc biệt phát hiện:

Mock quá mức
Assertion quá yếu
Test không verify DB
Test không verify security
Test chỉ verify HTTP 200
Test chỉ verify UI
Test bỏ qua business rule
Test bị disable
Test bị skip không có lý do
10. TEST ENVIRONMENT

Ghi nhận:

Application:
Database:
Profile:
Test profile:
Authentication setup:
External integrations:
Storage:
Browser/UI environment:

Reuse environment/profile hiện tại.

Không tạo test infrastructure mới nếu project đã có.

Không thay đổi production configuration chỉ để test pass.

11. TEST DATA

Test data phải tách khỏi production data.

Tối thiểu cần có:

Test Admin
Test Moderator
Test Vendor
Test Customer

Test Product
Test Review
Test ReportCase
Test Order
Test Voucher
Test Banner

Nếu project đã có fixture/factory:

REUSE

Không tạo duplicate test framework.

Không hard-code production data.

Không đưa test-only behavior vào production.

12. PHASE 1 — MODERATOR ACCESS TEST
Flow
Login
 ↓
Authentication
 ↓
Role
 ↓
Moderator Layout
 ↓
Moderator Sidebar
 ↓
/moderator/**
Security Matrix
Actor	Expected
Anonymous	DENY
CUSTOMER	DENY
VENDOR	DENY
MODERATOR	ALLOW
ADMIN	Theo Security Architecture
Test
Login đúng.
Login sai.
Authentication.
Role.
Unauthorized.
Forbidden.
Locked account nếu có.
Session.
Direct API access.
Direct URL access.
Method security.
Object-level access nếu applicable.

Nếu Security Core external implementation chưa available:

Security Contract:
Existing SecurityConfig / Method Security contract

User-Owned Test:
Route/controller/method contract

Integration:
NOT-INTEGRATED nếu Security Core chưa kết nối

Status:
INTEGRATION-READY

Không fake security success.

13. PHASE 2A — PRODUCT MODERATION TEST
Flow
Vendor Product
      ↓
Auto Moderation
      ↓
Moderator Queue
      ↓
Product Detail
      ↓
Approve / Reject / Escalate
      ↓
Database
      ↓
History / Audit nếu có
Test
Queue.
Detail.
Status.
Reason.
Approve.
Reject.
Mandatory reason nếu contract yêu cầu.
Escalate.
Pagination.
Filter.
Search nếu có.
Error handling.
State transition.
Object-level security.
Database persistence.
History/Audit integration nếu có.

Không fake Auto Moderation result.

Nếu Auto Moderation implementation chưa tồn tại:

Auto Moderation:
External Integration

Contract:
Existing Product moderation status/result contract

Moderator implementation:
Test independently

Auto Moderation E2E:
NOT-INTEGRATED

Status:
INTEGRATION-READY
14. PHASE 2B — REVIEW MODERATION TEST
Flow
Review
 ↓
Moderation
 ↓
Approve / Reject / Hide / Unhide

Test:

Queue.
Detail.
Status.
Reason.
Approve.
Reject.
Hide.
Unhide.
Invalid state.
Permission.
Object-level security.
Database persistence.
History/Audit nếu có.
Regression.

Không tạo Review backend mới trong Phase 5.

15. PHASE 2C — REPORTCASE TEST
Flow
Report
 ↓
Auto Moderation
 ↓
ReportCase
 ↓
Moderator Queue
 ↓
Case Detail
 ↓
Approve / Reject / Escalate
 ↓
History

Test:

Case Queue.
Case Detail.
Reporter.
Reason.
Evidence.
Auto Result nếu contract có.
Violation.
Vendor History.
Previous Cases.
Approve.
Reject.
Reject empty reason.
Reject spaces.
Escalate.
Escalate reason.
Final state.
History.
Object-level security.
Concurrency.
Database persistence.

Không fake ReportCase.

Nếu ReportCase backend chưa tồn tại:

ReportCase Contract:
...

Integration Seam:
...

Current External Implementation:
MISSING

Moderator/Admin-owned testing:
Contract-level / independent testing

Status:
INTEGRATION-READY
16. PHASE 3A — ADMIN ESCALATION + ENFORCEMENT
Flow
Moderator
 ↓
Escalation
 ↓
Admin
 ↓
Review
 ↓
Enforcement
 ↓
Audit

Test:

Admin nhận escalation.
Escalation detail.
Permission.
Enforcement permission.
Severe action.
State transition.
Validation.
Audit integration.
Unauthorized access.
Moderator không được thực hiện Admin-only enforcement.

Audit behavior phải tuân thủ contract hiện tại.

Nếu AuditLog implementation chưa tồn tại:

Audit Integration:
INTEGRATION-READY

Audit E2E:
NOT-INTEGRATED

Không fake AuditLog record.

17. PHASE 3B — KYC MONITORING
Flow
KYC
 ↓
Provider / Backend Verification
 ↓
Admin Monitoring

Test:

KYC status.
Pending.
Verified.
Rejected nếu contract hỗ trợ.
Monitoring UI.
Access control.
Detail.
Error handling.
PII handling.
Integration seam.

Không giả lập production KYC Provider thành VERIFIED.

Test double chỉ được dùng:

TEST PROFILE
18. PHASE 3C — VIOLATION + AUDITLOG QUERY

Kiểm tra:

Violation Query
AuditLog Query

Test:

List.
Detail.
Filter.
Pagination.
Status.
Severity.
Action.
Actor.
Date.
Empty state.
Invalid query.
Permission.
Direct API access.

AuditLog:

READ / QUERY

Không cho UI mutation nếu contract không cho phép.

19. PHASE 4A — DASHBOARD TEST

Dashboard phải sử dụng nguồn dữ liệu authoritative hiện có.

Phân biệt:

GMV
Platform Revenue
Vendor Sales
Vendor Payable
Refund

Không đồng nhất:

Delivered Order Total
=
Platform Revenue
Test
KPI API.
DTO.
Mapping.
Aggregation.
Date filter nếu có.
Empty data.
Large data.
UI rendering.
Backend response.
Database aggregation.
Security.

Nếu Finance source chưa tồn tại:

Finance Integration:
Contract:
Integration Seam:
Current External Implementation:
MISSING

Dashboard-owned test:
PASS / FAIL

External E2E:
NOT-INTEGRATED

Status:
INTEGRATION-READY

Không dùng hard-coded:

0

để che thiếu dữ liệu.

20. PHASE 4B — VOUCHER TEST
Flow
Voucher
 ↓
Cart / Checkout
 ↓
Discount
 ↓
Order

Test:

Voucher CRUD.
Validation.
Active/Inactive.
Expired.
Start date.
End date.
Usage limit.
Per-user limit nếu có.
Minimum order.
Discount calculation.
Invalid voucher.
Applicable.
Not applicable.
Successful order.
Failed order.
Cancel/refund nếu contract hỗ trợ.

Đặc biệt:

Voucher usage
MUST NOT be incremented
before successful order

nếu business contract yêu cầu.

Không fake discount.

21. PHASE 4C — BANNER TEST
Flow
Admin
 ↓
Banner CRUD
 ↓
Status / Schedule
 ↓
Public / Customer Display

Test:

Create.
View.
Update.
Delete nếu có.
Publish.
Unpublish.
Active.
Inactive.
Start time.
End time.
Invalid schedule.
Validation.
Admin permission.
Public display.
Expired banner.
Scheduled banner.
Empty state.
Error handling.
Double submit.
Image handling.
Target URL security nếu có.

Không fake schedule bằng JavaScript.

Không fake publish bằng UI state.

22. SECURITY TEST MATRIX

Bắt buộc kiểm tra:

Anonymous
CUSTOMER
VENDOR
MODERATOR
ADMIN

Cho các vùng:

/moderator/**
/admin/**
/vendor/**
/customer/**

Kiểm tra:

URL authorization.
Controller authorization.
Method security.
Object-level security.
IDOR.
Cross-user access.
State bypass.
Unauthorized API call.
Direct API access.
Missing authentication.
Forbidden response.
Role escalation.

Không chỉ test UI.

Phải test API trực tiếp.

23. OBJECT-LEVEL SECURITY

Test dữ liệu chéo:

User A
User B

Vendor A
Vendor B

Product A
Product B

Review A
Review B

Case A
Case B

Order A
Order B

Kiểm tra:

User A không đọc được dữ liệu User B
User A không sửa được dữ liệu User B

Vendor A không đọc/sửa dữ liệu Vendor B

Không xử lý Case ngoài quyền

Không xem Order ngoài quyền

Không sửa Product ngoài quyền

Không bypass UI bằng API

Đặc biệt test IDOR:

GET /resource/{id}
PUT /resource/{id}
DELETE /resource/{id}
POST /resource/{id}/action

nếu endpoint tồn tại.

24. STATE TRANSITION TEST

Kiểm tra các state thực tế của từng module.

Ví dụ:

PENDING
APPROVED
REJECTED
ESCALATED
ACTIVE
INACTIVE

Chỉ sử dụng enum/state thực tế.

Không tự tạo state.

Test:

Valid transition
Invalid transition
Final state
Repeated action
Direct API manipulation
Concurrent action

Ví dụ:

APPROVED
   ↓
REJECT

phải bị từ chối nếu backend contract không cho phép.

Không chỉ disable button frontend.

25. VALIDATION TEST

Test:

null
empty
spaces
too short
too long
invalid format
negative number
zero
duplicate
invalid ID
invalid enum
invalid date
invalid range

Áp dụng cho:

Product.
Review.
ReportCase.
Escalation.
Violation.
KYC.
Voucher.
Banner.
Dashboard filters.
Các API quan trọng khác.

Backend phải validate lại.

Không dựa duy nhất vào:

HTML
JavaScript
UI validation
26. ERROR HANDLING TEST

Kiểm tra nếu applicable:

400 Bad Request
401 Unauthorized
403 Forbidden
404 Not Found
409 Conflict
500 Internal Server Error

Expected:

Correct HTTP status
User-friendly message
No raw Java exception
No stack trace
No sensitive information
No broken page
No silent failure
27. CONCURRENCY TEST

Đối với state transition:

User A → Action
User B → Action

Ví dụ:

Moderator A → Approve Case
Moderator B → Reject Case

hoặc:

Moderator A → Escalate
Moderator B → Reject

Expected:

Không overwrite silently.
Không tạo state mâu thuẫn.
Backend bảo vệ transition.
Conflict được xử lý theo contract.
Không để UI quyết định concurrency safety.
28. DATABASE PERSISTENCE TEST

Sau action quan trọng kiểm tra DB:

Create
Update
Delete
Approve
Reject
Escalate
Enforcement
Voucher
Banner

Kiểm tra các field thực tế:

Status
Actor
Reason
Timestamp
Relation
Reference ID
Usage
Audit reference
Escalation
Violation

Chỉ kiểm tra field tồn tại trong actual implementation/contract.

Không yêu cầu field không tồn tại.

29. API END-TO-END TEST

Mỗi critical flow:

Request
 ↓
Authentication
 ↓
Authorization
 ↓
Validation
 ↓
Controller
 ↓
Service
 ↓
Repository
 ↓
Database
 ↓
Response

Kiểm tra response thực tế.

Không chấp nhận:

HTTP 200
+
fake body

nếu business operation chưa thực sự xảy ra.

30. ADMIN REGRESSION

Tối thiểu:

Admin Login
Dashboard
User Management
Shop Management
Category
Product
Review
Escalation
Enforcement
KYC Monitoring
Violation
AuditLog
Voucher
Banner

Kiểm tra:

Existing flow still works
Existing permission still works
Existing data still accessible
No regression from Phase 1 → 4C
31. MODERATOR REGRESSION

Tối thiểu:

Moderator Login
Dashboard
Sidebar
Product Moderation
Review Moderation
ReportCase
History
Escalation
Security
32. VENDOR REGRESSION

Tối thiểu:

Register / Login
Shop
Product CRUD
Product Status
Order-related flow nếu có
KYC-related flow nếu có

Không mở rộng Vendor feature.

33. CUSTOMER REGRESSION

Tối thiểu:

Register
Login
Product Browse
Product Detail
Cart
Checkout
Order
Review
Report Product / Review nếu có
Voucher
Banner
34. FULL E2E FLOW 1 — PRODUCT MODERATION
Vendor
 ↓
Create Product
 ↓
Auto Moderation nếu có
 ↓
Moderator Queue
 ↓
Product Detail
 ↓
Approve / Reject
 ↓
Database
 ↓
History / Audit

Nếu Auto Moderation chưa tồn tại:

Moderator portion:
TESTABLE

Auto Moderation integration:
NOT-INTEGRATED

Status:
INTEGRATION-READY

Không fake Auto Moderation result.

35. FULL E2E FLOW 2 — REVIEW MODERATION
Customer
 ↓
Review
 ↓
Moderation
 ↓
Moderator
 ↓
Approve / Reject / Hide
 ↓
Database

Test actual persistence.

36. FULL E2E FLOW 3 — REPORTCASE
Customer
 ↓
Report
 ↓
ReportCase
 ↓
Moderator
 ↓
Approve / Reject / Escalate
 ↓
History

Nếu external ReportCase backend chưa available:

NOT-INTEGRATED

Không fake Case.

37. FULL E2E FLOW 4 — ESCALATION
Moderator
 ↓
Escalate
 ↓
Admin
 ↓
Enforcement
 ↓
AuditLog

Test:

Permission.
State.
Persistence.
Audit integration.

Nếu AuditLog external implementation unavailable:

Escalation:
TESTED

Audit integration:
INTEGRATION-READY

Audit E2E:
NOT-INTEGRATED
38. FULL E2E FLOW 5 — ORDER + VOUCHER
Customer
 ↓
Product
 ↓
Cart
 ↓
Voucher
 ↓
Checkout
 ↓
Order
 ↓
Database

Verify:

Discount
Order total
Voucher usage
Order status
Persistence

Không fake discount/order.

39. FULL E2E FLOW 6 — DASHBOARD
Orders / Finance Data
 ↓
Dashboard API
 ↓
Aggregation
 ↓
DTO
 ↓
Dashboard UI

Kiểm tra source data và semantics.

Nếu external finance source unavailable:

Dashboard:
TESTED / INTEGRATION-READY

Finance E2E:
NOT-INTEGRATED
40. FULL E2E FLOW 7 — BANNER
Admin
 ↓
Create / Schedule / Publish
 ↓
Backend
 ↓
Public / Customer Display

Verify:

DB
Status
Schedule
Visibility
Permission

Không fake banner state.

41. CONTRACT TESTING

Đối với mỗi external integration phải xác định:

Contract:
Integration Seam:
Request:
Response:
Error:
Authentication:
Authorization:
State:

Ví dụ:

Dashboard → Finance
Moderator → Auto Moderation
Moderation → AuditLog
Admin → KYC
ReportCase → Violation

Nếu contract đã tồn tại:

REUSE

Không tạo duplicate interface.

Nếu chưa có seam:

Define minimal seam
consistent with existing architecture

Không tạo duplicate domain service/entity.

42. EXTERNAL INTEGRATION TESTING

Mỗi external integration phải có:

Field	Result
Contract	
Integration Seam	
Current Implementation	
User-Owned Test	
External Integration Test	
Status	
Notes	

Các trạng thái:

AVAILABLE
MISSING
UNKNOWN

Integration status:

INTEGRATED
INTEGRATION-READY
NOT-INTEGRATED
43. TEST DOUBLE RULE

Test double được phép:

Unit Test
Integration Test isolation
Contract Test
Test profile

Không được:

Production code
Production profile
Real user flow
Production response
Production data

Ví dụ hợp lệ:

Mock FinanceGateway

trong test.

Không hợp lệ:

if finance unavailable:
    return 0

để làm production test pass.

44. BUG FIX RULE

Khi test phát hiện lỗi:

1. Reproduce
2. Capture actual result
3. Identify root cause
4. Classify
5. Fix nếu thuộc Phase 1 → 4C
6. Rerun affected test
7. Rerun regression

Classification:

BUG
MISSING
WRONG
PARTIAL
INTEGRATION-READY
NOT-INTEGRATED
OUT OF SCOPE
BLOCKED

Không fix bằng:

Disable security
Skip test
Comment-out test
Hard-code response
Fake database
Fake API
Fake status
Ignore exception
45. PRODUCTION CODE CHANGE RULE

Được sửa production code chỉ khi:

Confirmed defect
+
Belongs to Phase 1 → Phase 4C
+
Fix is necessary for correct behavior

Mỗi production fix phải ghi:

File:
Function:
Defect:
Root cause:
Fix:
Test:
Regression:

Không refactor lớn nếu không cần thiết.

Không rewrite module của developer khác.

46. OWNERSHIP RULE

Không dùng ownership của developer khác làm prerequisite.

Ví dụ:

AuditLog backend chưa có

không có nghĩa:

Phase 5 BLOCKED

Phải ghi:

AuditLog external implementation:
MISSING

User-owned audit integration test:
IMPLEMENTED

Contract:
AVAILABLE

Integration:
INTEGRATION-READY

Audit E2E:
NOT-INTEGRATED

Tương tự:

Finance
KYC Provider
Auto Moderation
ReportCase Backend
Violation Backend
Security Core
47. MASTER TEST MATRIX

Tạo bảng thực tế:

#	Module	Unit	API	Security	DB	E2E	Regression	Implementation	Integration	Seam	Result	Notes
1	Moderator Access	?	?	?	N/A	?	?	?	?	?	?	
2	Product Moderation	?	?	?	?	?	?	?	?	?	?	
3	Review Moderation	?	?	?	?	?	?	?	?	?	?	
4	ReportCase	?	?	?	?	?	?	?	?	?	?	
5	Escalation	?	?	?	?	?	?	?	?	?	?	
6	Enforcement	?	?	?	?	?	?	?	?	?	?	
7	KYC Monitoring	?	?	?	?	?	?	?	?	?	?	
8	Violation	?	?	?	?	?	?	?	?	?	?	
9	AuditLog Query	?	?	?	?	?	?	?	?	?	?	
10	Dashboard	?	?	?	?	?	?	?	?	?	?	
11	Voucher	?	?	?	?	?	?	?	?	?	?	
12	Banner	?	?	?	?	?	?	?	?	?	?	
13	Admin Regression	?	?	?	?	?	?	?	?	?	?	
14	Moderator Regression	?	?	?	?	?	?	?	?	?	?	
15	Vendor Regression	?	?	?	?	?	?	?	?	?	?	
16	Customer Regression	?	?	?	?	?	?	?	?	?	?	

Không tự điền PASS.

Chỉ điền sau khi test thực tế.

48. TEST CHECKLIST — AUTHENTICATION
[ ] Register
[ ] Login
[ ] Wrong password
[ ] Missing authentication
[ ] Unauthorized
[ ] Forbidden
[ ] Role access
[ ] Locked account nếu có
[ ] Session/authentication behavior
[ ] Direct API authentication
49. TEST CHECKLIST — MODERATOR
[ ] Product Moderation
[ ] Review Moderation
[ ] ReportCase
[ ] History
[ ] Escalation
[ ] Security
[ ] Object-level security
[ ] State transition
[ ] Database persistence
50. TEST CHECKLIST — ADMIN
[ ] Dashboard
[ ] User
[ ] Shop
[ ] Product
[ ] Review
[ ] Escalation
[ ] Enforcement
[ ] KYC
[ ] Violation
[ ] AuditLog
[ ] Voucher
[ ] Banner
51. TEST CHECKLIST — COMMERCE
[ ] Product
[ ] Cart
[ ] Checkout
[ ] Order
[ ] Voucher
[ ] Review
52. TEST CHECKLIST — SECURITY
[ ] Anonymous
[ ] CUSTOMER
[ ] VENDOR
[ ] MODERATOR
[ ] ADMIN

[ ] URL Security
[ ] API Security
[ ] Controller Security
[ ] Method Security
[ ] Object Security
[ ] IDOR
[ ] Cross-user access
[ ] State bypass
[ ] Direct API access
53. TEST CHECKLIST — VALIDATION
[ ] null
[ ] empty
[ ] spaces
[ ] too short
[ ] too long
[ ] invalid format
[ ] negative
[ ] zero
[ ] duplicate
[ ] invalid ID
[ ] invalid enum
[ ] invalid date
[ ] invalid date range
54. TEST CHECKLIST — ERROR HANDLING
[ ] 400
[ ] 401
[ ] 403
[ ] 404
[ ] 409
[ ] 500

[ ] Correct response
[ ] User-friendly message
[ ] No stack trace
[ ] No raw exception
[ ] No sensitive information
[ ] No silent failure
55. TEST CHECKLIST — STATE
[ ] Valid transition
[ ] Invalid transition
[ ] Final state
[ ] Repeated action
[ ] Direct API manipulation
[ ] Concurrent action
[ ] Conflict handling
56. TEST CHECKLIST — DATABASE
[ ] Create persistence
[ ] Update persistence
[ ] Delete persistence
[ ] Approve persistence
[ ] Reject persistence
[ ] Escalate persistence
[ ] Enforcement persistence
[ ] Voucher persistence
[ ] Banner persistence
[ ] Status
[ ] Actor
[ ] Reason
[ ] Timestamp
[ ] Relations
[ ] Reference IDs
[ ] Usage
57. BUILD VERIFICATION

Chạy các command phù hợp với project.

Tối thiểu:

mvn test
mvn clean test
mvn clean package

Nếu project có test profile:

REUSE existing profile.

Không tạo profile mới chỉ để bỏ qua lỗi.

Ghi:

TEST: PASS / FAIL
BUILD: PASS / FAIL

Nếu fail:

Command:
Error:
Root cause:
File:
Function:
Classification:
Integration:

Không che giấu build failure.

58. TEST RESULT

Thống kê:

Total tests:
Passed:
Failed:
Skipped:
Errors:

Theo category:

Unit:
Controller:
Service:
Repository:
Integration:
API:
Security:
Functional:
E2E:
Regression:

Nếu test skipped:

Test:
Reason:
External Integration:
Status:

Không coi:

SKIPPED

là:

PASS
59. REGRESSION RESULT

Phải ghi riêng:

Admin:
PASS / FAIL / PARTIAL / NOT-INTEGRATED

Moderator:
PASS / FAIL / PARTIAL / NOT-INTEGRATED

Vendor:
PASS / FAIL / PARTIAL / NOT-INTEGRATED

Customer:
PASS / FAIL / PARTIAL / NOT-INTEGRATED

Và:

Product Moderation:
Review Moderation:
ReportCase:
Escalation:
KYC:
Violation:
AuditLog:
Dashboard:
Voucher:
Banner:
60. E2E RESULT

Mỗi flow:

Flow:
Precondition:
Actual execution:
Expected:
Actual:
Result:
External Integration:

Không ghi:

PASS

nếu chỉ kiểm tra UI.

61. SECURITY RESULT

Báo cáo:

Authentication:
Authorization:
URL Security:
API Security:
Method Security:
Object-level Security:
IDOR:
Cross-user:
State Bypass:

Nếu có issue:

Endpoint:
Actor:
Expected:
Actual:
Severity:
Root cause:
Fix:
Retest:
62. DATABASE RESULT

Báo cáo:

Create:
Update:
Delete:
State transition:
Relations:
Audit reference:
Usage:
Timestamp:

Chỉ báo cáo field thực tế tồn tại.

63. FILE CHANGES

Sau khi hoàn thành:

Created:
Modified:
Deleted:

Mỗi file quan trọng:

File:
Purpose:
Why changed:
Defect:
Test:

Chỉ ghi file thực tế thay đổi.

64. GIT REVIEW CUỐI

Chạy:

git status
git diff --stat
git diff

Kiểm tra:

[ ] Không có file ngoài scope
[ ] Không có debug code
[ ] Không có fake production data
[ ] Không có hard-code bypass
[ ] Không disable security
[ ] Không comment-out test
[ ] Không xóa test fail
[ ] Không overwrite code người khác
[ ] Không có production test double
[ ] Không có accidental config change
65. DEFINITION OF DONE — MODULE TEST

Một module có thể ghi PASS khi:

Functional behavior đúng.
API đúng nếu module có API.
Validation đúng.
Security đúng.
Persistence đúng nếu có DB.
State transition đúng nếu có state.
Error handling đúng.
Regression liên quan không fail.
E2E pass nếu E2E applicable và integration tồn tại.
Không fake production behavior.
Không bypass security.
Không có critical defect chưa xử lý trong phần đã implement.

Không yêu cầu external integration phải tồn tại để đánh giá user-owned implementation.

Nếu external integration chưa tồn tại:

Implementation:
PASS / TESTED

Integration:
INTEGRATION-READY

External E2E:
NOT-INTEGRATED
66. CRITICAL DEFECT RULE

Critical defect là lỗi có ảnh hưởng nghiêm trọng tới:

Authentication
Authorization
Object-level Security
Data integrity
Critical state transition
Financial correctness
Order correctness
Compliance boundary
PII/security
Core production flow

Nếu critical defect tồn tại:

FINAL STATUS ≠ READY

Phải:

FIX
→ RETEST
→ REGRESSION

hoặc ghi rõ:

BLOCKED

nếu thực sự không thể tiếp tục.

67. PHASE 5 COMPLETION RULE

Phase 5 không yêu cầu mọi external integration phải có sẵn để bắt đầu hoặc hoàn thành user-owned testing.

Phải hoàn thành:

Phase 1 testing
Phase 2A testing
Phase 2B testing
Phase 2C testing
Phase 3A testing
Phase 3B testing
Phase 3C testing
Phase 4A testing
Phase 4B testing
Phase 4C testing

theo khả năng thực tế của từng contract/integration.

Mỗi phần phải được phân loại:

TESTED
NOT-INTEGRATED
INTEGRATION-READY
FAIL
PARTIAL
MISSING
WRONG
OUT OF SCOPE
68. FINAL STATUS MODEL

Không dùng chỉ:

READY
BLOCKED

Sử dụng:

READY FOR FINAL AUDIT

Khi:

user-owned implementation đã test;
critical flows đã test;
security đã test;
regression đã test;
build pass;
critical defects không còn;
external integrations chưa có được ghi rõ là INTEGRATION-READY/NOT-INTEGRATED;
không có test failure chưa được phân loại.
READY WITH INTEGRATION-READY ITEMS

Khi:

phần thuộc Phase 5 đã hoàn thành;
còn external integration chưa kết nối;
contract/seam đã sẵn sàng;
không có critical defect trong phần user-owned;
các integration gap đã được documented.

Ví dụ:

Dashboard:
IMPLEMENTED + TESTED + INTEGRATION-READY

AuditLog:
INTEGRATION-READY

KYC Provider:
NOT-INTEGRATED
PARTIAL

Khi user-owned testing/implementation còn thiếu.

Ví dụ:

Security test chưa hoàn thành
E2E critical flow chưa test
Regression chưa chạy
Production bug chưa fix
BLOCKED

Chỉ khi:

Actual environment blocker

làm không thể thực hiện test bắt buộc.

Không dùng:

Backend của developer khác chưa xong

làm BLOCKED.

69. PHASE 5 → FINAL AUDIT GATE

Trước khi chuyển Final Audit phải xác nhận:

[ ] Phase 1 tested
[ ] Phase 2A tested
[ ] Phase 2B tested
[ ] Phase 2C tested
[ ] Phase 3A tested
[ ] Phase 3B tested
[ ] Phase 3C tested
[ ] Phase 4A tested
[ ] Phase 4B tested
[ ] Phase 4C tested

[ ] Authentication tested
[ ] Authorization tested
[ ] Object-level security tested
[ ] IDOR tested
[ ] Validation tested
[ ] State transition tested
[ ] Error handling tested
[ ] Database persistence tested
[ ] Concurrency tested where applicable
[ ] Critical E2E tested
[ ] Admin regression
[ ] Moderator regression
[ ] Vendor regression
[ ] Customer regression

[ ] Build verified
[ ] Existing tests reviewed
[ ] New tests reviewed
[ ] Test failures classified
[ ] Integration gaps documented
[ ] Git diff reviewed
[ ] Files changed reported
[ ] Known issues reported
70. MASTER RESULT CLASSIFICATION

Mỗi issue/test result phải có format:

Module:
Test:
Expected:
Actual:
Classification:
Implementation Status:
Integration Status:
Severity:
Root Cause:
Fix:
Retest:
Regression:

Ví dụ:

Module:
AuditLog

Test:
Moderator approve Product → AuditLog

Expected:
Audit event created

Actual:
AuditLog backend chưa tồn tại

Classification:
NOT-INTEGRATED

Implementation Status:
IMPLEMENTED

Integration Status:
INTEGRATION-READY

Severity:
N/A

Root Cause:
External implementation unavailable

Fix:
No production fake

Retest:
Contract test PASS

Regression:
Moderator moderation PASS
71. FINAL REPORT

Sau khi hoàn thành phải tạo report:

# PHASE 5 RESULT

## 1. Git Baseline

Branch:
HEAD:
Working tree:
Uncommitted changes:

## 2. Environment

Application:
Database:
Profile:
Test profile:
External integrations:

## 3. Test Coverage

Unit:
Controller:
Service:
Repository:
Integration:
API:
Security:
Functional:
E2E:
Regression:

## 4. Phase 1 Result

...

## 5. Phase 2A Result

...

## 6. Phase 2B Result

...

## 7. Phase 2C Result

...

## 8. Phase 3A Result

...

## 9. Phase 3B Result

...

## 10. Phase 3C Result

...

## 11. Phase 4A Result

...

## 12. Phase 4B Result

...

## 13. Phase 4C Result

...

## 14. PASS

...

## 15. FAIL

...

## 16. PARTIAL

...

## 17. MISSING

...

## 18. WRONG

...

## 19. BUG FIXED

...

## 20. SECURITY RESULT

...

## 21. DATABASE RESULT

...

## 22. E2E RESULT

...

## 23. REGRESSION RESULT

Admin:
Moderator:
Vendor:
Customer:

## 24. External Integration

Contract:
Integration Seam:
Current Implementation:
Integration Status:
...

## 25. NOT-INTEGRATED

...

## 26. INTEGRATION-READY

...

## 27. KNOWN ISSUES

...

## 28. BLOCKED

...

## 29. Test Result

Total:
Passed:
Failed:
Skipped:
Errors:

## 30. Build

mvn test:
mvn clean test:
mvn clean package:

## 31. Files Changed

Created:
Modified:
Deleted:

## 32. Git Review

...

## 33. Final Status

READY FOR FINAL AUDIT
/
READY WITH INTEGRATION-READY ITEMS
/
PARTIAL
/
BLOCKED
72. KHÔNG ĐƯỢC FAKE TEST RESULT

Tuyệt đối không:

PASS

khi chưa chạy.

Không:

Mock success
→ report PASS

Không:

HTTP 200
→ report PASS

Không:

UI hiển thị
→ report PASS

Không:

Maven PASS
→ report functional PASS

Không:

External backend missing
→ fake response
→ E2E PASS
73. KHÔNG ĐƯỢC BYPASS SECURITY

Không:

disable security

để test.

Không:

permitAll

chỉ để test API.

Không:

hard-code ADMIN

chỉ để test.

Không bỏ authorization để làm E2E chạy được.

Security phải được test trong trạng thái thật của application.

74. KHÔNG ĐƯỢC DÙNG TEST ĐỂ CHE IMPLEMENTATION THIẾU

Nếu implementation thiếu:

MISSING

Nếu sai:

WRONG

Nếu lỗi:

FAIL

Nếu external integration chưa tồn tại:

INTEGRATION-READY
+
NOT-INTEGRATED

Không được biến tất cả thành:

PASS

bằng cách:

mock production;
hard-code response;
disable assertion;
skip test;
comment test;
return default data.
75. PHASE 5 EXECUTION PROCEDURE

Thực hiện theo thứ tự:

1. Inspect Git
        ↓
2. Read Source of Truth
        ↓
3. Inspect Existing Tests
        ↓
4. Inspect Actual Implementation
        ↓
5. Identify Contracts / Integration Seams
        ↓
6. Prepare Test Environment
        ↓
7. Prepare Test Data
        ↓
8. Run Existing Tests
        ↓
9. Run Phase 1 Tests
        ↓
10. Run Phase 2 Tests
        ↓
11. Run Phase 3 Tests
        ↓
12. Run Phase 4 Tests
        ↓
13. Security Testing
        ↓
14. Object-level Security
        ↓
15. State Transition
        ↓
16. Validation
        ↓
17. Error Handling
        ↓
18. Database Verification
        ↓
19. Critical E2E
        ↓
20. Regression
        ↓
21. Identify Defects
        ↓
22. Fix Confirmed Defects
        ↓
23. Retest
        ↓
24. Regression Again
        ↓
25. Build
        ↓
26. Git Diff Review
        ↓
27. Final Report
        ↓
28. Final Audit Handoff
76. DEFINITION OF DONE — PHASE 5

Phase 5 được coi là hoàn thành khi:

Functional
[ ] Phase 1 verified
[ ] Phase 2A verified
[ ] Phase 2B verified
[ ] Phase 2C verified
[ ] Phase 3A verified
[ ] Phase 3B verified
[ ] Phase 3C verified
[ ] Phase 4A verified
[ ] Phase 4B verified
[ ] Phase 4C verified
Security
[ ] Authentication
[ ] Authorization
[ ] Object-level security
[ ] IDOR
[ ] Direct API security
[ ] State bypass
Data
[ ] Database persistence
[ ] State
[ ] Relations
[ ] Timestamps
[ ] References
Quality
[ ] Validation
[ ] Error handling
[ ] Concurrency where applicable
[ ] Regression
[ ] E2E
Integration
[ ] Contracts identified
[ ] Integration seams identified
[ ] External implementations classified
[ ] INTEGRATION-READY items documented
[ ] NOT-INTEGRATED items documented
Build
[ ] mvn test
[ ] mvn clean test
[ ] mvn clean package
Safety
[ ] No fake production behavior
[ ] No fake production data
[ ] No security bypass
[ ] No disabled failing tests
[ ] No accidental overwrite
[ ] Git diff reviewed
77. PHASE 5 RANH GIỚI

Phase 5:

TEST
 ↓
FIND BUG
 ↓
CLASSIFY
 ↓
FIX CONFIRMED BUG
 ↓
RETEST
 ↓
REGRESSION
 ↓
BUILD
 ↓
REPORT

Không phải:

TEST
 ↓
PHÁT HIỆN BACKEND CHƯA CÓ
 ↓
VIẾT LẠI BACKEND NGƯỜI KHÁC

Không phải:

TEST
 ↓
THẤY FEATURE THIẾU
 ↓
TỰ THÊM FEATURE MỚI

Không phải:

TEST
 ↓
MISSING DEPENDENCY
 ↓
BLOCKED

Thay vào đó:

External implementation unavailable
        ↓
Identify contract
        ↓
Test user-owned scope
        ↓
Use test double only in test
        ↓
Mark INTEGRATION-READY
        ↓
Mark external E2E NOT-INTEGRATED
        ↓
Continue other tests
78. FINAL PRINCIPLE

Phase 5 phải đảm bảo:

TEST REAL IMPLEMENTATION
        +
VERIFY REAL BEHAVIOR
        +
VERIFY SECURITY
        +
VERIFY DATABASE
        +
VERIFY STATE
        +
VERIFY INTEGRATION
        +
VERIFY REGRESSION

Không:

FAKE TEST

Không:

FAKE DATA

Không:

FAKE SUCCESS

Không:

BYPASS SECURITY

Không:

SKIP UNEXPLAINED FAILURE

Không:

BLOCK PHASE
chỉ vì external implementation chưa tồn tại

Nguyên tắc cuối cùng:

“Tôi implement được phần của tôi bằng contract/interface hiện tại; backend của Tuấn là integration point, không phải prerequisite để bắt đầu hoặc hoàn thành implementation.”

Và đối với Phase 5:

“Tôi test được phần của tôi bằng implementation và contract hiện tại; external backend là integration point. Nếu external implementation chưa tồn tại, tôi không fake production behavior, mà test độc lập phần thuộc scope, ghi nhận INTEGRATION-READY / NOT-INTEGRATED, và chỉ chạy E2E integration thật khi implementation thực tế đã có.”

79. PHASE 5 OUTPUT CUỐI CÙNG

Sau Phase 5 phải có đủ:

1. Test execution result
2. Master test matrix
3. Security test result
4. API test result
5. Database verification
6. State transition result
7. E2E result
8. Admin regression
9. Moderator regression
10. Vendor regression
11. Customer regression
12. Bug list
13. Bug fixes
14. Missing list
15. Wrong behavior list
16. Partial list
17. Integration-ready list
18. Not-integrated list
19. Real blockers
20. Build result
21. Git diff review
22. Files changed
23. Known issues
24. Final status
Final status chỉ được chọn:
READY FOR FINAL AUDIT

hoặc:

READY WITH INTEGRATION-READY ITEMS

hoặc:

PARTIAL

hoặc:

BLOCKED

Trong đó:

BLOCKED

chỉ khi có real hard blocker của environment/infrastructure, không phải vì một developer khác chưa hoàn thành implementation.

PHASE 5 = FULL FUNCTIONAL TESTING + REGRESSION

Phase 1 → Phase 4C
        ↓
REAL TEST
        ↓
DEFECT FIX
        ↓
SECURITY
        ↓
DATABASE
        ↓
E2E
        ↓
REGRESSION
        ↓
BUILD
        ↓
FINAL AUDIT

Đây là phase kiểm chứng toàn bộ hệ thống, không phải phase để bù implementation còn thiếu bằng cách fake, bypass hoặc rewrite module của người khác.