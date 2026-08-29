# QUICK TEST GUIDE - REVIEW & VOUCHER UI

## ✅ ĐANG CHẠY
- Spring Boot app: **http://localhost:8081**
- Status: ✅ Running

---

## 🧪 TEST 1: REVIEW FEATURES (5 phút)

### Bước 1: Login
1. Mở: http://localhost:8081
2. Click "Đăng nhập" 
3. Login với account có sẵn

### Bước 2: Vào Product Detail
1. Click vào bất kỳ product nào
2. Scroll xuống tab "Đánh giá"

### Bước 3: Viết Review
1. Thấy form "Viết đánh giá của bạn"
2. Click vào stars (1-5 sao)
3. Nhập comment
4. Click "Gửi đánh giá"
5. ✅ Review xuất hiện ngay

### Bước 4: Sửa Review
1. Tìm review vừa tạo
2. Thấy buttons **"Sửa"** và **"Xóa"**
3. Click "Sửa"
4. Modal hiện ra
5. Thay đổi rating hoặc comment
6. Click "Lưu thay đổi"
7. ✅ Review được update

### Bước 5: Xóa Review
1. Click "Xóa"
2. Confirm dialog hiện ra
3. Click OK
4. ✅ Review biến mất

---

## 🧪 TEST 2: VOUCHER INPUT UI (2 phút)

### Bước 1: Vào Cart
1. Add product vào cart (nếu chưa có)
2. Vào: http://localhost:8081/cart

### Bước 2: Test Voucher Input
1. Scroll xuống "Tóm tắt đơn hàng"
2. Thấy section "Mã giảm giá"
3. ✅ Textbox **KHÔNG bị disabled**
4. ✅ Button "Áp dụng" **KHÔNG bị disabled**

### Bước 3: Nhập Voucher
1. Click vào textbox
2. Nhập: **SUMMER2026**
3. Click "Áp dụng"
4. ✅ Alert hiện: "Mã voucher 'SUMMER2026' đã được ghi nhận..."
5. ✅ Cart total **KHÔNG THAY ĐỔI** (đúng theo yêu cầu)

### Bước 4: Test Enter Key
1. Nhập voucher code khác
2. Press **Enter** key
3. ✅ Alert hiện ra (không cần click button)

---

## ⚠️ EXPECTED BEHAVIORS

### Review
- ✅ Chỉ user đã login mới viết review được
- ✅ Chỉ thấy Edit/Delete cho review của chính mình
- ✅ Không review được 2 lần cho cùng product
- ✅ Product rating tự động update

### Voucher
- ✅ Input enabled (có thể nhập text)
- ✅ Button enabled (có thể click)
- ✅ Alert hiển thị message rõ ràng
- ✅ Cart total **KHÔNG THAY ĐỔI** (chưa có business logic)
- ✅ Không gọi API backend

---

## 🐛 IF ISSUES

### Issue: Review form không hiện
**Check:**
- Đã login chưa?
- Đã review product này chưa? (chỉ review 1 lần)

### Issue: Edit/Delete buttons không hiện
**Check:**
- Review có phải của mình không?
- Đang login đúng account không?

### Issue: Voucher input bị disabled
**Check:**
- Browser cache? (Ctrl+F5 để refresh)
- Đang ở đúng trang /cart không?

### Issue: Cart total thay đổi khi nhập voucher
**Alert:** ⚠️ KHÔNG ĐÚNG! Report ngay!
Cart total phải **KHÔNG THAY ĐỔI** vì chưa có business logic.

---

## ✅ FILES CHANGED (chỉ 3 files)

1. `src/main/resources/templates/web/product-detail.html` - Review UI
2. `src/main/resources/static/css/product-detail.css` - Review styles
3. `src/main/resources/templates/web/cart.html` - Voucher input UI

**NO database changes**
**NO backend changes**
**NO business logic for voucher**

---

## 📝 AFTER TESTING

Nếu tất cả tests ✅ PASS:
1. Ready to commit
2. Ready to push
3. Ready to create PR

Nếu có issues:
1. Check console logs
2. Check browser console (F12)
3. Check IMPLEMENTATION_REPORT.md
4. Report issues

---

**Happy Testing! 🚀**
