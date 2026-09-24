
PHASE 3C — SCHEDULER + IDEMPOTENCY
0. ĐỊNH VỊ PHASE 3C THEO BÁO CÁO ĐỐI CHIẾU CODE HIỆN TẠI

Phase 3C không được xem là “bật @Scheduled rồi gọi một service”.

Theo đối chiếu với source hiện tại:

Scheduler backend:
MISSING

Scheduler domain:
MISSING

Scheduler job/persistence:
MISSING

Idempotency domain:
MISSING

Idempotency record/state:
MISSING

Cơ chế chống duplicate execution:
MISSING

Trong khi Phase 3 cũ yêu cầu Scheduler phải có lifecycle:

Find overdue complaint
        ↓
Read current level/status
        ↓
Check deadline
        ↓
Check assignment
        ↓
Check existing escalation
        ↓
Determine eligibility
        ↓
Create/update escalation
        ↓
History
        ↓
Audit

và phải tránh:

scheduler run 1 → escalation 1
scheduler run 2 → escalation 2
scheduler run 3 → escalation 3

Các yêu cầu này nằm trực tiếp trong Phase 3 cũ.

Do đó Phase 3C phải được triển khai như một capability/domain mới có persistence và cơ chế idempotency thực sự, không phải chỉ thêm một annotation vào service hiện có.

1. TÊN PHASE
   PHASE 3C — SCHEDULER + IDEMPOTENCY
   Mục tiêu chính

Xây dựng capability:

Scheduler
+
Job Execution
+
Eligibility Detection
+
Idempotency
+
Duplicate Prevention
+
Execution History
+
Audit Integration

để hỗ trợ lifecycle Phase 3:

Complaint
   ↓
Deadline
   ↓
Scheduler detects overdue
   ↓
Eligibility check
   ↓
Escalation
   ↓
History
   ↓
Audit

Nhưng Phase 3C không được tự phát minh lại Complaint, Escalation, Moderator hoặc Admin.

Các domain đó phải được reuse từ Phase 3A/3B hoặc source hiện tại.

2. BÁO CÁO ĐỐI CHIẾU VỚI CODE HIỆN TẠI
   2.1. Scheduler

Audit hiện tại xác định:

Existing @Scheduled:
CHƯA XÁC NHẬN CAPABILITY PHASE 3

Scheduler service:
MISSING

Scheduler job:
MISSING

Scheduler persistence:
MISSING

Scheduler execution history:
MISSING

Vì vậy không được coi:

@Scheduled(...)

nếu có ở một dependency nào đó là bằng chứng Phase 3 Scheduler đã hoàn thành.

Phải trace:

@Scheduled
   ↓
Job
   ↓
Service
   ↓
Repository
   ↓
DB
   ↓
Business action

Nếu chỉ có annotation nhưng không có lifecycle thực tế:

PARTIAL / MISSING

không phải:

IMPLEMENTED
3. IDEMPOTENCY

Audit hiện tại cũng không xác nhận có:

SchedulerExecution
JobExecution
IdempotencyRecord
JobRun
ExecutionLock
ExecutionKey

hoặc cơ chế tương đương.

Do đó:

Idempotency:
MISSING

Đây là domain capability cần xây, không phải một configuration flag.

Mục tiêu:

Same logical job
+
Same target
+
Same execution window/event
        ↓
MUST NOT produce duplicate side effect

Ví dụ:

Complaint C001 overdue
        ↓
Scheduler Run #1
        ↓
Escalation created

Scheduler Run #2
        ↓
Same logical escalation
        ↓
NO duplicate escalation
4. SOURCE OF TRUTH

Thứ tự bắt buộc:

Current Source Code
        +
Phase 1 Final Result
        +
Phase 2 Final Result
        +
Phase 3A / 3B Result
        +
Phase 3C Specification
        +
Marketplace Policy
        +
SHOPEE_MARKETPLACE_MODEL

Phase 3 cũ yêu cầu không coi report cũ là bằng chứng implementation; capability phải được trace từ requirement đến source, persistence, security và runtime/build/test.

Nếu report cũ nói:

Scheduler DONE

nhưng source hiện tại không có scheduler hoạt động:

MISSING

Nếu có một phần:

@Scheduled
+
service

nhưng chưa có idempotency:

PARTIAL

Không được đánh dấu IMPLEMENTED.

5. PHẠM VI FEATURE CỦA PHASE 3C

Phase 3C được phép xây:

Scheduler infrastructure
Scheduler job
Scheduler configuration
Job execution model
Idempotency model
Idempotency key
Execution state
Execution persistence
Duplicate prevention
Eligibility evaluation
Escalation trigger integration
Execution history
Audit integration
Concurrency protection
Failure/retry handling nếu policy/architecture yêu cầu
Security boundary
Observability cần thiết
6. KHÔNG THUỘC FEATURE SCOPE

Không xây lại:

Complaint
Order
OrderItem
Moderator Foundation
Product Moderation
ReportCase
Violation
Admin Enforcement

nếu các domain đó đã tồn tại.

Phase 3 cũ cũng yêu cầu reuse các capability Phase 1–2 và không tạo domain song song.

7. DEPENDENCY SCOPE

Có thể sửa dependency ngoài Phase 3C chỉ khi nó trực tiếp ngăn Scheduler hoạt động.

Ví dụ:

Scheduler
   ↓
EscalationService
   ↓
BUG
   ↓
Không thể tạo escalation

Được phép:

Minimal dependency fix

