package com.codex.swordgrowth.domain.save;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record PlayerSaveData(
        String userId,
        String currentWeaponId,
        Map<String, Integer> materials,
        Map<String, Integer> specialItems,
        Map<String, Integer> weaponInventory,
        List<String> ownedWeaponIds,
        List<String> unlockedWeaponShop,
        List<String> discoveredWeaponIds,
        String highestReachedWeaponId,
        Instant updatedAt
) {
}
