# CNJ70 Ecommerce

## 1. Giới thiệu

- **Tên project**: CNJ70 Ecommerce (`com.ecommerce.cnj70`)
- **Mục đích**: Sàn thương mại điện tử đa nhà bán, hỗ trợ 4 vai trò Customer / Vendor / Moderator / Admin trên cùng một ứng dụng web.
- **Công nghệ chính**: Spring Boot 3.2.0, Spring Security 6, Spring Data MongoDB, Thymeleaf, MongoDB.

## 2. Chức năng chính

### Customer
- Đăng ký, đăng nhập, đăng xuất.
- Xem và tìm kiếm sản phẩm, xem chi tiết sản phẩm.
- Xem shop công khai, xem kho voucher WEB.
- Quản lý giỏ hàng (thêm / sửa / xóa), áp dụng voucher khi thanh toán.
- Checkout, đặt hàng, xem lịch sử đơn hàng.
- Tạo, sửa, xóa, báo cáo review sản phẩm.
- Tạo khiếu nại / yêu cầu trả hàng / hoàn tiền.

### Vendor
- Dashboard nhà bán.
- Tạo và cập nhật shop, upload logo / banner.
- CRUD sản phẩm (kèm thông số kỹ thuật, biến thể, upload ảnh).
- Quản lý đơn hàng của shop, cập nhật trạng thái và vận chuyển.
- Quản lý voucher SHOP.
- Hồ sơ nhà bán, KYC, xử lý khiếu nại liên quan shop.

### Moderator
- Dashboard moderation.
- Hàng đợi kiểm duyệt (`/moderator/queue`), duyệt / từ chối / leo thang sản phẩm.
- Duyệt review (`/moderator/reviews`).
- Duyệt shop (`/moderator/shops`).
- Duyệt KYC (`/moderator/kyc`).
- Xử lý khiếu nại (`/moderator/complaints`) và report case (`/moderator/cases`).
- Lịch sử kiểm duyệt (`/moderator/history`), xem vi phạm (`/moderator/violations`).

### Admin
- Dashboard quản trị, lọc theo khoảng thời gian.
- Quản lý người dùng: khóa / mở khóa tài khoản.
- Quản lý shop: duyệt / kích hoạt / vô hiệu hóa / từ chối.
- Quản lý danh mục (CRUD, lọc active).
- Kiểm duyệt sản phẩm (hide / unhide / delete).
- Xem đơn hàng (read-only) theo `AdminOrderController`.
- Quản lý review, banner, voucher WEB.
- Quản lý KYC, vi phạm, leo thang (`escalations`).
- Audit log.

## 3. Công nghệ sử dụng

| Technology | Version | Vai trò |
|---|---|---|
| Java | 17 | Ngôn ngữ chính |
| Spring Boot | 3.2.0 | Framework ứng dụng |
| Spring Security | 6 (theo starter) | Xác thực, phân quyền, JWT filter |
| Spring Data MongoDB | theo starter | Truy cập MongoDB |
| MongoDB | 6.0+ | Cơ sở dữ liệu |
| Thymeleaf | theo starter | Template engine phía server |
| Thymeleaf Spring Security Extras | theo starter | Hỗ trợ `${#authentication...}` |
| Lombok | 1.18.46 | Giảm boilerplate |
| JJWT | 0.12.3 | Ký / xác thực JWT |
| Dotenv Java | 3.0.2 | Nạp biến môi trường từ `.env` |
| Maven | 3.6+ | Build & dependency |

## 4. Yêu cầu môi trường

- **JDK** 17+
- **Maven** 3.6+
- **MongoDB** 6.0+ (cục bộ hoặc MongoDB Atlas)
- File cấu hình `.env` ở thư mục gốc dự án (chứa `JWT_SECRET`, `ENCRYPTION_KEY`, có thể kèm `MONGODB_URI`, `MONGODB_DATABASE`).

