package com.coursistant.lms.v2.common;

import lombok.Data;

@Data
public class FileStorageResult {
    private String originalFilename;
    private Double fileSize;
    private String mimeType;
    private String filePath;
    private Boolean success;
    private String message;
    private String error;

    public static FileStorageResult success(String originalFilename, Double fileSize, String mimeType, String filePath) {
        FileStorageResult result = new FileStorageResult();
        result.setOriginalFilename(originalFilename);
        result.setFileSize(fileSize);
        result.setMimeType(mimeType);
        result.setFilePath(filePath);
        result.setSuccess(true);
        result.setMessage("File uploaded successfully");
        return result;
    }

    public static FileStorageResult fail(String error, String originalFilename) {
        FileStorageResult result = new FileStorageResult();
        result.setOriginalFilename(originalFilename);
        result.setSuccess(false);
        result.setError(error);
        result.setMessage("File upload failed");
        return result;
    }
}