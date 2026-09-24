HASE 4B – WEB VOUCHER HARDENING

Implementation instruction: Read this Phase 4B file as the single source of truth.

Core independence rule:

“Tôi implement được phần của tôi bằng contract/interface hiện tại; backend của Tuấn là integration point, không phải prerequisite để bắt đầu hoặc hoàn thành implementation.”

Phase 4B phải được triển khai độc lập theo ownership, không chờ developer khác, branch khác, commit khác, backend implementation khác hoặc merge request khác.

Nếu implementation bên ngoài chưa tồn tại:

Xác định contract/interface hiện tại.
Xác định integration seam.
Implement phần thuộc Phase 4B.
Test phần có thể test độc lập bằng mock/stub/fixture chỉ trong test.
Đánh dấu INTEGRATION-READY.
Không fake production behavior/data.
Khi backend thật xuất hiện, integrate tại đúng seam và chạy integration test.

External backend là integration point, không phải prerequisite để bắt đầu hoặc hoàn thành phần implementation thuộc Phase 4B.

First inspect only the files/classes directly relevant to this phase.

Do not scan the entire repository unless required to resolve a dependency.

Reuse existing architecture and do not re-explain unrelated code.

1. MỤC TIÊU

Phase 4B tập trung hardening WEB Voucher Management sau các phase trước.

Mục tiêu:

ADMIN
  ↓
WEB VOUCHER
  ↓
┌─────────────────────────────────────┐
│ List / Search / Filter              │
│ Create                              │
│ View / Detail                       │
│ Edit                                │
│ Activate                            │
│ Deactivate                          │
│ Delete nếu business rule cho phép   │
│ Validation                          │
│ Lifecycle / State Transition        │
│ Security                            │
│ Concurrency / Double Submit         │
│ Error Handling                      │
│ Audit Integration                   │
└─────────────────────────────────────┘
        ↓
Database Verification
        ↓
Regression Test
        ↓
Build
        ↓
Report

Phase 4B phải:

CODE
+
TEST
+
VERIFY DATABASE
+
SECURITY TEST
+
REGRESSION
+
REPORT

Không chỉ audit.

Đồng thời phải phân biệt rõ:

IMPLEMENTATION
≠
INTEGRATION

Ví dụ:

Admin Voucher UI + Controller + Service + Gateway
        ↓
IMPLEMENTED

Audit backend chưa expose contract
        ↓
INTEGRATION-READY

Không được ghi:
BLOCKED BY TUẤN
2. PHẠM VI PHASE 4B

Phase 4B chỉ tập trung vào:

WEB Voucher

Bao gồm:

Voucher List
Voucher Search
Voucher Filter
Voucher Detail
Voucher Create
Voucher Edit
Voucher Activate
Voucher Deactivate
Voucher Delete nếu được phép
Voucher Validation
Voucher Lifecycle
Voucher Security
Voucher Error Handling
Voucher Pagination
Voucher Audit Integration
Voucher Database Verification
Voucher Regression

Nếu cần sửa backend để hoàn thiện đúng WEB Voucher và backend đó thuộc ownership Phase 4B:

được phép sửa.

Nếu backend thuộc ownership khác:

KHÔNG coi là prerequisite.

Xác định:
- contract
- integration seam
- implementation status
- integration status

Sau đó tiếp tục implement phần Phase 4B có thể hoàn thành độc lập.

3. KHÔNG THUỘC PHASE 4B

TUYỆT ĐỐI không mở rộng Phase 4B thành Phase 4 tổng.

Không làm:

Admin Dashboard

Dashboard KPI

GMV

Platform Revenue

Vendor Sales

Vendor Payable

Refund

Banner Management

PR / Marketing CMS

Admin Product Management mới

Admin Review Management mới

KYC Monitoring

Violation Management

Escalation Management

AuditLog Query UI

Customer Voucher UI

Vendor / Shop Voucher Management

Payment Core

Commission Core

Settlement Core

Payout Core

Refund Core

Shipping Core

Cart Core

Order Core

Auto Moderation

ReportCase

Complaint

KYC Provider

Đặc biệt:

WEB Voucher
≠
SHOP Voucher

Không được biến Phase 4B thành Shop Voucher Management.

4. NGUYÊN TẮC ĐỘC LẬP BẮT BUỘC
4.1. Independent implementation

Phase này phải có khả năng được triển khai độc lập.

Không chờ:

developer khác
branch khác
commit khác
backend implementation khác
merge request khác
4.2. Contract-first

Nếu external implementation chưa tồn tại:

Existing Contract
       ↓
Integration Seam
       ↓
Phase 4B Implementation
       ↓
Independent Test
       ↓
INTEGRATION-READY
       ↓
Later Integration

Không làm:

External backend missing
       ↓
dừng toàn bộ Phase 4B
4.3. Không fake production behavior

Không được tạo production code kiểu:

return fakeVoucher();
return fakeAudit();
return fakeSuccess();
return List.of(mockVoucher);

để làm cho UI có vẻ hoàn thành.

Mock/stub/fixture chỉ được dùng trong:

unit test
integration test
test profile
contract test
4.4. Status vocabulary

Sử dụng:

IMPLEMENTED

Phần code thuộc Phase 4B đã hoàn thành và test độc lập.

INTEGRATION-READY

Contract/seam đã sẵn sàng nhưng external implementation chưa được kết nối.

INTEGRATED

External implementation thật đã được kết nối và test.

PARTIAL

Implementation thuộc Phase 4B còn thiếu.

MISSING

Required capability chưa tồn tại.

WRONG

Implementation hiện tại vi phạm contract/business rule/security/data integrity.

OUT OF SCOPE

Không thuộc Phase 4B.

BLOCKED

Chỉ dùng khi có hard environmental blocker thực sự, ví dụ repository/build environment không thể chạy do baseline infrastructure hỏng.

Không dùng:

BLOCKED BY TUẤN
BLOCKED BY QUỐC ANH
BLOCKED BY OWNER

chỉ vì implementation bên ngoài chưa xong.

5. GIT SAFETY

Trước khi code:

git status
git branch
git log --oneline --decorate -10

Ghi:

Current branch:
Current HEAD:
Working tree:
Uncommitted changes:

Nếu có thay đổi chưa commit:

TUYỆT ĐỐI KHÔNG RESET
TUYỆT ĐỐI KHÔNG STASH
TUYỆT ĐỐI KHÔNG XÓA

Phải bảo toàn code hiện tại.

Sau khi code:

git status
git diff --stat
git diff

Không:

commit
push
reset
stash
merge

nếu chưa được yêu cầu.

6. ĐỌC KẾT QUẢ CÁC PHASE TRƯỚC

Kiểm tra thực tế:

Phase 1
Security / Admin Foundation

Phase 2
Moderator / Marketplace moderation

Phase 3
Admin Escalation / Enforcement / KYC / Violation / Audit

Phase 4A
Dashboard Core

Đối với Voucher phải kiểm tra:

Voucher Entity
VoucherStatus
VoucherType
VoucherScope
VoucherController
VoucherService
VoucherRepository
VoucherDTO
Voucher validation
Voucher template
Voucher JavaScript
Voucher CSS
Security
AuditLog contract/integration
Order/Voucher integration contract

Không giả định:

Voucher backend đã hoàn chỉnh

chỉ vì UI đã tồn tại.

7. PHASE READINESS ≠ PHASE DEPENDENCY

Trước khi code, kiểm tra Phase 1–4A để xác định:

existing contract
existing route
existing service
existing DTO
existing security
existing voucher model
existing integration seam

Nhưng:

Phase trước chưa hoàn thành

không tự động có nghĩa:

Phase 4B BLOCKED

Chỉ cần xác định phần nào có thể reuse.

Ví dụ:

Voucher UI hiện có
+
Voucher Controller contract hiện có
+
Audit interface hiện có
+
Audit implementation chưa có

Kết quả:

Voucher implementation:
IMPLEMENTED

Audit integration:
INTEGRATION-READY

Phase 4B:
không bị block bởi Audit backend
8. DEPENDENCY / INTEGRATION READINESS AUDIT

Trước khi sửa phải xác định:

WEB Voucher backend
Admin Voucher UI
Security
Database
AuditLog
Order/Voucher usage integration

