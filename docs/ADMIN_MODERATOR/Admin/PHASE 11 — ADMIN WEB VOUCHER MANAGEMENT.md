PHASE 11 — ADMIN WEB VOUCHER MANAGEMENT
🎯 Mục tiêu Phase 11

Hoàn thiện và xác minh Admin WEB Voucher Management theo đúng Workbook, business contract và implementation thực tế của source.

Phase 11 không tạo lại toàn bộ Voucher module.

Mục tiêu là:

Existing Voucher Implementation
        ↓
Audit
        ↓
Chốt WEB Voucher Contract
        ↓
Bổ sung phần thực sự thiếu
        ↓
WEB / SHOP Isolation
        ↓
Validation
        ↓
Automated Test
        ↓
Runtime + MongoDB
        ↓
Regression
        ↓
WEB Voucher hoàn chỉnh

Luồng Admin:

ADMIN
  ↓
/admin/vouchers
  ↓
ADMIN WEB VOUCHER
  ├── List
  ├── Create
  ├── Edit
  ├── Activate
  ├── Deactivate
  └── Delete theo Contract

Phase 11 chỉ tập trung vào WEB Voucher.

Voucher
│
├── WEB
│   └── ADMIN
│       └── PHASE 11
│
└── SHOP
    └── VENDOR
        └── OUT OF SCOPE

Theo source hiện tại:

Voucher.type = VoucherType.WEB

WEB Voucher được tạo thông qua:

createWebVoucher(...)

Contract cần xác minh:

WEB Voucher
    ↓
type = WEB
    ↓
shopId = null

Admin không được sử dụng Admin Voucher flow để thao tác SHOP Voucher.

1. VOUCHER BUSINESS CONTRACT
WEB Voucher
type = WEB
shopId = null
owner = ADMIN

Admin có thể quản lý WEB Voucher theo Workbook/Contract.

SHOP Voucher
type = SHOP
shopId = Vendor Shop
owner = VENDOR

Vendor quản lý SHOP Voucher.

Admin không được:

Edit SHOP Voucher
Delete SHOP Voucher
Activate SHOP Voucher
Deactivate SHOP Voucher

Không được biến:

WEB → SHOP
SHOP → WEB

thông qua Admin flow.

2. ⚠️ ĐIỀU KIỆN BẮT ĐẦU

Phase 11 chỉ implementation sau khi audit source thực tế.

Luồng bắt buộc:

Gate 0 / Voucher Contract
        ↓
Voucher Model
        ↓
VoucherType
        ↓
Voucher Repository
        ↓
Voucher Service
        ↓
Voucher Controller
        ↓
Admin Voucher Template
        ↓
Security
        ↓
MongoDB
        ↓
Existing Tests
        ↓
PHASE 11

Phải xác minh các field hiện tại:

Voucher.type
Voucher.shopId
Voucher.productIds
Voucher.active
Voucher.startDate
Voucher.endDate
Voucher.quantity
Voucher.used
Voucher.discountType
Voucher.discountValue
Voucher.minOrderValue
Voucher.maxDiscountAmount

Không được giả định field có ý nghĩa khác với source.

3. SOURCE-FIRST RULE

Trước khi code phải đọc source thực tế.

Tối thiểu:

Voucher.java
VoucherType.java
VoucherFormReq.java

VoucherRepository.java
VoucherService.java
VoucherServiceImpl.java
VoucherController.java

admin/voucher-list.html
admin/voucher-create.html

vendor/voucher-list.html
vendor/voucher-create.html
vendor/voucher-edit.html

SecurityConfig.java

Voucher-related tests

Sau đó xác định:

Controller
    ↓
Service
    ↓
Repository
    ↓
MongoDB
    ↓
Template

Phải phân loại:

[EXISTING]
[IMPLEMENTED]
[MISSING]
[BUG]
[DEPENDENCY]
[OUT OF SCOPE]

Không code dựa trên roadmap cũ nếu source thực tế khác.

