package com.kidsgame.mathapp.quiz;

public record ScoreBreakdownResponse(
        QuizMode mode,
        int correct,
        int total
) {
}
