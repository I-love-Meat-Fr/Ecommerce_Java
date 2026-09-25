PHASE 4C – BANNER + MARKETING HARDENING

Implementation instruction: Read this Phase 4C file as the single source of truth.

Core independence rule:
“Tôi implement được phần của tôi bằng contract/interface hiện tại; backend của Tuấn là integration point, không phải prerequisite để bắt đầu hoặc hoàn thành implementation.”

Phase 4C phải được triển khai độc lập. Không chờ developer khác, branch khác, commit khác, backend implementation khác hoặc merge request khác.

Nếu một backend/integration bên ngoài chưa tồn tại:

Xác định contract/interface hiện tại.
Xác định integration seam.
Implement đầy đủ phần thuộc Phase 4C.
Test phần có thể test độc lập bằng mock/test double/fixture chỉ trong test.
Đánh dấu INTEGRATION-READY.
Không fake production behavior.
Khi backend thật tồn tại, kết nối tại integration seam và chuyển trạng thái sang INTEGRATED.

Backend của developer khác là integration point, không phải implementation blocker.

1. MỤC TIÊU

Phase 4C tập trung hoàn thiện, harden và kiểm thử Banner / PR / Marketing Content phía Admin sau các Phase trước.

Phase 4C:

≠ Phase 4 tổng
≠ Dashboard Core
≠ Voucher
≠ Phase 5 Full Integration

Phạm vi chính:

ADMIN

  ↓

BANNER / MARKETING

  ├── List
  ├── View
  ├── Create
  ├── Edit
  ├── Delete
  ├── Publish
  ├── Unpublish
  ├── Scheduling
  ├── Priority
  ├── Validation
  ├── Error Handling
  ├── Security
  ├── Audit Integration
  └── Regression Test

Mục tiêu cuối cùng:

Admin UI
   ↓
Existing Backend Contract / Integration Seam
   ↓
Service
   ↓
Repository
   ↓
Database

Phải hoạt động với dữ liệu và business rule thật khi backend tương ứng tồn tại.

Không coi:

UI hiển thị được

là hoàn thành.

Phase 4C phải thực hiện:

CODE
 ↓
BUILD
 ↓
TEST
 ↓
REGRESSION
 ↓
REPORT
2. CORE INDEPENDENCE RULE

Đây là nguyên tắc bắt buộc của Phase 4C.

Phase 4C phải có khả năng được triển khai độc lập.

Không chờ:
- Developer khác
- Branch khác
- Commit khác
- Backend implementation khác
- Merge Request khác

Nếu Banner/Marketing backend hoặc AuditLog integration của developer khác chưa tồn tại:

KHÔNG:
- Fake production data
- Fake production status
- Fake publish
- Fake audit event
- Hard-code response
- Rewrite backend ownership khác

PHẢI:
- Xác định contract
- Xác định integration seam
- Implement phần Admin thuộc Phase 4C
- Test bằng test double trong test scope
- Đánh dấu INTEGRATION-READY

Ví dụ:

Admin Banner UI
       ↓
BannerGateway / existing BannerService contract
       ↓
[Backend thật chưa tồn tại]

Phase 4C vẫn có thể hoàn thành phần của mình:

UI
Controller integration
DTO mapping
validation
error handling
security
tests
integration seam

và trạng thái:

IMPLEMENTED
+
INTEGRATION-READY

Không được báo:

BLOCKED BY TUẤN

chỉ vì backend của Tuấn chưa tồn tại.

3. ĐỊNH NGHĨA TRẠNG THÁI

Phase 4C sử dụng các trạng thái sau.

IMPLEMENTED

Phần thuộc ownership Phase 4C đã code xong và được test độc lập.

INTEGRATION-READY

Contract/integration seam đã hoàn chỉnh nhưng implementation bên ngoài chưa kết nối hoặc chưa tồn tại.

Đây không phải blocker.

INTEGRATED

Implementation thật bên ngoài đã được kết nối và integration/E2E đã verify.

PARTIAL

Phần thuộc ownership Phase 4C vẫn chưa hoàn thành.

MISSING

Required capability chưa có implementation.

WRONG

Implementation tồn tại nhưng behavior sai business/security/technical contract.

OUT OF SCOPE

Không thuộc Phase 4C.

BLOCKED

Chỉ sử dụng khi có hard environmental blocker thực sự, ví dụ:

Repository không build được do baseline corruption
Môi trường không thể chạy test
Database/test infrastructure bị hỏng nghiêm trọng
Credential bắt buộc không tồn tại và không có test seam thay thế

Không sử dụng BLOCKED chỉ vì:

Backend developer khác chưa làm
AuditLog chưa implement
Security implementation khác chưa merge

Trong các trường hợp đó:

INTEGRATION-READY
4. VỊ TRÍ CỦA PHASE 4C
GATE 0
  ↓
Phase 1 — Moderator Access, Layout, Dashboard Foundation
  ↓
Phase 2A — Product Moderation
  ↓
Phase 2B — Review Moderation
  ↓
Phase 2C — ReportCase / Escalation Writer / Moderator History
  ↓
Phase 3A — Admin Escalation & Enforcement + Audit Writer
  ↓
Phase 3B — KYC Monitoring
  ↓
Phase 3C — Violation & AuditLog Query
  ↓
Phase 4A — Dashboard Core
  ↓
Phase 4B — WEB Voucher Hardening
  ↓
Phase 4C — Banner + Marketing Hardening

Phase 4C:

≠ Phase 4A Dashboard
≠ Phase 4B WEB Voucher
≠ Phase 5 Full Integration

Không kéo các phần chưa thuộc Phase 4C vào task.

5. SCOPE CHÍNH

Phase 4C chỉ tập trung:

1. Banner Admin UI
2. Banner CRUD integration
3. Banner Publish / Unpublish
4. Banner Scheduling
5. Banner Priority
6. Banner Validation
7. Banner Status
8. Banner Filter / Search / Pagination nếu contract hỗ trợ
9. Marketing / PR integration hiện có
10. Admin Security
11. Method Security
12. Error Handling
13. Confirmation Action
14. Double Submit Protection
15. AuditLog integration
16. Backend ↔ UI verification
17. Regression Test
18. Build
19. Final Report

Nếu project chỉ có Banner và chưa có module PR riêng:

Không tạo CMS/Marketing platform mới.

Chỉ harden Banner functionality thực sự tồn tại.
6. OUT OF SCOPE

TUYỆT ĐỐI không mở rộng Phase 4C sang:

Admin Dashboard Core
WEB Voucher
Shop Voucher
Customer Banner UI
Vendor Marketing UI
Customer UI
Vendor UI
Product Moderation
Review Moderation
ReportCase Backend
Violation Backend
KYC Provider
KYC Verification
Escalation Backend
Enforcement Backend
AuditLog Backend Core
Payment Core
Commission Core
Settlement Core
Payout Core
Refund Core
Shipping Core
Cart Core
Stock Core
Auto Moderation Engine
Complaint Backend
Phase 5 Full Integration Test

Không refactor module trên chỉ để Banner dễ implement hơn.

Nếu phát hiện bug ngoài scope:

OUT OF SCOPE

hoặc:

EXTERNAL INTEGRATION / KNOWN ISSUE
7. OWNERSHIP VÀ INTEGRATION POINTS
7.1 User-owned Phase 4C

Phần thuộc Phase 4C:

Admin Banner UI
Admin Marketing UI nếu đã tồn tại
Admin PR UI nếu đã tồn tại
Banner Controller integration
Banner Service integration nếu thuộc Admin scope
Banner DTO mapping
Banner Template
Banner CSS
Banner JS
Form validation
Filter / pagination integration
Publish / Unpublish UI
Scheduling UI
Priority UI
Error handling
Confirmation
Double-submit protection
Admin security integration
Tests
Regression
Integration adapters/seams
7.2 External integration points

