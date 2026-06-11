package com.codex.swordgrowth.web.dto;

import com.codex.swordgrowth.application.shop.WeaponPurchaseService;

import java.util.Map;

public record WeaponPurchaseResponse(
        String userId,
        String weaponId,
        Map<String, Integer> cost,
        Map<String, Integer> remainingMaterials,
        String equippedWeaponId
) {
    public static WeaponPurchaseResponse from(WeaponPurchaseService.PurchaseResult result) {
        return new WeaponPurchaseResponse(
                result.userId(),
                result.weaponId(),
                result.cost(),
                result.remainingMaterials(),
                result.equippedWeaponId()
        );
    }
}

