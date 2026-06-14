package com.swordforge.domain.economy;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EconomyTransactionLogRepository extends JpaRepository<EconomyTransactionLogEntity, Long> {

    @Query("""
            select log from EconomyTransactionLogEntity log
            where log.userId = :userId
              and (:transactionType is null or log.transactionType = :transactionType)
              and (:resourceKind is null or log.resourceKind = :resourceKind)
              and (:resourceId is null or log.resourceId = :resourceId)
            order by log.createdAt desc
            """)
    Page<EconomyTransactionLogEntity> findByUserIdWithFilters(
            @Param("userId") String userId,
            @Param("transactionType") String transactionType,
            @Param("resourceKind") String resourceKind,
            @Param("resourceId") String resourceId,
            Pageable pageable
    );
}
