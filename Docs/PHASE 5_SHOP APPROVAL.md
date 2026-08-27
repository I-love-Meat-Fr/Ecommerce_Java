# PHASE 5 — SHOP APPROVAL

## 🎯 Mục tiêu Phase 5

Hoàn thiện chức năng Admin quản lý Shop trong đúng phạm vi BTL:

ADMIN
 ↓
Shop Management
 ↓
├── Xem danh sách Shop chờ duyệt
├── Xem thông tin Shop
├── Duyệt Shop
├── Từ chối Shop
└── Quản lý trạng thái Shop
    (chỉ khi Gate 0 đã chốt)

Phase này chỉ bắt đầu khi các Shop Contract/Dependency cần thiết trong Gate 0 đã ở trạng thái DECIDED.

Không tự mở rộng sang Vendor Management.

---

## ⚠️ PHẠM VI

Phase này CHỈ làm:

- Pending Shop List.
- Shop Detail.
- Approve Shop.
- Reject Shop.
- Active/Inactive nếu Gate 0 đã xác định.
- Kiểm tra ảnh hưởng tới Vendor behavior.

Không tự làm:

- Vendor CRUD.
- Product Management.
- Vendor Order Management.
- Customer Management.
- Authentication/JWT.
- Thiết kế lại Vendor workflow.
- Tạo thêm trạng thái Shop nếu chưa có contract.
- Tự thêm `REJECTED`, `APPROVED`, `PENDING` hoặc enum mới nếu source/team chưa xác nhận.

Nếu phát hiện nghiệp vụ chưa được Gate 0 chốt:

[DEPENDENCY]

→ DỪNG task liên quan và báo Hoàn.

---

# TASK 5.1 — AUDIT SHOP HIỆN TẠI

## 🎯 Mục tiêu

Hiểu chính xác Shop hiện tại đang được lưu trữ và xử lý như thế nào trước khi code.

## AI cần kiểm tra

Tìm trong source thật:

- Shop Controller.
- Shop Service.
- Shop Repository.
- Shop Document/Model.
- Vendor Document/Model.
- User Document/Model.
- DTO liên quan Shop/Vendor.
- Template Shop/Vendor.
- CSS/JS liên quan.
- SecurityConfig.
- MongoDB mapping.
- Các trạng thái Shop hiện tại.

AI phải xác định:

Shop
├── id
├── vendor/user reference
├── name
├── status
├── verified
├── active
└── các field thực tế khác

Không được tự bịa field.

Nếu chưa chắc file nào:

[NEED AUDIT]

## Không code

Chỉ audit và báo cáo:

[CURRENT]

[PROBLEM]

[DEPENDENCY]

[PROPOSED CHANGE]

[OUT OF SCOPE]

## DONE

Hoàn hiểu:

Shop hiện tại
↓
Controller
↓
Service/Repository
↓
MongoDB
↓
Template
↓
Vendor behavior

---

# TASK 5.2 — PENDING SHOP LIST

## 🎯 Mục tiêu

Admin có thể xem danh sách Shop đang chờ xử lý/duyệt theo đúng Shop Contract.

## Backend

AI phải kiểm tra trạng thái Pending thực tế trong source.

Không tự tạo enum/state mới.

Nếu repository hiện tại chưa có query phù hợp:

Chỉ thêm query cần thiết cho Shop Approval.

Không refactor Repository toàn project.

## UI

Hiển thị tối thiểu thông tin cần thiết để Admin nhận biết Shop:

- Shop name.
- Vendor/User liên quan nếu source có.
- Status.
- Thông tin cần thiết khác theo source/workbook.

Có action:

[View Detail]

## DONE

Admin truy cập được Pending Shop List.

Dữ liệu lấy từ MongoDB thật.

Không hiển thị sai trạng thái.

---

# TASK 5.3 — SHOP DETAIL

## 🎯 Mục tiêu

Admin có thể xem thông tin chi tiết Shop trước khi quyết định Approve/Reject.

## Backend

Tìm Shop theo ID.

Nếu không tồn tại:

→ xử lý Not Found đúng cơ chế Admin Shell.

Không trả về dữ liệu nhạy cảm của User/Vendor nếu không cần thiết.

## UI

Hiển thị các thông tin Shop cần thiết theo workbook/source.

Có action:

[Approve]

[Reject]

nếu trạng thái hiện tại cho phép.

## DONE

Admin có thể:

Pending Shop List
↓
View Detail
↓
Xem đúng Shop

