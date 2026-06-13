package com.swordforge.domain.idle;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "idle_income_states")
public class IdleIncomeStateEntity {

    @Id
    @Column(name = "user_id", nullable = false, length = 128)
    private String userId;

    @Column(name = "last_claimed_at", nullable = false)
    private Instant lastClaimedAt;

    protected IdleIncomeStateEntity() {
    }

    public IdleIncomeStateEntity(String userId, Instant lastClaimedAt) {
        this.userId = userId;
        this.lastClaimedAt = lastClaimedAt;
    }

    public String getUserId() {
        return userId;
    }

    public Instant getLastClaimedAt() {
        return lastClaimedAt;
    }

    public void setLastClaimedAt(Instant lastClaimedAt) {
        this.lastClaimedAt = lastClaimedAt;
    }
}
