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

## 5A. Product Image Upload Tests

### 5A.1 Upload Image
- [ ] Upload ảnh đúng định dạng (JPG, PNG, WEBP, GIF) → thành công
- [ ] Upload ảnh > 5MB → bị reject với thông báo lỗi
- [ ] Upload file không phải ảnh (PDF, TXT...) → bị reject
- [ ] Preview ảnh hiển thị đúng ngay khi chọn file (client-side)
- [ ] Khi edit, ảnh cũ vẫn hiển thị nếu không upload ảnh mới
- [ ] Ảnh được lưu đúng URL vào `product.imageUrls[]`

---

## 5B. Product Specification Tests (Thông số kỹ thuật)

### 5B.1 Structured Fields
- [ ] Lưu được các field có cấu trúc: `brand`, `warrantyMonths`, `manufacturer`, `manufacturerAddress`
- [ ] `warrantyMonths` nhận giá trị 0 → hợp lệ (không bắt buộc)
- [ ] `warrantyMonths` âm → validation error
- [ ] `brand` để trống → vẫn lưu được (không bắt buộc)
- [ ] `manufacturerAddress` hiển thị được tiếng Việt có dấu

### 5B.2 Custom Specifications (Tên + Mô tả)
- [ ] Thêm được nhiều dòng `(name, value)` qua nút "Thêm thông số"
- [ ] Xóa được từng dòng specification
- [ ] Khi edit, danh sách specifications cũ được load lại đầy đủ
- [ ] Submit form với 0 specifications → lưu thành công (list rỗng)
- [ ] `ProductSpecification` document lưu đúng field `name`, `value`, `unit` (unit hiện không dùng trong UI nhưng field tồn tại)

---

## 5C. Product Variant Tests (Biến thể sản phẩm)

### 5C.1 Create Variant
- [ ] Vendor thêm được nhiều biến thể qua nút "Thêm biến thể"
- [ ] Mỗi biến thể có field `price` riêng
- [ ] Variant giá < 0 → validation error
- [ ] Variant để trống giá → validation error
- [ ] Mỗi variant có thể có danh sách `(name, value)` phân biệt (VD: Màu sắc = Đỏ)

### 5C.2 Edit Variant
- [ ] Khi edit, danh sách variants được load đầy đủ
- [ ] Xóa 1 variant → STT các variant còn lại được đánh lại đúng
- [ ] Input name của variant được re-index lại đúng format `variants[i].specifications[j]`
- [ ] Submit form sau khi xóa variant → backend nhận đúng số lượng còn lại

### 5C.3 Variant Storage
- [ ] `Product.variants` lưu đúng danh sách `ProductVariant`
- [ ] Mỗi `ProductVariant` chứa `price` + `List<ProductSpecification>`

---

## 5D. Rich Description Tests (TinyMCE)

- [ ] Editor TinyMCE load thành công khi mở form tạo/sửa sản phẩm
- [ ] Nội dung HTML từ editor được lưu vào `product.richDescription`
- [ ] Khi edit, nội dung `richDescription` hiển thị lại đúng trong editor
- [ ] Nội dung HTML chứa ảnh, danh sách, bảng — lưu và render đúng ở trang chi tiết
- [ ] XSS: Nội dung HTML độc hại (`<script>`, `onerror=...`) được sanitize khi hiển thị public

---

## 5E. Product Search & Filter Tests

- [ ] Trang `/vendor/products` có thanh tìm kiếm sản phẩm trên topbar
- [ ] Tìm kiếm filter theo `name` (chứa chuỗi, không phân biệt hoa thường)
- [ ] Tìm kiếm kết hợp với filter status (ACTIVE/DRAFT/INACTIVE)
- [ ] Stock pill hiển thị đúng màu theo ngưỡng (≤10 = low, ≤0 = out)
- [ ] Sắp xếp sản phẩm theo ngày tạo mới nhất

---

## 6. Order Management Tests### 6.1 Multi-Shop Order Support
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

