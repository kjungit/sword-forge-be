package com.swordforge.web.dto;

import jakarta.validation.constraints.NotBlank;

public record EvolutionAttemptRequest(
        @NotBlank String userId
) {
}

