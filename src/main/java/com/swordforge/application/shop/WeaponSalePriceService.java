package com.swordforge.application.shop;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swordforge.domain.shop.WeaponSalePriceDefinition;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class WeaponSalePriceService {

    private static final TypeReference<List<WeaponSalePriceDefinition>> SALE_PRICE_LIST_TYPE = new TypeReference<>() {};

    private final ObjectMapper objectMapper;
    private Map<String, Integer> priceByWeaponId = Map.of();

    public WeaponSalePriceService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    void loadSalePrices() {
        try (InputStream inputStream = new ClassPathResource("data/weapon_sale_prices.json").getInputStream()) {
            List<WeaponSalePriceDefinition> definitions = objectMapper.readValue(inputStream, SALE_PRICE_LIST_TYPE);
            LinkedHashMap<String, Integer> indexed = new LinkedHashMap<>();
            for (WeaponSalePriceDefinition definition : definitions) {
                if (indexed.containsKey(definition.weaponId())) {
                    throw new IllegalStateException("duplicate sale price weapon id: " + definition.weaponId());
                }
                indexed.put(definition.weaponId(), definition.goldPrice());
            }
            priceByWeaponId = Map.copyOf(indexed);
        } catch (IOException ex) {
            throw new IllegalStateException("failed to load weapon sale prices", ex);
        }
    }

    public int findPrice(String weaponId) {
        Integer price = priceByWeaponId.get(weaponId);
        if (price == null) {
            throw new IllegalArgumentException("unknown sale price weapon id: " + weaponId);
        }
        return price;
    }
}
