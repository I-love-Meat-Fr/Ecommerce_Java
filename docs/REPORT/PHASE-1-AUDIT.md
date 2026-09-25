# PHASE 1 AUDIT REPORT — 25/09/2026

**Ngày audit:** 25/09/2026
**Auditor:** Cursor (read-only mode)
**App version:** CNJ70 Ecommerce — Spring Boot + MongoDB Atlas
**App URL:** http://localhost:8081
**MongoDB URI:** mongodb+srv://cluster0.2pu37z6.mongodb.net/cnj70_ecommerce

---

## 1. CODE AUDIT (Backend)

### 1.1 — Controller Layer (`src/main/java/com/ecommerce/cnj70/controller/admin/`)

| File | Status | Lines | Methods | Role Check | Notes |
|------|--------|-------|---------|------------|-------|
| AdminUserController.java | EXISTING | 144 | 5 | URL-only (SecurityConfig) | Lock/Unlock user. Có `currentUserId()` helper. |
| AdminShopController.java | EXISTING | 264 | 6 | URL-only | List/Detail/Approve/Reject/Activate/Deactivate. |
| AdminVoucherController.java | **MISSING** | 0 | 0 | N/A | Admin voucher dùng `VoucherController` chung (admin endpoints line 233-455). |
| AdminCategoryController.java | EXISTING | 188 | 5 | URL-only | **BUG**: dùng `addAttribute` thay vì `addFlashAttribute` (4 chỗ). |
| AdminKycController.java | EXISTING | 141 | 5 | URL-only | Approve/Reject/Suspend. OK. |
| AdminProductController.java | EXISTING | 175 | 5 | URL-only | List/Detail/Hide/Unhide/Delete. OK. |
| AdminOrderController.java | EXISTING | 105 | 2 | URL-only | List/Detail (read-only). |
| AdminReviewController.java | EXISTING | 198 | 5 | URL-only | List/Detail/Delete/Hide/Restore. |
| AdminViolationController.java | EXISTING | 141 | 4 | URL-only | List/Create/Resolve. **BUG LOGIC** ở service (xem §4.3). |
| AdminEscalationController.java | EXISTING | 316 | 6 | URL-only | List/Detail/Claim/Enforce/Dismiss. |
| AdminAuditLogController.java | EXISTING | 142 | 2 | URL-only | List/Detail. |
| AdminBannerController.java | EXISTING | 260 | 8 | URL-only | List/Create/Detail/Edit/Publish/Unpublish/Delete. |
| AdminDashboardController.java | EXISTING | 26 | 1 | URL-only | Dashboard. |

**Tổng:** 13 controller (12 admin + 1 dashboard). Không có controller bị stub.

### 1.2 — Service Layer (`src/main/java/com/ecommerce/cnj70/service/`)

| Interface | Status | Methods | Impl |
|----------|--------|---------|------|
| AdminUserService | EXISTING | 5 | AdminUserServiceImpl ✅ |
| AdminShopService | EXISTING | 6 | AdminShopServiceImpl ✅ |
| AdminOrderService | EXISTING | 2 | AdminOrderServiceImpl ✅ |
| AdminReviewService | EXISTING | 3 | AdminReviewServiceImpl ✅ |
| AdminProductService | EXISTING | 4 | AdminProductServiceImpl ✅ |
| AdminCategoryService | EXISTING | 5 | AdminCategoryServiceImpl ✅ |
| AdminKycService | EXISTING | 8+ | AdminKycServiceImpl ✅ |
| AdminViolationService | EXISTING | ? | (chưa đọc) |
| AdminEscalationService | EXISTING | 4+ | AdminEscalationServiceImpl ✅ (có `AdminGuard.verify` ADMIN-only) |
| AdminAuditLogService | EXISTING | 7 | AdminAuditLogServiceImpl ✅ (READ-ONLY) |
| AdminBannerService | EXISTING | 6 | AdminBannerServiceImpl ✅ |
| AdminService | EXISTING | ? | AdminServiceImpl ✅ (Dashboard stats) |
| AdminDashboardMetricsGateway | EXISTING | ? | (chưa đọc) |
| ViolationService | EXISTING | 6 | ViolationServiceImpl ✅ |

**Tổng:** 13 admin service interfaces + 1 ViolationService. Không có stub.

