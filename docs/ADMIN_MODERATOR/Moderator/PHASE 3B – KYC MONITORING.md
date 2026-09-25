PHASE 3B — ADMIN KYC MONITORING

Implementation instruction: Read this Phase 3B file as the single source of truth.

Core independence rule:

“Tôi implement được phần của tôi bằng contract/interface hiện tại; backend của Tuấn là integration point, không phải prerequisite để bắt đầu hoặc hoàn thành implementation.”

Phase 3B phải được triển khai độc lập ở phần thuộc Admin KYC Monitoring. Việc KYC Backend/Provider của Anh Tuấn đã có implementation hay chưa không được dùng làm điều kiện để bắt đầu hoặc hoàn thành phần Admin-owned.

Nếu backend bên ngoài chưa có:

Xác định contract/interface hiện tại.
Xác định integration seam.
Implement phần Admin KYC Monitoring dựa trên contract.
Test phần có thể test độc lập.
Đánh dấu INTEGRATION-READY.
Không tạo fake KYC production behavior.
Khi backend thật có, connect implementation thật tại integration seam.
Chỉ đánh dấu INTEGRATED sau khi integration thực tế pass.
1. MỤC TIÊU

Implement hoàn chỉnh nghiệp vụ:

ADMIN KYC MONITORING

Phase 3B tập trung vào việc để Admin:

theo dõi hồ sơ KYC của Vendor/Shop;
xem trạng thái KYC;
lọc/tìm kiếm hồ sơ KYC;
xem chi tiết KYC;
xem provider/reference/status/rejection reason;
kiểm tra quan hệ giữa KYC và Shop lifecycle;
bảo vệ PII;
kiểm tra security;
kiểm tra database;
kiểm tra integration với KYC backend/provider;
regression test.

Flow:

VENDOR
   ↓
KYC SUBMIT
   ↓
KYC BACKEND / PROVIDER
   ↓
KYC STATUS
   ↓
ADMIN KYC MONITORING
   ↓
KYC LIST
   ↓
KYC DETAIL
   ↓
SHOP LIFECYCLE

Ranh giới:

KYC PROVIDER / BACKEND
        ↓
    VERIFICATION
        ↓
       STATUS
        ↓
ADMIN MONITORING

Admin MONITOR, không trở thành KYC provider.

Admin không tự xác minh giấy tờ nếu business flow/backend hiện tại quy định verification thuộc KYC backend/provider.

2. NGUYÊN TẮC ĐỘC LẬP CỦA PHASE 3B

Phase 3B có hai lớp:

PHẦN THUỘC PHASE 3B
        ↓
Admin KYC Monitoring
        ↓
Controller / Service / Adapter / UI / Security / Tests
        ↓
KYC Contract / Interface
        ↓
EXTERNAL INTEGRATION POINT
        ↓
KYC Backend / Provider
Quy tắc

Backend của Anh Tuấn là:

INTEGRATION POINT

không phải:

PREREQUISITE

Không được viết:

DEPENDENCY – Anh Tuấn

với ý nghĩa:

không làm được Phase 3B

Thay bằng:

External Integration:
KYC Backend

Contract:
<existing KYC API/service/interface>

Integration Seam:
<existing adapter/gateway/service boundary>

Current External Implementation:
AVAILABLE / PARTIAL / MISSING

Implementation Status:
IMPLEMENTED / PARTIAL

Integration Status:
INTEGRATION-READY / INTEGRATED

Note:
External backend implementation is not required to implement
the Admin-owned Phase 3B layer.
Nếu KYC backend chưa tồn tại

Được phép:

define contract
create adapter/interface seam nếu kiến trúc chưa có
implement Admin layer
write unit/component tests
write integration contract tests

Không được:

fake KYC production records
fake provider result
fake KYC status
fake verification result
fake PII
fake API response trong production

Test double/mock chỉ được dùng:

TEST ENVIRONMENT

không được đưa vào production flow.

3. PHẠM VI PHASE 3B

Phase 3B gồm:

KYC Backend Contract Audit
Admin KYC Route
KYC List
KYC Detail
KYC Status
KYC Search
KYC Filter
KYC Pagination
Provider Information
Reference ID
Submitted At
Verified At
Rejection Reason
Business Type
Vendor Information
Shop Information
KYC → Shop Lifecycle relation
PII Masking
KYC Security
Admin-only Access
Locked Account Check
Error Handling
Database Verification
Contract/Integration Test
Regression Test
End-to-End KYC Monitoring Test
File Scope Verification
External Integration Report
Final Phase 3B Report
4. KHÔNG THUỘC PHASE 3B
Phase 3A

Không triển khai:

Admin Escalation Queue
Escalation Detail
Suspend Product
Suspend Shop
Ban Vendor
Enforcement Reason
Enforcement Confirmation
Admin Enforcement Audit Writer

Thuộc:

PHASE 3A – ADMIN ESCALATION + ENFORCEMENT + AUDIT WRITER

Phase 3C

Không triển khai:

Violation Management UI
Violation List
Violation Detail
Violation Filter
Violation Action
Violation Status Management
AuditLog List
AuditLog Query
AuditLog Filter
AuditLog Detail
AuditLog Viewer

Thuộc:

PHASE 3C – VIOLATION & AUDITLOG QUERY

Ngoài phạm vi

Không viết lại:

KYC Provider
KYC verification engine
Auto Moderation
ReportCase backend
Escalation backend
Violation backend
AuditLog backend nếu đã có owner
Payment
Finance
Settlement
Payout
Refund
Order Core
Customer UI
Vendor UI toàn bộ
Shop UI toàn bộ
Dashboard tài chính
5. NGUYÊN TẮC BẮT BUỘC
Được phép
đọc source code;
tìm kiếm file;
đọc Git;
chạy build;
chạy test;
chạy application;
tạo/sửa Admin KYC UI;
tạo/sửa Admin Controller nếu cần;
tạo/sửa Admin Service nếu cần;
tạo/sửa DTO/ViewModel nếu cần;
tạo/sửa Template;
tạo/sửa CSS/JS liên quan;
tạo adapter/gateway integration seam nếu kiến trúc chưa có seam phù hợp;
kết nối KYC backend API;
test KYC monitoring;
test security;
test database;
test integration.
Không được
reset;
stash;
merge;
push;
xóa thay đổi của người khác;
overwrite code người khác;
refactor lớn ngoài phạm vi;
tự thay đổi business rule;
tự thay đổi KYC provider;
tự thay đổi KYC verification algorithm;
tự tạo fake KYC production data;
tự tạo status chỉ để UI hoạt động;
tạo duplicate KYC backend service/entity nếu đã tồn tại;
tạo duplicate enum nếu backend đã có source of truth.
6. GIT SAFETY

