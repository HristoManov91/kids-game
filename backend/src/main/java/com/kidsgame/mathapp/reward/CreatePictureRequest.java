package com.kidsgame.mathapp.reward;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreatePictureRequest(
        @Size(max = 40, message = "Името може да е до 40 знака.") String name,
        @NotBlank(message = "Избери свят за картината.") String themeId
) {
}