Không được:

Rewrite EscalationService

hoặc xây:

SchedulerEscalationV2

nếu existing contract có thể mở rộng.

Báo cáo phải ghi:

Dependency:
EscalationService

Issue:
<actual issue></actual>

Impact:
Scheduler không thể thực hiện escalation

Minimal Fix:
<actual change></actual>

Regression:
PASS / FAIL / NOT TESTED
8. DISCOVERY BẮT BUỘC TRƯỚC KHI CODE

Không tạo file ngay.

Inspect:

Scheduler
@Scheduled
@EnableScheduling
Scheduling configuration
TaskScheduler
ScheduledExecutorService
existing jobs
existing background services
Complaint
Complaint
ComplaintStatus
ComplaintLevel
ComplaintRepository
ComplaintService
deadline fields
Escalation
Escalation
EscalationRepository
EscalationService
Admin Escalation
History
ComplaintHistory
EscalationHistory
ModerationHistory
Audit
Configuration
application.properties
application.yml
@ConfigurationProperties
existing timeout configuration
Security
SecurityConfig
role checks
service authorization
ownership
MongoDB
MongoRepository
@Document
indexes
existing unique indexes
Audit

Reuse:

AuditEventWriter
AuditLog
AuditLogEntry

nếu đã tồn tại.

Notification

Chỉ inspect integration point nếu Scheduler cần notification.

9. KHÔNG ĐỌC TOÀN BỘ PROJECT

Chỉ inspect dependency trực tiếp:

Scheduler
→ Complaint
→ Escalation
→ Deadline
→ Audit
→ MongoDB
→ Configuration
→ Security
→ Notification nếu cần

Không refactor toàn project.

10. SCHEDULER ARCHITECTURE

Kiến trúc mục tiêu:

Spring Scheduler
        ↓
Scheduler Job
        ↓
Scheduler Service
        ↓
Eligibility Service
        ↓
Idempotency Service
        ↓
Domain Service
        ↓
Repository

Cụ thể:

@Scheduled
    ↓
ComplaintEscalationScheduler
    ↓
ComplaintEscalationJobService
    ↓
findEligibleTargets()
    ↓
checkIdempotency()
    ↓
EscalationService
    ↓
Audit / History

Scheduler chỉ trigger job.

Không đặt toàn bộ business logic vào:

@Scheduled

Phase 3 cũ cũng quy định Scheduler phải gọi service thay vì tự chứa business logic.

11. SCHEDULER KHÔNG ĐƯỢC findAll() → ESCALATE

Cấm logic kiểu:

findAll complaints
        ↓
for each
        ↓
escalate

Scheduler phải lọc theo eligibility.

Flow:

Find candidate
        ↓
Current status
        ↓
Current level
        ↓
Deadline
        ↓
Assignment
        ↓
Existing escalation
        ↓
Policy/config
        ↓
Idempotency
        ↓
Action
12. ELIGIBILITY

Một complaint chỉ được Scheduler xử lý nếu tất cả điều kiện cần thiết đều đúng.

Tối thiểu phải inspect:

Complaint tồn tại
Complaint chưa resolved/closed
Deadline tồn tại nếu workflow yêu cầu
Deadline đã đến/hết
Current level phù hợp
Current assignment phù hợp
Chưa có escalation tương ứng
Không có execution đang xử lý cùng logical action

Không tự đặt thêm điều kiện business nếu Policy chưa yêu cầu.

13. DEADLINE

Deadline phải lấy từ:

Existing Complaint deadline

hoặc:

Existing configuration

hoặc contract đã được Phase 3A/3B xác định.

Không rải:

now.plusHours(48)

ở nhiều nơi.

Phase 3 cũ yêu cầu deadline phải:

stored
comparable
auditable
configurable nếu Policy yêu cầu

và không hard-code thời gian nghiệp vụ rải rác.

14. KHÔNG TỰ PHÁT MINH SLA

Không tự quyết định:

48h
72h
5 days
7 days

chỉ để Scheduler chạy được.

Nếu Policy/source đã có:

reuse

Nếu chưa có:

BUSINESS DECISION REQUIRED

hoặc:

CONFIGURATION REQUIRED

Không biến con số tự nghĩ ra thành business rule.

15. TIMEZONE

Inspect convention hiện tại.

Nếu project đang dùng:

UTC

thì reuse.

Nếu:

Asia/Ho_Chi_Minh

thì reuse.

Không đổi timezone toàn project trong Phase 3C.

Scheduler và deadline phải dùng cùng một strategy.

16. IDEMPOTENCY DOMAIN

Vì audit xác định idempotency chưa tồn tại, phải thiết kế capability thực sự.

Có thể cần một abstraction tương đương:

SchedulerExecution

hoặc:

JobExecution

hoặc:

IdempotencyRecord

Tên cuối cùng phải bám architecture thực tế.

Không bắt buộc tên class cụ thể.

17. MỤC ĐÍCH CỦA IDEMPOTENCY RECORD

Record phải cho phép trả lời:

Job nào?
Target nào?
Logical action nào?
Execution key nào?
Đã chạy chưa?
Đang chạy hay đã hoàn thành?
Kết quả thế nào?
Chạy lúc nào?

Tùy architecture, có thể cần:

id
jobType
targetType
targetId
operation
idempotencyKey
status
startedAt
completedAt
updatedAt
error/reference

Không thêm toàn bộ field chỉ vì danh sách này.

Chỉ thêm field thật sự cần để enforce idempotency.

