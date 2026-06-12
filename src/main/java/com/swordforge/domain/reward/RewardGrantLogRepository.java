package com.swordforge.domain.reward;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RewardGrantLogRepository extends JpaRepository<RewardGrantLogEntity, Long> {
    @Query("""
            select log from RewardGrantLogEntity log
            where log.userId = :userId
              and (:rewardKind is null or log.rewardKind = :rewardKind)
              and (:sourceType is null or log.sourceType = :sourceType)
            order by log.createdAt desc
            """)
    Page<RewardGrantLogEntity> findByUserIdWithFilters(
            @Param("userId") String userId,
            @Param("rewardKind") String rewardKind,
            @Param("sourceType") String sourceType,
            Pageable pageable
    );
}
