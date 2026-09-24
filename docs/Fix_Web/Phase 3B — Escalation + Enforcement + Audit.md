
Phase 3B — Escalation + Enforcement + Audit

Mục tiêu: hoàn thiện phần Escalation persistence → Admin/Moderator enforcement boundary → AuditLog write path, bám sát audit đối chiếu Phase với source hiện tại, kế thừa Phase 1–2, không xây lại subsystem đã có, và tuyệt đối không xóa/reset dữ liệu MongoDB.

Phase 3 cũ xác định rõ flow Moderator → Escalate → Admin Escalation Queue → Admin Decision, yêu cầu kiểm chứng persistence thực tế và không tạo EscalationV2/AdminEscalationV2.

Đồng thời, Phase 3 cũ yêu cầu Audit phải dùng lại Audit Source of Truth, không âm thầm tạo hệ thống audit thứ ba.

0. ĐỊNH VỊ PHASE 3B TRONG TOÀN BỘ ROADMAP

Phase 3B không phải toàn bộ Phase 3 Complaint/Return/Refund/Scheduler.

Phase này tập trung riêng vào:

Escalation Persistence
        ↓
Escalation Workflow
        ↓
Moderator → Admin boundary
        ↓
Admin Enforcement
        ↓
AuditLog Write Path
        ↓
Security / IDOR
        ↓
Persistence Verification

Kiến trúc mục tiêu:

                 MODERATOR
                     │
                     │ ESCALATE
                     ▼
             ┌─────────────────┐
             │   ESCALATION    │
             │   PERSISTENCE   │
             └────────┬────────┘
                      │
                      ▼
             ADMIN ESCALATION QUEUE
                      │
                      ▼
                   ADMIN
                      │
          ┌───────────┴───────────┐
          ▼                       ▼
      RESOLVE                 ENFORCEMENT
                                  │
                     ┌────────────┼────────────┐
                     ▼            ▼            ▼
                 Product        Shop        Vendor
                 action         action       action
                     │
                     ▼
                  AUDITLOG

Điểm quan trọng: Escalation và AuditLog ở Phase 3B phải là persistence thật, không chỉ có class/service/interface.

1. TÀI LIỆU BẮT BUỘC

Đọc theo thứ tự:

1. Phase 3B specification
2. Phase 3 cũ
3. Phase 1 Final Report
4. Phase 2 Final Report
5. BỘ LUẬT & CHÍNH SÁCH CỦA SÀN MARKETPLACE – Version 1.0
6. SHOPEE_MARKETPLACE_MODEL.md
7. Current Source Code

Phase 3 cũ quy định rõ:

Current Source
+
Phase 1
+
Phase 2
+
Phase 3 Specification
+
Marketplace Policy
+
Marketplace Model

là nguồn để đối chiếu; report cũ không được coi là bằng chứng implementation hiện tại.

2. SOURCE OF TRUTH

Thứ tự ưu tiên:

CURRENT SOURCE CODE
        ↓
CURRENT DATABASE/PERSISTENCE
        ↓
PHASE 1 FOUNDATION
        ↓
PHASE 2 IMPLEMENTATION
        ↓
PHASE 3 SPECIFICATION
        ↓
MARKETPLACE POLICY
        ↓
SHOPEE_MARKETPLACE_MODEL

Không được kết luận:

Phase 2 report nói Escalation DONE
        ↓
Escalation = IMPLEMENTED

Phải kiểm tra:

Escalation requirement
        ↓
Entity
        ↓
Repository
        ↓
Service
        ↓
Controller/API
        ↓
Security
        ↓
MongoDB persistence
        ↓
Admin queue
        ↓
Enforcement
        ↓
AuditLog persistence
        ↓
Build / manual evidence
3. ĐỐI CHIẾU VỚI CODE HIỆN TẠI

Theo phần audit source hiện tại đã đối chiếu trước đó, phải đặc biệt coi các điểm sau là gap cần xác minh, không được coi là đã hoàn thành chỉ vì tên capability xuất hiện:

Hiện trạng cần đặc biệt kiểm tra
Escalation domain
    ?
EscalationRepository
    ?
EscalationService
    ?
Escalation persistence
    ?
Admin escalation queue
    ?
Admin enforcement
    ?
AuditLog entity
    ?
AuditLog repository
    ?
AuditLogService / writer
    ?
Actual MongoDB writes
    ?

Trong khi audit trước đó cho thấy Phase 1–2 có các khái niệm liên quan đến:

ReportCase
Violation
Admin Escalation
Moderation History
Audit

nhưng nguyên tắc của phase là:

EXISTS ≠ IMPLEMENTED

Phải trace đến persistence/runtime.

4. MỤC TIÊU CHÍNH

Phase 3B phải đạt được:

Moderator
   ↓
Escalate
   ↓
Escalation record được persist
   ↓
Admin có thể đọc escalation
   ↓
Admin xử lý
   ↓
Nếu cần enforcement
   ↓
Admin Enforcement
   ↓
AuditLog được persist

Không được có flow giả:

Moderator
   ↓
UI báo "Escalated"
   ↓
redirect

nhưng MongoDB không có escalation.

5. FEATURE SCOPE

Phase 3B được phép triển khai:

Escalation
Escalation persistence
Escalation service
Moderator escalation action
Admin escalation queue
Admin escalation detail
Admin resolution
Admin enforcement integration
Audit event
AuditLog write path
AuditLog persistence
Security
IDOR protection
Persistence consistency
Regression Phase 1–2
6. KHÔNG THUỘC PHASE 3B

Không mở rộng Phase 3B thành:

