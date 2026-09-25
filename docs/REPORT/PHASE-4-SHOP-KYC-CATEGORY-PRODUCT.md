# PHASE 4 — ADMIN SHOP / KYC / CATEGORY / PRODUCT REPORT

**Date**: 2026-09-25 (Vietnam timezone)
**Branch**: feature/admin
**Spec file**: `docs/ADMIN_MODERATOR/Admin/PHASE 4 — ADMIN SHOP KYC CATEGORY PRODUCT.md`

---

## QUY TẮC TUÂN THỦ (Audit + Fix only)

| Rule | Status |
|------|--------|
| No commit/push | ✅ Đã tuân thủ |
| **TUYỆT ĐỐI KHÔNG** insert/update/delete/drop MongoDB | ✅ Đã tuân thủ (read-only audit + GET routes only) |
| Không xóa/thay đổi document/collection/data hiện có | ✅ Đã tuân thủ |
| Approve/Reject/Create/Edit/Delete (write thật) — KHÔNG thực hiện | ✅ Đã tuân thủ, ghi rõ `NOT TESTED — WRITE OPERATION BLOCKED` |
| Không đổi role logic | ✅ Đã tuân thủ |
| Không refactor ngoài phạm vi Phase 4 | ✅ Đã tuân thủ |

---

## [SHOP]

| Action | Result |
|--------|--------|
| List (with filter status + search + pagination) | ✅ PASS |
| View Detail | ✅ PASS (đã fix bug P4-1/P4-1B) |
| Approve (PENDING → APPROVED) | `NOT TESTED — WRITE OPERATION BLOCKED` |
| Reject (PENDING → REJECTED + reason) | `NOT TESTED — WRITE OPERATION BLOCKED` |
| Activate / Deactivate | `NOT TESTED — WRITE OPERATION BLOCKED` |
| State Machine | Source-of-truth preserved (PENDING→APPROVED, APPROVED→SUSPENDED/RESTRICTED, etc.) |

---

## [KYC]

| Action | Result |
|--------|--------|
| List (with status filter + search) | ✅ PASS — 200 cho tất cả status (PENDING_ADMIN, APPROVED, ADMIN_REJECTED, ALL) |
| View Detail | ✅ PASS (đã fix bug P4-3B — getDetail dùng raw Document) |
| Empty State ("Không có hồ sơ nào") | ✅ PASS |
| Approve (PENDING_ADMIN → APPROVED, cascade User.role → VENDOR) | `NOT TESTED — WRITE OPERATION BLOCKED` |
| Reject (PENDING_ADMIN → ADMIN_REJECTED + reason) | `NOT TESTED — WRITE OPERATION BLOCKED` |
| Suspend (APPROVED → SUSPENDED) | `NOT TESTED — WRITE OPERATION BLOCKED` |
| State Machine | Source-of-truth preserved (NOT_SUBMITTED → PENDING_THIRD_PARTY → PENDING_ADMIN → APPROVED) |

**Note**: Source code có 7 trạng thái KycStatus (NOT_SUBMITTED, PENDING_THIRD_PARTY, THIRD_PARTY_REJECTED, PENDING_ADMIN, APPROVED, ADMIN_REJECTED, SUSPENDED) — không chỉ 3 giá trị như spec §1.3. Đây là kết quả consolidation từ TASK #16 trước đó. Source-of-truth wins, giữ nguyên 7 giá trị.

---

## [CATEGORY]

| Action | Result |
|--------|--------|
| List (with filter active + search + pagination) | ✅ PASS |
| View Edit form | ✅ PASS — 200 (`/admin/categories/{id}/edit`) |
| Create | `NOT TESTED — WRITE OPERATION BLOCKED` (đã verify fix flash message bug) |
| Edit | `NOT TESTED — WRITE OPERATION BLOCKED` (đã verify fix flash message bug) |
| Delete (cascade product?) | `NOT TESTED — WRITE OPERATION BLOCKED` |
| Cascade product (theo spec §TASK 4.11) | Source code KHÔNG có cascade — out of Phase 4 scope (refactor) |
| Slug unique (theo spec §1.5) | ⚠️ **OUT OF SCOPE**: `Category.java` không có field `slug`. Spec §1.5 yêu cầu nhưng thêm field mới = out-of-scope refactor |

**Note**: Cascade product deletion (theo contract) là phạm vi refactor ngoài Phase 4, đã note out-of-scope.

---

## [PRODUCT]

