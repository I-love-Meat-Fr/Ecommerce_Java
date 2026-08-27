PHASE 7 — ADMIN DASHBOARD & REVENUE
🎯 Mục tiêu Phase 7

Hoàn thiện Admin Dashboard dựa trên các module Admin đã hoàn thành ở các Phase trước.

Luồng:

ADMIN
  ↓
/admin/dashboard
  ↓
Dashboard
  ├── Tổng quan User
  ├── Tổng quan Shop
  ├── Tổng quan Order
  └── Revenue nếu Gate 0/Workbook yêu cầu

Phase 7 tập trung vào tổng hợp dữ liệu để Admin theo dõi, không tạo lại nghiệp vụ của User, Shop, Category hoặc Order.

Dashboard phải lấy dữ liệu từ hệ thống thực tế, không hard-code số liệu.

⚠️ PHẠM VI
Phase này làm
Admin Dashboard.
Các KPI được Workbook giao.
User statistics nếu được giao.
Shop statistics nếu được giao.
Order statistics nếu được giao.
Revenue nếu Gate 0 đã xác nhận.
Dashboard UI.
Security.
Automated Test.
Runtime + MongoDB verification.
Regression.
Không tự làm
User Management.
Shop Approval.
Category Management.
Order Management.
Vendor Dashboard.
Customer Dashboard.
Checkout.
Payment.
Refund.
Thay đổi Order workflow.
Thay đổi Shop workflow.
Thay đổi User workflow.
Tạo nghiệp vụ Revenue mới.
Tự định nghĩa Revenue nếu Workbook chưa chốt.
Redesign toàn bộ Admin UI.
Refactor toàn bộ project.

Nếu một KPI chưa được xác định:

[DEPENDENCY]
→ Không tự suy đoán.
TASK 7.1 — AUDIT ADMIN DASHBOARD HIỆN TẠI
🎯 Mục tiêu

Xác định Dashboard hiện tại đã có gì và dữ liệu có thể lấy từ đâu.

AI cần kiểm tra
AdminDashboardController.
Dashboard Service nếu có.
User Repository.
Shop Repository.
Order Repository.
Product Repository nếu Dashboard cần.
Revenue-related code nếu có.
Admin Dashboard Template.
CSS/JS.
SecurityConfig.
MongoDB structure.

Không được tự bịa tên file.

Nếu không tìm thấy:

[NEED AUDIT]
AI phải xác định
Dashboard
   ↓
Controller
   ↓
Service / Repository
   ↓
MongoDB

Các dữ liệu hiện có:

User
Shop
Order
Product
Revenue

Phải xác định:

Dashboard hiện tại có hoạt động không?
Có hard-code dữ liệu không?
KPI nào đã có?
KPI nào thiếu?
Repository hiện tại có query đủ không?
Có thể dùng lại dữ liệu từ Phase 3–6 không?
Không code

Task này chỉ audit.

Báo cáo:

[CURRENT]

[PROBLEM]

[DEPENDENCY]

[OWNER]

[PROPOSED CHANGE]

[OUT OF SCOPE]
DONE

Có kết luận chính xác Dashboard hiện tại cần sửa hoặc xây phần nào.

TASK 7.2 — CHỐT DASHBOARD CONTRACT

Task này chỉ thực hiện nếu Gate 0 chưa chốt đầy đủ.

AI không được tự quyết định KPI.

Phải xác định Dashboard cần hiển thị những gì.

Ví dụ:

Users
Shops
Orders
Revenue

Nhưng chỉ triển khai những mục thực sự thuộc Workbook.

User Statistics

Nếu được giao:

Total Users
Active Users
Locked Users

Nếu không:

[SKIPPED]
Shop Statistics

Nếu được giao:

Total Shops
Pending Shops
Approved/Active Shops

Phải sử dụng đúng trạng thái thực tế của Shop.

Không tự tạo state mới.

Order Statistics

Nếu được giao:

Total Orders
Pending Orders
Completed Orders
Cancelled Orders

Phải dùng status thực tế trong source.

Không tự tạo status.

Revenue

Nếu Workbook/Gate 0 yêu cầu Revenue:

Phải xác định:

Revenue =
?

Ví dụ có thể liên quan:

Tổng tiền Order.
Order đã thanh toán.
Order đã hoàn thành.
Order bị hủy có tính hay không.

Không tự chọn công thức Revenue.

Nếu chưa có contract:

[DEPENDENCY]

Không triển khai Revenue.

DONE

Dashboard Contract xác định rõ:

KPI
↓
Source
↓
Query
↓
Calculation
↓
Display
TASK 7.3 — ADMIN DASHBOARD DATA LAYER
🎯 Mục tiêu

