
PHASE 4A — DASHBOARD / VOUCHER / BANNER HARDENING
0. MỤC ĐÍCH

Phase 4A là sub-phase đầu của Phase 4, tập trung đúng ba nhóm:

DASHBOARD
VOUCHER
BANNER

Mục tiêu không phải viết lại CRUD, mà là:

Current Source
      ↓
Verify existing implementation
      ↓
Trace Controller
      ↓
Service
      ↓
Repository / MongoDB
      ↓
Security
      ↓
UI
      ↓
Actual behavior
      ↓
Identify WRONG / PARTIAL / MISSING
      ↓
Minimal hardening
      ↓
Regression
      ↓
Build

Phase 4A phải đặc biệt bám vào kết quả đối chiếu code hiện tại:

Voucher CRUD/filter/pagination/activate/deactivate đã tồn tại.
Banner CRUD/publish/unpublish/filter/pagination đã tồn tại.
Admin Product/Review/Shop/Order/Voucher/Banner hiện đã có controller/service/template tương ứng.
Customer đã có flow hiển thị Banner ở trạng thái PUBLISHED.
Dashboard hiện có metric totalPlatformRevenue, nhưng source hiện tại đang tính dựa trên tổng amount của delivered orders, chưa chứng minh đó là platform revenue thực sự.
Product, Review, Shop, User hiện có các state riêng; Phase 4A không được tự mở rộng state chỉ để làm Dashboard/Voucher/Banner đẹp hơn.

Vì vậy:

Phase 4A ưu tiên kiểm chứng và sửa semantic/security/data-source của implementation hiện có; không mặc định tạo CRUD mới.

1. SOURCE OF TRUTH

Thứ tự:

PHASE 4A SCOPE
        ↓
BỘ LUẬT & CHÍNH SÁCH
        ↓
CURRENT SOURCE CODE
        ↓
Phase 4 / Phase 3 handoff
        ↓
SHOPEE_MARKETPLACE_MODEL

Các Phase/Report cũ chỉ là:

BASELINE

không phải bằng chứng implementation hiện tại.

Đặc biệt:

VoucherController tồn tại
≠ Voucher feature hoàn chỉnh

BannerController tồn tại
≠ Banner runtime đúng

Dashboard có metric
≠ Metric đúng business meaning
2. PHASE 4A FEATURE SCOPE
2.1 Dashboard

Được phép:

Verify dashboard
Verify metric source
Verify aggregation
Verify permission
Verify empty-state
Verify pagination/filter nếu có
Verify financial terminology
Fix incorrect metric
Fix incorrect repository query
Fix security
Fix integration trực tiếp
2.2 Voucher

Được phép:

Verify existing CRUD
Verify validation
Verify ownership/authority
Verify activate/deactivate
Verify filtering
Verify pagination
Verify status
Verify customer visibility/application boundary
Fix security
Fix persistence
Fix incorrect business behavior
2.3 Banner

Được phép:

Verify CRUD
Verify publish/unpublish
Verify status
Verify startAt/endAt nếu source có
Verify priority nếu source có
Verify effective visibility
Verify Admin authority
Verify customer visibility
Fix security
Fix backend selection
Fix persistence
3. DEPENDENCY SCOPE

Chỉ được sửa dependency ngoài Phase 4A khi:

Dashboard/Voucher/Banner
        ↓
Dependency
        ↓
Dependency bug
        ↓
Phase 4A acceptance fail

Ví dụ:

Dashboard
 ↓
OrderRepository
 ↓
query delivered orders sai

→ được phép sửa minimal query.

Hoặc:

Admin Voucher
 ↓
Security
 ↓
vendor có thể gọi Admin endpoint

→ được phép sửa security boundary.

Không vì vậy mà:

rewrite Order
rewrite Security
rewrite Finance
rewrite Admin
4. CURRENT CODE BASELINE — PHẢI GIỮ

Theo đối chiếu hiện tại, Phase 4A không bắt đầu từ zero.

Voucher hiện có

Đã có các capability thuộc nhóm:

Voucher CRUD
Filter
Pagination
Activate
Deactivate

Do đó:

KHÔNG tạo VoucherV2
KHÔNG tạo VoucherAdminV2
KHÔNG tạo VoucherServiceV2

Việc cần làm là trace:

Admin UI
 ↓
VoucherController
 ↓
VoucherService
 ↓
Repository
 ↓
MongoDB

và kiểm chứng behavior.

Banner hiện có

Đã có:

Banner CRUD
Publish
Unpublish
Filter
Pagination

Customer có flow lấy Banner PUBLISHED.

Do đó:

KHÔNG tạo BannerV2
KHÔNG tạo BannerManagementV2

Phải kiểm chứng:

Admin
 ↓
Banner Controller
 ↓
Banner Service
 ↓
Repository
 ↓
MongoDB
 ↓
Customer Banner Query
Dashboard hiện có

Dashboard đã tồn tại.

Nhưng metric:

totalPlatformRevenue

cần được kiểm chứng lại vì source hiện tại đang dựa trên:

Delivered Orders
        ↓
Order Amount
        ↓
totalPlatformRevenue

Điều này không tự động đồng nghĩa:

Platform Revenue

Phải phân biệt:

Order Sales / GMV
Vendor Sales
Platform Fee
Platform Revenue
Refund
Settlement
Vendor Payable

Do đó đây là một hardening issue trực tiếp của Dashboard, không phải lý do để xây Finance V2.

5. GATE 4A-1 — DISCOVERY

Trước khi sửa:

git status
git branch
git log --oneline --decorate -10

