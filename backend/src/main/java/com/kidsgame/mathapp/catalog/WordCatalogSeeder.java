package com.kidsgame.mathapp.catalog;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(10)
public class WordCatalogSeeder implements ApplicationRunner {
    private final WordCatalogService service;

    public WordCatalogSeeder(WordCatalogService service) {
        this.service = service;
    }

    @Override
    public void run(ApplicationArguments args) {
        service.seedDefaults();
    }
}
