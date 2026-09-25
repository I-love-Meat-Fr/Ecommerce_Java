
# GATE 0 — SOURCE AUDIT + SCOPE LOCK

## Implementation Instruction

Đọc Gate 0 này như **source of truth cho phạm vi Gate 0**.

Gate 0 được xây dựng dựa trên:

1. Source code hiện tại sau merge.
2. `AUDIT REPORT — POST-MERGE CNJAVA MARKETPLACE`.
3. `BỘ LUẬT & CHÍNH SÁCH CỦA SÀN MARKETPLACE – Phiên bản 1.0`.
4. `SHOPEE_MARKETPLACE_MODEL.md`.
5. Phase 1 — Moderator Foundation + Security + Source of Truth Reconciliation.

Mục tiêu của Gate 0 là:

```text
SOURCE AUDIT
    ↓
DOMAIN CONTRACT
    ↓
ROLE CONTRACT
    ↓
STATE CONTRACT
    ↓
ADMIN / MODERATOR SCOPE
    ↓
PHASE BOUNDARY
    ↓
SAFE HANDOFF TO PHASE 1
```

Gate 0 **không phải implementation phase**.

Không được dùng Gate 0 để triển khai Product Moderation, Review Moderation, ReportCase, Violation, Complaint, Return/Refund, Escalation, Scheduler hoặc các feature thuộc Phase 2/3/4.

---

# 0. NGUYÊN TẮC BẮT BUỘC

## 0.1 Audit Report chỉ là INPUT

Audit Report được dùng để định hướng việc kiểm tra.

Không được coi:

```text
EXISTS
DONE
IMPLEMENTED
```

trong Audit là bằng chứng chức năng đã hoàn chỉnh.

Phải đối chiếu:

```text
Audit Finding
      ↓
Source Code
      ↓
Controller / Route
      ↓
Service
      ↓
Repository
      ↓
Persistence
      ↓
Security
      ↓
UI / Consumer
      ↓
Runtime / Test evidence nếu có
```

Nếu Audit nói một feature tồn tại nhưng source hiện tại không chứng minh được:

```text
STATUS = UNKNOWN / PARTIAL / MISSING
```

Không được tự nâng thành `PASS`.

---

# 0.2 Gate 0 là READ / AUDIT / CONTRACT FREEZE

Gate 0 được phép:

* Đọc source code.
* Tìm class/entity/enum/service/controller/repository/template.
* Đọc Policy.
* Đọc Marketplace Model.
* Đối chiếu Audit với source hiện tại.
* Xác định role.
* Xác định permission boundary.
* Xác định state hiện tại.
* Xác định transition hiện tại.
* Xác định domain đã tồn tại.
* Xác định domain còn thiếu.
* Xác định source-of-truth conflict.
* Xác định dependency giữa các Phase.
* Ghi nhận các điểm `UNKNOWN`.
* Ghi nhận `NEEDS BUSINESS CLARIFICATION`.
* Khóa phạm vi cho Phase 1 và các Phase tiếp theo.

Gate 0 không được tự ý:

* Xây Moderator workflow.
* Xây ReportCase.
* Xây Violation.
* Xây Complaint.
* Xây Return/Refund.
* Xây Escalation.
* Xây Scheduler.
* Xây Auto Moderation.
* Xây Finance/Settlement.
* Xây dashboard metric mới.
* Tạo production fake data.
* Tạo business rule không có nguồn.

---

# 0.3 BUSINESS DOCUMENT COMPLIANCE

Bắt buộc đối chiếu với:

```text
BỘ LUẬT & CHÍNH SÁCH CỦA SÀN MARKETPLACE – Phiên bản 1.0
```

và:

```text
SHOPEE_MARKETPLACE_MODEL.md
```

Đối với mỗi business contract quan trọng phải ghi:

```text
Policy:
<heading / section>

Marketplace Model:
<heading / section>

Current Code:
<class / enum / method / route>

Audit Finding:
<finding>

Conclusion:
<confirmed / partial / missing / unknown>

Phase:
<owner>
```

Không được dùng kiến thức marketplace chung để tự bổ sung business rule.

Nếu Policy và Model không xác định rõ:

```text
NEEDS BUSINESS CLARIFICATION
```

Nếu Policy và code mâu thuẫn:

```text
POLICY ↔ CODE CONFLICT
```

Nếu Model và code mâu thuẫn:

```text
MODEL ↔ CODE GAP
```

