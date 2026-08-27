PHASE 4 — CATEGORY MANAGEMENT
🎯 Mục tiêu Phase 4

Hoàn thiện chức năng Category Management của Admin theo đúng Category Contract đã được chốt tại Gate 0.

Luồng:

ADMIN
   ↓
/admin/categories
   ↓
┌─────────────────────────────────────────┐
│          CATEGORY MANAGEMENT             │
├─────────────────────────────────────────┤
│ Category List                            │
│ Create Category                          │
│ Edit Category                            │
│ Delete Category                          │
└─────────────────────────────────────────┘

Admin phải có thể:

Xem danh sách Category.
Tạo Category.
Sửa Category.
Xóa Category an toàn.
Nhận thông báo lỗi/thành công rõ ràng.
Dữ liệu được cập nhật chính xác trong MongoDB.
Category UI sử dụng Admin Shell của Phase 2.
Có automated test.
Có runtime test.
Có MongoDB verification.
Không làm hỏng Product/Vendor/Customer/Admin User Management.
⚠️ QUY TẮC QUAN TRỌNG

Gate 0 là nguồn quyết định nghiệp vụ.

AI không được tự quyết định:

Category active
Category hierarchy
parentId
Rename Product.categoryName
Delete Product khi xóa Category
Cascade delete
Category state mới

Nếu một decision chưa được chốt:

[DEPENDENCY — GATE 0]

Nếu rename cần thay đổi Product:

[DEPENDENCY — MẠNH QUÂN]

Không tự sửa Product.

🚫 KHÔNG LÀM TRONG PHASE 4

Không làm:

Product Management.
Product CRUD.
Vendor Management.
Shop Management.
Order Management.
Customer Management.
Review Management.
Thay đổi Product schema nếu không bắt buộc.
Đồng bộ Product.categoryName khi rename nếu chưa được Mạnh Quân xác nhận.
Cascade delete Product.
Category hierarchy nếu Gate 0 = SKIPPED.
Category Active/Inactive nếu Gate 0 = SKIPPED.
Redesign toàn bộ Admin UI.
Refactor toàn bộ project.
Tạo Service mới nếu Service hiện tại đã tồn tại.
Tạo Repository mới nếu Repository hiện tại đã đủ.
TASK 4.1 — AUDIT CATEGORY MANAGEMENT HIỆN TẠI
🎯 Mục tiêu

Đọc source hiện tại để xác định chính xác Category Management đang triển khai tới đâu.

AI phải kiểm tra
AdminCategoryController
AdminCategoryService
AdminCategoryServiceImpl
CategoryRepository
Category Document/Model.
Product Document/Model.
Product Repository.
Category DTO/Form nếu có.
category-manage.html
Các Category template liên quan.
CSS/JS liên quan.
SecurityConfig
MongoDB mapping.
Global Exception Handler.
Admin Shell.
AI phải xác định
/admin/categories hiện có chưa?
Controller đang gọi Repository trực tiếp ở đâu?
AdminCategoryService đã tồn tại chưa?
AdminCategoryServiceImpl đã có logic nào?
List đã hoạt động chưa?
Create đã hoạt động chưa?
Edit đã có backend chưa?
Edit UI đã có chưa?
Delete đã hoạt động chưa?
Duplicate check hiện tại thế nào?
name == null hiện xử lý thế nào?
Database duplicate exception hiện được xử lý thế nào?
Product reference được kiểm tra thế nào?
categoryId và categoryName của Product đang được sử dụng thế nào?
active có thực sự được sử dụng không?
parentId có thực sự được sử dụng không?
Category Service hiện đã có Product-reference guard chưa?
Template hiện có Flash Message chưa?
Admin Shell đã được sử dụng chưa?
Quan trọng

Source hiện đã có:

AdminCategoryServiceImpl

và Service đã có một phần logic:

trim
duplicate
edit
Product-reference guard

AI không được viết lại những logic này từ đầu.

Phải:

Audit
 ↓
Tận dụng code hiện tại
 ↓
Sửa phần thiếu
 ↓
Controller → Service
Không code

Task 4.1 chỉ audit.

DONE

Báo cáo:

[CURRENT]

[PROBLEM]

[FILES INVOLVED]

[EXISTING SERVICE]

[EXISTING CONTROLLER]

[EXISTING REPOSITORY]

[EXISTING BUSINESS LOGIC]

[PRODUCT DEPENDENCY]

[GATE 0 DEPENDENCY]

[PROPOSED CHANGE]

