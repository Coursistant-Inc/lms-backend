package com.coursistant.lms.module.file.storage;

import com.coursistant.lms.shared.config.S3Properties;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.MetadataDirective;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

@Service
public class S3ObjectStorage {

    static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

    private final S3Client s3Client;
    private final S3Properties properties;

    public S3ObjectStorage(S3Client s3Client, S3Properties properties) {
        this.s3Client = s3Client;
        this.properties = properties;
    }

    public void putObject(String key, InputStream inputStream, long contentLength, String contentType) {
        requireKey(key);
        if (inputStream == null) {
            throw new IllegalArgumentException("inputStream must not be null");
        }
        if (contentLength < 0) {
            throw new IllegalArgumentException("contentLength must be non-negative");
        }
        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket())
                            .key(key)
                            .contentType(resolveContentType(contentType))
                            .contentLength(contentLength)
                            .build(),
                    RequestBody.fromInputStream(inputStream, contentLength));
        } catch (RuntimeException e) {
            throw wrap(e, "putObject failed for key " + key);
        }
    }

    public void putObject(String key, MultipartFile file, String canonicalMime) {
        requireKey(key);
        if (file == null) {
            throw new IllegalArgumentException("file must not be null");
        }
        try (InputStream in = file.getInputStream()) {
            putObject(key, in, file.getSize(), canonicalMime);
        } catch (IOException e) {
            throw new S3StorageException("Failed to read multipart file for key " + key, e);
        }
    }

    public S3ObjectPayload getObject(String key) {
        requireKey(key);
        try {
            ResponseInputStream<GetObjectResponse> stream = s3Client.getObject(
                    GetObjectRequest.builder()
                            .bucket(bucket())
                            .key(key)
                            .build());
            GetObjectResponse response = stream.response();
            return new S3ObjectPayload(stream, toMetadata(response.contentType(), response.contentLength(), response.eTag()));
        } catch (NoSuchKeyException e) {
            throw new S3ObjectNotFoundException(key, e);
        } catch (S3Exception e) {
            if (isNotFound(e)) {
                throw new S3ObjectNotFoundException(key, e);
            }
            throw wrap(e, "getObject failed for key " + key);
        } catch (RuntimeException e) {
            throw wrap(e, "getObject failed for key " + key);
        }
    }

    public void deleteObject(String key) {
        requireKey(key);
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucket())
                    .key(key)
                    .build());
        } catch (RuntimeException e) {
            throw wrap(e, "deleteObject failed for key " + key);
        }
    }

    public void copyObject(String sourceKey, String destinationKey) {
        requireKey(sourceKey);
        requireKey(destinationKey);
        try {
            s3Client.copyObject(CopyObjectRequest.builder()
                    .sourceBucket(bucket())
                    .sourceKey(sourceKey)
                    .destinationBucket(bucket())
                    .destinationKey(destinationKey)
                    .metadataDirective(MetadataDirective.COPY)
                    .build());
        } catch (RuntimeException e) {
            throw wrap(e, "copyObject failed from " + sourceKey + " to " + destinationKey);
        }
    }

    public Optional<S3ObjectMetadata> headObject(String key) {
        requireKey(key);
        try {
            HeadObjectResponse response = s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(bucket())
                    .key(key)
                    .build());
            return Optional.of(toMetadata(response.contentType(), response.contentLength(), response.eTag()));
        } catch (NoSuchKeyException e) {
            return Optional.empty();
        } catch (S3Exception e) {
            if (isNotFound(e)) {
                return Optional.empty();
            }
            throw wrap(e, "headObject failed for key " + key);
        } catch (RuntimeException e) {
            throw wrap(e, "headObject failed for key " + key);
        }
    }

    String bucket() {
        return properties.getBucket();
    }

    private static void requireKey(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("object key must not be blank");
        }
    }

    private static String resolveContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return DEFAULT_CONTENT_TYPE;
        }
        return contentType;
    }

    private static S3ObjectMetadata toMetadata(String contentType, Long contentLength, String etag) {
        return new S3ObjectMetadata(contentType, contentLength, etag);
    }

    private static boolean isNotFound(S3Exception e) {
        return e.statusCode() == 404 || "NoSuchKey".equals(e.awsErrorDetails() != null
                ? e.awsErrorDetails().errorCode()
                : null);
    }

    private static RuntimeException wrap(RuntimeException e, String message) {
        if (e instanceof S3StorageException storage) {
            return storage;
        }
        return new S3StorageException(message, e);
    }
}
