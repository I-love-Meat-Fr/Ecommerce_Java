package com.ecommerce.cnj70.dto.request;

import com.ecommerce.cnj70.enums.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutReq {
    
    @NotBlank(message = "Shipping address is required")
    private String shippingAddress;
    
    private String phone;
    
    private PaymentMethod paymentMethod;
    
    private List<CheckoutItemReq> items;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CheckoutItemReq {
        private String productId;
        private int quantity;
    }
}
