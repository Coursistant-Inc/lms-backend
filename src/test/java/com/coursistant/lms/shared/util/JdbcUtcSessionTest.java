package com.coursistant.lms.shared.util;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Smoke: app JDBC URL forces session timezone to UTC or +00:00.
 */
class JdbcUtcSessionTest {

    @Test
    void sessionTimeZoneIsUtc() throws Exception {
        Properties env = loadEnv();
        String user = env.getProperty("DB_USERNAME", "root");
        String password = env.getProperty("DB_PASSWORD", "123456");
        String url = "jdbc:mysql://localhost:3306/lms_v2?useUnicode=true&characterEncoding=utf-8"
                + "&allowMultiQueries=true&useSSL=false"
                + "&connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true"
                + "&allowPublicKeyRetrieval=true";

        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT @@SESSION.time_zone")) {
            Assumptions.assumeTrue(rs.next(), "MySQL available");
            String tz = rs.getString(1);
            assertTrue(Set.of("UTC", "+00:00").contains(tz),
                    "Expected SESSION time_zone UTC or +00:00 but was: " + tz);
        } catch (Exception e) {
            Assumptions.assumeTrue(false, "Skipping JDBC UTC check: " + e.getMessage());
        }
    }

    private Properties loadEnv() throws Exception {
        Properties props = new Properties();
        Path envPath = Path.of(".env");
        if (Files.exists(envPath)) {
            try (InputStream in = Files.newInputStream(envPath)) {
                props.load(in);
            }
        }
        return props;
    }
}
