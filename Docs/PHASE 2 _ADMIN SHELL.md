PHASE 2 — ADMIN SHELL & ADMIN UI FOUNDATION
🎯 Mục tiêu Phase 2

Xây dựng một Admin Shell duy nhất và nhất quán làm nền tảng cho toàn bộ Admin.

Luồng:

ADMIN LOGIN
    ↓
/admin/dashboard
    ↓
┌─────────────────────────────────────────────┐
│                 ADMIN TOPBAR                │
├──────────────┬──────────────────────────────┤
│              │                              │
│ ADMIN        │        ADMIN CONTENT         │
│ SIDEBAR      │                              │
│              │                              │
│ Dashboard    │                              │
│ Users        │                              │
│ Categories   │                              │
│ Shops*       │                              │
│ Orders*      │                              │
│ Logout       │                              │
│              │                              │
└──────────────┴──────────────────────────────┘

* chỉ hiển thị navigation khi endpoint đã tồn tại hoặc theo cơ chế placeholder được thống nhất.

Kết quả bắt buộc

Sau Phase 2:

Có một Admin Layout chính.
Admin Header dùng chung.
Admin Sidebar dùng chung.
Admin Flash Message dùng chung.
Admin Error Page/Global Error hoạt động.
Dashboard/User/Category sử dụng cùng Admin Shell.
CSS Admin được tổ chức rõ ràng.
Không làm ảnh hưởng Vendor UI.
Không thay đổi Authentication/JWT.
Không triển khai nghiệp vụ Phase 3 trở đi.
⚠️ PHẠM VI
Phase 2 được làm
Admin Layout.
Admin Sidebar.
Admin Header/Topbar.
Admin Content container.
Admin Flash Message.
Global Error Page cần thiết cho Admin.
Responsive Admin Shell.
Đồng bộ Dashboard với Admin Shell.
Đồng bộ User List với Admin Shell.
Đồng bộ Category List với Admin Shell.
Kiểm tra CSS/JS liên quan Admin.
Admin navigation.
Runtime/UI verification.
Không làm
User Management.
Category Management.
Shop Management.
Order Management.
Dashboard business logic.
Search.
Notification system.
Avatar/Profile Management.
Authentication/JWT.
Role mapping.
Vendor workflow.
Customer workflow.
Product CRUD.
Redesign toàn bộ website.
Refactor toàn bộ project.
Đặc biệt

Không được biến Phase 2 thành:

Admin Shell
+
User Management
+
Category Management
+
Dashboard Development

Phase 2 chỉ tạo nền UI dùng chung.

TASK 2.1 — AUDIT TOÀN BỘ ADMIN UI
🎯 Mục tiêu

Xác định chính xác cấu trúc UI Admin hiện tại trước khi sửa.

AI phải kiểm tra source thật

Tìm và kiểm tra:

admin-sidebar.html
admin-topbar.html
admin-layout.html
dashboard-layout.html
dashboard.css
dashboard.html
user-list.html
user-detail.html
category-manage.html
Admin fragments khác.
CSS Admin.
CSS dùng chung.
JS Admin.
admin-vendor.css.
Các Vendor template sử dụng CSS/fragment chung.

Không được tự đoán file.

Nếu file không tồn tại:

[NEED AUDIT]
AI phải xác định
Admin Page
    ↓
Layout
    ↓
Header
    ↓
Sidebar
    ↓
Content
    ↓
CSS
    ↓
JS

Và lập bảng:

Component	File hiện tại	Đang được sử dụng bởi	Owner	Cần sửa?
Sidebar	...	...	Admin	...
Header	...	...	Admin	...
Layout	...	...	Admin	...
Dashboard CSS	...	...	Admin	...
Vendor CSS	...	Vendor	Vendor	Không nếu không cần
Quan trọng

Nếu admin-vendor.css đang được Vendor sử dụng:

Không được sửa rộng trực tiếp.

