package com.codex.swordgrowth.application.shop;

import com.codex.swordgrowth.domain.shop.WeaponPurchaseCostDefinition;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class WeaponPurchaseCostService {

    private static final TypeReference<List<WeaponPurchaseCostDefinition>> COST_LIST_TYPE = new TypeReference<>() {};

    private final ObjectMapper objectMapper;
    private Map<String, Map<String, Integer>> costByWeaponId = Map.of();

    public WeaponPurchaseCostService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    void loadPurchaseCosts() {
        try (InputStream inputStream = new ClassPathResource("data/weapon_purchase_costs.json").getInputStream()) {
            List<WeaponPurchaseCostDefinition> definitions = objectMapper.readValue(inputStream, COST_LIST_TYPE);
            LinkedHashMap<String, Map<String, Integer>> indexed = new LinkedHashMap<>();
            for (WeaponPurchaseCostDefinition definition : definitions) {
                if (indexed.containsKey(definition.weaponId())) {
                    throw new IllegalStateException("duplicate purchase cost weapon id: " + definition.weaponId());
                }
                indexed.put(definition.weaponId(), Map.copyOf(definition.cost()));
            }
            costByWeaponId = Map.copyOf(indexed);
        } catch (IOException ex) {
            throw new IllegalStateException("failed to load weapon purchase costs", ex);
        }
    }

    public Map<String, Integer> findCost(String weaponId) {
        Map<String, Integer> cost = costByWeaponId.get(weaponId);
        if (cost == null) {
            throw new IllegalArgumentException("unknown purchase cost weapon id: " + weaponId);
        }
        return cost;
    }
}

