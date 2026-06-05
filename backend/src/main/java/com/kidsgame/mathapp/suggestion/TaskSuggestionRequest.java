package com.kidsgame.mathapp.suggestion;

import com.kidsgame.mathapp.quiz.QuizCategory;
import com.kidsgame.mathapp.quiz.QuizMode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TaskSuggestionRequest(
        QuizCategory category,
        QuizMode mode,
        @Min(1) @Max(10) Integer difficulty,
        @NotBlank @Size(max = 1500) String message
) {
}
