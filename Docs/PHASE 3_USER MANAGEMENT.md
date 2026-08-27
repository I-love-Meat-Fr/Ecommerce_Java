PHASE 3 — USER MANAGEMENT
🎯 Mục tiêu Phase 3

Hoàn thiện chức năng User Management của Admin theo đúng contract đã được chốt tại Gate 0.

Luồng chính:

ADMIN
  ↓
/admin/users
  ↓
┌─────────────────────────────────────────┐
│             USER MANAGEMENT              │
├─────────────────────────────────────────┤
│ User List                                │
│ User Detail                              │
│ Role                                     │
│ Status                                   │
│ Lock                                     │
│ Unlock                                   │
└─────────────────────────────────────────┘

Admin phải có thể:

Xem danh sách User đúng USER LIST SCOPE của Gate 0.
Xem chi tiết User.
Lock User.
Unlock User.
Role Management chỉ khi Gate 0 cho phép.
Nhận thông báo lỗi/thành công rõ ràng.
Thao tác thông qua Admin Shell của Phase 2.
Dữ liệu phải được cập nhật chính xác trong MongoDB.
Có automated test, runtime test và regression test.
⚠️ Quy tắc quan trọng

Gate 0 là nguồn quyết định nghiệp vụ.

Không tự quyết định:

User Scope
Role Management
ADMIN có được Lock hay không
ADMIN có được Lock chính mình hay không
UNVERIFIED → LOCKED
LOCKED → ACTIVE
LOCKED → trạng thái trước đó
CUSTOMER → VENDOR
CUSTOMER → ADMIN
VENDOR → ADMIN

Nếu Gate 0 chưa quyết định:

[DEPENDENCY — GATE 0]

Không tự suy luận.

🚫 KHÔNG LÀM TRONG PHASE 3

Không làm:

Shop Management.
Product Management.
Order Management.
Review Management.
Vendor Management.
Customer UI ngoài phần cần để verify User behavior.
Authentication architecture mới.
JWT architecture mới.
Token blacklist/revocation architecture.
Refactor toàn bộ Security.
Thay đổi User model nếu không bắt buộc.
Hiển thị password/passwordHash/JWT.
Role Management nếu Gate 0 không cho phép.
Tạo Service mới nếu Service hiện tại đã đủ.
Tạo Controller mới nếu Controller hiện tại có thể mở rộng.
Tự tạo trạng thái User mới.

Nếu phát hiện vấn đề thuộc Core Auth/JWT:

[DEPENDENCY — QUỐC ANH]

Hoàn chỉ sửa nếu đã được xác định thuộc ownership của Phase 3.

TASK 3.1 — AUDIT USER MANAGEMENT HIỆN TẠI
🎯 Mục tiêu

Đọc source hiện tại và xác định chính xác User Management đang có gì trước khi sửa.

AI phải kiểm tra
AdminUserController
AdminUserService
AdminUserServiceImpl
UserRepository
User
AccountStatus
UserRole
DTO/ViewModel liên quan.
user-list.html
user-detail.html
CSS/JS liên quan.
SecurityConfig
Auth flow liên quan User status.
AI phải xác định
/admin/users hiện hoạt động thế nào?
Controller đang gọi Repository trực tiếp ở đâu?
AdminUserService đã tồn tại chưa?
Service implementation đã tồn tại chưa?
User List hiện query role nào?
User Detail đã có template chưa?
Route /admin/users/{id} đã tồn tại chưa?
Lock hiện tại hoạt động thế nào?
Unlock hiện tại hoạt động thế nào?
User có role gì?
User có status gì?
Repository có method nào phục vụ User Management?
Có type mismatch giữa String, UserRole, AccountStatus không?
User entity/document có field nhạy cảm nào?
Admin Shell Phase 2 đang được sử dụng thế nào?
Quan trọng

Source hiện đã có:

AdminUserService
AdminUserServiceImpl
user-detail.html

AI không được tạo lại những thành phần này.

Phải tận dụng và hoàn thiện code hiện có.

Không code

Task 3.1 chỉ audit.

DONE

Báo cáo:

[CURRENT]

[PROBLEM]

[FILES INVOLVED]

[EXISTING SERVICE]

[EXISTING CONTROLLER]

[EXISTING REPOSITORY]

