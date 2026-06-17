package com.kidsgame.mathapp.admin;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AdminAttemptOwnerUpdateRequest(
        @NotNull @Positive Long childId
) {
}
