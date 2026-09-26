# CNJ70 Ecommerce — Mô tả hệ thống & Dữ liệu 5 năm (2021–2026)

> **Tài liệu đặc tả nghiệp vụ + dữ liệu** cho sàn thương mại điện tử đa nhà bán CNJ70.
> Tài liệu đi kèm bộ dữ liệu mẫu (seed) chứa **≥ 1.000 bản ghi / collection**, được
> sinh tự động bằng [`ProductionDataSeeder`](../src/main/java/com/ecommerce/cnj70/config/ProductionDataSeeder.java).

---

## 1. Tổng quan hệ thống

| Mục | Giá trị |
|---|---|
| Tên dự án | CNJ70 Ecommerce (cnj70-ecommerce) |
| Mô tả | Sàn thương mại điện tử **đa nhà bán** (multi-vendor marketplace) |
| Stack | Spring Boot 3.2 · Spring Security · Spring Data MongoDB · Thymeleaf |
| Cơ sở dữ liệu | MongoDB (Atlas cluster, db: `cnj70_ecommerce`) |
| Ngôn ngữ | Java 17 + Lombok |
| Ngày phát hành | 09/2021 → 26/09/2026 (5 năm hoạt động) |
| Số collection | **21** (20 nghiệp vụ + 1 collection `audit_logs` lưu cả `AuditLog` và `AuditLogEntry`) |
| Tổng bản ghi seed | **~28.000** bản ghi thật trong MongoDB |

---

## 2. Kiến trúc & phân lớp

```
┌─────────────────────────────────────────────────────────────────┐
│                    Browser (Customer / Vendor)                   │
└──────────────────────────┬──────────────────────────────────────┘
                           │ HTTP / Thymeleaf / JSON
┌──────────────────────────▼──────────────────────────────────────┐
│  Spring MVC Controllers (web/, api/, admin/, moderator/, vendor/)│
│  + Spring Security (form login + JWT cho API)                   │
└──────────────────────────┬──────────────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────────────┐
│  Services  (OrderService, AuthService, AdminEscalationService,  │
│             ModeratorProductService, VoucherService, ...)       │
│  + Guard pattern (VendorGuard, ModeratorGuard, AdminGuard)      │
└──────────────────────────┬──────────────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────────────┐
│  Repositories (Spring Data Mongo) + MongoTemplate                │
└──────────────────────────┬──────────────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────────────┐
│  MongoDB Atlas (21 collections, indexed)                        │
└─────────────────────────────────────────────────────────────────┘
```

Các **Scheduler** chạy nền:
- `ComplaintEscalationScheduler` — tự động leo thang khiếu nại quá hạn.
- `SchedulerIdempotencyRecord` đảm bảo idempotency.

---

## 3. 21 collection nghiệp vụ