Không tự chọn một phía nếu chưa có quyết định nghiệp vụ.

---

# 0.4 KHÔNG XÓA DỮ LIỆU MONGODB

Đây là nguyên tắc **BẮT BUỘC CHO GATE 0 VÀ CÁC PHASE TIẾP THEO**.

Tuyệt đối không:

```text
delete
deleteMany
deleteAll
dropCollection
dropDatabase
truncate
database reset
test database reset
destructive migration
```

Không chạy:

```java
repository.deleteAll();
```

để dọn dữ liệu.

Không dùng:

```text
cleanup()
seed()
reset()
migration
```

theo cách làm mất hoặc ghi đè dữ liệu hiện tại.

Không xóa dữ liệu để:

* giải quyết schema conflict;
* test Security;
* test Moderator;
* test KYC;
* test AuditLog;
* test Violation;
* test Review;
* test ReportCase.

MongoDB contract:

```text
Existing data
    ↓
PRESERVE
```

Gate 0 mặc định:

```text
MongoDB write = NONE
```

Nếu cần dữ liệu test:

```text
DO NOT DELETE EXISTING DATA
```

Chỉ được tạo dữ liệu test riêng biệt khi phase tương ứng cho phép và phải bảo đảm không ảnh hưởng dữ liệu hiện hữu.

---

# 0.5 KHÔNG RESET SOURCE / GIT

Không được:

```text
git reset
git clean
git checkout branch khác
git stash
git merge
git rebase
git commit
git push
```

Không overwrite thay đổi đang tồn tại.

Trước audit chỉ được quan sát:

```bash
git status
git branch
git log --oneline --decorate -10
```

Nếu working tree đã có thay đổi:

```text
DO NOT OVERWRITE
DO NOT RESET
DO NOT CLEAN
```

---

# 1. MỤC TIÊU GATE 0

Gate 0 phải hoàn thành 6 mục tiêu:

### G0.1 Source Audit

Đối chiếu toàn bộ finding quan trọng của Audit với code hiện tại.

### G0.2 Domain Contract

Xác định domain nào:

```text
EXISTS
PARTIAL
MISSING
UNKNOWN
```

### G0.3 Role Contract

Chốt boundary giữa:

```text
ADMIN
MODERATOR
VENDOR
CUSTOMER
```

### G0.4 State Contract

Xác định state hiện tại và state còn thiếu dựa trên Policy/Model.

### G0.5 Scope Lock

Xác định rõ:

```text
Gate 0
Phase 1
Phase 2A
Phase 2B
Phase 3A
Phase 3B
Phase 3C
Phase 4A
Phase 4B
```

### G0.6 Safe Handoff

Phase 1 chỉ được nhận những contract đã có đủ evidence.

---

# 2. AUDIT-TO-CODE RECONCILIATION

Dựa trên báo cáo đối chiếu hiện tại, phải xác minh các nhóm sau.

## 2.1 Moderator

Audit hiện tại cho thấy đã có dấu hiệu:

```text
MODERATOR role
/moderator/**
ROLE_MODERATOR
```

Nhưng phải kiểm tra riêng:

```text
Role
Security
Route
Controller
Service
Repository
Template
Dashboard
Login redirect
Account status
```

Không kết luận Moderator Foundation hoàn chỉnh chỉ vì role và route đã tồn tại.

---

# 2.2 Admin hiện tại

Audit cho thấy source hiện tại đã có các capability Admin như:

```text
Admin Product
Admin Review
Admin Shop
Admin Order
WEB Voucher
Banner
Dashboard
```

Đặc biệt:

```text
AdminProductService
    ├── hide
    ├── unhide
    └── delete

AdminReviewService
    ├── list
    ├── detail
    └── delete
```

Các capability này phải được giữ nguyên trừ khi có dependency trực tiếp gây lỗi.

Không được xóa Admin functionality để “nhường” cho Moderator.

---

# 2.3 Moderator ≠ Admin

Gate 0 phải khóa boundary:

```text
ADMIN
    =
Platform Management
+
Severe Enforcement
+
Administrative Control

MODERATOR
    =
Normal Moderation Workflow
```

Đây là **scope boundary**, không được tự biến thành business rule mới nếu Policy chưa xác nhận chi tiết.

Đặc biệt không được để:

```text
Admin
+
Moderator
```

cùng mutate một resource mà không có:

```text
Permission Contract
+
State Transition Contract
+
Actor Contract
```

