package com.swordforge.web.dto;

import jakarta.validation.constraints.NotBlank;

public record WeaponLockRequest(
        @NotBlank String userId,
        @NotBlank String weaponId
) {
}
