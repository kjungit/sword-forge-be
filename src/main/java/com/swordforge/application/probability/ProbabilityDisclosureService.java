package com.swordforge.application.probability;

import com.swordforge.application.enhance.EnhanceCostService;
import com.swordforge.application.enhance.EnhanceTableService;
import com.swordforge.application.evolution.EvolutionRequirementService;
import com.swordforge.application.item.SpecialItemCatalogService;
import com.swordforge.application.save.PlayerSaveService;
import com.swordforge.application.weapon.WeaponCatalogService;
import com.swordforge.domain.enhance.EnhanceTableDefinition;
import com.swordforge.domain.item.SpecialItemDefinition;
import com.swordforge.domain.save.PlayerSaveData;
import com.swordforge.domain.weapon.Grade;
import com.swordforge.domain.weapon.WeaponDefinition;
import org.springframework.stereotype.Service;

@Service
public class ProbabilityDisclosureService {

    private static final double PITY_STACK_RATE_BONUS = 0.02;
    private static final double MAX_PITY_RATE_BONUS = 0.20;
    private static final double PROTECTION_RATE_BONUS = 0.05;

    private final WeaponCatalogService weaponCatalogService;
    private final EnhanceTableService enhanceTableService;
    private final EnhanceCostService enhanceCostService;
    private final EvolutionRequirementService evolutionRequirementService;
    private final PlayerSaveService playerSaveService;
    private final SpecialItemCatalogService specialItemCatalogService;

    public ProbabilityDisclosureService(
            WeaponCatalogService weaponCatalogService,
            EnhanceTableService enhanceTableService,
            EnhanceCostService enhanceCostService,
            EvolutionRequirementService evolutionRequirementService,
            PlayerSaveService playerSaveService,
            SpecialItemCatalogService specialItemCatalogService
    ) {
        this.weaponCatalogService = weaponCatalogService;
        this.enhanceTableService = enhanceTableService;
        this.enhanceCostService = enhanceCostService;
        this.evolutionRequirementService = evolutionRequirementService;
        this.playerSaveService = playerSaveService;
        this.specialItemCatalogService = specialItemCatalogService;
    }

    public EnhanceProbability discloseEnhance(
            String userId,
            String weaponId,
            boolean useProtection,
            String rateBoostItemId
    ) {
        WeaponDefinition weapon = weaponCatalogService.findById(weaponId);
        EnhanceTableDefinition table = enhanceTableService.findByWeaponId(weaponId);
        String catalogNextWeaponId = weapon.nextWeaponId() != null && weaponCatalogService.exists(weapon.nextWeaponId())
                ? weapon.nextWeaponId()
                : null;
        boolean enhancementAvailable = catalogNextWeaponId != null
                && weaponCatalogService.findById(catalogNextWeaponId).grade() == weapon.grade();
        String nextWeaponId = enhancementAvailable ? catalogNextWeaponId : null;
        boolean requiresEvolution = evolutionRequirementService.exists(weaponId);
        String pityKey = weapon.grade().code();
        int pityStack = 0;
        if (userId != null && !userId.isBlank()) {
            PlayerSaveData save = playerSaveService.getOrCreate(userId);
            pityStack = save.pityStacks().getOrDefault(pityKey, 0);
        }
        double pityBonus = Math.min(MAX_PITY_RATE_BONUS, pityStack * PITY_STACK_RATE_BONUS);
        boolean itemBoostAllowed = enhancementAvailable && weapon.grade() != Grade.NORMAL;
        boolean effectiveProtection = itemBoostAllowed && useProtection;
        String effectiveRateBoostItemId = itemBoostAllowed ? rateBoostItemId : null;
        double protectionBonus = effectiveProtection ? PROTECTION_RATE_BONUS : 0.0;
        double rateBoostBonus = rateBoostBonus(effectiveRateBoostItemId);
        double adjustedSuccessRate = enhancementAvailable
                ? Math.min(1.0, table.successRate() + pityBonus + protectionBonus + rateBoostBonus)
                : 0.0;
        int goldCost = enhancementAvailable ? enhanceCostService.findCost(weaponId) : 0;

        return new EnhanceProbability(
                userId,
                weaponId,
                nextWeaponId,
                enhancementAvailable,
                requiresEvolution,
                table.successRate(),
                table.failRate(),
                adjustedSuccessRate,
                goldCost,
                effectiveProtection,
                protectionBonus,
                effectiveRateBoostItemId,
                rateBoostBonus,
                pityKey,
                pityStack,
                pityBonus
        );
    }

    private double rateBoostBonus(String rateBoostItemId) {
        if (rateBoostItemId == null || rateBoostItemId.isBlank()) {
            return 0.0;
        }
        SpecialItemDefinition rateBoostItem = specialItemCatalogService.findById(rateBoostItemId);
        return switch (rateBoostItem.effect()) {
            case "enhance_rate_plus_5" -> 0.05;
            case "enhance_rate_plus_10" -> 0.10;
            default -> throw new IllegalArgumentException("item cannot be used for enhancement boost: " + rateBoostItem.id());
        };
    }

    public record EnhanceProbability(
            String userId,
            String weaponId,
            String nextWeaponId,
            boolean enhancementAvailable,
            boolean requiresEvolution,
            double baseSuccessRate,
            double failRate,
            double adjustedSuccessRate,
            int goldCost,
            boolean protectionSelected,
            double protectionBonus,
            String rateBoostItemId,
            double rateBoostBonus,
            String pityKey,
            int pityStack,
            double pityBonus
    ) {
    }
}
