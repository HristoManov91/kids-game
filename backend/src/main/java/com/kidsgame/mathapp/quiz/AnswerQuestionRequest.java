package com.kidsgame.mathapp.quiz;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record AnswerQuestionRequest(
        @Positive int questionId,
        @NotBlank String answer
) {
}