[OUT OF SCOPE]
TASK 4.2 — XÁC NHẬN CATEGORY CONTRACT
🎯 Mục tiêu

Đảm bảo implementation chỉ sử dụng những decision đã được Gate 0 chốt.

AI phải kiểm tra
Category Name
null
empty
whitespace
trim
duplicate
case sensitivity

Ví dụ:

"Phone"
" Phone "
"phone"

Phải áp dụng đúng contract đã chốt.

Description

Xác định:

Required hay optional.
Empty có được phép không.
Edit

Xác định:

Field nào được sửa.
Field nào không được sửa.
Rename

Đặc biệt kiểm tra:

Category
    ↓
name = Phone
    ↓
rename = Smartphone

Product có:

categoryId
categoryName

Nếu Product đang lưu:

categoryName = "Phone"

thì phải xác định:

Rename Category
       ↓
Product.categoryName
       ↓
Có update hay không?

Nếu chưa được Mạnh Quân xác nhận:

[DEPENDENCY — MẠNH QUÂN]

Không sửa Product.

Active

Nếu Gate 0:

Category Active Management = SKIPPED

→ không tạo UI/logic Active.

Hierarchy

Nếu Gate 0:

Category Hierarchy = SKIPPED

→ không xử lý:

parentId
child category
category tree
cycle detection
DONE

Category Contract đủ rõ để implementation mà không phải tự suy đoán.

TASK 4.3 — CONNECT CATEGORY SERVICE
🎯 Mục tiêu

Sử dụng Service hiện có thay vì để Controller gọi Repository trực tiếp.

Kiến trúc mục tiêu:

AdminCategoryController
          ↓
AdminCategoryService
          ↓
AdminCategoryServiceImpl
          ↓
CategoryRepository
          ↓
MongoDB
Quan trọng

Không tạo Service mới.

Nếu AdminCategoryService và AdminCategoryServiceImpl đã tồn tại:

→ sử dụng chúng.

AI phải:

Chuyển business logic phù hợp khỏi Controller.
Controller không gọi CategoryRepository trực tiếp nếu logic đó thuộc Service.
Tận dụng duplicate check hiện tại.
Tận dụng Product-reference guard hiện tại.
Không refactor các module khác.
DONE

Controller Category sử dụng Service hiện có:

Controller
   ↓
Service
   ↓
Repository
TASK 4.4 — CATEGORY LIST
🎯 Mục tiêu

Hoàn thiện:

GET /admin/categories
Backend
AdminCategoryController
        ↓
AdminCategoryService
        ↓
CategoryRepository
        ↓
MongoDB
UI

Hiển thị tối thiểu:

ID nếu cần.
Name.
Description nếu thuộc contract.
Action.

Action:

Add
Edit
Delete

Không hiển thị field ngoài contract.

DONE
/admin/categories hoạt động.
MongoDB trả dữ liệu đúng.
Empty list xử lý đúng.
UI sử dụng Admin Shell.
Không lỗi Thymeleaf.
TASK 4.5 — CREATE CATEGORY
🎯 Mục tiêu

Admin tạo được Category hợp lệ.

Backend

Flow:

Request
 ↓
Validate
 ↓
Normalize name
 ↓
Duplicate check
 ↓
Save
 ↓
MongoDB
Validation bắt buộc

Phải xử lý:

name == null

trước khi gọi:

name.trim()

Không để:

NullPointerException
Phải xử lý
null
→ Error
""
→ Error
"   "
→ Error
duplicate
→ Error
valid
→ Create
Database duplicate

Ngoài duplicate check ở Service, AI phải kiểm tra khả năng:

save()
 ↓
MongoDB duplicate exception

Nếu có unique index/constraint hoặc race condition:

→ phải xử lý exception phù hợp.

Không chỉ dựa vào:

existsByName()
UI

Form:

Name
Description
[Create]

Flash message:

Success
Error
DONE
Create thành công.
Null không crash.
Empty bị reject.
Duplicate bị reject.
Database exception phù hợp được xử lý.
MongoDB đúng.
UI hiển thị kết quả.
TASK 4.6 — EDIT CATEGORY
🎯 Mục tiêu

Hoàn thiện:

GET /admin/categories/{id}/edit
POST /admin/categories/{id}/edit

Chỉ sử dụng route thực tế phù hợp với source. Không tự bắt buộc đúng URL trên nếu project đang dùng route khác.

Backend

Flow:

Category ID
    ↓
Find Category
    ↓
Not Found?
    ↓
Validation
    ↓
Normalize
    ↓
Duplicate Check
    ↓
Update
    ↓
