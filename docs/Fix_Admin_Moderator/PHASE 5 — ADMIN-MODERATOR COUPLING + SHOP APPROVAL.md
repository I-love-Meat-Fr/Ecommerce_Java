PHASE 5 — ADMIN-MODERATOR COUPLING + SHOP APPROVAL FLOW

0. IMPLEMENTATION INSTRUCTION

Phase 5 kết nối chặt Admin ↔ Moderator:

Shop approval: thêm nút duyệt shop cho Moderator (giống Admin).
Audit Log chia sẻ: Moderator action phải ghi vào AuditLog (admin có thể xem qua /admin/audit).
Coupling transition state: Shop PENDING → APPROVED phải đồng bộ giữa admin/shop-list và moderator/shop-queue.
Coupling Audit: Khi moderator approve product → AuditLog có entry mà admin xem được.
Coupling User Lock: Khi moderator tạo violation CRITICAL → admin có thể lock user (cùng user).

Dựa trực tiếp trên:

Phase 1-4 đã có.
PHASE 2B — MODERATOR WORKFLOW.md.
PHASE 7 — AUDIT LOG VIEWER + ROLE LOGIC REGRESSION.md.
User report: "admin và moderator liên kết chặt chẽ với nhau".
User report: "Shop Queue — CNJ70 Moderator đang bị hỏng trạng thái xem và chưa có nút duyệt shop như bên admin".

CURRENT SOURCE
        ↓
COUPLING MAP
        ↓
SHOP APPROVAL COUPLING
        ↓
AUDIT LOG COUPLING
        ↓
USER LOCK COUPLING
        ↓
PRODUCT MODERATION COUPLING
        ↓
BUILD
        ↓
PHASE 5 READY

1. MỤC TIÊU

Phase 5 phải kết nối được:

| Action | Admin | Moderator | Shared Resource | Status coupling |
|---|---|---|---|---|
| Duyệt Shop | /admin/shops/{id}/approve | /moderator/shop-queue/{id}/approve | shops | Both use same AdminShopService.approve() |
| Từ chối Shop | /admin/shops/{id}/reject | /moderator/shop-queue/{id}/reject | shops | Both use AdminShopService.reject() |
| Audit Log | /admin/audit | (read by admin only) | audit_logs | Moderator writes audit, Admin reads |
| Product approve | /admin/products/{id} (fallback) | /moderator/product-queue/{id}/approve | products | Single AdminProductService.approve() |
| User lock | /admin/users/{id}/lock | (chỉ admin) | users | Moderator vi phạm CRITICAL → escalation → admin lock |
| KYC approve | /admin/kyc/{id}/approve | /moderator/kyc-queue/{id}/approve | kyc_profiles | Both use AdminKycService.approve() |

2. KẾT QUẢ ĐỐI CHIẾU SOURCE

Theo source audit:

Theo Phase 1 audit §6.1 + Phase 2 Security Report:
- /admin/shops/** ADMIN + MODERATOR
- /admin/products/** ADMIN + MODERATOR
- /admin/reviews/** ADMIN + MODERATOR
- /admin/kyc/** ADMIN + MODERATOR
- /admin/audit/** ADMIN-only
- /admin/orders/** ADMIN-only
- /admin/vouchers/** ADMIN-only
- /admin/banners/** ADMIN-only

Tức là admin endpoint ĐÃ cho phép moderator truy cập nhưng moderator có endpoint riêng /moderator/...

Theo user report Phase 4:
Shop Queue có nút duyệt shop phải thêm vào — Phase 4 chỉ view, Phase 5 thêm action.

Audit Log:
Moderator action phải ghi audit.
Admin xem tất cả audit.
Coupling: 1 audit_logs collection, 2 cách đọc.

3. NGUYÊN TẮC BẮT BUỘC — KHÔNG XÓA MONGODB

Tuyệt đối KHÔNG:
deleteAll() / deleteMany({}) / drop() / dropDatabase()

4. KHÔNG FAKE

Không fake audit log chỉ để hiển thị đẹp.

5. SOURCE OF TRUTH

CURRENT SOURCE > USER REPORT > PHASE 1-4 > ADMIN_PHASE docs

6. DISCOVERY RULE

SEARCH → EXISTS? → INSPECT → REUSE/FIX → CREATE

7. PHẠM VI PHASE 5

| Coupling | Hành động | File chính |
|---|---|---|
| Shop Approval | Thêm nút duyệt vào moderator/shop-queue | ModeratorQueueController.java + shop-queue.html + AdminShopService (shared) |
| Shop Detail | Tạo moderator/shop-detail.html có action | moderator/shop-detail.html |
| Audit Log share | Moderator ghi audit mỗi action | Mọi ModeratorXxxService |
| User Lock coupling | Moderator tạo violation CRITICAL → escalation | ViolationService + EscalationService |
| Product Moderation | Ensure AdminProductService.approve được dùng chung | AdminProductService (đã có) + ModeratorProductService (nếu có) |
| KYC Approval | Coupling logic admin/moderator | AdminKycService + ModeratorKycService (nếu có) |
| Status Sync | Sau approve ở moderator → admin list update | (audit + status transition) |

8. SHOP APPROVAL COUPLING CONTRACT

8.1 — MỤC TIÊU

Moderator từ /moderator/shop-queue có thể:
- Xem shop detail
- Approve shop
- Reject shop
- Activate/Deactivate

Cùng logic với Admin. Cùng MongoDB collection.

8.2 — SERVICE BOUNDARY

⚠️ Quyết định kiến trúc: dùng AdminShopService hay tạo ModeratorShopService?

Theo Phase 2A §33-37 (Service Boundary):
> Không đưa business logic vào Controller.
> Phải giữ:
> ADMIN = platform management / enforcement
> MODERATOR = normal moderation workflow

→ Phase 5 tạo ModeratorShopService nếu chưa có, REUSE AdminShopService methods.

Option 1 (REUSE — đơn giản):
ModeratorShopController gọi AdminShopService.approve() / reject() / activate() / deactivate().

Option 2 (SEPARATE — bounded):
ModeratorShopService có method riêng nhưng delegate sang AdminShopService.

Phase 5 chọn Option 1 (REUSE) đơn giản + ít file mới.

```java
// ModeratorShopController
@PostMapping("/moderator/shop-queue/{id}/approve")
public String approve(@PathVariable String id, RedirectAttributes ra) {
    Shop s = adminShopService.approve(id);
    ra.addFlashAttribute("flashSuccess", "Đã duyệt shop: " + s.getName());
    return "redirect:/moderator/shop-queue";
}

@PostMapping("/moderator/shop-queue/{id}/reject")
public String reject(@PathVariable String id,
                     @RequestParam String reason,
                     RedirectAttributes ra) {
    Shop s = adminShopService.reject(id, reason);
    ra.addFlashAttribute("flashSuccess", "Đã từ chối");
    return "redirect:/moderator/shop-queue";
}

@PostMapping("/moderator/shop-queue/{id}/activate")
public String activate(@PathVariable String id, RedirectAttributes ra) {
    Shop s = adminShopService.activate(id);
    ra.addFlashAttribute("flashSuccess", "Đã kích hoạt");
    return "redirect:/moderator/shop-queue";
}

@PostMapping("/moderator/shop-queue/{id}/deactivate")
public String deactivate(@PathVariable String id, RedirectAttributes ra) {
    Shop s = adminShopService.deactivate(id);
    ra.addFlashAttribute("flashSuccess", "Đã vô hiệu");
    return "redirect:/moderator/shop-queue";
}
```

8.3 — TEMPLATE

moderator/shop-queue.html:

```html
<th:block th:replace="fragments/moderator-shell :: layout(~{::content})">
    <th:block th:fragment="content">
        <div th:if="${flashSuccess}" class="alert alert-success" th:text="${flashSuccess}"></div>

        <table>
            <thead>
                <tr><th>Tên shop</th><th>Owner</th><th>Trạng thái</th><th>Thao tác</th></tr>
            </thead>
            <tbody>
                <tr th:each="shop : ${shops}">
                    <td th:text="${shop.name}"></td>
                    <td th:text="${shop.ownerUserId}"></td>
                    <td>
                        <span th:if="${shop.status.name() == 'PENDING'}" class="badge bg-warning">PENDING</span>
                        <span th:if="${shop.status.name() == 'APPROVED'}" class="badge bg-success">APPROVED</span>
                        <span th:if="${shop.status.name() == 'REJECTED'}" class="badge bg-danger">REJECTED</span>
                        <span th:if="${shop.status.name() == 'SUSPENDED'}" class="badge bg-secondary">SUSPENDED</span>
                    </td>
                    <td>
                        <a th:href="@{'/moderator/shop-queue/' + ${shop.id}}" class="btn btn-sm btn-info">Xem</a>
                        <form th:action="@{'/moderator/shop-queue/' + ${shop.id} + '/approve'}" method="post" style="display:inline">
                            <button type="submit" class="btn btn-sm btn-success">Duyệt</button>
                        </form>
                        <form th:action="@{'/moderator/shop-queue/' + ${shop.id} + '/reject'}" method="post" style="display:inline">
                            <input type="text" name="reason" required placeholder="Lý do" class="form-control-sm" style="width:120px"/>
                            <button type="submit" class="btn btn-sm btn-danger">Từ chối</button>
                        </form>
                    </td>
                </tr>
            </tbody>
        </table>
    </th:block>
</th:block>
```

moderator/shop-detail.html:

```html
<th:block th:replace="fragments/moderator-shell :: layout(~{::content})">
    <th:block th:fragment="content">
        <div th:if="${flashSuccess}" class="alert alert-success" th:text="${flashSuccess}"></div>

        <h2 th:text="${shop.name}"></h2>
        <p>Mô tả: <span th:text="${shop.description}"></span></p>
        <p>Owner: <span th:text="${shop.ownerUserId}"></span></p>
        <p>Trạng thái: <span th:text="${shop.status}"></span></p>

        <form th:action="@{'/moderator/shop-queue/' + ${shop.id} + '/approve'}" method="post">
            <button type="submit" class="btn btn-success">Duyệt</button>
        </form>
        <form th:action="@{'/moderator/shop-queue/' + ${shop.id} + '/reject'}" method="post">
            <input type="text" name="reason" required/>
            <button type="submit" class="btn btn-danger">Từ chối</button>
        </form>
        <form th:action="@{'/moderator/shop-queue/' + ${shop.id} + '/activate'}" method="post">
            <button type="submit" class="btn btn-primary">Activate</button>
        </form>
        <form th:action="@{'/moderator/shop-queue/' + ${shop.id} + '/deactivate'}" method="post">
            <button type="submit" class="btn btn-warning">Deactivate</button>
        </form>
    </th:block>
</th:block>
```

9. AUDIT LOG COUPLING CONTRACT

9.1 — SOURCE OF TRUTH

AuditLog đã có (theo Phase 1 §1.3, collection `audit_logs`).
AuditAction enum đã có.

9.2 — MODERATOR WRITE AUDIT

Mỗi moderator action phải ghi audit:

| Action | AuditAction | ResourceType |
|---|---|---|
| Moderator approve shop | SHOP_APPROVED | Shop |
| Moderator reject shop | SHOP_REJECTED | Shop |
| Moderator approve product | PRODUCT_APPROVED | Product |
| Moderator reject product | PRODUCT_REJECTED | Product |
| Moderator hide product | PRODUCT_HIDDEN | Product |
| Moderator approve review | REVIEW_APPROVED | Review |
| Moderator hide review | REVIEW_HIDDEN | Review |
| Moderator delete review | REVIEW_DELETED | Review |
| Moderator resolve violation | VIOLATION_RESOLVED | Violation |
| Moderator escalate | ESCALATION_CREATED | Escalation |
| Moderator approve KYC | KYC_APPROVED | KycProfile |
| Moderator reject KYC | KYC_REJECTED | KycProfile |
| Moderator claim report case | REPORTCASE_CLAIMED | ReportCase |
| Moderator resolve report case | REPORTCASE_RESOLVED | ReportCase |

9.3 — IMPLEMENTATION

Trong AdminShopService hoặc moderator controller, thêm:

```java
private final AuditLogService auditLogService;

public Shop approve(String id) {
    Shop s = shopRepository.findById(id).orElseThrow(...);
    ShopStatus old = s.getStatus();
    s.setStatus(ShopStatus.APPROVED);
    s.setUpdatedAt(LocalDateTime.now());
    Shop saved = shopRepository.save(s);
    auditLogService.log(SHOP_APPROVED, "Shop", saved.getId(), 
        Map.of("oldStatus", old, "newStatus", ShopStatus.APPROVED));
    return saved;
}
```

⚠️ AuditAction enum có thể cần thêm value mới. Verify enum hiện có.

9.4 — ADMIN VIEW ALL AUDIT

Admin /admin/audit đã có (Phase 7). Đảm bảo filter có thể lọc:
- action = SHOP_APPROVED (do moderator)
- actorRole = MODERATOR
- tất cả moderator actions

Phase 5 verify /admin/audit hiển thị được audit ghi bởi moderator.

10. USER LOCK COUPLING

10.1 — FLOW

Moderator tạo violation CRITICAL cho user → Escalation lên Admin → Admin lock user.

10.2 — IMPLEMENT

Trong ViolationService.createViolation() nếu severity=CRITICAL:

```java
Violation v = violationRepository.save(violation);
if (violation.getSeverity() == ViolationSeverity.CRITICAL) {
    Escalation e = new Escalation();
    e.setType(EscalationType.CRITICAL_VIOLATION);
    e.setUserId(violation.getActorId());
    e.setViolationId(violation.getId());
    e.setStatus(EscalationStatus.PENDING);
    e.setCreatedAt(LocalDateTime.now());
    escalationRepository.save(e);
    auditLogService.log(ESCALATION_CREATED, "Escalation", e.getId(), Map.of("from", "MODERATOR"));
}
return v;
```

10.3 — ADMIN LOCK

Admin /admin/users/{id}/lock đã có (Phase 3).

Trong AdminEscalationService, khi admin resolve escalation:
- Nếu approve: lock user → tạo AuditLog USER_LOCKED
- Nếu dismiss: chỉ mark escalation RESOLVED

Phase 5 verify coupling.

11. PRODUCT MODERATION COUPLING

Moderator /moderator/product-queue/{id}/approve gọi AdminProductService.approve() (cùng service với admin).

Admin fallback: /admin/products/{id} có nút Approve (Phase 3 đã fix).

Coupling: 1 collection `products`, 2 cách vào, 1 service.

12. KYC COUPLING

Moderator /moderator/kyc-queue/{id}/approve gọi AdminKycService.approve().

Admin /admin/kyc/{id}/approve → cùng service.

Khi KYC APPROVED → User.role chuyển sang có thể tạo Shop. Logic này đã có (theo source audit).

13. REVIEW + VIOLATION COUPLING

Moderator /moderator/violations tạo violation từ report case.

Coupling: ReportCase RESOLVED → có thể tạo Violation nếu severity cao.

Phase 5 verify coupling flow.

14. STATUS TRANSITION TABLE

| Action | Shop | Product | Review | Violation | Escalation |
|---|---|---|---|---|---|
| Approve | PENDING → APPROVED | PENDING → ACTIVE | HIDDEN → ACTIVE | OPEN → RESOLVED | PENDING → IN_REVIEW |
| Reject | PENDING → REJECTED | PENDING → REJECTED | HIDDEN → DELETED | OPEN → REJECTED | PENDING → DISMISSED |
| Hide | N/A | ACTIVE → HIDDEN | ACTIVE → HIDDEN | N/A | N/A |
| Activate | APPROVED/REJECTED → APPROVED | HIDDEN → ACTIVE | HIDDEN → ACTIVE | N/A | IN_REVIEW → RESOLVED |
| Deactivate | APPROVED → SUSPENDED | N/A | N/A | N/A | N/A |

15. AUDIT ACTION ENUM CHECK

Đọc AuditAction.java. Verify có các value:

- SHOP_APPROVED
- SHOP_REJECTED
- PRODUCT_APPROVED
- PRODUCT_REJECTED
- PRODUCT_HIDDEN
- REVIEW_APPROVED
- REVIEW_HIDDEN
- REVIEW_DELETED
- VIOLATION_CREATED
- VIOLATION_RESOLVED
- ESCALATION_CREATED
- ESCALATION_RESOLVED
- KYC_APPROVED
- KYC_REJECTED
- REPORTCASE_CLAIMED
- REPORTCASE_RESOLVED

Nếu thiếu → thêm vào enum. KHÔNG xóa value cũ.

16. SHOP + COUPLING TEST

```java
@Test void moderatorApproveShop_adminListUpdates() { ... }
@Test void moderatorRejectShop_adminListUpdates() { ... }
@Test void moderatorApproveShop_auditLogWritten() { ... }
@Test void adminViewAudit_seesModeratorActions() { ... }
@Test void moderatorCriticalViolation_escalationCreated() { ... }
@Test void adminLockUserFromEscalation_auditWritten() { ... }
@Test void moderatorApproveProduct_adminCanFallbackApprove() { ... }
@Test void moderatorApproveKyc_sameAsAdmin() { ... }
```

17. COUPLING REGRESSION

Verify không hỏng:
- Admin shop moderation (đã fix Phase 2)
- Admin user lock (đã fix)
- Customer UI
- Vendor UI
- Audit log immutability (Phase 7)

18. COUPLING SECURITY

Giữ nguyên Phase 2:
/admin/shops/**   ADMIN + MODERATOR
/admin/products/** ADMIN + MODERATOR
/admin/kyc/**     ADMIN + MODERATOR
/admin/audit/**   ADMIN-only
/moderator/**     MODERATOR-only

Moderator KHÔNG được truy cập /admin/audit.

19. EXISTING DATA

Sau Phase 5:
- audit_logs: PRESERVED + thêm moderator audit logs
- shops: PRESERVED + transitions
- users: PRESERVED
- escalations: PRESERVED + escalation mới (nếu test)

20. MONGODB SAFETY

Không drop collection.
Không deleteMany.

21. TEST DATA

Nếu cần:
[TEST-PHASE-5]
- 3 AuditLog moderator
- 1 Escalation mới

Không xóa sau test.

22. KHÔNG LÀM MẤT CHỨC NĂNG HIỆN TẠI

Không hỏng:
- Admin role logic (Phase 7 §6 đã verify 48/48)
- Moderator role logic
- Audit immutability
- Vendor / Customer

23. FILE CHANGE REPORT

Files Modified:
- src/main/java/com/ecommerce/cnj70/service/impl/AdminShopServiceImpl.java (thêm auditLogService + write audit)
- src/main/java/com/ecommerce/cnj70/service/impl/AdminProductServiceImpl.java (audit)
- src/main/java/com/ecommerce/cnj70/service/impl/AdminKycServiceImpl.java (audit)
- src/main/java/com/ecommerce/cnj70/service/impl/AdminReviewServiceImpl.java (audit)
- src/main/java/com/ecommerce/cnj70/service/impl/ViolationServiceImpl.java (CRITICAL → escalation)
- src/main/java/com/ecommerce/cnj70/service/impl/AdminEscalationServiceImpl.java (resolve cascade)
- src/main/java/com/ecommerce/cnj70/controller/moderator/ModeratorShopController.java (tạo mới, nếu chưa có)
- src/main/resources/templates/moderator/shop-queue.html (thêm action form)
- src/main/resources/templates/moderator/shop-detail.html (thêm action form)
- src/main/java/com/ecommerce/cnj70/enums/AuditAction.java (thêm value mới nếu cần)

Files Created:
- src/main/java/com/ecommerce/cnj70/controller/moderator/ModeratorShopController.java
- src/main/resources/templates/moderator/shop-detail.html
- src/test/java/com/ecommerce/cnj70/fix/phase5/CouplingTest.java

Files Deleted:
NONE

24. DATA CHANGE REPORT

MongoDB:
Existing data deleted: NONE
Database reset: NO
Collection dropped: NO

Test records inserted:
- 3+ AuditLog (moderator action)
- 1+ Escalation (từ CRITICAL violation)
- (nếu có) shop transitions

Test records deleted:
NO

25. BUILD REPORT

Command: mvn clean package -DskipTests
Result: PASS / FAIL

26. MANUAL TEST REPORT

Test 1 — Shop coupling:
Q1: Login MODERATOR
Q2: /moderator/shop-queue → hiển thị TEST-PHASE-4 shop
Q3: Bấm "Duyệt" → flashSuccess, shop status=APPROVED
Q4: Login ADMIN (khác session)
Q5: /admin/shops → list → shop đó hiển thị status=APPROVED (đồng bộ)
Q6: Verify MongoDB shops collection

Test 2 — Audit coupling:
R1: MODERATOR approve product → AuditLog PRODUCT_APPROVED
R2: ADMIN /admin/audit → thấy log mới với actorId=moderator, actorRole=MODERATOR
R3: ADMIN filter theo actorRole=MODERATOR → thấy đúng logs

Test 3 — Escalation coupling:
S1: MODERATOR /moderator/violations → tạo violation CRITICAL → Escalation tự động
S2: ADMIN /admin/escalations → thấy escalation mới
S3: ADMIN approve escalation → User bị lock
S4: Verify MongoDB users.status = LOCKED

Test 4 — Product coupling:
T1: MODERATOR approve product → status ACTIVE
T2: ADMIN /admin/products → fallback vẫn có thể approve (nếu cần)
T3: Audit log ghi đúng

Test 5 — KYC coupling:
U1: MODERATOR approve KYC → status APPROVED
U2: ADMIN /admin/kyc → thấy KYC status=APPROVED
U3: User đó giờ có thể tạo shop (vendor flow)

27. KNOWN ISSUES

Mỗi issue:
Issue: ...
Root Cause: ...
Phase 5 Status: ...

28. STATUS CLASSIFICATION

EXISTING / FIXED / PARTIAL / MISSING / WRONG / OUT OF SCOPE

29. COUPLING FINAL MAP

| Resource | Admin Endpoint | Moderator Endpoint | Service | Audit |
|---|---|---|---|---|
| Shop | /admin/shops/{id}/approve | /moderator/shop-queue/{id}/approve | AdminShopService | SHOP_APPROVED |
| Shop | /admin/shops/{id}/reject | /moderator/shop-queue/{id}/reject | AdminShopService | SHOP_REJECTED |
| Shop | /admin/shops/{id}/activate | /moderator/shop-queue/{id}/activate | AdminShopService | SHOP_ACTIVATED |
| Shop | /admin/shops/{id}/deactivate | /moderator/shop-queue/{id}/deactivate | AdminShopService | SHOP_DEACTIVATED |
| Product | /admin/products/{id}/hide | /moderator/product-queue/{id}/hide | AdminProductService | PRODUCT_HIDDEN |
| Review | /admin/reviews/{id}/delete | /moderator/review-queue/{id}/delete (nếu có) | AdminReviewService | REVIEW_DELETED |
| KYC | /admin/kyc/{id}/approve | /moderator/kyc-queue/{id}/approve | AdminKycService | KYC_APPROVED |
| Violation | /admin/violations/{id} | /moderator/violations/{id}/resolve | ViolationService | VIOLATION_RESOLVED |
| Escalation | /admin/escalations/{id}/resolve | (moderator tạo) | AdminEscalationService | ESCALATION_RESOLVED |
| User | /admin/users/{id}/lock | (admin only) | AdminUserService | USER_LOCKED |
| AuditLog | /admin/audit | (admin only — read) | AuditLogService | (immutable) |

30. ACCEPTANCE CRITERIA

Phase 5 PASS khi:
- Moderator có nút duyệt shop (4 actions: approve/reject/activate/deactivate)
- Moderator action ghi vào audit_logs
- Admin /admin/audit thấy moderator actions
- Escalation flow tự động khi violation CRITICAL
- Admin xử lý escalation cascade lock user
- 1 collection, 2 cách access, 1 service
- Manual test 26.1-26.5 PASS
- Build PASS
- MongoDB data preserved
- Existing admin role logic không hỏng
- Phase 7 Role Matrix 48/48 vẫn PASS

31. ĐIỀU KIỆN KẾT THÚC

Phase 5 PASS khi:
- Shop approval coupling hoạt động (Moderator approve → Admin list update)
- Audit coupling hoạt động (Moderator write → Admin read)
- Escalation coupling hoạt động (Violation CRITICAL → Escalation → Admin lock)
- Manual test PASS
- Build PASS
- MongoDB preserved
- Phase 7 regression test PASS