18. IDEMPOTENCY KEY

Phải xác định một logical key ổn định.

Ví dụ về mặt khái niệm:

<jobType></jobtype>:<targetId></targetid>:<logicalAction></logicalaction>

Ví dụ:

COMPLAINT_ESCALATION:C001:MODERATOR_ESCALATION

Nhưng format cuối cùng phải dựa trên domain thực tế.

Không dùng:

random UUID

làm idempotency key cho cùng một logical event nếu mỗi lần Scheduler chạy lại tạo UUID mới.

Sai:

run 1 → UUID-A
run 2 → UUID-B

Đúng về nguyên tắc:

same logical operation
→ same logical idempotency identity
19. UNIQUE CONSTRAINT

Idempotency phải được enforce ở persistence layer nếu Mongo architecture cho phép.

Không chỉ:

if (repository.existsByKey(key)) {
    return;
}

vì hai scheduler instance có thể cùng chạy:

Instance A:
exists = false

Instance B:
exists = false

A → insert
B → insert

Do đó phải xem xét:

unique index
+
atomic insert

hoặc mechanism tương đương phù hợp MongoDB hiện tại.

20. MONGODB IDEMPOTENCY INDEX

Nếu tạo persistence model mới, phải xem xét unique index cho logical key.

Ví dụ về mặt thiết kế:

unique:
jobType
+
targetId
+
operation
+
logical execution identity

Không được tự ý tạo index không cần thiết.

Không được drop/recreate collection để tạo index.

Index migration phải:

NON-DESTRUCTIVE
21. ATOMIC CLAIM

Scheduler phải có khả năng:

claim execution

theo cách tránh hai worker cùng thực hiện một logical job.

Concept:

Candidate
   ↓
Atomic claim
   ↓
SUCCESS
   ↓
Execute

Candidate
   ↓
Atomic claim
   ↓
ALREADY CLAIMED
   ↓
SKIP

Không được dựa hoàn toàn vào:

check rồi insert

nếu race condition vẫn tồn tại.

22. CONCURRENT SCHEDULER

Phải xét:

Scheduler instance A
+
Scheduler instance B

hoặc:

same application
+
overlapping scheduler execution

Nếu architecture chỉ chạy single instance thì vẫn phải tránh duplicate execution do overlapping run.

Không mặc định giả định:

Scheduler chỉ chạy một lần.
23. KHÔNG LOCK TOÀN HỆ THỐNG

Không vì Phase 3C mà thay đổi persistence strategy toàn project.

Ưu tiên:

Existing Mongo architecture
        ↓
Existing repository pattern
        ↓
Minimal idempotency mechanism

Không thêm distributed lock framework nếu không thực sự cần.

24. EXECUTION STATUS

Nếu cần trạng thái execution, phải định nghĩa tối thiểu.

Ví dụ về mặt khái niệm:

RUNNING
SUCCESS
FAILED

Nhưng chỉ tạo enum/state khi architecture thực sự cần.

Không tự thêm:

QUEUED
RETRYING
CANCELLED
TIMEOUT
SKIPPED
ABORTED

nếu chưa có requirement.

25. FAILURE HANDLING

Nếu Scheduler xử lý:

10 complaints

và complaint thứ 4 lỗi:

Complaint 1 → success
Complaint 2 → success
Complaint 3 → success
Complaint 4 → failure
Complaint 5 → ?

Phải xác định behavior theo architecture.

Không để một exception vô tình làm chết toàn bộ job nếu requirement cần tiếp tục.

Nhưng cũng không tự phát minh retry policy.

26. RETRY

Chỉ implement retry nếu:

Policy
+
technical requirement
+
existing architecture

yêu cầu.

Không mặc định:

retry 3 times

Không tự đặt:

5 minutes
10 minutes
30 minutes
27. IDEMPOTENCY VỚI ESCALATION

Đây là use case trọng tâm.

Flow:

Complaint C001
   ↓
overdue
   ↓
Scheduler
   ↓
Eligibility
   ↓
Idempotency Key
   ↓
Claim
   ↓
Existing Escalation?
   ├── YES → NOOP
   └── NO
        ↓
Create Escalation
        ↓
History
        ↓
Audit

Không tạo:

Escalation #1
Escalation #2
Escalation #3

cho cùng logical escalation.

28. EXISTING ESCALATION CHECK

Trước khi tạo escalation:

find existing escalation

theo boundary thực tế của Phase 2/3B.

Kiểm tra:

target
complaint
level
operation
status

nếu các field đó tồn tại.

Không tạo ComplaintEscalationV2.

Phase 3 cũ yêu cầu reuse Escalation, EscalationRepository, EscalationService, Admin Escalation.

29. IDEMPOTENCY ≠ EXISTING ESCALATION CHECK

Hai lớp này không được nhầm lẫn.

Existing Escalation Check
=========================

domain-level protection

Trong khi:

Idempotency
===========

execution-level protection

Cả hai đều cần thiết.

Ví dụ:

Scheduler A
Scheduler B

cùng thấy chưa có escalation.

Idempotency phải ngăn hai execution cùng tạo side effect.

30. SCHEDULER EXECUTION FLOW

Flow chuẩn:

Scheduler Trigger
        ↓
Acquire/Start Job Execution
        ↓
Find Candidates
        ↓
For each Candidate
        ↓
Validate Current State
        ↓
Validate Deadline
        ↓
Validate Assignment
        ↓
Validate Existing Escalation
        ↓
