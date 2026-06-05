package com.kidsgame.mathapp.reward;

public record DeleteRewardCatalogItemResponse(
        String itemId,
        String itemName,
        int removedPlacedItems,
        long refundedPurchases,
        long refundedCrystals
) {
}