Nhưng sử dụng format:

External Integration:

Contract:

Integration Seam:

Current External Implementation:

Required by Phase 4B:

User-Owned Implementation:

Integration Status:

Blocking:
YES / NO

Ví dụ:

External Integration:
AuditLog

Contract:
Existing AuditService interface

Integration Seam:
VoucherService → AuditService

Current External Implementation:
Audit writer chưa hỗ trợ đầy đủ Voucher resource

Required by Phase 4B:
Audit event contract

User-Owned Implementation:
VoucherService gọi đúng contract

Integration Status:
INTEGRATION-READY

Blocking:
NO

Không ghi:

DEPENDENCY – Anh Tuấn
BLOCKED

nếu phần của Phase 4B vẫn implement được.

9. OWNERSHIP MODEL
9.1. User-owned Phase 4B

Phần thuộc Phase 4B có thể bao gồm:

Admin Voucher UI

WEB Voucher CRUD integration

Voucher list

Search

Filter

Pagination

Detail

Create

Edit

Activate

Deactivate

Delete integration

Validation integration

Lifecycle handling

Scope protection

Error handling

Double-submit protection

Admin route integration

Controller integration

Service integration

DTO integration

Repository integration nếu thuộc module

Voucher test

Security test integration

Audit integration seam

Regression test
9.2. External integration points

Các hệ thống bên ngoài Phase 4B có thể gồm:

Security Core

AuditLog Backend

Order/Voucher usage backend

Shop Voucher implementation

Payment/Order financial modules

Các module này là:

INTEGRATION POINT

không mặc định là:

PREREQUISITE
10. QUY TẮC CONTRACT / INTERFACE

Nếu project đã có:

VoucherService
VoucherRepository
VoucherController
AuditService
SecurityService
VoucherGateway

hoặc interface tương đương:

REUSE

Không tạo:

VoucherServiceV2
VoucherControllerV2
VoucherRepositoryV2
AuditServiceV2

chỉ để tránh code hiện tại.

Nếu không có integration seam cho external service:

Kiểm tra architecture hiện tại.
Xác định boundary phù hợp.
Chỉ tạo seam tối thiểu nếu thực sự cần.
Không tạo duplicate domain service.
Test seam độc lập.
Đánh dấu INTEGRATION-READY.
11. WEB VOUCHER VS SHOP VOUCHER

Bắt buộc:

WEB Voucher
≠
SHOP Voucher
WEB Voucher

Voucher do:

Platform / Admin

quản lý.

Admin có thể thực hiện các action được business rule cho phép:

List
View
Create
Edit
Activate
Deactivate
Delete
SHOP Voucher

Voucher thuộc:

Vendor / Shop

Phase 4B không được biến Admin WEB Voucher API thành Shop Voucher API.

12. VOUCHER OWNERSHIP

Phải xác định:

scope = WEB

hoặc field tương đương trong model hiện tại.

Không hard-code ownership chỉ bằng:

URL

Ví dụ:

/admin/vouchers

không đủ để chứng minh record là WEB Voucher.

Backend phải kiểm tra:

voucher scope/type

theo model hiện tại.

13. VOUCHER ENTITY

Đọc entity hiện tại.

Xác định field thực tế, ví dụ:

id
code
name
discountType
discountValue
minimumOrder
maximumDiscount
startAt
endAt
usageLimit
usedCount
status
scope/type
createdAt
updatedAt

Đây chỉ là ví dụ.

Nếu entity thực tế khác:

dùng tên hiện tại.

Không rename toàn project chỉ để đồng nhất naming.

14. VOUCHER STATUS

Kiểm tra enum/status thực tế.

Ví dụ:

ACTIVE
INACTIVE

hoặc:

DRAFT
ACTIVE
INACTIVE
EXPIRED

Không tự tạo enum mới nếu project đã có status model.

Bắt buộc phân biệt:

Status

và:

Time validity

Ví dụ:

ACTIVE + endAt đã qua

không được tự kết luận business state nếu backend chưa định nghĩa như vậy.

15. VOUCHER LIFECYCLE

Xác định transition thực tế:

CREATE
   ↓
ACTIVE / INACTIVE / DRAFT
   ↓
ACTIVATE
   ↓
ACTIVE
   ↓
DEACTIVATE
   ↓
INACTIVE

Chỉ cho phép transition phù hợp với backend/business rule hiện tại.

Không tự tạo lifecycle mới.

Nếu backend hỗ trợ idempotent action:

giữ behavior hiện tại.
16. LIST

Route sử dụng route thực tế hiện tại.

Ví dụ:

/admin/vouchers

Không đổi URL nếu không cần.

List tối thiểu phải hiển thị field thực tế:

Code
Name
Discount Type
Discount Value
Minimum Order
Maximum Discount
Start At
End At
Usage Limit
Used Count
Status
Actions

Không hiển thị field giả.

17. SEARCH

Nếu backend đã hỗ trợ:

Search by code
Search by name

phải truyền đúng parameter backend.

Không filter giả ở frontend đối với dataset lớn.

Flow:

Search UI
    ↓
Request Parameter
    ↓
Controller
    ↓
Service / Gateway
    ↓
Repository / External API
    ↓
Database / Backend
18. FILTER

Nếu model/business rule hỗ trợ:

Status
Discount Type
Start Date
End Date

thì reuse.

Không tự thêm filter phức tạp ngoài scope.

Nếu status hiện tại hỗ trợ:

ACTIVE
INACTIVE
EXPIRED
UPCOMING

thì map đúng.

Nếu không có:

không tự tạo status.
19. PAGINATION

Voucher list phải sử dụng pagination nếu backend đã hỗ trợ.

Không:

load toàn bộ voucher collection

chỉ để hiển thị một page.

Kiểm tra:

page
size
sort
total

Nếu repository/API đã có pagination:

REUSE

Nếu external implementation chưa có:

Phase 4B implement pagination contract/seam nếu thuộc ownership.

External implementation:
INTEGRATION-READY
20. SORTING

Nếu backend hiện tại hỗ trợ:

createdAt
updatedAt
startAt
endAt
code

giữ pattern hiện tại.

Không tự thay đổi default sort.

Nếu bổ sung sort:

sort phải được backend kiểm soát.

Không nhận field tùy ý từ client rồi đưa thẳng vào query nếu có rủi ro query abuse/injection.

21. DETAIL

Admin phải có thể xem detail nếu UI/backend contract hỗ trợ.

Detail có thể gồm:

Code
Name
Scope / Type
Discount
Minimum Order
Maximum Discount
Start At
End At
Usage Limit
Used Count
Status
Created At
Updated At

Chỉ hiển thị field thực tế.

Không expose:

secret
internal token
security credential
22. CREATE WEB VOUCHER

Admin có thể tạo WEB Voucher theo business rule.

Form phải sử dụng:

WEB Voucher scope/type

đúng contract.

Không cho Admin vô tình tạo:

SHOP Voucher

từ màn hình WEB Voucher.

23. CREATE VALIDATION

Tùy entity thực tế, kiểm tra các field bắt buộc:

Code
Name
Discount Type
Discount Value
Start At
End At

và các field khác theo contract.

Không hard-code field list nếu entity có contract khác.

24. CODE VALIDATION

Code phải:

Không rỗng
Không chỉ chứa whitespace
Đúng format nếu backend quy định
Không trùng nếu business rule yêu cầu unique

Nếu backend có unique constraint:

phải xử lý conflict.

Không chỉ check frontend.

25. DISCOUNT VALIDATION

Kiểm tra:

Discount Value > 0

nếu business rule yêu cầu.

Không cho:

negative discount
NaN
Infinity
invalid decimal

Nếu:

PERCENTAGE

thì range theo backend rule.

Nếu:

FIXED

thì không áp dụng validation percentage.

26. ORDER VALUE VALIDATION

Nếu có:

minimumOrder
maximumDiscount

kiểm tra:

không âm

và relationship theo business rule hiện tại.

Không tự thêm rule chưa tồn tại.

27. DATE VALIDATION

Tối thiểu:

Start At < End At

Không cho:

Start At > End At

Không tự động sửa End At.

Timezone phải theo architecture hiện tại.

28. USAGE LIMIT

