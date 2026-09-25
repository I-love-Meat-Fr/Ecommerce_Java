# PHASE 2 — SECURITY FIX & ROUTE MAPPING

## 🎯 Mục tiêu Phase 2

Sửa các lỗi 403 sai, route thiếu và access denied handler dựa trên báo cáo Phase 1.

Phase 2 không refactor toàn bộ Security.

```
Existing Security Implementation
        ↓
Audit từ Phase 1
        ↓
Sửa SecurityConfig
        ↓
Sửa ModerationGuard
        ↓
Thêm AccessDeniedHandler nếu thiếu
        ↓
Test lại với 4 role
        ↓
SECURITY PASS
```

Luồng Phase 2:

```
USER
  ↓
SecurityConfig (filter chain)
  ↓
  ├── ADMIN → allow
  ├── MODERATOR → allow (trừ audit)
  ├── CUSTOMER → deny
  └── VENDOR → deny

Anonymous
  ↓
AuthenticationEntryPoint
  ↓
Redirect /auth/login
```

---

## 1. SECURITY CONTRACT

### 1.1 — Role × Module Matrix

```
Module                  ADMIN  MODERATOR  CUSTOMER  VENDOR
/admin/users            ALLOW  ALLOW      DENY      DENY
/admin/categories       ALLOW  ALLOW      DENY      DENY
/admin/vouchers         ALLOW  DENY       DENY      DENY
/admin/shops            ALLOW  ALLOW      DENY      DENY
/admin/kyc              ALLOW  ALLOW      DENY      DENY
/admin/violations       ALLOW  ALLOW      DENY      DENY
/admin/orders           ALLOW  DENY       DENY      DENY
/admin/products         ALLOW  ALLOW      DENY      DENY
/admin/reviews          ALLOW  ALLOW      DENY      DENY
/admin/escalations      ALLOW  ALLOW      DENY      DENY
/admin/audit            ALLOW  DENY       DENY      DENY
/admin/banners          ALLOW  DENY       DENY      DENY
```

### 1.2 — Phân quyền Service Layer

```
ModerationGuard.assertCanModerate()
        ↓
Allow: ADMIN, MODERATOR
Deny: CUSTOMER, VENDOR, Anonymous
```

### 1.3 — Anonymous Flow

```
Anonymous request vào /admin/**
        ↓
AuthenticationEntryPoint
        ↓
Redirect /auth/login?denied=1
```

---

## 2. ⚠️ ĐIỀU KIỆN BẮT ĐẦU

Phase 2 chỉ chạy khi:

```
PHASE 1 DONE
        +
docs/REPORT/PHASE-1-AUDIT.md tồn tại
        +
Có bảng lỗi 403 sai từ Phase 1
```

Nếu thiếu:

```
[DEPENDENCY — PHASE 1 REPORT]
```

---

## 3. ⚠️ PHẠM VI PHASE 2

### CHỈ LÀM

```
Sửa SecurityConfig (route mapping)
Sửa ModerationGuard (allow ADMIN)
Thêm AccessDeniedHandler nếu thiếu
Build project (KHÔNG commit)
Test lại với 4 role
Ghi báo cáo
```

### ❌ KHÔNG ĐƯỢC LÀM

```
Refactor toàn bộ Security
Đổi UserRole enum
Đổi logic role trong entity
Tạo mới Controller
Tạo mới Service
Sửa template
Test tự động
Commit
```

---

## TASK 2.1 — ĐỌC SOURCE HIỆN TẠI

### 🎯 Mục tiêu

Xác minh chính xác code Security đang có gì.

### Đọc

```
config/SecurityConfig.java
security/ModerationGuard.java
security/CustomUserDetails.java
security/JwtAuthenticationEntryPoint.java
enums/UserRole.java
```

### Ghi nhận

```
- Có AccessDeniedHandler không?
- Có AuthenticationEntryPoint không?
- Filter chain order
- CSRF / CORS config
```

### DONE

Có checklist:

```
[ ] SecurityConfig đã đọc
[ ] ModerationGuard đã đọc
[ ] UserRole đã đọc
[ ] JwtAuthenticationEntryPoint đã đọc
[ ] Xác định được route nào sai
```

---

## TASK 2.2 — SỬA SECURITY CONFIG (ROUTE MAPPING)

### 🎯 Mục tiêu

Map đúng route → role theo matrix ở mục 1.1.

### Cách làm

