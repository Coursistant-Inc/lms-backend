package com.coursistant.lms.module.file.storage;

import com.coursistant.lms.shared.config.S3Properties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfEnvironmentVariable(named = "RUN_S3_SMOKE", matches = "true")
class S3ObjectStorageSmokeTest {

    private S3Client client;
    private S3ObjectStorage storage;
    private String sourceKey;
    private String copyKey;

    @BeforeEach
    void setUp() {
        String region = required("AWS_S3_REGION");
        String bucket = required("AWS_S3_BUCKET");
        S3Properties properties = new S3Properties();
        properties.setEnabled(true);
        properties.setRegion(region);
        properties.setBucket(bucket);
        client = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
        storage = new S3ObjectStorage(client, properties);
        String prefix = "_s3-verification/" + UUID.randomUUID();
        sourceKey = prefix + "/source.txt";
        copyKey = prefix + "/copy.txt";
    }

    @AfterEach
    void tearDown() {
        try {
            if (storage != null) {
                storage.deleteObject(sourceKey);
                storage.deleteObject(copyKey);
            }
        } finally {
            if (client != null) {
                client.close();
            }
        }
    }

    @Test
    void putHeadGetCopyDelete_roundTrip() throws Exception {
        byte[] body = ("smoke-" + UUID.randomUUID()).getBytes(StandardCharsets.UTF_8);
        storage.putObject(sourceKey, new ByteArrayInputStream(body), body.length, "text/plain");

        S3ObjectMetadata head = storage.headObject(sourceKey).orElseThrow();
        assertEquals(body.length, head.contentLength());
        assertEquals("text/plain", head.contentType());

        try (S3ObjectPayload payload = storage.getObject(sourceKey)) {
            assertArrayEquals(body, payload.content().readAllBytes());
            assertEquals("text/plain", payload.metadata().contentType());
        }

        storage.copyObject(sourceKey, copyKey);
        S3ObjectMetadata copyHead = storage.headObject(copyKey).orElseThrow();
        assertEquals("text/plain", copyHead.contentType());
        try (S3ObjectPayload copyPayload = storage.getObject(copyKey)) {
            assertArrayEquals(body, copyPayload.content().readAllBytes());
        }

        storage.deleteObject(sourceKey);
        storage.deleteObject(copyKey);
        assertTrue(storage.headObject(sourceKey).isEmpty());
        assertTrue(storage.headObject(copyKey).isEmpty());
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " must be set for S3 smoke test");
        }
        return value;
    }
}