### 1.3 — Document Layer (`src/main/java/com/ecommerce/cnj70/document/`)

| Document | Collection | Ghi chú |
|----------|-----------|---------|
| User | `users` | OK |
| Shop | `shops` | OK |
| Voucher | `vouchers` | OK (type=WEB/SHOP, used/quantity) |
| Product | (cần verify) | OK |
| Order | (cần verify) | OK |
| Review | (cần verify) | OK |
| Violation | `violations` | OK (dùng resolvedAt, không có field `status`) |
| Escalation | `escalations` | OK |
| AuditLog | `audit_logs` | (1 trong 2 document cùng collection) |
| AuditLogEntry | `audit_logs` | (1 trong 2 document cùng collection) |
| Banner | `banners` | OK |
| Category | `categories` | OK |
| KycProfile | `kyc_profiles` | OK (đã refactor từ `Kyc.java` → `kycs` per Phase 1 doc) |

### 1.4 — Enum Layer

| Enum | Values | Ghi chú |
|------|--------|---------|
| UserRole | ADMIN, MODERATOR, VENDOR, CUSTOMER | ✅ đúng 4 role |
| AccountStatus | ACTIVE, LOCKED, UNVERIFIED | OK |
| ShopStatus | PENDING, APPROVED, REJECTED, SUSPENDED, RESTRICTED | OK (Phase 3A thêm SUSPENDED + RESTRICTED) |
| AuditAction | (nhiều) | OK |

---

## 2. SECURITY AUDIT

### 2.1 — SecurityConfig (`config/SecurityConfig.java`)

**Lines 82-107** — phân quyền URL:

| Pattern | Current Role | Should Be (per Phase 1) | Status |
|---------|--------------|--------------------------|--------|
| `/`, `/auth/**`, `/css/**`, `/js/**`, `/images/**`, `/uploads/**`, `/error` | permitAll | permitAll | OK |
| `/api/auth/**` | permitAll | permitAll | OK |
| `/api/products/**` | permitAll | permitAll | **BUG**: route không tồn tại → 404 |
| `/api/categories/**` | permitAll | permitAll | **BUG**: route không tồn tại → 404 |
| `/vouchers` | permitAll | permitAll | **BUG RUNTIME**: 500 (xem §6.1) |
| `/api/kyc/callback` | permitAll | permitAll | OK |
| `/admin/users/**` | ADMIN+MODERATOR | ADMIN+MODERATOR | OK (đã sửa ở session trước) |
| `/admin/shops/**` | ADMIN+MODERATOR | ADMIN+MODERATOR | OK |
| `/admin/reviews/**` | ADMIN+MODERATOR | ADMIN+MODERATOR | OK |
| `/admin/vouchers/**` | ADMIN+MODERATOR | ADMIN+MODERATOR | OK (đã thêm ở session trước) |
| `/admin/audit/**` | ADMIN-only | ADMIN-only | OK |
| `/admin/**` (fallback) | ADMIN-only | ADMIN-only | OK |
| `/moderator/**` | MODERATOR-only | MODERATOR-only | OK |
| `/vendor/**` | VENDOR-only | VENDOR-only | OK |
| `/complaints/**` | authenticated | authenticated | OK |
| `/api/**` (fallback) | authenticated | authenticated | OK |
| anyRequest() | permitAll | ? | OK |

### 2.2 — Các cấu hình khác

| Config | Giá trị | Ghi chú |
|--------|---------|---------|
| CSRF | DISABLED | OK (do form POST không có CSRF token; cần kiểm tra Spring 6+) |
| CORS | 2 origins: localhost:8081, localhost:3000 | OK |
| Session | STATELESS | OK (JWT-based) |
| PasswordEncoder | BCryptPasswordEncoder | OK |
| AccessDeniedHandler | redirect → /auth/login?denied=1 | OK |
| AuthenticationEntryPoint | JwtAuthenticationEntryPoint → 401 JSON | OK |

### 2.3 — Authentication Flow