Build Idempotency Key
        ↓
Atomic Idempotency Claim
        ↓
Execute Domain Action
        ↓
Persist Result
        ↓
Write History
        ↓
Write Audit
31. JOB EXECUTION PERSISTENCE

Nếu Scheduler là domain mới, phải xác định persistence.

Có thể có:

SchedulerExecution

với:

@Document

hoặc architecture tương đương.

Mục đích:

auditability
+
idempotency
+
debugging

Không lưu execution state chỉ trong:

static variable

hoặc:

memory

nếu cần survive application restart.

32. APPLICATION RESTART

Phải xem xét:

Application
   ↓
Scheduler starts
   ↓
Previous job was RUNNING

Không được mặc định:

memory cleared
→ execute again blindly

Cách xử lý phải dựa trên execution state và architecture.

Không tự tạo recovery policy nếu chưa có business/technical requirement.

33. MULTIPLE INSTANCE

Nếu project có khả năng chạy nhiều instance:

Instance A
Instance B

phải đảm bảo:

same logical job
→ one effective execution

Nếu hiện tại project chưa hỗ trợ distributed deployment, vẫn phải thiết kế persistence idempotency không phụ thuộc hoàn toàn vào in-memory state.

34. SCHEDULER CONFIGURATION

Scheduler frequency có thể là configuration.

Ví dụ:

scheduler.enabled
scheduler.interval

nhưng không tự chọn giá trị business.

Nếu chưa có configuration contract:

Define technical configuration

nhưng không hard-code SLA.

Phân biệt:

Scheduler polling interval

với:

Complaint response SLA

Hai thứ này không phải một.

35. POLLING INTERVAL ≠ BUSINESS DEADLINE

Ví dụ:

Scheduler chạy mỗi 5 phút

không có nghĩa:

Vendor có 5 phút để trả lời.

Scheduler interval chỉ là technical configuration.

Business deadline phải lấy từ:

Complaint / Policy / Configuration
36. SCHEDULER ENABLE/DISABLE

Nếu thêm:

scheduler.enabled

phải bảo đảm:

disabled
→ không chạy job

nhưng không được:

disabled
→ xóa execution

hoặc:

disabled
→ reset state
37. NO FAKE SCHEDULER

Cấm:

if (true) {
    escalate();
}

Cấm:

escalateAll();

Cấm:

findAll().forEach(...)

mà không có eligibility.

Phase 3 cũ quy định Scheduler phải dựa trên status, level, deadline, assignment, existing escalation và policy/config.

38. AUDIT

Reuse Audit Source of Truth.

Không tạo:

SchedulerAudit
SchedulerAuditLogV2

nếu existing Audit có thể sử dụng.

Các event có thể cần audit:

Scheduler Job Started
Scheduler Job Completed
Scheduler Execution Claimed
Scheduler Action Executed
Scheduler Action Skipped
Scheduler Action Failed
Escalation Created By Scheduler

Nhưng chỉ audit những event thực sự được implementation.

Không log PII không cần thiết.

39. AUDIT KHÔNG THAY THẾ IDEMPOTENCY

Không được nghĩ:

đã ghi Audit
→ coi như idempotent

Audit:

record what happened

Idempotency:

prevent duplicate side effect

Hai capability khác nhau.

40. HISTORY

Nếu Complaint/Escalation đã có history:

reuse

Scheduler chỉ append transition cần thiết.

Không tạo:

SchedulerHistoryV2

nếu history hiện tại đủ.

Không delete historical records.

41. NOTIFICATION

Nếu Scheduler cần notification:

existing Notification
        ↓
reuse

Nếu chưa có:

Notification Contract
        ↓
Integration seam
        ↓
INTEGRATION-READY

Không fake:

email sent
SMS sent
push sent
42. SECURITY

Scheduler là server-side capability.

Không expose endpoint cho Customer/Vendor để:

run scheduler
execute job
force escalation
change idempotency

trừ khi có requirement rõ ràng.

Nếu có administrative trigger:

Admin-only

và phải enforce ở backend.

43. IDOR

Nếu có endpoint xem execution:

GET /scheduler/executions/{id}

phải có authorization.

Không cho user đọc execution record chỉ bằng ID.

Không expose internal execution data ra Customer UI.

44. CUSTOMER / VENDOR KHÔNG ĐƯỢC CAN THIỆP SCHEDULER

Không cho:

Customer → trigger scheduler
Vendor → trigger scheduler
Customer → mark overdue
Vendor → mark overdue
Customer → create escalation directly
Vendor → manipulate idempotency

nếu không có requirement.

45. ADMIN / MODERATOR

Moderator/Admin có thể có business actions:

Resolve
Escalate
Request Evidence

nhưng Scheduler phải là:

system-triggered

Không trộn:

manual action

với:

scheduled action

trong cùng một idempotency key nếu chúng là hai logical operations khác nhau.

46. MANUAL ESCALATION VS SCHEDULED ESCALATION

Ví dụ:

Moderator manually escalates C001

và sau đó:

Scheduler sees C001 overdue

Scheduler phải nhận biết complaint đã được xử lý/escalated.

Không tạo duplicate.

47. IDEMPOTENCY KEY PHẢI PHÂN BIỆT OPERATION

Không dùng:

C001

làm key cho mọi thứ.

Vì một complaint có thể có:

MODERATOR_ESCALATION
ADMIN_ESCALATION
NOTIFICATION

Logical operations khác nhau.

Key phải phân biệt được operation theo architecture.

