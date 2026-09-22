package com.ecommerce.cnj70.config;

import io.github.cdimascio.dotenv.Dotenv;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

/**
 * Load .env file BEFORE Spring context starts.
 * This ensures ENCRYPTION_KEY is available for CryptoUtil constructor.
 * 
 * Also sets System properties so classes using System.getProperty() can access them.
 */
@Slf4j
public class DotenvInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        try {
            Dotenv dotenv = Dotenv.configure()
                    .ignoreIfMissing()
                    .load();

            Map<String, Object> envMap = new HashMap<>();
            dotenv.entries().forEach(entry -> {
                // Set to both System properties (for CryptoUtil) and Spring Environment
                System.setProperty(entry.getKey(), entry.getValue());
                envMap.put(entry.getKey(), entry.getValue());
            });

            ConfigurableEnvironment environment = applicationContext.getEnvironment();
            environment.getPropertySources()
                    .addFirst(new MapPropertySource("dotenv", envMap));

            log.info("DotenvInitializer: Loaded {} environment variables from .env", envMap.size());
        } catch (Exception e) {
            log.warn("DotenvInitializer: Could not load .env file: {}", e.getMessage());
        }
    }
}
