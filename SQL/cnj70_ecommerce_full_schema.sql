-- =====================================================================
-- CNJ70 ECOMMERCE - COMPLETE SQL SCHEMA
-- Generated from MongoDB Document classes → MySQL/PostgreSQL
-- Database: cnj70_ecommerce
-- =====================================================================

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS users, shops, categories, products, product_specifications,
                     product_variants, variant_specifications, orders, order_items,
                     order_shippings, carts, cart_items, vouchers, reviews,
                     kyc_profiles, banners, complaints, violations, report_cases,
                     refunds, returns, audit_logs, audit_log_entries,
                     escalations, moderation_history, legal_documents,
                     scheduler_idempotency;
SET FOREIGN_KEY_CHECKS = 1;


-- =====================================================================
-- 1. USERS (com.ecommerce.cnj70.document.User)
-- =====================================================================
CREATE TABLE users (
    id                        VARCHAR(50) PRIMARY KEY,
    email                     VARCHAR(255) NOT NULL UNIQUE,
    password                  VARCHAR(255),
    full_name                 VARCHAR(255),
    phone                     VARCHAR(20),
    address                   TEXT,
    role                      ENUM('ADMIN','MODERATOR','VENDOR','CUSTOMER')
                              NOT NULL DEFAULT 'CUSTOMER',
    status                    ENUM('ACTIVE','LOCKED','UNVERIFIED')
                              NOT NULL DEFAULT 'UNVERIFIED',
    shop_id                   VARCHAR(50),
    avatar_url                TEXT,

    -- PII encrypted fields (AES-256-GCM Base64)
    encrypted_citizen_id      TEXT,
    encrypted_tax_code        TEXT,
    encrypted_bank_account    TEXT,

    -- Terms / Privacy acceptance tracking
    accepted_terms_at         DATETIME,
    accepted_terms_version    INT DEFAULT 0,
    accepted_privacy_at       DATETIME,
    accepted_privacy_version  INT DEFAULT 0,
    marketing_opt_in          BOOLEAN DEFAULT FALSE,

    -- KYC status (denormalized from KycProfile)
    kyc_status                ENUM('NOT_SUBMITTED','PENDING_THIRD_PARTY',
                                  'THIRD_PARTY_REJECTED','PENDING_ADMIN',
                                  'APPROVED','ADMIN_REJECTED','SUSPENDED')
                              DEFAULT 'NOT_SUBMITTED',

    -- Phase 3A enforcement
    enforcement_actor_id      VARCHAR(50),
    enforcement_reason        TEXT,
    enforcement_at            DATETIME,

    created_at                DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at                DATETIME DEFAULT CURRENT_TIMESTAMP
                                    ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_users_email       (email),
    INDEX idx_users_role        (role),
    INDEX idx_users_status      (status),
    INDEX idx_users_shop_id     (shop_id),
    INDEX idx_users_kyc_status  (kyc_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- =====================================================================
-- 2. SHOPS (com.ecommerce.cnj70.document.Shop)
-- =====================================================================
CREATE TABLE shops (
    id                    VARCHAR(50) PRIMARY KEY,
    owner_id              VARCHAR(50) NOT NULL,

    shop_name             VARCHAR(255) NOT NULL UNIQUE,
    description           TEXT,
    logo_url              TEXT,
    banner_url            TEXT,

    status                ENUM('PENDING','APPROVED','REJECTED',
                              'SUSPENDED','RESTRICTED')
                          DEFAULT 'PENDING',
    active                BOOLEAN DEFAULT TRUE,

    kyc_status            ENUM('NOT_SUBMITTED','PENDING_THIRD_PARTY',
                              'THIRD_PARTY_REJECTED','PENDING_ADMIN',
                              'APPROVED','ADMIN_REJECTED','SUSPENDED')
                          DEFAULT 'NOT_SUBMITTED',
    kyc_reference_id      VARCHAR(255),
    kyc_approved_at       DATETIME,
    kyc_rejection_reason  TEXT,
    kyc_submitted_at      DATETIME,
    kyc_document_version  INT DEFAULT 0,

    encrypted_citizen_id  TEXT,
    encrypted_tax_code    TEXT,
    encrypted_bank_account TEXT,

    rejection_reason      TEXT,
    deactivation_reason   TEXT,
    action_by             VARCHAR(50),
    action_at             DATETIME,

    enforcement_actor_id  VARCHAR(50),
    enforcement_reason    TEXT,
    enforcement_at        DATETIME,

    created_at            DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at            DATETIME DEFAULT CURRENT_TIMESTAMP
                                ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_shops_owner_id   (owner_id),
    INDEX idx_shops_shop_name  (shop_name),
    INDEX idx_shops_status     (status),
    INDEX idx_shops_active     (active),
    INDEX idx_shops_kyc_status (kyc_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- =====================================================================
-- 3. CATEGORIES (com.ecommerce.cnj70.document.Category)
-- =====================================================================
CREATE TABLE categories (
    id           VARCHAR(50) PRIMARY KEY,
    name         VARCHAR(255) NOT NULL UNIQUE,
    description  TEXT,
    icon_url     TEXT,
    parent_id    VARCHAR(50),
    sort_order   INT DEFAULT 0,
    active       BOOLEAN DEFAULT TRUE,

    created_at   DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME DEFAULT CURRENT_TIMESTAMP
                       ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_categories_name        (name),
    INDEX idx_categories_parent_id   (parent_id),
    INDEX idx_categories_active      (active),
    INDEX idx_categories_sort_order  (sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- =====================================================================
-- 4. PRODUCTS (com.ecommerce.cnj70.document.Product)
-- =====================================================================
CREATE TABLE products (
    id                    VARCHAR(50) PRIMARY KEY,
    shop_id               VARCHAR(50) NOT NULL,
    shop_name             VARCHAR(255),

    name                  VARCHAR(500) NOT NULL,
    brand                 VARCHAR(255),
    warranty_months       INT,
    manufacturer          VARCHAR(255),
    manufacturer_address  TEXT,
    description           TEXT,
    rich_description      TEXT,

    price                 DECIMAL(15,2) NOT NULL DEFAULT 0,
    stock                 INT NOT NULL DEFAULT 0,

    category_id           VARCHAR(50),
    category_name         VARCHAR(255),

    image_urls            JSON,                      -- list of URLs
    thumbnail_url         TEXT,

    status                ENUM('DRAFT','ACTIVE','OUT_OF_STOCK','HIDDEN')
                          DEFAULT 'DRAFT',

    -- Phase 2A moderation workflow
    moderation_status     ENUM('PENDING_MANUAL','AUTO_PASSED','AUTO_REJECTED',
                              'APPROVED','REJECTED','ESCALATED','PENDING_AUTO')
                          DEFAULT 'PENDING_MANUAL',
    moderation_actor_id   VARCHAR(50),
    moderation_reason     TEXT,
    moderation_at         DATETIME,

    -- Phase 3A enforcement
    enforcement_actor_id  VARCHAR(50),
    enforcement_reason    TEXT,
    enforcement_at        DATETIME,

    rating                DOUBLE DEFAULT 0.0,
    review_count          INT DEFAULT 0,
    sold                  INT DEFAULT 0,

    created_at            DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at            DATETIME DEFAULT CURRENT_TIMESTAMP
                                ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_products_shop_id            (shop_id),
    INDEX idx_products_name               (name),
    INDEX idx_products_category_id        (category_id),
    INDEX idx_products_status             (status),
    INDEX idx_products_moderation_status  (moderation_status),
    INDEX idx_products_rating             (rating DESC),
    INDEX idx_products_sold               (sold DESC),
    INDEX idx_products_created_at         (created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- Embedded array: Product.specifications
CREATE TABLE product_specifications (
    id          VARCHAR(50) PRIMARY KEY,
    product_id  VARCHAR(50) NOT NULL,
    name        VARCHAR(255) NOT NULL,
    value       VARCHAR(500),
    unit        VARCHAR(50),

    INDEX idx_prod_spec_product_id (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- Embedded array: Product.variants
CREATE TABLE product_variants (
    id          VARCHAR(50) PRIMARY KEY,
    product_id  VARCHAR(50) NOT NULL,
    sku         VARCHAR(100),
    price       DECIMAL(15,2),
    stock       INT DEFAULT 0,

    INDEX idx_prod_var_product_id (product_id),
    UNIQUE INDEX idx_prod_var_sku (sku)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- Nested array inside variant: ProductVariant.specifications
CREATE TABLE variant_specifications (
    id          VARCHAR(50) PRIMARY KEY,
    variant_id  VARCHAR(50) NOT NULL,
    name        VARCHAR(255) NOT NULL,
    value       VARCHAR(500),
    unit        VARCHAR(50),

    INDEX idx_var_spec_variant_id (variant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- =====================================================================
-- 5. ORDERS (com.ecommerce.cnj70.document.Order)
-- =====================================================================
CREATE TABLE orders (
    id                VARCHAR(50) PRIMARY KEY,
    user_id           VARCHAR(50) NOT NULL,
    user_name         VARCHAR(255),
    user_email        VARCHAR(255),
    user_phone        VARCHAR(20),
    shipping_address  TEXT,

    subtotal          DECIMAL(15,2) DEFAULT 0,
    shipping_fee      DECIMAL(15,2) DEFAULT 0,
    discount          DECIMAL(15,2) DEFAULT 0,
    total_amount      DECIMAL(15,2) DEFAULT 0,

    voucher_id        VARCHAR(50),
    voucher_code      VARCHAR(50),
    voucher_name      VARCHAR(255),

    status            ENUM('PENDING','PREPARING','SHIPPING','DELIVERED','CANCELLED')
                      DEFAULT 'PENDING',

    payment_method    ENUM('COD','VNPAY','BANK_QR') DEFAULT 'COD',
    payment_status    ENUM('PENDING','PAID','FAILED','REFUNDED')
                      DEFAULT 'PENDING',
    paid              BOOLEAN DEFAULT FALSE,
    paid_at           DATETIME,
    refunded_at       DATETIME,

    shop_id           VARCHAR(50),
    shop_name         VARCHAR(255),

    -- Map<shopId, SubOrderShipping> stored as JSON for flexibility
    shipping_by_shop  JSON,

    delivered_at      DATETIME,

    created_at        DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME DEFAULT CURRENT_TIMESTAMP
                            ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_orders_user_id         (user_id),
    INDEX idx_orders_status          (status),
    INDEX idx_orders_payment_status  (payment_status),
    INDEX idx_orders_shop_id         (shop_id),
    INDEX idx_orders_voucher_id      (voucher_id),
    INDEX idx_orders_created_at      (created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- Embedded array: Order.items (OrderItem)
CREATE TABLE order_items (
    id            VARCHAR(50) PRIMARY KEY,
    order_id      VARCHAR(50) NOT NULL,
    shop_id       VARCHAR(50),
    shop_name     VARCHAR(255),
    product_id    VARCHAR(50),
    product_name  VARCHAR(500),
    image_url     TEXT,
    price         DECIMAL(15,2) NOT NULL,
    quantity      INT NOT NULL DEFAULT 1,
    subtotal      DECIMAL(15,2) NOT NULL,

    INDEX idx_order_items_order_id   (order_id),
    INDEX idx_order_items_product_id (product_id),
    INDEX idx_order_items_shop_id    (shop_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- Normalized SubOrderShipping from Order.shippingByShop map
CREATE TABLE order_shippings (
    id               VARCHAR(50) PRIMARY KEY,
    order_id         VARCHAR(50) NOT NULL,
    shop_id          VARCHAR(50) NOT NULL,
    shop_name        VARCHAR(255),

    status           ENUM('PENDING','PACKED','PICKED_UP','IN_TRANSIT',
                          'DELIVERED','FAILED','RETURNED') DEFAULT 'PENDING',
    shipping_fee     DECIMAL(15,2) DEFAULT 0,
    tracking_number  VARCHAR(100),
    carrier          VARCHAR(100),
    shipped_at       DATETIME,
    delivered_at     DATETIME,
    failure_reason   TEXT,
    note             TEXT,

    updated_at       DATETIME DEFAULT CURRENT_TIMESTAMP
                          ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_order_shippings_order_id (order_id),
    INDEX idx_order_shippings_shop_id  (shop_id),
    INDEX idx_order_shippings_status   (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- =====================================================================
-- 6. CARTS (com.ecommerce.cnj70.document.Cart)
-- =====================================================================
CREATE TABLE carts (
    id                   VARCHAR(50) PRIMARY KEY,
    user_id              VARCHAR(50) NOT NULL UNIQUE,
    applied_voucher_code VARCHAR(50),
    updated_at           DATETIME DEFAULT CURRENT_TIMESTAMP
                              ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_carts_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- Embedded array: Cart.items (CartItem)
CREATE TABLE cart_items (
    id           VARCHAR(50) PRIMARY KEY,
    cart_id      VARCHAR(50) NOT NULL,
    product_id   VARCHAR(50),
    product_name VARCHAR(500),
    image_url    TEXT,
    price        DECIMAL(15,2),
    quantity     INT DEFAULT 1,
    subtotal     DECIMAL(15,2),
    shop_id      VARCHAR(50),
    shop_name    VARCHAR(255),
    stock        INT,

    INDEX idx_cart_items_cart_id    (cart_id),
    INDEX idx_cart_items_product_id (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- =====================================================================
-- 7. REVIEWS (com.ecommerce.cnj70.document.Review)
-- =====================================================================
CREATE TABLE reviews (
    id                          VARCHAR(50) PRIMARY KEY,
    product_id                  VARCHAR(50) NOT NULL,
    user_id                     VARCHAR(50) NOT NULL,
    user_name                   VARCHAR(255),
    user_avatar                 TEXT,
    rating                      INT NOT NULL,
    comment                     TEXT,
    images                      JSON,                  -- list of URLs

    -- Review moderation
    moderation_status           ENUM('VISIBLE','REPORTED','HIDDEN','DELETED')
                                DEFAULT 'VISIBLE',
    moderation_reason           TEXT,
    moderated_by                VARCHAR(50),
    moderated_at                DATETIME,
    report_count                INT DEFAULT 0,

    -- Phase 2B
    moderation_actor_id         VARCHAR(50),
    moderation_at               DATETIME,
    hidden                      BOOLEAN DEFAULT FALSE,
    hidden_reason               TEXT,
    pipeline_moderation_status  ENUM('PENDING_MANUAL','AUTO_PASSED','AUTO_REJECTED',
                                    'APPROVED','REJECTED','ESCALATED','PENDING_AUTO'),

    created_at                  DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at                  DATETIME DEFAULT CURRENT_TIMESTAMP
                                    ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_reviews_product_id         (product_id),
    INDEX idx_reviews_user_id            (user_id),
    INDEX idx_reviews_moderation_status  (moderation_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- =====================================================================
-- 8. VOUCHERS (com.ecommerce.cnj70.document.Voucher)
-- =====================================================================
CREATE TABLE vouchers (
    id                  VARCHAR(50) PRIMARY KEY,
    code                VARCHAR(50) NOT NULL UNIQUE,
    name                VARCHAR(255),
    type                ENUM('SHOP','WEB') NOT NULL,

    shop_id             VARCHAR(50),
    shop_name           VARCHAR(255),
    product_ids         JSON,                       -- null/empty = all products

    discount_type       ENUM('PERCENT','AMOUNT') NOT NULL,
    discount_value      DECIMAL(15,2),
    max_discount_amount DECIMAL(15,2),
    min_order_value     DECIMAL(15,2),

    quantity            INT NOT NULL DEFAULT 0,
    used                INT DEFAULT 0,

    start_date          DATETIME,
    end_date            DATETIME,
    active              BOOLEAN DEFAULT TRUE,

    created_by          VARCHAR(50),
    created_at          DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME DEFAULT CURRENT_TIMESTAMP
                            ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_vouchers_code       (code),
    INDEX idx_vouchers_type       (type),
    INDEX idx_vouchers_shop_id    (shop_id),
    INDEX idx_vouchers_end_date   (end_date),
    INDEX idx_vouchers_active     (active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- =====================================================================
-- 9. KYC PROFILES (com.ecommerce.cnj70.document.KycProfile)
-- =====================================================================
CREATE TABLE kyc_profiles (
    id                       VARCHAR(50) PRIMARY KEY,
    user_id                  VARCHAR(50) NOT NULL UNIQUE,

    status                   ENUM('NOT_SUBMITTED','PENDING_THIRD_PARTY',
                                 'THIRD_PARTY_REJECTED','PENDING_ADMIN',
                                 'APPROVED','ADMIN_REJECTED','SUSPENDED')
                             DEFAULT 'NOT_SUBMITTED',

    owner_full_name          VARCHAR(255),
    id_number                VARCHAR(50),
    id_front_image_url       TEXT,
    id_back_image_url        TEXT,

    business_name            VARCHAR(255),
    tax_code                 VARCHAR(50),
    business_license_url     TEXT,

    bank_account             VARCHAR(50),
    bank_name                VARCHAR(255),
    bank_branch              VARCHAR(255),

    third_party_result       VARCHAR(50),
    third_party_rejection_reason TEXT,
    third_party_reference_id VARCHAR(255),
    third_party_verified_at  DATETIME,

    admin_reviewer_id        VARCHAR(50),
    admin_reviewer_name      VARCHAR(255),
    admin_note               TEXT,
    admin_reviewed_at        DATETIME,

    submitted_at             DATETIME,
    last_resubmitted_at      DATETIME,
    submit_count             INT,

    created_at               DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at               DATETIME DEFAULT CURRENT_TIMESTAMP
                                 ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_kyc_user_id (user_id),
    INDEX idx_kyc_status  (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- =====================================================================
-- 10. BANNERS (com.ecommerce.cnj70.document.Banner)
-- =====================================================================
CREATE TABLE banners (
    id           VARCHAR(50) PRIMARY KEY,
    title        VARCHAR(500),
    description  TEXT,
    tag          VARCHAR(100),
    tag_icon     VARCHAR(100),
    image_url    TEXT,
    link         TEXT,
    cta_text     VARCHAR(100),
    cta_icon     VARCHAR(100),
    theme        VARCHAR(50),
    status       ENUM('PUBLISHED','UNPUBLISHED') DEFAULT 'UNPUBLISHED',
    position     VARCHAR(100),
    sort_order   INT,

    created_at   DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME DEFAULT CURRENT_TIMESTAMP
                      ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_banners_image_url (image_url),
    INDEX idx_banners_status    (status),
    INDEX idx_banners_position  (position)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- =====================================================================
-- 11. COMPLAINTS (com.ecommerce.cnj70.document.Complaint)
-- =====================================================================
CREATE TABLE complaints (
    id                          VARCHAR(50) PRIMARY KEY,

    customer_id                 VARCHAR(50) NOT NULL,
    customer_name               VARCHAR(255),
    customer_email              VARCHAR(255),

    shop_id                     VARCHAR(50),
    shop_name                   VARCHAR(255),
    vendor_id                   VARCHAR(50),

    order_id                    VARCHAR(50),

    reason                      ENUM('NON_DELIVERY','WRONG_PRODUCT','DAMAGED',
                                    'WARRANTY','OTHER'),
    description                 TEXT,
    evidence                    JSON,                  -- list of URLs

    status                      ENUM('OPEN','VENDOR_RESPONDED','ESCALATED',
                                    'MODERATOR_REVIEW','ESCALATED_L2',
                                    'ADMIN_REVIEW','RESOLVED','CLOSED','REJECTED')
                                DEFAULT 'OPEN',
    level                       ENUM('LEVEL_0','LEVEL_1','LEVEL_2')
                                DEFAULT 'LEVEL_0',

    vendor_response             TEXT,
    vendor_responded_at         DATETIME,
    vendor_responded_by_id      VARCHAR(50),
    vendor_responded_by_email   VARCHAR(255),
    vendor_reply_count          INT DEFAULT 0,

    assigned_moderator_id       VARCHAR(50),
    assigned_moderator_email    VARCHAR(255),
    moderator_assigned_at       DATETIME,

    assigned_admin_id           VARCHAR(50),
    assigned_admin_email        VARCHAR(255),
    admin_assigned_at           DATETIME,

    decision                    TEXT,
    decision_reason             TEXT,
    resolved_at                 DATETIME,
    resolved_by_id              VARCHAR(50),
    resolved_by_email           VARCHAR(255),
    resolved_by_role            VARCHAR(50),

    vendor_response_deadline    DATETIME,
    moderator_resolution_deadline DATETIME,
    escalation_count            INT DEFAULT 0,

    order_item_ids              JSON,

    created_at                  DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at                  DATETIME DEFAULT CURRENT_TIMESTAMP
                                    ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_complaints_customer_id (customer_id),
    INDEX idx_complaints_shop_id     (shop_id),
    INDEX idx_complaints_order_id    (order_id),
    INDEX idx_complaints_status      (status),
    INDEX idx_complaints_level       (level),
    INDEX idx_complaints_created_at  (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- =====================================================================
-- 12. VIOLATIONS (com.ecommerce.cnj70.document.Violation)
-- =====================================================================
CREATE TABLE violations (
    id              VARCHAR(50) PRIMARY KEY,

    shop_id         VARCHAR(50) NOT NULL,
    product_id      VARCHAR(50),
    product_name    VARCHAR(500),
    order_id        VARCHAR(50),

    type            ENUM('WARNING','VIOLATION','BAN') NOT NULL,
    severity        ENUM('LOW','MEDIUM','HIGH','CRITICAL') NOT NULL,

    reason          TEXT,
    admin_note      TEXT,
    created_by      VARCHAR(50),
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,

    resolved_at     DATETIME,
    resolved_by     VARCHAR(50),
    resolution_note TEXT,

    INDEX idx_violations_shop_id     (shop_id),
    INDEX idx_violations_type        (type),
    INDEX idx_violations_severity    (severity),
    INDEX idx_violations_created_at  (created_at),
    INDEX idx_violations_resolved_at (resolved_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- =====================================================================
-- 13. REPORT CASES (com.ecommerce.cnj70.document.ReportCase)
-- =====================================================================
CREATE TABLE report_cases (
    id                       VARCHAR(50) PRIMARY KEY,

    resource_type            ENUM('PRODUCT','REVIEW'),
    resource_id              VARCHAR(50),
    resource_name            VARCHAR(500),

    target_type              ENUM('PRODUCT','REVIEW','SHOP','USER'),
    target_id                VARCHAR(50),
    target_snapshot          VARCHAR(500),

    reporter_id              VARCHAR(50),
    reporter_name            VARCHAR(255),
    reporter_email           VARCHAR(255),

    auto_moderation_status   VARCHAR(50),

    assigned_moderator_email VARCHAR(255),

    vendor_id                VARCHAR(50),
    vendor_name              VARCHAR(255),
    shop_id                  VARCHAR(50),
    shop_name                VARCHAR(255),

    source                   VARCHAR(10) DEFAULT 'USER',  -- USER | AUTO

    reason                   TEXT,
    report_reason            ENUM('SPAM','FAKE','OFFENSIVE','COPYRIGHT',
                                 'FRAUD','OTHER'),
    description              TEXT,
    evidence                 JSON,
    auto_flags               JSON,

    status                   ENUM('PENDING','OPEN','IN_REVIEW','RESOLVED',
                                 'APPROVED','REJECTED','ESCALATED')
                             DEFAULT 'PENDING',
    assigned_moderator_id    VARCHAR(50),

    decision                 ENUM('APPROVE','REJECT','ESCALATE','NO_ACTION'),
    decision_reason          TEXT,
    note                     TEXT,
    target_status_before     VARCHAR(50),
    target_status_after      VARCHAR(50),

    priority                 INT DEFAULT 0,

    created_at               DATETIME DEFAULT CURRENT_TIMESTAMP,
    assigned_at              DATETIME,
    resolved_at              DATETIME,

    escalation_severity      ENUM('LOW','MEDIUM','HIGH','CRITICAL'),
    escalation_reason        TEXT,
    escalation_reviewed      BOOLEAN DEFAULT FALSE,

    decision_moderator_email VARCHAR(255),
    decision_moderator_id    VARCHAR(50),
    decision_note            TEXT,
    decided_at               DATETIME,

    updated_at               DATETIME DEFAULT CURRENT_TIMESTAMP
                                 ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_report_cases_resource_type (resource_type),
    INDEX idx_report_cases_resource_id   (resource_id),
    INDEX idx_report_cases_target_type   (target_type),
    INDEX idx_report_cases_target_id     (target_id),
    INDEX idx_report_cases_reporter_id   (reporter_id),
    INDEX idx_report_cases_status        (status),
    INDEX idx_report_cases_assigned      (assigned_moderator_id),
    INDEX idx_report_cases_created_at    (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- =====================================================================
-- 14. REFUND REQUESTS (com.ecommerce.cnj70.document.RefundRequest)
-- =====================================================================
CREATE TABLE refunds (
    id                      VARCHAR(50) PRIMARY KEY,

    complaint_id            VARCHAR(50),
    return_id               VARCHAR(50),
    order_id                VARCHAR(50),

    customer_id             VARCHAR(50) NOT NULL,
    shop_id                 VARCHAR(50),
    vendor_id               VARCHAR(50),

    declared_amount         DECIMAL(15,2),
    settled_amount          DECIMAL(15,2),

    provider                VARCHAR(50),
    provider_transaction_id VARCHAR(255),

    status                  ENUM('REQUESTED','PROCESSING','SUCCEEDED',
                                'FAILED','CANCELLED') DEFAULT 'REQUESTED',

    provider_message        TEXT,

    requested_by_id         VARCHAR(50),
    requested_by_email      VARCHAR(255),
    requested_by_role       VARCHAR(50),

    created_at              DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at              DATETIME DEFAULT CURRENT_TIMESTAMP
                                ON UPDATE CURRENT_TIMESTAMP,
    settled_at              DATETIME,

    INDEX idx_refunds_complaint_id (complaint_id),
    INDEX idx_refunds_return_id    (return_id),
    INDEX idx_refunds_order_id     (order_id),
    INDEX idx_refunds_customer_id  (customer_id),
    INDEX idx_refunds_shop_id      (shop_id),
    INDEX idx_refunds_status       (status),
    INDEX idx_refunds_created_at   (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- =====================================================================
-- 15. RETURN REQUESTS (com.ecommerce.cnj70.document.ReturnRequest)
-- =====================================================================
CREATE TABLE returns (
    id              VARCHAR(50) PRIMARY KEY,

    complaint_id    VARCHAR(50),
    order_id        VARCHAR(50),
    order_item_ids  JSON,

    customer_id     VARCHAR(50) NOT NULL,
    customer_name   VARCHAR(255),

    shop_id         VARCHAR(50),
    shop_name       VARCHAR(255),
    vendor_id       VARCHAR(50),

    reason          TEXT,
    evidence        JSON,

    status          ENUM('REQUESTED','APPROVED','IN_TRANSIT','RECEIVED',
                        'REJECTED','COMPLETED','CANCELLED') DEFAULT 'REQUESTED',

    refund_id       VARCHAR(50),
    declared_amount DECIMAL(15,2),

    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP
                        ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_returns_complaint_id (complaint_id),
    INDEX idx_returns_order_id     (order_id),
    INDEX idx_returns_customer_id  (customer_id),
    INDEX idx_returns_shop_id      (shop_id),
    INDEX idx_returns_status       (status),
    INDEX idx_returns_created_at   (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- =====================================================================
-- 16. AUDIT LOGS (com.ecommerce.cnj70.document.AuditLog)
-- =====================================================================
CREATE TABLE audit_logs (
    id             VARCHAR(50) PRIMARY KEY,

    actor_id       VARCHAR(50),
    actor_username VARCHAR(255),
    actor_role     VARCHAR(50),

    action         VARCHAR(100),
    resource_type  VARCHAR(50),
    resource_id    VARCHAR(50),

    before_state   TEXT,
    after_state    TEXT,

    ip_address     VARCHAR(45),
    user_agent     TEXT,
    reason         TEXT,
    severity       ENUM('INFO','WARNING','CRITICAL') DEFAULT 'INFO',

    metadata       JSON,

    created_at     DATETIME DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_audit_actor_id      (actor_id),
    INDEX idx_audit_action        (action),
    INDEX idx_audit_resource_type (resource_type),
    INDEX idx_audit_resource_id   (resource_id),
    INDEX idx_audit_severity      (severity),
    INDEX idx_audit_created_at    (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- =====================================================================
-- 17. AUDIT LOG ENTRIES (com.ecommerce.cnj70.document.AuditLogEntry)
-- =====================================================================
CREATE TABLE audit_log_entries (
    id             VARCHAR(50) PRIMARY KEY,

    actor_id       VARCHAR(50),
    actor_email    VARCHAR(255),
    role           ENUM('ADMIN','MODERATOR','VENDOR','CUSTOMER'),

    action         VARCHAR(100),
    resource_type  VARCHAR(50),
    resource_id    VARCHAR(50),

    reason         TEXT,
    severity       VARCHAR(50),

    before         TEXT,
    after          TEXT,
    ip             VARCHAR(45),

    created_at     DATETIME DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_audit_entries_actor      (actor_id),
    INDEX idx_audit_entries_role       (role),
    INDEX idx_audit_entries_action     (action),
    INDEX idx_audit_entries_res_type   (resource_type),
    INDEX idx_audit_entries_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- =====================================================================
-- 18. ESCALATIONS (com.ecommerce.cnj70.document.Escalation)
-- =====================================================================
CREATE TABLE escalations (
    id                    VARCHAR(50) PRIMARY KEY,

    report_case_id        VARCHAR(50) NOT NULL,
    resource_type         ENUM('PRODUCT','REVIEW'),
    resource_id           VARCHAR(50),

    vendor_id             VARCHAR(50),
    shop_id               VARCHAR(50),

    reason                TEXT,
    severity              ENUM('LOW','MEDIUM','HIGH','CRITICAL'),

    moderator_id          VARCHAR(50),
    moderator_email       VARCHAR(255),
    moderator_role        VARCHAR(50),

    source_status         ENUM('PENDING','OPEN','IN_REVIEW','RESOLVED',
                              'APPROVED','REJECTED','ESCALATED'),

    status                ENUM('PENDING','IN_REVIEW','RESOLVED') DEFAULT 'PENDING',

    assigned_admin_id     VARCHAR(50),
    assigned_admin_email  VARCHAR(255),

    decision_admin_id     VARCHAR(50),
    decision_admin_email  VARCHAR(255),
    decision_action       ENUM('WARNING','SUSPEND','RESTORE','BAN','NO_ACTION'),
    decision_note         TEXT,
    resolved_at           DATETIME,

    created_at            DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at            DATETIME DEFAULT CURRENT_TIMESTAMP
                                ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_esc_report_case_id (report_case_id),
    INDEX idx_esc_status         (status),
    INDEX idx_esc_created_at     (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- =====================================================================
-- 19. MODERATION HISTORY (com.ecommerce.cnj70.document.ModerationHistory)
-- =====================================================================
CREATE TABLE moderation_history (
    id              VARCHAR(50) PRIMARY KEY,

    resource_type   VARCHAR(50) DEFAULT 'PRODUCT',
    resource_id     VARCHAR(50) NOT NULL,
    resource_name   VARCHAR(500),

    action          ENUM('APPROVE','REJECT','ESCALATE','RESTORE','HIDE','UNHIDE'),
    before_status   ENUM('PENDING_MANUAL','AUTO_PASSED','AUTO_REJECTED',
                         'APPROVED','REJECTED','ESCALATED','PENDING_AUTO'),
    after_status    ENUM('PENDING_MANUAL','AUTO_PASSED','AUTO_REJECTED',
                         'APPROVED','REJECTED','ESCALATED','PENDING_AUTO'),

    reason          TEXT,

    moderator_id        VARCHAR(50),
    moderator_email     VARCHAR(255),
    moderator_role      ENUM('ADMIN','MODERATOR','VENDOR','CUSTOMER'),

    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_mod_hist_resource_id (resource_id),
    INDEX idx_mod_hist_created_at  (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- =====================================================================
-- 20. LEGAL DOCUMENTS (com.ecommerce.cnj70.document.LegalDocument)
-- =====================================================================
CREATE TABLE legal_documents (
    id                  VARCHAR(50) PRIMARY KEY,

    type                ENUM('TERMS','PRIVACY','RETURN','SHIPPING',
                             'WARRANTY','COMPLAINT','PAYMENT','SITEMAP')
                        NOT NULL UNIQUE,

    title               VARCHAR(255),
    content             TEXT,
    version             INT DEFAULT 1,
    effective_date      DATETIME DEFAULT CURRENT_TIMESTAMP,

    updated_by          VARCHAR(50),

    created_at          DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME DEFAULT CURRENT_TIMESTAMP
                            ON UPDATE CURRENT_TIMESTAMP,

    rendered_content    TEXT,
    meta_description    TEXT,

    INDEX idx_legal_type (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- =====================================================================
-- 21. SCHEDULER IDEMPOTENCY (com.ecommerce.cnj70.document.SchedulerIdempotencyRecord)
-- =====================================================================
CREATE TABLE scheduler_idempotency (
    id              VARCHAR(50) PRIMARY KEY,

    operation_key   VARCHAR(500) NOT NULL UNIQUE,
    job_name        VARCHAR(255) NOT NULL,
    entity_type     VARCHAR(50),
    entity_id       VARCHAR(50),

    operation       VARCHAR(100),
    status          VARCHAR(50),
    detail          TEXT,

    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    expires_at      DATETIME,

    INDEX idx_sched_op_key    (operation_key),
    INDEX idx_sched_job_name  (job_name),
    INDEX idx_sched_entity_id (entity_id),
    INDEX idx_sched_created   (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- =====================================================================
-- END OF SCHEMA
-- Total tables: 27 (15 main + 12 sub / embedded)
-- =====================================================================
