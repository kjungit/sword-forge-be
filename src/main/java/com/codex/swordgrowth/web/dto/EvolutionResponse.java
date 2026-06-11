package com.codex.swordgrowth.web.dto;

import com.codex.swordgrowth.application.evolution.EvolutionService;

import java.util.Map;

public record EvolutionResponse(
        String userId,
        String fromWeaponId,
        String toWeaponId,
        Map<String, Integer> consumedWeapons,
        Map<String, Integer> consumedMaterials,
        String equippedWeaponId
) {
    public static EvolutionResponse from(EvolutionService.EvolutionResult result) {
        return new EvolutionResponse(
                result.userId(),
                result.fromWeaponId(),
                result.toWeaponId(),
                result.consumedWeapons(),
                result.consumedMaterials(),
                result.equippedWeaponId()
        );
    }
}

