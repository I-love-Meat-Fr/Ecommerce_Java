
PHASE 2A — MODERATION DOMAIN FOUNDATION
0. IMPLEMENTATION INSTRUCTION

Phase 2A là phase xây dựng nền tảng domain cho hệ thống moderation dựa trực tiếp trên:

Source code hiện tại sau Phase 1.
Gate 0 — Source Audit + Scope Lock.
Phase 1 — Moderator Foundation + Security.
BỘ LUẬT & CHÍNH SÁCH CỦA SÀN MARKETPLACE – Phiên bản 1.0.
SHOPEE_MARKETPLACE_MODEL.md.
Kết quả audit source hiện tại.

Phase 2A không được coi audit report hoặc kế hoạch cũ là bằng chứng implementation đã tồn tại.

Audit chỉ là:

BASELINE

Source code thực tế mới là:

SOURCE OF TRUTH

Mục tiêu Phase 2A:

CURRENT SOURCE
      ↓
DOMAIN GAP
      ↓
CONTRACT
      ↓
ENTITY / ENUM / REPOSITORY
      ↓
SERVICE BOUNDARY
      ↓
PERSISTENCE
      ↓
AUDIT EVENT CONTRACT
      ↓
SECURITY BOUNDARY
      ↓
BUILD
      ↓
PHASE 2A READY

Phase 2A không hoàn thành toàn bộ moderation workflow.

1. MỤC TIÊU

Phase 2A phải tạo được foundation để các phase sau có thể triển khai:

Product Moderation
Review Moderation
ReportCase
Violation
Audit Event

Kiến trúc mục tiêu:

                    MODERATION DOMAIN
                           │
        ┌──────────────────┼──────────────────┐
        │                  │                  │
     PRODUCT             REVIEW           REPORT CASE
   MODERATION          MODERATION             │
        │                  │                  │
        └──────────────────┼──────────────────┘
                           │
                       VIOLATION
                           │
                           ▼
                     AUDIT EVENT

Phase 2A không triển khai toàn bộ:

Moderator Queue
Moderator Dashboard
Approve UI
Reject UI
Escalation UI
Admin Enforcement UI
Scheduler
Complaint
Return
Refund

Các phần đó thuộc phase tiếp theo.

2. KẾT QUẢ ĐỐI CHIẾU SOURCE HIỆN TẠI

Theo source audit:

Đã tồn tại
Product
Review
Admin Product
Admin Review
UserRole
SecurityConfig

Product hiện có lifecycle như:

DRAFT
ACTIVE
OUT_OF_STOCK
HIDDEN

Review hiện có:

rating
comment

nhưng chưa có đầy đủ moderation state.

Chưa tồn tại hoặc chưa đủ
ModerationStatus
Review Moderation State
ReportCase
Violation
AuditLog
ModerationHistory
Moderation Decision Contract
Moderation Event Contract

Do đó Phase 2A phải bổ sung domain foundation, không chỉ sửa UI hiện tại.

3. NGUYÊN TẮC BẮT BUỘC
   Không xóa MongoDB

TUYỆT ĐỐI KHÔNG:

deleteAll()
deleteMany({})
drop()
dropDatabase()
collection reset
database cleanup

Không được tạo script:

cleanup()
resetDatabase()
clearData()

để phục vụ test Phase 2A.

4. KHÔNG FAKE PRODUCTION DATA

Không tạo dữ liệu giả để làm cho feature có vẻ đã hoạt động.

Không tạo:

fake violation
fake report
fake audit
fake moderation result
fake product state

chỉ để UI hiển thị.

Nếu cần test data:

TEST-PHASE-2A

và phải báo rõ số lượng.

Không xóa dữ liệu test sau đó.

5. KHÔNG PHỤ THUỘC DEVELOPER KHÁC

Phase 2A phải triển khai độc lập.

Không chờ:

developer khác
branch khác
commit khác
PR khác
backend khác
merge khác

Nếu dependency chưa tồn tại:

DEFINE CONTRACT
      ↓
IMPLEMENT OWNED FOUNDATION
      ↓
INTEGRATION-READY

Không báo:

BLOCKED - teammate chưa code

nếu phần Phase 2A vẫn có thể hoàn thành.

6. KHÔNG THAY ĐỔI GIT STATE

Không:

git checkout
git switch
git stash
git merge
git reset
git commit
git push

Không tự tạo branch.

Chỉ sửa những file thực sự thuộc Phase 2A.

7. SOURCE OF TRUTH

Thứ tự ưu tiên:

CURRENT SOURCE CODE
        ↓
GATE 0
        ↓
PHASE 1 FINAL RESULT
        ↓
PHASE 2A
        ↓
MARKETPLACE POLICY
        ↓
SHOPEE_MARKETPLACE_MODEL

Không lấy conceptual model để ép source phải giống hoàn toàn.

Nếu source đã có model tương đương:

REUSE

Không tạo:

ProductModerationV2
ReviewModerationV2
ViolationV2
AuditLogV2

chỉ vì tên khác.

8. DISCOVERY RULE

Trước khi tạo bất kỳ entity/service/enum nào:

SEARCH
   ↓
EXISTS?
 ├── YES
 │    ↓
 │  INSPECT
 │    ↓
 │  REUSE / EXTEND / FIX
 │
 └── NO
      ↓
    CREATE

Không tạo duplicate domain.

9. PHẠM VI DOMAIN

Phase 2A gồm:

Product Moderation Domain
Review Moderation Domain
ReportCase Domain
Violation Domain
Audit Event Contract
Moderation Decision Contract
Moderation History Contract
Security Boundary
Persistence Foundation
10. PRODUCT MODERATION FOUNDATION

Product hiện tại đã có lifecycle.

Không được biến:

Product.status

thành moderation state nếu hai khái niệm khác nhau.

Phải phân biệt:

PRODUCT LIFECYCLE

và:

MODERATION STATE

Nếu cần moderation state:

Product
 ├── lifecycle/status
 └── moderationStatus

hoặc cơ chế tương đương phù hợp với architecture hiện tại.

Không tạo enum thứ ba nếu không cần.

11. PRODUCT MODERATION STATUS

Nếu source chưa có moderation state, Phase 2A phải định nghĩa contract dựa trên Policy.

Ví dụ conceptual:

PENDING
APPROVED
REJECTED
MANUAL_REVIEW

Nhưng:

Không được tự động tạo toàn bộ enum trên nếu Policy/source không yêu cầu.

Trước khi quyết định state:

Policy
 ↓
Existing Product lifecycle
 ↓
Current Product workflow
 ↓
Moderation requirement
 ↓
Final state contract
12. PRODUCT MODERATION DECISION

Domain phải có khả năng biểu diễn decision:

APPROVE
REJECT
MANUAL_REVIEW

nếu những action này thuộc Policy.

Decision phải có khả năng truy nguyên:

Actor
Decision
Reason
Resource
Timestamp

Không nhất thiết phải dùng đúng tên field trên.

Reuse naming convention hiện tại.

13. REJECTION REASON

Nếu Product bị reject:

REJECT
   ↓
REASON

Reason phải được persistence nếu business rule yêu cầu.

Không chỉ:

UI message

mà phải tồn tại trong backend/domain.

Không tự tạo violation code ngoài Policy.

14. REVIEW MODERATION FOUNDATION

Source hiện tại có Review:

rating
comment

nhưng chưa có moderation state đầy đủ.

Phase 2A phải xác định contract:

Review
   ↓
Moderation State
   ↓
Decision
   ↓
Reason
   ↓
Actor

Nếu cần field mới:

moderationStatus
moderationReason
moderatedBy
moderatedAt

chỉ thêm field thực sự cần thiết.

Không thêm hàng loạt field theo conceptual model nếu không cần.

15. REVIEW VS DELETE

Source hiện tại có Admin Review delete.

Không coi:

DELETE REVIEW

là:

MODERATION

Phase 2A phải giữ rõ boundary:

Admin management

khác:

Moderation decision

Nếu Moderator cần:

hide
approve
reject

thì phải có moderation state hoặc contract tương ứng.

Không dùng delete như moderation state.

