package com.coursistant.lms.v2.service;

import com.coursistant.lms.v2.common.FileStorageResult;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    FileStorageResult upload(MultipartFile file, String storagePath);

    boolean delete(String filePath);
}
