package com.swordforge.web.dto;

import com.swordforge.application.item.SpecialItemService;

import java.util.Map;

public record SpecialItemPurchaseResponse(
        String userId,
        String itemId,
        int amount,
        int unitGoldPrice,
        int totalGoldCost,
        int remainingGold,
        Map<String, Integer> specialItems
) {
    public static SpecialItemPurchaseResponse from(SpecialItemService.PurchaseResult result) {
        return new SpecialItemPurchaseResponse(
                result.userId(),
                result.itemId(),
                result.amount(),
                result.unitGoldPrice(),
                result.totalGoldCost(),
                result.remainingGold(),
                result.specialItems()
        );
    }
}
