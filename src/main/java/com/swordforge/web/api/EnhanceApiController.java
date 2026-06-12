package com.swordforge.web.api;

import com.swordforge.application.enhance.EnhanceService;
import com.swordforge.common.api.ApiResponse;
import com.swordforge.common.security.RequestUserGuard;
import com.swordforge.web.dto.EnhanceAttemptRequest;
import com.swordforge.web.dto.EnhanceAttemptResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/enhance")
public class EnhanceApiController {

    private final EnhanceService enhanceService;
    private final RequestUserGuard requestUserGuard;

    public EnhanceApiController(EnhanceService enhanceService, RequestUserGuard requestUserGuard) {
        this.enhanceService = enhanceService;
        this.requestUserGuard = requestUserGuard;
    }

    @PostMapping("/preview")
    public ApiResponse<EnhanceService.EnhancePreview> preview(@Valid @RequestBody EnhanceAttemptRequest request) {
        requestUserGuard.requireSelfOrAdmin(request.userId());
        return ApiResponse.ok(enhanceService.preview(request.userId(), request.weaponId(), request.useProtection()));
    }

    @PostMapping("/attempt")
    public ApiResponse<EnhanceAttemptResponse> attempt(@Valid @RequestBody EnhanceAttemptRequest request) {
        requestUserGuard.requireSelfOrAdmin(request.userId());
        return ApiResponse.ok(EnhanceAttemptResponse.from(
                enhanceService.attempt(request.userId(), request.weaponId(), request.useProtection(), request.rateBoostItemId())
        ));
    }
}
