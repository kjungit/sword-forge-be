package com.swordforge.domain.enhance;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EnhanceAttemptRepository extends JpaRepository<EnhanceAttemptEntity, Long> {
    @Query("""
            select log from EnhanceAttemptEntity log
            where log.userId = :userId
              and (:outcome is null or log.outcome = :outcome)
              and (:protectionUsed is null or log.protectionUsed = :protectionUsed)
            order by log.createdAt desc
            """)
    Page<EnhanceAttemptEntity> findByUserIdWithFilters(
            @Param("userId") String userId,
            @Param("outcome") String outcome,
            @Param("protectionUsed") Boolean protectionUsed,
            Pageable pageable
    );
}
