# PHASE – SỬA TOÀN BỘ CHỨC NĂNG PRODUCT DETAIL V3

## 1. MỤC TIÊU

Sửa hoàn chỉnh trang:

```text
/products/{productId}
```

Ví dụ:

```text
http://localhost:8081/products/6aa4fa015adfab313ec9edc2
```

Các lỗi hiện tại cần xử lý:

1. **Thêm vào giỏ hàng**

   * Khi click, browser đang mở trực tiếp JSON response.
   * Mong muốn: xử lý bằng JavaScript/AJAX và hiển thị message/toast ngay trên Product Detail.

2. **Mua hàng**

   * Nút hiện có nhưng có thể không hoạt động do JavaScript không bind đúng.

3. **Mô tả / Thông số / Đánh giá**

   * Tab/panel hiện không hoạt động đúng.

4. **Quantity**

   * Product Detail có quantity control:

     * `increaseQty()`
     * `decreaseQty()`
     * `#quantity-input`
   * Nhưng khi Add to Cart, quantity người dùng chọn chưa được truyền/áp dụng đúng.
   * Phải đảm bảo user chọn bao nhiêu thì Cart nhận đúng số lượng đó.

5. **Stock**

   * Quantity phải bị giới hạn bởi stock thực tế của Product.
   * Không được hard-code giới hạn stock nếu backend đã có stock thực tế.

Mục tiêu cuối cùng:

> Product Detail phải hoạt động đầy đủ từ UI → JavaScript → Backend → Database → Response → UI, không chỉ hiển thị giao diện.

---

# 2. NGUYÊN TẮC BẮT BUỘC

* Làm trực tiếp trên branch hiện tại `feature/admin`.
* Không merge branch khác.
* Không reset.
* Không cherry-pick toàn bộ branch.
* Không copy toàn bộ Product Detail từ branch khác.
* Không viết lại chức năng đã tồn tại nếu chỉ cần sửa integration.
* **Phải đọc và xác định nguyên nhân thực tế trước khi sửa.**
* Không giả định JS/API đang thiếu nếu code hiện tại đã có.
* Không tạo fake data.
* Không thay đổi business logic không liên quan.
* Không tự ý thay đổi Admin Dashboard.
* Không tự ý sửa Admin/Vendor nếu không có dependency trực tiếp.
* Không xóa/reset thay đổi chưa commit của người dùng.
* Không commit.
* Không push.
* Không mở rộng scope ngoài Product Detail và các dependency trực tiếp cần thiết để Product Detail hoạt động.

---

# 3. PHASE 1 – AUDIT VÀ XÁC ĐỊNH ROOT CAUSE

**Không sửa code ngay.**

Đọc và kiểm tra toàn bộ Product Detail hiện tại.

Các file cần ưu tiên kiểm tra:

```text
web/product-detail.html
CSS liên quan Product Detail
JS liên quan Product Detail
main.js
ProductController
ProductService
CartController
CartService
Cart model/DTO/repository
ReviewController
ReviewService
Review model/DTO/repository
SecurityConfig
các route/API liên quan
```

Phân loại từng chức năng:

```text
[WORKING]
[PARTIAL]
[BROKEN]
[MISSING]
```

## 3.1 Kiểm tra HTML structure

Đặc biệt kiểm tra:

* Số lượng `<main>` mở/đóng.
* `<main>` có bị lồng sai không.
* Có thẻ HTML mở nhưng không đóng không.
* Có thẻ đóng sai vị trí không.
* `#add-to-cart-form` có nằm trong DOM hợp lệ không.
* `#quantity-input` có nằm đúng trong form không.
* `#tab-description` có tồn tại không.
* `#tab-specs` có tồn tại không.
* `#tab-reviews` có tồn tại không.
* `#pdp-toast` có tồn tại không.
* Các `<script>` có nằm đúng vị trí không.

**Nếu phát hiện HTML structure sai, phải sửa HTML trước khi kết luận JavaScript bị lỗi.**