48. KHÔNG XÓA IDEMPOTENCY RECORD

Idempotency record là historical/technical state.

Không cleanup bằng:

deleteAll()
deleteMany({})
drop()

Không xóa record chỉ vì:

test
debug
duplicate
scheduler failed
49. MONGODB SAFETY — TUYỆT ĐỐI

Phase 3C phải kế thừa nguyên tắc của toàn bộ phase:

CẤM TUYỆT ĐỐI
deleteAll()
deleteMany({})
drop()
dropDatabase()
truncate
reset collection
reset database
destructive migration

Không có ngoại lệ.

Phase 3 cũ xác định rõ MongoDB data phải được giữ nguyên và test data nếu tạo thì chỉ insert, không delete.

50. KHÔNG XÓA DATA KHI TẠO INDEX

Nếu cần unique index cho idempotency:

KHÔNG:
drop collection
recreate collection
delete duplicate data tự động

Nếu existing data gây conflict:

STOP
+
REPORT CONFLICT
+
BUSINESS/TECHNICAL DECISION REQUIRED

Không tự xóa record cũ để migration thành công.

51. NẾU EXISTING DATA TRÙNG IDEMPOTENCY KEY

Đây là tình huống quan trọng.

Nếu phát hiện:

existing records
+
duplicate logical key

Không:

delete duplicate

Không:

keep latest
delete oldest

Không:

reset collection

Phải báo:

MONGODB DATA CONFLICT

và xác định:

affected collection
affected records
required migration decision

Migration nếu cần phải non-destructive.

52. TEST DATA

Nếu cần dữ liệu:

INSERT ONLY

Ví dụ:

[TEST-PHASE-3C] Complaint
[TEST-PHASE-3C] SchedulerExecution
[TEST-PHASE-3C] Escalation

Chỉ tạo:

1–2 records / scenario

Không tạo hàng trăm record.

Không delete sau test.

53. TEST CASE — BASIC SCHEDULER

Tạo một scenario hợp lệ:

Complaint
+
deadline overdue
+
eligible
+
no existing escalation

Chạy scheduler.

Expected:

ONE execution
+
ONE escalation
+
history
+
audit
54. TEST CASE — REPEATED RUN

Cùng complaint.

Chạy scheduler:

Run 1
Run 2
Run 3

Expected:

1 logical escalation

Không:

3 escalations

Đây là acceptance test quan trọng nhất của Phase 3C.

55. TEST CASE — EXISTING ESCALATION

Scenario:

Complaint overdue
+
existing valid escalation

Scheduler chạy.

Expected:

NO duplicate escalation

Có thể:

NOOP

hoặc behavior theo existing escalation contract.

56. TEST CASE — NOT OVERDUE
    Complaint

deadline future

Scheduler chạy.

Expected:

NO escalation

Không tạo execution side effect nghiệp vụ.

57. TEST CASE — RESOLVED
    Complaint

status resolved/closed
+
deadline passed

Scheduler chạy.

Expected:

NO escalation

Không reopen complaint.

58. TEST CASE — WRONG LEVEL

Nếu complaint đang ở level không phù hợp với scheduled action:

NO ACTION

Không tự chuyển level.

Level transition phải theo contract Phase 3B.

59. TEST CASE — ASSIGNMENT

Nếu policy/workflow yêu cầu assignment:

check assignment

Không escalate chỉ vì deadline đã qua.

Không tự tạo assignment giả.

60. TEST CASE — CONCURRENT EXECUTION

Nếu có thể test:

Scheduler A
+
Scheduler B

cùng logical target.

Expected:

one effective execution
one side effect

Không duplicate escalation.

61. TEST CASE — APPLICATION RESTART

Nếu execution persistence được thiết kế để survive restart:

Run
↓
restart
↓
run again

phải không tạo duplicate side effect.

Không dùng in-memory Set làm cơ chế duy nhất.

Sai:

Set<String></string> processedKeys;

vì restart sẽ mất state.

62. TEST CASE — FAILED EXECUTION

Nếu job fail:

Execution
→ FAILED

phải giữ record để trace.

Không xóa:

FAILED execution

chỉ để lần chạy sau “sạch”.

63. TEST CASE — RETRY

Chỉ test retry nếu retry đã được định nghĩa.

Nếu chưa có:

Retry:
OUT OF SCOPE / BUSINESS DECISION REQUIRED

không tự thêm retry policy.

64. TEST CASE — MANUAL ACTION THEN SCHEDULER
    Manual escalation
    ↓
    Scheduler

Expected:

Scheduler detects existing state
        ↓
No duplicate
65. TEST CASE — SCHEDULER THEN MANUAL ACTION
Scheduler
   ↓
Escalation
   ↓
Moderator/Admin

Manual action phải tiếp tục trên domain state hiện tại.

Không reset idempotency record.

66. TEST CASE — WRONG ACTOR

Customer/Vendor không được gọi scheduler internal action.

Expected:

DENIED
67. TEST CASE — IDEMPOTENCY RECORD ACCESS

Nếu execution API tồn tại:

Unauthorized user
→ execution/{id}

Expected:

DENIED
68. BUILD

Theo workflow Phase 3:

mvn clean package -DskipTests

Chỉ chạy command này cho build verification.

Không tự động chạy:

mvn test
mvn verify
mvn install
mvn package

và không tự động start Spring Boot. Quy tắc này được giữ nguyên từ Phase 3 cũ.

69. WEBSITE

