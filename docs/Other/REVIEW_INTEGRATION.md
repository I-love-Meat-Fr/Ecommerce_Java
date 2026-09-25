# Review Integration Documentation

## Table of Contents

1. [Architecture Analysis](#1-architecture-analysis)
2. [Database](#2-database)
3. [MongoDB Schema](#3-mongodb-schema)
4. [MongoDB Indexes](#4-mongodb-indexes)
5. [API Endpoints](#5-api-endpoints)
6. [Create Review Flow](#6-create-review-flow)
7. [Read Reviews Flow](#7-read-reviews-flow)
8. [Edit Review Flow](#8-edit-review-flow)
9. [Delete Review Flow](#9-delete-review-flow)
10. [Ownership Validation](#10-ownership-validation)
11. [Duplicate Review Prevention](#11-duplicate-review-prevention)
12. [Product Rating Auto-Update](#12-product-rating-auto-update)
13. [Purchase Verification (#21)](#13-purchase-verification-21)
14. [Error Handling](#14-error-handling)
15. [Security Requirements](#15-security-requirements)
16. [Frontend Integration Guide (for Anh Quân)](#16-frontend-integration-guide-for-anh-quan)
17. [Test Cases](#17-test-cases)
18. [Task Status](#18-task-status)
19. [Files Reference](#19-files-reference)
20. [Ownership Boundaries](#20-ownership-boundaries)
21. [Instructions for Future Cursor Sessions](#21-instructions-for-future-cursor-sessions)

---

## 1. Architecture Analysis

### 1.1 Current Architecture

| Layer | Technology | Implementation |
|-------|------------|----------------|
| Backend framework | Spring Boot 3.2.0 | — |
| Database | **MongoDB only** (no SQL) | `spring-boot-starter-data-mongodb` |
| Authentication | Spring Security + JWT | `JwtAuthenticationFilter`, `CustomUserDetails` |
| Frontend | Thymeleaf templates | `src/main/resources/templates/web/` |
| Build tool | Maven | `pom.xml` |

**Database configuration** (from `application.yml`):

```yaml
spring:
  data:
    mongodb:
      uri: ${MONGODB_URI:...}
      database: ${MONGODB_DATABASE:cnj70_ecommerce}
      auto-index-creation: true
```

All existing entities (Product, User, Order, Cart, Voucher, Review) are MongoDB documents.
**No SQL database is present in this project.**

### 1.2 Existing Review Code

The Review module was **already fully implemented** by a previous team member before this audit.
The following files exist in the codebase:

| Type | File |
|------|------|
| Document | `document/Review.java` |
| Repository | `repository/ReviewRepository.java` |
| Service Interface | `service/ReviewService.java` |
| Service Impl | `service/impl/ReviewServiceImpl.java` |
| Controller | `controller/web/ReviewController.java` |
| Request DTO | `dto/request/ReviewReq.java` |
| Response DTO | `dto/response/ReviewRes.java` |
| UI Template | `templates/web/product-detail.html` (Reviews tab section) |
| Product Controller | `controller/web/ProductController.java` (loads reviews for product detail) |

**Architecture:**

```
HTTP Request (Thymeleaf form or AJAX)
        ↓
ReviewController          ← Web controller, auth check, redirects
        ↓
ReviewService             ← Business logic interface
        ↓
ReviewServiceImpl         ← Validation, ownership, MongoDB operations
        ↓
ReviewRepository          ← Spring Data MongoDB
        ↓
MongoDB  →  "reviews" collection
```

### 1.3 User Identification Method

`CustomUserDetails` is injected via `@AuthenticationPrincipal` in controllers:

```java
@AuthenticationPrincipal CustomUserDetails user
// user.getId() → String userId (from JWT)
```

The `userId` is **never read from the request body**. It comes from the authenticated session/JWT token.

### 1.4 Product Identification Method

Product ID is a MongoDB `String` (`@Id`). Verified via `ProductRepository.findById(productId)`.

### 1.5 Existing API Conventions

- Thymeleaf `@Controller` returning `String` view names + `Model` for page rendering
- `@ResponseBody` + `ResponseEntity<?>` for JSON API endpoints
- `RedirectAttributes` for flash attributes after form submission
- Error messages passed via `model.addAttribute("error", ...)` or query param `?error=...`

---

## 2. Database

### 2.1 MongoDB Configuration

- **URI:** from `${MONGODB_URI}` environment variable (fallback to Atlas cluster)
- **Database name:** `cnj70_ecommerce` (from `${MONGODB_DATABASE}`)
- **Auto-index-creation:** `true` — indexes defined via `@Indexed` annotations are created automatically on startup

### 2.2 Collection

**Collection name:** `reviews`

No other database (SQL) is used in this project.

---

## 3. MongoDB Schema

### 3.1 Document Structure

```json
{
  "_id": "ObjectId(\"...\")",
  "productId": "prod-001",
  "userId": "user-001",
  "userName": "Nguyễn Văn A",
  "userAvatar": "https://cdn.example.com/avatars/user-001.jpg",
  "rating": 5,
  "comment": "Sản phẩm rất tốt, giao hàng nhanh!",
  "createdAt": "2026-08-30T14:00:00"
}
```

### 3.2 Field Reference

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `_id` | String (ObjectId) | auto | MongoDB document ID |
| `productId` | String | yes | Links to `Product.id`. Indexed for query performance. |
| `userId` | String | yes | Links to `User.id`. Indexed for ownership queries. |
| `userName` | String | yes | Copied from `User.fullName` at creation time. **Not updated** when User changes name. |
| `userAvatar` | String | yes | Copied from `User.avatarUrl` at creation time. **Not updated** when User changes avatar. |
| `rating` | int | yes | 1–5. Validated server-side. |
| `comment` | String | yes | Non-empty, trimmed. Max length: no hard limit enforced by document. |
| `createdAt` | LocalDateTime | auto | Set by Spring Data `@CreatedDate`. |

### 3.3 Design Decisions

- **`userName` and `userAvatar` are snapshots**: They are copied at review creation time.
  This preserves review integrity (the name shown matches what the user had when they wrote it).
  It also avoids a dependency on the User module for every review read.
- **No `updatedAt` field**: The existing implementation does not track when a review is edited.
  The `createdAt` field is preserved on update.
- **No soft-delete**: The project does not use soft-delete for reviews. Deletion is permanent (hard delete).

---

## 4. MongoDB Indexes

### 4.1 Existing Indexes

Indexes are defined via `@Indexed` annotations on the `Review` document class and created
automatically by Spring Data MongoDB when `auto-index-creation: true`.

| Index | Field | Type | Purpose |
|-------|-------|------|---------|
| `@Indexed` on `productId` | `productId` | ascending | Speeds up `findByProductId(productId)` — used for listing reviews on product detail page |
| `@Indexed` on `userId` | `userId` | ascending | Speeds up `findByUserId(userId)` — used for "my reviews" page and ownership lookups |
| Compound (derived) | `productId + userId` | via `findByProductIdAndUserId` | Used for duplicate-review check and `hasUserReviewedProduct` |

### 4.2 Query Patterns

| Query | Method | Index Used |
|-------|--------|------------|
| Get all reviews for a product | `findByProductIdOrderByCreatedAtDesc` | `productId` |
| Get all reviews by a user | `findByUserId` | `userId` |
| Check duplicate review | `findByProductIdAndUserId` | compound on `productId + userId` |
| Count reviews per product | `countByProductId` | `productId` |
| Get average rating | `findByProductId` → Java stream | `productId` |

---

## 5. API Endpoints

All endpoints are in `ReviewController.java`.

### 5.1 GET /products/{productId}/reviews

**Purpose:** Get all reviews for a product (public JSON API).

**Authentication:** none (public)

**Method:** `GET`

**Path variable:** `productId` (String)

**Response:** `200 OK`

```json
[
  {
    "id": "abc123",
    "productId": "prod-001",
    "userId": "user-001",
    "userName": "Nguyễn Văn A",
    "userAvatar": "https://cdn.example.com/avatars/user-001.jpg",
    "rating": 5,
    "comment": "Sản phẩm rất tốt!",
    "createdAt": "2026-08-30T14:00:00"
  }
]
```

**Notes:** Returns reviews sorted newest-first (`findByProductIdOrderByCreatedAtDesc`).

---

### 5.2 POST /products/{productId}/reviews

**Purpose:** Create a review for a product.

**Authentication:** required (redirects to `/auth/login` if not authenticated)

**Method:** `POST`

**Content-Type:** `application/x-www-form-urlencoded`

**Path variable:** `productId` (String)

**Request body:**

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| `rating` | int | yes | 1–5, `@NotNull`, `@Min(1)`, `@Max(5)` |
| `comment` | String | yes | non-blank, `@NotBlank`, trimmed |

**Success response:** `302 Redirect` to `/products/{productId}`

**Error response:** `302 Redirect` to `/products/{productId}?error=<message>`

**Validation chain:**

1. User authenticated → yes / redirect to login
2. Product exists (`ProductRepository.findById`) → yes / `400 Bad Request`
3. User exists (`UserRepository.findById`) → yes / `400 Bad Request`
4. User has NOT already reviewed this product → yes / `400 Bad Request`
5. Rating 1–5 → valid / `400 Bad Request`
6. Comment non-empty (after trim) → valid / `400 Bad Request`
7. Save review to MongoDB
8. Update `Product.rating` and `Product.reviewCount`

**Note:** `userId` is taken from `CustomUserDetails.getId()`, **never from request body**.

---

### 5.3 GET /reviews/{reviewId}

**Purpose:** Get a single review by ID.

**Authentication:** none (public)

**Method:** `GET`

**Path variable:** `reviewId` (String)

**Response:** `200 OK`

```json
{
  "id": "abc123",
  "productId": "prod-001",
  "userId": "user-001",
  "userName": "Nguyễn Văn A",
  "userAvatar": "https://cdn.example.com/avatars/user-001.jpg",
  "rating": 5,
  "comment": "Sản phẩm rất tốt!",
  "createdAt": "2026-08-30T14:00:00"
}
```

---

### 5.4 POST /reviews/{reviewId}/edit

**Purpose:** Update own review.

**Authentication:** required + **owner-only**

**Method:** `POST`

**Content-Type:** `application/x-www-form-urlencoded`

**Path variable:** `reviewId` (String)

**Request body:**

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| `rating` | int | yes | 1–5 |
| `comment` | String | yes | non-blank, trimmed |

**Success response:** `302 Redirect` to `/products/{productId}`

**Error response:** `302 Redirect` to `/products/{productId}?error=<message>`

**Validation chain:**

1. User authenticated → yes / redirect to login
2. Review exists → yes / `400 Bad Request`
3. **Ownership check:** `Review.userId == CurrentUser.id` → yes / `400 Bad Request`
4. Rating 1–5 → valid / `400 Bad Request`
5. Comment non-empty (after trim) → valid / `400 Bad Request`
6. Update `review.rating` and `review.comment` only — `productId`, `userId`, `createdAt` are **never changed**
7. Save updated review to MongoDB
8. Update `Product.rating` and `Product.reviewCount`

---

### 5.5 POST /reviews/{reviewId}/delete

**Purpose:** Delete own review.

**Authentication:** required + **owner-only**

**Method:** `POST`

**Path variable:** `reviewId` (String)

**Success response:** `302 Redirect` to `/products/{productId}`

**Validation chain:**

1. User authenticated → yes / redirect to login
2. Review exists → yes / `400 Bad Request`
3. **Ownership check:** `Review.userId == CurrentUser.id` → yes / `400 Bad Request`
4. Hard-delete review from MongoDB
5. Update `Product.rating` and `Product.reviewCount`

**Note:** Uses hard delete. No soft-delete mechanism exists in this project.

---

### 5.6 GET /my-reviews

**Purpose:** List all reviews by the current authenticated user.

**Authentication:** required

**Method:** `GET`

**Response:** renders `web/my-reviews` template with `List<ReviewRes>` in model.

---

### 5.7 GET /api/reviews/check

**Purpose:** Check whether the current user has already reviewed a product.

**Authentication:** optional (returns `false` if not logged in)

**Method:** `GET`

**Query parameter:** `productId` (String)

**Response:** `200 OK`

```json
true
```

**Use case:** The product detail page uses this to show/hide the review form.

---

## 6. Create Review Flow

```
Customer (authenticated)
        ↓
Opens Product Detail page (/products/{productId})
        ↓
Clicks "Viết đánh giá" tab or button
        ↓
Fills form: rating (1-5 stars) + comment textarea
        ↓
Submits form → POST /products/{productId}/reviews
        ↓
ReviewController.createReview()
        ↓
@AuthenticationPrincipal → CurrentUser.id  ← NEVER from request body
        ↓
ReviewService.createReview(userId, productId, rating, comment)
        ↓
        ├─ Validate rating: 1 ≤ rating ≤ 5
        ├─ Validate comment: not blank after trim
        ├─ Verify Product exists (ProductRepository.findById)
        ├─ Verify User exists (UserRepository.findById)
        ├─ Check NOT duplicate: findByProductIdAndUserId
        │      └─ If found → BadRequestException
        ├─ Build Review (snapshot userName + userAvatar from User)
        ├─ Save to MongoDB reviews collection
        ├─ updateProductRating(productId)
        │      └─ Compute avg rating + count from all reviews
        │      └─ Save to Product document
        └─ Return saved Review
        ↓
Redirect to /products/{productId}
        ↓
Page reloads → reviews tab shows new review
```

---

## 7. Read Reviews Flow

### 7.1 Server-rendered (Product Detail page)

```
Product Detail page loads (/products/{productId})
        ↓
ProductController.productDetail()
        ↓
  ├─ Fetch Product by ID
  ├─ Fetch List<Review> by productId
  │      └─ reviewService.getReviewsByProductId(productId)
  │             └─ reviewRepository.findByProductIdOrderByCreatedAtDesc
  ├─ Map to List<ReviewRes> (DTO)
  ├─ If user logged in: check hasUserReviewedProduct(userId, productId)
  ├─ Add to model: reviews, hasReviewed
  └─ Return "web/product-detail" view
        ↓
Thymeleaf renders ${reviews} list in tab-reviews section
```

### 7.2 JSON API (for AJAX frontend)

```
GET /products/{productId}/reviews
        ↓
ReviewController.getProductReviews(productId)
        ↓
reviewService.getReviewsByProductId(productId)
        ↓
ResponseEntity<List<ReviewRes>> → 200 OK JSON
```

---

## 8. Edit Review Flow

```
Customer (authenticated, owns the review)
        ↓
Opens Product Detail page → reviews tab
        ↓
Clicks "Sửa" (edit) button on own review
        ↓
JavaScript editReview(reviewId, rating, comment) → opens modal
        ↓
Edits rating / comment in modal form
        ↓
Submits → POST /reviews/{reviewId}/edit
        ↓
ReviewController.updateReview()
        ↓
  ├─ Check user authenticated
  ├─ reviewService.updateReview(reviewId, userId, rating, comment)
  │      └─ Find review by reviewId
  │      └─ Ownership check: review.userId == userId → else BadRequestException
  │      └─ Validate rating + comment
  │      └─ Update review.rating + review.comment ONLY
  │             (productId, userId, createdAt are immutable)
  │      └─ Save to MongoDB
  │      └─ updateProductRating(productId)
  └─ Return
        ↓
Redirect to /products/{productId}
```

---

## 9. Delete Review Flow

```
Customer (authenticated, owns the review)
        ↓
Opens Product Detail page → reviews tab
        ↓
Clicks "Xóa" (delete) button on own review
        ↓
Browser shows confirm() dialog
        ↓
Submits → POST /reviews/{reviewId}/delete
        ↓
ReviewController.deleteReview()
        ↓
  ├─ Check user authenticated
  ├─ Find review to get productId
  ├─ reviewService.deleteReview(reviewId, userId)
  │      └─ Find review by reviewId
  │      └─ Ownership check: review.userId == userId → else BadRequestException
  │      └─ Hard-delete from MongoDB
  │      └─ updateProductRating(productId)
  └─ Return
        ↓
Redirect to /products/{productId}
```

---

## 10. Ownership Validation

**Critical security requirement.**

The backend **never** trusts `userId` from the client.

### 10.1 Wrong approach (do not use)

```json
// Client sends:
{ "reviewId": 10, "userId": 5, "rating": 4, "comment": "..." }
```

```java
// WRONG: Backend uses userId from client
reviewService.deleteReview(reviewId, request.getUserId());
```

### 10.2 Correct approach (implemented)

```java
// RIGHT: Backend uses userId from authenticated session
@AuthenticationPrincipal CustomUserDetails user
reviewService.deleteReview(reviewId, user.getId());
```

The `ReviewServiceImpl` then validates:

```java
Review review = reviewRepository.findById(reviewId)
    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đánh giá"));

if (!review.getUserId().equals(userId)) {
    throw new BadRequestException("Bạn không có quyền xóa đánh giá này");
}
```

### 10.3 Ownership cannot be changed

During edit, `productId` and `userId` are **never updated**:

```java
// Only these fields change:
review.setRating(rating);
review.setComment(comment.trim());
// productId and userId remain from the existing database record
```

---

## 11. Duplicate Review Prevention

The project enforces **one review per user per product**.

Enforcement method in `ReviewServiceImpl.createReview`:

```java
if (reviewRepository.findByProductIdAndUserId(productId, userId).isPresent()) {
    throw new BadRequestException("Bạn đã đánh giá sản phẩm này rồi");
}
```

The `ReviewRepository.findByProductIdAndUserId` method uses the compound
`productId + userId` query pattern. The `@Indexed` annotation on both fields
ensures this query is efficient.

---

## 12. Product Rating Auto-Update

After every create, update, or delete review, `ReviewServiceImpl` calls:

```java
private void updateProductRating(String productId) {
    Product product = productRepository.findById(productId).orElse(null);
    if (product != null) {
        double avgRating = getAverageRatingByProductId(productId);
        int reviewCount = getReviewCountByProductId(productId);
        product.setRating(avgRating);
        product.setReviewCount(reviewCount);
        productRepository.save(product);
    }
}
```

This keeps `Product.rating` (average) and `Product.reviewCount` always
synchronized with the actual review data in MongoDB.

---

## 13. Purchase Verification (#21)

### 13.1 Status: WAITING FOR REQUIREMENT

The phrase "nếu hệ thống yêu cầu" (if the system requires it) makes this conditional.

**Current behavior:** Any authenticated user can write a review, regardless of purchase history.
The system does **not** currently enforce purchase-before-review.

### 13.2 How to implement if required

If the team decides to require purchase verification, the Review module can call
the existing `OrderService`:

```java
// In ReviewServiceImpl (conceptual — do not implement without team decision)
List<Order> orders = orderService.getOrdersByUserId(userId);
boolean hasPurchased = orders.stream()
    .flatMap(o -> o.getItems().stream())
    .anyMatch(item -> item.getProductId().equals(productId));

if (!hasPurchased) {
    throw new BadRequestException("Bạn cần mua sản phẩm trước khi đánh giá");
}
```

**Required from Order module:**

- `OrderService.getOrdersByUserId(userId)` — already exists
- `Order.items` contains `OrderItem.productId`
- A completed/delivered order status check may be needed

**Important:** Do NOT modify `OrderServiceImpl` or `Order` entity.
Create a Review-side adapter or request the Order member to add a helper method.

### 13.3 Integration for Quốc Anh

If you want to make purchase verification easier for Review, add a method to `OrderService`:

```java
boolean hasUserPurchasedProduct(String userId, String productId);
```

This would eliminate the need for Review to iterate over all orders.

---

## 14. Error Handling

All errors use the project's existing exception handling (`GlobalExceptionHandler`):

| Case | Exception | HTTP Status | User Message |
|------|-----------|------------|-------------|
| Product not found | `ResourceNotFoundException` | 400 | Không tìm thấy sản phẩm |
| User not found | `ResourceNotFoundException` | 400 | Không tìm thấy người dùng |
| Review not found | `ResourceNotFoundException` | 400 | Không tìm thấy đánh giá |
| Rating out of range | `BadRequestException` | 400 | Rating phải từ 1 đến 5 sao |
| Empty comment | `BadRequestException` | 400 | Nội dung đánh giá không được để trống |
| Duplicate review | `BadRequestException` | 400 | Bạn đã đánh giá sản phẩm này rồi |
| Not owner (edit) | `BadRequestException` | 400 | Bạn không có quyền sửa đánh giá này |
| Not owner (delete) | `BadRequestException` | 400 | Bạn không có quyền xóa đánh giá này |
| Not authenticated | `redirect:/auth/login` | 302 | — |

No stack traces or internal details are exposed to the client.

---

## 15. Security Requirements

| Requirement | Implementation |
|-------------|----------------|
| Authenticated user only (create) | `@AuthenticationPrincipal CustomUserDetails` — null check |
| Authenticated user only (edit/delete) | `@AuthenticationPrincipal CustomUserDetails` — null check |
| Owner-only edit | `Review.userId == CurrentUser.id` in `ReviewServiceImpl` |
| Owner-only delete | `Review.userId == CurrentUser.id` in `ReviewServiceImpl` |
| Never trust `userId` from client | `userId` comes from JWT/auth session, not request body |
| Never trust `rating` from client | Validated server-side: `1 ≤ rating ≤ 5` |
| Never trust `comment` from client | Validated server-side: non-blank after trim |
| Never change ownership on edit | Only `rating` and `comment` are updated |
| Never expose sensitive user data | `ReviewRes` has no password, token, or security fields |
| Verify product exists | `ProductRepository.findById` called before saving |

---

## 16. Frontend Integration Guide (for Anh Quân)

### 16.1 Current State

The `product-detail.html` template **already contains a complete Review UI**.
You do not need to build anything from scratch unless you want to switch
to AJAX (non-refresh) interactions.

### 16.2 Model Attributes (set by ProductController)

| Attribute | Type | When set | Description |
|-----------|------|----------|-------------|
| `reviews` | `List<ReviewRes>` | always | All reviews for the product |
| `hasReviewed` | `Boolean` | when user logged in | `true` if current user already reviewed |
| `product` | `Product` | always | Has `rating` (avg) and `reviewCount` fields |

### 16.3 Server-Rendered Review List (already working)

The Thymeleaf template iterates over `${reviews}` and renders each review:

```html
<th:block th:each="review : ${reviews}">
  <div class="pdp-review-item" th:id="'review-' + ${review.id}">
    <div class="pdp-review-avatar">
      <i class="fas fa-user"></i>
    </div>
    <div class="pdp-review-content">
      <div class="pdp-review-header">
        <span class="pdp-review-author" th:text="${review.userName}">...</span>
        <!-- star icons -->
      </div>
      <span class="pdp-review-date" th:text="${#temporals.format(review.createdAt, 'dd/MM/yyyy')}">...</span>
      <div class="pdp-review-text" th:text="${review.comment}">...</div>
      
      <!-- Edit/Delete — only for own review -->
      <div class="pdp-review-actions"
           sec:authorize="isAuthenticated()"
           th:if="${#authentication.principal.id == review.userId}">
        <button onclick="editReview(...)">Sửa</button>
        <form th:action="@{/reviews/{id}/delete(id=${review.id})}" method="post">Xóa</form>
      </div>
    </div>
  </div>
</th:block>
```

### 16.4 Review Form (already working)

- Shows when: user is logged in AND `hasReviewed == false`
- Submits: `POST /products/{productId}/reviews` with `rating` + `comment`
- Redirects back to product detail on success/error

### 16.5 Edit Modal (already working)

- JavaScript `editReview(reviewId, currentRating, currentComment)` opens a modal
- Form submits: `POST /reviews/{reviewId}/edit` with `rating` + `comment`
- Modal has cancel button and keyboard escape to close

### 16.6 Delete (already working)

- Delete button with `confirm()` dialog
- Submits: `POST /reviews/{reviewId}/delete`
- Redirects back to product detail

### 16.7 AJAX Alternative (if you want non-refresh)

If you prefer AJAX interactions instead of form-submit with redirect:

**Create review:**

```javascript
fetch('/products/' + productId + '/reviews', {
  method: 'POST',
  headers: { 'Content-Type': 'application/x-www-form-urlencoded',
             'X-Requested-With': 'XMLHttpRequest' },
  body: 'rating=' + rating + '&comment=' + encodeURIComponent(comment)
}).then(r => {
  if (r.redirected) window.location.href = r.url;
});
```

**Check if user reviewed:**

```javascript
fetch('/api/reviews/check?productId=' + productId)
  .then(r => r.json())
  .then(hasReviewed => {
    if (hasReviewed) { /* hide form, show "already reviewed" */ }
  });
```

**Get reviews (JSON):**

```javascript
fetch('/products/' + productId + '/reviews')
  .then(r => r.json())
  .then(reviews => { /* render reviews list */ });
```

---

## 17. Test Cases

The following test cases should be implemented in `ReviewServiceImplTest.java`.

### 17.1 Create Review Tests

**TC01 — Authenticated user creates valid review → Success**

```
Input: userId="u1", productId="p1", rating=5, comment="Good product"
Expected: Review saved in MongoDB, Product.rating updated
```

**TC02 — Unauthenticated user creates review → Denied**

```
Input: user = null
Expected: ReviewController redirects to /auth/login
```

**TC03 — Product does not exist → Error**

```
Input: productId="nonexistent"
Expected: ResourceNotFoundException("Không tìm thấy sản phẩm")
```

**TC04 — Rating = 0 → Validation error**

```
Input: rating=0
Expected: BadRequestException("Rating phải từ 1 đến 5 sao")
```

**TC05 — Rating = 6 → Validation error**

```
Input: rating=6
Expected: BadRequestException("Rating phải từ 1 đến 5 sao")
```

**TC06 — Rating = -1 → Validation error**

```
Input: rating=-1
Expected: BadRequestException("Rating phải từ 1 đến 5 sao")
```

**TC07 — Empty comment → Validation error**

```
Input: comment=""
Expected: BadRequestException("Nội dung đánh giá không được để trống")
```

**TC08 — Whitespace-only comment → Validation error**

```
Input: comment="     "
Expected: BadRequestException("Nội dung đánh giá không được để trống")
```

**TC09 — Duplicate review → Error**

```
Input: User u1 already reviewed Product p1, then tries to review again
Expected: BadRequestException("Bạn đã đánh giá sản phẩm này rồi")
```

### 17.2 Read Review Tests

**TC10 — Get reviews for Product A → Only Product A reviews**

```
Input: productId="p1"
Setup: Reviews exist for p1 (u1, u2) and p2 (u3)
Expected: Only reviews with productId=p1 returned
```

**TC11 — Product has no reviews → Empty list, no crash**

```
Input: productId="p-no-reviews"
Expected: Empty list returned, no exception
```

**TC12 — Get reviews sorted newest first**

```
Input: productId="p1"
Setup: Review created at T1, Review created at T2 (T2 > T1)
Expected: T2 review appears before T1 in returned list
```

### 17.3 Update Review Tests

**TC13 — User edits own review → Success**

```
Input: reviewId="r1", userId="u1" (owner), rating=4, comment="Updated"
Setup: Review r1 belongs to u1
Expected: Review updated, Product.rating updated
```

**TC14 — User edits another user's review → Denied**

```
Input: reviewId="r1", userId="u2" (NOT owner)
Setup: Review r1 belongs to u1
Expected: BadRequestException("Bạn không có quyền sửa đánh giá này")
```

**TC15 — User attempts to change userId → Denied/ignored**

```
Input: Malicious request tries to set different userId
Expected: review.userId remains original value
```

**TC16 — User attempts to change productId → Denied/ignored**

```
Input: Malicious request tries to set different productId
Expected: review.productId remains original value
```

**TC17 — User tries to edit nonexistent review → Error**

```
Input: reviewId="nonexistent"
Expected: ResourceNotFoundException("Không tìm thấy đánh giá")
```

### 17.4 Delete Review Tests

**TC18 — User deletes own review → Success**

```
Input: reviewId="r1", userId="u1" (owner)
Setup: Review r1 belongs to u1
Expected: Review hard-deleted from MongoDB, Product.rating updated
```

**TC19 — User deletes another user's review → Denied**

```
Input: reviewId="r1", userId="u2" (NOT owner)
Expected: BadRequestException("Bạn không có quyền xóa đánh giá này")
Review NOT deleted from MongoDB.
```

**TC20 — Delete nonexistent review → Appropriate error**

```
Input: reviewId="nonexistent"
Expected: ResourceNotFoundException("Không tìm thấy đánh giá")
```

### 17.5 MongoDB Tests

**TC21 — Review successfully persisted in MongoDB**

```
Input: Valid review creation
Expected: Document appears in MongoDB reviews collection with correct fields
```

**TC22 — Review retrieved from MongoDB**

```
Input: Existing reviewId
Expected: All fields match original saved document
```

**TC23 — Product rating updated after review creation**

```
Setup: Product p1 has rating=0, reviewCount=0
Input: Create review with rating=5 for p1
Expected: Product.rating=5.0, Product.reviewCount=1
```

**TC24 — Product rating recalculated after review deletion**

```
Setup: Product p1 has rating=5.0, reviewCount=2 (two 5-star reviews)
Input: Delete one review
Expected: Product.rating=5.0, Product.reviewCount=1
```

### 17.6 Purchase Verification Tests

**TC25 — Purchased product + verification disabled → Allowed**

```
Setup: System does not enforce purchase verification
Input: User has NOT purchased product, creates review
Expected: Review created successfully (current behavior)
```

**TC26 — Verification disabled → Current system behavior applies**

```
Input: Any authenticated user creates review
Expected: No purchase check performed
```

---

## 18. Task Status

| # | Task | Status | Notes |
|---|------|--------|-------|
| #16 | Customer viết Review | **DONE** | Full: controller, service, DTO, validation, UI |
| #17 | Lưu Review | **DONE** | Service saves to MongoDB, updates product rating |
| #18 | Hiển thị Review | **DONE** | JSON API + server-rendered list in product-detail.html |
| #19 | Customer sửa Review | **DONE** | Owner-only edit with ownership validation |
| #20 | Customer xóa Review | **DONE** | Owner-only delete with ownership validation |
| #21 | Kiểm tra Customer đã mua Product | **WAITING FOR REQUIREMENT** | System does not currently enforce. Requires team decision. |

---

## 19. Files Reference

| File | Type | Modified by Review |
|------|------|-------------------|
| `document/Review.java` | MongoDB document | Already existed |
| `repository/ReviewRepository.java` | MongoDB repository | Already existed |
| `service/ReviewService.java` | Service interface | Already existed |
| `service/impl/ReviewServiceImpl.java` | Service implementation | Already existed |
| `controller/web/ReviewController.java` | Web controller | Already existed |
| `dto/request/ReviewReq.java` | Request DTO | Already existed |
| `dto/response/ReviewRes.java` | Response DTO | Already existed |
| `controller/web/ProductController.java` | Product controller | Already existed (uses ReviewService) |
| `templates/web/product-detail.html` | Product detail template | Already existed (Review UI embedded) |
| `application.yml` | Config | Already existed (MongoDB configured) |
| `docs/REVIEW_INTEGRATION.md` | Documentation | **Created in this session** |
| `src/test/.../ReviewServiceImplTest.java` | Unit tests | **Created in this session** |

---

## 20. Ownership Boundaries

| Function | Owner | Review module modified? |
|----------|-------|----------------------|
| Review creation | Review | Already done |
| Review storage | Review | Already done |
| Review editing | Review | Already done |
| Review deletion | Review | Already done |
| Ownership validation | Review | Already done |
| Review display UI | Anh Quân | N/A (already in product-detail.html) |
| Product data | Product module | No |
| Product rating aggregation | Review (read-only on Product) | No business logic change |
| Current user / auth | Auth module | No |
| Purchase verification (#21) | Order + Review | WAITING FOR REQUIREMENT |
| Order creation | Quốc Anh | No |
| Order query | Quốc Anh | No |
| Cart | Cart module | No |
| Voucher | Voucher module | No |

---

## 21. Instructions for Future Cursor Sessions

1. **Read this document** before modifying the Review module.
2. **Inspect actual code** before making assumptions. Use `git log`, `git blame`, and direct file reads.
3. **Do not modify another member's module** (Product, Order, Cart, User, Voucher, etc.) without explicit permission.
4. **Reuse existing services.** If you need to check a Product, use `ProductRepository`. Do not duplicate.
5. **Do not duplicate Review logic.** If a feature exists in `ReviewServiceImpl`, do not re-implement it elsewhere.
6. **Do not modify `OrderServiceImpl` for Review convenience.** If purchase verification is needed, request the Order member to add a helper method.
7. **Keep changes minimal.** Review features should touch Review files first.
8. **Ownership validation is mandatory.** Any edit/delete must validate `Review.userId == CurrentUser.id` server-side.
9. **Rating validation is mandatory.** Always validate `1 ≤ rating ≤ 5` server-side.
10. **Check `git diff` before commit.** Verify only intended files changed.
11. **Update this document** if the Review API or contract changes.
12. **No MongoDB credentials should be committed.** Use environment variables (`MONGODB_URI`, `MONGODB_DATABASE`).

---

## Appendix A — ReviewService Methods

| Method | Parameters | Returns | Description |
|--------|-----------|---------|-------------|
| `createReview` | `userId, productId, rating, comment` | `Review` | Create with all validations |
| `updateReview` | `reviewId, userId, rating, comment` | `Review` | Update own review, ownership enforced |
| `deleteReview` | `reviewId, userId` | `void` | Delete own review, ownership enforced |
| `getReviewById` | `reviewId` | `Review` | Get single review |
| `getReviewsByProductId` | `productId` | `List<Review>` | All reviews for product (newest first) |
| `getReviewsByUserId` | `userId` | `List<Review>` | All reviews by user |
| `hasUserReviewedProduct` | `userId, productId` | `boolean` | Check if user already reviewed product |
| `getAverageRatingByProductId` | `productId` | `double` | Compute average rating |
| `getReviewCountByProductId` | `productId` | `int` | Count reviews for product |

## Appendix B — Example MongoDB Document

```json
{
  "_id": "66f1234567890abcdef12345",
  "productId": "66eabcdef1234567890abcd",
  "userId": "66dabcdef1234567890abcd",
  "userName": "Nguyễn Văn A",
  "userAvatar": "https://cdn.example.com/avatars/user.png",
  "rating": 5,
  "comment": "Sản phẩm rất tốt, giao hàng nhanh, đóng gói cẩn thận!",
  "createdAt": "2026-08-30T14:00:00"
}
```

## Appendix C — Configuration

```yaml
# application.yml (existing — no changes needed)
spring:
  data:
    mongodb:
      uri: ${MONGODB_URI}
      database: ${MONGODB_DATABASE:cnj70_ecommerce}
      auto-index-creation: true
```

Environment variables:

| Variable | Description |
|----------|-------------|
| `MONGODB_URI` | Full MongoDB connection URI |
| `MONGODB_DATABASE` | Database name (default: `cnj70_ecommerce`) |
