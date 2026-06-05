package com.kidsgame.mathapp.catalog;

import com.kidsgame.mathapp.quiz.QuizCategory;

import java.util.List;

public record WordCatalogSuggestionResponse(
        QuizCategory category,
        String word,
        List<String> letters,
        List<String> suggestedSyllables,
        int suggestedDifficulty
) {
}
