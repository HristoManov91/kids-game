package com.kidsgame.mathapp.suggestion;

import com.kidsgame.mathapp.issue.IssueReportStatus;
import com.kidsgame.mathapp.quiz.QuizCategory;
import com.kidsgame.mathapp.quiz.QuizMode;
import com.kidsgame.mathapp.user.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "task_suggestions")
public class TaskSuggestion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private QuizCategory category;

    @Enumerated(EnumType.STRING)
    @Column(length = 60)
    private QuizMode mode;

    private Integer difficulty;

    @Column(nullable = false, length = 1500)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private IssueReportStatus status;

    @Column(length = 1000)
    private String adminNote;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    private Instant resolvedAt;

    protected TaskSuggestion() {
    }

    public TaskSuggestion(UserEntity user, QuizCategory category, QuizMode mode, Integer difficulty, String message) {
        this.user = user;
        this.category = category;
        this.mode = mode;
        this.difficulty = difficulty;
        this.message = message;
        this.status = IssueReportStatus.OPEN;
        this.createdAt = Instant.now();
        this.updatedAt = createdAt;
    }

    public Long getId() {
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

    public Integer getDifficulty() {
        return difficulty;
    }

    public String getMessage() {
        return message;
    }

    public IssueReportStatus getStatus() {
        return status;
    }

    public String getAdminNote() {
        return adminNote;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public void update(IssueReportStatus status, String adminNote) {
        this.status = status;
        this.adminNote = adminNote;
        this.updatedAt = Instant.now();
        this.resolvedAt = status == IssueReportStatus.OPEN ? null : updatedAt;
    }
}
