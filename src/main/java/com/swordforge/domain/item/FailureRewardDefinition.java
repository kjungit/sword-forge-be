package com.swordforge.domain.item;

import java.util.List;

public record FailureRewardDefinition(
        String groupId,
        List<RewardRange> rewards
) {
}

