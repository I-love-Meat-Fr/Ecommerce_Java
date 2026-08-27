GATE 0 — ADMIN CONTRACT & DECISION LOG
🎯 Mục tiêu Gate 0

Gate 0 dùng để chốt phạm vi, nghiệp vụ, ownership và các dependency cần thiết cho Admin trước khi bắt đầu triển khai Phase 1–8.

Gate 0 không viết code.

Gate 0 phải phân biệt rõ:

DIRECT TASK — HOÀN
        ↓
DEPENDENCY — QUỐC ANH / MẠNH QUÂN
        ↓
OPTIONAL / SKIPPED
        ↓
BLOCKED

Không được coi Dependency là task của Hoàn.

1. ADMIN RUNTIME ACCOUNT
Decision
DECIDED
Quyết định

Admin runtime dùng một tài khoản ADMIN hợp lệ được tạo thủ công trong MongoDB để phục vụ development/runtime testing.

Không dùng Public Registration để tạo ADMIN.

Không bắt buộc phải sửa DataInitializer trong Phase 1.

Nếu sau này Team muốn seed Admin bằng DataInitializer:

→ tạo thành decision riêng
→ không block Admin hiện tại
Owner
Hoàn — sử dụng/test
Quốc Anh — Auth/Password encoding nếu cần sửa
Acceptance
MongoDB
   ↓
ADMIN
   ↓
Login thành công
   ↓
/admin/dashboard
2. PUBLIC REGISTRATION — ADMIN ROLE
Decision
DECIDED
Quyết định

Public Registration không được phép tự tạo ADMIN.

Nếu client gửi:

role=ADMIN

backend phải REJECT request.

Không fallback âm thầm thành CUSTOMER.

Owner
Quốc Anh
Hoàn

Chỉ:

Xác minh requirement.
Test behavior.
Ghi nhận kết quả.

Không tự sửa Core Registration nếu không được giao.

Acceptance
POST Registration
role=ADMIN
       ↓
REJECT
       ↓
Không tạo ADMIN
3. USER ACCOUNT STATUS
Decision
DECIDED
Quyết định

Hệ thống sử dụng trạng thái:

ACTIVE
LOCKED
UNVERIFIED

Behavior tối thiểu:

ACTIVE
→ được Login

LOCKED
→ không được Login

UNVERIFIED
→ behavior phải tuân theo Auth contract hiện tại

Không tự thay đổi semantics của UNVERIFIED nếu source/team chưa có requirement cụ thể.

Owner
Quốc Anh — Core Auth
Hoàn — Admin User Management / verification
Acceptance

Phase 1 phải kiểm tra riêng:

Web Login
API Login
Old JWT
4. LOCKED USER + OLD JWT
Decision
DECIDED
Quyết định

Nếu:

ACTIVE
 ↓
JWT được cấp
 ↓
User bị LOCKED
 ↓
JWT cũ

thì JWT cũ không được tạo Authentication hợp lệ.

Owner
Quốc Anh
Hoàn

Không tự thiết kế:

Token blacklist.
Token revocation architecture.
JWT architecture mới.

Hoàn chỉ:

Requirement
+
Test
+
Evidence
Acceptance

Locked User + old JWT:

→ authenticated endpoint
→ FAIL
5. AUTH / JWT OWNERSHIP
Decision
DECIDED
Ownership
Thành phần	Owner
Authentication Core	Quốc Anh
Registration	Quốc Anh
JWT creation	Quốc Anh
JWT validation	Quốc Anh
JWT Filter	Quốc Anh
Account state authentication	Quốc Anh
Cookie/CSRF Core Security	Quốc Anh
Admin authorization requirement	Hoàn
Admin access verification	Hoàn
Admin security testing	Hoàn
Quy tắc

Hoàn không tự refactor Core Auth/JWT để giải quyết lỗi Admin nếu chưa được Quốc Anh xác nhận.

