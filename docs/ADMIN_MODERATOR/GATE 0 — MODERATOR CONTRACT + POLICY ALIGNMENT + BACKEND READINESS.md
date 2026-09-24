GATE 0 — MODERATOR/ADMIN CONTRACT, POLICY ALIGNMENT & INDEPENDENT PHASE READINESS

Bản hoàn chỉnh dùng làm prompt thực hiện Gate 0 trong repository.

1. Mục tiêu

Audit source code hiện tại, file Luật & Chính sách sàn Marketplace và kế hoạch Phase 1 → Phase 4 để chốt:

Role, permission và ranh giới Admin/Moderator.

Business contract, API contract, entity, enum và state transition.

Dependency giữa các Phase và giữa các thành viên.

Cách tổ chức code để từng Phase có thể triển khai độc lập, không phải chờ backend của người khác.

Quy tắc tích hợp khi merge code từ nhiều thành viên.

Acceptance criteria để Phase không bị báo READY khi còn mandatory requirement chưa đạt.

Gate 0 chỉ audit và chốt contract. Không implement nghiệp vụ, không sửa source code, không commit/push.

2. Source of Truth

Phải đọc và đối chiếu đầy đủ:

Source code hiện tại trong repository.

File GATE 0 hiện tại.

File Luật & Chính sách sàn Marketplace.

Tài liệu requirement của từng Phase 1 → Phase 4.

Các kết quả audit và trạng thái thực tế của các Phase đã chạy.

Thứ tự căn cứ:

Nội dung

	

Source of Truth




Business rule, policy, violation, enforcement

	

File Luật & Chính sách




Phạm vi chức năng

	

File requirement của Phase




Code, API, DB, security hiện có

	

Repository thực tế




Contract tích hợp giữa các Phase

	

Contract được chốt tại Gate 0

Không tự suy diễn policy hoặc tự tạo business rule chưa được tài liệu quy định. Nếu tài liệu mâu thuẫn, ghi rõ nội dung, nguồn và yêu cầu xác nhận trước khi triển khai phần bị ảnh hưởng.

3. Nguyên tắc độc lập Phase và tích hợp code

Đây là requirement bắt buộc của toàn bộ kế hoạch Phase.

3.1. Không phụ thuộc vào implementation chưa tồn tại

Mỗi Phase phải được thiết kế để có thể triển khai trong phạm vi được giao, kể cả khi backend của thành viên khác chưa hoàn thành.

Không được mặc định rằng một entity, service, API hoặc module sẽ xuất hiện sau này mà chưa có contract thống nhất.

Nếu cần tích hợp module khác:

Chốt interface, DTO, enum, API contract trước.

Tạo adapter hoặc integration boundary trong phạm vi Phase nếu cần.

Tách phần đã implement và phần chưa có implementation.

Không duplicate business logic của module khác.

Không fake response, fake KPI, fake entity hoặc giả lập kết quả nghiệp vụ để báo PASS.

3.2. Phân loại dependency

Loại

	

Ý nghĩa

	

Cách xử lý




CONTRACT_DEPENDENCY

	

Cần thống nhất API/model/interface

	

Chốt contract tại Gate 0




IMPLEMENTATION_DEPENDENCY

	

Module khác chưa có code thực tế

	

Tách adapter, triển khai phần độc lập




DATA_DEPENDENCY

	

Chưa có nguồn dữ liệu authoritative

	

Hiển thị N/A/Unavailable đúng contract




POLICY_DEPENDENCY

	

Policy chưa định nghĩa đủ

	

Ghi blocker, yêu cầu xác nhận




INTEGRATION_DEPENDENCY

	

Cần merge/test với module khác

	

Tạo integration test và checklist merge

Không được đánh dấu toàn bộ Phase là BLOCKED chỉ vì một implementation dependency, nếu các requirement còn lại có thể hoàn thành độc lập.

Chỉ đánh dấu BLOCKED khi dependency đó thực sự ngăn một mandatory requirement hoạt động đúng, an toàn và có thể kiểm chứng.

3.3. Quy tắc merge code

Không tự ý đổi tên hoặc thay đổi contract đã thống nhất.