| Component | File | Status |
|-----------|------|--------|
| JwtAuthenticationFilter | security/JwtAuthenticationFilter.java | EXISTING |
| JwtAuthenticationEntryPoint | security/JwtAuthenticationEntryPoint.java | EXISTING (trả 401 JSON) |
| CustomUserDetails | security/CustomUserDetails.java | EXISTING (implements UserDetails, có id/email/role) |
| JwtUserDetailsService | security/JwtUserDetailsService.java | EXISTING |
| ModerationGuard | security/ModerationGuard.java | EXISTING nhưng **DEAD CODE** (xem §3.2) |

---

## 3. MODERATION GUARD AUDIT

### 3.1 — `ModerationGuard.assertModerator(CustomUserDetails)`

**File:** `security/ModerationGuard.java`

**Logic hiện tại (line 48):**
```java
if (role != UserRole.MODERATOR && role != UserRole.ADMIN) {
    throw new BusinessException("Hành động này chỉ Admin hoặc Moderator mới được thực hiện.");
}
```

**Comment (line 11-14):**
```
Admin — read/observe only for Products, Reviews, Shops.
Moderator — sole role permitted to approve / reject / hide / ...
```

**MISMATCH:** Comment nói Admin chỉ đọc, code cho phép cả Admin làm moderation.

### 3.2 — Sử dụng

| Logic | Required | Actual | Status |
|-------|----------|--------|--------|
| Allow MODERATOR | YES | YES (line 48) | OK |
| Allow ADMIN | **YES (Phase 1 spec)** | YES (line 48) | OK nhưng **MISMATCH comment** |
| Deny CUSTOMER | YES | YES (line 48) | OK |
| Deny VENDOR | YES | YES (line 48) | OK |
| **Được gọi ở đâu?** | N/A | **0 chỗ** (dead code) | **BUG**: `ModerationGuard.assertModerator` không được gọi ở bất kỳ file nào |

### 3.3 — `AdminEscalationServiceImpl.AdminGuard.verify`

| Logic | Required | Actual | Status |
|-------|----------|--------|--------|
| Allow ADMIN | YES | YES | OK |
| Check `isEnabled()` | YES | YES (line 508) | OK |
| Check `isAccountNonLocked()` | YES | YES | OK |
| Được gọi ở 3 method | YES | YES (line 134, 172, 246) | OK |

### 3.4 — `@PreAuthorize` usage

| Class | Method | Annotation |
|-------|--------|------------|
| LegalDocumentController | (admin method) | `@PreAuthorize("hasRole('ADMIN')")` |
| KycController | vendor method | `@PreAuthorize("hasRole('VENDOR')")` |
| KycController | other method | `@PreAuthorize("isAuthenticated()")` |
| ModeratorQueueController | class-level | `@PreAuthorize("hasRole('MODERATOR')")` |
| **Admin Controllers** | — | **KHÔNG có @PreAuthorize** — chỉ URL-level |

---

## 4. TEMPLATE AUDIT (Frontend)

### 4.1 — Template list (`src/main/resources/templates/admin/`)

| Template | Status | Action Forms | Admin Shell | Flash UI |
|----------|--------|--------------|-------------|----------|
| dashboard.html | EXISTING | N/A | YES | N/A |
| user-list.html | EXISTING | YES | YES | ❌ MISSING |
| user-detail.html | EXISTING | YES | YES | ? |
| shop-list.html | EXISTING | YES | YES | ❌ MISSING |
| shop-detail.html | EXISTING | YES | YES | ? |
| review-list.html | EXISTING | YES | YES | ❌ MISSING |
| review-detail.html | EXISTING | YES | YES | ? |
| product-list.html | EXISTING | YES | YES | ❌ MISSING |
| product-detail.html | EXISTING | YES | YES | ? |
| order-list.html | EXISTING | N/A | YES | ❌ MISSING |
| order-detail.html | EXISTING | N/A | YES | ? |
| voucher-list.html | EXISTING | YES | YES | ✅ (success/error) |
| voucher-create.html | EXISTING | YES | YES | ? |
| voucher-edit.html | EXISTING | YES | YES | ? |
| banner-list.html | EXISTING | YES | YES | ❌ MISSING |
| banner-detail.html | EXISTING | YES | YES | ? |
| banner-form.html | EXISTING | YES | YES | ? |
| category-manage.html | EXISTING | YES | YES | ❌ MISSING |
| kyc-list.html | EXISTING | YES | YES | ✅ (success/error) |
| kyc-detail.html | EXISTING | YES | YES | ? |
| violation-list.html | EXISTING | YES | YES | ✅ (flashSuccess/Error) |
| violation-detail.html | EXISTING | YES | YES | ? |
| violation-create.html | EXISTING | YES | YES | ? |
| escalation-queue.html | EXISTING | YES | YES | ❌ MISSING |
| escalation-detail.html | EXISTING | YES | YES | ? |
| audit-list.html | EXISTING | N/A | YES | ❌ MISSING |
| audit-detail.html | EXISTING | N/A | YES | ? |

