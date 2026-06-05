package com.kidsgame.mathapp.reward;

import java.util.List;

public record RewardCatalogResponse(
        List<RewardThemeResponse> themes,
        List<RewardCatalogItemResponse> items
) {
}
