package com.swordforge.application.enhance;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swordforge.domain.enhance.EnhanceCostDefinition;
import com.swordforge.application.weapon.WeaponCatalogService;
import com.swordforge.domain.weapon.WeaponDefinition;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class EnhanceCostService {

    private static final TypeReference<List<EnhanceCostDefinition>> COST_LIST_TYPE = new TypeReference<>() {};

    private final ObjectMapper objectMapper;
    private final WeaponCatalogService weaponCatalogService;
    private Map<String, Integer> costByWeaponId = Map.of();
    private Map<String, Integer> investedGoldByWeaponId = Map.of();

    public EnhanceCostService(ObjectMapper objectMapper, WeaponCatalogService weaponCatalogService) {
        this.objectMapper = objectMapper;
        this.weaponCatalogService = weaponCatalogService;
    }

    @PostConstruct
    void loadEnhanceCosts() {
        try (InputStream inputStream = new ClassPathResource("data/enhance_costs.json").getInputStream()) {
            List<EnhanceCostDefinition> definitions = objectMapper.readValue(inputStream, COST_LIST_TYPE);
            LinkedHashMap<String, Integer> indexed = new LinkedHashMap<>();
            for (EnhanceCostDefinition definition : definitions) {
                if (indexed.containsKey(definition.weaponId())) {
                    throw new IllegalStateException("duplicate enhance cost weapon id: " + definition.weaponId());
                }
                indexed.put(definition.weaponId(), definition.goldCost());
            }
            costByWeaponId = Map.copyOf(indexed);
            investedGoldByWeaponId = calculateInvestedGold(indexed);
        } catch (IOException ex) {
            throw new IllegalStateException("failed to load enhance costs", ex);
        }
    }

    public int findCost(String weaponId) {
        Integer cost = costByWeaponId.get(weaponId);
        if (cost == null) {
            throw new IllegalArgumentException("unknown enhance cost weapon id: " + weaponId);
        }
        return cost;
    }

    public int investedGoldFor(String weaponId) {
        Integer investedGold = investedGoldByWeaponId.get(weaponId);
        if (investedGold == null) {
            throw new IllegalArgumentException("unknown invested gold weapon id: " + weaponId);
        }
        return investedGold;
    }

    private Map<String, Integer> calculateInvestedGold(Map<String, Integer> indexedCosts) {
        List<WeaponDefinition> orderedWeapons = weaponCatalogService.findAll().stream()
                .sorted(Comparator
                        .comparing((WeaponDefinition weapon) -> weapon.grade().ordinal())
                        .thenComparingInt(WeaponDefinition::stage))
                .toList();

        LinkedHashMap<String, Integer> invested = new LinkedHashMap<>();
        int cumulativeGold = 0;
        for (WeaponDefinition weapon : orderedWeapons) {
            invested.put(weapon.id(), cumulativeGold);

            String nextWeaponId = weapon.nextWeaponId();
            if (nextWeaponId == null || !weaponCatalogService.exists(nextWeaponId)) {
                continue;
            }
            WeaponDefinition nextWeapon = weaponCatalogService.findById(nextWeaponId);
            if (nextWeapon.grade() == weapon.grade()) {
                cumulativeGold += indexedCosts.getOrDefault(weapon.id(), 0);
            }
        }
        return Map.copyOf(invested);
    }
}
