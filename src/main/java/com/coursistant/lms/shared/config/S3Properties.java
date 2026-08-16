package com.coursistant.lms.shared.config;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Validated
@Component
@ConfigurationProperties(prefix = "aws.s3")
public class S3Properties {

    private boolean enabled = true;
    private String region = "us-west-2";
    private String bucket = "";
    private String endpointOverride = "";

    @PostConstruct
    void validateRequired() {
        if (!enabled) {
            throw new IllegalStateException("aws.s3.enabled=false is not allowed; S3 object storage is required");
        }
        if (isBlank(region)) {
            throw new IllegalStateException("aws.s3.region must be set when aws.s3.enabled=true");
        }
        if (isBlank(bucket)) {
            throw new IllegalStateException("aws.s3.bucket must be set when aws.s3.enabled=true");
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public String getEndpointOverride() {
        return endpointOverride;
    }

    public void setEndpointOverride(String endpointOverride) {
        this.endpointOverride = endpointOverride;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
