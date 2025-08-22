package com.coursistant.lms.entity.DTO;


import org.springframework.web.multipart.MultipartFile;
import java.io.*;
import java.nio.file.*;

public class PathMultipartFile implements MultipartFile {
    private final Path path;
    private final String fieldName;
    private final String originalFilename;
    private final String contentType;

    public PathMultipartFile(Path path, String fieldName, String originalFilename, String contentType) {
        this.path = path; this.fieldName = fieldName;
        this.originalFilename = originalFilename; this.contentType = contentType;
    }
    @Override public String getName() { return fieldName; }
    @Override public String getOriginalFilename() { return originalFilename; }
    @Override public String getContentType() { return contentType; }
    @Override public boolean isEmpty() { return !Files.exists(path); }
    @Override public long getSize() { try { return Files.size(path); } catch (IOException e) { return 0; } }
    @Override public byte[] getBytes() throws IOException { return Files.readAllBytes(path); }
    @Override public InputStream getInputStream() throws IOException { return Files.newInputStream(path); }
    @Override public void transferTo(File dest) throws IOException { Files.copy(path, dest.toPath(), StandardCopyOption.REPLACE_EXISTING); }
}
