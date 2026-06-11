package com.codex.swordgrowth.domain.item;

import java.util.List;

public record FailureRewardDefinition(
        String groupId,
        List<RewardRange> rewards
) {
}

