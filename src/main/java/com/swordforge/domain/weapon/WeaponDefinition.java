package com.swordforge.domain.weapon;

public record WeaponDefinition(
        String id,
        String nameKo,
        String nameEn,
        Grade grade,
        int stage,
        int attackPower,
        double successRate,
        double failRate,
        String failureRewardGroup,
        String nextWeaponId,
        boolean purchaseUnlockRequired,
        String description
) {
}

