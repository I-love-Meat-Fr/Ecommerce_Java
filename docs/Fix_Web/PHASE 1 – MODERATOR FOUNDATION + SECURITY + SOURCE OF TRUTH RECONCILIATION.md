
# PHASE 1 – MODERATOR FOUNDATION + SECURITY + SOURCE OF TRUTH RECONCILIATION

## Implementation Instruction

Đọc Phase 1 này như **source of truth cho phạm vi triển khai Phase 1**.

Phase 1 được xây dựng dựa trên:

1. Source code hiện tại sau merge.
2. `AUDIT REPORT — POST-MERGE CNJAVA MARKETPLACE`.
3. `BỘ LUẬT & CHÍNH SÁCH CỦA SÀN MARKETPLACE`.
4. `SHOPEE_MARKETPLACE_MODEL.md` đã được cung cấp trước đó.

`SHOPEE_MARKETPLACE_MODEL.md` là baseline nghiệp vụ/conceptual model dùng xuyên suốt các Phase. Không cần yêu cầu người dùng đính kèm lại. Chỉ đọc section liên quan trực tiếp đến Phase 1, không đọc lại toàn bộ file nếu không cần.

---

# 0. NGUYÊN TẮC THỰC HIỆN QUAN TRỌNG

## 0.1 Audit Report chỉ là INPUT, không phải bằng chứng hoàn thành

Audit Report có thể ghi:

```text
EXISTS
DONE
IMPLEMENTED
```

nhưng **không được mặc định chức năng đó đang hoạt động đúng**.

Phải kiểm tra lại bằng source code thực tế.

Ví dụ:

```text
Audit:
Moderator Queue = EXISTS
```

Không được kết luận:

```text
Moderator Queue = IMPLEMENTED
```

chỉ dựa vào Audit.

Phải kiểm tra:

```text
UI
 ↓
Route
 ↓
Controller
 ↓
Service
 ↓
Repository
 ↓
Database
 ↓
Security
 ↓
Runtime behavior
 ↓
Test
```

Chỉ sau khi có đủ bằng chứng mới xác định status.

---

# 0.2 Phase Scope và Dependency Scope là hai khái niệm khác nhau

## Feature Scope

Là những capability Phase 1 được phép triển khai/sửa:

```text
Moderator Foundation
Security
Role Routing
Account Status Guard
KYC Source-of-Truth
AuditLog Source-of-Truth
Review Moderation Status
Reject → Violation consistency
ReportCase Status
Regression Protection
Integration Contract
```

Không được tự ý xây feature của Phase 2/3/4.

## Dependency Scope

Là những component hiện có, kể cả thuộc feature/phase khác, nhưng **trực tiếp ảnh hưởng đến khả năng hoàn thành hoặc kiểm chứng Phase 1**.

Ví dụ:

```text
ModeratorService
    ↓
ShopService
    ↓
KYC status
```

Nếu `ShopService` đang lỗi và khiến Moderator KYC flow fail thì:

```text
KHÔNG được đánh dấu OUT OF SCOPE chỉ vì Shop/KYC thuộc phần khác.
```

Phải:

```text
inspect dependency
↓
xác định root cause
↓
fix tối thiểu nếu cần
↓
retest Phase 1
```

Nhưng không được mở rộng thành việc xây toàn bộ feature của Phase khác.

---

# 0.3 OUT OF SCOPE chỉ được dùng khi thực sự không ảnh hưởng Phase 1

Chỉ được đánh dấu:

```text
OUT OF SCOPE
```

khi đồng thời thỏa mãn:

```text
Không thuộc acceptance criteria Phase 1
+
Không phải dependency cần thiết để Phase 1 hoạt động
+
Không gây failure/regression cho Phase 1
+
Không cần thay đổi để hoàn thành Phase 1
```

Không được dùng:

```text
OUT OF SCOPE
```

để bỏ qua:

```text
compile error
runtime error
dependency error
security regression
integration error
test failure
```

nếu các lỗi đó trực tiếp ảnh hưởng Phase 1.

---

# 0.4 Mỗi Phase phải audit source độc lập

Không được kế thừa trạng thái:

```text
DONE
EXISTS
IMPLEMENTED
```

từ Phase trước hoặc Audit Report mà không kiểm tra lại.

Mỗi lần bắt đầu Phase 1 phải thực hiện:

```text
Read Phase 1
   ↓
Read relevant Policy
   ↓
Read relevant Marketplace Model sections
   ↓
Inspect current source
   ↓
Inspect dependencies
   ↓
Verify runtime/test behavior
   ↓
Determine actual status
```

Không được giả định:

```text
Phase trước làm rồi
→ Phase này bỏ qua
```

---

# 0.5 Không được đánh giá chức năng chỉ dựa trên sự tồn tại của class/file

Ví dụ:

```text
ModeratorController.java tồn tại
```

không có nghĩa:

```text
Moderator Foundation = IMPLEMENTED
```

Phải kiểm tra:

```text
Route có đúng không?
Security có đúng không?
Controller có gọi đúng service không?
Service có behavior đúng không?
Repository có dữ liệu đúng không?
UI có gọi đúng API không?
Runtime có hoạt động không?
```

Tương tự:

```text
Service tồn tại
Repository tồn tại
Entity tồn tại
Test class tồn tại
```

đều **không tự động chứng minh feature đã hoàn thành**.

---

# 0.6 Nếu phát hiện lỗi, phải truy root cause

Không chỉ ghi:

```text
TEST FAIL
```

Phải trace:

```text
Test
 ↓
API / Controller
 ↓
Service
 ↓
Dependency
 ↓
Repository / DB
 ↓
Root Cause
 ↓
Minimal Fix
 ↓
Retest
```

Nếu dependency của Phase khác gây lỗi trực tiếp cho Phase 1:

```text
Fix dependency tối thiểu nếu cần
```

Không mở rộng thành implementation của Phase khác.

---

# 0.7 Không được đánh dấu PASS / IMPLEMENTED khi chưa kiểm chứng

Status phải dựa trên evidence thực tế.

Được dùng:

```text
IMPLEMENTED
INTEGRATION-READY
INTEGRATED
PARTIAL
MISSING
WRONG
OUT OF SCOPE
BLOCKED
```

Trong đó:

### IMPLEMENTED

Đã implement và đã có evidence kiểm chứng behavior thuộc Phase 1.

### INTEGRATION-READY

Implementation/contract của Phase 1 đã sẵn sàng nhưng integration bên ngoài chưa kết nối.

### INTEGRATED

Đã kết nối với implementation thật và đã kiểm chứng.

### PARTIAL

Có implementation nhưng còn thiếu một phần thuộc Phase 1.

### MISSING

Chưa có implementation cần thiết.

### WRONG

Có implementation nhưng behavior hiện tại sai requirement/policy.

### OUT OF SCOPE

Thực sự không thuộc Phase 1 và không ảnh hưởng Phase 1.

### BLOCKED

Chỉ dùng khi có technical blocker thật sự khiến Phase 1 không thể tiếp tục/kiểm chứng an toàn.

Không dùng:

```text
BLOCKED – teammate chưa code
BLOCKED – branch chưa merge
BLOCKED – backend chưa hoàn thành
```

---

# 0.8 Evidence Matrix bắt buộc

Trước khi kết luận Phase 1, mỗi capability quan trọng phải được kiểm tra theo:

```text
Requirement
↓
Source implementation
↓
Dependencies
↓
API / Route
↓
Security
↓
Database
↓
UI nếu có
↓
Runtime / Test
↓
Actual Status
```

Nếu một tầng chưa được kiểm chứng:

```text
không được tự động kết luận IMPLEMENTED
```

---

# 1. MỤC TIÊU

Phase 1 có 2 mục tiêu chính:

```text
A. GIỮ VỮNG MODERATOR FOUNDATION + SECURITY ĐÃ CÓ
                         +
B. DỌN CÁC CONFLICT / SOURCE-OF-TRUTH GAP ĐÃ ĐƯỢC AUDIT PHÁT HIỆN
```

Không xây lại những phần đã hoàn chỉnh.

Theo Audit Report hiện tại:

* Moderator Foundation đã EXISTS.
* Moderator Queue đã EXISTS.
* Case Detail đã EXISTS.
* Approve đã EXISTS.
* Escalate đã EXISTS.
* History đã EXISTS.
* Admin Escalation Queue đã EXISTS.
* Admin Suspend Shop đã EXISTS.
* Admin Suspend Product/Ban Vendor đã EXISTS.
* Moderator Dashboard đã EXISTS nhưng có một gap về hiệu năng `countActiveViolations()`.

**Các thông tin trên chỉ được xem là discovery hints.**

Phải kiểm tra lại source code và behavior thực tế trước khi kết luận trạng thái.

Vì vậy Phase 1 phải:

```text
DISCOVER
   ↓
ĐỌC SOURCE CODE THỰC TẾ
   ↓
TRACE DEPENDENCIES
   ↓
XÁC NHẬN ACTUAL BEHAVIOR
   ↓
ĐỐI CHIẾU REQUIREMENT / POLICY
   ↓
PHÂN LOẠI:
   IMPLEMENTED / WRONG / PARTIAL / MISSING /
   INTEGRATION-READY / OUT OF SCOPE / BLOCKED
   ↓
KHÔNG XÂY LẠI NẾU ĐANG ĐÚNG
   ↓
XÁC ĐỊNH GAP THỰC TẾ
   ↓
SỬA TỐI THIỂU
   ↓
REGRESSION
   ↓
SOURCE-OF-TRUTH CONSISTENCY
   ↓
BUILD
   ↓
MANUAL VERIFICATION KHI ĐƯỢC YÊU CẦU
   ↓
INTEGRATION-READY / INTEGRATED
```

Phase 1 không được biến thành việc xây lại toàn bộ Moderator system.

---

# 2. PHẠM VI PHASE 1

Phase 1 tập trung vào:

```text
Moderator Foundation
+
Security
+
Role Routing
+
Account Status Guard
+
KYC Source-of-Truth Reconciliation
+
AuditLog Source-of-Truth Reconciliation
+
Review Moderation Status Reconciliation
+
Reject → Violation consistency
+
ReportCase Status consistency
+
Regression Protection
+
Integration Contract
```

Các phần đã hoạt động đúng phải được **giữ nguyên**.

Không được tự ý xóa hoặc thay thế implementation đang hoạt động chỉ vì muốn viết lại theo cách khác.

Nếu implementation tồn tại nhưng behavior sai:

```text
WRONG
↓
identify root cause
↓
minimal targeted fix
↓
regression
```