Mở `config/SecurityConfig.java`. Tìm method `filterChain(HttpSecurity http)`.

Trong khối `authorizeHttpRequests(auth -> auth { ... })`:

#### Bước 1 — Thêm 11 dòng TRƯỚC dòng `requestMatchers("/admin/**").hasRole("ADMIN")` (giữ nguyên dòng gốc):

```java
.requestMatchers("/admin/users/**").hasAnyRole("ADMIN", "MODERATOR")
.requestMatchers("/admin/categories/**").hasAnyRole("ADMIN", "MODERATOR")
.requestMatchers("/admin/shops/**").hasAnyRole("ADMIN", "MODERATOR")
.requestMatchers("/admin/kyc/**").hasAnyRole("ADMIN", "MODERATOR")
.requestMatchers("/admin/violations/**").hasAnyRole("ADMIN", "MODERATOR")
.requestMatchers("/admin/escalations/**").hasAnyRole("ADMIN", "MODERATOR")
.requestMatchers("/admin/products/**").hasAnyRole("ADMIN", "MODERATOR")
.requestMatchers("/admin/reviews/**").hasAnyRole("ADMIN", "MODERATOR")
.requestMatchers("/admin/orders/**").hasRole("ADMIN")
.requestMatchers("/admin/vouchers/**").hasRole("ADMIN")
.requestMatchers("/admin/audit/**").hasRole("ADMIN")
.requestMatchers("/admin/banners/**").hasRole("ADMIN")
```

#### Bước 2 — Thêm `exceptionHandling` TRƯỚC `return http.build();`:

```java
.exceptionHandling(exception -> exception
    .authenticationEntryPoint(jwtAuthenticationEntryPoint)
    .accessDeniedHandler((req, res, ex) -> {
        res.sendRedirect("/auth/login?denied=1");
    })
)
```

### Quy tắc cứng

```
✅ GIỮ NGUYÊN tất cả permitAll() hiện có
✅ GIỮ NGUYÊN dòng requestMatchers("/admin/**").hasRole("ADMIN") gốc
✅ KHÔNG đổi tên biến
✅ KHÔNG xóa rule cũ
❌ KHÔNG refactor toàn bộ method
```

### DONE

File save thành công + diff chỉ thêm dòng.

---

## TASK 2.3 — SỬA MODERATION GUARD

### 🎯 Mục tiêu

Cho phép ADMIN được moderate.

### Cách làm

Mở `security/ModerationGuard.java`.

Tìm method check role hiện tại (ví dụ `assertModerator` hoặc tên tương tự).

**GIỮ NGUYÊN TÊN METHOD** — chỉ sửa logic bên trong.

Thay điều kiện hiện tại bằng:

```java
if (role != UserRole.MODERATOR && role != UserRole.ADMIN) {
    throw new BusinessException(
        "Hành động này chỉ Admin hoặc Moderator mới được thực hiện.");
}
```

### Quy tắc cứng

```
✅ GIỮ NGUYÊN tên method
✅ GIỮ NGUYÊN signature
✅ GIỮ NGUYÊN throws clause
❌ KHÔNG thêm parameter mới
❌ KHÔNG đổi tên method
```

### DONE

Build thành công, signature không đổi.

---

## TASK 2.4 — BUILD (KHÔNG COMMIT)

### 🎯 Mục tiêu

Verify code compile được.

### Lệnh

```bash
cd "D:/Code Full/Java/BTL_E_commerce/Ecommerce_Java"
mvn -q -DskipTests clean compile
```

### DONE

```
BUILD SUCCESS
```

Nếu FAIL → fix compile error → build lại. KHÔNG commit.

---

## TASK 2.5 — RE-TEST ROLE MATRIX

### 🎯 Mục tiêu

Verify đúng matrix ở mục 1.1.

### Test

Với mỗi role × URL:

```
1. Login
2. Truy cập URL
3. Ghi nhận HTTP status:
   - 200 → OK
   - 403 → OK (nếu matrix yêu cầu DENY)
   - redirect /auth/login?denied=1 → OK (Anonymous)
```

### DONE

Bảng 48 ô:

```
| URL | ADMIN | MODERATOR | CUSTOMER | VENDOR |
|-----|-------|-----------|----------|--------|
| /admin/users | 200 | 200 | 403 | 403 |
| /admin/audit | 200 | 403 | 403 | 403 |
| /admin/vouchers | 200 | 403 | 403 | 403 |
| ... | | | | |
```