Sau đó inspect trực tiếp:

Dashboard
Voucher
Banner
Order
Product
Shop
User
Security

Ưu tiên tìm:

Controller
Service
Repository
Entity
DTO
Enum
Template
Security config
Query
Aggregation

Không đọc lại toàn project một cách mù quáng.

6. GATE 4A-2 — REUSE CHECK

Trước khi tạo bất kỳ class nào:

Search existing
        ↓
Exists?
 ├── YES → REUSE / FIX
 └── NO  → CREATE only if acceptance requires

Không tạo:

DashboardV2
VoucherV2
BannerV2
FinanceDashboardV2
PlatformRevenueV2

chỉ vì implementation hiện tại còn thiếu hardening.

7. DASHBOARD HARDENING
   7.1 Dashboard không được coi là UI-only

Phải trace:

Dashboard Page
      ↓
Controller
      ↓
Service
      ↓
Repository / Query
      ↓
MongoDB

Mỗi metric phải trả lời được:

Metric này lấy từ đâu?
Query nào?
Entity nào?
Field nào?
Có filter status không?
Có duplicate không?
Có hardcode không?
Có phụ thuộc data thật không?
8. DASHBOARD METRIC SOURCE

Mỗi metric hiện có phải phân loại:

Metric	Cần kiểm chứng
Orders	Order source
Products	Product source
Shops	Shop source
Users	User source
Voucher	Voucher source
Banner	Banner source
Moderation	Moderation source nếu tồn tại
Complaint	Complaint source nếu tồn tại
Escalation	Escalation source nếu tồn tại
Violation	Violation source nếu tồn tại
Revenue	Financial source, không mặc định Order Total

Không thêm metric chỉ vì dashboard design có chỗ trống.

9. DASHBOARD — NO FAKE METRIC

Cấm:

return 100;

hoặc:

pending = 10
revenue = 100000000
orders = 500

hoặc mock list trong production.

Nếu database không có dữ liệu:

0

chỉ hợp lệ nếu 0 thực sự là kết quả query.

Không được:

hardcoded 0

chỉ để UI không lỗi.

10. TOTAL PLATFORM REVENUE — HARDENING BẮT BUỘC

Đây là điểm cần kiểm tra kỹ nhất của Dashboard hiện tại.

Current logic được đối chiếu:

Delivered Orders
       ↓
sum order amount
       ↓
totalPlatformRevenue

Không được mặc định chấp nhận semantic này.

Phải trace:

Policy
 ↓
Financial definition
 ↓
Current field
 ↓
Current query
 ↓
Dashboard label

Nếu source/policy xác định:

Platform Revenue
================

Platform Fee / Commission

thì:

Order Total ≠ Platform Revenue

và metric hiện tại phải đánh dấu:

WRONG

nếu đang hiển thị nó dưới tên Platform Revenue.

11. CÁCH FIX TOTAL PLATFORM REVENUE

Không được tự chọn:

commission = 10%

hoặc:

platformRevenue = orderTotal * X

nếu Policy/source không quy định.

Thay vào đó:

Trường hợp A

Có financial source thật:

Settlement / Fee / Revenue source
        ↓
Dashboard

→ dùng source đó.

Trường hợp B

Chỉ có Order Total:

Order Total

nhưng chưa có platform fee/revenue source:

→ không giả lập platform revenue.

Có thể:

đổi semantic metric về Order Sales/GMV

chỉ khi terminology đó phù hợp source/policy, hoặc:

để metric unavailable / chưa hỗ trợ

nếu chưa đủ source.

12. DASHBOARD STATUS FILTER

Các metric có trạng thái phải kiểm tra:

ACTIVE
INACTIVE
PUBLISHED
DRAFT
DELIVERED
CANCELLED
...

chỉ dùng những state thực sự tồn tại.

Không tạo:

DashboardStatus

mới nếu không cần.

Ví dụ:

Order count

phải xác định rõ:

all orders?
delivered?
paid?
completed?

Không tự chọn chỉ vì tên metric.

13. DASHBOARD DUPLICATE COUNT

Kiểm tra:

Join / aggregation
        ↓
duplicate document
        ↓
count sai

Đặc biệt với:

Order
OrderItem
Product
Shop
Voucher

Không để một Order bị đếm nhiều lần chỉ vì có nhiều OrderItem.

Nếu query hiện tại đã đúng:

REUSE

Không refactor chỉ để đổi style.

14. DASHBOARD EMPTY STATE

Kiểm tra:

0 record

không gây:

NullPointerException
500
NaN

Các trường hợp:

no orders
no voucher
no banner
no shop
no product

phải xử lý bằng behavior thực tế của source/UI.

Không seed dữ liệu giả chỉ để dashboard có số.

15. DASHBOARD SECURITY

Dashboard Admin phải kiểm tra:

ADMIN → Dashboard

và:

MODERATOR → Admin Dashboard
CUSTOMER → Admin Dashboard
VENDOR → Admin Dashboard
ANONYMOUS → Admin Dashboard

Expected theo security matrix thực tế:

Admin scope → ALLOW
Unauthorized → DENY

Không chỉ hide menu.

Backend phải enforce.

16. DASHBOARD IDOR

Nếu dashboard nhận:

shopId
vendorId
userId

từ request:

phải kiểm tra có thực sự cần hay không.

Nếu Admin dashboard là platform-wide:

Không nên trust arbitrary user-provided scope

Nếu dashboard có scoped view:

Authenticated principal
        ↓
Server-side authorization
        ↓