16. REPORT CASE

Phase 2A phải tạo foundation cho:

ReportCase

nếu source chưa có.

ReportCase dùng để đại diện cho moderation/report case.

Conceptual:

Reporter
Resource
Reason
Description
Evidence
Status
CreatedAt
UpdatedAt

Chỉ giữ các field thực sự cần.

17. REPORT CASE RESOURCE

ReportCase phải có khả năng xác định resource bị report.

Ví dụ:

Product
Review
Shop

nhưng chỉ những resource thuộc Phase 2.

Không tự mở rộng:

Order
Complaint
Refund

vì đó là domain Phase 3.

18. REPORT CASE STATUS

Nếu cần status:

OPEN
IN_REVIEW
RESOLVED
REJECTED

chỉ tạo state phù hợp với business requirement.

Không tạo state machine lớn nếu Phase 2A chưa cần.

19. REPORT CASE OWNERSHIP

ReportCase phải giữ được:

reporter
resource
createdAt

để phase sau có thể enforce:

Customer
   ↓
Own Report

và:

Moderator
   ↓
Authorized Case
20. VIOLATION FOUNDATION

Phase 2A tạo foundation:

Violation

Violation phải biểu diễn được policy breach.

Conceptual:

Actor
Policy Code
Violation Code
Severity
Action
Resource
Reason
Evidence
CreatedAt

Nhưng phải map vào architecture thực tế.

21. VIOLATION KHÔNG TỰ PHÁT MINH

Không tự thêm:

VIOLATION_X
VIOLATION_Y

nếu Marketplace Policy không định nghĩa.

Ví dụ nếu Policy có:

FALSE_PRODUCT_INFORMATION
MISLEADING_DESCRIPTION
MISLEADING_PRICE
FALSE_CLAIM
MISLEADING_MEDICAL_CLAIM

thì chỉ sử dụng các code được source/policy hỗ trợ.

22. VIOLATION SEVERITY

Nếu Policy yêu cầu severity:

Severity

phải được thiết kế.

Nhưng không tự tạo:

LOW
MEDIUM
HIGH
CRITICAL

nếu chưa có business basis.

23. VIOLATION ACTION

Violation có thể dẫn tới action theo Policy.

Không tự code:

BAN
SUSPEND
DELETE
FREEZE

trong Phase 2A nếu enforcement chưa thuộc scope.

Phase 2A chỉ tạo:

Violation Contract

để Phase sau sử dụng.

24. AUDIT EVENT CONTRACT

AuditLog hiện chưa tồn tại trong source.

Phase 2A không cần xây toàn bộ Audit platform.

Phải xây:

AUDIT EVENT CONTRACT

để các phase sau có thể ghi audit thống nhất.

Conceptual:

AuditEvent
 ├── actor
 ├── action
 ├── resourceType
 ├── resourceId
 ├── timestamp
 └── metadata

Tên field phải theo architecture thực tế.

25. AUDIT EVENT KHÔNG LOG PII

Không đưa:

password
token
full address
phone
private evidence URL

vào audit nếu không cần.

Audit chỉ ghi thông tin cần để truy nguyên action.

26. MODERATION HISTORY

Nếu cần history riêng:

ModerationHistory

phải phân biệt với:

AuditEvent

History:

business state history

Audit:

system/security action

Không tạo hai hệ thống trùng nhau nếu một abstraction hiện tại đủ dùng.

27. DOMAIN BOUNDARY

Phase 2A phải xác định rõ:

Product
   ↓
Product Moderation

Review
   ↓
Review Moderation

Customer Report
   ↓
ReportCase

Policy Breach
   ↓
Violation

System Action
   ↓
Audit Event

Không trộn:

ReportCase
Complaint
Escalation
Violation

thành một entity duy nhất.

28. PRODUCT MODERATION ≠ REPORTCASE

Một Product có thể:

Moderation

mà không cần ReportCase.

Một ReportCase có thể:

Report Product

và dẫn tới:

Moderation Review

Quan hệ phải rõ:

ReportCase
      ↓
Moderation decision
      ↓
Violation nếu Policy yêu cầu

