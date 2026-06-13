package com.swordforge.application.idle;

import com.swordforge.application.economy.EconomyLogService;
import com.swordforge.application.save.PlayerSaveService;
import com.swordforge.application.weapon.WeaponCatalogService;
import com.swordforge.domain.idle.IdleIncomeStateEntity;
import com.swordforge.domain.idle.IdleIncomeStateRepository;
import com.swordforge.domain.save.PlayerSaveData;
import com.swordforge.domain.weapon.WeaponDefinition;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class IdleIncomeService {

    private static final int MAX_CLAIM_SECONDS = 3600;
    private static final int GOLD_DAMAGE_DIVISOR = 10;
    private static final double CRITICAL_RATE = 0.10;
    private static final double CRITICAL_MULTIPLIER = 2.0;

    private final IdleIncomeStateRepository idleIncomeStateRepository;
    private final PlayerSaveService playerSaveService;
    private final WeaponCatalogService weaponCatalogService;
    private final EconomyLogService economyLogService;

    public IdleIncomeService(
            IdleIncomeStateRepository idleIncomeStateRepository,
            PlayerSaveService playerSaveService,
            WeaponCatalogService weaponCatalogService,
            EconomyLogService economyLogService
    ) {
        this.idleIncomeStateRepository = idleIncomeStateRepository;
        this.playerSaveService = playerSaveService;
        this.weaponCatalogService = weaponCatalogService;
        this.economyLogService = economyLogService;
    }

    @Transactional
    public IdleClaimResult claim(String userId) {
        Instant now = Instant.now();
        PlayerSaveData currentSave = playerSaveService.getOrCreate(userId);
        WeaponDefinition weapon = weaponCatalogService.findById(currentSave.currentWeaponId());
        IdleIncomeStateEntity state = idleIncomeStateRepository.findById(userId)
                .orElseGet(() -> new IdleIncomeStateEntity(userId, now.minusSeconds(1)));

        int elapsedSeconds = elapsedSeconds(state.getLastClaimedAt(), now);
        int attackTicks = Math.min(MAX_CLAIM_SECONDS, elapsedSeconds);
        boolean critical = attackTicks > 0 && ThreadLocalRandom.current().nextDouble() < CRITICAL_RATE;
        int damage = attackTicks <= 0 ? 0 : rollDamage(weapon.attackPower(), critical) * attackTicks;
        int goldGained = damage <= 0 ? 0 : Math.max(1, damage / GOLD_DAMAGE_DIVISOR);

        PlayerSaveData updatedSave = currentSave;
        if (goldGained > 0) {
            updatedSave = playerSaveService.mutate(userId, save -> new PlayerSaveData(
                    save.userId(),
                    save.currentWeaponId(),
                    playerSaveService.addGold(save.materials(), goldGained),
                    save.specialItems(),
                    save.weaponInventory(),
                    save.lockedWeaponIds(),
                    save.ownedWeaponIds(),
                    save.unlockedWeaponShop(),
                    save.discoveredWeaponIds(),
                    save.highestReachedWeaponId(),
                    save.pityStacks(),
                    now
            ));
            economyLogService.logMaterialDeltas(
                    userId,
                    "idle_gold_claim",
                    weapon.id(),
                    Map.of(PlayerSaveService.GOLD_MATERIAL_ID, goldGained),
                    updatedSave.materials(),
                    Map.of(
                            "weaponId", weapon.id(),
                            "attackPower", weapon.attackPower(),
                            "attackTicks", attackTicks,
                            "damage", damage,
                            "isCritical", critical
                    )
            );
        }

        state.setLastClaimedAt(now);
        idleIncomeStateRepository.save(state);

        return new IdleClaimResult(
                userId,
                weapon.id(),
                damage,
                critical,
                goldGained,
                playerSaveService.goldOf(updatedSave.materials()),
                now
        );
    }

    private int elapsedSeconds(Instant lastClaimedAt, Instant now) {
        if (lastClaimedAt == null || lastClaimedAt.isAfter(now)) {
            return 0;
        }
        long seconds = Duration.between(lastClaimedAt, now).toSeconds();
        if (seconds <= 0) {
            return 0;
        }
        return seconds > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) seconds;
    }

    private int rollDamage(int attackPower, boolean critical) {
        double variance = ThreadLocalRandom.current().nextDouble(0.85, 1.16);
        double multiplier = critical ? CRITICAL_MULTIPLIER : 1.0;
        return Math.max(1, (int) Math.round(attackPower * variance * multiplier));
    }

    public record IdleClaimResult(
            String userId,
            String weaponId,
            int damage,
            boolean isCritical,
            int goldGained,
            int totalGold,
            Instant lastClaimedAt
    ) {
    }
}
