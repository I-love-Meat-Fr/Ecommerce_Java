package com.ecommerce.cnj70.config;

import io.github.cdimascio.dotenv.Dotenv;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DotenvConfig {

    @PostConstruct
    public void init() {
        try {
            Dotenv dotenv = Dotenv.configure()
                    .ignoreIfMissing()
                    .load();
            
            dotenv.entries().forEach(entry -> {
                if (System.getProperty(entry.getKey()) == null) {
                    System.setProperty(entry.getKey(), entry.getValue());
                }
            });
            
            log.info("Loaded environment variables from .env file");
        } catch (Exception e) {
            log.warn("Could not load .env file, using system properties or defaults");
        }
    }
}