4. ⚠️ PHẠM VI PHASE 11
CHỈ LÀM
Admin WEB Voucher
Audit implementation hiện tại.
Chốt WEB Voucher contract.
List WEB Voucher.
Create WEB Voucher.
Edit WEB Voucher nếu Workbook yêu cầu.
Activate WEB Voucher nếu Workbook yêu cầu.
Deactivate WEB Voucher nếu Workbook yêu cầu.
Delete theo Delete Contract.
Validation.
WEB / SHOP isolation.
Security.
Empty State.
UI consistency.
Automated Test.
Runtime verification.
MongoDB verification.
Regression smoke test.
Data Layer

Ưu tiên sử dụng:

Existing Repository
Existing Service
Existing Model

Chỉ thêm query/method khi thực sự cần.

Không refactor toàn bộ Voucher module.

5. ❌ KHÔNG ĐƯỢC LÀM

Không làm:

Vendor SHOP Voucher Management
Customer Voucher UI
Checkout
Payment
Order
Refund
Product Management
Shop Management
User Management
Category Management

Không được:

Tạo VoucherType mới
Tạo Voucher status mới
Đổi discount calculation
Đổi Checkout Voucher logic
Đổi Vendor Voucher workflow
Đổi Customer Voucher workflow

Không:

Refactor toàn bộ VoucherController
Redesign toàn bộ Admin UI
Redesign toàn bộ Voucher architecture

Không tự chuyển sang:

PHASE 12
6. SEARCH / FILTER / PAGINATION RULE

Đây là điểm phải khóa rõ.

Không mặc định Phase 11 phải triển khai:

Search
Filter
Pagination

Chỉ triển khai nếu:

Workbook
      +
Team Contract

yêu cầu.

Nếu source đã có:

Search / Filter / Pagination

→ audit.

Nếu chưa có và Workbook không yêu cầu:

[SKIPPED — OUT OF WORKBOOK]

Không tự mở rộng Phase.

TASK 11.1 — AUDIT VOUCHER IMPLEMENTATION
🎯 Mục tiêu

Xác định chính xác trạng thái Voucher hiện tại trước khi sửa.

Đọc source
Voucher.java
VoucherType.java
VoucherFormReq.java
VoucherRepository.java
VoucherService.java
VoucherServiceImpl.java
VoucherController.java
Admin Voucher templates
Vendor Voucher templates
SecurityConfig
Voucher tests
Kiểm tra
List
Create
Edit
Delete
Activate
Deactivate
Search
Filter
Pagination
Validation
WEB / SHOP separation
active
shopId
productIds

Theo audit hiện tại cần xác minh các phần:

✓ WEB Voucher List
✓ WEB Voucher Create
✓ Delete endpoint
✓ Empty State
✓ Security
✓ Repository query
✓ Service layer

Các phần cần kiểm tra thiếu:

? Admin Edit endpoint
? Admin Edit template
? Activate endpoint
? Deactivate endpoint
? Voucher automated test

Không coi những phần trên là chắc chắn thiếu nếu chưa đọc source thực tế.

Đặc biệt kiểm tra Delete

Nếu source:

deleteVoucher(id)
      ↓
voucher.setActive(false)
      ↓
save()

thì hành vi thực tế là:

Delete
   ↓
active = false

Đây là soft delete/deactivation, không phải hard delete.

Không được gọi là Hard Delete nếu document vẫn tồn tại trong MongoDB.

DONE

Có báo cáo:

[CURRENT IMPLEMENTATION]

[MISSING]

[BUG]

[DEPENDENCY]

[PROPOSED CHANGE]

[OUT OF SCOPE]

Không code trong task này.

TASK 11.2 — CHỐT WEB VOUCHER CONTRACT
🎯 Mục tiêu

Xác định chính xác Admin được phép quản lý Voucher nào.

Thành phần	WEB Voucher
type	WEB
shopId	null
Admin List	Có
Admin Create	Có
Admin Edit	Theo Workbook
Admin Activate	Theo Workbook
Admin Deactivate	Theo Workbook
Admin Delete	Theo Delete Contract

Contract:

ADMIN
 ↓
WEB Voucher
 ↓
type = WEB
 ↓
shopId = null

