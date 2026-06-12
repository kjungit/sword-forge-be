package com.swordforge.domain.item;

public record RewardRange(
        String materialId,
        int minAmount,
        int maxAmount
) {
}

