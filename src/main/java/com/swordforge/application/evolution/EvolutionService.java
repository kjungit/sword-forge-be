package com.swordforge.application.evolution;

import com.swordforge.application.economy.EconomyLogService;
import com.swordforge.application.save.PlayerSaveService;
import com.swordforge.application.weapon.WeaponCatalogService;
import com.swordforge.domain.evolution.EvolutionRequirementDefinition;
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

@Service
public class EvolutionService {

    private final WeaponCatalogService weaponCatalogService;
    private final EvolutionRequirementService evolutionRequirementService;
    private final PlayerSaveService playerSaveService;
    private final EconomyLogService economyLogService;

    public EvolutionService(
            WeaponCatalogService weaponCatalogService,
            EvolutionRequirementService evolutionRequirementService,
            PlayerSaveService playerSaveService,
            EconomyLogService economyLogService
    ) {
        this.weaponCatalogService = weaponCatalogService;
        this.evolutionRequirementService = evolutionRequirementService;
        this.playerSaveService = playerSaveService;
        this.economyLogService = economyLogService;
    }

    public EvolutionPreview preview(String fromWeaponId) {
        EvolutionRequirementDefinition requirement = evolutionRequirementService.findByWeaponId(fromWeaponId);
        WeaponDefinition target = weaponCatalogService.findFirstByGrade(Grade.from(requirement.toGrade()));
        return new EvolutionPreview(fromWeaponId, target.id(), requirement.requiredWeapons(), requirement.requiredMaterials());
    }

    @Transactional
    public EvolutionResult evolve(String userId) {
        PlayerSaveData currentSave = playerSaveService.getOrCreate(userId);
        EvolutionRequirementDefinition requirement = evolutionRequirementService.findByWeaponId(currentSave.currentWeaponId());
        WeaponDefinition source = weaponCatalogService.findById(currentSave.currentWeaponId());
        WeaponDefinition target = weaponCatalogService.findFirstByGrade(Grade.from(requirement.toGrade()));

        Map<String, Integer> updatedMaterials = deductMaterials(currentSave.materials(), requirement.requiredMaterials());
        Map<String, Integer> updatedInventory = new LinkedHashMap<>(currentSave.weaponInventory());
        consumeWeaponRequirement(updatedInventory, requirement.requiredWeapons(), currentSave.currentWeaponId());
        updatedInventory = playerSaveService.addWeaponCount(updatedInventory, target.id(), 1);
        updatedInventory = playerSaveService.ensureStarterWeapon(updatedInventory);

        List<String> ownedWeaponIds = playerSaveService.deriveOwnedWeaponIds(updatedInventory);
        List<String> unlockedWeaponShop = playerSaveService.appendUnique(currentSave.unlockedWeaponShop(), target.id());
        List<String> discoveredWeaponIds = playerSaveService.appendUnique(currentSave.discoveredWeaponIds(), target.id());
        Map<String, Integer> pityStacks = playerSaveService.resetPityStack(currentSave.pityStacks(), source.grade().code());

        PlayerSaveData updated = playerSaveService.upsert(new PlayerSaveData(
                currentSave.userId(),
                target.id(),
                updatedMaterials,
                currentSave.specialItems(),
                updatedInventory,
                currentSave.lockedWeaponIds(),
                ownedWeaponIds,
                unlockedWeaponShop,
                discoveredWeaponIds,
                target.id(),
                pityStacks,
                Instant.now()
        ));
        economyLogService.logMaterialDeltas(
                userId,
                "evolution_material_cost",
                source.id(),
                negate(requirement.requiredMaterials()),
                updated.materials(),
                Map.of("fromWeaponId", source.id(), "toWeaponId", target.id())
        );

        return new EvolutionResult(
                userId,
                source.id(),
                target.id(),
                requirement.requiredWeapons(),
                requirement.requiredMaterials(),
                updated.currentWeaponId()
        );
    }

