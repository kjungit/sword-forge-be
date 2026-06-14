package com.swordforge.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record WeaponSaleRequest(
        @NotBlank String userId,
        @NotBlank String weaponId,
        @Min(1) int amount
) {
}
