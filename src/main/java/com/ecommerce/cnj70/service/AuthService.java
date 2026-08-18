package com.ecommerce.cnj70.service;

import com.ecommerce.cnj70.document.User;
import com.ecommerce.cnj70.dto.request.RegisterReq;
import com.ecommerce.cnj70.security.CustomUserDetails;

public interface AuthService {
    
    User register(RegisterReq request);
    
    CustomUserDetails login(String email, String password);
    
    boolean existsByEmail(String email);
}