Complaint creation
Return engine
Refund engine
Payment engine
Finance
Settlement
Escrow calculation
Dashboard
GMV
Platform revenue
KYC
Product moderation rewrite
ReportCase rewrite
Violation rewrite
Moderator Foundation rewrite

Nếu một capability bên ngoài trực tiếp làm Escalation/Audit/Enforcement không hoạt động:

Inspect
→ Root Cause
→ Minimal Fix
→ Regression

Không rewrite toàn subsystem.

7. INDEPENDENT IMPLEMENTATION

Không chờ:

teammate
branch khác
commit khác
PR khác
merge khác

Nếu dependency chưa hoàn chỉnh:

Existing contract?
       ↓
YES
       ↓
Implement against contract
       ↓
INTEGRATION-READY

Nếu contract cũng chưa tồn tại nhưng Phase 3B cần:

Define minimal internal contract
       ↓
Persist own domain if required
       ↓
Expose integration seam
       ↓
INTEGRATION-READY

Chỉ BLOCKED khi có technical blocker thực sự.

Không dùng:

BLOCKED – teammate chưa code
8. NGUYÊN TẮC REUSE

Trước khi tạo bất kỳ file nào:

Search
  ↓
Existing?
 ├── YES → REUSE / MODIFY
 └── NO  → CREATE

Không tạo:

EscalationV2
AdminEscalationV2
AuditLogV2
AuditLogEntryV2
ModeratorEscalationV2
EnforcementV2

nếu architecture hiện tại đã có domain tương ứng.

Phase 3 cũ cũng quy định rõ phải reuse Escalation, EscalationRepository, EscalationService, Admin Escalation thay vì tạo domain thứ hai.

9. GATE 3B-1 — ESCALATION DOMAIN/PERSISTENCE

Không triển khai workflow trước khi Gate này đạt.

Phải xác định:

Escalation
EscalationRepository
EscalationService
Escalation status/state
Escalation source
Escalation target
EscalatedBy
CreatedAt
UpdatedAt
Resolution

Chỉ thêm field nếu Phase 3B thực sự cần.

Không tạo schema chỉ để đẹp.

10. ESCALATION ENTITY

Escalation phải biểu diễn tối thiểu được:

id
sourceType
sourceId
fromRole / actor
targetRole
status
reason
createdAt
updatedAt
resolvedAt

Nhưng đây là contract kiểm tra, không phải danh sách field bắt buộc phải copy nguyên xi.

Nếu source hiện tại đã có field tương đương:

REUSE CURRENT FIELD

Không rename chỉ để khớp specification.

11. ESCALATION SOURCE

Escalation có thể bắt nguồn từ:

ReportCase
Violation
Complaint
Moderation case

Phase 3B phải sử dụng domain hiện tại.

Ví dụ:

ReportCase
   ↓
Moderator Decision
   ↓
Escalation

hoặc:

Complaint
   ↓
Moderator
   ↓
Escalation

Không tạo:

ComplaintEscalation
ReportCaseEscalation
ViolationEscalation

riêng biệt nếu Escalation chung đã đáp ứng domain boundary.

12. ESCALATION PERSISTENCE — BẮT BUỘC

Đây là điểm mới cần siết chặt hơn Phase 3 cũ.

Không được coi flow là hoàn thành nếu chỉ:

service method chạy

Phải chứng minh:

Controller
   ↓
Service
   ↓
Repository.save(...)
   ↓
MongoDB document

Ví dụ evidence cần có:

Before:
Escalation count = N

Moderator escalates

After:
Escalation count = N + 1

hoặc kiểm tra trực tiếp document được tạo/cập nhật.

Không cần xóa document sau test.

13. ESCALATION WRITE PATH

Write path chuẩn:

Moderator Controller
        ↓
Escalation Service
        ↓
Validate source case
        ↓
Validate permission
        ↓
Validate current state
        ↓
Create/Update Escalation
        ↓
EscalationRepository
        ↓
MongoDB
        ↓
AuditLog writer
        ↓
MongoDB AuditLog

Không cho controller tự:

repository.save()

nếu architecture hiện tại quy định business logic nằm ở service.

14. DUPLICATE ESCALATION

Một case không được tạo escalation vô hạn.

Trước khi tạo:

Find existing escalation
        ↓
Check status
        ↓
Check source
        ↓
Check target
        ↓
Determine whether new escalation is allowed

Không:

POST /escalate
POST /escalate
POST /escalate

→ ba escalation giống nhau.

Nếu Policy yêu cầu multiple escalation hợp lệ, phải bám Policy; không tự áp dụng uniqueness tuyệt đối.

15. ESCALATION STATE

Không tự phát minh enum nếu source đã có.

Ví dụ chỉ dùng nếu source/spec xác nhận:

OPEN
IN_PROGRESS
RESOLVED
REJECTED
CLOSED

Không tự thêm:

ESCALATED_AGAIN
SUPER_ESCALATED
URGENT_ESCALATED

chỉ để phục vụ UI.

16. MODERATOR RESPONSIBILITY
    Moderator được:
    Review case
    ↓
    Make moderator-level decision
    ↓
    Escalate when permitted

Moderator không phải Admin Enforcement actor.

Moderator không được trực tiếp thực hiện:

BAN_VENDOR
SUSPEND_SHOP
PLATFORM_ENFORCEMENT

trừ khi current policy/source explicitly giao quyền đó cho Moderator.

17. ADMIN RESPONSIBILITY

Admin là owner của platform-level enforcement.

Boundary:

MODERATOR
    │
    ├── Review
    ├── Resolve within moderator scope
    └── Escalate
             ↓
          ADMIN
             │
             ├── Final platform decision
             └── Enforcement