Chuẩn bị dữ liệu Dashboard mà không làm thay đổi nghiệp vụ của các module khác.

Luồng mong muốn:

Dashboard UI
      ↓
AdminDashboardController
      ↓
DashboardService
      ↓
Repositories
      ↓
MongoDB

Nếu project hiện tại đã có kiến trúc khác:

AI phải ưu tiên tận dụng kiến trúc hiện tại.

Không refactor toàn bộ project chỉ để làm Dashboard.

Không được

Sửa:

User business logic.
Shop business logic.
Order business logic.
Product business logic.

chỉ để lấy KPI.

Nếu Repository hiện tại chưa đủ query:

Chỉ bổ sung query cần thiết cho Dashboard.

DONE

Dashboard có data layer rõ ràng và không làm thay đổi nghiệp vụ module khác.

TASK 7.4 — ADMIN DASHBOARD OVERVIEW
🎯 Mục tiêu

Admin truy cập được:

/admin/dashboard

và nhìn thấy tổng quan hệ thống.

UI

Dashboard có thể gồm:

┌──────────────┐
│ Total Users  │
└──────────────┘

┌──────────────┐
│ Total Shops  │
└──────────────┘

┌──────────────┐
│ Total Orders │
└──────────────┘

┌──────────────┐
│ Revenue      │
└──────────────┘

Chỉ hiển thị KPI đã được Contract xác nhận.

Không hard-code:

Users = 120
Orders = 50
Revenue = 100M
DONE

Dashboard hiển thị dữ liệu thực tế từ MongoDB.

TASK 7.5 — USER STATISTICS
🎯 Mục tiêu

Chỉ triển khai nếu Workbook yêu cầu.

Dashboard có thể lấy dữ liệu từ User Management.

Ví dụ:

Total Users
      ↓
UserRepository
      ↓
MongoDB

Nếu cần:

ACTIVE
LOCKED
UNVERIFIED

phải dùng state thực tế.

Không thay đổi User Management.

DONE

Dashboard hiển thị đúng User statistics.

Nếu không được giao:

[SKIPPED]
TASK 7.6 — SHOP STATISTICS
🎯 Mục tiêu

Hiển thị tổng quan Shop nếu Workbook yêu cầu.

Có thể bao gồm:

Total Shops
Pending Shops
Approved Shops
Inactive Shops

Chỉ sử dụng state đã tồn tại.

Không tự tạo:

APPROVED
REJECTED

nếu Shop Contract chưa có.

Không sửa Shop Approval workflow.

DONE

Shop statistics chính xác với MongoDB.

TASK 7.7 — ORDER STATISTICS
🎯 Mục tiêu

Hiển thị tổng quan Order nếu Workbook yêu cầu.

Ví dụ:

Total Orders
Pending
Processing
Completed
Cancelled

Status phải lấy từ source thật.

Không tự tạo status mới.

Không thay đổi Order workflow.

DONE

Order statistics khớp dữ liệu thực tế.

TASK 7.8 — REVENUE
🎯 Mục tiêu

Chỉ triển khai nếu Gate 0/Workbook đã xác nhận Revenue.

Phải xác định rõ:

Revenue Calculation Contract

Ví dụ:

Order
 ↓
Eligible Order
 ↓
Order Total
 ↓
Revenue

Phải biết:

Order nào được tính.
Order nào không được tính.
Cancelled có tính không.
Pending có tính không.
Refund có ảnh hưởng không.
Quan trọng

Nếu chưa có quyết định:

[DEPENDENCY]

Không tự đưa một con số Revenue vào Dashboard.

DONE

Revenue được tính đúng theo Contract.

MongoDB/API data có thể đối chiếu.

TASK 7.9 — DASHBOARD SECURITY
🎯 Mục tiêu

Đảm bảo chỉ Admin được truy cập Dashboard.

Test:

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

Không chỉ kiểm tra Sidebar có/không.

Phải kiểm tra endpoint thật.

DONE

Dashboard được bảo vệ đúng ROLE_ADMIN.

TASK 7.10 — DASHBOARD DATA ACCURACY TEST
🎯 Mục tiêu

Đảm bảo KPI trên Dashboard khớp MongoDB.

Ví dụ:

MongoDB:

Users = 10
Shops = 4
Orders = 15

Dashboard phải phản ánh đúng dữ liệu theo Contract.

Không test bằng cách chỉ nhìn UI.

Kiểm tra
Empty database.
Có dữ liệu.
Locked User.
Pending Shop.
Order status.
Revenue nếu có.
DONE

Dashboard data khớp nguồn dữ liệu.

TASK 7.11 — DASHBOARD AUTOMATED TEST
🎯 Mục tiêu