## 5. Cài đặt và chạy

### 5.1 Chuẩn bị MongoDB

Mặc định ứng dụng kết nối đến MongoDB Atlas đã khai báo trong `src/main/resources/application.yml`. Để dùng MongoDB cục bộ, đặt biến môi trường trong `.env`:

```env
MONGODB_URI=mongodb://localhost:27017
MONGODB_DATABASE=cnj70_ecommerce
```

### 5.2 Tạo file `.env`

Sao chép `.env.example` thành `.env` ở thư mục gốc dự án và bổ sung các giá trị cần thiết (đặc biệt `JWT_SECRET` ≥ 256-bit và `ENCRYPTION_KEY` Base64 32 byte).

### 5.3 Build & Run

```bash
mvn clean install
mvn spring-boot:run
```

Khi chạy thành công, ứng dụng lắng nghe tại port **8081**.

### 5.4 URL truy cập

| Mục | URL |
|---|---|
| Trang chủ | `http://localhost:8081/` |
| Đăng nhập | `http://localhost:8081/auth/login` |
| Đăng ký | `http://localhost:8081/auth/register` |

## 6. Tài khoản mẫu / Demo Accounts

Tài khoản ADMIN và MODERATOR được seed tự động bởi `AdminBootstrap` và `ModeratorBootstrap` — **chỉ khi chạy với profile tương ứng**. Hai tài khoản này **chỉ dùng để test/demo, không dùng cho môi trường production**.