Audit đã chỉ ra overlap hiện tại:

```text
Admin Product
    hide / unhide / delete

Moderator Product
    approve / reject / escalate
```

và:

```text
Admin Review
    delete

Moderator Review
    approve / reject / hide / unhide
```

Gate 0 phải ghi nhận đây là:

```text
DOMAIN / PERMISSION BOUNDARY TO RESOLVE
```

Không tự sửa implementation tại Gate 0.

---

# 3. DOMAIN INVENTORY

Lập bảng:

| Domain        | Code hiện tại | Audit            | Policy/Model | Status | Owning Phase       |
| ------------- | --------------- | ---------------- | ------------ | ------ | ------------------ |
| User          | kiểm tra       | có              | kiểm tra    |        | Phase 1            |
| Role          | kiểm tra       | có MODERATOR    | kiểm tra    |        | Phase 1            |
| Shop          | kiểm tra       | có              | kiểm tra    |        | Phase 1/3          |
| KYC           | kiểm tra       | chưa đủ       | kiểm tra    |        | Phase 1 → Phase 3 |
| Product       | có             | có Admin        | kiểm tra    |        | Phase 2            |
| Review        | có             | có Admin        | kiểm tra    |        | Phase 2            |
| ReportCase    | kiểm tra       | chưa thấy đủ | kiểm tra    |        | Phase 2            |
| Violation     | kiểm tra       | chưa thấy đủ | kiểm tra    |        | Phase 2            |
| Escalation    | kiểm tra       | chưa thấy      | kiểm tra    |        | Phase 3            |
| AuditLog      | kiểm tra       | chưa thấy đủ | kiểm tra    |        | Phase 2/3          |
| Complaint     | kiểm tra       | chưa thấy      | kiểm tra    |        | Phase 3A           |
| Return/Refund | kiểm tra       | chưa thấy      | kiểm tra    |        | Phase 3A           |
| Scheduler     | kiểm tra       | chưa thấy      | kiểm tra    |        | Phase 3C           |
| Dashboard     | có             | có              | kiểm tra    |        | Phase 4            |
| Voucher       | có             | có              | kiểm tra    |        | Phase 4A           |
| Banner        | có             | có              | kiểm tra    |        | Phase 4A           |

Không điền `EXISTS` chỉ vì Audit nói tồn tại.

---

# 4. ROLE CONTRACT

## 4.1 Roles cần xác minh

```text
CUSTOMER
VENDOR
MODERATOR
ADMIN
```

Không tạo role mới tại Gate 0.

Phải xác định:

```text
Role source
Role persistence
Role assignment
Role authority
Route access
Method access
Public registration
```

---

# 4.2 ADMIN CONTRACT

Gate 0 phải xác minh Admin hiện tại:

```text
ADMIN
    ↓
/admin/**
    ↓
Existing Admin functionality
```

Các capability đã được Audit ghi nhận phải được kiểm tra:

```text
User Management
Shop Management
Product
Review
Order
WEB Voucher
Banner
Dashboard
```

Không mở thêm quyền Moderator cho Admin nếu architecture hiện tại không có.

Đặc biệt không tự thêm:

```text
ADMIN → /moderator/**
```

nếu source hiện tại không xác lập.

---

# 4.3 MODERATOR CONTRACT

Contract mục tiêu:

```text
MODERATOR
    ↓
/moderator/**
    ↓
ALLOW
```

Và:

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

Phải phân biệt:

```text
URL-level security
Method-level security
Runtime authorization
```

Không chỉ kiểm tra UI.

---

# 4.4 ACCOUNT STATUS CONTRACT

Không tạo enum status thứ hai nếu source hiện tại đã có:

```text
UserStatus
```

hoặc:

```text
AccountStatus
```

Phải xác minh các trạng thái đang tồn tại, đặc biệt:

```text
ACTIVE
LOCKED
UNVERIFIED
```

Nếu Policy yêu cầu trạng thái khác mà code chưa có:

```text
MISSING STATE
```

Không tự thêm tại Gate 0.

Expected security behavior:

```text
ACTIVE MODERATOR
    ↓
LOGIN
    ↓
ALLOW
```

```text
LOCKED MODERATOR
    ↓
LOGIN
    ↓
DENY
```

Nếu lock sau login nhưng architecture chưa có realtime session invalidation:

```text
INTEGRATION LIMITATION
```

Không rewrite toàn bộ authentication architecture.

---

# 4.5 PUBLIC REGISTRATION

