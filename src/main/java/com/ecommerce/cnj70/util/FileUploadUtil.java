package com.ecommerce.cnj70.util;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.UUID;

public class FileUploadUtil {
    
    private static final String UPLOAD_DIR = "src/main/resources/static/uploads/";
    
    public static String saveFile(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            return null;
        }
        
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        
        String filename = UUID.randomUUID().toString() + extension;
        
        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        
        Path filePath = uploadPath.resolve(filename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        
        return "/uploads/" + filename;
    }
    
    public static String saveBase64Image(String base64Data, String filename) throws IOException {
        if (base64Data == null || base64Data.isEmpty()) {
            return null;
        }
        
        String imageData = base64Data;
        if (base64Data.contains(",")) {
            imageData = base64Data.split(",")[1];
        }
        
        byte[] imageBytes = Base64.getDecoder().decode(imageData);
        
        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        
        Path filePath = uploadPath.resolve(filename);
        Files.write(filePath, imageBytes);
        
        return "/uploads/" + filename;
    }
    
    public static void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return;
        }
        
        String filename = fileUrl.replace("/uploads/", "");
        Path filePath = Paths.get(UPLOAD_DIR, filename);
        
        try {
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
