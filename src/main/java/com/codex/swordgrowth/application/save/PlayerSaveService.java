package com.codex.swordgrowth.application.save;

import com.codex.swordgrowth.domain.save.PlayerSaveEntity;
import com.codex.swordgrowth.domain.save.PlayerSaveRepository;
import com.codex.swordgrowth.domain.save.PlayerSaveData;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Service
public class PlayerSaveService {

    private static final TypeReference<Map<String, Integer>> MAP_INTEGER_TYPE = new TypeReference<>() {};
    private static final TypeReference<List<String>> LIST_STRING_TYPE = new TypeReference<>() {};

    private final PlayerSaveRepository playerSaveRepository;
    private final ObjectMapper objectMapper;

    public PlayerSaveService(PlayerSaveRepository playerSaveRepository, ObjectMapper objectMapper) {
        this.playerSaveRepository = playerSaveRepository;
        this.objectMapper = objectMapper;
    }

    public PlayerSaveData getOrCreate(String userId) {
        return playerSaveRepository.findById(userId)
                .map(this::toDomain)
                .orElseGet(() -> {
                    PlayerSaveData defaultSave = createDefaultSave(userId);
                    playerSaveRepository.save(toEntity(defaultSave));
                    return defaultSave;
                });
    }

    @Transactional
    public PlayerSaveData upsert(PlayerSaveData saveData) {
        List<String> ownedWeaponIds = deriveOwnedWeaponIds(saveData.weaponInventory());
        PlayerSaveData normalized = new PlayerSaveData(
                saveData.userId(),
                saveData.currentWeaponId(),
                Map.copyOf(saveData.materials()),
                Map.copyOf(saveData.specialItems()),
                Map.copyOf(saveData.weaponInventory()),
                ownedWeaponIds,
                List.copyOf(saveData.unlockedWeaponShop()),
                List.copyOf(saveData.discoveredWeaponIds()),
                saveData.highestReachedWeaponId(),
                Instant.now()
        );
        playerSaveRepository.save(toEntity(normalized));
        return normalized;
    }

    @Transactional
    public PlayerSaveData mutate(String userId, Function<PlayerSaveData, PlayerSaveData> mutator) {
        PlayerSaveData current = getOrCreate(userId);
        PlayerSaveData mutated = mutator.apply(current);
        if (!userId.equals(mutated.userId())) {
            throw new IllegalArgumentException("save user id mismatch");
        }
        return upsert(mutated);
    }

    public Map<String, Integer> mergeMaterials(Map<String, Integer> currentMaterials, Map<String, Integer> addedMaterials) {
        Map<String, Integer> merged = new LinkedHashMap<>(currentMaterials);
        for (Map.Entry<String, Integer> entry : addedMaterials.entrySet()) {
            merged.merge(entry.getKey(), entry.getValue(), Integer::sum);
        }
        return Map.copyOf(merged);
    }

    public List<String> appendUnique(List<String> source, String value) {
        if (source.contains(value)) {
            return source;
        }
        List<String> updated = new java.util.ArrayList<>(source);
        updated.add(value);
        return List.copyOf(updated);
    }

    public Map<String, Integer> addWeaponCount(Map<String, Integer> inventory, String weaponId, int amount) {
        if (amount <= 0) {
            return Map.copyOf(inventory);
        }
        Map<String, Integer> updated = new LinkedHashMap<>(inventory);
        updated.merge(weaponId, amount, Integer::sum);
        return Map.copyOf(updated);
    }

    public Map<String, Integer> removeWeaponCount(Map<String, Integer> inventory, String weaponId, int amount) {
        if (amount <= 0) {
            return Map.copyOf(inventory);
        }
        Map<String, Integer> updated = new LinkedHashMap<>(inventory);
        int current = updated.getOrDefault(weaponId, 0);
        if (current < amount) {
            throw new IllegalArgumentException("not enough weapon copies: " + weaponId);
        }
        int next = current - amount;
        if (next <= 0) {
            updated.remove(weaponId);
        } else {
            updated.put(weaponId, next);
        }
        return Map.copyOf(updated);
    }

    public List<String> deriveOwnedWeaponIds(Map<String, Integer> inventory) {
        return inventory.entrySet().stream()
                .filter(entry -> entry.getValue() != null && entry.getValue() > 0)
                .map(Map.Entry::getKey)
                .sorted()
                .toList();
    }

    public Map<String, Integer> ensureStarterWeapon(Map<String, Integer> inventory) {
        if (inventory.getOrDefault("normal_01", 0) > 0) {
            return Map.copyOf(inventory);
        }
        return addWeaponCount(inventory, "normal_01", 1);
    }

    private PlayerSaveData createDefaultSave(String userId) {
        return new PlayerSaveData(
                userId,
                "normal_01",
                Map.of(),
                Map.of(),
                Map.of("normal_01", 1),
                List.of("normal_01"),
                List.of("normal_01"),
                List.of("normal_01"),
                "normal_01",
                Instant.now()
        );
    }

    private PlayerSaveData toDomain(PlayerSaveEntity entity) {
        return new PlayerSaveData(
                entity.getUserId(),
                entity.getCurrentWeaponId(),
                readMap(entity.getMaterialsJson()),
                readMap(entity.getSpecialItemsJson()),
                readMap(entity.getWeaponInventoryJson()),
                readList(entity.getOwnedWeaponIdsJson()),
                readList(entity.getUnlockedWeaponShopJson()),
                readList(entity.getDiscoveredWeaponIdsJson()),
                entity.getHighestReachedWeaponId(),
                entity.getUpdatedAt()
        );
    }

    private PlayerSaveEntity toEntity(PlayerSaveData data) {
        return new PlayerSaveEntity(
                data.userId(),
                data.currentWeaponId(),
                writeJson(data.materials()),
                writeJson(data.specialItems()),
                writeJson(data.weaponInventory()),
                writeJson(data.ownedWeaponIds()),
                writeJson(data.unlockedWeaponShop()),
                writeJson(data.discoveredWeaponIds()),
                data.highestReachedWeaponId(),
                data.updatedAt()
        );
    }

    private Map<String, Integer> readMap(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, MAP_INTEGER_TYPE);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("failed to parse save data map", ex);
        }
    }

    private List<String> readList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, LIST_STRING_TYPE);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("failed to parse save data list", ex);
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("failed to serialize save data", ex);
        }
    }
}
