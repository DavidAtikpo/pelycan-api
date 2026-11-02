package com.pelycan.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pelycan.agora")
public record AgoraProperties(String appId, String appCertificate, int tokenTtlSeconds) {}