## 7A. Voucher Management Tests (Vendor)

### 7A.1 View Vouchers
- [ ] GET `/vendor/vouchers` hiển thị danh sách voucher thuộc shop của vendor
- [ ] Vendor KHÔNG thấy voucher của shop khác
- [ ] Vendor chưa có shop bị redirect về `/vendor/shop` với thông báo lỗi
- [ ] Thống kê hiển thị đúng: Tổng voucher / Đang hoạt động / Đã sử dụng (used)
- [ ] Empty state hiển thị khi chưa có voucher, có button "Tạo Voucher đầu tiên" gradient cam
- [ ] Icon voucher-card đổi màu theo trạng thái: cam (active), xanh (used up), xám (expired)

### 7A.2 Create Voucher (SHOP type)
- [ ] GET `/vendor/vouchers/create` hiển thị form
- [ ] Form fields: code, name, discountType (PERCENT/AMOUNT), discountValue, maxDiscountAmount, minOrderValue, quantity, startDate, endDate
- [ ] Code trống → validation error tiếng Việt
- [ ] Code < 3 ký tự hoặc > 20 ký tự → validation error
- [ ] Code chứa ký tự thường/ký tự đặc biệt → validation error (chỉ chấp nhận `^[A-Z0-9_-]+$`)
- [ ] Name trống hoặc > 100 ký tự → validation error
- [ ] discountValue <= 0 → validation error
- [ ] quantity < 1 → validation error
- [ ] minOrderValue âm → validation error
- [ ] Code trùng với voucher khác (unique) → bị reject
- [ ] Vendor tạo voucher → `voucher.type = SHOP`, `voucher.shopId = shopId hiện tại`, `voucher.shopName` đúng
- [ ] Sau khi tạo thành công → redirect `/vendor/vouchers` + flash success

### 7A.3 Update Voucher
- [ ] GET `/vendor/vouchers/edit/{id}` hiển thị form pre-filled dữ liệu voucher
- [ ] Vendor sửa được code, name, discount, quantity, dates
- [ ] Vendor KHÔNG thể sửa voucher của shop khác → redirect + flash error "Bạn không có quyền sửa voucher này"
- [ ] Submit form → cập nhật đúng các field, giữ nguyên `used` và `type`

### 7A.4 Delete Voucher (Soft Delete)
- [ ] POST `/vendor/vouchers/delete/{id}` → `active = false` (không xóa khỏi DB)
- [ ] Vendor KHÔNG thể xóa voucher của shop khác → redirect + flash error
- [ ] Voucher đã xóa không còn hiển thị trong danh sách vendor
- [ ] Voucher đã xóa (active=false) không thể áp dụng khi checkout

### 7A.5 Voucher Availability Logic
- [ ] Voucher `active = false` → KHÔNG khả dụng (kể cả còn hạn, còn lượt)
- [ ] Voucher `used >= quantity` → KHÔNG khả dụng (hết lượt)
- [ ] Voucher có `startDate` ở tương lai → KHÔNG khả dụng (chưa đến)
- [ ] Voucher có `endDate` ở quá khứ → KHÔNG khả dụng (hết hạn)
- [ ] Voucher `used < quantity`, trong khoảng [startDate, endDate], `active = true` → KHẢ DỤNG
- [ ] Field `getRemainingQuantity()` trả về `quantity - used` (min 0)

---

## 7B. Voucher Application Tests (Checkout)

### 7B.1 Apply Voucher - Validation
- [ ] POST `/checkout/apply-voucher` với `code` không tồn tại → success=false, message lỗi
- [ ] Apply voucher đã bị soft-delete (`active=false`) → bị reject
- [ ] Apply voucher hết lượt → bị reject
- [ ] Apply voucher chưa đến `startDate` → bị reject
- [ ] Apply voucher quá `endDate` → bị reject
- [ ] Apply voucher SHOP không thuộc shop hiện tại → bị reject (nếu truyền shopId)
- [ ] Apply voucher WEB cho sản phẩm không nằm trong `productIds` → bị reject (nếu productIds khác rỗng)
- [ ] Apply voucher WEB có `productIds` rỗng/null → áp dụng được cho mọi sản phẩm