Đây là điểm phải giữ rất rõ để tránh việc Phase 3B biến Moderator thành Admin.

Phase 3 cũ cũng mô tả Level 2 là Admin và gắn FINAL PLATFORM DECISION / ENFORCEMENT với Admin.

18. ENFORCEMENT THUỘC AI?
    Kết luận kiến trúc của Phase 3B
    Moderator
    MODERATOR
    → moderation decision
    → request evidence
    → resolve moderator-scope case
    → escalate
    Admin
    ADMIN
    → final platform decision
    → enforcement

Admin enforcement có thể liên quan:

Product
Shop
Vendor
Account

nhưng chỉ action nào được Policy/current source hỗ trợ mới được triển khai.

19. ADMIN ENFORCEMENT KHÔNG ĐƯỢC PHÁT MINH

Không tự tạo rule:

3 violation → suspend
5 violation → ban

hoặc:

1 escalation → ban vendor

nếu Policy chưa quy định.

Phase 2 cũ cũng yêu cầu severity/action phải bám Policy và không tự thêm penalty.

20. ENFORCEMENT MATRIX
    Action	Moderator	Admin
    Review case	ALLOW	ALLOW theo scope
    Escalate	ALLOW	Có thể xử lý escalation
    Resolve moderator case	ALLOW	ALLOW
    Product moderation	Theo Phase 2 permission	Theo Admin scope
    Hide product	Chỉ nếu permission hiện tại cho phép	ALLOW nếu Admin Enforcement hỗ trợ
    Suspend Shop	Không mặc định	ALLOW nếu Policy/source hỗ trợ
    Ban Vendor	Không	ALLOW nếu Policy/source hỗ trợ
    Platform enforcement	Không	ALLOW

Đây là boundary kiến trúc, không phải tự thêm business rule.

21. ADMIN ESCALATION QUEUE

Admin phải đọc được:

Escalation ID
Source
Source ID
Reason
Created time
Current status
Escalated by
Priority nếu existing architecture có
History

Không expose dữ liệu PII không cần thiết.

22. ADMIN ESCALATION DETAIL

Flow:

Admin
 ↓
Escalation Queue
 ↓
Escalation Detail
 ↓
Source Case
 ↓
History
 ↓
Evidence
 ↓
Moderator Decision
 ↓
Admin Decision
 ↓
Optional Enforcement

Không tạo dashboard mới.

Nếu Admin đã có escalation page:

REUSE
23. ADMIN DECISION

Admin decision phải đi qua service:

Admin Controller
      ↓
Admin Escalation Service
      ↓
Validate escalation
      ↓
Validate permission
      ↓
Validate state
      ↓
Apply decision
      ↓
Persist
      ↓
Audit

Không để frontend quyết định:

role
actorId
enforcementType
targetVendorId
24. IDOR — ESCALATION

Bắt buộc kiểm tra:

Moderator A
 → escalation B

nếu A không có permission.

Phải:

DENY

Tương tự:

Customer → Admin escalation
Vendor   → Admin escalation
Moderator → Admin-only enforcement

đều phải bị backend chặn.

25. ACTOR ID KHÔNG ĐƯỢC TRUST FRONTEND

Không dùng:

request.moderatorId
request.adminId
request.actorId

làm nguồn xác định actor.

Phải lấy actor từ:

authenticated security context

Sau đó kiểm tra role/permission.

26. AUDIT — MỤC TIÊU CỐT LÕI

Phase 3B phải hoàn thiện:

AUDIT EVENT
     ↓
AUDIT WRITE PATH
     ↓
AUDIT REPOSITORY
     ↓
MONGODB

Không chỉ:

logger.info(...)

Không chỉ:

AuditEvent object created
27. AUDIT SOURCE OF TRUTH

Phải inspect Phase 1/current source:

AuditLog
AuditLogEntry
AuditEventWriter
AuditService
AuditRepository

Nếu đã tồn tại:

REUSE

Nếu có conflict:

TRACE
→ ROOT CAUSE
→ MINIMAL FIX

Không tạo hệ thống audit thứ ba.

Phase 3 cũ quy định rõ Audit phải reuse Audit Source of Truth và không âm thầm tạo hệ thống mới.

28. AUDITLOG WRITE PATH — BẮT BUỘC

Flow phải là:

Business Action
      ↓
Audit Event
      ↓
AuditLogWriter / AuditService
      ↓
AuditLogRepository
      ↓
MongoDB

Ví dụ:

Moderator Escalate
      ↓
Escalation persisted
      ↓
AuditLog persisted

và:

Admin Enforcement
      ↓
Enforcement persisted
      ↓
AuditLog persisted
29. AUDIT KHÔNG ĐƯỢC LÀ “LOG GIẢ”

Không coi:

log.info("Escalated case");

là AuditLog.

Cũng không coi:

auditEventPublisher.publish(...)

là persistence hoàn chỉnh nếu chưa có consumer/write path thật.

Nếu event-driven architecture đã có:

Event
 ↓
Listener
 ↓
Audit writer
 ↓
MongoDB

thì phải verify đến MongoDB.

30. ACTION PHẢI AUDIT

Ít nhất phải kiểm tra các action Phase 3B thực sự implement:

ESCALATION_CREATED
ESCALATION_UPDATED
ESCALATION_RESOLVED
ADMIN_DECISION
ENFORCEMENT_APPLIED

Tên event không bắt buộc phải đúng các chuỗi trên nếu source đã có convention khác.

Phải reuse convention hiện tại.

31. AUDIT DATA

Audit nên đủ để trả lời:

WHO?
WHAT?
WHEN?
ON WHICH RESOURCE?
FROM WHAT STATE?
TO WHAT STATE?
WHY?

Nhưng:

PII tối thiểu

Không dump toàn bộ:

Customer object
Vendor object
Address
Phone
Evidence binary

vào audit nếu không cần.

32. AUDIT IMMUTABILITY

AuditLog không được có flow:

delete
update historical event

trong Phase 3B.

Audit là historical record.

Cho phép:

INSERT

và nếu architecture cần:

append-only

Không cleanup audit sau test.

33. ESCALATION + AUDIT CONSISTENCY

Một action:

Moderator Escalate

phải tạo được:

Escalation document
+
AuditLog document

Nếu Escalation đã persist nhưng Audit fail:

KHÔNG im lặng bỏ qua

Phải xác định consistency strategy của project:

transaction
outbox/event
best-effort
retry

Không tự áp dụng distributed transaction nếu project không có architecture tương ứng.

34. KHÔNG @Transactional MÙ QUÁNG

Không thêm:

@Transactional

vào mọi method.

Phải inspect MongoDB transaction support và architecture hiện tại.

Nếu persistence strategy hiện tại không dùng transaction:

reuse current architecture

và xử lý consistency theo pattern đang có.

35. ENFORCEMENT PERSISTENCE

Không được coi:

Admin clicked Suspend

là enforcement hoàn thành.

Phải trace:

Admin Action
 ↓
Enforcement Service
 ↓
Target entity
 ↓
Persistence
 ↓
Audit

Ví dụ:

Suspend Shop
 ↓
Shop.status updated
 ↓
MongoDB
 ↓
AuditLog

Nếu current source chưa có Admin Enforcement thực:

MISSING

hoặc:

INTEGRATION-READY

nếu đã có contract nhưng external capability chưa nối.

36. ENFORCEMENT KHÔNG FAKE

Cấm:

return "Suspended successfully";

mà không cập nhật persistence.

Cấm:

audit("vendor banned");

mà vendor vẫn active.

Cấm tạo:

FakeEnforcementService

chỉ để UI chạy.

37. VIOLATION → ENFORCEMENT

Nếu Phase 2 có:

Violation
Severity
Action

Phase 3B chỉ consume:

Violation
   ↓
Existing decision
   ↓
Admin enforcement

Không xây lại Violation engine.

Nếu policy yêu cầu:

CRITICAL
→ Admin escalation

thì Phase 3B có thể nối:

Violation
 ↓
Escalation
 ↓
Admin

nhưng không tự tạo threshold.

38. MODERATOR → ADMIN ESCALATION FLOW

Flow chuẩn:

Moderator
   ↓
POST escalation action
   ↓
Authorization
   ↓
Validate source case
   ↓
Validate current state
   ↓
Persist Escalation
   ↓
Write AuditLog
   ↓
Admin Queue

Không:

Moderator
 ↓
Direct Admin Enforcement
39. ADMIN → ENFORCEMENT FLOW
Admin
 ↓
Open Escalation
 ↓
Review source case
 ↓
Review history
 ↓
Review evidence
 ↓
Decision
 ↓
Optional Enforcement
 ↓
Persist
 ↓
AuditLog
40. SECURITY MATRIX
Actor	Read escalation	Create escalation	Resolve escalation	Admin enforcement
Anonymous	DENY	DENY	DENY	DENY
Customer	DENY	DENY*	DENY	DENY
Vendor	DENY	DENY*	DENY	DENY
Moderator	ALLOW theo permission	ALLOW theo scope	ALLOW theo scope	DENY
Admin	ALLOW	ALLOW theo admin scope	ALLOW	ALLOW

* Nếu Complaint/ReportCase workflow có action escalation dành cho Customer/Vendor thì phải dùng contract hiện tại; không tự thay đổi permission model của Phase 2.

41. ADMIN ENFORCEMENT SECURITY

Mọi enforcement endpoint phải kiểm tra:

authenticated
+
ROLE_ADMIN
+
resource exists
+
current state valid
+
action permitted

Không đủ:

URL /admin/enforcement

mà phải enforce backend.

42. IDOR TEST

Bắt buộc kiểm tra:

Moderator A → Escalation B
Moderator → arbitrary escalation ID
Customer → escalation API
Vendor → escalation API
Moderator → admin enforcement endpoint
Admin → nonexistent resource
Admin → unauthorized enforcement target

Expected:

DENY / NOT FOUND

theo security architecture hiện tại.

43. AUDIT IDOR

Không cho actor đọc audit tùy ý bằng:

/audit/{id}

nếu endpoint đó không được phép.

Audit có thể chứa sensitive operational information.

Nếu không có Audit UI:

Không cần tạo Audit UI chỉ cho Phase 3B.

Nhưng write path phải hoạt động.

44. PERSISTENCE DISCOVERY CHECKLIST

Trước khi code:

Escalation
[ ] Entity
[ ] Repository
[ ] Service
[ ] Controller
[ ] State
[ ] Mongo collection
[ ] save/update path
[ ] read path
Audit
[ ] Entity
[ ] Repository
[ ] Service/writer
[ ] Event
[ ] Consumer nếu có
[ ] Mongo write
[ ] read/inspection evidence
Enforcement
[ ] Existing service
[ ] Target entity
[ ] State mutation
[ ] Persistence
[ ] Permission
[ ] Audit integration
45. SOURCE DISCOVERY — ESCALATION

Inspect sâu:

Escalation
EscalationRepository
EscalationService
EscalationServiceImpl
AdminEscalation
Moderator controller/service
ReportCase
Violation
Complaint nếu đã có
SecurityConfig

