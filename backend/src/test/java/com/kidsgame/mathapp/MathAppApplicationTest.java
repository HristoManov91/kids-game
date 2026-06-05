package com.kidsgame.mathapp;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MathAppApplicationTest {
    @Test
    void keepsJdbcDatabaseUrlUnchanged() {
        var normalized = MathAppApplication.normalizeDatabaseUrl("jdbc:postgresql://localhost:5432/kids_game");

        assertThat(normalized).isNotNull();
        assertThat(normalized.jdbcUrl()).isEqualTo("jdbc:postgresql://localhost:5432/kids_game");
        assertThat(normalized.optionalUsername()).isEmpty();
        assertThat(normalized.optionalPassword()).isEmpty();
    }

    @Test
    void convertsPostgresUrlToJdbcUrlAndCredentials() {
        var normalized = MathAppApplication.normalizeDatabaseUrl(
                "postgresql://kids_user:p%40ssword@host.example.com:5432/kids_game?sslmode=require"
        );

        assertThat(normalized).isNotNull();
        assertThat(normalized.jdbcUrl()).isEqualTo("jdbc:postgresql://host.example.com:5432/kids_game?sslmode=require");
        assertThat(normalized.optionalUsername()).contains("kids_user");
        assertThat(normalized.optionalPassword()).contains("p@ssword");
    }

    @Test
    void convertsRenderPostgresUrlAlias() {
        var normalized = MathAppApplication.normalizeDatabaseUrl("postgres://user:secret@render.internal/db");

        assertThat(normalized).isNotNull();
        assertThat(normalized.jdbcUrl()).isEqualTo("jdbc:postgresql://render.internal/db");
        assertThat(normalized.optionalUsername()).contains("user");
        assertThat(normalized.optionalPassword()).contains("secret");
    }
}
