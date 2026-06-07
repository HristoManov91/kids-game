package com.kidsgame.mathapp.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlywayConfig {
    @Bean
    FlywayMigrationStrategy flywayMigrationStrategy(@Value("${spring.flyway.repair-on-start:false}") boolean repairOnStart) {
        return flyway -> {
            if (repairOnStart) {
                flyway.repair();
            }
            flyway.migrate();
        };
    }
}
