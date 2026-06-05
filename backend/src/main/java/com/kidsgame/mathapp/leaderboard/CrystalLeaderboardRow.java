package com.kidsgame.mathapp.leaderboard;

public record CrystalLeaderboardRow(
        int rank,
        Long userId,
        String displayName,
        int crystals,
        int completedAttempts
) {
}
