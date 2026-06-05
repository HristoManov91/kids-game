package com.kidsgame.mathapp.admin;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kidsgame.mathapp.issue.QuestionIssueReportService;
import com.kidsgame.mathapp.quiz.AnswerRecord;
import com.kidsgame.mathapp.quiz.AttemptStatus;
import com.kidsgame.mathapp.quiz.QuizAttempt;
import com.kidsgame.mathapp.quiz.QuizAttemptCleanupService;
import com.kidsgame.mathapp.quiz.QuizAttemptRepository;
import com.kidsgame.mathapp.quiz.QuizCategory;
import com.kidsgame.mathapp.quiz.QuizMode;
import com.kidsgame.mathapp.suggestion.TaskSuggestionService;
import com.kidsgame.mathapp.user.Role;
import com.kidsgame.mathapp.user.UserEntity;
import com.kidsgame.mathapp.user.UserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.IntStream;

@Service
public class AdminMonitoringService {
    private static final ZoneId SOFIA = ZoneId.of("Europe/Sofia");
    private static final DateTimeFormatter SHORT_DATE = DateTimeFormatter.ofPattern("dd.MM");
    private static final TypeReference<List<AnswerRecord>> ANSWER_LIST = new TypeReference<>() {
    };

    private final UserRepository userRepository;
    private final QuizAttemptRepository attemptRepository;
    private final QuizAttemptCleanupService cleanupService;
    private final ObjectMapper objectMapper;
    private final QuestionIssueReportService reportService;
    private final TaskSuggestionService suggestionService;

    public AdminMonitoringService(
            UserRepository userRepository,
            QuizAttemptRepository attemptRepository,
            QuizAttemptCleanupService cleanupService,
            ObjectMapper objectMapper,
            QuestionIssueReportService reportService,
            TaskSuggestionService suggestionService
    ) {
        this.userRepository = userRepository;
        this.attemptRepository = attemptRepository;
        this.cleanupService = cleanupService;
        this.objectMapper = objectMapper;
        this.reportService = reportService;
        this.suggestionService = suggestionService;
    }

