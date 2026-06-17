package com.kidsgame.mathapp.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kidsgame.mathapp.auth.UserResponse;
import com.kidsgame.mathapp.issue.QuestionIssueReportService;
import com.kidsgame.mathapp.quiz.QuizAttempt;
import com.kidsgame.mathapp.quiz.QuizAttemptRepository;
import com.kidsgame.mathapp.quiz.QuizCategory;
import com.kidsgame.mathapp.quiz.QuizMode;
import com.kidsgame.mathapp.user.Role;
import com.kidsgame.mathapp.user.UserEntity;
import com.kidsgame.mathapp.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAttemptServiceTest {
    @Mock
    private QuizAttemptRepository attemptRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private QuestionIssueReportService reportService;

    @Test
    void movesAttemptToSelectedChildProfile() {
        UserEntity hristo = child(1L, "hristo", "Христо");
        UserEntity hrisia = child(2L, "hrisia", "Хрисия");
        QuizAttempt attempt = new QuizAttempt(
                hristo,
                QuizCategory.BULGARIAN,
                QuizMode.WORD_LETTERS,
                1,
                "[]",
                "[]",
                true,
                10
        );
        AdminAttemptService service = new AdminAttemptService(
                attemptRepository,
                userRepository,
                new ObjectMapper().findAndRegisterModules(),
                reportService
        );

        when(attemptRepository.findById(attempt.getId())).thenReturn(Optional.of(attempt));
        when(userRepository.findById(2L)).thenReturn(Optional.of(hrisia));
        when(userRepository.findByRoleOrderByDisplayNameAsc(Role.CHILD)).thenReturn(List.of(hrisia, hristo));
        when(reportService.reportsForAttempt(attempt.getId())).thenReturn(List.of());

        AdminAttemptDetailResponse response = service.updateOwner(
                attempt.getId(),
                new AdminAttemptOwnerUpdateRequest(2L)
        );

        assertThat(attempt.getUser()).isSameAs(hrisia);
        assertThat(response.attempt().userId()).isEqualTo(2L);
        assertThat(response.attempt().displayName()).isEqualTo("Хрисия");
        assertThat(response.children()).extracting(UserResponse::displayName).containsExactly("Хрисия", "Христо");
    }

    private static UserEntity child(Long id, String username, String displayName) {
        UserEntity user = new UserEntity(username, displayName, "hash", Role.CHILD);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
