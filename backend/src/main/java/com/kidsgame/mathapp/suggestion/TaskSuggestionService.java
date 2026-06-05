package com.kidsgame.mathapp.suggestion;

import com.kidsgame.mathapp.auth.UserPrincipal;
import com.kidsgame.mathapp.issue.IssueReportStatus;
import com.kidsgame.mathapp.quiz.QuizCategory;
import com.kidsgame.mathapp.quiz.QuizMode;
import com.kidsgame.mathapp.user.UserEntity;
import com.kidsgame.mathapp.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
public class TaskSuggestionService {
    private static final Set<QuizMode> MATH_MODES = EnumSet.of(
            QuizMode.ADDITION,
            QuizMode.SUBTRACTION,
            QuizMode.MIXED,
            QuizMode.UNKNOWN_ADDITION,
            QuizMode.UNKNOWN_SUBTRACTION,
            QuizMode.UNKNOWN_MIXED,
            QuizMode.MULTIPLICATION,
            QuizMode.DIVISION,
            QuizMode.MULTIPLICATION_DIVISION,
            QuizMode.COMPARE
    );
    private static final Set<QuizMode> BULGARIAN_MODES = EnumSet.of(
            QuizMode.WORD_LETTERS,
            QuizMode.WORD_SYLLABLES,
            QuizMode.WORD_TYPING,
            QuizMode.WORD_PICTURE,
            QuizMode.WORD_MISSING_LETTER,
            QuizMode.WORD_FIRST_LETTER_GROUP,
            QuizMode.WORD_WRONG_LETTER
    );
    private static final Set<QuizMode> LOGIC_MODES = EnumSet.of(
            QuizMode.FIND_OBJECT,
            QuizMode.SPOT_DIFFERENCES,
            QuizMode.MEMORY_PAIRS,
            QuizMode.PATTERN_SEQUENCE
    );

    private final TaskSuggestionRepository suggestionRepository;
    private final UserRepository userRepository;

    public TaskSuggestionService(TaskSuggestionRepository suggestionRepository, UserRepository userRepository) {
        this.suggestionRepository = suggestionRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public TaskSuggestionResponse create(TaskSuggestionRequest request, UserPrincipal principal) {
        UserEntity user = userRepository.findById(principal.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Потребителят не е намерен."));
        String message = request.message().trim();
        validateContext(request);
        TaskSuggestion suggestion = new TaskSuggestion(
                user,
                request.category(),
                request.mode(),
                request.difficulty(),
                message
        );
        return toResponse(suggestionRepository.save(suggestion));
    }

    @Transactional(readOnly = true)
    public List<TaskSuggestionResponse> recentSuggestions() {
        return suggestionRepository.findTop50ByOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public TaskSuggestionResponse update(Long suggestionId, UpdateTaskSuggestionRequest request) {
        TaskSuggestion suggestion = suggestionRepository.findById(suggestionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Предложението не е намерено."));
        suggestion.update(request.status(), request.adminNote() == null ? null : request.adminNote().trim());
        return toResponse(suggestion);
    }

    @Transactional(readOnly = true)
    public long openSuggestionsCount() {
        return suggestionRepository.countByStatus(IssueReportStatus.OPEN);
    }

    private void validateContext(TaskSuggestionRequest request) {
        boolean hasSpecificContext = request.category() != null || request.mode() != null || request.difficulty() != null;
        if (!hasSpecificContext) {
            return;
        }
        if (request.category() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Избери предмет или остави предложението общо.");
        }
        if (request.mode() != null && !modesForCategory(request.category()).contains(request.mode())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Избраната подкатегория не е към този предмет.");
        }
    }

    private Set<QuizMode> modesForCategory(QuizCategory category) {
        return switch (category) {
            case BULGARIAN -> BULGARIAN_MODES;
            case LOGIC -> LOGIC_MODES;
            case MATH -> MATH_MODES;
        };
    }

    private TaskSuggestionResponse toResponse(TaskSuggestion suggestion) {
        return new TaskSuggestionResponse(
                suggestion.getId(),
                suggestion.getUser().getId(),
                suggestion.getUser().getDisplayName(),
                suggestion.getCategory(),
                suggestion.getMode(),
                suggestion.getDifficulty(),
                suggestion.getMessage(),
                suggestion.getStatus(),
                suggestion.getAdminNote(),
                suggestion.getCreatedAt(),
                suggestion.getUpdatedAt(),
                suggestion.getResolvedAt()
        );
    }
}