Allowed scope
17. DASHBOARD PERFORMANCE — CHỈ HARDEN NẾU CẦN

Có thể kiểm tra:

N+1 query
full collection load
unbounded query
repeated repository call

Nhưng không biến Phase 4A thành:

rewrite all repositories

Chỉ tối ưu khi:

metric hiện tại không đáp ứng acceptance

hoặc query gây lỗi/thực tế không dùng được.

18. VOUCHER HARDENING — KHÔNG REWRITE CRUD

Existing:

CRUD
Filter
Pagination
Activate
Deactivate

Phase 4A phải xác minh từng capability.

Test matrix
Capability	Kiểm tra
Create	Persistence
Read	Correct data
Update	Correct document
Filter	Actual query
Pagination	Stable result
Activate	State persisted
Deactivate	State persisted
Security	Authority
Customer visibility	Backend behavior
Invalid input	Validation
19. VOUCHER CREATE

Trace:

Admin UI
 ↓
POST/Controller
 ↓
Service
 ↓
Validation
 ↓
Repository.save()
 ↓
MongoDB

Không coi:

HTTP 200

là bằng chứng persistence.

Phải kiểm tra document thực tế nếu manual/runtime verification được thực hiện.

20. VOUCHER UPDATE

Kiểm tra:

existing voucher
 ↓
update
 ↓
save

Đảm bảo không:

tạo document duplicate

thay vì update document cũ.

Nếu source đã dùng đúng:

REUSE
21. VOUCHER ACTIVATE / DEACTIVATE

Phải xác minh:

Activate
 ↓
MongoDB status changed

và:

Deactivate
 ↓
MongoDB status changed

Không chỉ:

redirect /vouchers

hoặc:

flash message "Success"

mà không persistence.

22. VOUCHER STATUS SEMANTIC

Không tự thêm:

EXPIRED
USED
SUSPENDED
PLATFORM
SHOP

nếu current model/policy chưa có.

Nếu source có status/active field:

reuse

Nếu policy yêu cầu state nhưng model chưa hỗ trợ:

DEPENDENCY GAP

và phải xác định có thuộc Phase 4A hay không.

23. VOUCHER AUTHORITY

Đây là security gate quan trọng.

Phải xác định:

Who owns this Voucher?

và:

Who may Create?
Who may Update?
Who may Activate?
Who may Deactivate?
Who may View?
Who may Apply?

Không tự mở quyền.

24. VOUCHER OWNER ID

Nếu request chứa:

ownerId
shopId
vendorId
userId

phải kiểm tra backend có đang trust trực tiếp hay không.

Ưu tiên:

Authenticated Principal
        ↓
Server-side ownership
        ↓
Authorization

Không:

frontend says shopId = X
→ backend accepts X

nếu Vendor có quyền tạo voucher theo Shop ownership.

25. VOUCHER ADMIN BOUNDARY

Nếu existing Voucher là Admin-only:

Vendor → Admin Voucher CRUD

phải:

DENY

Nếu có Shop Voucher architecture:

Vendor → Own Shop Voucher

chỉ được allow khi current source/policy hỗ trợ.

Không tự mở:

Vendor → Platform Voucher
26. VOUCHER FILTER

Kiểm tra:

filter parameter
 ↓
Controller
 ↓
Service
 ↓
Repository query

Không để filter chỉ hoạt động ở UI.

Ví dụ nếu UI chọn:

ACTIVE

thì backend phải thực sự query:

active/status = ACTIVE

theo model hiện tại.

27. VOUCHER PAGINATION

Kiểm tra:

page
size
total
content

và:

page out of range
invalid size
negative page

Không tạo pagination implementation thứ hai nếu hiện tại đã có.

28. VOUCHER VALIDATION

Chỉ validate những rule có source/policy hỗ trợ.

Ví dụ các field bắt buộc hiện có phải được kiểm tra.

Không tự thêm:

minimumOrder = X
maxDiscount = Y
usageLimit = Z

nếu Policy/source không quy định.

29. VOUCHER CUSTOMER SIDE

Nếu voucher có customer application/use flow:

phải trace:

Customer
 ↓
Voucher lookup
 ↓
Eligibility
 ↓
Apply

Nhưng Phase 4A không được xây mới voucher application engine nếu hiện tại chưa có và đó không phải acceptance của hardening.

Nếu external/apply capability chưa tồn tại:

INTEGRATION-READY

hoặc:

OUT OF SCOPE

tùy dependency thực tế.

30. VOUCHER NO FAKE USAGE

Không tạo:

usedCount = 100

hoặc:

voucherUsage = fake

để dashboard hiển thị đẹp.

Nếu usage metric không có source:

NOT AVAILABLE

không fake.

31. BANNER HARDENING — KHÔNG REWRITE CRUD

Existing:

CRUD
Filter
Pagination
Publish
Unpublish

Phase 4A phải verify:

Create
Read
Update
Delete nếu source có
Publish
Unpublish
Filter
Pagination
Customer visibility
Security
Persistence
32. BANNER PUBLISH

Trace:

Admin
 ↓
Publish action
 ↓
Service
 ↓
Repository
 ↓
MongoDB

Phải xác nhận state thực sự thay đổi.

Không coi:

redirect + success message

là persistence evidence.

33. BANNER UNPUBLISH

Tương tự:

Unpublish
 ↓
Persist
 ↓
Customer query
 ↓
Banner no longer effective

Nếu customer query hiện tại chỉ lọc:

PUBLISHED

thì phải verify unpublish thực sự loại banner khỏi customer response.

34. BANNER CUSTOMER VISIBILITY

