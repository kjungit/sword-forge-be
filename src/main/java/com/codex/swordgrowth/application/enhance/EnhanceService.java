package com.codex.swordgrowth.application.enhance;

import com.codex.swordgrowth.domain.enhance.EnhanceAttemptEntity;
import com.codex.swordgrowth.domain.enhance.EnhanceAttemptRepository;
import com.codex.swordgrowth.domain.enhance.EnhanceTableDefinition;
import com.codex.swordgrowth.application.save.PlayerSaveService;
import com.codex.swordgrowth.application.reward.RewardLogService;
import com.codex.swordgrowth.application.reward.FailureRewardService;
import com.codex.swordgrowth.application.item.SpecialItemCatalogService;
import com.codex.swordgrowth.application.weapon.WeaponCatalogService;
import com.codex.swordgrowth.domain.enhance.EnhanceOutcome;
import com.codex.swordgrowth.domain.item.SpecialItemDefinition;
import com.codex.swordgrowth.domain.save.PlayerSaveData;
import com.codex.swordgrowth.domain.weapon.Grade;
import com.codex.swordgrowth.domain.weapon.WeaponDefinition;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class EnhanceService {

    private final WeaponCatalogService weaponCatalogService;
    private final EnhanceTableService enhanceTableService;
    private final PlayerSaveService playerSaveService;
    private final EnhanceAttemptRepository enhanceAttemptRepository;
    private final FailureRewardService failureRewardService;
    private final RewardLogService rewardLogService;
    private final SpecialItemCatalogService specialItemCatalogService;

    public EnhanceService(
            WeaponCatalogService weaponCatalogService,
            EnhanceTableService enhanceTableService,
            PlayerSaveService playerSaveService,
            EnhanceAttemptRepository enhanceAttemptRepository,
            FailureRewardService failureRewardService,
            RewardLogService rewardLogService,
            SpecialItemCatalogService specialItemCatalogService
    ) {
        this.weaponCatalogService = weaponCatalogService;
        this.enhanceTableService = enhanceTableService;
        this.playerSaveService = playerSaveService;
        this.enhanceAttemptRepository = enhanceAttemptRepository;
        this.failureRewardService = failureRewardService;
        this.rewardLogService = rewardLogService;
        this.specialItemCatalogService = specialItemCatalogService;
    }

    public EnhancePreview preview(String weaponId, boolean useProtection) {
        WeaponDefinition weapon = weaponCatalogService.findById(weaponId);
        EnhanceTableDefinition table = enhanceTableService.findByWeaponId(weaponId);
        String nextWeaponId = weapon.nextWeaponId() != null && weaponCatalogService.exists(weapon.nextWeaponId())
                ? weapon.nextWeaponId()
                : null;
        double adjustedSuccessRate = table.successRate() + (useProtection ? 0.05 : 0.0);
        return new EnhancePreview(
                weapon.id(),
                nextWeaponId,
                table.successRate(),
                adjustedSuccessRate,
                table.failRate(),
                useProtection
        );
    }

    @Transactional
    public EnhanceResult attempt(String userId, String weaponId, boolean useProtection) {
        return attempt(userId, weaponId, useProtection, null, null);
    }

    @Transactional
    public EnhanceResult attempt(String userId, String weaponId, boolean useProtection, Double rollOverride) {
        return attempt(userId, weaponId, useProtection, null, rollOverride);
    }

    @Transactional
    public EnhanceResult attempt(String userId, String weaponId, boolean useProtection, String rateBoostItemId) {
        return attempt(userId, weaponId, useProtection, rateBoostItemId, null);
    }

    @Transactional
    public EnhanceResult attempt(String userId, String weaponId, boolean useProtection, String rateBoostItemId, Double rollOverride) {
        WeaponDefinition weapon = weaponCatalogService.findById(weaponId);
        EnhanceTableDefinition table = enhanceTableService.findByWeaponId(weaponId);
        PlayerSaveData currentSave = playerSaveService.getOrCreate(userId);
        if (!weaponId.equals(currentSave.currentWeaponId())) {
            throw new IllegalArgumentException("current weapon mismatch: " + currentSave.currentWeaponId());
        }

        SpecialItemDefinition rateBoostItem = rateBoostItemId == null ? null : specialItemCatalogService.findById(rateBoostItemId);
        double rateBonus = useProtection ? 0.05 : 0.0;
        if (rateBoostItem != null) {
            rateBonus += switch (rateBoostItem.effect()) {
                case "enhance_rate_plus_5" -> 0.05;
                case "enhance_rate_plus_10" -> 0.10;
                default -> throw new IllegalArgumentException("item cannot be used for enhancement boost: " + rateBoostItem.id());
            };
        }
        double roll = rollOverride != null ? rollOverride : ThreadLocalRandom.current().nextDouble();
        double successThreshold = Math.min(1.0, table.successRate() + rateBonus);
        String protectionItemId = useProtection ? protectionItemIdFor(weapon.grade()) : null;
        if (useProtection && currentSave.specialItems().getOrDefault(protectionItemId, 0) <= 0) {
            throw new IllegalArgumentException("not enough protection item: " + protectionItemId);
        }
        if (rateBoostItem != null && currentSave.specialItems().getOrDefault(rateBoostItem.id(), 0) <= 0) {
            throw new IllegalArgumentException("not enough boost item: " + rateBoostItem.id());
        }

        EnhanceOutcome outcome = roll < successThreshold
                ? EnhanceOutcome.SUCCESS
                : (useProtection ? EnhanceOutcome.PROTECTED_FAIL : EnhanceOutcome.FAIL_DESTROYED);

        String nextWeaponId = outcome == EnhanceOutcome.SUCCESS
                ? resolveNextWeaponId(weapon)
                : null;
        Map<String, Integer> failureRewards = outcome == EnhanceOutcome.SUCCESS
                ? Map.of()
                : failureRewardService.generateRewards(weapon.failureRewardGroup());
        PlayerSaveData updated = playerSaveService.mutate(userId, save -> {
            Map<String, Integer> mergedMaterials = playerSaveService.mergeMaterials(save.materials(), failureRewards);
            Map<String, Integer> updatedSpecialItems = useProtection
                    ? removeItemCount(save.specialItems(), protectionItemId, 1)
                    : save.specialItems();
            if (rateBoostItem != null) {
                updatedSpecialItems = removeItemCount(updatedSpecialItems, rateBoostItem.id(), 1);
            }
            if (outcome == EnhanceOutcome.SUCCESS) {
                Map<String, Integer> updatedInventory = playerSaveService.addWeaponCount(save.weaponInventory(), nextWeaponId, 1);
                updatedInventory = playerSaveService.ensureStarterWeapon(updatedInventory);
                return new PlayerSaveData(
                        save.userId(),
                        nextWeaponId,
                        mergedMaterials,
                        updatedSpecialItems,
                        updatedInventory,
                        playerSaveService.deriveOwnedWeaponIds(updatedInventory),
                        playerSaveService.appendUnique(save.unlockedWeaponShop(), nextWeaponId),
                        playerSaveService.appendUnique(save.discoveredWeaponIds(), nextWeaponId),
                        nextWeaponId,
                        Instant.now()
                );
            }

            if (outcome == EnhanceOutcome.PROTECTED_FAIL) {
                return new PlayerSaveData(
                        save.userId(),
                        save.currentWeaponId(),
                        mergedMaterials,
                        updatedSpecialItems,
                        save.weaponInventory(),
                        playerSaveService.deriveOwnedWeaponIds(save.weaponInventory()),
                        save.unlockedWeaponShop(),
                        save.discoveredWeaponIds(),
                        save.highestReachedWeaponId(),
                        Instant.now()
                );
            }

            Map<String, Integer> updatedInventory = playerSaveService.removeWeaponCount(save.weaponInventory(), weaponId, 1);
            updatedInventory = playerSaveService.ensureStarterWeapon(updatedInventory);
            return new PlayerSaveData(
                    save.userId(),
                    "normal_01",
                    mergedMaterials,
                    updatedSpecialItems,
                    updatedInventory,
                    playerSaveService.deriveOwnedWeaponIds(updatedInventory),
                    save.unlockedWeaponShop(),
                    save.discoveredWeaponIds(),
                    save.highestReachedWeaponId(),
                    Instant.now()
            );
        });
        enhanceAttemptRepository.save(new EnhanceAttemptEntity(
                userId,
                weaponId,
                outcome.name().toLowerCase(),
                roll,
                successThreshold,
                outcome == EnhanceOutcome.PROTECTED_FAIL,
                Instant.now()
        ));
        if (!failureRewards.isEmpty()) {
            rewardLogService.logMaterialRewards(userId, "enhance_attempt", weaponId, failureRewards);
        }

        return new EnhanceResult(
                userId,
                weaponId,
                outcome,
                roll,
                successThreshold,
                nextWeaponId,
                outcome == EnhanceOutcome.PROTECTED_FAIL,
                failureRewards,
                rateBoostItem == null ? null : rateBoostItem.id()
        );
    }

    private String protectionItemIdFor(Grade grade) {
        return switch (grade) {
            case NORMAL -> "basic_protection_ticket";
            case RARE -> "middle_protection_ticket";
            case EPIC -> "advanced_protection_ticket";
            case LEGENDARY -> "legendary_protection_ticket";
        };
    }

    private Map<String, Integer> removeItemCount(Map<String, Integer> inventory, String itemId, int amount) {
        if (amount <= 0) {
            return Map.copyOf(inventory);
        }
        Map<String, Integer> updated = new java.util.LinkedHashMap<>(inventory);
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

    private String resolveNextWeaponId(WeaponDefinition weapon) {
        String nextWeaponId = weapon.nextWeaponId();
        if (nextWeaponId == null || !weaponCatalogService.exists(nextWeaponId)) {
            throw new IllegalArgumentException("weapon has no next stage: " + weapon.id());
        }
        return nextWeaponId;
    }

    public record EnhancePreview(
            String weaponId,
            String nextWeaponId,
            double baseSuccessRate,
            double adjustedSuccessRate,
            double failRate,
            boolean protectedAttempt
    ) {
    }

    public record EnhanceResult(
            String userId,
            String weaponId,
            EnhanceOutcome outcome,
            double roll,
            double successThreshold,
            String nextWeaponId,
            boolean protectionUsed,
            Map<String, Integer> failureRewards,
            String rateBoostItemId
    ) {
    }
}