| # | Collection | Document | Số bản ghi seed | Mô tả ngắn |
|--:|---|---|--:|---|
| 1 | `users` | `User` | **1.500** | Tài khoản hệ thống (admin, moderator, vendor, customer). |
| 2 | `shops` | `Shop` | **300** | Gian hàng thuộc vendor (KYC → APPROVED → ACTIVE). |
| 3 | `products` | `Product` | **2.000** | Sản phẩm niêm yết (auto/manual moderation). |
| 4 | `orders` | `Order` | **5.000** | Đơn hàng (multi-shop, multi-payment, multi-shipping). |
| 5 | `reviews` | `Review` | **3.000** | Đánh giá sản phẩm (auto-pipeline + manual moderation). |
| 6 | `carts` | `Cart` | **1.500** | Giỏ hàng hiện tại của customer. |
| 7 | `categories` | `Category` | **30** | Danh mục 2 cấp (parent/child). |
| 8 | `vouchers` | `Voucher` | **200** | Mã giảm giá (SHOP-scope và WEB-scope). |
| 9 | `banners` | `Banner` | **100** | Banner trang chủ / category / checkout / flash sale. |
| 10 | `kyc_profiles` | `KycProfile` | **300** | Hồ sơ KYC của vendor. |
| 11 | `complaints` | `Complaint` | **1.500** | Khiếu nại Customer ↔ Vendor (3 cấp escalation). |
| 12 | `returns` | `ReturnRequest` | **1.000** | Yêu cầu trả hàng (gắn với complaint/order). |
| 13 | `refunds` | `RefundRequest` | **1.000** | Hoàn tiền (internal-ledger, stripe, momo). |
| 14 | `report_cases` | `ReportCase` | **1.500** | Báo cáo vi phạm (product/review) lên Moderator. |
| 15 | `moderation_history` | `ModerationHistory` | **2.000** | Lịch sử ra quyết định của Moderator. |
| 16 | `violations` | `Violation` | **300** | Vi phạm shop (warning/ban). |
| 17 | `escalations` | `Escalation` | **300** | Leo thang từ Moderator → Admin. |
| 18 | `legal_documents` | `LegalDocument` | **100** | Điều khoản, chính sách (nhiều version). |
| 19 | `audit_logs` | `AuditLog` | **5.000** | Audit đầy đủ (JSON before/after). |
| 20 | `audit_logs` | `AuditLogEntry` | **5.000** | Audit projection (read-only, indexed). |
| 21 | `scheduler_idempotency` | `SchedulerIdempotencyRecord` | **200** | TTL record chống chạy lặp scheduler. |

**Tổng: ~28.000 bản ghi.**

---

## 4. Phân bố dữ liệu qua 5 năm (2021-09 → 2026-09)

Tất cả các trường `createdAt` / `paidAt` / `deliveredAt` / `moderationAt` / … được
random trong cửa sổ 5 năm, theo **Random có seed = 42** (deterministic).

```
                  2021   2022   2023   2024   2025   2026
                  Q3 Q4  Q1 Q2 Q3 Q4  Q1 Q2 Q3 Q4  Q1 Q2 Q3 Q4  Q1 Q2 Q3 Q4  Q1 Q2 Q3
  users        ░░░░░░ ░░░░░░ ░░░░░░ ░░░░░░ ░░░░░░ ░░░░░░   (~250 user/năm)
  shops        ░░ ░░ ░░ ░░ ░░ ░░ ░░ ░░ ░░ ░░ ░░ ░░ ░░ ░░   (~50 shop/năm)
  products     ░░░░░░ ░░░░░░ ░░░░░░ ░░░░░░ ░░░░░░ ░░░░░░   (~330 sp/năm)
  orders       ░░░░░░ ░░░░░░ ░░░░░░ ░░░░░░ ░░░░░░ ░░░░░░   (~830 đơn/năm)
  reviews      ░░░░░░ ░░░░░░ ░░░░░░ ░░░░░░ ░░░░░░ ░░░░░░   (~500 review/năm)
  complaints   ░░░░░░ ░░░░░░ ░░░░░░ ░░░░░░ ░░░░░░ ░░░░░░   (~250 complaint/năm)
  audit_logs   ░░░░░░ ░░░░░░ ░░░░░░ ░░░░░░ ░░░░░░ ░░░░░░   (~830 log/năm)
```

**Đặc điểm phân bố:**
- Tăng trưởng theo năm: năm 2021 còn ít (mới thành lập), đỉnh điểm năm 2024–2025 (tăng trưởng nóng), năm 2026 ổn định.
- Audit logs: 10.000 bản ghi (AuditLog + AuditLogEntry) phản ánh hoạt động admin/moderator/vendor.
- Compliance: 100 `LegalDocument` versions (mỗi loại ~12 versions qua 5 năm).
- Moderation: ~2.000 lịch sử + 300 violations + 300 escalations.

---

## 5. Vai trò & phân quyền

