package com.kidsgame.mathapp.reward;

public record RemoveRewardItemResponse(
        RewardBalanceResponse balance,
        AlbumPictureDetailResponse picture,
        int refundedCrystals,
        String message
) {
}
