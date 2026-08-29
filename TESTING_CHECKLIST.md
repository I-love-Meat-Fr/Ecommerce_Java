# ✅ TESTING CHECKLIST

**Date:** ___________  
**Tester:** ___________  
**Branch:** feature/cart  
**App URL:** http://localhost:8081

---

## 📋 PRE-TESTING CHECKLIST

- [ ] Spring Boot app đang chạy
- [ ] MongoDB connected
- [ ] Browser đã mở http://localhost:8081
- [ ] Đã có account để login
- [ ] Đã đọc QUICK_TEST_GUIDE.md

---

## 🧪 REVIEW FEATURES

### Test 1: Create Review ✅
- [ ] Login thành công
- [ ] Vào Product Detail page
- [ ] Click tab "Đánh giá"
- [ ] Thấy form "Viết đánh giá của bạn"
- [ ] Star rating hiển thị (1-5 stars)
- [ ] Click vào star → star sáng lên
- [ ] Nhập comment vào textarea
- [ ] Click "Gửi đánh giá"
- [ ] Page reload
- [ ] Review xuất hiện trong list
- [ ] Product rating được update
- [ ] Review count tăng lên

**Status:** ⬜ Pass | ⬜ Fail  
**Notes:** ___________________________________________

---

### Test 2: Display Reviews ✅
- [ ] Reviews hiển thị đúng thứ tự (mới nhất trước)
- [ ] Rating stars hiển thị đúng (filled/empty)
- [ ] Comment text hiển thị đúng
- [ ] User name hiển thị
- [ ] Created date hiển thị (dd/MM/yyyy)
- [ ] Empty state hiển thị khi chưa có review

**Status:** ⬜ Pass | ⬜ Fail  
**Notes:** ___________________________________________

---

### Test 3: Edit Own Review ✅
- [ ] Thấy button "Sửa" cho review của mình
- [ ] KHÔNG thấy button "Sửa" cho review người khác
- [ ] Click "Sửa"
- [ ] Modal hiển thị
- [ ] Rating hiện tại được select đúng
- [ ] Comment hiện tại hiển thị đúng
- [ ] Thay đổi rating
- [ ] Thay đổi comment
- [ ] Click "Lưu thay đổi"
- [ ] Modal đóng
- [ ] Page reload
- [ ] Review được update
- [ ] Product rating được recalculate

**Status:** ⬜ Pass | ⬜ Fail  
**Notes:** ___________________________________________

---

### Test 4: Delete Own Review ✅
- [ ] Thấy button "Xóa" cho review của mình
- [ ] KHÔNG thấy button "Xóa" cho review người khác
- [ ] Click "Xóa"
- [ ] Confirm dialog hiển thị
- [ ] Message: "Bạn có chắc muốn xóa đánh giá này?"
- [ ] Click "Cancel" → review vẫn còn
- [ ] Click "Xóa" lần nữa
- [ ] Click "OK" → page reload
- [ ] Review biến mất
- [ ] Product rating được recalculate
- [ ] Review count giảm xuống

**Status:** ⬜ Pass | ⬜ Fail  
**Notes:** ___________________________________________

---

### Test 5: Already Reviewed Check ✅
- [ ] Tạo review cho Product A
- [ ] Reload page
- [ ] Form "Viết đánh giá" KHÔNG hiển thị
- [ ] Thấy message "Bạn đã đánh giá sản phẩm này"
- [ ] Background màu xanh lá
- [ ] Icon check circle hiển thị

**Status:** ⬜ Pass | ⬜ Fail  
**Notes:** ___________________________________________

---

### Test 6: Authentication Required ✅
- [ ] Logout
- [ ] Vào Product Detail
- [ ] KHÔNG thấy form "Viết đánh giá"
- [ ] Thấy message "Bạn cần đăng nhập để viết đánh giá"
- [ ] Link "đăng nhập" hoạt động
- [ ] Click link → redirect to login page