---

# 3. CÁC GAP PHASE 1 PHẢI XỬ LÝ

Theo Audit Report, Phase 1 phải ưu tiên các gap sau.

**Audit Report chỉ dùng để định hướng discovery.**

Mỗi gap phải được source code thực tế xác nhận trước khi sửa.

---

## 3.1 KYC status không đồng bộ

Hiện tại có:

```text
User.kycStatus
Shop.kycStatus
KycProfile.status
```

Audit xác định đây là:

```text
HIGH RISK
```

Đặc biệt:

`VendorKycServiceImpl.submitKyc()` sync `User.kycStatus` nhưng `Shop.kycStatus` có thể bị stale.

Trong khi `ModeratorServiceImpl` có thể query theo `Shop.kycStatus`.

Phase 1 phải:

* xác định source of truth thực tế bằng source code;
* trace flow KYC từ controller/service/repository đến DB;
* xác định component nào đọc từng field;
* không tạo thêm KYC status mới;
* không tạo thêm KYC Entity nếu không cần;
* đồng bộ các trường denormalized hiện có nếu architecture hiện tại yêu cầu;
* đảm bảo KYC submit/update không để `Shop.kycStatus` và source status bị lệch trong flow đang sử dụng;
* giữ backward compatibility với dữ liệu hiện tại;
* kiểm chứng behavior sau khi sửa.

Không được tự ý đổi business rule KYC ngoài Policy.

Nếu chưa đủ evidence để xác định source of truth:

```text
UNKNOWN
```

Không được suy đoán.

---

# 4. AUDITLOG SOURCE OF TRUTH

Audit phát hiện hai hệ thống:

```text
AuditLog
    ↓
audit_logs

AuditLogEntry
    ↓
audit_log
```

Đây là:

```text
HIGH / MEDIUM SOURCE-OF-TRUTH CONFLICT
```

Phase 1 phải:

1. Inspect toàn bộ usage trực tiếp của:

   * `AuditLog`
   * `AuditLogEntry`
   * `AuditLogRepository`
   * `AdminAuditLogService`
   * `AuditLogService`
   * `AuditEventWriter`
   * các controller/service gọi audit.
2. Trace thực tế:

   ```text
   Event
   ↓
   Writer/Service
   ↓
   Repository
   ↓
   Collection
   ↓
   Reader/UI
   ```
3. Xác định implementation nào đang thực sự được sử dụng bởi từng flow.
4. Xác định source of truth logic hiện tại.
5. Không tạo AuditLog system thứ ba.
6. Không xóa dữ liệu MongoDB hiện có.
7. Không `deleteAll()` collection audit.
8. Không migration destructive.
9. Không tự ý đổi format dữ liệu cũ nếu không cần.

Mục tiêu:

```text
Audit Event
      ↓
Một source-of-truth logic rõ ràng
      ↓
Admin Audit UI đọc đúng dữ liệu
      ↓
Future Moderation/Compliance dùng cùng integration seam
```

Nếu cần giữ backward compatibility giữa hai collection, phải ghi rõ:

```text
Current Source:
...

Compatibility:
...

New Write Path:
...

Read Path:
...

Migration:
NONE / NON-DESTRUCTIVE
```

Không được xóa dữ liệu cũ để "thống nhất".

---

# 5. REVIEW MODERATION STATUS

Audit phát hiện:

```text
Review.moderationStatus
        VS
Review.pipelineModerationStatus
```

đang có nguy cơ trở thành hai nguồn trạng thái.

Phase 1 phải:

* inspect toàn bộ usage;
* trace nơi field được write;
* trace nơi field được read;
* xác định field nào đang được flow moderation hiện tại sử dụng;
* xác định field nào được UI/API sử dụng;
* không tạo thêm status field;
* không xóa field cũ nếu còn dữ liệu/usage;
* không rewrite Review system;
* tránh để hai field có thể biểu diễn hai trạng thái khác nhau mà không có quy tắc rõ ràng;
* nếu cần compatibility, giữ field cũ nhưng xác định rõ field chính và cách đồng bộ;
* retest flow sau khi sửa.

Nếu source hiện tại chưa đủ bằng chứng để quyết định source of truth:

```text
UNKNOWN
```

Không được tự suy đoán.

---

# 6. MODERATOR REJECT → VIOLATION

Audit xác định gap:

```text
ModeratorReportCaseServiceImpl.reject()
        ↓
REJECTED
        ↓
History
        ↓
Audit
```

nhưng:

```text
ViolationRepository
```

chưa được gọi.

Đây là gap cần kiểm chứng.

Phase 1 phải:

1. Trace `reject()` thực tế.
2. Xác định business rule trong Policy.
3. Xác định trường hợp nào phải tạo Violation.
4. Kiểm tra Violation hiện tại có được tạo ở service khác hay không.
5. Nếu thiếu và Policy yêu cầu thì sửa flow tối thiểu.

Flow mục tiêu khi applicable:

```text
ReportCase
    ↓
Moderator Decision
    ↓
REJECT / VIOLATION DECISION
    ↓
Violation nếu business rule yêu cầu
    ↓
History
    ↓
AuditLog
```

Không được tự tạo penalty mới.

Không tự đặt:

```text
3 violations = BAN
```

Không tự đặt severity hoặc punishment ngoài Policy.

Phải reuse:

```text
ViolationSeverity
ViolationAction
policyCode
violationCode
```

đang tồn tại.

Nếu Reject trong một trường hợp cụ thể theo Policy không phải lúc nào cũng tạo Violation, phải giữ đúng business rule của Policy.

---

# 7. REPORTCASE STATUS

Audit phát hiện:

```text
ReportCaseStatus
```

thiếu:

```text
DISMISSED
```

Phase 1 phải:

* kiểm tra Policy;
* kiểm tra toàn bộ usage;
* kiểm tra state transition hiện tại;
* xác định `DISMISSED` có thực sự thuộc scope hiện tại hay không;
* nếu Policy/task yêu cầu thì bổ sung theo architecture hiện tại;
* cập nhật toàn bộ usage liên quan;
* retest state transition;
* không tạo status duplicate.

Nếu chưa đủ bằng chứng:

```text
UNKNOWN / OUT OF SCOPE
```

Không tự thêm chỉ vì conceptual model có đề cập.

---

# 8. MODERATOR FOUNDATION – KHÔNG XÂY LẠI NẾU ĐANG ĐÚNG

Audit đã xác nhận Moderator Foundation tồn tại.

Do đó trước tiên phải inspect:

```text
ModeratorController
ModeratorService
Moderator Dashboard
Moderator Queue
Moderator Case Detail
Moderator History
Moderator Layout
Moderator Sidebar
Moderator Topbar
```

Nhưng **không được coi việc tồn tại của các file trên là bằng chứng hoàn thành**.

Kiểm tra:

```text
Route
Controller
Service
Repository
Security
DB
UI
Runtime
Test
```

Nếu behavior đúng:

```text
KEEP
```

Nếu có bug:

```text
WRONG
↓
fix minimally
↓
regression
```

Nếu thiếu:

```text
MISSING
↓
implement only if required by Phase 1
```

Không rewrite.

Không tạo:

```text
ModeratorControllerV2
ModeratorServiceV2
ModeratorDashboardV2
```

Không tạo duplicate route.

---

# 9. INDEPENDENT IMPLEMENTATION PRINCIPLE

“Tôi implement được phần của tôi bằng contract/interface hiện tại; backend của developer khác là integration point, không phải prerequisite để bắt đầu hoặc hoàn thành implementation.”

Phase 1 phải có khả năng triển khai độc lập.

Không chờ:

* developer khác;
* branch khác;
* commit khác;
* pull request khác;
* backend implementation khác;
* merge của developer khác.

Nếu capability Phase sau chưa tồn tại:

```text
Capability chưa có
        ↓
Xác định contract nếu Phase 1 cần
        ↓
Implement phần Phase 1 sở hữu
        ↓
Không fake production behavior
        ↓
INTEGRATION-READY
```

Tuy nhiên nếu capability/dependency chưa có **nhưng việc thiếu nó trực tiếp làm Phase 1 không hoạt động hoặc không thể kiểm chứng**, phải inspect dependency và fix tối thiểu nếu có thể.

Không được dùng "developer khác chưa làm" làm lý do bỏ qua lỗi kỹ thuật.

---

# 10. PHÂN BIỆT DEPENDENCY

## Technical Dependency

Ví dụ:

```text
Moderator Login
    ↓
Existing Authentication
```

Được phép reuse.

Nếu Authentication đang lỗi và làm Moderator Login fail:

```text
Inspect
↓
Root Cause
↓
Minimal Fix nếu cần
↓
Regression Admin/Vendor/Customer
```

Không rewrite authentication architecture.

## Integration Dependency

Ví dụ:

```text
Moderator Dashboard
    ↓
Future Moderation Metrics
```

Nếu backend chưa có:

```text
Current:
NOT AVAILABLE

Contract:
DEFINED nếu Phase 1 thực sự cần

Production Fake:
FORBIDDEN

Phase 1:
INTEGRATION-READY
```

Nếu metric hiện có bị lỗi và trực tiếp làm Moderator Dashboard Phase 1 fail:

```text
dependency bug
↓
inspect
↓
minimal fix nếu cần
```

Không dùng `OUT OF SCOPE` để bỏ qua.

## Blocking Dependency

Chỉ được đánh dấu `BLOCKED` nếu:

```text
Baseline hiện tại không build được
+
không thể triển khai Phase 1 độc lập
+
không có workaround an toàn
```

Không được dùng:

```text
Developer chưa code
Branch chưa merge
Backend chưa hoàn thành
```

làm lý do BLOCKED.

---

# 11. SOURCE OF TRUTH

Thứ tự ưu tiên:

```text
1. Phase 1 Scope
2. Business Policy
3. Source Code hiện tại
4. Audit Report
5. SHOPEE_MARKETPLACE_MODEL.md
```

Trong đó:

### Phase 1

Quyết định Phase này phải làm gì.

### Business Policy

Quyết định business rule.

### Source Code

Cho biết implementation hiện tại và behavior thực tế.

### Audit Report

Cho biết gap đã được phát hiện để định hướng kiểm tra.

### SHOPEE_MARKETPLACE_MODEL

Là conceptual/business baseline.

Không được dùng Marketplace Model để tự ý tạo Entity/schema/feature mới.

---

# 12. KHÔNG ĐƯỢC XÓA DỮ LIỆU MONGODB

Đây là quy tắc **BẮT BUỘC TOÀN PHASE**.

Trong mọi trường hợp:

```text
KHÔNG DELETE
KHÔNG deleteMany
KHÔNG deleteAll
KHÔNG DROP COLLECTION
KHÔNG DROP DATABASE
KHÔNG RESET DATABASE
KHÔNG RESET TEST DATABASE ĐANG DÙNG
KHÔNG TRUNCATE
```

Không được chạy code/test có hành vi:

```java
repository.deleteAll();
```

để dọn MongoDB.

Không được dùng:

```text
cleanup()
seed()
reset()
migration
```

theo cách làm mất dữ liệu hiện tại.

Không được xóa dữ liệu để giải quyết conflict schema.

Không được xóa dữ liệu để test security.

Không được xóa dữ liệu để test KYC.

Không được xóa dữ liệu để test AuditLog.

Không được xóa dữ liệu để test Violation.

---

# 13. DỮ LIỆU MẪU

Nếu manual test thực sự cần dữ liệu:

```text
Được phép chèn tối đa 1–2 bản ghi mẫu.
```

Ví dụ:

```text
1 Moderator test account
1 ReportCase/Violation test record
```

Nhưng:

* chỉ insert nếu thực sự cần;
* không thay đổi dữ liệu business hiện tại;
* không overwrite record hiện có;
* không dùng `_id` trùng;
* phải đánh dấu rõ là test data;
* không dùng dữ liệu mẫu để giả lập production metrics;
* không tạo hàng loạt dữ liệu.

Sau khi test:

```text
KHÔNG TỰ ĐỘNG XÓA
```

Nếu người dùng muốn dọn test data sau này, đó là một thao tác riêng và phải được người dùng yêu cầu rõ ràng.

---

# 14. KHÔNG FAKE PRODUCTION DATA

Không tạo dashboard giả:

```text
Pending Products = 15
Reports = 8
Violations = 3
Escalations = 2
```

Nếu dữ liệu thật chưa có:

```text
--
Chưa có dữ liệu
Not available
```

Test double chỉ được dùng trong test environment và không được ghi thành production data.

---

# 15. GIT SAFETY

Trước khi code bắt buộc chạy:

```bash
git status
git branch
git log --oneline --decorate -10
```

Ghi nhận:

```text
Current branch:
Current HEAD:
Working tree:
Uncommitted changes:
Merge state:
Stash state:
```

Bắt buộc:

```text
KHÔNG reset
KHÔNG stash
KHÔNG checkout sang branch khác
KHÔNG overwrite uncommitted changes
KHÔNG merge branch khác
KHÔNG commit
KHÔNG push
```

Đặc biệt:

**Không tự ý checkout sang branch khác để test.**

Không tự ý xử lý stash.

Không tự ý tạo commit sau khi hoàn thành Phase.

---

# 16. DISCOVERY – KIỂM TRA SOURCE

Trước khi sửa phải tìm implementation hiện tại của:

```text
UserRole
User
UserStatus
AccountStatus
SecurityConfig
Authentication
Authorization
AuthController
AuthService

ModeratorController
ModeratorService
Moderator Dashboard
Moderator Queue
Moderator Case
Moderator History

ReportCase
ReportCaseStatus
ReportCaseService
ModeratorReportCaseService

Violation
ViolationRepository
ViolationService

KycProfile
KycStatus
KycService
VendorKycService

Review
ReviewModerationStatus
ModerationStatus

AuditLog
AuditLogEntry
AuditLogService
AdminAuditLogService
AuditEventWriter
```

Sau đó trace dependency thực tế.

Mục tiêu:

```text
EXISTS + BEHAVIOR ĐÚNG
→ REUSE

EXISTS + BEHAVIOR SAI
→ WRONG → FIX MINIMALLY

EXISTS + CHƯA ĐỦ
→ PARTIAL → FIX MINIMALLY

MISSING + REQUIRED
→ IMPLEMENT

MISSING + FUTURE + KHÔNG ẢNH HƯỞNG PHASE 1
→ OUT OF SCOPE

DEPENDENCY MISSING/BUG + ẢNH HƯỞNG PHASE 1
→ INSPECT / FIX MINIMALLY NẾU CÓ THỂ
```

---

# 17. MODERATOR ROLE

Nếu project đã có:

```text
MODERATOR
```

thì reuse.

Không tạo:

```text
MOD
STAFF
CONTENT_MODERATOR
MODERATOR_ADMIN
```

Role hiện tại:

```text
CUSTOMER
VENDOR
MODERATOR
ADMIN
```

Phase 1 không đổi role architecture nếu không có lỗi trực tiếp.

---

# 18. MODERATOR LOGIN / ROUTING

Reuse authentication hiện tại.

Expected:

```text
MODERATOR
   ↓
Existing Login
   ↓
Authentication
   ↓
ROLE = MODERATOR
   ↓
/moderator/dashboard
```

Không tạo authentication riêng.

Không tạo session/JWT architecture thứ hai.

Phải verify:

```text
Login
→ Authentication
→ Authorization
→ Redirect
→ Dashboard
```

Regression:

```text
ADMIN     → Admin area
MODERATOR → Moderator area
VENDOR    → Vendor area
CUSTOMER  → Customer area
```

---

# 19. MODERATOR SECURITY CONTRACT

Phải xác nhận:

```text
MODERATOR
    ↓
/moderator/**
    ↓
ALLOW
```

và:

```text
MODERATOR
    ↓
/admin/**
    ↓
DENY
```

Các role khác:

```text
CUSTOMER → /moderator/** → DENY
VENDOR   → /moderator/** → DENY
ANON     → /moderator/** → DENY
```

Admin permission phải giữ nguyên architecture hiện tại.

Không tự mở:

```text
ADMIN → /moderator/**
```

nếu source hiện tại không có.

Phải kiểm tra cả:

```text
URL-level security
+
Method-level security
+
Actual runtime authorization
```

nếu các lớp này tồn tại trong architecture.

---

# 20. METHOD-LEVEL SECURITY

Nếu source hiện tại dùng:

```java
@PreAuthorize
```

thì reuse.

Không tạo security mechanism thứ hai.

Ví dụ nếu phù hợp với architecture:

```java
@PreAuthorize("hasRole('MODERATOR')")
```

Security phải được enforce ở backend.

Không chỉ ẩn menu trên UI.

Nếu UI ẩn đúng nhưng backend vẫn cho phép request:

```text
WRONG
```

---

# 21. ACCOUNT STATUS GUARD

Reuse status hiện tại:

```text
UserStatus
hoặc
AccountStatus
```

Không tạo enum duplicate.

Expected:

```text
ACTIVE MODERATOR
        ↓
LOGIN
        ↓
ALLOW
```

và:

```text
LOCKED MODERATOR
        ↓
LOGIN
        ↓
DENY
```

Nếu account bị lock sau khi login:

```text
Protected Request
        ↓
Account Status Check
        ↓
DENY nếu architecture hiện tại hỗ trợ
```

Nếu source hiện tại chưa có realtime session invalidation:

```text
INTEGRATION-LIMITATION
```

Không rewrite toàn bộ authentication architecture.

Nhưng phải kiểm tra actual behavior và báo cáo chính xác.

---

# 22. PUBLIC REGISTER SECURITY

Public registration không được tự tạo Moderator.

Test:

```text
POST /register
role=MODERATOR
```

Expected:

```text
Không tạo Public Moderator
```

Không thay đổi registration flow hiện tại ngoài phần cần thiết để bảo vệ role.

Phải verify behavior thực tế nếu test được.

---

# 23. MODERATOR UI

Moderator UI đã tồn tại theo Audit.

Do đó:

```text
DISCOVER
↓
VERIFY
↓
KEEP nếu đúng
```

Chỉ sửa nếu có lỗi trực tiếp.

Reuse Admin UI baseline hiện tại:

* Header
* Sidebar
* Card
* Button
* Table
* Badge
* Modal
* Typography
* Spacing
* Responsive behavior

Không:

```text
rewrite frontend framework
rewrite CSS toàn project
xóa Admin UI
tạo design system mới
```

---

# 24. MODERATOR MENU

Giữ các menu đã có.

Có thể gồm:

```text
Moderator
├── Dashboard
├── Moderation Queue
├── Report Cases
├── Violations
└── History
```

Không báo cáo menu là business feature nếu backend chưa thực sự hỗ trợ.

Không tạo business backend chỉ để menu nhìn đầy đủ.

---

# 25. MODERATOR DASHBOARD

Dashboard hiện tại được Audit xác nhận tồn tại.

Không xây lại nếu behavior đúng.

Phải verify:

```text
Route
Controller
Service
Repository
Metrics source
UI
Security
Runtime
```

Đặc biệt Audit phát hiện:

```text
countActiveViolations()
```

đang dùng:

```text
findAll()
+
stream filter
```

Phase 1 có thể xác định và chuẩn bị integration contract, nhưng **không được mở rộng scope thành một performance refactor toàn hệ thống**.

Nếu sửa trong Phase 1:

```text
countActiveViolations()
```

nên chuyển sang query/repository method phù hợp nếu architecture hiện tại cho phép.

Không thay đổi business meaning của metric.

Sau khi sửa phải regression dashboard và các flow dùng cùng repository/service.

---

# 26. KYC SOURCE-OF-TRUTH CONTRACT

Phase 1 phải xác định bằng evidence:

```text
KYC Source of Truth:
<existing implementation được chứng minh từ source>
```

Phải ghi rõ:

```text
Write path:
...

Read path:
...

Persistence:
...

Denormalized fields:
...

Sync rule:
...
```

Các field khác nếu là denormalized:

```text
User.kycStatus
Shop.kycStatus
```

phải có quy tắc đồng bộ rõ ràng.

Không tạo thêm:

```text
KycStatusV2
KycProfileV2
VendorKycStatus
```

Nếu cần sync:

```text
KYC update
   ↓
Source of Truth
   ↓
Update existing denormalized state
```

Không xóa dữ liệu cũ.

---

# 27. AUDITLOG CONTRACT

Phase 1 phải xác định:

```text
Audit Event
    ↓
AuditEventWriter / existing audit seam
    ↓
Current Audit implementation
```

Mục tiêu:

```text
Không có logic audit mới thứ ba.
```

Nếu có hai implementation cũ:

```text
AuditLog
AuditLogEntry
```

phải ghi rõ:

```text
Source of Truth:
...

Legacy / Compatibility:
...

Read:
...

Write:
...

Future Integration:
...
```

Không destructive migration.

Không delete collection.

Phải kiểm chứng ít nhất các flow Phase 1 có liên quan đến AuditLog.

---

