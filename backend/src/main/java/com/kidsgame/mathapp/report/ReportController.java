package com.kidsgame.mathapp.report;

import com.kidsgame.mathapp.auth.UserResponse;
import com.kidsgame.mathapp.auth.UserPrincipal;
import com.kidsgame.mathapp.quiz.QuizCategory;
import com.kidsgame.mathapp.quiz.QuizMode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@Validated
@RequestMapping("/api/reports")
public class ReportController {
    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/children")
    @PreAuthorize("hasRole('PARENT')")
    public List<UserResponse> children() {
        return reportService.children();
    }

    @GetMapping("/children/{childId}/attempts")
    @PreAuthorize("hasRole('PARENT')")
    public ChildReportResponse childReport(
            @PathVariable Long childId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) QuizCategory category,
            @RequestParam(required = false) QuizMode mode,
            @RequestParam(required = false) List<QuizMode> modes,
            @RequestParam(required = false) @Min(1) @Max(10) Integer difficulty
    ) {
        return reportService.childReport(childId, from, to, category, mode, modes, difficulty);
    }

    @GetMapping("/children/{childId}/attempts/{attemptId}")
    @PreAuthorize("hasRole('PARENT')")
    public ReportAttemptDetailResponse attemptDetail(@PathVariable Long childId, @PathVariable UUID attemptId) {
        return reportService.attemptDetail(childId, attemptId);
    }

    @GetMapping("/me/attempts")
    public ChildReportResponse myReport(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) QuizCategory category,
            @RequestParam(required = false) QuizMode mode,
            @RequestParam(required = false) List<QuizMode> modes,
            @RequestParam(required = false) @Min(1) @Max(10) Integer difficulty
    ) {
        return reportService.childReport(principal.id(), from, to, category, mode, modes, difficulty);
    }

    @GetMapping("/me/attempts/{attemptId}")
    public ReportAttemptDetailResponse myAttemptDetail(@AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID attemptId) {
        return reportService.attemptDetail(principal.id(), attemptId);
    }
}