Không thêm JavaScript mới để che lỗi HTML.

---

# 4. PHASE 2 – KIỂM TRA JAVASCRIPT BINDING

Trước khi viết hoặc thêm bất kỳ JS nào, kiểm tra JS hiện tại.

Đặc biệt:

```javascript
document.getElementById('add-to-cart-form')
```

phải trả về element.

Kiểm tra:

```javascript
document.getElementById('quantity-input')
```

Kiểm tra:

```javascript
typeof increaseQty
typeof decreaseQty
typeof buyNow
```

Kiểm tra event handler:

```text
Add to Cart
Buy Now
Quantity +
Quantity -
Quantity input
Tabs
Review form
```

## Kiểm tra Console

Chạy application và mở DevTools Console.

Nếu có JavaScript error:

```text
Uncaught ...
ReferenceError ...
TypeError ...
Cannot read properties of null ...
```

phải xác định và sửa lỗi đó trước.

Đặc biệt kiểm tra các đoạn:

```text
#region DEBUG
_log()
_send()
_dbg
console.log(...)
document.elementFromPoint(...)
sendBeacon(...)
```

Nếu debug code đang gây exception hoặc làm cản trở các handler chính:

* Có thể comment/remove phần debug không cần thiết.
* Không được xóa logic nghiệp vụ đang hoạt động.
* Không tạo thêm một bộ handler mới trùng với handler hiện tại.

---

# 5. PHASE 3 – KIỂM TRA CSS PRODUCT DETAIL

Kiểm tra file CSS thực tế.

## 5.1 Tab panel

Phải xác nhận logic tương đương:

```css
.pdp-tab-panel {
    display: none;
}

.pdp-tab-panel.active {
    display: block;
}
```

Nếu project dùng cách triển khai CSS khác thì giữ cách hiện tại, miễn bảo đảm:

```text
Tab active
    ↓
Panel tương ứng
    ↓
Hiển thị đúng
```

## 5.2 Toast

Kiểm tra:

```text
.pdp-toast
.pdp-toast.show
```

Phải xác nhận:

* Toast tồn tại.
* Toast mặc định không che nội dung.
* Khi thêm class `show` thì toast hiển thị.
* Toast nằm ở vị trí phù hợp.
* Toast không che quantity/button.
* Toast có thể hiển thị cả success và error.

**Không tạo thêm toast mới nếu project đã có `#pdp-toast`.**

---

# 6. PHASE 4 – FIX ADD TO CART

## 6.1 Không được mặc định rằng AJAX đang thiếu

Code hiện tại đã có:

```text
form #add-to-cart-form
fetch('/api/cart/add')
e.preventDefault()
response.json()
showToast(...)
```

Do đó:

> Không được viết lại Add to Cart thành một AJAX flow mới trước khi xác định tại sao handler hiện tại không chạy.

## 6.2 Kiểm tra nguyên nhân

Theo thứ tự:

```text
HTML structure
    ↓
#add-to-cart-form tồn tại?
    ↓
JS có chạy đến handler không?
    ↓
Có JavaScript error trước handler không?
    ↓
submit có e.preventDefault() không?
    ↓
fetch có được gọi không?
    ↓
Network request có gửi không?
```

## 6.3 Network Test

Khi click Add to Cart:

```text
Method: POST
URL: /api/cart/add
```

Kiểm tra:

* Request body/form data.
* `productId`.
* `quantity`.
* HTTP status.
* Response JSON.

Response thành công có dạng tương đương:

```json
{
    "success": true,
    "message": "...",
    "itemCount": 5
}
```

## 6.4 UI behavior

Sau success:

```text
Không đổi URL
Không mở JSON page
Không reload trang không cần thiết
        ↓
Hiển thị toast/message
```

Ví dụ:

```text
Đã thêm sản phẩm vào giỏ hàng
```

Sau error:

```text
Hiển thị lỗi ngay trên Product Detail
```