Existing customer behavior đã có:

Customer
 ↓
PUBLISHED Banner

Phase 4A phải kiểm chứng:

DRAFT
→ not visible

UNPUBLISHED
→ not visible

theo state thực tế.

Không thêm state mới nếu không cần.

35. BANNER EFFECTIVE TIME

Nếu Banner model/source có:

startAt
endAt

phải kiểm tra backend có thực sự dùng:

current time
+
startAt
+
endAt

hay chỉ kiểm tra:

status = PUBLISHED

Nếu Policy yêu cầu effective time mà backend bỏ qua:

WRONG

→ minimal fix.

Nếu source/policy không yêu cầu time-based selection:

không tự phát minh
36. BANNER PRIORITY

Nếu model hiện tại có:

priority

phải kiểm chứng:

priority
 ↓
query/sort
 ↓
customer effective banners

Nếu priority chỉ tồn tại trong entity nhưng không được sử dụng:

Entity field exists
≠
Priority behavior implemented

Phân loại theo evidence:

PARTIAL / WRONG

Không tự invent priority algorithm nếu Policy chưa định nghĩa.

37. BANNER AUTHORITY

Phải xác định authority hiện tại.

Nếu Banner là platform-level:

ADMIN → Create/Update/Publish/Unpublish

Vendor không được tự thao tác Platform Banner nếu source/security không cho phép.

Không tạo:

VendorBanner

chỉ để lấp khoảng trống.

38. BANNER IDOR

Nếu API nhận:

bannerId

backend phải:

load banner
authorize actor
perform action

Không:

frontend button hidden

là security.

39. BANNER DELETE

Nếu existing Banner CRUD có delete:

phải kiểm tra kỹ.

Không được delete historical/business-critical record chỉ vì:

banner đã publish
banner đã từng hiển thị
debug
test

Nếu Banner history cần giữ theo policy/source:

deactivate/unpublish

có thể là behavior phù hợp hơn, nhưng không được tự thay đổi semantics nếu policy chưa quy định.

40. DASHBOARD ↔ VOUCHER INTEGRATION

Nếu Dashboard có metric Voucher:

Dashboard
 ↓
VoucherRepository
 ↓
MongoDB

Không:

Dashboard count
===============

UI list size

nếu pagination làm list chỉ chứa một page.

Phải phân biệt:

page content count

và:

total document count
41. DASHBOARD ↔ BANNER INTEGRATION

Nếu Dashboard có:

Published Banners

query phải xác định rõ:

published count

không phải:

all banners

Nếu có:

effective banners

thì phải dùng backend effective-selection logic nếu source/policy hỗ trợ.

Không lấy:

UI visible banners

làm source of truth.

42. SECURITY HARDENING CHUNG

Phase 4A phải review:

Authentication
Authorization
Role
Ownership
IDOR
Input Validation
State Transition

Đặc biệt:

ADMIN
VENDOR
MODERATOR
CUSTOMER

Không tạo role mới.

Không tạo:

ADMIN_V2
MARKETING_ADMIN
BANNER_ADMIN
VOUCHER_ADMIN

nếu source/policy không có.

43. ADMIN / MODERATOR BOUNDARY

Phase 4A không được vô tình mở:

Moderator → Voucher management
Moderator → Banner management
Moderator → Admin Dashboard

nếu đây là Admin-only functionality.

Security phải ở backend:

Request
 ↓
Authentication
 ↓
Authorization
 ↓
Service

không chỉ menu/UI.

44. ACCOUNT STATUS

Nếu Security dùng:

ACTIVE
LOCKED
UNVERIFIED

thì phải reuse state hiện tại.

Không tự thêm:

BANNED

chỉ để Dashboard có metric:

Banned users

Current source audit chưa có BANNED trong UserStatus.

Do đó nếu dashboard design có “banned users”:

KHÔNG INVENT STATE

Phải xác định metric có source thực sự hay không.

45. SHOP STATUS

Current source có:

PENDING
APPROVED
REJECTED

Không tự thêm:

SUSPENDED

chỉ để Dashboard hiển thị:

Suspended Shops

Nếu Phase 3B/Policy đã tạo contract khác thì phải trace current source trước.

Nếu chưa có:

Metric unavailable

hoặc dependency gap.

Không fake.

46. PRODUCT STATUS

Current source có:

DRAFT
ACTIVE
OUT_OF_STOCK
HIDDEN

Dashboard/Voucher/Banner không được tự thêm:

REJECTED
PENDING_MODERATION

vào Product enum chỉ vì dashboard muốn đếm.

Nếu Phase 2 thực sự đã bổ sung state nhưng current source chưa có:

reconcile current source

trước khi sửa.

47. MONGODB SAFETY

Phase 4A tuyệt đối:

NO deleteAll()
NO deleteMany({})
NO drop()
NO dropDatabase()
NO truncate
NO reset collection
NO destructive migration

Không xóa dữ liệu Voucher/Banner hiện có để test.

Không xóa:

Order
Product
Shop
User
Review
Voucher
Banner

để “làm sạch dashboard”.

48. DASHBOARD TEST DATA

Nếu Dashboard không có dữ liệu:

KHÔNG seed hàng loạt

Nếu cần test:

1–2 records tối thiểu

và:

INSERT ONLY

Không delete sau test.

Không dùng test data để tạo:

fake revenue
fake GMV
fake voucher usage
fake banner impression
49. VOUCHER TEST DATA

Nếu cần:

1 test voucher

marker:

[TEST-PHASE-4A]

hoặc cơ chế marker tương đương nếu model hỗ trợ.

