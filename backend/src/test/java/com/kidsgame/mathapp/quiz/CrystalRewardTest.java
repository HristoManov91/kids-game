package com.kidsgame.mathapp.quiz;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CrystalRewardTest {
    @Test
    void usesBaseFormulaForTenQuestionScale() {
        assertThat(CrystalReward.calculate(3, 7, 10)).isEqualTo(29);
    }

    @Test
    void normalizesShortAndLongTestsToSameBonusRange() {
        assertThat(CrystalReward.calculate(2, 5, 5)).isEqualTo(30);
        assertThat(CrystalReward.calculate(2, 20, 20)).isEqualTo(30);
        assertThat(CrystalReward.calculate(2, 0, 5)).isEqualTo(10);
    }

    @Test
    void clampsImpossibleScores() {
        assertThat(CrystalReward.calculate(1, 99, 5)).isEqualTo(25);
        assertThat(CrystalReward.calculate(1, -2, 5)).isEqualTo(5);
    }

    @Test
    void acceptsNullableScoresFromStoredAttempts() {
        Integer score = 4;

        assertThat(CrystalReward.calculate(1, score, 5)).isEqualTo(21);
        assertThat(CrystalReward.calculate(1, null, 5)).isNull();
    }
}
