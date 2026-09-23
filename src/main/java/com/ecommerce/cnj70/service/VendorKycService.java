package com.ecommerce.cnj70.service;

import com.ecommerce.cnj70.document.KycProfile;
import com.ecommerce.cnj70.document.User;
import com.ecommerce.cnj70.dto.request.KycFormReq;
import com.ecommerce.cnj70.dto.response.KycStatusRes;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.multipart.MultipartFile;

public interface VendorKycService {

    /**
     * Lấy trạng thái KYC hiện tại của vendor (tạo rỗng nếu chưa có)
     */
    KycStatusRes getKycStatus(UserDetails userDetails);

    /**
     * Nộp / gửi lại hồ sơ KYC (upsert theo userId)
     * Sau khi submit sẽ gọi 3rd-party để xác minh
     */
    KycProfile submitKyc(UserDetails userDetails, KycFormReq request);

    /**
     * Lấy form KYC đã pre-fill từ profile hiện có (dùng cho /vendor/kyc/edit).
     * Throw BadRequestException nếu status hiện tại không cho phép chỉnh sửa.
     * Nếu chưa có profile → trả form rỗng.
     */
    KycFormReq getEditableForm(UserDetails userDetails);

    /**
     * Upload ảnh KYC → trả URL, gọi trước khi submit form
     */
    String uploadKycImage(UserDetails userDetails, MultipartFile file);

    /**
     * Lấy User hiện tại (helper)
     */
    User getCurrentVendor(UserDetails userDetails);

    /**
     * Kiểm tra vendor đã APPROVED KYC chưa (helper để dùng ở controller khác)
     */
    boolean isVendorKycApproved(String userId);

    /**
     * Kiểm tra vendor đã có hồ sơ KYC chưa (dùng để check có thể tạo shop)
     * Throw exception nếu chưa → controller sẽ redirect
     */
    void requireKycApproved(UserDetails userDetails);
}
