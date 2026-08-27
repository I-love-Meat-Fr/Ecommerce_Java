package com.ecommerce.cnj70.service.impl;

import com.ecommerce.cnj70.document.Voucher;
import com.ecommerce.cnj70.enums.VoucherType;
import com.ecommerce.cnj70.exception.BadRequestException;
import com.ecommerce.cnj70.exception.ResourceNotFoundException;
import com.ecommerce.cnj70.repository.VoucherRepository;
import com.ecommerce.cnj70.service.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VoucherServiceImpl implements VoucherService {

    private final VoucherRepository voucherRepository;
    private final MongoTemplate mongoTemplate;

    @Override
    public Voucher save(Voucher voucher) {
        if (voucherRepository.existsByCode(voucher.getCode())) {
            throw new BadRequestException("Mã voucher đã tồn tại: " + voucher.getCode());
        }
        return voucherRepository.save(voucher);
    }

    @Override
    public Voucher findByCode(String code) {
        return voucherRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy voucher: " + code));
    }

    @Override
    public Voucher findById(String id) {
        return voucherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy voucher"));
    }

    @Override
    public List<Voucher> findAvailable() {
        LocalDateTime now = LocalDateTime.now();
        return voucherRepository.findAll().stream()
                .filter(v -> v.isActive())
                .filter(v -> v.getStartDate() != null && !now.isBefore(v.getStartDate()))
                .filter(v -> v.getEndDate() != null && !now.isAfter(v.getEndDate()))
                .filter(v -> v.getUsed() < v.getQuantity())
                .toList();
    }

    @Override
    public List<Voucher> findAvailableWeb() {
        return findAvailable().stream()
                .filter(v -> v.getType() == VoucherType.WEB)
                .toList();
    }

    @Override
    public List<Voucher> findAvailableByShopId(String shopId) {
        return findAvailable().stream()
                .filter(v -> shopId.equals(v.getShopId()))
                .toList();
    }

    @Override
    public List<Voucher> findByShopId(String shopId) {
        return voucherRepository.findByShopId(shopId);
    }

    @Override
    public void incrementUsed(String voucherId) {
        Voucher voucher = findById(voucherId);
        voucher.setUsed(voucher.getUsed() + 1);
        voucherRepository.save(voucher);
    }

    @Override
    public boolean incrementUsedIfAvailable(String voucherId) {
        Query query = new Query(Criteria.where("_id").is(voucherId)
                .and("used").lt(1));
        Voucher voucher = findById(voucherId);
        if (voucher.getUsed() < voucher.getQuantity()) {
            incrementUsed(voucherId);
            return true;
        }
        return false;
    }
}