Nếu có:

usageLimit

thì validation theo business rule hiện tại.

Nếu unlimited được hỗ trợ:

dùng representation hiện tại.

Không tự biến:

0

thành:

unlimited
29. DUPLICATE CODE

Test:

WEB Voucher A
Code = CNJ10

Create WEB Voucher B
Code = CNJ10

Expected:

Backend reject

nếu code phải unique.

UI phải hiển thị conflict rõ ràng.

Không tạo duplicate record.

30. EDIT

Admin có thể Edit theo business rule.

Flow:

Load current voucher
       ↓
Validate existence
       ↓
Validate WEB scope
       ↓
Validate current status
       ↓
Validate editable fields
       ↓
Update

Không update record không tồn tại.

31. IMMUTABLE FIELDS

Nếu business rule khóa field sau khi voucher đã được sử dụng:

không cho sửa.

Ví dụ có thể là:

Code
Discount
Usage rule

nhưng không được tự suy đoán.

Phải đọc:

backend contract
business rule
existing implementation

Nếu chưa có rule:

ghi rõ decision needed / integration gap

Không tự tạo production rule.

32. SCOPE PROTECTION

Không được dùng request edit WEB Voucher để đổi:

WEB → SHOP

nếu scope immutable.

Backend phải validate:

current scope
requested scope

Không tin:

hidden input

từ browser.

33. ACTIVATE

Flow:

Admin
  ↓
Confirm nếu cần
  ↓
Controller
  ↓
Service / Gateway
  ↓
Validate transition
  ↓
Database / external backend
  ↓
Audit integration
  ↓
UI refresh

Không chỉ:

JavaScript status = ACTIVE
34. ACTIVATE VALIDATION

Trước activate kiểm tra business rule hiện tại:

Voucher tồn tại
WEB scope
Không bị deleted
Transition hợp lệ

Nếu date/lifecycle có rule:

validate theo backend contract.

Không invent rule.

35. DEACTIVATE

Action:

ACTIVE
→
DEACTIVATE

Phải update backend thật.

Expected:

Database/backend status changed
UI status changed

Không chỉ reload page.

36. DELETE

Chỉ cho delete nếu business rule/backend cho phép.

Trước delete:

Confirmation Modal

Delete phải đi qua backend/service.

Không delete frontend-only.

37. DELETE USED VOUCHER

Kiểm tra:

Voucher đã được sử dụng?

Nếu backend không cho delete:

Backend reject

UI hiển thị message theo contract.

Không bypass bằng:

direct Mongo update
38. DELETE SCOPE PROTECTION

Delete endpoint phải xác nhận:

WEB Voucher

Không được delete:

SHOP Voucher

qua WEB Voucher API.

39. CONFIRMATION + DOUBLE SUBMIT

Các action:

Delete
Activate
Deactivate

nếu business rule yêu cầu confirmation thì phải có confirmation.

Các action:

Create
Edit
Activate
Deactivate
Delete

phải chống double submit.

Có thể dùng:

Disable button while submitting

hoặc mechanism hiện tại.

40. REASON

Nếu backend/business rule yêu cầu reason:

Deactivate
Delete

thì reason bắt buộc.

Nếu chưa có rule:

không tự thêm reason vào DB.
41. BACKEND VALIDATION

Frontend validation không đủ.

Backend phải kiểm tra:

Authentication
Authorization
Voucher existence
WEB scope
Field validation
Status transition
Business constraints
Duplicate code
Date validity
Usage constraints

Không tin dữ liệu từ browser.

42. SECURITY

Các route thực tế của WEB Voucher chỉ dành cho:

ADMIN

Test:

Anonymous
CUSTOMER
VENDOR
MODERATOR
ADMIN

Expected:

Anonymous  → DENIED
CUSTOMER   → DENIED
VENDOR     → DENIED
MODERATOR  → DENIED
ADMIN      → ALLOWED

Nếu project có ngoại lệ đã thống nhất:

ghi rõ contract.
43. METHOD SECURITY

Nếu project dùng:

@PreAuthorize

hoặc mechanism tương đương:

kiểm tra create/update/delete/activate/deactivate.

Không chỉ bảo vệ route.

Không tự redesign Security Core.

Nếu Security Core là external integration:

reuse contract
+
test integration seam
+
INTEGRATION-READY nếu implementation ngoài phase chưa có.
44. OBJECT-LEVEL SECURITY

Không chỉ kiểm tra:

role = ADMIN

mà phải kiểm tra:

voucher tồn tại
voucher thuộc WEB scope
action hợp lệ trên voucher

Không cho request WEB Voucher API tác động Shop Voucher.

45. LOCKED ADMIN

Test:

ADMIN active
→ Voucher Management
→ ALLOWED

và:

ADMIN locked
→ Voucher Management
→ DENIED

theo Security Core hiện tại.

Nếu Security Core chưa expose seam:

Integration Status:
INTEGRATION-READY

Blocking:
NO

trừ khi environment thực sự không thể test.

46. CSRF / SESSION / AUTH

Kiểm tra architecture hiện tại:

JWT
Session
Cookie
CSRF

Không tự đổi authentication architecture.

Mutation:

POST
PUT
PATCH
DELETE

phải được bảo vệ theo architecture hiện tại.

47. ERROR HANDLING

Voucher phải xử lý tối thiểu:

400 Bad Request
401 Unauthorized
403 Forbidden
404 Not Found
409 Conflict
500 Internal Server Error

UI không hiển thị:

stack trace
raw exception
internal implementation details
48. ERROR MESSAGE

Phân biệt:

Validation error
Unauthorized
Forbidden
Not found
Conflict
Server error

Không biến mọi lỗi thành:

Có lỗi xảy ra

nếu backend có message cụ thể.

Không expose:

MongoDB query
stack trace
internal path
secret
credential
49. CONCURRENCY

Test:

Admin A mở Voucher
Admin B mở cùng Voucher

Admin A deactivate

Admin B edit/activate

Kiểm tra:

Không silently overwrite state sai.

Nếu backend có:

version
updatedAt check
optimistic locking

reuse.

Nếu external backend chưa có:

Phase 4B không tự tạo locking architecture lớn ngoài scope.

Contract/seam:
được xác định nếu cần.

Status:
INTEGRATION-READY
50. STATE TRANSITION CONFLICT

Ví dụ:

Voucher đã bị deactivate

Admin gửi:

activate

Phải xử lý theo state hiện tại.

Nếu request không còn hợp lệ:

409 Conflict

hoặc response pattern hiện tại.

Không trả success nếu action thực tế không xảy ra.

51. DATABASE VERIFICATION

Sau các action phải verify:

Create
Edit
Activate
Deactivate
Delete

Kiểm tra:

Record exists
Field values correct
Scope correct
Status correct
Timestamp correct
Usage count not corrupted

Nếu external database chưa có trong environment:

Không fake DB verification.

Test bằng repository/service integration test nếu có thể.

Mark:
INTEGRATION-READY / NOT VERIFIED AGAINST EXTERNAL ENVIRONMENT
52. KHÔNG DIRECT DB TỪ UI

Flow bắt buộc:

UI
 ↓
Controller
 ↓
Service / Gateway
 ↓
Repository / External API
 ↓
Database

Business rule nằm ở backend.

53. USAGE COUNT SAFETY

Nếu Voucher có:

usedCount

Phase 4B không được làm hỏng counter.

Admin:

Activate
Deactivate
Edit

không được tự ý reset:

usedCount = 0

Nếu:

Edit Voucher
→ usedCount bị reset

đánh:

WRONG

và sửa nếu thuộc scope.

54. CREATE/EDIT KHÔNG LÀM HỎNG USAGE

Test:

Voucher usedCount = 5

Admin:

Edit

Expected:

usedCount vẫn = 5

trừ khi business rule hiện tại quy định khác.

55. DATE / TIMEZONE SAFETY

Kiểm tra:

startAt
endAt
createdAt
updatedAt

Không để:

off-by-one-day

do timezone conversion.

Xác định architecture hiện tại:

Browser timezone
Application timezone
Database timezone
Display timezone

Không tự đổi timezone toàn hệ thống.

56. MONETARY / DECIMAL SAFETY

Nếu discount dùng:

BigDecimal