Không redirect tới JSON.

---

# 7. PHASE 5 – FIX QUANTITY CONTROL VÀ STOCK

Đây là phần bắt buộc.

Product Detail hiện có:

```html
<div class="pdp-quantity-control">
    <button class="pdp-qty-btn" onclick="decreaseQty()">
        <i class="fas fa-minus"></i>
    </button>

    <input
        type="number"
        id="quantity-input"
        name="quantity"
        value="1"
        min="1"
        max="7">

    <button class="pdp-qty-btn" onclick="increaseQty()">
        <i class="fas fa-plus"></i>
    </button>
</div>
```

## 7.1 Quantity phải hoạt động thực tế

Kiểm tra:

```text
+ → quantity tăng 1
- → quantity giảm 1
```

Không được:

```text
quantity < 1
```

## 7.2 Quantity phải dựa trên stock thực tế

Không mặc định `max="7"` là đúng.

Phải kiểm tra stock thực tế của Product.

Ví dụ:

```text
stock = 7
→ max quantity = 7

stock = 20
→ max quantity = 20

stock = 0
→ không cho Add to Cart / Buy Now
```

Không hard-code stock nếu database/backend đã có giá trị thật.

Frontend chỉ hỗ trợ validation UX.

**Backend vẫn phải kiểm tra stock cuối cùng.**

## 7.3 Nhập quantity trực tiếp

User có thể nhập:

```text
1
2
3
...
```

Phải validate:

```text
quantity >= 1
quantity <= stock
```

Nếu nhập sai:

```text
→ không gửi request
→ hiển thị lỗi/message phù hợp
```

## 7.4 Add to Cart phải gửi quantity thực tế

Đây là yêu cầu quan trọng nhất.

Nếu user chọn:

```text
quantity = 3
```

thì request phải gửi:

```text
productId = ...
quantity = 3
```

Không được gửi mặc định:

```text
quantity = 1
```

Flow bắt buộc:

```text
User chọn quantity
        ↓
#quantity-input
        ↓
Click Add to Cart
        ↓
JavaScript đọc quantity hiện tại
        ↓
POST /api/cart/add
        ↓
Backend nhận quantity chính xác
        ↓
Cart lưu quantity chính xác
```

## 7.5 Kiểm tra Backend

Trace:

```text
#quantity-input
    ↓
JavaScript
    ↓
POST /api/cart/add
    ↓
CartController
    ↓
CartService
    ↓
CartItem
    ↓
Database
```

Xác nhận backend không:

* Bỏ qua quantity.
* Luôn set quantity = 1.
* Override quantity.
* Không kiểm tra stock.
* Cho quantity vượt stock.

Nếu backend đã đúng thì **chỉ sửa frontend integration**.

Không sửa backend không cần thiết.

---

# 8. PHASE 6 – FIX BUY NOW

Kiểm tra function hiện tại:

```javascript
buyNow()
```

Không tạo flow mới nếu function đã tồn tại.

## Flow phải là:

### User đã đăng nhập

```text
Product Detail
    ↓
Chọn quantity
    ↓
Mua ngay
    ↓
POST /api/cart/add
    ↓
quantity = quantity user đã chọn
    ↓
success
    ↓
/checkout
```

### User chưa đăng nhập

Phải sử dụng authentication hiện tại:

```text
Mua ngay
    ↓
API trả 401
    ↓
/auth/login
```

Không tạo authentication flow mới.

## Kiểm tra

* Product ID.
* Quantity.
* Stock.
* API.
* HTTP status.
* Login.
* Redirect `/checkout`.
* Product/quantity xuất hiện đúng trong Checkout.

---

# 9. PHASE 7 – FIX TAB MÔ TẢ

Tab:

```text
Mô tả sản phẩm
```

Kiểm tra:

```text
.pdp-tab
data-tab="description"
#tab-description
```

Khi click:

```text
button active
    ↓
#tab-description
    ↓
class active
    ↓
display block
```

