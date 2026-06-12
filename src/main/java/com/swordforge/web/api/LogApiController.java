package com.swordforge.web.api;

import com.swordforge.application.reward.RewardLogService;
import com.swordforge.common.api.ApiResponse;
import com.swordforge.common.api.PageResponse;
import com.swordforge.web.dto.EnhanceAttemptLogResponse;
import com.swordforge.web.dto.RewardGrantLogResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/logs")
public class LogApiController {

    private final RewardLogService rewardLogService;
    private final com.swordforge.domain.enhance.EnhanceAttemptRepository enhanceAttemptRepository;

    public LogApiController(
            RewardLogService rewardLogService,
            com.swordforge.domain.enhance.EnhanceAttemptRepository enhanceAttemptRepository
    ) {
        this.rewardLogService = rewardLogService;
        this.enhanceAttemptRepository = enhanceAttemptRepository;
    }

    @GetMapping("/enhance/{userId}")
    public ApiResponse<PageResponse<EnhanceAttemptLogResponse>> listEnhanceLogs(
            @PathVariable String userId,
            @RequestParam(required = false) String outcome,
            @RequestParam(required = false) Boolean protectionUsed,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = pageRequest(page, size);
        return ApiResponse.ok(PageResponse.from(enhanceAttemptRepository
                .findByUserIdWithFilters(userId, blankToNull(outcome), protectionUsed, pageable)
                .map(EnhanceAttemptLogResponse::from)));
    }

    @GetMapping("/rewards/{userId}")
    public ApiResponse<PageResponse<RewardGrantLogResponse>> listRewardLogs(
            @PathVariable String userId,
            @RequestParam(required = false) String rewardKind,
            @RequestParam(required = false) String sourceType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = pageRequest(page, size);
        return ApiResponse.ok(PageResponse.from(rewardLogService
                .findByUserId(userId, rewardKind, sourceType, pageable)
                .map(RewardGrantLogResponse::from)));
    }

    private Pageable pageRequest(int page, int size) {
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
