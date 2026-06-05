package com.kidsgame.mathapp.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kidsgame.mathapp.quiz.AnswerRecord;
import com.kidsgame.mathapp.quiz.AttemptStatus;
import com.kidsgame.mathapp.quiz.GeneratedQuestion;
import com.kidsgame.mathapp.quiz.Medal;
import com.kidsgame.mathapp.quiz.QuestionKind;
import com.kidsgame.mathapp.quiz.QuestionScoring;
import com.kidsgame.mathapp.quiz.QuizAttempt;
import com.kidsgame.mathapp.quiz.QuizAttemptRepository;
import com.kidsgame.mathapp.quiz.QuizCategory;
import com.kidsgame.mathapp.quiz.QuizGenerator;
import com.kidsgame.mathapp.quiz.QuizMode;
import com.kidsgame.mathapp.user.Role;
import com.kidsgame.mathapp.user.UserEntity;
import com.kidsgame.mathapp.user.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Component
@Order(20)
public class DataSeeder implements ApplicationRunner {
    private static final ZoneId SOFIA = ZoneId.of("Europe/Sofia");
    private static final String ADMIN_USERNAME = "admin";
    private static final String HRISTO_PASSWORD = "Hristo-Kids!2026";

    private final UserRepository userRepository;
    private final QuizAttemptRepository attemptRepository;
    private final PasswordEncoder passwordEncoder;
    private final QuizGenerator quizGenerator;
    private final ObjectMapper objectMapper;
    private final boolean seedEnabled;