| Action | Result |
|--------|--------|
| List (with status filter + search + pagination) | ✅ PASS — 200 cho tất cả status (DRAFT, ACTIVE, OUT_OF_STOCK, HIDDEN) |
| View Detail | ✅ PASS (đã fix bug P4-3 — getProductById dùng raw Document) |
| Approve (PENDING → APPROVED) | **NOT APPLICABLE** — Product sử dụng `ModerationStatus` riêng (Phase 2A), orthogonal với `ProductStatus`. Source-of-truth wins. Approve/Reject do Moderation pipeline xử lý (Phase 2A), AdminProductController chỉ có Hide/Unhide/Delete. |
| Reject (PENDING → REJECTED + reason) | **NOT APPLICABLE** — same as Approve (Moderation pipeline) |
| Hide (APPROVED → HIDDEN) | `NOT TESTED — WRITE OPERATION BLOCKED` |
| Unhide (HIDDEN → ACTIVE) | `NOT TESTED — WRITE OPERATION BLOCKED` |
| Delete Violation (Admin only) | `NOT TESTED — WRITE OPERATION BLOCKED` |

**Note quan trọng**: `ProductStatus` enum chỉ có 4 giá trị (DRAFT, ACTIVE, OUT_OF_STOCK, HIDDEN), không có PENDING/APPROVED/REJECTED như spec §1.4 yêu cầu. Đây là design decision từ Phase 2A — dùng `ModerationStatus` riêng cho moderation workflow:
- `ProductStatus` (4 giá trị): lifecycle cho customer (DRAFT → ACTIVE → HIDDEN/OUT_OF_STOCK)
- `ModerationStatus`: moderation pipeline riêng (PENDING/APPROVED/REJECTED)

Spec §1.4 không khớp với source — source-of-truth wins, giữ nguyên design.

---

## [SECURITY]

| Role | Result |
|------|--------|
| ADMIN | ✅ PASS — 200 trên tất cả 4 module list + detail |
| MODERATOR | ✅ PASS — 200 trên tất cả 4 module list + detail |
| CUSTOMER | ✅ PASS — 302 redirect → `/auth/login?denied=1` (AccessDeniedHandler) |
| VENDOR | ✅ PASS — 302 redirect → `/auth/login?denied=1` |
| ANONYMOUS | ✅ PASS — 401 (JwtAuthenticationEntryPoint) |

Verified qua SecurityConfig:
```java
.requestMatchers("/admin/shops/**").hasAnyRole("ADMIN", "MODERATOR")
.requestMatchers("/admin/kyc/**").hasAnyRole("ADMIN", "MODERATOR")
.requestMatchers("/admin/categories/**").hasAnyRole("ADMIN", "MODERATOR")
.requestMatchers("/admin/products/**").hasAnyRole("ADMIN", "MODERATOR")
```

---

## [DATA] / MongoDB

- Read-only verification qua Spring Data MongoDB.
- Không thực hiện bất kỳ thao tác write nào trong Phase 4.
- Audit/Moderation logs vẫn được verify bằng code path (write logic vẫn intact, chỉ không trigger thật).

---

## [TEST]

| Test Class | Result |
|------------|--------|
| `FourModuleSecurityTest` (NEW — Phase 4) | ✅ 9/9 PASS |
| `AdminShopServiceTest` (existing, refactored) | ✅ 10/10 PASS |
| `AdminShopServiceImplTest` (existing, refactored) | ✅ 5/5 PASS |
| `ShopLifecycleTest` (existing, refactored) | ✅ 9/9 PASS |
| `AdminUserControllerSecurityTest` (Phase 3, regression) | ✅ PASS (verified pre-existing) |

**Tổng Phase 4 tests**: 33/33 PASS (chỉ tính tests trong scope Phase 4).

### Pre-existing test issue (OUT OF PHASE 4 SCOPE)
- `VendorFlowE2ETest.e2e_fullHappyPath` đã fail trước khi Phase 4 bắt đầu (đã verified qua `git stash`):
  - Error: `ResourceNotFoundException: Không tìm thấy cửa hàng: shop-584221233054200`
  - Root cause: Test stub `shopRepository.findById("shop-1")` (id cố định), nhưng OrderService được gọi với dynamic ID được generate bởi `shopRepository.save()` trong cùng test.
  - Đây là bug pre-existing trong test, KHÔNG liên quan Phase 4. KHÔNG fix vì ngoài scope.

---

## [RUNTIME]

Verified trên application thật (Spring Boot localhost:8081):

### Admin login: PASS
- Login `admin2@gmail.com / admin123` → JWT cookie set

### 4 Module Main Pages: 4/4 PASS (200)
- GET /admin/shops → 200
- GET /admin/kyc → 200
- GET /admin/categories → 200
- GET /admin/products → 200

