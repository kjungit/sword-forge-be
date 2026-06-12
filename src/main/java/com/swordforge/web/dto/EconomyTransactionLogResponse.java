package com.swordforge.web.dto;

import com.swordforge.domain.economy.EconomyTransactionLogEntity;

import java.time.Instant;

public record EconomyTransactionLogResponse(
        Long id,
        String userId,
        String transactionType,
        String referenceId,
        String resourceKind,
        String resourceId,
        int amount,
        Integer balanceAfter,
        String detailsJson,
        Instant createdAt
) {
    public static EconomyTransactionLogResponse from(EconomyTransactionLogEntity entity) {
        return new EconomyTransactionLogResponse(
                entity.getId(),
                entity.getUserId(),
                entity.getTransactionType(),
                entity.getReferenceId(),
                entity.getResourceKind(),
                entity.getResourceId(),
                entity.getAmount(),
                entity.getBalanceAfter(),
                entity.getDetailsJson(),
                entity.getCreatedAt()
        );
    }
}
