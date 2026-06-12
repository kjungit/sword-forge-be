package com.swordforge.application.item;

import com.swordforge.application.economy.EconomyLogService;
import com.swordforge.application.save.PlayerSaveService;
import com.swordforge.application.reward.RewardLogService;
import com.swordforge.domain.save.PlayerSaveData;
import com.swordforge.domain.item.SpecialItemDefinition;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class SpecialItemService {

    private final PlayerSaveService playerSaveService;
    private final SpecialItemCatalogService specialItemCatalogService;
    private final ItemPurchasePriceService itemPurchasePriceService;
    private final RewardLogService rewardLogService;
    private final EconomyLogService economyLogService;

    public SpecialItemService(
            PlayerSaveService playerSaveService,
            SpecialItemCatalogService specialItemCatalogService,
            ItemPurchasePriceService itemPurchasePriceService,
            RewardLogService rewardLogService,
            EconomyLogService economyLogService
    ) {
        this.playerSaveService = playerSaveService;
        this.specialItemCatalogService = specialItemCatalogService;
        this.itemPurchasePriceService = itemPurchasePriceService;
        this.rewardLogService = rewardLogService;
        this.economyLogService = economyLogService;
    }

    public SpecialItemDefinition preview(String itemId) {
        return specialItemCatalogService.findById(itemId);
    }

    @Transactional
    public PlayerSaveData grant(String userId, String itemId, int amount) {
        specialItemCatalogService.findById(itemId);
        PlayerSaveData updatedSave = playerSaveService.mutate(userId, save -> {
            Map<String, Integer> updated = addCount(save.specialItems(), itemId, amount);
            return new PlayerSaveData(
                    save.userId(),
                    save.currentWeaponId(),
                    save.materials(),
                    updated,
                    save.weaponInventory(),
                    save.lockedWeaponIds(),
                    save.ownedWeaponIds(),
                    save.unlockedWeaponShop(),
                    save.discoveredWeaponIds(),
                    save.highestReachedWeaponId(),
                    save.pityStacks(),
                    Instant.now()
            );
        });
        rewardLogService.logSpecialItemReward(userId, "item_grant", itemId, itemId, amount);
        economyLogService.logSpecialItemDeltas(
                userId,
                "item_grant",
                itemId,
                Map.of(itemId, amount),
                updatedSave.specialItems(),
                Map.of("itemId", itemId, "amount", amount)
        );
        return updatedSave;
    }

    @Transactional
    public PlayerSaveData consume(String userId, String itemId, int amount) {
        specialItemCatalogService.findById(itemId);
        PlayerSaveData updatedSave = playerSaveService.mutate(userId, save -> {
            Map<String, Integer> updated = removeCount(save.specialItems(), itemId, amount);
            return new PlayerSaveData(
                    save.userId(),
                    save.currentWeaponId(),
                    save.materials(),
                    updated,
                    save.weaponInventory(),
                    save.lockedWeaponIds(),
                    save.ownedWeaponIds(),
                    save.unlockedWeaponShop(),
                    save.discoveredWeaponIds(),
                    save.highestReachedWeaponId(),
                    save.pityStacks(),
                    Instant.now()
            );
        });
        economyLogService.logSpecialItemDeltas(
                userId,
                "item_consume",
                itemId,
                Map.of(itemId, -amount),
                updatedSave.specialItems(),
                Map.of("itemId", itemId, "amount", amount)
        );
        return updatedSave;
    }

    @Transactional
    public PurchaseResult purchase(String userId, String itemId, int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("purchase amount must be positive");
        }
        specialItemCatalogService.findById(itemId);
        int unitGoldPrice = itemPurchasePriceService.findPrice(itemId);
        int totalGoldCost = unitGoldPrice * amount;
        PlayerSaveData updatedSave = playerSaveService.mutate(userId, save -> {
            Map<String, Integer> updatedMaterials = playerSaveService.spendGold(save.materials(), totalGoldCost);
            Map<String, Integer> updatedItems = addCount(save.specialItems(), itemId, amount);
            return new PlayerSaveData(
                    save.userId(),
                    save.currentWeaponId(),
                    updatedMaterials,
                    updatedItems,
                    save.weaponInventory(),
                    save.lockedWeaponIds(),
                    save.ownedWeaponIds(),
                    save.unlockedWeaponShop(),
                    save.discoveredWeaponIds(),
                    save.highestReachedWeaponId(),
                    save.pityStacks(),
                    Instant.now()
            );
        });
        rewardLogService.logSpecialItemReward(userId, "item_purchase", itemId, itemId, amount);
        economyLogService.logMaterialDeltas(
                userId,
                "item_purchase_gold_cost",
                itemId,
                Map.of(PlayerSaveService.GOLD_MATERIAL_ID, -totalGoldCost),
                updatedSave.materials(),
                Map.of("itemId", itemId, "amount", amount)
        );
        economyLogService.logSpecialItemDeltas(
                userId,
                "item_purchase",
                itemId,
                Map.of(itemId, amount),
                updatedSave.specialItems(),
                Map.of("itemId", itemId, "amount", amount)
        );

        return new PurchaseResult(
                userId,
                itemId,
                amount,
                unitGoldPrice,
                totalGoldCost,
                playerSaveService.goldOf(updatedSave.materials()),
                updatedSave.specialItems()
        );
    }

    private Map<String, Integer> addCount(Map<String, Integer> inventory, String itemId, int amount) {
        Map<String, Integer> updated = new LinkedHashMap<>(inventory);
        updated.merge(itemId, amount, Integer::sum);
        return Map.copyOf(updated);
    }

    private Map<String, Integer> removeCount(Map<String, Integer> inventory, String itemId, int amount) {
        Map<String, Integer> updated = new LinkedHashMap<>(inventory);
        int current = updated.getOrDefault(itemId, 0);
        if (current < amount) {
            throw new IllegalArgumentException("not enough special items: " + itemId);
        }
        int next = current - amount;
        if (next <= 0) {
            updated.remove(itemId);
        } else {
            updated.put(itemId, next);
        }
        return Map.copyOf(updated);
    }

    public record PurchaseResult(
            String userId,
            String itemId,
            int amount,
            int unitGoldPrice,
            int totalGoldCost,
            int remainingGold,
            Map<String, Integer> specialItems
    ) {
    }
}