Không đọc toàn project.

46. SOURCE DISCOVERY — AUDIT

Inspect:

AuditLog
AuditLogEntry
AuditRepository
AuditLogRepository
AuditService
AuditLogService
AuditEventWriter
AuditEvent

và toàn bộ usages:

search "audit"
search "AuditLog"
search "AuditEvent"
search "saveAudit"

Mục tiêu:

tìm source of truth thật
47. SOURCE DISCOVERY — ENFORCEMENT

Inspect:

AdminController
AdminService
ShopService
ProductService
UserService
ViolationService

và tìm các operation:

suspend
ban
lock
hide
reject
restrict
enforce

Không giả định method tồn tại chỉ vì UI có button.

48. CURRENT CODE FINDING → ACTION

Áp dụng:

EXISTS + WORKS
→ REUSE

EXISTS + WRONG
→ FIX MINIMALLY

EXISTS + NO PERSISTENCE
→ COMPLETE WRITE PATH

EXISTS + BROKEN DEPENDENCY
→ TRACE + MINIMAL FIX

MISSING + REQUIRED
→ IMPLEMENT

MISSING + FUTURE
→ OUT OF SCOPE

DUPLICATE
→ REPORT CONFLICT

UNKNOWN
→ INSPECT
49. KHÔNG TẠO DUPLICATE DOMAIN

Nếu phát hiện:

Escalation
+
ComplaintEscalation

hoặc:

AuditLog
+
AuditTrail

không được tự xóa một cái.

Báo:

DUPLICATED / CONFLICT

sau đó xác định source of truth.

50. MONGODB SAFETY — TUYỆT ĐỐI

Phase 3B kế thừa nguyên tắc Phase 3:

NO deleteAll()
NO deleteMany({})
NO drop()
NO dropDatabase()
NO truncate
NO reset collection
NO destructive migration

Phase 3 specification cũ xác nhận rõ không có ngoại lệ cho các thao tác destructive này.

51. KHÔNG XÓA DATA ĐỂ TEST ESCALATION

Không được:

Tạo escalation test
↓
test fail
↓
delete escalation

Không được cleanup:

AuditLog
Escalation
Violation
Complaint
ReportCase

sau test.

52. TEST DATA

Nếu bắt buộc cần data:

INSERT ONLY

Marker:

[TEST-PHASE-3B]

Ví dụ:

[TEST-PHASE-3B] Escalation
[TEST-PHASE-3B] Audit

Số lượng:

1–2 records/scenario

Không tạo fake production metrics.

53. EXISTING DATA

Ưu tiên:

Existing ReportCase
Existing Violation
Existing Complaint
Existing Product
Existing Shop
Existing User

Nếu có record phù hợp thì dùng record thật.

Không mutate dữ liệu thật chỉ để “test đẹp”.

Nếu cần mutate một record existing:

ghi rõ:

- collection
- record
- field
- lý do
- expected effect

54. MONGODB PERSISTENCE VERIFICATION

Phải chứng minh:

Escalation
Before
 ↓
Action
 ↓
After
 ↓
MongoDB contains escalation
Audit
Before
 ↓
Action
 ↓
After
 ↓
MongoDB contains AuditLog
Enforcement
Before target state
 ↓
Admin action
 ↓
After target state
 ↓
Audit exists
55. KHÔNG FAKE AUDIT

Không coi:

console log
application log
debug log

là:

AuditLog persistence
56. AUDIT FAILURE

Nếu:

Escalation saved
Audit failed

không được nuốt exception:

try {
   audit();
} catch (...) {
   // ignore
}

nếu làm mất tính audit mà architecture yêu cầu.

Phải xác định strategy hiện tại:

transaction
retry
event
outbox
explicit failure

và báo rõ.

57. ENFORCEMENT FAILURE

Nếu:

Audit save fail

hoặc:

target update fail

phải xác định behavior nhất quán.

Không trả:

SUCCESS

nếu enforcement chưa thật sự persist.

58. AUDIT EVENT CONTRACT

Nếu project đã có event:

AuditEvent

reuse.

Nếu chưa có nhưng AuditLog persistence đã tồn tại:

AuditLogWriter

có thể là seam phù hợp.

Không tạo:

Phase3BAuditService

nếu AuditLogService đã là source of truth.

59. AUDIT ACTOR

Audit phải lấy:

authenticated user

Không lấy:

request.actorId
request.adminId
request.moderatorId

làm nguồn tin cậy.

60. AUDIT RESOURCE

Mỗi event cần identify resource đủ để trace:

resourceType
resourceId

Ví dụ:

ESCALATION / <id></id>
REPORT_CASE / <id></id>
VIOLATION / <id></id>
SHOP / <id></id>
PRODUCT / <id></id>
USER / <id></id>

chỉ dùng loại resource đã có trong architecture.

61. AUDIT REASON

Nếu action yêu cầu reason:

Moderator Escalation Reason
Admin Decision Reason
Enforcement Reason

phải lấy từ request hoặc business rule phù hợp.

Không tạo reason giả:

"Admin decision"

chỉ để database có dữ liệu.

62. UI SCOPE

Phase 3B chỉ cần:

Moderator:
Escalate action

Admin:
Escalation Queue
Escalation Detail
Decision
Enforcement action nếu đã có

Không cần:

Audit dashboard
Analytics dashboard
Finance dashboard
63. BACKEND-FIRST

UI không được là nơi enforce:

role
permission
state transition
enforcement

Frontend chỉ:

display
submit
handle response

Business rule:

Controller
 ↓
Service
 ↓
Repository
64. STATE TRANSITION

