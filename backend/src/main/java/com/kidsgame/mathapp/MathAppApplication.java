package com.kidsgame.mathapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@SpringBootApplication
@EnableScheduling
public class MathAppApplication {
    public static void main(String[] args) {
        normalizeDatabaseUrl();
        SpringApplication.run(MathAppApplication.class, args);
    }

    static void normalizeDatabaseUrl() {
        String databaseUrl = firstPresent(System.getenv("DB_URL"), System.getenv("DATABASE_URL"));
        if (databaseUrl == null || databaseUrl.isBlank()) {
            return;
        }
        NormalizedDatabaseUrl normalized = normalizeDatabaseUrl(databaseUrl);
        if (normalized == null) {
            return;
        }
        System.setProperty("DB_URL", normalized.jdbcUrl());
        normalized.optionalUsername().ifPresent(username -> System.setProperty("DB_USERNAME", username));
        normalized.optionalPassword().ifPresent(password -> System.setProperty("DB_PASSWORD", password));
    }

    static NormalizedDatabaseUrl normalizeDatabaseUrl(String databaseUrl) {
        String trimmed = databaseUrl.trim();
        if (trimmed.startsWith("jdbc:")) {
            return new NormalizedDatabaseUrl(trimmed, null, null);
        }
        if (!trimmed.startsWith("postgresql://") && !trimmed.startsWith("postgres://")) {
            return null;
        }

        URI uri = URI.create(trimmed.replaceFirst("^postgres://", "postgresql://"));
        String path = uri.getRawPath();
        String jdbcUrl = "jdbc:postgresql://" + uri.getHost()
                + (uri.getPort() > -1 ? ":" + uri.getPort() : "")
                + (path == null || path.isBlank() ? "/" : path)
                + (uri.getRawQuery() == null || uri.getRawQuery().isBlank() ? "" : "?" + uri.getRawQuery());

        String username = null;
        String password = null;
        String userInfo = uri.getRawUserInfo();
        if (userInfo != null && !userInfo.isBlank()) {
            String[] parts = userInfo.split(":", 2);
            username = decode(parts[0]);
            if (parts.length > 1) {
                password = decode(parts[1]);
            }
        }
        return new NormalizedDatabaseUrl(jdbcUrl, username, password);
    }

    private static String firstPresent(String first, String second) {
        return first == null || first.isBlank() ? second : first;
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    record NormalizedDatabaseUrl(String jdbcUrl, String username, String password) {
        java.util.Optional<String> optionalUsername() {
            return java.util.Optional.ofNullable(username).filter(value -> !value.isBlank());
        }

        java.util.Optional<String> optionalPassword() {
            return java.util.Optional.ofNullable(password).filter(value -> !value.isBlank());
        }
    }
}
