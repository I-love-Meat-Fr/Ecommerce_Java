package com.ecommerce.cnj70.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KycFormReq {

    // === Thông tin chủ shop ===
    @NotBlank(message = "Họ tên chủ shop không được để trống")
    private String ownerFullName;

    @NotBlank(message = "Số CCCD không được để trống")
    @Pattern(regexp = "^\\d{12}$", message = "CCCD phải gồm đúng 12 chữ số")
    private String idNumber;

    private String idFrontImageUrl;
    private String idBackImageUrl;

    // === Thông tin doanh nghiệp ===
    private String businessName;

    @Pattern(regexp = "^\\d{10,13}$", message = "Mã số thuế phải từ 10 đến 13 chữ số", flags = Pattern.Flag.CASE_INSENSITIVE)
    private String taxCode;

    private String businessLicenseUrl;

    // === Thông tin ngân hàng ===
    @NotBlank(message = "Số tài khoản không được để trống")
    private String bankAccount;

    @NotBlank(message = "Tên ngân hàng không được để trống")
    private String bankName;

    private String bankBranch;
}
