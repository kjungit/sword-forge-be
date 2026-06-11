package com.codex.swordgrowth.web.api;

import com.codex.swordgrowth.application.enhance.EnhanceService;
import com.codex.swordgrowth.application.reward.RewardLogService;
import com.codex.swordgrowth.common.api.ApiResponse;
import com.codex.swordgrowth.web.dto.EnhanceAttemptLogResponse;
import com.codex.swordgrowth.web.dto.RewardGrantLogResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/logs")
public class LogApiController {

    private final RewardLogService rewardLogService;
    private final com.codex.swordgrowth.domain.enhance.EnhanceAttemptRepository enhanceAttemptRepository;

    public LogApiController(
            RewardLogService rewardLogService,
            com.codex.swordgrowth.domain.enhance.EnhanceAttemptRepository enhanceAttemptRepository
    ) {
        this.rewardLogService = rewardLogService;
        this.enhanceAttemptRepository = enhanceAttemptRepository;
    }

    @GetMapping("/enhance/{userId}")
    public ApiResponse<List<EnhanceAttemptLogResponse>> listEnhanceLogs(@PathVariable String userId) {
        return ApiResponse.ok(enhanceAttemptRepository.findTop20ByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(EnhanceAttemptLogResponse::from)
                .toList());
    }

    @GetMapping("/rewards/{userId}")
    public ApiResponse<List<RewardGrantLogResponse>> listRewardLogs(@PathVariable String userId) {
        return ApiResponse.ok(rewardLogService.findRecentByUserId(userId).stream()
                .map(RewardGrantLogResponse::from)
                .toList());
    }
}
