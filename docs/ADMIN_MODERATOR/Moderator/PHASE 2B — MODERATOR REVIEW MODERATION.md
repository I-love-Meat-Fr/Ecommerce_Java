PHASE 2B — MODERATOR REVIEW MODERATION

Implementation instruction: Read this Phase 2B specification as the single source of truth for Phase 2B.

Phase 2B phải có khả năng được implement, build, test và hoàn thiện độc lập với tiến độ của các developer khác.

Nguyên tắc cốt lõi:

Tôi implement được phần của tôi bằng contract/interface hiện tại; backend của Anh Tuấn là integration point, không phải prerequisite để bắt đầu hoặc hoàn thành implementation.

Không chờ developer khác, branch khác, commit khác hoặc backend implementation khác để bắt đầu hoặc hoàn thành phần thuộc Phase 2B.

Nếu backend bên ngoài Phase 2B chưa tồn tại:

Xác định contract cần tích hợp.
Xác định integration seam.
Implement phần Moderator thuộc Phase 2B.
Dùng interface/adapter để tách khỏi implementation cụ thể.
Dùng test double/mock/fake chỉ trong test nếu cần.
Không đưa fake production data/behavior vào production.
Test độc lập phần Phase 2B.
Đánh dấu INTEGRATION-READY.
Khi backend thật có, thay implementation adapter tại integration seam.

Không duplicate Review Backend, ReportCase Backend, Violation Backend hoặc Audit Backend chỉ để làm Phase 2B chạy.

1. MỤC TIÊU

Implement hoàn chỉnh nghiệp vụ:

Moderator xử lý Review

Phạm vi chính:

Review Moderation Queue
Review Detail
Review Approve
Review Reject
Review Hide
Review Unhide
Review Escalate
Report / Evidence nếu contract/backend cung cấp
Verified Purchase information nếu contract/backend cung cấp
Vendor / Product context
Moderator History / Audit integration
Security
Backend ↔ UI integration
Pagination
Filter
State transition
Concurrency
Regression test

Flow mục tiêu:

CUSTOMER
   ↓
REVIEW
   ↓
REVIEW MODERATION / REPORT
   ↓
MODERATOR QUEUE
   ↓
REVIEW DETAIL
   ↓
APPROVE / REJECT / HIDE / UNHIDE / ESCALATE
   ↓
HISTORY / AUDIT
   ↓
Nếu cần
   ↓
ADMIN

Mục tiêu cuối:

MODERATOR ROLE
      ↓
MODERATOR REVIEW QUEUE
      ↓
MODERATOR REVIEW DETAIL
      ↓
MODERATOR CÓ THỂ THỰC SỰ XỬ LÝ REVIEW
2. NGUỒN THAM CHIẾU

Trước implementation phải đọc:

1. Source code hiện tại
2. File PHASE 2B — REVIEW MODERATION
3. BỘ LUẬT & CHÍNH SÁCH SÀN MARKETPLACE
4. Kết quả GATE 0
5. Kết quả Phase 1
6. Contract/backend hiện có liên quan Review
7. Các integration contract hiện có

Policy là nguồn tham chiếu nghiệp vụ.

Không tự thêm nghiệp vụ Review trái Policy.

Nếu policy không quy định rõ:

Không tự suy đoán.
Không tự tạo rule mới.
Ghi nhận GAP.
3. NGUYÊN TẮC INDEPENDENT IMPLEMENTATION
3.1 Không phụ thuộc developer

Phase 2B không được có dependency dạng:

DEPENDENCY – Anh Tuấn
BLOCKED – Anh Tuấn
WAIT FOR TUẤN
WAIT FOR COMMIT
WAIT FOR BRANCH
WAIT FOR BACKEND

thay cho việc implementation.

Thay bằng:

Current State:
Contract:
Integration Seam:
Implementation Status:
Integration Status:
Missing External Implementation:
4. PHÂN BIỆT IMPLEMENTATION VÀ INTEGRATION

Phase 2B có hai lớp trạng thái:

Implementation
IMPLEMENTED
PARTIAL
MISSING
WRONG
OUT OF SCOPE
Integration
INTEGRATED
INTEGRATION-READY
INTEGRATION-PENDING

Ví dụ:

Review Queue UI:
IMPLEMENTED

Review Moderation Contract:
IMPLEMENTED

Review Backend thật:
NOT AVAILABLE

Integration:
INTEGRATION-READY