Phải xác minh:

```text
POST /register
role=MODERATOR
```

Expected:

```text
Public registration
      ↓
Không tạo Public Moderator
```

Không thay đổi registration flow ngoài phạm vi bảo vệ privileged role.

---

# 5. STATE CONTRACT

Đây là phần bắt buộc của Gate 0.

Lập bảng:

| Domain | State hiện có | Source | Meaning hiện tại | Transition hiện có | State còn thiếu | Evidence |
| ------ | --------------- | ------ | ------------------ | -------------------- | ----------------- | -------- |

---

# 5.1 USER STATE

Audit hiện tại ghi nhận:

```text
ACTIVE
LOCKED
UNVERIFIED
```

Phải xác minh:

```text
Enum
Field
Writer
Reader
Login guard
Admin action
```

Không tạo `ModeratorStatus` nếu UserStatus đã là source hiện tại.

---

# 5.2 SHOP STATE

Audit hiện tại ghi nhận:

```text
PENDING
APPROVED
REJECTED
```

Phải xác minh:

```text
ShopStatus
KYC relationship
Admin Shop flow
Vendor access
```

Không tự thêm:

```text
SUSPENDED
BANNED
UNDER_REVIEW
```

nếu Policy/Model/code chưa xác lập.

---

# 5.3 PRODUCT STATE

Audit hiện tại ghi nhận:

```text
DRAFT
ACTIVE
OUT_OF_STOCK
HIDDEN
```

Gate 0 phải phân biệt:

```text
Product Lifecycle State
```

với:

```text
Product Moderation Decision
```

Không tự coi:

```text
HIDDEN
```

là:

```text
MODERATION_REJECTED
```

Không tự thêm:

```text
PENDING_MODERATION
APPROVED
REJECTED
```

nếu chưa có business evidence.

Nếu Policy/Model yêu cầu moderation state nhưng code chưa có:

```text
STATE GAP
→ Owner: Phase 2A
```

---

# 5.4 REVIEW STATE

Audit hiện tại cho thấy Review chưa có đầy đủ:

```text
moderationStatus
reason
moderatorId
```

Phải kiểm tra source thực tế.

Nếu tồn tại các field như:

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
Moderation Flow
Persistence
```

Không để business logic tương lai đọc hai field tùy ý.

Nếu chưa xác định được canonical field:

```text
NEEDS BUSINESS CLARIFICATION
```

Không xóa field cũ.

---

# 5.5 REPORT CASE STATE

Audit cho thấy `ReportCase` chưa có domain đầy đủ.

Gate 0 phải xác định:

```text
Có Entity không?
Có Repository không?
Có Status enum không?
Có persistence không?
Có API không?
Có actor không?
```

Nếu chưa có:

```text
REPORT CASE DOMAIN = MISSING
```

Không tự tạo implementation tại Gate 0.

Nếu Policy yêu cầu state mà code chưa có:

```text
STATE REQUIREMENT
→ Phase 2A
```

---

# 5.6 VIOLATION STATE

Gate 0 phải kiểm tra:

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

Chỉ ghi nhận những gì thực tế tồn tại.

Không tự tạo:

```text
severity
penalty
threshold
strike count
ban duration
```

nếu Policy chưa xác lập.

---

# 5.7 ESCALATION STATE

Audit cho thấy Escalation persistence chưa đầy đủ.

Gate 0 phải ghi:

```text
Escalation = dependency for Phase 2B / Phase 3B
```

Không cho phép Phase 2B giả lập escalation bằng UI-only state.

Nếu chưa có persistence:

```text
ESCALATION PERSISTENCE = MISSING
```

Phase owner phải được ghi rõ.

---

# 5.8 AUDIT STATE / EVENT

Phải xác minh nếu có:

```text
AuditLog
AuditLogEntry
AuditEvent
AuditEventWriter
```

Nếu tồn tại hai implementation:

```text
AuditLog
AuditLogEntry
```

phải xác định:

```text
Source of Truth
Read Path
Write Path
Persistence
Legacy Compatibility
Future Integration
```

Không tạo audit system thứ ba.

Không destructive migration.

Không xóa historical audit data.

---

# 6. SOURCE-OF-TRUTH RECONCILIATION

Gate 0 phải lập bảng:

| Domain             | Candidate source | Writer | Reader | Persistence | Conflict | Decision |
| ------------------ | ---------------- | ------ | ------ | ----------- | -------- | -------- |
| KYC                |                  |        |        |             |          |          |
| Review moderation  |                  |        |        |             |          |          |
| Audit              |                  |        |        |             |          |          |
| ReportCase         |                  |        |        |             |          |          |
| Violation          |                  |        |        |             |          |          |
| Product moderation |                  |        |        |             |          |          |

---

# 6.1 KYC

Audit đã chỉ ra KYC là một gap cần source reconciliation.

Phải kiểm tra:

```text
User.kycStatus
Shop.kycStatus
KycProfile.status
```

nếu thực tế tồn tại.

Xác định:

```text
Write Path
Read Path
Persistence
Denormalized Field
Sync Rule
```

Không tạo:

```text
KycStatusV2
KycProfileV2
VendorKycStatus
```

Không xóa dữ liệu KYC hiện tại.

Nếu chưa thể xác định source:

```text
KYC SOURCE OF TRUTH
=
NEEDS BUSINESS CLARIFICATION
```

---

# 6.2 REVIEW MODERATION

Nếu có nhiều field liên quan moderation:

```text
Field A
Field B
```

không được tự chọn field nào là canonical.

Phải dựa trên:

```text
Writer
Reader
Persistence
Policy
Model
```

Nếu chưa đủ evidence:

```text
REVIEW MODERATION SOURCE = UNKNOWN
```

---

# 6.3 REJECT → VIOLATION

Contract cần xác minh:

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

Không mặc định:

```text
Every Reject = Violation
```

Chỉ áp dụng nếu Policy xác lập.

Không tự đặt penalty.

---

# 7. ADMIN / MODERATOR RESOURCE BOUNDARY

Đây là một trong các output quan trọng nhất của Gate 0.

## Product

Hiện tại:

```text
Admin
    ├── hide
    ├── unhide
    └── delete