Các phần sau không phải prerequisite để bắt đầu Phase 4C:

AuditLog Backend
Security Core
Banner backend implementation nếu chưa tồn tại
Storage implementation nếu thuộc module khác
Customer Banner implementation
Marketing backend implementation nếu thuộc module khác

Nếu implementation bên ngoài chưa có:

Contract:
Integration Seam:
Current External Implementation:
Integration Required:
Status:
8. KHÔNG DÙNG PERSON-BASED DEPENDENCY

Không viết:

DEPENDENCY – Anh Tuấn
DEPENDENCY – Quốc Anh
BLOCKED – chờ Tuấn

như một điều kiện để bắt đầu/hoàn thành Phase 4C.

Thay bằng:

External Integration:
AuditLog Backend

Contract:
Existing AuditLog interface/service contract

Integration Seam:
Banner action → AuditLog adapter

Current External Implementation:
MISSING

Integration Required:
Create/Edit/Delete/Publish/Unpublish audit events

Implementation Status:
INTEGRATION-READY

Integration Status:
NOT INTEGRATED

Owner có thể được ghi ở phần informational report nếu cần, nhưng không biến owner thành blocker.

9. QUY TẮC CONTRACT-FIRST

Trước khi tạo class/interface mới:

1. Tìm existing Entity
2. Tìm existing DTO
3. Tìm existing Service
4. Tìm existing Repository
5. Tìm existing Controller
6. Tìm existing interface/gateway
7. Tìm existing Security
8. Tìm existing Audit integration

Nếu đã có contract tương đương:

REUSE

Không tạo:

BannerService2
BannerControllerV2
MarketingServiceNew
BannerGatewayV2

chỉ để tránh dùng code hiện tại.

Nếu không có integration seam tương ứng và Phase 4C thực sự cần seam:

Tạo minimal contract/interface tại application boundary.

Ví dụ minh họa:

interface AdminBannerGateway {
    BannerPage getBanners(...);
    BannerDetail getBanner(...);
    BannerResult create(...);
    BannerResult update(...);
    BannerResult delete(...);
    BannerResult publish(...);
    BannerResult unpublish(...);
}

Đây chỉ là ví dụ.

Phải dùng project-equivalent nếu project đã có contract khác.

Không được tạo duplicate domain service/entity.

10. GIT BASELINE

Trước khi sửa:

git status
git branch
git log --oneline --decorate -10

Ghi:

Current branch:
Current HEAD:
Working tree:
Uncommitted changes:

Nếu working tree có thay đổi:

KHÔNG RESET
KHÔNG STASH
KHÔNG XÓA

Không làm mất code hiện tại.

Sau khi code:

git status
git diff --stat
git diff

Không:

commit
push
merge
reset
stash

nếu chưa được yêu cầu.

11. ĐỌC CÁC PHASE TRƯỚC

Kiểm tra trạng thái thực tế:

Phase 1
Phase 2
Phase 3
Phase 4A
Phase 4B

Nhưng đây là readiness/contract check, không phải prerequisite cứng.

Đặc biệt kiểm tra:

Admin Layout
Admin Security
Banner
Marketing
PR
Storage / Image handling
AuditLog integration

Không giả định Phase trước đã hoàn thành 100%.

Nếu một implementation bên ngoài chưa có:

Current status:
MISSING

Contract:
...

Integration seam:
...

Required:
...

Implementation status:
INTEGRATION-READY

Integration status:
NOT INTEGRATED
12. KIỂM TRA BANNER HIỆN TẠI

Tìm:

Banner Entity
Banner Repository
Banner Service
Banner Controller
Banner DTO
Banner Mapper
Banner Template
Banner CSS
Banner JS
Banner Status
Banner Image handling
Banner scheduling logic
Banner priority logic
AuditLog integration
Security

Kiểm tra:

CRUD
Publish
Unpublish
startAt
endAt
priority
status
image
target URL
validation
pagination
audit logging

Phân loại:

EXISTS + CORRECT
EXISTS + BUG
EXISTS + INCOMPLETE
MISSING
EXTERNAL INTEGRATION
OUT OF SCOPE

Không tạo duplicate implementation.

13. ADMIN UI

Phải đọc:

Admin Layout
Admin Sidebar
Admin Header
Admin Table
Admin Form
Admin Modal
Admin Button
Admin Alert
Admin Badge
Admin Pagination
Admin Filter
Admin CSS
Admin JS

Nguyên tắc:

REUSE ADMIN UI

Nếu component tồn tại:

REUSE

Nếu thiếu và thực sự cần:

ADD SMALL COMPONENT

Không tạo design system mới.

14. BANNER DOMAIN

Không tự giả định Entity có tất cả field.

Có thể tồn tại:

id
title
image
imageUrl
targetUrl
status
startAt
endAt
priority
createdAt
updatedAt

Nhưng phải đọc code thật.

Chỉ dùng field thực tế.

15. BANNER LIST / VIEW

Admin phải hỗ trợ:

List
View

nếu contract/backend hiện tại hỗ trợ.

Hiển thị đúng field thực tế:

Title
Image
Target
Status
Start At
End At
Priority
Created At
Updated At
Actions

Không tự thêm field DB chỉ để UI đẹp hơn.

16. SEARCH / FILTER

Nếu backend/contract hỗ trợ:

Search
Status
Date
Priority

Chỉ sử dụng status thực tế.

Ví dụ:

Draft
Scheduled
Published
Unpublished
Expired

chỉ khi domain hiện tại có những trạng thái đó.

Flow:

Filter UI
    ↓
HTTP Request
    ↓
Controller
    ↓
Service / Gateway
    ↓
Repository / Backend
    ↓
Database

Không filter giả trên browser nếu backend pagination/query đang được sử dụng.

17. PAGINATION

Nếu backend có pagination:

REUSE

Không load toàn bộ collection chỉ để render một page.

Nếu backend chưa có:

Không tự refactor lớn chỉ vì pagination.


Chỉ bổ sung nếu thực sự cần cho Phase 4C và phù hợp architecture hiện tại.

18. CREATE

Nếu backend contract cho phép:

Admin
 ↓
Create Banner
 ↓
Backend Contract
 ↓
Service
 ↓
Repository
 ↓
Database

Form dùng đúng field hiện tại.

Có thể gồm:

Title
Image
Target URL
Start At
End At
Priority
Status

chỉ khi tồn tại thực tế.

19. CREATE VALIDATION

Tối thiểu kiểm tra các field thực tế:

Title không rỗng
Image hợp lệ nếu bắt buộc
URL hợp lệ nếu có
Start At < End At nếu có
Priority hợp lệ nếu có
Status hợp lệ nếu được nhập

Frontend validation:

UX

Backend validation:

Business Rule Enforcement

Không chỉ validate bằng JavaScript.

20. EDIT

Khi edit:

Admin
 ↓
Edit
 ↓
Backend Contract
 ↓
Service
 ↓
Database

Không vô tình reset:

Image
Start At
End At
Priority
Status
Target URL

nếu những field đó không được chỉnh sửa.

Đặc biệt xác định:

PATCH / partial update

hay:

PUT / full replacement

theo contract hiện tại.

21. DELETE

Nếu business rule cho phép:

Delete
 ↓
Confirmation
 ↓
Backend

Nếu backend reject:

UI hiển thị backend error.

Không bypass.

Không xóa trực tiếp record bằng JavaScript.

Nếu backend delete chưa tồn tại:

Phase 4C có thể implement UI/contract integration seam.
Status:
IMPLEMENTED + INTEGRATION-READY

Không fake successful deletion.

22. PUBLISH

