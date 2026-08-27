PHASE 1 — ADMIN ACCESS & SECURITY
🎯 Mục tiêu Phase 1

Phase 1 chỉ tập trung vào việc làm cho Admin Access hoạt động đúng và xác minh các Security requirement trực tiếp liên quan đến Admin.

Luồng bắt buộc:

ADMIN ACCOUNT
      ↓
Web Login
      ↓
Authentication SUCCESS
      ↓
JWT Cookie
      ↓
ROLE_ADMIN
      ↓
/admin/dashboard
      ↓
ALLOW

Các role khác:

CUSTOMER ──→ /admin/** ──→ 403
VENDOR   ──→ /admin/** ──→ 403
ANONYMOUS ─→ /admin/** ──→ DENIED

Đối với User bị khóa:

LOCKED USER
    ├── Web Login
    │      ↓
    │    FAIL
    │
    ├── API Login
    │      ↓
    │    FAIL
    │
    └── Old JWT
           ↓
       KHÔNG AUTHENTICATE
⚠️ Phân biệt Web Login và API Login

Web:

/auth/login
    ↓
Authentication
    ↓
JWT Cookie
    ↓
redirect:/admin/dashboard

API:

/api/auth/login
    ↓
Authentication
    ↓
JWT / response
    ↓
Client tự xử lý

Không yêu cầu /api/auth/login redirect đến /admin/dashboard.

🚫 PHẠM VI KHÔNG LÀM

Phase 1 không làm:

User List.
User Detail.
Lock/Unlock UI.
Category.
Shop.
Order.
Dashboard statistics.
Product Management.
Vendor Management.
Customer Management.
Refactor toàn bộ Security.
Thiết kế lại JWT architecture.
Token blacklist/revocation architecture.
Security hardening toàn hệ thống.
Cookie/CSRF hardening nếu chưa được Quốc Anh giao.
Thay đổi Authentication architecture chỉ vì muốn code đẹp hơn.

Nếu phát hiện vấn đề ngoài phạm vi:

[OUT OF SCOPE]

hoặc:

[DEPENDENCY — QUỐC ANH]

Không tự sửa.

TASK 1.1 — AUDIT WEB LOGIN / API LOGIN / JWT / ADMIN ACCESS
🎯 Mục tiêu

Xác định chính xác flow hiện tại trước khi sửa.

AI phải kiểm tra
AuthController
AuthServiceImpl
JwtAuthenticationFilter
SecurityConfig
Login DTO
Registration DTO
User Document/Model
User Repository
User Role
User State
JWT creation
JWT validation
JWT Cookie
Admin Dashboard Controller
Admin Dashboard template
application.yml
run.ps1
Test configuration nếu có
AI phải trả lời
Web Login
/auth/login
      ↓
?
      ↓
JWT
      ↓
?
      ↓
redirect
API Login
/api/auth/login
      ↓
?
      ↓
JWT
      ↓
Response
Admin Access
Request
   ↓
JWT Filter
   ↓
Authentication
   ↓
SecurityConfig
   ↓
/admin/**
Đặc biệt kiểm tra
Admin đã tồn tại chưa?
Admin có ROLE_ADMIN đúng không?
/admin/dashboard đã tồn tại chưa?
SecurityConfig đã bảo vệ /admin/** chưa?
Web login có redirect cố định /home không?
Login có che mất lỗi không?
run.ps1 có bật profile cần thiết không?
Test database đã có chưa?
Quy tắc

KHÔNG CODE.

Chỉ báo cáo:

[CURRENT]

[PROBLEM]

[ROOT CAUSE]

[DEPENDENCY]

[OWNER]

[PROPOSED CHANGE]

[OUT OF SCOPE]
DONE

Hoàn hiểu chính xác tại sao Admin hiện tại chưa vào được Dashboard.

TASK 1.2 — ADMIN DEVELOPMENT ACCOUNT & RUNTIME CONFIGURATION
🎯 Mục tiêu

Có một tài khoản Admin hợp lệ để test runtime.

Phải phân biệt:

Development Admin
        ≠
Automated Test Fixture
AI kiểm tra
DataInitializer
profile dev
run.ps1
User Repository
User Document
UserRole
Password Encoder
MongoDB configuration
Với source hiện tại

Nếu DataInitializer chỉ tạo Vendor và run.ps1 không bật dev:

AI phải ghi rõ:

[CURRENT]
DataInitializer không tạo Admin.

[CURRENT]
run.ps1 không bật dev profile.

[IMPACT]
Không có Admin runtime tự động từ source hiện tại.
Không được tự quyết định

AI không tự chọn:

Seed Admin.
MongoDB thủ công.
Profile dev.
Cơ chế khác.

Nếu Gate 0 đã chốt cách tạo Admin thì tuân thủ đúng Gate 0.

DONE

Có Admin hợp lệ:

role = ADMIN
state = ACTIVE
password = BCrypt

và có thể dùng để test runtime.

TASK 1.3 — ADMIN WEB LOGIN & REDIRECT
🎯 Mục tiêu

Fix đúng lỗi:

ADMIN
 ↓
/auth/login
 ↓
LOGIN SUCCESS
 ↓
/admin/dashboard

Theo source hiện tại, nếu AuthController.login() đang:

Authentication
 ↓
JWT Cookie
 ↓
redirect:/home

thì AI phải sửa đúng phần redirect cần thiết.

Yêu cầu

Nếu user là:

ROLE_ADMIN

→:

redirect:/admin/dashboard

Customer/Vendor:

→ giữ flow hiện tại.

Login Error

AI phải kiểm tra và xử lý lỗi bị che.

Ví dụ nếu Controller truyền:

model.addAttribute("error", ...)

nhưng template kiểm tra:

${param.error}

thì phải sửa mismatch trong phạm vi Task này để lỗi login hiển thị được.

Không tạo hệ thống error mới.

Không được
Refactor AuthService toàn bộ.
Thiết kế lại Authentication.
Thay JWT architecture.
Thay Customer/Vendor flow nếu không cần.

Nếu cần sửa Core Auth:

[DEPENDENCY — QUỐC ANH]
DONE
ADMIN
 ↓
Web Login
 ↓
SUCCESS
 ↓
/admin/dashboard
 ↓
HTTP 200

và khi login thất bại:

Login
 ↓
FAIL
 ↓
User nhìn thấy lỗi phù hợp
TASK 1.4 — ADMIN AUTHORIZATION
🎯 Mục tiêu

Xác nhận /admin/** được bảo vệ đúng.

Test
ADMIN
 ↓
/admin/dashboard
 ↓
ALLOW
CUSTOMER
 ↓
/admin/dashboard
 ↓
403
VENDOR
 ↓
/admin/dashboard
 ↓
403
ANONYMOUS
 ↓
/admin/dashboard
 ↓
DENIED / LOGIN
AI phải kiểm tra

SecurityConfig

đặc biệt:

/admin/**
hasRole("ADMIN")

Nếu source đã đúng:

Không sửa chỉ để thay đổi code.

DONE

Authorization đúng cho cả:

Admin.
Customer.
Vendor.
Anonymous.
TASK 1.5 — REGISTRATION ADMIN GUARD
🎯 Mục tiêu

Đảm bảo public registration không thể tự tạo Admin.

Public Registration
       ↓
role=ADMIN
       ↓
KHÔNG tạo ADMIN trái phép

Hành vi cụ thể:

REJECT

hoặc:

FALLBACK CUSTOMER

phải theo Gate 0.

Ownership

Phần Registration thuộc Core Auth.

Nếu cần sửa:

[DEPENDENCY — QUỐC ANH]

Hoàn không tự refactor Registration.

DONE

Có bằng chứng:

role=ADMIN
 ↓
Public registration
 ↓
Không tạo Admin trái phép
TASK 1.6 — ACCOUNT STATE LOGIN GUARD
🎯 Mục tiêu

Đảm bảo User state được xử lý đúng khi login.

Phải test riêng:

ACTIVE
LOCKED
UNVERIFIED
Web Login
ACTIVE
 → SUCCESS

LOCKED
 → FAIL

UNVERIFIED
 → theo Gate 0
API Login
ACTIVE
 → SUCCESS

LOCKED
 → FAIL

UNVERIFIED
 → theo Gate 0
Quan trọng

Hai flow:

/auth/login

và:

/api/auth/login

phải được test riêng.

Ownership

Đây là Core Auth.

Nếu cần sửa:

[DEPENDENCY — QUỐC ANH]

Hoàn không tự sửa AuthServiceImpl hoặc Authentication architecture nếu chưa được xác nhận ownership.

DONE

Một trong hai:

FIXED + TESTED

hoặc nếu chưa được sửa:

DEPENDENCY HANDED OFF TO QUỐC ANH

Nhưng Phase 1 chưa được coi là hoàn thành toàn bộ Security requirement nếu requirement vẫn chưa được xử lý.

TASK 1.7 — LOCKED USER + OLD JWT
🎯 Mục tiêu

Đảm bảo:

ACTIVE USER
     ↓
JWT được cấp
     ↓
USER bị LOCKED
     ↓
JWT cũ
     ↓
KHÔNG AUTHENTICATE
AI phải

Tìm một endpoint authenticated đang tồn tại thật.

Ví dụ:

/api/auth/me

chỉ sử dụng nếu source thực sự có.

Nếu không:

[NEED AUDIT]
Không được

Tự tạo:

Token blacklist.
Revocation service.
JWT architecture mới.

Nếu cần sửa JwtAuthenticationFilter:

[DEPENDENCY — QUỐC ANH]
DONE

Có test chứng minh JWT cũ của Locked User không còn authenticate.

Nếu chưa xử lý được:

[BLOCKED — QUỐC ANH]

Không giả vờ DONE.

TASK 1.8 — COOKIE / CSRF / JWT SECURITY AUDIT
🎯 Mục tiêu

Audit nhưng không biến thành Security Hardening Phase.

Kiểm tra:

HttpOnly.
Secure.
SameSite.
JWT Cookie.
Authorization Header.
CSRF.
Stateless Session.
JWT Filter.

AI phải báo:

CURRENT
 ↓
RISK
 ↓
IMPACT
 ↓
RECOMMENDATION
 ↓
OWNER
 ↓
DECISION

Ví dụ:

HttpOnly = false

↓
Security risk

↓
Core Security

↓
Quốc Anh

↓
DEFER / FIX NOW / OUT OF SCOPE
Không được

Tự sửa Cookie/CSRF nếu chưa có quyết định.

TASK 1.9 — TEST DATABASE & TEST STRATEGY
🎯 Mục tiêu

Xác định automated test chạy như thế nào mà không phá MongoDB development.

AI kiểm tra:

src/test
src/test/resources
Maven
application configuration
MongoDB configuration
profile
run.ps1

Phải phân biệt:

Unit Test
 ↓
Mock
 ↓
Không cần MongoDB thật

và:

Integration Test
 ↓
MongoDB
 ↓
Database isolation
Đặc biệt

AI phải kiểm tra:

run.ps1

vì hiện tại script build bằng:

-DskipTests

Không được coi:

BUILD SUCCESS

là bằng chứng Security đã pass.

DONE

Có test strategy rõ ràng.

TASK 1.10 — AUTOMATED SECURITY TEST
🎯 Mục tiêu

Có automated test cho behavior Phase 1.

Tối thiểu:

[ ] Admin login/access
[ ] Admin authorization
[ ] Customer denied
[ ] Vendor denied
[ ] Anonymous denied
[ ] Admin registration protection
[ ] Login redirect
[ ] LOCKED login
[ ] LOCKED old JWT

Nếu behavior phụ thuộc Core Auth:

[DEPENDENCY — QUỐC ANH]

Không tự fake test để làm đẹp kết quả.

DONE

Có test source thực tế trong:

src/test/

và test kiểm tra behavior, không chỉ kiểm tra project compile.

TASK 1.11 — RUNTIME TEST
🎯 Mục tiêu

Kiểm tra hệ thống chạy thật.

Chạy theo project hiện tại:

mvn test

sau đó:

mvn spring-boot:run

hoặc cách chạy chính thức được xác định trong run.ps1.

Kiểm tra
ADMIN
 ↓
Login
 ↓
/admin/dashboard
 ↓
SUCCESS
CUSTOMER
 ↓
/admin/dashboard
 ↓
403
VENDOR
 ↓
/admin/dashboard
 ↓
403
ANONYMOUS
 ↓
/admin/dashboard
 ↓
DENIED
LOCKED
 ↓
Login
 ↓
FAIL
LOCKED
 ↓
OLD JWT
 ↓
Authenticated endpoint
 ↓
FAIL
TASK 1.12 — REGRESSION TEST

Sau khi sửa Admin Access, kiểm tra không phá flow cũ.

Tối thiểu:

[ ] Register
[ ] Customer Login
[ ] Vendor Login
[ ] Admin Login
[ ] Logout
[ ] Home
[ ] Product
[ ] Existing JWT flow

Không được vì sửa Admin login mà làm:

Customer login hỏng
Vendor login hỏng
Registration hỏng
Product hỏng
JWT flow hỏng
TASK 1.13 — PHASE REVIEW

AI phải báo cáo chính xác:

[COMPLETED]

[ROOT CAUSE OF ADMIN ACCESS ISSUE]

[FILES CHANGED]

[FILES NOT CHANGED]

[CHANGES MADE BY HOÀN]

[DEPENDENCIES — QUỐC ANH]

[SECURITY AUDIT RESULT]

[AUTOMATED TEST RESULT]

[RUNTIME TEST RESULT]

[MONGODB RESULT]

[NOT DONE]

[OUT OF SCOPE]

[BLOCKED]

Đặc biệt:

HOÀN ĐÃ LÀM
       ≠
QUỐC ANH CẦN LÀM

Nếu còn dependency chưa xử lý:

không được tự đánh dấu toàn bộ Phase 1 là DONE.

Không chuyển sang Phase 2.

Chờ Hoàn xác nhận:

PHASE 1 DONE
📌 TỔNG HỢP PHASE 1
PHASE 1 — ADMIN ACCESS & SECURITY
│
├── TASK 1.1
│   └── Audit Web Login / API Login / JWT / Admin Access
│
├── TASK 1.2
│   └── Admin Development Account & Runtime Config
│
├── TASK 1.3
│   └── Fix Admin Web Login & Redirect
│
├── TASK 1.4
│   └── Admin Authorization
│
├── TASK 1.5
│   └── Registration ADMIN Guard
│       └── Dependency Quốc Anh
│
├── TASK 1.6
│   └── Account State Login Guard
│       ├── Web Login
│       └── API Login
│
├── TASK 1.7
│   └── LOCKED User + Old JWT
│
├── TASK 1.8
│   └── Cookie / CSRF / JWT Security Audit
│
├── TASK 1.9
│   └── Test Database & Test Strategy
│
├── TASK 1.10
│   └── Automated Security Test
│
├── TASK 1.11
│   └── Runtime Test
│
├── TASK 1.12
│   └── Regression Test
│
└── TASK 1.13
    └── Phase Review