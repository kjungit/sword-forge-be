package com.swordforge.web.dto;

import com.swordforge.domain.save.PlayerSaveData;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record SaveDataResponse(
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
    public static SaveDataResponse from(PlayerSaveData saveData) {
        return new SaveDataResponse(
                saveData.userId(),
                saveData.currentWeaponId(),
                saveData.materials(),
                saveData.specialItems(),
                saveData.weaponInventory(),
                saveData.lockedWeaponIds(),
                saveData.ownedWeaponIds(),
                saveData.unlockedWeaponShop(),
                saveData.discoveredWeaponIds(),
                saveData.highestReachedWeaponId(),
                saveData.pityStacks(),
                saveData.updatedAt()
        );
    }
}