# 28. REVIEW STATUS CONTRACT

Phải xác định:

```text
Review moderation status source:
...
```

Nếu có:

```text
Review.moderationStatus
Review.pipelineModerationStatus
```

phải trace:

```text
Writer
Reader
API
UI
Moderation flow
```

và tránh để business logic mới đọc hai field tùy ý.

Không xóa dữ liệu.

Không tạo status thứ ba.

---

# 29. REPORTCASE → VIOLATION CONTRACT

Contract:

```text
ReportCase
    ↓
Moderator Decision
    ↓
Violation Decision
    ↓
Violation
    ↓
History
    ↓
Audit
```

Nếu business rule yêu cầu Violation.

Contract phải reuse:

```text
Violation
ViolationSeverity
ViolationAction
policyCode
violationCode
evidence
resourceType
resourceId
```

Không tự tạo penalty rules.

Phải kiểm tra cả:

```text
Database persistence
+
History
+
Audit
+
API response
```

nếu các thành phần này thuộc flow hiện tại.

---

# 30. KHÔNG LẤN SANG PHASE 2 / 3

Phase 1 không triển khai mới:

```text
Auto Moderation Engine
Blacklist Engine
Duplicate Product Engine
Duplicate Image Engine
Abnormal Price Engine
Image Content Engine
Product Moderation Business Flow mới
Complaint System
Complaint Scheduler
KYC Provider mới
Fraud Engine
Review Moderation Engine mới
```

Không mở rộng Product Moderation chỉ vì Moderator Queue đã tồn tại.

Không xây Complaint vì Phase 3 mới xử lý.

**Tuy nhiên:**

Nếu một component của các hệ thống trên đã tồn tại và là dependency trực tiếp gây lỗi Phase 1:

```text
inspect dependency
↓
fix only the minimum required
↓
do not implement the future feature
```

---

# 31. PHASE 1 KHÔNG ĐƯỢC XÓA CHỨC NĂNG ĐÃ HOÀN CHỈNH

Theo Audit:

```text
Admin:
41/41 DONE

Moderator:
Foundation EXISTS
Queue EXISTS
Case Detail EXISTS
Approve EXISTS
Escalate EXISTS
History EXISTS
Admin Escalation EXISTS
Suspend Shop EXISTS
Suspend Product/Ban Vendor EXISTS
```

Các thông tin trên phải được verify lại.

Không được:

```text
rewrite
replace
remove
disable
```

các chức năng đang hoạt động đúng nếu không có lỗi trực tiếp được chứng minh.

Nếu cần sửa một file dùng chung:

```text
Preserve existing behavior
+
Fix targeted issue
+
Regression check
```

Nếu chức năng được Audit đánh dấu DONE nhưng source hiện tại chứng minh behavior sai:

```text
WRONG
```

không phải:

```text
DONE
```

---

# 32. DATA SAFETY TESTING

Mọi test Phase 1 phải tuân thủ:

```text
TEST
 ↓
READ existing data
 ↓
ASSERT behavior
```

Không:

```text
TEST
 ↓
DELETE ALL
 ↓
SEED
```

Không chạy test có:

```java
deleteAll()
deleteMany()
drop()
dropDatabase()
```

trên MongoDB hiện tại.

Nếu test cần dữ liệu:

```text
INSERT 1–2 TEST RECORDS
```

và giữ nguyên các dữ liệu khác.

Không tự động cleanup bằng delete.

---

# 33. BUILD / TEST RULE

Theo workflow hiện tại của project:

**Không tự động chạy Maven test.**

Khi cần build:

```bash
mvn clean package -DskipTests
```

Không tự chạy:

```text
mvn test
mvn verify
mvn install
```

Không tự start Spring Boot nếu người dùng chưa yêu cầu.

Không tự kết nối MongoDB để seed/delete/reset dữ liệu.

Nếu người dùng yêu cầu chạy website để manual test thì mới start application sau khi build đạt.

**Lưu ý:** Nếu build `-DskipTests` fail do compile error, phải inspect và fix lỗi nếu nó trực tiếp ảnh hưởng Phase 1. Không được đánh dấu `OUT OF SCOPE` chỉ vì source lỗi nằm ở module/component khác.

---

# 34. MANUAL SECURITY TEST

Sau khi build, nếu người dùng yêu cầu manual website testing, kiểm tra:

```text
Moderator Login
Moderator Dashboard
Moderator → /moderator/**
Moderator → /admin/**
Customer → /moderator/**
Vendor → /moderator/**
Anonymous → /moderator/**
Locked Moderator
Locked Admin
Public Register → MODERATOR
Admin Login
Admin Dashboard
```

Không đánh dấu PASS nếu chưa thực sự kiểm tra.

Nếu chưa chạy:

```text
NOT RUN
```

Không đổi thành PASS dựa trên source code.

---

# 35. SECURITY TEST MATRIX

| Test                                 | Expected                           |
| ------------------------------------ | ---------------------------------- |
| ADMIN →`/admin/**`                | Allow theo architecture hiện tại |
| MODERATOR →`/moderator/**`        | Allow                              |
| MODERATOR →`/admin/**`            | Deny                               |
| CUSTOMER →`/moderator/**`         | Deny                               |
| VENDOR →`/moderator/**`           | Deny                               |
| Anonymous →`/moderator/**`        | Deny/Login Required                |
| Locked Moderator                     | Deny                               |
| Locked Admin                         | Deny theo architecture hiện tại  |
| Public Register →`role=MODERATOR` | Deny                               |

Phân biệt:

```text
Source verification
Runtime verification
Manual verification
```

Không được đánh đồng ba loại evidence.

---

# 36. ADMIN REGRESSION

Sau mọi thay đổi Security/Authentication/Common Service:

kiểm tra các chức năng Admin đã tồn tại:

```text
Admin Login
Admin Dashboard
User Management
Shop Management
Category
WEB Voucher
Banner
Admin Escalation
Admin Violation
Admin Audit
```

Nếu test chưa được chạy:

```text
NOT RUN
```

Không đánh dấu PASS.

Không sửa lại business logic Admin nếu không liên quan trực tiếp.

Mục tiêu:

```text
Phase 1 change
      ↓
Moderator works
      +
Admin vẫn works
```

Nếu thay đổi dependency chung làm Admin fail:

```text
dependency regression
↓
fix minimally
↓
retest
```

---

# 37. FILE CHANGE SAFETY

Trước khi tạo file mới:

```text
Search existing implementation
        ↓
Exists?
 ├── YES → inspect behavior
 │          ├── correct → REUSE
 │          └── wrong → MODIFY MINIMALLY
 │
 └── NO  → CREATE ONLY IF REQUIRED
```

Không tạo duplicate:

```text
Entity
Service
Repository
Enum
Controller
DTO
Security Config
Audit System
KYC System
```

---

# 38. STATUS DEFINITIONS

## IMPLEMENTED

Phần Phase 1 đã hoàn thành và có thể kiểm chứng độc lập bằng evidence phù hợp.

## INTEGRATION-READY

Contract/seam đã sẵn sàng, implementation bên ngoài chưa kết nối.

## INTEGRATED

Implementation thật đã kết nối và kiểm chứng.

## PARTIAL

Còn thiếu một phần thuộc Phase 1.

## MISSING

Chưa có implementation.

## WRONG

Có implementation nhưng behavior sai yêu cầu.

## OUT OF SCOPE

Không thuộc Phase 1 **và không phải dependency ảnh hưởng Phase 1**.

## BLOCKED

Chỉ dùng khi có technical blocker thật sự.

Không dùng:

```text
BLOCKED – teammate chưa code
BLOCKED – branch chưa merge
BLOCKED – backend chưa có
```

---

# 39. INTEGRATION STATUS

Mỗi integration point phải báo cáo:

```text
Integration Point:
<name>

Contract:
<interface / DTO / route / expected behavior>

Current Implementation:
<AVAILABLE / NOT AVAILABLE>

Dependency:
<dependency nếu có>

Phase 1 Implementation:
<what has been completed>

Integration Seam:
<future connection point>

Production Fake:
FORBIDDEN

Integration Status:
<IMPLEMENTED / INTEGRATION-READY / INTEGRATED /
 PARTIAL / MISSING / OUT OF SCOPE>
```

Nếu dependency đang lỗi nhưng trực tiếp ảnh hưởng Phase 1:

```text
Dependency Status:
WRONG / PARTIAL / MISSING

Impact:
<exact Phase 1 impact>

Action:
<minimal fix / integration-ready / blocked>
```

---

# 40. BUILD

Chỉ build bằng:

```bash
mvn clean package -DskipTests
```

Báo cáo:

```text
BUILD:
PASS / FAIL
```

Nếu FAIL:

```text
Error:
Root cause:
Affected module:
Dependency:
Phase 1 caused? YES / NO / UNKNOWN
Fix applied:
Retest:
```

Không tự sửa ngoài scope để ép build PASS.

Nhưng nếu lỗi nằm ngoài Feature Scope nhưng là **dependency trực tiếp khiến Phase 1 không build/không hoạt động**, được phép sửa tối thiểu.

---

# 41. FINAL REPORT

Sau khi hoàn thành phải báo cáo rõ ràng.

## 41.1 Phase Status

```text
PHASE 1 – MODERATOR FOUNDATION + SECURITY +
SOURCE OF TRUTH RECONCILIATION

Implementation:
IMPLEMENTED / PARTIAL / MISSING

Integration:
INTEGRATION-READY / INTEGRATED
```

Chỉ chọn `IMPLEMENTED` khi các acceptance criteria thuộc Phase 1 đã được kiểm chứng đủ.

---

## 41.2 Completed

Báo cáo chính xác từng mục:

```text
Moderator Foundation:
<status + evidence>

Moderator Security:
<status + evidence>

Role Routing:
<status + evidence>

Account Status Guard:
<status + evidence>

KYC Source-of-Truth:
<status + evidence>

AuditLog Source-of-Truth:
<status + evidence>

Review Moderation Status:
<status + evidence>

ReportCase → Violation:
<status + evidence>

ReportCaseStatus:
<status + evidence>

Admin Regression:
<status + evidence>
```

Không báo `DONE` nếu chưa thực sự kiểm chứng.

---

# 41.3 DEPENDENCY FIXES

Nếu đã sửa dependency ngoài Feature Scope nhưng trực tiếp ảnh hưởng Phase 1:

```text
Dependency:
<name>

Why it affected Phase 1:
<reason>

Root Cause:
<root cause>

Minimal Fix:
<what changed>

Why this is allowed:
<direct Phase 1 dependency>

Regression:
<result>
```

