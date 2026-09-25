
PHASE 2B — MODERATOR WORKFLOW
0. NGUYÊN TẮC BẮT BUỘC
KHÔNG XÓA DỮ LIỆU MONGODB

KHÔNG RESET DATABASE
KHÔNG DROP COLLECTION
KHÔNG deleteMany({})
KHÔNG deleteAll()
KHÔNG cleanup() làm mất dữ liệu hiện có

KHÔNG FAKE PRODUCTION DATA
KHÔNG FAKE MODERATION RESULT
KHÔNG FAKE VIOLATION
KHÔNG FAKE REPORT CASE

KHÔNG CHECKOUT BRANCH
KHÔNG STASH
KHÔNG MERGE
KHÔNG COMMIT
KHÔNG PUSH

KHÔNG CHỜ DEVELOPER KHÁC

KHÔNG ĐƯỢC BỎ QUA DEPENDENCY.

Nếu dependency chưa tồn tại:
    TRACE SOURCE
    XÁC ĐỊNH CONTRACT
    IMPLEMENT PHẦN THUỘC PHASE 2B
    BUILD
    TEST
    BÁO CÁO INTEGRATION-READY nếu external integration chưa có

Phase 2B phải được thực hiện độc lập trong phạm vi được giao.

Không được coi:

File exists
        ≠
Feature completed

Controller exists
        ≠
Workflow completed

Template exists
        ≠
Backend completed

Enum exists
        ≠
Business rule completed

Menu exists
        ≠
Security completed

Test class exists
        ≠
Test passed

1. MỤC TIÊU PHASE 2B

Phase 2B hoàn thiện Moderator Workflow thực tế trên nền tảng đã được xác định ở:

Gate 0
   ↓
Phase 1
Moderator Foundation + Security
   ↓
Phase 2A
Moderation Domain Foundation
   ↓
PHASE 2B
Moderator Workflow

Mục tiêu cuối:

MODERATOR LOGIN
      ↓
MODERATOR DASHBOARD
      ↓
MODERATION QUEUE
      ↓
CASE / ITEM DETAIL
      ↓
REVIEW EVIDENCE
      ↓
MODERATOR DECISION
      ↓
STATE TRANSITION
      ↓
AUDIT / HISTORY
      ↓
ESCALATE nếu workflow cho phép

Phase 2B không chỉ tạo UI.

Phải hoàn thành xuyên suốt:

UI
 ↓
Controller
 ↓
Security
 ↓
Service
 ↓
Domain
 ↓
Persistence
 ↓
State transition
 ↓
History / Audit contract
2. SOURCE OF TRUTH

Thứ tự ưu tiên:

CURRENT SOURCE CODE
        ↓
GATE 0 CONTRACT
        ↓
PHASE 1 FINAL RESULT
        ↓
PHASE 2A FINAL RESULT
        ↓
PHASE 2B SPECIFICATION
        ↓
MARKETPLACE POLICY
        ↓
SHOPEE_MARKETPLACE_MODEL

Không được lấy Phase 2 cũ làm bằng chứng rằng feature đã tồn tại.

Phải kiểm tra source thực tế.

3. ĐIỀU KIỆN ĐẦU VÀO

Phase 2B được phép triển khai khi đã xác định:

Moderator security
MODERATOR role tồn tại.
/moderator/** được bảo vệ.
Moderator không truy cập Admin-only route.
Customer/Vendor không truy cập Moderator route.
Moderation domain

Phải xác định được ít nhất các contract cần thiết cho workflow:

Moderation target
Moderation action
Moderation state
Actor
Reason
Evidence nếu có
History/Audit contract
Persistence

Phải xác định:

Repository
Entity / Document
Identifier
State field
Update mechanism

Nếu Phase 2A chưa có một thành phần cần thiết:

TRACE
   ↓
IMPLEMENT MINIMUM FOUNDATION
   ↓
CONTINUE PHASE 2B

Không được báo:

BLOCKED – developer khác chưa làm

nếu phần đó có thể tự triển khai trong phạm vi Phase 2B.

4. KHÔNG PHỤ THUỘC DEVELOPER KHÁC

Phase 2B là một phần độc lập.

Nếu source hiện tại thiếu:

ReportCase
Violation
AuditLog
ModerationHistory

thì không mặc định chờ người khác.

Phải phân loại:

Có thể tự implement
        ↓
IMPLEMENT

Có contract nhưng external implementation chưa có
        ↓
INTEGRATION-READY

Thực sự không thể triển khai vì dependency ngoài phạm vi
        ↓
BLOCKED

BLOCKED chỉ được dùng khi có blocker kỹ thuật thực sự.

5. PHẠM VI

Phase 2B tập trung vào:

Moderator Queue
Moderator Case Detail
Product Moderation Workflow
Review Moderation Workflow
Report Case Workflow
Violation Workflow integration
Moderator Decision
Reason
Evidence
State Transition
Assignment
Escalation contract
History
Audit integration
Security
Authorization
IDOR protection
UI
Backend
Persistence
Regression
6. KHÔNG LÀM TRONG PHASE 2B

Không triển khai lại:

Moderator Role
JWT Foundation
Authentication Foundation
Admin Foundation
Customer Authentication
Vendor Authentication

nếu Phase 1 đã hoàn thành.

Không triển khai:

Complaint
Return
Refund
Escrow
Settlement
Finance Dashboard
Scheduler

Các phần này thuộc Phase 3/Phase 4.

7. PHÂN BIỆT ADMIN VÀ MODERATOR

Đây là một mục bắt buộc.

Source hiện tại đã có:

Admin Product
Admin Review

Nhưng không được mặc định coi chúng là Moderator workflow.

Phải xác định boundary:

MODERATOR
    ↓
Normal moderation workflow

ADMIN
    ↓
Platform management
Severe enforcement
Admin-only operations

Moderator không được tự động có:

Admin product delete
Admin shop management
Admin order management
Admin user management
Admin-only enforcement

nếu Security Contract không cho phép.

8. MODERATOR QUEUE

Phải có khả năng lấy danh sách moderation task/case mà Moderator được phép xử lý.

Concept:

Moderator
    ↓
/moderator/...
    ↓
Queue
    ↓
Available moderation cases

Queue phải kiểm tra:

authenticated user
role
permission
case scope
state

Không được load toàn bộ dữ liệu nhạy cảm không cần thiết.

9. MODERATOR CASE DETAIL

Moderator phải có thể xem dữ liệu cần thiết để ra quyết định.

Tùy domain, có thể gồm:

Product
Product
Shop
Vendor
Category
Relevant content
Moderation state
Evidence
History
Review
Review
Customer
Product
Shop
Content
Current moderation state
History
ReportCase
Reporter
Target
Reason
Description
Evidence
Current state
History
Previous decision

Chỉ expose dữ liệu cần thiết.

10. PRODUCT MODERATION

Nếu Product Moderation contract đã tồn tại ở Phase 2A, Phase 2B triển khai workflow.

Ví dụ:

Product
   ↓
MODERATION QUEUE
   ↓
Moderator Review
   ↓
Decision

Các action chỉ được triển khai nếu đã có contract:

APPROVE
REJECT
HIDE
REQUEST_EVIDENCE
ESCALATE

Không tự tạo thêm action chỉ vì UI cần.

11. PRODUCT STATE TRANSITION

Không được cập nhật Product bằng logic tùy ý trong Controller.

Sai:

controller
   ↓
product.setStatus(...)

Ưu tiên:

Controller
    ↓
ModeratorService
    ↓
Validation
    ↓
State Transition
    ↓
Repository

Phải kiểm tra:

Current State
        ↓
Allowed Action
        ↓
New State

Không cho phép transition tùy tiện.

12. REVIEW MODERATION

Source hiện tại chỉ có Review functionality cơ bản.

Phase 2B chỉ triển khai moderation workflow nếu domain/state contract đã được xác định.

Không coi:

AdminReviewService.delete()

là Moderator moderation.

Nếu cần:

VISIBLE
HIDDEN
UNDER_REVIEW
REJECTED

thì phải lấy từ Phase 2A/Policy/Source of Truth.

Không tự phát minh enum nếu chưa có căn cứ.

13. REVIEW DECISION

Moderator có thể thực hiện những action được contract hỗ trợ.

Ví dụ:

Review
 ↓
Moderator Review
 ↓
Approve / Hide / Reject

Mỗi decision phải:

kiểm tra quyền
kiểm tra state hiện tại
validate target
ghi thay đổi persistence
ghi history/audit nếu contract yêu cầu
14. REPORT CASE

Nếu Phase 2A đã tạo ReportCase, Phase 2B sử dụng chính domain đó.

Không tạo:

ModeratorCase
ProductReportCase
ReviewReportCase
ComplaintReportCase

chỉ vì khác nghiệp vụ.

Nếu ReportCase đủ khả năng biểu diễn:

target
reason
evidence
status
actor
decision

thì reuse.

15. REPORT CASE WORKFLOW

Concept:

REPORT CASE
    ↓
OPEN
    ↓
MODERATOR REVIEW
    ↓
DECISION
    ├── RESOLVE
    ├── REJECT
    ├── REQUEST EVIDENCE
    └── ESCALATE

Tên state phải lấy từ contract thực tế.

Không tự tạo hàng loạt status.

16. MODERATOR DECISION

Mọi decision phải đi qua Service.

POST action
      ↓
Authentication
      ↓
Authorization
      ↓
Ownership / Scope
      ↓
Current State validation
      ↓
Business validation
      ↓
Persistence
      ↓
History / Audit

Không đặt business logic chính trong:

Controller
Template
JavaScript
17. MANDATORY REASON

Nếu Policy/Domain Contract yêu cầu reason cho action:

REJECT
HIDE
ESCALATE

thì backend phải enforce.

Không chỉ:

HTML required

Mà phải:

Backend validation
18. EVIDENCE

Nếu ReportCase/Moderation có evidence:

Evidence
    ↓
Existing Upload Security

Không tạo uploader mới.

Phải kiểm tra:

ownership
permission
file type
file size
access control

Không cho truy cập file chỉ bằng:

fileId
URL
filename
19. VIOLATION INTEGRATION

Nếu Moderator decision phát hiện policy violation:

Moderation
     ↓
Violation

Phase 2B phải reuse Violation domain đã được xác định.

Không tạo:

ModeratorViolation
ProductViolation
ReviewViolation

nếu Violation hiện tại đủ dùng.

20. ESCALATION INTEGRATION

Nếu workflow có escalation:

Moderator
    ↓
Escalate
    ↓
Existing Escalation contract

Không tạo:

ModeratorEscalationV2
ProductEscalation
ReviewEscalation

nếu domain chung đã tồn tại.

Nếu Escalation chưa tồn tại nhưng Phase 2B cần contract:

DEFINE CONTRACT
      ↓
INTEGRATION-READY

Không fake escalation production.

21. AUDIT / HISTORY

Không được đánh đồng:

History
Audit

với nhau.

Nếu AuditLog chưa tồn tại trong source:

AuditLog = MISSING

không được báo:

Audit completed

chỉ vì có history field.

Phase 2B phải ghi rõ:

Audit:
IMPLEMENTED
INTEGRATION-READY
MISSING
OUT OF SCOPE

tùy thực tế.

22. MODERATOR SECURITY

Bắt buộc test:

Moderator → Moderator
ALLOW

trong scope được phép.

Customer → Moderator
DENY
Vendor → Moderator
DENY
Anonymous → Moderator
DENY
Moderator → Admin
DENY

đối với Admin-only endpoint.

23. IDOR

Bắt buộc kiểm tra:

Moderator A
    ↓
Case không thuộc scope

Expected:

DENY

Không chỉ kiểm tra UI.

Backend phải enforce.

24. AUTHORIZATION

Mọi mutation phải kiểm tra:

Authentication
+
Role
+
Permission
+
Resource scope
+
Current state

Không tin:

moderatorId
caseId
productId
reviewId

do client gửi lên nếu backend có thể xác định từ authenticated principal.

25. ADMIN REGRESSION

Không rebuild Admin.

Chỉ regression những phần bị ảnh hưởng:

Admin Product
Admin Review
Admin Shop
Admin Order
Admin Security

Đặc biệt kiểm tra:

Moderator ≠ Admin

Không để thêm quyền Moderator vô tình mở Admin endpoint.

26. DATABASE SAFETY

TUYỆT ĐỐI KHÔNG:

deleteAll()
deleteMany({})
drop()
dropDatabase()
collection.drop()
reset()
cleanup()

Không xóa:

User
Shop
Product
Review
Order
existing moderation data
27. TEST DATA

Nếu cần dữ liệu manual test:

[TEST-PHASE-2B]

Có thể tạo lượng nhỏ.

Ví dụ:

1 test Product
1 test Review
1 test ReportCase

Nhưng:

NO DELETE AFTER TEST

Dữ liệu test được giữ nguyên.

28. KHÔNG FAKE MODERATION

Không được tạo:

status = APPROVED

chỉ để dashboard hiển thị đẹp.

Không fake:

Violation
ReportCase
Moderator decision
Audit
Escalation

Nếu dependency chưa có:

INTEGRATION-READY
29. UI

Moderator UI tối thiểu:

Moderator Dashboard
        ↓
Moderation Queue
        ↓
Case Detail
        ↓
Decision Form

UI phải gọi backend thật.

Không tạo mock:

static JSON
hardcoded cases
fake counters
fake decisions
30. BACKEND CONTRACT

Trước khi tạo endpoint:

SEARCH EXISTING CONTROLLER
SEARCH SERVICE
SEARCH REPOSITORY
SEARCH ROUTE

Nếu đã có:

REUSE

Không tạo duplicate controller/service.

Ví dụ chỉ là contract tham khảo:

GET /moderator/...
GET /moderator/cases/{id}
POST /moderator/cases/{id}/decision

Không bắt buộc dùng đúng route này.

Route thực tế phải theo architecture hiện tại.

31. BUSINESS LOGIC

Architecture ưu tiên:

Controller
    ↓
Service
    ↓
Repository

Không:

Controller
    ↓
MongoRepository trực tiếp

nếu project hiện tại đã có service layer.

32. TRANSACTION / CONSISTENCY

Nếu decision thay đổi nhiều domain:

Target
+
ReportCase
+
Violation
+
History
+
Audit

phải kiểm tra consistency.

Không tự thay đổi persistence strategy toàn project.

Không thêm transaction hàng loạt nếu không cần.

33. CONCURRENCY

Nếu hai Moderator xử lý cùng một case:

Moderator A
       ↓
Decision

Moderator B
       ↓
Decision

phải tránh transition không hợp lệ.

Kiểm tra:

current state

trước khi mutation.

Nếu architecture hỗ trợ optimistic locking/versioning thì reuse.

Không tự xây concurrency framework mới.

34. STATE MACHINE

Không tạo state machine mới nếu domain đã có state.

Mục tiêu:

OPEN
 ↓
IN_REVIEW
 ↓
DECISION
 ↓
RESOLVED

hoặc workflow thực tế của source.

State phải được xác định từ:

Gate 0
Phase 2A
Policy
Current source
35. DEPENDENCY MATRIX

Phase 2B:

Dependency	Trạng thái cần có
MODERATOR role	Có
Moderator route security	Có
Moderator dashboard	Có
Moderation domain	Có từ Phase 2A hoặc phải implement minimum contract
Persistence	Bắt buộc
Product moderation	Có contract trước workflow
Review moderation	Có contract trước workflow
ReportCase	Có contract trước workflow
Violation	Có contract nếu workflow tạo violation
Audit	Có hoặc INTEGRATION-READY
Escalation	Có contract nếu workflow escalate

Không được giả định dependency đã hoàn thành chỉ vì kế hoạch ghi là đã có.

36. MANUAL TEST — MODERATOR LOGIN
    Test 1
    Moderator login
    ↓
    /moderator/dashboard

Expected:

ALLOW
37. MANUAL TEST — SECURITY
Test 2
Customer
→ /moderator/...

Expected:

DENY
Test 3
Vendor
→ /moderator/...

Expected:

DENY
Test 4
Moderator
→ /admin/...

Expected:

DENY

nếu endpoint Admin-only.

38. MANUAL TEST — MODERATION QUEUE
    Moderator
    ↓
    Dashboard
    ↓
    Queue
    ↓
    Open case

Expected:

Case data loaded
Permission validated
No unauthorized data exposed
39. MANUAL TEST — DECISION
Moderator
 ↓
Case
 ↓
Decision
 ↓
Reason
 ↓
Submit

Expected:

Backend validates permission
Backend validates state
State updated
History/Audit updated nếu contract hỗ trợ
40. MANUAL TEST — IDOR
Moderator
 ↓
Case ID không thuộc scope

Expected:

DENY
41. MANUAL TEST — INVALID STATE

Ví dụ:

Case = CLOSED

Moderator cố:

REJECT

Expected:

DENY / validation error

Không được mutation tùy ý.

42. MONGODB SAFETY TEST

Báo cáo bắt buộc:

Existing data deleted:
NONE

Database reset:
NO

Collection dropped:
NO

deleteAll:
NO

deleteMany({}):
NO

Test data inserted:
<number></number>

Test data deleted:
NO

Nếu không insert:

Test data inserted:
0
43. BUILD RULE

Không tự động chạy:

mvn test
mvn verify
mvn install

Build theo quy định:

mvn clean package -DskipTests

Báo:

PASS

hoặc:

FAIL

Không ghi PASS nếu build chưa chạy.

44. FILE CHANGE REPORT

Cuối Phase 2B phải báo:

Files Created:

- ...

Files Modified:

- ...

Files Deleted:
NONE

Không tự xóa file.

Không:

git add .

một cách mù quáng.

45. SECURITY REPORT

Bắt buộc báo:

Moderator authentication:
PASS / FAIL

Moderator authorization:
PASS / FAIL

Customer → Moderator:
PASS / FAIL

Vendor → Moderator:
PASS / FAIL

Moderator → Admin:
PASS / FAIL

IDOR:
PASS / FAIL

Resource scope:
PASS / FAIL
46. WORKFLOW REPORT
Moderator Login:
PASS / FAIL / NOT TESTED

Moderator Dashboard:
PASS / FAIL / NOT TESTED

Moderation Queue:
PASS / FAIL / NOT TESTED

Product Moderation:
PASS / FAIL / NOT TESTED

Review Moderation:
PASS / FAIL / NOT TESTED

ReportCase:
PASS / FAIL / NOT TESTED

Decision:
PASS / FAIL / NOT TESTED

Violation Integration:
PASS / FAIL / INTEGRATION-READY / OUT OF SCOPE

Escalation:
PASS / FAIL / INTEGRATION-READY / OUT OF SCOPE

Audit:
PASS / FAIL / INTEGRATION-READY / MISSING

IDOR:
PASS / FAIL

Security:
PASS / FAIL
47. INTEGRATION STATUS

Mỗi dependency phải báo theo mẫu:

Integration Point:
<name></name>

Existing Contract:
<entity/service/interface/route>

Phase 2B Implementation:
<what was implemented></what>

External Implementation:
AVAILABLE / NOT AVAILABLE

Status:
IMPLEMENTED
INTEGRATION-READY
INTEGRATED
PARTIAL
MISSING
OUT OF SCOPE

Notes:
<evidence></evidence>

Không dùng:

BLOCKED – teammate chưa code
48. KNOWN ISSUES

Mỗi issue:

Issue:
Current Behavior:
Expected Behavior:
Root Cause:
Source:
Severity:
Dependency:
Status:

Ví dụ:

Issue:
AuditLog backend chưa tồn tại.

Current Behavior:
Moderator decision chưa có persistent AuditLog.

Expected Behavior:
Decision được ghi vào AuditLog.

Root Cause:
AuditLog domain chưa tồn tại trong source.

Status:
INTEGRATION-READY / MISSING

Không nói:

BLOCKED

chỉ vì chưa có developer khác implement.

49. ACCEPTANCE CRITERIA

Phase 2B chỉ được coi là hoàn thành khi workflow thực tế hoạt động:

Moderator Login
      ↓
Moderator Dashboard
      ↓
Moderation Queue
      ↓
Case Detail
      ↓
Authorization
      ↓
Decision
      ↓
State Transition
      ↓
Persistence
      ↓
History / Audit Contract

Và:

Unauthorized User
      ↓
DENY
Unauthorized Case
      ↓
DENY
Invalid State Transition
      ↓
DENY
50. PHASE 2B KHÔNG ĐƯỢC LÀM HỎNG PHASE 1

Regression bắt buộc:

MODERATOR ROLE
MODERATOR LOGIN
MODERATOR SECURITY
ADMIN LOGIN
ADMIN ROUTES
CUSTOMER ACCESS
VENDOR ACCESS

Không được để Phase 2B làm thay đổi ngoài ý muốn:

Admin authorization
Customer authorization
Vendor authorization
JWT behavior
Existing product behavior
Existing review behavior
51. KHÔNG LÀM SAI RANH GIỚI PHASE 2B

Phase 2B:

Moderator Workflow

Không biến thành:

Complaint System
Finance System
Refund System
Escrow System
Scheduler System
Admin Dashboard
52. PHASE 2B OUTPUT

Sau khi hoàn thành phải bàn giao được:

Moderator
    ↓
Dashboard
    ↓
Queue
    ↓
Moderation Case
    ↓
Review
    ↓
Decision
    ↓
Persistence
    ↓
History/Audit Contract
    ↓
Violation/Escalation Contract

với security thực tế.

53. FINAL REPORT

Cuối Phase 2B bắt buộc báo:

Phase Status
PHASE 2B
MODERATOR WORKFLOW

Status:
IMPLEMENTED
/ PARTIAL
/ INTEGRATION-READY
Completed
Moderator Queue:
...

Case Detail:
...

Product Moderation:
...

Review Moderation:
...

ReportCase:
...

Decision:
...

Violation:
...

Escalation:
...

Audit:
...

Security:
...

Chỉ đánh dấu phần thực sự hoàn thành.

MongoDB
Existing data deleted:
NONE

Database reset:
NO

Collection dropped:
NO

deleteAll:
NO

deleteMany({}):
NO

Test data inserted:
<number></number>

Test data deleted:
NO
Build
Command:
mvn clean package -DskipTests

Result:
PASS / FAIL
Git
Commit:
NOT DONE

Push:
NOT DONE

Merge:
NOT DONE

Checkout:
NOT DONE

Reset:
NOT DONE

Stash:
NOT DONE
54. KIẾN TRÚC SAU PHASE 2B
                 MODERATOR
                     │
                     ▼
             MODERATOR DASHBOARD
                     │
                     ▼
             MODERATION QUEUE
                     │
                     ▼
              CASE / TARGET
                     │
          ┌──────────┼──────────┐
          │          │          │
       PRODUCT     REVIEW    REPORT CASE
          │          │          │
          └──────────┼──────────┘
                     │
                     ▼
              MODERATOR REVIEW
                     │
          ┌──────────┼───────────┐
          │          │           │
       APPROVE     REJECT     ESCALATE
          │          │           │
          ▼          ▼           ▼
       STATE       STATE     ESCALATION
       UPDATE      UPDATE     CONTRACT
          │          │           │
          └──────────┼───────────┘
                     │
                     ▼
              HISTORY / AUDIT
                     │
                     ▼
              VIOLATION nếu có
55. NGUYÊN TẮC CUỐI CÙNG
GATE 0
Source Audit

+ Domain/Role/State Contract
  ↓
  PHASE 1
  Moderator Foundation
+ Security
  ↓
  PHASE 2A
  Moderation Domain Foundation
+ Persistence
+ Product/Review/ReportCase/Violation contracts
  ↓
  PHASE 2B
  Moderator Workflow
+ Queue
+ Detail
+ Decision
+ State Transition
+ Security
+ Integration
  ↓
  PHASE 3
  Complaint
+ Return/Refund
+ Escalation
+ Scheduler
  ↓
  PHASE 4
  Dashboard
+ Finance
+ Full Integration
+ Final Verification

Điểm quan trọng nhất của Phase 2B: không lấy việc “đã có Admin Product/Review” làm bằng chứng rằng Moderator Workflow đã có. Phase 2B phải tạo ra đường đi hoàn chỉnh từ Moderator → Queue → Case → Authorization → Decision → Persistence, đồng thời reuse domain đã được chốt ở Gate 0/2A và không làm mất bất kỳ dữ liệu MongoDB hiện có.
