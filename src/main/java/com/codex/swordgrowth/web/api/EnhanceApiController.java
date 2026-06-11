package com.codex.swordgrowth.web.api;

import com.codex.swordgrowth.application.enhance.EnhanceService;
import com.codex.swordgrowth.common.api.ApiResponse;
import com.codex.swordgrowth.web.dto.EnhanceAttemptRequest;
import com.codex.swordgrowth.web.dto.EnhanceAttemptResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/enhance")
public class EnhanceApiController {

    private final EnhanceService enhanceService;

    public EnhanceApiController(EnhanceService enhanceService) {
        this.enhanceService = enhanceService;
    }

    @PostMapping("/preview")
    public ApiResponse<EnhanceService.EnhancePreview> preview(@Valid @RequestBody EnhanceAttemptRequest request) {
        return ApiResponse.ok(enhanceService.preview(request.weaponId(), request.useProtection()));
    }

    @PostMapping("/attempt")
    public ApiResponse<EnhanceAttemptResponse> attempt(@Valid @RequestBody EnhanceAttemptRequest request) {
        return ApiResponse.ok(EnhanceAttemptResponse.from(
                enhanceService.attempt(request.userId(), request.weaponId(), request.useProtection(), request.rateBoostItemId())
        ));
    }
}
