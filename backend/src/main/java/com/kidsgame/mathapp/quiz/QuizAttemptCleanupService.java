package com.kidsgame.mathapp.quiz;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Service
public class QuizAttemptCleanupService {
    private static final Duration ACTIVE_ATTEMPT_TTL = Duration.ofDays(3);

    private final QuizAttemptRepository attemptRepository;

    public QuizAttemptCleanupService(QuizAttemptRepository attemptRepository) {
        this.attemptRepository = attemptRepository;
    }

    @Scheduled(fixedDelay = 60 * 60 * 1000)
    @Transactional
    public void removeExpiredActiveAttemptsScheduled() {
        removeExpiredActiveAttempts();
    }

    @Transactional
    public long removeExpiredActiveAttempts() {
        return attemptRepository.deleteByStatusAndStartedAtBefore(
                AttemptStatus.ACTIVE,
                Instant.now().minus(ACTIVE_ATTEMPT_TTL)
        );
    }
}
