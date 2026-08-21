package com.ecommerce.cnj70.config;

import com.ecommerce.cnj70.document.Shop;
import com.ecommerce.cnj70.document.User;
import com.ecommerce.cnj70.enums.AccountStatus;
import com.ecommerce.cnj70.enums.UserRole;
import com.ecommerce.cnj70.repository.ShopRepository;
import com.ecommerce.cnj70.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@Profile("dev")
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ShopRepository shopRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        createTestVendor();
    }

    private void createTestVendor() {
        String testEmail = "vendor@test.com";

        if (userRepository.existsByEmail(testEmail)) {
            log.info("Test vendor user already exists: {}", testEmail);
            return;
        }

        User vendor = User.builder()
                .email(testEmail)
                .password(passwordEncoder.encode("password123"))
                .fullName("Test Vendor Shop")
                .phone("0123456789")
                .address("123 Test Street, HCM City")
                .role(UserRole.VENDOR)
                .status(AccountStatus.ACTIVE)
                .build();

        User savedVendor = userRepository.save(vendor);
        log.info("Created test vendor user: {}", savedVendor.getEmail());

        Shop shop = Shop.builder()
                .shopName("Test Vendor Shop")
                .description("Shop for testing purpose")
                .active(true)
                .build();

        Shop savedShop = shopRepository.save(shop);
        log.info("Created test shop: {} with ID: {}", savedShop.getShopName(), savedShop.getId());

        savedVendor.setShopId(savedShop.getId());
        userRepository.save(savedVendor);
        log.info("Linked vendor {} to shop {}", savedVendor.getEmail(), savedShop.getId());
    }
}
