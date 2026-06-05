package com.kidsgame.mathapp.quiz;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record StartQuizRequest(
        @NotNull QuizCategory category,
        @NotNull QuizMode mode,
        @Min(1) @Max(10) int difficulty,
        List<QuizMode> includedModes
) {
}
