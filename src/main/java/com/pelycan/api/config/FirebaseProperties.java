package com.pelycan.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pelycan.firebase")
public record FirebaseProperties(String credentialsPath, boolean adminNotifyEnabled) {}
