package com.codex.swordgrowth.web.dto;

import com.codex.swordgrowth.domain.save.PlayerSaveData;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record SaveDataResponse(
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
    public static SaveDataResponse from(PlayerSaveData saveData) {
        return new SaveDataResponse(
                saveData.userId(),
                saveData.currentWeaponId(),
                saveData.materials(),
                saveData.specialItems(),
                saveData.weaponInventory(),
                saveData.ownedWeaponIds(),
                saveData.unlockedWeaponShop(),
                saveData.discoveredWeaponIds(),
                saveData.highestReachedWeaponId(),
                saveData.updatedAt()
        );
    }
}