    @Transactional
    public AdminMonitoringResponse snapshot(LocalDate fromDate, LocalDate toDate) {
        LocalDate today = LocalDate.now(SOFIA);
        LocalDate effectiveFrom = fromDate == null ? today : fromDate;
        LocalDate effectiveTo = toDate == null ? effectiveFrom : toDate;
        if (effectiveTo.isBefore(effectiveFrom)) {
            effectiveTo = effectiveFrom;
        }
        DateRange range = dateRange(effectiveFrom, effectiveTo);
        cleanupService.removeExpiredActiveAttempts();
        List<UserEntity> users = userRepository.findAll(Sort.by(Sort.Direction.ASC, "displayName"));
        List<QuizAttempt> attempts = attemptRepository.findAll();
        List<QuizAttempt> completed = attempts.stream()
                .filter(attempt -> attempt.getStatus() == AttemptStatus.COMPLETED)
                .toList();
        List<QuizAttempt> periodCompleted = completed.stream()
                .filter(attempt -> inRange(attempt.getCompletedAt(), range))
                .toList();
        List<QuizAttempt> periodStarted = attempts.stream()
                .filter(attempt -> inRange(attempt.getStartedAt(), range))
                .toList();

        return new AdminMonitoringResponse(
                Instant.now(),
                effectiveFrom,
                effectiveTo,
                range.start(),
                range.end(),
                users.size(),
                users.stream().filter(user -> user.getRole() == Role.CHILD).count(),
                attempts.size(),
                periodStarted.size(),
                attempts.stream().filter(attempt -> attempt.getStatus() == AttemptStatus.ACTIVE).count(),
                0,
                completed.size(),
                periodCompleted.size(),
                activeUsersInRange(attempts, range),
                averageGrade(periodCompleted),
                averageDurationSeconds(periodCompleted),
                fastestDurationSeconds(periodCompleted),
                slowestDurationSeconds(periodCompleted),
                averageGrade(completed),
                averageDurationSeconds(completed),
                reportService.openReportsCount(),
                suggestionService.openSuggestionsCount(),
                users.stream().map(user -> userRow(user, attempts)).toList(),
                attempts.stream()
                        .filter(attempt -> attempt.getStatus() == AttemptStatus.ACTIVE)
                        .sorted(Comparator.comparing(QuizAttempt::getStartedAt, Comparator.reverseOrder()))
                        .limit(20)
                        .map(this::attemptRow)
                        .toList(),
                completed.stream()
                        .sorted(Comparator.comparing(QuizAttempt::getCompletedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                        .limit(20)
                        .map(this::attemptRow)
                        .toList(),
                periodCompleted.stream()
                        .sorted(Comparator.comparing(QuizAttempt::getCompletedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                        .map(this::attemptRow)
                        .toList(),
                List.of(QuizCategory.values()).stream()
                        .map(category -> categoryRow(category, periodCompleted))
                        .toList(),
                List.of(QuizCategory.values()).stream()
                        .map(category -> categoryRow(category, completed))
                        .toList(),
                modeRows(periodCompleted),
                usageRows(periodStarted, periodCompleted, range, effectiveFrom.equals(effectiveTo)),
                reportService.recentReports(),
                suggestionService.recentSuggestions()
        );
    }

    private AdminMonitoringResponse.UserMonitorRow userRow(UserEntity user, List<QuizAttempt> attempts) {
        List<QuizAttempt> userAttempts = attempts.stream()
                .filter(attempt -> attempt.getUser().getId().equals(user.getId()))
                .toList();
        List<QuizAttempt> completed = userAttempts.stream()
                .filter(attempt -> attempt.getStatus() == AttemptStatus.COMPLETED)
                .toList();
        return new AdminMonitoringResponse.UserMonitorRow(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                userAttempts.stream().filter(attempt -> attempt.getStatus() == AttemptStatus.ACTIVE).count(),
                completed.size(),
                userAttempts.size(),
                averageGrade(completed),
                userAttempts.stream()
                        .map(this::lastActivity)
                        .filter(Objects::nonNull)
                        .max(Comparator.naturalOrder())
                        .orElse(null)
        );
    }

    private AdminMonitoringResponse.AttemptMonitorRow attemptRow(QuizAttempt attempt) {
        return new AdminMonitoringResponse.AttemptMonitorRow(
                attempt.getId(),
                attempt.getUser().getId(),
                attempt.getUser().getDisplayName(),
                attempt.getCategory(),
                attempt.getMode(),
                attempt.getDifficulty(),
                attempt.getStatus(),
                attempt.isLeaderboardEligible(),
                attempt.getScore(),
                attempt.getTotalQuestions(),
                attempt.getGrade(),
                attempt.getStartedAt(),
                attempt.getCompletedAt(),
                Math.max(0, attempt.getActiveDurationSeconds()),
                readAnswers(attempt).size()
        );
    }

    private AdminMonitoringResponse.CategoryMonitorRow categoryRow(QuizCategory category, List<QuizAttempt> completed) {
        List<QuizAttempt> categoryAttempts = completed.stream()
                .filter(attempt -> attempt.getCategory() == category)
                .toList();
        return new AdminMonitoringResponse.CategoryMonitorRow(
                category,
                categoryAttempts.size(),
                averageGrade(categoryAttempts),
                averageScorePercent(categoryAttempts)
        );
    }

    private List<AdminMonitoringResponse.ModeMonitorRow> modeRows(List<QuizAttempt> periodCompleted) {
        Map<ModeKey, List<QuizAttempt>> attemptsByMode = periodCompleted.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        attempt -> new ModeKey(attempt.getCategory(), attempt.getMode()),
                        java.util.LinkedHashMap::new,
                        java.util.stream.Collectors.toList()
                ));
        return attemptsByMode.entrySet().stream()
                .sorted(Comparator
                        .comparing((Map.Entry<ModeKey, List<QuizAttempt>> entry) -> entry.getKey().category())
                        .thenComparing(entry -> entry.getKey().mode()))
                .map(entry -> new AdminMonitoringResponse.ModeMonitorRow(
                        entry.getKey().category(),
                        entry.getKey().mode(),
                        entry.getValue().size(),
                        averageGrade(entry.getValue()),
                        averageDurationSeconds(entry.getValue()),
                        fastestDurationSeconds(entry.getValue()),
                        slowestDurationSeconds(entry.getValue()),
                        averageScorePercent(entry.getValue()),
                        entry.getValue().stream()
                                .sorted(Comparator.comparing(QuizAttempt::getCompletedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                                .map(this::attemptRow)
                                .toList()
                ))
                .toList();
    }

    private List<AdminMonitoringResponse.UsageBucketRow> usageRows(
            List<QuizAttempt> periodStarted,
            List<QuizAttempt> periodCompleted,
            DateRange range,
            boolean singleDay
    ) {
        if (singleDay) {
            return IntStream.range(0, 24)
                    .mapToObj(hour -> new AdminMonitoringResponse.UsageBucketRow(
                            String.format("%02d", hour),
                            periodStarted.stream()
                                    .filter(attempt -> attempt.getStartedAt().atZone(SOFIA).getHour() == hour)
                                    .count(),
                            periodCompleted.stream()
                                    .filter(attempt -> attempt.getCompletedAt() != null
                                            && attempt.getCompletedAt().atZone(SOFIA).getHour() == hour)
                                    .count()
                    ))
                    .toList();
        }
        LocalDate startDate = range.start().atZone(SOFIA).toLocalDate();
        LocalDate endDate = range.end().atZone(SOFIA).toLocalDate();
        int days = (int) java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);
        return IntStream.range(0, days)
                .mapToObj(offset -> {
                    LocalDate day = startDate.plusDays(offset);
                    return new AdminMonitoringResponse.UsageBucketRow(
                            day.format(SHORT_DATE),
                            periodStarted.stream()
                                    .filter(attempt -> attempt.getStartedAt().atZone(SOFIA).toLocalDate().equals(day))
                                    .count(),
                            periodCompleted.stream()
                                    .filter(attempt -> attempt.getCompletedAt() != null
                                            && attempt.getCompletedAt().atZone(SOFIA).toLocalDate().equals(day))
                                    .count()
                    );
                })
                .toList();
    }

    private DateRange dateRange(LocalDate fromDate, LocalDate toDate) {
        return new DateRange(fromDate.atStartOfDay(SOFIA).toInstant(), toDate.plusDays(1).atStartOfDay(SOFIA).toInstant());
    }

    private boolean inRange(Instant value, DateRange range) {
        return value != null && !value.isBefore(range.start()) && value.isBefore(range.end());
    }

    private long activeUsersInRange(List<QuizAttempt> attempts, DateRange range) {
        return attempts.stream()
                .filter(attempt -> inRange(lastActivity(attempt), range))
                .map(attempt -> attempt.getUser().getId())
                .distinct()
                .count();
    }

    private long fastestDurationSeconds(List<QuizAttempt> attempts) {
        return attempts.stream()
                .mapToLong(QuizAttempt::getActiveDurationSeconds)
                .filter(duration -> duration > 0)
                .min()
                .orElse(0);
    }

    private long slowestDurationSeconds(List<QuizAttempt> attempts) {
        return attempts.stream()
                .mapToLong(QuizAttempt::getActiveDurationSeconds)
                .max()
                .orElse(0);
    }

    private Instant lastActivity(QuizAttempt attempt) {
        if (attempt.getCompletedAt() != null) {
            return attempt.getCompletedAt();
        }
        if (attempt.getLastHeartbeatAt() != null) {
            return attempt.getLastHeartbeatAt();
        }
        return attempt.getStartedAt();
    }

    private BigDecimal averageGrade(List<QuizAttempt> attempts) {
        List<BigDecimal> grades = attempts.stream()
                .map(QuizAttempt::getGrade)
                .filter(Objects::nonNull)
                .toList();
        if (grades.isEmpty()) {
            return null;
        }
        BigDecimal total = grades.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(grades.size()), 2, RoundingMode.HALF_UP);
    }

    private long averageDurationSeconds(List<QuizAttempt> attempts) {
        return Math.round(attempts.stream()
                .mapToLong(QuizAttempt::getActiveDurationSeconds)
                .average()
                .orElse(0));
    }

    private int averageScorePercent(List<QuizAttempt> attempts) {
        List<Integer> percents = attempts.stream()
                .filter(attempt -> attempt.getScore() != null && attempt.getTotalQuestions() > 0)
                .map(attempt -> Math.round((attempt.getScore() * 100f) / attempt.getTotalQuestions()))
                .toList();
        if (percents.isEmpty()) {
            return 0;
        }
        int total = percents.stream().mapToInt(Integer::intValue).sum();
        return Math.round(total / (float) percents.size());
    }

    private List<AnswerRecord> readAnswers(QuizAttempt attempt) {
        try {
            return objectMapper.readValue(attempt.getAnswersJson(), ANSWER_LIST);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not read stored quiz answers", ex);
        }
    }

    private record ModeKey(QuizCategory category, QuizMode mode) {
    }

    private record DateRange(Instant start, Instant end) {
    }
}
