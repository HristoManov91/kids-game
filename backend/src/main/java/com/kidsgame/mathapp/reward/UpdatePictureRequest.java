package com.kidsgame.mathapp.reward;

import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdatePictureRequest(
        @Size(max = 40, message = "Името може да е до 40 знака.") String name,
        List<PlacedRewardItemDto> placedItems
) {
}
