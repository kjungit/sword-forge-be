package com.codex.swordgrowth.application.reward;

import com.codex.swordgrowth.domain.item.FailureRewardDefinition;
import com.codex.swordgrowth.domain.item.RewardRange;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class FailureRewardService {

    private static final TypeReference<List<FailureRewardDefinition>> REWARD_LIST_TYPE = new TypeReference<>() {};

    private final ObjectMapper objectMapper;
    private Map<String, FailureRewardDefinition> rewardByGroup = Map.of();

    public FailureRewardService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    void loadRewardCatalog() {
        try (InputStream inputStream = new ClassPathResource("data/failure_rewards.json").getInputStream()) {
            List<FailureRewardDefinition> rewardDefinitions = objectMapper.readValue(inputStream, REWARD_LIST_TYPE);
            LinkedHashMap<String, FailureRewardDefinition> indexed = new LinkedHashMap<>();
            for (FailureRewardDefinition definition : rewardDefinitions) {
                if (indexed.containsKey(definition.groupId())) {
                    throw new IllegalStateException("duplicate reward group id: " + definition.groupId());
                }
                indexed.put(definition.groupId(), definition);
            }
            rewardByGroup = Map.copyOf(indexed);
        } catch (IOException ex) {
            throw new IllegalStateException("failed to load failure reward catalog", ex);
        }
    }

    public Map<String, Integer> generateRewards(String groupId) {
        FailureRewardDefinition definition = rewardByGroup.get(groupId);
        if (definition == null) {
            throw new IllegalArgumentException("unknown reward group id: " + groupId);
        }

        Map<String, Integer> rewards = new LinkedHashMap<>();
        for (RewardRange rewardRange : definition.rewards()) {
            int amount = randomBetween(rewardRange.minAmount(), rewardRange.maxAmount());
            rewards.merge(rewardRange.materialId(), amount, Integer::sum);
        }
        return Map.copyOf(rewards);
    }

    private int randomBetween(int min, int max) {
        if (min > max) {
            throw new IllegalArgumentException("min must be <= max");
        }
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }
}