| Role | Số user seed | Quyền chính |
|---|--:|---|
| **ADMIN** | 8 | Duyệt KYC, ban shop, duyệt escalation, cập nhật legal doc, xem audit logs. |
| **MODERATOR** | 22 | Duyệt product (manual), review moderation, xử lý report case, đóng complaint level 1. |
| **VENDOR** | 370 | Tạo shop, niêm yết product, xử lý đơn hàng của mình, trả lời complaint level 0. |
| **CUSTOMER** | 1.100 | Mua hàng, đánh giá, khiếu nại, báo cáo vi phạm. |

Phân bố thực tế giữa Customer / Vendor được giữ ổn định ~3:1.

---

## 6. Nghiệp vụ chính (10 quy trình)

### 6.1. Onboarding Vendor

```
Customer register ──► Submit KYC ──► 3rd-party verify ──► Admin review ──► Create Shop ──► List Product
                                          │
                                          └─► THIRD_PARTY_REJECTED → vendor resubmit
```

**Bằng chứng trong seed:**
- 1.500 user, trong đó 370 VENDOR.
- 300 KYC profile (1 user VENDOR đã APPROVED ↔ 1 shop).
- Mỗi KYC profile có đầy đủ trạng thái: APPROVED (75%), PENDING_ADMIN (13%), PENDING_THIRD_PARTY (7%), ADMIN_REJECTED (5%).

### 6.2. Niêm yết & Auto-Moderation Product

```
Vendor create Product ──► Auto-moderation pipeline ──► AUTO_PASSED | AUTO_REJECTED | PENDING_MANUAL
                                                                  │
                                                  AUTO_REJECTED → Vendor fix → re-submit
                                                  PENDING_MANUAL → Moderator review → APPROVE/REJECT
```

**Trong seed:**
- 2.000 product, phân bố:
  - 70% APPROVED
  - 12% AUTO_PASSED
  - 8% PENDING_MANUAL (chờ moderator)
  - 5% AUTO_REJECTED
  - 3% REJECTED
  - 2% ESCALATED

### 6.3. Mua hàng (Checkout → Order)

```
Cart ──► Checkout ──► Voucher apply ──► Order (PENDING) ──► Payment (VNPay/COD/BankQR)
                                                       │
                                                       └─► paid → Order (PREPARING)
                                                                          │
                                              Vendor prepare ─► SHIPPING (per shop)
                                                                          │
                                                                  DELIVERED → close
```

**Trong seed:**
- 5.000 order, phân bố: 70% DELIVERED, 10% SHIPPING, 8% PREPARING, 8% PENDING, 4% CANCELLED.
- 30% đơn có áp voucher (200 voucher).
- Payment: 50% VNPay, 30% COD, 20% Bank QR.
- Mỗi order có thể multi-shop (chia shippingByShop riêng).

### 6.4. Đánh giá & Review Moderation

```
Customer review ──► Auto-pipeline ──► AUTO_PASSED → VISIBLE
                                  └─► AUTO_REJECTED → HIDDEN
                                  └─► PENDING_MANUAL → Moderator review → VISIBLE/HIDDEN
```

User có thể **report** review → sinh `ReportCase` → Moderator xử lý.

### 6.5. Khiếu nại (Complaint) — 3 cấp escalation

```
LEVEL_0: Customer ↔ Vendor
   │  (vendor không phản hồi trong deadline 48h)
   ▼
LEVEL_1: Moderator xử lý
   │  (vẫn không giải quyết được / vi phạm nghiêm trọng)
   ▼
LEVEL_2: Admin ra quyết định cuối
```

Trong seed: 1.500 complaint, phân bố:
- 40% RESOLVED (đã xử lý xong)
- 15% VENDOR_RESPONDED (vendor đã phản hồi)
- 15% OPEN (đang chờ vendor)
- 10% MODERATOR_REVIEW
- 8% ADMIN_REVIEW
- 6% ESCALATED
- 6% CLOSED

### 6.6. Trả hàng & Hoàn tiền

```
Complaint → Return request (REQUESTED) → Vendor APPROVED → Customer ship back
                → RECEIVED → COMPLETED → Refund (SUCCEEDED via stripe/momo/internal-ledger)
```