Phải ưu tiên:

Admin-specific selector

hoặc tách CSS nếu thực sự cần.

Không code

Task 2.1 chỉ audit.

DONE

Có báo cáo rõ:

Admin UI
   ↓
Layout
   ↓
Fragment
   ↓
CSS
   ↓
JS
   ↓
Dependency
   ↓
Files cần sửa
TASK 2.2 — CHỌN ADMIN SHELL CHÍNH
🎯 Mục tiêu

Chọn một Admin Shell duy nhất làm chuẩn.

Source hiện có:

admin-sidebar.html
admin-topbar.html
admin-layout.html

và:

dashboard-layout.html
dashboard.css

AI phải xác định:

ADMIN SHELL CHÍNH
        ↓
Header
        +
Sidebar
        +
Content
        +
Flash
Quy tắc

Không duy trì song song:

Admin Layout A
Admin Layout B
Dashboard Layout riêng

nếu chúng cùng phục vụ Admin.

Ưu tiên tái sử dụng:

admin-layout
admin-sidebar
admin-topbar

nếu audit xác nhận chúng phù hợp.

Không được tạo thêm một Admin Shell mới nếu source hiện tại đã có shell có thể sử dụng.

DONE

Có một Admin Shell chính thức.

TASK 2.3 — ADMIN LAYOUT SKELETON
🎯 Mục tiêu

Hoàn thiện cấu trúc:

Admin Layout
├── Topbar
├── Sidebar
├── Flash Message
└── Main Content

Template Admin có thể inject nội dung riêng:

Admin Shell
      ↓
Page Content

Ví dụ:

/admin/dashboard
/admin/users
/admin/categories

đều sử dụng cùng Shell.

Không làm

Không thêm business logic.

Không tạo Controller mới.

Không thay đổi Security.

DONE

Admin pages có thể sử dụng cùng layout mà không duplicate Header/Sidebar.

TASK 2.4 — ADMIN SIDEBAR
🎯 Mục tiêu

Tạo Sidebar Admin dùng chung.

Tối thiểu:

Dashboard
Users
Categories
Shops
Orders
Logout

URL phải được kiểm tra từ source thật:

/admin/dashboard
/admin/users
/admin/categories
/admin/shops
/admin/orders
Endpoint chưa tồn tại

Không tạo Controller giả.

Có thể:

Ẩn

hoặc:

Disabled / Coming Soon

AI phải chọn phương án ít ảnh hưởng nhất dựa trên source.

Active menu

Trang hiện tại phải có trạng thái active nếu hệ thống hỗ trợ.

DONE

Tất cả Admin page dùng chung Sidebar.

Không có duplicate Sidebar.

TASK 2.5 — ADMIN HEADER / TOPBAR
🎯 Mục tiêu

Tạo Header dùng chung.

Tối thiểu:

Admin
Username
ROLE_ADMIN
Logout

Tận dụng dữ liệu User hiện tại.

Không thêm
Search.
Notification.
Avatar upload.
Profile Management.

Nếu chưa thuộc contract.

DONE

Dashboard/User/Category đều sử dụng cùng Header.

TASK 2.6 — ADMIN LOGOUT UI
🎯 Mục tiêu

Admin có thể logout từ Admin Shell.

AI phải audit trước
Logout
   ↓
Authentication mechanism
   ↓
JWT
   ↓
Cookie / Header
   ↓
Existing logout flow

Phase 2 không sửa JWT architecture.

Chỉ gọi flow logout hiện tại.

Không được tuyên bố
JWT đã bị revoke

nếu source không có token revocation.

DONE

Admin bấm Logout → sử dụng đúng logout flow hiện tại.

TASK 2.7 — ADMIN FLASH MESSAGE
🎯 Mục tiêu

Tạo một cơ chế hiển thị message thống nhất:

SUCCESS
ERROR
WARNING
INFO

Ví dụ:

✓ User updated successfully

