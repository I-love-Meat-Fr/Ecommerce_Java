PHASE 4A – ADMIN DASHBOARD CORE

Implementation instruction: Read this Phase 4A file as the single source of truth.

Core independence rule:

“Tôi implement được phần của tôi bằng contract/interface hiện tại; backend của Tuấn là integration point, không phải prerequisite để bắt đầu hoặc hoàn thành implementation.”

Phase 4A phải có khả năng được triển khai độc lập.

Không chờ:

developer khác
branch khác
commit khác
merge request khác
backend implementation khác
provider implementation khác

Nếu backend/source dữ liệu bên ngoài chưa tồn tại:

Xác định contract hiện tại.
Xác định integration seam.
Implement phần Dashboard thuộc Phase 4A.
Test độc lập bằng fixture/test double trong test scope.
Đánh dấu INTEGRATION-READY.
Không tạo fake production data.
Không duplicate backend/domain/service của phase khác.
Khi backend thật có mặt, integrate tại seam đã xác định.
1. MỤC TIÊU

Phase 4A tập trung hoàn thiện:

ADMIN DASHBOARD CORE

sau các phase nền tảng trước.

Flow mục tiêu:

Admin Login
      ↓
Admin Dashboard
      ↓
┌─────────────────────────────────────────────┐
│ Platform Overview                           │
│                                             │
│ Users                                       │
│ Shops                                       │
│ Products                                    │
│ Orders                                      │
│                                             │
│ GMV                                         │
│ Platform Revenue                            │
│ Vendor Sales                                │
│ Vendor Payable                              │
│ Refund                                      │
│                                             │
│ Pending KYC                                 │
│ Pending Moderation                          │
│ Open Escalation                             │
│ Open Violation                              │
│ Suspended Shops                             │
└─────────────────────────────────────────────┘
      ↓
Navigation tới module Admin hiện có

Phase 4A phải thực sự:

INSPECT
↓
CONTRACT
↓
IMPLEMENT
↓
BUILD
↓
TEST
↓
INTEGRATION-READY / INTEGRATED
↓
REGRESSION
↓
REPORT

Không chỉ audit.

2. CORE INDEPENDENCE RULE
2.1 Không dùng dependency developer làm blocker

Không viết:

DEPENDENCY – Anh Tuấn
BLOCKED – chờ Tuấn code

chỉ vì backend chưa được implement.

Thay bằng:

External Integration Point:
KYC / Moderation / Escalation / Violation backend

Contract:
<contract hiện tại>

Integration Seam:
<service / gateway / adapter / API>

Current External Implementation:
AVAILABLE / NOT AVAILABLE / PARTIAL

User-owned Implementation:
IMPLEMENTED / PARTIAL

Integration Status:
INTEGRATED / INTEGRATION-READY
3. ĐỊNH NGHĨA TRẠNG THÁI

Bắt buộc dùng các trạng thái sau:

IMPLEMENTED

Phần thuộc Phase 4A đã được code và test độc lập.

INTEGRATION-READY

Phần Phase 4A đã hoàn thành nhưng backend/service bên ngoài chưa kết nối thực tế.

Ví dụ:

DashboardService
    ↓
AdminDashboardGateway
    ↓
KYC contract

Gateway đã sẵn sàng nhưng KYC backend chưa expose implementation thật.

Khi đó:

Dashboard: IMPLEMENTED
KYC integration: INTEGRATION-READY

Không phải:

Dashboard: BLOCKED
INTEGRATED

Backend thật đã kết nối và integration test thực tế PASS.

PARTIAL

Phần user-owned của Phase 4A chưa hoàn thành.

MISSING

Contract hoặc component bắt buộc chưa tồn tại và chưa có seam phù hợp.

WRONG

Code hiện tại tồn tại nhưng sai business rule/contract/security.

OUT OF SCOPE

Không thuộc Phase 4A.

BLOCKED

Chỉ dùng khi có hard environmental blocker thực sự, ví dụ:

repository không build được do baseline corruption
MongoDB environment không thể khởi động
Maven dependency infrastructure bị hỏng

Không dùng BLOCKED chỉ vì developer khác chưa code backend.

4. PHẠM VI PHASE 4A
IN SCOPE
1. Admin Dashboard UI
2. Dashboard Controller
3. Dashboard Service
4. Dashboard DTO / projection nếu cần
5. Dashboard Repository / aggregation nếu thuộc Dashboard
6. Dashboard integration layer
7. Dashboard metric integration
8. Dashboard pending/risk counters
9. Dashboard navigation
10. Loading state
11. Error state
12. Empty state
13. Dashboard security integration
14. Dashboard tests
15. Admin regression liên quan Dashboard
16. Integration seams cho external metrics

Metrics:

Total Users
Total Shops
Total Products
Total Orders

GMV
Platform Revenue
Vendor Sales
Vendor Payable
Refund

Pending KYC
Pending Moderation
Open Escalation
Open Violation
Suspended Shops
5. OUT OF SCOPE – BẮT BUỘC

Không triển khai:

WEB Voucher CRUD
WEB Voucher Hardening

Banner CRUD
Banner Scheduling
Banner Priority

PR/CMS mới

KYC Monitoring UI
KYC Provider
KYC Verification Engine

Violation Management UI
Violation Query UI

AuditLog Query UI
AuditLog Writer mới

ReportCase Backend

Moderator Moderation Flow
Product Moderation Flow
Review Moderation Flow

Admin Enforcement Flow

Payment Core
Commission Core
Settlement Core
Payout Core
Refund Core

Shipping Core
Cart Core
Stock Core

Customer UI
Vendor UI
Complaint UI

Phase 5 Full Integration Test

Nếu Dashboard cần dữ liệu từ module trên:

CONSUME CONTRACT

không:

REBUILD MODULE
6. PHASE BOUNDARY
Phase 3A
Admin Escalation + Enforcement + Audit Writer

Phase 3B
KYC Monitoring

Phase 3C
Violation + AuditLog Query

Phase 4A
Dashboard Core

Phase 4B
WEB Voucher Hardening

Phase 4C
Banner Hardening

Phase 4A:

READ / AGGREGATE

dữ liệu của Phase 3.

Không xây lại:

KYC Backend
Violation Backend
Escalation Backend
Moderation Backend
AuditLog Backend
7. BUSINESS CONTEXT

Project:

Java Spring Boot Marketplace

Package:

com.ecommerce.cnj70

Stack:

Spring Boot 3.2.0
Java 17
Spring Security 6
Spring Data MongoDB
Thymeleaf
Lombok
Maven
MongoDB Atlas

Roles:

ADMIN
VENDOR
CUSTOMER
MODERATOR

Không tạo:

SUPPLIER

Dashboard chỉ phục vụ:

ADMIN
8. OWNERSHIP MODEL
8.1 User-owned Phase 4A
Admin Dashboard UI
Dashboard Controller
Dashboard Service
Dashboard DTO/projection thuộc Dashboard
Dashboard aggregation thuộc Dashboard
Dashboard integration adapter/gateway
Dashboard navigation
Dashboard loading/error/empty state
Dashboard security integration
Dashboard tests
Admin regression
8.2 External integration points

Có thể có các external integration point:

KYC Backend
Moderation Backend
Escalation Backend
Violation Backend
Finance / Order / Payment aggregation
Security Core
Shop status source

Các module này:

KHÔNG PHẢI PREREQUISITE ĐỂ BẮT ĐẦU IMPLEMENT PHASE 4A

Chúng là:

INTEGRATION POINT
9. GIT SAFETY

Trước khi code:

git status
git branch
git log --oneline --decorate -10

Ghi nhận:

Current branch:
Current HEAD:
Working tree:
Uncommitted changes:

Nếu có thay đổi hiện tại:

DO NOT RESET
DO NOT STASH
DO NOT DELETE
DO NOT OVERWRITE

Không chạy:

git reset --hard
git clean -fd
git stash
git checkout -- .

Không:

commit
push
merge

trừ khi user yêu cầu.

10. ĐỌC CODE TRƯỚC KHI SỬA

Chỉ inspect các file trực tiếp liên quan.

Tối thiểu:

Admin Controller
Admin Service
Admin Template
Admin Layout
Admin CSS
Admin JS

User Entity
Shop Entity
Product Entity
Order Entity

KYC contract/model/service nếu tồn tại
Moderation contract/service nếu tồn tại
Violation contract/service nếu tồn tại
Escalation contract/service nếu tồn tại

Finance contract/service nếu tồn tại

SecurityConfig
JWT/Auth
Role handling

Tìm:

DashboardController
DashboardService
DashboardDTO
AdminDashboardDTO
dashboard.html
admin/dashboard

Không giả định tên class/file.

11. KIỂM TRA PHASE TRƯỚC

Xác định:

Phase 1
Phase 2
Phase 3A
Phase 3B
Phase 3C

Nhưng:

Chưa hoàn thành phase trước không tự động làm Phase 4A bị block.

Chỉ cần xác định contract/seam tương ứng.

Ví dụ:

KYC backend:
NOT IMPLEMENTED

Contract:
KYC status/count contract

Dashboard:
implement consumer side

Status:
INTEGRATION-READY
12. ADMIN UI REUSE

Phải reuse:

Admin layout
Sidebar
Header
Card
Table
Filter
Pagination
Modal
Alert
Status badge
Button
CSS
JS

Không:

redesign Admin

Không tạo:

new design system

Nếu component tồn tại:

REUSE

Nếu thiếu:

ADD SMALL COMPONENT
13. DASHBOARD INFORMATION ARCHITECTURE
A. Platform Overview
Users
Shops
Products
Orders
B. Financial Overview
GMV
Platform Revenue
Vendor Sales
Vendor Payable
Refund
C. Moderation / Compliance
Pending KYC
Pending Moderation
Open Escalation
Open Violation
D. Operational Risk
Suspended Shops

Không bắt buộc chart nếu backend không có dữ liệu đáng tin cậy.

14. PLATFORM METRICS

Dashboard phải consume source-of-truth:

User backend
Shop backend
Product backend
Order backend

Không:

hard-code
frontend state làm source-of-truth
fake response
15. TOTAL USERS

Trước khi implement xác định:

User status
Role
Deleted / soft deleted
Business definition

Không tự giả định:

document count = active users

Nếu contract định nghĩa:

COUNT ALL USERS

thì dùng đúng contract.

Nếu contract có filter:

COUNT theo business rule
16. TOTAL SHOPS

Kiểm tra:

Shop status
Deleted state
Approval state
Operational state

Không tự suy diễn:

Approved = Active
17. TOTAL PRODUCTS

Kiểm tra:

Product status
Deleted
Visibility
Moderation state

Không tự chọn:

ACTIVE only

nếu contract là total products.

18. TOTAL ORDERS

Kiểm tra:

Order status
Cancelled
Completed
Deleted
Business definition

Không dùng:

Completed Orders

thay cho:

Total Orders

nếu contract không quy định vậy.

19. FINANCIAL METRICS – CONTRACT FIRST

Bắt buộc phân biệt:

GMV
≠ Platform Revenue
≠ Vendor Sales
≠ Vendor Payable
≠ Refund

Mỗi metric phải có:

Definition
Source
Contract
Unit
Currency
Precision
Availability
20. GMV

Không tự chọn:

subtotal
total
paid amount
completed amount

Agent phải inspect business/backend definition.

Nếu contract hiện tại có:

GMV

consume contract.

Nếu chưa có implementation:

Integration Status:
INTEGRATION-READY

Không fake.

21. PLATFORM REVENUE

Không được:

platformRevenue = gmv;

trừ khi business contract thực sự định nghĩa như vậy.

Nếu chưa có backend implementation:

Dashboard financial integration seam:
READY

External implementation:
NOT AVAILABLE

Status:
INTEGRATION-READY

Không dùng GMV để giả Platform Revenue.

22. VENDOR SALES

Consume:

existing vendor/order financial contract

Không tự map:

Platform Revenue → Vendor Sales
23. VENDOR PAYABLE

Consume:

settlement/payable contract

Không:

Vendor Payable = Vendor Sales
24. REFUND