Không duplicate cùng một nghiệp vụ.

29. REPORTCASE ≠ VIOLATION

ReportCase:

someone reported something

Violation:

system determined policy breach

Không tự động:

Report → Violation

nếu chưa có decision.

30. VIOLATION ≠ ENFORCEMENT

Phase 2A chỉ tạo:

Violation

Không xây toàn bộ:

Suspend Shop
Ban User
Freeze Payable

Enforcement thuộc phase sau.

31. AUDIT ≠ HISTORY

Không dùng AuditEvent để thay thế business history nếu architecture cần history.

Ngược lại cũng không dùng history để thay thế security audit.

32. REPOSITORY FOUNDATION

Nếu entity mới:

ReportCase
Violation
AuditEvent
ModerationHistory

thì tạo repository tương ứng chỉ khi service/domain thực sự cần persistence.

Không tạo repository chỉ để "cho đủ kiến trúc".

33. SERVICE BOUNDARY

Phase 2A phải tạo service boundary.

Ví dụ:

ProductModerationService
ReviewModerationService
ReportCaseService
ViolationService
AuditEventService

Tên thực tế phải theo architecture.

Không đưa business logic vào:

Controller
Template
JavaScript
Repository
34. CONTROLLER SCOPE

Phase 2A chỉ tạo controller/API nếu cần để:

create
read
persist
contract verification

Không triển khai toàn bộ Moderator workflow API nếu Phase 2B mới phụ trách.

35. UI SCOPE

Phase 2A:

NO FULL MODERATOR UI

Có thể tạo minimal integration surface nếu backend cần.

Không làm:

Moderator Queue
Case Detail UI
Approve Page
Reject Page
Escalation Page

Các phần này thuộc Phase 2B.

36. SECURITY FOUNDATION

Phải giữ nguyên security từ Phase 1:

MODERATOR
    ↓