### 7B.2 Discount Calculation
- [ ] Voucher `PERCENT` 10% trên đơn 500.000đ → discount = 50.000đ
- [ ] Voucher `PERCENT` 50% có `maxDiscountAmount = 30.000` trên đơn 1.000.000đ → discount = 30.000đ (không vượt max)
- [ ] Voucher `AMOUNT` 50.000đ trên đơn bất kỳ → discount = 50.000đ
- [ ] Voucher `AMOUNT` 100.000đ trên đơn 50.000đ → discount = 50.000đ (không vượt total)
- [ ] Order total < `minOrderValue` → discount = 0
- [ ] `finalTotal` không bao giờ âm (luôn ≥ 0)

### 7B.3 Used Counter
- [ ] Sau khi đặt hàng thành công với voucher → `voucher.used` tăng +1
- [ ] Khi `used == quantity` → voucher tự động không còn khả dụng
- [ ] `incrementUsed()` được gọi trong `/checkout/place-order` chỉ khi voucher hợp lệ

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

### Vendor Module

| Endpoint | Method | Description | Auth Required |
|----------|--------|-------------|---------------|
| /vendor/profile | GET | Xem profile | VENDOR |
| /vendor/shop | GET | Xem thông tin shop | VENDOR |
| /vendor/shop/create | GET | Form tạo shop | VENDOR |
| /vendor/shop/create | POST | Tạo shop mới | VENDOR |
| /vendor/shop/update | POST | Cập nhật shop | VENDOR |
| /vendor/products | GET | Danh sách sản phẩm | VENDOR |
| /vendor/products/create | GET | Form tạo sản phẩm | VENDOR |
| /vendor/products/create | POST | Tạo sản phẩm (multipart: `imageFile`) | VENDOR |
| /vendor/products/edit/{id} | GET | Form sửa sản phẩm | VENDOR |
| /vendor/products/edit/{id} | POST | Cập nhật sản phẩm (multipart) | VENDOR |
| /vendor/products/delete/{id} | POST | Xóa sản phẩm | VENDOR |
| /vendor/products/categories | GET | Danh sách categories | VENDOR |
| /vendor/orders | GET | Danh sách đơn hàng | VENDOR |
| /vendor/orders/{id} | GET | Chi tiết đơn hàng | VENDOR |
| /vendor/orders/{id}/status | POST | Cập nhật trạng thái | VENDOR |
| /vendor/vouchers | GET | Danh sách voucher của shop | VENDOR |
| /vendor/vouchers/create | GET | Form tạo voucher | VENDOR |
| /vendor/vouchers/create | POST | Tạo voucher (SHOP type) | VENDOR |
| /vendor/vouchers/edit/{id} | GET | Form sửa voucher | VENDOR |
| /vendor/vouchers/edit/{id} | POST | Cập nhật voucher | VENDOR |
| /vendor/vouchers/delete/{id} | POST | Xóa voucher (soft delete) | VENDOR |
| /vendor/dashboard | GET | Dashboard | VENDOR |

### Public Voucher / Checkout

| Endpoint | Method | Description | Auth Required |
|----------|--------|-------------|---------------|
| /vouchers | GET | Trang công khai hiển thị voucher khả dụng | Public |
| /checkout/apply-voucher | POST | Validate + tính discount cho voucher (JSON) | Authenticated |
| /checkout/place-order | POST | Đặt hàng + `incrementUsed()` cho voucher | Authenticated |

### Admin Module