    public DataSeeder(
            UserRepository userRepository,
            QuizAttemptRepository attemptRepository,
            PasswordEncoder passwordEncoder,
            QuizGenerator quizGenerator,
            ObjectMapper objectMapper,
            @Value("${app.demo.seed-enabled:true}") boolean seedEnabled
    ) {
        this.userRepository = userRepository;
        this.attemptRepository = attemptRepository;
        this.passwordEncoder = passwordEncoder;
        this.quizGenerator = quizGenerator;
        this.objectMapper = objectMapper;
        this.seedEnabled = seedEnabled;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!seedEnabled) {
            return;
        }
        createIfMissing("mila", "Мила", "mila123", Role.CHILD);
        UserEntity hristo = ensureHristo();
        removeExtraAdminAccount();
        seedDemoAttempts(hristo);
        normalizeHristoDemoAttempts(hristo);
    }

    private UserEntity ensureHristo() {
        return userRepository.findByUsernameIgnoreCase("христо")
                .map(user -> {
                    user.updateIdentity("Христо", "Христо");
                    if (!passwordEncoder.matches(HRISTO_PASSWORD, user.getPasswordHash())) {
                        user.updatePassword(passwordEncoder.encode(HRISTO_PASSWORD));
                    }
                    return userRepository.save(user);
                })
                .orElseGet(() -> userRepository.save(
                        new UserEntity("Христо", "Христо", passwordEncoder.encode(HRISTO_PASSWORD), Role.CHILD)
                ));
    }

    private void removeExtraAdminAccount() {
        userRepository.findByUsernameIgnoreCase(ADMIN_USERNAME)
                .ifPresent(userRepository::delete);
    }

    private void createIfMissing(String username, String displayName, String password, Role role) {
        if (!userRepository.existsByUsernameIgnoreCase(username)) {
            userRepository.save(new UserEntity(username, displayName, passwordEncoder.encode(password), role));
        }
    }

    private void seedDemoAttempts(UserEntity user) {
        List<DemoSpec> specs = demoSpecs();
        int specIndex = 0;
        for (DemoSpec spec : specs) {
            specIndex++;
            for (int difficulty = 1; difficulty <= 10; difficulty++) {
                if (attemptRepository.existsByUser_IdAndCategoryAndModeAndDifficultyAndStatus(
                        user.getId(),
                        spec.category(),
                        spec.mode(),
                        difficulty,
                        AttemptStatus.COMPLETED
                )) {
                    continue;
                }
                seedDemoAttempt(user, spec, difficulty, specIndex);
            }
        }
    }

    private List<DemoSpec> demoSpecs() {
        List<DemoSpec> specs = new ArrayList<>();
        QuizGenerator.MATH_PRIMITIVE_MODES.forEach(mode ->
                specs.add(new DemoSpec(QuizCategory.MATH, mode, List.of(mode)))
        );
        specs.add(new DemoSpec(QuizCategory.MATH, QuizMode.MIXED, List.of(QuizMode.ADDITION, QuizMode.SUBTRACTION)));
        specs.add(new DemoSpec(QuizCategory.MATH, QuizMode.UNKNOWN_MIXED, List.of(QuizMode.UNKNOWN_ADDITION, QuizMode.UNKNOWN_SUBTRACTION)));
        specs.add(new DemoSpec(QuizCategory.MATH, QuizMode.MULTIPLICATION_DIVISION, List.of(QuizMode.MULTIPLICATION, QuizMode.DIVISION)));
        specs.add(new DemoSpec(QuizCategory.MATH, QuizMode.ALL_GROUP, QuizGenerator.MATH_LINEAR_MODES));
        specs.add(new DemoSpec(QuizCategory.MATH, QuizMode.CUSTOM_GROUP, List.of(QuizMode.ADDITION, QuizMode.SUBTRACTION, QuizMode.COMPARE)));

        QuizGenerator.BULGARIAN_PRIMITIVE_MODES.forEach(mode ->
                specs.add(new DemoSpec(QuizCategory.BULGARIAN, mode, List.of(mode)))
        );
        specs.add(new DemoSpec(QuizCategory.BULGARIAN, QuizMode.ALL_GROUP, QuizGenerator.BULGARIAN_PRIMITIVE_MODES));
        specs.add(new DemoSpec(QuizCategory.BULGARIAN, QuizMode.CUSTOM_GROUP, List.of(QuizMode.WORD_LETTERS, QuizMode.WORD_SYLLABLES)));

        QuizGenerator.LOGIC_PRIMITIVE_MODES.forEach(mode ->
                specs.add(new DemoSpec(QuizCategory.LOGIC, mode, List.of(mode)))
        );
        specs.add(new DemoSpec(QuizCategory.LOGIC, QuizMode.ALL_GROUP, QuizGenerator.LOGIC_PRIMITIVE_MODES));
        return specs;
    }

    private void seedDemoAttempt(UserEntity user, DemoSpec spec, int difficulty, int specIndex) {
        List<GeneratedQuestion> questions = spec.mode() == QuizMode.CUSTOM_GROUP
                ? quizGenerator.generate(spec.category(), spec.includedModes(), difficulty)
                : quizGenerator.generate(spec.category(), spec.mode(), difficulty);
        int total = questions.stream().mapToInt(QuestionScoring::weight).sum();
        int mistakes = (difficulty + specIndex) % Math.max(2, Math.min(6, total / 2));
        int score = Math.max(0, total - mistakes);
        List<AnswerRecord> answers = new ArrayList<>();
        Instant answeredAt = Instant.now().minusSeconds(3600L + specIndex * 60L + difficulty * 10L);
        int remainingCorrect = score;
        for (int index = 0; index < questions.size(); index++) {
            GeneratedQuestion question = questions.get(index);
            int questionWeight = QuestionScoring.weight(question);
            int questionCorrect = Math.min(questionWeight, Math.max(0, remainingCorrect));
            remainingCorrect -= questionCorrect;
            boolean correct = questionCorrect == questionWeight;
            answers.add(new AnswerRecord(
                    question.id(),
                    answerWithScore(question, questionCorrect),
                    correct,
                    QuestionScoring.publicCorrectAnswer(question),
                    answeredAt.plusSeconds(index * 4L)
            ));
        }

        QuizAttempt attempt = new QuizAttempt(
                user,
                spec.category(),
                spec.mode(),
                difficulty,
                writeJson(questions),
                writeJson(spec.includedModes()),
                false,
                total
        );
        addDemoDuration(attempt, total * (4L + difficulty) + specIndex * 3L);
        attempt.complete(score, calculateGrade(score, total), medalFor(score, total), writeJson(answers));
        attemptRepository.save(attempt);
    }

    private String answerWithScore(GeneratedQuestion question, int correctCount) {
        if (question.kind() == QuestionKind.SPOT_DIFFERENCES) {
            return java.util.Arrays.stream(question.answer().split(";"))
                    .map(String::trim)
                    .filter(part -> !part.isBlank())
                    .limit(Math.max(0, correctCount))
                    .collect(java.util.stream.Collectors.joining(";"));
        }
        if (question.kind() == QuestionKind.MEMORY_PAIRS) {
            int pairs = memoryPairs(question);
            int perfectMistakes = memoryPerfectMistakes(question);
            if (correctCount <= 0 || pairs <= 0) {
                return "";
            }
            int extraAttempts = switch (correctCount) {
                case 10 -> perfectMistakes;
                case 9 -> perfectMistakes + Math.max(1, (int) Math.floor(pairs * 0.25));
                case 8 -> perfectMistakes + Math.max(1, (int) Math.floor(pairs * 0.5));
                case 7 -> perfectMistakes + Math.max(1, pairs);
                case 6 -> perfectMistakes + Math.max(1, (int) Math.floor(pairs * 1.5));
                case 5 -> perfectMistakes + Math.max(1, pairs * 2);
                case 4 -> perfectMistakes + Math.max(1, pairs * 3);
                default -> perfectMistakes + pairs * 4;
            };
            return "SOLVED|attempts=" + (pairs + extraAttempts) + "|pairs=" + pairs;
        }
        return correctCount > 0 ? QuestionScoring.publicCorrectAnswer(question) : wrongAnswer(question);
    }

    private void normalizeHristoDemoAttempts(UserEntity user) {
        List<QuizAttempt> attempts = attemptRepository.findByUser_IdAndStatusOrderByStartedAtDesc(user.getId(), AttemptStatus.COMPLETED);
        LocalDate today = LocalDate.now(SOFIA);
        for (int index = 0; index < attempts.size(); index++) {
            QuizAttempt attempt = attempts.get(index);
            long duration = attempt.getActiveDurationSeconds() > 0
                    ? attempt.getActiveDurationSeconds()
                    : Math.max(40, attempt.getTotalQuestions() * 6L);
            int dayOffset = index % 5;
            LocalTime completedTime = LocalTime.of(9 + (index % 9), (index * 7) % 60);
            Instant completedAt = today.minusDays(dayOffset)
                    .atTime(completedTime)
                    .atZone(SOFIA)
                    .toInstant();
            attempt.markDemoAttempt(completedAt.minusSeconds(duration), completedAt, duration);
        }
        attemptRepository.saveAll(attempts);
    }

    private String wrongAnswer(GeneratedQuestion question) {
        if (question.kind() == QuestionKind.CHOICE) {
            return question.choices().stream()
                    .filter(choice -> !choice.equals(question.answer()))
                    .findFirst()
                    .orElse("?");
        }
        if (question.kind() == QuestionKind.NUMERIC) {
            return question.answer().equals("0") ? "1" : "0";
        }
        if (question.kind() == QuestionKind.SPOT_DIFFERENCES) {
            String[] parts = question.answer().split(";");
            return parts.length > 0 ? parts[0] : "";
        }
        if (question.kind() == QuestionKind.MEMORY_PAIRS) {
            int pairs = memoryPairs(question);
            return pairs > 0 ? "SOLVED|attempts=" + (pairs * 5) + "|pairs=" + pairs : "";
        }
        return "ГРЕШНО";
    }

    private int memoryPairs(GeneratedQuestion question) {
        return question.answerSlots().stream()
                .filter(slot -> slot != null && slot.startsWith("M|"))
                .findFirst()
                .map(slot -> {
                    String[] parts = slot.split("\\|", -1);
                    if (parts.length < 3) {
                        return 0;
                    }
                    try {
                        return Integer.parseInt(parts[2]);
                    } catch (NumberFormatException ex) {
                        return 0;
                    }
                })
                .orElse(0);
    }

    private int memoryPerfectMistakes(GeneratedQuestion question) {
        return question.answerSlots().stream()
                .filter(slot -> slot != null && slot.startsWith("M|"))
                .findFirst()
                .map(slot -> {
                    String[] parts = slot.split("\\|", -1);
                    if (parts.length < 4) {
                        return 0;
                    }
                    try {
                        return Integer.parseInt(parts[3]);
                    } catch (NumberFormatException ex) {
                        return 0;
                    }
                })
                .orElse(0);
    }

    private void addDemoDuration(QuizAttempt attempt, long seconds) {
        Instant marker = Instant.now().minusSeconds(seconds);
        attempt.recordActivity(marker, 15);
        long remaining = seconds;
        while (remaining > 0) {
            long step = Math.min(10, remaining);
            marker = marker.plusSeconds(step);
            attempt.recordActivity(marker, 15);
            remaining -= step;
        }
    }

    private BigDecimal calculateGrade(int score, int total) {
        double half = total / 2.0;
        if (score < half) {
            return BigDecimal.valueOf(2).setScale(2, RoundingMode.HALF_UP);
        }
        double grade = 3.0 + ((score - half) / half) * 3.0;
        return BigDecimal.valueOf(Math.min(6.0, grade)).setScale(2, RoundingMode.HALF_UP);
    }

    private Medal medalFor(int score, int total) {
        double ratio = score / (double) total;
        if (ratio == 1.0) {
            return Medal.DIAMOND_CUP;
        }
        if (ratio >= 0.9) {
            return Medal.GOLD_CUP;
        }
        if (ratio >= 0.75) {
            return Medal.SILVER_MEDAL;
        }
        if (ratio >= 0.5) {
            return Medal.BRONZE_STAR;
        }
        return Medal.KEEP_GOING;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not write demo data", ex);
        }
    }

    private record DemoSpec(
            QuizCategory category,
            QuizMode mode,
            List<QuizMode> includedModes
    ) {
    }
}