Consume:

existing refund/order financial contract

Không:

Refund = GMV - something

trừ khi backend contract định nghĩa.

25. FINANCIAL INTEGRATION SEAM

Nếu finance backend đã có:

REUSE EXISTING API/SERVICE

Nếu chưa có implementation nhưng có contract:

IMPLEMENT DASHBOARD CONSUMER

Nếu chưa có cả contract:

DEFINE MINIMAL DASHBOARD INTEGRATION CONTRACT

nhưng:

DO NOT IMPLEMENT FINANCE CORE

Ví dụ contract concept:

DashboardFinancialMetrics

gmv
platformRevenue
vendorSales
vendorPayable
refund
currency

Tên thực tế phải follow architecture hiện tại.

Không tạo duplicate domain model nếu project đã có model tương đương.

26. PENDING KYC

Dashboard chỉ:

READ COUNT

Không:

approve
reject
review
upload
call provider

Nếu KYC backend chưa implement:

Dashboard consumer:
IMPLEMENTED

KYC integration:
INTEGRATION-READY

Không fake:

0

trừ khi contract thực sự trả 0.

27. PENDING MODERATION

Consume moderation contract:

pending moderation count

Không tạo moderation workflow.

Không tạo fake:

0

để dashboard nhìn hoàn chỉnh.

28. OPEN ESCALATION

Consume:

Escalation read contract

Đếm theo status enum thực tế.

Không tự tạo:

OPEN

nếu enum không tồn tại.

29. OPEN VIOLATION

Consume:

Violation read/count contract

Không tạo:

ViolationService
ViolationController
Violation Query UI

chỉ để Dashboard hoạt động.

30. SUSPENDED SHOPS

Consume:

Shop operational status

Không map:

KYC_REJECTED → SUSPENDED
SHOP_REJECTED → SUSPENDED

trừ khi contract/business rule định nghĩa.

31. KHÔNG TỰ TẠO ENUM

Trước khi dùng:

PENDING
OPEN
SUSPENDED
ACTIVE

inspect:

ShopStatus
OrderStatus
ProductStatus
KycStatus
ViolationStatus
EscalationStatus

Nếu đã tồn tại:

REUSE

Không thêm enum mới chỉ để Dashboard compile.

32. DASHBOARD DTO

Nếu cần DTO:

AdminDashboardDTO

hoặc project-equivalent.

Logical grouping:

platform
    totalUsers
    totalShops
    totalProducts
    totalOrders

financial
    gmv
    platformRevenue
    vendorSales
    vendorPayable
    refund

compliance
    pendingKyc
    pendingModeration
    openEscalation
    openViolation

risk
    suspendedShops

Nếu project đã có DTO phù hợp:

REUSE
33. EXTERNAL INTEGRATION CONTRACT

Mỗi external metric phải có integration seam.

Ví dụ conceptual:

AdminDashboardMetricsGateway

hoặc project-equivalent.

Có thể expose:

getPlatformMetrics()
getFinancialMetrics()
getComplianceMetrics()
getRiskMetrics()

Nhưng:

Đây chỉ là ví dụ contract. Agent phải reuse architecture hiện tại nếu đã có equivalent.

Không tạo thêm interface nếu existing service/API đã đóng vai trò integration seam.

34. PRODUCTION IMPLEMENTATION RULE

Production code:

REAL BACKEND
REAL SERVICE
REAL API
REAL DATABASE SOURCE

Không dùng:

FakeDashboardService
MockDashboardService
HardcodedDashboardData
DemoMetricsProvider

trong production.

Test có thể dùng:

mock
stub
fixture
test double
35. KHÔNG ĐƯA BUSINESS LOGIC VÀO TEMPLATE

Flow:

Repository
    ↓
Service
    ↓
Integration Gateway
    ↓
DTO
    ↓
Controller
    ↓
Thymeleaf

Không:

query database

Không:

calculate financial metrics
36. DASHBOARD CONTROLLER

Nếu có:

DashboardController

thì reuse.

Không tạo duplicate:

AdminDashboardController
DashboardAdminController
AdminHomeController

chỉ vì khác naming.

Route phải follow project convention.

37. DASHBOARD SERVICE

Nếu có:

DashboardService

thì mở rộng.

Không tạo:

DashboardServiceV2
DashboardMetricService2

nếu không cần.

Service chịu trách nhiệm:

load metrics
aggregate
map DTO
coordinate integrations
handle dependency result
38. REPOSITORY / QUERY

Ưu tiên:

countDocuments
aggregation
match
group

Không:

load entire collection
→ Java loop
→ count

nếu MongoDB có thể count trực tiếp.

39. PERFORMANCE

Kiểm tra:

DB query count
Duplicate query
N+1
Full collection loading
Unnecessary aggregation

Ưu tiên:

Correctness
→ Maintainability
→ Reasonable performance

Không over-engineer.

40. EMPTY DATABASE

Nếu source-of-truth thực sự trả zero:

Users = 0
Shops = 0
Products = 0
Orders = 0

Các metric khác chỉ hiển thị 0 khi 0 có business meaning.

Không:

null
NaN
undefined
41. MISSING EXTERNAL DATA

Nếu backend chưa tồn tại:

DO NOT FAKE
DO NOT HARDCODE 0
DO NOT BYPASS
DO NOT DUPLICATE

Thay vào đó:

External Implementation:
NOT AVAILABLE

Dashboard Implementation:
IMPLEMENTED

Integration:
INTEGRATION-READY

UI có thể hiển thị:

N/A

nếu UI contract cho phép.

42. PARTIAL DATA FAILURE

Ví dụ:

Users       → OK
Shops       → OK
Products    → OK
Orders      → OK
Finance     → unavailable

Không:

Finance = 0

nếu 0 mang nghĩa thực tế.

Có thể:

Finance = N/A

nếu UI contract hỗ trợ.

43. DASHBOARD CARDS

Mỗi card:

Label
Value
Optional context
Optional navigation

Không bắt buộc chart/trend nếu chưa có contract dữ liệu.

44. LABEL ACCURACY

Ví dụ:

