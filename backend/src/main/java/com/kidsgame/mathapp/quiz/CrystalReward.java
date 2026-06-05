package com.kidsgame.mathapp.quiz;

public final class CrystalReward {
    private static final int BASELINE_QUESTION_COUNT = 10;

    private CrystalReward() {
    }

    public static int calculate(int difficulty, int score, int total) {
        int level = Math.max(1, difficulty);
        int levelBonus = level * 5;
        if (total <= 0) {
            return levelBonus;
        }
        int safeScore = Math.max(0, Math.min(score, total));
        int normalizedCorrect = (int) Math.round((safeScore / (double) total) * BASELINE_QUESTION_COUNT);
        return levelBonus + normalizedCorrect * 2;
    }

    public static Integer calculate(int difficulty, Integer score, int total) {
        if (score == null) {
            return null;
        }
        return calculate(difficulty, score.intValue(), total);
    }
}