Mỗi action phải validate:

current state
+
allowed transition
+
actor permission

Không cho:

RESOLVED
 ↓
ESCALATED

nếu state machine hiện tại không cho phép.

Không tự tạo state machine mới.

65. ADMIN ENFORCEMENT BOUNDARY — QUY TẮC CHỐT

Đây là boundary chính thức của Phase 3B:

MODERATOR
────────────────────────────
Case review
Decision
Evidence request
Escalate
Moderator-scope resolution

ADMIN
────────────────────────────
Escalation resolution
Final platform decision
Platform enforcement

Nếu current source có permission khác:

Source mismatch
+
Policy check
+
Report

Không âm thầm đổi permission.

66. PHASE 3B VỚI VIOLATION

Không xây lại:

Violation
ViolationRepository
ViolationService
ViolationSeverity
ViolationAction

Nếu existing Violation phát sinh escalation:

Violation
 ↓
Escalation
 ↓
Admin

Nếu Policy không yêu cầu escalation:

Không tự tạo.
67. PHASE 3B VỚI REPORTCASE

ReportCase là một nguồn case có thể dẫn tới:

Moderator
 ↓
Decision
 ↓
Escalation

Không tạo:

ReportCaseEscalationV2
68. PHASE 3B VỚI COMPLAINT

Nếu Complaint đã tồn tại trong Phase 3A:

Complaint
 ↓
Moderator
 ↓
Escalation
 ↓
Admin

Phase 3B chỉ xử lý escalation boundary.

Không xây lại Complaint lifecycle.

69. ENFORCEMENT TARGET VALIDATION

Admin không được gửi:

targetType
targetId

rồi service tin tuyệt đối.

Phải:

target exists
+
target type valid
+
source case permits action
+
Admin permission
+
current state valid
70. NO PRIVILEGE ESCALATION

Không cho Moderator truyền:

role=ADMIN
action=BAN

để biến request thành Admin action.

Actor role phải đến từ:

authenticated security context
71. CONCURRENCY

Nếu hai Moderator cùng escalate:

Moderator A → escalate
Moderator B → escalate

phải tránh tạo duplicate nếu policy/domain không cho phép.

Không cần phát minh distributed lock.

Ưu tiên:

existing repository query
+
state check
+
atomic/update strategy hiện tại

Nếu architecture yêu cầu locking mới:

minimal implementation
72. AUDIT DUPLICATION

Một action retry không được vô tình tạo audit vô hạn nếu architecture yêu cầu idempotency.

Ví dụ:

Request retry
 ↓
same escalation
 ↓
duplicate audit?

Phải kiểm tra behavior.

Không tự tạo idempotency layer toàn hệ thống.

73. MANUAL TEST — ESCALATION
    Test 1
    Moderator
    ↓
    Open case
    ↓
    Escalate

Expected:

Escalation created
Test 2

Kiểm tra MongoDB:

Escalation document EXISTS
Test 3

Admin:

Open Admin Escalation Queue

Expected:

Escalation visible
74. MANUAL TEST — AUDIT

Sau Moderator escalation:

AuditLog document EXISTS

Kiểm tra:

actor
action
resource
timestamp

không chứa PII không cần thiết.

75. MANUAL TEST — ADMIN ENFORCEMENT

Nếu existing enforcement support:

Admin
 ↓
Escalation
 ↓
Enforcement

Kiểm tra:

Target state changed
+
AuditLog exists

Không chỉ kiểm tra toast/message.

76. SECURITY TEST
    Customer
    GET escalation
    → DENY
    Vendor
    GET admin escalation
    → DENY
    Moderator
    Admin enforcement endpoint
    → DENY
    Admin
    Authorized escalation
    → ALLOW
77. IDOR TEST
    Moderator A → Escalation B

Expected:

DENY

nếu B không thuộc permission scope của A.

78. AUDIT SECURITY TEST

Actor không được:

forge actorId
forge adminId
forge moderatorId

Expected:

Audit actor = authenticated principal
79. BUILD RULE

Giữ nguyên workflow của phase cũ:

mvn clean package -DskipTests

Không tự động:

mvn test
mvn verify
mvn install

Không tự động start Spring Boot.

Phase 3 cũ quy định rõ build command này và không tự động seed/reset MongoDB.

80. GIT SAFETY

Trước khi code:

git status
git branch
git log --oneline --decorate -10

Không:

git checkout
git switch
git reset
git stash
git merge
git rebase
git commit
git push

Không overwrite uncommitted code.

81. FILE CHANGE SCOPE
    Có thể tạo/sửa
    Escalation entity
    Escalation repository
    Escalation service
    Escalation controller
    Admin escalation controller/service
    AuditLog writer/service
    AuditLog repository
    Security configuration
    Relevant templates
    Relevant DTO

Chỉ khi source hiện tại thiếu hoặc sai.

Không được tự xóa
ANY EXISTING FILE

Nếu duplicate:

DUPLICATED / CONFLICT
82. ACCEPTANCE CRITERIA — ESCALATION

Phase 3B phải có:

Moderator
 ↓
Escalate
 ↓
Escalation persisted
 ↓
Admin queue
 ↓
Admin detail

và:

Escalation action
 ↓
AuditLog persisted
83. ACCEPTANCE CRITERIA — ENFORCEMENT

Nếu Admin Enforcement hiện có:

Admin
 ↓
Decision
 ↓
Enforcement
 ↓
Target persistence
 ↓
AuditLog

Nếu capability enforcement chưa có nhưng Phase 3B có thể tạo contract:

INTEGRATION-READY

Không fake enforcement.

84. ACCEPTANCE CRITERIA — SECURITY

