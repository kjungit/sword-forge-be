package com.codex.swordgrowth.web.dto;

import com.codex.swordgrowth.application.enhance.EnhanceService;
import com.codex.swordgrowth.domain.enhance.EnhanceOutcome;

import java.util.Map;

public record EnhanceAttemptResponse(
        String userId,
        String weaponId,
        String outcome,
        double roll,
        double successThreshold,
        String nextWeaponId,
        boolean protectionUsed,
        Map<String, Integer> failureRewards,
        String rateBoostItemId
) {
    public static EnhanceAttemptResponse from(EnhanceService.EnhanceResult result) {
        EnhanceOutcome outcome = result.outcome();
        return new EnhanceAttemptResponse(
                result.userId(),
                result.weaponId(),
                outcome.name().toLowerCase(),
                result.roll(),
                result.successThreshold(),
                result.nextWeaponId(),
                result.protectionUsed(),
                result.failureRewards(),
                result.rateBoostItemId()
        );
    }
}