- 1.000 ReturnRequest, 1.000 RefundRequest.
- Có thể gắn với complaint (50% returns) hoặc order trực tiếp.

### 6.7. Report & Moderation Pipeline

```
User report (Product/Review) → ReportCase (PENDING)
                                       │
                                       ▼
                          Moderator claim (OPEN → IN_REVIEW)
                                       │
                          ┌────────────┴────────────┐
                       APPROVE                  REJECT | ESCALATE
                          │                          │
                       RESOLVED              REJECTED | ESCALATED → Admin
```

- 1.500 report case (60% về review, 40% về product).

### 6.8. Vi phạm & Shop Enforcement

```
Moderator phát hiện → ReportCase ESCALATE → Escalation (PENDING)
                                                  │
                                       Admin nhận (IN_REVIEW)
                                                  │
                                       Admin quyết định:
                                       WARNING / PRODUCT_HIDDEN / SHOP_RESTRICTED /
                                       SHOP_SUSPENDED / VENDOR_BANNED
                                                  │
                                                  ▼
                                          Violation record (audit)
```

- 300 Violation, 300 Escalation.

### 6.9. Banner & Marketing

- 100 banner (80% PUBLISHED, 20% UNPUBLISHED).
- Vị trí: HOME_HERO / HOME_PROMO / CATEGORY_TOP / CHECKOUT_BANNER / FLASH_SALE.

### 6.10. Legal & Compliance

- 100 `LegalDocument` versions qua 5 năm:
  - TERMS, PRIVACY, RETURN, SHIPPING, WARRANTY, COMPLAINT, PAYMENT, SITEMAP.
  - Mỗi loại ~12 versions (phát hành mỗi ~6 tháng).
- User chấp nhận Terms/Privacy tại thời điểm đăng ký (`acceptedTermsAt`).

---

## 7. Cấu trúc Audit Log

Hệ thống lưu **2 dạng** audit cùng collection `audit_logs`:

| Loại | Class | Khi nào dùng |
|---|---|---|
| `AuditLog` | 14 field, JSON before/after | Ghi đầy đủ (full audit). |
| `AuditLogEntry` | 12 field, indexed theo `actorId + createdAt`, `resourceType + resourceId`, `action + createdAt` | Projection read-only cho Admin dashboard. |

Cả 2 cùng được seed 5.000 bản ghi (tổng 10.000 trong collection).

Các action hay xuất hiện: `USER_CREATED`, `SHOP_APPROVED`, `PRODUCT_APPROVED`,
`REVIEW_REPORTED`, `COMPLAINT_CREATED`, `REPORT_CASE_RESOLVED`, `VIOLATION_WARNED`,
`ESCALATION_RESOLVED`, `PII_ACCESSED`, `LOGIN_SUCCESS`…

---

## 8. Indexes quan trọng (đã auto-create)

```
users:         email (unique)
shops:         shopName (unique)
products:      name, moderationStatus
orders:        userId
reviews:       productId, userId, moderationStatus, pipelineModerationStatus
complaints:    customerId, shopId, orderId, status, level, createdAt
kyc_profiles:  userId (unique)
refunds:       complaintId, returnId, orderId, customerId, shopId, status, createdAt
returns:       complaintId, orderId, customerId, shopId, status, createdAt
report_cases:  resourceType, resourceId, targetType, targetId, reporterId,
               status, assignedModeratorId, createdAt
moderation_history: resourceId, createdAt
violations:    shopId, type, severity, createdAt
escalations:   reportCaseId, status, createdAt
audit_logs:    actor_created_idx, resource_type_id_idx, action_created_idx
scheduler_idempotency:
               operationKey (unique), jobName+status, expiresAt (TTL)
```

---

## 9. Cách chạy Seed Data

### 9.1. Yêu cầu

- MongoDB URI hợp lệ trong `application.yml` (đã có sẵn cho cluster Atlas).
- JDK 17 + Maven.

