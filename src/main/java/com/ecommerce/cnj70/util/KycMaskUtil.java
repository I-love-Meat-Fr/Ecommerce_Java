package com.ecommerce.cnj70.util;

/**
 * Utility để mask dữ liệu nhạy cảm theo Luật Bảo vệ Dữ liệu cá nhân VN 2023.
 *
 * Admin chỉ cần xem ảo (masked) để định danh hồ sơ, không cần biết số thật.
 * Dữ liệu thật đã được 3rd-party xử lý - bên thứ 3 chịu 100% trách nhiệm verify.
 */
public final class KycMaskUtil {

    private KycMaskUtil() {}

    /**
     * Mask chuỗi giữ lại 3 ký tự đầu + "***" + 3 ký tự cuối.
     * <p>Ví dụ:
     * <ul>
     *   <li>{@code "079123456789"} → {@code "079***789"}</li>
     *   <li>{@code "0123456789"}   → {@code "012***789"}</li>
     *   <li>{@code "12345"}        → {@code "12***45"}</li>
     *   <li>{@code "abc"}          → {@code "***"}</li>
     *   <li>{@code null}           → {@code "—"}</li>
     * </ul>
     *
     * @param value giá trị gốc (CCCD, MST, số tài khoản...)
     * @return chuỗi đã mask
     */
    public static String mask(String value) {
        if (value == null || value.isBlank()) {
            return "—";
        }
        String trimmed = value.trim();
        int len = trimmed.length();
        if (len <= 6) {
            // Quá ngắn → mask toàn bộ
            return "***";
        }
        if (len <= 8) {
            // Độ dài trung bình → giữ 2 đầu + 2 cuối
            return trimmed.substring(0, 2) + "***"
                    + trimmed.substring(len - 2);
        }
        // Độ dài dài → giữ 3 đầu + "***" + 3 cuối
        return trimmed.substring(0, 3) + "***"
                + trimmed.substring(len - 3);
    }

    /**
     * Mask CCCD với format 12 số (chuẩn VN): giữ 3 đầu + "***" + 4 cuối.
     * Ví dụ: "079123456789" → "079***6789"
     */
    public static String maskCccd(String cccd) {
        if (cccd == null || cccd.isBlank()) {
            return "—";
        }
        String trimmed = cccd.trim();
        int len = trimmed.length();
        if (len < 6) {
            return "***";
        }
        if (len <= 9) {
            return trimmed.substring(0, Math.min(3, len / 2))
                    + "***"
                    + trimmed.substring(len - Math.min(3, len / 2));
        }
        return trimmed.substring(0, 3) + "***" + trimmed.substring(len - 4);
    }

    /**
     * Mask mã số thuế (10-13 số): giữ 3 đầu + "***" + 3 cuối.
     */
    public static String maskTaxCode(String tax) {
        return mask(tax);
    }

    /**
     * Mask số tài khoản ngân hàng: giữ 3 đầu + "***" + 3 cuối.
     */
    public static String maskBankAccount(String acc) {
        return mask(acc);
    }
}
