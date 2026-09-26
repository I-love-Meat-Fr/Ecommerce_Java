PHASE 6 — CUSTOMER HOME USER DROPDOWN

0. IMPLEMENTATION INSTRUCTION

Phase 6 sửa 1 module Customer:

Home Header — User dropdown không hiện khi bấm avatar. Phải hiển thị cho MỌI role đăng nhập (ADMIN, MODERATOR, CUSTOMER, VENDOR).

Dựa trực tiếp trên:

Source code hiện tại (templates/web/fragments/header.html + CSS + JS).
Gate 0 — Source Audit + Scope Lock.
User report:
"Trên header không hiện thanh home-user-dropdown... Tôi muốn nó hiện thanh này dù đăng nhập bằng role nào đi chăng nữa".

CURRENT SOURCE
        ↓
HEADER FRAGMENT AUDIT
        ↓
CSS CLASS AUDIT
        ↓
JS TOGGLE AUDIT
        ↓
ROLE DETECTION FIX
        ↓
DROPDOWN TOGGLE FIX
        ↓
BUILD
        ↓
PHASE 6 READY

1. MỤC TIÊU

Phase 6 phải fix được:

User dropdown (#user-dropdown) hiện khi bấm avatar (#user-menu-btn).
Dropdown áp dụng cho MỌI role đăng nhập: ADMIN, MODERATOR, CUSTOMER, VENDOR.
Hiển thị đúng:
  - Username / email
  - Link "Đơn hàng của tôi" (Customer) / "Quản lý đơn" (Vendor) / "Trang Admin" (Admin) / "Trang kiểm duyệt" (Moderator)
  - Link "Giỏ hàng" (chỉ Customer)
  - Link "Profile"
  - Form logout

2. KẾT QUẢ ĐỐI CHIẾU SOURCE

Theo source audit:

Trang chủ template:
src/main/resources/templates/web/index.html (hoặc home.html)
src/main/resources/templates/web/fragments/header.html (fragments dùng chung)

Header structure từ user report:

```html
<button class="home-user-btn" id="user-menu-btn">
    <div class="home-user-avatar">
        <i class="fas fa-user"></i>
    </div>
    <i class="fas fa-chevron-down home-user-arrow"></i>
</button>

<div class="home-user-dropdown" id="user-dropdown">
    <div class="home-user-dropdown-header">
        <div class="home-user-name">moderator_test@gmail.com</div>
        <div class="home-user-email">Xem và chỉnh sửa hồ sơ</div>
    </div>
    <div class="home-user-dropdown-divider"></div>
    <a href="/orders" class="home-dropdown-item">
        <i class="fas fa-receipt"></i> Đơn hàng của tôi
    </a>
    <a href="/cart" class="home-dropdown-item">
        <i class="fas fa-shopping-cart"></i> Giỏ hàng
    </a>
    <a href="/moderator/dashboard" class="home-dropdown-item">
        <i class="fas fa-user-shield"></i> Trang kiểm duyệt
    </a>
    <div class="home-user-dropdown-divider"></div>
    <form action="/auth/logout" method="post">
        <button type="submit" class="home-dropdown-item home-dropdown-item-danger" id="logout-btn">
            <i class="fas fa-sign-out-alt"></i> Đăng xuất
        </button>
    </form>
</div>
```

⚠️ Cấu trúc đã có. Bug = dropdown không hiện khi click.

User báo: "Trên header thì không hiện thanh [dropdown]" → bug UI.

3. NGUYÊN TẮC BẮT BUỘC — KHÔNG XÓA MONGODB

Tuyệt đối KHÔNG:
deleteAll() / deleteMany({}) / drop() / dropDatabase()

Phase 6 không đụng MongoDB.

4. KHÔNG FAKE

Không fake user info chỉ để UI đẹp.

5. SOURCE OF TRUTH

CURRENT SOURCE > USER REPORT > HEADER HTML provided by user

6. DISCOVERY RULE

SEARCH → EXISTS? → INSPECT → FIX

7. PHẠM VI PHASE 6

| Module | Hành động | File chính |
|---|---|---|
| Header | Sửa dropdown toggle | templates/web/fragments/header.html |
| CSS | Sửa .home-user-dropdown ẩn/hiện | static/css/home-header.css (hoặc tương đương) |
| JS | Sửa/Thêm toggle script | static/js/home-header.js (hoặc tương đương) |
| Role-based items | Hiển thị link theo role | templates/web/fragments/header.html |

8. HEADER DROP DOWN CONTRACT

8.1 — ROLE DETECTION

Cần biết role user đăng nhập. Có 2 cách:

Cách 1: Server-side render (Thymeleaf)
```html
<div th:if="${session.user != null}">
    <!-- dropdown hiển thị -->
</div>
```

Cách 2: Client-side JavaScript
```javascript
const userRole = document.body.dataset.userRole || 'GUEST';
if (userRole !== 'GUEST') {
    // show dropdown
}
```

Phase 6 dùng Cách 1 (server-side render) — an toàn hơn cho security và SEO.

8.2 — TIEPHIS HEADER FRAGMENT

Header fragment hiện tại (theo user report) đã có cấu trúc dropdown. Cần:

1. Verify controller home đã truyền `session.user` / `currentUser` cho Thymeleaf.
2. Điều kiện `<div th:if="${session.user != null}">` cho dropdown.
3. Mỗi link trong dropdown theo role.

Ví dụ:

```html
<!-- User button -->
<button class="home-user-btn" id="user-menu-btn"
        th:if="${session.user != null}">
    <div class="home-user-avatar">
        <i class="fas fa-user"></i>
    </div>
    <i class="fas fa-chevron-down home-user-arrow"></i>
</button>

<!-- Guest button (chưa login) -->
<a href="/auth/login" class="home-user-btn" th:if="${session.user == null}">
    <i class="fas fa-user"></i> Đăng nhập
</a>

<!-- Dropdown -->
<div class="home-user-dropdown" id="user-dropdown"
     th:if="${session.user != null}">
    <div class="home-user-dropdown-header">
        <div class="home-user-name" th:text="${session.user.email}">user@email</div>
        <div class="home-user-email">Xem và chỉnh sửa hồ sơ</div>
    </div>

    <div class="home-user-dropdown-divider"></div>

    <!-- CUSTOMER + VENDOR links -->
    <a href="/orders" class="home-dropdown-item"
       th:if="${session.user.role.name() == 'CUSTOMER' or session.user.role.name() == 'VENDOR'}">
        <i class="fas fa-receipt"></i> Đơn hàng của tôi
    </a>

    <a href="/cart" class="home-dropdown-item"
       th:if="${session.user.role.name() == 'CUSTOMER'}">
        <i class="fas fa-shopping-cart"></i> Giỏ hàng
    </a>

    <!-- ADMIN link -->
    <a href="/admin/dashboard" class="home-dropdown-item"
       th:if="${session.user.role.name() == 'ADMIN'}">
        <i class="fas fa-cogs"></i> Trang quản trị
    </a>

    <!-- MODERATOR link -->
    <a href="/moderator/dashboard" class="home-dropdown-item"
       th:if="${session.user.role.name() == 'MODERATOR'}">
        <i class="fas fa-user-shield"></i> Trang kiểm duyệt
    </a>

    <!-- VENDOR link -->
    <a href="/vendor/dashboard" class="home-dropdown-item"
       th:if="${session.user.role.name() == 'VENDOR'}">
        <i class="fas fa-store"></i> Trang bán hàng
    </a>

    <a href="/profile" class="home-dropdown-item">
        <i class="fas fa-user-circle"></i> Hồ sơ
    </a>

    <div class="home-user-dropdown-divider"></div>

    <form action="/auth/logout" method="post">
        <button type="submit" class="home-dropdown-item home-dropdown-item-danger" id="logout-btn">
            <i class="fas fa-sign-out-alt"></i> Đăng xuất
        </button>
    </form>
</div>
```

8.3 — CSS

CSS hiện tại (theo user report):

```css
.home-user-dropdown {
    display: none;
    position: absolute;
    top: 100%;
    right: 0;
    background: white;
    border-radius: 8px;
    box-shadow: 0 4px 12px rgba(0,0,0,0.15);
    min-width: 200px;
    z-index: 1000;
}

.home-user-dropdown.show {
    display: block;
}
```

Có thể CSS chưa có `.show` class hoặc JS chưa toggle.

Đảm bảo:

```css
.home-user-dropdown {
    display: none;
    position: absolute;
    top: 100%;
    right: 0;
    background: white;
    border-radius: 8px;
    box-shadow: 0 4px 12px rgba(0,0,0,0.15);
    min-width: 220px;
    z-index: 1000;
    padding: 8px 0;
}

.home-user-dropdown.show {
    display: block;
}

.home-user-btn {
    position: relative;  /* cho dropdown absolute positioning */
    cursor: pointer;
}

/* Click bên ngoài đóng dropdown */
.home-user-dropdown-overlay {
    display: none;
    position: fixed;
    top: 0; left: 0; right: 0; bottom: 0;
    z-index: 999;
}
.home-user-dropdown-overlay.show {
    display: block;
}
```

8.4 — JS

Tìm file JS hiện tại cho home header (có thể là home.js, header.js, hoặc inline).

```javascript
document.addEventListener('DOMContentLoaded', function() {
    const btn = document.getElementById('user-menu-btn');
    const dropdown = document.getElementById('user-dropdown');
    
    if (!btn || !dropdown) return;  // GUEST thì button có thể không có
    
    btn.addEventListener('click', function(e) {
        e.preventDefault();
        e.stopPropagation();
        dropdown.classList.toggle('show');
    });
    
    document.addEventListener('click', function(e) {
        if (!btn.contains(e.target) && !dropdown.contains(e.target)) {
            dropdown.classList.remove('show');
        }
    });
    
    document.addEventListener('keydown', function(e) {
        if (e.key === 'Escape') {
            dropdown.classList.remove('show');
        }
    });
});
```

Nếu JS đã có nhưng lỗi:
- btn có thể null nếu user GUEST (button không render).
- event listener không attach đúng.
- CSS class .show không đúng tên.

8.5 — INTERCEPTOR (OPTIONAL)

Có thể có Interceptor set `currentUser` vào request → Thymeleaf dùng.

Đọc controller home (HomeController hoặc WebController):

```java
@GetMapping("/")
public String home(HttpServletRequest request, Model model) {
    // Get current user from security context
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        model.addAttribute("currentUser", userDetails);
    }
    return "web/index";  // hoặc home.html
}
```

Sau đó header dùng `${currentUser != null}` thay vì `${session.user}`.

8.6 — AVATAR CLICK GUEST

Nếu user GUEST, button avatar không có / chuyển thành link "Đăng nhập":

```html
<a href="/auth/login" class="home-user-btn" th:if="${currentUser == null}">
    <i class="fas fa-user"></i>
    <span>Đăng nhập</span>
</a>
```

9. HEADER TEST

```java
@Test void headerUser_authenticatedUser_showsAvatar() { ... }
@Test void headerUser_anonymous_showsLoginLink() { ... }
@Test void headerDropdown_adminUser_showsAdminLink() { ... }
@Test void headerDropdown_moderatorUser_showsModeratorLink() { ... }
@Test void headerDropdown_customerUser_showsOrdersAndCart() { ... }
@Test void headerDropdown_vendorUser_showsVendorLink() { ... }
@Test void headerDropdown_logout_postsToLogout() { ... }
```

(Manual test thay vì automated vì liên quan đến JS event.)

10. HEADER REGRESSION

- Customer home vẫn load (Phase 2 §5.1 đã verify `/` → 302 → /auth/login cho guest)
- Auth flow vẫn hoạt động (Phase 1 verified)
- Các trang khác không bị hỏng header

11. HEADER SECURITY

- Dropdown chỉ hiển thị khi authenticated (server-side render, không thể bypass client-side).
- Logout endpoint đã có (/auth/logout POST).
- Không leak user info qua URL hoặc hidden form.

12. EXISTING DATA

Không đụng MongoDB.

13. HEADER MONGODB SAFETY

Không có write.

14. TEST DATA

Không cần.

15. KHÔNG LÀM MẤT CHỨC NĂNG HIỆN TẠI

Không được làm hỏng:
- Auth flow
- Cart count interceptor (đã có)
- Home page banner display
- Login redirect sau khi login

16. FILE CHANGE REPORT

Files Modified:
- src/main/resources/templates/web/fragments/header.html (thêm th:if cho từng role)
- src/main/resources/static/css/home-header.css (hoặc tương đương) (nếu CSS thiếu .show)
- src/main/resources/static/js/home-header.js (hoặc tương đương) (sửa toggle nếu sai)

Files Created (nếu chưa có):
- src/main/resources/static/css/home-header.css
- src/main/resources/static/js/home-header.js

Files Deleted:
NONE

17. DATA CHANGE REPORT

MongoDB:
Existing data: PRESERVED
No writes

18. BUILD REPORT

Command: mvn clean package -DskipTests
Result: PASS / FAIL

19. MANUAL TEST REPORT

V1: Truy cập GET / khi chưa login → 302 → /auth/login (giữ nguyên)
V2: Login CUSTOMER → redirect → home
V3: Trên header, click avatar → dropdown hiện
V4: Dropdown có:
  - Email customer
  - "Đơn hàng của tôi"
  - "Giỏ hàng"
  - "Hồ sơ"
  - "Đăng xuất"
V5: Click bên ngoài → dropdown đóng
V6: ESC → dropdown đóng
V7: Click "Đơn hàng của tôi" → /orders
V8: Click "Giỏ hàng" → /cart
V9: Click "Đăng xuất" → logout → redirect login

V10: Login ADMIN → home
V11: Click avatar → dropdown có "Trang quản trị"
V12: Click → /admin/dashboard

V13: Login MODERATOR → home
V14: Click avatar → dropdown có "Trang kiểm duyệt"
V15: Click → /moderator/dashboard

V16: Login VENDOR → home
V17: Click avatar → dropdown có "Trang bán hàng" + "Đơn hàng của tôi"
V18: Click → tương ứng

20. KNOWN ISSUES

Mỗi issue:
Issue: dropdown không hiện
Root Cause: có thể JS thiếu event listener, CSS thiếu .show class, hoặc th:if chưa pass
Phase 6 Status: ...

21. STATUS CLASSIFICATION

EXISTING / FIXED / PARTIAL / MISSING / WRONG / OUT OF SCOPE

22. ACCEPTANCE CRITERIA

Phase 6 PASS khi:
- Mọi role đăng nhập (ADMIN, MODERATOR, CUSTOMER, VENDOR) thấy dropdown khi click avatar
- Dropdown hiển thị đúng link theo role
- Click bên ngoài đóng dropdown
- ESC đóng dropdown
- Logout hoạt động
- Manual test V1-V18 PASS
- Build PASS
- Không làm hỏng home page hiện tại
- Không làm hỏng auth flow

23. ĐIỀU KIỆN KẾT THÚC

Phase 6 PASS khi:
- Header dropdown hoạt động đúng cho 4 roles
- Manual test PASS
- Build PASS
- Không MongoDB write
- Không hỏng chức năng khác
