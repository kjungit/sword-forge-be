package com.codex.swordgrowth.application.item;

import com.codex.swordgrowth.application.save.PlayerSaveService;
import com.codex.swordgrowth.application.reward.RewardLogService;
import com.codex.swordgrowth.domain.save.PlayerSaveData;
import com.codex.swordgrowth.domain.item.SpecialItemDefinition;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class SpecialItemService {

    private final PlayerSaveService playerSaveService;
    private final SpecialItemCatalogService specialItemCatalogService;
    private final RewardLogService rewardLogService;

    public SpecialItemService(
            PlayerSaveService playerSaveService,
            SpecialItemCatalogService specialItemCatalogService,
            RewardLogService rewardLogService
    ) {
        this.playerSaveService = playerSaveService;
        this.specialItemCatalogService = specialItemCatalogService;
        this.rewardLogService = rewardLogService;
    }

    public SpecialItemDefinition preview(String itemId) {
        return specialItemCatalogService.findById(itemId);
    }

    @Transactional
    public PlayerSaveData grant(String userId, String itemId, int amount) {
        specialItemCatalogService.findById(itemId);
        return playerSaveService.mutate(userId, save -> {
            Map<String, Integer> updated = addCount(save.specialItems(), itemId, amount);
            rewardLogService.logSpecialItemReward(userId, "item_grant", itemId, itemId, amount);
            return new PlayerSaveData(
                    save.userId(),
                    save.currentWeaponId(),
                    save.materials(),
                    updated,
                    save.weaponInventory(),
                    save.ownedWeaponIds(),
                    save.unlockedWeaponShop(),
                    save.discoveredWeaponIds(),
                    save.highestReachedWeaponId(),
                    Instant.now()
            );
        });
    }

    @Transactional
    public PlayerSaveData consume(String userId, String itemId, int amount) {
        specialItemCatalogService.findById(itemId);
        return playerSaveService.mutate(userId, save -> {
            Map<String, Integer> updated = removeCount(save.specialItems(), itemId, amount);
            return new PlayerSaveData(
                    save.userId(),
                    save.currentWeaponId(),
                    save.materials(),
                    updated,
                    save.weaponInventory(),
                    save.ownedWeaponIds(),
                    save.unlockedWeaponShop(),
                    save.discoveredWeaponIds(),
                    save.highestReachedWeaponId(),
                    Instant.now()
            );
        });
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
}
