package com.coursistant.lms.v2.service;

import cn.hutool.core.io.FileUtil;
import com.coursistant.lms.v2.common.FileStorageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.util.UUID;

@Slf4j
@Service("localFileStorageService")
@RequiredArgsConstructor
public class LocalFileStorageService implements FileStorageService {
    private static final String BASE_FILE_PATH = "disk/";

    @Override
    public FileStorageResult upload(MultipartFile file, String storagePath) {
        try {
            if (file == null || file.isEmpty()) {
                return FileStorageResult.fail("File is empty", file != null ? file.getOriginalFilename() : null);
            }
            var originalFilename = file.getOriginalFilename();
            if (storagePath == null) storagePath = BASE_FILE_PATH;
            else if (!storagePath.startsWith(BASE_FILE_PATH)) storagePath = BASE_FILE_PATH + storagePath;
            if (!storagePath.endsWith("/")) storagePath += "/";
            
            // Create absolute directory path
            var directoryPath = Paths.get(storagePath).toAbsolutePath();
            System.out.println("ORIGINAL DIRECTORY PATH: " + directoryPath);

            Path basePath = directoryPath.getRoot().resolve(directoryPath.subpath(0,3));

            System.out.println("BASE PATH: "+basePath);
            
            Path remainingPath = directoryPath.subpath(4,directoryPath.getNameCount());
            System.out.println("REMAINING PATH: "+remainingPath);

            Path newPath = basePath.resolve(remainingPath);
            
            System.out.println("NEW PATH: "+newPath);
            if (!Files.exists(newPath)) {
                Files.createDirectories(newPath);
                log.info("Created directory: {}", newPath);
            } else {
                System.out.println("Directory already exists");
            }
            
            // Save file in parent directory
            var fullFilePath = newPath.resolve(originalFilename);
            System.out.println("FULL FILE PATH: " + fullFilePath);
            
            double fileSizeKB = BigDecimal.valueOf(file.getBytes().length)
                    .divide(BigDecimal.valueOf(1024), 3, RoundingMode.HALF_UP)
                    .doubleValue();
            
            file.transferTo(fullFilePath);
            log.info("File saved locally: {}", fullFilePath);
            
            return FileStorageResult.success(
                    originalFilename,
                    fileSizeKB,
                    file.getContentType(),
                    fullFilePath.toString()
            );
        } catch (IOException e) {
            log.error("Failed to upload file: {}", file.getOriginalFilename(), e);
            return FileStorageResult.fail("Upload failed: " + e.getMessage(),
                    file.getOriginalFilename());
        } catch (Exception e) {
            var name = file == null ? null : file.getOriginalFilename();
            log.error("Unexpected error during file upload: {}", name, e);
            return FileStorageResult.fail("Unexpected error: " + e.getMessage(), name);
        }
    }

    @Override
    public boolean delete(String filePath) {
        try {
            if (filePath == null || filePath.trim().isEmpty()) {
                log.warn("File path is empty");
                return false;
            }

            File file = new File(filePath);
            if (!file.exists()) {
                log.warn("File does not exist: {}", filePath);
                return false;
            }

            boolean deleted = FileUtil.del(file);
            if (deleted) {
                log.info("File deleted: {}", filePath);
            } else {
                log.error("Failed to delete file: {}", filePath);
            }

            return deleted;
        } catch (Exception e) {
            log.error("Error deleting file: {}", filePath, e);
            return false;
        }
    }
}
