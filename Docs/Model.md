Phase	Nội dung	Model nên dùng	Effort	Vì sao
Gate 0	Contract, Decision, Dependency, Ownership	GPT-5.6 Sol	🔴 High	Cần đọc toàn bộ source + workbook + quyết định phạm vi, không được tự suy đoán
Phase 1	Auth / Login / Security / JWT	Claude Opus 5	🔴 High	Phức tạp nhất về Security, JWT, AccountStatus, Web/API login
Phase 2	Admin Shell	Claude Sonnet 5	🟡 Medium	Chủ yếu Thymeleaf, Fragment, CSS, Layout, Responsive
Phase 3	User Management	Claude Opus 5	🔴 High	Lock/Unlock liên quan trực tiếp Auth + AccountStatus + JWT
Phase 4	Category Management	Claude Sonnet 5	🟡 Medium	CRUD tương đối rõ, nhưng Delete có Product reference guard
Phase 5	Shop Approval	Claude Opus 5	🔴 High	Có state transition + dependency với Vendor
Phase 6	Order Management	Claude Opus 5	🔴 High	Order liên quan Customer/Vendor/Product/Shop/Status
Phase 7	Dashboard	Claude Sonnet 5	🟡 Medium	Statistics, MongoDB query/aggregation, Thymeleaf + chart
Phase 8	Final Integration / QA / Admin Completion	GPT-5.6 Sol	🔴 High	Cần review toàn bộ Admin, tìm thiếu sót và regression