Trường hợp này không được đánh BLOCKED.

5. BACKEND CỦA ANH TUẤN LÀ INTEGRATION POINT

Các công việc của Anh Tuấn có liên quan Phase 2B có thể gồm:

Review Backend
Review Moderation
ReportCase Backend
Violation
Audit Integration

Phase 2B không được duplicate các backend này.

Thay vào đó xác định contract:

ReviewModerationGateway
ReviewQueryGateway
ReviewModerationActionGateway
ReviewAuditGateway
ReviewEvidenceGateway
VerifiedPurchaseGateway

Tên interface phải ưu tiên architecture thực tế hiện tại. Nếu project đã có interface tương đương thì reuse, không tạo interface trùng.

6. CONTRACT-FIRST IMPLEMENTATION

Trước khi viết UI phải xác định:

Input
Output
Action
State
Error
Authorization
Integration Seam

Ví dụ:

ReviewModerationGateway

có thể đại diện cho:

getReviewQueue()
getReviewDetail()
approve()
reject()
hide()
unhide()
escalate()

Nhưng:

Đây là contract concept, không phải yêu cầu tạo đúng tên class/interface này nếu project đã có architecture tương đương.

Ưu tiên:

Existing architecture
        ↓
Existing interface
        ↓
Existing service
        ↓
Adapter
        ↓
Moderator UI
7. TEST DOUBLE

Nếu backend thật chưa có, được phép tạo:

Mock
Stub
Fake
Test Double

nhưng chỉ trong test.

Không được:

Fake Review production service
Fake Audit production
Fake Review database
Hard-code Review list trong production
Hard-code Approve success
Hard-code Reject success

Mục tiêu:

Production
    ↓
Real Contract
    ↓
Real Backend khi available

Test
    ↓
Contract
    ↓
Test Double
8. PHASE 1 GATE

Trước khi implement phải verify:

Moderator Login
Moderator Role
Moderator Dashboard
Moderator Layout
Moderator Sidebar
/moderator/**
Moderator Security
Locked Account handling
Admin Security

Expected:

MODERATOR
    ↓
/moderator/**
    ↓
ALLOW
CUSTOMER / VENDOR / ANONYMOUS
    ↓
/moderator/**
    ↓
DENY

Nếu Phase 1 đã chốt:

MODERATOR
    ↓
/admin/**
    ↓
DENY

Nếu Phase 1 có issue:

Phase 2B vẫn không được bỏ qua security.

Phase 2B:
IMPLEMENTATION STATUS = tiếp tục phần độc lập được
PHASE 1 INTEGRATION = ghi nhận trạng thái thực tế

Không dùng Phase 1 chưa hoàn thành làm lý do để không implement phần Review.

9. GIT SAFETY

Trước implementation:

git status
git branch
git log --oneline --decorate -10

Ghi nhận:

Current branch:
Current HEAD:
Working tree:
Uncommitted changes:

Không:

git reset
git stash
git merge
git push
xóa thay đổi người khác
overwrite uncommitted work

Nếu file đang được developer khác sửa:

Không overwrite.

Tách implementation sang file/module phù hợp hoặc ghi nhận integration seam.

10. ĐỌC REVIEW POLICY

Phải xác định từ Policy:

Ai được tạo Review?
Khi nào Review được tạo?
Verified Purchase?
Delivered condition?
Review có thể bị reject khi nào?
Review có thể bị hide khi nào?
Review có thể unhide khi nào?
Review violation?
Fake / spam?
Manipulated content?
PII?
Off-platform contact?
Harassment?
Offensive content?
Evidence?
Appeal nếu policy có?

Không coi:

rating thấp
review tiêu cực
nhiều report
ngôn ngữ khó chịu

là bằng chứng tự động của violation.

11. BACKEND DISCOVERY

Inspect trực tiếp các thành phần liên quan:

Review
ReviewRepository
ReviewService
ReviewController
Review DTO
Review Status
Review Moderation Status
ModerationResult
ReportCase
ReportCaseService
Violation
AuditLog
Escalation
Verified Purchase
Order / OrderItem contract

Phân loại:

[AVAILABLE]
[PARTIAL]
[MISSING]
[INTEGRATION-READY]

Không dùng:

DEPENDENCY – PERSON

làm implementation status.

Ví dụ:

Review Moderation API
Current State: MISSING
Contract: DEFINED
Integration Seam: ReviewModerationGateway
Phase 2B UI: IMPLEMENTED
Integration: INTEGRATION-READY
12. REVIEW MODERATION BOUNDARY

Boundary:

CUSTOMER
   ↓
REVIEW

AUTO MODERATION
   ↓
FLAG / RESULT nếu có

MODERATOR
   ↓
MANUAL REVIEW MODERATION

ADMIN
   ↓
SEVERE ENFORCEMENT nếu policy/backend cho phép

Moderator không tự:

Ban Vendor
Suspend Shop
Freeze Funds

nếu đây là Admin responsibility.

13. REVIEW MODERATION QUEUE

Route:

/moderator/reviews

hoặc route tương đương architecture hiện tại.

Queue hiển thị Review cần xử lý.

Tối thiểu:

Review ID
Customer
Product
Shop
Vendor
Rating
Review Content
Review Status
Moderation Status
Report Count
Created At

Nếu contract cung cấp:

Severity
Auto Flag
Verified Purchase
Priority

thì hiển thị.

Không tạo field giả.

14. REVIEW QUEUE CONTRACT

Queue cần có contract tương đương:

ReviewQuery
ReviewQueueItem
ReviewPage
ReviewFilter

Contract cần xác định:

page
pageSize
status
moderationStatus
rating
product
shop
vendor
reported
date
severity
verifiedPurchase

Chỉ dùng field mà backend/contract thực tế hỗ trợ.

15. PAGINATION

Nếu backend contract hỗ trợ pagination:

Backend Pagination

Không:

Load all Review
↓
paginate frontend

Contract phải trả được tối thiểu:

items
page
pageSize
total

hoặc structure tương đương của project.

Test:

0 record
1 record
n records
page 1
page 2
last page
filter + pagination
reset filter
16. REVIEW FILTER

Các filter có thể hỗ trợ nếu contract có:

Rating
Status
Moderation Status
Reported
Product
Shop
Vendor
Date
Severity
Verified Purchase

Không tự thêm filter không có source data.

17. REVIEW DETAIL

Route:

/moderator/reviews/{id}

Hiển thị:

Review ID
Customer
Product
Shop
Vendor
Rating
Review Content
Images
Created At
Updated At
Review Status
Moderation Status
Report Count

Nếu contract cung cấp:

Report Reason
Evidence
Violation Flags
Moderation Result
Verified Purchase
Order
Order Item

thì hiển thị.

18. VENDOR / PRODUCT CONTEXT

Review Detail có thể hiển thị:

Product
Product ID
Shop
Shop ID
Vendor
Vendor ID

nếu backend contract cung cấp.

Không tự truy vấn hoặc suy luận:

Vendor violation
Shop severity
Vendor risk

nếu không thuộc Phase 2B.

19. VERIFIED PURCHASE

Nếu contract có:

verifiedPurchase
order
orderItem
delivered

hiển thị.

Ví dụ:

Verified Purchase: YES
Order: #...
Order Item: ...

Nếu backend chưa có:

Verified Purchase Integration:
INTEGRATION-PENDING

nhưng:

Review Detail:
IMPLEMENTED

Không set:

verifiedPurchase = true

để làm UI đẹp.

20. REVIEW CONTENT

Hiển thị đúng dữ liệu backend:

Rating
Text
Images
Created At

Không tự:

rewrite
translate
normalize
alter

nội dung evidence.

Nếu truncate:

View full content
21. REPORT / EVIDENCE

Nếu contract cung cấp:

Report Count
Report Reason
Reporter
Evidence
Evidence Created At

thì hiển thị.

Evidence phải:

read-only

Moderator không được thay đổi evidence gốc chỉ bằng UI Phase 2B.

Không tạo fake evidence.

22. AUTO MODERATION RESULT

Nếu contract có ModerationResult:

Moderation Status
Flags
Reason
Severity
Checked At

Có thể gồm:

Keyword Match
Image Content
Duplicate
Spam
PII
Off-platform Contact
Manipulated Content

nếu backend cung cấp.

Không tự tính:

risk score
severity
flag
reason

ở frontend.

Không tự chạy Auto Moderation Engine.

Auto Moderation Engine là integration point.

23. REVIEW STATUS CONTRACT

Inspect status hiện tại.

Không tạo enum mới nếu project đã có.

Ví dụ có thể là:

VISIBLE
HIDDEN
PENDING
APPROVED
REJECTED

hoặc tên khác.

Phải ghi:

Current Status:
Available Transitions:
Contract:
Integration Status:

Backend là source of truth cho transition.

24. ACTION CONTRACT

Phase 2B cần contract cho:

Approve
Reject
Hide
Unhide
Escalate

Mỗi action phải xác định:

Review ID
Actor
Reason nếu required
Current State
Allowed transition
Success response
Error response

UI không được tự quyết định state.

25. APPROVE

Flow:

Queue
 ↓
Detail
 ↓
Approve
 ↓
ReviewModerationGateway
 ↓
Backend
 ↓
Success
 ↓
Refresh Detail/Queue

Kiểm tra:

Resource tồn tại
Moderator authorized
State hợp lệ
Transition hợp lệ

Không update DB trực tiếp từ UI.

Nếu backend thật chưa có:

Approve Contract:
IMPLEMENTED

Moderator UI:
IMPLEMENTED

Integration:
INTEGRATION-READY
26. REJECT

Reject phải có reason nếu policy/contract yêu cầu.

UI:

Reject Review

Reason:
[________________]

[Cancel] [Confirm Reject]

Validation:

empty  → reject
spaces → reject
valid  → allow

Backend phải validate lại.

27. HIDE

Hide chỉ được thực hiện khi:

Policy cho phép
Backend contract cho phép
Current state cho phép
Moderator có quyền

Các policy context có thể gồm:

Spam
Fake Review
PII
Off-platform Contact
Offensive Content
Manipulated Content
Policy Violation

Nhưng UI không tự kết luận violation.

Không hard-delete Review để thay thế Hide.

28. UNHIDE

Nếu backend contract cho phép:

HIDDEN
   ↓
UNHIDE
   ↓
VISIBLE

Kiểm tra:

Resource tồn tại
Moderator authorized
State hợp lệ
Không bị final enforcement
Backend cho transition

Button UI chỉ là convenience.

Backend vẫn phải validate.

29. ESCALATE

Escalate chỉ tạo escalation theo contract.

Ví dụ policy/backend có thể xác định:

Severe Fraud
Illegal Activity
Serious PII
Serious policy violation
Severe vendor issue

Không tự tạo severity.

Flow:

Review
 ↓
Escalate
 ↓
Reason
 ↓
ReviewModerationGateway
 ↓
Escalation contract
 ↓
Admin

Moderator không thực hiện Admin enforcement.

30. ACTION BUTTON STATE

UI button phụ thuộc state contract.

Ví dụ:

PENDING
    Approve
    Reject
    Hide
    Escalate
VISIBLE
    Hide
    Escalate
HIDDEN
    Unhide
    Escalate
FINAL
    No action

Đây chỉ là UI representation.

Backend state transition mới là source of truth.

31. MANDATORY REASON

Các action có reason theo contract/policy:

Reject
Hide
Escalate

Test:

empty
spaces
valid

Không được bypass bằng API trực tiếp.

Backend phải validate.

32. OBJECT-LEVEL SECURITY

Không chỉ kiểm tra:

/moderator/**

Phải kiểm tra:

Review ID

Moderator không được:

Modify Review không tồn tại
Modify Review không thuộc allowed state
Bypass final state
Bypass authorization
Bypass transition

API direct request phải chịu security giống UI.

33. SECURITY CONTRACT

Phải xác định:

Authentication
Authorization
Role
Permission
Resource
State

Nếu project đã có:

@PreAuthorize

hoặc service authorization:

reuse.

Không tạo security mechanism thứ hai nếu không cần.

34. BACKEND ↔ UI INTEGRATION

Trước integration xác định:

Endpoint
HTTP Method
Request DTO
Response DTO
Status Code
Error Response
Reason
Authentication
Authorization

Ví dụ:

GET  /api/moderator/reviews
GET  /api/moderator/reviews/{id}

POST /api/moderator/reviews/{id}/approve
POST /api/moderator/reviews/{id}/reject
POST /api/moderator/reviews/{id}/hide
POST /api/moderator/reviews/{id}/unhide
POST /api/moderator/reviews/{id}/escalate

Đây chỉ là contract mẫu.

Nếu project đã có API khác, sử dụng API hiện tại.

Không tạo duplicate endpoint.

35. INTEGRATION ADAPTER

Nếu backend implementation của Anh Tuấn chưa tồn tại, Phase 2B có thể implement:

Moderator UI
      ↓
Review Moderation Interface
      ↓
Adapter
      ↓
Backend API

Sau này:

Backend của Anh Tuấn
      ↓
existing contract
      ↓
Adapter
      ↓
Moderator UI

Mục tiêu là:

UI không phụ thuộc trực tiếp vào concrete backend implementation.
36. ERROR HANDLING

Phải xử lý:

400
401
403
404
409
500

Ví dụ 409:

Review đã được Moderator khác xử lý.

Không:

white screen
raw exception
stack trace
raw JSON

nếu project đã có error handling mechanism.

Reuse error handling hiện tại.

37. CONCURRENCY

Test:

Moderator A → mở Review
Moderator B → mở Review

A → Approve
B → Reject

Expected:

Backend transition quyết định
Không silent overwrite
Không inconsistent state

Nếu backend trả:

409 Conflict

UI phải xử lý.

Tương tự:

Hide / Unhide
Approve / Reject
Escalate
38. AUDIT / HISTORY INTEGRATION

Nếu Audit contract đã có:

REVIEW_APPROVED
REVIEW_REJECTED
REVIEW_HIDDEN
REVIEW_UNHIDDEN
REVIEW_ESCALATED

hoặc tên tương đương.

Phase 2B cần tích hợp theo contract:

Actor
Role
Action
Resource
Reason
CreatedAt

Không fake AuditLog ở frontend.

Nếu Audit backend chưa có:

Audit Contract:
DEFINED / AVAILABLE / MISSING

Phase 2B:
INTEGRATION-READY

Không duplicate Audit backend.

39. ADMIN ESCALATION BOUNDARY

Phase 2B chỉ chịu trách nhiệm:

Create Escalation

hoặc gọi integration contract tương đương.

Không implement:

Admin Ban
Admin Suspend
Admin Freeze
Financial Enforcement

Các phần đó thuộc scope khác.

40. UI

Reuse Admin UI baseline:

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

Moderator UI có:

Moderator menu
Moderator permission
Moderator content
Moderator actions

nhưng cùng visual architecture.

41. UI STATE

Bắt buộc có:

Loading
Success
Empty
Error
Disabled
Validation
Conflict

Action button:

Approve
Reject
Hide
Unhide
Escalate

phải rõ ràng.

Double click không tạo duplicate action.

42. DATABASE VERIFICATION

Nếu backend contract có persistence:

sau action kiểm tra:

Review Status
Moderation Status
Reason
Moderator
UpdatedAt
Escalation
Audit

Không kiểm tra chỉ bằng UI.

Nếu persistence backend chưa tồn tại:

Database Integration:
INTEGRATION-PENDING

Phase 2B vẫn có thể hoàn thành phần contract/UI/test độc lập.

43. NO FAKE PRODUCTION DATA

Không:

Hard-code Review list
Hard-code Audit
Hard-code Verified Purchase
Hard-code Moderation Result
Hard-code success

Test data chỉ:

Test
Mock
Stub
Fixture

và không đi vào production logic.

44. FILE SCOPE

Ưu tiên chỉ thay đổi:

Moderator Review Controller
Moderator Review Service
Moderator Review Interface
Moderator Review Adapter
Moderator Review DTO/ViewModel
Moderator Review Template
Moderator Review CSS
Moderator Review JS
Moderator Security Integration
Review moderation integration
Review Moderator tests

Nếu project có architecture khác thì follow architecture thực tế.

Không:

Rewrite Admin
Rewrite Order
Rewrite Payment
Rewrite KYC
Rewrite Finance
Rewrite Review Backend
Rewrite ReportCase Backend
Rewrite Violation Backend
45. NGOÀI SCOPE

Không implement:

Product Moderation
Product Queue
Product Approve/Reject

ReportCase Backend
Violation Engine
KYC
Shop Suspension
Vendor Ban
Payment
Order Core
Checkout
Finance
Settlement
Payout
Refund
Customer Checkout
Vendor UI
Admin Enforcement
Auto Moderation Engine

Nếu cần để integration:

Define Contract
Define Integration Seam
Do not duplicate implementation
46. TEST CHECKLIST
A. Security
[ ] Moderator Login
[ ] Wrong password
[ ] Locked Moderator
[ ] Anonymous
[ ] Customer → Moderator = DENY
[ ] Vendor → Moderator = DENY
[ ] Moderator → Moderator = ALLOW
[ ] Moderator → Admin = DENY nếu Phase 1 yêu cầu
B. Queue
[ ] Queue
[ ] Filter
[ ] Pagination
[ ] Empty state
[ ] Loading
[ ] Error
C. Detail
[ ] Customer
[ ] Product
[ ] Shop
[ ] Vendor
[ ] Rating
[ ] Content
[ ] Images
[ ] Created At
[ ] Reports
[ ] Evidence
[ ] Verified Purchase nếu contract có
[ ] Moderation Result nếu contract có
D. Actions
[ ] Approve
[ ] Reject
[ ] Reject empty reason
[ ] Reject spaces
[ ] Reject valid reason
[ ] Hide
[ ] Hide reason nếu required
[ ] Unhide
[ ] Escalate
[ ] Escalate empty reason
[ ] Escalate valid reason
E. State
[ ] Final state không xử lý lại
[ ] Invalid transition rejected
[ ] Hide → Hidden
[ ] Unhide → Visible
[ ] Approve transition
[ ] Reject transition
F. Security bypass
[ ] Direct API request
[ ] Invalid Review ID
[ ] Unauthorized action
[ ] Wrong role
[ ] Invalid state
G. Concurrency
[ ] Moderator A Approve
[ ] Moderator B Reject
[ ] 409 nếu backend hỗ trợ
[ ] Không silent overwrite
H. Audit
[ ] Approve
[ ] Reject
[ ] Hide
[ ] Unhide
[ ] Escalate
47. MANUAL E2E
Flow 1 — Approve
Review
 ↓
Queue
 ↓
Detail
 ↓
Approve
 ↓
Contract
 ↓
Backend / Test Integration
 ↓
State
 ↓
History

Expected:

Correct transition
Correct actor
No duplicate action
Flow 2 — Reject
Review
 ↓
Reject
 ↓
Empty reason

Expected:

Không submit

Sau đó:

Valid reason
 ↓
Reject
 ↓
Backend
 ↓
Rejected
 ↓
History
Flow 3 — Hide / Unhide
Review
 ↓
Hide
 ↓
HIDDEN
 ↓
Unhide
 ↓
VISIBLE

Verify:

UI
Contract
State
Database nếu integration có
Flow 4 — Escalate
Review
 ↓
Evidence / Policy context
 ↓
Escalate
 ↓
Reason
 ↓
Escalation Contract
 ↓
Admin integration

Expected:

Escalation được tạo nếu backend hỗ trợ
Reason được truyền đúng
Evidence không bị thay đổi
Moderator không tự Admin enforcement

Nếu Admin backend chưa tồn tại:

Escalation:
IMPLEMENTED
Integration:
INTEGRATION-READY
48. REGRESSION — ADMIN

Sau Phase 2B:

Admin Login
Admin Dashboard
User Management
Shop Management
Category
Voucher
Banner
Admin Product
Admin Review

Không để Moderator Review ảnh hưởng Admin Review.

49. REGRESSION — MODERATOR
Moderator Login
Moderator Dashboard
Moderator Sidebar
Moderator Security
Moderator Queue

Không để:

/moderator/reviews/**

phá các Moderator route khác.

50. BUILD / TEST

Chạy:

mvn test
mvn clean package

hoặc command chính thức của project.

Báo cáo:

BUILD: PASS / FAIL
TEST: PASS / FAIL

Nếu fail:

Command:
Error:
Root cause:
File:
Function:
Affected module:
Integration Status:

Không che giấu lỗi.

51. MASTER STATUS

Không dùng bảng kiểu:

Owner = Tuấn
Dependency = Tuấn

làm trạng thái implementation.

Sử dụng:

#	Nghiệp vụ	Implementation	Contract	Integration	Security	Test	Status
1	Review Queue	?	?	?	?	?	?
2	Review Detail	?	?	?	?	?	?
3	Approve	?	?	?	?	?	?
4	Reject	?	?	?	?	?	?
5	Hide	?	?	?	?	?	?
6	Unhide	?	?	?	?	?	?
7	Escalate	?	?	?	?	?	?
8	Report/Evidence	?	?	?	?	?	?
9	Verified Purchase	?	?	?	?	?	?
10	Auto Moderation Result	?	?	?	?	?	?
11	Mandatory Reason	?	?	?	?	?	?
12	History/Audit	?	?	?	?	?	?
13	Pagination/Filter	?	?	?	?	?	?
14	Error Handling	?	?	?	?	?	?
15	Object Security	?	?	?	?	?	?
16	State Transition	?	?	?	?	?	?
17	Concurrency	?	?	?	?	?	?
18	Admin Regression	?	N/A	N/A	?	?	?
19	Moderator Regression	?	N/A	N/A	?	?	?
52. STATUS DEFINITIONS
IMPLEMENTED

Phần thuộc Phase 2B đã code xong và test độc lập.

INTEGRATION-READY

Phần Phase 2B đã hoàn thành, nhưng backend/external system chưa được nối.

Ví dụ:

Review Queue UI        IMPLEMENTED
Review Contract        IMPLEMENTED
Review Backend         NOT AVAILABLE
Integration            INTEGRATION-READY
INTEGRATED

Backend thật đã được nối và test thành công.

PARTIAL

Chính phần Phase 2B còn thiếu implementation.

MISSING

Chưa có implementation thuộc scope Phase 2B.

WRONG

Implementation hiện tại sai contract, policy hoặc architecture.

OUT OF SCOPE

Không thuộc Phase 2B.

53. KHÔNG DÙNG “DEPENDENCY – ANH TUẤN” ĐỂ ĐÁNH DẤU BLOCK

Thay vì:

Review Backend
DEPENDENCY – Anh Tuấn
BLOCKED

ghi:

Review Backend
Current State: NOT AVAILABLE
Required Contract: DEFINED
Integration Seam: ReviewModerationGateway
Moderator Implementation: IMPLEMENTED
Integration: INTEGRATION-READY
External Implementation: Pending

Đây là điểm quan trọng nhất của Phase 2B mới.

54. ĐIỀU KIỆN GHI IMPLEMENTED

Phase 2B có thể ghi:

IMPLEMENTED

khi phần thuộc Moderator Phase 2B:

UI hoàn chỉnh
Contract hoàn chỉnh
Integration seam hoàn chỉnh
Security hoàn chỉnh
Validation hoàn chỉnh
State handling hoàn chỉnh
Error handling hoàn chỉnh
Test độc lập pass

Không bắt buộc phải chờ:

Anh Tuấn commit backend
Anh Tuấn merge branch
ReportCase backend
Violation backend
Auto Moderation Engine
Audit backend

nếu những thứ đó nằm ngoài implementation ownership của Phase 2B.

55. ĐIỀU KIỆN GHI INTEGRATED

Chỉ ghi:

INTEGRATED

sau khi backend thật đã được nối và test:

Real API
Real persistence
Real authorization
Real state transition
Real audit

Nếu chưa có backend:

INTEGRATION-READY

không phải:

BLOCKED
56. FILE CHANGE REPORT

Sau implementation:

Created:
Modified:
Deleted:

Mỗi file quan trọng:

File:
Purpose:
Why changed:
Integration Seam:
Impact:

Không ghi file không thực sự thay đổi.

57. PHASE 2B RESULT

Sau implementation báo cáo:

# PHASE 2B RESULT

## 1. Git Baseline

Branch:
HEAD:
Working tree:
Uncommitted changes:

## 2. Phase 1

Moderator Login:
Moderator Dashboard:
Moderator Security:
Status:

## 3. Review Contract

Review Query:
Review Detail:
Review Action:
Reason:
State:
Integration Seam:

Status:

## 4. Review Queue

Queue:
Filter:
Pagination:
Status:

## 5. Review Detail

Customer:
Product:
Shop:
Vendor:
Rating:
Content:
Images:
Reports:
Evidence:
Verified Purchase:
Moderation Result:

Status:

## 6. Actions

Approve:
Reject:
Hide:
Unhide:
Escalate:

Status:

## 7. Security

Moderator:
Admin:
Vendor:
Customer:
Anonymous:
Locked Moderator:

Status:

## 8. Object / State Security

Invalid ID:
Unauthorized:
Final Review:
Invalid Transition:
Concurrency:

Status:

## 9. History / Audit

Approve:
Reject:
Hide:
Unhide:
Escalate:

Status:

## 10. Database

Review Status:
Moderation Status:
Reason:
Actor:
Timestamp:
Audit:
Escalation:

Status:

## 11. Integration

Real Backend:
Contract:
Adapter:
Integration Seam:
External Backend Status:

Status:
IMPLEMENTED / INTEGRATION-READY / INTEGRATED

## 12. Regression

Admin:
Moderator:

Status:

## 13. Build

PASS / FAIL

## 14. Test

PASS / FAIL

## 15. Files Changed

Created:
Modified:
Deleted:

## 16. IMPLEMENTED

...

## 17. INTEGRATION-READY

...

## 18. INTEGRATED

...

## 19. PARTIAL

...

## 20. MISSING

...

## 21. WRONG

...

## 22. OUT OF SCOPE

...

## 23. EXTERNAL INTEGRATION POINTS

Review Backend:
ReportCase:
Violation:
Auto Moderation:
Verified Purchase:
Audit:
Escalation:

## 24. KNOWN ISSUES

...

## 25. BLOCKERS

Chỉ ghi blocker nếu là blocker kỹ thuật thực sự của môi trường/repository.
Không ghi “developer chưa làm backend” là blocker.

## 26. PHASE 2C HANDOFF

Contract:
Integration Seam:
Data required:
State required:
API required:

## 27. FINAL STATUS

IMPLEMENTED
INTEGRATION-READY
INTEGRATED
PARTIAL
58. PHASE 2C HANDOFF

Phase 2B phải để lại cho Phase 2C các contract cần thiết nếu có:

Review
Review Moderation Result
Review Decision
Escalation
Audit
Moderator Actor
Review State

Phase 2C không cần phụ thuộc vào branch/commit cụ thể của Phase 2B.

Chỉ phụ thuộc vào:

Contract
Interface
Stable domain model
Integration seam
59. FINAL ACCEPTANCE CRITERIA

Phase 2B đạt yêu cầu khi:

1. Review Queue
2. Review Detail
3. Review information
4. Report / Evidence contract
5. Verified Purchase contract
6. Moderation Result contract
7. Approve
8. Reject
9. Reject reason validation
10. Hide
11. Unhide
12. Escalate
13. State transition handling
14. Object-level security
15. Mandatory reason
16. Error handling
17. Pagination
18. Filter
19. Concurrency handling
20. Audit integration contract
21. Moderator security
22. Admin regression
23. Moderator regression
24. Independent tests
25. Build
26. No fake production data
27. No overwrite developer code
28. No duplicate Review Backend
29. No duplicate ReportCase Backend
30. No duplicate Violation Backend
31. Integration seam documented
32. External backend can be connected later
60. MỤC TIÊU CUỐI CÙNG

Phase 2B phải đạt kiến trúc:

                   REVIEW BACKEND
                         │
                         │ Contract
                         ▼
              Review Moderation Interface
                         │
                         │
                  Integration Seam
                         │
                         ▼
              Moderator Review Service
                         │
              ┌──────────┼──────────┐
              ▼          ▼          ▼
           Queue       Detail     Actions
                                   │
                    ┌──────────────┼──────────────┐
                    ▼              ▼              ▼
                 Approve        Reject          Hide
                    │              │              │
                    └──────────────┼──────────────┘
                                   ▼
                                Unhide
                                   │
                                   ▼
                                Escalate
                                   │
                                   ▼
                              Audit/History

Backend thật của Anh Tuấn:

              REVIEW BACKEND
                    │
                    ▼
          Integration Contract
                    │
                    ▼
        Phase 2B Integration Seam

Không phải:

Phase 2B
   ↓
WAIT FOR TUẤN
   ↓
TUẤN FINISH
   ↓
MERGE
   ↓
MỚI ĐƯỢC CODE

Mà là:

Phase 2B
   ↓
DISCOVER EXISTING CONTRACT
   ↓
DEFINE / REUSE CONTRACT
   ↓
IMPLEMENT MODERATOR SCOPE
   ↓
TEST INDEPENDENTLY
   ↓
INTEGRATION-READY
   ↓
Khi backend thật có
   ↓
CONNECT ADAPTER / INTEGRATION SEAM
   ↓
INTEGRATED
Ranh giới cuối cùng
AUTO MODERATION
= Máy kiểm tra / flag nếu có
MODERATOR
= Xử lý Review thông thường
ADMIN
= Xử lý escalation / enforcement nghiêm trọng

Và quan trọng nhất:

Phase 2B không cần chờ Anh Tuấn hoàn thành Review Backend để bắt đầu hoặc hoàn thành phần implementation của mình. Nhưng Phase 2B cũng không được giả lập Review Backend trong production hoặc tự viết một Review Backend thứ hai.