### 9.2. Seed TẤT CẢ (full run — không khuyến nghị)

```bash
mvn spring-boot:run \
  -Dspring-boot.run.profiles=seed \
  -Dspring-boot.run.arguments="--app.seed.enabled=true --seed.wipe"
```

> Lưu ý: full run insert ~28.000 bản ghi, có thể mất **5–10 phút** tuỳ network.

### 9.3. Seed theo collection, từng batch 100 bản ghi (khuyến nghị)

```bash
# Bật chế độ seed
export APP_SEED_ENABLED=true

# 1) Categories (chỉ 30 records, 1 batch)
mvn spring-boot:run -Dspring-boot.run.profiles=seed \
  -Dspring-boot.run.arguments="--app.seed.enabled=true --seed.target=categories --seed.batch=0"

# 2) Users (1.500 records, 15 batches)
mvn spring-boot:run -Dspring-boot.run.profiles=seed \
  -Dspring-boot.run.arguments="--app.seed.enabled=true --seed.target=users --seed.batch=0"
mvn spring-boot:run -Dspring-boot.run.profiles=seed \
  -Dspring-boot.run.arguments="--app.seed.enabled=true --seed.target=users --seed.batch=1"
# ... lặp lại batch=2 → 14

# 3) Shops (300 records, 3 batches)
mvn spring-boot:run -Dspring-boot.run.profiles=seed \
  -Dspring-boot.run.arguments="--app.seed.enabled=true --seed.target=shops --seed.batch=0"
# ... lặp lại batch=1 → 2

# 4) Products (2.000 records, 20 batches)
# --seed.target=products --seed.batch=0 → 19

# 5) Orders (5.000 records, 50 batches) — lớn nhất
# --seed.target=orders --seed.batch=0 → 49

# 6) Reviews (3.000 records, 30 batches)
# 7) Carts (1.500 records, 15 batches)
# 8) Vouchers (200 records, 2 batches)
# 9) Banners (100 records, 1 batch)
# 10) KYC (300 records, 3 batches)
# 11) Complaints (1.500 records, 15 batches)
# 12) Returns (1.000 records, 10 batches)
# 13) Refunds (1.000 records, 10 batches)
# 14) ReportCases (1.500 records, 15 batches)
# 15) Moderation History (2.000 records, 20 batches)
# 16) Violations (300 records, 3 batches)
# 17) Escalations (300 records, 3 batches)
# 18) Legal Documents (100 records, 1 batch)
# 19) Audit Logs (5.000 records, 50 batches)
# 20) Audit Log Entries (5.000 records, 50 batches)
# 21) Scheduler Records (200 records, 2 batches)
```

### 9.4. Bảng batch size tổng hợp

| Target | Tổng | Số batch | batch=0 → ? |
|---|--:|--:|---|
| `categories` | 30 | 1 | 0 |
| `users` | 1.500 | 15 | 0–14 |
| `shops` | 300 | 3 | 0–2 |
| `products` | 2.000 | 20 | 0–19 |
| `orders` | 5.000 | 50 | 0–49 |
| `reviews` | 3.000 | 30 | 0–29 |
| `carts` | 1.500 | 15 | 0–14 |
| `vouchers` | 200 | 2 | 0–1 |
| `banners` | 100 | 1 | 0 |
| `kyc` | 300 | 3 | 0–2 |
| `complaints` | 1.500 | 15 | 0–14 |
| `returns` | 1.000 | 10 | 0–9 |
| `refunds` | 1.000 | 10 | 0–9 |
| `reportcases` | 1.500 | 15 | 0–14 |
| `moderation` | 2.000 | 20 | 0–19 |
| `violations` | 300 | 3 | 0–2 |
| `escalations` | 300 | 3 | 0–2 |
| `legal` | 100 | 1 | 0 |
| `auditlogs` | 5.000 | 50 | 0–49 |
| `auditentries` | 5.000 | 50 | 0–49 |
| `scheduler` | 200 | 2 | 0–1 |

