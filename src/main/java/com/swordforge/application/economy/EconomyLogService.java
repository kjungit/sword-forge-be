package com.swordforge.application.economy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swordforge.domain.economy.EconomyTransactionLogEntity;
import com.swordforge.domain.economy.EconomyTransactionLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

@Service
public class EconomyLogService {

    private final EconomyTransactionLogRepository economyTransactionLogRepository;
    private final ObjectMapper objectMapper;

    public EconomyLogService(
            EconomyTransactionLogRepository economyTransactionLogRepository,
            ObjectMapper objectMapper
    ) {
        this.economyTransactionLogRepository = economyTransactionLogRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void logMaterialDeltas(
            String userId,
            String transactionType,
            String referenceId,
            Map<String, Integer> deltas,
            Map<String, Integer> balancesAfter,
            Map<String, Object> details
    ) {
        logDeltas(userId, transactionType, referenceId, "material", deltas, balancesAfter, details);
    }

    @Transactional
    public void logSpecialItemDeltas(
            String userId,
            String transactionType,
            String referenceId,
            Map<String, Integer> deltas,
            Map<String, Integer> balancesAfter,
            Map<String, Object> details
    ) {
        logDeltas(userId, transactionType, referenceId, "special_item", deltas, balancesAfter, details);
    }

    public Page<EconomyTransactionLogEntity> findByUserId(
            String userId,
            String transactionType,
            String resourceKind,
            String resourceId,
            Pageable pageable
    ) {
        return economyTransactionLogRepository.findByUserIdWithFilters(
                userId,
                blankToNull(transactionType),
                blankToNull(resourceKind),
                blankToNull(resourceId),
                pageable
        );
    }

    private void logDeltas(
            String userId,
            String transactionType,
            String referenceId,
            String resourceKind,
            Map<String, Integer> deltas,
            Map<String, Integer> balancesAfter,
            Map<String, Object> details
    ) {
        String detailsJson = writeJson(details);
        Instant now = Instant.now();
        for (Map.Entry<String, Integer> entry : deltas.entrySet()) {
            int amount = entry.getValue();
            if (amount == 0) {
                continue;
            }
            String resourceId = entry.getKey();
            economyTransactionLogRepository.save(new EconomyTransactionLogEntity(
                    userId,
                    transactionType,
                    referenceId,
                    resourceKind,
                    resourceId,
                    amount,
                    balancesAfter.getOrDefault(resourceId, 0),
                    detailsJson,
                    now
            ));
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("failed to serialize economy log payload", ex);
        }
    }
}
