package com.swordforge.application.shop;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swordforge.application.enhance.EnhanceCostService;
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

    public static final double MIN_PROFIT_MULTIPLIER = 2.0;

    private static final TypeReference<List<WeaponSalePriceDefinition>> SALE_PRICE_LIST_TYPE = new TypeReference<>() {};

    private final ObjectMapper objectMapper;
    private final EnhanceCostService enhanceCostService;
    private Map<String, Integer> priceByWeaponId = Map.of();

    public WeaponSalePriceService(ObjectMapper objectMapper, EnhanceCostService enhanceCostService) {
        this.objectMapper = objectMapper;
        this.enhanceCostService = enhanceCostService;
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
        int investedGold = investedGoldFor(weaponId);
        int minimumLoopPrice = (int) Math.ceil(investedGold * MIN_PROFIT_MULTIPLIER);
        return Math.max(price, minimumLoopPrice);
    }

    public int configuredPriceFor(String weaponId) {
        Integer price = priceByWeaponId.get(weaponId);
        if (price == null) {
            throw new IllegalArgumentException("unknown sale price weapon id: " + weaponId);
        }
        return price;
    }

    public int investedGoldFor(String weaponId) {
        return enhanceCostService.investedGoldFor(weaponId);
    }

    public double profitMultiplierFor(String weaponId) {
        int investedGold = investedGoldFor(weaponId);
        if (investedGold <= 0) {
            return MIN_PROFIT_MULTIPLIER;
        }
        return findPrice(weaponId) / (double) investedGold;
    }
}