Không overwrite voucher hiện tại.

Không reuse production ID.

Không xóa test voucher sau test.

50. BANNER TEST DATA

Nếu cần:

1 test banner

để kiểm chứng:

PUBLISHED
UNPUBLISHED
startAt/endAt
priority

thì insert tối thiểu.

Không delete sau test.

Không dùng test banner để tạo production impression/metric giả.

51. DATA PRESERVATION CHECK

Trước khi test nếu có thể lấy baseline:

Voucher count
Banner count
Order count
Product count
Shop count
User count
Review count

Sau test:

Existing records preserved

Không cần reset database.

52. NO DESTRUCTIVE CRUD HARDENING

Nếu phát hiện:

DELETE

đang được dùng để “deactivate” Voucher/Banner:

phải inspect.

Không mặc định xóa document.

Nếu behavior hiện tại vi phạm data-preservation/business rule:

WRONG

→ minimal fix sang state update nếu policy/source hỗ trợ.

53. TRANSACTION / CONSISTENCY

Phase 4A không cần thêm transaction toàn hệ thống.

Chỉ kiểm tra operation:

Voucher update
Banner publish
Banner unpublish
Dashboard aggregation

có persistence nhất quán không.

Không thêm:

@Transactional

khắp nơi nếu Mongo architecture hiện tại không cần.

54. ERROR HANDLING

Kiểm tra:

invalid voucher id
invalid banner id
missing record
invalid status
unauthorized action
malformed input

Expected:

controlled error

Không expose:

stack trace
Mongo query
database internals
PII
55. VALIDATION

Voucher/Banner critical fields phải được validate theo model/policy hiện tại.

Không tự thêm business rule.

Ví dụ:

required field
date
status
id

chỉ khi source/policy yêu cầu.

56. FILTER/PAGINATION HARDENING

Cả Voucher và Banner phải kiểm tra:

page = 0
page > max
size = 0
size âm
filter rỗng
filter không hợp lệ

Không để request xấu gây:

500

nếu backend có thể xử lý hợp lệ.

57. BANNER TIME EDGE CASE

Nếu startAt/endAt thực sự tồn tại:

Kiểm tra:

now < startAt
startAt <= now <= endAt
now > endAt

Nhưng expected behavior phải lấy từ:

Policy/current source

không tự định nghĩa.

58. VOUCHER STATE EDGE CASE

Kiểm tra:

activate inactive
activate active
deactivate active
deactivate inactive
update inactive
update active

Expected phải theo current implementation/policy.

Không tự thêm transition state.

59. BANNER STATE EDGE CASE

Tương tự:

publish draft
publish published
unpublish published
unpublish unpublished
update published

Không tự định nghĩa state transition nếu source chưa quy định.

60. DASHBOARD FINANCIAL BOUNDARY

Phase 4A không phải Finance implementation phase.

Được phép:

identify wrong financial metric
fix source/label/query

Không được:

build full settlement engine
build payout engine
invent commission
invent fee
invent escrow

Nếu financial source chưa tồn tại:

INTEGRATION-READY

hoặc:

PARTIAL

tùy capability.

61. KHÔNG ĐÁNH ĐỒNG CÁC CONCEPT

Dashboard phải giữ distinction:

GMV
Vendor Sales
Platform Revenue
Platform Fee
Vendor Payout
Refund
Settlement

Không dùng:

Order Total

cho mọi metric.

62. EXISTING DASHBOARD METRIC RECONCILIATION

Mỗi metric hiện có lập bảng:

Metric hiện tại	Source hiện tại	Semantic đúng?	Action
Order-related count	Order	Verify	Keep/Fix
Product count	Product	Verify	Keep/Fix
Shop count	Shop	Verify	Keep/Fix
User count	User	Verify	Keep/Fix
Voucher count	Voucher	Verify	Keep/Fix
Banner count	Banner	Verify	Keep/Fix
totalPlatformRevenue	Delivered Order amount	Chưa chứng minh là Platform Revenue	Trace policy/source, fix semantic nếu cần

Điểm cuối là hardening bắt buộc, không được bỏ qua chỉ vì metric đã tồn tại.

63. VOUCHER ACCEPTANCE MATRIX
    Capability	Evidence bắt buộc
    Create	Controller → Service → Repository
    Read	Actual persisted data
    Update	Existing document updated
    Filter	Repository/query
    Pagination	Correct total/page
    Activate	Persisted state
    Deactivate	Persisted state
    Security	Backend authorization
    Ownership	Server-side check nếu có
    Validation	Invalid input handled
    Mongo safety	No destructive operation
64. BANNER ACCEPTANCE MATRIX
    Capability	Evidence bắt buộc
    Create	Persisted
    Read	Actual source
    Update	Existing document updated
    Filter	Actual backend query
    Pagination	Correct page/total
    Publish	Persisted
    Unpublish	Persisted
    Customer visibility	Actual customer query
    Time selection	Nếu source/policy yêu cầu
    Priority	Nếu source/policy yêu cầu
    Security	Backend authorization
    Mongo safety	No destructive operation
65. DASHBOARD ACCEPTANCE MATRIX
    Capability	Evidence
    Admin access	Security
    Metric	Backend source
    Query	Repository
    Data	MongoDB
    Semantic	Policy/source
    Empty state	Actual behavior
    Error handling	Controlled response
    Financial terminology	Correct distinction
    No fake data	Verified
    Performance	Only if acceptance impacted
66. REGRESSION

Không test lại toàn bộ hệ thống.

Tập trung:

Dashboard
Admin Login
→ Dashboard
Voucher
Admin
→ Voucher
→ Create
→ Update
→ Activate
→ Deactivate
→ Filter
→ Pagination
Banner
Admin
→ Banner
→ Create
→ Update
→ Publish
→ Unpublish
→ Filter
→ Pagination
Customer Banner
Customer
→ Banner query/view
Security
Moderator → Admin Dashboard/Voucher/Banner
Vendor → Admin Dashboard/Voucher/Banner
Customer → Admin Dashboard/Voucher/Banner
67. REGRESSION PHASE 1–3

Chỉ regression nếu Phase 4A sửa shared code:

Security
User
Shop
Product
Order
Audit
Repository
Common service

Nếu không đụng shared dependency:

không cần chạy lại toàn bộ Phase 1–3

Nhưng phải ghi rõ phạm vi regression.

68. FILE SCOPE DỰ KIẾN

Không mặc định tất cả đều phải sửa.

Ưu tiên inspect:

Admin Dashboard Controller
Dashboard Service
Dashboard Repository/query
Dashboard template

Voucher Controller
Voucher Service
Voucher Repository
Voucher entity/DTO
Voucher template

Banner Controller
Banner Service
Banner Repository
Banner entity/DTO
Banner template

SecurityConfig
User/Shop ownership logic nếu trực tiếp liên quan

Order repository/service
nếu Dashboard metric phụ thuộc trực tiếp
69. KHÔNG TẠO FILE NẾU KHÔNG CẦN

Nếu source hiện tại:

VoucherController
VoucherService
VoucherRepository

đã đủ:

FIX EXISTING

không tạo:

VoucherHardeningService
VoucherManagementService
VoucherAdminService

tương tự Banner/Dashboard.

70. DEPENDENCY FIX FORMAT

Nếu cần sửa dependency:

Dependency:
<name></name>

Current Behavior:
<actual></actual>

Phase 4A Impact:
<why Dashboard/Voucher/Banner fails>

Root Cause:
<actual root cause></actual>

Minimal Fix:
<change></change>

Regression:
<checked flows></checked>

Status:
FIXED / PARTIAL / WRONG / BLOCKED
71. STATUS CLASSIFICATION
IMPLEMENTED

Code tồn tại + behavior đúng + evidence phù hợp.

INTEGRATED

Đã verify end-to-end với real backend/data.

INTEGRATION-READY

Owned implementation hoàn thành nhưng external capability thật chưa có.

PARTIAL

Có code nhưng chưa đủ acceptance.

WRONG

Có code nhưng semantic/behavior/security sai.

MISSING

Không có implementation cần thiết.

OUT OF SCOPE

Không thuộc Phase 4A và không phải dependency trực tiếp.

BLOCKED

Chỉ khi technical blocker thực sự không thể tiếp tục.

72. ĐIỂM KHÔNG ĐƯỢC DÙNG OUT OF SCOPE

Ví dụ:

Dashboard revenue sai source

không được ghi:

OUT OF SCOPE – Finance

vì:

Dashboard metric

là Phase 4A.

Nhưng:

Full Settlement Engine

có thể:

OUT OF SCOPE

nếu không phải dependency trực tiếp.

73. BUILD RULE

Sau implementation:

mvn clean package -DskipTests

Không tự động:

mvn test
mvn verify
mvn install
mvn spring-boot:run

Nếu chưa runtime test:

NOT TESTED

Không ghi PASS.

74. MANUAL TEST — DASHBOARD

Nếu user yêu cầu runtime/manual test:

Test D1
Admin
→ Login
→ Dashboard

Expected:

ALLOW
Test D2
Dashboard
→ Metric
→ Backend
→ Actual database

Expected:

real value
Test D3
Customer/Vendor/Moderator
→ Admin Dashboard

Expected:

DENY

theo security implementation.

Test D4
Dashboard revenue

Phải xác minh:

semantic source

Không chỉ kiểm tra UI có số.

75. MANUAL TEST — VOUCHER
    Test V1
    Admin
    → Create Voucher

Verify:

MongoDB document exists
Test V2
Update

Verify:

same document updated
Test V3
Activate

Verify:

state persisted
Test V4
Deactivate

Verify:

state persisted
Test V5
Filter
Pagination

Verify backend query.

Test V6
Unauthorized actor
→ Voucher management

Expected:

DENY
76. MANUAL TEST — BANNER
Test B1
Admin
→ Create Banner

Verify persistence.

Test B2
Publish

Verify:

MongoDB state
Test B3
Customer
→ Banner

Verify published banner is actually returned.

Test B4
Unpublish

Verify customer no longer receives it, according to current source behavior.

Test B5

Nếu có:

startAt
endAt
priority

verify actual backend selection.

77. SECURITY TEST MATRIX
    Actor	Admin Dashboard	Voucher Admin	Banner Admin
    CUSTOMER	DENY	DENY	DENY
    VENDOR	DENY	DENY nếu platform-level	DENY nếu platform-level
    MODERATOR	DENY	DENY	DENY
    ADMIN	ALLOW	ALLOW	ALLOW
    ANONYMOUS	DENY	DENY	DENY

Matrix này là baseline cần kiểm tra; kết quả cuối cùng phải khớp security implementation/policy thực tế.

78. MONGODB SAFETY ACCEPTANCE

Bắt buộc:

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

Test records deleted:
NO

Existing Voucher deleted:
NO

Existing Banner deleted:
NO

Historical Order deleted:
NO

Nếu code Phase 4A chứa destructive reset để test:

FAIL
79. GIT SAFETY

