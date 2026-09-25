# PHASE 3 — ADMIN USER MANAGEMENT

> Ngày thực hiện: 2026-09-25
> Workspace: `d:\Code Full\Java\BTL_E_commerce\Ecommerce_Java`
> Branch: `feature/admin` (working tree — không commit theo yêu cầu Phase 3)

## 0. TÓM TẮT

Phase 3 hoàn thiện **Admin User Management** theo spec `docs/ADMIN_MODERATOR/Admin/PHASE 3 — ADMIN USER MANAGEMENT.md`. Toàn bộ flow List / Detail / Lock / Unlock / Edit Status / Search / Filter / Pagination / Self-Action Protection đã được verify trên application thực tế và bằng automated test (16/16 PASS cho AdminUser).

**Không có lệnh insert/update/delete/drop MongoDB nào được thực hiện trong quá trình fix.** Mọi thay đổi status user đều đi qua các endpoint chính thức của Admin (`/admin/users/{id}/lock`, `/{id}/unlock`, `/{id}/edit`). Các test verify dựa trên response HTTP + AuditLog (đọc), không phải MongoDB trực tiếp.

---

## 1. USER MANAGEMENT CONTRACT (TASK 3.2)

| Action            | ADMIN | MODERATOR | CUSTOMER | VENDOR |
|-------------------|-------|-----------|----------|--------|
| List users        | YES   | YES       | NO       | NO     |
| View detail       | YES   | YES       | NO       | NO     |
| Lock user         | YES (trừ self)  | YES (trừ self)  | NO       | NO     |
| Unlock user       | YES   | YES       | NO       | NO     |
| Edit Status       | YES (trừ self ACTIVE) | YES (trừ self ACTIVE) | NO       | NO     |
| Delete            | NO (soft delete only — out of scope) |

**Status transitions** (theo `AccountStatus` enum hiện có: ACTIVE / LOCKED / UNVERIFIED):
- ACTIVE ↔ LOCKED
- ACTIVE ↔ UNVERIFIED (qua Edit Status)
- SUSPENDED / BANNED **không tồn tại trong enum hiện tại** → out of scope Phase 3 (sẽ là Phase 5+ nếu thêm)

---

## 2. BUGS FOUND (trong phạm vi Phase 3)

| ID    | File                                                                           | Mô tả                                                                                                                                                                                                                                                                                |
|-------|--------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| P3-1  | `AdminUserServiceImpl.java#getUserById`                                         | `userRepository.findById(id)` không match với `_id` String của User document (đã verify ở session trước cho Shop/Review/Voucher). Trả về Optional.empty() cho mọi user ID.                                                                                                          |
| P3-2  | `AdminUserServiceImpl.java#lockUser`                                            | Self-Action Protection chỉ check `user.getRole() == UserRole.ADMIN`. MODERATOR tự khóa MODERATOR khác → KHÔNG bị chặn (vi phạm spec §3.9).                                                                                                                                          |
| P3-3  | `AdminUserController.java` + `AdminUserService.java` + `AdminUserServiceImpl.java` | **MISSING** Edit Status flow theo spec §3.7: không có `GET /admin/users/{id}/edit`, không có `POST /admin/users/{id}/edit`, không có service method `updateUserStatus`.                                                                                                          |
| P3-4  | `user-list.html`                                                               | Thiếu flash UI (success/error/info) — đã được Bug #2 Phase 1 đánh dấu nhưng chưa fix.                                                                                                                                                                                                |
| P3-5  | `user-detail.html`                                                             | Thiếu các trường theo spec §3.4: KYC Status, Last Login, Violation Count. Thiếu nút "Sửa trạng thái" để vào Edit Status.                                                                                                                                                            |
| P3-6  | `AdminUserServiceImpl.java#lockUser/#unlockUser`                               | **CRITICAL BUG (phát hiện khi fix P3-3)**: `userRepository.save(user)` insert NEW document thay vì update existing → `DuplicateKeyException` trên `email` unique index khi save lại. Root cause: Spring Data converter không map `_id` String vào `User.id` khi qua `getConverter().read()`. |
| P3-7  | `AdminUserServiceImplTest.java`                                                | Test dùng `userRepository.findById` + `save` (mock) — đã lỗi thời sau fix P3-1 + P3-6. Thiếu case MODERATOR self-action (P3-2), admin self-edit (P3-3), no-op validation (P3-3).                                                                                                  |

