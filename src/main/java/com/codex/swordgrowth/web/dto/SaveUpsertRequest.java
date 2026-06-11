package com.codex.swordgrowth.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

public record SaveUpsertRequest(
        @NotBlank String currentWeaponId,
        @NotNull Map<String, Integer> materials,
        @NotNull Map<String, Integer> specialItems,
        @NotNull Map<String, Integer> weaponInventory,
        @NotNull List<String> ownedWeaponIds,
        @NotNull List<String> unlockedWeaponShop,
        @NotNull List<String> discoveredWeaponIds,
        @NotBlank String highestReachedWeaponId
) {
}
