
PHASE 3A — COMPLAINT + RETURN/REFUND CONTRACT

Mục tiêu của Phase 3A: xây dựng và chốt domain contract + persistence foundation cho Complaint và phần tích hợp Return / Refund, bám sát code hiện tại và không giả định rằng workflow Complaint/Return/Refund đã tồn tại chỉ vì Phase 3 cũ mô tả nó.

Phạm vi này được tách ra từ Phase 3 cũ. Escalation, Admin Enforcement, Scheduler sẽ được xử lý ở các phase tiếp theo, không triển khai lẫn vào Phase 3A.

Tài liệu Phase 3 cũ yêu cầu phải xác minh từ Requirement → Current Source → Dependency → API/Service → Repository/DB → Security → UI → Runtime/Build/Test, và không được coi report cũ là bằng chứng implementation.

0. TÊN PHASE MỚI
   Tên chính thức
   Phase 3A — Complaint + Return/Refund Contract
   Không gọi là:
   Phase 3 — Complaint + Return/Refund + Escalation + Scheduler

vì Phase 3 đã được tách thành các phần nhỏ hơn.

Phân ranh giới
Phase 3A
Complaint
Return Contract
Refund Contract
Persistence
Ownership/Security
Evidence Contract
Audit integration seam nếu cần
        ↓
Phase 3B
Complaint Workflow
Moderator/Admin handling
Escalation
Enforcement
        ↓
Phase 3C
Scheduler
Deadline
Idempotency
Overdue Escalation

1. MỤC TIÊU

Phase 3A không nhằm khẳng định rằng toàn bộ Complaint lifecycle đã hoàn chỉnh.

Mục tiêu là tạo một nền tảng đủ rõ để các phase sau có thể triển khai:

Customer
   ↓
Complaint
   ↓
Order / OrderItem
   ↓
Shop / Vendor
   ↓
Evidence
   ↓
Return
   ↓
Refund

Trong đó:

Complaint
    = domain của tranh chấp liên quan Order / OrderItem

ReportCase
    = domain của moderation / policy report

Không tạo một domain thứ hai nếu architecture hiện tại đã có abstraction phù hợp.

2. BÁM SÁT AUDIT HIỆN TẠI

Theo source audit đã đối chiếu:

Order
OrderItem
Shop
User
Product
Review
Admin Order
Admin Product
Admin Review

đã có các thành phần hiện hữu.

Nhưng audit không xác nhận có backend hoàn chỉnh cho:

Complaint
Return
Refund
Escrow
Payment refund
Complaint Scheduler

Do đó Phase 3A không được giả định các capability trên đã tồn tại.

Đặc biệt:

Có Order
≠
Có Complaint

Có Admin Order
≠
Có Return workflow

Có transaction/order money
≠
Có Refund backend

Có dashboard revenue
≠
Có Finance/Refund settlement
3. KẾT QUẢ MONG MUỐN

Sau Phase 3A phải xác định được rõ:

Complaint
├── Domain boundary
├── Entity/model
├── State contract
├── Persistence
├── Order relationship
├── OrderItem relationship
├── Customer ownership
├── Vendor/Shop relationship
├── Evidence contract
└── Service contract

Return
├── Existing capability?
├── Existing contract?
├── Persistence?
└── Integration status

Refund
├── Existing capability?
├── Existing contract?
├── Financial dependency?
└── Integration status

Kết quả cuối cùng có thể là:

IMPLEMENTED
PARTIAL
MISSING
INTEGRATION-READY
WRONG
BUSINESS DECISION REQUIRED

Không được ép tất cả thành IMPLEMENTED.

4. SOURCE OF TRUTH

Thứ tự kiểm tra:

Current Source Code
        ↓
Current Domain Model
        ↓
Existing Order / OrderItem architecture
        ↓
Existing Payment / Financial capability
        ↓
Phase 1 Final Result
        ↓
Phase 2 Final Result
        ↓
Phase 3A Specification
        ↓
Marketplace Policy
        ↓
SHOPEE_MARKETPLACE_MODEL

Trong Phase 3A:

Current Source

là bằng chứng implementation.

Report cũ chỉ là baseline để biết cần kiểm tra gì.

Không được suy luận:

Phase 3 cũ ghi DONE
        ↓
Code hiện tại DONE
5. TÀI LIỆU BẮT BUỘC

Nếu các tài liệu tồn tại trong project/context, đọc:

1. BỘ LUẬT & CHÍNH SÁCH CỦA SÀN MARKETPLACE – Version 1.0
2. SHOPEE_MARKETPLACE_MODEL.md
3. Phase 1 Final Report
4. Phase 2 Final Report
5. Current source audit
6. Phase 3A Specification này

Đặc biệt phải tìm các policy liên quan:

Return Policy
Refund Policy
Complaint Policy
Warranty Policy
Order Cancellation Policy
Payment Policy
Escrow / Settlement Policy
Evidence Policy

Nếu policy không quy định:

Không tự đặt business rule.
6. KHÔNG TỰ PHÁT MINH BUSINESS RULE

Không tự quyết định:

Complaint reason
Complaint status
Return deadline
Refund deadline
Refund percentage
Refund amount
Refund fee
Return eligibility
Number of complaints
Escalation threshold
Vendor penalty
Automatic refund
Automatic return approval

nếu Policy/Source chưa quy định.

Ví dụ không được tự thêm:

48h → refund
3 complaints → suspend
5 complaints → ban
refund = 100%
return = 7 days

nếu chưa có source-of-truth xác nhận.

Nếu business decision chưa được chốt:

BUSINESS DECISION REQUIRED
7. NGUYÊN TẮC REUSE

Trước khi tạo bất kỳ file nào:

Search
  ↓
Existing?
  ├── YES → REUSE / MODIFY
  └── NO  → CREATE ONLY IF REQUIRED

Không tạo:

ComplaintV2
ComplaintCase
ComplaintCaseV2
ReturnRequestV2
RefundServiceV2
RefundContractV2

chỉ vì phase cũ dùng tên khác.

8. DISCOVERY — COMPLAINT

Trước tiên inspect:

Complaint
ComplaintStatus
ComplaintLevel
ComplaintRepository
ComplaintService
ComplaintServiceImpl
ComplaintController
Complaint DTO
Complaint template

Nếu không tồn tại:

MISSING

và tiếp tục xác định domain contract.

Không được báo:

BLOCKED

chỉ vì Complaint chưa tồn tại.

9. DISCOVERY — REPORTCASE

Inspect:

ReportCase
ReportCaseRepository
ReportCaseService
ReportCaseStatus

Mục tiêu là xác định boundary:

ReportCase
Product report
Seller report
Policy violation
Moderation case
Complaint
Order issue
OrderItem issue
Return
Refund
Non-delivery
Wrong item
Missing item
Damaged item
Warranty dispute

Nếu ReportCase hiện tại có thể đại diện đúng domain Complaint:

REUSE

Nếu không:

Complaint domain riêng

Nhưng phải ghi rõ boundary.

Không tạo song song:

Complaint
+
ReportCase
+
ComplaintCase

mà không có domain boundary.

10. COMPLAINT DOMAIN CONTRACT

Nếu Complaint thực sự cần entity riêng, tối thiểu phải xác định được:

Complaint
├── id
├── customer
├── order
├── orderItem
├── shop/vendor relationship
├── reason
├── description
├── evidence
├── status
├── timestamps
└── resolution/decision information nếu Policy yêu cầu

Không bắt buộc phải có toàn bộ field trên.

Mỗi field phải trả lời được:

Phase 3A cần?
Policy yêu cầu?
Architecture hiện tại chưa có?

Nếu không:

DO NOT ADD
11. COMPLAINT vs ORDER

Complaint phải có quan hệ rõ với Order.

Expected conceptual relationship:

Complaint
   ↓
Order
   ↓
OrderItem
   ↓
Product
   ↓
Shop
   ↓
Vendor

Không được chỉ tin dữ liệu frontend gửi:

customerId
orderId
shopId
productId

Backend phải xác minh relationship thực tế.

12. CUSTOMER OWNERSHIP

Khi Customer tạo Complaint:

Authenticated Customer
        ↓
Order
        ↓
Order belongs to Customer

Backend phải enforce.

Không chấp nhận:

POST /complaints
customerId = A
orderId = B

nếu Order B không thuộc Customer A.

13. ORDERITEM OWNERSHIP

Nếu Complaint gắn với OrderItem:

Complaint
   ↓
OrderItem
   ↓
Order

phải xác minh:

OrderItem thuộc Order
Order thuộc Customer

Không chỉ kiểm tra:

orderItemId exists
14. SHOP / VENDOR RELATIONSHIP

Nếu Complaint liên quan Shop:

Complaint
   ↓