**Status:** ⬜ Pass | ⬜ Fail  
**Notes:** ___________________________________________

---

### Test 7: Security - Cannot Edit Others ✅
- [ ] Login với User A
- [ ] Tạo review cho Product X
- [ ] Logout
- [ ] Login với User B
- [ ] Vào Product X detail
- [ ] Thấy review của User A
- [ ] KHÔNG thấy buttons "Sửa"/"Xóa" cho review của User A
- [ ] Chỉ thấy buttons cho review của User B (nếu có)

**Status:** ⬜ Pass | ⬜ Fail  
**Notes:** ___________________________________________

---

### Test 8: Validation ✅
- [ ] Tạo review không chọn rating → submit
- [ ] Browser validation: "Please select one of these options"
- [ ] Tạo review không nhập comment → submit
- [ ] Browser validation: "Please fill out this field"
- [ ] Tạo review với comment trống (spaces only)
- [ ] Backend error message hiển thị
- [ ] Rating < 1 hoặc > 5 → backend reject

**Status:** ⬜ Pass | ⬜ Fail  
**Notes:** ___________________________________________

---

## 🛒 VOUCHER INPUT UI

### Test 9: Input Enabled ✅
- [ ] Login
- [ ] Add product vào cart
- [ ] Vào http://localhost:8081/cart
- [ ] Scroll xuống "Tóm tắt đơn hàng"
- [ ] Thấy section "Mã giảm giá"
- [ ] Textbox KHÔNG bị disabled (có thể click)
- [ ] Button "Áp dụng" KHÔNG bị disabled
- [ ] Placeholder: "Nhập mã giảm giá"

**Status:** ⬜ Pass | ⬜ Fail  
**Notes:** ___________________________________________

---

### Test 10: Enter Voucher Code ✅
- [ ] Click vào voucher textbox
- [ ] Nhập: "SUMMER2026"
- [ ] Text hiển thị trong input
- [ ] Không có error
- [ ] Thử nhập > 50 characters
- [ ] Chỉ nhận đúng 50 characters (maxlength)

**Status:** ⬜ Pass | ⬜ Fail  
**Notes:** ___________________________________________

---

### Test 11: Apply Voucher (UI Only) ✅
- [ ] Nhập voucher code: "TEST123"
- [ ] Click button "Áp dụng"
- [ ] Alert hiển thị
- [ ] Alert message: "Mã voucher 'TEST123' đã được ghi nhận..."
- [ ] Alert message nói rõ: "Chức năng áp dụng voucher sẽ được kích hoạt sau khi hệ thống voucher hoàn thiện"
- [ ] Click OK
- [ ] Alert đóng
- [ ] Voucher code vẫn còn trong textbox

**Status:** ⬜ Pass | ⬜ Fail  
**Notes:** ___________________________________________

---

### Test 12: Cart Total Unchanged ✅ (CRITICAL!)
- [ ] Note lại Cart Subtotal hiện tại: ___________đ
- [ ] Note lại Cart Total hiện tại: ___________đ
- [ ] Nhập voucher code bất kỳ
- [ ] Click "Áp dụng"
- [ ] Kiểm tra Subtotal: ___________đ (phải GIỐNG)
- [ ] Kiểm tra Total: ___________đ (phải GIỐNG)
- [ ] KHÔNG có dòng "Giảm giá" xuất hiện
- [ ] KHÔNG có discount amount hiển thị

**Status:** ⬜ Pass | ⬜ Fail  
**Notes:** ___________________________________________

---

### Test 13: Empty Voucher Validation ✅
- [ ] Không nhập gì vào textbox
- [ ] Click "Áp dụng"
- [ ] Alert hiển thị: "Vui lòng nhập mã giảm giá"
- [ ] Cart total không thay đổi

**Status:** ⬜ Pass | ⬜ Fail  
**Notes:** ___________________________________________

