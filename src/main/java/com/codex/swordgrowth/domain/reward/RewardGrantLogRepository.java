package com.codex.swordgrowth.domain.reward;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RewardGrantLogRepository extends JpaRepository<RewardGrantLogEntity, Long> {
    List<RewardGrantLogEntity> findTop20ByUserIdOrderByCreatedAtDesc(String userId);
}