```

Roadmap Moderator:

```text
Moderator
    ├── approve
    ├── reject
    └── escalate
```

Gate 0 phải xác định:

```text
Admin action
≠
Moderator action
```

và không để hai role mutate cùng một resource mà không có transition contract.

---

## Review

Hiện tại:

```text
Admin
    └── delete
```

Roadmap Moderator:

```text
Moderator
    ├── approve
    ├── reject
    ├── hide
    └── unhide
```

Phải ghi:

```text
Existing Admin Review capability
+
Future Moderator Moderation capability
```

Không gọi Admin delete là Moderator moderation.

---

# 8. PHASE SCOPE LOCK

Dựa trên báo cáo đối chiếu hiện tại, khóa boundary:

```text
Gate 0
    ↓
Domain / Role / State Contract
    ↓
Phase 1
    ↓
Moderator Access + Foundation
    ↓
Phase 2A
    ↓
Moderation Domain Foundation
    ↓
Phase 2B
    ↓
Moderator Workflows
    ↓
Phase 3A
    ↓
Complaint + Return/Refund
    ↓
Phase 3B
    ↓
Escalation + Enforcement + AuditLog
    ↓
Phase 3C
    ↓
Scheduler + Idempotency
    ↓
Phase 4A
    ↓
Dashboard/Voucher/Banner Hardening
    ↓
Phase 4B
    ↓
