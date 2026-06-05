package com.kidsgame.mathapp.quiz;

import java.time.Instant;

public record AnswerRecord(
        int questionId,
        String answer,
        boolean correct,
        String correctAnswer,
        Instant answeredAt
) {
}

