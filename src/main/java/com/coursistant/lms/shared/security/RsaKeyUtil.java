package com.coursistant.lms.shared.security;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class RsaKeyUtil {

    private static final String CLASSPATH_PREFIX = "classpath:";

    public static PrivateKey loadPrivateKey(String path) throws Exception {
        String key = readKeyContent(path)
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] keyBytes = Base64.getDecoder().decode(key);
        return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
    }

    public static PublicKey loadPublicKey(String path) throws Exception {
        String key = readKeyContent(path)
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        byte[] keyBytes = Base64.getDecoder().decode(key);
        return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(keyBytes));
    }

    private static String readKeyContent(String path) throws Exception {
        if (path.startsWith(CLASSPATH_PREFIX)) {
            String resource = path.substring(CLASSPATH_PREFIX.length());
            try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource)) {
                if (is == null) {
                    throw new IllegalArgumentException("Classpath resource not found: " + resource);
                }
                return new String(is.readAllBytes());
            }
        }
        return new String(Files.readAllBytes(Paths.get(path)));
    }
}
