package com.kidsgame.mathapp.reward;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RewardCatalogTest {

    @Test
    void superheroCatalogContainsRequestedHeroes() {
        RewardCatalog catalog = new RewardCatalog();

        assertThat(catalog.theme("superheroes").name()).isEqualTo("Супергеройска вселена");
        assertThat(catalog.itemsForTheme("superheroes"))
                .extracting(RewardCatalogItemResponse::name)
                .contains("Капитан Америка", "Железният човек", "Тор", "Хълк");
    }
}
