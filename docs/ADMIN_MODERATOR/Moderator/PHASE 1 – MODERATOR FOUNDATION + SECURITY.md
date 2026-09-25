PHASE 1 – MODERATOR FOUNDATION + SECURITY

Implementation instruction: Đọc Phase 1 này như source of truth cho phạm vi triển khai Phase 1.

Independent Implementation Principle:
“Tôi implement được phần của tôi bằng contract/interface hiện tại; backend của Tuấn là integration point, không phải prerequisite để bắt đầu hoặc hoàn thành implementation.”

1. MỤC TIÊU

Dựng hoàn chỉnh nền tảng Moderator + Security để chuẩn bị cho các Phase Moderation tiếp theo.

Phase 1 phải có khả năng:

DISCOVER
   ↓
DEFINE CONTRACT
   ↓
IMPLEMENT PHASE 1
   ↓
TEST INDEPENDENTLY
   ↓
INTEGRATION-READY

Không chờ:

❌ developer khác
❌ branch khác
❌ commit khác
❌ pull request khác
❌ backend implementation khác
❌ merge của developer khác
Flow tổng thể
Audit Phase 0
      ↓
Xác nhận MODERATOR role hiện tại
      ↓
Kiểm tra Authentication / Authorization hiện tại
      ↓
Define Moderator Security Contract
      ↓
Dựng Moderator Login Flow
      ↓
Dựng Moderator Dashboard
      ↓
Dựng Moderator Layout / Menu
      ↓
