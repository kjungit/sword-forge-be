package com.swordforge.application.enhance;

import com.swordforge.domain.enhance.EnhanceAttemptEntity;
import com.swordforge.domain.enhance.EnhanceAttemptRepository;
import com.swordforge.domain.enhance.EnhanceTableDefinition;
import com.swordforge.application.economy.EconomyLogService;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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
    private final EconomyLogService economyLogService;
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
            EconomyLogService economyLogService,
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
        this.economyLogService = economyLogService;
        this.objectMapper = objectMapper;
    }

    public EnhancePreview preview(String weaponId, boolean useProtection) {
        return preview(null, weaponId, useProtection, null);
    }

    public EnhancePreview preview(String userId, String weaponId, boolean useProtection) {
        return preview(userId, weaponId, useProtection, null);
    }

    public EnhancePreview preview(String userId, String weaponId, boolean useProtection, String rateBoostItemId) {
        WeaponDefinition weapon = weaponCatalogService.findById(weaponId);
        EnhanceTableDefinition table = enhanceTableService.findByWeaponId(weaponId);
        String nextWeaponId = weapon.nextWeaponId() != null && weaponCatalogService.exists(weapon.nextWeaponId())
                ? weapon.nextWeaponId()
                : null;
        boolean enhancementAvailable = isSameGradeEnhancementAvailable(weapon, nextWeaponId);
        PlayerSaveData save = userId == null ? null : playerSaveService.getOrCreate(userId);
        String pityKey = weapon.grade().code();
        int pityStack = save == null ? 0 : save.pityStacks().getOrDefault(pityKey, 0);
        double pityBonus = Math.min(MAX_PITY_RATE_BONUS, pityStack * PITY_STACK_RATE_BONUS);
        String protectionItemId = protectionItemIdFor(weapon.grade());
        boolean protectionApplicable = protectionItemId != null;
        boolean protectionAvailable = protectionApplicable
                && save != null
                && save.specialItems().getOrDefault(protectionItemId, 0) > 0;
        boolean effectiveProtection = enhancementAvailable && useProtection && protectionApplicable;
        SpecialItemDefinition rateBoostItem = resolveRateBoostItem(weapon, rateBoostItemId);
        double rateBoostBonus = rateBoostBonus(rateBoostItem);
        double adjustedSuccessRate = enhancementAvailable
                ? Math.min(1.0, table.successRate() + pityBonus + (effectiveProtection ? 0.05 : 0.0) + rateBoostBonus)
                : 0.0;
        int goldCost = enhancementAvailable ? enhanceCostService.findCost(weaponId) : 0;
        List<ResourceRequirement> requiredItems = requiredItems(effectiveProtection, protectionItemId, rateBoostItem);
        List<MissingResource> missingResources = missingResources(save, goldCost, requiredItems);
        boolean canAfford = missingResources.isEmpty();
        String failureResult = !enhancementAvailable
                ? "requires_evolution_or_no_next"
                : (effectiveProtection ? "protected_fail" : "fail_destroyed");
        return new EnhancePreview(
                weapon.id(),
                nextWeaponId,
                table.successRate(),
                adjustedSuccessRate,
                adjustedSuccessRate,
                table.failRate(),
                goldCost,
                requiredItems,
                enhancementAvailable && !effectiveProtection,
                protectionAvailable,
                canAfford,
                missingResources,
                failureResult,
                effectiveProtection,
                enhancementAvailable,
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

        SpecialItemDefinition rateBoostItem = resolveRateBoostItem(weapon, rateBoostItemId);
        String protectionItemId = protectionItemIdFor(weapon.grade());
        boolean effectiveProtection = useProtection && protectionItemId != null;
        double rateBonus = pityBonus + (effectiveProtection ? 0.05 : 0.0);
        if (rateBoostItem != null) {
            rateBonus += rateBoostBonus(rateBoostItem);
        }
        double roll = rollOverride != null ? rollOverride : ThreadLocalRandom.current().nextDouble();
        double successThreshold = Math.min(1.0, table.successRate() + rateBonus);
        if (effectiveProtection && currentSave.specialItems().getOrDefault(protectionItemId, 0) <= 0) {
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
                : (effectiveProtection ? EnhanceOutcome.PROTECTED_FAIL : EnhanceOutcome.FAIL_DESTROYED);
        Map<String, Integer> failureRewards = outcome == EnhanceOutcome.SUCCESS
                ? Map.of()
                : failureRewardService.generateRewards(weapon.failureRewardGroup());
        PlayerSaveData updated = playerSaveService.mutate(userId, save -> {
            Map<String, Integer> materialsAfterCost = playerSaveService.spendGold(save.materials(), goldCost);
            Map<String, Integer> mergedMaterials = playerSaveService.mergeMaterials(materialsAfterCost, failureRewards);
            Map<String, Integer> updatedSpecialItems = effectiveProtection
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
                effectiveProtection,
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
            economyLogService.logMaterialDeltas(
                    userId,
                    "enhance_failure_reward",
                    weaponId,
                    failureRewards,
                    updated.materials(),
                    Map.of("weaponId", weaponId, "outcome", outcome.name().toLowerCase())
            );
        }
        economyLogService.logMaterialDeltas(
                userId,
                "enhance_cost",
                weaponId,
                Map.of(PlayerSaveService.GOLD_MATERIAL_ID, -goldCost),
                updated.materials(),
                Map.of("weaponId", weaponId, "outcome", outcome.name().toLowerCase())
        );
        EnhancePreview nextPreview = nextPreviewFor(userId, updated.currentWeaponId());
        boolean canRetry = nextPreview != null && nextPreview.enhancementAvailable() && nextPreview.canAfford();

        return new EnhanceResult(
                userId,
                weaponId,
                outcome,
                roll,
                successThreshold,
                outcome == EnhanceOutcome.SUCCESS ? nextWeaponId : null,
                effectiveProtection,
                failureRewards,
                rateBoostItem == null ? null : rateBoostItem.id(),
                goldCost,
                playerSaveService.goldOf(updated.materials()),
                updated.currentWeaponId(),
                updated.currentWeaponId(),
                updated.materials(),
                nextPreview,
                canRetry,
                nextPreview == null ? List.of() : nextPreview.missingResources(),
                pityKey,
                pityStackBefore,
                pityStackAfter,
                pityBonus
        );
    }

    private String protectionItemIdFor(Grade grade) {
        return switch (grade) {
            case NORMAL -> null;
            case RARE -> "middle_protection_ticket";
            case EPIC -> "advanced_protection_ticket";
            case LEGENDARY -> "legendary_protection_ticket";
        };
    }

    private boolean isSameGradeEnhancementAvailable(WeaponDefinition weapon, String nextWeaponId) {
        if (nextWeaponId == null) {
            return false;
        }
        return weaponCatalogService.findById(nextWeaponId).grade() == weapon.grade();
    }

    private double rateBoostBonus(SpecialItemDefinition rateBoostItem) {
        if (rateBoostItem == null) {
            return 0.0;
        }
        return switch (rateBoostItem.effect()) {
            case "enhance_rate_plus_5" -> 0.05;
            case "enhance_rate_plus_10" -> 0.10;
            default -> throw new IllegalArgumentException("item cannot be used for enhancement boost: " + rateBoostItem.id());
        };
    }

    private SpecialItemDefinition resolveRateBoostItem(WeaponDefinition weapon, String rateBoostItemId) {
        if (weapon.grade() == Grade.NORMAL || rateBoostItemId == null || rateBoostItemId.isBlank()) {
            return null;
        }
        return specialItemCatalogService.findById(rateBoostItemId);
    }

    private List<ResourceRequirement> requiredItems(
            boolean effectiveProtection,
            String protectionItemId,
            SpecialItemDefinition rateBoostItem
    ) {
        List<ResourceRequirement> requiredItems = new ArrayList<>();
        if (effectiveProtection && protectionItemId != null) {
            requiredItems.add(new ResourceRequirement("special_item", protectionItemId, 1));
        }
        if (rateBoostItem != null) {
            requiredItems.add(new ResourceRequirement("special_item", rateBoostItem.id(), 1));
        }
        return List.copyOf(requiredItems);
    }

    private List<MissingResource> missingResources(
            PlayerSaveData save,
            int goldCost,
            List<ResourceRequirement> requiredItems
    ) {
        if (save == null) {
            return List.of();
        }
        List<MissingResource> missingResources = new ArrayList<>();
        int ownedGold = playerSaveService.goldOf(save.materials());
        if (ownedGold < goldCost) {
            missingResources.add(new MissingResource(
                    "material",
                    PlayerSaveService.GOLD_MATERIAL_ID,
                    goldCost,
                    ownedGold,
                    goldCost - ownedGold
            ));
        }
        for (ResourceRequirement requiredItem : requiredItems) {
            int owned = switch (requiredItem.resourceKind()) {
                case "special_item" -> save.specialItems().getOrDefault(requiredItem.resourceId(), 0);
                case "material" -> save.materials().getOrDefault(requiredItem.resourceId(), 0);
                default -> 0;
            };
            if (owned < requiredItem.amount()) {
                missingResources.add(new MissingResource(
                        requiredItem.resourceKind(),
                        requiredItem.resourceId(),
                        requiredItem.amount(),
                        owned,
                        requiredItem.amount() - owned
                ));
            }
        }
        return List.copyOf(missingResources);
    }

    private EnhancePreview nextPreviewFor(String userId, String weaponId) {
        WeaponDefinition weapon = weaponCatalogService.findById(weaponId);
        String nextWeaponId = weapon.nextWeaponId();
        if (nextWeaponId == null || !weaponCatalogService.exists(nextWeaponId)) {
            return null;
        }
        if (weaponCatalogService.findById(nextWeaponId).grade() != weapon.grade()) {
            return null;
        }
        return preview(userId, weaponId, false, null);
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
            double successRate,
            double adjustedSuccessRate,
            double failRate,
            int goldCost,
            List<ResourceRequirement> requiredItems,
            boolean canBreak,
            boolean useProtectionAvailable,
            boolean canAfford,
            List<MissingResource> missingResources,
            String failureResult,
            boolean protectedAttempt,
            boolean enhancementAvailable,
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
            String currentWeaponId,
            String equippedWeaponId,
            Map<String, Integer> remainingMaterials,
            EnhancePreview nextPreview,
            boolean canRetry,
            List<MissingResource> missingResources,
            String pityKey,
            int pityStackBefore,
            int pityStackAfter,
            double pityBonus
    ) {
    }

    public record ResourceRequirement(
            String resourceKind,
            String resourceId,
            int amount
    ) {
    }

    public record MissingResource(
            String resourceKind,
            String resourceId,
            int requiredAmount,
            int ownedAmount,
            int missingAmount
    ) {
    }
}