Chỉ start website nếu user yêu cầu:

mvn spring-boot:run

Không tự start.

70. MANUAL TEST SCHEDULER

Khi được yêu cầu test website:

Create/identify valid complaint
        ↓
Set/use actual overdue state
        ↓
Run Scheduler
        ↓
Check Escalation
        ↓
Run Scheduler again
        ↓
Check no duplicate
        ↓
Check history
        ↓
Check audit

Không fake:

scheduler success

chỉ vì method đã được gọi.

71. KHÔNG ĐƯỢC ĐỔI DATA THỰC ĐỂ TEST DEADLINE

Không được sửa hàng loạt production data để làm:

deadline = yesterday

Nếu cần test:

INSERT test scenario

hoặc dùng test environment.

Không mutate hàng loạt existing complaints.

72. KHÔNG DELETE TEST DATA

Sau test:

Test records retained = YES

Không:

cleanup
delete
reset
73. GIT SAFETY

Trước implementation:

git status
git branch
git log --oneline --decorate -10

Không:

git reset
git stash
git checkout
git switch
git merge
git rebase
git commit
git push

Không overwrite uncommitted code.

74. REUSE RULE

Trước khi tạo:

Scheduler
SchedulerExecution
IdempotencyRecord
Repository
Service
Config

phải:

Search existing
      ↓
Exists?
 ├── YES → REUSE / EXTEND
 └── NO  → CREATE

Đặc biệt không tạo:

SchedulerV2
EscalationSchedulerV2
IdempotencyServiceV2
AuditSchedulerV2

chỉ vì muốn tách tên.

75. FILE SCOPE DỰ KIẾN

Chỉ tạo/sửa file trực tiếp liên quan.

Ví dụ có thể gồm:

Scheduler
Scheduler configuration
Scheduler service
Scheduler execution domain
Idempotency domain
Idempotency repository
Escalation integration
Complaint eligibility service
Audit integration
Mongo configuration/index

Tên file thực tế phải dựa trên package structure hiện tại.

Không yêu cầu bắt buộc phải tạo đúng các class tên trên.

76. CONTROLLER

Phase 3C không cần tạo controller chỉ để “có API”.

Scheduler có thể hoàn toàn server-side.

Nếu cần administrative trigger để debug/manual operation:

Admin-only

và phải có authorization.

Không expose scheduler endpoint cho Customer/Vendor.

77. SERVICE BOUNDARY

Ưu tiên:

Scheduler
   ↓
SchedulerService
   ↓
EligibilityService
   ↓
IdempotencyService
   ↓
Existing Domain Service

Không:

Scheduler
→ Repository
→ trực tiếp modify Complaint

Business logic chính phải nằm trong service/domain layer.

78. REPOSITORY BOUNDARY

Repository chịu trách nhiệm:

find candidates
find execution
claim execution
persist execution

Không đặt business rule lớn trong repository.

79. TRANSACTION / CONSISTENCY

Nếu một operation cần:

Idempotency claim
+
Escalation
+
History
+
Audit

phải inspect consistency requirement.

Không thêm @Transactional hàng loạt một cách máy móc.

MongoDB transaction chỉ dùng nếu architecture/current persistence strategy phù hợp và thực sự cần.

80. ATOMICITY

Điểm đặc biệt phải kiểm tra:

claim succeeded
+
business action failed

Không để execution record ở trạng thái gây:

permanent skip

nếu business action chưa thực sự hoàn thành.

Ngược lại cũng không được retry mù quáng gây duplicate side effect.

Phải xác định state transition rõ ràng.

81. IDEMPOTENCY STATE MACHINE

Tùy architecture, có thể có:

NOT_STARTED
    ↓
CLAIMED/RUNNING
    ↓
SUCCESS

hoặc:

RUNNING
 ↓
FAILED

Nhưng chỉ dùng state cần thiết.

Không tạo state machine quá lớn.

82. EXACTLY-ONCE vs EFFECTIVELY-ONCE

Không tuyên bố:

exactly once

nếu architecture không chứng minh được.

Mục tiêu thực tế:

same logical scheduler operation
→ effectively one business side effect

Thông qua:

idempotency
+
unique constraint
+
domain state check
83. SCHEDULER OBSERVABILITY

Nên có đủ thông tin để xác định:

Job started
Job finished
Candidates found
Candidates skipped
Actions executed
Actions failed

Nhưng không log PII.

Không log:

phone
address
full evidence URL
private customer information

nếu không cần.

84. LOGGING

Log nên ưu tiên:

jobType
executionId
targetId
operation
result
error category
timestamp

Không log dữ liệu nhạy cảm.

85. NO FAKE METRICS

Không tự tạo:

scheduler processed = 100
escalated = 20
success = 99%

nếu chưa có execution thật.

Dashboard metrics thuộc Phase 4 nếu chưa có requirement trực tiếp.

86. KHÔNG LẤN SANG PHASE 4

Phase 3C không triển khai:

Finance Dashboard
GMV
Vendor Sales
Platform Revenue
Vendor Payable
Refund Dashboard
Full System Dashboard
Performance Hardening
Full Regression
Final Integration
Banner verification

Phase 3 cũ xác định các phần này thuộc Phase 4.

87. KHÔNG LÀM LẠI PHASE 1–2

Không rebuild:

Moderator Foundation
Moderator Security
Product Moderation
Auto Moderation
ReportCase
Violation
Admin Escalation
Admin Enforcement

Nếu shared component lỗi trực tiếp:

Inspect
↓
Minimal Fix
↓
Regression
88. KHÔNG LÀM MẤT CHỨC NĂNG CŨ

Đặc biệt phải bảo vệ:

Complaint
Escalation
Violation
Moderator
Admin
Audit
Order
Shop
User

Nếu sửa shared entity:

Regression required
89. DEPENDENCY VỚI PHASE 3A/3B

Phase 3C không được giả định Phase 3A/3B đã hoàn thiện nếu source chưa chứng minh.

Phải kiểm tra:

Complaint exists?
deadline exists?
status exists?
level exists?
Escalation exists?
Escalation persistence works?
History exists?
Audit integration exists?

Nếu capability chưa có:

INTEGRATION-READY

nếu Phase 3C có thể định nghĩa seam.

Không báo:

BLOCKED – teammate chưa code

trừ khi technical blocker thật sự.

90. ESCALATION CONTRACT

Scheduler chỉ nên gọi:

existing EscalationService

hoặc contract tương đương.

Không tự persist escalation nếu existing service chịu trách nhiệm domain.

Flow:

Scheduler
   ↓
Eligibility
   ↓
Idempotency
   ↓
EscalationService
91. SCHEDULER KHÔNG QUYẾT ĐỊNH BUSINESS

Scheduler trả lời:

"Đã đến lúc kiểm tra action này chưa?"

Không tự quyết định:

"Vendor có lỗi không?"
"Vendor bị phạt bao nhiêu?"
"Refund bao nhiêu?"
"Shop bị suspend không?"

Các quyết định đó thuộc domain/policy tương ứng.

92. ESCALATION LEVEL

Nếu existing workflow có:

LEVEL 0
LEVEL 1
LEVEL 2

Scheduler chỉ thực hiện transition được phép.

Không tự tạo:

LEVEL 3
LEVEL 4

chỉ để scheduler dễ code.

93. ADMIN ESCALATION

Nếu Scheduler cần:

Moderator overdue
→ Admin escalation

phải reuse Admin Escalation.

Không tạo:

SchedulerAdminEscalation

nếu existing contract đáp ứng.

94. NOTIFICATION IDEMPOTENCY

Nếu notification được trigger bởi scheduler:

Scheduler
→ Escalation
→ Notification

phải xem notification có cơ chế idempotency riêng hay không.

Không giả định:

one escalation
→ one notification

nếu notification infrastructure chưa định nghĩa.

Nếu chưa có:

Notification:
INTEGRATION-READY
95. AUDIT IDEMPOTENCY

Audit event cũng không được duplicate vô hạn nếu cùng logical action bị retry.

Nếu architecture yêu cầu:

same action
→ one business audit event

thì audit event phải có logical identity hoặc được gọi sau khi domain action thành công.

Không ghi:

Escalation Created

ba lần cho một escalation chỉ vì scheduler retry.

96. ACCEPTANCE CRITERIA

Phase 3C chỉ đạt IMPLEMENTED khi:

Scheduler
Scheduler exists
+
Job executes
+
Candidate detection works
+
Eligibility works
+
Deadline works
Idempotency
Logical key exists
+
Persistence exists
+
Duplicate execution prevented
+
Concurrent execution protected
Integration
Scheduler
↓
Existing EscalationService

hoặc integration contract thực tế tương đương.

Persistence
Execution state persisted
+
No destructive migration
Audit
Relevant execution/action auditable
Safety
MongoDB existing data preserved
Build
mvn clean package -DskipTests
→ PASS
Test

Ít nhất phải chứng minh được:

overdue → action
repeat run → no duplicate
not overdue → no action
existing escalation → no duplicate

nếu các scenario này khả dụng trong source hiện tại.

97. STATUS CLASSIFICATION
    IMPLEMENTED
    Scheduler code exists

Persistence exists
+
Idempotency exists
+
Behavior verified
INTEGRATION-READY

Ví dụ:

Scheduler
+
Idempotency
+
Execution persistence
+
Escalation contract

đã sẵn sàng nhưng external domain chưa kết nối.

INTEGRATED
Scheduler
→ actual Complaint
→ actual Escalation
→ actual Audit

và test được end-to-end.

PARTIAL

Có scheduler nhưng:

idempotency missing

hoặc:

persistence incomplete
MISSING

Không có scheduler/idempotency implementation.

WRONG

Có implementation nhưng:

duplicate escalation

hoặc:

scheduler ignores deadline

hoặc:

destructive behavior
BUSINESS DECISION REQUIRED

Business rule còn thiếu.

BLOCKED

Chỉ dùng khi có technical blocker thực sự.

98. MONGODB DATA CHANGE REPORT

Bắt buộc cuối phase:

MongoDB:
Existing data deleted: NONE
Database reset: NO
Collection dropped: NO
deleteAll(): NO
deleteMany({}): NO
Destructive migration: NO

New collections:
<actual></actual>

Indexes added:
<actual></actual>

Existing records modified:
<actual></actual>

Test records inserted:
<number></number>

Test records deleted:
NO

Nếu không insert test data:

Test records inserted:
0
99. FILE CHANGE REPORT
Files Created:

- ...

Files Modified:

- ...

Files Deleted:
NONE

Không xóa file để “dọn project”.

Nếu phát hiện duplicate architecture:

DUPLICATED / CONFLICT

phải báo rõ trước khi xóa bất kỳ thứ gì.

100. MANUAL TEST REPORT

Báo riêng:

Scheduler Trigger:
PASS / FAIL / NOT TESTED