Không:

commit
push
merge
rebase
reset
stash
checkout branch
switch branch

Không overwrite uncommitted changes.

Cuối Phase:

Commit:
NOT DONE

Push:
NOT DONE

Merge:
NOT DONE
80. FILE CHANGE REPORT

Cuối Phase 4A phải ghi:

Files Created:

- ...

Files Modified:

- ...

Files Deleted:
NONE

Nếu không tạo file mới:

Files Created:
NONE

Điều này hoàn toàn hợp lệ vì Phase 4A ưu tiên hardening existing implementation.

81. KNOWN ISSUE FORMAT

Mỗi issue:

Issue:
<name></name>

Current Behavior:
<actual></actual>

Expected:
<expected></expected>

Source:
<file/module>

Root Cause:
<if known></if>

Impact:
<Dashboard/Voucher/Banner>

Status:
IMPLEMENTED / PARTIAL / WRONG / MISSING / INTEGRATION-READY / BLOCKED
82. CÁC ISSUE DỰ KIẾN PHẢI ĐƯỢC KIỂM CHỨNG

Không mặc định coi tất cả là bug trước khi inspect, nhưng phải verify:

Issue A — Dashboard revenue semantic
Current:
totalPlatformRevenue lấy từ delivered order amount

Question:
Đó có thực sự là Platform Revenue theo Policy/source không?

Nếu không:

WRONG

→ minimal semantic/query fix.

Issue B — Voucher CRUD

Không được viết lại.

Phải xác minh:

CRUD
+
Persistence
+
Security
+
Status transition
Issue C — Banner CRUD

Không được viết lại.

Phải xác minh:

CRUD
+
Publish
+
Unpublish
+
Customer visibility
+
Persistence
Issue D — Banner effective selection

Chỉ xử lý nếu current model/policy có:

startAt
endAt
priority

Không tự phát minh nếu không có.

83. PHASE 4A KHÔNG LÀM

Không triển khai:

Full Finance
Full Settlement
Full Escrow
Full Refund Engine
Full Scheduler
New Moderation Engine
New Complaint Engine
New Escalation Engine
New Audit Engine
New KYC Engine
New Security Architecture

Không tạo:

DashboardV2
VoucherV2
BannerV2
FinanceV2
84. KHÔNG REBUILD CRUD

Đây là nguyên tắc trung tâm:

Existing CRUD
     ↓
Inspect
     ↓
Verify
     ↓
Find gap
     ↓
Minimal fix

Không:

Existing CRUD
     ↓
"chưa hoàn hảo"
     ↓
rewrite
85. ACCEPTANCE CRITERIA — DASHBOARD

Dashboard đạt khi:

Admin authorization đúng
        +
Metric có backend source
        +
Query lấy data thật
        +
Không hardcode
        +
Semantic đúng
        +
Không fake financial data
        +
Empty/error case không phá UI

Đặc biệt:

Platform Revenue

chỉ được gọi đúng tên khi source/policy chứng minh được.

86. ACCEPTANCE CRITERIA — VOUCHER

Voucher đạt khi:

Existing CRUD verified
+
Persistence verified
+
Filter verified
+
Pagination verified
+
Activate verified
+
Deactivate verified
+
Authorization verified
+
Ownership verified nếu applicable
+
Validation verified

Không cần rewrite architecture.

87. ACCEPTANCE CRITERIA — BANNER

Banner đạt khi:

Existing CRUD verified
+
Publish verified
+
Unpublish verified
+
Persistence verified
+
Customer visibility verified
+
Authority verified
+
Time/priority verified nếu source/policy có
88. PHASE 4A EVIDENCE MATRIX
Area	Source	DB	Security	UI	Runtime
Dashboard	Required	Required	Required	Required	If available
Voucher CRUD	Required	Required	Required	Required	If available
Voucher status	Required	Required	Required	Required	If available
Banner CRUD	Required	Required	Required	Required	If available
Banner publish	Required	Required	Required	Required	If available
Banner customer visibility	Required	Required	N/A	Required	If available
Revenue semantic	Required	Required	N/A	Required	If available
89. FINAL STATUS KHÔNG ĐƯỢC SUY RA TỪ BUILD

Ví dụ:

mvn clean package -DskipTests
→ PASS

chỉ chứng minh:

Compilation / packaging

Không chứng minh:

Voucher correct
Banner correct
Dashboard metric correct
Security correct
MongoDB persistence correct

Do đó:

Build PASS
≠
Phase 4A PASS
90. FINAL REPORT — PHASE 4A
Phase
PHASE 4A
DASHBOARD / VOUCHER / BANNER HARDENING
Overall Status

Chọn theo evidence:

IMPLEMENTED
INTEGRATED
INTEGRATION-READY
PARTIAL
MISSING
WRONG
BLOCKED

Không ghi DONE chung chung.

Dashboard
Dashboard:
<status></status>

Metric Source:
<status></status>

Platform Revenue Semantic:
<status></status>

Security:
<status></status>

Data Accuracy:
<status></status>
Voucher
CRUD:
<status></status>

Filter:
<status></status>

Pagination:
<status></status>

Activate:
<status></status>

Deactivate:
<status></status>

Security:
<status></status>

Persistence:
<status></status>
Banner
CRUD:
<status></status>

Publish:
<status></status>

Unpublish:
<status></status>

Customer Visibility:
<status></status>

Effective Time:
<status / NOT APPLICABLE>

Priority:
<status / NOT APPLICABLE>

Security:
<status></status>

Persistence:
<status></status>
91. BUILD REPORT
Command:
mvn clean package -DskipTests

