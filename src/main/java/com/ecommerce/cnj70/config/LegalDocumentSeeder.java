package com.ecommerce.cnj70.config;

import com.ecommerce.cnj70.document.LegalDocument;
import com.ecommerce.cnj70.enums.LegalDocumentType;
import com.ecommerce.cnj70.repository.LegalDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * TASK #21 — Seeder tự động tạo 8 LegalDocument khi collection rỗng.
 *
 * Chạy khi app khởi động nếu chưa có LegalDocument nào trong DB.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LegalDocumentSeeder implements CommandLineRunner {

    private final LegalDocumentRepository legalDocumentRepository;

    @Override
    public void run(String... args) {
        if (legalDocumentRepository.count() > 0) {
            log.info("[LegalDocumentSeeder] Legal documents already exist, skipping");
            return;
        }

        log.info("[LegalDocumentSeeder] Seeding 8 legal documents...");

        Map<LegalDocumentType, LegalDocument> defaults = buildDefaults();

        for (LegalDocument doc : defaults.values()) {
            legalDocumentRepository.save(doc);
            log.info("  Created: {} ({})", doc.getTitle(), doc.getType());
        }

        log.info("[LegalDocumentSeeder] Done. Created {} legal documents", defaults.size());
    }

    private Map<LegalDocumentType, LegalDocument> buildDefaults() {
        Map<LegalDocumentType, LegalDocument> docs = new EnumMap<>(LegalDocumentType.class);
        LocalDateTime now = LocalDateTime.now();

        docs.put(LegalDocumentType.TERMS, LegalDocument.builder()
                .type(LegalDocumentType.TERMS)
                .title("Điều khoản sử dụng")
                .version(1)
                .effectiveDate(now)
                .updatedBy("SYSTEM")
                .metaDescription("Điều khoản và điều kiện sử dụng nền tảng CNJ70")
                .content(getTermsContent())
                .renderedContent(getTermsContent())
                .build());

        docs.put(LegalDocumentType.PRIVACY, LegalDocument.builder()
                .type(LegalDocumentType.PRIVACY)
                .title("Chính sách bảo mật")
                .version(1)
                .effectiveDate(now)
                .updatedBy("SYSTEM")
                .metaDescription("Chính sách bảo mật thông tin cá nhân người dùng")
                .content(getPrivacyContent())
                .renderedContent(getPrivacyContent())
                .build());

        docs.put(LegalDocumentType.RETURN, LegalDocument.builder()
                .type(LegalDocumentType.RETURN)
                .title("Chính sách đổi trả")
                .version(1)
                .effectiveDate(now)
                .updatedBy("SYSTEM")
                .metaDescription("Chính sách đổi trả và hoàn tiền")
                .content(getReturnContent())
                .renderedContent(getReturnContent())
                .build());

        docs.put(LegalDocumentType.SHIPPING, LegalDocument.builder()
                .type(LegalDocumentType.SHIPPING)
                .title("Chính sách vận chuyển")
                .version(1)
                .effectiveDate(now)
                .updatedBy("SYSTEM")
                .metaDescription("Chính sách giao hàng và vận chuyển")
                .content(getShippingContent())
                .renderedContent(getShippingContent())
                .build());

        docs.put(LegalDocumentType.WARRANTY, LegalDocument.builder()
                .type(LegalDocumentType.WARRANTY)
                .title("Chính sách bảo hành")
                .version(1)
                .effectiveDate(now)
                .updatedBy("SYSTEM")
                .metaDescription("Chính sách bảo hành sản phẩm")
                .content(getWarrantyContent())
                .renderedContent(getWarrantyContent())
                .build());

        docs.put(LegalDocumentType.COMPLAINT, LegalDocument.builder()
                .type(LegalDocumentType.COMPLAINT)
                .title("Quy định khiếu nại")
                .version(1)
                .effectiveDate(now)
                .updatedBy("SYSTEM")
                .metaDescription("Quy trình khiếu nại và giải quyết tranh chấp")
                .content(getComplaintContent())
                .renderedContent(getComplaintContent())
                .build());

        docs.put(LegalDocumentType.PAYMENT, LegalDocument.builder()
                .type(LegalDocumentType.PAYMENT)
                .title("Phương thức thanh toán")
                .version(1)
                .effectiveDate(now)
                .updatedBy("SYSTEM")
                .metaDescription("Các phương thức thanh toán được hỗ trợ")
                .content(getPaymentContent())
                .renderedContent(getPaymentContent())
                .build());

        docs.put(LegalDocumentType.SITEMAP, LegalDocument.builder()
                .type(LegalDocumentType.SITEMAP)
                .title("Sơ đồ website")
                .version(1)
                .effectiveDate(now)
                .updatedBy("SYSTEM")
                .metaDescription("Sơ đồ cấu trúc website CNJ70")
                .content(getSitemapContent())
                .renderedContent(getSitemapContent())
                .build());

        return docs;
    }

    private String getTermsContent() {
        return """
# ĐIỀU KHOẢN SỬ DỤNG

**Cập nhật: 01/09/2026**

## 1. Chấp nhận điều khoản
Bằng việc đăng ký và sử dụng nền tảng CNJ70 ("Nền tảng"), bạn đồng ý tuân thủ các điều khoản sau. Nếu bạn không đồng ý, vui lòng không sử dụng Nền tảng.

## 2. Tài khoản người dùng
- Bạn phải cung cấp thông tin chính xác khi đăng ký.
- Bạn chịu trách nhiệm bảo mật tài khoản và mật khẩu.
- Bạn phải từ đủ 18 tuổi trở lên để sử dụng Nền tảng.
- Nghiêm cấm sử dụng tài khoản cho mục đích lừa đảo hoặc bất hợp pháp.

## 3. Quyền và nghĩa vụ của người bán
- Người bán phải tuân thủ Quy định ngăn chặn hàng giả và vi phạm sở hữu trí tuệ.
- Nghiêm cấm đăng tải sản phẩm vi phạm pháp luật Việt Nam.
- Người bán phải cung cấp KYC và thông tin doanh nghiệp theo quy định.
- Người bán chịu trách nhiệm về chất lượng và nguồn gốc sản phẩm.

## 4. Quyền và nghĩa vụ của người mua
- Người mua phải thanh toán đúng hạn cho đơn hàng.
- Người mua có quyền khiếu nại theo Quy định khiếu nại của Nền tảng.
- Nghiêm cấm lợi dụng chính sách đổi trả để lừa đảo.

## 5. Giới hạn trách nhiệm
- CNJ70 không chịu trách nhiệm về spammy hoặc hành vi vi phạm của người dùng.
- CNJ70 giữ quyền đình chỉ tài khoản vi phạm điều khoản.
- Thông tin sản phẩm do Người bán cung cấp; CNJ70 không đảm bảo tính chính xác hoàn toàn.

## 6. Sở hữu trí tuệ
- Mọi nội dung trên Nền tảng thuộc bản quyền của CNJ70 hoặc bên cấp phép.
- Nghiêm cấm sao chép, phân phối nội dung khi chưa được phép.

## 7. Thay đổi điều khoản
- CNJ70 có quyền thay đổi điều khoản; thông báo trước 30 ngày qua email.
- Việc tiếp tục sử dụng sau thay đổi = đồng ý với điều khoản mới.

## 8. Luật áp dụng
- Điều khoản này được điều chỉnh bởi pháp luật Việt Nam.
- Tranh chấp được giải quyết tại cơ quan có thẩm quyền tại Việt Nam.
""";
    }

    private String getPrivacyContent() {
        return """
# CHÍNH SÁCH BẢO MẬT

**Cập nhật: 01/09/2026**

## 1. Mục đích thu thập
CNJ70 thu thập thông tin cá nhân để:
- Xác minh danh tính và hỗ trợ giao dịch
- Cung cấp dịch vụ khách hàng
- Tuân thủ nghĩa vụ pháp lý (KYC, thuế, chống rửa tiền)
- Cải thiện trải nghiệm người dùng

## 2. Thông tin thu thập
- Thông tin tài khoản: email, mật khẩu (đã mã hóa), họ tên, số điện thoại, địa chỉ
- Thông tin thanh toán: phương thức, không lưu số thẻ đầy đủ
- Thông tin KYC: CCCD, mã số thuế, số tài khoản ngân hàng (**được mã hóa AES-256**)
- Dữ liệu hành vi: lịch sử xem, tìm kiếm, mua hàng
- Dữ liệu thiết bị: IP, trình duyệt, cookie

## 3. Bảo mật dữ liệu nhạy cảm
- CCCD, mã số thuế, số tài khoản được **mã hóa AES-256-GCM** trước khi lưu.
- Chỉ giải mã khi cần xử lý pháp lý, với audit log đầy đủ.
- Không bao giờ hiển thị PII plaintext qua API.

## 4. Chia sẻ thông tin
- **Không bán** thông tin cá nhân cho bên thứ ba.
- Chia sẻ với Người bán (địa chỉ giao hàng, thông tin đơn hàng).
- Chia sẻ với cơ quan chức năng khi có yêu cầu pháp lý.
- Dùng dịch vụ bên thứ ba (payment gateway, KYC provider) theo hợp đồng bảo mật.

## 5. Lưu trữ dữ liệu
- Dữ liệu được lưu trữ theo quy định pháp luật Việt Nam.
- Dữ liệu thanh toán: lưu 5 năm sau khi giao dịch kết thúc.
- Dữ liệu KYC: lưu trong thời gian hoạt động tài khoản + 5 năm sau khi đóng.
- Bạn có quyền yêu cầu xóa dữ liệu (trừ khi pháp luật yêu cầu giữ lại).

## 6. Quyền của người dùng
- Truy cập dữ liệu cá nhân của mình
- Yêu cầu chỉnh sửa thông tin không chính xác
- Yêu cầu xóa dữ liệu (right to erasure)
- Phản đối xử lý dữ liệu cho mục đích marketing
- Rút lại consent marketing opt-in bất kỳ lúc nào

## 7. Cookie và tracking
- Cookie cần thiết: xác thực, giỏ hàng, preferences
- Cookie phân tích: Google Analytics (có anonymize IP)
- Cookie marketing: chỉ khi có opt-in đồng ý

## 8. Liên hệ
Email bảo mật: privacy@cnj70.com
""";
    }

    private String getReturnContent() {
        return """
# CHÍNH SÁCH ĐỔI TRẢ

**Cập nhật: 01/09/2026**

## 1. Điều kiện đổi trả
Sản phẩm được đổi trả khi:
- Sản phẩm bị lỗi từ nhà sản xuất (hỏng, không hoạt động đúng tính năng)
- Giao sai sản phẩm (sai màu, sai kích thước, thiếu items)
- Sản phẩm không đúng như mô tả trên website
- Yêu cầu trong thời hạn 7 ngày kể từ ngày nhận hàng

## 2. Sản phẩm KHÔNG được đổi trả
- Sản phẩm thực phẩm, đồ tươi sống đã qua sử dụng
- Sản phẩm được thiết kế riêng theo yêu cầu
- Sản phẩm giảm giá >50% (final sale)
- Sản phẩm đã qua sử dụng gây ra hư hỏng từ phía người mua
- Sản phẩm không còn nguyên vẹn, tem mác, packaging

## 3. Quy trình đổi trả
1. Gửi yêu cầu đổi trả qua mục "Đơn hàng" trên website trong vòng 7 ngày
2. Upload hình ảnh sản phẩm lỗi/hư hỏng + mô tả chi tiết
3. Chờ Người bán phản hồi trong 48 giờ làm việc
4. Nếu được duyệt, gửi sản phẩm về địa chỉ Người bán cung cấp
5. Sau khi Người bán xác nhận, hoàn tiền trong 7 ngày làm việc

## 4. Phương thức hoàn tiền
- Hoàn qua phương thức thanh toán ban đầu
- Thời gian hoàn tiền: 3-7 ngày làm việc (tùy ngân hàng)
- Người bán chịu phí ship đổi trả nếu lỗi từ Người bán

## 5. Tranh chấp đổi trả
- Nếu Người bán từ chối không hợp lý, người mua có thể escalate lên Khiếu nại (Complaint).
- Xem Quy định khiếu nại để biết thêm chi tiết.
""";
    }

    private String getShippingContent() {
        return """
# CHÍNH SÁCH VẬN CHUYỂN

**Cập nhật: 01/09/2026**

## 1. Đơn vị vận chuyển
CNJ70 hỗ trợ các đơn vị vận chuyển:
- Giao hàng nhanh (GHN, GHTK, Viettel Post)
- Giao hàng tiết kiệm
- Tự giao (Seller tự vận chuyển cho Shop đã verified)

## 2. Phí vận chuyển
- Phí ship cố định: **15.000đ** cho mọi đơn hàng (trừ khi có voucher free ship).
- Miễn phí ship cho đơn hàng từ **500.000đ** (áp dụng theo chương trình của Người bán).
- Phí ship cho sản phẩm nặng/quá khổ được tính riêng theo cân nặng thực tế.

## 3. Thời gian giao hàng dự kiến
- Nội thành HCM/HN: 1-2 ngày
- Các tỉnh/thành khác: 3-5 ngày
- Khu vực xa: 5-7 ngày
- Thời gian chỉ mang tính ước lượng, không tính ngày lễ/tết

## 4. Trách nhiệm vận chuyển
- **Người bán**: đóng gói và giao cho đơn vị vận chuyển trong 48 giờ làm việc.
- **Đơn vị vận chuyển**: chịu trách nhiệm mất mát/hư hỏng trong quá trình vận chuyển.
- **Người mua**: kiểm tra tình trạng sản phẩm ngay khi nhận, quay video unboxing.

## 5. Lỗi giao hàng
- Giao thiếu: người mua báo ngay, hoàn tiền hoặc giao lại.
- Giao sai: người mua có quyền giữ lại hoặc gửi lại, Người bán chịu chi phí.
- Mất package: người mua báo ngay, Người bán đối soát với đơn vị vận chuyển.

## 6. Giao hàng thất bại
- Nếu đơn vị vận chuyển không giao được (không có người nhận), sẽ thử lại 2 lần.
- Sau 2 lần thất bại, package được hoàn về Người bán.
- Người mua chịu phí ship lại nếu yêu cầu giao lại.
""";
    }

    private String getWarrantyContent() {
        return """
# CHÍNH SÁCH BẢO HÀNH

**Cập nhật: 01/09/2026**

## 1. Phạm vi bảo hành
Mọi sản phẩm được bảo hành theo chính sách của Người bán, tối thiểu:
- Sản phẩm điện tử: 12 tháng
- Sản phẩm gia dụng: 6 tháng
- Sản phẩm thời trang, phụ kiện: không có bảo hành (trừ lỗi từ nhà sản xuất)

## 2. Điều kiện bảo hành
- Sản phẩm còn trong thời hạn bảo hành
- Còn tem bảo hành, serial number chưa bị tẩy xóa
- Lỗi phát sinh từ nhà sản xuất (không phải do người dùng)
- Có hóa đơn mua hàng từ CNJ70

## 3. Trường hợp KHÔNG được bảo hành
- Hư hỏng do sử dụng sai cách, bất cẩn, tai nạn
- Sản phẩm đã tự ý sửa chữa/tuya chỉnh
- Hư hỏng do thiên tai, cháy nổ
- Sản phẩm hao mòn tự nhiên theo thời gian
- Phụ kiện đi kèm: pin, dây sạc, tai nghe (trừ khi có cam kết riêng)

## 4. Quy trình bảo hành
1. Liên hệ Người bán qua mục "Khiếu nại" trên website
2. Cung cấp hình ảnh/video lỗi + mô tả chi tiết
3. Người bán phản hồi trong 48 giờ
4. Nếu được duyệt: gửi sản phẩm về trung tâm bảo hành
5. Thời gian xử lý: 7-14 ngày làm việc

## 5. Bảo hành song song
- Bảo hành của Người bán song song với bảo hành của nhà sản xuất.
- Nếu Người bán không xử lý bảo hành, người dùng có quyền khiếu nại lên CNJ70.
""";
    }

    private String getComplaintContent() {
        return """
# QUY ĐỊNH KHIẾU NẠI

**Cập nhật: 01/09/2026**

## 1. Phạm vi khiếu nại
Người mua/Người bán có thể khiếu nại khi:
- Người bán không phản hồi trong 48 giờ
- Người bán từ chối đổi trả không hợp lý
- Sản phẩm không đúng mô tả nghiêm trọng
- Gian lận, lừa đảo
- Hành vi vi phạm điều khoản sử dụng

## 2. Cấp độ khiếu nại

### Level 0: Giữa Customer ↔ Vendor
- Thời hạn Vendor phản hồi: **48 giờ**
- Nếu quá hạn, tự động escalate lên Level 1
- Deadline: hiển thị rõ ràng trên giao diện

### Level 1: Moderator xử lý
- Moderator xem bằng chứng hai bên
- Thời hạn: 72 giờ làm việc
- Moderator có quyền: yêu cầu hoàn tiền, yêu cầu giao lại, đóng Shop tạm thời

### Level 2: Admin xử lý
- Khiếu nại phức tạp hoặc khi một trong hai bên không đồng ý quyết định Level 1
- Admin có quyền ban tài khoản, tịch thu tiền, báo cơ quan chức năng

## 3. Bằng chứng được chấp nhận
- Hình ảnh/video sản phẩm thực tế
- Video unboxing (quay rõ ngày giờ)
- Video call/show hàng
- Screenshot cuộc trò chuyện
- Báo cáo vận chuyển (giao hàng nhanh)
- Giấy tờ, hóa đơn liên quan

## 4. Quyền của các bên
- Cung cấp bằng chứng bổ sung trong thời gian xử lý
- Appeal quyết định trong 7 ngày
- Yêu cầu moderator trung lập (nếu có bằng chứng moderator thiên vị)

## 5. Xử lý khiếu nại sai
- Người mua khiếu nại sai (lừa đảo đổi trả): cảnh cáo, khóa tài khoản
- Người bán khiếu nại sai: cảnh cáo, đình chỉ Shop
- Bằng chứng giả mạo: báo cơ quan chức năng
""";
    }

    private String getPaymentContent() {
        return """
# PHƯƠNG THỨC THANH TOÁN

**Cập nhật: 01/09/2026**

## 1. Phương thức được hỗ trợ

### Thanh toán trực tuyến
- **VNPay**: Thẻ ATM nội địa, VnPay Wallet
- **MoMo**: Ví MoMo, thẻ ATM liên kết MoMo
- **ZaloPay**: Ví ZaloPay
- **Thẻ tín dụng/ghi nợ**: Visa, Mastercard, JCB

### Thanh toán khi nhận hàng (COD)
- Trả tiền mặt cho nhân viên giao hàng
- Áp dụng phí COD: **10.000đ** cho mỗi đơn hàng COD

### Chuyển khoản trực tiếp
- Chuyển khoản vào tài khoản Người bán (chỉ khi Shop verified)
- CNJ70 không chịu trách nhiệm cho giao dịch ngoài Nền tảng

## 2. Quy trình thanh toán
1. Chọn sản phẩm → Thêm vào giỏ hàng
2. Kiểm tra đơn hàng → Chọn phương thức thanh toán
3. Thanh toán qua cổng (VNPay/MoMo/ZaloPay/thẻ)
4. Nhận xác nhận thanh toán qua email
5. Đơn hàng được xác nhận và chuyển cho Người bán

## 3. Bảo mật thanh toán
- CNJ70 **KHÔNG** lưu số thẻ tín dụng đầy đủ
- Thông tin thanh toán được xử lý bởi cổng thanh toán (PCI-DSS compliant)
- Mọi giao dịch được mã hóa SSL/TLS

## 4. Thanh toán thất bại
- Thanh toán qua cổng thất bại: đơn hàng tự động hủy sau 30 phút
- Giao dịch bị duplicate: hoàn tiền trong 7 ngày làm việc
- Lỗi kỹ thuật: liên hệ hotlinesupport@cnj70.com

## 5. Hoàn tiền
- Hoàn tiền tự động qua phương thức thanh toán ban đầu
- Thời gian hoàn: 3-7 ngày làm việc (tùy ngân hàng)
- Không hoàn tiền mặt, chỉ chuyển khoản
""";
    }

    private String getSitemapContent() {
        return """
# SƠ ĐỒ WEBSITE CNJ70

**Cập nhật: 01/09/2026**

## Trang công khai
- Trang chủ /
- Sản phẩm /products
- Sản phẩm chi tiết /products/{id}
- Danh mục /categories
- Tìm kiếm /search

## Người mua (Customer)
- Đăng ký /auth/register
- Đăng nhập /auth/login
- Quên mật khẩu /auth/forgot-password
- Giỏ hàng /cart
- Checkout /checkout
- Đơn hàng /orders
- Đơn hàng chi tiết /orders/{id}
- Đánh giá sản phẩm /products/{id}#reviews
- Tài khoản /profile
- Ví Voucher /vouchers
- Khiếu nại /complaints

## Người bán (Vendor)
- Dashboard /vendor/dashboard
- Quản lý sản phẩm /vendor/products
- Thêm sản phẩm /vendor/products/add
- Sửa sản phẩm /vendor/products/{id}/edit
- Quản lý đơn hàng /vendor/orders
- Đơn hàng chi tiết /vendor/orders/{id}
- Cập nhật trạng thái /vendor/orders/{id}/status
- Quản lý Shop /vendor/shop
- Nộp KYC /vendor/kyc
- Khiếu nại của tôi /vendor/complaints

## Quản trị (Admin)
- Dashboard /admin/dashboard
- Quản lý Users /admin/users
- Quản lý Shops /admin/shops
- Quản lý Products /admin/products
- Quản lý Orders /admin/orders
- Quản lý Reviews /admin/reviews
- Moderation Queue /admin/moderation
- KYC Management /admin/kyc
- Violations /admin/violations
- Banners /admin/banners
- Legal Documents /admin/legal

## Tài liệu pháp lý
- Điều khoản sử dụng /legal/terms
- Chính sách bảo mật /legal/privacy
- Chính sách đổi trả /legal/return
- Chính sách vận chuyển /legal/shipping
- Chính sách bảo hành /legal/warranty
- Quy định khiếu nại /legal/complaint
- Phương thức thanh toán /legal/payment
- Sơ đồ website /legal/sitemap
""";
    }
}
