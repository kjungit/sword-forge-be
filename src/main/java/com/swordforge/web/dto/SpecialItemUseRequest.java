package com.swordforge.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record SpecialItemUseRequest(
        @NotBlank String userId,
        @NotBlank String itemId,
        @Min(1) int amount
) {
}