Không được biến phần này thành implementation của Phase sau.

---

# 42. FILES CREATED

Liệt kê chính xác:

```text
Files Created:
- path/to/file
```

Nếu không tạo:

```text
Files Created:
NONE
```

---

# 43. FILES MODIFIED

Liệt kê chính xác:

```text
Files Modified:
- path/to/file
- path/to/file
```

Mỗi file quan trọng nên ghi ngắn gọn:

```text
path/to/file
Reason:
<why modified>
```

Không ghi chung chung:

```text
Updated Moderator files
```

---

# 44. FILES DELETED

Bắt buộc:

```text
Files Deleted:
NONE
```

Phase 1 **không được xóa file chỉ để refactor cho đẹp**.

Nếu source bắt buộc phải xóa file do duplicate implementation:

```text
KHÔNG TỰ XÓA
```

phải báo cáo trước khi thực hiện vì quy tắc Phase 1 cấm xóa implementation chỉ để refactor và phải bảo toàn existing architecture.

---

# 45. MONGODB DATA SAFETY REPORT

Bắt buộc báo cáo:

```text
MongoDB existing data:
PRESERVED

deleteAll():
NOT USED

deleteMany():
NOT USED

dropCollection():
NOT USED

dropDatabase():
NOT USED

Database reset:
NOT USED

Existing records modified:
<YES/NO + exact reason nếu YES>

Sample records inserted:
<NONE / tối đa 1–2 record>

Sample record purpose:
<...>
```

Nếu có insert sample:

```text
Sample data:
1. <collection> – <purpose>
2. <collection> – <purpose>
```

Không tự động xóa sample data sau test.

---

# 46. MANUAL TEST REPORT

Chỉ báo PASS nếu đã thực sự kiểm tra.

```text
Moderator Login:
PASS / FAIL / NOT RUN

Moderator Dashboard:
PASS / FAIL / NOT RUN

Moderator → /moderator/**:
PASS / FAIL / NOT RUN

Moderator → /admin/**:
PASS / FAIL / NOT RUN

Customer → /moderator/**:
PASS / FAIL / NOT RUN

Vendor → /moderator/**:
PASS / FAIL / NOT RUN

Anonymous → /moderator/**:
PASS / FAIL / NOT RUN

Locked Moderator:
PASS / FAIL / NOT RUN

Locked Admin:
PASS / FAIL / NOT RUN

Public Register → MODERATOR:
PASS / FAIL / NOT RUN

Admin Regression:
PASS / FAIL / NOT RUN

KYC consistency:
PASS / FAIL / NOT RUN

AuditLog consistency:
PASS / FAIL / NOT RUN

Review status consistency:
PASS / FAIL / NOT RUN

Reject → Violation:
PASS / FAIL / NOT RUN
```

---

# 47. KNOWN ISSUES

Mỗi issue phải ghi:

```text
Issue:
Current behavior:
Expected:
Affected file/module:
Dependency:
Root cause:
Severity:
Phase 1 impact:
Action:
Integration status:
```

Không ghi:

```text
Teammate chưa làm
```

Mà ghi:

```text
Auto Moderation backend:
NOT AVAILABLE

Impact:
NONE FOR PHASE 1

Status:
OUT OF SCOPE / INTEGRATION-READY
```

Nếu dependency trực tiếp ảnh hưởng Phase 1:

```text
Impact:
AFFECTS PHASE 1

Status:
WRONG / PARTIAL / BLOCKED
```

---

# 48. PHASE 2 HANDOFF CONTRACT

Phase 1 bàn giao:

```text
Existing Authentication
        ↓
Moderator Account
        ↓
Role Check
        ↓
Account Status Check
        ↓
/moderator/**
        ↓
Moderator Foundation
        ↓
Moderator Queue
        ↓
ReportCase Integration Seam
        ↓
Violation Integration Seam
        ↓
Audit Integration Seam
```

Phase 2 có thể tiếp tục:

```text
Product Moderation
Auto Moderation
Report
ReportCase
Violation
Moderation Result
Vendor Product Moderation UI
```

mà không cần dựng lại Moderator Foundation.

---

# 49. PHASE 1 ACCEPTANCE CRITERIA

Phase 1 được coi là hoàn thành khi:

### Moderator

* Moderator role hiện tại được giữ nguyên.
* Moderator login sử dụng authentication hiện tại.
* Moderator redirect đúng `/moderator/dashboard`.
* Moderator Foundation hiện tại không bị phá.
* Moderator UI hiện tại không bị rewrite không cần thiết.
* Behavior của các capability thuộc Phase 1 đã được source/runtime/test kiểm chứng ở mức phù hợp.

### Security

* `/moderator/**` được bảo vệ.
* MODERATOR → Moderator area: Allow.
* MODERATOR → Admin area: Deny.
* CUSTOMER → Moderator: Deny.
* VENDOR → Moderator: Deny.
* Anonymous → Moderator: Deny/Login Required.
* Locked Moderator: Deny theo architecture.
* Public registration không tạo Moderator.

### Source of Truth

* KYC status conflict được xác định và xử lý theo source hiện tại.
* AuditLog dual-system conflict được xác định và xử lý an toàn.
* Review moderation status conflict được xác định và xử lý.
* Moderator Reject → Violation được xử lý đúng Policy nếu applicable.
* `ReportCaseStatus` được xử lý đúng scope/Policy.

### Dependency

* Dependency trực tiếp gây lỗi Phase 1 phải được inspect.
* Nếu có thể fix an toàn, phải fix tối thiểu.
* Không dùng `OUT OF SCOPE` để bỏ qua dependency gây Phase 1 failure.
* Không triển khai feature tương lai chỉ vì phải sửa dependency.

### Data Safety

* Không xóa dữ liệu MongoDB.
* Không reset database.
* Không drop collection.
* Không `deleteAll()`.
* Không `deleteMany()`.
* Không destructive migration.
* Nếu cần test data: tối đa 1–2 record mẫu.

### Regression

* Admin vẫn hoạt động.
* Vendor không bị ảnh hưởng.
* Customer không bị ảnh hưởng.
* Existing Moderator functionality không bị mất.
* Shared dependency changes không tạo regression.

### Git

* Không checkout branch khác.
* Không reset.
* Không stash.
* Không merge.
* Không commit.
* Không push.

### Build

```bash
mvn clean package -DskipTests
```

phải được kiểm tra và báo cáo chính xác.

---

# 50. FINAL ARCHITECTURE

Sau Phase 1:

```text
                    EXISTING AUTH
                         │
                         ▼
                 MODERATOR ACCOUNT
                         │
                         ▼
                       LOGIN
                         │
                         ▼
                    ROLE CHECK
                         │
                         ▼
                ACCOUNT STATUS CHECK
                         │
                         ▼
                /moderator/dashboard
                         │
              ┌──────────┴──────────┐
              ▼                     ▼
       Moderator Foundation      Security
              │                     │
              ▼                     ▼
        Existing Features       /moderator/**
                                    │
                    ┌───────────────┼───────────────┐
                    ▼               ▼               ▼
                 MODERATOR       CUSTOMER         VENDOR
                    ALLOW          DENY             DENY

                         │
                         ▼
                SOURCE OF TRUTH
                         │
          ┌──────────────┼──────────────┐
          ▼              ▼              ▼
         KYC          AuditLog        Review
          │              │              │
          └──────────────┼──────────────┘
                         ▼
              ReportCase → Violation
                         │
                         ▼
                  Phase 2 Integration
```

---

# 51. FINAL STATUS

Không được mặc định:

```text
IMPLEMENTED + INTEGRATION-READY
```

chỉ vì Audit Report ghi EXISTS/DONE.

Final status phải được xác định sau khi thực hiện:

```text
SOURCE DISCOVERY
        ↓
DEPENDENCY TRACE
        ↓
IMPLEMENTATION CHECK
        ↓
SECURITY CHECK
        ↓
DB CHECK
        ↓
UI CHECK
        ↓
RUNTIME / TEST CHECK
        ↓
BUILD
        ↓
FINAL STATUS
```

Expected final state nếu toàn bộ acceptance criteria thực sự đạt:

```text
PHASE 1
MODERATOR FOUNDATION + SECURITY
+
SOURCE OF TRUTH RECONCILIATION

Implementation:
IMPLEMENTED

Integration:
INTEGRATION-READY / INTEGRATED
```

Nếu chưa đạt, phải dùng status thực tế:

```text
PARTIAL
MISSING
WRONG
BLOCKED
```

Không ép kết quả thành `IMPLEMENTED`.

---

# 52. QUY TẮC CUỐI CÙNG – BẮT BUỘC

Trong toàn bộ Phase 1:

```text
# PHASE 1 — MODERATOR FOUNDATION + SECURITY

## 0. MỤC TIÊU PHASE

Phase 1 có nhiệm vụ hoàn thiện **Moderator Foundation** trên source code hiện tại.

Phase 1 chỉ tập trung vào:

```text
MODERATOR
    ↓
ACCOUNT
    ↓
LOGIN
    ↓
REDIRECT
    ↓
