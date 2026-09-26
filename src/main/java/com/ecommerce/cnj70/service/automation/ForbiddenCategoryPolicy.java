package com.ecommerce.cnj70.service.automation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * C6 — Forbidden category policy (task #7.0).
 *
 * <p>Lưu giữ danh sách categoryId bị cấm không cho vendor đăng bán, lấy từ
 * {@code application.yml}: {@code moderation.auto.forbidden-category-ids}.
 *
 * <p>Tách thành bean riêng (không hard-code config trong Check) cho phép:
 * <ul>
 *   <li>Sử dụng lại trong nhiều check khác (vd report case module).</li>
 *   <li>Hot-reload config qua {@code /actuator/refresh} nếu mai sau dùng
 *       Spring Cloud Config — không cần đổi check.</li>
 *   <li>Test dễ dàng: truyền list thẳng vào constructor.</li>
 * </ul>
 *
 * <p>Nếu config rỗng / null → set rỗng (luôn PASS). Không throw.
 */
@Slf4j
@Component
public class ForbiddenCategoryPolicy {

    private final Set<String> forbiddenIds;

    public ForbiddenCategoryPolicy(
            @Value("${moderation.auto.forbidden-category-ids:}") List<String> forbiddenIds) {
        Set<String> set = new HashSet<>();
        if (forbiddenIds != null) {
            for (String id : forbiddenIds) {
                if (id != null && !id.isBlank()) {
                    set.add(id.trim());
                }
            }
        }
        this.forbiddenIds = Collections.unmodifiableSet(set);
        log.info("[ForbiddenCategoryPolicy] Initialized with {} forbidden category ID(s)",
                this.forbiddenIds.size());
    }

    /** Test-only constructor. */
    public static ForbiddenCategoryPolicy forTest(List<String> forbiddenIds) {
        return new ForbiddenCategoryPolicy(forbiddenIds == null ? List.of() : forbiddenIds);
    }

    public boolean isForbidden(String categoryId) {
        if (categoryId == null || categoryId.isBlank()) {
            return false;
        }
        return forbiddenIds.contains(categoryId.trim());
    }

    public boolean isForbiddenAny(Collection<String> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return false;
        }
        for (String id : categoryIds) {
            if (isForbidden(id)) {
                return true;
            }
        }
        return false;
    }

    public Set<String> getForbiddenIds() {
        return forbiddenIds;
    }
}