Nếu phát hiện lỗi:

[DEPENDENCY — QUỐC ANH]
6. USER MANAGEMENT SCOPE
Decision
DECIDED
Quyết định

Hoàn phụ trách User Management trong phạm vi Admin workbook.

Scope:

User
├── List
├── View/Detail nếu workbook giao
├── Lock
└── Unlock

Không tự mở rộng:

User Registration
Authentication
JWT
Customer/Vendor business logic
User List Scope B

Phải ghi chính thức:

DECIDED

Admin User List quản lý User theo phạm vi workbook, không mặc định chỉ CUSTOMER.

Nếu workbook yêu cầu tất cả User:

CUSTOMER
VENDOR
ADMIN

thì List phải xử lý đúng contract.

Nếu có loại User không được phép Admin quản lý:

→ phải ghi rõ trong Decision Log.

7. ROLE MANAGEMENT
Decision
DECIDED — SKIPPED
Quyết định

Không triển khai Role Management trong Admin nếu workbook chỉ ghi:

Role Management — nếu cần

và Team chưa yêu cầu.

Không làm:

Change CUSTOMER → VENDOR
Change VENDOR → ADMIN
Change ADMIN → CUSTOMER
Lý do

Role Management không phải chức năng bắt buộc nếu workbook không giao.

Nếu Team sau này yêu cầu:

→ tạo requirement/decision mới
→ không tự thêm vào Phase hiện tại
8. CATEGORY CONTRACT
Decision
DECIDED
Scope bắt buộc
Category
├── List
├── Create
├── Edit
└── Safe Delete
Active
SKIPPED

Không làm UI:

Active / Inactive

nếu workbook không giao.

Hierarchy / parentId
SKIPPED

nếu Team không xác nhận sử dụng Category hierarchy.

Không xây:

Category Tree
Parent
Child
Cycle Detection

nếu không thuộc contract.

Delete
DECIDED

Nếu Category đang được Product sử dụng:

REJECT DELETE

Không cascade delete Product.

Rename
BLOCKED — DEPENDENCY MẠNH QUÂN

Lý do:

Product lưu:

categoryId
categoryName

Nếu:

Phone
 ↓
Smartphone

cần xác nhận Product behavior.

Hoàn không tự sửa Product.

9. CATEGORY DELETE — PRODUCT REFERENCE
Decision
DECIDED

Quyết định:

Category
   ↓
Product reference exists
   ↓
REJECT DELETE

Không:

Cascade Product
Set null
Delete Product

trừ khi Team thay đổi contract.

10. SHOP CONTRACT
Decision
DECIDED / BLOCKED theo từng state

Bắt buộc phải chốt:

Pending
Approve
Reject
Active / Inactive

Không tự tạo enum mới.

Đặc biệt:

REJECTED
APPROVED
PENDING

chỉ được sử dụng nếu source/Team đã xác nhận.

Nếu Reject chưa có representation:

BLOCKED

Owner:

Mạnh Quân / Team

Hoàn chỉ triển khai sau khi contract được chốt.

11. ADMIN ORDER
Decision
DECIDED

Admin được:

Xem / theo dõi Order

Không tự thay đổi:

Order status
Order workflow
Vendor order processing

nếu workbook không giao.

12. DASHBOARD / REVENUE
Decision
DECIDED

Nếu workbook giao Dashboard/Revenue:

→ Hoàn triển khai

Nếu workbook ghi:

nếu có

thì phải xác nhận Team.

Không tự tạo business metric mới.

13. TEST STRATEGY
Decision
DECIDED

Phân biệt:

Unit Test
→ Mock

Integration Test
→ Test MongoDB

Runtime Test
→ Application đang chạy

Không dùng database development thật cho automated integration test nếu có nguy cơ ghi/xóa dữ liệu.

Không tự thay đổi MongoDB architecture.

