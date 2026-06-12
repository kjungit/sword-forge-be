package com.swordforge.web.dto;

import com.swordforge.application.shop.WeaponPurchaseService;

import java.util.Map;

public record WeaponSaleResponse(
        String userId,
        String weaponId,
        int amount,
        int unitGoldPrice,
        int totalGold,
        int remainingGold,
        String equippedWeaponId,
        Map<String, Integer> weaponInventory
) {
    public static WeaponSaleResponse from(WeaponPurchaseService.SaleResult result) {
        return new WeaponSaleResponse(
                result.userId(),
                result.weaponId(),
                result.amount(),
                result.unitGoldPrice(),
                result.totalGold(),
                result.remainingGold(),
                result.equippedWeaponId(),
                result.weaponInventory()
        );
    }
}
