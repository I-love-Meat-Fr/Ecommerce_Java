# CNJ70 Ecommerce - Test Suite

Bộ test tự động cho 6 task vendor (Dashboard, Violation, Shop Lifecycle, Inventory, Vendor Order, Test E2E).

## Cấu trúc

```
src/test/java/com/ecommerce/cnj70/
├── support/
│   └── TestFixtures.java              # Builder pattern cho User/Shop/Product/Cart/Order/KycProfile
├── service/
│   ├── VendorServiceTest.java         # 15 tests - KYC guard, ownership, dashboard stats
│   ├── VendorKycServiceTest.java      # 14 tests - submit flow, 3rd-party verify
│   ├── AdminKycServiceTest.java       # 8 tests  - approve/reject/suspend
│   ├── AdminShopServiceTest.java      # 10 tests - shop lifecycle, idempotent
│   ├── ProductServiceTest.java        # 9 tests  - stock, status, ownership
│   ├── CartServiceTest.java           # 7 tests  - add to cart, maxStock clamp
│   └── OrderServiceTest.java          # 12 tests - checkout, status transition, restore
├── guard/
│   └── GuardTest.java                 # 9 tests - các guard bảo vệ flow (nested)
└── flow/
    ├── VendorFlowE2ETest.java         # 5 tests  - end-to-end KYC → ship
    ├── InventoryFlowTest.java         # 6 tests  - stock + ProductStatus transitions
    ├── ShopLifecycleTest.java         # 9 tests  - PENDING → APPROVED → REJECTED
    ├── OrderIsolationTest.java        # 4 tests  - multi-shop order filter
    └── ViolationIsolationTest.java    # 4 tests  - vendor chỉ thấy violation của mình
```

**Tổng: 112 tests, 0 failures, 0 errors**

## Cách chạy

```bash
# Chạy toàn bộ test
mvn test

# Chạy 1 class cụ thể
mvn test -Dtest=VendorServiceTest

# Chạy 1 method cụ thể
mvn test -Dtest=VendorServiceTest#getDashboardStats_aggregatesCorrectly

# Chạy theo package
mvn test -Dtest='com.ecommerce.cnj70.flow.*'
```

## Phân loại test

### Unit Test (Mockito-based)
Không cần Spring context, chạy nhanh (< 1s). Phù hợp cho CI.

- 7 file trong `service/` package
- Mock tất cả Repository/Service dependency
- Verify business logic và guard rules

### Flow Test
Test các flow end-to-end logic, mô phỏng nhiều service phối hợp.

- 5 file trong `flow/` package
- Test các scenario thực tế (vendor submit KYC → admin approve → vendor tạo shop → ...)
- Có mock đầy đủ các service liên quan

### Guard Test
Test các "hàng rào" bảo vệ security/access.

- 1 file `GuardTest.java` với 5 nested class
- Mỗi nested class test 1 loại guard (KYC, Shop ownership, Product status, Order ownership, Dashboard)

## Gap đã được document

Các test dưới đây PASS ngay cả khi code hiện tại CÓ BUG, để đánh dấu gap cần fix:

| File | Test | Gap |
|---|---|---|
| CartServiceTest | `addToCart_doesNotCheckProductStatus_currentBehavior` | Cart không check ProductStatus (HIDDEN/DRAFT vẫn add được) |
| OrderServiceTest | `createOrder_doesNotCheckProductStatus_currentBehavior` | OrderService không check ProductStatus |
| InventoryFlowTest | `checkout_stockReachesZero_doesNotAutoUpdateStatus_currentBehavior` | Không auto-set OUT_OF_STOCK khi stock=0 |
| InventoryFlowTest | `cancelOrder_restoredStock_doesNotRevertStatus_currentBehavior` | Không revert ACTIVE khi restore stock |
| ShopLifecycleTest | `currentBehavior_noReasonStored` | AdminShopService không lưu reason |
| ShopLifecycleTest | `shopStatusEnum_noSuspendedState_currentBehavior` | ShopStatus thiếu SUSPENDED |
| VendorFlowE2ETest | `e2e_fail_pendingShop_currentlyBypassed` | ProductService không check Shop.status |

Khi fix các gap này, test sẽ FAIL → bạn phải update lại expectation.

## Cấu hình

### Java version
Yêu cầu Java 17+. Đã test thành công trên Java 26.

### Maven Surefire
Đã thêm JVM argument `-Dnet.bytebuddy.experimental=true` để hỗ trợ Java 21+ (Byte Buddy chính thức chỉ support Java 22, cần flag experimental cho Java 26).

Xem `pom.xml`:
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <configuration>
        <argLine>-Dnet.bytebuddy.experimental=true</argLine>
    </configuration>
</plugin>
```

### Dependencies
- `spring-boot-starter-test` (JUnit 5 + Mockito + AssertJ)
- `spring-security-test`
- Tất cả đã có sẵn trong `pom.xml`, không cần thêm gì.

## Khi nào test fail?

Các tình huống thường gặp:

1. **Refactor service**: Đổi tên method, đổi parameter → fix test theo.
2. **Thêm field mới vào Document**: Ví dụ `Shop.reason` → update TestFixtures + viết test mới.
3. **Fix gap** (xem bảng trên): Update test expectation từ "current behavior" sang "expected behavior".

## CI/CD

GitHub Actions workflow mẫu đã có ở `.github/workflows/test.yml`. Workflow này sẽ:
- Checkout code
- Setup JDK 17
- Cache Maven dependencies
- Chạy `mvn test`
- Fail build nếu có test fail

## Note về Test Isolation

Mỗi test class đều `@MockitoSettings(strictness = Strictness.LENIENT)` để tránh UnnecessaryStubbingException khi một số stub không được sử dụng trong test case cụ thể.