/moderator/**
    ↓
MODERATOR DASHBOARD
    ↓
SECURITY BOUNDARY
```

Phase 1 **không triển khai business moderation**.

Không triển khai trong Phase 1:

* Product Moderation
* Review Moderation
* ReportCase
* Violation
* KYC
* AuditLog
* Complaint
* Return
* Refund
* Escalation
* Scheduler
* Finance
* Escrow
* Settlement

Các domain trên sẽ được xử lý ở các Phase/Gate sau.

---

# 1. NGUYÊN TẮC BẮT BUỘC

Trong toàn bộ Phase 1:

```text
KHÔNG XÓA DỮ LIỆU MONGODB

KHÔNG RESET DATABASE

KHÔNG DROP COLLECTION

KHÔNG deleteAll()

KHÔNG deleteMany({})

KHÔNG dropDatabase()

KHÔNG XÓA TEST DATA ĐÃ CÓ

KHÔNG FAKE PRODUCTION DATA

KHÔNG FAKE BUSINESS LOGIC

KHÔNG CHECKOUT BRANCH

KHÔNG STASH

KHÔNG MERGE

KHÔNG COMMIT

KHÔNG PUSH

KHÔNG RESET CODE

KHÔNG CHỜ DEVELOPER KHÁC
```

Phase 1 phải được triển khai độc lập.

Nếu gặp dependency chưa tồn tại:

```text
Inspect
   ↓
Determine dependency
   ↓
Implement phần thuộc Phase 1
   ↓
Define integration boundary nếu cần
   ↓
Test
   ↓
Build
```

Không được báo:

```text
BLOCKED – teammate chưa code
```

nếu phần Moderator Foundation vẫn có thể hoàn thành độc lập.

---

# 2. SOURCE OF TRUTH

Thứ tự ưu tiên khi quyết định implementation:

```text
CURRENT SOURCE CODE
        ↓
GATE 0 — SOURCE AUDIT + SCOPE LOCK
        ↓
PHASE 1 SPECIFICATION
        ↓
MARKETPLACE POLICY
        ↓
SHOPEE_MARKETPLACE_MODEL
```

Source code hiện tại là căn cứ kỹ thuật chính.

Không được giả định rằng kế hoạch cũ đã đúng.

Nếu source và kế hoạch khác nhau:

```text
SOURCE hiện tại
+
SPECIFICATION
+
POLICY
```

phải được đối chiếu trước khi sửa.

Nếu chưa xác minh được:

```text
NOT VERIFIED
```

Không tự kết luận.

---

# 3. KẾ THỪA TỪ GATE 0

Phase 1 phải kế thừa kết quả Gate 0:

```text
CUSTOMER
VENDOR
MODERATOR
ADMIN
```

và nguyên tắc:

```text
MODERATOR ≠ ADMIN
```

Ranh giới:

```text
MODERATOR
    ↓
Moderator Foundation
Moderator Area
Moderator Security
```

trong khi:

```text
ADMIN
    ↓
Admin Area
Platform Management
Admin Enforcement
```

Phase 1 không được biến Moderator thành một phiên bản Admin.

---

# 4. SOURCE HIỆN TẠI ĐÃ XÁC NHẬN

Theo source audit:

### Đã tồn tại

```text
UserRole.MODERATOR
```

Security đã có:

```text
/moderator/**
    ↓
ROLE_MODERATOR
```

Authentication hiện tại đã tồn tại.

Admin area đã tồn tại.

Customer/Vendor flow đã tồn tại.

### Chưa tồn tại đầy đủ

```text
Moderator Controller
Moderator Service
Moderator Dashboard
Moderator Template
Moderator Layout
Moderator-specific redirect
```

Do đó Phase 1 phải hoàn thiện phần còn thiếu.

Không xây lại authentication từ đầu.

Không tạo role mới.

Không tạo login system mới.

---

# 5. KHÔNG TẠO ROLE MỚI

Role chuẩn:

```text
CUSTOMER
VENDOR
MODERATOR
ADMIN
```

Không tạo:

```text
STAFF
MOD
CONTENT_MODERATOR
MODERATOR_ADMIN
MODERATOR_V2
```

nếu source không yêu cầu.

Nếu `MODERATOR` đã tồn tại:

```text
REUSE UserRole.MODERATOR
```

Không tạo duplicate enum.

---

# 6. MODERATOR ACCOUNT PROVISIONING

## 6.1 Mục tiêu

Phải có một tài khoản có:

```text
role = MODERATOR
```

để kiểm tra login và security.

## 6.2 Public Registration

Public registration **không được cho phép client tự tạo Moderator**.

Không được tin dữ liệu:

```text
role=MODERATOR
```

từ frontend.

Flow đúng:

```text
Public Register
      ↓
Server xác định role hợp lệ
      ↓
Customer/Vendor theo business flow hiện tại
```

Không được:

```text
POST register
role = MODERATOR
      ↓
Create Moderator
```

## 6.3 Provision Moderator Test Account

Nếu source hiện tại chưa có UI/API quản lý Moderator:

Không cần xây Admin Moderator Management trong Phase 1.

Có thể dùng cơ chế provision phù hợp với architecture hiện tại để tạo account test.

Nếu cần dữ liệu test:

```text
[TEST-PHASE-1] Moderator
```

Dữ liệu test phải:

* rõ ràng;
* tối thiểu;
* không ảnh hưởng dữ liệu production;
* không xóa sau test.

Nếu không cần insert dữ liệu:

```text
Test records inserted: 0
```

---

# 7. MODERATOR LOGIN

Không tạo hệ thống login riêng.

Reuse authentication hiện tại:

```text
Moderator
    ↓
Existing Login
    ↓
Authentication
    ↓
User
    ↓
Role = MODERATOR
    ↓
/moderator/dashboard
```

Phải kiểm tra:

* Authentication
* JWT/session hiện tại
* UserRole
* Account status
* Login redirect

---

# 8. LOGIN REDIRECT

Sau khi Moderator login thành công:

```text
MODERATOR
    ↓
/moderator/dashboard
```

Không redirect:

```text
/admin/dashboard
```

Không redirect Moderator vào Customer area.

Regression bắt buộc:

```text
ADMIN
    → Admin Dashboard

MODERATOR
    → Moderator Dashboard

CUSTOMER
    → Customer Area

VENDOR
    → Vendor Area
```

Không được sửa redirect của các role khác nếu không cần thiết.

---

# 9. MODERATOR DASHBOARD

Phase 1 phải tạo Dashboard tối thiểu:

```text
/moderator/dashboard
```

Dashboard ở Phase 1 chỉ là:

```text
FOUNDATION
```

Không phải dashboard nghiệp vụ hoàn chỉnh.

Không cần có:

* Complaint count
* Violation count
* ReportCase count
* Revenue
* Finance
* KYC metrics
* Escalation metrics
* Moderation statistics

Các metric đó thuộc Phase sau.

---

# 10. MODERATOR UI

Moderator UI phải reuse UI baseline hiện tại của Admin.

Ưu tiên reuse:

* Header
* Sidebar structure
* Typography
* Button
* Card
* Table
* Badge
* Form
* Modal
* Pagination
* Alert
* Toast
* Spacing
* Responsive layout
* Existing CSS/fragment/component

Không copy toàn bộ Admin business logic.

Kiến trúc:

```text
Existing Admin UI Pattern
          ↓
     UI Baseline
          ↓
 Moderator Layout
          ↓
 Moderator Dashboard
```

---

# 11. MODERATOR LAYOUT

Moderator phải có layout phù hợp role.

Ví dụ:

```text
Moderator Layout
├── Header
├── Sidebar
├── Content
└── Footer nếu project có
```

Menu chỉ hiển thị những phần thuộc Moderator Foundation.

Không hiển thị các menu Admin như:

```text
User Management
Category Management
WEB Voucher
Banner / PR
Admin Order Management
Finance
```

nếu chúng thuộc Admin-only scope.

---

# 12. ROUTE CONTRACT

Phase 1 tối thiểu phải xác định:

```text
/moderator/dashboard
```

Các route tương lai có thể được reserve về mặt architecture:

```text
/moderator/queue
/moderator/cases
/moderator/violations
/moderator/history
```

Nhưng Phase 1 **không bắt buộc triển khai business logic** cho các route này.

Không tạo controller/service chỉ để có tên route.

---

# 13. SECURITY CONTRACT

Security boundary bắt buộc:

```text
ADMIN
    ↓
/admin/**

MODERATOR
    ↓
/moderator/**

CUSTOMER
    ↓
DENY /moderator/**

VENDOR
    ↓
DENY /moderator/**

ANONYMOUS
    ↓
DENY /moderator/**

MODERATOR
    ↓
DENY Admin-only endpoints
```

Security phải được enforce ở backend.

Không chỉ ẩn menu.

Không chỉ kiểm tra frontend.

---

# 14. MODERATOR → MODERATOR AREA

Test:

```text
MODERATOR
    ↓
/moderator/dashboard
```

Expected:

```text
ALLOW
```

Test:

```text
MODERATOR
    ↓
/moderator/**
```

Expected:

```text
ALLOW
```

trong phạm vi route thực sự được triển khai.

---

# 15. MODERATOR → ADMIN AREA

Test trực tiếp:

```text
MODERATOR
    ↓
/admin/dashboard
```

Expected:

```text
DENY
```

Không được chỉ kiểm tra:

```text
Admin menu không hiển thị
```

mà phải kiểm tra request thực tế.

Nếu có Admin endpoint:

```text
MODERATOR
    ↓
Admin enforcement endpoint
```

Expected:

```text
DENY
```

---

# 16. CUSTOMER → MODERATOR

Test:

```text
CUSTOMER
    ↓
/moderator/dashboard
```

Expected:

```text
DENY
```

---

# 17. VENDOR → MODERATOR

Test:

```text
VENDOR
    ↓
/moderator/dashboard
```

Expected:

```text
DENY
```

---

# 18. ANONYMOUS → MODERATOR

Test:

```text
Anonymous
    ↓
/moderator/dashboard
```

Expected:

```text
LOGIN REQUIRED
```

hoặc:

```text
DENY
```

theo behavior chuẩn của Security architecture hiện tại.

---

# 19. LOCKED ACCOUNT

Nếu source có account status:

```text
LOCKED
```

phải kiểm tra Moderator:

```text
MODERATOR
+
LOCKED
    ↓
Login / Protected Request
```

Expected:

```text
DENY
```

Nếu source hiện tại chưa hỗ trợ session invalidation ngay lập tức sau khi account bị lock:

không tự xây cơ chế mới ngoài scope.

Phải ghi rõ:

```text
Limitation
Current Behavior
Expected Behavior
```

---

# 20. LOCKED ADMIN REGRESSION

Không chỉ test Moderator.

Phải kiểm tra Admin không bị regression:

```text
LOCKED ADMIN
    ↓
/admin/**
```

Expected:

```text
DENY
```

nếu đó là behavior của security architecture hiện tại.

Nếu source chưa hỗ trợ:

```text
NOT IMPLEMENTED / LIMITATION
```

Không ghi PASS khi chưa kiểm chứng.

---

# 21. PUBLIC REGISTRATION SECURITY

Phải kiểm tra:

```text
Public Register
    ↓
role=MODERATOR
```

Expected:

```text
DENY
```

hoặc server phải bỏ qua role do client gửi và áp dụng role hợp lệ theo registration flow.

Không được tạo Public Moderator Account.

Tương tự:

```text
role=ADMIN
```

không được cho phép tự đăng ký Admin.

---

# 22. BACKEND SECURITY

Kiểm tra source thực tế:

```text
UserRole
SecurityConfig
AuthController
Authentication
JWT/session
Route guard
Method security nếu có
```

Không tạo security layer thứ hai.

Không duplicate:

```text
ModeratorSecurityConfig
ModeratorJwtFilter
ModeratorAuthentication
```

nếu architecture hiện tại đã có security mechanism phù hợp.

---

# 23. ADMIN ↔ MODERATOR BOUNDARY

Phase 1 phải chốt rõ:

### Moderator

Có:

```text
Moderator authentication
Moderator area
Moderator dashboard
Moderator route
Moderator security
```

Không có:

```text
Admin management
Finance
Vendor enforcement
Platform configuration
```

### Admin

Giữ nguyên:

```text
Admin authentication
Admin area
Admin dashboard
Admin management
```

Phase 1 không rebuild Admin.

---

# 24. ADMIN REGRESSION

Sau khi sửa Moderator phải kiểm tra:

```text
ADMIN login
ADMIN redirect
/admin/**
Admin Dashboard
Existing Admin menu
```

Không được làm hỏng:

* Admin Product
* Admin Review
* Admin Shop
* Admin Order
* Admin Voucher
* Admin Banner

Các chức năng này đã tồn tại trong source và chỉ được regression test.

Không refactor chúng nếu không cần cho Phase 1.

---

# 25. CUSTOMER/VENDOR REGRESSION

Kiểm tra tối thiểu:

```text
CUSTOMER login
CUSTOMER redirect
VENDOR login
VENDOR redirect
```

Không thay đổi business flow Customer/Vendor.

Nếu có lỗi do thay đổi Security/Auth:

phải xác định:

```text
Root Cause
Impact
Fix
Regression
```

---

# 26. KHÔNG TRIỂN KHAI KYC

Phase 1 không làm:

```text
KycProfile
KycStatus
KYC verification
KYC dashboard
KYC approval
KYC rejection
```

Nếu source chưa có KYC:

```text
Status: OUT OF SCOPE / MISSING
```

KYC được xử lý ở Phase sau.

---

# 27. KHÔNG TRIỂN KHAI AUDITLOG

Phase 1 không tạo:

```text
AuditLog
AuditLogEntry
AuditRepository
AuditService
```

chỉ để phục vụ Phase 1.

Nếu AuditLog chưa tồn tại:

```text
Status: OUT OF SCOPE
```

Audit sẽ thuộc domain phase sau.

Không coi:

```text
file tên AuditLog tồn tại
```

là:

```text
Audit đã hoàn thành
```

---

# 28. KHÔNG TRIỂN KHAI VIOLATION

Phase 1 không tạo:

```text
Violation
ViolationService
ViolationRepository
```

Không thực hiện:

```text
Moderator
    ↓
Reject
    ↓
Violation
```

trong Phase 1.

Đây là dependency của Phase 2.

---

# 29. KHÔNG TRIỂN KHAI REPORTCASE

Phase 1 không tạo:

```text
ReportCase
ReportCaseService
ReportCaseRepository
```

Chỉ chuẩn bị boundary:

```text
Moderator Foundation
        ↓
Phase 2
        ↓
Moderation Domain
```

---

# 30. KHÔNG TRIỂN KHAI PRODUCT/REVIEW MODERATION

Phase 1 không triển khai:

```text
Product Approve
Product Reject
Review Hide
Review Unhide
Report
Violation
Evidence
Moderation History
```

Các chức năng này thuộc Phase 2.

Nếu Admin Product/Review hiện tại đã có:

```text
Admin Product Management
Admin Review Management
```

thì giữ nguyên.

Không biến chúng thành Moderator functionality trong Phase 1.

---

# 31. DATABASE SAFETY

Tuyệt đối không thực hiện:

```text
deleteAll()
deleteMany({})
drop()
dropDatabase()
collection.drop()
database reset
```

Không chạy script:

```text
cleanup()
reset()
seed-and-reset()
```

nếu script có khả năng xóa dữ liệu hiện tại.

Không xóa:

* User
* Shop
* Product
* Order
* Review
* Voucher
* Banner
* dữ liệu MongoDB hiện có.

---

# 32. TEST DATA

Nếu cần Moderator test account:

có thể insert dữ liệu tối thiểu.

Ví dụ:

```text
[TEST-PHASE-1] Moderator
```

Nhưng:

```text
NO DELETE AFTER TEST
```

Báo cáo bắt buộc:

```text
Test records inserted: <number>
Test records deleted: 0
Existing data deleted: NONE
```

Nếu không insert:

```text
Test records inserted: 0
```

---

# 33. KHÔNG FAKE PRODUCTION

Không tạo fake:

```text
moderation statistics
complaint count
violation count
revenue
KYC count
```

Dashboard Phase 1 chỉ cần foundation.

Nếu hiển thị thông tin:

phải lấy từ source thật hoặc để placeholder rõ ràng thuộc UI foundation.

Không tạo số liệu giả nhưng hiển thị như production data.

---

# 34. FILE SCOPE

Chỉ được inspect/modify những file trực tiếp liên quan:

```text
UserRole
SecurityConfig
AuthController
Authentication/JWT nếu cần
User/UserService nếu cần
Moderator Controller
Moderator Service nếu cần
Moderator template
Moderator layout
Relevant CSS/JS
Relevant security configuration
```

Không refactor toàn project.

Không sửa các domain không liên quan.

---

# 35. REUSE RULE

Trước khi tạo file:

```text
SEARCH
   ↓
EXISTING?
 ├── YES → REUSE
 └── NO  → CREATE
```

Không tạo duplicate:

```text
Role
AuthService
SecurityConfig
JwtFilter
User
Controller
Service
Repository
Layout
CSS
```

nếu source hiện tại đã có component phù hợp.

---

# 36. BUSINESS LOGIC LOCATION

Không đặt security/business logic chính trong:

```text
HTML
JavaScript
Template
```

Ưu tiên:

```text
Controller
    ↓
Service
    ↓
Repository
```

Security:

```text
SecurityConfig
Authentication
Authorization
Controller/Method security
```

theo architecture hiện tại.

---

# 37. TRANSACTION / PERSISTENCE

Phase 1 không thay đổi persistence architecture toàn hệ thống.

Không tự thêm transaction hàng loạt.

Không đổi:

```text
MongoDB strategy
Repository strategy
Authentication persistence
```

nếu không cần.

---

# 38. MANUAL TEST

## Test 1 — Moderator Login

```text
Moderator
→ Login
→ /moderator/dashboard
```

Expected:

```text
PASS
```

---

## Test 2 — Moderator Dashboard

```text
Moderator
→ /moderator/dashboard
```

Expected:

```text
ALLOW
```

---

## Test 3 — Moderator Area

```text
Moderator
→ /moderator/**
```

Expected:

```text
ALLOW
```

trong phạm vi route được triển khai.

---

## Test 4 — Moderator → Admin

```text
Moderator
→ /admin/dashboard
```

Expected:

```text
DENY
```

---

## Test 5 — Customer → Moderator

```text
Customer
→ /moderator/dashboard
```

Expected:

```text
DENY
```

---

## Test 6 — Vendor → Moderator

```text
Vendor
→ /moderator/dashboard
```

Expected:

```text
DENY
```

---

## Test 7 — Anonymous → Moderator

```text
Anonymous
→ /moderator/dashboard
```

Expected:

```text
DENY / LOGIN REQUIRED
```

---

## Test 8 — Locked Moderator

```text
MODERATOR
+
LOCKED
→ Login
```

Expected:

```text
DENY
```

nếu security architecture hiện tại hỗ trợ.

---

## Test 9 — Public Register Moderator

```text
Register
role=MODERATOR
```

Expected:

```text
DENY
```

hoặc role phải bị server kiểm soát.

---

## Test 10 — Admin Regression

```text
ADMIN
→ Login
→ Admin Dashboard
```

Expected:

```text
PASS
```

---

## Test 11 — Customer Regression

```text
CUSTOMER
→ Login
→ Customer Area
```

Expected:

```text
PASS
```

---

## Test 12 — Vendor Regression

```text
VENDOR
→ Login
→ Vendor Area
```

Expected:

```text
PASS
```

---

# 39. SECURITY TEST MATRIX

| Actor            | Target           | Expected                         |
| ---------------- | ---------------- | -------------------------------- |
| ADMIN            | `/admin/**`      | ALLOW                            |
| ADMIN            | `/moderator/**`  | DENY/không thuộc Moderator scope |
| MODERATOR        | `/moderator/**`  | ALLOW                            |
| MODERATOR        | `/admin/**`      | DENY                             |
| CUSTOMER         | `/moderator/**`  | DENY                             |
| VENDOR           | `/moderator/**`  | DENY                             |
| Anonymous        | `/moderator/**`  | DENY                             |
| Locked Moderator | Moderator area   | DENY nếu architecture hỗ trợ     |
| Locked Admin     | Admin area       | DENY nếu architecture hỗ trợ     |
| Public Register  | `role=MODERATOR` | DENY                             |

Actual result phải được ghi riêng.

Không ghi PASS chỉ dựa trên thiết kế.

---

# 40. BUILD RULE

Không tự động chạy:

```text
mvn test
mvn verify
mvn install
mvn package
```

Build chuẩn Phase 1:

```text
mvn clean package -DskipTests
```

Result:

```text
PASS
```

hoặc:

```text
FAIL
```

Nếu build fail:

phải báo:

```text
Command
Error
Root Cause
Affected Files
Fix
Status
```

---

# 41. TEST RULE

Nếu project hiện chưa có test:

không được ghi:

```text
Security PASS
```

chỉ vì không có test failure.

Phải phân biệt:

```text
NOT TESTED
```

với:

```text
PASS
```

Ví dụ:

```text
Automated Tests:
NOT AVAILABLE

Manual Security:
PASS
```

nếu thực sự đã manual test.

---

# 42. KHÔNG COI FILE EXISTS = FEATURE DONE

Không được coi:

```text
Controller tồn tại
```

là:

```text
Feature hoàn thành
```

Không được coi:

```text
Template tồn tại
```

là:

```text
Dashboard hoàn thành
```

Không được coi:

```text
SecurityConfig có /moderator/**
```

là:

```text
Moderator Foundation hoàn thành
```

Không được coi:

```text
UserRole.MODERATOR
```

là:

```text
Moderator đã triển khai
```

Phải kiểm tra end-to-end:

```text
Account
 ↓
Authentication
 ↓
Authorization
 ↓
Redirect
 ↓
Route
 ↓
Controller
 ↓
Template
```

---

# 43. DEPENDENCY RULE

Phase 1 có thể phụ thuộc vào:

```text
Existing Authentication
Existing User
Existing UserRole
Existing SecurityConfig
Existing Admin UI baseline
```

Không phụ thuộc bắt buộc vào:

```text
KYC
AuditLog
Violation
ReportCase
Complaint
Return
Refund
Escalation
Scheduler
Finance
```

Nếu một dependency ngoài scope xuất hiện:

ghi:

```text
Dependency:
<name>

Required by:
<component>

Status:
MISSING / OUT OF SCOPE / INTEGRATION-READY

Impact:
<impact>
```

Không kéo dependency đó vào Phase 1 nếu không cần.

---

# 44. INTEGRATION POINT

Phase 1 bàn giao:

```text
Moderator Account
       ↓
Authentication
       ↓
Authorization
       ↓
/moderator/**
       ↓
Moderator Dashboard
```

cho Phase 2.

Phase 2 có thể xây tiếp:

```text
Moderator Dashboard
       ↓
Moderation Queue
       ↓
Product Moderation
       ↓
Review Moderation
       ↓
ReportCase
       ↓
Violation
```

Phase 1 không cần implement các domain trên.

---

# 45. FILE CHANGE REPORT

Cuối Phase phải báo:

```text
Files Created:
- ...

Files Modified:
- ...

Files Deleted:
NONE
```

Không xóa file.

Không refactor file ngoài scope.

Không:

```text
git add .
```

một cách mù quáng.

Nếu cần staging để kiểm tra:

chỉ stage các file thuộc Phase 1.

Không commit.

---

# 46. DATA CHANGE REPORT

Bắt buộc:

```text
MongoDB:
Existing data deleted: NONE
Database reset: NO
Collection dropped: NO
deleteAll: NO
deleteMany({}): NO

Test records inserted:
<number>

Test records deleted:
0

Existing records modified:
<list hoặc NONE>
```

Nếu không có thay đổi MongoDB:

```text
MongoDB modified:
NO
```

---

# 47. SECURITY REPORT

Báo cáo:

```text
Moderator Authentication:
PASS / FAIL / NOT TESTED

Moderator Authorization:
PASS / FAIL / NOT TESTED

Moderator → Moderator:
PASS / FAIL / NOT TESTED

Moderator → Admin:
PASS / FAIL / NOT TESTED

Customer → Moderator:
PASS / FAIL / NOT TESTED

Vendor → Moderator:
PASS / FAIL / NOT TESTED

Anonymous → Moderator:
PASS / FAIL / NOT TESTED

Locked Moderator:
PASS / FAIL / NOT TESTED

Public Register → Moderator:
PASS / FAIL / NOT TESTED

Admin Regression:
PASS / FAIL / NOT TESTED
```

---

# 48. BUILD REPORT

```text
Command:
mvn clean package -DskipTests

Result:
PASS / FAIL

Notes:
<actual result>
```

Không ghi PASS nếu chưa chạy.

---

# 49. MANUAL TEST REPORT

```text
Moderator Login:
PASS / FAIL / NOT TESTED

Moderator Redirect:
PASS / FAIL / NOT TESTED

Moderator Dashboard:
PASS / FAIL / NOT TESTED

Moderator Route:
PASS / FAIL / NOT TESTED

Moderator → Admin:
PASS / FAIL / NOT TESTED

Customer → Moderator:
PASS / FAIL / NOT TESTED

Vendor → Moderator:
PASS / FAIL / NOT TESTED

Anonymous → Moderator:
PASS / FAIL / NOT TESTED

Locked Moderator:
PASS / FAIL / NOT TESTED

Public Register → Moderator:
PASS / FAIL / NOT TESTED

Admin Regression:
PASS / FAIL / NOT TESTED

Customer Regression:
PASS / FAIL / NOT TESTED

Vendor Regression:
PASS / FAIL / NOT TESTED
```

---

# 50. KNOWN ISSUES

Mỗi issue phải ghi:

```text
Issue:
<description>

Current Behavior:
<actual>

Expected Behavior:
<expected>

Root Cause:
<root cause>

Source:
<file/component>

Impact:
<impact>

Dependency:
<dependency>

Phase:
PHASE 1

Status:
MISSING / PARTIAL / WRONG / INTEGRATION-READY / OUT OF SCOPE
```

Không gọi là BLOCKED chỉ vì dependency thuộc Phase sau.

---

# 51. STATUS CLASSIFICATION

Chỉ sử dụng:

```text
IMPLEMENTED
INTEGRATED
INTEGRATION-READY
PARTIAL
MISSING
WRONG
OUT OF SCOPE
NOT TESTED
BLOCKED
```

`BLOCKED` chỉ dùng khi có blocker kỹ thuật thực sự khiến phần Phase 1 không thể tiếp tục.

Không dùng:

```text
BLOCKED – teammate chưa code
```

---

# 52. PHASE 2 HANDOFF

Phase 1 bàn giao:

```text
MODERATOR
    ↓
LOGIN
    ↓
AUTHENTICATION
    ↓
AUTHORIZATION
    ↓
/moderator/**
    ↓
MODERATOR DASHBOARD
```

Phase 2 sử dụng foundation này để triển khai:

```text
Product Moderation
Review Moderation
ReportCase
Violation
Moderation Queue
Moderation History
```

Phase 2 không cần xây lại:

```text
Moderator Role
Moderator Login
Moderator Authentication
Moderator Route Guard
```

nếu Phase 1 đã hoàn thành và source vẫn giữ nguyên.

---

# 53. ACCEPTANCE CRITERIA

Phase 1 chỉ được đánh dấu:

```text
IMPLEMENTED
```

khi tối thiểu đạt:

1. `MODERATOR` role được reuse đúng.
2. Moderator account có thể authenticate.
3. Moderator login thành công.
4. Moderator redirect tới `/moderator/dashboard`.
5. Moderator Dashboard tồn tại và hoạt động.
6. Moderator layout hoạt động.
7. `/moderator/**` được backend bảo vệ.
8. Moderator truy cập được Moderator area.
9. Customer không truy cập Moderator area.
10. Vendor không truy cập Moderator area.
11. Anonymous không truy cập Moderator area.
12. Moderator không truy cập Admin area.
13. Public registration không thể tự tạo Moderator.
14. Admin login/redirect không bị regression.
15. Customer/Vendor login không bị regression.
16. Không có MongoDB destructive operation.
17. Không xóa dữ liệu hiện có.
18. Không tạo fake production data.
19. Build được thực hiện theo command quy định.
20. Manual/security result được báo cáo đúng thực tế.

Nếu một điều kiện chưa hoàn thành:

```text
PARTIAL
```

Không tự đánh dấu:

```text
IMPLEMENTED
```

---

# 54. FINAL REPORT

Cuối Phase phải báo theo cấu trúc:

## Phase Status

```text
PHASE 1
MODERATOR FOUNDATION + SECURITY

Status:
IMPLEMENTED / PARTIAL / INTEGRATION-READY
```

## Completed

```text
Moderator Role:
...

Moderator Account:
...

Moderator Login:
...

Moderator Redirect:
...

Moderator Dashboard:
...

Moderator Layout:
...

Moderator Security:
...

Admin Boundary:
...

Customer/Vendor Boundary:
...

Registration Security:
...
```

## Security

```text
Moderator Authorization:
...

Moderator → Admin:
...

Customer → Moderator:
...

Vendor → Moderator:
...

Anonymous → Moderator:
...

Locked Account:
...

IDOR/Unauthorized Access:
...
```

## Regression

```text
Admin:
...

Customer:
...

Vendor:
...
```

## MongoDB

```text
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
<number>

Test records deleted:
NO
```

## Build

```text
mvn clean package -DskipTests

Result:
PASS / FAIL
```

## Files

```text
Files Created:
...

Files Modified:
...

Files Deleted:
NONE
```

## Known Issues

```text
Issue:
...

Root Cause:
...

Impact:
...

Status:
...
```

## Out of Scope

```text
KYC
AuditLog
Violation
ReportCase
Product Moderation
Review Moderation
Complaint
Return
Refund
Escalation
Scheduler
Finance
```

## Phase 2 Handoff

```text
Moderator Foundation
        ↓
Product Moderation
        ↓
Review Moderation
        ↓
ReportCase
        ↓
Violation
        ↓
Moderation Workflow
```

---

# 55. QUY TẮC CUỐI CÙNG

Trong Phase 1:

```text
KHÔNG LÀM LẠI AUTHENTICATION
KHÔNG TẠO ROLE MỚI
KHÔNG TẠO SECURITY SYSTEM THỨ HAI
KHÔNG TẠO MODERATOR V2
KHÔNG LÀM KYC
KHÔNG LÀM AUDITLOG
KHÔNG LÀM VIOLATION
KHÔNG LÀM REPORTCASE
KHÔNG LÀM PRODUCT MODERATION
KHÔNG LÀM REVIEW MODERATION
KHÔNG LÀM COMPLAINT
KHÔNG LÀM REFUND
KHÔNG LÀM ESCALATION
KHÔNG LÀM SCHEDULER

KHÔNG XÓA MONGODB
KHÔNG RESET MONGODB
KHÔNG DELETE TEST DATA
KHÔNG FAKE PRODUCTION DATA

KHÔNG COMMIT
KHÔNG PUSH
KHÔNG MERGE
KHÔNG CHECKOUT
KHÔNG STASH

KHÔNG CHỜ TEAMMATE

PHẢI:
ĐỌC SOURCE
TRACE DEPENDENCY
REUSE EXISTING ARCHITECTURE
IMPLEMENT PHẦN THUỘC PHASE 1
TEST SECURITY
REGRESSION
BUILD
BÁO CÁO THỰC TẾ
```

## FINAL ARCHITECTURE

```text
                 EXISTING AUTHENTICATION
                          │
                          ▼
                       USER
                          │
                          ▼
                  ROLE = MODERATOR
                          │
                          ▼
                /moderator/dashboard
                          │
                          ▼
                MODERATOR FOUNDATION
                          │
             ┌────────────┴────────────┐
             │                         │
        AUTHORIZATION              UI BASELINE
             │                         │
             ▼                         ▼
       /moderator/**             Moderator Layout
             │                   Moderator Dashboard
             │
     ┌───────┼────────┐
     │       │        │
 CUSTOMER  VENDOR   ANONYMOUS
     │       │        │
     └───────┴────────┘
             │
            DENY

MODERATOR
     │
     ▼
/admin/**
     │
    DENY
```

**Phase 1 kết thúc ở đây.**

Phase 1 có nhiệm vụ tạo **nền tảng truy cập + bảo mật + UI foundation cho Moderator**, không chịu trách nhiệm hoàn thiện hệ thống moderation nghiệp vụ. Các domain chưa tồn tại trong source như KYC, AuditLog, ReportCase, Violation được giữ nguyên cho các phase sau và **không được kéo vào Phase 1 chỉ để làm cho kế hoạch “đẹp” hơn**.
```