[USER SCOPE]

[SECURITY DEPENDENCY]

[PROPOSED CHANGE]

[OUT OF SCOPE]
TASK 3.2 — CONNECT EXISTING USER SERVICE
🎯 Mục tiêu

Hoàn thiện kiến trúc User Management hiện tại:

AdminUserController
        ↓
AdminUserService
        ↓
UserRepository
        ↓
MongoDB
Quan trọng

Không tạo Service mới.

Source đã có AdminUserService và implementation thì phải sử dụng chúng.

AI cần:

Chuyển business logic phù hợp từ Controller sang Service.
Controller không gọi Repository trực tiếp nếu logic đó thuộc Service.
Giữ nguyên kiến trúc hiện tại của các module khác.
Không refactor toàn project.
Service có thể chịu trách nhiệm
Find User.
Find User Detail.
List User.
Validate User tồn tại.
Lock.
Unlock.
Business state transition.
Role rule nếu Gate 0 cho phép.
Controller chịu trách nhiệm
Mapping request.
Gọi Service.
Chọn View/redirect.
Flash message.
HTTP response.
Không được

Không biến Task này thành:

Refactor toàn bộ project
DONE

User Management sử dụng được:

Controller
    ↓
AdminUserService
    ↓
UserRepository

và không còn các business logic không cần thiết nằm trực tiếp trong Controller.

TASK 3.3 — USER LIST
🎯 Mục tiêu

Hoàn thiện:

GET /admin/users
Backend

User List phải tuân thủ:

Gate 0
   ↓
USER LIST SCOPE

AI không tự chọn scope.

Ví dụ:

CUSTOMER

hoặc:

CUSTOMER + VENDOR

hoặc scope khác nếu Gate 0 đã chốt.

UI

Hiển thị tối thiểu:

ID nếu cần.
Email.
Full name.
Role.
Status.
Action.

Không hiển thị:

Password.
Password hash.
JWT.
Secret.
Security token.

Action tối thiểu:

View
Lock
Unlock

theo trạng thái và contract.

DONE
/admin/users
       ↓
AdminUserService
       ↓
MongoDB
       ↓
User List

hiển thị đúng User Scope.

TASK 3.4 — USER DETAIL
🎯 Mục tiêu

Hoàn thiện:

GET /admin/users/{id}
Quan trọng

Template user-detail.html đã tồn tại.

Không tạo lại template nếu không cần.

AI phải nối:

/admin/users/{id}
       ↓
AdminUserController
       ↓
AdminUserService
       ↓
UserRepository
       ↓
MongoDB
       ↓
user-detail.html
Thông tin được hiển thị

Theo source/contract:

ID.
Email.
Full name.
Role.
Status.
Các thông tin User hợp lệ khác.
Tuyệt đối không hiển thị
password
passwordHash
JWT
token
secret

Nếu View đang nhận trực tiếp User, AI phải đánh giá nguy cơ expose field.

Chỉ tạo DTO/ViewModel nếu thực sự cần.

Not Found

User không tồn tại:

→ 404 / Global Error Handler hiện tại

Không tạo Error System mới nếu hệ thống hiện tại đã có.

DONE
/admin/users/{id} hoạt động.
User đúng được hiển thị.
ID không tồn tại được xử lý.
Không expose security data.
TASK 3.5 — LOCK USER
🎯 Mục tiêu

Hoàn thiện chức năng Lock từ UI đến MongoDB.

Luồng:

Admin
 ↓
Lock
 ↓
Controller
 ↓
AdminUserService
 ↓
Business Guard
 ↓
Update Status
 ↓
MongoDB
Phải kiểm tra
User không tồn tại
→ Error

User đã LOCKED
→ Behavior theo contract

Admin tự khóa mình
→ Behavior theo Gate 0

Admin khóa Admin khác
→ Behavior theo Gate 0

UNVERIFIED
→ Behavior theo Gate 0

Không tự suy luận state transition.

Security dependency

Phase 1 chịu trách nhiệm Core Security.

Phase 3 phải verify lại:

LOCKED
 ↓
Login
 ↓
FAIL

và:

LOCKED
 ↓
OLD JWT
 ↓
FAIL

Nếu behavior vẫn sai do Core Auth/JWT:

