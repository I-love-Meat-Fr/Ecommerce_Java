PHASE 2A – PRODUCT MODERATION

Implementation instruction: Đọc Phase 2A này như single source of truth cho phạm vi triển khai Phase 2A.

Independent Implementation Principle:
“Tôi implement được phần của tôi bằng contract/interface hiện tại; backend của Tuấn là integration point, không phải prerequisite để bắt đầu hoặc hoàn thành implementation.”

Phase 2A phải có khả năng được triển khai, build, test và đánh giá độc lập với tiến độ của developer khác.

1. MỤC TIÊU

Implement hoàn chỉnh nghiệp vụ:

MODERATOR PRODUCT MODERATION
        +
PRODUCT QUEUE
        +
PRODUCT DETAIL
        +
AUTO MODERATION RESULT
        +
APPROVE
        +
REJECT
        +
ESCALATE
        +
MODERATION HISTORY
        +
SECURITY
        +
TEST

Phase 2A biến:

MODERATOR FOUNDATION

thành:

MODERATOR
    ↓
CÓ THỂ XỬ LÝ PRODUCT

Flow mục tiêu:

Vendor tạo Product
        ↓
Auto Moderation
        ↓
Auto Moderation Result
        ↓
PENDING_MANUAL
        ↓
Moderator Product Queue
        ↓
Product Detail
        ↓
Product
+
Auto Result
+
Flags
+
Evidence
+
Vendor/Shop Context
        ↓
┌─────────────┬─────────────┬─────────────┐
│   APPROVE   │   REJECT    │   ESCALATE  │
└─────────────┴─────────────┴─────────────┘
        ↓             ↓             ↓
    APPROVED      REJECTED      ESCALATED
        └─────────────┼─────────────┘
                      ↓
              History / Audit

Flow này phù hợp với mục tiêu cuối trong file gốc.

2. INDEPENDENT IMPLEMENTATION RULE
2.1 Không được block bởi developer khác

Không được sử dụng các trạng thái:

BLOCKED – Anh Tuấn chưa làm
BLOCKED – Quốc Anh chưa làm
WAIT FOR BACKEND
WAIT FOR BRANCH
WAIT FOR COMMIT
WAIT FOR MERGE
WAIT FOR PR

Thay vào đó:

External Capability
        ↓
Contract / Interface
        ↓
Integration Seam
        ↓
Implement phần Phase 2A
        ↓
Test độc lập
        ↓
INTEGRATION-READY
        ↓
Connect backend thật sau
3. PHÂN BIỆT DEPENDENCY VÀ INTEGRATION

Phase 2A có 3 loại quan hệ.

3.1 Existing Architecture Dependency

Ví dụ:

Phase 2A
    ↓
Existing Product Entity
Existing User
Existing Shop
Existing Authentication
Existing Security

Được phép reuse.

3.2 Integration Point

Ví dụ:

Phase 2A
    ↓
AutoModerationResult

Nếu Auto Moderation backend chưa tồn tại:

Current:
NOT AVAILABLE

Contract:
DEFINED

Integration Seam:
DEFINED

Phase 2A:
INTEGRATION-READY

Không block.

3.3 True Blocker

Chỉ được ghi BLOCKED khi:

Repository/build baseline bị lỗi nghiêm trọng
+
Phase 2A không thể implement/test độc lập
+
không có workaround hợp lệ

Không được dùng việc developer khác chưa hoàn thành code làm blocker.

4. SOURCE OF TRUTH

Trước khi code phải đọc:

Source code hiện tại.
Gate 0.
Phase 1 result.
File Phase 2A.
Bộ Luật & Chính sách Marketplace.

Đối chiếu:

Product Moderation
Prohibited Product
Restricted Product
Product Content
Counterfeit / IP
Misleading Product
Vendor Violation
Moderator Responsibility
Admin Escalation
Audit

Không tự tạo policy mới.

Không tự thay đổi policy để thuận tiện implementation.

File gốc cũng yêu cầu đối chiếu Product status, ModerationResult, policy/violation code, evidence, reject reason, moderator decision và history.

5. GIT SAFETY

Trước khi bắt đầu:

git status
git branch
git log --oneline --decorate -10

Ghi nhận:

Current Branch:
HEAD:
Working Tree:
Uncommitted Changes:

Bắt buộc:

KHÔNG reset
KHÔNG stash
KHÔNG checkout làm mất code
KHÔNG overwrite code người khác
KHÔNG merge code người khác
KHÔNG push

Chỉ sửa phần thuộc Phase 2A.

