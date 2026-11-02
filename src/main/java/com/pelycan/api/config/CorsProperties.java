package com.pelycan.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pelycan.cors")
public record CorsProperties(String allowedOrigins) {}
