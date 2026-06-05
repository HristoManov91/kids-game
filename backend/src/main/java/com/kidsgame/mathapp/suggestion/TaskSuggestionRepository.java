package com.kidsgame.mathapp.suggestion;

import com.kidsgame.mathapp.issue.IssueReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskSuggestionRepository extends JpaRepository<TaskSuggestion, Long> {
    long countByStatus(IssueReportStatus status);

    List<TaskSuggestion> findTop50ByOrderByCreatedAtDesc();
}
