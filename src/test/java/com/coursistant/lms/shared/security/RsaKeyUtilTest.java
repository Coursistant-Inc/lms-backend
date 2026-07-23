package com.coursistant.lms.shared.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import static org.junit.jupiter.api.Assertions.*;

class RsaKeyUtilTest {

    @Test
    void loadPrivateKey_fromClasspath_success() throws Exception {
        PrivateKey key = RsaKeyUtil.loadPrivateKey("classpath:test-private.pem");
        assertInstanceOf(RSAPrivateKey.class, key);
        assertEquals("RSA", key.getAlgorithm());
    }

    @Test
    void loadPublicKey_fromClasspath_success() throws Exception {
        PublicKey key = RsaKeyUtil.loadPublicKey("classpath:test-public.pem");
        assertInstanceOf(RSAPublicKey.class, key);
        assertEquals("RSA", key.getAlgorithm());
    }

    @Test
    void loadPrivateKey_fromFilesystem_success(@TempDir Path tempDir) throws Exception {
        Path source = Path.of("src/test/resources/test-private.pem");
        Path dest = tempDir.resolve("private.pem");
        Files.copy(source, dest);

        PrivateKey key = RsaKeyUtil.loadPrivateKey(dest.toAbsolutePath().toString());
        assertInstanceOf(RSAPrivateKey.class, key);
    }

    @Test
    void loadKey_classpathNotFound_throwsException() {
        Exception ex = assertThrows(IllegalArgumentException.class,
                () -> RsaKeyUtil.loadPrivateKey("classpath:nonexistent-key.pem"));
        assertTrue(ex.getMessage().contains("Classpath resource not found"));
    }
}