Overdue Detection:
PASS / FAIL / NOT TESTED

Eligibility:
PASS / FAIL / NOT TESTED

Idempotency:
PASS / FAIL / NOT TESTED

Repeated Scheduler Run:
PASS / FAIL / NOT TESTED

Existing Escalation Protection:
PASS / FAIL / NOT TESTED

Concurrent Execution:
PASS / FAIL / NOT TESTED

Execution Persistence:
PASS / FAIL / NOT TESTED

Audit:
PASS / FAIL / NOT TESTED

Security:
PASS / FAIL / NOT TESTED

MongoDB Safety:
PASS

Không ghi PASS nếu chưa test thực tế.

101. KNOWN ISSUES

Mỗi issue:

Issue:
<name></name>

Current Behavior:
<actual></actual>

Expected Behavior:
<expected></expected>

Source:
<file/class>

Root Cause:
<actual></actual>

Impact:
<impact></impact>

Status:
PARTIAL / MISSING / WRONG /
INTEGRATION-READY /
BUSINESS DECISION REQUIRED
102. INTEGRATION REPORT

Cho từng integration:

Integration Point:
Escalation

Existing Contract:
EscalationService

Current Implementation:
AVAILABLE / NOT AVAILABLE

Phase 3C Implementation:
Scheduler trigger

External Capability:
AVAILABLE / NOT AVAILABLE

Idempotency:
AVAILABLE / NOT AVAILABLE

Integration Seam:
<actual></actual>

Status:
IMPLEMENTED /
INTEGRATED /
INTEGRATION-READY /
PARTIAL /
MISSING /
WRONG

Evidence:
<source/build/test>
103. DEPENDENCY FIX REPORT

Nếu sửa dependency:

Dependency:
<module></module>

Issue:
<actual issue></actual>

Impact on Phase 3C:
<actual impact></actual>

Minimal Fix:
<change></change>

Regression:
PASS / FAIL / NOT TESTED

Status:
FIXED / PARTIAL
104. GIT SAFETY REPORT
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
105. BUILD REPORT
Command:
mvn clean package -DskipTests

BUILD:
PASS / FAIL

Nếu fail:

Root Cause:
<actual></actual>

Affected Module:
<actual></actual>

Phase 3C Impact:
YES / NO / UNKNOWN
106. FINAL PHASE 3C REPORT

Cuối cùng phải kết luận theo format:

PHASE 3C
SCHEDULER + IDEMPOTENCY

Implementation:
IMPLEMENTED / PARTIAL / MISSING / WRONG

Integration:
INTEGRATED / INTEGRATION-READY
Completed
Scheduler:
<status></status>

Eligibility:
<status></status>

Deadline:
<status></status>

Idempotency:
<status></status>

Duplicate Prevention:
<status></status>

Concurrency Protection:
<status></status>

Execution Persistence:
<status></status>

Escalation Integration:
<status></status>

Audit:
<status></status>
Not Completed
<actual items></actual>
MongoDB
Existing data:
PRESERVED

Database reset:
NO

Collection drop:
NO

deleteAll:
NO

deleteMany({}):
NO

Test data deleted:
NO
Build
mvn clean package -DskipTests:
PASS / FAIL
107. ĐIỀU KIỆN ĐỂ ĐƯỢC COI LÀ HOÀN THÀNH

Không được đánh dấu Phase 3C IMPLEMENTED chỉ vì có:

@Scheduled

Phase 3C phải chứng minh được chuỗi:

Scheduler
   ↓
Candidate Detection
   ↓
Eligibility
   ↓
Deadline
   ↓
Idempotency Identity
   ↓
Atomic Claim / Duplicate Protection
   ↓
Existing Domain Service
   ↓
Persistence
   ↓
History
   ↓
Audit

và quan trọng nhất:

Run 1
→ side effect

Run 2
→ NO duplicate side effect

Run 3
→ NO duplicate side effect

Đây chính là điểm khiến Scheduler + Idempotency là một capability/domain mới, chứ không phải một thay đổi cấu hình nhỏ.

108. NGUYÊN TẮC CUỐI CÙNG CỦA PHASE 3C
     BÁM CODE HIỆN TẠI
     ↓
     XÁC NHẬN CAPABILITY THỰC TẾ
     ↓
     SCHEDULER ĐANG MISSING
     ↓
     IDEMPOTENCY ĐANG MISSING
     ↓
     XÂY DOMAIN MỚI TỐI THIỂU
     ↓
     REUSE COMPLAINT / ESCALATION
     ↓
     PERSIST EXECUTION STATE
     ↓
     ENFORCE IDEMPOTENCY
     ↓
     PREVENT DUPLICATE SIDE EFFECT
     ↓
     AUDIT
     ↓
     BUILD
     ↓
     MANUAL TEST
     ↓
     REPORT TRUNG THỰC

Tuyệt đối không:

xóa MongoDB
reset MongoDB
xóa test data
fake scheduler
fake escalation
fake refund
tạo duplicate domain
tự phát minh SLA
tự phát minh escalation rule
rewrite Phase 1
rewrite Phase 2
nhảy sang Phase 4
chờ teammate
commit
push
merge
checkout
stash
reset

Các nguyên tắc này giữ nguyên tinh thần của Phase 3 cũ: reuse source hiện tại, không fake production behavior, không fake scheduler, không xóa/reset MongoDB và mỗi capability phải được xác minh bằng source + persistence + security + runtime/build/test.
