package com.kidsgame.mathapp.catalog;

import com.kidsgame.mathapp.quiz.QuizCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record WordCatalogSuggestionRequest(
        @NotNull QuizCategory category,
        @NotBlank @Size(max = 120) String word
) {
}
