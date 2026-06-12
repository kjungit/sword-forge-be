package com.swordforge.web.dto;

import com.swordforge.domain.reward.RewardGrantLogEntity;

import java.time.Instant;

public record RewardGrantLogResponse(
        Long id,
        String userId,
        String sourceType,
        String sourceId,
        String rewardKind,
        String rewardId,
        int amount,
        String detailsJson,
        Instant createdAt
) {
    public static RewardGrantLogResponse from(RewardGrantLogEntity entity) {
        return new RewardGrantLogResponse(
                entity.getId(),
                entity.getUserId(),
                entity.getSourceType(),
                entity.getSourceId(),
                entity.getRewardKind(),
                entity.getRewardId(),
                entity.getAmount(),
                entity.getDetailsJson(),
                entity.getCreatedAt()
        );
    }
}