Kiểm tra cả:

* HTML.
* JavaScript.
* CSS.
* DOM.
* Content binding.

Không dùng description hard-code.

---

# 10. PHASE 8 – FIX TAB THÔNG SỐ

Kiểm tra:

```text
data-tab="specs"
#tab-specs
```

Khi click:

```text
Thông số
    ↓
#tab-specs
    ↓
active
    ↓
display block
```

Kiểm tra dữ liệu:

```text
ProductController
    ↓
Product
    ↓
specifications
    ↓
product-detail.html
```

Nếu backend đã có `product.specifications` thì dùng dữ liệu đó.

Không tạo specification giả.

---

# 11. PHASE 9 – FIX REVIEW / RATING

Kiểm tra toàn bộ:

```text
Product Detail
    ↓
Review section
    ↓
Review list
    ↓
Rating
    ↓
Review form
    ↓
Submit
    ↓
ReviewController
    ↓
ReviewService
    ↓
Database
    ↓
Product Detail
```

## Kiểm tra frontend

* Review form.
* Product ID.
* Rating input.
* Comment.
* Submit button.
* JavaScript validation.
* Error handling.

## Kiểm tra backend

* `ReviewController`.
* `ReviewService`.
* `ReviewServiceImpl`.
* Repository.
* Model/DTO.

Không viết lại Review module nếu module hiện tại đã hoạt động.

## Review validation

Phải giữ validation hiện tại:

```text
rating 1–5
comment hợp lệ
user authentication
không review trùng nếu business rule đang áp dụng
```

## Kiểm tra update/delete

Nếu Product Detail có chức năng sửa/xóa review, phải kiểm tra:

* `updateReview`.
* `deleteReview`.
* Error handling.

Không để exception bị nuốt im lặng.

Nếu update/delete thất bại phải có xử lý phù hợp thay vì chỉ log rồi bỏ qua.

---

# 12. PHASE 10 – KIỂM TRA TOÀN BỘ PRODUCT DETAIL UI

Sau khi sửa các lỗi chính, kiểm tra toàn bộ action đang có:

```text
Product Detail
│
├── Product image
├── Thumbnail
├── Quantity +
├── Quantity -
├── Quantity input
├── Add to Cart
├── Buy Now
├── Description
├── Specifications
├── Reviews
├── Rating
├── Review submit
├── Review update nếu có
└── Review delete nếu có
```

Không sửa những chức năng không lỗi chỉ để thay đổi code.

---

# 13. PHASE 11 – TRACE FRONTEND → BACKEND

Mỗi chức năng phải trace được:

```text
HTML
 ↓
JavaScript
 ↓
Request
 ↓
Controller
 ↓
Service
 ↓
Repository
 ↓
Database
 ↓
Response
 ↓
JavaScript
 ↓
UI
```

Đặc biệt với Add to Cart:

```text
quantity-input
      ↓
JavaScript
      ↓
POST /api/cart/add
      ↓
quantity
      ↓
CartService
      ↓
Cart database
```

Không chấp nhận:

```text
UI có button nhưng không có handler

hoặc

handler tồn tại nhưng không bind

hoặc

fetch gửi sai quantity

hoặc

API nhận sai quantity

hoặc

backend bỏ qua quantity

hoặc

response đúng nhưng UI không xử lý
```

---

# 14. PHASE 12 – REGRESSION

Sau khi sửa Product Detail phải kiểm tra:

```text
Product List
Cart
Checkout
Order
Login/Auth
Review
Admin
Vendor
```

Đặc biệt:

* Add to Cart từ Product List không bị hỏng.
* Cart không bị hỏng.
* Checkout không bị hỏng.
* Order không bị hỏng.
* Login không bị hỏng.
* Admin không bị ảnh hưởng.
* Vendor không bị ảnh hưởng.

Không thay đổi Admin Dashboard nếu không có dependency trực tiếp.

---

# 15. PHASE 13 – BUILD