Có automated test thực sự.

Tối thiểu:

[ ] Admin authorization
[ ] Customer denied
[ ] Vendor denied
[ ] Anonymous denied
[ ] Dashboard loads
[ ] Empty data
[ ] User statistics
[ ] Shop statistics
[ ] Order statistics
[ ] Revenue

Chỉ test Revenue nếu Revenue được triển khai.

Không dùng:

BUILD SUCCESS

làm bằng chứng duy nhất.

Nếu cần MongoDB Integration Test:

→ sử dụng Test DB Strategy đã xác nhận.
TASK 7.12 — DASHBOARD UI CONSISTENCY
🎯 Mục tiêu

Dashboard phải sử dụng Admin Shell từ Phase 2.

Kiểm tra:

Header.
Sidebar.
Navigation.
KPI Cards.
Table nếu có.
Button.
Flash/Error.
Responsive cơ bản.
Empty State.

Không redesign toàn bộ Admin UI.

Không sửa UI Customer/Vendor.

DONE

Dashboard đồng nhất với Admin Shell.

TASK 7.13 — RUNTIME + MONGODB TEST
🎯 Mục tiêu

Kiểm tra Dashboard trên application thật.

Chạy:

mvn test

sau đó:

mvn spring-boot:run

Truy cập:

/admin/dashboard

Kiểm tra:

Login Admin
     ↓
Dashboard
     ↓
KPI
     ↓
MongoDB

Đối chiếu:

MongoDB
   ↕
Dashboard

Kiểm tra khi:

Có dữ liệu.
Không có dữ liệu.
Thêm User.
Lock User.
Thêm/Approve Shop.
Có Order.
Order thay đổi theo workflow hiện tại.
Revenue nếu có.

Không tự thay đổi dữ liệu chỉ để Dashboard hiển thị đẹp.

TASK 7.14 — REGRESSION TEST

Sau Dashboard phải kiểm tra:

Login.
Register.
Home.
Product.
Customer.
Vendor.
Admin Shell.
User Management.
Category Management.
Shop Approval.
Order Management.

Đặc biệt:

Dashboard
   ↓
READ DATA
   ↓
Không làm thay đổi nghiệp vụ

Không được làm hỏng:

Admin Login.
Admin Authorization.
User Management.
Category.
Shop.
Order.
Product.
Vendor.
Customer.
MongoDB.

Nếu phát hiện lỗi ngoài phạm vi:

[OUT OF SCOPE]

hoặc:

[DEPENDENCY]

Không tự sửa.

TASK 7.15 — PHASE 7 REVIEW

Trước khi kết thúc Phase 7, AI phải báo cáo:

[COMPLETED]

[FILES CHANGED]

[DASHBOARD CONTRACT]

[KPI IMPLEMENTED]

[BACKEND CHANGES]

[FRONTEND CHANGES]

[SECURITY CHANGES]

[TESTS]

[RUNTIME RESULT]

[MONGODB RESULT]

[DEPENDENCIES]

[SKIPPED]

[NOT DONE]

[OUT OF SCOPE]

Đặc biệt phải phân biệt:

HOÀN ĐÃ LÀM

với:

DEPENDENCY — NGƯỜI KHÁC

và:

SKIPPED — KHÔNG THUỘC WORKBOOK

Không tự chuyển sang Phase tiếp theo.

Chờ Hoàn xác nhận:

PHASE 7 DONE
📋 TỔNG KẾT PHASE 7
PHASE 7 — ADMIN DASHBOARD & REVENUE

│
├── TASK 7.1
│   └── Audit Admin Dashboard
│
├── TASK 7.2
│   └── Chốt Dashboard Contract
│
├── TASK 7.3
│   └── Dashboard Data Layer
│
├── TASK 7.4
│   └── Admin Dashboard Overview
│
├── TASK 7.5
│   └── User Statistics
│       └── Nếu Workbook yêu cầu
│
├── TASK 7.6
│   └── Shop Statistics
│       └── Nếu Workbook yêu cầu
│
├── TASK 7.7
│   └── Order Statistics
│       └── Nếu Workbook yêu cầu
│
├── TASK 7.8
│   └── Revenue
│       └── Nếu Gate 0/Workbook xác nhận
│
├── TASK 7.9
│   └── Dashboard Security
│
├── TASK 7.10
│   └── Dashboard Data Accuracy
│
├── TASK 7.11
│   └── Automated Test
│
├── TASK 7.12
│   └── UI Consistency
│
├── TASK 7.13
│   └── Runtime + MongoDB
│
├── TASK 7.14
│   └── Regression Test
│
└── TASK 7.15
    └── Phase Review