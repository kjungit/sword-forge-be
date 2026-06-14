package com.swordforge.web.dto;

import com.swordforge.domain.enhance.EnhanceAttemptEntity;

import java.time.Instant;

public record EnhanceAttemptLogResponse(
        Long id,
        String userId,
        String weaponId,
        String outcome,
        double rollValue,
        double successThreshold,
        boolean protectionUsed,
        String detailsJson,
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
                entity.getDetailsJson(),
                entity.getCreatedAt()
        );
    }
}