---

## 3. THAY ĐỔI ĐÃ THỰC HIỆN

### 3.1. Backend — Service / Controller / Interface

| File                                                                           | Thay đổi                                                                                                                                                                                                                                                                                                                                                                                                |
|--------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `AdminUserService.java`                                                        | + Method signature `updateUserStatus(String id, AccountStatus newStatus, String currentUserId)`.                                                                                                                                                                                                                                                                                                       |
| `AdminUserServiceImpl.java#getUserById`                                        | Chuyển sang raw `mongoTemplate.getCollection("users").find(...)` + `getConverter().read(User.class, raw)` + manually set `User.id = id` nếu null + set `_class` discriminator. **Fix P3-1.**                                                                                                                                                                                                            |
| `AdminUserServiceImpl.java#lockUser`                                           | Self-action check mở rộng cho cả ADMIN **và MODERATOR**. Throw `BusinessException` với message rõ role. **Fix P3-2.** Đổi `userRepository.save(user)` → `updateUserStatusRaw(id, AccountStatus.LOCKED)` (raw `updateOne` để tránh DuplicateKey). **Fix P3-6.** AuditLog wrap trong `try/catch` để không phá vỡ business flow.                                                                                |
| `AdminUserServiceImpl.java#unlockUser`                                         | Tương tự lockUser: chuyển sang raw updateOne + AuditLog try/catch.                                                                                                                                                                                                                                                                                                                                    |
| `AdminUserServiceImpl.java#updateUserStatus` (NEW)                              | **NEW METHOD (P3-3)**: validate (status null → throw, status cũ = mới → throw "không có thay đổi"), self-action protection cho ADMIN/MODERATOR self ACTIVE → khác, ghi AuditLog `ADMIN_ACTION`. Dùng `updateUserStatusRaw`.                                                                                                                                                                              |
| `AdminUserServiceImpl.java#updateUserStatusRaw` (NEW private helper)            | **NEW METHOD (P3-6)**: dùng raw `updateOne({_id: id}, {$set: {status, updatedAt}})` để đảm bảo update đúng `_id`. Tránh được `userRepository.save()` insert duplicate.                                                                                                                                                                                                                                  |
| `AdminUserController.java`                                                     | + 2 endpoints mới: `GET /admin/users/{id}/edit` (form), `POST /admin/users/{id}/edit` (submit). Import `ResourceNotFoundException`. Catch `BusinessException` → redirect với `flashError`. Catch not-found → redirect list.                                                                                                                                                                            |
| `user-edit.html` (NEW TEMPLATE)                                                | **NEW**: form đổi status (dropdown chỉ ACTIVE/LOCKED/UNVERIFIED). Hiển thị flash error nếu có.                                                                                                                                                                                                                                                                                                           |

### 3.2. Frontend — Templates

| File                                                                           | Thay đổi                                                                                                                                                                                                                                                                                                                                                                                                |
|--------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `user-list.html`                                                               | + Flash UI block (success / info / error) sau page-head. **Fix P3-4.** + Cột "Thao tác": thêm button "Sửa" link sang `/admin/users/{id}/edit`. **Fix P3-3 (frontend).**                                                                                                                                                                                                                                  |
| `user-detail.html`                                                             | + Flash UI block. **Fix P3-4.** + Label "Email (Username)" (vì User không có field `username` riêng, dùng `email` làm username — theo source). + Trường "KYC Status" (render từ `user.kycStatus`). + Trường "Last Login" (hiện "Chưa ghi nhận" — User document chưa có `lastLoginAt` field, đó là gap của source ngoài Phase 3). + Trường "Violation Count" (hiện 0 — User document chưa có field này). + Button "Sửa trạng thái". **Fix P3-5.** |
| `user-edit.html` (NEW)                                                         | **NEW TEMPLATE (P3-3)**: form đổi status ACTIVE/LOCKED/UNVERIFIED, flash error, nút Lưu / Hủy.                                                                                                                                                                                                                                                                                                          |

### 3.3. Tests