OrderItem
   ↓
Product
   ↓
Shop
   ↓
Vendor

Backend phải xác định relationship từ dữ liệu hiện tại.

Không cho client tùy ý gửi:

shopId
vendorId

rồi coi đó là source-of-truth.

15. COMPLAINT CREATE CONTRACT

Phase 3A cần xác định contract cho việc tạo Complaint.

Conceptual flow:

Customer
   ↓
Select Order
   ↓
Select OrderItem
   ↓
Reason
   ↓
Description
   ↓
Evidence
   ↓
Create Complaint

Backend validation:

Customer authenticated
Order exists
Order belongs to Customer
OrderItem belongs to Order
Product relation valid
Shop/Vendor relation valid
Request complies with Policy

Nếu Policy yêu cầu điều kiện khác:

Bám Policy

Không tự thêm điều kiện.

16. COMPLAINT STATE CONTRACT

Đây là phần cần đặc biệt thận trọng.

Audit hiện tại không chứng minh Complaint có state machine hoàn chỉnh.

Do đó:

Không tự tạo hàng loạt enum

ví dụ:

OPEN
VENDOR_REVIEW
MODERATOR_REVIEW
ADMIN_REVIEW
RESOLVED
REJECTED
CLOSED

trừ khi Policy/architecture yêu cầu.

Phải xác định:

Existing status?
Existing workflow?
Policy status?
Required transitions?

Nếu chưa đủ căn cứ:

BUSINESS DECISION REQUIRED
17. COMPLAINT LEVEL

Phase 3 cũ mô tả:

Level 0
Customer ↔ Vendor

Level 1
Moderator

Level 2
Admin

Nhưng Phase 3A không triển khai workflow escalation.

Phase 3A chỉ cần xác định:

Complaint có cần level field không?
Nếu có:
    field nào?
    enum nào?
    source nào?

Không tự tạo:

ComplaintLevelV2
18. COMPLAINT HISTORY

Phase 3A cần xác định contract cho history nếu workflow sau này cần state transition.

Có thể cần:

created
status changed
evidence added
response added
decision recorded

Nhưng không tạo history system thứ hai nếu project đã có:

Audit
ModerationHistory
Event

Phải phân biệt:

Domain History
≠
Audit Event

nếu architecture yêu cầu cả hai.

19. EVIDENCE CONTRACT

Complaint có thể cần:

Image
Document
Description
Order information

Nhưng phải reuse Upload infrastructure hiện tại.

Không tạo:

ComplaintUploadServiceV2

nếu Upload service hiện tại đủ khả năng.

20. EVIDENCE SECURITY

Không cho phép truy cập evidence chỉ bằng:

fileId
filename
public URL

Phải kiểm tra:

Authenticated user
+
Complaint access
+
Evidence ownership/permission

Customer A không được đọc evidence của Complaint B.

Vendor cũng không được đọc case ngoài Shop scope nếu Policy không cho phép.

21. PII

Complaint có thể chứa:

Customer information
Address
Phone
Evidence
Order information

Không log PII không cần thiết.

Không đưa PII vào:

URL
Query parameter
Exception message
Audit metadata
Debug log

nếu không cần.

Reuse security convention hiện tại.

22. RETURN — DISCOVERY TRƯỚC

Audit hiện tại không xác nhận Return backend hoàn chỉnh.

Trước khi tạo:

Search:
Return
ReturnRequest
ReturnStatus
ReturnRepository
ReturnService

và:

Order return
Order cancellation
Payment

Nếu đã có:

REUSE

Nếu không:

DEFINE RETURN CONTRACT
23. RETURN CONTRACT

Contract phải xác định được:

Complaint
   ↓
Return Request
   ↓
Order / OrderItem

và các relationship cần thiết.

Không tự tạo workflow hoàn chỉnh nếu chưa có business rule.

Phase 3A chỉ cần đủ để Phase 3B/financial integration biết:

Return request là gì?
Gắn với entity nào?
Persistence ở đâu?
Service boundary ở đâu?
State được quản lý ở đâu?
24. RETURN STATE

Không tự tạo:

RETURN_REQUESTED
RETURN_APPROVED
RETURN_IN_TRANSIT
RETURN_RECEIVED
RETURN_REJECTED

chỉ để schema “đẹp”.

Trước tiên kiểm tra:

Existing Return state
Policy
Order lifecycle

Nếu không có state chính thức:

BUSINESS DECISION REQUIRED
25. RETURN OWNERSHIP

Nếu Customer tạo Return từ Complaint:

Customer
 ↓
Complaint
 ↓
Order
 ↓
OrderItem

phải verify ownership.

Không cho:

Customer A
→ Return Order B
26. RETURN — KHÔNG XÓA ORDER HISTORY

Return không được làm:

delete Order
delete OrderItem
delete history

Order history phải được giữ lại.

Return là lifecycle record bổ sung.

27. REFUND — DISCOVERY

Audit hiện tại cũng không xác nhận production Refund backend.

Phải inspect:

Refund
RefundRequest
RefundService
PaymentService
PaymentRepository
Wallet
Escrow
Settlement
Transaction

nếu các capability này tồn tại.

28. REFUND CONTRACT

Nếu financial backend tồn tại:

REUSE

Nếu chưa:

Define Refund Contract
        ↓
Integration Seam
        ↓
INTEGRATION-READY

Không tạo:

FakeRefundService

để làm UI chạy.

29. REFUND KHÔNG ĐƯỢC FAKE

Tuyệt đối không làm:

balance += refundAmount;

hoặc:

refundStatus = SUCCESS

mà không có financial operation thực tế.

Không fake:

Refund amount
Escrow amount
Vendor payable
Settlement
Wallet balance
Payment transaction
30. REFUND AMOUNT

Không tự quyết định:

refund = 100%
refund = item price
refund = item price - fee
refund = order total

nếu Policy chưa xác định.

Nếu amount phải được quyết định ở external financial service:

Refund contract
+
integration seam

là đủ cho Phase 3A.

31. REFUND RELATIONSHIP

Refund nếu tồn tại phải xác định relationship:

Refund
 ├── Order
 ├── Complaint
 ├── Return
 └── Financial transaction

Không bắt buộc tất cả field phải tồn tại nếu architecture hiện tại dùng relationship khác.

Điểm quan trọng là:

Không có refund record mồ côi
Không có refund không gắn với business event hợp lệ
32. ESCROW / SETTLEMENT BOUNDARY

Phase 3A không triển khai Finance/Settlement.

Chỉ xác định integration point:

Complaint
   ↓
Return / Refund decision
   ↓
Existing Financial Contract

Nếu financial backend chưa có:

INTEGRATION-READY

Không tạo fake:

Vendor Payable
Frozen Funds
Refund Balance
Settlement Amount
33. COMPLAINT → RETURN

Chỉ cần tạo integration contract:

Complaint
   ↓
Return Request

Không triển khai:

Scheduler
Automatic approval
Automatic shipment tracking
Automatic refund

trong Phase 3A nếu chưa thuộc contract hiện tại.

34. COMPLAINT → REFUND

Contract:

Complaint
   ↓
Resolution
   ↓
Refund Request / Refund Contract

Nếu financial capability chưa tồn tại:

INTEGRATION-READY

Không coi đó là:

FAILURE

nếu contract đã sẵn sàng.

35. COMPLAINT vs REPORTCASE vs VIOLATION

Phải giữ boundary:

ReportCase
    = moderation/report

Complaint
    = customer order dispute

Violation
    = policy enforcement record

Ví dụ:

Customer reports prohibited product
        ↓
ReportCase

Trong khi:

Customer receives wrong product
        ↓
Complaint
        ↓
Return / Refund

Nếu Complaint phát hiện policy violation:

Complaint
   ↓
Existing ViolationService

Không tạo:

ComplaintViolation

nếu không cần.

36. ESCALATION — OUT OF SCOPE PHASE 3A

Không triển khai ở Phase 3A:

Moderator Escalation Queue
Admin Escalation Queue
Escalation Scheduler
Overdue Detection
Automatic Escalation
Admin Enforcement

Các phần này bàn giao cho:

Phase 3B
Phase 3C

Phase 3A chỉ phải để contract không cản trở các phase đó.

37. SCHEDULER — OUT OF SCOPE

Không tạo:

@Scheduled

cho Complaint trong Phase 3A.

Không triển khai:

48h escalation
5 day escalation
overdue scanner
idempotency record

ở phase này.

Đó là Phase 3C.

38. AUDIT INTEGRATION

Nếu Phase 1/2 đã có Audit contract:

REUSE

Không tạo Audit system mới.

Phase 3A chỉ cần xác định event contract nếu Complaint/Return/Refund cần phát event:

ComplaintCreated
ComplaintUpdated
EvidenceAdded
ReturnRequested
RefundRequested

Tên event cuối cùng phải theo architecture hiện tại.

Không tự tạo event taxonomy thứ hai.

39. NOTIFICATION — KHÔNG FAKE

Nếu Notification infrastructure đã có:

REUSE

Nếu chưa:

Notification Contract
+
Integration Seam

Không fake:

email sent
SMS sent
push sent

chỉ bằng cách ghi log.

Notification implementation đầy đủ có thể thuộc phase khác nếu không cần để Phase 3A hoạt động.

40. SECURITY MATRIX
    Actor	Own Complaint	Other Complaint	Own Order Return	Other Order Return	Refund
    CUSTOMER	ALLOW	DENY	ALLOW nếu Policy	DENY	Theo ownership/policy
    VENDOR	Theo Shop ownership	DENY	Theo Shop ownership	DENY	Không tự ý
    MODERATOR	Theo permission	Theo permission	Theo permission	Theo permission	Theo permission
    ADMIN	Theo Admin scope	Theo Admin scope	Theo Admin scope	Theo Admin scope	Theo Admin/financial scope

Không hard-code matrix này nếu current security architecture có rule cụ thể khác được Policy hỗ trợ.

Phải test behavior thực tế.

41. IDOR TEST

Tối thiểu phải kiểm tra contract/security cho:

Customer A → Complaint B
Customer A → Order B Complaint

Vendor A → Complaint of Shop B
Vendor A → Return of Shop B

Customer → Moderator endpoint
Vendor → Moderator endpoint

Customer → Admin endpoint
Vendor → Admin endpoint

Expected:

DENIED

nếu không có permission hợp lệ.

42. CONTROLLER / SERVICE / REPOSITORY

Business logic:

Controller
    ↓
Service
    ↓
Repository

Controller không được chứa:

ownership logic
refund calculation
return decision
policy decision

Service chịu trách nhiệm business behavior.

Repository chịu persistence.

43. PERSISTENCE

Complaint persistence phải được kiểm tra thực tế:

Entity/Document
   ↓
Repository
   ↓
MongoDB

Không chỉ tạo class:

Complaint.java

rồi coi feature hoàn thành.

Phải xác định:

collection
indexes nếu cần
relationships
serialization
timestamps
existing data compatibility
44. MONGODB SCHEMA SAFETY

Nếu thêm field mới vào entity hiện tại:

Existing MongoDB documents

phải tiếp tục đọc được.

Không bắt buộc migrate hàng loạt nếu không cần.

Ưu tiên:

nullable
default
backward-compatible mapping

theo convention hiện tại.

Không thực hiện destructive migration.

45. INDEX

Nếu Complaint cần index:

customer
order
orderItem
shop/vendor
status
createdAt

chỉ tạo index thực sự cần.

Index creation phải:

non-destructive

Không reset collection để tạo index.

46. MONGODB — TUYỆT ĐỐI KHÔNG XÓA

Cấm tuyệt đối:

deleteAll()
deleteMany({})
drop()
dropDatabase()
truncate
reset collection
reset database
destructive migration

Không được xóa dữ liệu vì:

test fail
schema conflict
duplicate
build fail
debug
manual test
integration fail

Nguyên tắc này kế thừa trực tiếp từ phase cũ.

47. TEST DATA

Nếu thực sự cần test data:

INSERT ONLY

Số lượng nhỏ:

1–2 records / scenario

Có marker:

[TEST-PHASE-3A]

Ví dụ:

[TEST-PHASE-3A] Complaint
[TEST-PHASE-3A] Return

Không:

delete test data

sau test.

48. KHÔNG FAKE PRODUCTION DATA

Không tạo hàng trăm:

Complaint
Return
Refund
Order

để làm dashboard hoặc metric trông “đúng”.

Không fake:

refund success
financial balance
vendor payable
complaint statistics
49. GIT SAFETY

Trước khi code:

git status
git branch
git log --oneline --decorate -10

Không được:

git reset
git stash
git checkout
git switch
git merge
git rebase
git commit
git push

Không overwrite uncommitted changes.

50. KHÔNG LÀM LẠI CÁC PHASE CŨ

Không rebuild:

Moderator Foundation
Moderator Security
Product Moderation
ReportCase
Violation
Admin Escalation
Admin Enforcement

Nếu dependency cũ có bug trực tiếp làm Complaint không hoạt động:

Trace
 ↓