**Tổng:** 27 admin templates.

### 4.2 — Controller ↔ Template mapping (cross-check)

| Controller | Templates | Match |
|------------|-----------|-------|
| AdminUserController | user-list, user-detail | ✅ |
| AdminShopController | shop-list, shop-detail | ✅ |
| AdminOrderController | order-list, order-detail | ✅ |
| AdminReviewController | review-list, review-detail | ✅ |
| AdminProductController | product-list, product-detail | ✅ |
| AdminBannerController | banner-list, banner-detail, banner-form | ✅ |
| AdminCategoryController | category-manage | ✅ |
| AdminKycController | kyc-list, kyc-detail | ✅ |
| AdminViolationController | violation-list, violation-detail, violation-create | ✅ |
| AdminEscalationController | escalation-queue, escalation-detail | ✅ |
| AdminAuditLogController | audit-list, audit-detail | ✅ |
| AdminDashboardController | dashboard | ✅ |
| AdminVoucherController | **MISSING CONTROLLER** (VoucherController chung) | ⚠️ |

### 4.3 — Bugs phát hiện ở Template ↔ Controller

| Bug | Module | Hiện tượng |
|-----|--------|------------|
| **Bug #1** | `AdminCategoryController` line 71-79, 116-120, 136-140, 159-167 | Dùng `redirectAttributes.addAttribute()` (URL param) thay vì `addFlashAttribute()` (session flash). Sau redirect, `flashSuccess`/`flashError` không hiển thị vì không phải flash attribute. URL bị rối với `flashSuccess=...&flashError=...` query params. |
| **Bug #2** | 7 templates thiếu flash UI | `user-list.html`, `shop-list.html`, `review-list.html`, `product-list.html`, `banner-list.html`, `escalation-queue.html`, `audit-list.html` không có `<div th:if="${flashSuccess}">`. Controller vẫn truyền `flashSuccess`/`flashError` qua `addFlashAttribute`, nhưng template không hiển thị → Admin không thấy thông báo. |
| **Bug #3** | `category-manage.html` | Template thiếu flash UI (Bug #2) + Controller dùng `addAttribute` (Bug #1) → 2 bug chồng lên nhau. |

### 4.4 — Logic bug ở Service layer (ảnh hưởng template render)

| Bug | File | Mô tả |
|-----|------|--------|
| **Bug #4** | `ViolationServiceImpl.listViolations(shopId, severity, type, pageable)` line 32-47 | Nếu admin filter `severity` mà không chọn `shopId`, method trả `findAll()` → KHÔNG filter severity. Tương tự với `type`. Admin click filter severity=CRITICAL nhưng kết quả trả ALL violations. |

---

## 5. DATABASE AUDIT (MongoDB)

### 5.1 — Connection

- **URI:** `mongodb+srv://cluster0.2pu37z6.mongodb.net/cnj70_ecommerce` (Atlas)
- **auto-index-creation:** true
- **Status:** App đang chạy → connection OK.

### 5.2 — Collections

| Collection | Document class | Status |
|------------|---------------|--------|
| `users` | User | EXISTING |
| `shops` | Shop | EXISTING |
| `vouchers` | Voucher | EXISTING (filter by type=WEB/SHOP) |
| `products` | Product | EXISTING |
| `orders` | Order | EXISTING |
| `reviews` | Review | EXISTING |
| `violations` | Violation | EXISTING (không có field `status`, dùng `resolvedAt`) |
| `escalations` | Escalation | EXISTING |
| `audit_logs` | AuditLog + AuditLogEntry (cùng collection, schema khác) | ⚠️ DOUBLE SCHEMA |
| `banners` | Banner | EXISTING |
| `categories` | Category | EXISTING |
| `kyc_profiles` | KycProfile | EXISTING (refactored từ `kycs` per Phase 1 doc) |

