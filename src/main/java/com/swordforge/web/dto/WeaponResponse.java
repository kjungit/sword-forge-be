package com.swordforge.web.dto;

import com.swordforge.domain.weapon.Grade;
import com.swordforge.domain.weapon.WeaponDefinition;

public record WeaponResponse(
        String id,
        String nameKo,
        String nameEn,
        String grade,
        int stage,
        int attackPower,
        double successRate,
        double failRate,
        String failureRewardGroup,
        String nextWeaponId,
        boolean purchaseUnlockRequired,
        String description
) {
    public static WeaponResponse from(WeaponDefinition weapon) {
        Grade grade = weapon.grade();
        return new WeaponResponse(
                weapon.id(),
                weapon.nameKo(),
                weapon.nameEn(),
                grade.code(),
                weapon.stage(),
                weapon.attackPower(),
                weapon.successRate(),
                weapon.failRate(),
                weapon.failureRewardGroup(),
                weapon.nextWeaponId(),
                weapon.purchaseUnlockRequired(),
                weapon.description()
        );
    }
}