Root Cause
 ↓
Minimal Fix
 ↓
Regression

Không rewrite subsystem.

51. KHÔNG LẤN SANG PHASE 4

Phase 3A không triển khai:

Finance Dashboard
GMV
Platform Revenue
Vendor Sales
Vendor Payable Dashboard
Refund Dashboard
Final Integration
Performance Hardening
Full Regression
Banner verification

Đặc biệt không dùng:

totalPlatformRevenue

hiện có để giả định rằng hệ thống đã có Refund/Finance.

52. PHASE 3A DEPENDENCY RULE

Nếu phát hiện:

Complaint implementation
        ↓
OrderService bug

thì không báo:

OUT OF SCOPE – Order

nếu bug đó trực tiếp làm Complaint fail.

Báo:

Dependency:
OrderService

Issue:
<actual issue></actual>

Impact:
Complaint creation cannot verify ownership

Minimal Fix:
<fix></fix>

Regression:
PASS / FAIL / NOT TESTED
53. RETURN/REFUND DEPENDENCY RULE

Nếu Return chưa tồn tại:

Complaint:
IMPLEMENTED / PARTIAL

Return:
INTEGRATION-READY

Nếu Refund backend chưa tồn tại:

Refund:
INTEGRATION-READY

Không đánh dấu toàn bộ Phase 3A là BLOCKED chỉ vì financial backend chưa có.

BLOCKED chỉ dùng khi có technical blocker thực sự.

54. STATUS DEFINITIONS
    IMPLEMENTED

Code tồn tại, persistence/API/security phù hợp và behavior đã được kiểm chứng ở mức cần thiết.

INTEGRATION-READY

Contract và integration seam đã sẵn sàng nhưng external capability chưa tồn tại/chưa kết nối.

INTEGRATED

Backend thật đã kết nối và flow đã kiểm chứng.

PARTIAL

Có implementation nhưng chưa đầy đủ.

MISSING

Chưa có implementation.

WRONG

Có implementation nhưng behavior/architecture/business rule sai.

BUSINESS DECISION REQUIRED

Thiếu quyết định business chính thức, không được tự chọn.

OUT OF SCOPE

Không thuộc Phase 3A và không cần để Phase 3A hoạt động.

BLOCKED

Chỉ dùng cho technical blocker thực sự.

55. ACCEPTANCE CRITERIA

Phase 3A đạt khi đã xác định và triển khai được phần thuộc scope:

Complaint Domain
       ↓
Complaint Persistence
       ↓
Order Relationship
       ↓
OrderItem Relationship
       ↓
Customer Ownership
       ↓
Vendor/Shop Ownership
       ↓
Evidence Contract
       ↓
Return Contract
       ↓
Refund Contract
       ↓
Security

Và:

Return
    → INTEGRATED
    hoặc
    → INTEGRATION-READY

Refund
    → INTEGRATED
    hoặc
    → INTEGRATION-READY

Không fake financial behavior.

56. PHASE 3A KHÔNG YÊU CẦU

Không bắt buộc Phase 3A phải hoàn thành:

Customer ↔ Vendor full conversation
Moderator queue
Admin escalation
Automatic escalation
Scheduler
Deadline scanner
Refund settlement
Escrow settlement
Financial dashboard
Notification provider

Những phần đó được bàn giao cho phase tiếp theo nếu contract đã đủ.

57. MANUAL TEST

Theo workflow cũ, website chỉ được start khi user yêu cầu; không tự động chạy Spring Boot.

Khi được yêu cầu manual test:

Customer
Login
 ↓
Own Order
 ↓
Create Complaint

Expected:

ALLOW
IDOR
Customer A
 ↓
Complaint B

Expected:

DENY
Evidence
Customer
 ↓
Own Complaint
 ↓
Add Evidence

Expected:

ALLOW

nếu contract hỗ trợ.

Vendor
Vendor A
 ↓
Complaint of Shop A

Expected:

ALLOW

và:

Vendor A
 ↓
Complaint of Shop B

Expected:

DENY
58. RETURN TEST

Nếu Return backend thật tồn tại:

Complaint
 ↓
Return
 ↓
Persistence

kiểm tra actual behavior.

Nếu chưa có:

Return:
INTEGRATION-READY

Không fake.

59. REFUND TEST

Nếu financial backend thật tồn tại:

Complaint
 ↓
Return/Resolution
 ↓
Refund Contract
 ↓
Real Financial Backend

