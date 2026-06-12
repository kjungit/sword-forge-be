package com.swordforge.application.shop;

import com.swordforge.application.save.PlayerSaveService;
import com.swordforge.application.reward.RewardLogService;
import com.swordforge.application.weapon.WeaponCatalogService;
import com.swordforge.domain.save.PlayerSaveData;
import com.swordforge.domain.weapon.WeaponDefinition;
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
    private final WeaponSalePriceService weaponSalePriceService;
    private final RewardLogService rewardLogService;

    public WeaponPurchaseService(
            WeaponCatalogService weaponCatalogService,
            PlayerSaveService playerSaveService,
            WeaponPurchaseCostService weaponPurchaseCostService,
            WeaponSalePriceService weaponSalePriceService,
            RewardLogService rewardLogService
    ) {
        this.weaponCatalogService = weaponCatalogService;
        this.playerSaveService = playerSaveService;
        this.weaponPurchaseCostService = weaponPurchaseCostService;
        this.weaponSalePriceService = weaponSalePriceService;
        this.rewardLogService = rewardLogService;
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
                    save.lockedWeaponIds(),
                    playerSaveService.deriveOwnedWeaponIds(updatedInventory),
                    unlockedWeaponShop,
                    discoveredWeaponIds,
                    save.highestReachedWeaponId(),
                    save.pityStacks(),
                    Instant.now()
            );
        });

        return new PurchaseResult(userId, weaponId, calculateCost(weapon), updated.materials(), updated.currentWeaponId());
    }

    public SalePreview salePreview(String weaponId) {
        weaponCatalogService.findById(weaponId);
        return new SalePreview(weaponId, weaponSalePriceService.findPrice(weaponId));
    }

    @Transactional
    public SaleResult sell(String userId, String weaponId, int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("sale amount must be positive");
        }
        weaponCatalogService.findById(weaponId);
        int unitGoldPrice = weaponSalePriceService.findPrice(weaponId);
        int totalGold = unitGoldPrice * amount;
        PlayerSaveData updated = playerSaveService.mutate(userId, save -> {
            int ownedCount = save.weaponInventory().getOrDefault(weaponId, 0);
            if (ownedCount < amount) {
                throw new IllegalArgumentException("not enough weapon copies: " + weaponId);
            }
            if (save.lockedWeaponIds().contains(weaponId)) {
                throw new IllegalArgumentException("weapon is locked: " + weaponId);
            }
            if (totalOwnedWeaponCount(save.weaponInventory()) <= amount) {
                throw new IllegalArgumentException("cannot sell all owned weapons");
            }

            Map<String, Integer> updatedInventory = playerSaveService.removeWeaponCount(save.weaponInventory(), weaponId, amount);
            String nextCurrentWeaponId = save.currentWeaponId();
            if (updatedInventory.getOrDefault(nextCurrentWeaponId, 0) <= 0) {
                nextCurrentWeaponId = chooseBestOwnedWeaponId(updatedInventory);
            }
            Map<String, Integer> updatedMaterials = playerSaveService.addGold(save.materials(), totalGold);

            return new PlayerSaveData(
                    save.userId(),
                    nextCurrentWeaponId,
                    updatedMaterials,
                    save.specialItems(),
                    updatedInventory,
                    save.lockedWeaponIds(),
                    playerSaveService.deriveOwnedWeaponIds(updatedInventory),
                    save.unlockedWeaponShop(),
                    save.discoveredWeaponIds(),
                    save.highestReachedWeaponId(),
                    save.pityStacks(),
                    Instant.now()
            );
        });
        rewardLogService.logMaterialRewards(userId, "weapon_sale", weaponId, Map.of(PlayerSaveService.GOLD_MATERIAL_ID, totalGold));

        return new SaleResult(
                userId,
                weaponId,
                amount,
                unitGoldPrice,
                totalGold,
                playerSaveService.goldOf(updated.materials()),
                updated.currentWeaponId(),
                updated.weaponInventory()
        );
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

    private int totalOwnedWeaponCount(Map<String, Integer> inventory) {
        return inventory.values().stream()
                .filter(value -> value != null && value > 0)
                .mapToInt(Integer::intValue)
                .sum();
    }

    private String chooseBestOwnedWeaponId(Map<String, Integer> inventory) {
        return inventory.entrySet().stream()
                .filter(entry -> entry.getValue() != null && entry.getValue() > 0)
                .map(entry -> weaponCatalogService.findById(entry.getKey()))
                .max((left, right) -> {
                    int gradeCompare = Integer.compare(left.grade().ordinal(), right.grade().ordinal());
                    if (gradeCompare != 0) {
                        return gradeCompare;
                    }
                    return Integer.compare(left.stage(), right.stage());
                })
                .map(WeaponDefinition::id)
                .orElse("normal_01");
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

    public record SalePreview(
            String weaponId,
            int goldPrice
    ) {
    }

    public record SaleResult(
            String userId,
            String weaponId,
            int amount,
            int unitGoldPrice,
            int totalGold,
            int remainingGold,
            String equippedWeaponId,
            Map<String, Integer> weaponInventory
    ) {
    }
}