Chạy:

```bash
mvn clean package
```

Kiểm tra kết quả thực tế.

`BUILD SUCCESS` chỉ xác nhận build thành công.

Không được dùng `BUILD SUCCESS` để kết luận:

```text
Add to Cart PASS
Buy Now PASS
Review PASS
```

Các chức năng phải được test riêng.

---

# 16. PHASE 14 – FUNCTIONAL TEST THỰC TẾ

Chạy application và test trực tiếp bằng browser.

## 16.1 Product Detail

* [ ] Product Detail mở thành công.
* [ ] Product name đúng.
* [ ] Product image đúng.
* [ ] Price đúng.
* [ ] Stock hiển thị đúng.

## 16.2 Quantity

* [ ] Mặc định quantity = 1.
* [ ] `+` tăng quantity đúng.
* [ ] `-` giảm quantity đúng.
* [ ] Không giảm dưới 1.
* [ ] Không tăng vượt stock.
* [ ] Nhập trực tiếp quantity hoạt động.
* [ ] Quantity > stock bị chặn.
* [ ] Quantity không hợp lệ không được gửi lên backend.

## 16.3 Add to Cart

* [ ] Chọn quantity = 2 → Add to Cart.
* [ ] Network có `POST /api/cart/add`.
* [ ] Request gửi `quantity = 2`.
* [ ] Backend nhận quantity = 2.
* [ ] Cart lưu đúng quantity = 2.
* [ ] Không mở JSON thành trang riêng.
* [ ] URL không bị chuyển sang JSON.
* [ ] Toast/message hiển thị ngay trên Product Detail.
* [ ] Cart badge/count cập nhật đúng.
* [ ] Error cũng hiển thị tại trang.

Lặp lại test với:

```text
quantity = 1
quantity = 3
quantity = stock
```

## 16.4 Buy Now

* [ ] Login → chọn quantity → Buy Now.
* [ ] API nhận đúng quantity.
* [ ] Redirect `/checkout`.
* [ ] Checkout hiển thị đúng product.
* [ ] Checkout hiển thị đúng quantity.
* [ ] Chưa login → xử lý đúng theo authentication hiện tại.

## 16.5 Tabs

* [ ] Mô tả hiển thị.
* [ ] Thông số hiển thị.
* [ ] Đánh giá hiển thị.
* [ ] Active tab đúng.
* [ ] Không có panel bị ẩn sai.

## 16.6 Review

* [ ] Review list hiển thị.
* [ ] Rating hiển thị.
* [ ] User chưa login → xử lý đúng.
* [ ] User login → form hiển thị đúng.
* [ ] Chọn rating.
* [ ] Nhập comment.
* [ ] Submit.
* [ ] Review mới xuất hiện.
* [ ] Update review nếu có.
* [ ] Delete review nếu có.
* [ ] Error được thông báo rõ ràng.

## 16.7 Browser Console

Phải kiểm tra:

```text
F12 → Console
```

Không có JavaScript error đỏ liên quan Product Detail.

## 16.8 Network

Kiểm tra các request quan trọng:

```text
/api/cart/add
/products/{id}/reviews
```

Xác nhận:

* URL đúng.
* Method đúng.
* Request data đúng.
* Response đúng.
* Frontend xử lý response đúng.

---

# 17. PHASE 15 – CLEANUP DEBUG CODE

Sau khi xác định chức năng đã hoạt động:

Kiểm tra các đoạn:

```text
#region DEBUG
_log()
_send()
_dbg
console.log('[PDP-DEBUG]')
sendBeacon()
```

Nếu là debug tạm thời và không còn cần thiết:

* Xóa hoặc comment theo cách an toàn.
* Không xóa logic nghiệp vụ.
* Không để debug code gây ảnh hưởng runtime.
* Console cuối cùng phải sạch các lỗi/debug không cần thiết.

Không bắt buộc phải xóa toàn bộ log hợp lệ của hệ thống nếu đó là logging nghiệp vụ cần thiết.