ID không tồn tại → lỗi rõ ràng.

---

# TASK 5.4 — APPROVE SHOP

## 🎯 Mục tiêu

Admin có thể duyệt Shop theo Shop Contract đã được Gate 0 xác nhận.

## Backend

Flow:

Admin
 ↓
Approve Shop
 ↓
Authorization
 ↓
Validate current state
 ↓
Update Shop
 ↓
MongoDB

Chỉ cho phép transition hợp lệ.

Không tự quyết định state mới.

Ví dụ:

PENDING
 ↓
APPROVED

chỉ được sử dụng nếu Gate 0/source đã xác định state này.

Nếu source sử dụng:

verified = true

thì phải dùng đúng model hiện tại.

Không tự thêm enum.

## DONE

Approve thành công:

- Shop được cập nhật đúng trạng thái.
- MongoDB chứa dữ liệu đúng.
- Pending List không còn hiển thị Shop đó nếu contract yêu cầu.
- UI hiển thị success message.

Test:

- Approve Shop hợp lệ.
- Shop không tồn tại.
- Shop đã ở trạng thái khác.
- User không có quyền Admin.

---

# TASK 5.5 — REJECT SHOP

## 🎯 Mục tiêu

Admin có thể từ chối Shop theo Shop Contract.

## Quan trọng

Không tự tạo:

`REJECTED`

nếu Gate 0/source chưa xác nhận.

Nếu hệ thống hiện tại chưa có cách biểu diễn Reject:

[DEPENDENCY]

→ Dừng phần implementation và báo Hoàn.

## Backend

Flow:

Admin
 ↓
Reject Shop
 ↓
Authorization
 ↓
Validate current state
 ↓
Update Shop
 ↓
MongoDB

Chỉ transition đúng theo contract.

## UI

Có action Reject.

Nếu project/workbook yêu cầu lý do Reject và contract đã xác định:

→ xử lý theo contract.

Nếu chưa:

→ Không tự thêm Reject Reason.

## DONE

Reject chỉ thực hiện được khi state transition hợp lệ.

MongoDB được cập nhật chính xác.

Không tạo state mới ngoài contract.

---

# TASK 5.6 — ACTIVE / INACTIVE

## 🎯 Mục tiêu

Quản lý trạng thái Active/Inactive của Shop CHỈ khi Gate 0 đã xác định chức năng này.

## Trước khi code

AI phải kiểm tra:

Gate 0
↓
Shop Contract
↓
Active/Inactive được quyết định?

Nếu:

DECIDED
→ thực hiện.

Nếu:

OPTIONAL/SKIPPED
→ không làm.

Nếu:

BLOCKED
→ [DEPENDENCY] và STOP.

## Không tự quyết định

Không tự suy luận:

active = false

có nghĩa Vendor bị khóa.

Phải xác định behavior thực tế.

## DONE

Nếu chức năng được phép làm:

- Active hoạt động đúng.
- Inactive hoạt động đúng.
- Không ảnh hưởng sai sang Vendor/Product.

---

# TASK 5.7 — VENDOR BEHAVIOR VERIFICATION

## 🎯 Mục tiêu

Xác nhận trạng thái Shop sau Admin Approval có ảnh hưởng đúng tới Vendor workflow.

## Quan trọng

Đây là VERIFICATION, không phải Vendor Management.

AI cần kiểm tra:

Shop
 ↓
Vendor
 ↓
Product / Vendor functionality

Ví dụ:

Shop chưa được duyệt
→ Vendor behavior thế nào?

Shop được duyệt
→ Vendor behavior thế nào?

Shop bị reject
→ Vendor behavior thế nào?

Shop inactive
→ Vendor behavior thế nào?

Chỉ kiểm tra behavior đã tồn tại trong source/contract.

Không tự sửa Vendor code.

Nếu phát hiện Vendor behavior sai:

[DEPENDENCY]

- Vấn đề:
- File liên quan:
- Owner: Mạnh Quân
- Hoàn có cần sửa không:
- Có thể tiếp tục Phase 5 không:

---

# TASK 5.8 — SHOP APPROVAL SECURITY TEST

## 🎯 Mục tiêu

Đảm bảo chỉ ADMIN được thao tác Shop Approval.

Test:

ADMIN
→ Pending Shop
→ OK

ADMIN
→ Shop Detail
→ OK

ADMIN
→ Approve
→ OK

ADMIN
→ Reject
→ OK

CUSTOMER
→ /admin/shops
→ 403

