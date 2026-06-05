package com.kidsgame.mathapp.catalog;

import com.kidsgame.mathapp.quiz.QuizCategory;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record WordCatalogUpsertRequest(
        @NotNull QuizCategory category,
        @NotBlank @Size(max = 120) String word,
        @NotBlank @Size(max = 40) String image,
        List<@NotBlank @Size(max = 20) String> syllables,
        @Min(1) @Max(10) int difficulty,
        boolean active
) {
}