Cross-domain Metrics
```

---

# 8.1 GATE 0

Được làm:

```text
Source Audit
Domain Inventory
Role Contract
Permission Boundary
State Contract
State Gap
Source-of-Truth Analysis
Phase Boundary
Dependency Map
Business Decision Register
MongoDB Safety Contract
```

Không implementation feature.

---

# 8.2 PHASE 1

Phase 1 nhận:

```text
Moderator Access
Security
Role Routing
Account Status Guard
Public Register Protection
Source-of-Truth reconciliation cần thiết cho Phase 1
Regression Protection
Integration Contract
```

Phase 1 không tự mở rộng thành:

```text
Product Moderation
Review Moderation Engine
Complaint
Return/Refund
Scheduler
Fraud Engine
Auto Moderation
```

---

# 8.3 PHASE 2A

Ownership:

```text
Product Moderation Domain
Review Moderation Domain
ReportCase
Violation
Audit Event Contract
```

Không gọi Admin CRUD hiện tại là implementation của Phase 2.

---

# 8.4 PHASE 2B

Ownership:

```text
Moderator Queue
Detail
Approve
Reject
Hide
Unhide
Escalate
History
```

Chỉ được triển khai sau khi domain foundation tương ứng tồn tại.

---

# 8.5 PHASE 3A

Ownership:

```text
Complaint
Return
Refund
```

Không trộn với Scheduler.

---

# 8.6 PHASE 3B

Ownership:

```text
Admin Escalation
Enforcement
AuditLog integration
```

Phải có persistence contract từ Phase 2 trước khi triển khai enforcement.

---

# 8.7 PHASE 3C

Ownership:

```text
Scheduler
Deadline
Retry
Idempotency
```

Không tự đặt deadline/SLA nếu Policy chưa xác lập.

---

# 8.8 PHASE 4A

Ownership:

```text
Dashboard hardening
Voucher hardening
Banner hardening
```

Audit hiện tại cho thấy phần lớn các feature này đã có code.

Không rewrite nếu behavior đúng.

---

# 8.9 PHASE 4B

Chỉ thực hiện metrics khi domain backend thực sự tồn tại.

Ví dụ Audit đã chỉ ra:

```text
totalPlatformRevenue
```

hiện đang được tính từ:

```text
DELIVERED orders
```

Không được tự gọi đó là:

```text
Platform Commission Revenue
```

nếu Finance/Commission contract chưa tồn tại.

Không fake:

```text
GMV
Platform Fee
Vendor Payable
Refund
Escrow
Settlement
```

---

# 9. DEPENDENCY CONTRACT

Phân biệt:

## Technical Dependency

Ví dụ:

```text
Moderator Login
    ↓
Existing Authentication
```

Có thể reuse.

Nếu dependency lỗi và trực tiếp làm Phase 1 fail:

```text
Inspect
↓
Root Cause
↓
Minimal Safe Fix
↓
Regression
```

Không rewrite architecture.

---

## Integration Dependency

Ví dụ:

```text
Moderator Dashboard
    ↓
Future Moderation Metrics
```

Nếu backend chưa có:

```text
Current = NOT AVAILABLE
Contract = INTEGRATION-READY
Production Fake = FORBIDDEN
```

Không dùng fake metric để làm UI có vẻ hoàn chỉnh.

---

## Blocking Dependency

Chỉ được ghi:

```text
BLOCKED
```

khi:

```text
Baseline không build được
+
Phase không thể triển khai độc lập
+
Không có workaround an toàn
```

Không dùng:

```text
Developer chưa code
Branch chưa merge
Feature chưa hoàn thành
```

làm lý do BLOCKED.

---

# 10. STATE GAP REGISTER

Gate 0 phải tạo bảng riêng:

| Gap ID | Domain | Current State | Required/Expected State | Evidence | Impact | Owner Phase | Status |
| ------ | ------ | ------------- | ----------------------- | -------- | ------ | ----------- | ------ |

Ví dụ cần kiểm tra:

```text
User
Shop
KYC
Product
Review
ReportCase
Violation
Escalation
Audit
Complaint
Return
Refund
```

Không tự thêm state.

Nếu state chỉ được suy ra từ Model nhưng chưa được Policy xác nhận:

```text
MODEL REFERENCE ONLY
```

Nếu Policy yêu cầu nhưng code chưa có:

```text
MISSING IMPLEMENTATION
```

---

# 11. BUSINESS DECISION REGISTER

Mọi vấn đề chưa đủ căn cứ phải ghi:

| ID | Domain | Question | Evidence | Impact | Owner | Phase | Status |
| -- | ------ | -------- | -------- | ------ | ----- | ----- | ------ |

Các câu hỏi cần đặc biệt kiểm tra:

```text
Admin có quyền gì mà Moderator không có?

Moderator có quyền gì mà Admin không nên trực tiếp thực hiện?

Product lifecycle state có tách moderation state không?

Review lifecycle có tách moderation status không?

Reject ReportCase có tạo Violation trong mọi trường hợp không?

KYC source of truth là gì?

AuditLog source of truth là gì?

Escalation thuộc Moderator hay Admin?

Platform Revenue được định nghĩa thế nào?

Refund tác động metric nào?
```

Đây là **câu hỏi cần resolve**, không phải business rule mặc định.

---

# 12. EXISTING IMPLEMENTATION PRESERVATION

Gate 0 phải ghi nhận rõ các capability đã tồn tại để các Phase sau không vô tình rewrite:

```text
Admin Product
Admin Review
Admin Shop
Admin Order
WEB Voucher
Banner
Dashboard
Existing Authentication
Existing User / Shop status
```

Nguyên tắc:

```text
DISCOVER
   ↓