[DEPENDENCY — QUỐC ANH]

Không tự thiết kế JWT revocation.

UI

Có:

Lock

và:

Success / Error message
DONE

Lock không chỉ là:

repository.save()

mà phải có business validation và behavior đúng contract.

TASK 3.6 — UNLOCK USER
🎯 Mục tiêu

Hoàn thiện Unlock theo User State Contract.

Luồng:

Admin
 ↓
Unlock
 ↓
Controller
 ↓
AdminUserService
 ↓
Validate
 ↓
State Transition
 ↓
MongoDB
Quan trọng

Gate 0 phải xác định:

LOCKED → ACTIVE

hay:

LOCKED → Previous State

Nếu chưa có quyết định:

[DEPENDENCY — GATE 0]

Không tự tạo previousStatus hoặc cơ chế mới.

Test
LOCKED → Unlock

phải chuyển sang đúng state.

ACTIVE → Unlock

phải có behavior rõ ràng.

Not Found
→ Error
DONE

Unlock hoạt động đúng State Contract.

TASK 3.7 — USER BUSINESS GUARD
🎯 Mục tiêu

Đảm bảo các business rule của User Management được thực thi tại Service.

Ví dụ:

Lock User
   ↓
User tồn tại?
   ↓
State hợp lệ?
   ↓
Operation được phép?
   ↓
Update

Không để UI quyết định business rule.

Không để Controller tự quyết định state transition.

Phải kiểm tra
User không tồn tại.
State hiện tại.
Lock.
Unlock.
Admin protection theo Gate 0.
Role protection theo Gate 0.
State transition theo Gate 0.
DONE

Business rule nằm ở Service layer phù hợp.

TASK 3.8 — ROLE MANAGEMENT — CONDITIONAL
⚠️ CHỈ THỰC HIỆN NẾU GATE 0 = YES

Nếu:

Role Management = SKIPPED

→ bỏ qua Task 3.8.

Nếu:

Role Management = DECIDED / YES

AI phải kiểm tra trước:

Role nào được thay đổi?
CUSTOMER → VENDOR?
VENDOR → CUSTOMER?
Có được → ADMIN?
Có được sửa ADMIN?
Có ảnh hưởng Security không?
Có ảnh hưởng Vendor/Customer domain không?

Không được tự thêm dropdown role chỉ vì dễ làm.

DONE

Role Management chỉ tồn tại đúng contract.

TASK 3.9 — USER UI CONSISTENCY

Task này không xây lại User UI.

Kiểm tra các UI đã hoàn thiện:

3.3 User List
3.4 User Detail
3.5 Lock
3.6 Unlock

phải dùng Admin Shell Phase 2.

Kiểm tra:

Header.
Sidebar.
Table.
Detail.
Button.
Status badge.
Flash message.
Error.
Navigation.
Responsive cơ bản.

Không redesign toàn bộ Admin UI.

DONE

User Management nhất quán với Admin Shell.

TASK 3.10 — USER ERROR HANDLING

Kiểm tra tối thiểu:

User không tồn tại
→ 404

Invalid ID
→ behavior đúng source

Lock User không tồn tại
→ Error

Unlock User không tồn tại
→ Error

User đã LOCKED
→ Contract behavior

User ACTIVE
→ Contract behavior

Unauthorized
→ 403

Không tạo Exception System mới nếu Global Exception Handler hiện tại đã đủ.

DONE

Các lỗi User Management được xử lý rõ ràng và nhất quán.

TASK 3.11 — USER SECURITY & AUTHORIZATION TEST

Kiểm tra:

ADMIN
 ↓
/admin/users
 ↓
ALLOW
CUSTOMER
 ↓
/admin/users
 ↓
403
VENDOR
 ↓
/admin/users
 ↓
403

Kiểm tra endpoint thực tế, không chỉ UI.

Không lặp Phase 1

