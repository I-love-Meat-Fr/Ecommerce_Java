PHASE 3A — ADMIN ESCALATION + ENFORCEMENT + AUDIT WRITER

Implementation instruction: Read this Phase 3A file as the single source of truth.

Core independence rule:

“Tôi implement được phần của tôi bằng contract/interface hiện tại; backend của Tuấn là integration point, không phải prerequisite để bắt đầu hoặc hoàn thành implementation.”

Phase 3A phải có khả năng được triển khai độc lập ở phần thuộc scope của phase. Không chờ developer khác, branch khác, commit khác hoặc backend implementation khác để bắt đầu/hoàn thành phần implementation của mình.

Nếu backend bên ngoài chưa tồn tại:

Xác định contract/interface từ architecture hiện tại.
Xác định integration seam.
Implement phần thuộc Phase 3A.
Test độc lập bằng test double/mock chỉ trong test.
Đánh dấu INTEGRATION-READY.
Không tạo fake production data/behavior.
Khi backend thật tồn tại, integrate tại seam đã xác định.

INTEGRATION-READY không có nghĩa là INTEGRATED.

Không được dùng việc backend của người khác chưa hoàn thành làm lý do để không implement phần thuộc Phase 3A.

1. MỤC TIÊU

Implement hoàn chỉnh phần Admin Escalation + Enforcement + Audit Writer Integration:

MODERATOR
    ↓
Escalate Severe Case
    ↓
ESCALATION CONTRACT
    ↓
ADMIN ESCALATION QUEUE
    ↓
ADMIN ESCALATION DETAIL
    ↓
Review Context
    ├── Moderator Decision
    ├── Evidence
    ├── Auto Moderation Result
    ├── Vendor History
    └── Severity / Policy / Violation
    ↓
ADMIN ENFORCEMENT
    ├── Suspend Product
    ├── Suspend Shop
    └── Ban Vendor
    ↓
STATE TRANSITION
    ↓
AUDITLOG WRITER CONTRACT

Phase 3A tập trung vào:

Admin Escalation Queue
Escalation Detail
Moderator Decision
Evidence
Auto Moderation Result
Vendor History nếu backend hỗ trợ
Admin Enforcement
Suspend Product
Suspend Shop
Ban Vendor
Mandatory Enforcement Reason
Confirmation Modal
State Transition Validation
AuditLog Writer integration
Security
Integration Contract
Integration Test
Database Verification
Regression Test
2. NGUYÊN TẮC IMPLEMENTATION ĐỘC LẬP
2.1. Backend của Tuấn là integration point

Không sử dụng cách phân loại:

DEPENDENCY – Anh Tuấn
BLOCKED

chỉ vì backend của Tuấn chưa được implement.

Thay bằng:

External Integration:
Escalation Backend

Contract:
Escalation read/action contract

Integration Seam:
Admin Escalation Gateway / Service / API client hiện tại

Current External Implementation:
MISSING / PARTIAL / AVAILABLE

Implementation Status:
IMPLEMENTED / PARTIAL

Integration Status:
INTEGRATION-READY / INTEGRATED

Ví dụ:

Escalation Backend:
MISSING

Admin Escalation UI:
IMPLEMENTED

Contract:
DEFINED

Integration Seam:
READY

Production Fake:
NONE

Test Double:
USED ONLY IN TEST

Integration Status:
INTEGRATION-READY

Không tạo escalation giả trong production để làm UI hoạt động.

2.2. Không duplicate backend domain

Nếu repository đã có:

Escalation
ReportCase
ModerationResult
Product
Review
Shop
User
Vendor
AuditLog

thì Phase 3A phải reuse.

Không tạo:

AdminEscalationEntity
AdminReportCaseEntity
AdminAuditLogEntity

chỉ để phục vụ UI.

Nếu đã có service tương đương:

EscalationService
ModerationService
AuditLogService
EnforcementService

thì reuse service/contract hiện tại.

Chỉ tạo adapter/interface integration nếu architecture hiện tại chưa có integration seam phù hợp và việc tạo seam thuộc scope Phase 3A.

2.3. Test double không được đi vào production

Được phép:

Mock
Stub
Fake
Test Fixture
Test Double

trong:

Unit Test
Integration Test
Contract Test

Không được dùng test double để giả lập production:

Fake Escalation records
Fake AuditLog
Fake Product status
Fake Vendor history
Fake moderation result
3. KHÔNG TRIỂN KHAI TRONG PHASE 3A

Không làm:

KYC Monitoring
KYC Detail
KYC Provider

Violation Management UI
Violation Query

AuditLog List
AuditLog Detail UI
AuditLog Filter

Dashboard Core
WEB Voucher Hardening
Banner Hardening

Payment
Commission
Settlement
Payout
Refund
Finance

Order Core

Customer UI
Vendor UI

Các phần trên thuộc phase khác.

4. FLOW CHÍNH
4.1. Escalation Flow
Moderator
    ↓
Product / Review / ReportCase
    ↓
Severe Case
    ↓
ESCALATE
    ↓
Escalation Contract
    ↓
Admin Escalation Queue
    ↓
Admin Escalation Detail
    ↓
Review:
    - Moderator Decision
    - Reason
    - Evidence
    - Auto Moderation Result
    - Vendor History
    - Severity
    - Policy / Violation
    ↓
Admin Enforcement

Phase 3A consume escalation contract.

Phase 3A không cần tự implement lại:

Product Moderation
Review Moderation
ReportCase creation
Auto Moderation Engine
5. ENFORCEMENT FLOW
Admin Escalation Detail
        ↓
Choose Action
        ↓
