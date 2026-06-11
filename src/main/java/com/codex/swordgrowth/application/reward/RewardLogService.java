package com.codex.swordgrowth.application.reward;

import com.codex.swordgrowth.domain.reward.RewardGrantLogEntity;
import com.codex.swordgrowth.domain.reward.RewardGrantLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
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

    public List<RewardGrantLogEntity> findRecentByUserId(String userId) {
        return rewardGrantLogRepository.findTop20ByUserIdOrderByCreatedAtDesc(userId);
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("failed to serialize reward log payload", ex);
        }
    }
}