VERIFY
   ↓
KEEP nếu đúng
   ↓
MINIMAL FIX nếu có dependency trực tiếp
```

Không:

```text
rewrite
replace
duplicate
delete
```

chỉ vì roadmap có domain mới.

---

# 13. MONGODB SAFETY CHECKLIST

Trước khi Gate 0 kết thúc phải xác nhận:

```text
[ ] Không delete document
[ ] Không deleteMany
[ ] Không deleteAll
[ ] Không drop collection
[ ] Không drop database
[ ] Không reset database
[ ] Không truncate
[ ] Không destructive migration
[ ] Không xóa legacy field
[ ] Không xóa historical record
[ ] Không reset test database đang chứa dữ liệu hiện tại
[ ] Không seed production data
[ ] Không overwrite existing record
```

Nếu Gate 0 không truy cập MongoDB:

```text
MongoDB verification = NOT RUN
```

Không được viết thành:

```text
MongoDB safe = PASS
```

chỉ vì chưa chạy.

---

# 14. TEST / BUILD EVIDENCE

Phải phân biệt:

```text
SOURCE VERIFICATION
RUNTIME VERIFICATION
TEST VERIFICATION
MANUAL VERIFICATION
```

Không đánh đồng chúng.

Nếu không chạy:

```text
Build = NOT RUN
Tests = NOT RUN
Runtime = NOT RUN
```

Không biến:

```text
NOT RUN
```

thành:

```text
PASS
```

Nếu được yêu cầu build, báo cáo command và kết quả chính xác.

---

# 15. GATE 0 ACCEPTANCE CRITERIA

Gate 0 chỉ đạt khi tất cả điều kiện sau hoàn thành.

## 15.1 Source Audit

* Mọi Audit finding quan trọng đã được đối chiếu với source hiện tại.
* Finding cũ không được mặc định vẫn đúng.
* Có evidence cho status.
* Phân biệt `EXISTS`, `PARTIAL`, `MISSING`, `UNKNOWN`.

## 15.2 Domain Contract

Đã có inventory cho:

```text
User
Role
Shop
KYC
Product
Review
ReportCase
Violation
Escalation
Audit
Complaint
Return
Refund
Voucher
Banner
Order
Dashboard
Scheduler
```

## 15.3 Role Contract

Đã xác định:

```text
ADMIN
MODERATOR
VENDOR
CUSTOMER
```

và boundary:

```text
ADMIN ≠ MODERATOR
```

Không có privileged role creation qua public registration.

## 15.4 State Contract

Đã xác định:

```text
Current State
Transition
Actor
Guard
Missing State
Business Evidence
Owning Phase
```

## 15.5 Source of Truth

Đã xác định hoặc đánh dấu rõ:

```text
KYC
Review Moderation
Audit
ReportCase
Violation
Product Moderation
```

## 15.6 Phase Boundary

Không còn ambiguity về:

```text
Gate 0
Phase 1
Phase 2A
Phase 2B
Phase 3A
Phase 3B
Phase 3C
Phase 4A
Phase 4B
```

## 15.7 Data Safety

```text
MongoDB existing data preserved
No delete
No reset
No drop
No destructive migration
```

## 15.8 No Scope Creep

Gate 0 không triển khai:

```text
Product Moderation workflow
Review Moderation workflow
ReportCase workflow
Violation workflow
Complaint
Return/Refund
Escalation
Scheduler
Auto Moderation
Finance
```

---

# 16. FINAL GATE 0 REPORT

Output cuối cùng bắt buộc có cấu trúc:

## 16.1 Gate Status

```text
GATE 0 — SOURCE AUDIT + SCOPE LOCK

Source Audit:
COMPLETE / PARTIAL / BLOCKED

Domain Contract:
COMPLETE / PARTIAL

Role Contract:
COMPLETE / PARTIAL

State Contract:
COMPLETE / PARTIAL

Source-of-Truth:
COMPLETE / PARTIAL / NEEDS BUSINESS CLARIFICATION

Phase Scope:
LOCKED / PARTIAL

