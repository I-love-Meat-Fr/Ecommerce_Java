package com.ecommerce.cnj70.service.impl;

import com.ecommerce.cnj70.document.Voucher;
import com.ecommerce.cnj70.dto.request.VoucherFormReq;
import com.ecommerce.cnj70.enums.DiscountType;
import com.ecommerce.cnj70.enums.VoucherType;
import com.ecommerce.cnj70.exception.BadRequestException;
import com.ecommerce.cnj70.exception.ResourceNotFoundException;
import com.ecommerce.cnj70.repository.VoucherRepository;
import com.ecommerce.cnj70.service.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implement VoucherService
 */
@Service
@RequiredArgsConstructor
public class VoucherServiceImpl implements VoucherService {
    
    private final VoucherRepository voucherRepository;
    
    @Override
    public Voucher createVoucher(VoucherFormReq request, String shopId, String shopName, String createdBy) {
        // Validate code không trùng
        if (voucherRepository.existsByCode(request.getCode())) {
            throw new BadRequestException("Mã voucher đã tồn tại");
        }
        
        // Validate discount value
        if (request.getDiscountType() == DiscountType.PERCENT) {
            if (request.getDiscountValue().doubleValue() > 100) {
                throw new BadRequestException("Phần trăm giảm không được vượt quá 100%");
            }
        }
        
        // Validate ngày
        if (request.getEndDate() != null && request.getStartDate() != null 
            && request.getEndDate().isBefore(request.getStartDate())) {
            throw new BadRequestException("Ngày kết thúc phải sau ngày bắt đầu");
        }
        
        Voucher voucher = Voucher.builder()
                .code(request.getCode().toUpperCase())
                .name(request.getName())
                .type(VoucherType.SHOP)
                .shopId(shopId)
                .shopName(shopName)
                .productIds(request.getProductIds() != null ? request.getProductIds() : new ArrayList<>())
                .discountType(request.getDiscountType())
                .discountValue(request.getDiscountValue())
                .maxDiscountAmount(request.getMaxDiscountAmount())
                .minOrderValue(request.getMinOrderValue() != null ? request.getMinOrderValue() : java.math.BigDecimal.ZERO)
                .quantity(request.getQuantity())
                .used(0)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .active(true)
                .createdBy(createdBy)
                .build();
        
        return voucherRepository.save(voucher);
    }
    
    @Override
    public Voucher createWebVoucher(VoucherFormReq request, String createdBy) {
        if (voucherRepository.existsByCode(request.getCode())) {
            throw new BadRequestException("Mã voucher đã tồn tại");
        }
        
        if (request.getDiscountType() == DiscountType.PERCENT) {
            if (request.getDiscountValue().doubleValue() > 100) {
                throw new BadRequestException("Phần trăm giảm không được vượt quá 100%");
            }
        }
        
        Voucher voucher = Voucher.builder()
                .code(request.getCode().toUpperCase())
                .name(request.getName())
                .type(VoucherType.WEB)
                .productIds(request.getProductIds() != null ? request.getProductIds() : new ArrayList<>())
                .discountType(request.getDiscountType())
                .discountValue(request.getDiscountValue())
                .maxDiscountAmount(request.getMaxDiscountAmount())
                .minOrderValue(request.getMinOrderValue() != null ? request.getMinOrderValue() : java.math.BigDecimal.ZERO)
                .quantity(request.getQuantity())
                .used(0)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .active(true)
                .createdBy(createdBy)
                .build();
        
        return voucherRepository.save(voucher);
    }
    