/moderator/**

Phase 2A phải xác định permission boundary cho domain mới.

Ví dụ:

Customer
→ ReportCase create/read own

Moderator
→ ReportCase moderation scope

Admin
→ platform scope

Chi tiết phải khớp SecurityConfig hiện tại.

37. ADMIN BOUNDARY

Không biến:

AdminProductService
AdminReviewService

thành Moderator service.

Phải giữ:

ADMIN
=====

platform management / enforcement

và:

MODERATOR
=========

normal moderation workflow
38. PRODUCT OWNERSHIP

Vendor chỉ được thao tác Product thuộc Shop của mình.

Phase 2A phải đảm bảo domain contract không phá ownership hiện tại.

Không tin:

shopId
vendorId
productId

do client gửi nếu backend có thể xác định từ authenticated user.

39. REVIEW OWNERSHIP

Nếu Review thuộc Order/Customer:

phải preserve ownership relationship hiện tại.

Không cho Moderator/Customer API truy cập arbitrary Review nếu permission không cho phép.

40. IDOR FOUNDATION

Các domain mới phải được thiết kế để chống:

ReportCase A → ReportCase B
Violation A → Violation B
Product A → Product B
Review A → Review B

nếu actor không có quyền.

41. MONGODB SAFETY

Phase 2A tuyệt đối không:

drop database
drop collection
deleteMany({})
deleteAll()
reset database

Migration nếu cần phải:

NON-DESTRUCTIVE

Không sửa/xóa dữ liệu production hiện tại chỉ để phù hợp schema mới.

42. EXISTING DATA REGRESSION

Sau Phase 2A phải đảm bảo dữ liệu hiện tại còn nguyên:

User
Shop
Product
Order
OrderItem
Review
Voucher
Banner

Không được bị xóa.

43. SCHEMA EVOLUTION

Nếu cần thêm field:

moderationStatus
moderationReason
...

phải đảm bảo document cũ vẫn đọc được.

Ưu tiên:

backward-compatible

Không yêu cầu reset MongoDB.

44. TEST DATA

Nếu cần test:

[TEST-PHASE-2A]

Chỉ insert lượng nhỏ.

Ví dụ:

1 ReportCase
1 Violation
1 moderation test record

Không tạo hàng loạt.

Không xóa sau test.

45. KHÔNG FAKE AUDIT

Không tạo:

AuditLog

chỉ để dashboard hiển thị "Audit OK".

Audit event phải được tạo từ action thật.

Nếu chưa có action caller:

CONTRACT READY

không fake event.

46. KHÔNG FAKE VIOLATION

Không tạo violation chỉ để chứng minh collection tồn tại.

Violation phải đến từ:

actual moderation decision

hoặc test case rõ ràng.

47. KHÔNG FAKE REPORTCASE

ReportCase phải có nguồn:

actual report

hoặc test record được đánh dấu rõ.

48. POLICY INTEGRATION

Phase 2A phải đọc các section Policy liên quan:

PRODUCT_CONTENT_POLICY
PRODUCT MODERATION POLICY
REVIEW POLICY
REPORT POLICY
VIOLATION POLICY

Chỉ đọc phần cần thiết.

Không cần đọc lại toàn bộ Policy nếu section liên quan đã được xác định.

49. SHOPEE_MARKETPLACE_MODEL

Dùng SHOPEE_MARKETPLACE_MODEL.md làm conceptual baseline để kiểm tra:

Product
Review
Moderation
Report
Violation
Audit

Nhưng:

Conceptual model
≠
mandatory implementation schema

Source architecture có quyền quyết định tên entity/field phù hợp.

50. DEPENDENCY RULE

Phase 2A dependency:

Gate 0
   ↓
Phase 1 Security
   ↓
Phase 2A
   ↓
Phase 2B Moderator Workflow
   ↓
Phase 3 Complaint / Escalation

Không đảo dependency.

51. PHASE 2A KHÔNG LÀM

Không triển khai:

Complaint
Return
Refund
Escalation
Scheduler
Finance
Escrow
Settlement
KYC
Dashboard metrics

Không làm Moderator full UI.

52. PHASE 2A INPUT

Input bắt buộc:

Current Source Code
Gate 0
Phase 1 Final Result
Marketplace Policy
SHOPEE_MARKETPLACE_MODEL

Input thực tế phải xác định:

UserRole
SecurityConfig
Product
Review
Shop
Order
existing Admin Product
existing Admin Review
existing authentication
53. PHASE 2A OUTPUT

Sau Phase 2A phải có:

Product Moderation Contract
        ↓
Review Moderation Contract
        ↓
ReportCase Domain
        ↓
Violation Domain
        ↓
Audit Event Contract
        ↓
Persistence Foundation
        ↓
Security Boundary
        ↓
Build PASS

Để Phase 2B có thể bắt đầu mà không phải thiết kế lại domain.

54. ACCEPTANCE CRITERIA — PRODUCT

Phải xác minh:

Product moderation state
        +
Decision contract
        +
Reason
        +
Persistence

Nếu chưa có runtime workflow:

FOUNDATION READY

không ghi:

PRODUCT MODERATION IMPLEMENTED
55. ACCEPTANCE CRITERIA — REVIEW

Phải có:

Review moderation state/contract
Decision
Reason
Actor
Persistence boundary

Nếu chưa có Moderator UI:

UI = OUT OF SCOPE
56. ACCEPTANCE CRITERIA — REPORTCASE

Phải xác định:

ReportCase entity
Repository
Service boundary
Resource relationship
Status
Ownership/security boundary
57. ACCEPTANCE CRITERIA — VIOLATION

Phải xác định:

Violation entity
Policy code
Violation code
Resource
Reason
Actor
Timestamp

theo schema thực tế.

58. ACCEPTANCE CRITERIA — AUDIT EVENT

Phải có:

Audit Event Contract
Actor
Action
Resource
Timestamp

và boundary để phase sau ghi event.

Không bắt buộc Phase 2A phải hoàn thành toàn bộ Audit infrastructure nếu đó là scope riêng của phase sau.

59. ACCEPTANCE CRITERIA — SECURITY

Kiểm tra tối thiểu:

MODERATOR → /moderator/**       ALLOW
MODERATOR → /admin/**           DENY
CUSTOMER → moderation admin API DENY
VENDOR → unauthorized resource DENY

và IDOR cho domain mới.

60. ACCEPTANCE CRITERIA — DATA

Bắt buộc:

MongoDB existing data preserved
No collection dropped
No deleteAll
No deleteMany({})
No database reset
61. ACCEPTANCE CRITERIA — BUILD

Chạy:

mvn clean package -DskipTests

Không tự động chạy:

mvn test
mvn verify
mvn install

Nếu project không có test:

NOT AVAILABLE / NO TESTS

Không ghi:

TEST PASS

chỉ vì Maven báo No tests to run.

62. TEST PLAN
    Test 1 — Product Domain

Kiểm tra:

Product
↓
Moderation state
↓
Persistence
Test 2 — Review Domain

Kiểm tra:

Review
↓
Moderation state
↓
Persistence
Test 3 — ReportCase

Kiểm tra:

Create ReportCase
↓
Database
↓
Read
Test 4 — Violation

Kiểm tra:

Create/derive Violation
↓
Database
↓
Read
Test 5 — Audit Contract

Kiểm tra:

Domain Action
↓
Audit Event

nếu caller đã được triển khai.

Nếu chưa:

Contract verified
63. SECURITY TEST

Kiểm tra:

Moderator → own authorized domain
ALLOW
Moderator → /admin/**
DENY
Customer → moderator endpoint
DENY
Vendor → unauthorized product/report
DENY
64. DATA PRESERVATION TEST

Trước implementation ghi nhận:

existing User count
existing Shop count
existing Product count
existing Order count
existing Review count

Không cần dump toàn bộ database.

Sau implementation:

Existing records preserved

Không delete dữ liệu để đạt expected count.

65. REGRESSION

Không test lại toàn bộ project.

Chỉ regression:

Authentication
SecurityConfig
Product
Review
Shop ownership
Admin Product
Admin Review

nếu Phase 2A thay đổi shared entity/service.

66. STATUS CLASSIFICATION

Chỉ sử dụng:

IMPLEMENTED
INTEGRATED
INTEGRATION-READY
PARTIAL
MISSING
WRONG
OUT OF SCOPE
BLOCKED

BLOCKED chỉ dùng khi thực sự không thể tiếp tục kỹ thuật.

Không dùng:

BLOCKED — teammate chưa code
67. INTEGRATION STATUS

Mỗi dependency:

Integration Point:
<name></name>

Existing Contract:
<entity/service/interface>

Phase 2A Implementation:
<what was created></what>

External Implementation:
AVAILABLE / NOT AVAILABLE

Status:
IMPLEMENTED / INTEGRATION-READY / PARTIAL / MISSING / OUT OF SCOPE

Notes:
<evidence></evidence>
68. FILE CHANGE REPORT

Cuối Phase:

Files Created:

- ...

Files Modified:

- ...

Files Deleted:
NONE

Không xóa file chỉ vì không dùng.

Không refactor toàn project.

69. DATA CHANGE REPORT

Bắt buộc:

MongoDB:
Existing data deleted: NONE
Database reset: NO
Collection dropped: NO
deleteAll: NO
deleteMany({}): NO

Test records inserted:
<number></number>

Test records deleted:
NO

Existing data preserved:
YES / NO / NOT VERIFIED
70. BUILD REPORT
Command:
mvn clean package -DskipTests

Result:
PASS / FAIL

Nếu fail:

Root Cause:
...

Affected File:
...

Dependency:
...

Status:
...
71. MANUAL TEST REPORT

Báo chính xác:

Product Moderation Foundation:
PASS / FAIL / NOT TESTED

Review Moderation Foundation:
PASS / FAIL / NOT TESTED

ReportCase:
PASS / FAIL / NOT TESTED

Violation:
PASS / FAIL / NOT TESTED

Audit Event Contract:
PASS / FAIL / NOT TESTED

Security:
PASS / FAIL / NOT TESTED

IDOR:
PASS / FAIL / NOT TESTED

Data Preservation:
PASS / FAIL / NOT VERIFIED

Build:
PASS / FAIL

Không ghi PASS nếu chưa thực sự kiểm tra.

72. KNOWN ISSUES

Mỗi issue:

Issue:
...

Current Behavior:
...

Expected Behavior:
...

Root Cause:
...

Source:
...

Severity:
...

Phase:
Phase 2A

Status:
...
73. PHASE 2B HANDOFF

Phase 2A phải bàn giao foundation:

Product Moderation
        ↓
Review Moderation
        ↓
ReportCase
        ↓
Violation
        ↓
Audit Event
        ↓
Security

cho:

PHASE 2B — MODERATOR WORKFLOW

Phase 2B sẽ xây:

Moderator Queue
Moderator Case Detail
Approve
Reject
Request Evidence
Escalate
Violation creation
ReportCase handling
74. KHÔNG LÀM MẤT CHỨC NĂNG HIỆN TẠI

Không được làm hỏng:

Authentication
Authorization
Admin Product
Admin Review
Shop
Product
Review
Order
Voucher
Banner
Cart
Checkout

Nếu sửa shared entity:

Regression Required
75. FINAL ARCHITECTURE SAU PHASE 2A
                         USER
                          │
              ┌───────────┴───────────┐
              │                       │
           PRODUCT                  REVIEW
              │                       │
              ▼                       ▼
       PRODUCT MODERATION      REVIEW MODERATION
              │                       │
              └───────────┬───────────┘
                          │
                          ▼
                    MODERATION
                       DECISION
                          │
             ┌────────────┴────────────┐
             │                         │
          REPORT                    VIOLATION
             │                         │
             └────────────┬────────────┘
                          │
                          ▼
                    AUDIT EVENT

Phase 2B mới tiếp tục:

                         MODERATION DOMAIN
                                │
                                ▼
                         MODERATOR QUEUE
                                │
                         ┌──────┴──────┐
                         │             │
                      RESOLVE       ESCALATE
                                       │
                                       ▼
                                  ADMIN DOMAIN
76. FINAL RULE

Trong toàn bộ Phase 2A:

KHÔNG ĐỌC LẠI TOÀN BỘ SOURCE NẾU KHÔNG CẦN

KHÔNG LÀM LẠI PHASE 1

KHÔNG DUPLICATE DOMAIN

KHÔNG FAKE PRODUCTION DATA

KHÔNG FAKE AUDIT

KHÔNG FAKE VIOLATION

KHÔNG FAKE REPORTCASE

KHÔNG XÓA MONGODB

KHÔNG RESET MONGODB

KHÔNG DELETE TEST DATA

KHÔNG COMMIT

KHÔNG PUSH

KHÔNG MERGE

KHÔNG CHECKOUT

KHÔNG STASH

KHÔNG CHỜ DEVELOPER KHÁC

KHÔNG TỰ PHÁT MINH BUSINESS RULE

KHÔNG COI FILE EXISTS = FEATURE DONE

KHÔNG COI TEST CLASS EXISTS = TEST PASS

KHÔNG COI MENU EXISTS = BACKEND DONE

KHÔNG COI AUDIT REPORT = IMPLEMENTATION EVIDENCE

CHỈ IMPLEMENT FOUNDATION THUỘC PHASE 2A
77. ĐIỀU KIỆN KẾT THÚC

Phase 2A chỉ được đánh dấu IMPLEMENTED khi foundation thực sự tồn tại và build/kiểm chứng được:

Product Moderation Contract
        +
Review Moderation Contract
        +
ReportCase
        +
Violation
        +
Audit Event Contract
        +
Persistence
        +
Security Boundary
        +
Backward Compatibility
        +
MongoDB Data Preservation
        +
Build PASS

Nếu một phần chưa có implementation nhưng contract đã hoàn chỉnh:

INTEGRATION-READY

Nếu mới làm được một phần:

PARTIAL

Nếu chưa tồn tại:

MISSING

Phase 2A không được tự nhận là hoàn thành Moderator workflow. Nó chỉ có nhiệm vụ tạo domain foundation đủ chắc để Phase 2B triển khai workflow mà không phải thiết kế lại Product Moderation, Review Moderation, ReportCase, Violation và Audit từ đầ
