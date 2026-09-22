package com.ecommerce.cnj70.util;

import com.ecommerce.cnj70.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.Set;
import java.util.UUID;

/**
 * TASK #20 — Upload Security utilities.
 *
 * Security rules:
 * 1. File size limit: 5MB per file (configurable via MAX_FILE_SIZE env var)
 * 2. MIME type validation: chỉ cho phép whitelist MIME types
 * 3. UUID filename: KHÔNG dùng originalFilename từ user
 * 4. Extension whitelist: chỉ cho phép .jpg, .jpeg, .png, .pdf
 *
 * Allowed MIME types:
 * - image/jpeg  (.jpg, .jpeg)
 * - image/png   (.png)
 * - application/pdf (.pdf)
 */
@Slf4j
public class FileUploadUtil {

    private static final String UPLOAD_DIR = "src/main/resources/static/uploads/";

    /** 5MB default, có thể override qua env var MAX_FILE_SIZE (bytes) */
    private static final long DEFAULT_MAX_SIZE = 5 * 1024 * 1024L; // 5MB

    /** Allowed MIME types */
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "application/pdf"
    );

    /** Allowed extensions (phòng trường hợp MIME bị spoof) */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".jpg", ".jpeg", ".png", ".pdf"
    );

    /**
     * Lưu file với đầy đủ security validation.
     *
     * @param file MultipartFile từ request
     * @return URL path đã lưu (/uploads/{uuid}.{ext})
     * @throws BadRequestException nếu file vượt size, sai MIME, hoặc extension không hợp lệ
     */
    public static String saveFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File không được để trống");
        }

        // ===== TASK #20: Validate file size =====
        long maxSize = getMaxFileSize();
        if (file.getSize() > maxSize) {
            throw new BadRequestException(
                    String.format("File vượt quá dung lượng cho phép (%d MB). File của bạn: %.2f MB",
                            maxSize / (1024 * 1024), file.getSize() / (1024.0 * 1024.0)));
        }

        // ===== TASK #20: Validate MIME type =====
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            throw new BadRequestException(
                    "Loại file không được phép. Chỉ chấp nhận: JPG, PNG, PDF. MIME: " + contentType);
        }

        // ===== TASK #20: Validate extension (phòng MIME spoof) =====
        String originalFilename = file.getOriginalFilename();
        String extension = getExtension(originalFilename);
        if (extension == null || !ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new BadRequestException(
                    "Phần mở rộng file không được phép. Chỉ chấp nhận: .jpg, .jpeg, .png, .pdf");
        }

        // ===== TASK #20: UUID filename - KHÔNG dùng originalFilename =====
        String uuidFilename = UUID.randomUUID().toString() + extension.toLowerCase();

        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        Path filePath = uploadPath.resolve(uuidFilename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        log.info("[FileUpload] Saved: {}, originalName: {}, size: {} bytes",
                uuidFilename, originalFilename, file.getSize());

        return "/uploads/" + uuidFilename;
    }

    /**
     * Validate file trước khi upload (dùng cho KYC/evidence upload).
     * Gọi method này trước khi saveFile để có error message riêng.
     */
    public static void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File không được để trống");
        }

        long maxSize = getMaxFileSize();
        if (file.getSize() > maxSize) {
            throw new BadRequestException(
                    String.format("File vượt quá %d MB", maxSize / (1024 * 1024)));
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            throw new BadRequestException("Loại file không được phép. Chỉ chấp nhận: JPG, PNG, PDF");
        }

        String extension = getExtension(file.getOriginalFilename());
        if (extension == null || !ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new BadRequestException("Phần mở rộng file không hợp lệ");
        }
    }

    /**
     * Lưu Base64 image với UUID filename.
     */
    public static String saveBase64Image(String base64Data, String extension) throws IOException {
        if (base64Data == null || base64Data.isEmpty()) {
            return null;
        }

        // Validate extension
        String ext = extension != null ? extension.toLowerCase() : ".jpg";
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new BadRequestException("Phần mở rộng không được phép");
        }

        String imageData = base64Data;
        if (base64Data.contains(",")) {
            imageData = base64Data.split(",")[1];
        }

        byte[] imageBytes;
        try {
            imageBytes = Base64.getDecoder().decode(imageData);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Dữ liệu Base64 không hợp lệ");
        }

        // ===== TASK #20: Validate decoded size =====
        long maxSize = getMaxFileSize();
        if (imageBytes.length > maxSize) {
            throw new BadRequestException(
                    String.format("File vượt quá %d MB", maxSize / (1024 * 1024)));
        }

        // ===== TASK #20: UUID filename =====
        String uuidFilename = UUID.randomUUID().toString() + ext;

        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        Path filePath = uploadPath.resolve(uuidFilename);
        Files.write(filePath, imageBytes);

        return "/uploads/" + uuidFilename;
    }

    /**
     * Xóa file (chỉ xóa nếu nằm trong thư mục uploads để tránh path traversal).
     */
    public static void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return;
        }

        // Path traversal protection: chỉ xóa file trong /uploads/
        if (!fileUrl.contains("/uploads/")) {
            log.warn("[FileUpload] Refused to delete file outside uploads: {}", fileUrl);
            return;
        }

        String filename = fileUrl.replace("/uploads/", "");
        // Loại bỏ path traversal attempts
        filename = filename.replace("..", "").replace("/", "").replace("\\", "");

        Path filePath = Paths.get(UPLOAD_DIR, filename);

        try {
            Files.deleteIfExists(filePath);
            log.info("[FileUpload] Deleted: {}", filename);
        } catch (IOException e) {
            log.warn("[FileUpload] Could not delete file: {}", filePath);
        }
    }

    private static long getMaxFileSize() {
        String maxSizeStr = System.getProperty("MAX_FILE_SIZE");
        if (maxSizeStr != null && !maxSizeStr.isBlank()) {
            try {
                return Long.parseLong(maxSizeStr);
            } catch (NumberFormatException e) {
                log.warn("Invalid MAX_FILE_SIZE env var: {}, using default 5MB", maxSizeStr);
            }
        }
        return DEFAULT_MAX_SIZE;
    }

    private static String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return null;
        }
        return filename.substring(filename.lastIndexOf(".")).toLowerCase();
    }
}
