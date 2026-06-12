package com.swordforge.application.enhance;

import com.swordforge.domain.enhance.EnhanceAttemptEntity;
import com.swordforge.domain.enhance.EnhanceAttemptRepository;
import com.swordforge.domain.enhance.EnhanceTableDefinition;
import com.swordforge.application.save.PlayerSaveService;
import com.swordforge.application.reward.RewardLogService;
import com.swordforge.application.reward.FailureRewardService;
import com.swordforge.application.item.SpecialItemCatalogService;
import com.swordforge.application.weapon.WeaponCatalogService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swordforge.domain.enhance.EnhanceOutcome;
import com.swordforge.domain.item.SpecialItemDefinition;
import com.swordforge.domain.save.PlayerSaveData;
import com.swordforge.domain.weapon.Grade;
import com.swordforge.domain.weapon.WeaponDefinition;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class EnhanceService {

    private static final double PITY_STACK_RATE_BONUS = 0.02;
    private static final double MAX_PITY_RATE_BONUS = 0.20;

    private final WeaponCatalogService weaponCatalogService;
    private final EnhanceTableService enhanceTableService;
    private final PlayerSaveService playerSaveService;
    private final EnhanceAttemptRepository enhanceAttemptRepository;
    private final FailureRewardService failureRewardService;
    private final RewardLogService rewardLogService;
    private final SpecialItemCatalogService specialItemCatalogService;
    private final EnhanceCostService enhanceCostService;
    private final ObjectMapper objectMapper;

    public EnhanceService(
            WeaponCatalogService weaponCatalogService,
            EnhanceTableService enhanceTableService,
            PlayerSaveService playerSaveService,
            EnhanceAttemptRepository enhanceAttemptRepository,
            FailureRewardService failureRewardService,
            RewardLogService rewardLogService,
            SpecialItemCatalogService specialItemCatalogService,
            EnhanceCostService enhanceCostService,
            ObjectMapper objectMapper
    ) {
        this.weaponCatalogService = weaponCatalogService;
        this.enhanceTableService = enhanceTableService;
        this.playerSaveService = playerSaveService;
        this.enhanceAttemptRepository = enhanceAttemptRepository;
        this.failureRewardService = failureRewardService;
        this.rewardLogService = rewardLogService;
        this.specialItemCatalogService = specialItemCatalogService;
        this.enhanceCostService = enhanceCostService;
        this.objectMapper = objectMapper;
    }

    public EnhancePreview preview(String weaponId, boolean useProtection) {
        return preview(null, weaponId, useProtection);
    }

    public EnhancePreview preview(String userId, String weaponId, boolean useProtection) {
        WeaponDefinition weapon = weaponCatalogService.findById(weaponId);
        EnhanceTableDefinition table = enhanceTableService.findByWeaponId(weaponId);
        String nextWeaponId = weapon.nextWeaponId() != null && weaponCatalogService.exists(weapon.nextWeaponId())
                ? weapon.nextWeaponId()
                : null;
        String pityKey = weapon.grade().code();
        int pityStack = userId == null ? 0 : playerSaveService.getOrCreate(userId).pityStacks().getOrDefault(pityKey, 0);
        double pityBonus = Math.min(MAX_PITY_RATE_BONUS, pityStack * PITY_STACK_RATE_BONUS);
        double adjustedSuccessRate = Math.min(1.0, table.successRate() + pityBonus + (useProtection ? 0.05 : 0.0));
        int goldCost = enhanceCostService.findCost(weaponId);
        return new EnhancePreview(
                weapon.id(),
                nextWeaponId,
                table.successRate(),
                adjustedSuccessRate,
                table.failRate(),
                goldCost,
                useProtection,
                pityKey,
                pityStack,
                pityBonus
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
        String nextWeaponId = resolveNextWeaponId(weapon);
        int goldCost = enhanceCostService.findCost(weaponId);
        String pityKey = weapon.grade().code();
        int pityStackBefore = currentSave.pityStacks().getOrDefault(pityKey, 0);
        double pityBonus = Math.min(MAX_PITY_RATE_BONUS, pityStackBefore * PITY_STACK_RATE_BONUS);

        SpecialItemDefinition rateBoostItem = rateBoostItemId == null ? null : specialItemCatalogService.findById(rateBoostItemId);
        double rateBonus = pityBonus + (useProtection ? 0.05 : 0.0);
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
        if (playerSaveService.goldOf(currentSave.materials()) < goldCost) {
            throw new IllegalArgumentException("not enough gold");
        }

        EnhanceOutcome outcome = roll < successThreshold
                ? EnhanceOutcome.SUCCESS
                : (useProtection ? EnhanceOutcome.PROTECTED_FAIL : EnhanceOutcome.FAIL_DESTROYED);
        Map<String, Integer> failureRewards = outcome == EnhanceOutcome.SUCCESS
                ? Map.of()
                : failureRewardService.generateRewards(weapon.failureRewardGroup());
        PlayerSaveData updated = playerSaveService.mutate(userId, save -> {
            Map<String, Integer> materialsAfterCost = playerSaveService.spendGold(save.materials(), goldCost);
            Map<String, Integer> mergedMaterials = playerSaveService.mergeMaterials(materialsAfterCost, failureRewards);
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
                        save.lockedWeaponIds(),
                        playerSaveService.deriveOwnedWeaponIds(updatedInventory),
                        playerSaveService.appendUnique(save.unlockedWeaponShop(), nextWeaponId),
                        playerSaveService.appendUnique(save.discoveredWeaponIds(), nextWeaponId),
                        nextWeaponId,
                        Map.copyOf(save.pityStacks()),
                        Instant.now()
                );
            }

            Map<String, Integer> updatedPityStacks = playerSaveService.incrementPityStack(save.pityStacks(), pityKey);
            if (outcome == EnhanceOutcome.PROTECTED_FAIL) {
                return new PlayerSaveData(
                        save.userId(),
                        save.currentWeaponId(),
                        mergedMaterials,
                        updatedSpecialItems,
                        save.weaponInventory(),
                        save.lockedWeaponIds(),
                        playerSaveService.deriveOwnedWeaponIds(save.weaponInventory()),
                        save.unlockedWeaponShop(),
                        save.discoveredWeaponIds(),
                        save.highestReachedWeaponId(),
                        updatedPityStacks,
                        Instant.now()
                );
            }

            Map<String, Integer> updatedInventory = playerSaveService.removeWeaponCount(save.weaponInventory(), weaponId, 1);
            updatedInventory = playerSaveService.ensureStarterWeapon(updatedInventory);
            updatedInventory = ensureGradeFloor(updatedInventory, save.discoveredWeaponIds(), weapon.grade());
            String fallbackWeaponId = chooseBestOwnedWeaponId(updatedInventory);
            return new PlayerSaveData(
                    save.userId(),
                    fallbackWeaponId,
                    mergedMaterials,
                    updatedSpecialItems,
                    updatedInventory,
                    save.lockedWeaponIds(),
                    playerSaveService.deriveOwnedWeaponIds(updatedInventory),
                    save.unlockedWeaponShop(),
                    save.discoveredWeaponIds(),
                    save.highestReachedWeaponId(),
                    updatedPityStacks,
                    Instant.now()
            );
        });
        int pityStackAfter = updated.pityStacks().getOrDefault(pityKey, 0);
        enhanceAttemptRepository.save(new EnhanceAttemptEntity(
                userId,
                weaponId,
                outcome.name().toLowerCase(),
                roll,
                successThreshold,
                outcome == EnhanceOutcome.PROTECTED_FAIL,
                writeDetailsJson(Map.of(
                        "beforeWeaponId", currentSave.currentWeaponId(),
                        "afterWeaponId", updated.currentWeaponId(),
                        "pityKey", pityKey,
                        "pityStackBefore", pityStackBefore,
                        "pityStackAfter", pityStackAfter,
                        "pityBonus", pityBonus,
                        "goldCost", goldCost,
                        "remainingGold", playerSaveService.goldOf(updated.materials()),
                        "failureRewards", failureRewards,
                        "rateBoostItemId", rateBoostItem == null ? "" : rateBoostItem.id()
                )),
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
                outcome == EnhanceOutcome.SUCCESS ? nextWeaponId : null,
                outcome == EnhanceOutcome.PROTECTED_FAIL,
                failureRewards,
                rateBoostItem == null ? null : rateBoostItem.id(),
                goldCost,
                playerSaveService.goldOf(updated.materials()),
                pityKey,
                pityStackBefore,
                pityStackAfter,
                pityBonus
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
            throw new IllegalArgumentException("weapon requires evolution or has no next stage: " + weapon.id());
        }
        WeaponDefinition nextWeapon = weaponCatalogService.findById(nextWeaponId);
        if (nextWeapon.grade() != weapon.grade()) {
            throw new IllegalArgumentException("grade upgrade requires evolution: " + weapon.id());
        }
        return nextWeaponId;
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

    private Map<String, Integer> ensureGradeFloor(
            Map<String, Integer> inventory,
            java.util.List<String> discoveredWeaponIds,
            Grade destroyedGrade
    ) {
        if (destroyedGrade == Grade.NORMAL) {
            return inventory;
        }
        boolean hasGradeWeapon = inventory.entrySet().stream()
                .filter(entry -> entry.getValue() != null && entry.getValue() > 0)
                .map(entry -> weaponCatalogService.findById(entry.getKey()))
                .anyMatch(weapon -> weapon.grade() == destroyedGrade);
        if (hasGradeWeapon) {
            return inventory;
        }
        String floorWeaponId = weaponCatalogService.findFirstByGrade(destroyedGrade).id();
        if (!discoveredWeaponIds.contains(floorWeaponId)) {
            return inventory;
        }
        return playerSaveService.addWeaponCount(inventory, floorWeaponId, 1);
    }

    private String writeDetailsJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(new LinkedHashMap<>(value));
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("failed to serialize enhance log details", ex);
        }
    }

    public record EnhancePreview(
            String weaponId,
            String nextWeaponId,
            double baseSuccessRate,
            double adjustedSuccessRate,
            double failRate,
            int goldCost,
            boolean protectedAttempt,
            String pityKey,
            int pityStack,
            double pityBonus
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
            String rateBoostItemId,
            int goldCost,
            int remainingGold,
            String pityKey,
            int pityStackBefore,
            int pityStackAfter,
            double pityBonus
    ) {
    }
}