mới test actual refund.

Nếu chưa:

Refund:
INTEGRATION-READY

Không tạo refund giả để đánh dấu PASS.

60. BUILD RULE

Theo workflow phase cũ, chỉ chạy:

mvn clean package -DskipTests

Không tự động chạy:

mvn test
mvn verify
mvn install
mvn package

và không tự động start Spring Boot.

Nếu build fail:

Root Cause
Affected Module
Phase 3A caused: YES/NO/UNKNOWN
Dependency caused: YES/NO/UNKNOWN
Fix
Retest
61. FILE SCOPE

Chỉ inspect/create/modify trực tiếp các nhóm:

Complaint
Complaint
ComplaintStatus
ComplaintRepository
ComplaintService
ComplaintServiceImpl
ComplaintController
Complaint DTO
Order dependency
Order
OrderItem
OrderRepository
OrderService
ownership-related service
Return
Return
ReturnRequest
ReturnService
ReturnRepository

nếu thực sự tồn tại.

Refund
Refund
RefundService
Payment
Financial contract

nếu thực sự tồn tại.

Evidence
Upload
File metadata
Access control
Security
SecurityConfig
authorization
ownership checks

Không refactor toàn project.

62. FILE CHANGE REPORT

Sau implementation:

Files Created:

- ...

Files Modified:

- ...

Files Deleted:
NONE

Không tự xóa file.

Nếu phát hiện duplicate:

DUPLICATED / CONFLICT

phải báo cáo trước khi quyết định xử lý.

63. MONGODB DATA CHANGE REPORT

Bắt buộc:

MongoDB existing data:
PRESERVED

Existing data deleted:
NONE

Database reset:
NO

Collection dropped:
NO

deleteAll():
NOT USED

deleteMany({}):
NOT USED

Test records inserted:
<number></number>

Test records retained:
YES

Existing records modified:
<YES/NO + exact reason>
64. SECURITY REPORT

Báo:

Customer Ownership:
PASS / FAIL / NOT TESTED

Vendor Ownership:
PASS / FAIL / NOT TESTED

Complaint IDOR:
PASS / FAIL / NOT TESTED

Evidence Access:
PASS / FAIL / NOT TESTED

Return Ownership:
PASS / FAIL / NOT TESTED

Refund Authorization:
PASS / FAIL / NOT TESTED / INTEGRATION-READY

PII Protection:
PASS / FAIL / NOT TESTED

Không ghi PASS chỉ vì source có annotation hoặc controller tồn tại.

65. INTEGRATION REPORT
    Complaint
    Existing Contract:
    <...>

Persistence:
<...>

API:
<...>

Security:
<...>

Status:
IMPLEMENTED / PARTIAL / MISSING / WRONG
Return
Existing Contract:
<...>

Phase 3A Seam:
<...>

External Capability:
AVAILABLE / NOT AVAILABLE

Status:
INTEGRATED / INTEGRATION-READY / MISSING
Refund
Existing Financial Contract:
<...>

Phase 3A Seam:
<...>

Financial Backend:
AVAILABLE / NOT AVAILABLE

Status:
INTEGRATED / INTEGRATION-READY / MISSING
66. KNOWN ISSUES

Mỗi issue:

Issue:
Current Behavior:
Expected Behavior:
Root Cause:
Affected File/Module:
Severity:
Phase 3A Impact:
Dependency Impact:
Fix:
Retest:
Status:

Ví dụ:

Issue:
No production Refund backend

Current Behavior:
No financial refund capability found in current source

Expected Behavior:
Refund integration contract

Phase 3A Impact:
Cannot execute real financial refund

Status:
INTEGRATION-READY

Không ghi:

BLOCKED – teammate chưa code
67. FINAL REPORT TEMPLATE
Phase Status
PHASE 3A
COMPLAINT + RETURN/REFUND CONTRACT

Implementation:
IMPLEMENTED / PARTIAL / MISSING / WRONG

Return Integration:
INTEGRATED / INTEGRATION-READY / MISSING

Refund Integration:
INTEGRATED / INTEGRATION-READY / MISSING
Completed
Complaint domain
Complaint persistence
Order relationship
OrderItem relationship
Customer ownership
Vendor ownership
Evidence contract
Return contract
Refund contract
Security
Not Completed

Ví dụ:

Moderator escalation:
OUT OF SCOPE – Phase 3B

Scheduler:
OUT OF SCOPE – Phase 3C