Cách kích hoạt seed (chạy 1 trong 2):

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=admin-bootstrap
mvn spring-boot:run -Dspring-boot.run.profiles=moderator-bootstrap
```

| Role | Email | Password | Mục đích |
|---|---|---|---|
| ADMIN | `admin2@gmail.com` | `admin123` | Test chức năng quản trị |
| MODERATOR | `moderator2@gmail.com` | `moderator123` | Test chức năng kiểm duyệt |

Tài khoản VENDOR và CUSTOMER **không có seeder trong source**. Cần tự đăng ký qua `/auth/register`, sau đó dùng AdminBootstrap (với system property `-Dadmin-bootstrap.lock-email=...`) hoặc seed tay trong MongoDB để nâng role / đổi trạng thái nếu cần test luồng đặc biệt.

> Lưu ý: `AdminBootstrap` chỉ reset mật khẩu cho user `admin2@gmail.com` đã tồn tại trong DB; nếu collection rỗng, tài khoản sẽ không được tạo.

## 7. Hướng dẫn sử dụng nhanh

### Customer
1. Đăng nhập tại `/auth/login`.
2. Duyệt sản phẩm ở `/products`, xem chi tiết ở `/products/{id}`.
3. Thêm sản phẩm vào giỏ (`/cart`).
4. Checkout tại `/checkout`, áp dụng voucher qua `POST /checkout/apply-voucher`, đặt hàng qua `POST /checkout/place-order`.
5. Theo dõi đơn ở `/orders`, đánh giá sản phẩm ở `/products/{id}/reviews`.

### Vendor
1. Đăng nhập bằng tài khoản role `VENDOR`.
2. Tạo shop ở `/vendor/shop/create`.
3. Quản lý sản phẩm ở `/vendor/products`, tạo sản phẩm mới ở `/vendor/products/create`.
4. Xem và cập nhật đơn hàng của shop ở `/vendor/orders`.
5. Quản lý voucher SHOP ở `/vendor/vouchers`.

### Moderator
1. Đăng nhập bằng tài khoản role `MODERATOR`.
2. Mở hàng đợi tại `/moderator/queue`.
3. Duyệt sản phẩm `/moderator/products/{id}`, duyệt review `/moderator/reviews`, duyệt shop `/moderator/shops`, duyệt KYC `/moderator/kyc`.
4. Xử lý khiếu nại `/moderator/complaints`, leo thang report case `/moderator/cases`.
5. Xem lịch sử kiểm duyệt `/moderator/history`.

### Admin
1. Đăng nhập bằng tài khoản role `ADMIN`.
2. Dashboard tại `/admin/dashboard`.
3. Quản lý người dùng `/admin/users`, shop `/admin/shops`, danh mục `/admin/categories`, sản phẩm `/admin/products`, voucher WEB `/admin/vouchers`, banner `/admin/banners`, review `/admin/reviews`, KYC `/admin/kyc`, vi phạm `/admin/violations`, leo thang `/admin/escalations`, audit `/admin/audit`.

## 8. Luồng sử dụng nhanh

```
Login -> Module -> Thao tác -> Kết quả
```

Ví dụ:

```
Login (Admin) -> /admin/users -> Search / Lock / Unlock -> Trạng thái tài khoản cập nhật
Login (Vendor) -> /vendor/products -> Create / Edit / Delete -> Sản phẩm được lưu
Login (Customer) -> /products -> Add to cart -> /checkout -> Place order -> Order lưu trong DB
```

## 9. Cấu trúc project

```
cnj70-ecommerce
├── pom.xml
├── src/main/java/com/ecommerce/cnj70/
│   ├── Cnj70EcommerceApplication.java
│   ├── config/           # SecurityConfig, MongoConfig, WebMvcConfig, bootstrap helpers
│   ├── controller/       # web/, auth/, admin/, vendor/, moderator/, api/
│   ├── document/         # MongoDB @Document classes (User, Shop, Product, ...)
│   ├── dto/              # dto.request / dto.response
│   ├── enums/            # UserRole, AccountStatus, OrderStatus, ...
│   ├── exception/        # BadRequestException, GlobalExceptionHandler, ...
│   ├── interceptor/      # CartCountInterceptor, ...
│   ├── repository/       # MongoRepository interfaces + custom impl
│   ├── security/         # JwtAuthenticationFilter, JwtUtils, CustomUserDetails
│   ├── service/          # Service interfaces
│   └── service/impl/     # Service implementations
├── src/main/resources/
│   ├── application.yml
│   ├── application-test.yml
│   ├── static/           # CSS, JS, images
│   └── templates/        # Thymeleaf templates (admin/, vendor/, moderator/, customer, ...)
└── docs/                 # Báo cáo, sơ đồ kiến trúc
```

## 10. Kiểm tra hệ thống

### Build

```bash
mvn clean install
```

### Test

```bash
mvn test
```

### Khởi chạy

```bash
mvn spring-boot:run
```

### Xác nhận chạy thành công

1. Mở `http://localhost:8081/` — trang chủ render (redirect về `/home`).
2. Truy cập `http://localhost:8081/auth/login` — form đăng nhập hiển thị.
3. Trong log console xuất hiện `Started Cnj70EcommerceApplication ...` và không có stack trace lỗi kết nối MongoDB.

## 11. Lưu ý

- Cổng chạy mặc định là **8081** (xem `server.port` trong `application.yml`).
- Biến môi trường ưu tiên cao hơn giá trị mặc định trong `application.yml` (xem `${MONGODB_URI:...}`, `${MONGODB_DATABASE:...}`).
- `application.yml` mặc định dùng MongoDB Atlas; chuyển sang MongoDB cục bộ bằng cách đặt `MONGODB_URI` trong `.env`.
- File `.env` chứa thông tin nhạy cảm (`JWT_SECRET`, `ENCRYPTION_KEY`) — **không commit** vào git.
- `AdminBootstrap` và `ModeratorBootstrap` chỉ chạy khi bật profile tương ứng; **tắt profile này ở môi trường production**.
- Truy cập các URL quản trị (`/admin/**`, `/vendor/**`, `/moderator/**`, `/api/**` trừ `/api/auth/**`, `/api/products/**`, `/api/categories/**`) yêu cầu đăng nhập với đúng vai trò — theo `SecurityConfig`.