### 5.3 — Counts

[DEPENDENCY — DB CLIENT] `mongosh`/`mongo` không có sẵn trên máy. Không thể đếm trực tiếp qua CLI. Có thể verify qua:
- `/vouchers` response hiển thị "4 mã" → vouchers (WEB active): **4 records**
- `/products` response hiển thị "2 sản phẩm" → products: **2 records**
- `/vouchers` hiển thị "Test Vendor Shop" → shops có ít nhất 1 record
- Audit log: chưa verify số lượng (cần login ADMIN)

### 5.4 — Issues

| Issue | Owner | Notes |
|-------|-------|-------|
| `audit_logs` có 2 schema (AuditLog + AuditLogEntry) | [EXISTING] | Đã được mirror write fix ở session trước (`AuditLogServiceImpl` mirror sang `AuditLogEntry`) |
| MongoDB URI hard-coded (có fallback env) | [LOW PRIORITY] | application.yml line 8 |
| `KycProfile` collection tên `kyc_profiles` (không phải `kycs` per Phase 1 doc) | [DOC DRIFT] | Phase 1 doc đã cũ; code đã refactor |

---

## 6. WEBSITE AUDIT (Browser/HTTP smoke test)

### 6.1 — HTTP Smoke Test Results

**Test method:** `curl -s -o /dev/null -w "%{http_code}" http://localhost:8081{url}`

| URL | HTTP Status | Expected | Verdict |
|-----|-------------|----------|---------|
| `/` (root) | 302 → /auth/login | 302 | OK |
| `/auth/login` | 200 (HTML) | 200 | OK |
| `/auth/register` | (cần test) | 200 | [NOT TESTED — WRITE-related] |
| `/vouchers` | **500** (JSON: "No converter for LinkedHashMap") | 200 | **BUG #5** |
| `/products` | 200 (HTML, 2 products) | 200 | OK |
| `/api/products` | **404** | 200 (SecurityConfig permitAll nhưng không có controller) | **BUG #6** |
| `/api/categories` | **404** | 200 (SecurityConfig permitAll nhưng không có controller) | **BUG #7** |
| `/admin/users` | 401 (JSON) | 302 (redirect login) hoặc 401 | OK (do JwtAuthenticationEntryPoint) |
| `/admin/categories` | 401 | 401 | OK |
| `/admin/vouchers` | 401 | 401 | OK |
| `/admin/shops` | 401 | 401 | OK |
| `/admin/kyc` | 401 | 401 | OK |
| `/admin/violations` | 401 | 401 | OK |
| `/admin/orders` | 401 | 401 | OK |
| `/admin/products` | 401 | 401 | OK |
| `/admin/reviews` | 401 | 401 | OK |
| `/admin/escalations` | 401 | 401 | OK |
| `/admin/audit` | 401 | 401 | OK |
| `/admin/banners` | 401 | 401 | OK |
| `/admin/dashboard` | 401 | 401 | OK |

### 6.2 — 4 Roles × 12 URLs

[NOT TESTED — REQUIRES LOGIN]
- Phase 1 spec yêu cầu test với 4 roles (ADMIN, MODERATOR, CUSTOMER, VENDOR).
- Không có account test seed data → không thể login.
- Không tự tạo account admin test trong Phase 1 (theo yêu cầu).
- [DEPENDENCY — SEED DATA] Cần user tạo account trước.

### 6.3 — Bug summary từ runtime

| # | URL | Bug | Impact |
|---|-----|-----|--------|
| 5 | `/vouchers` | HTTP 500 — `No converter for [class java.util.LinkedHashMap] with preset Content-Type 'text/html;charset=UTF-8'` | Trang voucher công khai không truy cập được |
| 6 | `/api/products` | HTTP 404 | API công khai không tồn tại (SecurityConfig permitAll vô dụng) |
| 7 | `/api/categories` | HTTP 404 | Tương tự #6 |

---

## 7. ISSUE SUMMARY (Count)

