package com.codex.swordgrowth.application.enhance;

import com.codex.swordgrowth.domain.enhance.EnhanceTableDefinition;
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
public class EnhanceTableService {

    private static final TypeReference<List<EnhanceTableDefinition>> TABLE_LIST_TYPE = new TypeReference<>() {};

    private final ObjectMapper objectMapper;
    private Map<String, EnhanceTableDefinition> tableByWeaponId = Map.of();

    public EnhanceTableService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    void loadEnhanceTable() {
        try (InputStream inputStream = new ClassPathResource("data/enhance_table.json").getInputStream()) {
            List<EnhanceTableDefinition> definitions = objectMapper.readValue(inputStream, TABLE_LIST_TYPE);
            LinkedHashMap<String, EnhanceTableDefinition> indexed = new LinkedHashMap<>();
            for (EnhanceTableDefinition definition : definitions) {
                if (indexed.containsKey(definition.weaponId())) {
                    throw new IllegalStateException("duplicate enhance table weapon id: " + definition.weaponId());
                }
                indexed.put(definition.weaponId(), definition);
            }
            tableByWeaponId = Map.copyOf(indexed);
        } catch (IOException ex) {
            throw new IllegalStateException("failed to load enhance table", ex);
        }
    }

    public EnhanceTableDefinition findByWeaponId(String weaponId) {
        EnhanceTableDefinition definition = tableByWeaponId.get(weaponId);
        if (definition == null) {
            throw new IllegalArgumentException("unknown enhance table weapon id: " + weaponId);
        }
        return definition;
    }
}