phải giữ precision hiện tại.

Không:

BigDecimal → double

chỉ để render.

Không để floating-point rounding làm sai discount.

57. VOUCHER CODE NORMALIZATION

Kiểm tra behavior hiện tại:

ABC10
abc10
 ABC10
ABC10

Nếu business rule yêu cầu normalization:

dùng backend rule hiện tại.

Không tự đổi casing globally.

Nếu uniqueness case-insensitive:

backend phải enforce.
58. API CONTRACT

Kiểm tra:

Request DTO
Response DTO
HTTP status
Error response
Pagination response

Không để UI phụ thuộc trực tiếp vào Entity serialization nếu project đang sử dụng DTO.

59. DTO SAFETY

Không trả toàn bộ Entity nếu có field không cần expose.

Chỉ expose field cần cho Admin:

id
code
name
discount
status
dates
scope
usage

Không expose:

internal implementation details
security-sensitive fields
60. AUDIT INTEGRATION

Phase 4B:

KHÔNG xây AuditLog Query UI.

KHÔNG viết lại toàn bộ AuditLog Backend.

Nếu Phase 3 đã có AuditLog writer/service:

REUSE

Các action cần kiểm tra theo contract:

Create WEB Voucher
Edit WEB Voucher
Activate Voucher
Deactivate Voucher
Delete Voucher

Nếu Audit implementation chưa tồn tại:

Không fake AuditLog.

Implement đúng integration seam.

Status:
INTEGRATION-READY
61. AUDIT DATA

Nếu Audit contract hỗ trợ, kiểm tra:

actorId
actorEmail
actorRole
action
resourceType
resourceId
before
after
reason nếu có
IP nếu architecture hỗ trợ
createdAt

Không log:

password
JWT
secret
raw credentials

Before/after chỉ chứa field cần thiết.

62. AUDIT FAILURE

Nếu action fail:

Delete Voucher
→ backend reject

Expected:

Không tạo SUCCESS audit event

Tương tự:

Activate fail
Deactivate fail
Edit fail
Create fail

Không ghi success audit nếu action chưa thành công.

63. AUDIT DUPLICATION

Một action:

Activate Voucher

không được tạo:

2 hoặc 3 audit records

chỉ vì UI retry/controller gọi nhiều lần.

Reuse idempotency/audit pattern hiện tại nếu có.

64. ADMIN UI REUSE

Reuse:

Admin layout
Admin sidebar
Admin header
Admin table
Admin form
Admin modal
Admin button
Admin alert
Admin badge
Admin pagination

Không tạo:

New Admin Design System

Nếu template đã tồn tại:

minimal change
65. VOUCHER TABLE UX

Kiểm tra:

Readable columns
Status badge
Action buttons
Pagination
Empty state
Error state
Loading state

Không để:

overflow
button mất
column vỡ
66. VOUCHER FORM UX

Form phải:

Label rõ
Required field rõ
Validation message rõ
Date input đúng
Number input đúng
Submit state rõ
Cancel/back hoạt động

Không xóa dữ liệu user nhập khi backend trả validation error nếu không cần.

67. EMPTY STATE

Nếu không có voucher:

Không có WEB Voucher

hoặc message tương đương.

Không coi empty list là 500.

68. LOADING STATE

Trong request:

Create
Edit
Activate
Deactivate
Delete

UI phải có trạng thái đang xử lý.

Không cho click liên tục.

69. FILTER STATE

Khi filter/search:

status
keyword
date
type

request phải truyền đúng backend.

Nếu pagination:

filter change
→ page reset

nếu pattern hiện tại yêu cầu.

70. ROUTE CONSISTENCY

Không tạo route duplicate như:

/admin/voucher
/admin/vouchers
/admin/web-voucher
/admin/web-vouchers

nếu project đã có canonical route.

Xác định:

Canonical Route

và reuse.

71. API CONSISTENCY

Không tạo endpoint duplicate nếu canonical endpoint đã tồn tại.

Ưu tiên:

REUSE EXISTING CONTRACT

Nếu cần thay đổi:

Current:
Changed:
Reason:
Impact:
Compatibility:
72. SHOP VOUCHER REGRESSION

Phase 4B không làm Shop Voucher.

Nhưng phải kiểm tra mức regression cần thiết.

Expected:

WEB Voucher changes
≠
Shop Voucher behavior changes
73. ORDER / CHECKOUT REGRESSION

Nếu Voucher được sử dụng trong Order/Checkout:

Phase 4B không redesign Order.

Kiểm tra tối thiểu:

Voucher still resolvable
Voucher status respected
Expired voucher not treated as active
Inactive voucher not treated as active
Usage count not corrupted

Nếu external checkout implementation chưa available:

Contract:
Order/Voucher integration contract

Status:
INTEGRATION-READY

Không fake checkout success.
74. BUSINESS RULE CHECK

Bắt buộc:

[ ] WEB Voucher ≠ SHOP Voucher

[ ] Admin WEB Voucher chỉ xử lý WEB scope

[ ] Vendor không được CRUD WEB Voucher

[ ] Customer không được CRUD WEB Voucher

[ ] Moderator không được CRUD WEB Voucher

[ ] Voucher status dùng enum/model hiện tại

[ ] Không tự tạo status duplicate

[ ] Start At < End At

[ ] Discount validation đúng type

[ ] Duplicate code được backend xử lý

[ ] usedCount không bị reset

[ ] Activate cập nhật backend thật

[ ] Deactivate cập nhật backend thật

[ ] Delete tuân thủ business rule

[ ] UI không fake state

[ ] UI không direct DB

[ ] Failed action không tạo success audit

[ ] Không duplicate audit

[ ] Không expose secret/credential

[ ] Không phá Shop Voucher
75. TEST CASE – LIST
ADMIN
→ /admin/vouchers

Expected:

200 / page hiển thị
Danh sách đúng backend contract
76. TEST CASE – EMPTY LIST

Database không có WEB Voucher.

Expected:

Empty state
Không 500
77. TEST CASE – SEARCH

Tạo:

CNJ10
CNJ20
ABC10

Search:

CNJ

Expected:

CNJ10
CNJ20

theo search contract hiện tại.

78. TEST CASE – STATUS FILTER

Có:

ACTIVE
INACTIVE

Filter:

ACTIVE

Expected:

chỉ record ACTIVE
79. TEST CASE – PAGINATION

Có nhiều voucher.

Test:

page 0
page 1
page 2

Expected:

Không duplicate
Không missing ngoài backend contract
Total đúng
80. TEST CASE – CREATE SUCCESS
ADMIN
→ Add WEB Voucher
→ nhập dữ liệu hợp lệ
→ Save

Expected:

Create success
Database/backend có record
scope = WEB
UI hiển thị record

Nếu external backend chưa tồn tại:

Unit/contract test PASS
Production integration:
INTEGRATION-READY

Không giả database record trong production.

81. TEST CASE – CREATE INVALID CODE
Code = empty

Expected:

Validation error
Không tạo record
82. TEST CASE – DUPLICATE CODE
Code = existing code

Expected:

409 / business error
Không tạo duplicate
83. TEST CASE – INVALID DISCOUNT

Test:

negative
zero nếu không hợp lệ
invalid percentage
invalid decimal

Expected:

Reject

theo business rule hiện tại.

84. TEST CASE – INVALID DATE
Start At > End At

Expected:

Reject
85. TEST CASE – CREATE SHOP SCOPE FROM WEB UI

Request:

scope = SHOP

từ WEB Voucher endpoint.

Expected:

DENIED / VALIDATION ERROR

Không tạo Shop Voucher.

86. TEST CASE – EDIT SUCCESS
Admin
→ Edit WEB Voucher
→ sửa field hợp lệ
→ Save

Expected:

Database/backend updated
UI updated
87. TEST CASE – EDIT NOT FOUND

Request:

unknown voucher id

Expected:

404

hoặc error contract hiện tại.

Không tạo record ngoài ý muốn.

88. TEST CASE – EDIT SHOP VOUCHER

Dùng WEB Voucher endpoint với Shop Voucher ID.

Expected:

DENIED / NOT FOUND

theo security/data isolation contract.

Không update Shop Voucher.

89. TEST CASE – ACTIVATE
INACTIVE
→ Activate