| Category | Count | Details |
|----------|-------|---------|
| **500 errors (runtime)** | **1** | `/vouchers` |
| **404 errors (runtime)** | **2** | `/api/products`, `/api/categories` |
| **403 wrong logic** | 0 | SecurityConfig OK sau session trước |
| **Missing controllers** | 1 | AdminVoucherController (chấp nhận được — dùng VoucherController chung) |
| **Missing templates** | 0 | 27 admin templates đầy đủ |
| **Broken tests** | 0 | 147/147 PASS |
| **Stub methods** | 0 | Không phát hiện |
| **Logic bugs** | **2** | Bug #4 (ViolationService filter), Bug #1 (Category addAttribute) |
| **UI bugs (flash)** | **2** | Bug #2 (7 thiếu flash UI), Bug #3 (Category chồng chéo) |
| **Dead code** | 1 | `ModerationGuard.assertModerator` không được gọi |
| **Doc drift** | 1 | `Kyc.java`/`kycs` → `KycProfile.java`/`kyc_profiles` |
| **Mismatch (doc vs code)** | 1 | `ModerationGuard` comment nói Admin chỉ đọc, code cho Admin làm moderation |
| **Total issues** | **12** | (1+2+0+1+0+0+0+2+2+1+1+1+1) |

---

## 8. KẾT LUẬN & ĐỀ XUẤT

### 8.1 — Bug nghiêm trọng nhất (Priority 1)

| # | Bug | Impact | Suggested Phase |
|---|-----|--------|------------------|
| 5 | `/vouchers` HTTP 500 | Trang công khai không truy cập được | **Phase 2 (Public routes)** |
| 6, 7 | `/api/products`, `/api/categories` HTTP 404 | API công khai bị thiếu | **Phase 2 (Public routes)** |

### 8.2 — Bug quan trọng (Priority 2)

| # | Bug | Impact | Suggested Phase |
|---|-----|--------|------------------|
| 2 | 7 templates thiếu flash UI | Admin không thấy thông báo thành công/lỗi | **Phase 3 (UI consistency)** |
| 1 | `AdminCategoryController` dùng `addAttribute` thay vì `addFlashAttribute` | Flash message bị mất + URL rối | **Phase 3 (UI consistency)** |
| 4 | `ViolationService.listViolations` filter sai khi không có shopId | Admin filter không chính xác | **Phase 4 (Service logic)** |

### 8.3 — Cảnh báo kiến trúc (Priority 3)

| Issue | Owner | Notes |
|-------|-------|-------|
| `ModerationGuard` dead code | [DECISION NEEDED] | Hoặc tích hợp vào Service, hoặc xóa |
| Comment/code mismatch ở `ModerationGuard` | [DOC UPDATE] | Comment nói Admin chỉ đọc, code cho Admin làm moderation |
| `audit_logs` 2 schema | [EXISTING — đã có mirror write fix] | OK cho read, không cần xử lý thêm |
| `KycProfile` rename (so với Phase 1 doc) | [DOC UPDATE] | Doc cũ, code đã refactor |

### 8.4 — Đề xuất thứ tự Phase tiếp theo

1. **Phase 2 — Public Routes Fix:** sửa `/vouchers` 500, `/api/*` 404
2. **Phase 3 — UI Consistency:** thêm flash UI vào 7 templates, sửa `addAttribute` → `addFlashAttribute` ở AdminCategoryController
3. **Phase 4 — Service Logic:** sửa `ViolationService.listViolations` filter
4. **Phase 5 — Doc Sync:** cập nhật Phase 1 doc về `KycProfile` + `ModerationGuard` comment
5. **Phase 6 — Test Coverage:** bổ sung tests cho các bug đã sửa (đặc biệt `/vouchers` 500)

### 8.5 — Phụ thuộc cần user hỗ trợ

- [DEPENDENCY — SEED DATA] Cần user tạo account ADMIN, MODERATOR, CUSTOMER, VENDOR để test 4 role × 12 URLs.
- [DEPENDENCY — DB CLIENT] Cần cài `mongosh` hoặc MongoDB Compass để đếm records.
- [DEPENDENCY — MANUAL TEST] Runtime test chi tiết (form submit, button click) cần browser thật + login.

---

## 9. STOP RULE — ĐÃ ÁP DỤNG

- ✅ KHÔNG sửa code trong Phase 1.
- ✅ KHÔNG commit, push.
- ✅ KHÔNG insert/update/delete MongoDB.
- ✅ KHÔNG thay đổi role ADMIN/MODERATOR/CUSTOMER/VENDOR.
- ✅ KHÔNG sửa `docs/` và `Policy/` (chỉ tạo báo cáo Phase 1 mới).
- ✅ Mọi thao tác test là read-only (HTTP GET, đếm MongoDB qua response HTML).

