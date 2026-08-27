# VENDOR MODULE - TEST CHECKLIST

## 1. Security & Authentication Tests

### 1.1 Authentication
- [ ] Vendor đăng nhập thành công với credentials đúng
- [ ] Vendor đăng nhập thất bại với credentials sai
- [ ] Unauthenticated user bị chuyển hướng về trang login khi truy cập /vendor/**
- [ ] Session hết hạn sau thời gian quy định

### 1.2 Authorization
- [ ] User VENDOR có quyền truy cập /vendor/**
- [ ] User CUSTOMER không có quyền truy cập /vendor/**
- [ ] User ADMIN có thể truy cập /vendor/**

---

## 2. Shop Management Tests

### 2.1 Create Shop
- [ ] Vendor chưa có shop có thể tạo shop mới
- [ ] Vendor đã có shop không thể tạo shop thứ 2 (phải update)
- [ ] Tên shop trống → validation error
- [ ] Tên shop < 3 ký tự → validation error
- [ ] Tên shop trùng với shop khác → validation error
- [ ] Tạo shop thành công → user.shopId được cập nhật

### 2.2 View Shop
- [ ] Vendor đã có shop xem được thông tin shop
- [ ] Vendor chưa có shop được chuyển hướng sang trang tạo shop

### 2.3 Update Shop
- [ ] Vendor cập nhật tên shop thành công
- [ ] Vendor cập nhật mô tả thành công
- [ ] Vendor không thể cập nhật shop của vendor khác

---

## 3. Product Management Tests

### 3.1 Create Product
- [ ] Vendor có shop có thể tạo sản phẩm mới
- [ ] Vendor chưa có shop không thể tạo sản phẩm (chuyển hướng tạo shop)
- [ ] Tên sản phẩm trống → validation error
- [ ] Giá <= 0 → validation error
- [ ] Stock < 0 → validation error
- [ ] Category không tồn tại → validation error
- [ ] Tạo sản phẩm thành công → product.shopId đúng
- [ ] Tạo sản phẩm thành công → categoryName được lưu

### 3.2 View Products
- [ ] Vendor chỉ thấy sản phẩm thuộc shop của mình
- [ ] Vendor không thấy sản phẩm của vendor khác

### 3.3 Update Product
- [ ] Vendor cập nhật sản phẩm thuộc shop mình thành công
- [ ] Vendor không thể cập nhật sản phẩm của vendor khác
- [ ] Cập nhật sản phẩm không tồn tại → 404

### 3.4 Delete Product
- [ ] Vendor xóa sản phẩm thuộc shop mình thành công
- [ ] Vendor không thể xóa sản phẩm của vendor khác

### 3.5 Product Status
- [ ] Sản phẩm có thể đổi sang trạng thái ACTIVE
- [ ] Sản phẩm có thể đổi sang trạng thái DRAFT
- [ ] Sản phẩm có thể đổi sang trạng thái INACTIVE

---

## 4. Category Tests

### 4.1 View Categories
- [ ] API GET /vendor/products/categories trả về danh sách categories
- [ ] Categories được filter theo active = true
- [ ] Categories được sắp xếp theo sortOrder

---

## 5. ~~Inventory Management Tests~~ (Removed - managed via products)

---

## 6. Order Management Tests

### 6.1 Multi-Shop Order Support
- [ ] Order chứa items từ nhiều shop được tạo thành công
- [ ] Mỗi OrderItem có shopId đúng

### 6.2 View Orders (Vendor)
- [ ] Vendor chỉ thấy orders chứa items thuộc shop của mình
- [ ] Vendor không thấy orders không liên quan
- [ ] Order detail chỉ hiển thị items thuộc shop của vendor
- [ ] Tổng tiền trong order detail chỉ tính items của vendor

### 6.3 Update Order Status
- [ ] Vendor có thể cập nhật PENDING → PROCESSING
- [ ] Vendor có thể cập nhật PROCESSING → SHIPPED
- [ ] Vendor có thể cập nhật SHIPPED → DELIVERED
- [ ] Vendor có thể hủy PENDING order
- [ ] Vendor không thể chuyển DELIVERED → PENDING (invalid transition)
- [ ] Vendor không thể cập nhật order của vendor khác

### 6.4 Order Cancellation with Stock Restore
- [ ] Khi order bị hủy, stock được khôi phục
- [ ] Khi order chuyển sang CANCELLED, stock được khôi phục

---

## 7. Checkout & Inventory Integration Tests

### 7.1 Stock Validation
- [ ] Checkout thất bại nếu stock < số lượng đặt
- [ ] Thông báo lỗi hiển thị số lượng còn lại

### 7.2 Stock Decrease on Order Creation
- [ ] Khi checkout thành công, stock giảm đúng số lượng
- [ ] Stock không bị âm sau khi giảm

### 7.3 Transaction Integrity
- [ ] Nếu 1 item trong cart hết hàng, toàn bộ order bị rollback
- [ ] Không xảy ra trường hợp order tạo thành công nhưng stock không giảm

---

## 8. Dashboard Tests

### 8.1 Stats Accuracy
- [ ] Tổng số sản phẩm = số sản phẩm của shop
- [ ] Số sản phẩm hết hàng = số sản phẩm có stock = 0
- [ ] Tổng đơn hàng = số orders chứa items của shop
- [ ] Doanh thu = tổng totalAmount của orders DELIVERED
- [ ] Doanh thu tháng = doanh thu trong tháng hiện tại

### 8.2 No Shop Scenario
- [ ] Vendor chưa có shop thấy thông báo tạo shop
- [ ] Dashboard stats = 0 cho vendor không có shop

---

## 9. Vendor Profile Tests

### 9.1 Profile View
- [ ] Vendor xem được thông tin cá nhân
- [ ] Vendor xem được thông tin shop (nếu có)
- [ ] Avatar hiển thị đúng

### 9.2 Authorization
- [ ] Vendor không thể xem profile của vendor khác qua /vendor/profile/{id}

---

## 10. Error Handling Tests

### 10.1 Validation Errors
- [ ] Form validation hiển thị message tiếng Việt
- [ ] Các trường required được validate

### 10.2 Authorization Errors
- [ ] Truy cập data không thuộc shop → 403 Forbidden
- [ ] Thông báo lỗi rõ ràng

### 10.3 Resource Not Found
- [ ] Product không tồn tại → 404
- [ ] Order không tồn tại → 404

---

## 11. API Endpoints Summary

| Endpoint | Method | Description | Auth Required |
|----------|--------|-------------|---------------|
| /vendor/profile | GET | Xem profile | VENDOR |
| /vendor/shop | GET | Xem thông tin shop | VENDOR |
| /vendor/shop/create | GET | Form tạo shop | VENDOR |
| /vendor/shop/create | POST | Tạo shop mới | VENDOR |
| /vendor/shop/update | POST | Cập nhật shop | VENDOR |
| /vendor/products | GET | Danh sách sản phẩm | VENDOR |
| /vendor/products/create | GET | Form tạo sản phẩm | VENDOR |
| /vendor/products/create | POST | Tạo sản phẩm | VENDOR |
| /vendor/products/edit/{id} | GET | Form sửa sản phẩm | VENDOR |
| /vendor/products/edit/{id} | POST | Cập nhật sản phẩm | VENDOR |
| /vendor/products/delete/{id} | POST | Xóa sản phẩm | VENDOR |
| /vendor/products/categories | GET | Danh sách categories | VENDOR |
| /vendor/orders | GET | Danh sách đơn hàng | VENDOR |
| /vendor/orders/{id} | GET | Chi tiết đơn hàng | VENDOR |
| /vendor/orders/{id}/status | POST | Cập nhật trạng thái | VENDOR |
| /vendor/dashboard | GET | Dashboard | VENDOR |

---

## 12. Database Schema Changes

### Order.OrderItem - Add shopId
```javascript
db.orders.updateMany(
  { "items.shopId": { $exists: false } },
  [{ $set: { "items": { $map: { input: "$items", as: "item", in: { $mergeObjects: ["$$item", { shopId: null }] } } } } }]
)
```

### Index Recommendations
```javascript
db.orders.createIndex({ "items.shopId": 1 })
db.orders.createIndex({ "items.shopId": 1, "createdAt": -1 })
db.products.createIndex({ "shopId": 1 })
db.products.createIndex({ "categoryId": 1 })
```

---

## 13. Pre-Merge Checklist

- [ ] Tất cả unit tests pass
- [ ] Tất cả integration tests pass
- [ ] Code review approved
- [ ] Documentation updated
- [ ] Database migration scripts ready (nếu cần)
- [ ] Frontend team notified về API changes (Order.OrderItem.shopId)
