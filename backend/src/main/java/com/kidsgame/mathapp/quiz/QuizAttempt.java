package com.kidsgame.mathapp.quiz;

import com.kidsgame.mathapp.user.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "quiz_attempts")
public class QuizAttempt {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private QuizCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 60)
    private QuizMode mode;

    @Column(nullable = false)
    private int difficulty;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AttemptStatus status;

    @Column(nullable = false, columnDefinition = "text")
    private String questionsJson;

    @Column(nullable = false, columnDefinition = "text")
    private String answersJson;

    @Column(nullable = false, columnDefinition = "text")
    private String includedModesJson;

    @Column(nullable = false)
    private boolean leaderboardEligible;

    private Integer score;

    @Column(nullable = false)
    private int totalQuestions;

    private BigDecimal grade;

    @Enumerated(EnumType.STRING)
    @Column(length = 60)
    private Medal medal;

    @Column(nullable = false)
    private Instant startedAt;

    private Instant completedAt;

    @Column(nullable = false)
    private long activeDurationSeconds;

    private Instant lastHeartbeatAt;

    protected QuizAttempt() {
    }

    public QuizAttempt(
            UserEntity user,
            QuizCategory category,
            QuizMode mode,
            int difficulty,
            String questionsJson,
            String includedModesJson,
            boolean leaderboardEligible,
            int totalQuestions
    ) {
        this.id = UUID.randomUUID();
        this.user = user;
        this.category = category;
        this.mode = mode;
        this.difficulty = difficulty;
        this.status = AttemptStatus.ACTIVE;
        this.questionsJson = questionsJson;
        this.answersJson = "[]";
        this.includedModesJson = includedModesJson;
        this.leaderboardEligible = leaderboardEligible;
        this.totalQuestions = totalQuestions;
        this.startedAt = Instant.now();
        this.activeDurationSeconds = 0;
    }

    public UUID getId() {
        return id;
    }

    public UserEntity getUser() {
        return user;
    }

    public QuizCategory getCategory() {
        return category;
    }

    public QuizMode getMode() {
        return mode;
    }

    public int getDifficulty() {
        return difficulty;
    }

    public AttemptStatus getStatus() {
        return status;
    }

    public String getQuestionsJson() {
        return questionsJson;
    }

    public String getAnswersJson() {
        return answersJson;
    }

    public String getIncludedModesJson() {
        return includedModesJson;
    }

    public boolean isLeaderboardEligible() {
        return leaderboardEligible;
    }

    public Integer getScore() {
        return score;
    }

    public int getTotalQuestions() {
        return totalQuestions;
    }

    public BigDecimal getGrade() {
        return grade;
    }

    public Medal getMedal() {
        return medal;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public long getActiveDurationSeconds() {
        return activeDurationSeconds;
    }

    public Instant getLastHeartbeatAt() {
        return lastHeartbeatAt;
    }

    public void replaceAnswers(String answersJson) {
        this.answersJson = answersJson;
    }

    public void recordActivity(Instant now, long maxGapSeconds) {
        if (status != AttemptStatus.ACTIVE) {
            return;
        }
        if (lastHeartbeatAt != null) {
            long seconds = java.time.Duration.between(lastHeartbeatAt, now).toSeconds();
            if (seconds > 0 && seconds <= maxGapSeconds) {
                activeDurationSeconds += seconds;
            }
        }
        lastHeartbeatAt = now;
    }

    public void complete(int score, BigDecimal grade, Medal medal, String answersJson) {
        this.status = AttemptStatus.COMPLETED;
        this.score = score;
        this.grade = grade;
        this.medal = medal;
        this.answersJson = answersJson;
        this.completedAt = Instant.now();
    }

    public void replaceGradedAnswers(int score, BigDecimal grade, Medal medal, String answersJson) {
        this.score = score;
        this.grade = grade;
        this.medal = medal;
        this.answersJson = answersJson;
    }

    public void markDemoAttempt(Instant startedAt, Instant completedAt, long activeDurationSeconds) {
        this.leaderboardEligible = false;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.activeDurationSeconds = Math.max(0, activeDurationSeconds);
        this.lastHeartbeatAt = completedAt;
    }
}