GMV
Platform Revenue
Vendor Sales
Vendor Payable
Refund

phải đúng semantic.

Không rút gọn:

Revenue

cho một metric thực chất là:

GMV
45. DASHBOARD NAVIGATION

Chỉ link tới route thực sự tồn tại.

Ví dụ:

Pending KYC
    → KYC Monitoring

Pending Moderation
    → Moderator Moderation

Open Escalation
    → Admin Escalation

Open Violation
    → Admin Violation

Suspended Shops
    → Admin Shop Management

Nếu route chưa tồn tại:

NO DEAD LINK

Không tạo route giả.

46. ADMIN ROUTE

Inspect route hiện tại.

Không tự mặc định:

/admin/dashboard

nếu project dùng route khác.

Không break URL cũ.

47. DASHBOARD SECURITY

Dashboard chỉ:

ADMIN

Expected:

User	Dashboard
Anonymous	DENIED
CUSTOMER	DENIED
VENDOR	DENIED
MODERATOR	DENIED
ADMIN	ALLOWED

Nếu project có security architecture khác:

REUSE EXISTING SECURITY CONTRACT
48. URL SECURITY

Không chỉ:

hide menu

Phải backend enforce.

Ví dụ nếu architecture dùng:

@PreAuthorize("hasRole('ADMIN')")

thì reuse pattern đó.

Không thay đổi Security Core toàn project.

49. LOCKED ADMIN

Theo Security Core hiện tại:

ADMIN ACTIVE
→ ALLOWED
ADMIN LOCKED
→ DENIED

Nếu Security Core chưa hỗ trợ:

Dashboard security integration:
IMPLEMENTED / INTEGRATION-READY

Security Core:
EXTERNAL INTEGRATION POINT

Không duplicate authentication system.

50. LOADING / ERROR / EMPTY

Nếu AJAX/API:

Loading
Success
Empty
Error

Nếu server-side Thymeleaf:

reuse server-rendering pattern

Không tạo loading framework mới.

51. ERROR HANDLING

Handle theo architecture:

401
403
404
409
500

Không expose:

stack trace
database exception
Java exception

cho Admin.

52. SESSION EXPIRED

Reuse authentication pattern:

redirect login

hoặc:

unauthorized message

Không tạo authentication mechanism mới.

53. REFRESH

Nếu có refresh:

CALL REAL BACKEND

Không refresh từ fake JS variables.

Nếu requirement chưa có:

NOT REQUIRED
54. DATE FILTER

Không tự thêm date filter.

Nếu đã tồn tại:

preserve

Nếu business contract yêu cầu:

implement against contract
55. TIMEZONE

Kiểm tra project convention.

Không tự đổi timezone chỉ trong Dashboard.

56. CURRENCY

Reuse existing currency convention.

Không tự đổi:

VND → USD

Không thay đổi financial domain logic.

57. NUMBER FORMATTING

Count:

1,234

Financial:

existing formatter

Chỉ format UI, không thay đổi backend value.

58. NO HARDCODE

Tuyệt đối không:

users = 1234;
shops = 42;
gmv = 100000000;

Không:

fake API response

trong production.

59. NO DUPLICATE BUSINESS LOGIC

Không duplicate:

Order financial calculation
Commission calculation
Settlement calculation
Payout calculation
Refund calculation

Dashboard:

READ / AGGREGATE

không trở thành Finance Core.

60. AUDITLOG BOUNDARY

Không implement:

AuditLog Writer
AuditLog Query
AuditLog UI

Phase 4A chỉ consume nếu đã có contract và thực sự cần.

61. KYC BOUNDARY

Phase 4A:

READ COUNT ONLY

External backend:

KYC integration point

Không:

approve
reject
review
upload
provider call
62. VIOLATION BOUNDARY

Phase 4A:

READ COUNT ONLY

Không:

create
update
assign
enforce
63. ESCALATION BOUNDARY

Phase 4A:

READ COUNT ONLY

Không:

process
approve
reject
suspend
ban
64. MODERATION BOUNDARY

Chỉ:

Pending Moderation count

Không:

Approve
Reject
Hide
Unhide
Escalate
65. SUSPENDED SHOP BOUNDARY

Chỉ:

COUNT

Không:

Suspend
Activate
Ban Vendor
66. EXISTING ADMIN NAVIGATION

Preserve các route hiện có:

User
Shop
Category
Order
Product
Review
KYC
Violation
Escalation
AuditLog

nếu route tồn tại.

67. DATABASE SAFETY

Không:

drop collection
delete production data
reset database
clear database
mass migration

Không seed production.

68. TEST DATA

Test dùng:

local/test environment
controlled fixtures
test database
test doubles

Không fake production code.

69. UNIT TEST

Test:

DashboardService
DashboardController
Dashboard aggregation
Dashboard integration adapters

Tối thiểu:

User count
Shop count
Product count
Order count

và các metric có contract.

70. FINANCIAL TEST

Test riêng:

GMV
Platform Revenue
Vendor Sales
Vendor Payable
Refund

Expected:

Không map nhầm field.
Không dùng GMV làm Platform Revenue.
Không dùng Vendor Sales làm Vendor Payable.
Không dùng GMV làm Refund.

Nếu backend thật chưa có:

Unit/contract test:
PASS

Real integration:
NOT INTEGRATED

Status:
INTEGRATION-READY
71. COMPLIANCE TEST

Test contract:

Pending KYC
Pending Moderation
Open Escalation
Open Violation
Suspended Shops

Nếu external backend chưa có:

consumer test:
PASS

real integration:
NOT RUN

integration status:
INTEGRATION-READY

Không ghi PASS cho E2E thật khi chưa có backend.

72. DATABASE VERIFICATION

Khi source-of-truth thật tồn tại:

DB/backend result
=
Dashboard result

Nếu external implementation chưa tồn tại:

DB verification:
NOT INTEGRATED

Contract verification:
PASS / FAIL

Không giả vờ database verification đã pass.

73. SECURITY TEST MATRIX
User	Expected
Anonymous	DENIED
CUSTOMER	DENIED
VENDOR	DENIED
MODERATOR	DENIED
ADMIN	ALLOWED
Locked ADMIN	DENIED