Tất cả 48 ô phải đúng matrix. Nếu sai → fix lại SecurityConfig.

---

## TASK 2.6 — TEST ANONYMOUS FLOW

### 🎯 Mục tiêu

Verify unauthenticated user redirect đúng.

### Test

```
1. Logout
2. Truy cập /admin/users
3. Phải redirect /auth/login?denied=1
```

### DONE

```
[ ] Anonymous → /admin/users → redirect /auth/login?denied=1
[ ] Anonymous → /admin/vouchers → redirect
[ ] Anonymous → /admin/audit → redirect
```

---

## TASK 2.7 — GHI BÁO CÁO PHASE 2

### 🎯 Mục tiêu

Tạo `docs/REPORT/PHASE-2-SECURITY.md`.

### DONE

File tồn tại với format:

```markdown
# PHASE 2 SECURITY REPORT — DD/MM/YYYY

## 1. ROUTE MAPPING CHANGES
[Bảng diff]

## 2. MODERATION GUARD CHANGES
[Bảng diff]

## 3. ROLE MATRIX TEST RESULT
[Bảng 48 ô]

## 4. ANONYMOUS FLOW TEST
[Checklist]

## 5. REGRESSION CHECK
- [ ] Admin Dashboard: still works
- [ ] Customer Home: still works
- [ ] Vendor Dashboard: still works

## 6. FILES CHANGED
[List đường dẫn tuyệt đối]

## 7. STATUS: PASS/FAIL
```

---

## TASK 2.8 — PHASE 2 FINAL REVIEW

### 🎯 Mục tiêu

Checklist cuối Phase 2.

### DONE Checklist

```
[ ] TASK 2.1 đã đọc hết file security
[ ] TASK 2.2 đã sửa SecurityConfig
[ ] TASK 2.3 đã sửa ModerationGuard
[ ] TASK 2.4 BUILD SUCCESS
[ ] TASK 2.5 Role Matrix test pass
[ ] TASK 2.6 Anonymous flow test pass
[ ] TASK 2.7 báo cáo đã ghi
[ ] TASK 2.8 checklist review
```

---

## 🛑 STOP RULE

Sau khi hoàn thành Phase 2:

**DỪNG.**

Không tự chuyển sang Phase 3.

Không commit.

User sẽ review diff + commit + xác nhận:

```
PHASE 2 DONE
```

mới tiếp tục:

```
PHASE 3 — ADMIN USER MANAGEMENT
```

---

## 📋 TỔNG KẾT PHASE 2

```
PHASE 2 — SECURITY FIX & ROUTE MAPPING
│
├── TASK 2.1
│   └── Đọc Source Hiện Tại
│
├── TASK 2.2
│   └── Sửa Security Config (Route Mapping)
│
├── TASK 2.3
│   └── Sửa Moderation Guard
│
├── TASK 2.4
│   └── Build (KHÔNG Commit)
│
├── TASK 2.5
│   └── Re-test Role Matrix
│
├── TASK 2.6
│   └── Test Anonymous Flow
│
├── TASK 2.7
│   └── Ghi Báo Cáo Phase 2
│
└── TASK 2.8
    └── Phase 2 Final Review
```

---

## THỨ TỰ THỰC THI

```
Đọc Source
      ↓
Sửa SecurityConfig
      ↓
Sửa ModerationGuard
      ↓
Build (không commit)
      ↓
Test 4 role × 12 URL
      ↓
Test Anonymous flow
      ↓
Ghi báo cáo
      ↓
Review checklist
      ↓
PHASE 2 DONE
```

---

## NGUYÊN TẮC PHASE 2

```
READ SOURCE FIRST
        ↓
MINIMAL CHANGE TO SECURITY
        ↓
KEEP EXISTING permitAll
        ↓
KEEP EXISTING hasRole
        ↓
ADD NEW requestMatchers BEFORE OLD ONES
        ↓
ADD exceptionHandling BEFORE build()
        ↓
KEEP MODERATION GUARD METHOD NAME
        ↓
BUILD
        ↓
TEST 4 ROLES
        ↓
TEST ANONYMOUS
        ↓
WRITE REPORT
        ↓
STOP
```

---

## Điểm quan trọng nhất

Phase 2 **KHÔNG PHẢI** là viết lại Security.

Phase 2 là **sửa đúng phần sai** dựa trên báo cáo Phase 1.

Mọi thay đổi phải tối thiểu, có mục đích, có diff rõ ràng.