Admin không được thao tác:

type = SHOP

Không tự tạo:

ADMIN
GLOBAL
SYSTEM

hoặc VoucherType mới.

DONE

WEB Voucher ownership và Admin scope được xác định rõ.

Nếu chưa rõ:

[DEPENDENCY — TEAM]
TASK 11.3 — CHỐT DELETE / DEACTIVATE CONTRACT
🎯 Mục tiêu

Không để Delete và Deactivate có ý nghĩa mơ hồ.

Trường hợp A

Nếu Contract quy định:

Delete = Deactivate

thì:

Delete
 ↓
active = false

được giữ nguyên.

UI có thể dùng:

Deactivate

thay cho Delete nếu Team/Workbook thống nhất.

Trường hợp B

Nếu Contract quy định:

Delete = Hard Delete
Deactivate = active=false

thì phải tách:

Delete
 ↓
remove document

Deactivate
 ↓
active=false

Không tự chọn A/B.

Nếu chưa chốt:

[DEPENDENCY — TEAM]

Không code thay đổi hành vi Delete.

DONE

Delete/Deactivate có definition rõ ràng.

TASK 11.4 — ADMIN WEB VOUCHER LIST
🎯 Mục tiêu

Đảm bảo /admin/vouchers chỉ hiển thị WEB Voucher.

Route:

/admin/vouchers

Flow:

/admin/vouchers
        ↓
VoucherController
        ↓
VoucherService
        ↓
findByType(WEB)
        ↓
MongoDB

Phải đảm bảo:

WEB
→ HIỂN THỊ
SHOP
→ KHÔNG HIỂN THỊ

Không hard-code dữ liệu.

Search / Filter / Pagination

Chỉ audit hoặc triển khai nếu Workbook yêu cầu.

DONE

Admin Voucher List chỉ hiển thị WEB Voucher đúng contract.

TASK 11.5 — CREATE WEB VOUCHER
🎯 Mục tiêu

Verify và hoàn thiện Create WEB Voucher.

Flow:

Admin
 ↓
/admin/vouchers/create
 ↓
VoucherFormReq
 ↓
Validation
 ↓
createWebVoucher(...)
 ↓
MongoDB

Bắt buộc:

type = WEB
shopId = null

Không cho Admin tạo SHOP Voucher.

Fields cần audit
code
name
discountType
discountValue
maxDiscountAmount
minOrderValue
quantity
startDate
endDate
productIds

Không tự bỏ field hoặc thay đổi ý nghĩa field.

Validation

Kiểm tra:

Code không rỗng.
Code format.
Code duplicate.
Discount type.
Discount value.
Percent limit theo contract.
Quantity.
Date.
endDate >= startDate.
Các validation đã tồn tại trong VoucherFormReq.

Audit hiện tại đặc biệt cần xác minh:

createVoucher(...)
→ có endDate validation

createWebVoucher(...)
→ cần kiểm tra có thiếu hay không

Nếu source thực tế thiếu và Workbook/Contract yêu cầu:

→ bổ sung validation.

Không tự thêm business rule mới.

DONE

Create WEB Voucher lưu đúng:

type = WEB
shopId = null

và validation đúng contract.

TASK 11.6 — EDIT WEB VOUCHER
🎯 Mục tiêu

Bổ sung Edit nếu Workbook yêu cầu và source thực tế chưa có Admin flow phù hợp.

Flow:

Admin
 ↓
GET /admin/vouchers/edit/{id}
 ↓
Load Voucher
 ↓
Check WEB
 ↓
Validate
 ↓
Update
 ↓
MongoDB

POST:

/admin/vouchers/edit/{id}

Phải guard:

voucher.type != WEB
        ↓
DENY

Không cho:

SHOP → Edit bởi Admin

Không cho:

WEB → SHOP

Không cho thay đổi ownership:

shopId = null

thành Shop của Vendor.

Nếu type/ownership immutable:

→ giữ nguyên.

Template:

admin/voucher-edit.html

Có thể reuse form nếu phù hợp với source.

Không bắt buộc tạo Controller mới.

DONE