┌──────────────┬──────────────┬──────────────┐
↓              ↓              ↓
Suspend        Suspend        Ban
Product        Shop           Vendor
↓              ↓              ↓
Reason         Reason         Reason
↓              ↓              ↓
Confirm        Confirm        Confirm
↓              ↓              ↓
Backend Contract
        ↓
State Transition
        ↓
AuditLog Writer

Frontend không tự quyết định state transition.

6. AUDIT FLOW
Admin Action
    ↓
Enforcement Contract
    ↓
Backend validates
    ↓
State Transition
    ↓
AuditLog Writer
    ↓
AuditLog persisted

Phase 3A implement/integrate AuditLog Writer contract.

Phase 3A không implement AuditLog Query UI.

AuditLog Query:

/admin/audit

thuộc Phase 3C.

7. RANH GIỚI ADMIN VS MODERATOR

Bắt buộc giữ:

AUTO MODERATION
= Máy kiểm tra / gắn flag

MODERATOR
= Normal Moderation + Escalation

ADMIN
= Severe Escalation + Enforcement

Moderator:

Product
Review
ReportCase
Normal moderation
Normal violation handling
Escalate severe case

Admin:

Escalation
Severe enforcement
Suspend Product
Suspend Shop
Ban Vendor

Không chuyển toàn bộ Product/Review moderation sang Admin.

Admin không cần approve từng Product hoặc Review thông thường.

8. SOURCE OF TRUTH

Trước khi code phải đọc:

Source code hiện tại
Gate 0
Phase 1
Phase 2A
Phase 2B
Phase 2C nếu đã có
File:
BỘ LUẬT & CHÍNH SÁCH CỦA SÀN MARKETPLACE

Đặc biệt kiểm tra:

Seller Policy
Prohibited Product Policy
Product Content Policy
Counterfeit/IP Policy
Vendor Violation Policy
Complaint/Escalation Policy
KYC Policy
Audit Policy
Role Policy

Không tự tạo business rule mới.

Nếu:

Policy != Backend behavior

ghi:

MISMATCH

Nếu contract/backend chưa tồn tại:

MISSING

và tạo integration seam nếu thuộc scope Phase 3A.

Không fake để ghi INTEGRATED.

9. GIT BASELINE

Trước khi code:

git status
git branch
git log --oneline --decorate -10

Ghi:

Current branch:
Current HEAD:
Working tree:
Uncommitted changes:
Cấm

Không:

reset
stash
merge
push
checkout làm mất thay đổi
overwrite code người khác
xóa uncommitted changes

Nếu file đang có thay đổi của người khác:

DO NOT OVERWRITE

Phase 3A chỉ chỉnh phần cần thiết.

10. KIỂM TRA PHASE 2

Kiểm tra:

Product Moderation
Review Moderation
ReportCase
Moderator History
Escalate

Đặc biệt:

Moderator
    ↓
Escalate
    ↓
Escalation Contract

Nếu Phase 2 đã có contract nhưng backend implementation chưa có:

Phase 3A:
CONTINUE

Integration:
INTEGRATION-READY

Không:

BLOCKED

trừ khi có hard environmental blocker thực sự.

Nếu Phase 2 chỉ có button UI và chưa có contract/seam, Phase 3A phải:

Xác định contract cần consume.
Tạo integration seam thuộc Phase 3A nếu phù hợp.
Implement Admin side.
Mark INTEGRATION-READY.
Không tạo escalation giả.
11. BACKEND / CONTRACT AUDIT

Tìm implementation hoặc contract hiện tại của:

Escalation
ReportCase
ModerationResult
Product
Review
Shop
User
Vendor
AuditLog
Enforcement

Kiểm tra:

Entity
Repository
Service
Controller
DTO
Enum
Status
Database
API

Tìm nếu tồn tại:

EscalationService
EscalationController
ReportCaseService
ModerationService
ProductService
ShopService
UserService
VendorService
EnforcementService
AuditLogService
AuditLogWriter

Phân loại:

[GREEN] Existing implementation/contract
[YELLOW] Partial
[RED] Missing
[BLUE] Integration seam available but external implementation absent

Không đánh dấu complete chỉ vì Entity tồn tại.

12. CONTRACT-FIRST CLASSIFICATION

Mỗi external capability phải được phân biệt:

IMPLEMENTED

Phần Phase 3A thuộc quyền implement đã hoàn thành và test được độc lập.

INTEGRATION-READY

Phần Phase 3A đã hoàn thành contract/seam/test nhưng implementation bên ngoài chưa kết nối.

INTEGRATED

Backend implementation thật đã được kết nối và integration/E2E đã chạy thành công.

PARTIAL

Phần thuộc Phase 3A chưa hoàn thành.

MISSING

Required capability chưa tồn tại và chưa có implementation thuộc phase.

WRONG

Behavior hiện tại vi phạm contract/policy.

OUT OF SCOPE

Không thuộc Phase 3A.

BLOCKED

Chỉ dùng khi có hard blocker thực sự như:

Repository không build được do baseline corruption
Environment không chạy được
Database không thể khởi động
Required infrastructure unavailable

Không dùng:

Tuấn chưa code
Quốc Anh chưa code
Phase khác chưa merge
Branch khác chưa merge

làm BLOCKED.

13. OWNERSHIP VÀ INTEGRATION BOUNDARY
Phần thuộc Phase 3A

Hoàn implement:

Admin Escalation UI
Escalation Queue
Escalation Detail
Moderator Decision display
Evidence display
Auto Moderation Result display
Vendor History display
Enforcement UI
Confirmation Modal
Admin route integration
Contract adapter/integration layer
Error handling UI
Phase 3A tests
Regression tests
External integration points

Backend có thể thuộc developer khác:

Escalation Backend
ReportCase Backend
Auto Moderation Backend
Enforcement Backend
AuditLog Writer Backend
Security Core

Nhưng đây là:

INTEGRATION POINT

không phải prerequisite để bắt đầu implementation Phase 3A.

14. ADMIN ESCALATION QUEUE

Route:

/admin/escalations

Nếu project đã có route tương đương:

Ưu tiên route hiện tại.

Queue sử dụng dữ liệu từ Escalation contract.

Các field nếu contract hỗ trợ:

Escalation ID
Case ID
Resource Type
Resource ID
Product / Review
Vendor
Shop
Severity
Policy Code
Violation Code
Moderator
Status
Created At

Optional:

Priority
Escalation Reason

Không tạo backend field chỉ để UI có dữ liệu.

Nếu field chưa được backend hỗ trợ:

Status:
NOT PROVIDED

hoặc dùng trạng thái UI tương ứng theo architecture hiện tại.

Không tự suy diễn.

15. ESCALATION FILTER

Filter theo field contract thực sự hỗ trợ:

Status
Severity
Resource Type
Product
Review
Vendor
Shop
Violation Code
Policy Code
Moderator
Date

Nếu backend hỗ trợ pagination:

MUST use backend pagination

Không:

GET ALL
→ frontend filter

nếu dataset có thể lớn và backend đã có pagination/filter contract.

16. ESCALATION DETAIL

Route:

/admin/escalations/{id}
Escalation
Escalation ID
Case ID
Status
Severity
Created At
Updated At
Resource
Resource Type
Resource ID
Product / Review
Vendor
Shop
Moderator
Moderator
Moderator Action
Moderator Decision
Moderator Reason
Decision Time
Escalation
Escalation Reason
Policy Code
Violation Code
Evidence
Product Images
Review Content
Review Images
Report Evidence
Moderation Evidence
Violation Evidence

Chỉ hiển thị field backend cung cấp.

Auto Moderation
Status
Flags
Reason
Severity
Score
Rule Code
Checked At

Chỉ field backend trả về.

Vendor History

Nếu contract hỗ trợ:

Previous Reports
Previous Violations
Rejected Products
Hidden Reviews
Previous Escalations
Previous Suspensions

Không tự suy diễn.

17. VIEW MODERATOR DECISION

Admin chỉ xem Moderator Decision.

Ví dụ:

Moderator:
Nguyen A

Action:
REJECT_PRODUCT

Reason:
Sản phẩm có dấu hiệu vi phạm chính sách.

Created At:
...

Admin không được overwrite Moderator Decision từ màn hình escalation.

Nếu Admin thực hiện enforcement khác:

Moderator Decision
        ≠
Admin Enforcement

thì đây là action mới và phải đi qua enforcement contract + AuditLog.

18. VIEW AUTO MODERATION RESULT

Nếu ModerationResult contract tồn tại:

Status
Flags
Reason
Severity
Score
Rule Code
Checked At

Không:

fake score
fake flag
fake confidence
UI tự tính moderation result

Không rerun Auto Moderation từ UI nếu backend contract/policy không quy định.

19. VIEW EVIDENCE

Phase 3A chỉ:

READ
DISPLAY

Evidence có thể:

Product image
Review content
Review image
Report evidence
Moderation evidence
Violation evidence

Không:

delete
overwrite
edit
change evidence metadata

Nếu evidence backend chưa có:

Evidence:
NOT PROVIDED
Integration Status:
INTEGRATION-READY

Không fake evidence.

20. VENDOR HISTORY

Nếu backend contract hỗ trợ:

Vendor:
shop01

Previous Reports:
5

Previous Violations:
2

Rejected Products:
3

Previous Escalations:
1

Previous Suspensions:
0

Chỉ hiển thị dữ liệu backend.

Không suy luận:

3 rejected products
=
vendor fraud

Severity/history không tự động quyết định enforcement.

21. ADMIN ENFORCEMENT CONTRACT

Phase 3A chỉ consume/implement integration cho các action được policy/backend contract hỗ trợ:

SUSPEND_PRODUCT
SUSPEND_SHOP
BAN_VENDOR

Nếu backend dùng enum khác:

Reuse enum/backend hiện tại.

Không đổi enum chỉ để khớp UI.

Không tự thêm:

FUNDS_FROZEN
PAYMENT_BLOCKED
PAYOUT_BLOCKED

vì Finance/Settlement không thuộc Phase 3A.

22. SUSPEND PRODUCT

Flow:

Escalation Detail
    ↓
Suspend Product
    ↓
Reason
    ↓
Confirm
    ↓
Enforcement Contract
    ↓
Backend State Transition
    ↓
AuditLog Writer

Frontend phải:

validate required reason
call contract
handle response
refresh state
display result

Không update database trực tiếp.

Backend contract phải kiểm tra:

Authentication
Authorization
Product existence
Current status
Allowed transition
Actor
Reason

Nếu:

409

UI hiển thị:

Resource đã được xử lý hoặc trạng thái đã thay đổi.
23. SUSPEND SHOP

Flow:

Escalation
    ↓
Suspend Shop
    ↓
Reason
    ↓
Confirm
    ↓
Enforcement Contract
    ↓
Shop State Transition
    ↓
AuditLog Writer

Không nhầm:

KYC_REJECTED

với:

SHOP_SUSPENDED

KYC không thuộc Phase 3A.

24. BAN VENDOR

Flow:

Escalation
    ↓
Ban Vendor
    ↓
Mandatory Reason
    ↓
Confirm
    ↓
Enforcement Contract
    ↓
Vendor Enforcement State
    ↓
AuditLog Writer