Không sửa file/module của thành viên khác ngoài scope nếu chưa thống nhất.

Không duplicate entity, enum, API hoặc business rule.

Nếu cần thay đổi contract, phải ghi rõ breaking change và các module bị ảnh hưởng.

Sau merge, phải build và test trên code tích hợp thực tế.

Không báo integration PASS chỉ dựa trên build riêng từng nhánh.

4. Role & Responsibility Contract

Xác nhận đúng 4 role:

CUSTOMER, VENDOR, MODERATOR, ADMIN

Role

	

Trách nhiệm




CUSTOMER

	

Mua hàng, thanh toán, review, complaint, report




VENDOR

	

Quản lý shop/product/order, complaint, appeal




MODERATOR

	

Product/Review Moderation, ReportCase, xử lý case thông thường




ADMIN

	

Platform management, severe escalation, enforcement, KYC monitoring, AuditLog, Finance/Policy

Bắt buộc xác nhận:

MODERATOR ≠ ADMIN

Moderator không có quyền Admin chỉ vì dùng chung layout hoặc component.

Admin không thay thế Moderator để duyệt thủ công toàn bộ Product/Review.

Admin Product Management không đồng nghĩa Moderator Product Moderation.

Admin Review Management không đồng nghĩa Moderator Review Moderation.

Nếu source hiện tại khác contract, ghi rõ source, requirement, mức độ ảnh hưởng và Phase cần sửa.

5. Security Contract

Chốt security contract trước khi triển khai Phase 1:

Actor

	

Access




ADMIN

	

