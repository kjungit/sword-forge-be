package com.swordforge.web.api;

import com.swordforge.application.probability.ProbabilityDisclosureService;
import com.swordforge.common.api.ApiResponse;
import com.swordforge.common.security.RequestUserGuard;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/probabilities")
public class ProbabilityApiController {

    private final ProbabilityDisclosureService probabilityDisclosureService;
    private final RequestUserGuard requestUserGuard;

    public ProbabilityApiController(
            ProbabilityDisclosureService probabilityDisclosureService,
            RequestUserGuard requestUserGuard
    ) {
        this.probabilityDisclosureService = probabilityDisclosureService;
        this.requestUserGuard = requestUserGuard;
    }

    @GetMapping("/enhance/{weaponId}")
    public ApiResponse<ProbabilityDisclosureService.EnhanceProbability> enhance(
            @PathVariable String weaponId,
            @RequestParam(required = false) String userId,
            @RequestParam(defaultValue = "false") boolean useProtection,
            @RequestParam(required = false) String rateBoostItemId
    ) {
        if (userId != null && !userId.isBlank()) {
            requestUserGuard.requireSelfOrAdmin(userId);
        }
        return ApiResponse.ok(probabilityDisclosureService.discloseEnhance(
                userId,
                weaponId,
                useProtection,
                rateBoostItemId
        ));
    }
}
