package com.codex.swordgrowth.domain.enhance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EnhanceAttemptRepository extends JpaRepository<EnhanceAttemptEntity, Long> {
    List<EnhanceAttemptEntity> findTop20ByUserIdOrderByCreatedAtDesc(String userId);
}
