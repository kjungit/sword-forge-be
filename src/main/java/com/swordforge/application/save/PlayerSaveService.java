package com.swordforge.application.save;

import com.swordforge.domain.save.PlayerSaveEntity;
import com.swordforge.domain.save.PlayerSaveRepository;
import com.swordforge.domain.save.PlayerSaveData;
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

    public static final String GOLD_MATERIAL_ID = "gold";
    public static final int DEFAULT_STARTING_GOLD = 100_000;

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
                saveData.lockedWeaponIds() == null ? List.of() : List.copyOf(saveData.lockedWeaponIds()),
                ownedWeaponIds,
                List.copyOf(saveData.unlockedWeaponShop()),
                List.copyOf(saveData.discoveredWeaponIds()),
                saveData.highestReachedWeaponId(),
                saveData.pityStacks() == null ? Map.of() : Map.copyOf(saveData.pityStacks()),
                Instant.now()
        );
        PlayerSaveEntity entity = playerSaveRepository.findById(normalized.userId())
                .orElseGet(() -> toEntity(normalized));
        applyToEntity(entity, normalized);
        playerSaveRepository.save(entity);
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

    public int goldOf(Map<String, Integer> materials) {
        return materials.getOrDefault(GOLD_MATERIAL_ID, 0);
    }

    public Map<String, Integer> addGold(Map<String, Integer> materials, int amount) {
        if (amount <= 0) {
            return Map.copyOf(materials);
        }
        Map<String, Integer> updated = new LinkedHashMap<>(materials);
        updated.merge(GOLD_MATERIAL_ID, amount, Integer::sum);
        return Map.copyOf(updated);
    }

    public Map<String, Integer> spendGold(Map<String, Integer> materials, int amount) {
        if (amount <= 0) {
            return Map.copyOf(materials);
        }
        int current = materials.getOrDefault(GOLD_MATERIAL_ID, 0);
        if (current < amount) {
            throw new IllegalArgumentException("not enough gold");
        }
        Map<String, Integer> updated = new LinkedHashMap<>(materials);
        int next = current - amount;
        if (next <= 0) {
            updated.remove(GOLD_MATERIAL_ID);
        } else {
            updated.put(GOLD_MATERIAL_ID, next);
        }
        return Map.copyOf(updated);
    }

    public List<String> appendUnique(List<String> source, String value) {
        if (source.contains(value)) {
            return source;
        }
        List<String> updated = new java.util.ArrayList<>(source);
        updated.add(value);
        return List.copyOf(updated);
    }

    public List<String> removeValue(List<String> source, String value) {
        if (!source.contains(value)) {
            return source;
        }
        List<String> updated = new java.util.ArrayList<>(source);
        updated.removeIf(value::equals);
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

    public Map<String, Integer> incrementPityStack(Map<String, Integer> pityStacks, String key) {
        Map<String, Integer> updated = new LinkedHashMap<>(pityStacks);
        updated.merge(key, 1, Integer::sum);
        return Map.copyOf(updated);
    }

    public Map<String, Integer> resetPityStack(Map<String, Integer> pityStacks, String key) {
        if (!pityStacks.containsKey(key)) {
            return Map.copyOf(pityStacks);
        }
        Map<String, Integer> updated = new LinkedHashMap<>(pityStacks);
        updated.remove(key);
        return Map.copyOf(updated);
    }

    private PlayerSaveData createDefaultSave(String userId) {
        return new PlayerSaveData(
                userId,
                "normal_01",
                Map.of(GOLD_MATERIAL_ID, DEFAULT_STARTING_GOLD),
                Map.of(),
                Map.of("normal_01", 1),
                List.of(),
                List.of("normal_01"),
                List.of("normal_01"),
                List.of("normal_01"),
                "normal_01",
                Map.of(),
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
                readList(entity.getLockedWeaponIdsJson()),
                readList(entity.getOwnedWeaponIdsJson()),
                readList(entity.getUnlockedWeaponShopJson()),
                readList(entity.getDiscoveredWeaponIdsJson()),
                entity.getHighestReachedWeaponId(),
                readMap(entity.getPityStacksJson()),
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
                writeJson(data.lockedWeaponIds()),
                writeJson(data.ownedWeaponIds()),
                writeJson(data.unlockedWeaponShop()),
                writeJson(data.discoveredWeaponIds()),
                data.highestReachedWeaponId(),
                writeJson(data.pityStacks()),
                data.updatedAt()
        );
    }

    private void applyToEntity(PlayerSaveEntity entity, PlayerSaveData data) {
        entity.setCurrentWeaponId(data.currentWeaponId());
        entity.setMaterialsJson(writeJson(data.materials()));
        entity.setSpecialItemsJson(writeJson(data.specialItems()));
        entity.setWeaponInventoryJson(writeJson(data.weaponInventory()));
        entity.setLockedWeaponIdsJson(writeJson(data.lockedWeaponIds()));
        entity.setOwnedWeaponIdsJson(writeJson(data.ownedWeaponIds()));
        entity.setUnlockedWeaponShopJson(writeJson(data.unlockedWeaponShop()));
        entity.setDiscoveredWeaponIdsJson(writeJson(data.discoveredWeaponIds()));
        entity.setHighestReachedWeaponId(data.highestReachedWeaponId());
        entity.setPityStacksJson(writeJson(data.pityStacks()));
        entity.setUpdatedAt(data.updatedAt());
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
