package com.kidsgame.mathapp.reward;

import jakarta.validation.constraints.NotBlank;

public record PurchaseRewardItemRequest(
        @NotBlank(message = "Избери предмет.") String itemId
) {
}
