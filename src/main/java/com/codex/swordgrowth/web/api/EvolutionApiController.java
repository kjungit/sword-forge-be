package com.codex.swordgrowth.web.api;

import com.codex.swordgrowth.application.evolution.EvolutionService;
import com.codex.swordgrowth.common.api.ApiResponse;
import com.codex.swordgrowth.web.dto.EvolutionAttemptRequest;
import com.codex.swordgrowth.web.dto.EvolutionResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/evolution")
public class EvolutionApiController {

    private final EvolutionService evolutionService;

    public EvolutionApiController(EvolutionService evolutionService) {
        this.evolutionService = evolutionService;
    }

    @GetMapping("/preview/{weaponId}")
    public ApiResponse<EvolutionService.EvolutionPreview> preview(@PathVariable String weaponId) {
        return ApiResponse.ok(evolutionService.preview(weaponId));
    }

    @PostMapping("/attempt")
    public ApiResponse<EvolutionResponse> attempt(@Valid @RequestBody EvolutionAttemptRequest request) {
        return ApiResponse.ok(EvolutionResponse.from(evolutionService.evolve(request.userId())));
    }
}
