PHASE 6 — ADMIN ORDER MANAGEMENT
🎯 Mục tiêu Phase 6

Hoàn thiện chức năng Admin xem và theo dõi Order trong đúng phạm vi BTL.

Luồng:

ADMIN
  ↓
/admin/orders
  ↓
Order List
  ↓
Order Detail
  ↓
Theo dõi thông tin Order

Admin có thể:

Xem danh sách Order.
Xem chi tiết Order.
Kiểm tra thông tin Customer.
Kiểm tra Shop/Vendor liên quan nếu dữ liệu có.
Kiểm tra Product/Item trong Order.
Theo dõi trạng thái Order.

Quan trọng:

Phase này không mặc định cho Admin sửa trạng thái Order.

Nếu Workbook/Team Contract chỉ yêu cầu Admin xem/theo dõi Order, thì:

Admin
  ↓
READ ONLY

Không tự thêm:

Approve Order
Cancel Order
Change Status
Refund
Update Payment
Delete Order

nếu Gate 0/Workbook chưa giao.

⚠️ PHẠM VI
Phase 6 CHỈ làm
Order List.
Order Detail.
Order filtering/search nếu Workbook đã yêu cầu.
Hiển thị Order Status.
Hiển thị thông tin Customer.
Hiển thị Shop/Vendor liên quan nếu source có.
Hiển thị Order Items.
Security cho /admin/orders.
Automated Test.
Runtime + MongoDB verification.
Regression.
Không tự làm
Customer Order Management.
Vendor Order Management.
Checkout.
Cart.
Payment.
Refund.
Order creation.
Thay đổi Order workflow.
Thay đổi trạng thái Order nếu chưa được giao.
Xóa Order.
Sửa Product.
Sửa Vendor.
Sửa Customer.
Authentication/JWT.
Redesign toàn bộ Admin UI.
Refactor toàn bộ Order module.

Nếu phát hiện Order workflow chưa rõ:

[DEPENDENCY]
→ DỪNG task liên quan
→ Báo Hoàn
TASK 6.1 — AUDIT ORDER HIỆN TẠI
🎯 Mục tiêu

Hiểu chính xác Order hiện tại đang được lưu trữ và xử lý như thế nào trước khi code.

AI cần kiểm tra

Tìm trong source thật:

Order Controller.
Order Service.
Order Repository.
Order Document/Model.
Order DTO.
Order Item.
Product Document/Model.
User/Customer reference.
Shop/Vendor reference.
Order Status.
Payment information nếu có.
Order Template.
CSS/JS.
SecurityConfig.
MongoDB mapping.

Không được tự bịa tên file.

Nếu không xác định được:

[NEED AUDIT]
AI phải xác định
Order
├── id
├── customer/user
├── shop/vendor
├── items
├── total
├── status
├── payment
├── createdAt
└── các field thực tế khác

Đặc biệt phải xác định:

Order có phải một document không?
Một Customer có nhiều Order không?
Một Shop có nhiều Order không?
Order Item lưu productId hay embedded Product?
Product đã bị xóa thì Order Item còn hiển thị thế nào?
Status hiện có những giá trị nào?
Admin Order Controller đã tồn tại chưa?
Repository hiện tại có query đủ cho Admin không?
Không code

Chỉ audit.

Báo cáo:

[CURRENT]

[PROBLEM]

[DEPENDENCY]

[OWNER]

[PROPOSED CHANGE]

[OUT OF SCOPE]
DONE

Có sơ đồ:

Admin UI
   ↓
Admin Order Controller
   ↓
Order Service / Repository
   ↓
MongoDB

và xác định chính xác phần cần sửa.

TASK 6.2 — CHỐT ORDER ADMIN CONTRACT

Task này chỉ xử lý những điểm Gate 0/Workbook chưa chốt.

AI không được tự quyết định nghiệp vụ mới.

Phải xác định Admin được phép:

VIEW
DETAIL
FILTER
SEARCH

hay có:

UPDATE STATUS

Nếu Workbook chỉ ghi Admin xem/theo dõi:

ORDER MANAGEMENT = READ ONLY
Cần xác định
Order Status

Ví dụ source có:

PENDING
CONFIRMED
PROCESSING
SHIPPED
DELIVERED
CANCELLED

AI phải dùng status thực tế trong source.

