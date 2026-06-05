package com.kidsgame.mathapp.reward;

import java.util.List;

public record RewardThemeResponse(
        String id,
        String name,
        String description,
        String backgroundImage,
        String thumbnailImage,
        List<String> categories,
        String defaultPictureName
) {
}
