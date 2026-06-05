package com.kidsgame.mathapp.quiz;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record QuizSummaryResponse(
        UUID attemptId,
        QuizCategory category,
        QuizMode mode,
        List<QuizMode> includedModes,
        int difficulty,
        boolean leaderboardEligible,
        int score,
        int totalQuestions,
        BigDecimal grade,
        Medal medal,
        int crystals,
        Instant completedAt,
        long durationSeconds,
        List<QuestionResultResponse> results,
        List<ScoreBreakdownResponse> scoreBreakdown
) {
}
