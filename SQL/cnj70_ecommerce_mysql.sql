-- =====================================================================
-- MYSQL DATABASE EXPORT
-- Source: MongoDB Compass database cnj70_ecommerce
-- Collections: banners, carts, categories, orders, products, reviews,
--              shops, users, vouchers
-- Generated from the 9 exported Extended JSON files supplied by the user.
--
-- NOTE:
-- 1. MongoDB ObjectId values are preserved as VARCHAR(64).
-- 2. MongoDB embedded arrays are normalized into child tables.
-- 3. Some test data references IDs not present in the exported master
--    collections (e.g. shop-1 / p-1), so foreign-key constraints are
--    intentionally not enforced. All original reference values are kept.
-- 4. Money fields are DECIMAL(20,2); timestamps are stored in UTC.
-- =====================================================================

CREATE DATABASE IF NOT EXISTS cnj70_ecommerce
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE cnj70_ecommerce;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS voucher_products;
DROP TABLE IF EXISTS variant_specifications;
DROP TABLE IF EXISTS product_variants;
DROP TABLE IF EXISTS product_specifications;
DROP TABLE IF EXISTS product_images;
DROP TABLE IF EXISTS order_items;
DROP TABLE IF EXISTS cart_items;
DROP TABLE IF EXISTS vouchers;
DROP TABLE IF EXISTS reviews;
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS carts;
DROP TABLE IF EXISTS products;
DROP TABLE IF EXISTS banners;
DROP TABLE IF EXISTS categories;
DROP TABLE IF EXISTS shops;
DROP TABLE IF EXISTS users;

