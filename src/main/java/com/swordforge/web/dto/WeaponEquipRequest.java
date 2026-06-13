package com.swordforge.web.dto;

import jakarta.validation.constraints.NotBlank;

public record WeaponEquipRequest(
        @NotBlank String userId,
        @NotBlank String weaponId
) {
}
