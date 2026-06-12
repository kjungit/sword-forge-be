package com.swordforge.application.enhance;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swordforge.domain.enhance.EnhanceCostDefinition;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class EnhanceCostService {

    private static final TypeReference<List<EnhanceCostDefinition>> COST_LIST_TYPE = new TypeReference<>() {};

    private final ObjectMapper objectMapper;
    private Map<String, Integer> costByWeaponId = Map.of();

    public EnhanceCostService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
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
}
