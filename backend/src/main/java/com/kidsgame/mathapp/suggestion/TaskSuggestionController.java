package com.kidsgame.mathapp.suggestion;

import com.kidsgame.mathapp.auth.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/suggestions")
public class TaskSuggestionController {
    private final TaskSuggestionService suggestionService;

    public TaskSuggestionController(TaskSuggestionService suggestionService) {
        this.suggestionService = suggestionService;
    }

    @PostMapping
    public TaskSuggestionResponse create(
            @Valid @RequestBody TaskSuggestionRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return suggestionService.create(request, principal);
    }
}
