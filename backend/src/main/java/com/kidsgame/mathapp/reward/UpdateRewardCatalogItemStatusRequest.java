package com.kidsgame.mathapp.reward;

import jakarta.validation.constraints.NotNull;

public record UpdateRewardCatalogItemStatusRequest(
        @NotNull Boolean active
) {
}