Admin chỉ Edit được WEB Voucher.

TASK 11.7 — DELETE WEB VOUCHER
🎯 Mục tiêu

Bảo vệ Delete theo WEB-only contract.

Endpoint hiện tại cần xác minh:

POST /admin/vouchers/delete/{id}

Flow:

Load Voucher
      ↓
Check type
      ↓
WEB?
 ├── YES → Delete theo Contract
 └── NO  → DENY

Nếu source hiện tại:

active = false

thì phải giữ đúng contract đã chốt ở TASK 11.3.

Không gọi:

active=false

là Hard Delete.

DONE

Admin Delete không thể tác động SHOP Voucher.

TASK 11.8 — ACTIVATE WEB VOUCHER
🎯 Mục tiêu

Bổ sung Activate nếu Workbook/Contract yêu cầu.

Endpoint nếu source chưa có:

POST /admin/vouchers/activate/{id}

Flow:

Admin
 ↓
Load Voucher
 ↓
Check WEB
 ↓
active = true
 ↓
save

Chỉ thay đổi:

active

Không tự thay đổi:

code
type
shopId
discount
quantity
productIds
dates
DONE

WEB Voucher được Activate đúng contract.

TASK 11.9 — DEACTIVATE WEB VOUCHER
🎯 Mục tiêu

Bổ sung Deactivate nếu Workbook/Contract yêu cầu.

Flow:

Admin
 ↓
Load Voucher
 ↓
Check WEB
 ↓
active = false
 ↓
save

Không xóa document nếu contract chỉ định Deactivate.

Nếu Delete cũng thực hiện:

active = false

thì hành vi phải được giải thích rõ theo TASK 11.3.

DONE

Deactivate WEB Voucher hoạt động đúng và không gây nhầm lẫn với Delete.

TASK 11.10 — VOUCHER VALIDATION
🎯 Mục tiêu

Audit validation của Create/Edit.

Kiểm tra:

code
name
discountType
discountValue
maxDiscountAmount
minOrderValue
quantity
startDate
endDate
productIds

Kiểm tra:

existsByCode()

cho Create/Edit nếu source/business rule yêu cầu.

Đặc biệt:

endDate >= startDate

Phải được đảm bảo ở WEB Voucher nếu đây là contract hiện tại.

Không tự thay đổi validation của Vendor Voucher nếu không cần thiết cho Admin WEB flow.

DONE

Admin không thể lưu WEB Voucher invalid theo business contract.

TASK 11.11 — WEB / SHOP VOUCHER ISOLATION
🎯 Mục tiêu

Đảm bảo mọi Admin Voucher operation đều kiểm tra ownership.

Các operation:

LIST
CREATE
EDIT
DELETE
ACTIVATE
DEACTIVATE
Test quan trọng
SHOP Voucher ID
        ↓
Admin Edit
        ↓
DENIED
SHOP Voucher ID
        ↓
Admin Delete
        ↓
DENIED
SHOP Voucher ID
        ↓
Admin Activate
        ↓
DENIED
SHOP Voucher ID
        ↓
Admin Deactivate
        ↓
DENIED

WEB:

WEB Voucher
        ↓
Admin
        ↓
ALLOW
DONE

Không có Admin operation nào tác động nhầm SHOP Voucher.

TASK 11.12 — VOUCHER DATA ACCURACY
🎯 Mục tiêu

Đối chiếu Admin Voucher với MongoDB.

Ví dụ:

MongoDB

WEB  = 5
SHOP = 8

Admin:

/admin/vouchers

phải chỉ hiển thị:

WEB = 5

Không được:

WEB + SHOP = 13

Kiểm tra các flow:

Create
 ↓
MongoDB
Edit
 ↓
MongoDB
Activate
 ↓
MongoDB
Deactivate
 ↓
MongoDB
Delete
 ↓
MongoDB

Đối chiếu:

MongoDB
   ↕
Repository
   ↕
Service
   ↕
Controller
   ↕
Template
DONE

Admin WEB Voucher khớp dữ liệu MongoDB thực tế.

