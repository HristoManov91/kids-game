package com.kidsgame.mathapp.admin;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminAnswerUpdateRequest(
        @NotNull @Size(max = 2000) String answer
) {
}
