package com.coursistant.lms.module.file.storage;

public class S3ObjectNotFoundException extends S3StorageException {

    public S3ObjectNotFoundException(String key) {
        super("S3 object not found: " + key);
    }

    public S3ObjectNotFoundException(String key, Throwable cause) {
        super("S3 object not found: " + key, cause);
    }
}