Chạy:

git status
git branch
git log --oneline --decorate -10

Ghi:

Current branch:
Current HEAD:
Working tree:
Uncommitted changes:

Nếu có thay đổi chưa commit:

Không reset.
Không stash.
Không checkout làm mất thay đổi.
Không overwrite file đang được người khác chỉnh sửa.
Không merge branch khác.
Không push.
7. PRE-CHECK PHASE 1 + PHASE 2 + PHASE 3A
Phase 1

Kiểm tra:

Admin Login
Admin Security
Admin Layout
Admin namespace
Authentication
Authorization
Locked account behavior
Phase 2

Kiểm tra contract liên quan đến:

Vendor KYC Submit nếu đã có;
Vendor KYC flow nếu đã có;
KYC data được tạo ở đâu;
KYC status được quản lý ở đâu.
Phase 3A

Kiểm tra không phá:

Admin security;
Admin layout;
Admin enforcement;
Audit Writer integration.

Phase 3B consume contract, không cần Phase 3A implementation làm prerequisite cho Admin KYC Monitoring.

Nếu một external implementation chưa tồn tại:

Current Implementation:
MISSING

Integration Seam:
DEFINED / NOT DEFINED

Phase 3B:
IMPLEMENTABLE / PARTIAL

Integration:
INTEGRATION-READY

Chỉ BLOCKED nếu có hard environmental blocker thật sự, ví dụ repository/build baseline không thể chạy, không phải vì developer khác chưa code backend.

8. KYC BACKEND CONTRACT AUDIT

Trước khi viết UI phải inspect các thành phần hiện có:

KYC Entity
KYC Repository
KYC Service
KYC Controller
KYC DTO
KYC Enum
KYC Status
KYC Provider
KYC Reference
Vendor
Shop
User
Security
Database

Tìm:

KycService
KycController
KycRepository
KycProvider
KycStatus
VendorService
ShopService
UserService

Phân loại:

[GREEN] Existing implementation/contract
[YELLOW] Partial implementation/contract
[RED] Missing required contract
[BLUE] Integration seam

Không dùng:

[BLUE] = blocked

[BLUE] chỉ có nghĩa:

external integration point chưa được nối implementation thật
9. CONTRACT-FIRST KYC INTEGRATION

Trước khi viết UI xác định:

KYC ID
Vendor ID
Shop ID
Status
Provider
Reference ID
Submitted At
Verified At
Rejection Reason
Business Type

Nếu backend contract có:

Provider Status
Verification Score
Verification Result
Failure Code
Document Type
Last Checked At

chỉ hiển thị khi contract/business rule cho phép.

Không tự thêm field không tồn tại trong contract.

10. INTEGRATION SEAM

Ưu tiên reuse:

existing KYC service
existing API client
existing gateway
existing adapter
existing DTO
existing interface

Nếu kiến trúc chưa có boundary phù hợp, Phase 3B có thể tạo một integration seam tối thiểu thuộc phạm vi Admin integration.

Ví dụ khái niệm:

AdminKycMonitoring
        ↓
KycMonitoringGateway
        ↓
Existing KYC API / KYC Service

Tên thật phải theo architecture hiện tại.

Không tạo duplicate:

Second KycService
Second KycRepository
Second KycEntity
Second KycStatus
Second KycProvider

Integration seam chỉ định nghĩa cách Admin layer consume KYC data.

11. KYC STATUS

Sử dụng status thực tế của backend.

Ví dụ backend có:

PENDING
REJECTED
VERIFIED
SUSPENDED

thì dùng chính contract đó.

Không tự tạo:

PENDING_KYC
KYC_REJECTED
APPROVED

chỉ để UI đẹp hơn.

Báo cáo:

Current backend status:
Expected business status:
Mapping:
Source of truth:
Integration status:

Nếu không có status contract:

Status Contract:
MISSING

Implementation:
INTEGRATION-READY / PARTIAL

Production behavior:
NO FAKE STATUS
12. KYC STATUS ≠ SHOP STATUS

Bắt buộc phân biệt:

KYC STATUS

và:

SHOP STATUS

Không mặc định:

KYC APPROVED = SHOP APPROVED

hoặc:

KYC REJECTED = SHOP REJECTED

trừ khi backend/business logic hiện tại định nghĩa như vậy.

Báo cáo:

Current lifecycle:
KYC → Shop relation:
Automatic transition:
Manual transition:
Source of truth:
Integration status:
13. ADMIN KYC ROUTE

Tạo/hoàn thiện:

/admin/kyc

Nếu project đã có route tương đương:

reuse route hiện tại.

Không tạo duplicate route.

Route phải:

nằm trong Admin namespace;
dùng Admin layout;
chỉ ADMIN truy cập;
không phá route cũ.

Detail:

/admin/kyc/{id}

hoặc route tương đương đang tồn tại.

14. KYC LIST

KYC List hiển thị tối thiểu:

KYC ID
Vendor
Shop
Owner Name
Business Type
KYC Status
Provider
Reference ID
Submitted At
Verified At
Rejection Reason

Nếu backend không cung cấp field:

N/A

hoặc bỏ field theo UI contract.

Không fake.

15. KYC TABLE UX

Phải có:

Header rõ ràng;
Status badge;
Date formatting;
Empty state;
Loading state;
Error state;
Detail action;
Pagination nếu backend hỗ trợ.

Không để:

raw enum khó đọc;
raw JSON;
stack trace;
text tràn;
layout vỡ.
16. KYC STATUS BADGE

