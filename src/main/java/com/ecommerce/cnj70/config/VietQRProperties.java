package com.ecommerce.cnj70.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Cấu hình VietQR API credentials và thông tin tài khoản merchant.
 * 
 * <p>Load từ application.yml (prefix: vietqr):
 * <ul>
 *   <li>vietqr.api.client-id: Client ID từ VietQR Developer Portal</li>
 *   <li>vietqr.api.api-key: API Key từ VietQR Developer Portal</li>
 *   <li>vietqr.merchant.account-number: Số tài khoản ngân hàng</li>
 *   <li>vietqr.merchant.bank-code: Mã ngân hàng (MBB = MB Bank)</li>
 *   <li>vietqr.merchant.account-name: Tên chủ tài khoản</li>
 *   <li>vietqr.callback.base-url: Base URL cho webhook callback</li>
 * </ul>
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "vietqr")
public class VietQRProperties {

    private Api api = new Api();
    private Merchant merchant = new Merchant();
    private Callback callback = new Callback();

    @Data
    public static class Api {
        private String baseUrl = "https://api.vietqr.io/v2";
        private String clientId;
        private String apiKey;
        private int timeoutConnect = 10;
        private int timeoutRead = 30;
    }

    @Data
    public static class Merchant {
        private String accountNumber;
        private String bankCode = "MBB";
        private String bankName = "MB Bank";
        private String accountName;
    }

    @Data
    public static class Callback {
        private String baseUrl;
    }
}