Expected:

Backend status = ACTIVE
UI status = ACTIVE
90. TEST CASE – DEACTIVATE
ACTIVE
→ Deactivate

Expected:

Backend status = INACTIVE
UI status = INACTIVE
91. TEST CASE – INVALID TRANSITION

Nếu backend không cho:

ACTIVE → ACTIVE

Expected:

Conflict / validation

theo contract.

Không giả success.

92. TEST CASE – DELETE

Nếu business rule cho phép:

Delete
→ Confirm

Expected:

Record deleted

hoặc inactive/soft-delete theo actual model.

93. TEST CASE – DELETE USED VOUCHER

Voucher:

usedCount > 0

Nếu delete không được phép:

Backend reject
UI hiển thị đúng lỗi

Không direct DB delete.

94. TEST CASE – DOUBLE CLICK

Test:

double click Create
double click Activate
double click Deactivate
double click Delete

Expected:

Không duplicate action
95. TEST CASE – CONCURRENT UPDATE

Test:

Admin A
Admin B

cùng một voucher.

Expected:

Không silently overwrite state trái business rule.

Nếu external implementation chưa có concurrency contract:

Integration Status:
INTEGRATION-READY

Không đánh FAIL implementation nếu user-owned seam đã hoàn thành.
96. TEST CASE – AUDIT SUCCESS

Sau:

Create
Edit
Activate
Deactivate
Delete

kiểm tra AuditLog nếu contract hỗ trợ.

Expected:

Có đúng event
Actor đúng
Role đúng
Resource đúng
Resource ID đúng
Action đúng
Timestamp có
Before/After đúng nếu action yêu cầu

Nếu Audit writer chưa có:

Audit integration:
INTEGRATION-READY

Không fake event production.

97. TEST CASE – AUDIT FAILURE

Action fail:

duplicate code
invalid date
not found
forbidden
conflict

Expected:

Không có SUCCESS audit event
98. TEST CASE – SECURITY

Security matrix:

User	List	View	Create	Edit	Activate	Deactivate	Delete
Anonymous	DENIED	DENIED	DENIED	DENIED	DENIED	DENIED	DENIED
CUSTOMER	DENIED	DENIED	DENIED	DENIED	DENIED	DENIED	DENIED
VENDOR	DENIED	DENIED	DENIED	DENIED	DENIED	DENIED	DENIED
MODERATOR	DENIED	DENIED	DENIED	DENIED	DENIED	DENIED	DENIED
ADMIN	ALLOWED	ALLOWED	ALLOWED	ALLOWED	ALLOWED	ALLOWED	ALLOWED*

* Delete chỉ ALLOWED nếu business rule/backend cho phép.

99. TEST LOCKED ADMIN
ADMIN active
→ Voucher
→ ALLOWED
ADMIN locked
→ Voucher
→ DENIED

theo Security Core hiện tại.

100. TEST SHOP VOUCHER REGRESSION

Nếu Shop Voucher tồn tại:

Create
Edit
Apply
Status

test tối thiểu flow liên quan.

Expected:

WEB Voucher hardening không phá Shop Voucher.
101. TEST ORDER/VOUCHER REGRESSION

Nếu voucher được sử dụng trong checkout:

Active WEB Voucher
→ usable theo business rule

Inactive WEB Voucher
→ không usable

Expired WEB Voucher
→ không usable nếu business rule yêu cầu

Không sửa Order Core để workaround lỗi Voucher.

102. TEST DATABASE

Sau test verify:

WEB Voucher count
Voucher status
Voucher scope
Discount value
Dates
Usage limit
Used count

Không có:

duplicate code
corrupted record
wrong scope
unexpected status

Nếu DB external chưa available:

DB integration:
INTEGRATION-READY

và phải có unit/contract/repository test phù hợp.

103. TEST PHÂN TẦNG

Phase 4B phải phân biệt:

Independent tests

Có thể chạy không cần external implementation:

Service unit test
Controller test
Validation test
Security rule test
Gateway contract test
DTO test
UI/controller integration test
Integration tests

Cần external implementation thật:

Audit integration
Order/Voucher integration
External Security integration
External Voucher backend integration
Không được
Mock external backend
→
đánh dấu production integration PASS

Mock chỉ chứng minh:

Phase 4B implementation đúng contract.
104. BUILD

Chạy:

mvn test

và:

mvn clean package

Ghi:

TEST: PASS / FAIL

BUILD: PASS / FAIL

Nếu fail:

Error:
Root cause:
Affected file/module:
Owner:
Integration point:
Blocking:
105. GIT SAU KHI CODE

Chạy:

git status
git diff --stat
git diff

Kiểm tra:

Added
Modified
Deleted

Đảm bảo không có thay đổi ngoài scope.

106. FILE SCOPE

Có thể thay đổi:

Admin Voucher Controller

Admin Voucher Service

Voucher DTO

Voucher Repository

Voucher Entity

Voucher Enum

Admin Voucher Template

Admin Voucher JS

Admin Voucher CSS

Security integration

Audit integration

Voucher Tests

Chỉ thay đổi Entity/Enum khi thực sự cần.

Không sửa module ngoài scope chỉ để refactor.

107. KHÔNG OVERWRITE CODE NGƯỜI KHÁC

Nếu phát hiện code đang được người khác chỉnh:

Không xóa
Không replace toàn bộ file
Không reset

Chỉ sửa phần cần thiết và preserve changes hiện tại.

Nếu external implementation chưa có:

không tạo fake production implementation để lấp chỗ trống.
108. BUSINESS RULE KHÔNG ĐƯỢC TỰ ĐỔI

Giữ nguyên:

WEB Voucher
≠
SHOP Voucher
ADMIN
≠
VENDOR
ACTIVE
≠
EXPIRED

nếu backend model phân biệt.

Voucher status
≠
Voucher time validity

nếu business model phân biệt.

usedCount

không được tự reset.

Không tự thay đổi:

discount calculation
voucher applicability
order calculation
payment

nếu không thuộc Phase 4B.

109. WRONG DETECTION

Nếu phát hiện:

Admin WEB Voucher endpoint update được Shop Voucher

→ WRONG

Duplicate code vẫn tạo được

→ WRONG

Start At > End At vẫn save

→ WRONG

Activate chỉ đổi UI

→ WRONG

Deactivate chỉ đổi UI

→ WRONG

usedCount bị reset khi edit

→ WRONG

Failed action tạo success audit

→ WRONG

Vendor truy cập được /admin/vouchers

→ WRONG

110. BÁO CÁO IMPLEMENTATION STATUS

Báo cáo riêng:

Implementation Status:
IMPLEMENTED / PARTIAL / MISSING / WRONG

Không trộn với integration status.

Ví dụ:

Voucher UI:
IMPLEMENTED

Voucher CRUD contract:
IMPLEMENTED

Audit external integration:
INTEGRATION-READY

Order/Voucher integration:
INTEGRATION-READY
111. BÁO CÁO INTEGRATION STATUS

Mỗi integration phải ghi:

Integration:

Contract:

Integration Seam:

Current External Implementation:

User-Owned Part:

Integration Status:

Verified:

Remaining:

Ví dụ:

Integration:
AuditLog

Contract:
Existing AuditService

Integration Seam:
VoucherService → AuditService

Current External Implementation:
Audit writer chưa support đầy đủ Voucher event

User-Owned Part:
Voucher action gọi đúng AuditService contract

Integration Status:
INTEGRATION-READY

Verified:
Unit/contract test

Remaining:
Connect real Audit writer
112. BÁO CÁO – GIT BASELINE
## 1. Git Baseline

Current branch:

HEAD before:

HEAD after:

Working tree:

Uncommitted changes:
113. BÁO CÁO – DONE

Chỉ đánh [x] nếu phần đó đã thực sự implement/test.

## 2. DONE

### WEB Voucher

- [x] List
- [x] Search
- [x] Filter
- [x] Pagination
- [x] Detail
- [x] Create
- [x] Edit
- [x] Activate
- [x] Deactivate
- [x] Delete nếu business rule cho phép
- [x] Validation
- [x] Scope protection
- [x] Security
- [x] Error handling
- [x] Database verification
- [x] Audit integration seam
- [x] Regression