MongoDB:
NO DATA CHANGES
```

---

## 16.2 Audit Reconciliation

| Finding | Current Code | Actual Status | Evidence | Owning Phase |
| ------- | ------------ | ------------- | -------- | ------------ |

---

## 16.3 Domain Contract

| Domain | Existing | Missing | Conflict | Owning Phase |
| ------ | -------- | ------- | -------- | ------------ |

---

## 16.4 Role Contract

| Role      | Existing Permission | Required Boundary | Status |
| --------- | ------------------- | ----------------- | ------ |
| ADMIN     |                     |                   |        |
| MODERATOR |                     |                   |        |
| VENDOR    |                     |                   |        |
| CUSTOMER  |                     |                   |        |

---

## 16.5 State Contract

| Domain | Current States | Missing States | Transition Gap | Owner |
| ------ | -------------- | -------------- | -------------- | ----- |

---

## 16.6 Admin / Moderator Boundary

Phải ghi rõ:

```text
Admin existing capabilities:
...

Moderator existing capabilities:
...

Overlap:
...

Required separation:
...

Unresolved:
...
```

Không được đưa ra ranking hoặc “role nào tốt hơn”.

---

## 16.7 Source-of-Truth Register

Phải có ít nhất:

```text
KYC
Review Moderation
Audit
ReportCase
Violation
Product Moderation
```

---

## 16.8 Business Decision Register

Liệt kê tất cả:

```text
UNKNOWN
NEEDS BUSINESS CLARIFICATION
POLICY ↔ CODE CONFLICT
MODEL ↔ CODE GAP
```

---

## 16.9 Phase Handoff

### Phase 1

```text
Confirmed:
...

Dependencies:
...

Open Decisions:
...

Regression Risks:
...

Must Preserve:
...
```

### Phase 2A

```text
Required Domain Contracts:
...

Missing Backend:
...

Dependencies:
...
```

### Phase 2B

```text
Required:
...

Blocked by:
...
```

### Phase 3A

```text
Required:
Complaint
Return
Refund
```

### Phase 3B

```text
Required:
Escalation
Enforcement
AuditLog
```

### Phase 3C

```text
Required:
Scheduler
Deadline
Idempotency
Retry
```

### Phase 4A

```text
Dashboard
Voucher
Banner
```

### Phase 4B

```text
Cross-domain metrics
Finance-dependent metrics
Moderation-dependent metrics
```

---

# 17. FILE / DATABASE SAFETY REPORT

Báo cáo cuối:

```text
Files Modified:
<list hoặc NONE>

Files Created:
<list hoặc NONE>

Files Deleted:
NONE

MongoDB:
Existing data preserved

Deletes:
NONE

deleteAll():
NOT USED

deleteMany():
NOT USED

dropCollection():
NOT USED

dropDatabase():
NOT USED

Database Reset:
NOT USED

Destructive Migration:
NOT USED

Existing Records Modified:
NO

Production Seed:
NONE
```

Nếu một mục không được thực hiện/kiểm chứng:

```text
NOT RUN
```

Không được tự ghi `NO` nếu không có evidence.

---

# 18. GATE 0 → PHASE 1 HANDOFF

Phase 1 chỉ được bắt đầu trên baseline:

```text
Audit findings đã reconcile
+
Role contract đã xác định
+
Admin/Moderator boundary đã khóa
+
Account status contract đã xác định
+
State gaps đã đăng ký
+
Source-of-truth conflicts đã ghi nhận
+
MongoDB preservation rule đã khóa
+
Phase boundary đã khóa
```

Các vấn đề chưa đủ căn cứ phải chuyển tiếp:

```text
UNKNOWN
NEEDS BUSINESS CLARIFICATION
```

Không được Phase 1 tự biến chúng thành business rule.

---

# 19. FINAL PRINCIPLE

```text
AUDIT ≠ PROOF

CODE
    =
CURRENT IMPLEMENTATION

POLICY
    =
BUSINESS RULE

MARKETPLACE MODEL
    =
CONCEPTUAL / DOMAIN BASELINE

GATE 0
    =
SOURCE AUDIT
    +
DOMAIN CONTRACT
    +
ROLE CONTRACT
    +
STATE CONTRACT
    +
SCOPE LOCK

ADMIN
    ≠
MODERATOR

EXISTING DATA
    =
PRESERVE

NO DELETE
NO RESET
NO DROP
NO DESTRUCTIVE MIGRATION
NO FAKE DATA
NO FAKE BUSINESS RULE
NO SCOPE CREEP
```

**Gate 0 chỉ hoàn thành khi hệ thống đã có một baseline có thể truy vết từ Audit → Policy/Model → Code → Domain → Role → State → Phase, và mọi phần chưa đủ bằng chứng đều được ghi nhận rõ thay vì tự suy đoán.**
