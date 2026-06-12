package com.swordforge.application.item;

import com.swordforge.domain.item.SpecialItemDefinition;
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
public class SpecialItemCatalogService {

    private static final TypeReference<List<SpecialItemDefinition>> ITEM_LIST_TYPE = new TypeReference<>() {};

    private final ObjectMapper objectMapper;
    private Map<String, SpecialItemDefinition> itemById = Map.of();

    public SpecialItemCatalogService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    void loadItems() {
        try (InputStream inputStream = new ClassPathResource("data/special_items.json").getInputStream()) {
            List<SpecialItemDefinition> definitions = objectMapper.readValue(inputStream, ITEM_LIST_TYPE);
            LinkedHashMap<String, SpecialItemDefinition> indexed = new LinkedHashMap<>();
            for (SpecialItemDefinition definition : definitions) {
                if (indexed.containsKey(definition.id())) {
                    throw new IllegalStateException("duplicate special item id: " + definition.id());
                }
                indexed.put(definition.id(), definition);
            }
            itemById = Map.copyOf(indexed);
        } catch (IOException ex) {
            throw new IllegalStateException("failed to load special items", ex);
        }
    }

    public SpecialItemDefinition findById(String itemId) {
        SpecialItemDefinition definition = itemById.get(itemId);
        if (definition == null) {
            throw new IllegalArgumentException("unknown special item id: " + itemId);
        }
        return definition;
    }

    public List<SpecialItemDefinition> findAll() {
        return List.copyOf(itemById.values());
    }
}