Refund provider:
INTEGRATION-READY

Finance Dashboard:
OUT OF SCOPE – Phase 4
Dependency Fixes
Dependency:
<name></name>

Issue:
<actual issue></actual>

Minimal Fix:
<fix></fix>

Phase 3A Impact:
<impact></impact>

Regression:
PASS / FAIL / NOT TESTED
68. GIT SAFETY REPORT

Bắt buộc:

Commit:
NOT DONE

Push:
NOT DONE

Merge:
NOT DONE

Rebase:
NOT DONE

Checkout:
NOT DONE

Switch:
NOT DONE

Reset:
NOT DONE

Stash:
NOT DONE

Nếu thực tế khác, báo đúng thực tế.

69. BUILD REPORT
    Command:
    mvn clean package -DskipTests

BUILD:
PASS / FAIL

Nếu fail:

Root Cause:
...

Affected Module:
...

Phase 3A Caused:
YES / NO / UNKNOWN

Dependency Caused:
YES / NO / UNKNOWN

Fix:
...

Retest:
PASS / FAIL / NOT RUN
70. MANUAL TEST REPORT
Customer Complaint Creation:
PASS / FAIL / NOT TESTED

Customer Ownership:
PASS / FAIL / NOT TESTED

Vendor Ownership:
PASS / FAIL / NOT TESTED

Complaint Persistence:
PASS / FAIL / NOT TESTED

Evidence Access:
PASS / FAIL / NOT TESTED

Complaint IDOR:
PASS / FAIL / NOT TESTED

Return Contract:
INTEGRATED / INTEGRATION-READY / NOT TESTED

Refund Contract:
INTEGRATED / INTEGRATION-READY / NOT TESTED

Security:
PASS / FAIL / NOT TESTED

MongoDB Safety:
PASS

Không dùng source inspection để giả lập manual PASS. Nguyên tắc này cũng được giữ từ phase cũ.

71. ĐIỀU KIỆN KẾT THÚC PHASE 3A

Phase 3A chỉ được gọi là IMPLEMENTED khi phần thuộc chính Phase 3A đã:

Requirement
   ↓
Current Source inspected
   ↓
Domain boundary confirmed
   ↓
Contract defined
   ↓
Persistence verified
   ↓
Security verified
   ↓
Build
   ↓
Relevant verification

Core foundation:

Customer
   ↓
Order
   ↓
OrderItem
   ↓
Complaint
   ↓
Evidence
   ↓
Return Contract
   ↓
Refund Contract
72. RANH GIỚI BÀN GIAO CHO PHASE 3B

Phase 3A bàn giao:

Complaint
├── Domain
├── Persistence
├── Ownership
├── Evidence
├── Status contract
└── Service boundary

cho Phase 3B triển khai:

Customer ↔ Vendor
        ↓
Moderator
        ↓
Admin
        ↓
Escalation
        ↓
Enforcement

Không để Phase 3B phải tạo lại Complaint model.

73. RANH GIỚI BÀN GIAO CHO PHASE 3C

Phase 3A/3B sẽ cung cấp các contract cần thiết để Phase 3C triển khai:

Deadline
   ↓
Overdue detection
   ↓
Eligibility
   ↓
Escalation
   ↓
Idempotency

Phase 3A không tự thêm scheduler chỉ để “chuẩn bị trước”.

74. RANH GIỚI BÀN GIAO CHO PHASE 4

Phase 3A chỉ bàn giao:

Complaint
Return contract
Refund contract
Financial integration seam

Phase 4 mới xử lý các phần như:

Finance Dashboard
Refund Dashboard
Vendor Payable
Platform Revenue
GMV
Full-system integration
Final regression
Performance hardening
75. NGUYÊN TẮC QUAN TRỌNG NHẤT CỦA PHASE 3A

Tóm lại, khi thực hiện phase này phải giữ đúng 10 nguyên tắc:

1. Bám source hiện tại.
2. Không coi report cũ là bằng chứng implementation.
3. Complaint phải được phân biệt rõ với ReportCase.
4. Return/Refund phải xác định contract trước khi khẳng định workflow.
5. Không tự phát minh business rule.
6. Không fake financial behavior.
7. Không làm Escalation/Scheduler của Phase 3B/3C.
8. Backend phải enforce ownership/IDOR.
9. Tuyệt đối không xóa/reset dữ liệu MongoDB.
10. Không commit/push/merge/reset/stash/switch branch.