VENDOR
→ /admin/shops
→ 403

CUSTOMER
→ Approve endpoint
→ 403

VENDOR
→ Approve endpoint
→ 403

Không được chỉ bảo vệ UI.

Phải kiểm tra endpoint thực tế.

---

# TASK 5.9 — SHOP AUTOMATED TEST

## 🎯 Mục tiêu

Có automated test cho nghiệp vụ Shop Approval.

Tùy kiến trúc source:

### List

- Pending Shop List.
- Empty list.
- Authorization.

### Detail

- Valid ID.
- Not Found.
- Authorization.

### Approve

- Valid transition.
- Invalid transition.
- Not Found.
- Authorization.

### Reject

- Valid transition.
- Invalid transition.
- Not Found.
- Authorization.

### Active/Inactive

Chỉ test nếu Gate 0 cho phép.

Không dùng:

`BUILD SUCCESS`

làm bằng chứng duy nhất.

Nếu cần MongoDB Integration Test:

→ sử dụng Test DB strategy đã được Gate 0 xác nhận.

---

# TASK 5.10 — RUNTIME + MONGODB TEST

## 🎯 Mục tiêu

Kiểm tra chức năng thực tế trên application đang chạy.

Chạy:

mvn test

sau đó:

mvn spring-boot:run

Kiểm tra:

/admin/shops

Flow:

Pending Shop
 ↓
Detail
 ↓
Approve
 ↓
MongoDB verification

và:

Pending Shop
 ↓
Detail
 ↓
Reject
 ↓
MongoDB verification

Nếu Active/Inactive được phép:

→ test thêm flow đó.

Phải kiểm tra trực tiếp MongoDB:

- Shop trước action.
- Shop sau action.
- State/field thay đổi đúng.

Không chỉ nhìn UI.

---

# TASK 5.11 — UI CONSISTENCY

## 🎯 Mục tiêu

Đảm bảo Shop Management sử dụng Admin Shell đã hoàn thành ở Phase 2.

Kiểm tra:

- Header.
- Sidebar.
- Flash message.
- Table.
- Detail.
- Button.
- Error.
- Navigation.
- Responsive cơ bản.

Không redesign toàn bộ Admin UI.

Không sửa Vendor UI nếu không bắt buộc.

## DONE

Shop UI nhất quán với Admin Shell.

---

# TASK 5.12 — REGRESSION TEST

Sau khi hoàn thành Shop Approval phải kiểm tra:

- Login.
- Register.
- Home.
- Product.
- Customer.
- Vendor.
- Admin Shell.
- User Management.
- Category Management.

Đặc biệt:

Approve/Reject Shop

không được làm hỏng:

- Vendor login.
- Vendor access.
- Product flow.
- Customer flow.
- Admin Security.
- MongoDB data.

Nếu phát hiện lỗi ngoài phạm vi:

[OUT OF SCOPE]

hoặc:

[DEPENDENCY]

Không tự sửa nếu không thuộc Phase 5.

---

# TASK 5.13 — PHASE 5 REVIEW

Trước khi kết thúc Phase 5, AI phải báo cáo:

[COMPLETED]

[FILES CHANGED]

[SHOP STATE CHANGES]

[BACKEND CHANGES]

[FRONTEND CHANGES]

[SECURITY CHANGES]

[TESTS]

[RUNTIME RESULT]

[MONGODB RESULT]

[DEPENDENCIES]

[NOT DONE]

[OUT OF SCOPE]

Không tự chuyển sang Phase 6.

Chờ Hoàn xác nhận:

`PHASE 5 DONE`

---

# 📌 TỔNG KẾT PHASE 5

PHASE 5 — SHOP APPROVAL

│
├── TASK 5.1
│   └── Audit Shop
│
├── TASK 5.2
│   └── Pending Shop List
│
├── TASK 5.3
│   └── Shop Detail
│
├── TASK 5.4
│   └── Approve Shop
│
├── TASK 5.5
│   └── Reject Shop
│
├── TASK 5.6
│   └── Active / Inactive
│
├── TASK 5.7
│   └── Vendor Behavior Verification
│
├── TASK 5.8
│   └── Security Test
│
├── TASK 5.9
│   └── Automated Test
│
├── TASK 5.10
│   └── Runtime + MongoDB
│
├── TASK 5.11
│   └── UI Consistency
│
├── TASK 5.12
│   └── Regression Test
│
└── TASK 5.13
    └── Phase Review