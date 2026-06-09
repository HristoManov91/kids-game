package com.kidsgame.mathapp.reward;

import jakarta.validation.constraints.NotNull;

public record UpdateRewardThemeStatusRequest(
        @NotNull Boolean active
) {
}