Phải enforce:

Moderator authorization
Admin authorization
IDOR protection
Actor integrity
Resource authorization
State transition validation
85. ACCEPTANCE CRITERIA — AUDIT

Audit phải:

Have source of truth
+
Have write path
+
Persist to MongoDB
+
Record actor
+
Record action
+
Record resource
+
Be queryable/inspectable

Không chỉ có Java object.

86. STATUS CLASSIFICATION
    IMPLEMENTED
    Code exists

Persistence exists
+
Security exists
+
Behavior verified
INTEGRATED
Real dependency connected
+
End-to-end verified
INTEGRATION-READY
Contract/write seam ready
+
External capability genuinely unavailable
PARTIAL
Một phần flow đã có
MISSING
Chưa có implementation
WRONG
Có implementation nhưng behavior/architecture sai
BLOCKED

Chỉ khi:

Technical blocker thực sự
87. KHÔNG ĐƯỢC GHI “DONE” CHỈ VÌ CLASS TỒN TẠI

Ví dụ:

Escalation.java exists

chỉ chứng minh:

ENTITY EXISTS

Không chứng minh:

PERSISTENCE WORKS

Tương tự:

AuditLog.java exists

không chứng minh:

AuditLog write path exists
88. FINAL VERIFICATION MATRIX
Capability	Source	Persistence	Security	Runtime evidence	Status
Escalation entity	✓/✗	✓/✗	—	✓/✗
Escalation create	✓/✗	✓/✗	✓/✗	✓/✗
Escalation queue	✓/✗	✓/✗	✓/✗	✓/✗
Admin detail	✓/✗	✓/✗	✓/✗	✓/✗
Admin decision	✓/✗	✓/✗	✓/✗	✓/✗
Enforcement	✓/✗	✓/✗	✓/✗	✓/✗
Audit event	✓/✗	✓/✗	✓/✗	✓/✗
AuditLog write path	✓/✗	✓/✗	✓/✗	✓/✗
IDOR	—	—	✓/✗	✓/✗
MongoDB safety	—	✓	—	✓
89. DATA CHANGE REPORT

Bắt buộc cuối Phase:

MongoDB:
Existing data deleted: NONE
Database reset: NO
Collection dropped: NO
deleteAll(): NO
deleteMany({}): NO
drop(): NO

Test records inserted:
<number></number>

Test records retained:
YES

Existing records modified:
<exact records / NONE>

Existing records deleted:
NONE

Phase 3 cũ cũng yêu cầu existing Order, Product, Shop, User, Review, Complaint, Escalation, Violation phải được giữ lại.

90. FILE CHANGE REPORT

Bắt buộc:

Files Created:

- ...

Files Modified:

- ...

Files Deleted:
NONE

Không ghi chung chung:

Updated escalation files

phải ghi path cụ thể.

91. DEPENDENCY FIX REPORT

Nếu phải sửa dependency:

Dependency:
<module></module>

Issue:
<actual issue></actual>

Impact:
<why Phase 3B cannot work>

Root Cause:
<actual root cause></actual>

Minimal Fix:
<exact change></exact>

Regression:
PASS / FAIL / NOT TESTED

Status:
FIXED / PARTIAL / WRONG
92. AUDIT WRITE-PATH REPORT

Bắt buộc báo riêng:

Audit Source of Truth:
<actual class/module>

Audit Writer:
<actual class></actual>

Audit Repository:
<actual repository></actual>

MongoDB Collection:
<actual collection if known></actual>

Write Path:
Business Action
→ Audit Writer
→ Repository
→ MongoDB

Persistence Verified:
YES / NO

Status:
IMPLEMENTED / PARTIAL / MISSING / WRONG

Đây là acceptance item bắt buộc, không được gộp chung vào “Audit integration”.

93. ESCALATION PERSISTENCE REPORT
    Escalation Entity:
    <actual></actual>

Repository:
<actual></actual>

Service:
<actual></actual>

Write Endpoint:
<actual></actual>

MongoDB Persistence:
YES / NO

Duplicate Protection:
YES / NO / NOT REQUIRED

Admin Read Path:
YES / NO

Persistence Verified:
YES / NO

Status:
IMPLEMENTED / PARTIAL / MISSING / WRONG
94. ENFORCEMENT BOUNDARY REPORT

Bắt buộc ghi:

Moderator Enforcement:
<exact actions allowed></exact>

Admin Enforcement:
<exact actions allowed></exact>

Moderator → Admin escalation:
<route/service>

Admin-only actions:
<exact actions></exact>

Unauthorized Moderator enforcement:
DENIED / NOT TESTED

Nếu source hiện tại cho Moderator quyền rộng hơn dự kiến:

SOURCE/POLICY MISMATCH

Không tự âm thầm sửa.

95. MANUAL TEST REPORT
    Moderator Escalation:
    PASS / FAIL / NOT TESTED

Escalation Persistence:
PASS / FAIL / NOT TESTED

Admin Queue:
PASS / FAIL / NOT TESTED

Admin Detail:
PASS / FAIL / NOT TESTED

Admin Decision:
PASS / FAIL / NOT TESTED

Admin Enforcement:
PASS / FAIL / NOT TESTED / INTEGRATION-READY

Audit Event:
PASS / FAIL / NOT TESTED

AuditLog Persistence:
PASS / FAIL / NOT TESTED

Moderator Authorization:
PASS / FAIL / NOT TESTED

Admin Authorization:
PASS / FAIL / NOT TESTED

IDOR:
PASS / FAIL / NOT TESTED

Actor Integrity:
PASS / FAIL / NOT TESTED

MongoDB Safety:
PASS

