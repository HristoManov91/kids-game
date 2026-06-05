package com.kidsgame.mathapp.issue;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface QuestionIssueReportRepository extends JpaRepository<QuestionIssueReport, Long> {
    long countByStatus(IssueReportStatus status);

    List<QuestionIssueReport> findTop30ByOrderByCreatedAtDesc();

    List<QuestionIssueReport> findByAttempt_IdOrderByCreatedAtAsc(UUID attemptId);
}
