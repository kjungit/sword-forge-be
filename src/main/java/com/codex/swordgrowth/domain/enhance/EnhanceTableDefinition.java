package com.codex.swordgrowth.domain.enhance;

public record EnhanceTableDefinition(
        String weaponId,
        double successRate,
        double failRate,
        String failResult
) {
}

