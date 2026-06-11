package com.codex.swordgrowth.domain.shop;

import java.util.Map;

public record WeaponPurchaseCostDefinition(
        String weaponId,
        Map<String, Integer> cost
) {
}

