package com.kidsgame.mathapp.issue;

import com.kidsgame.mathapp.quiz.QuizAttempt;
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
@Table(name = "question_issue_reports")
public class QuestionIssueReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attempt_id", nullable = false)
    private QuizAttempt attempt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(nullable = false)
    private int questionId;

    @Column(nullable = false, length = 1000)
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

    protected QuestionIssueReport() {
    }

    public QuestionIssueReport(QuizAttempt attempt, UserEntity user, int questionId, String message) {
        this.attempt = attempt;
        this.user = user;
        this.questionId = questionId;
        this.message = message;
        this.status = IssueReportStatus.OPEN;
        this.createdAt = Instant.now();
        this.updatedAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public QuizAttempt getAttempt() {
        return attempt;
    }

    public UserEntity getUser() {
        return user;
    }

    public int getQuestionId() {
        return questionId;
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
