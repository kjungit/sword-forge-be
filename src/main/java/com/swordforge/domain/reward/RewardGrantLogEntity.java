package com.swordforge.domain.reward;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "reward_grant_logs")
public class RewardGrantLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, length = 128)
    private String userId;

    @Column(name = "source_type", nullable = false, length = 64)
    private String sourceType;

    @Column(name = "source_id", nullable = false, length = 128)
    private String sourceId;

    @Column(name = "reward_kind", nullable = false, length = 32)
    private String rewardKind;

    @Column(name = "reward_id", nullable = false, length = 128)
    private String rewardId;

    @Column(name = "amount", nullable = false)
    private int amount;

    @Column(name = "details_json", nullable = false, columnDefinition = "text")
    private String detailsJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected RewardGrantLogEntity() {
    }

    public RewardGrantLogEntity(
            String userId,
            String sourceType,
            String sourceId,
            String rewardKind,
            String rewardId,
            int amount,
            String detailsJson,
            Instant createdAt
    ) {
        this.userId = userId;
        this.sourceType = sourceType;
        this.sourceId = sourceId;
        this.rewardKind = rewardKind;
        this.rewardId = rewardId;
        this.amount = amount;
        this.detailsJson = detailsJson;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getSourceType() {
        return sourceType;
    }

    public String getSourceId() {
        return sourceId;
    }

    public String getRewardKind() {
        return rewardKind;
    }

    public String getRewardId() {
        return rewardId;
    }

    public int getAmount() {
        return amount;
    }

    public String getDetailsJson() {
        return detailsJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