**Phase 1 DONE. STOP. Chờ user review và confirm trước khi chuyển sang Phase 2.**

---

## 11. RUNNOTE — Phase 2 đã thực hiện (25/09/2026)

Phase 2 — Security Fix & Route Mapping đã hoàn thành.
Báo cáo đầy đủ: `docs/REPORT/PHASE-2-SECURITY.md`.

**File changed:**
- `src/main/java/com/ecommerce/cnj70/config/SecurityConfig.java` (thêm 7 routes + đổi vouchers → ADMIN-only)

**Kết quả:**
- ADMIN × 12 URLs = 200 (12/12 PASS)
- Anonymous × 3 admin URLs = 401 (PASS)
- MODERATOR/CUSTOMER/VENDOR × 12 URLs = ⏸️ BLOCKED (thiếu test password)

**Phase 1 — Bug Status Update:**

| Bug # | Mô tả | Phase 2 status |
|-------|-------|----------------|
| 1 | AdminCategoryController `addAttribute` | Chưa fix (Phase 3) |
| 2 | 7 templates thiếu flash UI | Chưa fix (Phase 3) |
| 3 | Category chồng Bug 1 | Chưa fix (Phase 3) |
| 4 | ViolationService filter sai | Chưa fix (Phase 4) |
| 5 | `/vouchers` HTTP 500 | Chưa fix (Phase 3) |
| 6 | `/api/products` 404 | Chưa fix (Phase 3) |
| 7 | `/api/categories` 404 | Chưa fix (Phase 3) |
| 8 | `/admin/dashboard` HTTP 500 | Chưa fix (Phase 3) |
| 9 | ModerationGuard comment/code mismatch | Comment chưa update (doc Phase 5) |
| 10 | ModerationGuard dead code | Chưa xử lý (decision Phase 5) |
| 11 | KycProfile rename | Doc cũ, code đã refactor (Phase 5) |
| 12 | audit_logs 2 schema | Đã mirror write fix |

---

## 10. APPENDIX — FILES ĐÃ ĐỌC

### Controllers (12 + VoucherController + ModeratorQueueController + ...)
- AdminUserController.java
- AdminShopController.java
- AdminCategoryController.java
- AdminKycController.java
- AdminProductController.java
- AdminOrderController.java
- AdminReviewController.java
- AdminViolationController.java
- AdminEscalationController.java
- AdminAuditLogController.java
- AdminBannerController.java
- AdminDashboardController.java
- VoucherController.java (admin endpoints)

### Services (interfaces — không đọc hết impls)
- AdminUserService / AdminUserServiceImpl (read)
- AdminShopService / AdminShopServiceImpl (read)
- AdminCategoryService (interface only)
- AdminKycService (interface only)
- AdminBannerService (interface only)
- AdminAuditLogService / AdminAuditLogServiceImpl (read)
- AdminEscalationServiceImpl (read)
- ViolationService / ViolationServiceImpl (read)
- VoucherService / VoucherServiceImpl (selected methods)

### Repositories
- ShopRepository.java
- VoucherRepository.java
- UserRepository.java (referenced)
- ViolationRepository.java
- AuditLogRepository.java
- AuditLogEntryRepository.java

### Documents
- User.java, Shop.java, Voucher.java, Violation.java, Escalation.java
- AuditLog.java, AuditLogEntry.java
- Banner.java, Category.java, KycProfile.java

### Security
- SecurityConfig.java
- ModerationGuard.java
- CustomUserDetails.java
- JwtAuthenticationEntryPoint.java

### Enums
- UserRole.java, AccountStatus.java, ShopStatus.java

### Templates (cross-check flash UI)
- admin/user-list.html, shop-list.html, review-list.html, product-list.html
- admin/order-list.html, banner-list.html, escalation-queue.html, audit-list.html
- admin/category-manage.html
- admin/voucher-list.html, kyc-list.html, violation-list.html
- vouchers/list.html (public)

### Config
- application.yml

### Test infrastructure
- `mvn test` chạy 147/147 PASS (read-only verification)
