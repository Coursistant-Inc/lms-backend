package com.coursistant.lms.module.auth.it.support;

import com.coursistant.lms.module.auth.support.FakeEmailCapture;
import com.coursistant.lms.shared.util.EmailUtil;
import io.minio.MinioClient;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

@TestConfiguration
@Profile("auth-it")
public class AuthItTestConfiguration {

    public static final Instant FIXED_INSTANT = Instant.parse("2026-07-28T12:00:00Z");

    @Bean
    @Primary
    public FakeEmailCapture fakeEmailCapture() {
        return new FakeEmailCapture();
    }

    @Bean
    @Primary
    public JavaMailSender javaMailSender() {
        return Mockito.mock(JavaMailSender.class);
    }

    @Bean
    @Primary
    public EmailUtil emailUtil(FakeEmailCapture capture) {
        return new EmailUtil() {
            @Override
            public void sendEmail(String to, String subject, String content) {
                capture.record(to, subject, content);
            }
        };
    }

    @Bean
    @Primary
    public MinioClient minioClient() {
        return Mockito.mock(MinioClient.class);
    }

    @Bean
    @Primary
    public MutableClock mutableClock() {
        return new MutableClock(FIXED_INSTANT);
    }

    @Bean
    @Primary
    public Clock clock(MutableClock mutableClock) {
        return mutableClock;
    }

    /**
     * Mutable UTC clock for grace / lock expiry without wall-clock waits.
     */
    public static final class MutableClock extends Clock {
        private final AtomicReference<Instant> instant;
        private final ZoneOffset zone = ZoneOffset.UTC;

        public MutableClock(Instant initial) {
            this.instant = new AtomicReference<>(initial);
        }

        public void setInstant(Instant next) {
            instant.set(next);
        }

        public void advanceSeconds(long seconds) {
            instant.updateAndGet(i -> i.plusSeconds(seconds));
        }

        @Override
        public ZoneOffset getZone() {
            return zone;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant.get();
        }
    }
}