Test thực tế theo security architecture hiện tại.

74. DASHBOARD UI TEST

Kiểm tra:

Header
Sidebar
Dashboard cards
Metric labels
Metric values
Navigation
Empty state
Error state
Responsive layout cơ bản
75. BUSINESS RULE CHECK
[ ] GMV ≠ Platform Revenue
[ ] Vendor Sales ≠ Vendor Payable
[ ] Refund không bị gộp sai vào GMV

[ ] Pending KYC lấy từ KYC contract
[ ] Pending Moderation lấy từ Moderation contract
[ ] Escalation lấy từ Escalation contract
[ ] Violation lấy từ Violation contract
[ ] Suspended Shops dựa trên Shop operational status

[ ] Không hard-code production data
[ ] Không duplicate Finance Core
[ ] Không duplicate KYC Backend
[ ] Không duplicate Violation Backend
[ ] Không duplicate Escalation Backend
[ ] Không duplicate AuditLog Backend
[ ] Moderator ≠ Admin
76. ADMIN REGRESSION

Tối thiểu:

Admin Login
Admin Dashboard

User List
User Detail
Lock User
Unlock User
Role Management

Shop List
Shop Detail
Shop Approval
Shop Reject
Shop Activate
Shop Deactivate

Category List
Category Add
Category Edit
Category Delete

Order List
Order Detail

Product Management
Review Management

KYC route
Violation route
Escalation route
AuditLog route

Dashboard không được phá Admin hiện tại.

77. BUILD

Sau code:

mvn test

và:

mvn clean package

Ghi:

BUILD: PASS / FAIL
TEST: PASS / FAIL

Nếu fail:

Error:
Root cause:
Affected file/module:
Real blocker:
External integration:
78. GIT SAU KHI CODE

Chạy:

git status
git diff --stat
git diff

Kiểm tra:

Added
Modified
Deleted

Không:

commit
push
merge
reset
stash
79. FILE SCOPE

Có thể sửa:

Admin Dashboard Controller
Admin Dashboard Service
Admin Dashboard DTO
Admin Dashboard Repository
Admin Dashboard Gateway/Adapter nếu thuộc Dashboard
Admin Dashboard Template
Admin Dashboard CSS
Admin Dashboard JS
Dashboard Tests
Dashboard Security Integration

Không tự ý sửa:

Payment
Commission
Settlement
Payout
Shipping
KYC Provider
Violation Backend
Escalation Backend
AuditLog Backend
Complaint
Moderator workflow
Customer
Vendor

trừ khi cần sửa một integration contract thuộc phase và thay đổi đó thực sự nằm trong ownership của Phase 4A.

80. EXTERNAL INTEGRATION MODEL

Thay cho dependency kiểu:

DEPENDENCY – Tuấn

dùng:

Integration	Contract	User-owned work	External implementation	Status
KYC	KYC count/status contract	Dashboard consumer	KYC backend	INTEGRATION-READY / INTEGRATED
Moderation	Pending count contract	Dashboard consumer	Moderation backend	INTEGRATION-READY / INTEGRATED
Escalation	Open count contract	Dashboard consumer	Escalation backend	INTEGRATION-READY / INTEGRATED
Violation	Open count contract	Dashboard consumer	Violation backend	INTEGRATION-READY / INTEGRATED
Finance	Financial metrics contract	Dashboard consumer	Finance backend	INTEGRATION-READY / INTEGRATED
Security	Admin authorization contract	Dashboard security	Security Core	INTEGRATION-READY / INTEGRATED
Shop	Operational status contract	Suspended count	Shop backend	INTEGRATION-READY / INTEGRATED

External implementation không phải prerequisite để bắt đầu hoặc hoàn thành user-owned implementation.

81. CONTRACT READINESS

Trước khi code từng integration:

Contract exists?
↓
YES
↓
Reuse

NO
↓
Is there existing equivalent?
↓
YES
↓
Reuse

NO
↓
Define minimal integration seam
↓
Implement consumer
↓
Test
↓
INTEGRATION-READY

Không tạo domain/backend duplicate.

82. CONTRACT EXAMPLE

Nếu architecture cần một dashboard metrics contract, logical model có thể là:

PlatformMetrics
    totalUsers
    totalShops
    totalProducts
    totalOrders

FinancialMetrics
    gmv
    platformRevenue
    vendorSales
    vendorPayable
    refund

ComplianceMetrics
    pendingKyc
    pendingModeration
    openEscalation
    openViolation

RiskMetrics
    suspendedShops

Đây là contract shape, không bắt buộc phải tạo class mới nếu project đã có equivalent.

83. EXTERNAL BACKEND ABSENT

Nếu backend của Tuấn chưa có:

DO NOT WAIT

Thực hiện:

1. Inspect existing contract
2. Implement Dashboard consumer
3. Implement mapping
4. Implement UI
5. Implement error state
6. Implement tests
7. Use test double only in test
8. Mark INTEGRATION-READY

Không tạo:

fake KYC backend
fake violation records
fake escalation records
fake moderation records
84. SECURITY CORE ABSENT/INCOMPLETE

Nếu Security Core contract đã có:

consume contract

Nếu Security Core implementation chưa hoàn thiện:

Dashboard security integration:
IMPLEMENTED / INTEGRATION-READY

Không tạo SecurityConfig duplicate.

Không sửa authentication architecture ngoài scope.

85. REAL INTEGRATION TEST

Chỉ đánh:

INTEGRATED

khi:

real backend
+
real contract
+
real Dashboard consumer
+
actual integration test

đã chạy.

Nếu chưa:

INTEGRATION-READY
86. TEST RESULT

Báo cáo:

## TEST RESULT

1. Admin Login: PASS/FAIL
2. Dashboard Load: PASS/FAIL
3. User Count: PASS/FAIL
4. Shop Count: PASS/FAIL
5. Product Count: PASS/FAIL
6. Order Count: PASS/FAIL