14. COOKIE / CSRF / SECURITY HARDENING
Decision
SKIPPED / OUT OF SCOPE FOR ADMIN GATE

Các vấn đề như:

HttpOnly
Secure
SameSite
CSRF
JWT architecture
Token revocation

không block Admin implementation trừ khi chúng trực tiếp khiến Admin Access không thể hoạt động.

Owner:

Quốc Anh

Hoàn chỉ:

AUDIT
→ REPORT
→ HANDOFF
15. LOGIN FLOW CONTRACT
Decision
DECIDED
Web Login
/auth/login

ADMIN
 ↓
302 /admin/dashboard
API Login
/api/auth/login

→ trả authentication/JWT data
→ KHÔNG yêu cầu redirect /admin/dashboard

Client sử dụng API chịu trách nhiệm navigation.

Đây là hai flow khác nhau và phải test riêng.

16. LOGIN ERROR HANDLING
Decision
DECIDED

Login failure phải hiển thị lỗi rõ ràng.

Không được có tình trạng:

Exception
 ↓
redirect/render login
 ↓
không có error message

Nếu source có mismatch:

${error}

và:

${param.error}

thì phải xác định và sửa trong Phase 1 nếu cần để debug/runtime Login.

Owner phụ thuộc file thực tế:

Hoàn — Admin login behavior/UI
Quốc Anh — Auth core nếu phải sửa service
17. RUN SCRIPT / RUNTIME CONFIGURATION
Decision
DECIDED

run.ps1 phải được kiểm tra để bảo đảm runtime sử dụng đúng configuration/profile cần thiết.

Đặc biệt:

DataInitializer
profile=dev

phải thống nhất với cách application được chạy.

Không để:

Code yêu cầu dev profile
        ↓
run.ps1 không bật dev

Nếu Admin được tạo thủ công MongoDB thì đây không phải blocker, nhưng phải ghi rõ runtime strategy.

18. FINAL DECISION LOG
Decision	Status	Owner
Admin runtime account	DECIDED	Hoàn
Admin public registration	DECIDED — REJECT	Quốc Anh
ACTIVE/LOCKED/UNVERIFIED	DECIDED	Quốc Anh
Locked old JWT	DECIDED	Quốc Anh
Auth/JWT ownership	DECIDED	Quốc Anh
User Management scope	DECIDED	Hoàn
Role Management	SKIPPED	Team
Category List/Create/Edit/Delete	DECIDED	Hoàn
Category Active	SKIPPED	Team
Category Hierarchy	SKIPPED nếu không dùng	Team
Category Safe Delete	DECIDED — REJECT if Product reference	Hoàn
Category Rename	BLOCKED — Mạnh Quân xác nhận Product behavior	Mạnh Quân
Shop Approval contract	DECIDED khi state mapping được chốt	Team/Mạnh Quân
Shop Active/Inactive	DECIDED/SKIPPED theo workbook	Team
Admin Order	DECIDED	Hoàn
Dashboard/Revenue	DECIDED theo workbook	Hoàn
Test Strategy	DECIDED	Team
Cookie/CSRF hardening	OUT OF SCOPE / HANDOFF	Quốc Anh
Login Web redirect	DECIDED	Hoàn + Quốc Anh
API Login	DECIDED — JWT/API response, không redirect	Quốc Anh
Login Error Handling	DECIDED	Hoàn + Quốc Anh
run.ps1/runtime config	DECIDED — phải audit	Hoàn
🚦 GATE 0 EXIT CRITERIA

Gate 0 chỉ được PASS khi:

                    GATE 0
                       ↓
        ┌──────────────┴──────────────┐
        ↓                             ↓
  Admin Requirements             Ownership
        ↓                             ↓
     DECIDED                     DECIDED
        ↓                             ↓
  Phase 1 dependencies ───────────────┘
        ↓
   Không còn BLOCKED
   đối với Admin Access
        ↓
     GATE 0 PASS
        ↓
     PHASE 1