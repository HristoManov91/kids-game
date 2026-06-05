package com.kidsgame.mathapp.reward;

public record PurchaseRewardItemResponse(
        RewardBalanceResponse balance,
        AlbumPictureDetailResponse picture,
        PlacedRewardItemDto placedItem,
        String message
) {
}
