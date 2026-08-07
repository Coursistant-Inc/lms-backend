package com.coursistant.lms.module.file.service;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import io.minio.BucketExistsArgs;
import io.minio.CopyObjectArgs;
import io.minio.CopySource;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;

@Service
public class MinIOService {

    private final MinioClient minioClient;

    public MinIOService(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    // CREATE
    public void uploadFile(String fileDest, MultipartFile file, String bucket) throws Exception {
        boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
        if (!found) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        }

        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucket)
                        .object(fileDest)
                        .stream(file.getInputStream(), file.getSize(), -1)
                        .contentType(file.getContentType())
                        .build()
        );
    }

    // READ
    public InputStream downloadFile(String fileDest, String bucket) throws Exception {
        return minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucket)
                        .object(fileDest)
                        .build()
        );
    }

    // UPDATE
    public void updateFile(String fileDest, MultipartFile file, String bucket) throws Exception {
        deleteFile(fileDest, bucket); // optional
        uploadFile(fileDest, file, bucket);
    }

    // DELETE
    public void deleteFile(String fileDest, String bucket) throws Exception {
        minioClient.removeObject(
                RemoveObjectArgs.builder()
                        .bucket(bucket)
                        .object(fileDest)
                        .build()
        );
    }

    public void copyObject(String bucket, String sourceKey, String destKey) throws Exception {
        minioClient.copyObject(
                CopyObjectArgs.builder()
                        .bucket(bucket)
                        .object(destKey)
                        .source(CopySource.builder().bucket(bucket).object(sourceKey).build())
                        .build()
        );
    }

}
