package com.coursistant.lms.service.system;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.web.multipart.MultipartFile;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.UploadObjectArgs;
import io.minio.errors.MinioException;

public class MinIOService {


    private final MinioClient minioClient;

    public MinIOService(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    // CREATE
    public void uploadFile(String fileName, MultipartFile file, String bucket) throws Exception {
        boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
        if (!found) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        }

        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucket)
                        .object(fileName)
                        .stream(file.getInputStream(), file.getSize(), -1)
                        .contentType(file.getContentType())
                        .build()
        );
    }

    // READ
    public InputStream downloadFile(String fileName, String bucket) throws Exception {
        return minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucket)
                        .object(fileName)
                        .build()
        );
    }

    // UPDATE
    public void updateFile(String fileName, MultipartFile file, String bucket) throws Exception {
        deleteFile(fileName, bucket); // optional
        uploadFile(fileName, file, bucket);
    }

    // DELETE
    public void deleteFile(String fileName, String bucket) throws Exception {
        minioClient.removeObject(
                RemoveObjectArgs.builder()
                        .bucket(bucket)
                        .object(fileName)
                        .build()
        );
    }

}