TASK 11.13 — ADMIN VOUCHER SECURITY
🎯 Mục tiêu

Chỉ ADMIN được truy cập và thao tác Admin Voucher.

Kiểm tra:

ADMIN
 ↓
/admin/vouchers
 ↓
ALLOW
CUSTOMER
 ↓
/admin/vouchers
 ↓
DENIED
VENDOR
 ↓
/admin/vouchers
 ↓
DENIED
ANONYMOUS
 ↓
/admin/vouchers
 ↓
LOGIN / DENIED

Kiểm tra tất cả endpoint thực tế:

/admin/vouchers
/admin/vouchers/create
/admin/vouchers/edit/{id}
/admin/vouchers/delete/{id}
/admin/vouchers/activate/{id}
/admin/vouchers/deactivate/{id}

Endpoint nào không tồn tại thì không được giả định là có.

Security phải được kiểm tra ở backend, không chỉ Sidebar.

DONE

Không có Customer/Vendor/Anonymous bypass Admin Voucher.

TASK 11.14 — ADMIN WEB VOUCHER AUTOMATED TEST
🎯 Mục tiêu

Bổ sung automated test cho Admin WEB Voucher dựa trên infrastructure hiện tại.

Đây là task bắt buộc phải audit kỹ vì Phase 11 trước đó có thể đã hoàn thành chức năng nhưng chưa có test riêng đầy đủ.

Kiểm tra:

src/test

và pattern test hiện tại.

Ưu tiên reuse:

JUnit 5
Spring Boot Test
MockMvc
@MockBean

hoặc infrastructure thực tế đang có.

Không dựng framework mới nếu không cần.

Test tối thiểu
Security
[ ] Admin allowed
[ ] Customer denied
[ ] Vendor denied
[ ] Anonymous denied
List
[ ] List WEB Voucher
[ ] SHOP Voucher không xuất hiện
Create
[ ] Create WEB Voucher
[ ] type = WEB
[ ] shopId = null
[ ] Validation
Edit
[ ] Edit WEB Voucher
[ ] Edit SHOP Voucher denied
Delete
[ ] Delete WEB Voucher
[ ] Delete SHOP Voucher denied
Activate / Deactivate
[ ] Activate WEB Voucher
[ ] Deactivate WEB Voucher
[ ] SHOP Voucher denied
Empty State
[ ] Empty WEB Voucher

Search / Filter / Pagination chỉ test nếu Workbook yêu cầu hoặc implementation thuộc Phase.

Nếu không:

[SKIPPED — OUT OF WORKBOOK]

Nếu infrastructure không đủ:

[DEPENDENCY — TEST INFRASTRUCTURE]
DONE

Admin WEB Voucher có automated test cho các flow quan trọng và kết quả được ghi nhận rõ.

TASK 11.15 — ADMIN VOUCHER UI CONSISTENCY
🎯 Mục tiêu

Đảm bảo Voucher UI sử dụng đúng Admin Shell.

Kiểm tra:

Header.
Sidebar.
Navigation.
Breadcrumb.
Page title.
Voucher table.
Create form.
Edit form.
Action buttons.
Active/Inactive state.
Flash message.
Error message.
Empty State.
Responsive cơ bản.

Sidebar:

Voucher WEB

phải link đúng:

/admin/vouchers

Action phải phản ánh đúng Contract:

Edit
Activate
Deactivate
Delete

Không redesign toàn bộ Admin UI.

DONE

Voucher UI nhất quán với Admin Core.

TASK 11.16 — VOUCHER EMPTY STATE
🎯 Mục tiêu

Xác minh Empty State khi không có WEB Voucher.

Ví dụ:

MongoDB
WEB Voucher = 0

Admin:

/admin/vouchers
        ↓
Empty State

Không:

Hiển thị SHOP Voucher.
Hiển thị fake data.
Crash template.
Hiển thị state gây hiểu nhầm.

Nếu source đã có Empty State đúng:

[CURRENT — PASS]

Không cần viết lại.

DONE

WEB Voucher = 0 được hiển thị đúng Empty State.