Không cho Moderator gọi action này.

Không tự áp:

CRITICAL
=
AUTO BAN

Severity chỉ là context nếu policy/backend cho phép.

Action hợp lệ phải do backend enforcement contract quyết định.

25. MANDATORY REASON

Với action yêu cầu reason:

Suspend Product
Suspend Shop
Ban Vendor

Frontend phải reject:

""
"   "

Backend cũng phải validate.

Frontend validation không thay thế backend validation.

26. CONFIRMATION MODAL

Dangerous action bắt buộc confirmation:

Suspend Product
Suspend Shop
Ban Vendor

Ví dụ:

Bạn có chắc chắn muốn Suspend Shop?

Reason:

[________________________]

[Cancel] [Confirm]

Confirm phải:

disable khi submit
loading state
prevent double click
handle backend error

Click button lần đầu không được lập tức thực hiện enforcement.

27. STATE TRANSITION

Frontend không được tự quyết định:

ACTIVE
→
SUSPENDED

hay:

ACTIVE
→
BANNED

Frontend chỉ request action.

Backend là source of truth.

Ví dụ:

POST /.../suspend-product

Request
    ↓
Authorization
    ↓
Resource validation
    ↓
State validation
    ↓
Transition
    ↓
Audit
    ↓
Response

Nếu transition không hợp lệ:

409 Conflict

UI phải refresh/reload state theo architecture hiện tại.

28. DOUBLE ACTION / CONCURRENCY

Test:

Admin A mở Escalation
Admin B mở cùng Escalation

Admin A → Suspend Shop
Admin B → Ban Vendor

Backend phải validate state tại thời điểm action.

Không:

silent overwrite
double action
invalid transition

Nếu:

409 Conflict

UI phải thông báo state đã thay đổi và yêu cầu refresh.

29. AUDITLOG WRITER INTEGRATION

Phase 3A không implement AuditLog Query.

Phase 3A phải bảo đảm enforcement đi qua AuditLog Writer contract.

Tối thiểu:

PRODUCT_SUSPENDED
SHOP_SUSPENDED
VENDOR_BANNED

Nếu backend đã có action tương ứng thì reuse.

Nếu backend dùng tên khác:

Reuse action enum hiện tại.

Không tự tạo enum mới chỉ để UI khớp.

30. AUDITLOG CONTRACT

AuditLog model/contract có thể gồm:

actorId
actorEmail
role
action
resourceType
resourceId
before
after
IP
reason
severity
createdAt

Chỉ sử dụng field backend thực tế hỗ trợ.

Nếu field không tồn tại:

MISSING

hoặc:

INTEGRATION-READY

tùy phần implementation.

Không fake:

IP
Actor
Before
After
Timestamp
Reason
31. AUDITLOG TRANSACTION

Mục tiêu:

Admin Action
    ↓
State Change
    +
AuditLog

Nếu backend transaction hỗ trợ:

State transition và AuditLog phải được thực hiện trong transaction boundary phù hợp.

Nếu transaction boundary chưa tồn tại:

Integration Status:
INTEGRATION-READY

External Contract:
Audit transaction boundary required

Không dùng fake AuditLog ở production.

Không báo thành công ở UI nếu backend contract báo enforcement thành công nhưng audit contract thực tế thất bại theo policy transaction semantics.

32. AUDITLOG BEFORE / AFTER

Nếu backend hỗ trợ:

Before:
ACTIVE

After:
SUSPENDED

Before/After phải lấy từ backend state transition.

Không tạo:

before
after

ở frontend bằng cách đoán trạng thái.

33. AUDITLOG PII

AuditLog có thể chứa:

User
Email
IP
Resource

Không expose PII không cần thiết.

Nếu before/after chứa:

Citizen ID
Tax Code
Bank Account

phải tuân theo backend/security policy masking.

Phase 3A không mở plaintext PII chỉ vì cần hiển thị context.

34. ADMIN SECURITY

Route:

