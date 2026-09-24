package com.ecommerce.cnj70.controller.vendor;

import com.ecommerce.cnj70.dto.request.KycFormReq;
import com.ecommerce.cnj70.dto.response.KycStatusRes;
import com.ecommerce.cnj70.exception.BadRequestException;
import com.ecommerce.cnj70.security.CustomUserDetails;
import com.ecommerce.cnj70.service.VendorKycService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
@RequestMapping("/vendor/kyc")
@RequiredArgsConstructor
@Slf4j
public class VendorKycController {

    private final VendorKycService vendorKycService;

    /**
     * Trang chính KYC:
     *  - Luôn hiển thị trang trạng thái để vendor nắm được lý do reject (nếu có)
     *    trước khi bấm "Sửa & gửi lại".
     *  - Riêng NOT_SUBMITTED: status page sẽ có nút "Bắt đầu nộp hồ sơ"
     *    dẫn sang /vendor/kyc/edit.
     */
    @GetMapping
    public String kycPage(@AuthenticationPrincipal CustomUserDetails user, Model model) {
        KycStatusRes status = vendorKycService.getKycStatus(user);
        model.addAttribute("kycStatus", status);
        return "vendor/kyc-status";
    }

    /**
     * Trang edit/submit form KYC.
     *  - Luôn pre-fill từ profile hiện có (vendor không phải nhập lại từ đầu).
     *  - Chỉ truy cập được khi status cho phép submit lại
     *    (NOT_SUBMITTED / THIRD_PARTY_REJECTED / ADMIN_REJECTED).
     *  - KYCStatusRes đính kèm để form hiển thị banner "Bị từ chối vì: ...".
     */
    @GetMapping("/edit")
    public String kycEditPage(@AuthenticationPrincipal CustomUserDetails user, Model model) {
        KycStatusRes status = vendorKycService.getKycStatus(user);
        KycFormReq form = vendorKycService.getEditableForm(user);
        model.addAttribute("kycStatus", status);
        model.addAttribute("kycFormReq", form);
        return "vendor/kyc-form";
    }

    /**
     * Upload ảnh KYC (ảnh CCCD, giấy phép...)
     */
    @PostMapping(value = "/upload-image", produces = "application/json")
    @ResponseBody
    public ResponseEntity<?> uploadImage(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam("file") MultipartFile file) {
        try {
            String url = vendorKycService.uploadKycImage(user, file);
            return ResponseEntity.ok(Map.of("success", true, "url", url));
        } catch (BadRequestException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error uploading KYC image: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "error", "Không thể upload ảnh"));
        }
    }

    /**
     * Submit form KYC → 3rd-party verify → chờ admin duyệt
     */
    @PostMapping("/submit")
    public String submitKyc(@AuthenticationPrincipal CustomUserDetails user,
                           @ModelAttribute @Valid KycFormReq request,
                           RedirectAttributes redirectAttributes) {
        try {
            vendorKycService.submitKyc(user, request);
            redirectAttributes.addFlashAttribute("success",
                    "Hồ sơ KYC đã được gửi. Vui lòng chờ xác minh và admin duyệt.");
            return "redirect:/vendor/kyc";
        } catch (BadRequestException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/vendor/kyc";
        }
    }
}
