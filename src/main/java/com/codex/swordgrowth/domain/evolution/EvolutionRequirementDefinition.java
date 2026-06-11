package com.codex.swordgrowth.domain.evolution;

import java.util.Map;

public record EvolutionRequirementDefinition(
        String fromWeaponId,
        String toGrade,
        Map<String, Integer> requiredWeapons,
        Map<String, Integer> requiredMaterials
) {
}

