package com.kidsgame.mathapp.quiz;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, UUID> {
    List<QuizAttempt> findTop20ByUser_IdOrderByStartedAtDesc(Long userId);

    List<QuizAttempt> findByUser_IdAndStatusOrderByStartedAtDesc(Long userId, AttemptStatus status);

    List<QuizAttempt> findTop10ByUser_IdAndCategoryAndModeAndDifficultyAndStatusOrderByCompletedAtDesc(
            Long userId,
            QuizCategory category,
            QuizMode mode,
            int difficulty,
            AttemptStatus status
    );

    List<QuizAttempt> findByUser_IdAndStartedAtBetweenOrderByStartedAtDesc(Long userId, Instant from, Instant to);

    List<QuizAttempt> findByCategoryAndStatus(QuizCategory category, AttemptStatus status);

    List<QuizAttempt> findByStatusAndLeaderboardEligibleTrue(AttemptStatus status);

    long deleteByStatusAndStartedAtBefore(AttemptStatus status, Instant startedAt);

    List<QuizAttempt> findByCategoryAndModeAndDifficultyAndStatusAndLeaderboardEligibleTrue(
            QuizCategory category,
            QuizMode mode,
            int difficulty,
            AttemptStatus status
    );

    boolean existsByUser_IdAndCategoryAndModeAndDifficultyAndStatus(
            Long userId,
            QuizCategory category,
            QuizMode mode,
            int difficulty,
            AttemptStatus status
    );
}
