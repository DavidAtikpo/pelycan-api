package com.pelycan.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pelycan.speech")
public record SpeechProperties(String apiKey, String defaultLanguage) {}
