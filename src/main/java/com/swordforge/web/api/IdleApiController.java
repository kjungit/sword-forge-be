package com.swordforge.web.api;

import com.swordforge.application.idle.IdleIncomeService;
import com.swordforge.common.api.ApiResponse;
import com.swordforge.common.security.RequestUserGuard;
import com.swordforge.web.dto.IdleClaimRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/idle")
public class IdleApiController {

    private final IdleIncomeService idleIncomeService;
    private final RequestUserGuard requestUserGuard;

    public IdleApiController(IdleIncomeService idleIncomeService, RequestUserGuard requestUserGuard) {
        this.idleIncomeService = idleIncomeService;
        this.requestUserGuard = requestUserGuard;
    }

    @PostMapping("/claim")
    public ApiResponse<IdleIncomeService.IdleClaimResult> claim(@Valid @RequestBody IdleClaimRequest request) {
        requestUserGuard.requireSelfOrAdmin(request.userId());
        return ApiResponse.ok(idleIncomeService.claim(request.userId()));
    }
}