| Endpoint | Method | Description | Auth Required |
|----------|--------|-------------|---------------|
| /admin/vouchers | GET | Danh sách voucher WEB | ADMIN |
| /admin/vouchers/create | GET | Form tạo voucher WEB | ADMIN |
| /admin/vouchers/create | POST | Tạo voucher WEB (áp dụng toàn hệ thống hoặc cho `productIds`) | ADMIN |
| /admin/vouchers/delete/{id} | POST | Xóa voucher WEB (soft delete) | ADMIN |

---

## 12. Database Schema Changes

### Order.OrderItem - Add shopId
```javascript
db.orders.updateMany(
  { "items.shopId": { $exists: false } },
  [{ $set: { "items": { $map: { input: "$items", as: "item", in: { $mergeObjects: ["$$item", { shopId: null }] } } } } }]
)
```

### Voucher Collection (vouchers)
- Schema: `Voucher.java`
- Fields chính: `id`, `code` (unique), `name`, `type` (SHOP/WEB), `shopId`, `shopName`,
  `productIds[]` (chỉ cho WEB), `discountType` (PERCENT/AMOUNT), `discountValue`,
  `maxDiscountAmount`, `minOrderValue`, `quantity`, `used`, `startDate`, `endDate`,
  `active`, `createdBy`, `createdAt`, `updatedAt`
- Soft delete: xóa bằng cách set `active = false` (KHÔNG xóa khỏi DB)

### Product Collection — Fields mới
- `brand` (String), `warrantyMonths` (Integer), `manufacturer` (String),
  `manufacturerAddress` (String) — thông số có cấu trúc
- `specifications` (List<ProductSpecification>) — thông số tùy chỉnh (name/value/unit)
- `variants` (List<ProductVariant>) — biến thể sản phẩm (price + specifications)
- `richDescription` (String HTML) — mô tả từ TinyMCE editor

### Index Recommendations
```javascript
db.orders.createIndex({ "items.shopId": 1 })
db.orders.createIndex({ "items.shopId": 1, "createdAt": -1 })
db.products.createIndex({ "shopId": 1 })
db.products.createIndex({ "categoryId": 1 })
db.products.createIndex({ "shopId": 1, "name": 1 })              // search theo shop + tên
db.products.createIndex({ "shopId": 1, "status": 1, "createdAt": -1 }) // filter + sort
db.vouchers.createIndex({ "code": 1 }, { unique: true })
db.vouchers.createIndex({ "shopId": 1, "active": 1 })
db.vouchers.createIndex({ "type": 1, "active": 1, "endDate": 1 }) // tìm voucher WEB khả dụng
```

### Migration Script cho Product — thêm fields mới
```javascript
db.products.updateMany(
  { $or: [
      { "brand": { $exists: false } },
      { "specifications": { $exists: false } },
      { "variants": { $exists: false } },
      { "richDescription": { $exists: false } }
  ]},
  { $set: {
      "brand": null,
      "warrantyMonths": null,
      "manufacturer": null,
      "manufacturerAddress": null,
      "specifications": [],
      "variants": [],
      "richDescription": ""
  }}
)
```

---

## 13. Pre-Merge Checklist

- [ ] Tất cả unit tests pass
- [ ] Tất cả integration tests pass
- [ ] Code review approved
- [ ] Documentation updated
- [ ] Database migration scripts ready (nếu cần)
  - [ ] Migration `items.shopId` cho orders
  - [ ] Migration thêm `brand`, `warrantyMonths`, `manufacturer`, `manufacturerAddress`, `specifications`, `variants`, `richDescription` cho products
  - [ ] Index `vouchers.code` unique + compound indexes cho `shopId`/`type`
- [ ] Frontend team notified về API changes
  - [ ] Order.OrderItem.shopId
  - [ ] Product có `specifications`/`variants`/`richDescription`
  - [ ] Voucher SHOP/WEB phân biệt theo `type`
- [ ] Voucher edge cases đã cover: hết lượt, hết hạn, soft-delete, maxDiscountAmount
- [ ] TinyMCE: kiểm tra XSS sanitize khi render `richDescription` ra trang public
- [ ] Image upload: kiểm tra size limit (5MB) + MIME type validation
