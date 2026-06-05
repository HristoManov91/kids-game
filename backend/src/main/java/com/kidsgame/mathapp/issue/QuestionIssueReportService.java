package com.kidsgame.mathapp.issue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kidsgame.mathapp.auth.UserPrincipal;
import com.kidsgame.mathapp.quiz.GeneratedQuestion;
import com.kidsgame.mathapp.quiz.QuizAttempt;
import com.kidsgame.mathapp.quiz.QuizAttemptRepository;
import com.kidsgame.mathapp.user.UserEntity;
import com.kidsgame.mathapp.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class QuestionIssueReportService {
    private static final TypeReference<List<GeneratedQuestion>> QUESTION_LIST = new TypeReference<>() {
    };

    private final QuestionIssueReportRepository reportRepository;
    private final QuizAttemptRepository attemptRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public QuestionIssueReportService(
            QuestionIssueReportRepository reportRepository,
            QuizAttemptRepository attemptRepository,
            UserRepository userRepository,
            ObjectMapper objectMapper
    ) {
        this.reportRepository = reportRepository;
        this.attemptRepository = attemptRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public QuestionIssueReportResponse create(
            UUID attemptId,
            int questionId,
            QuestionIssueReportRequest request,
            UserPrincipal principal
    ) {
        QuizAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Тестът не е намерен."));
        if (!attempt.getUser().getId().equals(principal.id())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Нямате достъп до този тест.");
        }
        GeneratedQuestion question = questionsById(attempt).get(questionId);
        if (question == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Задачата не е намерена.");
        }
        UserEntity user = userRepository.findById(principal.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Потребителят не е намерен."));
        String message = request.message() == null || request.message().isBlank()
                ? "Докладвана нередност без описание."
                : request.message().trim();
        return toResponse(reportRepository.save(new QuestionIssueReport(attempt, user, questionId, message)), question.prompt());
    }

    @Transactional(readOnly = true)
    public List<QuestionIssueReportResponse> recentReports() {
        return reportRepository.findTop30ByOrderByCreatedAtDesc()
                .stream()
                .map(report -> toResponse(report, questionPrompt(report.getAttempt(), report.getQuestionId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<QuestionIssueReportResponse> reportsForAttempt(UUID attemptId) {
        QuizAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Тестът не е намерен."));
        Map<Integer, GeneratedQuestion> questionsById = questionsById(attempt);
        return reportRepository.findByAttempt_IdOrderByCreatedAtAsc(attemptId)
                .stream()
                .map(report -> toResponse(report, questionPrompt(questionsById, report.getQuestionId())))
                .toList();
    }

    @Transactional
    public QuestionIssueReportResponse update(Long reportId, UpdateQuestionIssueReportRequest request) {
        QuestionIssueReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Докладът не е намерен."));
        report.update(request.status(), request.adminNote() == null ? null : request.adminNote().trim());
        return toResponse(report, questionPrompt(report.getAttempt(), report.getQuestionId()));
    }

    @Transactional(readOnly = true)
    public long openReportsCount() {
        return reportRepository.countByStatus(IssueReportStatus.OPEN);
    }

    private QuestionIssueReportResponse toResponse(QuestionIssueReport report, String questionPrompt) {
        QuizAttempt attempt = report.getAttempt();
        return new QuestionIssueReportResponse(
                report.getId(),
                attempt.getId(),
                report.getUser().getId(),
                report.getUser().getDisplayName(),
                attempt.getCategory(),
                attempt.getMode(),
                attempt.getDifficulty(),
                report.getQuestionId(),
                questionPrompt,
                report.getMessage(),
                report.getStatus(),
                report.getAdminNote(),
                report.getCreatedAt(),
                report.getUpdatedAt(),
                report.getResolvedAt()
        );
    }

    private String questionPrompt(QuizAttempt attempt, int questionId) {
        return questionPrompt(questionsById(attempt), questionId);
    }

    private String questionPrompt(Map<Integer, GeneratedQuestion> questionsById, int questionId) {
        GeneratedQuestion question = questionsById.get(questionId);
        return question == null ? "" : question.prompt();
    }

    private Map<Integer, GeneratedQuestion> questionsById(QuizAttempt attempt) {
        return readQuestions(attempt).stream()
                .collect(Collectors.toMap(GeneratedQuestion::id, Function.identity(), (first, ignored) -> first));
    }

    private List<GeneratedQuestion> readQuestions(QuizAttempt attempt) {
        try {
            return objectMapper.readValue(attempt.getQuestionsJson(), QUESTION_LIST);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not read stored quiz questions", ex);
        }
    }
}
