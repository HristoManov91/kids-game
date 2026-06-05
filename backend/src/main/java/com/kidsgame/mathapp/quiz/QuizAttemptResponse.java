package com.kidsgame.mathapp.quiz;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record QuizAttemptResponse(
        UUID attemptId,
        QuizCategory category,
        QuizMode mode,
        List<QuizMode> includedModes,
        int difficulty,
        boolean leaderboardEligible,
        AttemptStatus status,
        List<QuestionResponse> questions,
        List<QuestionResultResponse> answers,
        Integer score,
        int totalQuestions,
        BigDecimal grade,
        Medal medal,
        Integer crystals,
        Instant startedAt,
        Instant completedAt,
        long durationSeconds,
        List<ScoreBreakdownResponse> scoreBreakdown
) {
}
