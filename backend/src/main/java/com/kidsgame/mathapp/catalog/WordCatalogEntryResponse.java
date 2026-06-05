package com.kidsgame.mathapp.catalog;

import com.kidsgame.mathapp.quiz.QuizCategory;

import java.time.Instant;
import java.util.List;

public record WordCatalogEntryResponse(
        Long id,
        QuizCategory category,
        String word,
        String image,
        List<String> letters,
        List<String> syllables,
        int difficulty,
        boolean active,
        Instant createdAt,
        Instant updatedAt,
        long usedCount,
        long correctCount,
        long wrongCount
) {
}