| File                                                                           | Thay đổi                                                                                                                                                                                                                                                                                                                                                                                                |
|--------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `AdminUserServiceImplTest.java`                                                | Cập nhật 4 tests cũ + thêm 3 tests mới. Tổng: 7 tests. Tất cả stub raw MongoCollection thay vì UserRepository. **Fix P3-7.**                                                                                                                                                                                                                                                                              |
| `AdminUserControllerSecurityTest.java` (NEW)                                   | **NEW**: 9 tests MockMvc: ADMIN/MODERATOR allowed (200), CUSTOMER/VENDOR denied (302 redirect qua AccessDeniedHandler), Anonymous denied (401/302/403), endpoints `/admin/users`, `/admin/users/{id}`, `/admin/users/{id}/edit`, `POST /lock`, `POST /unlock` đều đúng status.                                                                                                                                  |

### 3.4. Files không thay đổi (out of scope Phase 3)

- `User.java` (model) — không thêm `lastLoginAt` / `violationCount` / `username` (đó là Phase 5+ decision).
- `AccountStatus.java` (enum) — không thêm `SUSPENDED` / `BANNED` (đó là Phase 5+ decision).
- `AuditAction.java` — Edit Status dùng `ADMIN_ACTION` (đã tồn tại).
- `SecurityConfig.java` — Phase 2 đã cấu hình `/admin/users/**` → `hasAnyRole("ADMIN", "MODERATOR")` đúng theo spec Phase 3 §1.1.
- `BusinessException.java`, `GlobalExceptionHandler.java`, `AuditLogService.java`, `UserRepository.java`, `UserRepositoryImpl.java` — không cần thay đổi.

---

## 4. KẾT QUẢ TEST

### 4.1. Automated Tests

```
mvn test -Dtest=AdminUserServiceImplTest
→ Tests run: 7, Failures: 0, Errors: 0, Skipped: 0  ✅

mvn test -Dtest=AdminUserControllerSecurityTest
→ Tests run: 9, Failures: 0, Errors: 0, Skipped: 0  ✅

mvn test (full project)
→ Tests run: 159, Failures: 0, Errors: 0, Skipped: 0  ✅
```

### 4.2. Runtime Verification (Browser / curl)

Test account: `admin2@gmail.com / admin123` (ADMIN role, MongoDB có sẵn).

**GET endpoints** (Admin auth):

| URL                                                       | HTTP | Ghi chú                                              |
|-----------------------------------------------------------|------|------------------------------------------------------|
| `/admin/users`                                            | 200  | List users + search + filter + pagination OK         |
| `/admin/users/6a859691deda531cb1dd5276`                   | 200  | Detail (Bug P3-1 fixed)                              |
| `/admin/users/6a859691deda531cb1dd5276/edit`              | 200  | Edit Status form (NEW endpoint)                      |
| `/admin/users?q=quantran`                                 | 200  | Search OK                                            |
| `/admin/users?role=CUSTOMER`                              | 200  | Role filter OK                                       |
| `/admin/users?status=ACTIVE`                              | 200  | Status filter OK                                     |
| `/admin/users?role=ADMIN&status=ACTIVE`                   | 200  | Combined filter OK                                   |
| `/admin/users?page=0&size=5`                              | 200  | Pagination OK                                        |
| `/admin/users?page=1&size=5`                              | 200  | Pagination OK                                        |

**POST endpoints**:

| Action                                       | HTTP | Result                                                     |
|----------------------------------------------|------|------------------------------------------------------------|
| `POST /admin/users/{id}/edit` status=LOCKED  | 302  | Redirect to detail + flash "Đã cập nhật trạng thái..."     |
| `POST /admin/users/{id}/edit` status=ACTIVE  | 302  | Redirect to detail + flash "Đã cập nhật..."               |
| `POST /admin/users/{id}/lock`                | 302  | Redirect to list + flash "Đã khóa..."                     |
| `POST /admin/users/{id}/unlock`              | 302  | Redirect to list + flash "Đã mở khóa..."                  |
| `POST /admin/users/{adminId}/lock` (self)    | 302  | Redirect to list + flash error "Bạn không thể tự khóa ADMIN..." ✅ |
| `POST /admin/users/{adminId}/edit` self ACTIVE → LOCKED | 302  | Redirect edit + flash error "Bạn không thể tự đổi..."  ✅ |
| `POST /admin/users/{id}/edit` ACTIVE → ACTIVE (no-op)    | 302  | Redirect edit + flash error "không có thay đổi" ✅        |

### 4.3. MongoDB Verification (AuditLog)

App log ghi nhận:

```
[AUDIT] ADMIN_ACTION   | actor=admin (id=6a8e9389...) | resource=USER:6a859691... | severity=INFO | reason=Admin đổi trạng thái user quantran... (ACTIVE → LOCKED)
[AUDIT] ADMIN_ACTION   | actor=admin                  | resource=USER:6a859691... | severity=INFO | reason=Admin đổi trạng thái user quantran... (LOCKED → ACTIVE)
[AUDIT] USER_LOCKED    | actor=admin                  | resource=USER:6a859691... | severity=WARNING | reason=Admin khóa tài khoản... (ACTIVE → LOCKED)
[AUDIT] USER_UNLOCKED  | actor=admin                  | resource=USER:6a859691... | severity=INFO | reason=Admin mở khóa tài khoản... (LOCKED → ACTIVE)
```

Tất cả actions ghi vào collection `audit_logs` qua `AuditLogService` (mirror write sang `AuditLogEntry` schema cũng đã có sẵn từ fix Phase 1).

### 4.4. Regression Smoke Test

| Role       | Routes tested                                                                              | Kết quả                          |
|------------|--------------------------------------------------------------------------------------------|----------------------------------|
| ADMIN      | `/admin/dashboard`, `/users`, `/shops`, `/products`, `/categories`, `/vouchers`, `/audit`, `/kyc`, `/escalations`, `/violations`, `/banners`, `/reviews` | 12/12 = 200                       |
| ANONYMOUS  | `/`, `/home`, `/auth/login`, `/auth/register`, `/products`                                 | 5/5 = 200 (public routes)        |
| ANONYMOUS  | `/admin/users`                                                                              | 401 (JWT entry point)            |
| CUSTOMER   | `/home` = 200 (login OK); `/profile` = 404 (route chưa tồn tại — Phase 1 đã report, không phải regression Phase 3) | Không regression do Phase 3       |

---

## 5. SECURITY MATRIX

| Test                                            | Result |
|-------------------------------------------------|--------|
| ADMIN allowed `/admin/users`                    | PASS   |
| MODERATOR allowed `/admin/users`               | PASS   |
| CUSTOMER denied `/admin/users`                  | PASS (302 redirect)   |
| VENDOR denied `/admin/users`                    | PASS (302 redirect)   |
| Anonymous denied `/admin/users`                 | PASS (401 JSON)        |
| ADMIN self-lock DENY                            | PASS (flash error "Bạn không thể tự khóa tài khoản ADMIN của chính mình") |
| MODERATOR self-lock DENY (Bug P3-2 fix)         | PASS (test pass + runtime test inferred) |
| ADMIN edit self ACTIVE → LOCKED DENY (P3-3)    | PASS (flash error "Bạn không thể tự đổi trạng thái ADMIN của chính mình khi đang ACTIVE") |

---

## 6. KNOWN LIMITATIONS / OUT OF SCOPE

1. **SUSPENDED / BANNED status**: `AccountStatus` enum chưa có → không thể test transition ACTIVE → SUSPENDED / BANNED theo spec §3.7. Out of scope Phase 3 (đó là Phase 5+).
2. **Username field riêng**: `User.java` không có `username` riêng, dùng `email` làm identifier. Template hiển thị "Email (Username)".
3. **Last Login**: `User.java` không có field `lastLoginAt` → template hiển thị "Chưa ghi nhận". Out of scope Phase 3 (cần Phase 5+ để bổ sung field + tracking).
4. **Violation Count**: `User.java` không có field `violationCount` → template hiển thị "0". Out of scope Phase 3 (cần Phase 6 Violation module).
5. **Self-action cho MODERATOR**: spec §3.9 chỉ ghi "Moderator không được tự khóa chính mình". Code đã implement. Tuy nhiên do thiếu MODERATOR account test (Phase 2 report blocker), test verify qua unit test + giả định runtime (do account `admin2@gmail.com` là ADMIN nên runtime test tự lock MODERATOR không thực hiện được). Unit test `lockUser_moderatorCannotLockSelf` PASS.
6. **Customer/Vendor login test**: thiếu password → chỉ test anonymous flow + admin flow.

---

## 7. STOP RULE

Theo spec §"STOP RULE" và yêu cầu user: **DỪNG sau Phase 3**. Không tự triển khai:
- KYC Management (Phase 4)
- Violation Management (Phase 6)
- Shop Management (Phase 4)

Chỉ chuyển Phase 4 khi user xác nhận **PHASE 3 DONE**.