    private Map<String, Integer> deductMaterials(Map<String, Integer> currentMaterials, Map<String, Integer> cost) {
        Map<String, Integer> updated = new LinkedHashMap<>(currentMaterials);
        for (Map.Entry<String, Integer> entry : cost.entrySet()) {
            int current = updated.getOrDefault(entry.getKey(), 0);
            if (current < entry.getValue()) {
                throw new IllegalArgumentException("not enough materials: " + entry.getKey());
            }
            int next = current - entry.getValue();
            if (next <= 0) {
                updated.remove(entry.getKey());
            } else {
                updated.put(entry.getKey(), next);
            }
        }
        return Map.copyOf(updated);
    }

    private Map<String, Integer> negate(Map<String, Integer> values) {
        Map<String, Integer> negated = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : values.entrySet()) {
            negated.put(entry.getKey(), -entry.getValue());
        }
        return Map.copyOf(negated);
    }

    private void consumeWeaponRequirement(Map<String, Integer> inventory, Map<String, Integer> requiredWeapons, String currentWeaponId) {
        for (Map.Entry<String, Integer> requirement : requiredWeapons.entrySet()) {
            String requirementId = requirement.getKey();
            int amount = requirement.getValue();
            if (requirementId.endsWith("_or_higher")) {
                consumeRangeRequirement(inventory, requirementId, amount);
                continue;
            }
            if (requirementId.equals(currentWeaponId)) {
                amount -= 1;
                if (amount < 0) {
                    throw new IllegalArgumentException("invalid evolution requirement count for current weapon");
                }
            }
            if (amount > 0) {
                int current = inventory.getOrDefault(requirementId, 0);
                if (current < amount) {
                    throw new IllegalArgumentException("not enough weapons: " + requirementId);
                }
                removeExact(inventory, requirementId, amount);
            }
        }
        if (inventory.getOrDefault(currentWeaponId, 0) > 0) {
            removeExact(inventory, currentWeaponId, 1);
        }
    }

    private void consumeRangeRequirement(Map<String, Integer> inventory, String requirementId, int amount) {
        String baseId = requirementId.substring(0, requirementId.length() - "_or_higher".length());
        WeaponDefinition baseWeapon = weaponCatalogService.findById(baseId);
        List<WeaponDefinition> candidates = weaponCatalogService.findAll().stream()
                .filter(weapon -> weapon.grade() == baseWeapon.grade() && weapon.stage() >= baseWeapon.stage())
                .sorted((a, b) -> Integer.compare(b.stage(), a.stage()))
                .toList();

        int remaining = amount;
        for (WeaponDefinition candidate : candidates) {
            int owned = inventory.getOrDefault(candidate.id(), 0);
            if (owned <= 0) {
                continue;
            }
            int used = Math.min(owned, remaining);
            removeExact(inventory, candidate.id(), used);
            remaining -= used;
            if (remaining <= 0) {
                return;
            }
        }
        throw new IllegalArgumentException("not enough range weapons for: " + requirementId);
    }

    private void removeExact(Map<String, Integer> inventory, String weaponId, int amount) {
        int current = inventory.getOrDefault(weaponId, 0);
        if (current < amount) {
            throw new IllegalArgumentException("not enough weapon copies: " + weaponId);
        }
        int next = current - amount;
        if (next <= 0) {
            inventory.remove(weaponId);
        } else {
            inventory.put(weaponId, next);
        }
    }

    public record EvolutionPreview(
            String fromWeaponId,
            String toWeaponId,
            Map<String, Integer> requiredWeapons,
            Map<String, Integer> requiredMaterials
    ) {
    }

    public record EvolutionResult(
            String userId,
            String fromWeaponId,
            String toWeaponId,
            Map<String, Integer> consumedWeapons,
            Map<String, Integer> consumedMaterials,
            String equippedWeaponId
    ) {
    }
}