Nếu contract có Publish:

Admin
 ↓
Publish
 ↓
Controller / Gateway
 ↓
Service
 ↓
Database
 ↓
Refresh

Status sau action phải lấy từ backend.

Không làm:

status = "PUBLISHED";

để giả lập thành công.

Nếu backend Publish chưa tồn tại:

Production:
Không fake.

Test:
Mock contract được phép.

Status:
INTEGRATION-READY
23. UNPUBLISH

Flow:

Admin
 ↓
Unpublish
 ↓
Backend
 ↓
Database
 ↓
Refresh

Status phải lấy lại từ backend.

Không fake.

24. CONFIRMATION

Các action có khả năng thay đổi dữ liệu:

Delete
Publish
Unpublish

phải sử dụng confirmation nếu phù hợp với Admin UI pattern/business rule hiện tại.

Không tạo cơ chế confirmation mới nếu project đã có modal/component dùng chung.

25. STATUS

Không tạo enum mới nếu đã có:

BannerStatus
MarketingStatus
ContentStatus

Ví dụ backend dùng:

DRAFT
PUBLISHED
UNPUBLISHED

thì map đúng.

Nếu backend dùng:

ACTIVE
INACTIVE

thì dùng đúng.

Không rename enum toàn project.

26. SCHEDULING

Scheduling chỉ được implement nếu backend/domain hiện tại có scheduling hoặc Phase 4C contract yêu cầu.

Nếu có:

startAt
endAt

phải phân biệt đúng:

now < startAt
startAt <= now <= endAt
now > endAt

theo business rule hiện tại.

Không tự tạo lifecycle mới.

Không dùng:

setTimeout(...)

để giả lập backend scheduling.

27. SCHEDULE VALIDATION

Nếu có startAt và endAt:

startAt < endAt

nếu business rule yêu cầu.

Không cho:

startAt == endAt
startAt > endAt

nếu rule không cho phép.

Kiểm tra timezone hiện tại.

Không đổi timezone toàn hệ thống.

28. PRIORITY

Nếu backend có priority:

View
Create
Edit

và sort nếu backend/business rule hỗ trợ.

Không tự thay đổi Customer Banner display logic.

29. PRIORITY VALIDATION

Nếu contract quy định integer:

1
2
3

được phép.

abc
1.5

không được phép.

Nếu đã có min/max:

Reuse existing rule

Nếu chưa có:

Không invent business rule.
30. IMAGE

Kiểm tra cơ chế hiện tại:

image
imageUrl
upload
storage

Nếu đã có upload:

REUSE

Không tạo storage architecture mới.

Nếu upload file, reuse validation hiện tại:

MIME/type
size
extension
required

Không tự tăng upload limit.

Nếu storage thuộc external module:

Integration Point

không phải person-based blocker.

31. TARGET URL

Nếu có targetUrl:

Validate URL

theo rule hiện tại.

Không cho scheme nguy hiểm nếu field được render trực tiếp:

javascript:
data:

Không render raw HTML từ input Admin nếu không cần.

32. MARKETING / PR

Kiểm tra module thực tế:

PR
Marketing
Promotion Content
Banner

Nếu module riêng đã tồn tại:

REUSE EXISTING MODULE

Nếu chỉ có Banner:

Phase 4C chỉ harden Banner.

Không tạo:

CMS
Marketing Automation
Campaign Engine
Email Marketing
Ad Platform
33. MARKETING STATUS / ROUTING

Nếu Marketing Content có status riêng:

READ EXISTING ENUM

Không invent status.

Nếu Admin sidebar đã có:

Banner
PR
Marketing

giữ route hiện tại.

Không đổi URL chỉ để đẹp hơn.

Nếu route không tồn tại:

MISSING

hoặc:

OUT OF SCOPE

tùy nguyên nhân.

Không tạo dead navigation.

34. ADMIN SECURITY

Các route Banner/Marketing thực tế phải được bảo vệ.

Ví dụ minh họa:

/admin/banner/**
/admin/banners/**
/admin/pr/**
/admin/marketing/**

Nhưng phải dùng route thực tế của project.

Mục tiêu:

ADMIN → ALLOWED
Others → DENIED

Không thay đổi role model.

35. SECURITY MATRIX
User	Banner	Marketing
Anonymous	DENIED	DENIED
CUSTOMER	DENIED	DENIED
VENDOR	DENIED	DENIED
MODERATOR	DENIED	DENIED
ADMIN	ALLOWED	ALLOWED

Nếu kiến trúc hiện tại có ngoại lệ:

Ghi rõ contract hiện tại.

Không tự thay đổi role.

36. METHOD SECURITY

Nếu project dùng:

@PreAuthorize

hoặc method-security tương đương, kiểm tra:

createBanner
updateBanner
deleteBanner
publishBanner
unpublishBanner

Không chỉ dựa vào URL nếu architecture đang sử dụng method security.

Nếu lỗi thuộc Security Core:

External Integration / Security Core
Status:
INTEGRATION-READY hoặc PARTIAL

Không rewrite toàn bộ authentication.

37. LOCKED ADMIN

Theo Security Core hiện tại:

ADMIN active
 → Banner
 → ALLOWED

và:

ADMIN locked
 → Banner
 → DENIED

Nếu Security Core chưa hỗ trợ test:

Integration Status:
NOT INTEGRATED

Không ghi PASS giả.

Không cần chờ Security Core để implement Admin Banner UI/security seam.

38. ERROR HANDLING

Xử lý theo backend contract:

400
401
403
404
409
500

nếu backend trả về.

Không hiển thị:

raw stack trace

hoặc error thô.

Dùng message phù hợp với Admin UI pattern.

Ví dụ:

Không có quyền thực hiện thao tác này.
Banner không tồn tại.
Dữ liệu không hợp lệ.
Banner đang được sử dụng và không thể xóa.
Không thể cập nhật Banner. Vui lòng thử lại.
39. EMPTY STATE

Khi:

0 Banner

phải hiển thị empty state theo Admin UI pattern.

Không:

blank page
NullPointerException

Nếu backend chưa tồn tại:

Integration unavailable

phải được phân biệt với:

valid empty dataset

Không biến lỗi integration thành 0 Banner.

40. NOT FOUND

Nếu:

/admin/banner/{id}

không tồn tại:

404 / Not Found UI

theo architecture hiện tại.

Không để:

500
NullPointerException
41. DOUBLE SUBMIT

Các action:

Create
Edit
Delete
Publish
Unpublish

phải tránh double submit.

Có thể reuse:

Disable button while submitting

hoặc cơ chế hiện tại.

Frontend protection không thay thế backend idempotency/business validation nếu backend cần.

42. FORM ERROR

Khi validation fail:

Giữ lại input hợp lệ

nếu architecture hỗ trợ.

Hiển thị lỗi gần field:

Title:
Không được để trống.

Start At:
Phải nhỏ hơn End At.

Target URL:
URL không hợp lệ.

Không chỉ:

Something went wrong.

nếu backend đã trả validation details.

43. CONCURRENCY / CONFLICT

Nếu backend có:

409 Conflict
version
updatedAt
optimistic locking

phải xử lý đúng.

Ví dụ:

Banner đã được cập nhật bởi người khác.
Vui lòng tải lại dữ liệu trước khi chỉnh sửa.

Không silently overwrite dữ liệu mới hơn.

Nếu backend chưa có conflict mechanism:

Không tự thêm distributed locking.
44. AUDITLOG INTEGRATION

Phase 4C:

KHÔNG VIẾT LẠI AUDITLOG BACKEND CORE.

Nhưng phải xác định integration seam cho:

Create Banner
Edit Banner
Delete Banner
Publish Banner
Unpublish Banner

Nếu AuditLog contract tồn tại:

Reuse

Nếu backend AuditLog implementation chưa tồn tại:

Admin integration:
IMPLEMENTED

Audit integration:
INTEGRATION-READY

External integration:
NOT INTEGRATED

Không fake audit event.

45. AUDIT CONTRACT

Mỗi action quan trọng nên map tới audit contract hiện tại:

Actor
Action
Resource
Resource ID
Timestamp
Reason / metadata nếu contract có

Không tự tạo AuditLog schema mới nếu project đã có.

Nếu contract không hỗ trợ Banner action:

Define integration seam only if required.

Không tự rewrite AuditLog backend ownership.

46. DATABASE SAFETY

TUYỆT ĐỐI không:

Drop collection
Reset database
Clear Banner collection
Mass delete Banner
Destructive migration

Test data:

Controlled test data

Không sử dụng production reset.

47. BACKEND ↔ UI CONTRACT

Với mỗi action:

List
View
Create
Edit
Delete
Publish
Unpublish

phải xác định:

UI
 ↓
Request
 ↓
Controller / Gateway
 ↓
Service
 ↓
Repository / External Backend
 ↓
Database

Nếu backend thật chưa tồn tại:

UI
 ↓
Contract / Integration Seam
 ↓
Test Double

chỉ trong test.

Không claim production integration đã hoàn thành.

48. KHÔNG HARD-CODE PRODUCTION DATA

Không hard-code:

Banner title
Banner ID
Banner status
Banner priority
Banner count

trong:

HTML
JavaScript
Thymeleaf

nếu đó là dữ liệu backend.

49. KHÔNG FAKE STATUS

Không:

status = "PUBLISHED";

để tạo cảm giác thành công.

Phải:

POST /publish
      ↓
Backend / Contract
      ↓
Database
      ↓
GET /list
      ↓
UI

Nếu backend chưa có:

Không fake production response.
50. KHÔNG FAKE SCHEDULING

Không:

setTimeout(...)

để giả lập business scheduling.

Backend chịu trách nhiệm nếu scheduling thuộc backend.

UI chỉ:

Display
Create
Edit

theo contract.

51. CUSTOMER BANNER

Không tự sửa:

Customer Home
Customer Banner Carousel
Customer Landing Page

trừ khi integration contract yêu cầu thay đổi trực tiếp.

Bug Customer Banner:

OUT OF SCOPE

hoặc:

KNOWN ISSUE
52. BANNER LIFECYCLE

Không tự invent lifecycle.

Nếu backend:

DRAFT
SCHEDULED
PUBLISHED
EXPIRED
UNPUBLISHED

map đúng.

Nếu:

ACTIVE
INACTIVE

map đúng.

Nếu chưa rõ:

READ ENUM
READ SERVICE
READ REPOSITORY

Không tự tạo:

SCHEDULED
EXPIRED

chỉ vì UI muốn có.

53. DELETE BUSINESS RULE

Nếu Banner đang:

PUBLISHED

không tự giả định delete được.

Kiểm tra backend contract/business rule.

Có thể:

Delete allowed

hoặc:

Delete rejected

Cả hai đều hợp lệ nếu contract quy định.

UI phải phản ánh backend.

54. PUBLISH BUSINESS RULE

Kiểm tra contract:

Can draft publish?
Can scheduled publish manually?
Can expired publish?
Can unpublished publish?

Không tự quyết định.

Nếu backend reject:

UI hiển thị lỗi.

Không bypass.

55. TIMEZONE

Kiểm tra:

Java timezone
MongoDB date
Browser timezone
Thymeleaf formatting

Không đổi timezone toàn project.

Đảm bảo:

Admin input
 ↓
Backend
 ↓
Database
 ↓
UI

không lệch ngoài business rule.

56. IMAGE / FILE SECURITY

Nếu upload:

Validate MIME/type
Validate size
Validate extension

theo cơ chế hiện tại.

Không chỉ tin filename extension.

Không cho arbitrary executable file.

Nếu upload/storage security thuộc module khác:

External Integration

không phải blocker của toàn Phase 4C.

57. ADMIN UI CONSISTENCY

Reuse:

Admin table
Admin form
Admin button
Admin badge
Admin modal
Admin alert
Admin pagination

Không thay đổi global:

typography
spacing
sidebar
header

trừ bug trực tiếp thuộc Banner và thay đổi nhỏ là cần thiết.

58. RESPONSIVE

Kiểm tra:

Desktop
Tablet
Narrow viewport

Đảm bảo:

Không overflow bất thường
Image không phá layout
Button không mất
Table không phá container
Modal không vượt viewport nghiêm trọng

Không redesign mobile toàn Admin.

59. TEST ARCHITECTURE

Ưu tiên:

Unit Test
Service Test
Controller Test
Integration Test
Security Test
Manual Test
Regression Test

Nếu external backend chưa tồn tại:

Mock/Test Double

được phép chỉ trong test.

Không đưa mock vào production runtime.

60. TEST UNIT / SERVICE

Kiểm tra:

Create Banner
Invalid Banner
Update Banner
Delete Banner
Publish Banner
Unpublish Banner
Schedule validation
Priority validation
URL validation
Permission

Chỉ thêm test mới nếu behavior chưa được cover.

61. TEST BANNER LIST
Admin
 → Banner
 → List

Expected:

Dữ liệu đúng theo backend contract.

Nếu external backend chưa tích hợp:

Contract test:
PASS

Production integration:
NOT INTEGRATED

Không gọi PASS cho integration thật.

62. TEST CREATE
Admin
 → Add Banner
 → Input valid data
 → Save

Expected khi backend thật tồn tại:

Request thành công
Database có record
UI hiển thị record

Nếu backend chưa tồn tại:

Admin-side implementation:
PASS

External integration:
INTEGRATION-READY
63. TEST INVALID CREATE

Kiểm tra các field thực tế:

Title empty
Invalid URL
Start At > End At
Invalid priority
Missing required image

Expected:

Validation error
Không tạo record sai
64. TEST EDIT
Admin
 → Edit Banner
 → Change field
 → Save

Expected:

Database updated
UI updated

Kiểm tra field không chỉnh sửa không bị reset.

65. TEST DELETE
Admin
 → Delete
 → Confirmation
 → Confirm

Expected:

Deleted

hoặc:

Backend reject đúng business rule

Không coi backend reject đúng rule là lỗi.

66. TEST PUBLISH
Unpublished / Draft
 → Publish

Expected:

Backend status changed
UI refresh
Status đúng

Nếu backend chưa tồn tại:

Publish integration:
INTEGRATION-READY

Không fake PUBLISHED.

67. TEST UNPUBLISH
Published
 → Unpublish

Expected:

Backend status changed
UI refresh
Status đúng
68. TEST SCHEDULE

Nếu applicable:

Future startAt
Current active period
Expired endAt
Invalid date range

Expected:

Behavior đúng business rule.

Nếu backend chưa có scheduling:

Do not fake.

Status:
INTEGRATION-READY
69. TEST PRIORITY

Nếu có priority:

Banner A = 1
Banner B = 2

Kiểm tra theo backend rule:

List
Sort
Display

Không tự thay đổi Customer behavior.

70. TEST FILTER / PAGINATION

Filter:

Status
Search
Date
Priority

kiểm tra:

Filter UI
 ↓
Request parameter
 ↓
Backend query
 ↓
Correct result

Pagination:

Page 1
Page 2
Next
Previous
Page size

Expected:

Correct records
Correct total
No unexpected duplicate/missing records
71. TEST EMPTY / NOT FOUND

Empty:

0 Banner

Expected:

Correct empty state

Not found:

Unknown Banner ID

Expected:

404 / Not Found UI

Không:

500
NullPointerException
72. TEST ERROR HANDLING

Nếu có thể reproduce:

400
403
404
409
500

Expected:

User-friendly error
No stack trace exposure
73. TEST SECURITY

Security matrix:

User	List	Create	Edit	Delete	Publish	Unpublish
Anonymous	DENIED	DENIED	DENIED	DENIED	DENIED	DENIED
CUSTOMER	DENIED	DENIED	DENIED	DENIED	DENIED	DENIED
VENDOR	DENIED	DENIED	DENIED	DENIED	DENIED	DENIED
MODERATOR	DENIED	DENIED	DENIED	DENIED	DENIED	DENIED
ADMIN	ALLOWED	ALLOWED	ALLOWED	ALLOWED	ALLOWED	ALLOWED

Nếu contract thực tế có restriction khác:

Document actual contract.
74. TEST LOCKED ADMIN
ADMIN active
 → Banner
 → ALLOWED
ADMIN locked
 → Banner
 → DENIED

Nếu Security Core chưa tích hợp:

Security integration:
NOT INTEGRATED

Không ghi PASS giả.

75. TEST AUDITLOG

Nếu AuditLog backend đã tích hợp:

Create Banner
Edit Banner
Delete Banner
Publish Banner
Unpublish Banner

Expected:

Audit event đúng action
Actor đúng
Target đúng
Timestamp hợp lệ

Nếu backend chưa tích hợp:

Audit contract:
READY

Audit integration:
NOT INTEGRATED

Không fake log.

76. TEST DOUBLE SUBMIT

Thử double-click:

Save
Publish
Delete

Expected:

Không tạo duplicate operation ngoài business rule.
77. TEST IMAGE

Nếu có image:

Valid image
Invalid type
Oversized image
Missing image

chỉ test validation thực tế của project.

78. TEST TARGET URL

Nếu có target URL:

Valid URL
Invalid URL
Empty URL nếu optional
Unsafe scheme

Expected:

Validation đúng business/security rule.
79. TEST TIMEZONE

Nếu scheduling tồn tại:

Admin input
 ↓
Backend
 ↓
Database
 ↓
UI

Expected:

Không lệch timezone ngoài rule hiện tại.
80. ADMIN REGRESSION

Sau Phase 4C:

Admin Login
Dashboard
User
Shop
Category
Order
Product
Review
KYC Monitoring
Violation
Escalation
AuditLog
WEB Voucher
Banner

Không để Banner changes phá:

Admin Layout
Sidebar
Header
Security
Existing routes

Không biến thành full marketplace regression.

81. BUILD

Sau code:

mvn test

và:

mvn clean package

Ghi:

TEST: PASS / FAIL

BUILD: PASS / FAIL

Nếu fail:

Error:
Root cause:
Affected file/module:
External integration:
Real blocker:

Không gán lỗi cho developer khác nếu chưa xác định nguyên nhân.

82. FILE SCOPE

Có thể thay đổi nếu cần:

Admin Banner Controller
Admin Banner Service
Banner DTO
Banner Repository
Banner Entity
Admin Banner Template
Admin Marketing Template
Admin CSS
Admin JS
Validation
Security integration
Integration adapter/gateway
Tests

Chỉ thay đổi khi thực sự cần.

Không sửa:

Payment
Order
Commission
Settlement
Payout
Refund
Shipping
Customer UI
Vendor UI
KYC Provider
Violation Backend
ReportCase Backend
Complaint Backend
Auto Moderation
83. BUSINESS RULE CHECK

Bắt buộc:

[ ] Banner status lấy từ backend/contract
[ ] Publish không chỉ đổi UI
[ ] Unpublish không chỉ đổi UI
[ ] Scheduling không fake bằng JavaScript
[ ] Priority không fake
[ ] Start At < End At nếu business rule yêu cầu
[ ] Target URL được validate
[ ] Image validation dùng backend rule hiện tại
[ ] Admin-only access được giữ
[ ] Locked Admin được xử lý theo Security Core
[ ] AuditLog không fake
[ ] Customer Banner logic không bị tự ý thay đổi
[ ] Không tạo CMS lớn ngoài scope
[ ] Không tạo duplicate service/entity
[ ] Không phụ thuộc người khác để hoàn thành phần user-owned
84. PHÂN LOẠI CODE

Trong quá trình làm:

EXISTS + CORRECT
EXISTS + BUG
EXISTS + INCOMPLETE
MISSING
INTEGRATION-READY
INTEGRATED
WRONG
OUT OF SCOPE

Ví dụ:

Banner CRUD
→ EXISTS + CORRECT

Publish UI
→ IMPLEMENTED

Publish backend
→ MISSING

Publish integration seam
→ INTEGRATION-READY

AuditLog backend
→ NOT INTEGRATED

Customer Banner
→ OUT OF SCOPE
85. MASTER IMPLEMENTATION / INTEGRATION TABLE

Final report nên có bảng:

#	Capability	Contract	Current Source/API	User-Owned Code	Security	DB	Test	Implementation Status	Integration Status	Integration Seam	Notes
1	Banner List	Existing contract	Actual source	Admin list	ADMIN	Read	Unit/UI	IMPLEMENTED	INTEGRATED / READY	Existing API	
2	Create	Existing contract	Actual service	Form/controller	ADMIN	Write	Unit/integration	IMPLEMENTED	INTEGRATED / READY	Existing service	
3	Edit	Existing contract	Actual service	Form/controller	ADMIN	Write	Unit/integration	IMPLEMENTED	INTEGRATED / READY	Existing service	
4	Delete	Existing contract	Actual service	Action/confirmation	ADMIN	Write	Integration	IMPLEMENTED	INTEGRATED / READY	Existing service	
5	Publish	Existing contract	Actual service	Action	ADMIN	Write	Integration	IMPLEMENTED	READY / INTEGRATED	Publish seam	
6	Unpublish	Existing contract	Actual service	Action	ADMIN	Write	Integration	IMPLEMENTED	READY / INTEGRATED	Unpublish seam	
7	Audit	Audit contract	Existing audit service	Adapter	ADMIN	Write	Contract test	IMPLEMENTED	READY / INTEGRATED	Audit seam	

Không đánh INTEGRATED nếu chưa thực sự verify backend.

86. EXTERNAL INTEGRATION RECORD

Mỗi integration chưa có implementation phải ghi:

External Integration:
Contract:
Integration Seam:
Current External Implementation:
Integration Required:
Implementation Status:
Integration Status:
Real Blocker:
Next Action:

Ví dụ:

External Integration:
AuditLog Banner Events

Contract:
Existing AuditLog service contract

Integration Seam:
Banner action → AuditLog adapter

Current External Implementation:
Audit event support chưa tồn tại

Integration Required:
Create/Edit/Delete/Publish/Unpublish

Implementation Status:
IMPLEMENTED

Integration Status:
INTEGRATION-READY

Real Blocker:
NONE

Next Action:
Connect actual AuditLog implementation when available
87. REPORT – GIT BASELINE
## 1. Git Baseline

Current branch:
HEAD before:
HEAD after:
Working tree:
Uncommitted changes:
88. REPORT – DONE

Chỉ đánh [x] nếu phần user-owned đã thực sự implement/test.

## DONE

### Banner

- [x] List
- [x] View
- [x] Create
- [x] Edit
- [x] Delete
- [x] Publish
- [x] Unpublish
- [x] Scheduling
- [x] Priority
- [x] Validation
- [x] Filter
- [x] Pagination
- [x] Error handling
- [x] Security
- [x] Integration seam
- [x] Audit integration seam

### Marketing / PR

- [x] Existing module verified
- [x] Admin integration
- [x] Validation
- [x] Security
- [x] Regression

Không đánh [x] cho capability không tồn tại hoặc chưa thuộc implementation scope.

89. REPORT – PARTIAL
## PARTIAL

Format:

Feature:
Completed:
Remaining:
File:
Reason:
Implementation Status:
Integration Status:
90. REPORT – MISSING
## MISSING

Format:

Feature:
Expected:
Current:
Reason:
Integration Seam:
Implementation Status:
Integration Status:
Real Blocker:

Không tự động ghi BLOCKED.

91. REPORT – WRONG
## WRONG

Format:

Feature:
Current behavior:
Expected behavior:
File:
Impact:
Fixed:

Ví dụ:

Feature:
Banner Publish

Current behavior:
Frontend đổi badge thành Published.

Expected behavior:
Backend phải cập nhật status.

Impact:
UI có thể hiển thị Published giả.

Fixed:
YES / NO
92. REPORT – INTEGRATION-READY
## INTEGRATION-READY

Format:

Integration:
Contract:
Integration Seam:
User-Owned Implementation:
External Implementation:
Current Integration Status:
Test Status:
What remains:

Ví dụ:

Integration:
Banner AuditLog

Contract:
Existing AuditLog contract

Integration Seam:
Banner action → Audit adapter

User-Owned Implementation:
Completed

External Implementation:
Not available

Current Integration Status:
NOT INTEGRATED

Test Status:
Contract test PASS

What remains:
Connect real AuditLog backend
93. REPORT – BLOCKED

Chỉ dùng nếu có hard blocker thật.

## BLOCKED

Format:

Blocker:
Environment:
Evidence:
Affected scope:
Why contract/test seam cannot proceed:
Work attempted:
Next action:

Không viết:

Blocked by Tuấn
Blocked by Quốc Anh

nếu vẫn có thể implement/test bằng contract.

94. REPORT – OUT OF SCOPE
## OUT OF SCOPE

Dashboard Core
WEB Voucher
Shop Voucher
Customer Banner UI
Vendor Marketing UI
Payment Core
Commission
Settlement
Payout
Refund Core
Shipping
Auto Moderation
KYC Provider
ReportCase Backend
Violation Backend
Complaint Backend
Escalation Backend
AuditLog Backend Core
Phase 5 Full Integration
95. REPORT – FILES CHANGED
## FILES CHANGED

### Added

- file
  Purpose:

### Modified

- file
  Purpose:

### Deleted

- file
  Purpose:

Không chỉ liệt kê filename.

96. REPORT – TEST RESULT
## TEST RESULT

1. Banner List: PASS/FAIL
2. Banner View: PASS/FAIL
3. Banner Create: PASS/FAIL
4. Banner Create Validation: PASS/FAIL
5. Banner Edit: PASS/FAIL
6. Banner Delete: PASS/FAIL
7. Banner Publish: PASS/FAIL/N/A
8. Banner Unpublish: PASS/FAIL/N/A
9. Banner Scheduling: PASS/FAIL/N/A
10. Banner Priority: PASS/FAIL/N/A
11. Filter: PASS/FAIL/N/A
12. Pagination: PASS/FAIL/N/A
13. Empty State: PASS/FAIL
14. Not Found: PASS/FAIL
15. Error Handling: PASS/FAIL
16. Image Validation: PASS/FAIL/N/A
17. Target URL Validation: PASS/FAIL/N/A
18. Double Submit: PASS/FAIL
19. Security: PASS/FAIL
20. Locked Admin: PASS/FAIL/NOT INTEGRATED
21. AuditLog Contract: PASS/FAIL/N/A
22. AuditLog Integration: PASS/FAIL/NOT INTEGRATED
23. Admin Regression: PASS/FAIL
24. Maven Test: PASS/FAIL
25. Maven Clean Package: PASS/FAIL

Không ghi PASS nếu chưa test.

97. MANUAL TEST MATRIX
#	Test	Expected	Actual	Status
1	Admin Login	Login thành công	?	?
2	Banner List	Dữ liệu đúng	?	?
3	Banner View	Detail đúng	?	?
4	Banner Create	Created	?	?
5	Invalid Create	Rejected	?	?
6	Banner Edit	Updated	?	?
7	Banner Delete	Deleted/Rejected đúng rule	?	?
8	Banner Publish	Backend updated	?	?
9	Banner Unpublish	Backend updated	?	?
10	Scheduling	Correct business rule	?	?
11	Priority	Correct	?	?
12	Filter	Correct result	?	?
13	Pagination	Correct page	?	?
14	Empty State	Correct empty state	?	?
15	Not Found	Correct 404	?	?
16	Error Handling	User-friendly error	?	?
17	Image Validation	Correct validation	?	?
18	URL Validation	Correct validation	?	?
19	Double Submit	No duplicate action	?	?
20	Customer Access	Denied	?	?
21	Vendor Access	Denied	?	?
22	Moderator Access	Denied	?	?
23	Locked Admin	Denied	?	?
24	AuditLog	Correct event / NOT INTEGRATED	?	?
25	Admin Regression	No regression	?	?
98. SECURITY TEST REPORT
## SECURITY TEST

Anonymous:
PASS / FAIL

CUSTOMER:
PASS / FAIL

VENDOR:
PASS / FAIL

MODERATOR:
PASS / FAIL

ADMIN:
PASS / FAIL

Locked ADMIN:
PASS / FAIL / NOT INTEGRATED

Nếu Security Core chưa tích hợp nhưng user-owned security seam đã hoàn thành:

Implementation:
IMPLEMENTED

Integration:
INTEGRATION-READY

Không ghi BLOCKED.

99. REGRESSION TEST
## REGRESSION TEST

Admin Login:
PASS / FAIL

Dashboard:
PASS / FAIL

User:
PASS / FAIL

Shop:
PASS / FAIL

Category:
PASS / FAIL

Order:
PASS / FAIL

Product:
PASS / FAIL

Review:
PASS / FAIL

KYC Monitoring:
PASS / FAIL / N/A

Violation:
PASS / FAIL / N/A

Escalation:
PASS / FAIL / N/A

AuditLog:
PASS / FAIL / N/A

WEB Voucher:
PASS / FAIL

Banner:
PASS / FAIL

Nếu module bên ngoài chưa tích hợp, không giả định PASS.

100. BUILD REPORT
## BUILD

Command:
mvn test

Result:
PASS / FAIL

Command:
mvn clean package

Result:
PASS / FAIL

Nếu FAIL:

Error:
Root cause:
Affected module:
Integration:
Real blocker:
101. KNOWN ISSUES
## KNOWN ISSUES

Format:

Issue:
Impact:
Implementation Status:
Integration Status:
Dependency:
Workaround:
Next Action:

Không che giấu issue.

102. BUSINESS RULE CHECK
## BUSINESS RULE CHECK

[ ] Banner status lấy từ backend/contract
[ ] Publish cập nhật backend thật
[ ] Unpublish cập nhật backend thật
[ ] Scheduling không fake
[ ] Priority không fake
[ ] Validation có backend enforcement
[ ] Admin-only security
[ ] Locked Admin được xử lý đúng theo contract
[ ] AuditLog không fake
[ ] Không tự sửa Customer Banner
[ ] Không tạo CMS mới ngoài scope
[ ] Không thay đổi Role
[ ] Không thêm SUPPLIER
[ ] Không overwrite ownership khác
[ ] Không fake production data
[ ] Không dùng mock production behavior
103. OWNER / INTEGRATION REPORT

Không dùng bảng dependency kiểu người làm thành blocker.

Dùng:

Area	User-Owned Implementation	External Integration	Integration Status
Banner UI	Phase 4C	Existing Banner backend	READY / INTEGRATED
Marketing UI	Phase 4C	Existing Marketing backend	READY / INTEGRATED
Security	Phase 4C integration	Security Core	READY / INTEGRATED
Audit	Phase 4C adapter/seam	AuditLog backend	READY / INTEGRATED
Storage	Phase 4C integration	Existing storage	READY / INTEGRATED

Nếu cần ghi owner:

External Owner (informational):
...

Owner không phải prerequisite.

104. FINAL STATUS

Không còn dùng mô hình:

READY hoặc BLOCKED

một cách cứng nhắc.

Final status phải phản ánh implementation status và integration status riêng biệt.

Trường hợp 1 — User-owned implementation hoàn thành
IMPLEMENTED
+
INTEGRATION-READY

Điều này có nghĩa:

Phần Phase 4C đã hoàn thành.
External integration chưa kết nối.
Không có hard blocker.

Đây là trạng thái hợp lệ để kết thúc Phase 4C.

Trường hợp 2 — Backend thật đã tích hợp
IMPLEMENTED
+
INTEGRATED

Có thể kết luận:

READY FOR NEXT PHASE

sau khi regression/build đạt.

Trường hợp 3 — User-owned implementation chưa hoàn thành
PARTIAL
Trường hợp 4 — Có hard environmental blocker
BLOCKED

và phải nêu evidence.

105. DEFINITION OF READY

Phase 4C được xem là user-owned implementation complete khi:

1. Banner List implementation hoàn thành
2. Banner View implementation hoàn thành
3. Banner Create implementation hoàn thành
4. Banner Edit implementation hoàn thành
5. Banner Delete implementation hoàn thành nếu contract yêu cầu
6. Publish integration seam hoàn thành nếu applicable
7. Unpublish integration seam hoàn thành nếu applicable
8. Scheduling UI/contract hoàn thành nếu applicable
9. Priority UI/contract hoàn thành nếu applicable
10. Validation hoàn thành
11. Error handling hoàn thành
12. Security integration hoàn thành
13. Audit integration seam hoàn thành
14. Không fake data
15. Không fake status
16. Không fake scheduling
17. Không bypass backend
18. Tests user-owned PASS
19. Maven Test PASS
20. Maven Clean Package PASS
21. Git diff đã kiểm tra
22. Final Report đầy đủ

External backend chưa tồn tại không làm Phase 4C user-owned implementation thành BLOCKED.

106. P0 / P1 RULE
P0
Admin không truy cập được Banner khi có quyền
Unauthorized role truy cập được Banner management
Publish/Unpublish gây sai dữ liệu nghiêm trọng
Delete nhầm dữ liệu
Data corruption
Security bypass
P1
CRUD chính không hoạt động
Validation business rule bị bypass
Scheduling sai nghiêm trọng
Priority sai dữ liệu
Backend/UI contract sai
Banner status không phản ánh backend
Regression làm hỏng Admin module

Không được kết luận:

IMPLEMENTED

nếu user-owned P0/P1 chưa xử lý.

Nhưng nếu P0/P1 nằm hoàn toàn ở external implementation:

User-owned:
INTEGRATION-READY

External:
NOT INTEGRATED

Không gọi đó là blocker nếu seam/test đã hoàn thành.

107. FINAL REPORT FORMAT

Sau khi hoàn thành:

# PHASE 4C – FINAL REPORT

## 1. Git Baseline

Current branch:
HEAD before:
HEAD after:
Working tree:
Uncommitted changes:

## 2. Objective

...

## 3. DONE

### Banner

...

### Marketing / PR

...

## 4. PARTIAL

...

## 5. MISSING

...

## 6. WRONG

...

## 7. INTEGRATION-READY

...

## 8. INTEGRATED

...

## 9. OUT OF SCOPE

...

## 10. FILES CHANGED

### Added

...

### Modified

...

### Deleted

...

## 11. TEST RESULT

...

## 12. SECURITY TEST

...

## 13. REGRESSION TEST

...

## 14. BUSINESS RULE CHECK

...

## 15. BUILD

mvn test:
PASS / FAIL

mvn clean package:
PASS / FAIL

## 16. KNOWN ISSUES

...

## 17. EXTERNAL INTEGRATIONS

...

## 18. PHASE STATUS

IMPLEMENTED + INTEGRATION-READY

hoặc

IMPLEMENTED + INTEGRATED

hoặc

PARTIAL

hoặc

BLOCKED

## 19. NEXT PHASE READINESS

...
108. TUYỆT ĐỐI KHÔNG BÁO CÁO SAI

Không ghi:

DONE

chỉ vì file đã tạo.

Không ghi:

PASS

chỉ vì compile thành công.

Không ghi:

Banner hoàn thành

nếu CRUD chỉ hoạt động frontend.

Không ghi:

Publish hoàn thành

nếu chỉ đổi status trên UI.

Không ghi:

Scheduling hoàn thành

nếu frontend chỉ dùng setTimeout.

Không ghi:

Security PASS

nếu chưa test hoặc verify contract.

Không ghi:

AuditLog PASS

nếu backend chưa verify event.

Không ghi:

INTEGRATED

nếu external backend chưa thực sự kết nối.

Không ghi:

BLOCKED

chỉ vì backend developer khác chưa implement.

109. KHI GẶP EXTERNAL BACKEND CHƯA CÓ

Nếu gặp:

Backend chưa có
AuditLog chưa có
Security implementation chưa sẵn sàng
Storage chưa có
Marketing backend chưa có

Thực hiện:

STEP 1
Xác định contract

↓

STEP 2
Xác định integration seam

↓

STEP 3
Implement phần Phase 4C

↓

STEP 4
Test bằng test double trong test scope

↓

STEP 5
Đánh dấu IMPLEMENTED

↓

STEP 6
Đánh dấu INTEGRATION-READY

↓

STEP 7
Không fake production behavior

Báo cáo:

External Integration:
...

Contract:
...

Integration Seam:
...

User-Owned Implementation:
IMPLEMENTED

External Implementation:
MISSING / PARTIAL

Integration Status:
NOT INTEGRATED

Real Blocker:
NONE
110. KHÔNG OVER-ENGINEER

Không biến Phase 4C thành:

CMS Platform
Marketing Automation
Campaign Management
Ad Management
Content Management System lớn

Phase 4C chỉ:

Banner
+
Marketing / PR module hiện có
+
Hardening
+
Validation
+
Security
+
Audit integration
+
Testing

Feature không cần thiết:

OUT OF SCOPE
111. NGUYÊN TẮC REUSE

Ưu tiên:

Existing Entity
Existing DTO
Existing Service
Existing Repository
Existing Controller
Existing Template
Existing CSS
Existing JS
Existing Security
Existing Validation
Existing Audit integration
Existing contract/interface

Chỉ tạo mới khi:

Không có implementation tương đương
+
Thực sự cần cho Phase 4C

Không tạo duplicate:

BannerService2
BannerControllerV2
MarketingServiceNew
112. CHECKLIST TRƯỚC KHI KẾT THÚC
[ ] Đã đọc Git baseline

[ ] Đã đọc Banner Entity
[ ] Đã đọc Banner Repository
[ ] Đã đọc Banner Service
[ ] Đã đọc Banner Controller
[ ] Đã đọc Banner DTO
[ ] Đã đọc Banner Template

[ ] Đã đọc Admin Layout
[ ] Đã đọc Security
[ ] Đã đọc AuditLog contract/integration

[ ] Đã xác định external integration points
[ ] Đã xác định integration seams
[ ] Đã xác định user-owned scope

[ ] Đã kiểm tra List
[ ] Đã kiểm tra View
[ ] Đã kiểm tra Create
[ ] Đã kiểm tra Edit
[ ] Đã kiểm tra Delete
[ ] Đã kiểm tra Publish
[ ] Đã kiểm tra Unpublish
[ ] Đã kiểm tra Scheduling
[ ] Đã kiểm tra Priority
[ ] Đã kiểm tra Validation
[ ] Đã kiểm tra Error Handling
[ ] Đã kiểm tra Empty State
[ ] Đã kiểm tra Not Found
[ ] Đã kiểm tra Filter
[ ] Đã kiểm tra Pagination
[ ] Đã kiểm tra Security
[ ] Đã kiểm tra Locked Admin
[ ] Đã kiểm tra Audit integration seam
[ ] Đã kiểm tra Double Submit

[ ] Đã chạy Unit/Service tests
[ ] Đã chạy Integration/Contract tests
[ ] Đã chạy Maven Test
[ ] Đã chạy Maven Clean Package

[ ] Đã kiểm tra Git Diff
[ ] Đã kiểm tra Admin Regression

[ ] Đã phân loại DONE
[ ] Đã phân loại PARTIAL
[ ] Đã phân loại MISSING
[ ] Đã phân loại WRONG
[ ] Đã phân loại INTEGRATION-READY
[ ] Đã phân loại INTEGRATED
[ ] Đã phân loại OUT OF SCOPE

[ ] Đã liệt kê file thay đổi
[ ] Đã ghi Known Issues
[ ] Đã ghi External Integrations
[ ] Đã xác định Final Status
113. QUY TRÌNH THỰC THI BẮT BUỘC

Coding Agent thực hiện:

STEP 1
Git baseline
        ↓
STEP 2
Đọc Phase 1–4B relevant state
        ↓
STEP 3
Audit Banner / Marketing hiện tại
        ↓
STEP 4
Audit existing contracts/interfaces
        ↓
STEP 5
Xác định EXISTS / BUG / INCOMPLETE / MISSING
        ↓
STEP 6
Xác định external integration points
        ↓
STEP 7
Xác định integration seams
        ↓
STEP 8
Reuse Admin UI hiện tại
        ↓
STEP 9
Implement Banner hardening
        ↓
STEP 10
Implement validation
        ↓
STEP 11
Implement Publish / Unpublish integration
        ↓
STEP 12
Implement Scheduling nếu applicable
        ↓
STEP 13
Implement Priority nếu applicable
        ↓
STEP 14
Implement Filter / Pagination nếu applicable
        ↓
STEP 15
Security verification
        ↓
STEP 16
Audit integration seam
        ↓
STEP 17
Unit / Contract / Integration tests
        ↓
STEP 18
Manual tests
        ↓
STEP 19
Admin regression
        ↓
STEP 20
mvn test
        ↓
STEP 21
mvn clean package
        ↓
STEP 22
git status
        ↓
STEP 23
git diff --stat
        ↓
STEP 24
git diff
        ↓
STEP 25
Final Report
        ↓
STEP 26
IMPLEMENTED + INTEGRATION-READY
hoặc
IMPLEMENTED + INTEGRATED
hoặc
PARTIAL
hoặc
BLOCKED

Không bỏ qua bước nếu chưa có lý do rõ ràng.

114. MỤC TIÊU CUỐI CÙNG

Sau Phase 4C:

ADMIN

 ↓

BANNER / MARKETING

 ├── List
 ├── View
 ├── Create
 ├── Edit
 ├── Delete
 ├── Publish
 ├── Unpublish
 ├── Schedule
 ├── Priority
 ├── Validation
 ├── Security
 ├── Audit Integration
 └── Regression

Flow production:

Admin UI
   ↓
Existing Backend Contract
   ↓
Service
   ↓
Repository / External Backend
   ↓
Database

Nếu external implementation chưa tồn tại:

Admin UI
   ↓
Contract / Integration Seam
   ↓
Test Double

chỉ trong test.

Không được biến thành:

Admin UI
   ↓
Fake JavaScript
115. YÊU CẦU CUỐI CÙNG CHO CODING AGENT
Trước khi bắt đầu
1. Đọc code hiện tại.
2. Đọc Banner/Marketing implementation hiện tại.
3. Đọc existing contract/interface.
4. Đọc Security.
5. Đọc AuditLog integration.
6. Kiểm tra Git.
7. Xác định ownership.
8. Xác định integration points.
9. Xác định integration seams.
10. Không giả định feature tồn tại.
Trong khi code
1. Reuse Admin UI.
2. Reuse backend hiện tại.
3. Reuse existing contract.
4. Không redesign lớn.
5. Không fake data.
6. Không fake status.
7. Không fake scheduling.
8. Không bypass backend.
9. Không overwrite code người khác.
10. Không mở rộng thành CMS.
11. Không phá Admin module cũ.
12. Không chờ developer khác để hoàn thành phần user-owned.
13. Dùng test double chỉ trong test.
14. Đánh dấu INTEGRATION-READY nếu external implementation chưa có.
Sau khi code
1. Test Banner List.
2. Test View.
3. Test Create.
4. Test Edit.
5. Test Delete.
6. Test Publish.
7. Test Unpublish.
8. Test Scheduling nếu applicable.
9. Test Priority nếu applicable.
10. Test Validation.
11. Test Error Handling.
12. Test Security.
13. Test Locked Admin.
14. Test Audit contract/integration.
15. Test Admin Regression.
16. Chạy mvn test.
17. Chạy mvn clean package.
18. Kiểm tra git diff.
19. Phân loại DONE/PARTIAL/MISSING/WRONG.
20. Phân loại INTEGRATION-READY/INTEGRATED.
21. Liệt kê toàn bộ file thay đổi.
22. Ghi Known Issues.
23. Ghi External Integration.
24. Kết luận Final Status.

Không được kết thúc task chỉ bằng câu “đã hoàn thành”. Phải có Final Report và Test Result thực tế.

116. DEFINITION OF DONE

Phase 4C được coi là hoàn thành phần implementation thuộc Phase 4C khi:

Banner backend contract đúng
+
Admin UI hoạt động
+
CRUD implementation hoàn thành
+
Publish/Unpublish integration seam hoàn thành nếu applicable
+
Scheduling đúng nếu business rule yêu cầu
+
Priority đúng nếu business rule yêu cầu
+
Validation đúng
+
Error handling đúng
+
Security integration đúng
+
Audit integration seam đúng
+
Regression không phá Admin hiện tại
+
mvn test PASS
+
mvn clean package PASS
+
Git diff được kiểm tra
+
Final Report đầy đủ

Nếu external backend đã tồn tại:

→ INTEGRATED

nếu đã verify thực tế.

Nếu external backend chưa tồn tại nhưng phần Phase 4C đã hoàn thành:

→ IMPLEMENTED + INTEGRATION-READY

Đây là trạng thái hoàn thành hợp lệ, không phải BLOCKED.

Chỉ ghi:

PARTIAL

khi phần implementation thuộc Phase 4C còn thiếu.

Chỉ ghi:

BLOCKED

khi có hard environmental blocker thực sự.

117. CÂU NGUYÊN TẮC BẮT BUỘC

Coding Agent phải giữ nguyên tinh thần câu này trong toàn bộ Phase 4C:

“Tôi implement được phần của tôi bằng contract/interface hiện tại; backend của Tuấn là integration point, không phải prerequisite để bắt đầu hoặc hoàn thành implementation.”

Áp dụng tương tự cho mọi external owner:

External backend/service/provider là integration point, không phải prerequisite để bắt đầu hoặc hoàn thành phần implementation thuộc Phase 4C.

Do đó:

Developer khác chưa làm
        ↓
KHÔNG DỪNG PHASE
        ↓
Read contract
        ↓
Define/use seam
        ↓
Implement owned scope
        ↓
Test independently
        ↓
INTEGRATION-READY
        ↓
Later connect real implementation

Không fake. Không bypass. Không duplicate. Không block bởi người khác.

END OF PHASE 4C