6. PHASE 1 INTEGRATION

Phase 2A sử dụng nền tảng Phase 1:

Moderator Login
Moderator Role
Moderator Security
Moderator Dashboard
Moderator Layout
Moderator Route
Account Status Guard

Không rebuild lại Phase 1.

Kiểm tra trước:

Moderator Login       → READY?
Moderator Security    → READY?
/moderator/**         → READY?
Account Status        → READY?

Nếu Phase 1 hoạt động:

Phase 2A:
READY TO IMPLEMENT

Nếu có lỗi:

Current:
...

Impact:
...

Workaround:
...

Status:
PARTIAL

Không mặc định BLOCKED.

7. EXISTING PRODUCT ARCHITECTURE

Trước khi tạo mới phải tìm:

Product
ProductRepository
ProductService
ProductController
ProductDTO
ProductStatus
Category
Shop
Vendor/User
ProductImage
ProductVariant
ProductSpecification

Nếu tồn tại:

REUSE

Không tạo:

ModeratorProduct
ModeratorProductEntity
DuplicateProductEntity

nếu không cần.

8. PRODUCT MODERATION CONTRACT

Phase 2A cần xác định contract giữa:

Product Core
        ↓
Auto Moderation
        ↓
Moderator Product Moderation

Contract tối thiểu:

ProductModerationContext

có thể chứa:

productId
productStatus
moderationStatus
autoModerationResult
autoModerationFlags
evidence
vendorContext
shopContext
violationContext nếu backend có
createdAt
updatedAt

Tên class/interface phải reuse architecture hiện tại nếu đã tồn tại.

Không tạo duplicate model nếu project đã có equivalent.

9. AUTO MODERATION – INTEGRATION POINT

Đây là thay đổi quan trọng nhất so với file cũ.

Phase 2A không implement Auto Moderation Engine.

Không tự viết:

Blacklist Engine
Duplicate Detection
Image Hash Engine
Price Anomaly Engine
Image Content Engine
Forbidden Category Engine

Các capability đó là external integration capability.

Phase 2A chỉ cần contract:

AutoModerationResult

Ví dụ:

AutoModerationResult
├── status
├── flags
├── severity nếu có
├── reason
├── evidence
└── checkedAt
10. AUTO MODERATION STATUS

Target flow:

PENDING_AUTO
      ↓
Auto Moderation
      ↓
AUTO_PASSED
AUTO_REJECTED
PENDING_MANUAL

Sau đó:

PENDING_MANUAL
      ↓
Moderator
      ↓
APPROVED / REJECTED / ESCALATED

Không tự tạo enum mới nếu project đã có enum tương đương.

Nếu source hiện tại khác:

Current Status:
...

Expected Status:
...

Mapping:
...

Compatibility:
...

Integration Status:
...

Không rename enum toàn project nếu không cần.

11. AUTO MODERATION CONTRACT STATUS

Nếu Auto Moderation backend đã tồn tại:

Current:
AVAILABLE

Action:
REUSE

Integration:
CONNECT

Nếu chưa tồn tại:

Current:
NOT AVAILABLE

Contract:
DEFINED

Phase 2A implementation:
READY

Production fake:
FORBIDDEN

Integration:
PENDING

Status:
INTEGRATION-READY

Không ghi:

BLOCKED – Anh Tuấn
12. PRODUCT MODERATION QUEUE

Route:

/moderator/queue

hoặc route tương đương hiện có.

Queue phải hiển thị Product cần Moderator xử lý.

Tối thiểu:

Product ID
Product Name
Shop
Vendor
Category
Price
Product Status
Moderation Status
Auto Moderation Result
Severity/Risk nếu backend cung cấp
Created At

Ví dụ:

ID | Product | Shop | Vendor | Status | Moderation | Auto Flag | Created

Không fake production data.

13. QUEUE DATA CONTRACT

Queue nên sử dụng:

ProductModerationQueueItem

hoặc DTO tương đương hiện tại.

Ví dụ:

productId
productName
shopName
vendorName
category
price
productStatus
moderationStatus
autoModerationStatus
flags
severity
createdAt

Nếu backend API đã tồn tại:

REUSE API

Nếu chưa:

DEFINE CONTRACT

Phase 2A có thể implement UI/controller boundary dựa trên contract mà không cần chờ Auto Moderation engine.

14. PRODUCT FILTER

Nếu backend support:

Status
Moderation Status
Category
Shop
Vendor
Auto Flag
Severity
Created Date

thì UI phải sử dụng API filter hiện tại.

Không tự filter business logic ở frontend nếu dữ liệu lớn hoặc filter yêu cầu business calculation.

Nếu backend chưa support một filter:

Filter:
Severity

Backend:
NOT AVAILABLE

Production calculation:
FORBIDDEN

Status:
INTEGRATION-READY

File gốc cũng yêu cầu không tự tạo business filter nếu backend không support.

15. PAGINATION

Pagination phải được thực hiện đúng layer.

Ưu tiên:

Backend Pagination
        ↓
UI

Ví dụ:

page
size
total
totalPages

Không load toàn bộ Product rồi paginate bằng frontend nếu architecture không thiết kế như vậy.

Test:

Page 1
Page 2
Last Page
Empty Page
Page Size
Filter + Pagination
Reset Filter
16. EMPTY STATE

Không có Product:

Không có Product cần xử lý

Không hiển thị fake rows.

17. PRODUCT DETAIL

Route ví dụ:

/moderator/products/{id}

hoặc route tương đương architecture hiện tại.

Product Detail phải hiển thị:

Product Information
Images
Variants
Specifications nếu có
Category
Shop
Vendor
Product Status
Moderation Status
Auto Moderation Result
Auto Moderation Flags
Evidence
Vendor/Shop Context
Violation Context nếu backend cung cấp
18. PRODUCT INFORMATION

Tối thiểu:

Product ID
Product Name
Description
Category
Price
Product Status
Moderation Status
Created At
Updated At

Không expose dữ liệu nhạy cảm không cần thiết.

19. PRODUCT IMAGES

Hiển thị image nếu Product backend có:

ProductImage
Image URL
Image metadata nếu cần

Không fake image.

Không tự chạy image moderation ở frontend.

Nếu image hash/content result từ Auto Moderation:

Backend Result
      ↓
Moderator UI
20. PRODUCT VARIANTS

Nếu Product có variants:

Variant
SKU
Price
Stock
Attributes

hiển thị theo architecture hiện tại.

Không tạo ProductVariant mới nếu entity đã tồn tại.

21. PRODUCT SPECIFICATIONS

Nếu Product có specification:

Specification
Key
Value

hiển thị.

Nếu không có:

Không có thông tin

Không tạo fake.

22. SHOP / VENDOR CONTEXT

Nếu backend có:

Vendor
Shop
Shop Status
Previous Product Violations
Previous Moderation Cases

thì hiển thị.

Mục tiêu:

Moderator
   ↓
Product Context
   +
Vendor Context

Không tự kết luận:

Vendor phải bị ban
Shop phải bị suspend

Chỉ hiển thị dữ liệu/decision context mà backend cung cấp.

File gốc cũng phân biệt rõ Violation Severity với Violation Action và không cho Phase 2A tự viết Violation Engine.

23. VIOLATION CONTEXT

Nếu backend đã cung cấp violation:

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

hiển thị context.

Severity:

LOW
MEDIUM
HIGH
CRITICAL

không phải action.

Action có thể là:

WARNING
PRODUCT_HIDDEN
PRODUCT_REJECTED
SHOP_RESTRICTED
SHOP_SUSPENDED
VENDOR_BANNED
FUNDS_FROZEN

Phase 2A:

READ CONTEXT

không:

CREATE VIOLATION ENGINE
24. AUTO MODERATION RESULT

Product Detail phải hiển thị kết quả backend:

AUTO_PASSED
AUTO_REJECTED
PENDING_MANUAL

và flags nếu có:

BLACKLIST
DUPLICATE
IMAGE
PRICE
CATEGORY
CONTENT
...

Không recompute kết quả ở frontend.

Ví dụ sai:

if product.name.includes("xxx")
    → flag

Không làm.

Frontend chỉ:

Backend Result
    ↓
Display
25. EVIDENCE

Nếu backend có evidence:

Evidence Type
Evidence Value
Evidence Source
Created At

hiển thị phù hợp.

Không tạo evidence giả.

26. PRODUCT APPROVE

Action:

POST /moderator/products/{id}/approve

hoặc endpoint hiện tại tương đương.

Flow:

Moderator
    ↓
Product Detail
    ↓
Approve
    ↓
Confirm
    ↓
Backend validation
    ↓
State transition
    ↓
Database
    ↓
History/Audit

Frontend không update DB trực tiếp.

27. APPROVE VALIDATION

Backend phải kiểm tra:

Authenticated?
Authorized?
Product exists?
Correct state?
Already processed?
Valid transition?

Ví dụ:

PENDING_MANUAL
        ↓
APPROVED

Allowed nếu policy/backend cho phép.

Không cho:

APPROVED
   ↓
APPROVED

hoặc:

REJECTED
   ↓
APPROVED

nếu transition không hợp lệ.

28. APPROVE UI

Khi click:

Approve

phải:

Confirmation
Loading
Disable duplicate click
Success/Error
Refresh state

Không double submit.

29. PRODUCT REJECT

Action:

POST /moderator/products/{id}/reject

Request:

reason

Reason bắt buộc.

30. REJECT REASON VALIDATION

Reject reason:

Required
Not empty
Not whitespace-only

Các case:

null       → DENY
""         → DENY
"   "      → DENY
"reason"   → ALLOW

Backend phải validate.

Frontend validation chỉ là UX.

31. REJECT FLOW
Moderator
   ↓
Reject
   ↓
Reason Modal
   ↓
Validate Reason
   ↓
Backend
   ↓
REJECTED
   ↓
Database
   ↓
History/Audit
32. PRODUCT ESCALATE

Action:

POST /moderator/products/{id}/escalate

Request:

reason

Reason bắt buộc.

Flow:

Moderator
    ↓
Escalate
    ↓
Reason
    ↓
Backend
    ↓
Escalation
    ↓
Admin xử lý
33. ESCALATE KHÔNG PHẢI ADMIN ENFORCEMENT

Moderator không được tự thực hiện:

SHOP_SUSPENDED
VENDOR_BANNED
FUNDS_FROZEN

nếu đó là quyền Admin.

Phase 2A chỉ:

Moderator
    ↓
Escalate
    ↓
Admin

Không implement Admin enforcement.

Đây là boundary được giữ nguyên từ file gốc.

34. STATE TRANSITION

Không để frontend tự quyết định state.

Backend phải là source of truth.

Ví dụ:

PENDING_MANUAL
 ├── APPROVE   → APPROVED
 ├── REJECT    → REJECTED
 └── ESCALATE  → ESCALATED / equivalent

Final states phải được kiểm tra theo enum/policy thực tế.

Nếu source có enum khác:

Current:
...

Expected:
...

Mapping:
...

Không phá backward compatibility.

35. FINAL STATE PROTECTION

Nếu Product đã final:

APPROVED
REJECTED

thì action không hợp lệ phải bị từ chối.

UI có thể disable button.

Nhưng backend vẫn phải enforce.

Ví dụ:

POST /approve

trực tiếp vẫn phải:

DENY

nếu state không cho phép.

36. OBJECT-LEVEL SECURITY

Không chỉ kiểm tra:

ROLE = MODERATOR

mà còn:

Product exists?
Product accessible?
Action valid?
Product state valid?

Ví dụ:

GET /moderator/products/999999

Expected:

404

hoặc behavior theo architecture.

Không được trả dữ liệu Product khác chỉ vì ID không tồn tại.

37. DIRECT API BYPASS

Test:

Customer → POST approve
Vendor → POST approve
Anonymous → POST approve
Moderator → invalid product
Moderator → final product

Tất cả phải được backend enforce.

Không dựa vào việc button không hiển thị trên UI.

38. ERROR HANDLING

Phải xử lý tối thiểu:

400
401
403
404
409
500

Ví dụ:

400

Invalid reason/request.

401

Unauthenticated.

403

Không có quyền.

404

Product không tồn tại.

409

State conflict/concurrency.

500

Unexpected server error.

Không hiển thị raw stack trace.

Không hiển thị raw JSON nếu project có centralized error handling.

Reuse error handling hiện tại.

39. CONCURRENCY

Phải test:

Moderator A → Product Detail
Moderator B → Product Detail

A → Approve
B → Reject

Expected:

Một state hợp lệ cuối cùng
+
Không silent overwrite

Nếu backend trả:

409 Conflict

UI phải thông báo.

File gốc yêu cầu kiểm tra concurrency và 409 conflict.

40. DOUBLE SUBMIT

Test:

Approve click × 2
Reject submit × 2
Escalate submit × 2

UI:

Loading
Disable button

Backend:

State validation
Idempotency nếu architecture hỗ trợ
41. MODERATION HISTORY

Route:

/moderator/history

hoặc route tương đương hiện tại.

History tối thiểu:

Action
Product
Moderator
Reason
Created At

Actions:

PRODUCT_APPROVED
PRODUCT_REJECTED
PRODUCT_ESCALATED

Nếu AuditLog backend cung cấp thêm:

Before
After
Severity
IP

thì hiển thị phù hợp.

Không tạo history giả ở frontend.

42. AUDIT INTEGRATION

AuditLog backend là:

INTEGRATION POINT

Không phải prerequisite để bắt đầu Phase 2A.

Contract:

AuditEvent
├── actor
├── role
├── action
├── resourceType
├── resourceId
├── reason
├── createdAt

Nếu backend AuditLog đã có:

CONNECT

Nếu chưa:

Contract:
DEFINED

Production fake:
FORBIDDEN

Phase 2A:
INTEGRATION-READY

Không ghi:

DEPENDENCY – Anh Tuấn
43. PRODUCT → MODERATOR INTEGRATION

Phase 2A không làm Product Core CRUD.

Nhưng phải xác định integration:

Vendor
   ↓
Create Product
   ↓
Existing Product Core
   ↓
Auto Moderation
   ↓
PENDING_MANUAL
   ↓
Moderator Queue

Nếu Product Core đã có:

REUSE

Nếu Product Core chưa đưa được vào queue:

Integration Point:
Product → Moderation

Current:
NOT AVAILABLE

Phase 2A:
Contract + Moderator side READY

Production fake:
FORBIDDEN

Status:
INTEGRATION-READY

Không fake Product trong production logic.

44. AUTO MODERATION INTEGRATION SEAM

Định nghĩa:

Product
    ↓
AutoModerationGateway / existing service
    ↓
AutoModerationResult
    ↓
ProductModerationQueue

Tên interface phải theo architecture thực tế nếu project đã có.

Nếu chưa có abstraction:

Tạo một seam tối thiểu

chỉ khi thực sự cần cho Phase 2A.

Không tạo abstraction framework quá mức.

45. CONTRACT EXAMPLE

Ví dụ contract conceptual:

interface AutoModerationResultProvider {
    AutoModerationResult getResult(ProductId productId);
}

và:

class AutoModerationResult {
    String status;
    List<AutoModerationFlag> flags;
    String reason;
    String severity;
    List<Evidence> evidence;
    Instant checkedAt;
}

Đây chỉ là contract mẫu.

Nếu project đã có:

AutoModerationService
ModerationResult
ModerationStatus

thì phải reuse.

Không tạo duplicate.

46. FALLBACK / TEST DOUBLE

Nếu backend của Tuấn chưa tồn tại:

Được phép dùng test double:

Unit Test
Integration Test
Contract Test

Ví dụ:

FakeAutoModerationResultProvider

nhưng chỉ trong test.

Không:

FakeAutoModerationService

trong production để giả lập Auto Moderation.

47. PHASE 2A KHÔNG ĐƯỢC IMPLEMENT

Không làm:

Review Moderation
Review Queue
Review Detail
Review Approve/Reject
Review Hide/Unhide

ReportCase Queue
ReportCase Detail
ReportCase Actions

Violation Engine
KYC
Payment
Order
Finance
Settlement
Payout
Refund

Và:

Auto Moderation Engine

nếu backend capability đó thuộc integration boundary.

File gốc cũng xác định các nhóm trên là out-of-scope.

48. ADMIN PRODUCT MANAGEMENT ≠ MODERATOR PRODUCT MODERATION

Bắt buộc phân biệt:

ADMIN PRODUCT MANAGEMENT
        ≠
MODERATOR PRODUCT MODERATION

Admin có thể có:

Product List
Search
Filter
Hide
Unhide
Delete

nhưng không có nghĩa đã có:

PENDING_MANUAL
Auto Moderation Result
Approve
Reject
Escalate
Moderation History

Không copy Admin Product page rồi đổi tên thành Moderator.

49. SECURITY CONTRACT

Phase 2A reuse Phase 1:

Actor	/moderator/**	/admin/**
ADMIN	Theo architecture	Allow
MODERATOR	Allow	Deny
VENDOR	Deny	Deny
CUSTOMER	Deny	Deny
Anonymous	Deny	Deny
Locked Moderator	Deny	—
Locked Admin	—	Deny theo architecture

Nếu behavior thực tế khác:

Current behavior:
...

Expected:
...

Gap:
...

Không tự đánh dấu PASS.

50. OBJECT SECURITY MATRIX
Case	Expected
Product ID hợp lệ + Moderator + valid state	Allow
Product ID không tồn tại	404 / architecture equivalent
Customer approve Product	403
Vendor approve Product	403
Anonymous approve Product	401/403
Moderator approve final Product	409/403 theo architecture
Moderator reject final Product	409/403
Moderator escalate invalid state	409/403
Direct API bypass	Deny
51. DATABASE VERIFICATION

Sau mỗi action phải kiểm tra DB.

Approve
Product status
Moderator
Timestamp
Reject
Product status
Reason
Moderator
Timestamp
Escalate
Escalation status/data
Reason
Moderator
Timestamp
History/Audit

Nếu model hỗ trợ:

Action
Actor
Role
Resource
Reason
CreatedAt

Không chỉ kiểm tra UI.

Các tiêu chí này được giữ từ file gốc.

52. END-TO-END – APPROVE

Phải test:

Vendor Product
      ↓
Auto Moderation
      ↓
PENDING_MANUAL
      ↓
Moderator Login
      ↓
Product Queue
      ↓
Product Detail
      ↓
View Auto Result
      ↓
Approve
      ↓
Database
      ↓
History / Audit

Expected:

Product = APPROVED
Nếu Auto Moderation backend chưa có

Không fake production.

Thay bằng:

Contract Test / Test Fixture

và ghi:

E2E external integration:
PENDING

Moderator-side implementation:
PASS

Status:
INTEGRATION-READY
53. END-TO-END – REJECT
Product
   ↓
PENDING_MANUAL
   ↓
Moderator
   ↓
Product Detail
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

Test thêm:

Reject + empty reason → DENY
Reject + spaces        → DENY
Reject + valid reason  → ALLOW
54. END-TO-END – ESCALATE
Product
   ↓
Moderator Queue
   ↓
Product Detail
   ↓
Escalate
   ↓
Reason
   ↓
Backend
   ↓
Escalation
   ↓
Admin

Expected:

Moderator không tự enforcement Admin.

Nếu Admin escalation backend chưa có:

Moderator-side escalation contract:
READY

Integration:
PENDING

Status:
INTEGRATION-READY

Không block.

55. CONCURRENCY TEST
Moderator A → Product Detail
Moderator B → Product Detail

A → Approve
B → Reject

Expected:

One valid final state
+
No silent overwrite

Nếu:

409 Conflict

UI phải báo:

Product đã được xử lý bởi Moderator khác.
Vui lòng tải lại dữ liệu.

hoặc message tương đương theo UI architecture.

56. REGRESSION ADMIN

Sau Phase 2A test:

Admin Login
Admin Dashboard
User Management
Lock / Unlock
Shop Management
Category
WEB Voucher
Banner
Admin Product
Admin Review

Moderator Product Moderation không được phá:

Admin Product Management
57. REGRESSION SECURITY

Test lại:

MODERATOR → /moderator/** = ALLOW
MODERATOR → /admin/**     = DENY
CUSTOMER  → /moderator/** = DENY
VENDOR    → /moderator/** = DENY
ANONYMOUS → /moderator/** = DENY
LOCKED MODERATOR          = DENY
58. TEST CHECKLIST
A. Moderator
[ ] Moderator Login
[ ] Moderator Security
[ ] Locked Moderator
[ ] Admin Regression
B. Product Queue
[ ] Product Queue
[ ] Empty State
[ ] Filter
[ ] Pagination
[ ] Reset Filter
C. Product Detail
[ ] Product Detail
[ ] Product Information
[ ] Images
[ ] Variants
[ ] Category
[ ] Shop
[ ] Vendor
[ ] Moderation Status
[ ] Auto Moderation Result
[ ] Auto Flags
[ ] Evidence
D. Product Action
[ ] Approve Product
[ ] Reject Product
[ ] Reject + Reason
[ ] Reject + Empty Reason
[ ] Reject + Spaces
[ ] Escalate Product
[ ] Escalate + Reason
[ ] Product final state cannot reprocess
[ ] Double Submit Protection
E. Security
[ ] Moderator → /moderator/** = ALLOW
[ ] Moderator → /admin/** = DENY
[ ] Customer → /moderator/** = DENY
[ ] Vendor → /moderator/** = DENY
[ ] Anonymous → /moderator/** = DENY
[ ] Locked Moderator = DENY
F. Object Security
[ ] Invalid Product ID
[ ] Unauthorized Product Action
[ ] Already Processed Product
[ ] State Transition Conflict
[ ] Direct API Bypass
[ ] 409 Conflict
G. Regression
[ ] Admin Login
[ ] Admin Dashboard
[ ] User
[ ] Shop
[ ] Category
[ ] WEB Voucher
[ ] Banner
[ ] Admin Product
[ ] Admin Review
59. BUILD

Chạy:

mvn clean package

Report:

Maven Build:
PASS / FAIL

Nếu fail:

Command:
Error:
Root Cause:
Affected Module:
Phase 2A caused? YES / NO / UNKNOWN
60. TEST

Chạy:

mvn test

Report:

Maven Test:
PASS / FAIL / NOT RUN

Application:

Startup:
PASS / FAIL

Manual:

Manual E2E:
PASS / FAIL
61. FILE CHANGE REPORT

Sau implementation:

Created:
- ...

Modified:
- ...

Deleted:
NONE

Với file quan trọng:

File:
Purpose:
Why changed:
Phase 2A responsibility:
Integration seam:

Chỉ ghi file thực tế đã thay đổi.

62. INTEGRATION STATUS REPORT

Không sử dụng:

Dependency – Anh Tuấn
Dependency – Quốc Anh
Blocked – Anh Tuấn

Sử dụng:

Auto Moderation
Integration Point:
Auto Moderation Result

Contract:
AutoModerationResult

Current Backend:
AVAILABLE / NOT AVAILABLE

Phase 2A:
Moderator-side integration IMPLEMENTED

Production Fake:
FORBIDDEN

Integration Status:
INTEGRATED / INTEGRATION-READY
Audit
Integration Point:
AuditLog

Contract:
AuditEvent

Current Backend:
AVAILABLE / NOT AVAILABLE

Phase 2A:
Action events defined/integrated

Production Fake:
FORBIDDEN

Status:
INTEGRATED / INTEGRATION-READY
Escalation
Integration Point:
Admin Escalation

Contract:
ProductEscalation

Current Backend:
AVAILABLE / NOT AVAILABLE

Phase 2A:
Escalation action implemented at Moderator boundary

Admin Enforcement:
OUT OF SCOPE

Status:
INTEGRATED / INTEGRATION-READY
63. STATUS DEFINITIONS
IMPLEMENTED

Phần thuộc Phase 2A đã code và test độc lập.

INTEGRATION-READY

Phần Phase 2A đã hoàn thành, nhưng external backend capability chưa kết nối.

INTEGRATED

External backend thật đã kết nối và test.

PARTIAL

Một phần scope Phase 2A còn thiếu.

MISSING

Chưa implement.

OUT OF SCOPE

Không thuộc Phase 2A.

BLOCKED

Chỉ dùng cho blocker kỹ thuật thật sự.

64. PHASE 2B / 2C KHÔNG PHẢI DEPENDENCY BLOCKER

Phase 2A không cần:

Review Moderation
ReportCase
Violation Engine

để hoàn thành Product Moderation.

Chỉ cần đảm bảo boundary rõ:

Phase 2A
    ↓
Product Moderation Contract

Sau đó:

Phase 2B
    ↓
Review Moderation Contract

và:

Phase 2C
    ↓
ReportCase Contract

Không coupling business logic giữa các phase.

65. FINAL REPORT TEMPLATE
# PHASE 2A RESULT

## 1. Git Baseline

Branch:
HEAD:
Working tree:
Uncommitted changes:

## 2. Phase 1 Integration

Moderator Login:
Moderator Dashboard:
Moderator Security:
Account Status:

Status:
IMPLEMENTED / PARTIAL

## 3. Product Moderation

Product Queue:
Product Filter:
Pagination:
Product Detail:
Images:
Variants:
Specifications:
Shop:
Vendor:

## 4. Auto Moderation Integration

Contract:
Current Backend:
Moderator Integration:
Status:

## 5. Product Actions

Approve:
Reject:
Reject Reason:
Escalate:
Escalation Reason:

## 6. State Transition

Current:
Expected:
Validation:
Status:

## 7. Security

Route Security:
Method Security:
Object Security:
API Bypass:
Account Status:

## 8. Database

Approve:
Reject:
Escalate:
History:
Audit:

## 9. Concurrency

Approve vs Reject:
409 Handling:
Double Submit:

## 10. Manual E2E

Approve:
Reject:
Escalate:

## 11. Regression

Admin:
Security:
Product Management:

## 12. Build

Maven Build:
PASS / FAIL

## 13. Test

Maven Test:
PASS / FAIL / NOT RUN

## 14. Files

Created:
Modified:
Deleted:

## 15. Integration Status

IMPLEMENTED:
INTEGRATION-READY:
INTEGRATED:
PARTIAL:
MISSING:
OUT OF SCOPE:

## 16. Known Issues

...

## 17. True Blockers

...

## 18. Final Status

IMPLEMENTED
+
INTEGRATION-READY

hoặc

PARTIAL
66. ĐIỀU KIỆN GHI IMPLEMENTED

Phase 2A có thể ghi:

IMPLEMENTED

khi phần thuộc quyền sở hữu Phase 2A đạt:

Product Queue.
Product Filter nếu contract/backend hỗ trợ.
Pagination.
Product Detail.
Images.
Variants.
Product/Shop/Vendor context.
Auto Moderation Result integration contract.
Auto Flags display.
Approve.
Reject.
Reject reason validation.
Escalate.
Escalation reason validation.
State transition validation.
Object-level security.
Error handling.
History integration.
Database verification.
Security test.
Concurrency test.
Admin regression.
Build.
Test.
Manual E2E.
Không fake production data.
Không overwrite code người khác.
Không triển khai Review/ReportCase.
Integration seams được ghi rõ.
Known issues được báo cáo.

Các tiêu chí này giữ lại các điều kiện kết thúc quan trọng của file gốc nhưng thay DEPENDENCY bằng trạng thái integration.

67. QUAN TRỌNG: KHI AUTO MODERATION CỦA TUẤN CHƯA XONG

Đây là trường hợp bạn quan tâm nhất.

Giả sử:

Auto Moderation Backend
= NOT IMPLEMENTED

Phase 2A không ghi:

BLOCKED – Anh Tuấn

Mà ghi:

Auto Moderation Backend:
NOT AVAILABLE

Contract:
DEFINED

Moderator Product Queue:
IMPLEMENTED

Auto Result UI:
IMPLEMENTED AGAINST CONTRACT

Test Double:
TEST ONLY

Production Fake:
FORBIDDEN

Integration:
PENDING

Phase 2A:
INTEGRATION-READY

Tức là bạn vẫn có thể hoàn thành:

Queue
Detail
Filter
Pagination
Approve
Reject
Escalate
Security
State validation
Error handling
History seam
Tests

mà không phải viết Auto Moderation Engine thay Tuấn.

68. KIẾN TRÚC ĐỘC LẬP CUỐI CÙNG
                    PRODUCT CORE
                         │
                         ▼
                AUTO MODERATION
                         │
                         │
                AutoModerationResult
                         │
                         ▼
              ┌────────────────────┐
              │ PRODUCT MODERATION │
              │      PHASE 2A      │
              └────────────────────┘
                         │
              ┌──────────┼──────────┐
              ▼          ▼          ▼
           Queue       Detail     Actions
              │          │       ┌──┼──┐
              │          │       ▼  ▼  ▼
              │          │    Approve
              │          │    Reject
              │          │    Escalate
              │          │
              └──────────┼──────────┘
                         ▼
                  State Transition
                         │
                         ▼
                    Database
                         │
                         ▼
                  History / Audit

External backend của Tuấn:

             ┌─────────────────────┐
             │ AUTO MODERATION     │
             │ REPORT/VIOLATION    │
             │ AUDIT CAPABILITY    │
             └──────────┬──────────┘
                        │
                  Contract / API
                        │
                        ▼
                 INTEGRATION SEAM
                        │
                        ▼
                    PHASE 2A

Không phải:

Tuấn code xong
     ↓
bạn mới được code

mà là:

Contract
   ↓
Bạn implement phần Phase 2A
   ↓
Test
   ↓
INTEGRATION-READY
   ↓
Tuấn/backend thật kết nối sau
69. FINAL SCOPE BOUNDARY
Phase 2A sở hữu
PRODUCT MODERATION

Queue
Filter
Pagination
Detail
Images
Variants
Specifications
Shop/Vendor Context
Auto Result Display
Flags Display
Approve
Reject
Reject Reason
Escalate
Escalation Reason
State Validation
Object Security
Error Handling
History Integration
Audit Integration
Database Verification
Concurrency
UI
Security
Regression
Test
External integration
Auto Moderation Engine
Auto Check Engines
Violation Backend
AuditLog Backend
Admin Escalation Backend
Product → Auto Moderation pipeline
Không làm
Review Moderation
ReportCase
Violation Engine
KYC
Payment
Order
Finance
Settlement
Payout
Refund
Admin Enforcement
70. FINAL STATUS MODEL
                    PHASE 2A
                       │
                       ▼
              IMPLEMENT OWN SCOPE
                       │
                       ▼
                  TEST LOCALLY
                       │
             ┌─────────┴─────────┐
             ▼                   ▼
       Backend có sẵn       Backend chưa có
             │                   │
             ▼                   ▼
         INTEGRATED        INTEGRATION-READY
             │                   │
             └─────────┬─────────┘
                       ▼
                Phase 2A Complete
                       │
                       ▼
                  Phase 2B