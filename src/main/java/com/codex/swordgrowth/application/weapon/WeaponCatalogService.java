package com.codex.swordgrowth.application.weapon;

import com.codex.swordgrowth.domain.weapon.WeaponDefinition;
import com.codex.swordgrowth.domain.weapon.Grade;
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
public class WeaponCatalogService {

    private static final TypeReference<List<WeaponDefinition>> WEAPON_LIST_TYPE = new TypeReference<>() {};
    private final ObjectMapper objectMapper;
    private List<WeaponDefinition> weapons = List.of();
    private Map<String, WeaponDefinition> weaponById = Map.of();

    public WeaponCatalogService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    void loadWeaponCatalog() {
        try (InputStream inputStream = new ClassPathResource("data/weapons.json").getInputStream()) {
            List<WeaponDefinition> weapons = objectMapper.readValue(inputStream, WEAPON_LIST_TYPE);
            LinkedHashMap<String, WeaponDefinition> indexed = new LinkedHashMap<>();
            for (WeaponDefinition weapon : weapons) {
                if (indexed.containsKey(weapon.id())) {
                    throw new IllegalStateException("duplicate weapon id: " + weapon.id());
                }
                indexed.put(weapon.id(), weapon);
            }
            this.weapons = List.copyOf(indexed.values());
            weaponById = Map.copyOf(indexed);
        } catch (IOException ex) {
            throw new IllegalStateException("failed to load weapon catalog", ex);
        }
    }

    public List<WeaponDefinition> findAll() {
        return weapons;
    }

    public WeaponDefinition findById(String weaponId) {
        WeaponDefinition weapon = weaponById.get(weaponId);
        if (weapon == null) {
            throw new IllegalArgumentException("unknown weapon id: " + weaponId);
        }
        return weapon;
    }

    public boolean exists(String weaponId) {
        return weaponById.containsKey(weaponId);
    }

    public WeaponDefinition findFirstByGrade(Grade grade) {
        return weapons.stream()
                .filter(weapon -> weapon.grade() == grade)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("unknown grade: " + grade));
    }
}
