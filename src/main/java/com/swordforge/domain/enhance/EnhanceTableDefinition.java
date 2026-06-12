package com.swordforge.domain.enhance;

public record EnhanceTableDefinition(
        String weaponId,
        double successRate,
        double failRate,
        String failResult
) {
}

