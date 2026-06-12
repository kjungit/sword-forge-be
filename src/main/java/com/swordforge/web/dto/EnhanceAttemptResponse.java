package com.swordforge.web.dto;

import com.swordforge.application.enhance.EnhanceService;
import com.swordforge.domain.enhance.EnhanceOutcome;

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
        String rateBoostItemId,
        int goldCost,
        int remainingGold,
        String pityKey,
        int pityStackBefore,
        int pityStackAfter,
        double pityBonus
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
                result.rateBoostItemId(),
                result.goldCost(),
                result.remainingGold(),
                result.pityKey(),
                result.pityStackBefore(),
                result.pityStackAfter(),
                result.pityBonus()
        );
    }
}
