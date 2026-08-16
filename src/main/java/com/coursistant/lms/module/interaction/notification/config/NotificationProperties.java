package com.coursistant.lms.module.interaction.notification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "lms.notification")
public class NotificationProperties {

    private final Outbox outbox = new Outbox();
    private final Digest digest = new Digest();
    private final Email email = new Email();

    public Outbox getOutbox() {
        return outbox;
    }

    public Digest getDigest() {
        return digest;
    }

    public Email getEmail() {
        return email;
    }

    public static class Outbox {
        private boolean enabled = true;
        private long pollMs = 5000;
        private int batchSize = 100;
        private int maxAttempts = 8;
        private int recipientInsertChunk = 500;
        private int leaseSeconds = 120;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public long getPollMs() {
            return pollMs;
        }

        public void setPollMs(long pollMs) {
            this.pollMs = pollMs;
        }

        public int getBatchSize() {
            return batchSize;
        }

        public void setBatchSize(int batchSize) {
            this.batchSize = batchSize;
        }

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        public int getRecipientInsertChunk() {
            return recipientInsertChunk;
        }

        public void setRecipientInsertChunk(int recipientInsertChunk) {
            this.recipientInsertChunk = recipientInsertChunk;
        }

        public int getLeaseSeconds() {
            return leaseSeconds;
        }

        public void setLeaseSeconds(int leaseSeconds) {
            this.leaseSeconds = leaseSeconds;
        }
    }

    public static class Digest {
        private String defaultTimeZone = "America/Los_Angeles";
        private int batchSize = 200;
        private int leaseSeconds = 120;
        private int maxAttempts = 5;

        public String getDefaultTimeZone() {
            return defaultTimeZone;
        }

        public void setDefaultTimeZone(String defaultTimeZone) {
            this.defaultTimeZone = defaultTimeZone;
        }

        public int getBatchSize() {
            return batchSize;
        }

        public void setBatchSize(int batchSize) {
            this.batchSize = batchSize;
        }

        public int getLeaseSeconds() {
            return leaseSeconds;
        }

        public void setLeaseSeconds(int leaseSeconds) {
            this.leaseSeconds = leaseSeconds;
        }

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }
    }

    public static class Email {
        private boolean enabled = true;
        private String provider = "log";
        private String fromAddress = "do.not.reply@coursistant.com";
        private String fromName = "xLearn";
        private String baseUrl = "https://dev.xlearnedu.com";
        private int maxAttempts = 5;
        private int backoffBaseSeconds = 2;
        private long pollMs = 5000;
        private int batchSize = 50;
        private int leaseSeconds = 120;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public String getFromAddress() {
            return fromAddress;
        }

        public void setFromAddress(String fromAddress) {
            this.fromAddress = fromAddress;
        }

        public String getFromName() {
            return fromName;
        }

        public void setFromName(String fromName) {
            this.fromName = fromName;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        public int getBackoffBaseSeconds() {
            return backoffBaseSeconds;
        }

        public void setBackoffBaseSeconds(int backoffBaseSeconds) {
            this.backoffBaseSeconds = backoffBaseSeconds;
        }

        public long getPollMs() {
            return pollMs;
        }

        public void setPollMs(long pollMs) {
            this.pollMs = pollMs;
        }

        public int getBatchSize() {
            return batchSize;
        }

        public void setBatchSize(int batchSize) {
            this.batchSize = batchSize;
        }

        public int getLeaseSeconds() {
            return leaseSeconds;
        }

        public void setLeaseSeconds(int leaseSeconds) {
            this.leaseSeconds = leaseSeconds;
        }
    }
}
