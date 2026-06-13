package com.swordforge.web.dto;

import jakarta.validation.constraints.NotBlank;

public record WeaponPurchaseRequest(
        @NotBlank String userId,
        @NotBlank String weaponId
) {
}