Result:
PASS / FAIL

Nếu FAIL:

Compilation:
PASS / FAIL

Packaging:
PASS / FAIL

Root Cause:
...

Impact:
...

Status:
...
92. MANUAL TEST REPORT
Dashboard:
PASS / FAIL / NOT TESTED

Dashboard Data Source:
PASS / FAIL / NOT TESTED

Revenue Semantic:
PASS / FAIL / NOT TESTED

Voucher CRUD:
PASS / FAIL / NOT TESTED

Voucher Activate:
PASS / FAIL / NOT TESTED

Voucher Deactivate:
PASS / FAIL / NOT TESTED

Voucher Filter:
PASS / FAIL / NOT TESTED

Voucher Pagination:
PASS / FAIL / NOT TESTED

Voucher Security:
PASS / FAIL / NOT TESTED

Banner CRUD:
PASS / FAIL / NOT TESTED

Banner Publish:
PASS / FAIL / NOT TESTED

Banner Unpublish:
PASS / FAIL / NOT TESTED

Banner Customer Visibility:
PASS / FAIL / NOT TESTED

Banner Effective Time:
PASS / FAIL / NOT TESTED / NOT APPLICABLE

Banner Priority:
PASS / FAIL / NOT TESTED / NOT APPLICABLE

Banner Security:
PASS / FAIL / NOT TESTED

MongoDB Safety:
PASS
93. DATABASE REPORT

Bắt buộc:

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

Test records deleted:
NO

Voucher records deleted:
NO

Banner records deleted:
NO

Historical orders deleted:
NO
94. DEPENDENCY REPORT

Nếu không có:

Dependency Fixes:
NONE

Nếu có:

Dependency:
<module></module>

Current Behavior:
...

Phase 4A Impact:
...

Root Cause:
...

Minimal Fix:
...

Regression:
...

Status:
...
95. FINAL ARCHITECTURE CỦA PHASE 4A

Sau Phase 4A, kiến trúc phải giữ:

                    ADMIN
                      │
          ┌───────────┼───────────┐
          │           │           │
          ▼           ▼           ▼
     DASHBOARD      VOUCHER      BANNER
          │           │           │
          ▼           ▼           ▼
      SERVICE      SERVICE      SERVICE
          │           │           │
          ▼           ▼           ▼
     REPOSITORY   REPOSITORY   REPOSITORY
          │           │           │
          └───────────┼───────────┘
                      ▼
                   MongoDB

Customer Banner:

CUSTOMER
   ↓
Banner Query
   ↓
Banner Service
   ↓
Published / Effective Banner
   ↓
MongoDB

Dashboard:

ADMIN
  ↓
Dashboard
  ↓
Dashboard Service
  ├── Order source
  ├── Product source
  ├── Shop source
  ├── User source
  ├── Voucher source
  ├── Banner source
  └── Financial source nếu thực sự tồn tại

Không tạo:

Dashboard V2
Voucher V2
Banner V2
96. PHASE 4A FINAL RULE

Trọng tâm của phase này là:

ĐÃ CÓ
   ↓
VERIFY

SAI
   ↓
MINIMAL FIX

THIẾU TRỰC TIẾP
   ↓
IMPLEMENT

ĐÚNG
   ↓
REUSE

KHÔNG CÓ EVIDENCE
   ↓
NOT TESTED / PARTIAL

EXTERNAL CAPABILITY CHƯA CÓ
   ↓
INTEGRATION-READY

KHÔNG THUỘC PHASE
   ↓
OUT OF SCOPE

Đặc biệt:

Voucher CRUD đã có
→ KHÔNG viết lại CRUD.

Banner CRUD đã có
→ KHÔNG viết lại CRUD.

Dashboard đã có
→ KHÔNG mặc định coi metric là đúng.

totalPlatformRevenue
→ PHẢI verify semantic/source.

MongoDB
→ KHÔNG reset/xóa dữ liệu.

Test data
→ INSERT ONLY, tối thiểu, không cleanup.

Security
→ Backend enforcement, không chỉ UI.

Build
→ mvn clean package -DskipTests.

Git
→ Không commit/push/merge/reset/stash/checkout.

Chuỗi thực thi cuối cùng
GATE 4A-1
Discovery + Git Safety
        ↓
GATE 4A-2
Existing Implementation Mapping
        ↓
GATE 4A-3
Dashboard Source/Metric Verification
        ↓
GATE 4A-4
Dashboard Financial Semantic Hardening
        ↓
GATE 4A-5
Voucher CRUD + State + Security Verification
        ↓
GATE 4A-6
Banner CRUD + Publish + Visibility Verification
        ↓
GATE 4A-7
IDOR / Authorization / Validation Hardening
        ↓
GATE 4A-8
MongoDB Data Preservation Verification
        ↓
GATE 4A-9
Targeted Regression
        ↓
GATE 4A-10
mvn clean package -DskipTests
        ↓
GATE 4A-11
Final Evidence Report

Điểm quan trọng nhất của Phase 4A: với code hiện tại, đây nên là một phase hardening + verification có trọng tâm, không phải phase CRUD. Hai vùng có implementation khá rõ là Voucher và Banner, nên mặc định phải reuse và kiểm chứng. Vùng cần scrutiny cao nhất là Dashboard, đặc biệt totalPlatformRevenue, vì việc lấy tổng tiền đơn đã giao để gọi là “platform revenue” chưa đủ cơ sở về mặt financial semantics. Không được giải quyết khoảng trống đó bằng một commission/fee giả định
