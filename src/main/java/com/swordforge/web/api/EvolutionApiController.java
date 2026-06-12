package com.swordforge.web.api;

import com.swordforge.application.evolution.EvolutionService;
import com.swordforge.common.api.ApiResponse;
import com.swordforge.common.security.RequestUserGuard;
import com.swordforge.web.dto.EvolutionAttemptRequest;
import com.swordforge.web.dto.EvolutionResponse;
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
    private final RequestUserGuard requestUserGuard;

    public EvolutionApiController(EvolutionService evolutionService, RequestUserGuard requestUserGuard) {
        this.evolutionService = evolutionService;
        this.requestUserGuard = requestUserGuard;
    }

    @GetMapping("/preview/{weaponId}")
    public ApiResponse<EvolutionService.EvolutionPreview> preview(@PathVariable String weaponId) {
        return ApiResponse.ok(evolutionService.preview(weaponId));
    }

    @PostMapping("/attempt")
    public ApiResponse<EvolutionResponse> attempt(@Valid @RequestBody EvolutionAttemptRequest request) {
        requestUserGuard.requireSelfOrAdmin(request.userId());
        return ApiResponse.ok(EvolutionResponse.from(evolutionService.evolve(request.userId())));
    }
}
