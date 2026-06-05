package com.kidsgame.mathapp.leaderboard;

import com.kidsgame.mathapp.quiz.Medal;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record LeaderboardRow(
        int rank,
        Long userId,
        String displayName,
        UUID attemptId,
        int score,
        int totalQuestions,
        BigDecimal grade,
        Medal medal,
        int crystals,
        long durationSeconds,
        Instant completedAt
) {
}
