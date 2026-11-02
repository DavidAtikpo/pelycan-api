package com.pelycan.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pelycan.aws.s3")
public record S3Properties(String bucket, int presignDurationMinutes) {}
