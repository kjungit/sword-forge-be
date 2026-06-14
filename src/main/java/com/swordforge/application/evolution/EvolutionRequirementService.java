package com.swordforge.application.evolution;

import com.swordforge.domain.evolution.EvolutionRequirementDefinition;
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
public class EvolutionRequirementService {

    private static final TypeReference<List<EvolutionRequirementDefinition>> REQUIREMENT_LIST_TYPE = new TypeReference<>() {};

    private final ObjectMapper objectMapper;
    private Map<String, EvolutionRequirementDefinition> requirementByWeaponId = Map.of();

    public EvolutionRequirementService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    void loadRequirements() {
        try (InputStream inputStream = new ClassPathResource("data/evolution_requirements.json").getInputStream()) {
            List<EvolutionRequirementDefinition> definitions = objectMapper.readValue(inputStream, REQUIREMENT_LIST_TYPE);
            LinkedHashMap<String, EvolutionRequirementDefinition> indexed = new LinkedHashMap<>();
            for (EvolutionRequirementDefinition definition : definitions) {
                if (indexed.containsKey(definition.fromWeaponId())) {
                    throw new IllegalStateException("duplicate evolution requirement weapon id: " + definition.fromWeaponId());
                }
                indexed.put(definition.fromWeaponId(), definition);
            }
            requirementByWeaponId = Map.copyOf(indexed);
        } catch (IOException ex) {
            throw new IllegalStateException("failed to load evolution requirements", ex);
        }
    }

    public EvolutionRequirementDefinition findByWeaponId(String weaponId) {
        EvolutionRequirementDefinition requirement = requirementByWeaponId.get(weaponId);
        if (requirement == null) {
            throw new IllegalArgumentException("unknown evolution requirement weapon id: " + weaponId);
        }
        return requirement;
    }

    public boolean exists(String weaponId) {
        return requirementByWeaponId.containsKey(weaponId);
    }
}
