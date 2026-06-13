package com.swordforge.web.dto;

import jakarta.validation.constraints.NotBlank;

public record EnhanceAttemptRequest(
        @NotBlank String userId,
        @NotBlank String weaponId,
        boolean useProtection,
        String rateBoostItemId
) {
}