---

### Test 14: Enter Key Support ✅
- [ ] Nhập voucher code
- [ ] Press Enter key (không click button)
- [ ] Alert hiển thị (giống như click button)
- [ ] Behavior chính xác

**Status:** ⬜ Pass | ⬜ Fail  
**Notes:** ___________________________________________

---

## 🔄 REGRESSION TESTS

### Test 15: Cart Functions Still Work ✅
- [ ] Add to cart hoạt động
- [ ] Increase quantity (+) hoạt động
- [ ] Decrease quantity (-) hoạt động
- [ ] Confirm khi giảm từ 1 → 0
- [ ] Remove item hoạt động
- [ ] Cart badge count đúng
- [ ] Cart total calculation đúng
- [ ] Navigate to checkout hoạt động

**Status:** ⬜ Pass | ⬜ Fail  
**Notes:** ___________________________________________

---

### Test 16: Product Functions Still Work ✅
- [ ] Product listing hiển thị
- [ ] Product search hoạt động
- [ ] Product filter by category hoạt động
- [ ] Product detail page hoạt động
- [ ] Product images hiển thị
- [ ] Add to cart from product detail hoạt động
- [ ] Stock display đúng
- [ ] Rating hiển thị đúng

**Status:** ⬜ Pass | ⬜ Fail  
**Notes:** ___________________________________________

---

### Test 17: Navigation Still Works ✅
- [ ] Home page hoạt động
- [ ] Header navigation hoạt động
- [ ] Footer links hoạt động
- [ ] Breadcrumbs hoạt động
- [ ] User dropdown menu hoạt động
- [ ] Logout hoạt động

**Status:** ⬜ Pass | ⬜ Fail  
**Notes:** ___________________________________________

---

## 🖥️ BROWSER CONSOLE CHECK

### Test 18: No JavaScript Errors ✅
- [ ] Open browser console (F12)
- [ ] Navigate các pages
- [ ] KHÔNG có error màu đỏ
- [ ] KHÔNG có warning quan trọng
- [ ] Network requests thành công (status 200)

**Status:** ⬜ Pass | ⬜ Fail  
**Errors found:** ___________________________________________

---

## 📱 RESPONSIVE TEST (Optional)

### Test 19: Mobile View ✅
- [ ] Resize browser xuống 375px width
- [ ] Review form vẫn hiển thị đúng
- [ ] Star rating vẫn click được
- [ ] Edit modal responsive
- [ ] Voucher input responsive
- [ ] Buttons không bị che
- [ ] Text không bị overflow

**Status:** ⬜ Pass | ⬜ Fail  
**Notes:** ___________________________________________

---

## 📊 FINAL RESULTS

### Test Summary
- **Total Tests:** 19
- **Passed:** _____
- **Failed:** _____
- **Pass Rate:** _____%

### Critical Issues Found
1. ___________________________________________
2. ___________________________________________
3. ___________________________________________

### Minor Issues Found
1. ___________________________________________
2. ___________________________________________
3. ___________________________________________

### Overall Status
- ⬜ ✅ ALL PASS - Ready to merge
- ⬜ ⚠️ MINOR ISSUES - Can merge with notes
- ⬜ ❌ CRITICAL ISSUES - Need fixes before merge

---

## 📝 TESTER NOTES

### Observations:
___________________________________________
___________________________________________
___________________________________________

### Suggestions:
___________________________________________
___________________________________________
___________________________________________

### Additional Tests Performed:
___________________________________________
___________________________________________
___________________________________________

---

## ✅ APPROVAL

**Tested by:** ___________________________________________  
**Date:** ___________________________________________  
**Time:** ___________________________________________  
**Signature:** ___________________________________________

**Ready for Code Review:** ⬜ YES | ⬜ NO  
**Ready for Merge:** ⬜ YES | ⬜ NO

---

**End of Testing Checklist**
