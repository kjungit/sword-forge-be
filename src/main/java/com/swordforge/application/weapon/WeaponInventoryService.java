package com.swordforge.application.weapon;

import com.swordforge.application.save.PlayerSaveService;
import com.swordforge.domain.save.PlayerSaveData;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class WeaponInventoryService {

    private final WeaponCatalogService weaponCatalogService;
    private final PlayerSaveService playerSaveService;

    public WeaponInventoryService(
            WeaponCatalogService weaponCatalogService,
            PlayerSaveService playerSaveService
    ) {
        this.weaponCatalogService = weaponCatalogService;
        this.playerSaveService = playerSaveService;
    }

    @Transactional
    public PlayerSaveData equip(String userId, String weaponId) {
        weaponCatalogService.findById(weaponId);
        return playerSaveService.mutate(userId, save -> {
            if (save.weaponInventory().getOrDefault(weaponId, 0) <= 0) {
                throw new IllegalArgumentException("weapon is not owned: " + weaponId);
            }
            return new PlayerSaveData(
                    save.userId(),
                    weaponId,
                    save.materials(),
                    save.specialItems(),
                    save.weaponInventory(),
                    save.lockedWeaponIds(),
                    playerSaveService.deriveOwnedWeaponIds(save.weaponInventory()),
                    save.unlockedWeaponShop(),
                    save.discoveredWeaponIds(),
                    save.highestReachedWeaponId(),
                    save.pityStacks(),
                    Instant.now()
            );
        });
    }

    @Transactional
    public PlayerSaveData lock(String userId, String weaponId) {
        weaponCatalogService.findById(weaponId);
        return playerSaveService.mutate(userId, save -> {
            if (save.weaponInventory().getOrDefault(weaponId, 0) <= 0) {
                throw new IllegalArgumentException("weapon is not owned: " + weaponId);
            }
            return new PlayerSaveData(
                    save.userId(),
                    save.currentWeaponId(),
                    save.materials(),
                    save.specialItems(),
                    save.weaponInventory(),
                    playerSaveService.appendUnique(save.lockedWeaponIds(), weaponId),
                    playerSaveService.deriveOwnedWeaponIds(save.weaponInventory()),
                    save.unlockedWeaponShop(),
                    save.discoveredWeaponIds(),
                    save.highestReachedWeaponId(),
                    save.pityStacks(),
                    Instant.now()
            );
        });
    }

    @Transactional
    public PlayerSaveData unlock(String userId, String weaponId) {
        weaponCatalogService.findById(weaponId);
        return playerSaveService.mutate(userId, save -> new PlayerSaveData(
                save.userId(),
                save.currentWeaponId(),
                save.materials(),
                save.specialItems(),
                save.weaponInventory(),
                playerSaveService.removeValue(save.lockedWeaponIds(), weaponId),
                playerSaveService.deriveOwnedWeaponIds(save.weaponInventory()),
                save.unlockedWeaponShop(),
                save.discoveredWeaponIds(),
                save.highestReachedWeaponId(),
                save.pityStacks(),
                Instant.now()
        ));
    }
}