Không dùng source inspection để giả lập PASS.

96. KNOWN ISSUES

Mỗi issue:

Issue:
Current Behavior:
Expected Behavior:
Root Cause:
Affected Module:
Severity:
Phase 3B Impact:
Dependency Impact:
Minimal Fix:
Retest:
Status:

Ví dụ:

Issue:
Escalation class tồn tại nhưng chưa persist MongoDB

Current Behavior:
Service tạo object nhưng không có repository write path

Expected:
Escalation được lưu thực tế

Root Cause:
Persistence layer chưa được nối

Phase 3B Impact:
Escalation persistence = MISSING/PARTIAL

Fix:
Implement repository write path

Status:
PARTIAL
97. ĐIỀU KIỆN KẾT THÚC PHASE 3B

Phase 3B chỉ được coi là IMPLEMENTED khi:

Escalation
   ↓
Persistence
   ↓
Admin Queue
   ↓
Admin Decision
   ↓
Enforcement nếu thuộc scope
   ↓
AuditLog Write
   ↓
AuditLog Persistence
   ↓
Security
   ↓
IDOR
   ↓
Build
   ↓
Relevant verification

đã có evidence thực tế.

98. FINAL ARCHITECTURE SAU PHASE 3B
    MODERATOR
    │
    ▼
    MODERATION CASE
    │
    ┌──────┴──────┐
    │             │
    RESOLVE       ESCALATE
    │
    ▼
    ESCALATION RECORD
    │
    MongoDB PERSIST
    │
    ▼
    ADMIN ESCALATION
    QUEUE
    │
    ▼
    ADMIN
    │
    ┌──────────┴──────────┐
    │                     │
    RESOLVE              ENFORCE
    │
    ┌──────────────┼──────────────┐
    ▼              ▼              ▼
    PRODUCT         SHOP           VENDOR
    │              │              │
    └──────────────┼──────────────┘
    ▼
    PERSISTENCE
    │
    ▼
    AUDITLOG
    │
    ▼
    MongoDB
99. PHASE 3C HANDOFF

Phase 3B bàn giao cho Scheduler:

Escalation persistence
        ↓
Existing escalation state
        ↓
Existing timestamps/deadlines
        ↓
Idempotent escalation lookup

Scheduler ở Phase 3C sẽ consume persistence này, không tạo một escalation system khác.

100. PHASE 4 HANDOFF

Phase 3B bàn giao:

Escalation data
+
Enforcement history
+
AuditLog

cho các phase tích hợp sau.

Không triển khai ở đây:

Finance Dashboard
Platform Revenue
GMV
Vendor Payable
Full Audit Dashboard
Full System Regression
Performance Hardening
101. FINAL REPORT FORMAT

Sau khi thực hiện Phase 3B, báo cáo đúng format này:

# PHASE 3B — FINAL REPORT

## 1. Overall Status

Phase:
3B — Escalation + Enforcement + Audit

Implementation:
IMPLEMENTED / PARTIAL / MISSING / WRONG

Integration:
INTEGRATED / INTEGRATION-READY / PARTIAL
2. Capability
Escalation Entity:
<status></status>

Escalation Persistence:
<status></status>

Moderator Escalation:
<status></status>

Admin Queue:
<status></status>

Admin Decision:
<status></status>

Admin Enforcement:
<status></status>

Audit Event:
<status></status>

AuditLog Write Path:
<status></status>

AuditLog Persistence:
<status></status>

Security:
<status></status>

IDOR:
<status></status>
3. Enforcement Boundary
Moderator:
<actions></actions>

Admin:
<actions></actions>
4. Dependency Fixes
<exact files/modules>
5. Files
Created:
...

Modified:
...

Deleted:
NONE
6. MongoDB
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

Test records inserted:
<number></number>

Test records retained:
YES
7. Build
mvn clean package -DskipTests

BUILD:
PASS / FAIL
8. Manual Verification
Escalation:
PASS / FAIL / NOT TESTED

Persistence:
PASS / FAIL / NOT TESTED

Admin Queue:
PASS / FAIL / NOT TESTED

Enforcement:
PASS / FAIL / INTEGRATION-READY / NOT TESTED

AuditLog:
PASS / FAIL / NOT TESTED

Security:
PASS / FAIL / NOT TESTED

IDOR:
PASS / FAIL / NOT TESTED
9. Git Safety
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
Chốt phạm vi Phase 3B

Điểm quan trọng nhất của phase này là không lặp lại lỗi “có class = feature đã xong”.

Phase 3B phải chứng minh được hai write path thật:

Moderator/Admin Action
        ↓
EscalationService
        ↓
EscalationRepository
        ↓
MongoDB

Business Action
        ↓
AuditEvent/AuditWriter
        ↓
AuditLogRepository
        ↓
MongoDB

và boundary phải rõ:

MODERATOR
→ Review / Resolve / Escalate
                    │
                    ▼
                 ADMIN
                    │
                    ▼
          Final Platform Decision
                    │
                    ▼
               Enforcement

Escalation persistence và AuditLog persistence là điều kiện bắt buộc, không được hạ xuống INTEGRATION-READY chỉ vì chưa kiểm tra database. INTEGRATION-READY chỉ dùng khi external capability thật sự chưa tồn tại nhưng contract/seam của Phase 3B đã hoàn chỉnh. Cách phân loại này phù hợp với nguyên tắc status của Phase 3 cũ.

Và xuyên suốt Phase 3B: không delete/reset MongoDB, không tạo fake enforcement/refund/audit, không tạo duplicate Escalation/Audit system, không commit/push/merge/checkout/stash/reset.