7. GMV: PASS/FAIL/INTEGRATION-READY
8. Platform Revenue: PASS/FAIL/INTEGRATION-READY
9. Vendor Sales: PASS/FAIL/INTEGRATION-READY
10. Vendor Payable: PASS/FAIL/INTEGRATION-READY
11. Refund: PASS/FAIL/INTEGRATION-READY

12. Pending KYC: PASS/FAIL/INTEGRATION-READY
13. Pending Moderation: PASS/FAIL/INTEGRATION-READY
14. Open Escalation: PASS/FAIL/INTEGRATION-READY
15. Open Violation: PASS/FAIL/INTEGRATION-READY
16. Suspended Shops: PASS/FAIL/INTEGRATION-READY

17. Dashboard Navigation: PASS/FAIL
18. Empty State: PASS/FAIL
19. Error State: PASS/FAIL
20. Security: PASS/FAIL
21. Locked Admin: PASS/FAIL
22. Admin Regression: PASS/FAIL
23. Maven Test: PASS/FAIL
24. Maven Package: PASS/FAIL
87. MANUAL TEST MATRIX
#	Test	Expected	Actual	Status
1	Admin Login	Login thành công	?	?
2	Dashboard Load	Dashboard hiển thị	?	?
3	User Count	Đúng contract/source	?	?
4	Shop Count	Đúng contract/source	?	?
5	Product Count	Đúng contract/source	?	?
6	Order Count	Đúng contract/source	?	?
7	GMV	Đúng backend contract	?	?
8	Platform Revenue	Đúng backend contract	?	?
9	Vendor Sales	Đúng backend contract	?	?
10	Vendor Payable	Đúng backend contract	?	?
11	Refund	Đúng backend contract	?	?
12	Pending KYC	Đúng contract	?	?
13	Pending Moderation	Đúng contract	?	?
14	Open Escalation	Đúng contract	?	?
15	Open Violation	Đúng contract	?	?
16	Suspended Shops	Đúng contract	?	?
17	Navigation	Route tồn tại	?	?
18	Empty State	Hiển thị hợp lệ	?	?
19	API Error	Error state	?	?
20	Customer Access	Denied	?	?
21	Vendor Access	Denied	?	?
22	Moderator Access	Denied	?	?
23	Admin Access	Allowed	?	?
24	Locked Admin	Denied	?	?
25	Admin Regression	Không lỗi	?	?
88. DATABASE VERIFICATION

Với backend thật:

Users DB/source
=
Dashboard Users

Shops DB/source
=
Dashboard Shops

Products DB/source
=
Dashboard Products

Orders DB/source
=
Dashboard Orders

Financial:

Dashboard
=
financial backend result

Compliance:

Dashboard
=
KYC/Moderation/Escalation/Violation result

Nếu external implementation chưa có:

Real DB Verification:
NOT INTEGRATED

Contract Verification:
PASS / FAIL
89. BÁO CÁO GIT BASELINE
## 1. Git Baseline

Current branch:

HEAD before:

HEAD after:

Working tree:

Uncommitted changes:

Không tự commit.

90. BÁO CÁO IMPLEMENTATION
## 2. IMPLEMENTATION

### Dashboard Core

- [ ] Admin Dashboard
- [ ] Total Users
- [ ] Total Shops
- [ ] Total Products
- [ ] Total Orders

### Financial

- [ ] GMV
- [ ] Platform Revenue
- [ ] Vendor Sales
- [ ] Vendor Payable
- [ ] Refund

### Compliance

- [ ] Pending KYC
- [ ] Pending Moderation
- [ ] Open Escalation
- [ ] Open Violation

### Risk

- [ ] Suspended Shops

### Cross-cutting

- [ ] Navigation
- [ ] Loading/Empty/Error
- [ ] Security
- [ ] Tests
- [ ] Regression

Chỉ đánh [x] khi phần user-owned thực sự đã implement và test.

Nếu backend chưa có nhưng consumer đã hoàn thành:

[x] Dashboard consumer
Status: INTEGRATION-READY
91. BÁO CÁO INTEGRATION-READY
## 3. INTEGRATION-READY

Integration:

Contract:

Integration Seam:

User-owned Implementation:

External Implementation:

Current External Status:

Tests Completed:

Remaining Integration Work:

Ví dụ:

Integration:
Pending KYC

Contract:
KYC count/status contract

Integration Seam:
Existing KYC service/gateway

User-owned Implementation:
Dashboard consumer + UI + tests

External Implementation:
Not available

Current External Status:
NOT IMPLEMENTED

Tests Completed:
Consumer unit tests PASS

Remaining Integration Work:
Connect actual KYC backend

Status:
INTEGRATION-READY
92. BÁO CÁO INTEGRATED
## 4. INTEGRATED

Integration:

Contract:

External Source:

Dashboard Consumer:

Integration Test:

Actual Result:

Status:
INTEGRATED
93. BÁO CÁO PARTIAL
## 5. PARTIAL

Feature:

Completed:

Remaining:

File:

Reason:

Current Status:

Không ghi owner như blocker nếu issue chỉ là external implementation.

94. BÁO CÁO MISSING
## 6. MISSING

Feature:

Expected:

Current:

Missing Contract/Component:

Impact:

Resolution:

Status:
95. BÁO CÁO WRONG
## 7. WRONG

Feature:

Current behavior:

Expected behavior:

File:

Impact:

Fixed:

YES / NO

External integration required:

YES / NO

Ví dụ:

Feature:
Platform Revenue

Current:
Dashboard đang lấy GMV

Expected:
Platform Revenue theo finance contract

Impact:
Metric sai semantic

Fixed:
YES / NO
96. BÁO CÁO EXTERNAL INTEGRATION
## 8. EXTERNAL INTEGRATION

### KYC

Contract:
Current backend:
Dashboard implementation:
Integration seam:
Status:

### Moderation

Contract:
Current backend:
Dashboard implementation:
Integration seam:
Status:

### Escalation

Contract:
Current backend:
Dashboard implementation:
Integration seam:
Status:

### Violation

Contract:
Current backend:
Dashboard implementation:
Integration seam:
Status:

### Finance

Contract:
Current backend:
Dashboard implementation:
Integration seam:
Status:

### Security

