package com.kidsgame.mathapp.reward;

public record RewardBalanceResponse(
        int earned,
        int spent,
        int available
) {
}
