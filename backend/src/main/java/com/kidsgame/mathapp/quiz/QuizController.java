package com.kidsgame.mathapp.quiz;

import com.kidsgame.mathapp.auth.UserPrincipal;
import com.kidsgame.mathapp.issue.QuestionIssueReportRequest;
import com.kidsgame.mathapp.issue.QuestionIssueReportResponse;
import com.kidsgame.mathapp.issue.QuestionIssueReportService;
import com.kidsgame.mathapp.leaderboard.CrystalLeaderboardRow;
import com.kidsgame.mathapp.leaderboard.LeaderboardRow;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.List;
import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/quizzes")
public class QuizController {
    private final QuizService quizService;
    private final QuestionIssueReportService issueReportService;

    public QuizController(QuizService quizService, QuestionIssueReportService issueReportService) {
        this.quizService = quizService;
        this.issueReportService = issueReportService;
    }

    @PostMapping("/start")
    public QuizAttemptResponse start(@Valid @RequestBody StartQuizRequest request, @AuthenticationPrincipal UserPrincipal principal) {
        return quizService.start(request, principal);
    }

    @GetMapping("/{attemptId}")
    public QuizAttemptResponse getAttempt(@PathVariable UUID attemptId, @AuthenticationPrincipal UserPrincipal principal) {
        return quizService.getAttempt(attemptId, principal);
    }

    @PostMapping("/{attemptId}/answer")
    public QuestionResultResponse answer(
            @PathVariable UUID attemptId,
            @Valid @RequestBody AnswerQuestionRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return quizService.answer(attemptId, request, principal);
    }

    @PostMapping("/{attemptId}/finish")
    public QuizSummaryResponse finish(@PathVariable UUID attemptId, @AuthenticationPrincipal UserPrincipal principal) {
        return quizService.finish(attemptId, principal);
    }

    @PostMapping("/{attemptId}/heartbeat")
    public QuizTimeResponse heartbeat(@PathVariable UUID attemptId, @AuthenticationPrincipal UserPrincipal principal) {
        return quizService.heartbeat(attemptId, principal);
    }

    @PostMapping("/{attemptId}/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@PathVariable UUID attemptId, @AuthenticationPrincipal UserPrincipal principal) {
        quizService.cancel(attemptId, principal);
    }

    @PostMapping("/{attemptId}/questions/{questionId}/issue-reports")
    @ResponseStatus(HttpStatus.CREATED)
    public QuestionIssueReportResponse reportQuestionIssue(
            @PathVariable UUID attemptId,
            @PathVariable int questionId,
            @Valid @RequestBody QuestionIssueReportRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return issueReportService.create(attemptId, questionId, request, principal);
    }

    @GetMapping("/active")
    public List<QuizAttemptResponse> activeAttempts(@AuthenticationPrincipal UserPrincipal principal) {
        return quizService.activeAttempts(principal);
    }

    @GetMapping("/history")
    public List<QuizAttemptResponse> history(@AuthenticationPrincipal UserPrincipal principal) {
        return quizService.history(principal);
    }

    @GetMapping("/recent")
    public List<RecentAttemptRow> recent(
            @RequestParam QuizCategory category,
            @RequestParam QuizMode mode,
            @RequestParam @Min(1) @Max(10) int difficulty,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return quizService.recentForSelection(principal, category, mode, difficulty);
    }

    @GetMapping("/crystals/total")
    public CrystalTotalResponse crystalTotal(@AuthenticationPrincipal UserPrincipal principal) {
        return quizService.crystalTotal(principal);
    }

    @GetMapping("/crystals/leaderboard")
    public List<CrystalLeaderboardRow> crystalLeaderboard() {
        return quizService.crystalLeaderboard();
    }

    @GetMapping("/leaderboard")
    public List<LeaderboardRow> leaderboard(
            @RequestParam QuizCategory category,
            @RequestParam QuizMode mode,
            @RequestParam @Min(1) @Max(10) int difficulty
    ) {
        return quizService.leaderboard(category, mode, difficulty);
    }
}
