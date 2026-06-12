package com.swordforge.application.reward;

import com.swordforge.domain.reward.RewardGrantLogEntity;
import com.swordforge.domain.reward.RewardGrantLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

@Service
public class RewardLogService {

    private final RewardGrantLogRepository rewardGrantLogRepository;
    private final ObjectMapper objectMapper;

    public RewardLogService(RewardGrantLogRepository rewardGrantLogRepository, ObjectMapper objectMapper) {
        this.rewardGrantLogRepository = rewardGrantLogRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void logMaterialRewards(String userId, String sourceType, String sourceId, Map<String, Integer> rewards) {
        for (Map.Entry<String, Integer> entry : rewards.entrySet()) {
            rewardGrantLogRepository.save(new RewardGrantLogEntity(
                    userId,
                    sourceType,
                    sourceId,
                    "material",
                    entry.getKey(),
                    entry.getValue(),
                    writeJson(Map.of("rewards", rewards)),
                    Instant.now()
            ));
        }
    }

    @Transactional
    public void logSpecialItemReward(String userId, String sourceType, String sourceId, String itemId, int amount) {
        rewardGrantLogRepository.save(new RewardGrantLogEntity(
                userId,
                sourceType,
                sourceId,
                "special_item",
                itemId,
                amount,
                writeJson(Map.of("itemId", itemId, "amount", amount)),
                Instant.now()
        ));
    }

    public Page<RewardGrantLogEntity> findByUserId(
            String userId,
            String rewardKind,
            String sourceType,
            Pageable pageable
    ) {
        return rewardGrantLogRepository.findByUserIdWithFilters(userId, blankToNull(rewardKind), blankToNull(sourceType), pageable);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("failed to serialize reward log payload", ex);
        }
    }
}