CREATE TABLE users (
    id VARCHAR(64) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    phone VARCHAR(50) NULL,
    address TEXT NULL,
    role VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    shop_id VARCHAR(64) NULL,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    mongo_class VARCHAR(255) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_email (email),
    KEY idx_users_shop_id (shop_id),
    KEY idx_users_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE shops (
    id VARCHAR(64) NOT NULL,
    owner_id VARCHAR(64) NULL,
    shop_name VARCHAR(255) NOT NULL,
    description TEXT NULL,
    logo_url VARCHAR(1000) NULL,
    banner_url VARCHAR(1000) NULL,
    status VARCHAR(50) NOT NULL,
    active TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    mongo_class VARCHAR(255) NULL,
    PRIMARY KEY (id),
    KEY idx_shops_owner_id (owner_id),
    KEY idx_shops_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE categories (
    id VARCHAR(64) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT NULL,
    sort_order INT NULL,
    active TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    mongo_class VARCHAR(255) NULL,
    PRIMARY KEY (id),
    KEY idx_categories_sort_order (sort_order),
    KEY idx_categories_active (active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE products (
    id VARCHAR(64) NOT NULL,
    shop_id VARCHAR(64) NULL,
    shop_name VARCHAR(255) NULL,
    name VARCHAR(1000) NOT NULL,
    brand VARCHAR(255) NULL,
    warranty_months INT NULL,
    manufacturer VARCHAR(255) NULL,
    manufacturer_address VARCHAR(1000) NULL,
    description LONGTEXT NULL,
    rich_description LONGTEXT NULL,
    price DECIMAL(20,2) NULL,
    stock INT NULL,
    category_id VARCHAR(64) NULL,
    category_name VARCHAR(255) NULL,
    thumbnail_url VARCHAR(2000) NULL,
    status VARCHAR(50) NULL,
    rating DECIMAL(10,2) NULL,
    review_count INT NULL,
    sold INT NULL,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    mongo_class VARCHAR(255) NULL,
    PRIMARY KEY (id),
    KEY idx_products_shop_id (shop_id),
    KEY idx_products_category_id (category_id),
    KEY idx_products_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE product_images (
    id BIGINT NOT NULL AUTO_INCREMENT,
    product_id VARCHAR(64) NOT NULL,
    image_url VARCHAR(2000) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_product_images_product_id (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE product_specifications (
    id BIGINT NOT NULL AUTO_INCREMENT,
    product_id VARCHAR(64) NOT NULL,
    name VARCHAR(500) NULL,
    value TEXT NULL,
    unit VARCHAR(100) NULL,
    sort_order INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_product_specs_product_id (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE product_variants (
    id BIGINT NOT NULL AUTO_INCREMENT,
    product_id VARCHAR(64) NOT NULL,
    price DECIMAL(20,2) NULL,
    stock INT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_product_variants_product_id (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE variant_specifications (
    id BIGINT NOT NULL AUTO_INCREMENT,
    variant_id BIGINT NOT NULL,
    name VARCHAR(500) NULL,
    value TEXT NULL,
    unit VARCHAR(100) NULL,
    sort_order INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_variant_specs_variant_id (variant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE carts (
    id VARCHAR(64) NOT NULL,
    user_id VARCHAR(64) NULL,
    updated_at DATETIME(3) NOT NULL,
    mongo_class VARCHAR(255) NULL,
    PRIMARY KEY (id),
    KEY idx_carts_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE cart_items (
    id BIGINT NOT NULL AUTO_INCREMENT,
    cart_id VARCHAR(64) NOT NULL,
    product_id VARCHAR(64) NULL,
    product_name VARCHAR(1000) NULL,
    image_url VARCHAR(2000) NULL,
    price DECIMAL(20,2) NULL,
    quantity INT NULL,
    subtotal DECIMAL(20,2) NULL,
    shop_id VARCHAR(64) NULL,
    shop_name VARCHAR(255) NULL,
    stock INT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_cart_items_cart_id (cart_id),
    KEY idx_cart_items_product_id (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE orders (
    id VARCHAR(64) NOT NULL,
    user_id VARCHAR(64) NULL,
    user_name VARCHAR(255) NULL,
    user_email VARCHAR(255) NULL,
    user_phone VARCHAR(50) NULL,
    shipping_address TEXT NULL,
    subtotal DECIMAL(20,2) NULL,
    shipping_fee DECIMAL(20,2) NULL,
    total_amount DECIMAL(20,2) NULL,
    status VARCHAR(50) NULL,
    payment_method VARCHAR(50) NULL,
    paid TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    delivered_at DATETIME(3) NULL,
    mongo_class VARCHAR(255) NULL,
    PRIMARY KEY (id),
    KEY idx_orders_user_id (user_id),
    KEY idx_orders_status (status),
    KEY idx_orders_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE order_items (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id VARCHAR(64) NOT NULL,
    shop_id VARCHAR(64) NULL,
    product_id VARCHAR(64) NULL,
    product_name VARCHAR(1000) NULL,
    image_url VARCHAR(2000) NULL,
    price DECIMAL(20,2) NULL,
    quantity INT NULL,
    subtotal DECIMAL(20,2) NULL,
    sort_order INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_order_items_order_id (order_id),
    KEY idx_order_items_product_id (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE reviews (
    id VARCHAR(64) NOT NULL,
    product_id VARCHAR(64) NULL,
    user_id VARCHAR(64) NULL,
    user_name VARCHAR(255) NULL,
    rating INT NULL,
    comment TEXT NULL,
    created_at DATETIME(3) NOT NULL,
    mongo_class VARCHAR(255) NULL,
    PRIMARY KEY (id),
    KEY idx_reviews_product_id (product_id),
    KEY idx_reviews_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE vouchers (
    id VARCHAR(64) NOT NULL,
    code VARCHAR(255) NOT NULL,
    name VARCHAR(500) NULL,
    type VARCHAR(50) NULL,
    shop_id VARCHAR(64) NULL,
    shop_name VARCHAR(255) NULL,
    discount_type VARCHAR(50) NULL,
    discount_value DECIMAL(20,2) NULL,
    max_discount_amount DECIMAL(20,2) NULL,
    min_order_value DECIMAL(20,2) NULL,
    quantity INT NULL,
    used INT NULL,
    start_date DATETIME(3) NULL,
    end_date DATETIME(3) NULL,
    active TINYINT(1) NOT NULL DEFAULT 0,
    created_by VARCHAR(64) NULL,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    mongo_class VARCHAR(255) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_vouchers_code (code),
    KEY idx_vouchers_shop_id (shop_id),
    KEY idx_vouchers_active (active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE voucher_products (
    id BIGINT NOT NULL AUTO_INCREMENT,
    voucher_id VARCHAR(64) NOT NULL,
    product_id VARCHAR(64) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_voucher_products_voucher_id (voucher_id),
    KEY idx_voucher_products_product_id (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE banners (
    id VARCHAR(64) NOT NULL,
    title VARCHAR(500) NULL,
    description TEXT NULL,
    tag VARCHAR(255) NULL,
    tag_icon VARCHAR(255) NULL,
    image_url VARCHAR(2000) NULL,
    link VARCHAR(2000) NULL,
    cta_text VARCHAR(500) NULL,
    cta_icon VARCHAR(255) NULL,
    theme VARCHAR(100) NULL,
    status VARCHAR(50) NULL,
    position VARCHAR(100) NULL,
    sort_order INT NULL,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    mongo_class VARCHAR(255) NULL,
    PRIMARY KEY (id),
    KEY idx_banners_status (status),
    KEY idx_banners_position (position)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- =====================================================================
-- DATA: users
-- =====================================================================
INSERT INTO users (id,email,password,full_name,phone,address,role,status,shop_id,created_at,updated_at,mongo_class) VALUES ('6a859691deda531cb1dd5276','quantran66778899@gmail.com','$2a$10$1SYejm3jdcll3ZSqczXbw./mihXGrFRXgYBELuZppJKpMYbskXO5O','Trần Mạnh Quân','0559605251','Hà Nội','VENDOR','ACTIVE','6a8ff2fad0f106679ac9391d','2026-08-19 11:42:09.091','2026-09-08 05:17:53.352','com.ecommerce.cnj70.document.User');
INSERT INTO users (id,email,password,full_name,phone,address,role,status,shop_id,created_at,updated_at,mongo_class) VALUES ('6a86ac30add146680e49cf63','vendor@test.com','$2a$10$H8WiY8oKdiibkZy2NIyPk.uWgRp3uCRXbyR.A15oTluzKCvYggUr2','Test Vendor Shop','0123456789','123 Test Street, HCM City','VENDOR','ACTIVE','6a86ac30add146680e49cf64','2026-08-20 07:26:40.294','2026-08-27 04:07:41.893','com.ecommerce.cnj70.document.User');
INSERT INTO users (id,email,password,full_name,phone,address,role,status,shop_id,created_at,updated_at,mongo_class) VALUES ('6a8bd006779b6112504cedfe','Anhquanql4.0@gmail.com','$2a$10$c5jNoHTkKwvExRQ4aHphruskcU46adChYTXMdg5CIN00fsdt5ws2O','Trần Anh Quân','0386267692','Quỳnh Lưu Nghệ An','CUSTOMER','ACTIVE',NULL,'2026-08-24 05:00:54.138','2026-08-26 16:49:58.937','com.ecommerce.cnj70.document.User');
INSERT INTO users (id,email,password,full_name,phone,address,role,status,shop_id,created_at,updated_at,mongo_class) VALUES ('6a8d245de036f2355e7be4fb','tuanvils540@gmail.com','$2a$10$HsBjOzaMpHzNl7JmXajL0u600BK0/jtQJCkapYYhtifhgFPls5eri','Vi Anh Tuấn','0365856259','test','CUSTOMER','ACTIVE',NULL,'2026-08-25 05:13:01.089','2026-08-25 05:13:01.089','com.ecommerce.cnj70.document.User');
INSERT INTO users (id,email,password,full_name,phone,address,role,status,shop_id,created_at,updated_at,mongo_class) VALUES ('6a8e9389f003d8236cd8267b','admin2@gmail.com','$2a$10$vcfMVARYnlYdAAWaqNY/U.jjLMl1MCQGlhMQ8DnH.1D7D5eagmzbu','admin','25032005','hihi','ADMIN','ACTIVE',NULL,'2026-08-26 07:19:37.916','2026-09-07 20:51:55.750','com.ecommerce.cnj70.document.User');
INSERT INTO users (id,email,password,full_name,phone,address,role,status,shop_id,created_at,updated_at,mongo_class) VALUES ('6a8ea0db78552e05a398e7f1','hihi@gmail.com','$2a$10$JZrfsgfJPpCRDzmUlulkxeJdcoanrau2HAALD8cqOavd99Yvk2bKC','hihi','25032005','hihi','CUSTOMER','ACTIVE',NULL,'2026-08-26 08:16:27.334','2026-08-26 15:51:31.750','com.ecommerce.cnj70.document.User');
INSERT INTO users (id,email,password,full_name,phone,address,role,status,shop_id,created_at,updated_at,mongo_class) VALUES ('6a8edee5f9bdef068537043f','testcust001@test.com','$2a$10$BAjvmWEgh6NnURAER9rx.uEbvt96N6CHpVqNjx4i/.3jc9z1m8UJC','Test Customer',NULL,NULL,'CUSTOMER','ACTIVE',NULL,'2026-08-26 12:41:09.767','2026-08-26 12:41:09.767','com.ecommerce.cnj70.document.User');
INSERT INTO users (id,email,password,full_name,phone,address,role,status,shop_id,created_at,updated_at,mongo_class) VALUES ('6a8edf2af9bdef0685370440','cust_new01@test.com','$2a$10$gzYypkwOb.XvuTnEsi639ebBG/egxNV5dnSeGhN510r2SBqm79YwK','New Customer',NULL,NULL,'CUSTOMER','ACTIVE',NULL,'2026-08-26 12:42:18.596','2026-09-08 05:17:49.591','com.ecommerce.cnj70.document.User');
INSERT INTO users (id,email,password,full_name,phone,address,role,status,shop_id,created_at,updated_at,mongo_class) VALUES ('6a8f904521c56e0a8987ec18','cust_new02@test.com','$2a$10$UVetfVvXSzRLiquXPFhyIee/1IazeOyBzdJCSAO1CSmtZWURdUzE2','customer2','031659989','Hà Nội','CUSTOMER','ACTIVE',NULL,'2026-08-27 01:17:57.986','2026-08-27 01:17:57.986','com.ecommerce.cnj70.document.User');
INSERT INTO users (id,email,password,full_name,phone,address,role,status,shop_id,created_at,updated_at,mongo_class) VALUES ('6a8fe9192a96511b3177872e','testvendor1787816216@example.com','$2a$10$iW1vi1ALIo72NznJraSuD.YDBwScGAb26wmh/Sb11qFK4R0ElC8na','Test Vendor','0901234567',NULL,'CUSTOMER','ACTIVE',NULL,'2026-08-27 07:36:57.242','2026-08-27 07:36:57.242','com.ecommerce.cnj70.document.User');
INSERT INTO users (id,email,password,full_name,phone,address,role,status,shop_id,created_at,updated_at,mongo_class) VALUES ('6a8fe9a42a96511b3177872f','customer1787816356@example.com','$2a$10$/gcN9hTpXmzRPu5ylRFJ6u2vvqYZ65EkdpaMJDH5uJNfMhg3N2ZLW','Test Customer',NULL,NULL,'CUSTOMER','ACTIVE',NULL,'2026-08-27 07:39:16.911','2026-08-27 07:39:16.911','com.ecommerce.cnj70.document.User');
INSERT INTO users (id,email,password,full_name,phone,address,role,status,shop_id,created_at,updated_at,mongo_class) VALUES ('6a8ff04a5b6d1620f21f78ca','vendor_phase5_1787818057@test.com','$2a$10$5./WjJ8bod7PwvnmfiPD3eRJjVDNKuIWiY98tX4v9LBAOv0Oa.lS.','Vendor Phase5','0909999999',NULL,'VENDOR','ACTIVE',NULL,'2026-08-27 08:07:38.395','2026-08-27 08:07:38.395','com.ecommerce.cnj70.document.User');
INSERT INTO users (id,email,password,full_name,phone,address,role,status,shop_id,created_at,updated_at,mongo_class) VALUES ('6a8ff1adeead2e70541cc7cf','vendor_final_1787818412@test.com','$2a$10$m46TeDCZCitRwoi2BTwdW.DQhEj8RtKAhk382JtqOsz6M.qykLQ/u','Vendor Final','0901787818412',NULL,'VENDOR','ACTIVE',NULL,'2026-08-27 08:13:33.383','2026-08-27 08:13:33.383','com.ecommerce.cnj70.document.User');
INSERT INTO users (id,email,password,full_name,phone,address,role,status,shop_id,created_at,updated_at,mongo_class) VALUES ('6a8ff226eead2e70541cc7d0','vendor_test_1787819000@test.com','$2a$10$8QL3gZ5DkblLJXy5crhrn.PK5hQUeeLlrKMG.nHoK/sno2dIQn2ti','Vendor Test','0909999999',NULL,'VENDOR','ACTIVE',NULL,'2026-08-27 08:15:34.793','2026-08-27 08:15:34.793','com.ecommerce.cnj70.document.User');
INSERT INTO users (id,email,password,full_name,phone,address,role,status,shop_id,created_at,updated_at,mongo_class) VALUES ('6a8ff229eead2e70541cc7d1','cust_1787819000@test.com','$2a$10$wReuLNCvGJEoPPp8J.8Am.oVObSSjEC6pwz6iO8jmWOvtejUYvSnO','Vendor Test','0909999999',NULL,'CUSTOMER','ACTIVE',NULL,'2026-08-27 08:15:37.085','2026-08-27 08:15:37.085','com.ecommerce.cnj70.document.User');
INSERT INTO users (id,email,password,full_name,phone,address,role,status,shop_id,created_at,updated_at,mongo_class) VALUES ('6a8ff263eead2e70541cc7d2','vendor_direct@test.com','$2a$10$rXwlHkodqZkeGlf3oo3DEeyEzOATMR61/0sb5URv08wEqfJkio7Nq','Vendor Direct','0909999999',NULL,'VENDOR','ACTIVE',NULL,'2026-08-27 08:16:35.811','2026-08-27 08:16:35.811','com.ecommerce.cnj70.document.User');
INSERT INTO users (id,email,password,full_name,phone,address,role,status,shop_id,created_at,updated_at,mongo_class) VALUES ('6a8ff34ceead2e70541cc7d3','vendor_phase5_test_1787818828@test.com','$2a$10$oy14A97M7tekrLq44mdk1.5qOZ/tZGSDrJt.1DCTUaKrpMyrSjsCK','Vendor Phase5 Test','0901787818828',NULL,'VENDOR','ACTIVE',NULL,'2026-08-27 08:20:28.688','2026-08-27 08:21:29.498','com.ecommerce.cnj70.document.User');
INSERT INTO users (id,email,password,full_name,phone,address,role,status,shop_id,created_at,updated_at,mongo_class) VALUES ('6a8ff42beead2e70541cc7d5','vendor_ph5_1787819051@test.com','$2a$10$eZ7BHe2qsl.ZxW5uugLB.e333pwoE9ATsrqMA5uiMBkzvgKmJQJxW','Vendor Phase5','0901787819051',NULL,'VENDOR','ACTIVE',NULL,'2026-08-27 08:24:11.938','2026-08-27 08:24:13.131','com.ecommerce.cnj70.document.User');
INSERT INTO users (id,email,password,full_name,phone,address,role,status,shop_id,created_at,updated_at,mongo_class) VALUES ('6a8ff775d619e83eaaff0c98','shop1@gmail.com','$2a$10$33SfDTgg7.MveMpORXrirO/Ly/h5rrkfXFS5tb1cQfbzGBt4lV95m','Shop 1','0312361648','Hà Nội','VENDOR','ACTIVE','6a8ff7f3d619e83eaaff0c9a','2026-08-27 08:38:13.454','2026-08-27 08:40:19.146','com.ecommerce.cnj70.document.User');
INSERT INTO users (id,email,password,full_name,phone,address,role,status,shop_id,created_at,updated_at,mongo_class) VALUES ('6a94682a10e0c55de9443c39','vendor.test1@gmail.com','$2a$10$i1SfBV5bXt.KC26NcdLzQ.gu1UN0kUQnzboBwy5S7vP.VhrSeRaIi','Test Vendor','0909090909',NULL,'VENDOR','ACTIVE',NULL,'2026-08-30 17:28:10.793','2026-08-30 17:28:10.793','com.ecommerce.cnj70.document.User');
INSERT INTO users (id,email,password,full_name,phone,address,role,status,shop_id,created_at,updated_at,mongo_class) VALUES ('6a95d7f7332e89711a732f4c','tuanvi4342@gmail.com','$2a$10$DiGk833Qn6gCSbTA.XEDweotEdyGPHHmX.fCghc/V8jJum05eJIH6','TEST','0365856259','test','CUSTOMER','ACTIVE',NULL,'2026-08-31 19:37:27.823','2026-08-31 19:37:27.823','com.ecommerce.cnj70.document.User');
INSERT INTO users (id,email,password,full_name,phone,address,role,status,shop_id,created_at,updated_at,mongo_class) VALUES ('6a9b98a47c0a9742185fd425','admin@cnj70.local','$2a$10$H.dby2Z8wknuozzhPie/n.PLeFy6RqSogeAeSdtbgNo1Q7mdC2eli','Phase10 Admin',NULL,NULL,'ADMIN','ACTIVE',NULL,'2026-09-05 04:20:52.927','2026-09-05 04:20:52.927','com.ecommerce.cnj70.document.User');
INSERT INTO users (id,email,password,full_name,phone,address,role,status,shop_id,created_at,updated_at,mongo_class) VALUES ('6a9b9a34289de826a22aa6c2','vendor@phase10.local','$2a$10$Z8fa0aaInbf6argdcK0/1eS.7rXDcqc1HZ0UHJHswX4N1wRgOT1Ea','Phase10 Vendor',NULL,NULL,'VENDOR','ACTIVE',NULL,'2026-09-05 04:27:32.578','2026-09-05 04:27:32.578','com.ecommerce.cnj70.document.User');
INSERT INTO users (id,email,password,full_name,phone,address,role,status,shop_id,created_at,updated_at,mongo_class) VALUES ('6a9b9a34289de826a22aa6c3','customer@phase10.local','$2a$10$EnvGzBTmDiRfLDgu8Z.CD.vPyl5EWuxwxncKiKRjzldTL1GtnIkja','Phase10 Customer',NULL,NULL,'CUSTOMER','ACTIVE',NULL,'2026-09-05 04:27:32.783','2026-09-05 04:27:32.783','com.ecommerce.cnj70.document.User');
INSERT INTO users (id,email,password,full_name,phone,address,role,status,shop_id,created_at,updated_at,mongo_class) VALUES ('6a9f0f353660e924ace56b55','phase11test@gmail.com','$2a$10$ED6EpQ7WeU7areA9tohMCuXeCZy.ZRimnIglpt0I5T/XYVC50tgf6','Test User','0123456789',NULL,'CUSTOMER','ACTIVE',NULL,'2026-09-07 19:23:33.597','2026-09-07 19:23:33.597','com.ecommerce.cnj70.document.User');
INSERT INTO users (id,email,password,full_name,phone,address,role,status,shop_id,created_at,updated_at,mongo_class) VALUES ('6a9f243e29ce9e19344a7498','vendor.test@gmail.com','$2a$10$e0tfYOmIZBmfumFVcVRWeenCN2C8enKJBn47NANtUxQxmCN..6Vuy','Vendor Test',NULL,NULL,'VENDOR','ACTIVE',NULL,'2026-09-07 20:53:18.640','2026-09-07 20:53:18.640','com.ecommerce.cnj70.document.User');
INSERT INTO users (id,email,password,full_name,phone,address,role,status,shop_id,created_at,updated_at,mongo_class) VALUES ('6a9f299d5563aa340eaf10ad','customer.test@gmail.com','$2a$10$JXfCj8prAQTzITLFDZHQkenNOJjmbyuD3mBkOIpdB0TG6Dc2fCBk.','Customer Test','0901234567',NULL,'CUSTOMER','ACTIVE',NULL,'2026-09-07 21:16:13.146','2026-09-07 21:16:13.146','com.ecommerce.cnj70.document.User');

-- DATA: shops
INSERT INTO shops (id,owner_id,shop_name,description,logo_url,banner_url,status,active,created_at,updated_at,mongo_class) VALUES ('6a86ac30add146680e49cf64',NULL,'Test 1 Vendor Shop','Shop for testing purpose','/uploads/22fe8f25cd2f42e5818864dce7598bf5.jpg','/uploads/9b40b82632af45aebda72d7be160f505.jpg','APPROVED',1,'2026-08-20 07:26:40.369','2026-09-08 06:07:49.693','com.ecommerce.cnj70.document.Shop');
INSERT INTO shops (id,owner_id,shop_name,description,logo_url,banner_url,status,active,created_at,updated_at,mongo_class) VALUES ('6a8ff7f3d619e83eaaff0c9a','6a8ff775d619e83eaaff0c98','Cửa hàng laptop','Bán laptop','/uploads/3f3f96180a6840dda1c2d11230a14dc9.webp','/uploads/7067798e0c144909823c8e6788bcaf79.webp','APPROVED',1,'2026-08-27 08:40:19.084','2026-08-28 07:26:57.834','com.ecommerce.cnj70.document.Shop');
INSERT INTO shops (id,owner_id,shop_name,description,logo_url,banner_url,status,active,created_at,updated_at,mongo_class) VALUES ('6a9b9a34289de826a22aa6c4','6a9b9a34289de826a22aa6c2','Phase10 Pending Shop',NULL,NULL,NULL,'PENDING',1,'2026-09-05 04:27:32.955','2026-09-05 04:27:32.955','com.ecommerce.cnj70.document.Shop');
INSERT INTO shops (id,owner_id,shop_name,description,logo_url,banner_url,status,active,created_at,updated_at,mongo_class) VALUES ('6a9b9a35289de826a22aa6c5','6a9b9a34289de826a22aa6c2','Phase10 Approved Shop',NULL,NULL,NULL,'APPROVED',1,'2026-09-05 04:27:33.071','2026-09-05 04:27:33.071','com.ecommerce.cnj70.document.Shop');
INSERT INTO shops (id,owner_id,shop_name,description,logo_url,banner_url,status,active,created_at,updated_at,mongo_class) VALUES ('6a9b9a35289de826a22aa6c6','6a9b9a34289de826a22aa6c2','Phase10 Rejected Shop',NULL,NULL,NULL,'REJECTED',0,'2026-09-05 04:27:33.177','2026-09-05 04:27:33.177','com.ecommerce.cnj70.document.Shop');
INSERT INTO shops (id,owner_id,shop_name,description,logo_url,banner_url,status,active,created_at,updated_at,mongo_class) VALUES ('6a9b9a35289de826a22aa6c7','6a9b9a34289de826a22aa6c2','Another Approved Store',NULL,NULL,NULL,'APPROVED',1,'2026-09-05 04:27:33.285','2026-09-05 04:27:33.285','com.ecommerce.cnj70.document.Shop');

-- DATA: categories
INSERT INTO categories (id,name,description,sort_order,active,created_at,updated_at,mongo_class) VALUES ('6a8fe365f3767c30080d2cec','Điện thoại','Điện thoại thông minh và điện thoại di động',3,1,'2026-08-27 07:12:37.497','2026-08-27 07:12:37.497','com.ecommerce.cnj70.document.Category');
INSERT INTO categories (id,name,description,sort_order,active,created_at,updated_at,mongo_class) VALUES ('6a8fe365f3767c30080d2ced','Máy tính bảng','Tablet phục vụ học tập, giải trí và công việc',4,1,'2026-08-27 07:12:37.497','2026-08-27 07:12:37.497','com.ecommerce.cnj70.document.Category');
INSERT INTO categories (id,name,description,sort_order,active,created_at,updated_at,mongo_class) VALUES ('6a8fe365f3767c30080d2cf3','Đồng hồ thông minh','Smartwatch và thiết bị đeo thông minh',10,1,'2026-08-27 07:12:37.497','2026-08-27 07:12:37.497','com.ecommerce.cnj70.document.Category');
INSERT INTO categories (id,name,description,sort_order,active,created_at,updated_at,mongo_class) VALUES ('6a8fe365f3767c30080d2cee','Linh kiện máy tính','CPU, RAM, GPU, Mainboard, SSD và linh kiện PC',5,1,'2026-08-27 07:12:37.497','2026-08-27 07:12:37.497','com.ecommerce.cnj70.document.Category');
INSERT INTO categories (id,name,description,sort_order,active,created_at,updated_at,mongo_class) VALUES ('6a8fe365f3767c30080d2cef','Màn hình','Màn hình máy tính, gaming và văn phòng',6,1,'2026-08-27 07:12:37.497','2026-08-27 07:12:37.497','com.ecommerce.cnj70.document.Category');
INSERT INTO categories (id,name,description,sort_order,active,created_at,updated_at,mongo_class) VALUES ('6a8fe365f3767c30080d2ceb','Laptop','Laptop gaming, văn phòng và học tập',2,1,'2026-08-27 07:12:37.497','2026-08-27 07:12:37.497','com.ecommerce.cnj70.document.Category');
INSERT INTO categories (id,name,description,sort_order,active,created_at,updated_at,mongo_class) VALUES ('6a8fe365f3767c30080d2cf0','Thiết bị âm thanh','Tai nghe, loa và các thiết bị âm thanh',7,1,'2026-08-27 07:12:37.497','2026-08-27 07:12:37.497','com.ecommerce.cnj70.document.Category');
INSERT INTO categories (id,name,description,sort_order,active,created_at,updated_at,mongo_class) VALUES ('6a8fe365f3767c30080d2cea','PC & Máy tính bàn','Máy tính để bàn, PC gaming và PC văn phòng',1,1,'2026-08-27 07:12:37.497','2026-08-27 07:12:37.497','com.ecommerce.cnj70.document.Category');
INSERT INTO categories (id,name,description,sort_order,active,created_at,updated_at,mongo_class) VALUES ('6a8fe365f3767c30080d2cf1','Phụ kiện công nghệ','Chuột, bàn phím, webcam, sạc và phụ kiện điện tử',8,1,'2026-08-27 07:12:37.497','2026-08-27 07:12:37.497','com.ecommerce.cnj70.document.Category');
INSERT INTO categories (id,name,description,sort_order,active,created_at,updated_at,mongo_class) VALUES ('6a8fe365f3767c30080d2cf2','Thiết bị mạng','Router, modem, switch và thiết bị mạng',9,1,'2026-08-27 07:12:37.497','2026-08-27 07:12:37.497','com.ecommerce.cnj70.document.Category');

-- DATA: products
INSERT INTO products (id,shop_id,shop_name,name,brand,warranty_months,manufacturer,manufacturer_address,description,rich_description,price,stock,category_id,category_name,thumbnail_url,status,rating,review_count,sold,created_at,updated_at,mongo_class) VALUES ('6a86e99da672993ccc5261fa','6a86ac30add146680e49cf64','Test Vendor Shop','Laptop Lenovo Legion Y7000X 2026 | Core 7 245HX, 16GB, 512GB, RTX 5060 8GB, 15.3 icnh WQXGA OLED 165Hz, Win 11','Sam sung',12,'sam sung','hanoi','<h2>Laptop Lenovo Legion 5i 2026 &mdash; Core 7 245HX, 16GB, RTX 5060 8GB, 15.3 inch WQXGA OLED 165Hz</h2>
<p><strong>Laptop Lenovo Legion 5i 2026</strong>&nbsp;l&agrave; chiếc laptop gaming tầm trung nơi&nbsp;<strong>Intel Core 7 245HX 14 nh&acirc;n 14 luồng</strong>&nbsp;gặp gỡ&nbsp;<strong>NVIDIA GeForce RTX 5060 8GB GDDR7</strong>&nbsp;tr&ecirc;n m&agrave;n h&igrave;nh&nbsp;<strong>15.3 icnh WQXGA OLED 165Hz DCI-P3 100%</strong>. Với&nbsp;<strong>16GB DDR5</strong>, SSD&nbsp;<strong>512GB NVMe</strong>, pin&nbsp;<strong>80Wh</strong>,&nbsp;<strong>Wi-Fi 6E</strong>,&nbsp;<strong>Windows 11 Home</strong>&nbsp;v&agrave; thiết kế&nbsp;<strong>kim loại mặt A</strong>&nbsp;nặng&nbsp;<strong>1.95kg</strong>&nbsp;d&agrave;y&nbsp;<strong>18.9mm</strong>, đ&acirc;y l&agrave; lựa chọn gaming di động &mdash; m&agrave;n h&igrave;nh OLED đỉnh cao, hiệu năng mạnh mẽ, thiết kế mỏng nhẹ, b&agrave;n ph&iacute;m RGB.</p>
<h3>Thiết Kế: Kim Loại &mdash; Gaming Mỏng Nhẹ</h3>
<p>Lenovo Legion 5i 2026 sở hữu thiết kế gaming tinh tế với mặt A bằng kim loại cao cấp, tạo cảm gi&aacute;c chắc chắn v&agrave; sang trọng. M&aacute;y c&oacute; độ d&agrave;y chỉ 18.9mm v&agrave; trọng lượng 1.95kg, kh&aacute; ấn tượng cho một chiếc laptop gaming hiệu năng cao 15.3 icnh. B&agrave;n ph&iacute;m RGB backlit với h&agrave;nh tr&igrave;nh ph&iacute;m thoải m&aacute;i, cho ph&eacute;p t&ugrave;y chỉnh hiệu ứng &aacute;nh s&aacute;ng theo phong c&aacute;ch c&aacute; nh&acirc;n. Touchpad rộng r&atilde;i đ&aacute;p ứng tốt thao t&aacute;c h&agrave;ng ng&agrave;y.</p>
<h3>M&agrave;n H&igrave;nh: 15.3 icnh OLED 165Hz &mdash; WQXGA DCI-P3 100%</h3>
<ul>
<li>K&iacute;ch thước: 15.3 icnh, tỷ lệ 16:10</li>
<li>Tấm nền: OLED, độ ph&acirc;n giải WQXGA (2560x1600), 10-bit</li>
<li>Tần số qu&eacute;t: 165Hz, thời gian phản hồi 3ms</li>
<li>Độ phủ m&agrave;u: 100% DCI-P3, m&agrave;u sắc rực rỡ v&agrave; ch&iacute;nh x&aacute;c tuyệt đối</li>
<li>Độ s&aacute;ng tối đa: 1100 nits, chứng nhận HDR 400, Dolby Vision</li>
<li>C&ocirc;ng nghệ: AMD FreeSync, G-Sync, DC Dimmer &mdash; kh&ocirc;ng nhấp nh&aacute;y, bảo vệ mắt</li>
</ul>
<h3>Hiệu Năng: Core 7 245HX + RTX 5060 8GB</h3>
<p><strong>CPU &mdash; Intel Core 7 245HX:</strong>&nbsp;14 nh&acirc;n 14 luồng, kiến tr&uacute;c Arrow Lake với hiệu năng đa nh&acirc;n vượt trội. D&ograve;ng HX mang đến sức mạnh tương đương desktop, đ&aacute;p ứng mọi t&aacute;c vụ gaming v&agrave; s&aacute;ng tạo nội dung nặng.</p>
<p><strong>GPU &mdash; NVIDIA GeForce RTX 5060 8GB GDDR7:</strong>&nbsp;Card đồ họa rời thế hệ mới với 8GB VRAM GDDR7, kiến tr&uacute;c Blackwell, hỗ trợ ray tracing v&agrave; DLSS 4. Đ&aacute;p ứng tốt mọi tựa game AAA ở độ ph&acirc;n giải WQXGA với thiết lập cao, mang đến trải nghiệm gaming mượt m&agrave; tr&ecirc;n m&agrave;n h&igrave;nh 165Hz.</p>
<h3>RAM &amp; Lưu Trữ: 16GB DDR5 + 512GB SSD</h3>
<p>M&aacute;y được trang bị&nbsp;<strong>16GB DDR5 6400MHz</strong>, c&oacute; thể n&acirc;ng cấp để đ&aacute;p ứng nhu cầu sử dụng ng&agrave;y c&agrave;ng cao. Tốc độ RAM 6400MHz cho băng th&ocirc;ng lớn, hỗ trợ tốt c&aacute;c t&aacute;c vụ gaming v&agrave; đa nhiệm.</p>
<p>Ổ cứng&nbsp;<strong>512GB PCIe NVMe M.2 SSD Gen 4</strong>&nbsp;cho tốc độ đọc ghi vượt trội, khởi động m&aacute;y v&agrave; load game nhanh ch&oacute;ng. Khe cắm M.2 hỗ trợ n&acirc;ng cấp khi cần th&ecirc;m dung lượng.</p>
<h3>Pin &amp; Sạc: 80Wh</h3>
<p>Vi&ecirc;n pin&nbsp;<strong>Li-ion 80Wh</strong>&nbsp;cho thời lượng sử dụng tốt trong ph&acirc;n kh&uacute;c laptop gaming hiệu năng cao. Bộ sạc đi k&egrave;m nạp đầy năng lượng nhanh ch&oacute;ng, sẵn s&agrave;ng cho mọi phi&ecirc;n chơi game d&agrave;i.</p>
<h3>Cổng Kết Nối</h3>
<p><strong>Kết nối c&oacute; d&acirc;y:</strong></p>
<ul>
<li>1 x USB-C 10Gbps hỗ trợ DP 2.1 v&agrave; PD 3.0</li>
<li>1 x USB-C 10Gbps hỗ trợ DP 1.4</li>
<li>2 x USB-A 3.2 5Gbps</li>
<li>1 x USB-A 3.2 10Gbps hỗ trợ Output 5V/2A</li>
<li>1 x HDMI 2.1</li>
<li>1 x RJ-45 (LAN)</li>
<li>1 x 3.5mm Audio Jack combo</li>
<li>1 x Power input</li>
</ul>
<p>Kh&ocirc;ng d&acirc;y:&nbsp;<strong>Wi-Fi 6E + Bluetooth 5.4</strong>&nbsp;&mdash; kết nối ổn định, tốc độ cao, độ trễ thấp.</p>
<h3>&Acirc;m Thanh &amp; Webcam</h3>
<p>Webcam&nbsp;<strong>HD</strong>&nbsp;t&iacute;ch hợp đ&aacute;p ứng nhu cầu hội họp trực tuyến cơ bản. Hệ thống &acirc;m thanh được tối ưu cho trải nghiệm gaming v&agrave; giải tr&iacute; h&agrave;ng ng&agrave;y.</p>
<h3>Phần Mềm: Windows 11 Home</h3>
<p>M&aacute;y được c&agrave;i đặt sẵn&nbsp;<strong>Windows 11 Home</strong>&nbsp;bản quyền, sẵn s&agrave;ng sử dụng ngay khi mở m&aacute;y. Giao diện trực quan c&ugrave;ng c&aacute;c t&iacute;nh năng gaming v&agrave; bảo mật được tối ưu h&oacute;a.</p>
<h3>Ai N&ecirc;n Mua</h3>
<ul>
<li>Game thủ cần m&agrave;n h&igrave;nh OLED 165Hz với m&agrave;u sắc DCI-P3 100% sống động v&agrave; RTX 5060 đủ sức chiến game</li>
<li>Người d&ugrave;ng y&ecirc;u th&iacute;ch laptop gaming mỏng nhẹ 1.95kg nhưng vẫn giữ hiệu năng cao</li>
<li>Người d&ugrave;ng cần m&aacute;y vừa gaming vừa s&aacute;ng tạo nội dung với m&agrave;n h&igrave;nh chuẩn m&agrave;u v&agrave; CPU Core 7 mạnh mẽ</li>
</ul>
<h3>Tổng Kết</h3>
<p>Lenovo Legion 5i 2026 l&agrave; chiếc laptop gaming tầm trung ấn tượng với CPU Intel Core 7 245HX 14 nh&acirc;n, GPU RTX 5060 8GB v&agrave; m&agrave;n h&igrave;nh 15.3 icnh WQXGA OLED 165Hz DCI-P3 100%. Thiết kế kim loại mặt A cao cấp, trọng lượng chỉ 1.95kg, pin 80Wh c&ugrave;ng b&agrave;n ph&iacute;m RGB &mdash; sự kết hợp ho&agrave;n hảo giữa hiệu năng v&agrave; t&iacute;nh di động trong ph&acirc;n kh&uacute;c gaming.</p>
<p>Li&ecirc;n hệ ngay với&nbsp;<strong>LaptopAZ</strong>&nbsp;để sở hữu Lenovo Legion 5i 2026 với mức gi&aacute; tốt nhất v&agrave; dịch vụ hậu m&atilde;i uy t&iacute;n!</p>
<p><em>Lưu &yacute;: B&agrave;i viết v&agrave; h&igrave;nh ảnh chỉ c&oacute; t&iacute;nh chất tham khảo v&igrave; cấu h&igrave;nh v&agrave; đặc t&iacute;nh sản phẩm c&oacute; thể thay đổi theo thị trường v&agrave; từng phi&ecirc;n bản. Qu&yacute; kh&aacute;ch cần cấu h&igrave;nh + h&igrave;nh ảnh cụ thể vui l&ograve;ng li&ecirc;n hệ với c&aacute;c tư vấn vi&ecirc;n để được trợ gi&uacute;p.</em></p>
<p><img src="https://laptopaz.vn/media/lib/3034_anhkh4anh.jpg" alt="Laptop Lenovo Legion 5i 2026"></p>
<p><strong>Địa chỉ mua b&aacute;n Laptop uy t&iacute;n tại H&agrave; Nội v&agrave; tr&ecirc;n to&agrave;n quốc - LaptopAZ.vn</strong><br>Cơ sở 1: Số 18 Ng&otilde; 121 - Th&aacute;i H&agrave; - Đống Đa - H&agrave; Nội<br>Cơ sở 2: Số 56 Trần Ph&uacute; - H&agrave; Đ&ocirc;ng - H&agrave; Nội<br>Li&ecirc;n hệ ngay: Hotline 0825.233.233<br><a href="https://laptopaz.vn/huong-dan-mua-hang-tu-xa-mobile.html">Chuyển h&agrave;ng đi to&agrave;n quốc nhận m&aacute;y đ&uacute;ng như m&ocirc; tả mới phải thanh to&aacute;n xem chi tiết tại đ&acirc;y</a></p>','<h2>Laptop Lenovo Legion 5i 2026 &mdash; Core 7 245HX, 16GB, RTX 5060 8GB, 15.3 inch WQXGA OLED 165Hz</h2>
<p><strong>Laptop Lenovo Legion 5i 2026</strong>&nbsp;l&agrave; chiếc laptop gaming tầm trung nơi&nbsp;<strong>Intel Core 7 245HX 14 nh&acirc;n 14 luồng</strong>&nbsp;gặp gỡ&nbsp;<strong>NVIDIA GeForce RTX 5060 8GB GDDR7</strong>&nbsp;tr&ecirc;n m&agrave;n h&igrave;nh&nbsp;<strong>15.3 icnh WQXGA OLED 165Hz DCI-P3 100%</strong>. Với&nbsp;<strong>16GB DDR5</strong>, SSD&nbsp;<strong>512GB NVMe</strong>, pin&nbsp;<strong>80Wh</strong>,&nbsp;<strong>Wi-Fi 6E</strong>,&nbsp;<strong>Windows 11 Home</strong>&nbsp;v&agrave; thiết kế&nbsp;<strong>kim loại mặt A</strong>&nbsp;nặng&nbsp;<strong>1.95kg</strong>&nbsp;d&agrave;y&nbsp;<strong>18.9mm</strong>, đ&acirc;y l&agrave; lựa chọn gaming di động &mdash; m&agrave;n h&igrave;nh OLED đỉnh cao, hiệu năng mạnh mẽ, thiết kế mỏng nhẹ, b&agrave;n ph&iacute;m RGB.</p>
<h3>Thiết Kế: Kim Loại &mdash; Gaming Mỏng Nhẹ</h3>
<p>Lenovo Legion 5i 2026 sở hữu thiết kế gaming tinh tế với mặt A bằng kim loại cao cấp, tạo cảm gi&aacute;c chắc chắn v&agrave; sang trọng. M&aacute;y c&oacute; độ d&agrave;y chỉ 18.9mm v&agrave; trọng lượng 1.95kg, kh&aacute; ấn tượng cho một chiếc laptop gaming hiệu năng cao 15.3 icnh. B&agrave;n ph&iacute;m RGB backlit với h&agrave;nh tr&igrave;nh ph&iacute;m thoải m&aacute;i, cho ph&eacute;p t&ugrave;y chỉnh hiệu ứng &aacute;nh s&aacute;ng theo phong c&aacute;ch c&aacute; nh&acirc;n. Touchpad rộng r&atilde;i đ&aacute;p ứng tốt thao t&aacute;c h&agrave;ng ng&agrave;y.</p>
<h3>M&agrave;n H&igrave;nh: 15.3 icnh OLED 165Hz &mdash; WQXGA DCI-P3 100%</h3>
<ul>
<li>K&iacute;ch thước: 15.3 icnh, tỷ lệ 16:10</li>
<li>Tấm nền: OLED, độ ph&acirc;n giải WQXGA (2560x1600), 10-bit</li>
<li>Tần số qu&eacute;t: 165Hz, thời gian phản hồi 3ms</li>
<li>Độ phủ m&agrave;u: 100% DCI-P3, m&agrave;u sắc rực rỡ v&agrave; ch&iacute;nh x&aacute;c tuyệt đối</li>
<li>Độ s&aacute;ng tối đa: 1100 nits, chứng nhận HDR 400, Dolby Vision</li>
<li>C&ocirc;ng nghệ: AMD FreeSync, G-Sync, DC Dimmer &mdash; kh&ocirc;ng nhấp nh&aacute;y, bảo vệ mắt</li>
</ul>
<h3>Hiệu Năng: Core 7 245HX + RTX 5060 8GB</h3>
<p><strong>CPU &mdash; Intel Core 7 245HX:</strong>&nbsp;14 nh&acirc;n 14 luồng, kiến tr&uacute;c Arrow Lake với hiệu năng đa nh&acirc;n vượt trội. D&ograve;ng HX mang đến sức mạnh tương đương desktop, đ&aacute;p ứng mọi t&aacute;c vụ gaming v&agrave; s&aacute;ng tạo nội dung nặng.</p>
<p><strong>GPU &mdash; NVIDIA GeForce RTX 5060 8GB GDDR7:</strong>&nbsp;Card đồ họa rời thế hệ mới với 8GB VRAM GDDR7, kiến tr&uacute;c Blackwell, hỗ trợ ray tracing v&agrave; DLSS 4. Đ&aacute;p ứng tốt mọi tựa game AAA ở độ ph&acirc;n giải WQXGA với thiết lập cao, mang đến trải nghiệm gaming mượt m&agrave; tr&ecirc;n m&agrave;n h&igrave;nh 165Hz.</p>
<h3>RAM &amp; Lưu Trữ: 16GB DDR5 + 512GB SSD</h3>
<p>M&aacute;y được trang bị&nbsp;<strong>16GB DDR5 6400MHz</strong>, c&oacute; thể n&acirc;ng cấp để đ&aacute;p ứng nhu cầu sử dụng ng&agrave;y c&agrave;ng cao. Tốc độ RAM 6400MHz cho băng th&ocirc;ng lớn, hỗ trợ tốt c&aacute;c t&aacute;c vụ gaming v&agrave; đa nhiệm.</p>
<p>Ổ cứng&nbsp;<strong>512GB PCIe NVMe M.2 SSD Gen 4</strong>&nbsp;cho tốc độ đọc ghi vượt trội, khởi động m&aacute;y v&agrave; load game nhanh ch&oacute;ng. Khe cắm M.2 hỗ trợ n&acirc;ng cấp khi cần th&ecirc;m dung lượng.</p>
<h3>Pin &amp; Sạc: 80Wh</h3>
<p>Vi&ecirc;n pin&nbsp;<strong>Li-ion 80Wh</strong>&nbsp;cho thời lượng sử dụng tốt trong ph&acirc;n kh&uacute;c laptop gaming hiệu năng cao. Bộ sạc đi k&egrave;m nạp đầy năng lượng nhanh ch&oacute;ng, sẵn s&agrave;ng cho mọi phi&ecirc;n chơi game d&agrave;i.</p>
<h3>Cổng Kết Nối</h3>
<p><strong>Kết nối c&oacute; d&acirc;y:</strong></p>
<ul>
<li>1 x USB-C 10Gbps hỗ trợ DP 2.1 v&agrave; PD 3.0</li>
<li>1 x USB-C 10Gbps hỗ trợ DP 1.4</li>
<li>2 x USB-A 3.2 5Gbps</li>
<li>1 x USB-A 3.2 10Gbps hỗ trợ Output 5V/2A</li>
<li>1 x HDMI 2.1</li>
<li>1 x RJ-45 (LAN)</li>
<li>1 x 3.5mm Audio Jack combo</li>
<li>1 x Power input</li>
</ul>
<p>Kh&ocirc;ng d&acirc;y:&nbsp;<strong>Wi-Fi 6E + Bluetooth 5.4</strong>&nbsp;&mdash; kết nối ổn định, tốc độ cao, độ trễ thấp.</p>
<h3>&Acirc;m Thanh &amp; Webcam</h3>
<p>Webcam&nbsp;<strong>HD</strong>&nbsp;t&iacute;ch hợp đ&aacute;p ứng nhu cầu hội họp trực tuyến cơ bản. Hệ thống &acirc;m thanh được tối ưu cho trải nghiệm gaming v&agrave; giải tr&iacute; h&agrave;ng ng&agrave;y.</p>
<h3>Phần Mềm: Windows 11 Home</h3>
<p>M&aacute;y được c&agrave;i đặt sẵn&nbsp;<strong>Windows 11 Home</strong>&nbsp;bản quyền, sẵn s&agrave;ng sử dụng ngay khi mở m&aacute;y. Giao diện trực quan c&ugrave;ng c&aacute;c t&iacute;nh năng gaming v&agrave; bảo mật được tối ưu h&oacute;a.</p>
<h3>Ai N&ecirc;n Mua</h3>
<ul>
<li>Game thủ cần m&agrave;n h&igrave;nh OLED 165Hz với m&agrave;u sắc DCI-P3 100% sống động v&agrave; RTX 5060 đủ sức chiến game</li>
<li>Người d&ugrave;ng y&ecirc;u th&iacute;ch laptop gaming mỏng nhẹ 1.95kg nhưng vẫn giữ hiệu năng cao</li>
<li>Người d&ugrave;ng cần m&aacute;y vừa gaming vừa s&aacute;ng tạo nội dung với m&agrave;n h&igrave;nh chuẩn m&agrave;u v&agrave; CPU Core 7 mạnh mẽ</li>
</ul>
<h3>Tổng Kết</h3>
<p>Lenovo Legion 5i 2026 l&agrave; chiếc laptop gaming tầm trung ấn tượng với CPU Intel Core 7 245HX 14 nh&acirc;n, GPU RTX 5060 8GB v&agrave; m&agrave;n h&igrave;nh 15.3 icnh WQXGA OLED 165Hz DCI-P3 100%. Thiết kế kim loại mặt A cao cấp, trọng lượng chỉ 1.95kg, pin 80Wh c&ugrave;ng b&agrave;n ph&iacute;m RGB &mdash; sự kết hợp ho&agrave;n hảo giữa hiệu năng v&agrave; t&iacute;nh di động trong ph&acirc;n kh&uacute;c gaming.</p>
<p>Li&ecirc;n hệ ngay với&nbsp;<strong>LaptopAZ</strong>&nbsp;để sở hữu Lenovo Legion 5i 2026 với mức gi&aacute; tốt nhất v&agrave; dịch vụ hậu m&atilde;i uy t&iacute;n!</p>
<p><em>Lưu &yacute;: B&agrave;i viết v&agrave; h&igrave;nh ảnh chỉ c&oacute; t&iacute;nh chất tham khảo v&igrave; cấu h&igrave;nh v&agrave; đặc t&iacute;nh sản phẩm c&oacute; thể thay đổi theo thị trường v&agrave; từng phi&ecirc;n bản. Qu&yacute; kh&aacute;ch cần cấu h&igrave;nh + h&igrave;nh ảnh cụ thể vui l&ograve;ng li&ecirc;n hệ với c&aacute;c tư vấn vi&ecirc;n để được trợ gi&uacute;p.</em></p>
<p><img src="https://laptopaz.vn/media/lib/3034_anhkh4anh.jpg" alt="Laptop Lenovo Legion 5i 2026"></p>
<p><strong>Địa chỉ mua b&aacute;n Laptop uy t&iacute;n tại H&agrave; Nội v&agrave; tr&ecirc;n to&agrave;n quốc - LaptopAZ.vn</strong><br>Cơ sở 1: Số 18 Ng&otilde; 121 - Th&aacute;i H&agrave; - Đống Đa - H&agrave; Nội<br>Cơ sở 2: Số 56 Trần Ph&uacute; - H&agrave; Đ&ocirc;ng - H&agrave; Nội<br>Li&ecirc;n hệ ngay: Hotline 0825.233.233<br><a href="https://laptopaz.vn/huong-dan-mua-hang-tu-xa-mobile.html">Chuyển h&agrave;ng đi to&agrave;n quốc nhận m&aacute;y đ&uacute;ng như m&ocirc; tả mới phải thanh to&aacute;n xem chi tiết tại đ&acirc;y</a></p>','24000000',7,'6a8fe365f3767c30080d2ceb','Laptop','/uploads/fd4b0e7e70ea41f3801f94b4ef5bd0c0.webp','ACTIVE',4,1,553,'2026-08-20 11:48:45.285','2026-09-07 06:33:57.197','com.ecommerce.cnj70.document.Product');
INSERT INTO products (id,shop_id,shop_name,name,brand,warranty_months,manufacturer,manufacturer_address,description,rich_description,price,stock,category_id,category_name,thumbnail_url,status,rating,review_count,sold,created_at,updated_at,mongo_class) VALUES ('6a9e0ca18384ad7d8e3ac12c','6a86ac30add146680e49cf64','Test Vendor Shop','Điện thoại Apple iPhone 17 Pro Max (Apple A19 Pro, 12GB, 512GB, 6.9" Super Retina XDR 120Hz, iOS 19)','Apple',12,'Apple','hanoi',NULL,'<p>iPhone 17 Pro Max sở hữu m&agrave;n h&igrave;nh Super Retina XDR OLED k&iacute;ch thước 6,9 inch, độ ph&acirc;n giải 2868 &times; 1320 pixel v&agrave; mật độ điểm ảnh 460 ppi. M&aacute;y được trang bị c&ocirc;ng nghệ ProMotion với tần số qu&eacute;t th&iacute;ch ứng l&ecirc;n đến 120Hz, mang lại trải nghiệm hiển thị mượt m&agrave;.</p>
<p>iPhone 17 Pro Max sử dụng chip Apple A19 Pro, với CPU 6 l&otilde;i, GPU 6 l&otilde;i v&agrave; Neural Engine 16 l&otilde;i. M&aacute;y c&oacute; c&aacute;c t&ugrave;y chọn bộ nhớ trong gồm 256GB, 512GB, 1TB v&agrave; 2TB.</p>
<p>Về camera, iPhone 17 Pro Max được trang bị hệ thống camera sau gồm camera ch&iacute;nh 48MP Fusion, camera g&oacute;c si&ecirc;u rộng 48MP Fusion Ultra Wide v&agrave; camera Telephoto 48MP Fusion Telephoto. Camera Telephoto hỗ trợ zoom quang học 4x v&agrave; khả năng zoom chất lượng quang học l&ecirc;n đến 8x ở ti&ecirc;u cự tương đương 200mm. Camera trước TrueDepth hỗ trợ Center Stage.</p>
<p>Thiết bị hỗ trợ quay video với ProRes, Dolby Vision v&agrave; c&aacute;c chuẩn video HDR. iPhone 17 Pro Max hỗ trợ kết nối 5G, Wi-Fi 7 v&agrave; Bluetooth 6. M&aacute;y sử dụng cổng USB-C với tốc độ truyền dữ liệu USB 3 l&ecirc;n đến 10Gb/s, đồng thời hỗ trợ sạc kh&ocirc;ng d&acirc;y MagSafe v&agrave; Qi2 l&ecirc;n đến 25W.</p>
<p>iPhone 17 Pro Max c&oacute; khả năng chống bụi v&agrave; nước đạt chuẩn IP68. K&iacute;ch thước m&aacute;y l&agrave; 163,4 &times; 78,0 &times; 8,75 mm v&agrave; trọng lượng khoảng 231g. Sản phẩm c&oacute; c&aacute;c m&agrave;u gồm Bạc, Cam Vũ Trụ v&agrave; Xanh Đậm.</p>
<p>Về thời lượng pin, iPhone 17 Pro Max c&oacute; thể ph&aacute;t video l&ecirc;n đến 37 giờ. M&aacute;y chạy hệ điều h&agrave;nh iOS v&agrave; hỗ trợ c&aacute;c t&iacute;nh năng Apple Intelligence.</p>','12000000',1,'6a8fe365f3767c30080d2cec','Điện thoại','/uploads/e3a46688c9c649d9864de2a568c004bb.webp','ACTIVE',5,1,553,'2026-09-07 01:00:17.704','2026-09-07 08:25:38.742','com.ecommerce.cnj70.document.Product');

-- DATA: product_images
INSERT INTO product_images (product_id,image_url,sort_order) VALUES ('6a86e99da672993ccc5261fa','/uploads/fd4b0e7e70ea41f3801f94b4ef5bd0c0.webp',0);
INSERT INTO product_images (product_id,image_url,sort_order) VALUES ('6a9e0ca18384ad7d8e3ac12c','/uploads/e3a46688c9c649d9864de2a568c004bb.webp',0);

-- DATA: product_specifications
INSERT INTO product_specifications (product_id,name,value,unit,sort_order) VALUES ('6a86e99da672993ccc5261fa','Card','RTX 5060 8GB','',0);

-- DATA: product_variants
INSERT INTO product_variants (product_id,price,stock,sort_order) VALUES ('6a9e0ca18384ad7d8e3ac12c','200000000',0,0);

-- DATA: variant_specifications
INSERT INTO variant_specifications (variant_id,name,value,unit,sort_order) VALUES (1,'Màu Trắng','Trắng 256Gb','',0);

-- DATA: carts
INSERT INTO carts (id,user_id,updated_at,mongo_class) VALUES ('6a86ed1139232d710ae279d5','6a86ac30add146680e49cf63','2026-09-06 16:48:07.346','com.ecommerce.cnj70.document.Cart');
INSERT INTO carts (id,user_id,updated_at,mongo_class) VALUES ('6a8bff48e57acb026d6ddc75','6a8bd006779b6112504cedfe','2026-09-07 06:33:57.711','com.ecommerce.cnj70.document.Cart');
INSERT INTO carts (id,user_id,updated_at,mongo_class) VALUES ('6a8d2468e036f2355e7be4fc','6a8d245de036f2355e7be4fb','2026-08-31 15:18:42.531','com.ecommerce.cnj70.document.Cart');
INSERT INTO carts (id,user_id,updated_at,mongo_class) VALUES ('6a8e87eaeefff645f30b5f4c','6a8e87d9eefff645f30b5f4b','2026-08-26 06:30:02.151','com.ecommerce.cnj70.document.Cart');
INSERT INTO carts (id,user_id,updated_at,mongo_class) VALUES ('6a8ea0ec78552e05a398e7f2','6a8ea0db78552e05a398e7f1','2026-08-26 08:16:44.928','com.ecommerce.cnj70.document.Cart');
INSERT INTO carts (id,user_id,updated_at,mongo_class) VALUES ('6a8f905a21c56e0a8987ec19','6a8f904521c56e0a8987ec18','2026-08-27 01:18:38.645','com.ecommerce.cnj70.document.Cart');
INSERT INTO carts (id,user_id,updated_at,mongo_class) VALUES ('6a8fb33367cbb361eca2a9e8','6a8e9389f003d8236cd8267b','2026-08-27 03:47:30.726','com.ecommerce.cnj70.document.Cart');
INSERT INTO carts (id,user_id,updated_at,mongo_class) VALUES ('6a8fed0f5b6d1620f21f78c9','6a8fec3e5b6d1620f21f78c8','2026-08-27 07:53:51.901','com.ecommerce.cnj70.document.Cart');
INSERT INTO carts (id,user_id,updated_at,mongo_class) VALUES ('6a8ff77bd619e83eaaff0c99','6a8ff775d619e83eaaff0c98','2026-08-29 05:59:42.935','com.ecommerce.cnj70.document.Cart');
INSERT INTO carts (id,user_id,updated_at,mongo_class) VALUES ('6a91359343b5535f8fca522b','6a859691deda531cb1dd5276','2026-08-28 07:15:31.286','com.ecommerce.cnj70.document.Cart');
INSERT INTO carts (id,user_id,updated_at,mongo_class) VALUES ('6a95d812332e89711a732f4d','6a95d7f7332e89711a732f4c','2026-08-31 19:37:54.136','com.ecommerce.cnj70.document.Cart');
INSERT INTO carts (id,user_id,updated_at,mongo_class) VALUES ('6a9f13f72b034c3230f6da9c','6a9f0f353660e924ace56b55','2026-09-07 19:43:51.764','com.ecommerce.cnj70.document.Cart');
INSERT INTO carts (id,user_id,updated_at,mongo_class) VALUES ('6a9f29af5563aa340eaf10ae','6a9f299d5563aa340eaf10ad','2026-09-07 21:16:31.595','com.ecommerce.cnj70.document.Cart');

-- DATA: cart_items
INSERT INTO cart_items (cart_id,product_id,product_name,image_url,price,quantity,subtotal,shop_id,shop_name,stock,sort_order) VALUES ('6a86ed1139232d710ae279d5','6a86e99da672993ccc5261fa','Laptop Lenovo Legion Y7000X 2026 | Core 7 245HX, 16GB, 512GB, RTX 5060 8GB, 15.3 icnh WQXGA OLED 165Hz, Win 11','/uploads/7fe13501a2844ddb84268b5bd3df93d1.webp','24000000',5,'120000000','6a86ac30add146680e49cf64','Test Vendor Shop',10,0);
INSERT INTO cart_items (cart_id,product_id,product_name,image_url,price,quantity,subtotal,shop_id,shop_name,stock,sort_order) VALUES ('6a8d2468e036f2355e7be4fc','6a86e99da672993ccc5261fa','Laptop Lenovo Legion Y7000X 2026 | Core 7 245HX, 16GB, 512GB, RTX 5060 8GB, 15.3 icnh WQXGA OLED 165Hz, Win 11','/uploads/7fe13501a2844ddb84268b5bd3df93d1.webp','24000000',4,'96000000','6a86ac30add146680e49cf64','Test Vendor Shop',10,0);
INSERT INTO cart_items (cart_id,product_id,product_name,image_url,price,quantity,subtotal,shop_id,shop_name,stock,sort_order) VALUES ('6a8ff77bd619e83eaaff0c99','6a86e99da672993ccc5261fa','Laptop Lenovo Legion Y7000X 2026 | Core 7 245HX, 16GB, 512GB, RTX 5060 8GB, 15.3 icnh WQXGA OLED 165Hz, Win 11','/uploads/7fe13501a2844ddb84268b5bd3df93d1.webp','24000000',1,'24000000','6a86ac30add146680e49cf64','Test Vendor Shop',10,0);

-- DATA: orders
INSERT INTO orders (id,user_id,user_name,user_email,user_phone,shipping_address,subtotal,shipping_fee,total_amount,status,payment_method,paid,created_at,updated_at,delivered_at,mongo_class) VALUES ('6a86ed1b39232d710ae279d6','6a86ac30add146680e49cf63','Test Vendor Shop','vendor@test.com','0559605251','sss','240000000','15000','240015000','DELIVERED','COD',0,'2026-08-20 12:03:39.017','2026-08-25 10:25:29.222','2026-08-25 10:25:29.222','com.ecommerce.cnj70.document.Order');
INSERT INTO orders (id,user_id,user_name,user_email,user_phone,shipping_address,subtotal,shipping_fee,total_amount,status,payment_method,paid,created_at,updated_at,delivered_at,mongo_class) VALUES ('6a8d6d3911be6d35b466040b','6a86ac30add146680e49cf63','Test Vendor Shop','vendor@test.com','0559605251','df','240000000','15000','240015000','CANCELLED','COD',0,'2026-08-25 10:23:53.300','2026-08-25 10:25:50.750','2026-08-25 10:25:18.745','com.ecommerce.cnj70.document.Order');
INSERT INTO orders (id,user_id,user_name,user_email,user_phone,shipping_address,subtotal,shipping_fee,total_amount,status,payment_method,paid,created_at,updated_at,delivered_at,mongo_class) VALUES ('6a8f906e21c56e0a8987ec1a','6a8f904521c56e0a8987ec18','customer2','cust_new02@test.com','0123456789','hà nội','24000000','15000','24015000','PENDING','VNPAY',0,'2026-08-27 01:18:38.438','2026-08-27 01:18:38.438',NULL,'com.ecommerce.cnj70.document.Order');
INSERT INTO orders (id,user_id,user_name,user_email,user_phone,shipping_address,subtotal,shipping_fee,total_amount,status,payment_method,paid,created_at,updated_at,delivered_at,mongo_class) VALUES ('6a8fb35267cbb361eca2a9e9','6a8e9389f003d8236cd8267b','admin','admin2@gmail.com','79899794','hà nội','24000000','15000','24015000','PENDING','COD',0,'2026-08-27 03:47:30.431','2026-08-27 03:47:30.431',NULL,'com.ecommerce.cnj70.document.Order');
INSERT INTO orders (id,user_id,user_name,user_email,user_phone,shipping_address,subtotal,shipping_fee,total_amount,status,payment_method,paid,created_at,updated_at,delivered_at,mongo_class) VALUES ('6a9446f310e0c55de9443c1d','6a8bd006779b6112504cedfe','Trần Anh Quân','Anhquanql4.0@gmail.com','0386267692','Quỳnh Lưu Nghệ An
dsad','480000000','15000','480015000','PENDING','COD',0,'2026-08-30 15:06:27.080','2026-08-30 15:06:27.080',NULL,'com.ecommerce.cnj70.document.Order');
INSERT INTO orders (id,user_id,user_name,user_email,user_phone,shipping_address,subtotal,shipping_fee,total_amount,status,payment_method,paid,created_at,updated_at,delivered_at,mongo_class) VALUES ('6a9b9a35289de826a22aa6c8','6a9b9a34289de826a22aa6c3','Phase10 Customer','customer@phase10.local',NULL,'123 Phase10 St','100000','10000','110000','PENDING','COD',0,'2026-09-05 04:27:33.449','2026-09-05 04:27:33.449',NULL,'com.ecommerce.cnj70.document.Order');
INSERT INTO orders (id,user_id,user_name,user_email,user_phone,shipping_address,subtotal,shipping_fee,total_amount,status,payment_method,paid,created_at,updated_at,delivered_at,mongo_class) VALUES ('6a9b9a35289de826a22aa6c9','6a9b9a34289de826a22aa6c3','Phase10 Customer','customer@phase10.local',NULL,'123 Phase10 St','100000','10000','110000','PREPARING','COD',0,'2026-09-05 04:27:33.508','2026-09-05 04:27:33.508',NULL,'com.ecommerce.cnj70.document.Order');
INSERT INTO orders (id,user_id,user_name,user_email,user_phone,shipping_address,subtotal,shipping_fee,total_amount,status,payment_method,paid,created_at,updated_at,delivered_at,mongo_class) VALUES ('6a9b9a35289de826a22aa6ca','6a9b9a34289de826a22aa6c3','Phase10 Customer','customer@phase10.local',NULL,'123 Phase10 St','100000','10000','110000','SHIPPING','COD',0,'2026-09-05 04:27:33.566','2026-09-05 04:27:33.566',NULL,'com.ecommerce.cnj70.document.Order');
INSERT INTO orders (id,user_id,user_name,user_email,user_phone,shipping_address,subtotal,shipping_fee,total_amount,status,payment_method,paid,created_at,updated_at,delivered_at,mongo_class) VALUES ('6a9b9a35289de826a22aa6cb','6a9b9a34289de826a22aa6c3','Phase10 Customer','customer@phase10.local',NULL,'123 Phase10 St','100000','10000','110000','DELIVERED','COD',0,'2026-09-05 04:27:33.621','2026-09-05 04:27:33.621',NULL,'com.ecommerce.cnj70.document.Order');
INSERT INTO orders (id,user_id,user_name,user_email,user_phone,shipping_address,subtotal,shipping_fee,total_amount,status,payment_method,paid,created_at,updated_at,delivered_at,mongo_class) VALUES ('6a9b9a35289de826a22aa6cc','6a9b9a34289de826a22aa6c3','Phase10 Customer','customer@phase10.local',NULL,'123 Phase10 St','100000','10000','110000','CANCELLED','COD',0,'2026-09-05 04:27:33.674','2026-09-05 04:27:33.674',NULL,'com.ecommerce.cnj70.document.Order');
INSERT INTO orders (id,user_id,user_name,user_email,user_phone,shipping_address,subtotal,shipping_fee,total_amount,status,payment_method,paid,created_at,updated_at,delivered_at,mongo_class) VALUES ('6a9e5ad58ce02763d4f231c3','6a8bd006779b6112504cedfe','Trần Anh Quân','Anhquanql4.0@gmail.com','0386267692','Quỳnh Lưu Nghệ An','36000000','15000','36015000','DELIVERED','COD',0,'2026-09-07 06:33:57.382','2026-09-07 06:35:12.191','2026-09-07 06:35:12.191','com.ecommerce.cnj70.document.Order');

-- DATA: order_items
INSERT INTO order_items (order_id,shop_id,product_id,product_name,image_url,price,quantity,subtotal,sort_order) VALUES ('6a86ed1b39232d710ae279d6','6a86ac30add146680e49cf64','6a86e99da672993ccc5261fa','Laptop Lenovo Legion Y7000X 2026 | Core 7 245HX, 16GB, 512GB, RTX 5060 8GB, 15.3 icnh WQXGA OLED 165Hz, Win 11',NULL,'240000000',1,'240000000',0);
INSERT INTO order_items (order_id,shop_id,product_id,product_name,image_url,price,quantity,subtotal,sort_order) VALUES ('6a8d6d3911be6d35b466040b','6a86ac30add146680e49cf64','6a86e99da672993ccc5261fa','Laptop Lenovo Legion Y7000X 2026 | Core 7 245HX, 16GB, 512GB, RTX 5060 8GB, 15.3 icnh WQXGA OLED 165Hz, Win 11',NULL,'240000000',1,'240000000',0);
INSERT INTO order_items (order_id,shop_id,product_id,product_name,image_url,price,quantity,subtotal,sort_order) VALUES ('6a8f906e21c56e0a8987ec1a','6a86ac30add146680e49cf64','6a86e99da672993ccc5261fa','Laptop Lenovo Legion Y7000X 2026 | Core 7 245HX, 16GB, 512GB, RTX 5060 8GB, 15.3 icnh WQXGA OLED 165Hz, Win 11','/uploads/e63b1b3d948f465a8fc3afe3f0fa64a2.webp','24000000',1,'24000000',0);
INSERT INTO order_items (order_id,shop_id,product_id,product_name,image_url,price,quantity,subtotal,sort_order) VALUES ('6a8fb35267cbb361eca2a9e9','6a86ac30add146680e49cf64','6a86e99da672993ccc5261fa','Laptop Lenovo Legion Y7000X 2026 | Core 7 245HX, 16GB, 512GB, RTX 5060 8GB, 15.3 icnh WQXGA OLED 165Hz, Win 11','/uploads/e63b1b3d948f465a8fc3afe3f0fa64a2.webp','24000000',1,'24000000',0);
INSERT INTO order_items (order_id,shop_id,product_id,product_name,image_url,price,quantity,subtotal,sort_order) VALUES ('6a9446f310e0c55de9443c1d','6a86ac30add146680e49cf64','6a86e99da672993ccc5261fa','Laptop Lenovo Legion Y7000X 2026 | Core 7 245HX, 16GB, 512GB, RTX 5060 8GB, 15.3 icnh WQXGA OLED 165Hz, Win 11','/uploads/7fe13501a2844ddb84268b5bd3df93d1.webp','240000000',2,'480000000',0);
INSERT INTO order_items (order_id,shop_id,product_id,product_name,image_url,price,quantity,subtotal,sort_order) VALUES ('6a9b9a35289de826a22aa6c8','shop-1','p-1','Phase10 Item',NULL,'50000',2,'100000',0);
INSERT INTO order_items (order_id,shop_id,product_id,product_name,image_url,price,quantity,subtotal,sort_order) VALUES ('6a9b9a35289de826a22aa6c9','shop-1','p-1','Phase10 Item',NULL,'50000',2,'100000',0);
INSERT INTO order_items (order_id,shop_id,product_id,product_name,image_url,price,quantity,subtotal,sort_order) VALUES ('6a9b9a35289de826a22aa6ca','shop-1','p-1','Phase10 Item',NULL,'50000',2,'100000',0);
INSERT INTO order_items (order_id,shop_id,product_id,product_name,image_url,price,quantity,subtotal,sort_order) VALUES ('6a9b9a35289de826a22aa6cb','shop-1','p-1','Phase10 Item',NULL,'50000',2,'100000',0);
INSERT INTO order_items (order_id,shop_id,product_id,product_name,image_url,price,quantity,subtotal,sort_order) VALUES ('6a9b9a35289de826a22aa6cc','shop-1','p-1','Phase10 Item',NULL,'50000',2,'100000',0);
INSERT INTO order_items (order_id,shop_id,product_id,product_name,image_url,price,quantity,subtotal,sort_order) VALUES ('6a9e5ad58ce02763d4f231c3','6a86ac30add146680e49cf64','6a9e0ca18384ad7d8e3ac12c','iphone 17 promax','/uploads/e3a46688c9c649d9864de2a568c004bb.webp','12000000',1,'12000000',0);
INSERT INTO order_items (order_id,shop_id,product_id,product_name,image_url,price,quantity,subtotal,sort_order) VALUES ('6a9e5ad58ce02763d4f231c3','6a86ac30add146680e49cf64','6a86e99da672993ccc5261fa','Laptop Lenovo Legion Y7000X 2026 | Core 7 245HX, 16GB, 512GB, RTX 5060 8GB, 15.3 icnh WQXGA OLED 165Hz, Win 11','/uploads/fd4b0e7e70ea41f3801f94b4ef5bd0c0.webp','24000000',1,'24000000',1);

-- DATA: reviews
INSERT INTO reviews (id,product_id,user_id,user_name,rating,comment,created_at,mongo_class) VALUES ('6a95d76d332e89711a732f4b','6a86e99da672993ccc5261fa','6a8d245de036f2355e7be4fb','Vi Anh Tuấn',4,'test1
TEST 2
TEST3','2026-08-31 19:35:09.237','com.ecommerce.cnj70.document.Review');

-- DATA: vouchers
INSERT INTO vouchers (id,code,name,type,shop_id,shop_name,discount_type,discount_value,max_discount_amount,min_order_value,quantity,used,start_date,end_date,active,created_by,created_at,updated_at,mongo_class) VALUES ('6a9ba46999fe5c0d33facdf7','PHASE11_1918','Phase11 Updated','WEB',NULL,NULL,'PERCENT','20','60000','100000',50,0,NULL,NULL,1,'6a8e9389f003d8236cd8267b','2026-09-05 05:11:05.429','2026-09-05 07:34:19.036','com.ecommerce.cnj70.document.Voucher');
INSERT INTO vouchers (id,code,name,type,shop_id,shop_name,discount_type,discount_value,max_discount_amount,min_order_value,quantity,used,start_date,end_date,active,created_by,created_at,updated_at,mongo_class) VALUES ('6a9d78cc3361df34205ef27d','GIAM123','Giam cho ngay 9/9','SHOP','6a8ff7f3d619e83eaaff0c9a','Cửa hàng laptop','PERCENT','50','100000','70000',90,0,'2026-09-06 14:29:00.000','2026-09-09 14:29:00.000',1,'6a8ff775d619e83eaaff0c98','2026-09-06 14:29:32.473','2026-09-06 14:29:32.473','com.ecommerce.cnj70.document.Voucher');
INSERT INTO vouchers (id,code,name,type,shop_id,shop_name,discount_type,discount_value,max_discount_amount,min_order_value,quantity,used,start_date,end_date,active,created_by,created_at,updated_at,mongo_class) VALUES ('6a9f0f0e3660e924ace56b54','PHASE11_TEST_002','Phase 11 Test Voucher 2','WEB',NULL,NULL,'PERCENT','15','50000','100000',100,0,'2026-09-07 17:00:00.000','2026-12-31 16:59:00.000',0,'6a8e9389f003d8236cd8267b','2026-09-07 19:22:54.441','2026-09-07 19:23:25.213','com.ecommerce.cnj70.document.Voucher');
INSERT INTO vouchers (id,code,name,type,shop_id,shop_name,discount_type,discount_value,max_discount_amount,min_order_value,quantity,used,start_date,end_date,active,created_by,created_at,updated_at,mongo_class) VALUES ('6a9f11e801c68b08d309f259','PHASE12_TEST_001','Phase 12 Test Voucher','WEB',NULL,NULL,'PERCENT','20','100000','200000',50,0,'2026-09-07 17:00:00.000','2026-12-31 16:59:00.000',1,'6a8e9389f003d8236cd8267b','2026-09-07 19:35:04.305','2026-09-07 19:35:04.305','com.ecommerce.cnj70.document.Voucher');

-- DATA: voucher_products

SET FOREIGN_KEY_CHECKS = 1;

-- =====================================================================
-- END OF EXPORT
-- =====================================================================