Không tự thêm:

APPROVED
REJECTED
COMPLETED
Admin có được thay đổi status?

Nếu chưa được giao:

[SKIPPED]
Customer information

Chỉ hiển thị thông tin cần thiết.

Không expose:

Password.
JWT.
Sensitive authentication data.
Thông tin không cần cho Admin Order.
Vendor/Shop information

Chỉ sử dụng dữ liệu thực tế có trong Order.

Không tự thay đổi Vendor model.

DONE

Order Admin Contract rõ ràng.

TASK 6.3 — ADMIN ORDER LIST
🎯 Mục tiêu

Admin xem được danh sách Order.

Luồng:

/admin/orders
      ↓
Controller
      ↓
Service / Repository
      ↓
MongoDB
UI tối thiểu

Hiển thị:

Order ID.
Customer.
Shop/Vendor nếu có.
Total.
Status.
Created date.
Action → Detail.

Không hiển thị field không thuộc phạm vi.

Trường hợp
Có Order
→ hiển thị danh sách
Không có Order
→ Empty State
DONE
/admin/orders

hoạt động.

Dữ liệu lấy từ MongoDB thật.

Không lỗi Thymeleaf.

UI sử dụng Admin Shell.

TASK 6.4 — ORDER DETAIL
🎯 Mục tiêu

Admin xem được đầy đủ thông tin cần thiết của một Order.

Luồng:

Order List
   ↓
View Detail
   ↓
/admin/orders/{id}
UI

Hiển thị tối thiểu:

Order
Order ID.
Created date.
Status.
Total.
Customer
Tên/email hoặc thông tin cần thiết theo source.
Shop/Vendor

Nếu Order có reference.

Order Items
Product
Quantity
Price
Subtotal
Payment

Chỉ hiển thị nếu Workbook/source cho phép.

Không expose sensitive information.

ID không tồn tại
/admin/orders/{invalid-id}
        ↓
NOT FOUND
        ↓
Admin error handling
DONE

Admin xem được Order Detail chính xác.

TASK 6.5 — ORDER FILTER / SEARCH
🎯 Mục tiêu

Chỉ triển khai nếu Workbook/Gate 0 yêu cầu.

AI phải kiểm tra trước:

Gate 0
   ↓
Workbook
   ↓
Order Contract

Nếu có yêu cầu:

Có thể hỗ trợ các filter thực sự cần thiết, ví dụ:

Status.
Customer.
Shop.
Date.

Không tự thêm hệ thống search/filter phức tạp.

Nếu không được giao:

[SKIPPED]
DONE

Nếu được giao:

Filter/Search trả đúng dữ liệu.

Nếu không:

SKIPPED — OUTSIDE WORKBOOK
TASK 6.6 — ORDER READ-ONLY GUARANTEE
🎯 Mục tiêu

Đảm bảo Admin không vô tình có quyền thay đổi Order nếu Phase 6 chỉ là quản lý theo dõi.

Kiểm tra:

Admin
  ↓
Order Detail
  ↓
READ ONLY

Không có action:

Edit
Delete
Change Status
Refund
Cancel

trừ khi Contract đã cho phép.

AI phải kiểm tra
Controller endpoint.
HTTP method.
Service.
Template.
Button/action.
Repository.
DONE

Admin chỉ thực hiện đúng các thao tác được Contract cho phép.

TASK 6.7 — ORDER BUSINESS DATA INTEGRITY
🎯 Mục tiêu

Đảm bảo Admin chỉ đọc dữ liệu Order, không làm thay đổi dữ liệu gốc.

Kiểm tra:

Order
   ↓
Customer
   ↓
Shop
   ↓
Product

AI phải xác định reference thực tế.

Không tự sửa schema.

Không tự cascade update.

Không tự đồng bộ Product/Vendor/Customer.

Quan trọng

Nếu Product đã bị thay đổi sau khi Order tạo:

AI phải xác định Order đang sử dụng:

snapshot

hay:

live Product reference

Không tự thay đổi behavior.

Nếu cần thay đổi:

[DEPENDENCY]
DONE

Order Detail phản ánh đúng dữ liệu hiện tại theo model/contract.

TASK 6.8 — ORDER SECURITY TEST
🎯 Mục tiêu

Đảm bảo chỉ ADMIN được truy cập Admin Order.

Test:

ADMIN
 ↓
/admin/orders
 ↓
ALLOW
ADMIN
 ↓
/admin/orders/{id}
 ↓
ALLOW
CUSTOMER
 ↓
/admin/orders
 ↓
403
VENDOR
 ↓
/admin/orders
 ↓
403
ANONYMOUS
 ↓
/admin/orders
 ↓
DENIED / LOGIN

Phải kiểm tra endpoint thực tế.

Không chỉ kiểm tra UI.

TASK 6.9 — ORDER AUTOMATED TEST
🎯 Mục tiêu

Có automated test cho nghiệp vụ Admin Order.

List
[ ] Admin can access
[ ] Has orders
[ ] Empty orders
[ ] Customer denied
[ ] Vendor denied
[ ] Anonymous denied
Detail
[ ] Valid ID
[ ] Not Found
[ ] Admin authorization
[ ] Customer denied
[ ] Vendor denied
Data
[ ] Customer information
[ ] Shop/Vendor information
[ ] Order items
[ ] Total
[ ] Status
Filter/Search

Chỉ test nếu Task 6.5 được triển khai.

Không dùng:

BUILD SUCCESS

làm bằng chứng duy nhất.

Nếu Integration Test cần MongoDB:

→ sử dụng Test DB Strategy đã được xác nhận.
TASK 6.10 — RUNTIME + MONGODB TEST
🎯 Mục tiêu

Kiểm tra chức năng trên application thực tế.

Chạy:

mvn test

sau đó:

mvn spring-boot:run

Kiểm tra:

/admin/orders
Flow
Admin
 ↓
Order List
 ↓
Order Detail

Kiểm tra:

Order có dữ liệu.
Empty state.
Detail.
Invalid ID.
Customer information.
Shop/Vendor information.
Order Items.
Status.
Total.

Kiểm tra trực tiếp MongoDB.

Không chỉ nhìn UI.

TASK 6.11 — ORDER UI CONSISTENCY
🎯 Mục tiêu

Đảm bảo Order Management sử dụng Admin Shell đã hoàn thành ở Phase 2.

Kiểm tra:

Header.
Sidebar.
Table.
Detail.
Button.
Flash message.
Error.
Navigation.
Responsive cơ bản.

Không redesign toàn bộ Admin UI.

Không sửa Customer/Vendor UI nếu không bắt buộc.

DONE

Order UI nhất quán với Admin Shell.

TASK 6.12 — REGRESSION TEST

Sau khi hoàn thành Order phải kiểm tra:

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

Đặc biệt:

Admin Order
      ↓
READ
      ↓
Không làm hỏng Order workflow

Không được làm hỏng:

Customer Order.
Vendor Order.
Product.
Shop.
Customer.
Vendor.
Login.
MongoDB.
Admin Security.

Nếu phát hiện lỗi ngoài phạm vi:

[OUT OF SCOPE]

hoặc:

[DEPENDENCY]

Không tự sửa nếu không thuộc Phase 6.

TASK 6.13 — PHASE 6 REVIEW

Trước khi kết thúc Phase 6, AI phải báo cáo:

[COMPLETED]

[FILES CHANGED]

[ORDER CONTRACT]

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

và:

DEPENDENCY — NGƯỜI KHÁC

Không tự chuyển sang Phase 7.

Chờ Hoàn xác nhận:

PHASE 6 DONE
📋 TỔNG KẾT PHASE 6
PHASE 6 — ADMIN ORDER MANAGEMENT

│
├── TASK 6.1
│   └── Audit Order
│
├── TASK 6.2
│   └── Chốt Order Admin Contract
│
├── TASK 6.3
│   └── Admin Order List
│
├── TASK 6.4
│   └── Order Detail
│
├── TASK 6.5
│   └── Filter / Search
│       └── Chỉ nếu Workbook yêu cầu
│
├── TASK 6.6
│   └── Order Read-only Guarantee
│
├── TASK 6.7
│   └── Order Data Integrity
│
├── TASK 6.8
│   └── Security Test
│
├── TASK 6.9
│   └── Automated Test
│
├── TASK 6.10
│   └── Runtime + MongoDB
│
├── TASK 6.11
│   └── UI Consistency
│
├── TASK 6.12
│   └── Regression Test
│
└── TASK 6.13
    └── Phase Review