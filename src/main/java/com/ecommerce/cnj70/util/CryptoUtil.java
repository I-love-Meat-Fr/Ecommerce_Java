package com.ecommerce.cnj70.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * TASK #19 — AES-256-GCM encryption/decryption cho PII data.
 *
 * AES-256-GCM cung cấp:
 * - Confidentiality: dữ liệu được mã hóa, không đọc được plaintext
 * - Authenticity/Integrity: ciphertext bị sửa đổi sẽ bị phát hiện khi giải mã
 *
 * Storage format: Base64(iv || ciphertext || authTag)
 *   iv = 12 bytes (thay đổi mỗi lần mã hóa)
 *   ciphertext = encrypted data + 16-byte auth tag
 *
 * Quy tắc:
 * - Key được đọc từ env var ENCRYPTION_KEY (Base64-encoded, 32 bytes)
 * - Nếu không có key → log lỗi, throw exception (không mã hóa bằng key mặc định)
 * - Giải mã chỉ thực hiện khi cần hiển thị (trong service/response mapping)
 * - Không bao giờ log plaintext hoặc key
 */
@Slf4j
@Component
public class CryptoUtil {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;      // 96-bit IV (recommended for GCM)
    private static final int GCM_TAG_LENGTH = 128;      // 128-bit authentication tag

    private static final String ENV_KEY = "ENCRYPTION_KEY";

    private final SecretKey secretKey;
    private final SecureRandom secureRandom;

    public CryptoUtil() {
        this.secureRandom = new SecureRandom();
        this.secretKey = loadKey();
    }

    /**
     * Mã hóa plaintext bằng AES-256-GCM.
     *
     * @param plaintext dữ liệu nhạy cảm cần mã hóa (CCCD, taxCode, bankAccount...)
     * @return Base64(iv || ciphertext || authTag). Null nếu input null/blank.
     */
    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isBlank()) {
            return null;
        }

        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec);

            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            // Format: iv || ciphertext (đã chứa auth tag)
            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + ciphertext.length);
            byteBuffer.put(iv);
            byteBuffer.put(ciphertext);

            return Base64.getEncoder().encodeToString(byteBuffer.array());
        } catch (Exception e) {
            log.error("Encryption failed for PII data: {}", e.getMessage());
            throw new SecurityException("Mã hóa PII data thất bại", e);
        }
    }

    /**
     * Giải mã ciphertext bằng AES-256-GCM.
     *
     * @param encryptedText Base64(iv || ciphertext || authTag)
     * @return plaintext gốc. Null nếu input null/blank.
     * @throws SecurityException nếu ciphertext bị sửa đổi hoặc key sai
     */
    public String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.isBlank()) {
            return null;
        }

        try {
            byte[] decoded = Base64.getDecoder().decode(encryptedText);

            ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[GCM_IV_LENGTH];
            byteBuffer.get(iv);
            byte[] ciphertext = new byte[byteBuffer.remaining()];
            byteBuffer.get(ciphertext);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec);

            byte[] plaintext = cipher.doFinal(ciphertext);
            return new String(plaintext, java.nio.charset.StandardCharsets.UTF_8);
        } catch (javax.crypto.AEADBadTagException e) {
            log.error("Decryption failed: ciphertext tampered or wrong key");
            throw new SecurityException("Decrypt PII data thất bại: dữ liệu bị sửa đổi hoặc key sai", e);
        } catch (Exception e) {
            log.error("Decryption failed for PII data: {}", e.getMessage());
            throw new SecurityException("Giải mã PII data thất bại", e);
        }
    }

    /**
     * Load AES-256 key từ environment variable.
     * Key phải là Base64-encoded 32-byte key.
     */
    private SecretKey loadKey() {
        String keyBase64 = System.getProperty(ENV_KEY);

        if (keyBase64 == null || keyBase64.isBlank()) {
            log.error("ENCRYPTION_KEY environment variable is not set. PII encryption disabled.");
            throw new IllegalStateException(
                    "ENCRYPTION_KEY env variable is required for PII encryption. " +
                    "Generate one with: openssl rand -base64 32");
        }

        try {
            byte[] keyBytes = Base64.getDecoder().decode(keyBase64);

            if (keyBytes.length != 32) {
                throw new IllegalStateException(
                        "ENCRYPTION_KEY must be 32 bytes (256 bits). " +
                        "Got " + keyBytes.length + " bytes. " +
                        "Generate with: openssl rand -base64 32");
            }

            return new SecretKeySpec(keyBytes, "AES");
        } catch (IllegalArgumentException e) {
            log.error("ENCRYPTION_KEY is not valid Base64");
            throw new IllegalStateException(
                    "ENCRYPTION_KEY must be Base64-encoded. " +
                    "Generate with: openssl rand -base64 32");
        }
    }
}
