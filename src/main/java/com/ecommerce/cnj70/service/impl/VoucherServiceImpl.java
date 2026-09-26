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
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
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

        // Phase 11 — Bổ sung validation endDate >= startDate (thiếu trong source cũ)
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
        voucher.setActive(true);
        return voucherRepository.save(voucher);
    }

    @Override
    public Voucher deactivateWebVoucher(String voucherId) {
        Voucher voucher = getVoucherById(voucherId);
        requireWebVoucher(voucher);
        voucher.setActive(false);
        return voucherRepository.save(voucher);
    }

    @Override
    public Voucher updateWebVoucher(String voucherId, VoucherFormReq request) {
        Voucher voucher = getVoucherById(voucherId);
        requireWebVoucher(voucher);

        // Phase 11 — Validate code (nếu đổi)
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

        return voucherRepository.save(voucher);
    }

    @Override
    public void deleteWebVoucher(String voucherId) {
        Voucher voucher = getVoucherById(voucherId);
        requireWebVoucher(voucher);
        voucher.setActive(false);
        voucherRepository.save(voucher);
    }
    
    @Override
    public Voucher getVoucherById(String id) {
        if (id == null || id.isBlank()) {
            throw new ResourceNotFoundException("Không tìm thấy voucher");
        }
        // Phase 5 fix + Phase 1 hotfix: Hỗ trợ CẢ 2 kiểu _id (String lẫn ObjectId).
        // Nhiều bản ghi voucher trong DB có _id là ObjectId (24 hex) do insert
        // bằng Compass / script / Spring Data. Nếu chỉ tìm bằng String thì miss →
        // trả null → ném ResourceNotFoundException("Không tìm thấy voucher").
        //
        // FIX (Sep 2026): KHÔNG chuyển _id từ ObjectId sang String trong raw
        // Document nữa. Lý do: nếu caller sau đó gọi save() (vd: incrementUsed),
        // Spring Data sẽ query bằng String _id, không match ObjectId _id trong DB
        // → MongoDB tưởng là INSERT mới → trigger E11000 duplicate key trên
        // unique index `code`. Cách an toàn:
        //   1) Dùng mongoTemplate.findById(oid, Voucher.class) — converter tự
        //      convert ObjectId → String cho Voucher.id, nhưng KHÔNG modify raw doc
        //   2) Nếu caller cần save lại, họ nên dùng updateFirst() (atomic, không
        //      bị bug này) — xem incrementUsed().
        //
        // Cách 1) thử String _id trước (cho voucher mới tạo qua Spring Data)
        org.bson.Document raw = mongoTemplate.getCollection("vouchers")
                .find(new org.bson.Document("_id", id))
                .first();
        // Cách 2) Fallback ObjectId _id (cho voucher cũ insert bằng Compass)
        if (raw == null && id.length() == 24 && id.matches("[0-9a-fA-F]+")) {
            org.bson.types.ObjectId oid = new org.bson.types.ObjectId(id);
            raw = mongoTemplate.getCollection("vouchers")
                    .find(new org.bson.Document("_id", oid))
                    .first();
            // QUAN TRỌNG: KHÔNG gọi raw.put("_id", id) — giữ nguyên ObjectId _id
            // để converter đọc đúng và để save() (nếu có) không bị duplicate key.
        }
        if (raw == null) {
            throw new ResourceNotFoundException("Không tìm thấy voucher");
        }
        // Ensure _class is set so MappingMongoConverter reads as Voucher.
        if (!raw.containsKey("_class")) {
            raw.put("_class", Voucher.class.getName());
        }
        Voucher voucher = mongoTemplate.getConverter().read(Voucher.class, raw);
        // Fallback: nếu _id trong DB không phải ObjectId (vd: voucher rỗng/legacy
        // data), set id từ parameter để caller có thể dùng.
        if (voucher.getId() == null) {
            voucher.setId(id);
        }
        return voucher;
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
        // Phase 1 hotfix — Dùng MongoTemplate để match CẢ 2 kiểu:
        //   1. type = "WEB" (enum được persist đúng)
        //   2. type IS NULL (data cũ import từ script ngoài chưa có field)
        // Tránh việc admin thấy trắng bảng dù DB có hàng chục voucher.
        Criteria criteria = new Criteria().orOperator(
                Criteria.where("type").is("WEB"),
                Criteria.where("type").is(VoucherType.WEB.name()),
                Criteria.where("type").exists(false),
                Criteria.where("type").is(null)
        );
        return mongoTemplate.find(Query.query(criteria), Voucher.class);
    }

    @Override
    public org.springframework.data.domain.Page<Voucher> getWebVouchers(
            Pageable pageable, String q, Boolean active) {
        boolean hasQ = StringUtils.hasText(q);
        boolean hasActive = (active != null);

        // Build baseCriteria: type=WEB OR type missing OR type null
        Criteria typeCriteria = new Criteria().orOperator(
                Criteria.where("type").is("WEB"),
                Criteria.where("type").is(VoucherType.WEB.name()),
                Criteria.where("type").exists(false),
                Criteria.where("type").is(null)
        );

        if (!hasQ && !hasActive) {
            Query query = Query.query(typeCriteria).with(pageable);
            long total = mongoTemplate.count(Query.query(typeCriteria), Voucher.class);
            List<Voucher> content = mongoTemplate.find(query, Voucher.class);
            return new PageImpl<>(content, pageable, total);
        }

        if (hasQ && !hasActive) {
            return searchWebVouchers(q.trim(), null, pageable, typeCriteria);
        }

        if (!hasQ && hasActive) {
            Criteria combined = new Criteria().andOperator(
                    typeCriteria,
                    Criteria.where("active").is(active)
            );
            Query query = Query.query(combined).with(pageable);
            long total = mongoTemplate.count(Query.query(combined), Voucher.class);
            List<Voucher> content = mongoTemplate.find(query, Voucher.class);
            return new PageImpl<>(content, pageable, total);
        }

        // search + filter
        return searchWebVouchers(q.trim(), active, pageable, typeCriteria);
    }

    /**
     * Tìm WEB Voucher theo keyword (code/name) kết hợp optional active.
     * Dùng MongoTemplate vì cần AND logic giữa type=WEB (fallback) + search criteria.
     */
    private org.springframework.data.domain.Page<Voucher> searchWebVouchers(
            String q, Boolean active, Pageable pageable, Criteria typeCriteria) {
        Pattern codePattern = Pattern.compile(Pattern.quote(q), Pattern.CASE_INSENSITIVE);
        Pattern namePattern = Pattern.compile(Pattern.quote(q), Pattern.CASE_INSENSITIVE);

        Criteria searchOr = new Criteria().orOperator(
                Criteria.where("code").regex(codePattern),
                Criteria.where("name").regex(namePattern)
        );

        Criteria baseCriteria = new Criteria().andOperator(
                typeCriteria,
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
        // Phase 12 + Phase 1 hotfix: Chỉ trả WEB Voucher khả dụng cho Customer website.
        // Match CẢ type=WEB (enum persist đúng) và type=null (data cũ).
        Criteria typeCriteria = new Criteria().orOperator(
                Criteria.where("type").is("WEB"),
                Criteria.where("type").is(VoucherType.WEB.name()),
                Criteria.where("type").exists(false),
                Criteria.where("type").is(null)
        );
        Criteria criteria = new Criteria().andOperator(
                typeCriteria,
                Criteria.where("active").is(true)
        );
        return mongoTemplate.find(Query.query(criteria), Voucher.class).stream()
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
    public void incrementUsed(String voucherId) {
        // Fix lỗi E11000 duplicate key trên collection `vouchers` khi increment
        // used counter sau khi tạo Order thành công.
        //
        // Bối cảnh: VoucherServiceImpl.getVoucherById() có "Phase 5 hotfix"
        // chuyển _id từ ObjectId sang String trong raw Document để converter
        // đọc được. Nhưng khi gọi voucherRepository.save() ngay sau đó,
        // Spring Data query bằng String _id không match ObjectId _id trong DB
        // → MongoDB hiểu là INSERT mới → trigger E11000 duplicate key trên
        // unique index `code` (vd: code "PHASE12_TEST_001" đã tồn tại).
        //
        // Fix: dùng mongoTemplate.updateFirst() với Criteria chấp nhận CẢ 2
        // kiểu _id (String lẫn ObjectId). Cách này:
        //   1) Atomic — không cần load cả voucher
        //   2) Không trigger unique-index conflict vì UPDATE không tạo document mới
        //   3) An toàn với cả voucher cũ (_id ObjectId) lẫn voucher mới (_id String)
        if (voucherId == null || voucherId.isBlank()) {
            log.warn("incrementUsed: voucherId null/blank, bỏ qua");
            return;
        }
        org.springframework.data.mongodb.core.query.Criteria idCriteria =
                buildIdCriteria(voucherId);
        org.springframework.data.mongodb.core.query.Query q =
                new org.springframework.data.mongodb.core.query.Query(idCriteria);
        org.springframework.data.mongodb.core.query.Update u =
                new org.springframework.data.mongodb.core.query.Update().inc("used", 1);
        var result = mongoTemplate.updateFirst(q, u, Voucher.class);
        if (result.getMatchedCount() == 0L) {
            // Voucher không tồn tại — log warning nhưng KHÔNG throw để tránh
            // rollback Order đã tạo thành công. Lý do: voucher có thể bị xóa
            // giữa lúc user apply và lúc createOrder chạy xong (race condition
            // hiếm gặp nhưng có thể xảy ra trong môi trường có nhiều admin).
            log.warn("incrementUsed: voucher {} không tìm thấy trong DB, bỏ qua", voucherId);
        }
    }

    /**
     * Build Criteria cho _id chấp nhận CẢ String lẫn ObjectId.
     * Phase 5 hotfix: Voucher cũ trong DB có _id là ObjectId (24 hex chars);
     * Voucher mới tạo qua Admin UI có _id là String UUID.
     * Cả 2 trường hợp cần được query thành công bằng cùng một id truyền vào.
     */
    private org.springframework.data.mongodb.core.query.Criteria buildIdCriteria(String voucherId) {
        org.springframework.data.mongodb.core.query.Criteria criteria =
                org.springframework.data.mongodb.core.query.Criteria.where("_id").is(voucherId);
        if (voucherId.length() == 24 && voucherId.matches("[0-9a-fA-F]+")) {
            criteria = org.springframework.data.mongodb.core.query.Criteria.where("_id")
                    .is(voucherId)
                    .orOperator(
                            org.springframework.data.mongodb.core.query.Criteria.where("_id")
                                    .is(voucherId),
                            org.springframework.data.mongodb.core.query.Criteria.where("_id")
                                    .is(new org.bson.types.ObjectId(voucherId))
                    );
        }
        return criteria;
    }

    @Override
    public boolean tryIncrementUsed(String voucherId) {
        if (voucherId == null) return false;
        org.springframework.data.mongodb.core.query.Criteria idCriteria = buildIdCriteria(voucherId);
        org.springframework.data.mongodb.core.query.Query q = new org.springframework.data.mongodb.core.query.Query(
                idCriteria
                        .and("active").is(true)
                        .andOperator(org.springframework.data.mongodb.core.query.Criteria.where("$expr").is(
                                new org.bson.Document("$lt",
                                        java.util.List.of("$used", "$quantity"))))
        );
        org.springframework.data.mongodb.core.query.Update u = new org.springframework.data.mongodb.core.query.Update()
                .inc("used", 1);
        var result = mongoTemplate.updateFirst(q, u, Voucher.class);
        if (result.getMatchedCount() == 0L) {
            log.debug("tryIncrementUsed: voucher {} exhausted/inactive/missing", voucherId);
            return false;
        }
        return true;
    }

    @Override
    public boolean tryDecrementUsed(String voucherId) {
        if (voucherId == null) return false;
        org.springframework.data.mongodb.core.query.Query q = new org.springframework.data.mongodb.core.query.Query(
                org.springframework.data.mongodb.core.query.Criteria.where("_id").is(voucherId)
                        .andOperator(org.springframework.data.mongodb.core.query.Criteria.where("$expr").is(
                                new org.bson.Document("$gt",
                                        java.util.List.of("$used", 0))))
        );
        org.springframework.data.mongodb.core.query.Update u = new org.springframework.data.mongodb.core.query.Update()
                .inc("used", -1);
        var result = mongoTemplate.updateFirst(q, u, Voucher.class);
        if (result.getMatchedCount() == 0L) {
            log.debug("tryDecrementUsed: voucher {} already at 0 / missing", voucherId);
            return false;
        }
        return true;
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