Contract:
Current security implementation:
Dashboard integration:
Status:
97. BÁO CÁO FILES CHANGED
## 9. FILES CHANGED

### Added

...

### Modified

...

### Deleted

...

Với file quan trọng:

Purpose:
98. SECURITY REPORT
## 10. SECURITY

Anonymous:
PASS / FAIL

CUSTOMER:
PASS / FAIL

VENDOR:
PASS / FAIL

MODERATOR:
PASS / FAIL

ADMIN:
PASS / FAIL

Locked ADMIN:
PASS / FAIL

Security Core Integration:
INTEGRATED / INTEGRATION-READY
99. BUSINESS RULE REPORT
## 11. BUSINESS RULE CHECK

[ ] GMV ≠ Platform Revenue
[ ] Vendor Sales ≠ Vendor Payable
[ ] Refund không bị gộp sai vào GMV
[ ] Dashboard không hard-code
[ ] Financial metric không duplicate Finance Core
[ ] Pending KYC lấy từ KYC contract
[ ] Pending Moderation lấy từ Moderation contract
[ ] Escalation lấy từ Escalation contract
[ ] Violation lấy từ Violation contract
[ ] Suspended Shop dựa trên Shop operational status
[ ] Dashboard không duplicate Phase 3 backend
[ ] Moderator ≠ Admin
100. BUILD REPORT
## 12. BUILD

Command:
mvn test

Result:
PASS / FAIL

Command:
mvn clean package

Result:
PASS / FAIL

If FAIL:

Error:

Root cause:

Affected module:

Real blocker:

External integration:
101. KNOWN ISSUES
## 13. KNOWN ISSUES

Issue:

Impact:

User-owned or External:

Integration Status:

Workaround:

Next Action:
102. OUT OF SCOPE REPORT
## 14. OUT OF SCOPE

WEB Voucher
Banner
KYC Monitoring UI
Violation Query UI
AuditLog Query UI
AuditLog Writer
Escalation Processing
Enforcement
Moderator Moderation Flow
Payment Core
Commission
Settlement
Payout
Refund Core
Shipping Core
Customer UI
Vendor UI
Phase 5 Full Integration
103. PHASE COMPLETION RULE

Phase 4A không yêu cầu backend của developer khác phải hoàn thành để user-owned implementation hoàn tất.

Ví dụ:

User Count
IMPLEMENTED + INTEGRATED

Pending KYC
IMPLEMENTED + INTEGRATION-READY

Open Violation
IMPLEMENTED + INTEGRATION-READY

Platform Revenue
IMPLEMENTED + INTEGRATION-READY

Phase 4A vẫn có thể xem phần Dashboard implementation là hoàn thành.

Điều kiện:

Dashboard consumer đã implement
Contract đã xác định
Integration seam đã xác định
Không fake production data
Tests độc lập đã pass
104. KHÔNG DÙNG DEPENDENCY LÀM IMPLEMENTATION BLOCKER

Không viết:

Blocked because Tuấn hasn't finished KYC backend.

Viết:

KYC Dashboard integration:

User-owned:
IMPLEMENTED

External backend:
NOT AVAILABLE

Contract:
DEFINED / EXISTING

Integration seam:
READY

Unit/contract tests:
PASS

Integration status:
INTEGRATION-READY
105. READY FOR PHASE 4B

Phase 4A có thể đánh:

READY FOR PHASE 4B

khi phần user-owned của Phase 4A đã hoàn thành:

1. Dashboard route hoạt động
2. Dashboard chỉ ADMIN truy cập được
3. User count implementation đúng contract
4. Shop count implementation đúng contract
5. Product count implementation đúng contract
6. Order count implementation đúng contract
7. GMV mapping đúng contract
8. Platform Revenue mapping đúng contract
9. Vendor Sales mapping đúng contract
10. Vendor Payable mapping đúng contract
11. Refund mapping đúng contract
12. Pending KYC consumer hoàn chỉnh
13. Pending Moderation consumer hoàn chỉnh
14. Open Escalation consumer hoàn chỉnh
15. Open Violation consumer hoàn chỉnh
16. Suspended Shop consumer hoàn chỉnh
17. Không hard-code production data
18. Không duplicate backend owner
19. Security implementation/test hoàn chỉnh
20. Locked Admin contract/test hoàn chỉnh
21. Dashboard tests PASS
22. Admin regression PASS
23. mvn test PASS
24. mvn clean package PASS
25. Không còn P0/P1 thuộc user-owned Phase 4A

Nếu external backend chưa có:

READY FOR PHASE 4B
+
INTEGRATION-READY

là hợp lệ.

Không bắt buộc:

INTEGRATED

để hoàn thành implementation của Phase 4A.

106. KHI NÀO THỰC SỰ BLOCKED

Chỉ:

BLOCKED

nếu có hard blocker thực tế như:

Project không build được do baseline corruption
Required source file không tồn tại và architecture không có seam khả dụng
Environment không thể chạy test
Maven infrastructure failure
Database infrastructure unavailable khi test bắt buộc và không có test seam thay thế

Không dùng:

BLOCKED

cho:

Tuấn chưa code
Quốc Anh chưa code
Backend khác chưa merge
Branch khác chưa merge
Developer khác chưa commit
107. FINAL STATUS RULE

Phase 4A final status dùng:

IMPLEMENTED + INTEGRATED

hoặc:

IMPLEMENTED + INTEGRATION-READY

hoặc:

PARTIAL

hoặc:

BLOCKED

Trong đó:

IMPLEMENTED + INTEGRATED

Phần Dashboard hoàn thành và backend thật đã kết nối.

IMPLEMENTED + INTEGRATION-READY

Phần Dashboard hoàn thành, nhưng một hoặc nhiều external backend chưa có implementation thật.

PARTIAL

User-owned Dashboard implementation chưa hoàn thành.

BLOCKED

Chỉ hard environmental blocker thực sự.

108. FINAL REPORT FORMAT

Sau khi thực hiện:

# PHASE 4A – FINAL REPORT

## 1. Git Baseline

Current branch:

HEAD before:

HEAD after:

Working tree:

Uncommitted changes:

## 2. Objective

...

## 3. Implementation Status

...

## 4. Integrated

...

## 5. Integration-Ready

...

## 6. Partial

...

## 7. Missing

...

## 8. Wrong

...

## 9. External Integration

...

## 10. Files Changed

### Added
...

### Modified
...

### Deleted
...

## 11. Test Result

...

## 12. Security Test

...

## 13. Regression Test

...

## 14. Business Rule Check

...

## 15. Build

mvn test:
PASS / FAIL

mvn clean package:
PASS / FAIL

## 16. Known Issues

...

## 17. Real Blockers

...

## 18. Out of Scope

...

## 19. Phase Status

IMPLEMENTED + INTEGRATED

hoặc

IMPLEMENTED + INTEGRATION-READY

hoặc

PARTIAL

hoặc

BLOCKED

## 20. Next Phase

Phase 4B – WEB Voucher Hardening
109. TUYỆT ĐỐI KHÔNG BÁO CÁO SAI

Không ghi:

DONE

chỉ vì:

Dashboard HTML đã tạo.

Không ghi:

PASS

chỉ vì:

mvn compile

Không ghi:

Integrated

nếu backend thật chưa kết nối.

Không ghi:

Platform Revenue hoàn thành

nếu đang lấy GMV.

Không ghi:

Pending KYC hoàn thành

nếu đang hard-code 0.

Không ghi:

Violation count hoàn thành

nếu chưa có source-of-truth/contract.

Không ghi:

Security PASS

nếu chưa test authorization.

110. QUY TẮC KHI GẶP EXTERNAL BACKEND CHƯA CÓ

Bắt buộc:

1. Inspect existing architecture.
2. Find existing contract.
3. Find existing equivalent service/interface.
4. Reuse if available.
5. Define minimal seam only if necessary.
6. Implement Dashboard consumer.
7. Implement UI.
8. Implement mapping.
9. Implement error state.
10. Test with test doubles only in test scope.
11. Mark INTEGRATION-READY.
12. Do not fake production behavior.
13. Do not duplicate external backend.
111. MỤC TIÊU CUỐI CÙNG

Sau Phase 4A:

ADMIN
  ↓
DASHBOARD
  │
  ├── Platform Overview
  │     ├── Users
  │     ├── Shops
  │     ├── Products
  │     └── Orders
  │
  ├── Financial Overview
  │     ├── GMV
  │     ├── Platform Revenue
  │     ├── Vendor Sales
  │     ├── Vendor Payable
  │     └── Refund
  │
  ├── Compliance Overview
  │     ├── Pending KYC
  │     ├── Pending Moderation
  │     ├── Open Escalation
  │     └── Open Violation
  │
  └── Risk Overview
        └── Suspended Shops

Data flow:

EXISTING CONTRACT
       ↓
DASHBOARD INTEGRATION SEAM
       ↓
DASHBOARD SERVICE
       ↓
DASHBOARD DTO
       ↓
ADMIN CONTROLLER
       ↓
THYMELEAF
       ↓
ADMIN UI

External implementation:

KYC Backend
Moderation Backend
Escalation Backend
Violation Backend
Finance Backend
Security Core

được kết nối tại:

INTEGRATION SEAM

chứ không phải:

PREREQUISITE TO START
112. YÊU CẦU CUỐI CÙNG CHO CODING AGENT
Trước khi bắt đầu
1. Đọc code hiện tại.
2. Đọc contract/implementation Phase 1–3 nếu liên quan.
3. Kiểm tra Git.
4. Xác định Dashboard contract.
5. Xác định integration seam.
6. Xác định source-of-truth.
7. Xác định external integration points.
8. Không coi developer khác là blocker.
9. Không giả định backend đã tồn tại.
Trong khi code
1. Reuse Admin UI.
2. Reuse backend contract.
3. Reuse existing service/interface.
4. Không redesign Admin.
5. Không làm Voucher.
6. Không làm Banner.
7. Không làm KYC UI.
8. Không làm Violation UI.
9. Không làm AuditLog UI.
10. Không làm Enforcement.
11. Không fake production data.
12. Không hard-code production metrics.
13. Không duplicate Finance Core.
14. Không duplicate Phase 3 backend.
15. Không bypass security.
16. Không overwrite code người khác.
17. Không phá Admin cũ.
18. Nếu external backend thiếu → INTEGRATION-READY.
Sau khi code
1. Chạy mvn test.
2. Chạy mvn clean package.
3. Kiểm tra git diff.
4. Test Dashboard.
5. Test tất cả metric có contract.
6. Test financial mapping.
7. Test compliance counters.
8. Test navigation.
9. Test Security.
10. Test Locked Admin.
11. Test Admin Regression.
12. Kiểm tra Business Rules.
13. Phân loại IMPLEMENTED / INTEGRATED / INTEGRATION-READY / PARTIAL / MISSING / WRONG.
14. Liệt kê toàn bộ file thay đổi.
15. Nêu rõ external integration chưa kết nối.
16. Chỉ ghi BLOCKED nếu có hard blocker thật.
17. Kết luận trạng thái Phase 4A.
113. NGUYÊN TẮC CHỐT

Không được hiểu “independent implementation” là tạo kiến trúc độc lập hoặc copy lại backend.

Định nghĩa đúng là:

INDEPENDENT IMPLEMENTATION
=
Không chờ người khác code
+
Không chờ branch/commit/MR
+
Có contract/interface rõ
+
Có integration seam
+
Implement phần thuộc ownership của mình
+
Test độc lập
+
Không fake production behavior
+
Sau đó connect backend thật

Vì vậy câu:

“Tôi implement được phần của tôi bằng contract/interface hiện tại; backend của Tuấn là integration point, không phải prerequisite để bắt đầu hoặc hoàn thành implementation.”

được áp dụng cho toàn bộ Phase 4A, không chỉ KYC.

Đây là thay đổi cốt lõi so với Phase 4A cũ: backend của Tuấn, backend finance của Quốc Anh và Security Core là integration points; chúng không còn là lý do để ngăn coding agent hoàn thành phần Dashboard thuộc Phase 4A.