Phase 1 đã kiểm tra /admin/**.

Phase 3 tập trung:

/admin/users/**

và các endpoint:

GET /admin/users
GET /admin/users/{id}
POST .../lock
POST .../unlock

theo route thực tế của source.

TASK 3.12 — AUTOMATED USER TEST
🎯 Mục tiêu

Có automated test thật cho User Management.

List
Admin được truy cập.
Đúng User Scope.
Empty list.
Unauthorized.
Detail
User tồn tại.
User không tồn tại.
Unauthorized.
Lock
Valid User.
Not Found.
Invalid state.
Business guard.
Unlock
Valid User.
Not Found.
Invalid state.
Business guard.
Role Management

Chỉ test nếu Gate 0 cho phép.

Quan trọng

Nếu test phụ thuộc Core Auth/JWT:

[DEPENDENCY]

Không giả vờ coi test pass là Core Security đã đúng.

TASK 3.13 — MONGODB INTEGRATION TEST
🎯 Mục tiêu

Xác minh thay đổi thực sự được ghi vào MongoDB.

Kiểm tra:

User List
 ↓
MongoDB
Lock
 ↓
MongoDB
 ↓
status = expected state
Unlock
 ↓
MongoDB
 ↓
status = expected state

Nếu test DB chưa được Gate 0 xác định:

[DEPENDENCY — TEST STRATEGY]

Không tự đổi database development.

Không tự tạo database production.

TASK 3.14 — RUNTIME TEST

Chạy:

mvn test

Sau đó:

mvn spring-boot:run

Kiểm tra thực tế:

/admin/users
/admin/users/{id}

Thực hiện:

List
Detail
Lock
Unlock
Invalid ID
Not Found
403

Kiểm tra MongoDB sau các thao tác thay đổi dữ liệu.

Không được

Không kết luận:

BUILD SUCCESS

là đủ.

TASK 3.15 — REGRESSION TEST

Sau User Management phải kiểm tra:

Login.
Register.
Admin Shell.
Customer.
Vendor.
Product.
Category.
Shop nếu đã tồn tại.
Logout.

Đặc biệt:

Lock Customer
 ↓
Customer Login
 ↓
Expected behavior

và:

Vendor
 ↓
Vendor Login
 ↓
Vendor functionality

Không để User Management làm hỏng:

Login.
Vendor.
Customer.
Product.
Category.
Admin Shell.
MongoDB.

Nếu phát hiện lỗi ngoài phạm vi:

[OUT OF SCOPE]

hoặc:

[DEPENDENCY]

Không tự mở rộng Phase 3.

TASK 3.16 — PHASE 3 REVIEW

AI phải báo cáo chính xác:

[COMPLETED]

[USER SCOPE]

[GATE 0 DECISIONS USED]

[FILES CHANGED]

[CONTROLLER]

[SERVICE]

[REPOSITORY]

[DTO / VIEW MODEL]

[TEMPLATE]

[BUSINESS RULES]

[SECURITY RESULT]

[DATABASE CHANGES]

[AUTOMATED TEST]

[INTEGRATION TEST]

[MONGODB VERIFICATION]

[RUNTIME RESULT]

[REGRESSION RESULT]

[DEPENDENCIES]

[NOT DONE]

[OUT OF SCOPE]

Đặc biệt phải tách rõ:

HOÀN ĐÃ LÀM

và:

QUỐC ANH / DEPENDENCY CẦN XỬ LÝ

Không được coi Dependency chưa xử lý là COMPLETED.

Sau báo cáo:

STOP

Không tự chuyển Phase 4.

Chờ Hoàn xác nhận:

PHASE 3 DONE
📌 TỔNG KẾT PHASE 3
PHASE 3 — USER MANAGEMENT
│
├── TASK 3.1
│   └── Audit User Management
│
├── TASK 3.2
│   └── Connect Existing User Service
│
├── TASK 3.3
│   └── User List
│
├── TASK 3.4
│   └── User Detail
│
├── TASK 3.5
│   └── Lock User
│
├── TASK 3.6
│   └── Unlock User
│
├── TASK 3.7
│   └── User Business Guard
│
├── TASK 3.8
│   └── Role Management — CONDITIONAL
│
├── TASK 3.9
│   └── UI Consistency
│
├── TASK 3.10
│   └── Error Handling
│
├── TASK 3.11
│   └── Security & Authorization Test
│
├── TASK 3.12
│   └── Automated User Test
│
├── TASK 3.13
│   └── MongoDB Integration Test
│
├── TASK 3.14
│   └── Runtime Test
│
├── TASK 3.15
│   └── Regression Test
│
└── TASK 3.16
    └── Phase Review