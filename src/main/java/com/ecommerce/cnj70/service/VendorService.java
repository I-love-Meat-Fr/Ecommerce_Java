package com.ecommerce.cnj70.service;

import com.ecommerce.cnj70.document.Shop;
import com.ecommerce.cnj70.document.User;
import com.ecommerce.cnj70.dto.request.ShopFormReq;
import com.ecommerce.cnj70.dto.response.VendorDashboardRes;
import com.ecommerce.cnj70.dto.response.VendorProfileRes;
import com.ecommerce.cnj70.exception.UnauthorizedException;
import org.springframework.security.core.userdetails.UserDetails;

public interface VendorService {
    
    User getCurrentVendor(UserDetails userDetails);
    
    String getShopIdFromUser(UserDetails userDetails);
    
    Shop getShopByCurrentVendor(UserDetails userDetails);
    
    Shop createShop(UserDetails userDetails, ShopFormReq request);
    
    Shop updateShop(UserDetails userDetails, ShopFormReq request);
    
    VendorProfileRes getVendorProfile(UserDetails userDetails);
    
    VendorDashboardRes getDashboardStats(UserDetails userDetails);
    
    void validateShopOwnership(String shopId, UserDetails userDetails);
    
    void validateProductOwnership(String productId, UserDetails userDetails);
    
    boolean isShopOwner(String shopId, UserDetails userDetails);
}