/admin/escalations/**

phải theo Admin Security architecture hiện tại.

Mặc định Phase 3A yêu cầu:

ADMIN
→ ALLOW

MODERATOR
→ DENY

VENDOR
→ DENY

CUSTOMER
→ DENY

ANONYMOUS
→ DENY

Nếu architecture hiện tại có rule khác:

MISMATCH

và phải xử lý theo Security Core hiện tại, không tự phá architecture.

35. OBJECT-LEVEL SECURITY

Không chỉ kiểm tra URL.

Phải kiểm tra:

Escalation ID
Product ID
Shop ID
Vendor ID

Backend phải validate:

Authentication
Authorization
Resource existence
Current state
Allowed action
Actor

Không bypass bằng cách đổi ID trong request.

36. MODERATOR SECURITY REGRESSION

Phải verify:

MODERATOR
    ↓
/moderator/**
    ↓
ALLOW

nhưng:

MODERATOR
    ↓
/admin/escalations
    ↓
DENY

và:

MODERATOR
    ↓
Suspend Product
    ↓
DENY
MODERATOR
    ↓
Suspend Shop
    ↓
DENY
MODERATOR
    ↓
Ban Vendor
    ↓
DENY

Nếu Security Core chưa implement:

Security Integration:
INTEGRATION-READY

Không biến thành blocker của Admin UI implementation.

37. LOCKED ACCOUNT

Verify theo Security architecture:

Locked Admin
    ↓
Admin protected routes
    ↓
DENY
Locked Moderator
    ↓
Moderator protected routes
    ↓
DENY

Không tự sửa Security Core nếu không thuộc Phase 3A.

38. API / CONTROLLER CONTRACT

Ưu tiên API hiện tại.

Không duplicate endpoint.

Mỗi integration API phải xác định:

HTTP Method
Path
Request
Response
Authentication
Authorization
Error
State Transition

Ví dụ minh họa:

GET /admin/escalations
GET /admin/escalations/{id}
POST /admin/escalations/{id}/suspend-product
POST /admin/escalations/{id}/suspend-shop
POST /admin/escalations/{id}/ban-vendor

Các path trên chỉ là contract candidate.

Nếu backend hiện tại có path khác:

Dùng path hiện tại.

Không ép backend đổi API chỉ để khớp Phase 3A UI.

39. INTEGRATION SEAM

Nếu architecture đã có gateway/service/client:

Reuse existing seam

Nếu chưa có seam và việc tạo seam thuộc Phase 3A:

Admin UI
    ↓
Existing Service / Gateway
    ↓
Escalation Contract
    ↓
External Backend

Ví dụ conceptual:

AdminEscalationGateway

Tên thật phải theo architecture project.

Không tạo class chỉ vì tên này.

Gateway/adapter có trách nhiệm:

getEscalations()
getEscalation(id)
suspendProduct(...)
suspendShop(...)
banVendor(...)

nếu các operation đó phù hợp với contract thực tế.

40. EXTERNAL BACKEND ABSENT

Nếu Tuấn chưa implement:

Escalation Backend:
MISSING

Phase 3A vẫn implement:

Queue structure
Detail structure
Filter contract
Pagination contract
Action request contract
Error mapping
Security integration
Confirmation modal
State-aware UI
Tests
Integration seam

Test bằng:

Mock / Stub / Test Double

chỉ trong test.

Production:

NO FAKE IMPLEMENTATION

Final status:

Implementation:
IMPLEMENTED

Integration:
INTEGRATION-READY

Không ghi:

INTEGRATED
41. ERROR HANDLING

Phải map:

400
401
403
404
409
500
400

Validation error.

401

Authentication.

403
Bạn không có quyền thực hiện thao tác này.
404
Không tìm thấy dữ liệu.
409
Dữ liệu đã được xử lý hoặc trạng thái đã thay đổi.
Vui lòng tải lại trang.
500

Message an toàn.

Không hiển thị:

Stack Trace
NullPointerException
Internal Java Exception
SQL error
42. UI REUSE

Reuse Admin UI hiện tại:

Sidebar
Header
Table
Card
Filter
Pagination
Modal
Status Badge
Button
Typography
Spacing
Alert
Empty State

Không tạo Design System mới.

Không copy toàn bộ Admin layout thành hệ thống riêng.

43. LOADING / EMPTY / ERROR
Loading
Đang tải dữ liệu...
Empty
Không có escalation.
Error
Không thể tải dữ liệu.
Vui lòng thử lại.

Không blank page.

Không fake production records.

44. FILTER / PAGINATION TEST

Phải test:

No data
1 record
Many records
Pagination
Filter
Multiple filters
Reset filter

Expected:

no crash
no duplicate
filter đúng
pagination đúng
reset đúng
empty state đúng
45. DATABASE VERIFICATION

Nếu backend implementation thật đã tích hợp, verify database.

Escalation
Status
Decision
Reason
Actor
Timestamp
Product
Status
UpdatedAt
Shop
Status
UpdatedAt
Vendor
Enforcement / Account Status
UpdatedAt
AuditLog
Actor
Role
Action
Resource
Reason
Before
After
CreatedAt

Nếu backend chưa tồn tại:

DB Integration:
NOT INTEGRATED

Không tạo fake DB result để đánh dấu PASS.

46. END-TO-END TEST 1 — PRODUCT ESCALATION

Khi backend thật đã tích hợp:

Moderator
    ↓
Product Moderation
    ↓
Severe Case
    ↓
Escalate
    ↓
Admin
    ↓
Escalation Queue
    ↓
Escalation Detail
    ↓
Moderator Decision
    ↓
Evidence
    ↓
Auto Moderation
    ↓
Suspend Product
    ↓
Reason
    ↓
Confirm
    ↓
Backend
    ↓
State Change
    ↓
AuditLog

Kiểm tra:

Escalation tồn tại
Detail đúng
Moderator Decision đúng
Evidence đúng
Product state thay đổi
Reason đúng
Actor đúng
AuditLog tồn tại

Nếu external backend chưa tồn tại:

Contract Test:
PASS

Actual E2E:
NOT INTEGRATED

Integration:
INTEGRATION-READY

Không gọi contract test là actual E2E.

47. END-TO-END TEST 2 — SHOP ESCALATION
Moderator
    ↓
Severe Case
    ↓
Escalate
    ↓
Admin
    ↓
Escalation Detail
    ↓
Suspend Shop
    ↓
Reason
    ↓
Confirm
    ↓
Backend
    ↓
AuditLog

Kiểm tra:

Shop status
Actor
Reason
Timestamp
AuditLog
48. END-TO-END TEST 3 — VENDOR BAN
Severe Case
    ↓
Escalation
    ↓
Admin
    ↓
Vendor Context
    ↓
Ban Vendor
    ↓
Mandatory Reason
    ↓
Confirm
    ↓
Backend
    ↓
AuditLog

Kiểm tra:

Vendor enforcement state
Actor
Reason
Timestamp
AuditLog

Đồng thời:

Moderator
    ↓
Ban Vendor
    ↓
DENY
49. AUDITLOG END-TO-END

Khi AuditLog Writer backend đã tích hợp:

Admin Suspend Product
    ↓
AuditLog
Admin Suspend Shop
    ↓
AuditLog
Admin Ban Vendor
    ↓
AuditLog

Kiểm tra:

Actor
Role
Action
Resource
Reason
Timestamp

Nếu backend hỗ trợ:

Before
After
IP
Severity

cũng kiểm tra.

50. SECURITY TEST MATRIX
Actor	Admin Escalation	Suspend Product	Suspend Shop	Ban Vendor
ADMIN	ALLOW	ALLOW	ALLOW	ALLOW
MODERATOR	DENY	DENY	DENY	DENY
VENDOR	DENY	DENY	DENY	DENY
CUSTOMER	DENY	DENY	DENY	DENY
ANONYMOUS	DENY	DENY	DENY	DENY
LOCKED ADMIN	DENY	DENY	DENY	DENY

Nếu implementation chưa tồn tại nhưng contract/security seam đã xác định:

INTEGRATION-READY

Nếu behavior hiện tại trái với policy/security:

WRONG

Không tự đánh dấu PASS.

51. REGRESSION ADMIN

Sau Phase 3A kiểm tra:

Admin Login
Dashboard
User Management
Lock / Unlock
Shop
Category
Product
Order
Review
WEB Voucher
Banner

Không để Phase 3A phá:

Admin Login
Admin Security
Existing CRUD
Existing Templates
Existing Sidebar
Existing Layout
52. FILE SCOPE

Chỉ thay đổi file cần thiết cho:

Admin Escalation
Admin Enforcement
Integration Contract / Adapter
AuditLog Writer integration
Admin Security integration
Tests

Nếu cần sửa ngoài scope:

File:
Reason:
Impact:
Why necessary:

Không yêu cầu developer khác hoàn thành file đó để tiếp tục phần của mình.

53. KHÔNG ĐƯỢC LÀM

Không:

rewrite Auto Moderation Engine
rewrite ReportCase Backend
rewrite Product Moderation Backend
rewrite Review Moderation Backend
rewrite KYC
write Violation Management
write AuditLog Query UI
write Finance
write Payment
write Settlement
write Payout
write Refund
write Order Core
write Customer UI
write Vendor UI
change unrelated business rules
fake escalation
fake evidence
fake moderation result
fake vendor history
fake AuditLog
hard delete resource
update DB directly from frontend
give Moderator Admin enforcement permission
54. MASTER TABLE PHASE 3A

Dùng bảng này để report contract-first, không dùng owner như blocker.

#	Nghiệp vụ	Contract	Current Backend	UI	Security	DB	Test	Implementation	Integration	Integration Seam
1	Escalation Queue	?	?	?	?	?	?	?	?	?
2	Escalation Detail	?	?	?	?	?	?	?	?	?
3	Moderator Decision	?	?	?	?	?	?	?	?	?
4	Evidence	?	?	?	?	?	?	?	?	?
5	Auto Moderation Result	?	?	?	?	?	?	?	?	?
6	Vendor History	?	?	?	?	?	?	?	?	?
7	Suspend Product	?	?	?	?	?	?	?	?	?
8	Suspend Shop	?	?	?	?	?	?	?	?	?
9	Ban Vendor	?	?	?	?	?	?	?	?	?
10	Enforcement Reason	?	?	?	?	?	?	?	?	?
11	Confirmation Modal	N/A	N/A	?	?	N/A	?	?	N/A	N/A
12	AuditLog Writer	?	?	N/A	?	?	?	?	?	?
13	Admin Security	?	?	?	?	N/A	?	?	?	?
14	Moderator Deny Admin	?	?	N/A	?	N/A	?	?	?	?
15	Concurrency	?	?	?	?	?	?	?	?	?
16	Admin Regression	N/A	N/A	?	?	N/A	?	?	?	?
Status semantics
IMPLEMENTATION:
IMPLEMENTED
PARTIAL
MISSING
WRONG
OUT OF SCOPE

INTEGRATION:
INTEGRATION-READY
INTEGRATED
NOT REQUIRED

Không dùng:

Owner = Dependency

để quyết định phase có thể bắt đầu hay không.

55. TEST CHECKLIST
A. Escalation
[ ] Admin Login
[ ] Escalation Queue
[ ] Filter
[ ] Pagination
[ ] Escalation Detail
[ ] Moderator Decision
[ ] Evidence
[ ] Auto Moderation Result
[ ] Vendor History
[ ] Suspend Product
[ ] Suspend Shop
[ ] Ban Vendor
[ ] Mandatory Reason
[ ] Confirmation Modal
[ ] 403 handling
[ ] 404 handling
[ ] 409 handling
[ ] Double Action
B. Enforcement
[ ] Product state transition
[ ] Shop state transition
[ ] Vendor enforcement state
[ ] Reason validation
[ ] Actor validation
[ ] Authorization
[ ] AuditLog
C. Audit Writer
[ ] Suspend Product → AuditLog
[ ] Suspend Shop → AuditLog
[ ] Ban Vendor → AuditLog
[ ] Actor
[ ] Role
[ ] Action
[ ] Resource
[ ] Reason
[ ] Timestamp
[ ] Before / After nếu backend hỗ trợ
[ ] Transaction consistency
D. Security
[ ] ADMIN → ALLOW
[ ] MODERATOR → DENY
[ ] VENDOR → DENY
[ ] CUSTOMER → DENY
[ ] ANONYMOUS → DENY
[ ] Locked Admin → DENY
E. Regression
[ ] Admin Login
[ ] Dashboard
[ ] User
[ ] Lock / Unlock
[ ] Shop
[ ] Category
[ ] Product
[ ] Order
[ ] Review
[ ] WEB Voucher
[ ] Banner
[ ] Moderator Security
56. BUILD / TEST

Chạy:

mvn clean package

Sau đó:

mvn test

Nếu project có integration test:

Run applicable integration tests.

Report:

BUILD: PASS / FAIL
UNIT TEST: PASS / FAIL
INTEGRATION TEST: PASS / FAIL / NOT INTEGRATED
MANUAL TEST: PASS / FAIL

Không ghi PASS nếu chưa chạy.

Nếu fail:

Command:
Error:
Root Cause:
File:
Function:

Nếu do external implementation chưa tồn tại:

External Integration:
NOT INTEGRATED

Implementation:
INTEGRATION-READY

không ghi BLOCKED.

57. PHÂN LOẠI KẾT QUẢ

Mỗi issue:

IMPLEMENTED
INTEGRATION-READY
INTEGRATED
PARTIAL
MISSING
WRONG
OUT OF SCOPE
BLOCKED

Trong đó:

IMPLEMENTED

Phần thuộc Phase 3A đã hoàn thành và test độc lập.

INTEGRATION-READY

Implementation hoàn chỉnh nhưng external backend chưa kết nối.

INTEGRATED

Backend thật đã kết nối và E2E thực tế đã pass.

PARTIAL

Implementation của chính Phase 3A còn thiếu.

MISSING

Capability bắt buộc chưa tồn tại.

WRONG

Existing behavior sai contract/policy.

BLOCKED

Chỉ hard environmental blocker.

Mỗi issue ghi:

File:
Function:
Current behavior:
Expected behavior:
Implementation Status:
Integration Status:
Integration Seam:
Root Cause:
Priority:

Không bắt buộc ghi tên người khác như một blocker.

58. BÁO CÁO FILE THAY ĐỔI

Bắt buộc:

Created:

Modified:

Deleted:

Với file quan trọng:

File:
Purpose:
Why changed:

Nếu không xóa:

Deleted:
NONE

Không ghi file không thực sự thay đổi.

59. BÁO CÁO SECURITY
ADMIN → /admin/escalations:
MODERATOR → /admin/escalations:
VENDOR → /admin/escalations:
CUSTOMER → /admin/escalations:
ANONYMOUS → /admin/escalations:

ADMIN → Suspend Product:
MODERATOR → Suspend Product:

ADMIN → Suspend Shop:
MODERATOR → Suspend Shop:

ADMIN → Ban Vendor:
MODERATOR → Ban Vendor:

Locked Admin:
Locked Moderator:

Kết quả:

PASS
FAIL
INTEGRATION-READY
60. BÁO CÁO EXTERNAL INTEGRATION

Không report:

DEPENDENCY – Anh Tuấn

theo nghĩa blocker.

Dùng:

Escalation Backend
Contract:
Current implementation:
Integration seam:
Status:
ReportCase
Contract:
Current implementation:
Integration seam:
Status:
Auto Moderation
Contract:
Current implementation:
Integration seam:
Status:
Enforcement Backend
Contract:
Current implementation:
Integration seam:
Status:
AuditLog Writer
Contract:
Current implementation:
Integration seam:
Status:

Ví dụ:

Escalation Backend:

Contract:
Defined from current architecture

Current implementation:
MISSING

Integration seam:
AdminEscalationGateway / existing equivalent

Phase 3A implementation:
IMPLEMENTED

Integration:
INTEGRATION-READY

Production fake:
NONE
61. BÁO CÁO SECURITY INTEGRATION
Quốc Anh / Security Core

Không coi đây là prerequisite để bắt đầu Admin UI.

Kiểm tra contract:

Authentication
Authorization
Method Security
Admin Route Security
Locked Account

Nếu security implementation chưa có:

Admin UI:
IMPLEMENTED

Security Integration:
INTEGRATION-READY

Khi Security Core tồn tại:

INTEGRATED

sau khi test thực tế.

62. PHASE 3A RESULT

Sau khi hoàn thành report đúng format:

PHASE 3A RESULT
1. Git Baseline
Current branch:

HEAD:

Working tree:

Uncommitted changes:
2. Phase 2 Integration Contract
Product Moderation:

Review Moderation:

ReportCase:

Escalation:

Moderator History:

Status:
3. Implementation
Implemented:

Partial:

Missing:

Wrong:
4. Integration
Integration-ready:

Integrated:

Not integrated:

Integration seams:
5. Escalation
Queue:

Detail:

Moderator Decision:

Evidence:

Auto Moderation Result:

Vendor History:

Status:
6. Enforcement
Suspend Product:

Suspend Shop:

Ban Vendor:

Reason:

Confirmation:

State Transition:

Status:
7. AuditLog Writer
Writer Contract:

Product Suspension:

Shop Suspension:

Vendor Ban:

Actor:

Reason:

Before / After:

Transaction:

Status:
8. Security
Admin:

Moderator:

Vendor:

Customer:

Anonymous:

Locked Admin:

Locked Moderator:
9. Database
Escalation:

Product:

Shop:

Vendor:

AuditLog:

Nếu chưa tích hợp backend:

NOT INTEGRATED

Không fake DB result.

10. E2E
Product Escalation:

Shop Escalation:

Vendor Ban:

AuditLog:

Phân biệt:

CONTRACT TEST
ACTUAL INTEGRATION TEST
11. Regression
Admin Login:

Dashboard:

User:

Lock / Unlock:

Shop:

Category:

Product:

Order:

Review:

WEB Voucher:

Banner:

Moderator Security:
12. Build
PASS / FAIL
13. Unit Test
PASS / FAIL
14. Integration Test
PASS / FAIL / NOT INTEGRATED
15. Manual Test
PASS / FAIL
16. Files Changed
Created:

Modified:

Deleted:
17. Known Issues
...
18. Hard Blockers

Chỉ ghi:

...

nếu thực sự có environmental blocker.

Không ghi backend của Tuấn chưa xong ở đây nếu Phase 3A đã INTEGRATION-READY.

63. PHASE 3B DEPENDENCY

Phase 3B chỉ nhận những contract/output thực sự cần:

Enforcement state transition
Escalation status
AuditLog Writer contract

Nếu Phase 3B chỉ cần contract:

Phase 3A:
IMPLEMENTED / INTEGRATION-READY

là đủ để Phase 3B bắt đầu phần implementation tương ứng.

Không yêu cầu toàn bộ external backend phải hoàn thành trước khi Phase 3B bắt đầu.

64. PHASE 3C DEPENDENCY

Phase 3C cần:

AuditLog data
AuditLog repository
AuditLog fields
AuditLog query contract

Phase 3A không làm:

AuditLog Query UI
AuditLog Filter UI
AuditLog Detail UI

Nhưng phải để lại contract/seam cần thiết nếu thuộc scope.

65. ĐIỀU KIỆN GHI IMPLEMENTED

Phần Phase 3A chỉ được ghi:

IMPLEMENTED

khi phần implementation thuộc phase đã được kiểm tra:

UI
Route
Contract
Validation
Error Handling
Security Integration
State-aware behavior
Tests

Nếu external backend chưa tồn tại:

IMPLEMENTED
+
INTEGRATION-READY

là trạng thái hợp lệ.

Không cần backend của Tuấn tồn tại để ghi IMPLEMENTED cho phần UI/adapter/test thuộc Phase 3A.

66. ĐIỀU KIỆN GHI INTEGRATED

Chỉ ghi:

INTEGRATED

khi:

Real backend API
    ↓
Real service
    ↓
Real database
    ↓
Real state transition
    ↓
Real AuditLog

đã được test thực tế.

Không ghi INTEGRATED khi chỉ:

Mock API
Stub
Fake data
Static JSON
Hardcoded object
Unit test
67. ĐIỀU KIỆN KẾT THÚC PHASE 3A
Escalation
[ ] Admin Escalation Queue
[ ] Escalation Detail
[ ] Moderator Decision
[ ] Evidence
[ ] Auto Moderation Result
[ ] Vendor History nếu backend hỗ trợ
[ ] Filter
[ ] Pagination nếu backend hỗ trợ
Enforcement
[ ] Suspend Product
[ ] Suspend Shop
[ ] Ban Vendor
[ ] Mandatory Reason
[ ] Confirmation Modal
[ ] Backend Authorization contract
[ ] State Transition contract
[ ] 409 Conflict handling
[ ] Double Action handling
Audit Writer
[ ] Enforcement → AuditLog contract
[ ] Actor
[ ] Role
[ ] Action
[ ] Resource
[ ] Reason
[ ] Timestamp
[ ] Before / After nếu backend hỗ trợ
Security
[ ] Admin allow
[ ] Moderator deny
[ ] Vendor deny
[ ] Customer deny
[ ] Anonymous deny
[ ] Locked Admin deny
Quality
[ ] Database verification khi backend integrated
[ ] Contract test
[ ] E2E test khi backend integrated
[ ] Regression test
[ ] Build
[ ] Unit test
[ ] Integration test
[ ] File changes report
[ ] Integration report
[ ] Known issues report
68. MỤC TIÊU CUỐI CÙNG

Phase 3A phải tạo được integration flow:

                  MODERATOR
                      │
                      │ Severe Case
                      ↓
               ┌───────────────┐
               │  ESCALATION   │
               │   CONTRACT    │
               └───────┬───────┘
                       ↓
                ADMIN QUEUE
                       ↓
                ADMIN DETAIL
                       ↓
       ┌───────────────┼────────────────┐
       ↓               ↓                ↓
   Evidence      Moderator Decision   Vendor History
       │               │                │
       └───────────────┼────────────────┘
                       ↓
                ADMIN ENFORCEMENT
                       │
             ┌─────────┼─────────┐
             ↓         ↓         ↓
         Suspend     Suspend     Ban
         Product      Shop      Vendor
             │         │         │
             └─────────┼─────────┘
                       ↓
                STATE TRANSITION
                       ↓
               AUDITLOG WRITER

Ranh giới:

AUTO MODERATION
= Máy kiểm tra

MODERATOR
= Normal Moderation + Escalation

ADMIN
= Severe Escalation + Enforcement

AUDITLOG WRITER
= Ghi nhận action + state transition

AUDITLOG QUERY
= Phase 3C
69. PHASE 3A KHÔNG LÀM
KYC
VIOLATION QUERY
AUDITLOG QUERY
FINANCE
PAYMENT
SETTLEMENT
PAYOUT
REFUND
ORDER CORE
CUSTOMER UI
VENDOR UI
DASHBOARD FINANCE
70. PHASE 3A KẾT THÚC KHI

Flow thuộc Phase 3A đã được implement theo contract:

Moderator
    ↓
Severe Case
    ↓
Escalation Contract
    ↓
Admin Queue
    ↓
Admin Detail
    ↓
Review Context
    ↓
Enforcement Contract
    ↓
State Transition
    ↓
AuditLog Writer

và phần implementation của Phase 3A đã được kiểm tra qua:

UI
→ Contract
→ Security
→ Service/API integration
→ Database khi integrated
→ AuditLog khi integrated

Nếu backend external chưa tồn tại:

Phase 3A implementation:
IMPLEMENTED

External integration:
INTEGRATION-READY

Nếu backend thật đã tồn tại và toàn bộ flow đã test:

Phase 3A implementation:
IMPLEMENTED

External integration:
INTEGRATED

Tuyệt đối không dùng fake production data để chuyển INTEGRATION-READY thành INTEGRATED.