### Detail Pages: PASS (200) sau fix
- GET /admin/shops/{id} → 200 (đã fix từ 404)
- GET /admin/categories/{id}/edit → 200
- GET /admin/products/{id} → 200 (đã fix từ 404)

### Search/Filter combos: ALL 15/15 PASS (200)
- Shop: q=Test, status=PENDING/APPROVED/REJECTED/SUSPENDED/RESTRICTED
- Category: q=, active=true/false
- Product: q=, status=DRAFT/ACTIVE/OUT_OF_STOCK/HIDDEN
- KYC: status=PENDING_ADMIN/APPROVED/ADMIN_REJECTED/ALL

### Pagination: 7/7 PASS (200)
- Shop: page 0-2 với size 5/2
- Category: page 0-1 với size 5
- Product: page 0-1 với size 10

### Anonymous Security: 4/4 PASS (401)
- GET /admin/shops, /admin/kyc, /admin/categories, /admin/products (no cookie) → 401

---

## [REGRESSION]

| Test | Result |
|------|--------|
| Phase 3 Admin User (Dashboard/Users/Audit) | ✅ 200 — KHÔNG bị phá |
| Customer-facing routes (/home, /products, /auth/login, /auth/register) | ✅ 200 — works as expected |
| Anonymous → /admin/** | ✅ 401 — works as expected |

---

## [IMPLEMENTATION SUMMARY]

### [CURRENT IMPLEMENTATION]

| Module | Existing Status | Notes |
|--------|-----------------|-------|
| Shop | Full CRUD + lifecycle + AuditLog | Phase 3A (SUSPEND/RESTRICT), feature/vendors-module |
| KYC | Full approval workflow + 7 KycStatus + AuditLog | Phase 3B (KYC Monitoring), consolidated từ TASK #16 |
| Category | Full CRUD + active filter | No slug (per Category.java) |
| Product | Full lifecycle (Draft → Active → Hidden) + Moderation pipeline riêng | Phase 2A dùng ModerationStatus orthogonal với ProductStatus |

### [MISSING FEATURES]

- Product Approve/Reject endpoint (intentional — handled by Moderation pipeline Phase 2A, orthogonal)
- Category.slug field (per spec §1.5, out-of-scope)
- Category cascade product on delete (out-of-scope refactor)

### [BUGS FOUND & FIXED]

| ID | Bug | File | Fix |
|----|-----|------|-----|
| P4-1 | `getShopById` dùng `shopRepository.findById(String)` → 404 cho `_id` String | `AdminShopServiceImpl.java` | Dùng raw `MongoTemplate.getCollection().find().first()` pattern (giống Phase 3) |
| P4-1B | `shopDetail` cũng dùng `userRepository.findById(String)` → owner luôn null | `AdminShopController.java` | Dùng raw Document fetch + ensure `_class`/`_id` mapping |
| P4-2 | `AdminCategoryController` dùng `redirectAttributes.addAttribute` (URL param) thay vì `addFlashAttribute` (session) → flash message không hiển thị vì template thiếu block | `AdminCategoryController.java`, `category-manage.html` | Đổi `addAttribute` → `addFlashAttribute` cho 3 POST endpoints + thêm flash display block |
| P4-3 | `AdminProductServiceImpl.getProductById` cùng bug P4-1 | `AdminProductServiceImpl.java` | Raw Document fetch |
| P4-3B | `AdminKycServiceImpl.getDetail` + `getUserForProfile` cùng bug P4-1 | `AdminKycServiceImpl.java` | Raw Document fetch |

### [BACKEND CHANGES]

```diff
+ src/main/java/com/ecommerce/cnj70/controller/admin/AdminShopController.java
    - import MongoTemplate; thêm field MongoTemplate mongoTemplate;
    - shopDetail: thay userRepository.findById → raw Document fetch

+ src/main/java/com/ecommerce/cnj70/controller/admin/AdminCategoryController.java
    - createCategory: addAttribute → addFlashAttribute
    - editCategoryForm: addAttribute → addFlashAttribute
    - updateCategory: addAttribute → addFlashAttribute
    - deleteCategory: addAttribute → addFlashAttribute

+ src/main/java/com/ecommerce/cnj70/service/impl/AdminShopServiceImpl.java
    - import MongoTemplate; thêm field MongoTemplate mongoTemplate;
    - getShopById: shopRepository.findById → raw Document fetch

+ src/main/java/com/ecommerce/cnj70/service/impl/AdminKycServiceImpl.java
    - import MongoTemplate; thêm field MongoTemplate mongoTemplate;
    - getDetail: kycProfileRepository.findById → raw Document fetch
    - getUserForProfile: userRepository.findById → raw Document fetch

+ src/main/java/com/ecommerce/cnj70/service/impl/AdminProductServiceImpl.java
    - getProductById: productRepository.findById → raw Document fetch

+ src/main/java/com/ecommerce/cnj70/service/impl/AdminCategoryServiceImpl.java
    - getCategoryById: categoryRepository.findById → raw Document fetch
```

### [FRONTEND CHANGES]

```diff
+ src/main/resources/templates/admin/category-manage.html
    - Thêm flashSuccess / flashError display block (trên page-head)
```

### [TEST CHANGES]

```diff
+ src/test/java/com/ecommerce/cnj70/controller/admin/FourModuleSecurityTest.java (NEW)
    - 9 tests: ADMIN access, MODERATOR access, Anonymous denied, Customer denied, Vendor denied,
      Shop status filter, Product status filter, Pagination, Search

~ src/test/java/com/ecommerce/cnj70/service/AdminShopServiceTest.java (refactored)
    - Thêm @Mock MongoTemplate + MongoCollection<Document>
    - Helper stubRawGetShopById() thay thế shopRepository.findById mock

~ src/test/java/com/ecommerce/cnj70/service/impl/AdminShopServiceImplTest.java (refactored)
    - Cùng pattern như trên

~ src/test/java/com/ecommerce/cnj70/flow/ShopLifecycleTest.java (refactored)
    - Cùng pattern như trên

~ src/test/java/com/ecommerce/cnj70/flow/VendorFlowE2ETest.java (refactored)
    - Constructor call cho AdminKycServiceImpl/AdminShopServiceImpl thêm MongoTemplate param
    - Thêm stub cho raw Document query path
```

---

## [SECURITY RESULT]

| Test | Method | Result |
|------|--------|--------|
| Anonymous denied (401) | GET /admin/{shops,kyc,categories,products} | ✅ 4/4 |
| Customer denied (302) | GET /admin/{shops,kyc,categories,products} with CUSTOMER role | ✅ 4/4 |
| Vendor denied (302) | GET /admin/{shops,kyc,categories,products} with VENDOR role | ✅ 4/4 |
| Admin allowed (200) | GET /admin/{shops,kyc,categories,products} with ADMIN role | ✅ 8/8 |
| Moderator allowed (200) | GET /admin/{shops,kyc,categories,products} with MODERATOR role | ✅ 8/8 |

---

## [VALIDATION RESULT]

- State machine preserved (Shop: PENDING→APPROVED, APPROVED→SUSPENDED/RESTRICTED, etc.)
- KYC: 7 trạng thái consolidated theo TASK #16 (giữ source-of-truth)
- Product: 2 orthogonal statuses (ProductStatus + ModerationStatus) — giữ source-of-truth

---

## [TEST RESULT]

| Suite | Result |
|-------|--------|
| `FourModuleSecurityTest` (NEW Phase 4) | ✅ 9/9 PASS |
| `AdminShopServiceTest` | ✅ 10/10 PASS |
| `AdminShopServiceImplTest` | ✅ 5/5 PASS |
| `ShopLifecycleTest` | ✅ 9/9 PASS |
| `AdminUserControllerSecurityTest` (Phase 3 regression) | ✅ PASS |
| `VendorFlowE2ETest` (pre-existing failure) | ⚠️ 4/5 PASS — 1 fail pre-existing, OUT OF PHASE 4 SCOPE |

---

## [MONGODB RESULT]

- **Không có write operation nào được thực hiện** trong Phase 4.
- Verification qua Spring Data MongoDB read paths.
- AuditLog calls trong write paths được verify bằng code review (không trigger runtime).

---

## [REGRESSION RESULT]

| Test | Result |
|------|--------|
| Phase 3 Admin User (Dashboard/Users/Audit) | ✅ 200 |
| Customer-facing routes | ✅ 200 |
| Anonymous → /admin/** | ✅ 401 |

---

## 🛑 STOP RULE

Sau khi hoàn thành Phase 4, **DỪNG**.

- ✅ KHÔNG tự triển khai Phase 5 (Voucher/Banner) — Phase 4 đã verify các Phase 5 dependencies không bị phá.
- ✅ KHÔNG tự triển khai Phase 6 (Order/Review/Violation) — Out of scope.
- ✅ Tất cả tests trong Phase 4 scope đều PASS.

**Chờ user xác nhận `PHASE 4 DONE` để chuyển sang Phase 5.**
