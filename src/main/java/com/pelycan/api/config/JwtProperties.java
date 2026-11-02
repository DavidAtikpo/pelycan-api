package com.pelycan.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pelycan.jwt")
public record JwtProperties(String secret, int expirationHours) {}
