package com.codex.swordgrowth.web.dto;

import jakarta.validation.constraints.NotBlank;

public record EvolutionAttemptRequest(
        @NotBlank String userId
) {
}