/admin/** theo quyền được phép




MODERATOR

	

/moderator/** theo quyền được phép




CUSTOMER/VENDOR

	

Không được truy cập Moderator/Admin protected routes




Anonymous

	

Không được truy cập protected routes




LOCKED account

	

Không được thực hiện protected actions

Phải audit:

UserRole, SecurityConfig, JWT/authentication.

Login redirect và account status.

Public registration.

Route-level và method-level authorization.

Object-level authorization.

Admin enforcement endpoint.

Moderator không thể thực hiện Admin enforcement.

Locked account không được truy cập hoặc thực hiện protected action.

Public registration không được tự tạo ADMIN hoặc MODERATOR.

Nếu JWT stateless khiến token chưa hết hạn vẫn có thể sử dụng sau khi tài khoản bị khóa, phải ghi rõ limitation và chốt cách xử lý; không được tuyên bố realtime invalidation nếu chưa implement.

6. Chốt API & Data Contract dùng chung

Đối với các module có khả năng tích hợp giữa nhiều Phase, phải xác định:

Entity/model owner duy nhất.

DTO request/response.

API path, HTTP method, validation.

Enum và status transition.

Error response.

Authorization.

Pagination, sorting, filtering nếu cần.

ID/reference giữa các module.

Quy tắc backward compatibility.

Integration test bắt buộc.

Ưu tiên reuse source code hiện có. Không tạo thêm entity hoặc endpoint trùng chức năng.

Nếu module chưa có backend, chốt contract để các Phase liên quan vẫn có thể phát triển mà không cần chờ implementation của module đó.

7. Moderator Contract

Chốt các route:

/moderator/dashboard
/moderator/queue
/moderator/cases
/moderator/violations
/moderator/history

Xác định rõ:

Controller boundary.

UI boundary.

API contract.

Role/authorization.

Data source.

Dependency.

Owner.

Acceptance criteria.

Moderator Foundation chỉ là nền tảng. Không được xem placeholder page là nghiệp vụ moderation đã hoàn thành.

8. Product Moderation Contract — Phase 2A

Chốt flow:

Vendor submits Product
        ↓
Auto Moderation
        ↓
PASS / REJECT / MANUAL REVIEW
        ↓
Moderator Review (nếu cần)
        ↓
Approve / Reject / Escalate

Phải xác định:

ProductStatus và state transition.

ModerationResult.

ModerationQueue.

ModerationHistory.

Auto flags, policy code, violation code, evidence.

Reject reason và validation.

Approve/Reject/Escalate API.

Authorization và object security.

Audit/history requirements.

Không tự tạo policy, violation code hoặc auto moderation threshold chưa được tài liệu định nghĩa.

9. Review Moderation Contract — Phase 2B

Chốt:

Order DELIVERED/COMPLETED
        ↓
Verified Purchase
        ↓
Customer Review
        ↓
Moderation
        ↓
KEEP / HIDE / REMOVE

Phải xác định verified purchase, review status, report flow, moderator decision, reason, history và quyền truy cập.

Không xem Admin Review Delete hiện tại là toàn bộ Moderator Review workflow.

10. ReportCase Contract — Phase 2C

ReportCase phải xác định khả năng liên kết:

Reporter.

Resource và Resource ID.

Reason, evidence.

Auto flags.

Vendor/shop context.

Policy.

Status.

Moderator decision.

Escalation reference.

History.

Chốt flow:

Customer/Vendor/IP Report
        ↓
Detection / Auto Moderation
        ↓
ReportCase
        ↓
Moderator
        ↓
Approve / Reject / Hide / Remove / Escalate

Chốt API, state transition, validation, authorization và integration boundary với Product/Review Moderation.

11. Violation Contract — Phase 3C

Không được trộn Severity với Action.

ViolationSeverity:
LOW / MEDIUM / HIGH / CRITICAL
ViolationAction:
WARNING
PRODUCT_HIDDEN
PRODUCT_REJECTED
SHOP_RESTRICTED
SHOP_SUSPENDED
VENDOR_BANNED
FUNDS_FROZEN (chỉ khi policy và scope cho phép)

Chốt entity fields tối thiểu:

id
vendorId
shopId
policyCode
violationCode
severity
action
status
resourceType
resourceId
description
evidence
createdAt
resolvedAt
escalationId
moderatorDecision

Các field chưa có trong source phải được phân loại MISSING, không tự giả định đã tồn tại.

12. Escalation & Enforcement Contract — Phase 3A

Chốt flow:

Moderator Escalate
        ↓
Admin Review
        ↓
Enforcement Decision
        ↓
AuditLog

Phải xác định:

Điều kiện escalation dựa trên policy.

Case/evidence/reference.

Actor và authorization.

Admin enforcement actions.

Reason bắt buộc.

Violation status/action.

Audit writer.

State transition và error handling.

Không hard-code ngưỡng kiểu 3 lỗi = ban nếu policy chưa chốt.

13. KYC Contract — Phase 3B

Phân biệt:

KYC Verification
≠ Admin KYC Monitoring
≠ Shop Operational Approval

Chốt status, provider/reference, verification timestamp, rejection reason, PII protection và quyền truy cập.

KYC != APPROVED không được bán hàng đầy đủ nếu policy quy định như vậy.

Admin monitoring không được tự thay thế KYC provider.

14. AuditLog Contract — Phase 3C

Chốt AuditLog read-only history, không hard-delete từ UI.

Fields tối thiểu:

actorId
actorEmail
role
action
resourceType
resourceId
reason
severity
before
after
ip
createdAt

Phải chốt AuditLog Writer contract từ Phase 3A và các action cần ghi audit.

Nếu writer chưa có, phải có interface/contract thống nhất để enforcement module tích hợp được sau merge.

Không báo AuditLog đầy đủ nếu chỉ có UI hoặc entity nhưng chưa ghi được action thực tế.

15. Dashboard Contract — Phase 4A
Platform Overview

Users

Shops

Products

Orders

Financial Overview

KPI

	

Contract




GMV

	

Tổng giá trị giao dịch theo định nghĩa đã chốt




Platform Revenue

	

Doanh thu thực tế của nền tảng theo Finance contract




Vendor Sales

	

Doanh số của Vendor theo Finance contract




Vendor Payable

	

Khoản phải trả Vendor theo Settlement contract




Refund

	

Giá trị hoàn tiền theo Refund contract

Không được đồng nhất GMV với Platform Revenue.

Compliance / Risk

Pending KYC.

Pending Moderation.

Open Escalation.

Open Violation.

Suspended Shops.

Mỗi KPI phải xác định authoritative data source, query/aggregation, DTO, period semantics và empty/unavailable behavior.

Nếu Finance hoặc Compliance backend chưa có dữ liệu authoritative, phải hiển thị N/A hoặc Unavailable đúng contract; không fake số liệu, không gán 0 để ngụ ý không có dữ liệu.

Dashboard không được load toàn bộ Orders vào memory chỉ để tính KPI. Phải chốt hướng aggregation/count query phù hợp.

16. Voucher & Banner Contract — Phase 4B/4C
WEB Voucher — Phase 4B

Phân biệt WEB Voucher và SHOP Voucher.

Audit CRUD, validation, checkout, Order, discount, usage.

Chốt cancel/refund behavior nếu thuộc requirement.

Usage chỉ tăng khi Order được tạo thành công.

Không fake discount.

Không phá business rule của SHOP Voucher.

Banner — Phase 4C

Đối chiếu MARKETING_CONTENT_POLICY.

Chốt lifecycle:

DRAFT → SCHEDULED → ACTIVE → EXPIRED

Kiểm tra startAt, endAt, status, priority, scheduling và validation theo policy.

Không tự mở rộng scope nếu tài liệu không yêu cầu.

17. Dependency & Phase Execution Plan

Chốt thứ tự Phase:

GATE 0
  ↓
PHASE 1 — Moderator Foundation + Security
  ↓
PHASE 2A — Product Moderation
  ↓
PHASE 2B — Review Moderation
  ↓
PHASE 2C — ReportCase + History
  ↓
PHASE 3A — Admin Escalation + Enforcement
  ↓
PHASE 3B — KYC Monitoring
  ↓
PHASE 3C — Violation + AuditLog
  ↓
PHASE 4A — Dashboard Core
  ↓
PHASE 4B — WEB Voucher
  ↓
PHASE 4C — Banner

Đây là thứ tự nghiệp vụ dự kiến, không có nghĩa mọi Phase phải chờ toàn bộ code Phase trước mới được bắt đầu.

Với mỗi Phase, lập dependency matrix:

Phase

	

Contract cần

	

Module có thể phát triển độc lập

	

Integration cần

	

Blocker thực sự




Phase 1

	

Role/Security

	

Moderator Foundation

	

Security regression

	

Chưa chốt role/security




Phase 2A

	

Product Moderation

	

Queue/UI/service adapter theo contract

	

Product + Auto Moderation

	

Contract/policy chưa chốt




Phase 2B

	

Review Moderation

	

Review UI/service adapter

	

Review + Order

	

Contract/policy chưa chốt




Phase 2C

	

ReportCase

	

Case UI/API adapter

	

Product/Review/Report

	

ReportCase contract chưa chốt




Phase 3A

	

Escalation/Enforcement

	

Admin UI/contract integration

	

ReportCase + Violation + Audit

	

Enforcement policy chưa chốt




Phase 3B

	

KYC

	

Monitoring UI/adapter

	

KYC data source

	

KYC contract chưa chốt




Phase 3C

	

Violation/AuditLog

	

Query UI/adapter

	

Violation + Audit writer

	

Schema/security chưa chốt




Phase 4A

	

Dashboard KPI

	

Dashboard UI/aggregation adapter

	

Finance + Compliance data

	

KPI definition/data source chưa chốt




Phase 4B

	

Voucher

	

Voucher UI/validation theo contract

	

Checkout + Order

	

Discount/usage contract chưa chốt




Phase 4C

	

Banner

	

Banner UI/validation

	

Banner backend

	

Lifecycle/policy chưa chốt

Chỉ chuyển trạng thái BLOCKED khi thiếu contract hoặc dữ liệu/implementation đó thực sự ngăn mandatory requirement hoàn thành.

18. Owner & Responsibility Boundary

Không mặc định owner là dependency bắt buộc của Phase khác.

Owner

	

Trách nhiệm cần chốt tại Gate 0




Hoàn

	

Xác nhận module/phần code đang sở hữu; chốt contract với các module liên quan




Quốc Anh

	

Finance, Security hoặc module đang được phân công; cung cấp contract và integration boundary




Anh Tuấn

	

Moderation, ReportCase, Violation, KYC, Audit hoặc module đang được phân công




Mạnh Quân

	

Các module/business rule được phân công; chốt contract liên quan




Anh Quân

	

Các module/business rule được phân công; chốt contract liên quan

Đối chiếu repository, tài liệu phân công và xác nhận thực tế. Không tự gán trách nhiệm chỉ dựa trên tên người trong audit cũ.

Mỗi dependency phải ghi:

Owner module.

Contract/API cần cung cấp.

Phase tiêu thụ.

Có thể triển khai adapter độc lập không.

Điều kiện merge.

Integration test cần chạy.

19. Phân loại kết quả Audit

Mỗi requirement phải dùng một trong các trạng thái:

PASS / PARTIAL / MISSING / WRONG / DEPENDENCY / OUT OF SCOPE

Mỗi DEPENDENCY phải ghi rõ loại dependency, owner/module, ảnh hưởng và phương án tiếp tục triển khai độc lập.

Không ghi chung chung “backend chưa có” mà không chỉ ra contract hoặc requirement nào đang bị ảnh hưởng.

20. Output bắt buộc

Trả báo cáo theo format:

A. Gate 0 Status

READY / BLOCKED

B. Role & Security Audit

Item

	

Source hiện tại

	

Requirement

	

Status

	

Cần sửa tại Phase

C. Policy Alignment

Policy

	

Module ảnh hưởng

	

Source hiện tại

	

Status

	

Phase

D. Contract cần chốt

Liệt kê Role, Route, API, Entity/Enum, Status, Violation, Severity, Action, Audit, KYC, Escalation, Dashboard KPI, Voucher, Banner.

E. Dependency Matrix

Dependency

	

Loại

	

Owner

	

Phase bị ảnh hưởng

	

Phương án độc lập

	

Blocker thực sự

F. Phase Execution Plan

Ghi rõ từng Phase có thể triển khai phần nào độc lập, contract nào cần thống nhất và điều kiện integration.

G. Task cần chuyển Phase

Task

	

Phase hiện tại

	

Phase đề xuất

	

Lý do

H. Task thiếu / Task thừa

Chỉ liệt kê task có căn cứ từ source, policy và requirement.

I. Owner Boundary

Chốt trách nhiệm, module owner, contract cung cấp và integration responsibility của từng thành viên.

J. Gate 0 Acceptance Criteria

Gate 0 chỉ đạt READY khi:

Role và security boundary rõ ràng.

Admin/Moderator boundary rõ ràng.

Các contract nghiệp vụ quan trọng được xác định.

Không còn policy ambiguity ảnh hưởng đến implementation.

Phase 1 → Phase 4 có dependency matrix.

Có phương án phát triển độc lập cho implementation dependency.

Có contract/API/DTO boundary để merge code.

Owner/module ownership được xác định.

Không còn task quan trọng đặt sai Phase.

Không có dependency vòng chưa được giải quyết.

Có acceptance criteria và integration test strategy cho từng Phase.

File Luật & Chính sách đã được đối chiếu với các module liên quan.

Nếu còn warning không ảnh hưởng Phase 1, ghi WARNING; không tự mở rộng scope.

21. Quy tắc báo cáo cuối cùng

Kết thúc đúng format:

GATE 0 RESULT

STATUS: READY / BLOCKED

BLOCKER:
- ...

WARNING:
- ...

PHASE 1:
- ...

PHASE 2:
- ...

PHASE 3:
- ...

PHASE 4:
- ...

POLICY ISSUES:
- ...

DEPENDENCY:
- ...

OWNER ISSUES:
- ...

INDEPENDENT EXECUTION PLAN:
- ...

INTEGRATION / MERGE PLAN:
- ...

Không sửa source code. Không implement nghiệp vụ. Không commit/push.

Mục tiêu cuối cùng: Sau Gate 0, mỗi Phase có contract rõ ràng để có thể triển khai trong phạm vi code được giao, không bị phụ thuộc cứng vào implementation chưa có của thành viên khác, và khi merge vẫn tích hợp đúng nghiệp vụ, API, security và dữ liệu thực tế.