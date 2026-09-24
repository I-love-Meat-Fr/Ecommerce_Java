package com.ecommerce.cnj70.service.impl;

import com.ecommerce.cnj70.document.Voucher;
import com.ecommerce.cnj70.dto.request.VoucherFormReq;
import com.ecommerce.cnj70.dto.moderation.AuditEvent;
import com.ecommerce.cnj70.enums.DiscountType;
import com.ecommerce.cnj70.enums.UserRole;
import com.ecommerce.cnj70.enums.VoucherType;
import com.ecommerce.cnj70.exception.BadRequestException;
import com.ecommerce.cnj70.exception.ConflictException;
import com.ecommerce.cnj70.exception.ResourceNotFoundException;
import com.ecommerce.cnj70.repository.VoucherRepository;
import com.ecommerce.cnj70.service.AuditEventWriter;
import com.ecommerce.cnj70.service.VoucherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Implement VoucherService
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VoucherServiceImpl implements VoucherService {

    private final VoucherRepository voucherRepository;
    private final MongoTemplate mongoTemplate;
    private final AuditEventWriter auditEventWriter;

    /* === Phase 4B — Audit event helpers (read-only Audit seam) === */

    private void emitAudit(String action, Voucher before, Voucher after, String reason, String actorId) {
        try {
            String beforeStr = before != null
                    ? "code=" + before.getCode() + ";active=" + before.isActive()
                            + ";used=" + before.getUsed() + ";quantity=" + before.getQuantity()
                    : null;
            String afterStr = after != null
                    ? "code=" + after.getCode() + ";active=" + after.isActive()
                            + ";used=" + after.getUsed() + ";quantity=" + after.getQuantity()
                    : null;
            AuditEvent ev = AuditEvent.builder()
                    .actorId(actorId)
                    .role(UserRole.ADMIN)
                    .action("VOUCHER_" + action)
                    .resourceType("VOUCHER")
                    .resourceId(after != null ? after.getId() : (before != null ? before.getId() : null))
                    .reason(reason)
                    .before(beforeStr)
                    .after(afterStr)
                    .createdAt(LocalDateTime.now())
                    .build();
            auditEventWriter.write(ev);
        } catch (Exception ex) {
            log.warn("Audit emission failed for voucher action {}: {}", action, ex.getMessage());
        }
    }
    
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
        // Phase 4B — duplicate code → 409 Conflict (was 400 BadRequest).
        if (voucherRepository.existsByCode(request.getCode())) {
            throw new ConflictException("Mã voucher '" + request.getCode() + "' đã tồn tại trong hệ thống. Vui lòng chọn mã khác.");
        }

        if (request.getDiscountType() == DiscountType.PERCENT) {
            if (request.getDiscountValue().doubleValue() > 100) {
                throw new BadRequestException("Phần trăm giảm không được vượt quá 100%");
            }
        }

        if (request.getDiscountValue() == null
                || request.getDiscountValue().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Giá trị giảm phải lớn hơn 0");
        }

        if (request.getQuantity() <= 0) {
            throw new BadRequestException("Tổng số lượt phải lớn hơn 0");
        }

        // Phase 11 — Bổ sung validation endDate >= startDate
        if (request.getEndDate() != null && request.getStartDate() != null
            && request.getEndDate().isBefore(request.getStartDate())) {
            throw new BadRequestException("Ngày kết thúc phải sau ngày bắt đầu");
        }

        Voucher voucher = Voucher.builder()
                .code(request.getCode().toUpperCase())
                .name(request.getName())
                .type(VoucherType.WEB)
                .shopId(null)
                .shopName(null)
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

        Voucher saved = voucherRepository.save(voucher);
        emitAudit("CREATED", null, saved, "Admin created WEB Voucher", createdBy);
        return saved;
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

    // ============================================================
    // Phase 11 — Admin WEB Voucher operations
    // ============================================================

    /**
     * Phase 11 — Helper kiểm tra voucher thuộc loại WEB.
     * Ném BadRequestException nếu là SHOP Voucher.
     */
    private void requireWebVoucher(Voucher voucher) {
        if (voucher == null || voucher.getType() != VoucherType.WEB) {
            throw new BadRequestException("Voucher này không phải Voucher WEB. Admin không được thao tác Voucher SHOP.");
        }
    }

    @Override
    public Voucher activateWebVoucher(String voucherId) {
        Voucher voucher = getVoucherById(voucherId);
        requireWebVoucher(voucher);
        Voucher before = cloneVoucher(voucher);
        voucher.setActive(true);
        Voucher saved = voucherRepository.save(voucher);
        emitAudit("ACTIVATED", before, saved, "Admin activated WEB Voucher", before.getCreatedBy());
        return saved;
    }

    @Override
    public Voucher deactivateWebVoucher(String voucherId) {
        Voucher voucher = getVoucherById(voucherId);
        requireWebVoucher(voucher);
        Voucher before = cloneVoucher(voucher);
        voucher.setActive(false);
        Voucher saved = voucherRepository.save(voucher);
        emitAudit("DEACTIVATED", before, saved, "Admin deactivated WEB Voucher", before.getCreatedBy());
        return saved;
    }

    @Override
    public Voucher updateWebVoucher(String voucherId, VoucherFormReq request) {
        Voucher voucher = getVoucherById(voucherId);
        requireWebVoucher(voucher);
        Voucher before = cloneVoucher(voucher);

        // Phase 4B — duplicate code → 409 Conflict
        if (request.getCode() != null && !request.getCode().equalsIgnoreCase(voucher.getCode())) {
            if (voucherRepository.existsByCode(request.getCode())) {
                throw new ConflictException("Mã voucher '" + request.getCode() + "' đã tồn tại trong hệ thống.");
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

        // Phase 11 — Bổ sung validation endDate >= startDate sau khi áp dụng giá trị mới
        if (voucher.getEndDate() != null && voucher.getStartDate() != null
            && voucher.getEndDate().isBefore(voucher.getStartDate())) {
            throw new BadRequestException("Ngày kết thúc phải sau ngày bắt đầu");
        }

        // Phase 11 — Đảm bảo WEB Voucher giữ shopId=null và type=WEB
        voucher.setType(VoucherType.WEB);
        voucher.setShopId(null);
        voucher.setShopName(null);

        Voucher saved = voucherRepository.save(voucher);
        emitAudit("UPDATED", before, saved, "Admin updated WEB Voucher", before.getCreatedBy());
        return saved;
    }

    private Voucher cloneVoucher(Voucher src) {
        return Voucher.builder()
                .id(src.getId())
                .code(src.getCode())
                .name(src.getName())
                .type(src.getType())
                .shopId(src.getShopId())
                .shopName(src.getShopName())
                .productIds(src.getProductIds() != null ? new ArrayList<>(src.getProductIds()) : null)
                .discountType(src.getDiscountType())
                .discountValue(src.getDiscountValue())
                .maxDiscountAmount(src.getMaxDiscountAmount())
                .minOrderValue(src.getMinOrderValue())
                .quantity(src.getQuantity())
                .used(src.getUsed())
                .startDate(src.getStartDate())
                .endDate(src.getEndDate())
                .active(src.isActive())
                .createdBy(src.getCreatedBy())
                .createdAt(src.getCreatedAt())
                .updatedAt(src.getUpdatedAt())
                .build();
    }

    @Override
    public void deleteWebVoucher(String voucherId) {
        Voucher voucher = getVoucherById(voucherId);
        requireWebVoucher(voucher);
        Voucher before = cloneVoucher(voucher);
        voucher.setActive(false);
        Voucher saved = voucherRepository.save(voucher);
        emitAudit("DELETED", before, saved, "Admin soft-deleted WEB Voucher", before.getCreatedBy());
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
    public org.springframework.data.domain.Page<Voucher> getWebVouchers(
            Pageable pageable, String q, Boolean active) {
        boolean hasQ = StringUtils.hasText(q);
        boolean hasActive = (active != null);

        if (!hasQ && !hasActive) {
            return voucherRepository.findByType(VoucherType.WEB, pageable);
        }

        if (hasQ && !hasActive) {
            return searchWebVouchers(q.trim(), null, pageable);
        }

        if (!hasQ && hasActive) {
            return voucherRepository.findByTypeAndActive(VoucherType.WEB, active, pageable);
        }

        // search + filter
        return searchWebVouchers(q.trim(), active, pageable);
    }

    /**
     * Tìm WEB Voucher theo keyword (code/name) kết hợp optional active.
     * Dùng MongoTemplate vì cần AND logic giữa type=WEB + search criteria.
     */
    private org.springframework.data.domain.Page<Voucher> searchWebVouchers(
            String q, Boolean active, Pageable pageable) {
        Pattern codePattern = Pattern.compile(Pattern.quote(q), Pattern.CASE_INSENSITIVE);
        Pattern namePattern = Pattern.compile(Pattern.quote(q), Pattern.CASE_INSENSITIVE);

        Criteria searchOr = new Criteria().orOperator(
                Criteria.where("code").regex(codePattern),
                Criteria.where("name").regex(namePattern)
        );

        Criteria baseCriteria = new Criteria().andOperator(
                Criteria.where("type").is("WEB"),
                searchOr
        );

        Criteria countCriteria;
        if (active != null) {
            countCriteria = new Criteria().andOperator(
                    baseCriteria,
                    Criteria.where("active").is(active)
            );
        } else {
            countCriteria = baseCriteria;
        }

        Query query = Query.query(countCriteria).with(pageable);
        long total = mongoTemplate.count(Query.query(countCriteria), Voucher.class);
        List<Voucher> content = mongoTemplate.find(query, Voucher.class);

        return new PageImpl<>(content, pageable, total);
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
    public List<Voucher> getAvailableWebVouchersForCustomer() {
        // Phase 12: Chỉ trả WEB Voucher khả dụng cho Customer website.
        // Lọc: type=WEB, active=true, còn hạn, còn lượt.
        // SHOP Voucher KHÔNG hiển thị trên website công khai.
        return voucherRepository.findByTypeAndActiveTrue(VoucherType.WEB).stream()
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
    public long countAvailableVouchers() {
        return getAvailableVouchers().size();
    }
    
    @Override
    @Deprecated
    public void incrementUsed(String voucherId) {
        Voucher voucher = getVoucherById(voucherId);
        voucher.setUsed(voucher.getUsed() + 1);
        voucherRepository.save(voucher);
    }

    /**
     * Phase 4B — Atomic usedCount increment (race-safe).
     *
     * <p>Performs Mongo conditional update:
     * {@code WHERE _id = ? AND active = true AND used < quantity SET used = used + 1}</p>
     *
     * <p>If matchedCount == 0, the voucher is either exhausted, deactivated,
     * or doesn't exist. Caller MUST treat this as "voucher could not be applied".</p>
     */
    @Override
    public boolean tryIncrementUsed(String voucherId) {
        if (voucherId == null || voucherId.isBlank()) return false;
        Query q = new Query(Criteria.where("_id").is(voucherId)
                .and("active").is(true)
                .andOperator(Criteria.where("$expr").is(
                        new org.bson.Document("$lt",
                                java.util.List.of("$used", "$quantity")))));
        Update u = new Update().inc("used", 1);
        var result = mongoTemplate.updateFirst(q, u, Voucher.class);
        boolean ok = result.getModifiedCount() == 1L;
        if (ok) {
            log.info("Voucher {} used++ (atomic)", voucherId);
        } else {
            log.warn("Voucher {} atomic increment failed (exhausted/inactive/missing)", voucherId);
        }
        return ok;
    }

    /**
     * Phase 4B — Atomic usedCount decrement (cancel/reversal seam).
     *
     * <p>Performs Mongo conditional update:
     * {@code WHERE _id = ? AND used > 0 SET used = used - 1}</p>
     */
    @Override
    public boolean tryDecrementUsed(String voucherId) {
        if (voucherId == null || voucherId.isBlank()) return false;
        Query q = new Query(Criteria.where("_id").is(voucherId)
                .andOperator(Criteria.where("$expr").is(
                        new org.bson.Document("$gt",
                                java.util.List.of("$used", 0)))));
        Update u = new Update().inc("used", -1);
        var result = mongoTemplate.updateFirst(q, u, Voucher.class);
        boolean ok = result.getModifiedCount() == 1L;
        if (ok) {
            log.info("Voucher {} used-- (atomic reversal)", voucherId);
        } else {
            log.warn("Voucher {} atomic decrement failed (already 0/missing)", voucherId);
        }
        return ok;
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
