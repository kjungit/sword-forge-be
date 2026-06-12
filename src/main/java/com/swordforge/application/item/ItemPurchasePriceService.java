package com.swordforge.application.item;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swordforge.domain.item.ItemPurchasePriceDefinition;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ItemPurchasePriceService {

    private static final TypeReference<List<ItemPurchasePriceDefinition>> PRICE_LIST_TYPE = new TypeReference<>() {};

    private final ObjectMapper objectMapper;
    private Map<String, Integer> priceByItemId = Map.of();

    public ItemPurchasePriceService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    void loadItemPrices() {
        try (InputStream inputStream = new ClassPathResource("data/item_purchase_prices.json").getInputStream()) {
            List<ItemPurchasePriceDefinition> definitions = objectMapper.readValue(inputStream, PRICE_LIST_TYPE);
            LinkedHashMap<String, Integer> indexed = new LinkedHashMap<>();
            for (ItemPurchasePriceDefinition definition : definitions) {
                if (indexed.containsKey(definition.itemId())) {
                    throw new IllegalStateException("duplicate item purchase price id: " + definition.itemId());
                }
                indexed.put(definition.itemId(), definition.goldPrice());
            }
            priceByItemId = Map.copyOf(indexed);
        } catch (IOException ex) {
            throw new IllegalStateException("failed to load item purchase prices", ex);
        }
    }

    public int findPrice(String itemId) {
        Integer price = priceByItemId.get(itemId);
        if (price == null) {
            throw new IllegalArgumentException("unknown item purchase price id: " + itemId);
        }
        return price;
    }
}
