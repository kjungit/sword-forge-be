package com.codex.swordgrowth.web.dto;

import com.codex.swordgrowth.domain.enhance.EnhanceAttemptEntity;

import java.time.Instant;

public record EnhanceAttemptLogResponse(
        Long id,
        String userId,
        String weaponId,
        String outcome,
        double rollValue,
        double successThreshold,
        boolean protectionUsed,
        Instant createdAt
) {
    public static EnhanceAttemptLogResponse from(EnhanceAttemptEntity entity) {
        return new EnhanceAttemptLogResponse(
                entity.getId(),
                entity.getUserId(),
                entity.getWeaponId(),
                entity.getOutcome(),
                entity.getRollValue(),
                entity.getSuccessThreshold(),
                entity.isProtectionUsed(),
                entity.getCreatedAt()
        );
    }
}