Không đánh [x] chỉ vì compile.

114. BÁO CÁO – PARTIAL
## 3. PARTIAL

Format:

Feature:

Completed:

Remaining:

File:

Reason:

Implementation Status:

Integration Status:

115. BÁO CÁO – MISSING
## 4. MISSING

Format:

Feature:

Expected:

Current:

Reason:

Required Contract:

Integration Seam:

Implementation Status:

Integration Status:
116. BÁO CÁO – WRONG
## 5. WRONG

Format:

Feature:

Current behavior:

Expected behavior:

File:

Impact:

Fixed:

Owner:

117. BÁO CÁO – EXTERNAL INTEGRATION
## 6. EXTERNAL INTEGRATION

Format:

Integration:

Contract:

Integration Seam:

External Implementation:

User-Owned Implementation:

Status:
INTEGRATION-READY / INTEGRATED

Verified:

Remaining:

Không ghi owner như một blocker.

Có thể ghi:

External Owner (informational):
...

nhưng owner không quyết định phase có được bắt đầu/kết thúc hay không.

118. BÁO CÁO – FILE CHANGES
## 7. FILES CHANGED

### Added

...

### Modified

...

### Deleted

...

Mỗi file quan trọng phải ghi:

Purpose:
119. BÁO CÁO – SECURITY
## 8. SECURITY RESULT

Anonymous:
PASS / FAIL / NOT INTEGRATED

Customer:
PASS / FAIL / NOT INTEGRATED

Vendor:
PASS / FAIL / NOT INTEGRATED

Moderator:
PASS / FAIL / NOT INTEGRATED

Admin:
PASS / FAIL / NOT INTEGRATED

Locked Admin:
PASS / FAIL / NOT INTEGRATED

Object-level WEB scope:
PASS / FAIL / NOT INTEGRATED

Không ghi PASS nếu chưa có evidence.

120. BÁO CÁO – TEST RESULT
## 9. TEST RESULT

1. Voucher List: PASS/FAIL/N/A
2. Search: PASS/FAIL/N/A
3. Filter: PASS/FAIL/N/A
4. Pagination: PASS/FAIL/N/A
5. Detail: PASS/FAIL/N/A
6. Create: PASS/FAIL/N/A
7. Create Validation: PASS/FAIL/N/A
8. Duplicate Code: PASS/FAIL/N/A
9. Edit: PASS/FAIL/N/A
10. Edit Validation: PASS/FAIL/N/A
11. Activate: PASS/FAIL/N/A
12. Deactivate: PASS/FAIL/N/A
13. Delete: PASS/FAIL/N/A
14. Shop Scope Protection: PASS/FAIL/N/A
15. Double Submit: PASS/FAIL/N/A
16. Concurrency: PASS/FAIL/INTEGRATION-READY
17. Audit Success: PASS/FAIL/INTEGRATION-READY
18. Audit Failure: PASS/FAIL/INTEGRATION-READY
19. Database Verification: PASS/FAIL/INTEGRATION-READY
20. Security: PASS/FAIL/INTEGRATION-READY
21. Locked Admin: PASS/FAIL/INTEGRATION-READY
22. Shop Voucher Regression: PASS/FAIL/N/A
23. Order/Voucher Regression: PASS/FAIL/N/A/INTEGRATION-READY
24. Maven Test: PASS/FAIL
25. Build: PASS/FAIL

Không được ghi PASS nếu chưa test.

121. BÁO CÁO – MANUAL TEST MATRIX
#	Test	Expected	Actual	Status
1	Admin Voucher List	Hiển thị đúng	?	?
2	Empty State	Không có voucher → empty state	?	?
3	Search	Kết quả đúng filter	?	?
4	Status Filter	Đúng status	?	?
5	Pagination	Đúng page/total	?	?
6	Create	Tạo WEB Voucher	?	?
7	Duplicate Code	Reject	?	?
8	Invalid Discount	Reject	?	?
9	Invalid Date	Reject	?	?
10	Edit	Update đúng	?	?
11	Activate	ACTIVE	?	?
12	Deactivate	INACTIVE	?	?
13	Delete	Delete/Reject đúng rule	?	?
14	Shop Scope via Web API	DENIED	?	?
15	Customer Access	DENIED	?	?
16	Vendor Access	DENIED	?	?
17	Moderator Access	DENIED	?	?
18	Locked Admin	DENIED	?	?
19	Double Click	Không duplicate	?	?
20	Audit Success	Có đúng audit / seam ready	?	?
21	Audit Failure	Không success audit	?	?
22	Database	Data đúng	?	?
23	Shop Voucher Regression	Không bị phá	?	?
24	Order/Voucher Regression	Không bị phá / integration-ready	?	?
25	Build	PASS	?	?
122. BÁO CÁO – BUSINESS RULE CHECK
[ ] WEB Voucher ≠ SHOP Voucher
[ ] WEB Voucher scope được backend enforce
[ ] Admin là role quản lý WEB Voucher theo contract
[ ] Moderator không được CRUD WEB Voucher
[ ] Vendor không được CRUD WEB Voucher
[ ] Customer không được CRUD WEB Voucher
[ ] Anonymous không được CRUD WEB Voucher
[ ] Voucher status dùng model hiện tại
[ ] Start At < End At
[ ] Discount validation đúng discount type
[ ] Duplicate code được backend enforce
[ ] usedCount không bị reset
[ ] Activate update backend thật
[ ] Deactivate update backend thật
[ ] Delete đúng business rule
[ ] UI không fake backend state
[ ] Không direct DB từ UI
[ ] Failed action không tạo success audit
[ ] Không duplicate audit
[ ] Không expose secret
[ ] Không phá Shop Voucher
[ ] Không phá Order/Voucher integration
123. BÁO CÁO – DATABASE
## 10. DATABASE VERIFICATION

Create:
PASS / FAIL / NOT INTEGRATED

Edit:
PASS / FAIL / NOT INTEGRATED

Activate:
PASS / FAIL / NOT INTEGRATED

Deactivate:
PASS / FAIL / NOT INTEGRATED

Delete:
PASS / FAIL / NOT INTEGRATED

Scope:
PASS / FAIL / NOT INTEGRATED

usedCount:
PASS / FAIL / NOT INTEGRATED

Duplicate code:
PASS / FAIL / NOT INTEGRATED

Nếu database thật chưa available:

Không fake PASS.
124. BÁO CÁO – AUDIT
## 11. AUDIT RESULT

Contract:

Integration Seam:

Create:

Edit:

Activate:

Deactivate:

Delete:

Failed Action:

Duplicate Event Protection:

Implementation Status:

Integration Status:

Evidence:

Remaining:
125. BÁO CÁO – REGRESSION
## 12. REGRESSION

Shop Voucher:
PASS / FAIL / N/A

Order/Voucher:
PASS / FAIL / N/A / INTEGRATION-READY

Existing Admin UI:
PASS / FAIL

Existing Voucher API:
PASS / FAIL

Security:
PASS / FAIL / INTEGRATION-READY
126. BÁO CÁO – BUILD
## 13. BUILD

Command:

mvn clean package

Result:

PASS / FAIL

Command:

mvn test

Result:

PASS / FAIL

If FAIL:

Error:

Root cause:

Affected module:

Integration point:

Blocking:
YES / NO
127. BÁO CÁO – KNOWN ISSUES
## 14. KNOWN ISSUES

Mỗi issue:

Issue:

Impact:

Implementation Status:

Integration Status:

Integration Seam:

Workaround:

Next action:


Không ghi owner như blocker nếu không phải hard blocker.

128. OUT OF SCOPE REPORT
## 15. OUT OF SCOPE

Admin Dashboard

Dashboard Financial KPI

Banner / PR

KYC Monitoring

Violation Management

Escalation Management

AuditLog Query UI

Shop Voucher Management

Customer Voucher UI

Vendor Voucher UI

Payment

Commission

Settlement

Payout

Refund Core

Shipping

Order Core

Auto Moderation

ReportCase

Complaint
129. PHÂN LOẠI KẾT QUẢ

Báo cáo bắt buộc:

DONE:

PARTIAL:

MISSING:

WRONG:

EXTERNAL INTEGRATION:

OUT OF SCOPE:

Không gom:

PARTIAL

thành:

DONE

Không gom:

INTEGRATION-READY

thành:

INTEGRATED
130. ĐIỀU KIỆN READY FOR PHASE 4C

Phase 4B được đánh:

READY FOR PHASE 4C

khi phần implementation thuộc Phase 4B đã hoàn thành, không còn user-owned P0/P1 unresolved, contract/seam cho external integration đã ổn định, và các test độc lập bắt buộc đã PASS.

Các điều kiện:

1. WEB Voucher List hoạt động theo contract
2. Search hoạt động nếu contract hỗ trợ
3. Filter hoạt động nếu contract hỗ trợ
4. Pagination hoạt động
5. Detail hoạt động nếu route/model có
6. Create implementation hoàn thành
7. Create validation hoàn thành
8. Duplicate code handling hoàn thành
9. Edit implementation hoàn thành
10. Activate implementation hoàn thành
11. Deactivate implementation hoàn thành
12. Delete implementation hoàn thành hoặc reject đúng business rule
13. WEB scope protection hoàn thành
14. Security integration seam hoàn thành
15. Locked Admin contract/seam được xử lý
16. Database/repository contract test PASS
17. Audit integration seam hoàn thành
18. Double submit được xử lý
19. Concurrency contract/seam được xác định nếu cần
20. Voucher/Order integration seam không bị phá
21. Maven test PASS
22. mvn clean package PASS
23. Không có P0/P1 thuộc implementation của Phase 4B
24. Git diff được kiểm tra
25. Final report đầy đủ

Nếu external backend chưa tồn tại nhưng:

contract đã rõ
seam đã hoàn thành
implementation của Phase 4B đã test độc lập

thì:

READY FOR PHASE 4C

vẫn có thể đạt.

Đồng thời ghi:

Integration Status:
INTEGRATION-READY

Không ghi BLOCKED.

131. PHASE STATUS MODEL

Phase status phải dùng:

Trường hợp 1 — User-owned implementation hoàn thành
PHASE STATUS:
IMPLEMENTED + INTEGRATION-READY

SEQUENCING:
READY FOR PHASE 4C
Trường hợp 2 — External integration đã hoàn thành
PHASE STATUS:
INTEGRATED

SEQUENCING:
READY FOR PHASE 4C
Trường hợp 3 — User-owned implementation còn thiếu
PHASE STATUS:
PARTIAL
Trường hợp 4 — Có hard environmental blocker
PHASE STATUS:
BLOCKED

và phải ghi rõ:

Real blocker:

Evidence:

Impact:

Recovery:

Không dùng BLOCKED cho external developer dependency thông thường.

132. FINAL REPORT FORMAT

Sau khi thực hiện Phase 4B, báo cáo đúng format:

# PHASE 4B – WEB VOUCHER HARDENING – FINAL REPORT

## 1. Git Baseline

Current branch:

HEAD before:

HEAD after:

Working tree:

Uncommitted changes:

## 2. Objective

...

## 3. DONE

...

## 4. PARTIAL

...

## 5. MISSING

...

## 6. WRONG

...

## 7. EXTERNAL INTEGRATION

...

## 8. OUT OF SCOPE

...

## 9. FILES CHANGED

### Added

...

### Modified

...

### Deleted

...

## 10. TEST RESULT

...

## 11. SECURITY RESULT

...

## 12. DATABASE VERIFICATION

...

## 13. AUDIT RESULT

...

## 14. BUSINESS RULE CHECK

...

## 15. REGRESSION

...

## 16. BUILD

mvn clean package:
PASS / FAIL

mvn test:
PASS / FAIL

## 17. KNOWN ISSUES

...

## 18. INTEGRATION STATUS

...

## 19. PHASE STATUS

IMPLEMENTED + INTEGRATION-READY

hoặc

INTEGRATED

hoặc

PARTIAL

hoặc

BLOCKED

## 20. SEQUENCING

READY FOR PHASE 4C / NOT READY

## 21. NEXT PHASE INTEGRATION SEAM

...
133. TUYỆT ĐỐI KHÔNG BÁO CÁO SAI

Không được ghi:

DONE

chỉ vì:

Voucher page mở được.

Không được ghi:

PASS

chỉ vì:

Maven compile thành công.

Không được ghi:

Voucher hoàn thành

nếu:

Create chỉ chạy frontend.

Không được ghi:

Activate PASS

nếu:

chỉ đổi badge trên UI.

Không được ghi:

Delete PASS

nếu:

backend reject nhưng UI vẫn báo success.

Không được ghi:

Security PASS

nếu chưa test:

Anonymous
Customer
Vendor
Moderator
Admin
Locked Admin

Không được ghi:

Audit PASS

nếu:

action fail vẫn tạo SUCCESS audit.

Không được ghi:

INTEGRATED

nếu chỉ mới:

mock external backend

Mock chỉ chứng minh:

contract/implementation behavior

không chứng minh production integration.

134. QUY TẮC KHI GẶP EXTERNAL INTEGRATION

Nếu gặp external backend chưa tồn tại:

Không fake
Không hard-code
Không bypass
Không sửa trực tiếp DB để workaround
Không overwrite code owner khác

Thực hiện:

1. Xác định contract.
2. Xác định integration seam.
3. Implement phần Phase 4B.
4. Test bằng test double nếu cần.
5. Mark INTEGRATION-READY.
6. Ghi external implementation còn thiếu.
7. Tiếp tục các phần Phase 4B không phụ thuộc implementation đó.

Ví dụ:

Integration:
Voucher Audit Writer

Contract:
AuditService

Integration Seam:
VoucherService → AuditService

Current External Implementation:
Chưa hỗ trợ đầy đủ Voucher resource

User-Owned Implementation:
VoucherService phát audit event theo contract

Status:
INTEGRATION-READY

Blocking:
NO

Câu nguyên tắc phải được hiểu như sau:

“Tôi implement được phần của tôi bằng contract/interface hiện tại; backend của Tuấn là integration point, không phải prerequisite để bắt đầu hoặc hoàn thành implementation.”

135. QUY TẮC KHI BACKEND ĐÃ CÓ SẴN

Nếu backend đã có:

VoucherController
VoucherService
VoucherRepository

thì:

REUSE

Không tạo:

VoucherControllerV2
VoucherServiceV2
VoucherRepositoryV2

Nếu API hiện tại sai nhưng sửa nhỏ trong scope:

sửa nhỏ

Nếu cần refactor lớn ngoài ownership:

không lấy đó làm lý do dừng toàn bộ Phase 4B.

Xác định integration seam.
Implement phần còn lại.
Mark integration-ready.
136. QUY TẮC KHI UI ĐÃ CÓ SẴN

Nếu Admin Voucher page đã có:

template
fragment
CSS
JS

thì:

REUSE

Không tạo:

voucher-new.html
voucher-v2.html
voucher-modern.html

chỉ để tránh đọc code cũ.

Ưu tiên:

minimal change
137. QUY TẮC DATABASE

TUYỆT ĐỐI không:

drop collection
delete all voucher
reset database
clear database
mass migration

Nếu cần test data:

seed có kiểm soát

và phải biết:

record nào được tạo
record nào được cleanup

Không xóa dữ liệu production.

138. QUY TẮC COMPATIBILITY

Phải giữ:

Existing URL
Existing Entity
Existing Repository
Existing Service
Existing DTO contract
Existing Template
Existing Security

nếu đang được sử dụng.

Nếu bắt buộc thay đổi:

Current:

Changed:

Reason:

Impact:

Compatibility:
139. MASTER IMPLEMENTATION / INTEGRATION MATRIX

Bắt buộc tạo matrix thực tế trong final report:

#	Capability	Existing Contract	Current Source/API	User-Owned Code	Security	DB	Test	Implementation	Integration	Integration Seam
1	List	?	?	?	?	?	?	?	?	?
2	Search	?	?	?	?	?	?	?	?	?
3	Filter	?	?	?	?	?	?	?	?	?
4	Pagination	?	?	?	?	?	?	?	?	?
5	Detail	?	?	?	?	?	?	?	?	?
6	Create	?	?	?	?	?	?	?	?	?
7	Edit	?	?	?	?	?	?	?	?	?
8	Activate	?	?	?	?	?	?	?	?	?
9	Deactivate	?	?	?	?	?	?	?	?	?
10	Delete	?	?	?	?	?	?	?	?	?
11	Validation	?	?	?	?	?	?	?	?	?
12	Scope Protection	?	?	?	?	?	?	?	?	?
13	Audit	?	?	?	?	?	?	?	?	?
14	Concurrency	?	?	?	?	?	?	?	?	?
15	Order/Voucher	?	?	?	?	?	?	?	?	?

