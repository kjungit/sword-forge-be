package com.swordforge.domain.economy;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "economy_transaction_logs")
public class EconomyTransactionLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, length = 128)
    private String userId;

    @Column(name = "transaction_type", nullable = false, length = 64)
    private String transactionType;

    @Column(name = "reference_id", nullable = false, length = 128)
    private String referenceId;

    @Column(name = "resource_kind", nullable = false, length = 32)
    private String resourceKind;

    @Column(name = "resource_id", nullable = false, length = 128)
    private String resourceId;

    @Column(name = "amount", nullable = false)
    private int amount;

    @Column(name = "balance_after")
    private Integer balanceAfter;

    @Column(name = "details_json", nullable = false, columnDefinition = "text")
    private String detailsJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected EconomyTransactionLogEntity() {
    }

    public EconomyTransactionLogEntity(
            String userId,
            String transactionType,
            String referenceId,
            String resourceKind,
            String resourceId,
            int amount,
            Integer balanceAfter,
            String detailsJson,
            Instant createdAt
    ) {
        this.userId = userId;
        this.transactionType = transactionType;
        this.referenceId = referenceId;
        this.resourceKind = resourceKind;
        this.resourceId = resourceId;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.detailsJson = detailsJson;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public String getResourceKind() {
        return resourceKind;
    }

    public String getResourceId() {
        return resourceId;
    }

    public int getAmount() {
        return amount;
    }

    public Integer getBalanceAfter() {
        return balanceAfter;
    }

    public String getDetailsJson() {
        return detailsJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
