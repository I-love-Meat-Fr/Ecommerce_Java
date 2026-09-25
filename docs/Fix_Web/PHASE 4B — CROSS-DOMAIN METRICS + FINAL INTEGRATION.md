
PHASE 4B — CROSS-DOMAIN METRICS + FINAL INTEGRATION

Nguyên tắc của Phase 4B: đây là phase tích hợp và xác minh metric giữa các domain đã tồn tại, không phải phase để dựng mới Finance/Settlement/Payment/Refund chỉ nhằm làm Dashboard có số liệu.

Đặc biệt: Finance/Risk metrics chỉ được triển khai khi domain nguồn thực sự tồn tại và có source of truth. Không tạo số giả, không suy diễn financial meaning từ những field không đủ semantics.

0. VỊ TRÍ CỦA PHASE 4B

Phase 4B nằm sau:

Phase 4A
Dashboard / Voucher / Banner Hardening
        ↓
Phase 4B
Cross-domain Metrics

+ Final Integration
+ Data Source Verification
+ Security / Consistency Verification
  ↓
  FINAL VERIFICATION

Phase 4B không thay thế Phase 4A.

Phase 4A tập trung vào:

Dashboard UI / existing dashboard
Voucher
Banner
Authority
Security

Phase 4B tập trung vào:

Cross-domain data
        ↓
Metrics
        ↓
Dashboard/service consumers
        ↓
Integration consistency
        ↓
Final verification

1. MỤC TIÊU

Phase 4B phải trả lời chính xác:

Metric này lấy từ đâu?
        ↓
Domain nguồn có thực sự tồn tại không?
        ↓
Có repository/query thật không?
        ↓
Có business semantics đủ rõ không?
        ↓
Có security boundary không?
        ↓
Có thể aggregate an toàn không?
        ↓
Dashboard/API đang đọc đúng source không?

Mục tiêu chính:

Xác minh source of truth của từng metric.
Kết nối các metric giữa nhiều domain đã tồn tại.
Fix dependency trực tiếp làm cross-domain integration sai.
Tách operational metrics khỏi financial metrics.
Tách financial concepts không được phép gộp.
Xác minh Dashboard không dùng số giả.
Xác minh Admin/Moderator chỉ nhìn thấy metric thuộc quyền.
Xác minh historical data không bị thay đổi trái phép.
Xác minh metric không làm thay đổi business state.
Kiểm tra consistency giữa các domain.
Regression các flow Phase 1–3.
Final build và final integration evidence.
2. CURRENT-CODE BASELINE PHẢI BÁM SÁT

Theo phần đối chiếu source hiện tại đã xác định:

Đã tồn tại
MODERATOR trong UserRole.
/moderator/** có security requirement ROLE_MODERATOR.
Admin Product / Review / Shop / Order / Voucher / Banner đã có implementation.
Voucher có CRUD/filter/pagination và activate/deactivate.
Banner có CRUD/publish/unpublish/filter/pagination.
Customer chỉ hiển thị Banner PUBLISHED.
Product có:
DRAFT
ACTIVE
OUT_OF_STOCK
HIDDEN
Shop có:
PENDING
APPROVED
REJECTED
User có:
ACTIVE
LOCKED
UNVERIFIED
Những điểm phải đặc biệt lưu ý khi làm 4B

Current dashboard totalPlatformRevenue đang dựa trên tổng amount của delivered orders.

Điều này không được tự động gọi là:

Platform Revenue

nếu chưa có domain/policy chứng minh rằng delivered-order amount chính là platform revenue.

Do đó Phase 4B phải sửa theo hướng:

Current delivered-order amount
        ↓
Xác định semantics thực tế
        ↓
Operational Order Amount / GMV-like metric

và chỉ gọi là:

Platform Revenue

khi có source of truth tương ứng.

Ngoài ra, hiện trạng source audit chưa cho thấy đầy đủ domain production cho:

Settlement
Vendor Payout
Escrow
Platform Fee
Financial Ledger
Risk Score

Vì vậy không được dựng các metric này bằng cách lấy Order.total hoặc một field tương tự rồi đổi tên.

3. HARD RULE — CONDITIONAL IMPLEMENTATION

Phase 4B chia metric thành hai nhóm.

Nhóm A — Domain đã tồn tại

Ví dụ:

User
Shop
Product
Order
Review
Voucher
Banner
ReportCase
Violation
Complaint
Escalation

Nếu source thực tế có:

Entity
+
Repository
+
Service/query
+
State semantics

thì Phase 4B có thể aggregate.

Nhóm B — Domain nguồn chưa tồn tại đầy đủ

Ví dụ:

Settlement
Vendor Payout
Platform Fee
Escrow Balance
Risk Score
Financial Ledger

thì:

KHÔNG TẠO METRIC GIẢ

Không được làm:

platformRevenue = orderTotal
vendorPayout = orderTotal
platformFee = orderTotal * 0.1
riskScore = violationCount * 10

nếu Policy/current source không định nghĩa những phép tính này.

Status:

INTEGRATION-READY

hoặc:

MISSING

tùy trường hợp.

4. SOURCE OF TRUTH HIERARCHY

Phase 4B sử dụng:

1. Phase 4 scope
2. Business Policy
3. Current source code
4. Verified Phase 3 result
5. Verified Phase 2 result
6. Verified Phase 1 result
7. Audit/final report
8. Shopee model

Không lấy tên field làm business meaning.

Ví dụ:

totalAmount

chỉ chứng minh có một amount.

Nó không tự chứng minh:

GMV
Platform Revenue
Vendor Sales
Vendor Payable
Settlement
5. DISCOVERY FLOW

Trước khi sửa metric:

Metric
 ↓
Controller / Dashboard
 ↓
Service
 ↓
Repository
 ↓
Entity
 ↓
Actual fields
 ↓
State
 ↓
MongoDB document
 ↓
Policy

Phải ghi lại:

Source:
<domain></domain>

Collection:
<collection nếu xác định được>

Field:
<field></field>

Filter:
<condition></condition>

Aggregation:
<logic></logic>

Business meaning:
<verified meaning></verified>

Status:
<status></status>
6. METRIC CLASSIFICATION

Mỗi metric phải được phân loại trước khi code.

6.1 Operational Metric

Ví dụ:

Total Users
Active Shops
Pending Shops
Active Products
Hidden Products
Pending Reports
Open Complaints
Open Escalations
Violations
Orders
Delivered Orders

Đây là nhóm dễ tích hợp nhất nếu source tồn tại.

6.2 Moderation Metric
Pending Product Moderation
Pending Review Moderation
Open ReportCase
Resolved ReportCase
Open Violation
Open Escalation

Chỉ triển khai nếu domain/state tương ứng tồn tại.

Không tự tạo status chỉ để count.

6.3 Customer/Order Metric

Ví dụ:

Orders
Delivered Orders
Cancelled Orders
Order Amount

Phải xác định:

count
+
status
+
time range
+
ownership

trước khi aggregate.

6.4 Financial Metric

Phải tách:

GMV
Vendor Sales
Platform Revenue
Platform Fee
Vendor Payable
Vendor Payout
Refund
Settlement

Không được dùng một field đại diện cho tất cả.

6.5 Risk Metric

Ví dụ:

Violation Count
Complaint Count
Escalation Count
Repeat Violations
Risk Score

Trong đó:

Violation Count
Complaint Count
Escalation Count

có thể là factual metrics nếu domain tồn tại.

Nhưng:

Risk Score

là một derived business metric.

Chỉ tạo khi Policy hoặc source hiện tại định nghĩa rõ:

formula
weights
time window
status
scope

Không tự phát minh scoring.

7. METRIC SOURCE CONTRACT

Mỗi metric Phase 4B phải có contract dạng:

Metric:
Pending Escalations

Source Domain:
Escalation

Source Collection:
<verified collection></verified>

Source State:
OPEN

Query:
countByStatus(OPEN)

Authority:
ADMIN

PII:
NONE / LIMITED

Financial:
NO

Status:
IMPLEMENTED / INTEGRATED / ...

Điều này giúp tránh:

Dashboard → random repository → number
8. DASHBOARD METRIC ARCHITECTURE

Kiến trúc mục tiêu:

Dashboard Controller
        ↓
Dashboard Service
        ↓
Metric Query / Domain Services
        ↓
Repositories
        ↓
MongoDB

Không:

Controller
 ↓
MongoDB trực tiếp

Không:

Template
 ↓
hardcoded number

Không:

JavaScript
 ↓
fake metric
9. KHÔNG TẠO DASHBOARD V2

Nếu Dashboard hiện tại tồn tại:

REUSE

Không tạo:

AdminDashboardV2
DashboardV2
FinanceDashboardV2
MetricDashboardV2

chỉ vì metric hiện tại chưa đầy đủ.

Nếu architecture hiện tại thiếu abstraction cần thiết:

Minimal extension

không duplicate toàn bộ Dashboard.

10. OPERATIONAL CROSS-DOMAIN METRICS

Ưu tiên hoàn thiện nhóm metric không cần financial domain mới.

Ví dụ:

Users
Shops
Products
Orders
Reviews
Reports
Complaints
Violations
Escalations

Các metric này có thể liên kết:

User
 ↓
Shop
 ↓
Product
 ↓
Order
 ↓
Complaint
 ↓
Violation
 ↓
Escalation

Nhưng mỗi count vẫn phải lấy từ domain source tương ứng.

11. USER METRICS

Chỉ dùng các status đã tồn tại.

Current source:

ACTIVE
LOCKED
UNVERIFIED

Do đó có thể xem xét:

Active Users
Locked Users
Unverified Users

Không tự thêm:

BANNED USERS
SUSPENDED USERS

nếu source chưa có status tương ứng.

12. SHOP METRICS

Current source có:

PENDING
APPROVED
REJECTED

Có thể aggregate:

Pending Shops
Approved Shops
Rejected Shops

Không tự tạo:

Suspended Shops

chỉ vì Dashboard cũ mong muốn metric đó.

Nếu Phase 3B/Phase 4A thực sự bổ sung enforcement state:

inspect current source first

sau đó mới aggregate.

13. PRODUCT METRICS

Current Product state:

DRAFT
ACTIVE
OUT_OF_STOCK
HIDDEN

Có thể tạo:

Draft Products
Active Products
Out-of-stock Products
Hidden Products

Nhưng phải phân biệt:

Hidden

với:

Moderation Rejected

nếu source không có rejected state.

Không map:

HIDDEN = REJECTED

một cách tự suy diễn.

14. ORDER METRICS

Order metrics phải dùng Order source hiện tại.

Có thể có:

Total Orders
Delivered Orders
Cancelled Orders

nếu các state thực sự tồn tại.

Nếu Dashboard đang lấy:

sum delivered order amount

thì phải đặt tên metric theo semantics thực tế.

Không tự gọi:

Platform Revenue

chỉ vì đó là số tiền của delivered orders.

15. GMV / ORDER VALUE

Nếu Policy và source cho phép xác định:

GMV

thì metric phải trace được:

Order
 ↓
eligible status
 ↓
eligible amount
 ↓
aggregation

Cần xác định rõ:

Cancelled included?
Refunded included?
Returned included?
Discount included?
Voucher included?
Shipping included?

Nếu Policy/source không trả lời:

DO NOT INVENT

Status:

INTEGRATION-READY

hoặc:

PARTIAL
16. PLATFORM REVENUE

Đây là metric phải kiểm soát chặt.

Không được:

Platform Revenue
================

SUM(Order.totalAmount)

trừ khi Policy/source explicitly định nghĩa như vậy.

Cần có:

Platform Revenue Source

và nếu có:

Platform Fee
+
Eligible Transactions
+
Financial State

thì aggregate từ financial domain.

Nếu chưa có:

Platform Revenue:
INTEGRATION-READY / MISSING

Không fake số.

17. VENDOR SALES

Không đồng nhất:

Vendor Sales
============

Platform Revenue

Vendor Sales có source riêng nếu business model định nghĩa.

Nếu chưa có financial source:

Do not create derived financial field

chỉ để Dashboard hiển thị.

18. PLATFORM FEE

Chỉ tính khi có:

Policy
+
fee source/rule

Không viết:

fee = orderTotal.multiply(BigDecimal.valueOf(0.1));

nếu Policy/source không có 10%.

Không hardcode:

2%
5%
10%
19. VENDOR PAYABLE / PAYOUT

Chỉ làm khi domain source tồn tại:

Vendor Payable
Vendor Payout
Settlement

Nếu chưa có:

Metric = INTEGRATION-READY

Không suy ra:

payout = orderTotal

hoặc:

payout = orderTotal - fee

nếu settlement semantics chưa tồn tại.

20. REFUND METRICS

Chỉ aggregate:

Refund Count
Refund Amount
Refunded Orders

khi Refund domain thực sự tồn tại.

Phải phân biệt:

Refund Requested
Refund Approved
Refund Completed
Refund Failed

nếu source có những state này.

Không lấy:

Complaint

để giả làm:

Refund
21. SETTLEMENT METRICS

Nếu Settlement source tồn tại:

Pending Settlement
Completed Settlement
Failed Settlement
Settlement Amount

phải lấy từ Settlement source.

Nếu không có:

Settlement Metrics:
MISSING / INTEGRATION-READY

Không fake bằng Order.

22. ESCROW METRICS

Nếu Escrow domain tồn tại:

Held Amount
Released Amount
Frozen Amount

phải lấy từ Escrow source.

Nếu không:

DO NOT CREATE ESCROW METRIC

chỉ để hoàn thành Dashboard.

23. RISK METRICS

Risk metrics chia thành hai cấp.

Factual
Violation Count
Complaint Count
Escalation Count
Repeat Violation Count

nếu source hỗ trợ.

Derived
Risk Score
Risk Level
High-risk Vendor

chỉ làm khi Policy/source có rule.

Không tạo:

riskScore = violations * 10

một cách tự phát minh.

24. CROSS-DOMAIN VIOLATION METRICS

Ví dụ:

Vendor
 ↓
Complaint
 ↓
Violation

Metric:

Violations per Vendor

phải xác định:

vendorId
violation source
status
time range

Không join bằng tên vendor.

Ưu tiên:

stable ID
25. CROSS-DOMAIN COMPLAINT METRICS

Ví dụ:

Open Complaints
Complaints by Shop
Complaints by Order
Complaints escalated

Không count complaint bằng:

Order status

nếu Complaint là domain riêng.

Complaint phải lấy từ Complaint source.

26. ESCALATION METRICS

Nếu Phase 3B đã tạo persistence:

Open Escalations
Resolved Escalations
Escalations by Source
Escalations by Status

phải đọc từ:

Escalation repository

Không đếm:

Complaint

rồi gọi đó là escalation.

27. AUDIT METRICS

Audit là historical event source.

Có thể dùng cho:

Enforcement actions
Admin decisions
Moderator decisions
Security-sensitive actions

Nhưng:

AuditLog không phải source of truth thay thế cho business state.

Ví dụ:

Audit says SHOP_SUSPENDED

không có nghĩa:

Shop.status = SUSPENDED

nếu Shop source không phản ánh state đó.

Metric business state lấy từ business domain.

Audit dùng để:

trace
verify
history
28. TIME-BASED METRICS

Nếu Dashboard có:

Today
This Week
This Month

phải xác định:

timezone
start boundary
end boundary
timestamp field

Không dùng:

String date comparison

nếu source đã có date type.

Không hardcode timezone nếu application đã có configuration.

29. METRIC IMMUTABILITY

Metric query:

READ ONLY

Không được Dashboard request làm:

update order
update shop
create violation
create refund

Dashboard phải:

read
aggregate
return
30. SECURITY BOUNDARY CHO METRICS

Không phải mọi metric đều public.

Ví dụ:

Customer

không được xem:

Platform Revenue
Vendor Payout
Risk Score
Admin Escalation
Internal Violation

nếu policy/security không cho phép.

31. ADMIN METRICS

Admin có thể xem các metric thuộc Admin scope nếu source hỗ trợ:

Users
Shops
Products
Orders
Complaints
Violations
Escalations
Financial metrics

nhưng từng metric vẫn phải:

server-side authorization

Không chỉ:

hide HTML element
32. MODERATOR METRICS

Moderator chỉ xem metric thuộc moderation scope.

Ví dụ:

Pending Cases
Reports
Complaints
Violations
Escalations

Không tự mở:

Revenue
Vendor Payout
Settlement
Platform Fee

nếu đây là Admin/Finance scope.

33. VENDOR METRICS

Vendor chỉ xem dữ liệu thuộc:

own shop
own products
own orders
own permitted financial data

Không:

all marketplace orders
other shop metrics
platform revenue
other vendor risk
34. CUSTOMER METRICS

Customer không được xem internal marketplace metrics.

Customer-facing metrics nếu có:

own orders
own reviews
own complaints

phải dùng ownership từ authenticated principal.

Không trust:

customerId

từ request.

35. IDOR TRONG METRICS

Kiểm tra:

Vendor A
→ /dashboard/vendor/B

Expected:

DENY

Tương tự:

Customer A
→ Customer B metrics

và:

Moderator
→ Admin metrics
36. METRIC OWNERSHIP

Mỗi metric phải xác định:

Global
Shop-scoped
Vendor-scoped
Customer-scoped
Moderator-scoped
Admin-only

Ví dụ:

Total Marketplace Orders

là:

Global / Admin

trong khi:

My Shop Orders

là:

Shop-scoped

Không dùng cùng query cho hai scope nếu điều kiện filter khác nhau.

37. CROSS-DOMAIN JOIN SAFETY

MongoDB không có nghĩa là được join tùy tiện trong service.

Trước khi aggregate:

Identify source IDs
        ↓
Verify relationship
        ↓
Query
        ↓
Aggregate

Không join bằng:

name
email
displayName
title

nếu stable ID tồn tại.

38. DATA CONSISTENCY

Ví dụ:

Order
Shop
Product

Nếu Product bị:

HIDDEN

không được làm:

historical order count = 0

Historical Order vẫn phải tồn tại.

Tương tự:

Shop rejected/locked/enforced

không được xóa transaction history.

39. FINANCIAL HISTORICAL DATA

Metric query không được:

modify
delete
recalculate-and-overwrite

financial records.

Nếu financial source có historical records:

READ
AGGREGATE
REPORT

Không sửa database chỉ để dashboard cho ra số đẹp.

40. NO DATA NORMALIZATION BY DELETION

Nếu phát hiện:

duplicate
inconsistent
legacy record

không:

delete old record

để metric đẹp hơn.

Phải:

Identify
↓
Report
↓
Determine safe correction
↓
Minimal non-destructive correction

Nếu chưa đủ information:

Known Issue
41. METRIC CACHING

Không tự thêm Redis/cache chỉ vì Dashboard query chậm.

Trước tiên:

Correctness

sau đó:

Performance

Nếu query performance trực tiếp ảnh hưởng acceptance:

minimal optimization

Nếu không:

OUT OF SCOPE
42. QUERY PERFORMANCE

Inspect:

count
sum
group
sort
date range
status filter
owner filter

Nếu collection lớn và query cần index:

minimal index

có thể được thêm nếu an toàn.

Index change phải:

non-destructive

Không migration kiểu:

drop collection
rebuild data
43. NULL / MISSING DATA

Metric phải xử lý:

field missing
null
empty collection

theo semantics.

Không biến mọi trường hợp thành:

0

nếu 0 và unknown/unavailable khác nhau.

Đặc biệt financial metrics:

NO DATA

không nhất thiết đồng nghĩa:

0 VND
44. FINANCE AVAILABILITY CONTRACT

Đây là gate quan trọng của Phase 4B.

Trước mỗi financial metric:

Financial Domain Exists?
        │
        ├── NO
        │    ↓
        │  Do not build fake metric
        │    ↓
        │  INTEGRATION-READY / MISSING
        │
        └── YES
             ↓
       Source of Truth Verified?
             │
             ├── NO → PARTIAL / WRONG
             │
             └── YES
                  ↓
             Policy Verified?
                  ↓
             Implement Metric
45. RISK AVAILABILITY CONTRACT

Tương tự:

Risk Domain Exists?
        ↓
Risk Rule Exists?
        ↓
Formula Exists?
        ↓
Time Window Exists?
        ↓
Source Fields Exist?
        ↓
Implement

Nếu thiếu bất kỳ phần business-critical nào:

DO NOT INVENT
46. DASHBOARD DISPLAY CONTRACT

Nếu metric không available:

Không hiển thị:

0

chỉ để tránh UI trống.

Có thể dùng semantics phù hợp architecture hiện tại như:

Unavailable
Not available
Integration-ready

nhưng phải nhất quán với frontend/backend hiện tại.

Không giả vờ rằng:

0 = no financial activity

khi thực tế:

financial source unavailable
47. METRIC API CONTRACT

Mỗi response phải rõ:

metric
value
scope
period
source/state nếu architecture hỗ trợ

Không thêm DTO V2 nếu DTO hiện tại có thể mở rộng an toàn.

Ví dụ conceptual:

orders
deliveredOrders
complaints
violations
escalations

lấy từ source thật.

Financial fields chỉ xuất hiện nếu financial source thật tồn tại.

48. KHÔNG LÀM FAKE FINANCE

Tuyệt đối không tạo:

FinanceService

chỉ có:

return orderService.total();

rồi expose:

platformRevenue
vendorPayout
settlement

Đó là false integration.

49. KHÔNG LÀM FAKE RISK

Không tạo:

RiskService

chỉ có:

violations * arbitraryWeight

nếu Policy không có risk scoring.

Factual violation count vẫn có thể được báo cáo nếu Violation domain tồn tại.

50. CROSS-DOMAIN FLOW

Mục tiêu integration:

User
 ↓
Shop
 ↓
Product
 ↓
Order
 ↓
Review
 ↓
Complaint
 ↓
Violation
 ↓
Escalation
 ↓
Admin
 ↓
Audit

Phase 4B không nhất thiết tạo mới các domain trên.

Nó phải chứng minh:

các domain hiện tại liên kết đúng

và:

metrics đọc đúng từ từng domain
51. CUSTOMER → ORDER METRICS

Verify:

Customer
 ↓
Order

Metric:

My Orders

phải filter theo authenticated customer.

Không:

findAllOrders()

rồi filter ở frontend.

52. VENDOR → SHOP → ORDER METRICS

Verify:

Authenticated Vendor
 ↓
Own Shop
 ↓
Own Orders

Không trust:

shopId

do frontend gửi nếu backend có thể derive.

53. PRODUCT → ORDER INTEGRATION

Không để:

Product hidden

làm historical metrics mất Order.

Metric cần phân biệt:

Current Active Products

và:

Historical Product Orders
54. MODERATION → VIOLATION

Nếu moderation decision tạo Violation:

Decision
 ↓
Violation

metric violation phải đọc từ Violation source.

Không đếm:

Rejected moderation cases

rồi gọi đó là:

Violation count

nếu hai domain có semantics khác nhau.

55. COMPLAINT → ESCALATION

Nếu Complaint được escalate:

Complaint
 ↓
Escalation

metric:

Open Escalations

phải đọc Escalation persistence.

Không:

Complaint.status = ESCALATED

rồi đếm complaint để thay thế Escalation nếu Escalation domain đã tồn tại.

56. ESCALATION → ADMIN

Admin Dashboard phải đọc:

Escalation Repository

và có thể trace:

Escalation
 ↓
source case
 ↓
actor
 ↓
decision
 ↓
audit

nếu các domain đó tồn tại.

57. ENFORCEMENT → METRIC

Nếu Admin enforcement làm thay đổi business state:

Admin Enforcement
 ↓
Target Domain
 ↓
State

metric phải đọc:

Target Domain

không đọc audit event làm source duy nhất.

Ví dụ:

Shop enforcement

thì Shop metric lấy từ Shop.

Audit dùng để verify lịch sử action.

58. AUDIT CROSS-CHECK

Có thể dùng AuditLog để đối chiếu:

Business state
vs
Historical action

Ví dụ:

Shop current state
+
latest enforcement audit

Nhưng không được giả định:

Audit = current state
59. VOUCHER METRICS

Nếu Voucher source hiện tại tồn tại:

Có thể verify:

Total Vouchers
Active Vouchers
Inactive Vouchers
Platform Vouchers
Shop Vouchers

chỉ khi type/authority thực sự tồn tại trong source.

Không tạo:

Voucher Usage
Voucher Savings
Voucher Revenue Impact

nếu source không lưu usage/financial semantics.

60. BANNER METRICS

Nếu Banner source tồn tại:

Có thể aggregate:

Published
Unpublished
Scheduled/effective

nếu status/time fields tồn tại.

Không tự tạo:

Impressions
Clicks
CTR
Revenue Impact

nếu không có event/analytics source.

61. BANNER EFFECTIVE METRIC

Nếu source có:

startAt
endAt
status
priority

effective selection phải dựa trên backend logic.

Không:

UI currently showing banner

để suy ra metric backend.

62. VOUCHER AUTHORITY

Metric phải respect:

Platform voucher
Shop voucher

nếu source có distinction.

Vendor không được nhìn aggregate platform voucher data ngoài permission.

63. NOTIFICATION

Nếu Notification source tồn tại:

Có thể verify:

notifications sent
pending
failed

nhưng chỉ nếu source lưu trạng thái.

Không:

logger.info("notification sent")

rồi count log làm metric.

64. SCHEDULER METRICS

Nếu Scheduler/Job persistence có source:

Có thể metric:

job executions
failed jobs
pending jobs

nếu architecture hỗ trợ.

Nếu scheduler chỉ là:

@Scheduled

không có execution persistence:

Do not invent historical job count.
65. SCHEDULER → ESCALATION

Đặc biệt verify:

Overdue case
 ↓
Scheduler
 ↓
Escalation

Một case chạy nhiều lần:

1 intended escalation

không:

N duplicate escalation records
66. METRIC IDEMPOTENCY

Metric query phải read-only, nên:

same DB state
+
same query
==========

same metric

trừ khi metric có time-dependent semantics.

Không để Dashboard request tạo side effect.

67. CONCURRENCY

Kiểm tra các write path ảnh hưởng metric:

Admin enforcement
Moderator decision
Scheduler
Complaint resolution
Refund

Nếu có race condition:

inspect current architecture

Chỉ thêm mechanism cần thiết.

Không xây distributed locking toàn hệ thống.

68. TRANSACTION BOUNDARY

Phase 4B chủ yếu là read/aggregation.

Không thêm transaction chỉ cho:

count
sum
aggregate

Nếu metric phụ thuộc multi-write consistency:

inspect write path

và sửa dependency nếu trực tiếp ảnh hưởng correctness.

69. MONGODB SAFETY

Phase 4B tuyệt đối không:

deleteAll()
deleteMany({})
drop()
dropDatabase()
truncate
reset
collection recreation

Không sửa dữ liệu cũ chỉ để:

metric = expected number
70. TEST DATA

Nếu cần verify cross-domain metrics:

1–2 minimal records

và chỉ tạo nếu thật sự cần.

Ví dụ:

1 test Order
1 test Complaint
1 test Escalation

Không tạo hàng trăm records để làm Dashboard đẹp.

Báo:

Test records inserted:
<number></number>

và:

Test records deleted:
NO
71. EXISTING DATA PRESERVATION

Phải verify:

Existing Orders:
preserved

Reviews:
preserved

Products:
preserved

Shops:
preserved

Voucher:
preserved

Banner:
preserved

Escalations:
preserved

Audit:
preserved

Financial records:
preserved

nếu các domain đó tồn tại.

72. METRIC CORRECTNESS TEST

Mỗi metric phải có test logic tối thiểu:

Known source data
        ↓
Expected aggregate
        ↓
Actual aggregate

Ví dụ:

3 ACTIVE products
2 HIDDEN products
1 DRAFT product

thì:

Active = 3
Hidden = 2
Draft = 1

Không cần tạo production-like dataset lớn.

73. ZERO VS UNAVAILABLE

Đây là acceptance quan trọng.

Nếu:

Settlement domain không tồn tại

không trả:

settlement = 0

nếu điều đó làm người dùng hiểu:

có settlement domain nhưng chưa có settlement.

Thay vào đó status phải thể hiện:

UNAVAILABLE / INTEGRATION-READY

theo contract hiện tại.

74. FINANCIAL METRIC GATE

Trước khi thêm bất kỳ field nào dưới nhóm Finance:

Metric:
<name></name>

Policy source:
<verified / missing>

Domain source:
<verified / missing>

Repository:
<verified / missing>

Business formula:
<verified / missing>

Historical data:
<available / unavailable>

Status:
...

Nếu thiếu formula:

DO NOT IMPLEMENT
75. RISK METRIC GATE

Tương tự:

Risk metric:
<name></name>

Source:
<Violation / Complaint / other>

Rule:
<verified / missing>

Formula:
<verified / missing>

Time window:
<verified / missing>

Status:
...

Không có formula:

No derived risk score.
76. CURRENT REVENUE GAP

Đây là dependency cần đặc biệt ghi vào report.

Current implementation có logic kiểu:

delivered order amount

được dùng cho:

totalPlatformRevenue

Phase 4B phải kiểm tra lại semantics.

Nếu Policy không định nghĩa delivered-order total là Platform Revenue:

Current Behavior:
Delivered Order Amount is surfaced as Platform Revenue

Expected:
Financial metric must use verified financial source

Root Cause:
Dashboard semantic mismatch

Status:
WRONG / PARTIAL

Cách sửa phải là:

rename/reclassify metric

hoặc:

connect real finance source

không được sửa bằng cách hardcode một công thức tài chính mới.

77. FINANCE DOMAIN MISSING

Nếu sau khi inspect vẫn không có:

Settlement
Vendor Payout
Platform Fee
Escrow
Financial Ledger

thì Phase 4B không được tự xây toàn bộ các domain đó.

Report:

Finance Metrics:
INTEGRATION-READY / MISSING

và ghi:

Reason:
Source financial domain unavailable.
No safe financial calculation can be derived from current Order data.
78. RISK DOMAIN MISSING

Nếu không có:

Risk Engine
Risk Score
Risk Policy

thì:

Risk Score:
MISSING / OUT OF SCOPE

nhưng:

Violation Count:
IMPLEMENTED

nếu Violation domain thực sự tồn tại.

Không đánh đồng hai khái niệm.

79. CROSS-DOMAIN METRIC MATRIX

Nên lập bảng:

Metric	Source domain	Source exists	Business rule exists	Calculation	Status
Users	User	Có	Có	Count	Verify
Active Shops	Shop	Có	Có	status=APPROVED nếu đúng semantics	Verify
Active Products	Product	Có	Có	status=ACTIVE	Verify
Orders	Order	Có	Có	Count	Verify
Delivered Orders	Order	Có	Có	Status filter	Verify
Reviews	Review	Có	Có	Count	Verify
Complaints	Complaint	Có/verify	Policy	Count	Conditional
Violations	Violation	Có/verify	Policy	Count	Conditional
Escalations	Escalation	Phase 3B dependent	Policy	Count	Conditional
Platform Revenue	Finance	Chưa xác nhận đầy đủ	Policy required	Financial source	Conditional
Vendor Payout	Settlement	Chưa xác nhận	Policy required	Settlement source	Conditional
Settlement	Settlement	Chưa xác nhận	Policy required	Settlement source	Conditional
Risk Score	Risk	Chưa xác nhận	Formula required	Derived	Conditional
80. STATUS CỦA METRIC

Chỉ dùng:

IMPLEMENTED
INTEGRATED
INTEGRATION-READY
PARTIAL
MISSING
WRONG
OUT OF SCOPE
BLOCKED

Không dùng:

DONE

làm status kỹ thuật.

81. INTEGRATED

Metric chỉ được:

INTEGRATED

khi đã chứng minh:

Source exists
+
Query exists
+
Correct aggregation
+
Correct security
+
Actual data verification
82. IMPLEMENTED

Có thể dùng:

IMPLEMENTED

khi implementation thuộc Phase 4B đã hoàn thành nhưng external/runtime integration chưa cần hoặc chưa thể verify.

Nếu tuyên bố final integration:

INTEGRATED

mới là mức mạnh hơn.

83. INTEGRATION-READY

Ví dụ:

Settlement metric

nếu:

Dashboard contract prepared
+
financial integration seam identified
+
no settlement provider/domain available

thì:

INTEGRATION-READY

Không fake number.

84. WRONG

Ví dụ hiện tại:

Delivered Order Total
        ↓
called Platform Revenue

nếu Policy/source không chứng minh semantics:

WRONG

hoặc:

PARTIAL

tùy mức độ mismatch.

85. DEPENDENCY FIX

Nếu cross-domain metric fail vì dependency:

Dashboard
 ↓
Service
 ↓
Order query
 ↓
wrong status filter

được phép:

fix Order query

vì đây là:

direct Phase 4B dependency

Không được nhân tiện:

rewrite Order system
86. SECURITY HARDENING

Phase 4B phải kiểm tra:

Admin metric
Moderator metric
Vendor metric
Customer metric

và:

IDOR
ownership
role
account status

Đặc biệt:

Vendor A
→ Vendor B metrics

phải bị deny.

87. ACCOUNT STATUS

Current source có:

ACTIVE
LOCKED
UNVERIFIED

Nếu account bị:

LOCKED

security boundary phải được verify theo architecture hiện tại.

Không tạo:

BANNED
SUSPENDED

chỉ để phục vụ metric.

88. ROLE BOUNDARY

Không cho:

Moderator → Admin Finance Metrics
Vendor → Marketplace-wide Finance
Customer → Internal Risk Metrics

trừ khi policy/security source hiện tại explicitly cho phép.

89. API / CONTROLLER SECURITY

Không chỉ:

hide dashboard card

Backend phải enforce:

ROLE
+
OWNERSHIP
+
RESOURCE SCOPE
90. NO FRONTEND TRUST

Không trust:

userId
vendorId
shopId

từ query/body khi backend có thể derive principal.

91. PERFORMANCE

Chỉ tối ưu khi evidence cho thấy query gây:

timeout
excessive scan

Ưu tiên:

correct query
+
appropriate index

Không rewrite Mongo architecture.

92. FINAL INTEGRATION FLOW

Phase 4B phải verify chain:

Domain Source
      ↓
Repository
      ↓
Cross-domain Query
      ↓
Metric Service
      ↓
Dashboard/API
      ↓
Role Security
      ↓
Actual MongoDB Data

Đối với financial:

Financial Domain
      ↓
Financial Source
      ↓
Metric

Nếu Financial Domain không tồn tại:

STOP

không tự derive.

93. FINAL CROSS-DOMAIN GRAPH

Architecture mục tiêu:

                    USER
                     │
          ┌──────────┴──────────┐
          │                     │
        SHOP                  CUSTOMER
          │                     │
       PRODUCT                 ORDER
          │                     │
          └──────────┬──────────┘
                     │
                   REVIEW
                     │
                 COMPLAINT
                     │
                VIOLATION
                     │
                ESCALATION
                     │
                   ADMIN
                     │
              ENFORCEMENT
                     │
                   AUDIT
                     │
              ┌──────┴──────┐
              │             │
          DASHBOARD       FINANCE

Trong đó:

DASHBOARD

là consumer.

Không phải:

source of truth
94. FINANCE GRAPH — CONDITIONAL

Chỉ khi domain tồn tại:

ORDER
  ↓
PAYMENT
  ↓
REFUND
  ↓
ESCROW
  ↓
SETTLEMENT
  ↓
VENDOR PAYOUT
  ↓
FINANCE METRICS

Nếu thiếu một domain critical:

Không tự tạo domain thay thế.
95. RISK GRAPH — CONDITIONAL

Nếu Risk domain thực sự tồn tại:

COMPLAINT
      ↓
VIOLATION
      ↓
RISK ENGINE
      ↓
RISK SCORE
      ↓
ADMIN

Nếu Risk Engine không tồn tại:

COMPLAINT
      ↓
VIOLATION COUNT

có thể báo cáo factual metric.

Nhưng:

RISK SCORE

không được tự suy ra.

96. FULL INTEGRATION ACCEPTANCE

Phase 4B đạt khi:

Operational
User
Shop
Product
Order
Review
Complaint
Violation
Escalation

được map đúng source.

Dashboard
Metric
 ↓
Backend
 ↓
Repository
 ↓
MongoDB

không fake.

Finance

Chỉ metric có financial source mới được gọi là financial metric.

Risk

Chỉ derived risk metric khi rule/formula tồn tại.

Security

Role + ownership được enforce.

Data

Không destructive operation.

Regression

Phase 1–3 không bị phá.

97. MANUAL VERIFICATION MATRIX
    Area	Expected
    User metrics	PASS / NOT TESTED
    Shop metrics	PASS / NOT TESTED
    Product metrics	PASS / NOT TESTED
    Order metrics	PASS / NOT TESTED
    Review metrics	PASS / NOT TESTED
    Complaint metrics	PASS / NOT TESTED
    Violation metrics	PASS / NOT TESTED
    Escalation metrics	PASS / NOT TESTED
    Dashboard source	PASS / FAIL / NOT TESTED
    Finance metrics	PASS / INTEGRATION-READY / NOT TESTED
    Risk metrics	PASS / INTEGRATION-READY / NOT TESTED
    Voucher metrics	PASS / NOT TESTED
    Banner metrics	PASS / NOT TESTED
    Admin security	PASS / NOT TESTED
    Moderator security	PASS / NOT TESTED
    Vendor ownership	PASS / NOT TESTED
    Customer ownership	PASS / NOT TESTED
    IDOR	PASS / NOT TESTED
    Historical data	PASS
    MongoDB safety	PASS
98. BUILD

Cuối Phase 4B:

mvn clean package -DskipTests

Báo chính xác:

Build:
PASS

hoặc:

Build:
FAIL

Không tự động:

mvn test
mvn verify
mvn install
mvn spring-boot:run

nếu chưa được yêu cầu.

99. MONGODB SAFETY ACCEPTANCE

Bắt buộc report:

Existing MongoDB data deleted:
NONE

Database reset:
NO

Collection dropped:
NO

deleteAll:
NO

deleteMany({}):
NO

Historical Order deleted:
NO

Financial data deleted:
NO

Existing Dashboard source data deleted:
NO

Test records inserted:
<number></number>

Test records deleted:
NO
100. GIT SAFETY

Không:

checkout
reset
stash
merge
rebase
commit
push

Không overwrite code của người khác.

Không dùng:

git add .

mù quáng.

Baseline:

git status
git branch
git log --oneline --decorate -10
101. FILE SCOPE

Ưu tiên:

Dashboard
Admin Dashboard
Metric Service
Dashboard Service
Relevant Repository
Relevant Query
Order
Shop
Product
Complaint
Violation
Escalation
Audit
Voucher
Banner
Security
Relevant templates
Relevant JS

Chỉ sửa domain nguồn khi:

metric integration trực tiếp bị sai

Không refactor toàn bộ domain.

102. KHÔNG TẠO DUPLICATE ARCHITECTURE

Không tạo:

FinanceV2
RiskV2
MetricV2
DashboardV2
AuditV2
EscalationV2
OrderMetricV2

nếu architecture hiện tại có thể mở rộng.

103. DEPENDENCY REPORT

Mỗi dependency fix:

Dependency:
<domain></domain>

Current Behavior:
<actual></actual>

Phase 4B Impact:
<metric/integration affected>

Root Cause:
<actual></actual>

Minimal Fix:
<change></change>

Regression Checked:
<flows></flows>

Status:
IMPLEMENTED / PARTIAL / WRONG / BLOCKED
104. METRIC REPORT

Mỗi metric:

Metric:
<name></name>

Domain:

<source>

Source:
<entity/repository>

Calculation:
<verified logic></verified>

Security Scope:
<scope></scope>

Financial:
YES / NO

Policy-backed:
YES / NO

Actual Data Verified:
YES / NO

Status:
<status></status>
105. FINANCE REPORT RIÊNG

Bắt buộc tách:

GMV:
<status></status>

Vendor Sales:
<status></status>

Platform Revenue:
<status></status>

Platform Fee:
<status></status>

Vendor Payable:
<status></status>

Vendor Payout:
<status></status>

Refund:
<status></status>

Settlement:
<status></status>

Escrow:
<status></status>

Không gộp:

Finance:
PASS

nếu chỉ có Order amount.

106. RISK REPORT RIÊNG
     Violation Count:
     <status></status>

Complaint Count:
<status></status>

Escalation Count:
<status></status>

Repeat Violation:
<status></status>

Risk Score:
<status></status>

Risk Level:
<status></status>

Đặc biệt:

Risk Score:
MISSING / INTEGRATION-READY

nếu không có scoring rule.

107. CURRENT-CODE FINDING QUAN TRỌNG

Phần này phải xuất hiện trong Final Report nếu source hiện tại vẫn như audit đã xác định:

Current Dashboard Revenue Finding:

The existing dashboard derives totalPlatformRevenue
from delivered-order amount.

This is not sufficient evidence that the value represents
true platform revenue.

Therefore:

Current behavior:
DELIVERED ORDER AMOUNT

Financial semantic:
NOT VERIFIED

Platform Revenue:
INTEGRATION-READY / WRONG

Không sửa thành financial formula giả.

108. FINAL STATUS LOGIC
     Trường hợp 1
     Operational metrics implemented

source verified
+
dashboard connected
+
security verified

→

INTEGRATED
Trường hợp 2
Finance source unavailable

nhưng operational integration hoàn chỉnh:

Operational:
INTEGRATED

Finance:
INTEGRATION-READY

Không hạ toàn bộ phase thành BLOCKED.

Trường hợp 3
Current metric uses wrong semantics

→

WRONG

sau đó minimal fix.

Trường hợp 4
Metric domain chưa tồn tại

→

MISSING

nếu Phase 4B acceptance thực sự yêu cầu domain đó.

Nếu Phase 4B chỉ cần chuẩn bị seam:

INTEGRATION-READY
109. FINAL REPORT FORMAT
PHASE
PHASE 4B
CROSS-DOMAIN METRICS + FINAL INTEGRATION
Overall Status
IMPLEMENTED
/
INTEGRATED
/
PARTIAL
/
INTEGRATION-READY
Operational Metrics
User:
...

Shop:
...

Product:
...

Order:
...

Review:
...

Complaint:
...

Violation:
...

Escalation:
...
Financial Metrics
GMV:
...

Vendor Sales:
...

Platform Revenue:
...

Platform Fee:
...

Vendor Payable:
...

Vendor Payout:
...

Refund:
...

Settlement:
...

Escrow:
...

Mỗi mục phải có:

Source
Evidence
Status
Risk Metrics
Violation Count:
...

Complaint Count:
...

Escalation Count:
...

Risk Score:
...

Risk Level:
...
Dashboard
Data source:
...

Queries:
...

Security:
...

Actual data verified:
YES / NO

Status:
...
Cross-domain Integration
User → Shop:
...

Shop → Product:
...

Product → Order:
...

Order → Review:
...

Order → Complaint:
...

Complaint → Violation:
...

Violation → Escalation:
...

Escalation → Admin:
...

Admin → Enforcement:
...

Enforcement → Audit:
...
110. SECURITY REPORT
Authentication:
PASS / FAIL / NOT TESTED

Authorization:
PASS / FAIL / NOT TESTED

Role Boundary:
PASS / FAIL / NOT TESTED

Ownership:
PASS / FAIL / NOT TESTED

IDOR:
PASS / FAIL / NOT TESTED

Admin Metrics:
PASS / FAIL / NOT TESTED

Moderator Metrics:
PASS / FAIL / NOT TESTED

Vendor Metrics:
PASS / FAIL / NOT TESTED

Customer Metrics:
PASS / FAIL / NOT TESTED
111. DATA SAFETY REPORT
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

Historical Order deleted:
NO

Financial records deleted:
NO

Test records inserted:
<number></number>

Test records deleted:
NO
112. BUILD REPORT
Command:
mvn clean package -DskipTests

Result:
PASS / FAIL
113. REGRESSION REPORT
Phase 1:
PASS / FAIL / NOT TESTED

Phase 2:
PASS / FAIL / NOT TESTED

Phase 3:
PASS / FAIL / NOT TESTED

Existing Core:
PASS / FAIL / NOT TESTED

Admin:
PASS / FAIL / NOT TESTED
114. KNOWN ISSUES

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
<root cause></root>

Impact:
<impact></impact>

Status:
MISSING / WRONG / PARTIAL / INTEGRATION-READY / BLOCKED
115. FINAL ACCEPTANCE CHECKLIST

Phase 4B chỉ được xem là hoàn tất khi có thể trả lời:

[ ] Mỗi metric có source of truth
[ ] Không có metric hardcoded
[ ] Không có fake financial metric
[ ] Không có fake risk score
[ ] Dashboard đọc backend thật
[ ] Backend đọc database thật
[ ] Operational metrics đúng semantics
[ ] Financial concepts được tách riêng
[ ] Finance chỉ làm khi source tồn tại
[ ] Risk chỉ làm khi rule tồn tại
[ ] Escalation đọc từ persistence thật
[ ] Audit không bị dùng sai làm business source
[ ] Security boundary được enforce
[ ] IDOR được kiểm tra
[ ] Historical data được giữ nguyên
[ ] Không delete MongoDB data
[ ] Không reset MongoDB
[ ] Không tạo duplicate domain
[ ] Không tạo FinanceV2/RiskV2/MetricV2
[ ] Phase 1 không regression
[ ] Phase 2 không regression
[ ] Phase 3 không regression
[ ] Build được chạy và báo kết quả thật
116. NGUYÊN TẮC QUAN TRỌNG NHẤT CỦA PHASE 4B
METRIC
    ↓
SOURCE OF TRUTH
    ↓
BUSINESS SEMANTICS
    ↓
QUERY
    ↓
SECURITY
    ↓
ACTUAL DATA
    ↓
DASHBOARD

Không được đảo ngược thành:

Dashboard cần số
    ↓
Tạo field
    ↓
Tự tính số
    ↓
Gọi đó là Finance

Đặc biệt:

Order Total
≠
Platform Revenue

Vendor Sales
≠
Platform Revenue

Refund
≠
Revenue deduction

Violation Count
≠
Risk Score

Audit Event
≠
Current Business State

trừ khi Policy + current architecture chứng minh rõ các equivalence đó.

117. FINAL PHASE 4 CHAIN

Sau khi hoàn thành 4A và 4B, architecture phải được hiểu theo:

PHASE 1
Moderator Foundation

+ Security
+ Source of Truth
  ↓
  PHASE 2
  Product Moderation
+ Auto Moderation
+ ReportCase
+ Violation
  ↓
  PHASE 3
  Complaint
+ Return/Refund
+ Escalation
+ Scheduler
+ Audit
  ↓
  PHASE 4A
  Admin
+ Dashboard Foundation
+ Voucher
+ Banner
+ Security Hardening
  ↓
  PHASE 4B
  Cross-domain Metrics
+ Finance Boundary
+ Risk Boundary
+ Final Integration
+ Regression
  ↓
  FINAL VERIFICATION
  Kết luận thực thi

Phase 4B không phải là phase “làm cho Dashboard có đủ số”.

Nó là phase chứng minh:

CURRENT SOURCE
      ↓
REAL DOMAIN DATA
      ↓
CORRECT BUSINESS SEMANTICS
      ↓
CROSS-DOMAIN QUERY
      ↓
SECURITY
      ↓
DASHBOARD / ADMIN

Với code hiện tại, điểm cần đặc biệt giữ nguyên là không lấy delivered order amount rồi tiếp tục gọi nó là Platform Revenue nếu chưa có financial source chứng minh điều đó. Finance/Settlement/Payout/Escrow/Risk Score phải đi theo conditional gate: domain nguồn tồn tại → semantics tồn tại → policy hỗ trợ → mới triển khai metric. Nếu thiếu, báo INTEGRATION-READY/MISSING, không tạo số giả.

Và xuyên suốt Phase 4B:

MongoDB existing data = PRESERVE
MongoDB reset          = NO
Destructive migration  = NO
Fake finance           = NO
Fake risk              = NO
Duplicate domain       = NO
Fake dashboard metric  = NO

Đây là ranh giới quan trọng nhất để Phase 4B thực sự là Cross-domain Metrics + Final Integration, thay vì biến thành một phase dựng số liệu giả để hoàn thành UI.
