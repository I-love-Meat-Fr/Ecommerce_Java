# CNJ70 Ecommerce - Sàn Thương Mại Điện Tử Đa Nhà Bán

## Giới thiệu
Đề tài mở: Xây dựng ứng dụng web quản lý sàn thương mại điện tử đa nhà bán sử dụng Spring Boot, Spring Security, Spring Data MongoDB, Thymeleaf và MongoDB.

## Công nghệ sử dụng
- **Backend**: Java 17, Spring Boot 3.2.0
- **Security**: Spring Security 6
- **Database**: MongoDB
- **ORM**: Spring Data MongoDB
- **Frontend**: Thymeleaf, HTML5, CSS3, JavaScript
- **Build Tool**: Maven

## Yêu cầu hệ thống
- JDK 17+
- Maven 3.6+
- MongoDB 6.0+

## Cài đặt

### 1. Cài đặt MongoDB
```bash
# macOS (Homebrew)
brew install mongodb-community@6.0
brew services start mongodb-community@6.0

# Windows - Download từ https://www.mongodb.com/try/download/community
# Chạy MongoDB Service

# Ubuntu/Linux
sudo apt install mongodb-org
sudo systemctl start mongod
```

### 2. Cấu hình MongoDB
Mặc định, ứng dụng kết nối đến MongoDB tại: `mongodb://localhost:27017/cnj70_ecommerce`

### 3. Build và chạy ứng dụng
```bash
# Build project
mvn clean install

# Chạy ứng dụng
mvn spring-boot:run
```

### 4. Truy cập ứng dụng
- URL: http://localhost:8081
- Trang đăng nhập: http://localhost:8081/auth/login

## Tài khoản mặc định
Sau khi chạy ứng dụng, truy cập http://localhost:8080/auth/register để đăng ký tài khoản mới.

## Cấu trúc dự án
```
cnj70-ecommerce
├── src/main/java/com/ecommerce/cnj70/
│   ├── config/           # Cấu hình hệ thống
│   ├── controller/       # Controllers (web, admin, vendor, auth)
│   ├── document/         # MongoDB Documents
│   ├── dto/              # Data Transfer Objects
│   ├── enums/            # Enumerations
│   ├── exception/        # Exception handlers
│   ├── repository/        # MongoDB Repositories
│   ├── security/         # Spring Security
│   ├── service/          # Business logic interfaces
│   └── service/impl/     # Business logic implementations
├── src/main/resources/
│   ├── static/           # CSS, JS, Images
│   └── templates/        # Thymeleaf templates
└── pom.xml              # Maven dependencies
```

## Tính năng chính

### Người dùng (Customer)
- Xem sản phẩm
- Tìm kiếm sản phẩm
- Thêm vào giỏ hàng
- Đặt hàng (COD/VNPAY)
- Xem lịch sử đơn hàng

### Nhà bán (Vendor)
- Quản lý cửa hàng
- CRUD sản phẩm
- Xem và cập nhật đơn hàng
- Thống kê doanh thu

### Quản trị (Admin)
- Quản lý người dùng (khóa/mở tài khoản)
- Quản lý danh mục sản phẩm
- Dashboard thống kê

## API Endpoints

### Auth
- `GET /auth/login` - Trang đăng nhập
- `POST /auth/login` - Xử lý đăng nhập
- `GET /auth/register` - Trang đăng ký
- `POST /auth/register` - Xử lý đăng ký

### Web (Customer)
- `GET /home` - Trang chủ
- `GET /products` - Danh sách sản phẩm
- `GET /products/{id}` - Chi tiết sản phẩm
- `GET /cart` - Giỏ hàng
- `POST /cart/add` - Thêm vào giỏ hàng
- `GET /checkout` - Trang thanh toán
- `POST /checkout` - Xử lý thanh toán
- `GET /orders` - Lịch sử đơn hàng

### Vendor
- `GET /vendor/dashboard` - Dashboard nhà bán
- `GET /vendor/products` - Danh sách sản phẩm
- `POST /vendor/products/create` - Tạo sản phẩm
- `POST /vendor/products/edit/{id}` - Sửa sản phẩm
- `POST /vendor/products/delete/{id}` - Xóa sản phẩm
- `GET /vendor/orders` - Danh sách đơn hàng
- `POST /vendor/orders/{id}/status` - Cập nhật trạng thái đơn

### Admin
- `GET /admin/dashboard` - Dashboard admin
- `GET /admin/users` - Danh sách người dùng
- `POST /admin/users/{id}/lock` - Khóa tài khoản
- `POST /admin/users/{id}/unlock` - Mở khóa tài khoản
- `GET /admin/categories` - Quản lý danh mục
- `POST /admin/categories/create` - Tạo danh mục
- `POST /admin/categories/{id}/delete` - Xóa danh mục

## Phát triển

### Chạy test
```bash
mvn test
```

### Clean build
```bash
mvn clean
mvn clean install
```

## License
Đồ án môn học - CNJ70
