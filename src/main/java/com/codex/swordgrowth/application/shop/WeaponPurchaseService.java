package com.codex.swordgrowth.application.shop;

import com.codex.swordgrowth.application.save.PlayerSaveService;
import com.codex.swordgrowth.application.weapon.WeaponCatalogService;
import com.codex.swordgrowth.domain.save.PlayerSaveData;
import com.codex.swordgrowth.domain.weapon.WeaponDefinition;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class WeaponPurchaseService {

    private final WeaponCatalogService weaponCatalogService;
    private final PlayerSaveService playerSaveService;
    private final WeaponPurchaseCostService weaponPurchaseCostService;

    public WeaponPurchaseService(
            WeaponCatalogService weaponCatalogService,
            PlayerSaveService playerSaveService,
            WeaponPurchaseCostService weaponPurchaseCostService
    ) {
        this.weaponCatalogService = weaponCatalogService;
        this.playerSaveService = playerSaveService;
        this.weaponPurchaseCostService = weaponPurchaseCostService;
    }

    public PurchasePreview preview(String weaponId) {
        WeaponDefinition weapon = weaponCatalogService.findById(weaponId);
        return new PurchasePreview(weaponId, calculateCost(weapon));
    }

    @Transactional
    public PurchaseResult purchase(String userId, String weaponId) {
        WeaponDefinition weapon = weaponCatalogService.findById(weaponId);
        PlayerSaveData updated = playerSaveService.mutate(userId, save -> {
            if (!save.unlockedWeaponShop().contains(weaponId)) {
                throw new IllegalArgumentException("weapon is not unlocked yet: " + weaponId);
            }

            Map<String, Integer> cost = calculateCost(weapon);
            assertEnoughMaterials(save.materials(), cost);
            Map<String, Integer> remainingMaterials = deductMaterials(save.materials(), cost);

            List<String> unlockedWeaponShop = playerSaveService.appendUnique(save.unlockedWeaponShop(), weaponId);
            List<String> discoveredWeaponIds = playerSaveService.appendUnique(save.discoveredWeaponIds(), weaponId);
            Map<String, Integer> updatedInventory = playerSaveService.addWeaponCount(save.weaponInventory(), weaponId, 1);
            updatedInventory = playerSaveService.ensureStarterWeapon(updatedInventory);

            return new PlayerSaveData(
                    save.userId(),
                    weaponId,
                    remainingMaterials,
                    save.specialItems(),
                    updatedInventory,
                    playerSaveService.deriveOwnedWeaponIds(updatedInventory),
                    unlockedWeaponShop,
                    discoveredWeaponIds,
                    save.highestReachedWeaponId(),
                    Instant.now()
            );
        });

        return new PurchaseResult(userId, weaponId, calculateCost(weapon), updated.materials(), updated.currentWeaponId());
    }

    private void assertEnoughMaterials(Map<String, Integer> currentMaterials, Map<String, Integer> cost) {
        for (Map.Entry<String, Integer> entry : cost.entrySet()) {
            int owned = currentMaterials.getOrDefault(entry.getKey(), 0);
            if (owned < entry.getValue()) {
                throw new IllegalArgumentException("not enough materials: " + entry.getKey());
            }
        }
    }

    private Map<String, Integer> deductMaterials(Map<String, Integer> currentMaterials, Map<String, Integer> cost) {
        Map<String, Integer> remaining = new LinkedHashMap<>(currentMaterials);
        for (Map.Entry<String, Integer> entry : cost.entrySet()) {
            remaining.merge(entry.getKey(), -entry.getValue(), Integer::sum);
            if (remaining.getOrDefault(entry.getKey(), 0) <= 0) {
                remaining.remove(entry.getKey());
            }
        }
        return Map.copyOf(remaining);
    }

    private Map<String, Integer> calculateCost(WeaponDefinition weapon) {
        return weaponPurchaseCostService.findCost(weapon.id());
    }

    public record PurchasePreview(
            String weaponId,
            Map<String, Integer> cost
    ) {
    }

    public record PurchaseResult(
            String userId,
            String weaponId,
            Map<String, Integer> cost,
            Map<String, Integer> remainingMaterials,
            String equippedWeaponId
    ) {
    }
}
