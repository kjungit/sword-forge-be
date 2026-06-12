package com.swordforge.domain.save;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record PlayerSaveData(
        String userId,
        String currentWeaponId,
        Map<String, Integer> materials,
        Map<String, Integer> specialItems,
        Map<String, Integer> weaponInventory,
        List<String> lockedWeaponIds,
        List<String> ownedWeaponIds,
        List<String> unlockedWeaponShop,
        List<String> discoveredWeaponIds,
        String highestReachedWeaponId,
        Map<String, Integer> pityStacks,
        Instant updatedAt
) {
    public PlayerSaveData(
            String userId,
            String currentWeaponId,
            Map<String, Integer> materials,
            Map<String, Integer> specialItems,
            Map<String, Integer> weaponInventory,
            List<String> ownedWeaponIds,
            List<String> unlockedWeaponShop,
            List<String> discoveredWeaponIds,
            String highestReachedWeaponId,
            Map<String, Integer> pityStacks,
            Instant updatedAt
    ) {
        this(
                userId,
                currentWeaponId,
                materials,
                specialItems,
                weaponInventory,
                List.of(),
                ownedWeaponIds,
                unlockedWeaponShop,
                discoveredWeaponIds,
                highestReachedWeaponId,
                pityStacks,
                updatedAt
        );
    }
}
