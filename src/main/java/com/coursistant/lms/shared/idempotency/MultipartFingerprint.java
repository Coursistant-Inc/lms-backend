package com.coursistant.lms.shared.idempotency;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Builds multipart fingerprint: sorted text parts + per-file index/metadata + SHA-256.
 * Must run before Redis claim; does not cache the raw multipart body.
 */
public final class MultipartFingerprint {

    private MultipartFingerprint() {
    }

    public static boolean isMultipart(HttpServletRequest request) {
        String ct = request.getContentType();
        return ct != null && ct.toLowerCase().startsWith("multipart/");
    }

    public static String compute(HttpServletRequest request) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update((request.getMethod() + ":" + request.getRequestURI() + ":").getBytes(StandardCharsets.UTF_8));

            if (!(request instanceof MultipartHttpServletRequest multipart)) {
                // Multipart not yet resolved — fingerprint only method+path (still requires Key).
                return HexFormat.of().formatHex(md.digest());
            }

            Map<String, String[]> params = new TreeMap<>(multipart.getParameterMap());
            for (Map.Entry<String, String[]> e : params.entrySet()) {
                md.update(e.getKey().getBytes(StandardCharsets.UTF_8));
                md.update((byte) '=');
                if (e.getValue() != null) {
                    for (String v : e.getValue()) {
                        if (v != null) {
                            md.update(v.getBytes(StandardCharsets.UTF_8));
                        }
                        md.update((byte) ',');
                    }
                }
                md.update((byte) ';');
            }

            List<MultipartFile> files = new ArrayList<>();
            for (List<MultipartFile> group : multipart.getMultiFileMap().values()) {
                if (group != null) {
                    files.addAll(group);
                }
            }
            md.update(("fileCount=" + files.size()).getBytes(StandardCharsets.UTF_8));
            for (int i = 0; i < files.size(); i++) {
                MultipartFile file = files.get(i);
                if (file == null || file.isEmpty()) {
                    continue;
                }
                String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
                String ct = file.getContentType() == null ? "" : file.getContentType();
                md.update(("idx=" + i + ";name=" + name + ";size=" + file.getSize() + ";ct=" + ct + ";")
                        .getBytes(StandardCharsets.UTF_8));
                MessageDigest fileMd = MessageDigest.getInstance("SHA-256");
                try (InputStream in = file.getInputStream()) {
                    byte[] buf = new byte[8192];
                    int n;
                    while ((n = in.read(buf)) >= 0) {
                        if (n > 0) {
                            fileMd.update(buf, 0, n);
                        }
                    }
                }
                md.update(fileMd.digest());
            }
            return HexFormat.of().formatHex(md.digest());
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute multipart fingerprint", e);
        }
    }
}