**Tổng: 320 batch nhỏ × 100 bản ghi ≈ 28.000 bản ghi.**

---

## 10. Đặc tính dữ liệu

- **Tên người Việt thật:** 13 họ × 44 tên nam × 44 tên nữ — sinh ngẫu nhiên.
- **Địa chỉ Việt Nam thật:** 25 tỉnh/thành × 20 quận/huyện × 22 tuyến đường.
- **Số điện thoại VN format:** prefix 03/05/07/08/09 + 8 chữ số.
- **Brand sản phẩm thật:** Samsung, Apple, Sony, LG, Nike, Adidas, L'Oreal, IKEA, Bosch…
- **Ngân hàng VN thật:** Vietcombank, Techcombank, BIDV, VietinBank, ACB, MBBank…
- **Ảnh sản phẩm:** `https://picsum.photos/seed/{id}/W/H` (deterministic seed → ảnh ổn định).
- **Mã voucher format:** `VC00001` … `VC00200`.
- **Trạng thái đa dạng:** đầy đủ các enum (`APPROVED`, `PENDING`, `REJECTED`, `ESCALATED`…).

---

## 11. Tài khoản test (đã seed)

| Role | Email | Mật khẩu |
|---|---|---|
| Admin | `user00000@cnj70.vn` | `password` |
| Admin | `user00001@cnj70.vn` | `password` |
| Moderator | `user00008@cnj70.vn` | `password` |
| Vendor | `user00030@cnj70.vn` | `password` |
| Customer | `user00400@cnj70.vn` | `password` |

> Tất cả user seed đều dùng cùng hash BCrypt placeholder (`$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy`)
> tương ứng với mật khẩu `password`. **CẢNH BÁO:** chỉ dùng cho môi trường dev/test,
> không dùng cho production.

---

## 12. Báo cáo sau khi seed

Sau khi chạy xong, seeder in ra bảng tổng kết:

```
----- DB SUMMARY -----
                 users = 1500
                 shops = 300
              products = 2000
            categories = 30
                orders = 5000
               reviews = 3000
                 carts = 1500
              vouchers = 200
               banners = 100
          kyc_profiles = 300
            complaints = 1500
               returns = 1000
               refunds = 1000
          report_cases = 1500
   moderation_history = 2000
            violations = 300
          escalations = 300
      legal_documents = 100
            audit_logs = 10000   (AuditLog + AuditLogEntry)
scheduler_idempotency = 200
```

---

## 13. Lưu ý vận hành

1. **Idempotency:** Chạy lại cùng batch sẽ insert thêm 100 bản ghi mới (UUID mới).
   Không có cơ chế upsert. Để reset, dùng `--seed.wipe`.
2. **Phụ thuộc FK:** Phase 1 phải chạy trước Phase 2:
   - `categories` → `users` → `shops` → `products` → `vouchers` →
     `orders` → `reviews` → `carts` → `kyc` → `complaints` → …
3. **Email trùng:** Mỗi user có email `user00000@cnj70.vn` … `user01499@cnj70.vn` (unique).
4. **Shop unique name:** dùng suffix `Tech 1`, `Fashion 2`… đảm bảo không trùng.
5. **Voucher code unique:** `VC00001` … `VC00200`.
6. **Cart unique userId:** Mỗi customer tối đa 1 cart (insert được unique index).

---

## 14. Liên hệ & đóng góp

- File seeder: [`src/main/java/com/ecommerce/cnj70/config/ProductionDataSeeder.java`](../src/main/java/com/ecommerce/cnj70/config/ProductionDataSeeder.java)
- Helper pools: [`src/main/java/com/ecommerce/cnj70/config/SeedPools.java`](../src/main/java/com/ecommerce/cnj70/config/SeedPools.java)
- Mọi thay đổi schema → sửa document trước, sau đó cập nhật method tương ứng trong seeder.