TASK 11.17 — ADMIN ↔ CUSTOMER WEB VOUCHER FLOW
🎯 Mục tiêu

Kiểm tra Admin WEB Voucher sau khi lưu có thể được Customer nhìn thấy/sử dụng đúng theo business rule hiện tại.

Flow:

ADMIN
 ↓
Create / Edit WEB Voucher
 ↓
MongoDB
 ↓
Customer Voucher Flow

Kiểm tra:

type = WEB
shopId = null
active
startDate
endDate
quantity
used
discount rules

Không sửa Customer Voucher hoặc Checkout logic nếu không có lỗi trực tiếp do Phase 11 gây ra.

Nếu Customer flow có lỗi tồn tại từ trước:

[DEPENDENCY]

Không tự mở rộng Phase 11.

DONE

Admin WEB Voucher synchronization với Customer được xác minh.

TASK 11.18 — RUNTIME VERIFICATION
🎯 Mục tiêu

Kiểm tra application thực tế.

Chạy:

mvn test

sau đó:

mvn spring-boot:run

Flow:

Admin Login
    ↓
/admin/vouchers
    ↓
List
    ↓
Create
    ↓
Edit
    ↓
Activate / Deactivate
    ↓
Delete theo Contract
    ↓
MongoDB

Kiểm tra:

HTTP status.
Redirect.
UI.
Form.
Validation.
Flash message.
WEB/SHOP isolation.
Active state.
MongoDB.
Security.

Đặc biệt:

WEB Voucher
    ↓
Admin
    ↓
ALLOW

và:

SHOP Voucher
    ↓
Admin operation
    ↓
DENIED

Không đánh dấu Runtime PASS chỉ vì:

BUILD SUCCESS

Nếu environment không ổn định:

[DEPENDENCY — RUNTIME]
DONE

Admin WEB Voucher đã được kiểm tra trên application thực tế.

TASK 11.19 — MONGODB VERIFICATION
🎯 Mục tiêu

Xác minh dữ liệu Voucher trực tiếp trên MongoDB.

Kiểm tra:

WEB Voucher
SHOP Voucher

Đối chiếu:

Voucher.type
Voucher.shopId
Voucher.active
Voucher.quantity
Voucher.used
Voucher.startDate
Voucher.endDate
WEB
type = WEB
shopId = null
SHOP
type = SHOP
shopId = Vendor Shop

Admin không được làm thay đổi ownership của SHOP Voucher.

Kiểm tra sau:

Create
Edit
Activate
Deactivate
Delete
DONE

MongoDB phản ánh đúng kết quả của Admin WEB Voucher operations.

TASK 11.20 — REGRESSION SMOKE TEST
🎯 Mục tiêu

Đảm bảo Phase 11 không phá các flow đang tồn tại.

Không cần chạy lại toàn bộ test Phase 1–10.

Chỉ smoke test các module có khả năng bị ảnh hưởng.

ADMIN
[ ] Admin Login
[ ] Admin Dashboard
[ ] Admin Navigation
[ ] Admin Voucher
CUSTOMER
[ ] Customer Login
[ ] Home
[ ] Product
[ ] Voucher
[ ] Cart
[ ] Checkout
[ ] Order
VENDOR
[ ] Vendor Login
[ ] Product CRUD
[ ] Shop
[ ] SHOP Voucher
[ ] Order

Đặc biệt:

ADMIN WEB Voucher
        ↓
VoucherService
        ↓
MongoDB

không được phá:

VENDOR SHOP Voucher

và:

CUSTOMER Voucher / Checkout

Nếu lỗi tồn tại từ trước và không liên quan Phase 11:

[PRE-EXISTING ISSUE]

Nếu do Phase 11:

[REGRESSION — MUST FIX]
DONE

Không có regression nghiêm trọng do Phase 11 gây ra.

TASK 11.21 — PHASE 11 FINAL REVIEW

Trước khi kết thúc Phase 11 phải báo cáo:

[VOUCHER CONTRACT]
WEB Voucher Ownership → PASS/FAIL
SHOP Voucher Ownership → PASS/FAIL
type = WEB → PASS/FAIL
shopId = null → PASS/FAIL
[LIST]
List WEB → PASS/FAIL
SHOP Isolation → PASS/FAIL
Search → PASS/FAIL/SKIPPED
Filter → PASS/FAIL/SKIPPED
Pagination → PASS/FAIL/SKIPPED
[CREATE]
Create → PASS/FAIL
Validation → PASS/FAIL
type → PASS/FAIL
shopId → PASS/FAIL
[EDIT]
Edit WEB → PASS/FAIL
Edit SHOP denied → PASS/FAIL
Ownership protection → PASS/FAIL
[ACTIVATE / DEACTIVATE]
Activate → PASS/FAIL/SKIPPED
Deactivate → PASS/FAIL/SKIPPED
active state → PASS/FAIL
[DELETE]
Delete → PASS/FAIL
Delete Contract → PASS/FAIL
SHOP isolation → PASS/FAIL
[SECURITY]
Admin → PASS/FAIL
Customer → PASS/FAIL
Vendor → PASS/FAIL
Anonymous → PASS/FAIL
[DATA]
MongoDB → PASS/FAIL
Admin UI → PASS/FAIL
Customer Sync → PASS/FAIL
[TEST]
Automated Test → PASS/FAIL/SKIPPED
Maven Test → PASS/FAIL
[RUNTIME]
Runtime → PASS/FAIL
[REGRESSION]
Admin → PASS/FAIL
Customer → PASS/FAIL
Vendor → PASS/FAIL
[IMPLEMENTATION SUMMARY]

Báo cáo chính xác:

[CURRENT IMPLEMENTATION]

[MISSING FEATURES]

[BUGS FOUND]

[BACKEND CHANGES]

[CONTROLLER CHANGES]

[SERVICE CHANGES]

[REPOSITORY CHANGES]

[FRONTEND CHANGES]

[SECURITY RESULT]

[VALIDATION RESULT]

[WEB/SHOP ISOLATION]

[TEST RESULT]

[RUNTIME RESULT]

[MONGODB RESULT]

[REGRESSION RESULT]
[DEPENDENCIES]

Phải phân loại rõ:

[DEPENDENCY — TEAM]
[DEPENDENCY — BUSINESS CONTRACT]
[DEPENDENCY — TEST INFRASTRUCTURE]
[DEPENDENCY — RUNTIME]

Không được biến Dependency thành implementation tự phát.

[SKIPPED]

Ghi rõ:

[SKIPPED — OUT OF WORKBOOK]

hoặc:

[SKIPPED — NOT REQUIRED]

Ví dụ:

Search
Filter
Pagination

nếu Workbook không yêu cầu.

[NOT DONE]

Liệt kê chức năng chưa hoàn thành.

[OUT OF SCOPE]
SHOP Voucher Management
Product Management
Review Management
Order Management
Payment
Settlement
Refund
Return
Checkout Redesign
Customer Voucher Redesign
Vendor Voucher Redesign
Phase 12
[REMAINING ISSUES]

Liệt kê tất cả issue còn lại.

❌ KHÔNG ĐƯỢC ĐÁNH DẤU PHASE 11 PASS NẾU

Một trong các điều kiện sau vẫn tồn tại:

Admin List hiển thị SHOP Voucher

hoặc:

Create WEB Voucher tạo type sai

hoặc:

WEB Voucher có shopId trái contract

hoặc:

Admin Edit được SHOP Voucher

hoặc:

Admin Delete được SHOP Voucher

hoặc:

Admin Activate/Deactivate được SHOP Voucher

hoặc:

Delete / Deactivate chưa được định nghĩa rõ

hoặc:

Validation quan trọng bị sai

hoặc:

Security có bypass

hoặc:

MongoDB ≠ Admin UI

hoặc:

Admin WEB Voucher
≠
Customer Voucher

trong trường hợp đáng lẽ phải đồng bộ theo contract.

Hoặc:

Automated Test quan trọng chưa được kiểm tra

mà không có lý do hợp lệ.

Hoặc:

Runtime chưa được verify

hoặc:

Regression do Phase 11 gây ra chưa được xử lý

hoặc:

AI tự thay đổi Voucher business contract

hoặc:

AI tự thay đổi Vendor/Customer Voucher workflow
🛑 STOP RULE

Sau khi hoàn thành Phase 11:

DỪNG.

Không tự triển khai:

Product Moderation
Review Moderation
Payment Settlement
Refund / Return
Banner / PR

Không tự chuyển sang Phase 12.

Chỉ khi Hoàn xác nhận:

PHASE 11 DONE

mới chuyển sang:

PHASE 12 — ADMIN FINAL CORE AUDIT & INTEGRATION
📋 TỔNG KẾT PHASE 11
PHASE 11 — ADMIN WEB VOUCHER MANAGEMENT
│
├── TASK 11.1
│   └── Audit Voucher Implementation
│
├── TASK 11.2
│   └── Chốt WEB Voucher Contract
│
├── TASK 11.3
│   └── Chốt Delete / Deactivate Contract
│
├── TASK 11.4
│   └── Admin WEB Voucher List
│
├── TASK 11.5
│   └── Create WEB Voucher
│
├── TASK 11.6
│   └── Edit WEB Voucher
│
├── TASK 11.7
│   └── Delete WEB Voucher
│
├── TASK 11.8
│   └── Activate WEB Voucher
│
├── TASK 11.9
│   └── Deactivate WEB Voucher
│
├── TASK 11.10
│   └── Voucher Validation
│
├── TASK 11.11
│   └── WEB / SHOP Voucher Isolation
│
├── TASK 11.12
│   └── Voucher Data Accuracy
│
├── TASK 11.13
│   └── Admin Voucher Security
│
├── TASK 11.14
│   └── Automated Test
│
├── TASK 11.15
│   └── Admin Voucher UI Consistency
│
├── TASK 11.16
│   └── Voucher Empty State
│
├── TASK 11.17
│   └── Admin ↔ Customer WEB Voucher Flow
│
├── TASK 11.18
│   └── Runtime Verification
│
├── TASK 11.19
│   └── MongoDB Verification
│
├── TASK 11.20
│   └── Regression Smoke Test
│
└── TASK 11.21
    └── Phase 11 Final Review
THỨ TỰ IMPLEMENTATION CHÍNH
Audit Source
      ↓
Chốt WEB Voucher Contract
      ↓
Chốt Delete / Deactivate Contract
      ↓
Audit Existing List
      ↓
Create
      ↓
Edit
      ↓
Delete
      ↓
Activate
      ↓
Deactivate
      ↓
Validation
      ↓
WEB / SHOP Isolation
      ↓
Security
      ↓
Data Accuracy
      ↓
Automated Test
      ↓
UI Consistency
      ↓
Empty State
      ↓
Admin ↔ Customer Flow
      ↓
Runtime
      ↓
MongoDB
      ↓
Regression
      ↓
Final Review
      ↓
PHASE 11 DONE
NGUYÊN TẮC IMPLEMENTATION
READ SOURCE FIRST
        ↓
IDENTIFY EXISTING IMPLEMENTATION
        ↓
DO NOT REBUILD EXISTING FEATURES
        ↓
ONLY IMPLEMENT WHAT IS ACTUALLY MISSING
        ↓
FOLLOW WORKBOOK + TEAM CONTRACT
        ↓
WEB ONLY
        ↓
NEVER TOUCH SHOP OWNERSHIP
        ↓
NO UNAUTHORIZED BUSINESS RULE CHANGES
        ↓
TEST
        ↓
RUNTIME
        ↓
MONGODB
        ↓
REGRESSION
        ↓
FINAL REVIEW
        ↓
STOP
Điểm quan trọng nhất của bản Phase 11 này
Phase 11
   ≠
Xây lại Voucher từ đầu

mà là:

Existing Voucher
      ↓
Audit
      ↓
Giữ phần đúng
      ↓
Sửa phần sai
      ↓
Bổ sung phần Workbook thực sự yêu cầu
      ↓
WEB / SHOP isolation
        ↓
Test
        ↓
Runtime

---