---

# 18. PHASE 16 – KIỂM TRA THAY ĐỔI CUỐI

Chạy:

```bash
git status
git diff --stat
git diff
```

Kiểm tra:

* File nào thay đổi.
* Vì sao thay đổi.
* Thay đổi có liên quan trực tiếp Product Detail không.
* Không có Admin/Vendor thay đổi ngoài ý muốn.
* Không có fake data.
* Không có code tạm thời.
* Không có duplicate JavaScript handler.
* Không có redirect JSON còn tồn tại.
* Quantity được truyền đúng.
* Stock được kiểm tra đúng.
* Không có thay đổi ngoài scope.

---

# 19. TIÊU CHÍ HOÀN THÀNH

Phase chỉ được coi là hoàn thành khi toàn bộ flow sau hoạt động thực tế:

```text
Product List
      ↓
Product Detail
      ↓
Chọn quantity
      ↓
Kiểm tra stock
      ↓
Add to Cart
      ↓
AJAX/fetch
      ↓
Backend nhận đúng quantity
      ↓
Cart lưu đúng quantity
      ↓
Toast hiển thị tại Product Detail
```

Và:

```text
Product Detail
├── Quantity +
├── Quantity -
├── Quantity input
├── Add to Cart
├── Buy Now
├── Mô tả
├── Thông số
├── Đánh giá
└── Review/Rating
```

đều phải hoạt động đúng.

Đặc biệt:

> **User chọn quantity bao nhiêu thì Add to Cart và Buy Now phải sử dụng đúng quantity đó. Không được tự động trở về quantity = 1 nếu user đã chọn số lượng khác.**

---

# 20. QUY TẮC BÁO CÁO KẾT QUẢ

Cuối cùng báo cáo theo format:

```text
## PRODUCT DETAIL – FINAL RESULT

### Root Cause
- ...

### Files Changed
- ...

### Fixed
- [PASS] Add to Cart
- [PASS] Toast
- [PASS] Quantity
- [PASS] Stock validation
- [PASS] Buy Now
- [PASS] Description
- [PASS] Specifications
- [PASS] Review/Rating

### Regression
- [PASS] Product List
- [PASS] Cart
- [PASS] Checkout
- [PASS] Order
- [PASS] Login
- [PASS] Admin
- [PASS] Vendor

### Build
- mvn clean package: PASS/FAIL

### Not Tested
- ...

### Out of Scope
- ...
```

Chỉ ghi `PASS` khi đã test thực tế.

Nếu không thể test:

```text
NOT TESTED
```

Không được tự suy đoán hoặc tạo kết quả test giả.

---

# 21. GIỚI HẠN SCOPE

Chỉ sửa những gì cần thiết để Product Detail hoạt động đúng.

Được phép sửa trực tiếp:

```text
Product Detail HTML
Product Detail CSS
Product Detail JS
main.js nếu cần cho Cart Badge
Product Controller nếu thực sự cần
Cart Controller/Service nếu thực sự cần để quantity hoạt động
Review Controller/Service nếu thực sự cần
các dependency trực tiếp của Product Detail
```

Không tự ý triển khai:

```text
Admin feature mới
Vendor feature mới
Customer feature mới ngoài Product Detail
Product CRUD mới
Cart redesign
Checkout redesign
Order redesign
Authentication redesign
```

Nếu phát hiện bug độc lập không cần thiết cho Product Detail:

```text
→ ghi OUT OF SCOPE
→ không tự sửa
```

---

# 22. KẾT THÚC

Sau khi hoàn thành:

* Không commit.
* Không push.
* Không merge.
* Không reset.
* Không thay đổi lịch sử Git.

Chỉ trả về báo cáo chính xác về:

```text
Root Cause
Files Changed
Fixes
Functional Test
Regression Test
Build
Not Tested
Out of Scope
```

Không được nói "hoàn thành" chỉ dựa trên việc code compile thành công.