✕ Category not found

⚠ Shop is pending approval
Quan trọng

Không tạo endpoint giả chỉ để test.

Tận dụng:

Thymeleaf model.
Flash attribute.
Cơ chế hiện tại của project.

Không tạo Notification Service.

DONE

Các Admin page có thể sử dụng chung Flash Message.

TASK 2.8 — GLOBAL ERROR PAGE
🎯 Mục tiêu

Đảm bảo các error page mà project thực sự sử dụng tồn tại và hoạt động.

Kiểm tra:

GlobalExceptionHandler
        ↓
Error Controller / View
        ↓
error/*

Tối thiểu audit:

400
403
404
500
Đặc biệt

Kiểm tra lỗi:

/admin/**
      ↓
403
      ↓
Error Page

Không tạo Admin Error System riêng nếu Global Error System hiện tại đã phù hợp.

DONE

Error page hoạt động mà không làm hỏng Customer/Vendor.

TASK 2.9 — MIGRATE DASHBOARD TO ADMIN SHELL
🎯 Mục tiêu

Đưa Dashboard về Admin Shell chính thức.

Hiện tại Dashboard đang có:

dashboard-layout.html
dashboard.css

AI phải kiểm tra và loại bỏ việc Dashboard sử dụng Shell riêng nếu không cần thiết.

Quan trọng — sửa mismatch đã phát hiện

Kiểm tra:

stats.recentActivity

so với DTO thực tế:

recentActivities

Kiểm tra:

act.user
act.timeAgo

so với DTO thực tế:

type
description
time

Nếu mismatch thực sự tồn tại:

→ sửa để Template khớp DTO.

Chart

Kiểm tra:

/*[[${stats.revenueTrend}]]*/

và:

th:inline="javascript"

Nếu thiếu khiến Thymeleaf không inline dữ liệu:

→ sửa trong phạm vi Dashboard integration.

Không làm

Không phát triển thêm Dashboard feature.

Không thêm Search.

Không thêm Notification.

Không thay đổi business calculation nếu không cần thiết để sửa integration.

DONE
/admin/dashboard
      ↓
Admin Shell
      ↓
Dashboard Content

hoạt động đúng với DTO hiện tại.

TASK 2.10 — MIGRATE USER & CATEGORY UI TO ADMIN SHELL
🎯 Mục tiêu

Đảm bảo:

user-list.html
category-manage.html
user-detail.html

đều sử dụng Admin Shell chính thức.

Hiện trạng cần sửa:

User List
    ↓
Sidebar riêng

Category
    ↓
Sidebar riêng

Thành:

Admin Shell
    ├── Sidebar
    ├── Header
    └── Page Content
Không làm

Không triển khai User CRUD.

Không triển khai Category CRUD.

Chỉ thay đổi layout integration.

DONE

Dashboard + User + Category dùng cùng Admin Shell.

TASK 2.11 — ADMIN CSS CONSOLIDATION
🎯 Mục tiêu

Đảm bảo CSS Admin không bị phân tán hoặc conflict.

AI phải kiểm tra:

dashboard.css
admin-vendor.css
style.css
other Admin CSS

Xác định:

CSS nào dành cho Admin.
CSS nào dùng chung.
CSS nào Vendor sử dụng.
CSS nào duplicate.
CSS nào gây conflict.
Quy tắc

Không được phá:

Vendor CSS
Vendor Sidebar
Vendor Header
Vendor Pages

Nếu cần selector Admin:

.admin-...

hoặc selector đủ cụ thể.

Không đổi global CSS nếu không cần.

DONE

Admin UI hiển thị đúng và Vendor không bị ảnh hưởng.

TASK 2.12 — RESPONSIVE ADMIN SHELL
🎯 Mục tiêu

Kiểm tra Admin Shell ở:

Desktop.
Laptop.
Tablet.

Kiểm tra:

Sidebar.
Header.
Content.
Table.
Form.
Button.
Flash Message.

Không để:

overflow
content bị che
sidebar đè content
button tràn
table phá layout

Đây chỉ là responsive cơ bản.

Không redesign toàn bộ website.

TASK 2.13 — ADMIN NAVIGATION TEST

Kiểm tra:

/admin/dashboard
/admin/users
/admin/categories
/admin/shops
/admin/orders

Phải phân biệt:

Endpoint tồn tại
      ↓
Có thể truy cập

và:

Endpoint chưa tồn tại
      ↓
Dependency

Không tạo Controller giả.

DONE

Admin Sidebar không dẫn người dùng vào chức năng giả.

TASK 2.14 — ADMIN SHELL TEST
UI
[ ] Admin Header
[ ] Admin Sidebar
[ ] Main Content
[ ] Flash Message
[ ] Error Page
[ ] Dashboard
[ ] User Page
[ ] Category Page
[ ] Navigation
[ ] Logout
[ ] CSS
[ ] JS
[ ] Responsive
Security
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

Security logic chính thuộc Phase 1.

Phase 2 chỉ regression verification.

Runtime
mvn test

sau đó:

mvn spring-boot:run

Không dùng:

BUILD SUCCESS

làm bằng chứng duy nhất.

TASK 2.15 — REGRESSION TEST

Sau khi hoàn thiện Admin Shell phải kiểm tra:

Login
Register
Logout
Home
Product
Customer
Vendor

Đặc biệt Vendor:

Vendor Sidebar
Vendor Header
Vendor Pages
Vendor CSS

vì có khả năng dùng chung CSS/fragment.

Không để Admin Shell làm hỏng Vendor.

TASK 2.16 — PHASE 2 REVIEW

AI phải báo cáo chính xác:

[COMPLETED]

[ADMIN SHELL]

[FILES CHANGED]

[FILES NOT CHANGED]

[DASHBOARD INTEGRATION]

[USER UI INTEGRATION]

[CATEGORY UI INTEGRATION]

[CSS CHANGES]

[GLOBAL ERROR CHANGES]

[SECURITY VERIFICATION]

[TEST RESULT]

[RUNTIME RESULT]

[REGRESSION RESULT]

[DEPENDENCIES]

[BLOCKERS]

[NOT DONE]

[OUT OF SCOPE]

Đặc biệt phải ghi rõ:

Admin Shell đã hoàn thành

và tách riêng:

Dashboard business logic
User Management
Category Management

vì những phần đó không thuộc Phase 2.

Sau đó:

STOP

Không tự chuyển sang Phase 3.

Chờ Hoàn xác nhận:

PHASE 2 DONE
📋 TỔNG KẾT PHASE 2
PHASE 2 — ADMIN SHELL & ADMIN UI FOUNDATION
│
├── TASK 2.1
│   └── Audit toàn bộ Admin UI
│
├── TASK 2.2
│   └── Chọn Admin Shell chính
│
├── TASK 2.3
│   └── Admin Layout Skeleton
│
├── TASK 2.4
│   └── Admin Sidebar
│
├── TASK 2.5
│   └── Admin Header / Topbar
│
├── TASK 2.6
│   └── Logout UI
│
├── TASK 2.7
│   └── Flash Message
│
├── TASK 2.8
│   └── Global Error Page
│
├── TASK 2.9
│   └── Dashboard Integration
│       ├── Admin Shell
│       ├── DTO mismatch
│       └── Chart inline
│
├── TASK 2.10
│   └── User + Category UI Integration
│
├── TASK 2.11
│   └── Admin CSS Consolidation
│
├── TASK 2.12
│   └── Responsive Admin Shell
│
├── TASK 2.13
│   └── Admin Navigation Test
│
├── TASK 2.14
│   └── Admin Shell Test
│
├── TASK 2.15
│   └── Regression Test
│
└── TASK 2.16
    └── Phase Review