Bảo vệ /moderator/**
      ↓
Kiểm tra Account Status
      ↓
Kiểm tra Moderator → Admin bị chặn
      ↓
Kiểm tra Admin Regression
      ↓
Independent Test
      ↓
INTEGRATION-READY
      ↓
Sẵn sàng cho Phase 2
2. PHẠM VI PHASE 1

Phase 1 tập trung vào:

Moderator Foundation
+
Security
+
UI Foundation
+
Role Routing
+
Account Status Guard
+
Integration Contract
+
Regression Test

Phase 1 không cần chờ backend Moderation để hoàn thành.

3. INDEPENDENT IMPLEMENTATION RULE

Đây là nguyên tắc bắt buộc.

3.1 Phase 1 phải implement độc lập

Không được viết:

WAIT FOR TUẤN
WAIT FOR QUỐC ANH
WAIT FOR BACKEND
WAIT FOR BRANCH
WAIT FOR COMMIT

Thay vào đó:

External capability chưa có
        ↓
Xác định contract/interface cần thiết
        ↓
Implement phần thuộc Phase 1
        ↓
Test độc lập
        ↓
Đánh dấu INTEGRATION-READY
        ↓
Backend thật tích hợp sau
3.2 Phân biệt 3 loại dependency
A. Technical dependency

Ví dụ:

Moderator Login
    ↓
Existing Authentication

Đây là dependency kiến trúc hiện hữu.

Được phép reuse.

B. Integration dependency

Ví dụ:

Moderator Dashboard
    ↓
Product moderation count

Nếu Product Moderation backend chưa có:

Current:
NOT AVAILABLE

Contract:
DEFINED

Integration:
PENDING

Phase 1:
INTEGRATION-READY

Không block Phase 1.

C. Blocking dependency

Chỉ coi là BLOCKED nếu:

Repository không build được do baseline
+
không thể thực hiện Phase 1 độc lập

Không được dùng:

"Tuấn chưa code"
"Quốc Anh chưa code"
"Backend chưa merge"

làm lý do BLOCKED.

4. SOURCE OF TRUTH

Trước khi code phải đọc:

Source code hiện tại.
Gate 0 / Phase 0.
File Phase 1.
Marketplace Policy nếu được cung cấp.
Source Code
     +
Gate 0
     +
Phase 1
     +
Marketplace Policy
     ↓
Implementation

Policy chỉ dùng để kiểm tra:

Role
Moderator responsibility
Admin / Moderator boundary
Moderation responsibility
Violation boundary
Escalation boundary
KYC boundary
Audit boundary

Không tự thêm business logic thuộc Phase sau.

5. GIT SAFETY

Trước khi code:

git status
git branch
git log --oneline --decorate -10

Ghi nhận:

Current branch:
Current HEAD:
Working tree:
Uncommitted changes:

Nếu working tree có thay đổi:

KHÔNG reset
KHÔNG stash
KHÔNG checkout làm mất thay đổi
KHÔNG overwrite code chưa commit
KHÔNG merge
KHÔNG push

Phase 1 chỉ sửa phần cần thiết.

6. DISCOVERY – KIỂM TRA SOURCE HIỆN TẠI

Trước khi tạo mới, tìm:

UserRole
MODERATOR
User
UserStatus
AccountStatus
SecurityConfig
Authentication
Authorization
@PreAuthorize
requestMatchers
AuthController
AuthService
ModeratorController
ModeratorService
Moderator DTO
Moderator Template
Moderator CSS
Moderator JS
Admin Layout
Admin Fragment

Kiểm tra:

MODERATOR đã tồn tại?
Route đã tồn tại?
Login flow đã tồn tại?
Dashboard đã tồn tại?
Security đã bảo vệ /moderator/**?
API /api/moderator/** tồn tại?
Method security bật?
Account Status kiểm tra ở đâu?
Admin UI có thể reuse gì?
7. REUSE EXISTING MODERATOR ROLE

Nếu project đã có:

MODERATOR

thì:

REUSE

Không tạo:

MOD
STAFF
CONTENT_MODERATOR
MODERATOR_ADMIN

nếu project không có.

Role chuẩn:

CUSTOMER
VENDOR
MODERATOR
ADMIN

Phase 1 không thay đổi phân quyền Moderator hiện tại nếu không có lỗi trực tiếp cản trở Phase 1.

8. MODERATOR ACCOUNT PROVISIONING

Public registration không được phép tự tạo Moderator.

Không cho:

POST /register

role=MODERATOR

tạo tài khoản Moderator.

Public Register phải tiếp tục dùng flow hiện tại:

Public Register
      ↓
Existing registration rule
      ↓
Normal role

Moderator phải được provision bằng cơ chế nội bộ hiện tại:

Admin
Seed
Test Account
Internal Provisioning

Không cần xây Moderator Management UI trong Phase 1.

Test
Register
role=MODERATOR
        ↓
Không tạo Public Moderator
9. MODERATOR LOGIN

Không tạo authentication riêng.

Reuse:

Existing Login
Existing Authentication
Existing Session/JWT
Existing Authorization

Flow:

Moderator
    ↓
Existing Login
    ↓
Authentication
    ↓
Role = MODERATOR
    ↓
/moderator/dashboard

Sau login:

MODERATOR → /moderator/dashboard

Không redirect Moderator vào:

/admin/dashboard

Regression:

ADMIN      → Admin area
MODERATOR  → Moderator area
CUSTOMER   → Customer area
VENDOR     → Vendor area
10. MODERATOR DASHBOARD

Dashboard Phase 1 chỉ là Foundation.

Không cần có business data thật của:

Product Moderation
Review Moderation
ReportCase
Violation
Escalation

Nếu backend chưa có:

--

hoặc:

Chưa có dữ liệu

Không fake:

Pending Products = 15
Reports = 8
Violations = 3
11. DASHBOARD CONTRACT

Nếu backend hiện tại đã có DTO:

ModeratorDashboardDTO

thì reuse.

Ví dụ contract:

ModeratorDashboardDTO

pendingProducts
pendingReviews
pendingReports
pendingViolations
pendingEscalations

Nếu chưa có backend:

Contract:
DEFINED

Implementation:
NOT AVAILABLE

Production fake:
FORBIDDEN

Phase 1:
INTEGRATION-READY

Phase 1 không cần implement các business service chỉ để lấy số liệu dashboard.

12. ADMIN UI REUSE

Reuse Admin UI baseline.

Có thể reuse:

Layout
Header
Sidebar pattern
Card
Button
Table
Badge
Modal
Form
Typography
Spacing
Responsive behavior
Fragment
CSS
Component

Mô hình:

Existing Admin UI
       ↓
UI Baseline
       ↓
Moderator UI

Chỉ thay:

Moderator Menu
Moderator Title
Moderator Content
Moderator Route
Role-specific content

Không:

Rewrite Admin UI
Rewrite CSS toàn project
Đổi frontend framework
Đổi theme toàn hệ thống
Xóa Admin component
13. MODERATOR LAYOUT

Moderator phải có layout riêng hoặc reuse layout nhưng render menu theo role.

Moderator Layout
├── Header
├── Sidebar
├── Content
└── Footer nếu project có

Moderator không được tự động thấy các Admin menu không thuộc scope:

User Management
Category Management
Voucher
Banner / PR
Admin Order Management
Vendor Management
Finance
14. MODERATOR MENU

Có thể chuẩn bị:

Moderator
├── Dashboard
├── Moderation Queue
├── Report Cases
├── Violations
└── History

Các menu Phase sau có thể là:

placeholder

nhưng:

Placeholder chỉ là UI Foundation, không được báo cáo là business feature đã hoàn thành.

Không tạo business backend chỉ để menu hoạt động.

15. MODERATOR ROUTES

Main route:

/moderator/dashboard

Có thể chuẩn bị:

/moderator/queue
/moderator/cases
/moderator/violations
/moderator/history

Nhưng Phase 1 không triển khai:

Product moderation
Review moderation
ReportCase business
Violation business
History business

Nếu route cần tồn tại cho UI foundation:

placeholder

là đủ.

16. SECURITY CONTRACT

Phase 1 phải xác định contract:

ModeratorAreaAccess

Role:
MODERATOR

Allowed:
 /moderator/**

Denied:
 /admin/**

Route matrix:

Role	/moderator/**	/admin/**
ADMIN	Theo architecture hiện tại	Allow
MODERATOR	Allow	Deny
VENDOR	Deny	Deny
CUSTOMER	Deny	Deny
Anonymous	Deny	Deny

ADMIN → /moderator/** không tự mở quyền mới.

Nếu architecture hiện tại cho phép Admin giám sát Moderator:

REUSE

Nếu không:

KEEP CURRENT RULE

Không tự thay đổi Admin permission.

17. METHOD-LEVEL SECURITY

Nếu project đã dùng:

@PreAuthorize

thì reuse.

Ví dụ:

@PreAuthorize("hasRole('MODERATOR')")

Không tạo Security mechanism thứ hai.

Mục tiêu:

Route Security
+
Method Security nếu architecture hiện tại có

Không refactor Security Core.

18. MODERATOR → ADMIN BOUNDARY

Bắt buộc:

MODERATOR
    ↓
/admin/**
    ↓
DENIED

Test trực tiếp:

GET /admin/dashboard

Không chỉ kiểm tra menu.

Phải kiểm tra request thực tế.

19. ACCOUNT STATUS GUARD

Reuse enum hiện tại:

UserStatus

hoặc:

AccountStatus

Không tạo enum duplicate.

Expected:

ACTIVE MODERATOR
    ↓
Login
    ↓
ALLOW

và:

LOCKED MODERATOR
    ↓
Login
    ↓
DENY

Nếu account bị lock sau login:

Authenticated Moderator
       ↓
Account LOCKED
       ↓
Next protected request
       ↓
DENY

Nếu architecture hiện tại chưa hỗ trợ realtime session invalidation:

Status:
INTEGRATION-LIMITATION

Không tự tạo session architecture mới chỉ vì Phase 1.

20. LOCKED ADMIN

Phải regression-test:

LOCKED ADMIN
    ↓
/admin/**
    ↓
DENY

Nếu architecture hiện tại chưa hỗ trợ:

Current:
NOT SUPPORTED

Phase 1:
Không rewrite Security Core

Status:
LIMITATION / INTEGRATION-READY
21. PUBLIC REGISTER SECURITY

Test:

POST /register
role=MODERATOR

Expected:

Không tạo Public Moderator

Đây là security requirement của Phase 1.

Không phụ thuộc Auto Moderation, ReportCase, Violation, KYC hay AuditLog.

22. BACKEND ↔ UI INTEGRATION BOUNDARY

Đây là phần được thay đổi quan trọng nhất so với file cũ.

Phase 1 chỉ sở hữu:

Controller
DTO nếu cần
Template
UI
Route
Security Integration
Login Redirect
Account Status Integration

Các backend feature Phase sau:

Auto Moderation
ReportCase
Moderation Result
Violation
KYC
Legal
AuditLog

không phải prerequisite.

23. EXTERNAL INTEGRATION CONTRACT

Nếu Phase 1 có điểm cần tích hợp backend tương lai:

Contract
Integration Seam
Current State
Integration Status

Ví dụ:

Integration Point:
Moderator Dashboard Metrics

Contract:
ModeratorDashboardDTO

Current State:
Backend chưa cung cấp

Production Fake:
Không sử dụng

UI:
Placeholder

Integration Status:
INTEGRATION-READY
24. AUTO MODERATION INTEGRATION

Auto Moderation backend là integration point của Phase sau.

Phase 1:

KHÔNG implement Auto Moderation.
KHÔNG chờ Auto Moderation.
KHÔNG fake Auto Moderation.

Nếu dashboard sau này cần dữ liệu:

Contract:
AutoModerationResult / ModeratorDashboardDTO

Current:
NOT AVAILABLE

Phase 1:
INTEGRATION-READY

Backend Auto Moderation sau này chỉ cần implement contract/seam phù hợp.

25. REPORTCASE / VIOLATION / KYC / AUDIT INTEGRATION

Tương tự:

ReportCase
Contract:
ReportCase summary/detail contract

Current:
Phase 2 backend chưa cần tồn tại

Phase 1:
INTEGRATION-READY
Violation
Contract:
Violation summary contract

Current:
Phase 3 capability chưa có

Phase 1:
INTEGRATION-READY
KYC
Contract:
KYC status/reference nếu UI tương lai cần

Current:
NOT REQUIRED FOR PHASE 1

Status:
OUT OF SCOPE
AuditLog

Phase 1 không xây AuditLog backend.

Nếu Security action cần audit event:

Audit Event Contract

có thể được xác định.

AuditLog implementation thật tích hợp sau.

26. KHÔNG LẤN SANG PHASE 2/3

Phase 1 không implement:

Product Moderation Queue
Product Moderation Detail
Approve Product
Reject Product
Escalate Product
Review Moderation
Review Queue
ReportCase Business Logic
ReportCase Detail
Violation Engine
Escalation Business Logic
Complaint Business Logic
Auto Moderation
ModerationResult Backend
KYC Backend
AuditLog Backend

Có thể tạo:

Menu
Route
Page Placeholder
Contract
Integration Seam

nhưng không triển khai business logic.

27. FILE SCOPE

Trước khi sửa:

Search existing file
      ↓
Exists?
 ├── YES → modify minimally
 └── NO  → create following existing architecture

Các nhóm file có thể liên quan:

UserRole
User
UserStatus / AccountStatus
SecurityConfig
Authentication
Authorization
AuthController
AuthService
ModeratorController
Moderator DTO
Moderator Service nếu cần
Admin Layout
Admin Fragment
Moderator Template
Moderator CSS
Moderator JS

Không tạo hàng loạt file nếu có thể reuse.

28. ACCEPTANCE CRITERIA

Phase 1 phải đạt:

Authentication
MODERATOR login
        ↓
success
        ↓
/moderator/dashboard
Authorization
MODERATOR → /moderator/**  ALLOW
MODERATOR → /admin/**      DENY
Other roles
CUSTOMER → /moderator/**   DENY
VENDOR   → /moderator/**   DENY
ANON     → /moderator/**   DENY
Account status
ACTIVE MODERATOR → ALLOW
LOCKED MODERATOR → DENY
Registration
Public Register
role=MODERATOR
        ↓
DENY
Regression
ADMIN
 ↓
Admin Dashboard

vẫn hoạt động.

29. TEST PLAN
29.1 Moderator Login
Valid Moderator
Valid Password
        ↓
Login success
        ↓
/moderator/dashboard
29.2 Wrong Password
Moderator
Wrong Password
        ↓
Login failed
29.3 Public Register
role=MODERATOR
        ↓
No Moderator account created
29.4 Moderator Access
MODERATOR → /moderator/dashboard

Expected:

ALLOW
29.5 Moderator Admin Access
MODERATOR → /admin/dashboard

Expected:

DENIED
29.6 Customer
CUSTOMER → /moderator/dashboard

Expected:

DENIED
29.7 Vendor
VENDOR → /moderator/dashboard

Expected:

DENIED
29.8 Anonymous
ANONYMOUS → /moderator/dashboard

Expected:

Login Required / DENIED

theo architecture hiện tại.

29.9 Locked Moderator
MODERATOR
STATUS=LOCKED
        ↓
Login
        ↓
DENIED
29.10 Locked Admin
ADMIN
STATUS=LOCKED
        ↓
/admin/dashboard
        ↓
DENIED

nếu architecture hiện tại yêu cầu.

30. SECURITY TEST MATRIX
Test	Expected
ADMIN → /admin/**	Allow theo architecture
MODERATOR → /moderator/**	Allow
MODERATOR → /admin/**	Deny
CUSTOMER → /moderator/**	Deny
VENDOR → /moderator/**	Deny
Anonymous → /moderator/**	Deny
Locked Moderator	Deny
Locked Admin	Deny theo architecture
Public Register → role=MODERATOR	Deny

Không đánh dấu PASS nếu chưa thực sự test.

31. UI TEST

So sánh:

Admin Dashboard
       vs
Moderator Dashboard

Kiểm tra:

Sidebar
Header
Font
Card
Button
Table
Badge
Modal
Spacing
Responsive

Expected:

Same UI baseline
+
Different business/menu
32. ADMIN REGRESSION

Sau khi thay Security/Route/UI:

Admin Login
Admin Dashboard
User Management
Shop Management
Category
WEB Voucher
Banner

test nếu các chức năng tồn tại.

Kiểm tra:

404
403
Redirect
Template
CSS
Menu
Authentication
33. BUILD & TEST

Chạy:

mvn clean package

và:

mvn test

Báo cáo:

BUILD:
PASS / FAIL

TEST:
PASS / FAIL / NOT RUN

Nếu fail:

Error:
Root cause:
Affected module:
Phase 1 caused? YES / NO / UNKNOWN

Không đổ lỗi cho integration backend nếu Phase 1 không sử dụng backend đó.

34. PHÂN LOẠI STATUS

Không dùng:

DEPENDENCY – ANH TUẤN
DEPENDENCY – QUỐC ANH
BLOCKED – TUẤN CHƯA CODE

Thay bằng:

IMPLEMENTED

Phần Phase 1 đã code và test được độc lập.

INTEGRATION-READY

Contract/seam đã xác định, Phase 1 hoàn thành phần sở hữu, backend bên ngoài chưa kết nối.

INTEGRATED

Backend thật đã kết nối và test thành công.

PARTIAL

Phần Phase 1 còn thiếu.

MISSING

Chưa implement.

OUT OF SCOPE

Không thuộc Phase 1.

BLOCKED

Chỉ dùng khi có blocker kỹ thuật thực sự khiến Phase 1 không thể tiếp tục.

35. INTEGRATION STATUS TEMPLATE

Mỗi integration point nếu có phải ghi:

Integration Point:
<name>

Contract:
<interface / DTO / route / expected behavior>

Current Implementation:
<AVAILABLE / NOT AVAILABLE>

Phase 1 Implementation:
<what Phase 1 has completed>

Integration Seam:
<where future backend connects>

Production Fake:
FORBIDDEN

Integration Status:
IMPLEMENTED / INTEGRATION-READY / INTEGRATED / PARTIAL / MISSING

Ví dụ:

Integration Point:
Moderator Dashboard Metrics

Contract:
ModeratorDashboardDTO

Current Implementation:
NOT AVAILABLE

Phase 1 Implementation:
Dashboard structure + placeholder state

Integration Seam:
Dashboard data provider

Production Fake:
FORBIDDEN

Integration Status:
INTEGRATION-READY
36. KHÔNG ĐƯỢC FAKE PRODUCTION DATA

Không tạo:

15 pending products
8 report cases
4 violations
2 escalations

chỉ để dashboard nhìn đẹp.

Được phép:

--
Chưa có dữ liệu
Not available

hoặc test double chỉ trong test environment.

37. PHASE 1 KHÔNG PHỤ THUỘC TEAMMATE ĐỂ IMPLEMENT

Bắt buộc đạt:

Phase 1 source code
       ↓
Build
       ↓
Unit/Integration tests
       ↓
Manual security test
       ↓
INTEGRATION-READY

Không có bước:

WAIT FOR TUẤN

hoặc:

WAIT FOR QUỐC ANH
38. FILE CHANGE REPORT

Sau khi code:

Files Created:
- ...

Files Modified:
- ...

Files Deleted:
NONE

Không ghi:

Updated Moderator files

phải ghi chính xác filename.

39. FINAL REPORT
1. Phase Status
PHASE 1 – MODERATOR FOUNDATION + SECURITY

Implementation Status:
IMPLEMENTED / PARTIAL / MISSING

Integration Status:
INTEGRATION-READY / INTEGRATED

Không dùng BLOCKED chỉ vì backend Phase sau chưa tồn tại.

2. Completed
Moderator Login
Moderator Dashboard
Moderator Layout
Moderator Menu
Moderator Route
Moderator Security
Moderator Login Redirect
Public Registration Moderator Protection
Locked Account Guard
Admin Access Regression
UI Reuse
Security Test
Route Matrix
3. Not Completed
- ...
4. Integration Points
Auto Moderation
Current:
NOT REQUIRED FOR PHASE 1

Integration:
Future Phase 2A

Status:
OUT OF SCOPE / INTEGRATION-READY
ReportCase
Current:
NOT REQUIRED FOR PHASE 1

Integration:
Future Phase 2C

Status:
OUT OF SCOPE / INTEGRATION-READY
Violation
Current:
NOT REQUIRED FOR PHASE 1

Integration:
Future Phase 3

Status:
OUT OF SCOPE / INTEGRATION-READY
KYC
Current:
NOT REQUIRED FOR PHASE 1

Status:
OUT OF SCOPE
AuditLog
Phase 1:
Không implement AuditLog backend.

Integration:
Future moderation/compliance phases.

Status:
OUT OF SCOPE / INTEGRATION-READY
40. FILES
Files Created:
- ...

Files Modified:
- ...

Files Deleted:
NONE
41. BUILD
mvn clean package:
PASS / FAIL
42. TEST
mvn test:
PASS / FAIL / NOT RUN
43. MANUAL TEST
Moderator Login:
PASS / FAIL

Moderator Dashboard:
PASS / FAIL

Moderator → /moderator/**:
PASS / FAIL

Moderator → /admin/**:
PASS / FAIL

Customer → /moderator/**:
PASS / FAIL

Vendor → /moderator/**:
PASS / FAIL

Anonymous → /moderator/**:
PASS / FAIL

Locked Moderator:
PASS / FAIL

Locked Admin:
PASS / FAIL

Public Register → MODERATOR:
PASS / FAIL

Admin Regression:
PASS / FAIL

UI Consistency:
PASS / FAIL
44. KNOWN ISSUES
Issue:
Current behavior:
Expected:
Affected file/module:
Severity:
Phase 1 impact:
Integration status:

Không ghi:

Tuấn chưa làm

Mà ghi:

Auto Moderation backend:
NOT AVAILABLE

Impact on Phase 1:
NONE

Phase 1 Status:
INTEGRATION-READY

Future Integration:
Phase 2A
45. PHASE 2 HANDOFF CONTRACT

Phase 1 bàn giao cho Phase 2:

Moderator Role
        ↓
Moderator Authentication
        ↓
Moderator Authorization
        ↓
/moderator/**
        ↓
Moderator Layout
        ↓
Moderator Menu
        ↓
Moderator Dashboard
        ↓
Account Status Guard
        ↓
Security Contract

Phase 2 có thể tích hợp:

Product Moderation
Review Moderation
ReportCase

mà không cần sửa lại nền tảng Moderator.

46. ĐIỀU KIỆN KẾT THÚC

Phase 1 được coi là:

IMPLEMENTED

khi:

Moderator Login hoạt động.
Redirect đúng /moderator/dashboard.
Moderator Dashboard hoạt động.
Moderator UI reuse Admin baseline.
Moderator Layout/Menu hoạt động.
/moderator/** được bảo vệ.
MODERATOR truy cập Moderator area.
CUSTOMER bị chặn.
VENDOR bị chặn.
Anonymous bị chặn.
MODERATOR bị chặn khỏi Admin.
Locked Moderator bị chặn.
Locked Admin được xử lý theo Security architecture.
Public Register không tự tạo Moderator.
Admin Login vẫn hoạt động.
Admin Dashboard vẫn hoạt động.
Admin UI không bị phá.
Build được kiểm tra.
Test được chạy nếu môi trường cho phép.
Manual Security Test hoàn thành.
Route Matrix được kiểm tra.
UI consistency được kiểm tra.
File thay đổi được báo cáo.
Integration points được ghi rõ.
Policy alignment được kiểm tra.
Không implement Auto Moderation.
Không implement KYC Backend.
Không implement ReportCase Backend.
Không implement Violation Backend.
Không implement Complaint Backend.
Không implement Product/Review Moderation business.
Không refactor lớn ngoài scope.
Không chờ implementation của developer khác để hoàn thành Phase 1.
47. FINAL ARCHITECTURE

Sau Phase 1:

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
          ┌───────────┴───────────┐
          ▼                       ▼
    Moderator UI             Security Layer
          │                       │
          ▼                       ▼
   Moderator Foundation      /moderator/**
                                  │
                                  ├── MODERATOR ✅
                                  ├── CUSTOMER   ❌
                                  ├── VENDOR     ❌
                                  ├── ANONYMOUS  ❌
                                  └── LOCKED     ❌

Và integration với các phase sau:

                    PHASE 1
                       │
                       │ Contract
                       ▼
              Moderator Foundation
                       │
          ┌────────────┼────────────┐
          ▼            ▼            ▼
       Phase 2A      Phase 2B      Phase 2C
       Product       Review        ReportCase
       Moderation    Moderation
          │            │            │
          └────────────┼────────────┘
                       ▼
                    Phase 3
                    Violation
                       │
                       ▼
                  Phase 4A–4C

Backend của Tuấn nằm sau integration seam, không nằm trước Phase 1:

Moderator Foundation
        │
        │ contract/interface
        ▼
Integration Seam
        ▲
        │
Backend capability
        │
   ┌────┴─────┐
   │          │
Auto Mod   ReportCase
Violation  KYC ...
48. FINAL STATUS
PHASE 1
Moderator Foundation + Security

Implementation:
INDEPENDENT

External Backend Dependency:
NONE FOR IMPLEMENTATION

Integration Dependency:
DEFERRED TO INTEGRATION SEAMS

Production Fake:
FORBIDDEN

Developer/Branch/Commit Blocking:
NONE

Expected Final State:
IMPLEMENTED + INTEGRATION-READY

NEXT:
PHASE 2