MongoDB
Phải xử lý
ID không tồn tại
→ Not Found
name == null
→ Validation Error
name empty
→ Validation Error
duplicate
→ Validation Error
Duplicate khi Edit

Không được coi chính Category hiện tại là duplicate của chính nó.

Ví dụ:

ID = 1
Name = Phone

Edit:

ID = 1
Name = Phone

→ hợp lệ nếu contract cho phép.

Nhưng:

ID = 1
Name = Laptop

và Category khác đã có:

Name = Laptop

→ reject.

Rename Product

Nếu rename ảnh hưởng:

Product.categoryName

và chưa có contract:

[DEPENDENCY — MẠNH QUÂN]

Không tự update Product.

UI

Phải có:

Edit form
Save
Cancel
Success message
Error message
DONE
Edit backend hoạt động.
Edit UI hoạt động.
Null được xử lý.
Duplicate được xử lý.
Not Found được xử lý.
MongoDB đúng.
Không tự sửa Product.
TASK 4.7 — SAFE DELETE CATEGORY
🎯 Mục tiêu

Không cho phép xóa Category đang được Product sử dụng.

Flow:

Delete Category
       ↓
Category tồn tại?
       ↓
Product đang reference?
       ↓
YES → REJECT
       ↓
NO
       ↓
DELETE
       ↓
MongoDB
Product Reference Guard

Tận dụng logic hiện có trong AdminCategoryServiceImpl.

Nếu Repository đã có method phù hợp:

findByCategoryId(...)

→ sử dụng.

Không tạo lại logic không cần thiết.

Nếu Category đang được sử dụng
REJECT DELETE

Không:

Cascade delete Product

Không sửa Product.

Nếu Category không tồn tại
NOT FOUND
Nếu Category không được sử dụng
DELETE
Hierarchy

Chỉ kiểm tra child Category nếu Gate 0 cho phép hierarchy.

Nếu hierarchy = SKIPPED:

→ không thêm child guard.

DONE

Ba case bắt buộc:

Not Found
→ Error
Used by Product
→ Reject
Not Used
→ Delete

MongoDB được xác minh sau thao tác.

TASK 4.8 — CATEGORY ERROR HANDLING
🎯 Mục tiêu

Category phải có lỗi rõ ràng cho người dùng.

Tối thiểu
Category không tồn tại
Category name null
Category name empty
Category name duplicate
Database duplicate exception
Category đang được Product sử dụng
Tận dụng
Admin Shell
+
Flash Message
+
Global Error Page

Không tạo Exception System mới nếu hệ thống hiện tại đã đủ.

DONE

Lỗi được:

xử lý.
log phù hợp nếu cần.
hiển thị rõ trên UI.
không làm ứng dụng crash.
TASK 4.9 — CATEGORY SECURITY
🎯 Mục tiêu

Chỉ ADMIN được sử dụng Category Management.

Kiểm tra:

ADMIN
 ↓
/admin/categories
 ↓
ALLOW
CUSTOMER
 ↓
/admin/categories
 ↓
403
VENDOR
 ↓
/admin/categories
 ↓
403

Phải kiểm tra cả:

GET list
GET edit
POST create
POST edit
POST delete

Không chỉ kiểm tra sidebar/UI.

Quan trọng