Không được điền PASS nếu chưa có evidence.

140. MỤC TIÊU CUỐI CÙNG

Sau Phase 4B:

ADMIN
  ↓
WEB VOUCHER
  ├── List
  ├── Search
  ├── Filter
  ├── Pagination
  ├── Detail
  ├── Create
  ├── Edit
  ├── Activate
  ├── Deactivate
  └── Delete nếu business rule cho phép
       ↓
Validation
       ↓
Authorization
       ↓
Business Rule
       ↓
Database / Backend Contract
       ↓
Audit Integration Seam
       ↓
Regression

Đồng thời:

WEB Voucher
    ≠
SHOP Voucher

và:

Admin
    ≠
Moderator
    ≠
Vendor
    ≠
Customer
141. QUY TRÌNH THỰC HIỆN BẮT BUỘC
Trước khi code
1. Đọc code hiện tại.
2. Đọc Phase 1–4A output.
3. Kiểm tra Git.
4. Xác định Voucher architecture.
5. Xác định WEB vs SHOP scope.
6. Xác định status/lifecycle.
7. Xác định security.
8. Xác định Audit contract.
9. Xác định Order/Voucher contract.
10. Xác định integration seam.
11. Phân biệt implementation status và integration status.
Trong khi code
1. Reuse code hiện tại.
2. Minimal change.
3. Không redesign Admin.
4. Không làm Shop Voucher.
5. Không fake data.
6. Không bypass backend.
7. Không direct DB.
8. Không overwrite owner khác.
9. Không phá Order/Voucher integration.
10. Không thay đổi business rule chưa được thống nhất.
11. Implement theo contract hiện tại.
12. Test external boundary bằng test double nếu cần.
13. Mark INTEGRATION-READY khi external implementation chưa có.
Sau khi code
1. mvn test.
2. mvn clean package.
3. git status.
4. git diff --stat.
5. git diff.
6. Test List.
7. Test Search.
8. Test Filter.
9. Test Pagination.
10. Test Create.
11. Test Validation.
12. Test Duplicate.
13. Test Edit.
14. Test Activate.
15. Test Deactivate.
16. Test Delete.
17. Test WEB/SHOP isolation.
18. Test Security.
19. Test Locked Admin.
20. Test Audit contract/integration.
21. Verify Database.
22. Regression Shop Voucher.
23. Regression Order/Voucher.
24. Báo cáo DONE/PARTIAL/MISSING/WRONG/EXTERNAL INTEGRATION.
25. Liệt kê toàn bộ file thay đổi.
26. Kết luận Phase Status.
27. Kết luận READY FOR PHASE 4C hoặc NOT READY.
142. FINAL ACCEPTANCE CRITERIA

Phase 4B được coi là hoàn thành phần implementation thuộc phase khi:

[ ] WEB Voucher CRUD implementation hoạt động theo business rule

[ ] WEB Voucher không bị nhầm với Shop Voucher

[ ] Backend enforce WEB scope hoặc contract/seam đã được bảo vệ

[ ] Validation frontend hoạt động

[ ] Validation backend/contract được enforce

[ ] Duplicate code được xử lý

[ ] Date validation đúng

[ ] Discount validation đúng

[ ] Lifecycle transition đúng

[ ] Activate cập nhật backend thật hoặc contract integration-ready

[ ] Deactivate cập nhật backend thật hoặc contract integration-ready

[ ] Delete đúng business rule

[ ] usedCount không bị reset

[ ] Double submit được kiểm soát

[ ] Concurrency được xử lý hoặc seam/contract được ghi nhận

[ ] Admin authorization PASS hoặc security integration-ready theo evidence

[ ] Anonymous DENIED

[ ] Customer DENIED

[ ] Vendor DENIED

[ ] Moderator DENIED

[ ] Locked Admin contract được xử lý

[ ] Audit integration seam hoàn thành

[ ] Failed action không tạo SUCCESS audit

[ ] Database/repository contract test PASS

[ ] Shop Voucher regression PASS

[ ] Order/Voucher regression PASS hoặc INTEGRATION-READY

[ ] mvn test PASS

[ ] mvn clean package PASS

[ ] Không có P0/P1 thuộc implementation chưa xử lý

[ ] Git diff được kiểm tra

[ ] Final report đầy đủ

[ ] Không có chức năng ngoài scope bị thay đổi ngoài ý muốn

External integration chưa hoàn tất không tự động làm Phase 4B blocked nếu:

contract rõ
integration seam rõ
user-owned implementation hoàn thành
independent tests PASS
không fake production behavior
143. YÊU CẦU CUỐI CÙNG CHO CODING AGENT

Bạn là Coding Agent thực hiện:

PHASE 4B – WEB VOUCHER HARDENING

Không được hiểu Phase 4B là:

xây lại toàn bộ Voucher module

Mà phải hiểu là:

kiểm tra implementation hiện tại
        ↓
xác định gap
        ↓
xác định contract/integration seam
        ↓
sửa đúng gap
        ↓
hardening production behavior
        ↓
test độc lập
        ↓
verify database khi có thể
        ↓
verify security
        ↓
verify audit contract
        ↓
regression
        ↓
report
        ↓
integration-ready nếu external implementation chưa có

Ưu tiên:

Correctness
>
Business Rule
>
Security
>
Data Integrity
>
Compatibility
>
Minimal Change
>
UI Polish

Không được hy sinh:

security
data integrity
business rule

để đổi lấy:

UI đẹp hơn
code ngắn hơn
compile nhanh hơn
144. CÂU NGUYÊN TẮC BẮT BUỘC PHẢI GIỮ

Trong quá trình thực hiện Phase 4B, Coding Agent phải áp dụng nguyên tắc:

“Tôi implement được phần của tôi bằng contract/interface hiện tại; backend của Tuấn là integration point, không phải prerequisite để bắt đầu hoặc hoàn thành implementation.”

Ý nghĩa operational:

Backend của Tuấn chưa có
        ↓
Không dừng Phase 4B
        ↓
Không fake backend
        ↓
Xác định contract
        ↓
Xác định integration seam
        ↓
Implement phần của Phase 4B
        ↓
Test độc lập
        ↓
INTEGRATION-READY
        ↓
Backend thật xuất hiện
        ↓
Connect tại seam
        ↓
Integration Test
        ↓
INTEGRATED

Đây là nguyên tắc áp dụng cho toàn bộ external integration, không riêng AuditLog.

145. KHÔNG ĐƯỢC KẾT THÚC TASK BẰNG MỘT CÂU CHUNG CHUNG

Không được chỉ trả:

Đã hoàn thành Phase 4B.

Bắt buộc trả:

# PHASE 4B – WEB VOUCHER HARDENING – FINAL REPORT

DONE:

...

PARTIAL:

...

MISSING:

...

WRONG:

...

EXTERNAL INTEGRATION:

...

OUT OF SCOPE:

...

FILES CHANGED:

...

TEST RESULT:

...

SECURITY RESULT:

...

DATABASE RESULT:

...

AUDIT RESULT:

...

REGRESSION RESULT:

...

BUILD:

...

KNOWN ISSUES:

...

IMPLEMENTATION STATUS:

IMPLEMENTED / PARTIAL / ...

INTEGRATION STATUS:

INTEGRATED / INTEGRATION-READY / ...

PHASE STATUS:

IMPLEMENTED + INTEGRATION-READY
hoặc
INTEGRATED
hoặc
PARTIAL
hoặc
BLOCKED

SEQUENCING:

READY FOR PHASE 4C / NOT READY

Không được báo cáo PASS, DONE, INTEGRATED nếu chưa có bằng chứng thực tế tương ứng.

Không được báo cáo BLOCKED chỉ vì backend của developer khác chưa hoàn thành.