Status badge phải mapping từ backend contract.

Không mapping theo suy đoán.

Nếu Admin đã có status badge component:

reuse component hiện tại.

17. KYC SEARCH

Nếu backend contract hỗ trợ:

Vendor
Shop
KYC ID
Reference ID

thì Admin được search.

Không tự tạo search plaintext cho:

Citizen ID
Bank Account
Sensitive PII

nếu không có business/security requirement.

Nếu backend không hỗ trợ:

Search:
NOT PROVIDED BY CONTRACT

Integration:
INTEGRATION-READY / PARTIAL

Không load toàn bộ database lên browser để giả lập backend search.

18. KYC FILTER

Nếu backend hỗ trợ:

KYC Status
Business Type
Provider
Vendor
Shop
Submitted Date
Verified Date

frontend phải consume đúng contract.

Không tự đổi parameter name.

Không tạo frontend-only filter trên toàn bộ dataset khi dataset có thể lớn.

19. MULTIPLE FILTER

Test:

Status = PENDING
Provider = VNPT
Business Type = ...

và:

Status = REJECTED
Vendor = ...
Date = ...

Expected:

đúng record;
không duplicate;
pagination đúng;
reset filter hoạt động.
20. RESET FILTER

Phải hỗ trợ:

Apply Filter
Reset Filter

Reset đưa list về trạng thái mặc định.

Nếu query state được quản lý trên URL thì reset phải xóa query filter theo architecture hiện tại.

21. KYC PAGINATION

Nếu backend hỗ trợ pagination:

phải dùng backend pagination.

Kiểm tra:

0 records
1 record
10 records
many records
multiple pages

Kiểm tra:

Current page
Page size
Total
Next
Previous

Không fetch toàn bộ dataset nếu backend đã hỗ trợ pagination.

22. KYC DETAIL

Detail phải hiển thị:

KYC ID
Vendor
Shop
Owner Name
Business Type
KYC Status
Provider
Reference ID
Submitted At
Verified At
Rejection Reason

Nếu contract có:

Provider Status
Verification Result
Failure Code
Last Checked At

chỉ hiển thị nếu được phép.

23. VENDOR INFORMATION

Hiển thị thông tin cần thiết:

Vendor ID
Vendor Name
Vendor Email

Không dump toàn bộ User object.

Tuân thủ existing Admin UI/security policy.

24. SHOP INFORMATION

Hiển thị:

Shop ID
Shop Name
Shop Status

nếu backend có.

Không fake Shop Status.

25. BUSINESS TYPE

Nếu backend có:

Business Type

hiển thị đúng backend value.

Ví dụ:

INDIVIDUAL
HOUSEHOLD_BUSINESS
COMPANY

Không tạo business type mới.

26. KYC PROVIDER

Nếu backend contract có:

VNPT
FPT
MOCK

Admin chỉ monitor:

Provider
Provider Reference
Status

Không triển khai:

Manual Provider Verification
Provider Configuration UI
Provider Credential Management
Provider Retry Engine
Provider Algorithm
27. REFERENCE ID

Reference là tracking data.

Admin:

được đọc;
có thể copy nếu existing UI hỗ trợ;
không được sửa;
không được overwrite.
28. SUBMITTED AT / VERIFIED AT

Submitted At:

lấy từ backend;
format theo Admin UI;
không sửa từ frontend.

Verified At:

lấy từ backend;
nếu chưa có: N/A;
không tự tạo timestamp.
29. REJECTION REASON

Nếu rejected:

Rejection Reason

lấy đúng backend value.

Nếu không rejected:

N/A

Không tự tạo reason.

Không cho Admin sửa rejection reason trong Monitoring UI nếu contract không hỗ trợ mutation.

30. KYC MONITORING READ-ONLY

Mặc định:

READ-ONLY

Admin được:

list;
search;
filter;
detail;
monitor status.

Admin không được tự ý:

sửa KYC;
sửa provider;
sửa reference;
sửa verification result;
sửa verified timestamp;
sửa rejection reason.

Nếu backend có mutation:

Separate business contract

và không tự đưa vào Phase 3B.

31. KYC REJECTION ≠ ADMIN MANUAL VERIFICATION

Admin được:

VIEW

rejection reason.

Admin không mặc định có quyền:

VERIFY
REJECT
OVERRIDE

KYC.

Nếu backend có Admin override contract thì phải có API/security/business rule rõ ràng.

Nếu không:

OUT OF SCOPE
32. PII SECURITY

KYC có thể chứa:

Citizen ID
Tax Code
Bank Account
Address
Phone
Email
Identity Document

Không expose plaintext chỉ vì UI muốn hiển thị full object.

Ưu tiên:

Backend-protected data
        ↓
Admin UI
33. PII MASKING

Nếu backend trả masked data:

********1234

thì UI giữ nguyên masking.

Nếu backend chưa hỗ trợ masking:

không expose toàn bộ PII.

Không dùng:

DEPENDENCY = blocker

Thay:

PII Contract:
MISSING / PARTIAL

Phase 3B UI:
DO NOT EXPOSE PLAINTEXT

Integration:
INTEGRATION-READY

Nếu cần backend change để hoàn thiện masking:

External Integration Requirement:
KYC backend must provide protected PII representation.

Current implementation:
MISSING

Phase 3B:
IMPLEMENTED / INTEGRATION-READY
34. PII LOGGING

Không đưa plaintext PII vào:

console.log
System.out.println
frontend debug log
AuditLog
exception message

Không log toàn bộ KYC DTO nếu DTO chứa PII.

35. KYC DOCUMENT

Nếu contract có document metadata:

Document Type
Document Status
Document Reference

có thể hiển thị.

Nếu có document viewer/download URL từ backend:

chỉ render theo contract.

Không tạo upload/download pipeline mới.

Không copy document sang hệ thống khác.

36. KYC STATUS HISTORY

Nếu KYC backend có history:

Previous Status
New Status
Changed At
Source

có thể hiển thị nếu contract cung cấp.

Nếu history thực chất là AuditLog:

Phase 3B chỉ consume reference/summary nếu contract có.

Không xây:

AuditLog Query
AuditLog Viewer
AuditLog Filter

Các nội dung này thuộc Phase 3C.

37. SHOP LIFECYCLE RELATION

Kiểm tra bằng backend contract:

KYC
 ↓
Shop Lifecycle

Không tự tạo lifecycle.

Báo cáo:

Current lifecycle:
KYC → Shop relation:
Automatic transition:
Manual transition:
Source of truth:
Integration status:
38. KYC PROVIDER FAILURE

Nếu backend trả:

FAILED
TIMEOUT
UNAVAILABLE
ERROR

UI phải phân biệt nếu contract hỗ trợ.

Không tự biến:

PROVIDER ERROR

thành:

KYC_REJECTED

Phải phân biệt:

Verification Failure

và:

Business Rejection

nếu backend contract định nghĩa.

39. ERROR HANDLING

Phải xử lý:

400
401
403
404
409
500
400

Request/data không hợp lệ.

401

Yêu cầu authentication.

403
Không có quyền truy cập KYC.
404
KYC không tồn tại.
409

Hiển thị conflict theo mechanism hiện tại.

500
Không thể tải dữ liệu KYC.
Vui lòng thử lại.

Không hiển thị:

StackTrace
NullPointerException
MongoException
Internal class name
40. ADMIN SECURITY

Route:

/admin/kyc/**

chỉ cho:

ADMIN

Expected:

ADMIN       → ALLOW
MODERATOR   → DENY
VENDOR      → DENY
CUSTOMER    → DENY
ANONYMOUS   → DENY / LOGIN

Không chỉ hide menu/button.

Backend phải enforce authorization.

41. OBJECT-LEVEL SECURITY

Test:

KYC ID
Vendor ID
Shop ID

Backend phải validate:

Authentication
Authorization
Resource existence

Không được có IDOR.

Ví dụ:

/admin/kyc/{random-id}

không được trả dữ liệu nếu resource không tồn tại hoặc không được phép truy cập.

42. METHOD SECURITY

Nếu architecture hiện tại dùng:

@PreAuthorize(...)

phải kiểm tra method KYC.

Không tự thay đổi toàn bộ SecurityConfig.

Nếu cần thay đổi Security Core ngoài ownership:

External Security Integration Requirement

không coi đó là prerequisite cho việc implement UI/integration seam.

43. LOCKED ADMIN

Test:

ADMIN
  ↓
LOCKED
  ↓
/admin/kyc

Expected:

DENY

theo Security Core hiện tại.

Nếu Security Core chưa có behavior nhưng contract đã xác định:

Phase 3B:
implement/verify integration seam

Integration:
INTEGRATION-READY

Không dừng toàn bộ Phase 3B.

44. MODERATOR SECURITY REGRESSION

Test:

MODERATOR
   ↓
/admin/kyc
   ↓
DENY

Moderator vẫn phải truy cập được:

/moderator/**

theo architecture hiện tại.

Không để /admin/kyc làm thay đổi Moderator security.

45. ADMIN UI REUSE

Reuse:

Sidebar
Header
Table
Card
Filter
Pagination
Status Badge
Modal
Button
Typography
Spacing
Form
Empty State
Error State

Không tạo design system mới.

46. ADMIN LAYOUT

Flow:

Existing Admin Layout
        ↓
KYC Content

Không copy toàn bộ Admin sidebar.

47. KYC LIST UX

Phải có:

Page title
Filter
Search nếu contract hỗ trợ
Table
Pagination nếu contract hỗ trợ
Empty state
Loading state
Error state
Detail action
48. KYC DETAIL UX

Nên chia:

KYC Information
Vendor Information
Shop Information
Verification Information
Provider Information
Rejection Information

Reuse Card/Section component hiện tại.

49. KYC DATA CONTRACT

Không tạo production:

fakeKyc
mockApproved
mockRejected

Nếu external backend chưa có:

Production:
NO FAKE DATA

Tests:
TEST FIXTURE / MOCK ALLOWED

Integration:
INTEGRATION-READY
50. BACKEND BOUNDARY
Phase 3B ownership
Admin KYC List UI
Admin KYC Detail UI
Admin KYC Filter
Admin KYC Search
Admin KYC Pagination
Admin KYC API Integration
Admin KYC ViewModel/Adapter
Admin KYC Security Integration
Admin KYC Testing
Admin Regression
External KYC integration point
KYC Backend
KYC Provider
KYC Persistence
KYC Business Rules
KYC Status Source of Truth
PII Protection Backend
KYC → Shop Lifecycle Source of Truth

Các phần external trên không phải prerequisite để implement Admin layer.

51. KYC API INTEGRATION

Nếu backend API tồn tại:

Frontend/Administration layer consume API hiện tại.

Ví dụ nếu contract thực tế là:

GET /api/admin/kyc
GET /api/admin/kyc/{id}

thì dùng contract đó.

Không tạo duplicate API.

Nếu API chưa có:

Contract:
DEFINED / TO BE DEFINED

Integration Seam:
READY / MISSING

Admin Implementation:
IMPLEMENTED / PARTIAL

Integration Status:
INTEGRATION-READY

Không fake production API.

52. KYC PAGINATION CONTRACT

Nếu response có:

{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 100,
  "totalPages": 5
}

frontend phải consume đúng contract.

Không tự tính total từ số record đang hiển thị.

53. KYC FILTER CONTRACT

Nếu API hỗ trợ:

status
provider
businessType
vendor
shop
from
to
page
size

frontend truyền đúng parameter.

Không tự đổi tên.

Nếu một filter chưa có backend support:

Filter:
NOT AVAILABLE

Production:
DO NOT FAKE

Integration:
INTEGRATION-READY
54. DATABASE VERIFICATION

Khi actual backend/database tồn tại, verify:

KYC ID
Vendor
Shop
Status
Provider
Reference
SubmittedAt
VerifiedAt
RejectionReason

Nếu có PII encryption:

Encryption at rest

phải đúng backend contract.

Phase 3B không tự sửa persistence chỉ để UI hoạt động.

55. KHÔNG SỬA DATABASE TRỰC TIẾP TỪ UI

Admin KYC UI không được:

direct database update

Phải đi qua architecture hiện tại:

Controller
   ↓
Service
   ↓
Repository

hoặc:

Admin UI
   ↓
KYC API

Không gọi MongoDB/database trực tiếp từ browser.

56. CONTRACT TEST

Nếu external backend chưa có implementation thật, phải test:

Admin KYC Adapter
        ↓
Expected KYC Contract

Test:

response shape;
status mapping;
pagination;
filter parameters;
error mapping;
null/optional fields;
PII representation.

Đây là contract test, không phải production fake.

57. END-TO-END TEST

Khi backend/provider thật đã tích hợp:

Vendor
   ↓
KYC Submit
   ↓
KYC Backend
   ↓
Provider
   ↓
KYC Status
   ↓
Admin Login
   ↓
/admin/kyc
   ↓
KYC Detail

Nếu external backend chưa tồn tại:

Actual E2E:
NOT INTEGRATED

Admin-owned tests:
PASS / FAIL

Integration:
INTEGRATION-READY

Không ghi E2E PASS bằng mock rồi gọi đó là production integration.

58. TEST PENDING KYC

Khi backend contract hỗ trợ:

PENDING

Admin kiểm tra:

KYC xuất hiện
Status = backend value
Verified At = N/A nếu backend chưa có

Không tự tạo PENDING record trong production.

59. TEST APPROVED/VERIFIED KYC

KYC có trạng thái backend tương ứng:

VERIFIED / APPROVED

Admin kiểm tra:

Status
Provider
Reference
Verified At

Không có mutation từ Monitoring UI.

60. TEST REJECTED KYC

KYC rejected:

Status
Rejection Reason
Provider
Reference

Expected:

Rejection Reason = backend value

Không fake reason.

61. TEST SUSPENDED KYC

Nếu backend hỗ trợ:

SUSPENDED

hiển thị đúng status.

Không suy diễn:

KYC SUSPENDED = SHOP SUSPENDED

nếu backend không định nghĩa.

62. TEST PROVIDER

Nếu contract có provider:

VNPT
FPT
MOCK

test:

Provider
Reference
Status

Không test provider internals trong Phase 3B.

63. TEST FILTER

Test theo contract:

PENDING
APPROVED/VERIFIED
REJECTED
SUSPENDED

Expected:

đúng record;
không duplicate;
pagination đúng;
reset đúng.
64. TEST SEARCH

Nếu contract hỗ trợ:

Vendor
Shop
KYC ID
Reference

Expected:

Correct records
No duplicate
No crash
No PII leakage
65. TEST PAGINATION

Test:

Page 1
Page 2
Last Page
Empty Page

Expected:

total đúng;
current page đúng;
không duplicate;
filter giữ đúng;
không crash.
66. TEST PII

Kiểm tra:

Browser UI
Network response
Console log
Server log

nếu môi trường cho phép.

Không expose plaintext:

Citizen ID
Tax Code
Bank Account
Identity Document

ngoài requirement/contract được phép.

67. TEST KYC NOT FOUND

Mở:

/admin/kyc/{invalid-id}

Expected:

404

UI:

KYC không tồn tại.

Không crash.

68. TEST ACCESS CONTROL
ADMIN       → ALLOW
MODERATOR   → DENY
VENDOR      → DENY
CUSTOMER    → DENY
ANONYMOUS   → DENY / LOGIN
LOCKED ADMIN → DENY theo Security Core

Kiểm tra cả:

UI route
API endpoint
service/method authorization nếu có
69. CONCURRENCY / STALE DATA

Phase 3B mặc định read-only.

Test stale data:

Admin mở KYC Detail
        ↓
Backend/provider thay đổi status
        ↓
Admin refresh

Expected:

UI lấy status mới từ backend.

Không giữ stale frontend state ngoài architecture cache policy.

70. REFRESH

Nếu có refresh button:

Refresh
   ↓
Backend request

Không chỉ update local state.

Nếu không có refresh button:

browser refresh phải load data mới theo backend.

71. ERROR STATE

List:

Không thể tải dữ liệu KYC.
Vui lòng thử lại.

Detail:

Không thể tải thông tin KYC.
Vui lòng thử lại.

Không hiển thị internal exception.

72. EMPTY STATE

Không có dữ liệu:

Không có hồ sơ KYC.

Filter không có kết quả:

Không tìm thấy hồ sơ KYC phù hợp.

Phân biệt:

No Data

và:

Error
73. REGRESSION ADMIN

Sau Phase 3B test:

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
Phase 3A Enforcement

Đảm bảo KYC không phá Admin hiện tại.

74. REGRESSION MODERATOR

Test:

Moderator Login
Moderator Dashboard
Product Moderation
Review Moderation
ReportCase
Escalation
Moderator History

Đặc biệt:

Moderator → /admin/kyc = DENY
75. FILE SCOPE

Chỉ thay đổi file cần thiết cho:

Admin KYC
KYC Integration
KYC Security Integration
KYC Tests
Admin Regression Tests

Nếu sửa ngoài scope:

File:
Reason:
Impact:
Existing Owner:
Integration Reason:

Không refactor toàn bộ Admin.

76. OWNERSHIP & INTEGRATION MAP
Hạng mục	Phase 3B implementation	External integration
Admin KYC List UI	Hoàn	KYC contract
Admin KYC Detail UI	Hoàn	KYC contract
KYC Filter UI	Hoàn	KYC filter contract
KYC Search UI	Hoàn	KYC search contract
KYC Pagination UI	Hoàn	KYC pagination contract
KYC API Integration	Hoàn	KYC API
KYC Backend	Không implement lại	Integration point
KYC Provider	Không implement lại	Integration point
KYC Persistence	Không implement lại	Integration point
KYC Status source	Consume	KYC backend
PII protection backend	Không implement lại	KYC backend/security contract
Shop Lifecycle	Consume relation	Existing backend contract
Security Core	Consume existing security	Security Core
Authentication	Consume	Existing Auth
Authorization	Integrate/test	Existing Security
Method Security	Integrate/test	Existing Security
Vendor KYC UI	Out of scope	External
Shop Lifecycle implementation	Out of scope	External

Điểm quan trọng: cột external integration không phải blocker.

77. MASTER TABLE PHASE 3B
#	Nghiệp vụ	Contract	UI	Security	DB	Test	Implementation Status	Integration Status	Integration Seam	Notes
1	KYC Route	?	?	?	N/A	?	?	?	?	
2	KYC List	?	?	?	?	?	?	?	?	
3	KYC Detail	?	?	?	?	?	?	?	?	
4	KYC Status	?	?	?	?	?	?	?	?	
5	KYC Search	?	?	?	?	?	?	?	?	
6	KYC Filter	?	?	?	?	?	?	?	?	
7	KYC Pagination	?	?	?	?	?	?	?	?	
8	Provider	?	?	?	?	?	?	?	?	
9	Reference ID	?	?	?	?	?	?	?	?	
10	Submitted At	?	?	?	?	?	?	?	?	
11	Verified At	?	?	?	?	?	?	?	?	
12	Rejection Reason	?	?	?	?	?	?	?	?	
13	Business Type	?	?	?	?	?	?	?	?	
14	Vendor Info	?	?	?	?	?	?	?	?	
15	Shop Info	?	?	?	?	?	?	?	?	
16	Shop Lifecycle Relation	?	?	?	?	?	?	?	?	
17	PII Masking	?	?	?	?	?	?	?	?	
18	Admin Security	Existing contract	?	?	N/A	?	?	?	Security seam	
19	Moderator Deny	Existing contract	?	?	N/A	?	?	?	Security seam	
20	Locked Admin	Existing contract	?	?	N/A	?	?	?	Security seam	
21	KYC Contract Test	?	N/A	?	N/A	?	?	?	KYC seam	
22	KYC E2E	?	?	?	?	?	?	?	KYC seam	
23	Admin Regression	Existing	?	?	N/A	?	?	?	N/A	
24	Moderator Regression	Existing	?	?	N/A	?	?	?	Security seam	

Không tự điền DONE.

Chỉ cập nhật bằng kết quả kiểm tra thực tế.

78. PHÂN LOẠI STATUS

Không dùng DONE như trạng thái duy nhất.

IMPLEMENTED

Phần thuộc Phase 3B đã code đầy đủ và đã test độc lập theo contract.

INTEGRATION-READY

Phần Phase 3B đã hoàn thiện, nhưng external implementation chưa được connect/test thực tế.

Ví dụ:

Admin KYC UI: IMPLEMENTED
KYC Adapter: IMPLEMENTED
Contract Test: PASS
KYC Backend: MISSING
Integration: INTEGRATION-READY
INTEGRATED

External backend/provider thật đã connect và integration test pass.

PARTIAL

Phần Phase 3B của mình chưa hoàn chỉnh.

MISSING

Contract/component cần thiết thực sự chưa tồn tại và chưa có seam để consume.

WRONG

Implementation hiện tại tồn tại nhưng vi phạm contract/business/security.

OUT OF SCOPE

Không thuộc Phase 3B.

BLOCKED

Chỉ dùng khi có hard blocker thực tế, ví dụ:

Repository cannot build because baseline is broken.
Required environment cannot start.
Required infrastructure is unavailable.

Không dùng BLOCKED chỉ vì:

Anh Tuấn chưa code KYC backend.
79. TEST CHECKLIST
A. KYC Access
[ ] Admin Login
[ ] /admin/kyc
[ ] Admin Access
[ ] Moderator Deny
[ ] Vendor Deny
[ ] Customer Deny
[ ] Anonymous Deny
[ ] Locked Admin
B. KYC List
[ ] KYC List
[ ] Empty State
[ ] Loading
[ ] Error
[ ] Status
[ ] Vendor
[ ] Shop
[ ] Business Type
[ ] Provider
[ ] Reference
[ ] Submitted At
[ ] Verified At
[ ] Rejection Reason
C. KYC Detail
[ ] Detail
[ ] Vendor
[ ] Shop
[ ] Business Type
[ ] Status
[ ] Provider
[ ] Reference
[ ] Submitted At
[ ] Verified At
[ ] Rejection Reason
[ ] Not Found
D. Filter / Search
[ ] Status
[ ] Provider
[ ] Business Type
[ ] Vendor
[ ] Shop
[ ] Date
[ ] Search
[ ] Multiple Filters
[ ] Reset Filter
[ ] Pagination
E. PII
[ ] Citizen ID
[ ] Tax Code
[ ] Bank Account
[ ] Identity Document
[ ] UI masking
[ ] Network response review
[ ] No plaintext logs
F. Lifecycle
[ ] KYC Pending
[ ] KYC Approved/Verified
[ ] KYC Rejected
[ ] KYC Suspended
[ ] KYC ≠ Shop Status
[ ] Lifecycle relation verified
80. END-TO-END TEST MATRIX
Pending
Vendor
 ↓
KYC Submit
 ↓
KYC Backend
 ↓
PENDING
 ↓
Admin
 ↓
KYC List
 ↓
KYC Detail

Expected:

KYC xuất hiện
Status = backend status
Provider đúng nếu có
Reference đúng nếu có
Submitted At đúng
Verified At = N/A nếu chưa verify

Nếu backend thật chưa tích hợp:

E2E Integration:
NOT INTEGRATED

Contract/UI:
TESTED
Approved / Verified
Vendor
 ↓
KYC
 ↓
Provider
 ↓
APPROVED / VERIFIED
 ↓
Admin Monitoring

Admin kiểm tra:

Status
Provider
Reference
Verified At

Không mutation.

Rejected
Vendor
 ↓
KYC
 ↓
Provider / Backend
 ↓
REJECTED
 ↓
Admin
 ↓
KYC Detail

Expected:

Status = backend value
Rejection Reason = backend value
Filter
Admin
 ↓
KYC List
 ↓
Status Filter
 ↓
Provider Filter
 ↓
Business Type Filter
 ↓
Search
 ↓
Pagination

Expected:

đúng records;
không duplicate;
filter giữ đúng;
reset hoạt động;
pagination đúng.
PII
Admin
 ↓
KYC Detail
 ↓
Sensitive Data

Expected:

MASKED / PROTECTED

theo contract.

Security
ADMIN       → ALLOW
MODERATOR   → DENY
VENDOR      → DENY
CUSTOMER    → DENY
ANONYMOUS   → DENY / LOGIN
LOCKED ADMIN → DENY
81. DATABASE VERIFICATION

Khi database/backend thật tồn tại, verify:

KYC ID
Vendor
Shop
Status
Provider
Reference
SubmittedAt
VerifiedAt
RejectionReason

Nếu PII có encryption:

Encryption / Masking

phải đúng contract.

Không chỉ kiểm tra UI.

Nếu backend chưa có:

DB Integration:
NOT INTEGRATED

không tạo fake database state trong production.

82. BUILD / TEST

Chạy:

mvn clean package

sau đó:

mvn test

và integration test phù hợp.

Ghi chính xác:

BUILD: PASS / FAIL / NOT RUN
UNIT TEST: PASS / FAIL / NOT RUN
CONTRACT TEST: PASS / FAIL / NOT RUN
INTEGRATION TEST: PASS / FAIL / NOT INTEGRATED
MANUAL TEST: PASS / FAIL / NOT RUN

Nếu FAIL:

Command:
Error:
Root Cause:
File:
Function:
Current Implementation:
Integration Point:
Owner:

Không ghi PASS nếu chưa chạy.

83. REGRESSION ADMIN
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
[ ] Phase 3A Enforcement
84. REGRESSION MODERATOR
[ ] Moderator Login
[ ] Dashboard
[ ] Product Moderation
[ ] Review Moderation
[ ] ReportCase
[ ] Escalation
[ ] Moderator History
[ ] Moderator → /admin/kyc = DENY
85. EXTERNAL INTEGRATION REPORT

Thay cho dependency report kiểu owner-blocking.

KYC Backend
Contract:
API:
Entity:
Status:
Persistence:
Provider:
PII:
Shop Lifecycle:
Current implementation:
Integration seam:
Integration status:
Security Core
Authentication:
Authorization:
Method Security:
Locked Account:
Current implementation:
Integration seam:
Integration status:
Các module khác

Chỉ ghi nếu Phase 3B thực sự consume contract.

Không ghi:

DEPENDENCY – <PERSON>

nếu thực tế phần đó không ngăn implementation.

86. FILE CHANGE REPORT

Báo cáo:

Created:
Modified:
Deleted:

Mỗi file quan trọng:

File:
Purpose:
Why changed:
Integration point:

Nếu không xóa:

Deleted:
NONE

Không ghi file không thực sự thay đổi.

87. SECURITY REPORT

Bắt buộc:

ADMIN → /admin/kyc:
MODERATOR → /admin/kyc:
VENDOR → /admin/kyc:
CUSTOMER → /admin/kyc:
ANONYMOUS → /admin/kyc:
LOCKED ADMIN:

Kết quả:

PASS / FAIL / NOT INTEGRATED

Nếu Security Core external chưa connect nhưng Admin-owned integration seam đã hoàn thiện:

Implementation:
IMPLEMENTED

Integration:
INTEGRATION-READY
88. KYC INTEGRATION REPORT

Bắt buộc báo cáo:

KYC Contract:
KYC API:
KYC Status Contract:
Provider Contract:
Reference Contract:
PII Contract:
Shop Lifecycle Contract:
Integration Seam:
External Implementation:

Ví dụ:

KYC Contract:
AVAILABLE

KYC API:
AVAILABLE

Admin Adapter:
IMPLEMENTED

Contract Test:
PASS

KYC Backend:
NOT CONNECTED

Final Integration Status:
INTEGRATION-READY

Đây là trạng thái hợp lệ của Phase 3B.

89. PHASE 3B FINAL REPORT

Dùng format:

# PHASE 3B RESULT

## 1. Git Baseline

Current branch:

HEAD:

Working tree:

Uncommitted changes:


## 2. KYC Contract

KYC Entity:

KYC Repository:

KYC Service:

KYC Controller:

KYC API:

KYC Provider:

KYC Status:

PII Protection:

Shop Lifecycle:


## 3. Implementation Status

IMPLEMENTED:

PARTIAL:

MISSING:

WRONG:

OUT OF SCOPE:


## 4. Integration Status

KYC Backend:

KYC Provider:

KYC API:

PII:

Shop Lifecycle:

Security:


## 5. KYC LIST

Route:

List:

Status:

Provider:

Reference:

Vendor:

Shop:

Business Type:

Pagination:

Implementation Status:

Integration Status:


## 6. KYC DETAIL

Detail:

Provider:

Reference:

Submitted At:

Verified At:

Rejection Reason:

Status:

Implementation Status:

Integration Status:


## 7. KYC FILTER / SEARCH

Status:

Provider:

Business Type:

Vendor:

Shop:

Date:

Search:

Pagination:

Status:


## 8. PII

Citizen ID:

Tax Code:

Bank Account:

Identity Document:

Masking:

Status:


## 9. SHOP LIFECYCLE

KYC → Shop:

Automatic transition:

Manual transition:

Source of Truth:

Status:


## 10. SECURITY

ADMIN:

MODERATOR:

VENDOR:

CUSTOMER:

ANONYMOUS:

LOCKED ADMIN:

Status:


## 11. DATABASE TEST

...


## 12. CONTRACT TEST

...


## 13. END-TO-END TEST

Pending:

Approved:

Rejected:

Suspended:

Filter:

PII:

Security:


## 14. REGRESSION TEST

Admin:

Moderator:


## 15. BUILD

PASS / FAIL / NOT RUN


## 16. UNIT TEST

PASS / FAIL / NOT RUN


## 17. CONTRACT TEST

PASS / FAIL / NOT RUN


## 18. INTEGRATION TEST

PASS / FAIL / NOT INTEGRATED


## 19. MANUAL TEST

PASS / FAIL / NOT RUN


## 20. FILES CHANGED

Created:

Modified:

Deleted:


## 21. KNOWN ISSUES

...


## 22. HARD BLOCKERS

...


## 23. EXTERNAL INTEGRATION POINTS

KYC Backend:

KYC Provider:

Security:

Shop Lifecycle:


## 24. PHASE 3C DEPENDENCY

...


## 25. FINAL STATUS

IMPLEMENTED

INTEGRATION-READY

hoặc

INTEGRATED

hoặc

PARTIAL
90. ĐIỀU KIỆN GHI IMPLEMENTED

Một chức năng Phase 3B được ghi:

IMPLEMENTED

khi phần thuộc Phase 3B đã hoàn thành, đã test độc lập theo contract/interface hiện tại.

Không bắt buộc external KYC backend phải được implement để đạt:

IMPLEMENTED

Ví dụ:

Admin KYC List UI        = IMPLEMENTED
Admin KYC Detail         = IMPLEMENTED
KYC Adapter              = IMPLEMENTED
Security Integration     = IMPLEMENTED
Contract Tests           = PASS
KYC Backend              = MISSING
Integration              = INTEGRATION-READY

Đây là trạng thái hợp lệ.

91. ĐIỀU KIỆN GHI INTEGRATED

Chỉ ghi:

INTEGRATED

khi:

KYC backend thật tồn tại;
contract khớp;
Admin integration đã connect;
API thật trả dữ liệu;
provider data thật được consume;
PII handling đúng;
security pass;
database verification pass;
E2E integration pass.

Không được dùng mock/test double để tuyên bố:

INTEGRATED
92. ĐIỀU KIỆN KẾT THÚC PHASE 3B
KYC Monitoring
Admin KYC route hoạt động.
KYC List hoạt động.
KYC Detail hoạt động.
KYC Status hiển thị đúng contract.
Vendor hiển thị đúng.
Shop hiển thị đúng.
Business Type hiển thị đúng.
Provider hiển thị đúng nếu contract hỗ trợ.
Reference ID hiển thị đúng.
Submitted At hiển thị đúng.
Verified At hiển thị đúng.
Rejection Reason hiển thị đúng.
Filter / Search
Status Filter hoạt động.
Provider Filter hoạt động nếu contract hỗ trợ.
Business Type Filter hoạt động nếu contract hỗ trợ.
Vendor/Shop Filter hoạt động nếu contract hỗ trợ.
Date Filter hoạt động nếu contract hỗ trợ.
Search hoạt động nếu contract hỗ trợ.
Pagination hoạt động nếu contract hỗ trợ.
Reset Filter hoạt động.
Security
ADMIN được phép.
MODERATOR bị deny.
VENDOR bị deny.
CUSTOMER bị deny.
Anonymous bị deny/login.
Locked Admin bị deny theo Security Core.
PII
PII không bị expose trái phép.
Citizen ID được xử lý đúng.
Tax Code được xử lý đúng.
Bank Account được xử lý đúng.
Identity Document được xử lý đúng.
Không log plaintext PII.
Lifecycle
KYC Status phân biệt Shop Status.
KYC → Shop relation lấy từ contract.
Không tự tạo lifecycle ngoài scope.
Testing
Pending KYC test.
Approved/Verified KYC test.
Rejected KYC test.
Suspended KYC test nếu contract hỗ trợ.
Filter test.
Search test nếu contract hỗ trợ.
Pagination test nếu contract hỗ trợ.
PII test.
Security test.
Database verification khi backend đã integrated.
Contract test.
KYC E2E khi backend/provider đã integrated.
Admin regression.
Moderator regression.
Build pass.
Test pass nếu test suite tồn tại.
File changes được báo cáo.
External integration points được báo cáo.
Known issues được báo cáo.
Phase 3C dependency được xác định.
93. MỤC TIÊU CUỐI CÙNG

Flow Phase 3B:

                    VENDOR
                       │
                       ↓
                  KYC SUBMIT
                       │
                       ↓
             KYC BACKEND / PROVIDER
                       │
                       ↓
                   KYC STATUS
                       │
                       ↓
              ┌─────────────────┐
              │  ADMIN MONITOR  │
              └────────┬────────┘
                       │
             ┌─────────┴─────────┐
             ↓                   ↓
         KYC LIST           KYC DETAIL
             │                   │
             ↓                   ↓
          FILTER              PROVIDER
          SEARCH              REFERENCE
        PAGINATION             STATUS
                              ↓
                       SHOP RELATION

Ranh giới:

KYC PROVIDER / BACKEND
= thực hiện verification

ADMIN
= monitoring / viewing / filtering

VENDOR
= submit KYC

SHOP
= operational lifecycle riêng

PII
= phải được bảo vệ

PHASE 3A
= Escalation + Enforcement + Audit Writer

PHASE 3B
= KYC Monitoring

PHASE 3C
= Violation + AuditLog Query
94. QUY TẮC QUAN TRỌNG NHẤT

Trong toàn bộ Phase 3B, áp dụng đúng câu:

“Tôi implement được phần của tôi bằng contract/interface hiện tại; backend của Tuấn là integration point, không phải prerequisite để bắt đầu hoặc hoàn thành implementation.”

Do đó:

Tuấn chưa code KYC Backend
        ↓
KHÔNG DỪNG Phase 3B
        ↓
Xác định KYC contract
        ↓
Xác định integration seam
        ↓
Implement Admin KYC Monitoring
        ↓
Test độc lập
        ↓
INTEGRATION-READY
        ↓
Khi KYC backend thật có
        ↓
Connect tại seam
        ↓
Integration test
        ↓
INTEGRATED

Không fake production KYC. Không duplicate KYC backend. Không chờ developer khác để bắt đầu hoặc hoàn thành phần của mình.

Phase 3B chỉ được kết luận INTEGRATED khi integration thật đã được kiểm tra. Nếu phần Admin đã hoàn chỉnh nhưng KYC backend chưa được nối, trạng thái hợp lệ là:

PHASE 3B
Implementation Status: IMPLEMENTED
Integration Status: INTEGRATION-READY

chứ không phải BLOCKED.