Nếu Phase 1 đã xác nhận /admin/** authorization:

Task này chỉ cần verify Category-specific endpoints.

Không refactor Security.

DONE

Không role nào ngoài ADMIN có thể thực hiện Category Management.

TASK 4.10 — CATEGORY AUTOMATED TEST
🎯 Mục tiêu

Có automated test thực tế cho Category.

List
Admin access.
Có dữ liệu.
Empty list.
Create
Valid.
Null name.
Empty name.
Whitespace.
Duplicate.
Database duplicate exception nếu có thể kiểm thử.
Edit
Valid.
Same existing name.
Duplicate với Category khác.
Null name.
Empty name.
Not Found.
Delete
Valid.
Not Found.
Product reference → Reject.
Security
Admin → Allow.
Customer → 403.
Vendor → 403.
Conditional

Nếu Gate 0 cho phép:

Active
Hierarchy

→ mới test.

Không dùng:

BUILD SUCCESS

làm bằng chứng duy nhất.

TASK 4.11 — MONGODB INTEGRATION TEST
🎯 Mục tiêu

Xác minh nghiệp vụ thực sự thay đổi MongoDB.

Create
Create Category
      ↓
MongoDB
      ↓
Document tồn tại
Edit
Edit Category
      ↓
MongoDB
      ↓
Name/Description đúng
Delete
Delete Category
      ↓
MongoDB
      ↓
Document không còn
Delete Guard
Category đang được Product sử dụng
       ↓
Delete
       ↓
REJECT
       ↓
Category vẫn tồn tại

Nếu Test DB strategy chưa được Gate 0 chốt:

[DEPENDENCY — TEST STRATEGY]

Không tự đổi MongoDB development/production.

TASK 4.12 — CATEGORY UI CONSISTENCY

Kiểm tra:

Header.
Sidebar.
Table.
Form.
Edit form.
Delete action.
Flash message.
Error.
Navigation.
Button.
Status nếu thuộc contract.
Responsive cơ bản.

Đảm bảo sử dụng Admin Shell Phase 2.

Quan trọng

Không xây lại Category UI từ đầu nếu UI hiện tại có thể sửa.

Không redesign toàn bộ Admin.

DONE

Category UI nhất quán với Admin Shell.

TASK 4.13 — RUNTIME TEST

Chạy:

mvn test

Sau đó:

mvn spring-boot:run

Kiểm tra thực tế:

/admin/categories
Test flow
List
 ↓
Create
 ↓
Edit
 ↓
Delete

và:

Duplicate
Null
Empty
Invalid ID
Product Reference
403
MongoDB

Sau mỗi mutation phải verify:

Create → MongoDB
Edit   → MongoDB
Delete → MongoDB
Reject → MongoDB unchanged

Không chỉ kiểm tra UI.

TASK 4.14 — REGRESSION TEST

Sau Category Management phải kiểm tra:

Login.
Register.
Admin Shell.
User Management.
Product.
Vendor.
Customer.
Logout.

Đặc biệt:

Category
   ↓
Product

Phải đảm bảo Product vẫn hoạt động.

Không được làm hỏng
Product.
Vendor.
Customer.
Login.
Admin User Management.
Admin Shell.
MongoDB.
Existing authentication.

Nếu phát hiện lỗi ngoài phạm vi:

[OUT OF SCOPE]

hoặc:

[DEPENDENCY]

Không tự mở rộng Phase 4.

TASK 4.15 — PHASE 4 REVIEW

AI phải báo cáo:

[COMPLETED]

[CATEGORY CONTRACT USED]

[GATE 0 DECISIONS USED]

[FILES CHANGED]

[CONTROLLER]

[SERVICE]

[REPOSITORY]

[MODEL / DOCUMENT]

[DTO / FORM]

[TEMPLATE]

[BUSINESS RULES]

[VALIDATION]

[DUPLICATE HANDLING]

[PRODUCT REFERENCE GUARD]

[PRODUCT RENAME DEPENDENCY]

[SECURITY RESULT]

[AUTOMATED TEST]

[INTEGRATION TEST]

[MONGODB RESULT]

[RUNTIME RESULT]

[REGRESSION RESULT]

[DEPENDENCIES]

[NOT DONE]

[OUT OF SCOPE]

Đặc biệt phải phân biệt:

HOÀN ĐÃ LÀM

với:

DEPENDENCY — MẠNH QUÂN
DEPENDENCY — QUỐC ANH
DEPENDENCY — GATE 0

Không được coi Dependency chưa xử lý là COMPLETED.

Sau báo cáo:

STOP

Không tự chuyển sang Phase 5.

Chờ Hoàn xác nhận:

PHASE 4 DONE
📋 TỔNG KẾT PHASE 4
PHASE 4 — CATEGORY MANAGEMENT
│
├── TASK 4.1
│   └── Audit Category Management
│
├── TASK 4.2
│   └── Confirm Category Contract
│
├── TASK 4.3
│   └── Connect Existing Category Service
│
├── TASK 4.4
│   └── Category List
│
├── TASK 4.5
│   └── Create Category
│       ├── Validation
│       ├── Duplicate
│       └── Database Exception
│
├── TASK 4.6
│   └── Edit Category
│       ├── Validation
│       ├── Duplicate
│       ├── Not Found
│       └── Rename Dependency
│
├── TASK 4.7
│   └── Safe Delete
│       └── Product Reference Guard
│
├── TASK 4.8
│   └── Error Handling
│
├── TASK 4.9
│   └── Security
│
├── TASK 4.10
│   └── Automated Test
│
├── TASK 4.11
│   └── MongoDB Integration Test
│
├── TASK 4.12
│   └── UI Consistency
│
├── TASK 4.13
│   └── Runtime Test
│
├── TASK 4.14
│   └── Regression Test
│
└── TASK 4.15
    └── Phase Review