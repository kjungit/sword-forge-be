package com.codex.swordgrowth.domain.item;

public record RewardRange(
        String materialId,
        int minAmount,
        int maxAmount
) {
}

