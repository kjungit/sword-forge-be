package com.swordforge.web.dto;

import jakarta.validation.constraints.NotBlank;

public record IdleClaimRequest(
        @NotBlank String userId
) {
}
