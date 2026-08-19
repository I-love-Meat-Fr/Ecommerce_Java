package com.ecommerce.cnj70.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    
    private String token;
    private String tokenType;
    private String userId;
    private String email;
    private String fullName;
    private String role;
    private long expiresIn;
}
