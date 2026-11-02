package com.pelycan.api.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Maps Node-style {@code DATABASE_URL} to {@code PELYCAN_JDBC_URL} for Spring Boot.
 */
public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String SOURCE = "pelycanNormalizedDatabaseUrl";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String raw = environment.getProperty("DATABASE_URL");
        if (raw == null || raw.isBlank()) {
            return;
        }

        Map<String, Object> props = new HashMap<>();
        String trimmed = raw.trim();

        if (trimmed.startsWith("jdbc:")) {
            props.put("PELYCAN_JDBC_URL", trimmed);
        } else {
            props.putAll(parseNodeStyleUrl(trimmed));
        }

        if (!props.isEmpty()) {
            environment.getPropertySources().addFirst(new MapPropertySource(SOURCE, props));
        }
    }

    private static Map<String, Object> parseNodeStyleUrl(String raw) {
        String normalized = raw;
        if (normalized.startsWith("postgres://")) {
            normalized = "postgresql://" + normalized.substring("postgres://".length());
        }
        if (!normalized.startsWith("postgresql://")) {
            throw new IllegalStateException(
                    "DATABASE_URL must start with jdbc:, postgresql://, or postgres://");
        }

        URI uri = URI.create(normalized);
        StringBuilder jdbc = new StringBuilder("jdbc:postgresql://");
        jdbc.append(uri.getHost());
        if (uri.getPort() > 0) {
            jdbc.append(':').append(uri.getPort());
        }
        String path = uri.getPath();
        if (path != null && !path.isEmpty()) {
            jdbc.append(path);
        }

        Map<String, Object> props = new HashMap<>();
        props.put("PELYCAN_JDBC_URL", jdbc.toString());

        String userInfo = uri.getUserInfo();
        if (userInfo != null && !userInfo.isEmpty()) {
            int colon = userInfo.indexOf(':');
            if (colon >= 0) {
                props.put("DATABASE_USERNAME", decode(userInfo.substring(0, colon)));
                props.put("DATABASE_PASSWORD", decode(userInfo.substring(colon + 1)));
            } else {
                props.put("DATABASE_USERNAME", decode(userInfo));
            }
        }
        return props;
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