    @Override
    public Voucher updateVoucher(String voucherId, VoucherFormReq request) {
        Voucher voucher = getVoucherById(voucherId);
        
        // Không cho đổi code nếu đã có người dùng
        if (request.getCode() != null && !request.getCode().equalsIgnoreCase(voucher.getCode())) {
            if (voucherRepository.existsByCode(request.getCode())) {
                throw new BadRequestException("Mã voucher đã tồn tại");
            }
            voucher.setCode(request.getCode().toUpperCase());
        }
        
        if (request.getName() != null) {
            voucher.setName(request.getName());
        }
        
        if (request.getDiscountType() != null) {
            voucher.setDiscountType(request.getDiscountType());
        }
        
        if (request.getDiscountValue() != null) {
            voucher.setDiscountValue(request.getDiscountValue());
        }
        
        if (request.getMaxDiscountAmount() != null) {
            voucher.setMaxDiscountAmount(request.getMaxDiscountAmount());
        }
        
        if (request.getMinOrderValue() != null) {
            voucher.setMinOrderValue(request.getMinOrderValue());
        }
        
        if (request.getQuantity() != null) {
            // Không cho giảm quantity thấp hơn số đã dùng
            if (request.getQuantity() < voucher.getUsed()) {
                throw new BadRequestException("Số lượng không được nhỏ hơn số đã sử dụng");
            }
            voucher.setQuantity(request.getQuantity());
        }
        
        if (request.getStartDate() != null) {
            voucher.setStartDate(request.getStartDate());
        }
        
        if (request.getEndDate() != null) {
            voucher.setEndDate(request.getEndDate());
        }
        
        if (request.getProductIds() != null) {
            voucher.setProductIds(request.getProductIds());
        }
        
        return voucherRepository.save(voucher);
    }
    
    @Override
    public void deleteVoucher(String voucherId) {
        Voucher voucher = getVoucherById(voucherId);
        voucher.setActive(false);
        voucherRepository.save(voucher);
    }
    
    @Override
    public Voucher getVoucherById(String id) {
        return voucherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy voucher"));
    }
    
    @Override
    public Voucher getVoucherByCode(String code) {
        return voucherRepository.findByCode(code.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy voucher với mã: " + code));
    }
    
    @Override
    public List<Voucher> getVouchersByShop(String shopId) {
        return voucherRepository.findByShopId(shopId);
    }
    
    @Override
    public List<Voucher> getWebVouchers() {
        return voucherRepository.findByType(VoucherType.WEB);
    }
    
    @Override
    public List<Voucher> getAvailableVouchers() {
        List<Voucher> all = voucherRepository.findAll();
        LocalDateTime now = LocalDateTime.now();
        
        return all.stream()
                .filter(Voucher::isAvailable)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Voucher> getAvailableVouchersByShop(String shopId) {
        List<Voucher> vouchers = voucherRepository.findByShopIdAndActiveTrue(shopId);
        LocalDateTime now = LocalDateTime.now();
        
        return vouchers.stream()
                .filter(Voucher::isAvailable)
                .collect(Collectors.toList());
    }
    
    @Override
    public void incrementUsed(String voucherId) {
        Voucher voucher = getVoucherById(voucherId);
        voucher.setUsed(voucher.getUsed() + 1);
        voucherRepository.save(voucher);
    }
    
    @Override
    public boolean isVoucherValid(String code) {
        try {
            Voucher voucher = getVoucherByCode(code);
            return voucher.isAvailable();
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public Voucher validateForCheckout(String code, String shopId, String productId) {
        Voucher voucher = getVoucherByCode(code);
        
        // 1. Kiểm tra active
        if (!voucher.isActive()) {
            throw new BadRequestException("Voucher đã bị vô hiệu hóa");
        }
        
        // 2. Kiểm tra thời gian
        LocalDateTime now = LocalDateTime.now();
        if (voucher.getStartDate() != null && now.isBefore(voucher.getStartDate())) {
            throw new BadRequestException("Voucher chưa bắt đầu");
        }
        if (voucher.getEndDate() != null && now.isAfter(voucher.getEndDate())) {
            throw new BadRequestException("Voucher đã hết hạn");
        }
        
        // 3. Kiểm tra số lượt
        if (voucher.getUsed() >= voucher.getQuantity()) {
            throw new BadRequestException("Voucher đã hết lượt sử dụng");
        }
        
        // 4. Kiểm tra voucher SHOP có thuộc về shop không
        if (voucher.getType() == VoucherType.SHOP && shopId != null) {
            if (!voucher.getShopId().equals(shopId)) {
                throw new BadRequestException("Voucher không áp dụng cho shop này");
            }
        }
        
        // 5. Kiểm tra voucher WEB có áp dụng cho sản phẩm không
        if (voucher.getType() == VoucherType.WEB && productId != null) {
            List<String> productIds = voucher.getProductIds();
            if (productIds != null && !productIds.isEmpty()) {
                if (!productIds.contains(productId)) {
                    throw new BadRequestException("Voucher không áp dụng cho sản phẩm này");
                }
            }
        }
        
        return voucher;
    }
}
