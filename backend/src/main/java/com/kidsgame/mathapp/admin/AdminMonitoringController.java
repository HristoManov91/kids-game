package com.kidsgame.mathapp.admin;

import com.kidsgame.mathapp.issue.QuestionIssueReportResponse;
import com.kidsgame.mathapp.issue.QuestionIssueReportService;
import com.kidsgame.mathapp.issue.UpdateQuestionIssueReportRequest;
import com.kidsgame.mathapp.suggestion.TaskSuggestionResponse;
import com.kidsgame.mathapp.suggestion.TaskSuggestionService;
import com.kidsgame.mathapp.suggestion.UpdateTaskSuggestionRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/monitoring")
@PreAuthorize("@adminAccess.isAdmin(authentication)")
public class AdminMonitoringController {
    private final AdminMonitoringService service;
    private final AdminAttemptService attemptService;
    private final QuestionIssueReportService issueReportService;
    private final TaskSuggestionService suggestionService;

    public AdminMonitoringController(
            AdminMonitoringService service,
            AdminAttemptService attemptService,
            QuestionIssueReportService issueReportService,
            TaskSuggestionService suggestionService
    ) {
        this.service = service;
        this.attemptService = attemptService;
        this.issueReportService = issueReportService;
        this.suggestionService = suggestionService;
    }

    @GetMapping
    public AdminMonitoringResponse snapshot(
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to
    ) {
        return service.snapshot(from, to);
    }

    @GetMapping("/attempts/{attemptId}")
    public AdminAttemptDetailResponse attemptDetail(@PathVariable UUID attemptId) {
        return attemptService.detail(attemptId);
    }

    @PutMapping("/attempts/{attemptId}/answers/{questionId}")
    public AdminAttemptDetailResponse updateAnswer(
            @PathVariable UUID attemptId,
            @PathVariable int questionId,
            @Valid @RequestBody AdminAnswerUpdateRequest request
    ) {
        return attemptService.updateAnswer(attemptId, questionId, request);
    }

    @PutMapping("/issue-reports/{reportId}")
    public QuestionIssueReportResponse updateIssueReport(
            @PathVariable Long reportId,
            @Valid @RequestBody UpdateQuestionIssueReportRequest request
    ) {
        return issueReportService.update(reportId, request);
    }

    @PutMapping("/suggestions/{suggestionId}")
    public TaskSuggestionResponse updateSuggestion(
            @PathVariable Long suggestionId,
            @Valid @RequestBody UpdateTaskSuggestionRequest request
    ) {
        return suggestionService.update(suggestionId, request);
    }
}
