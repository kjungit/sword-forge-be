package com.codex.swordgrowth.domain.enhance;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "enhance_attempt_logs")
public class EnhanceAttemptEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, length = 128)
    private String userId;

    @Column(name = "weapon_id", nullable = false, length = 128)
    private String weaponId;

    @Column(name = "outcome", nullable = false, length = 32)
    private String outcome;

    @Column(name = "roll_value", nullable = false)
    private double rollValue;

    @Column(name = "success_threshold", nullable = false)
    private double successThreshold;

    @Column(name = "protection_used", nullable = false)
    private boolean protectionUsed;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected EnhanceAttemptEntity() {
    }

    public EnhanceAttemptEntity(
            String userId,
            String weaponId,
            String outcome,
            double rollValue,
            double successThreshold,
            boolean protectionUsed,
            Instant createdAt
    ) {
        this.userId = userId;
        this.weaponId = weaponId;
        this.outcome = outcome;
        this.rollValue = rollValue;
        this.successThreshold = successThreshold;
        this.protectionUsed = protectionUsed;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getWeaponId() {
        return weaponId;
    }

    public String getOutcome() {
        return outcome;
    }

    public double getRollValue() {
        return rollValue;
    }

    public double getSuccessThreshold() {
        return successThreshold;
    }

    public boolean isProtectionUsed() {
        return protectionUsed;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
