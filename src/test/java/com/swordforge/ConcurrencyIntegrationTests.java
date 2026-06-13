package com.swordforge;

import com.swordforge.application.enhance.EnhanceService;
import com.swordforge.application.save.PlayerSaveService;
import com.swordforge.application.shop.WeaponPurchaseService;
import com.swordforge.domain.save.PlayerSaveData;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@SpringBootTest
class ConcurrencyIntegrationTests {

    @Autowired
    private PlayerSaveService playerSaveService;

    @Autowired
    private EnhanceService enhanceService;

    @Autowired
    private WeaponPurchaseService weaponPurchaseService;

    @Test
    void concurrentEnhanceAttemptsApplyOnlyOnce() throws Exception {
        String userId = "concurrent-enhance-user";
        playerSaveService.upsert(new PlayerSaveData(
                userId,
                "normal_01",
                Map.of("gold", 100),
                Map.of(),
                Map.of("normal_01", 1),
                List.of("normal_01"),
                List.of("normal_01"),
                List.of("normal_01"),
                "normal_01",
                Map.of(),
                Instant.now()
        ));

        List<ConcurrentResult> results = runConcurrently(2,
                () -> enhanceService.attempt(userId, "normal_01", false, 0.0));

        Assertions.assertEquals(1, successCount(results));
        Assertions.assertEquals(1, failureCount(results));

        PlayerSaveData updated = playerSaveService.getOrCreate(userId);
        Assertions.assertEquals("normal_02", updated.currentWeaponId());
        Assertions.assertEquals(1, updated.weaponInventory().get("normal_02"));
        Assertions.assertEquals(99, updated.materials().get("gold"));
    }

    @Test
    void concurrentWeaponSalesApplyOnlyOnce() throws Exception {
        String userId = "concurrent-sale-user";
        playerSaveService.upsert(new PlayerSaveData(
                userId,
                "normal_01",
                Map.of(),
                Map.of(),
                Map.of("normal_01", 1, "normal_02", 1),
                List.of("normal_01", "normal_02"),
                List.of("normal_01", "normal_02"),
                List.of("normal_01", "normal_02"),
                "normal_02",
                Map.of(),
                Instant.now()
        ));

        List<ConcurrentResult> results = runConcurrently(2,
                () -> weaponPurchaseService.sell(userId, "normal_02", 1));

        Assertions.assertEquals(1, successCount(results));
        Assertions.assertEquals(1, failureCount(results));

        PlayerSaveData updated = playerSaveService.getOrCreate(userId);
        Assertions.assertFalse(updated.weaponInventory().containsKey("normal_02"));
        Assertions.assertEquals(4, updated.materials().get("gold"));
    }

    private List<ConcurrentResult> runConcurrently(int workers, ThrowingRunnable action) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(workers);
        CountDownLatch ready = new CountDownLatch(workers);
        CountDownLatch start = new CountDownLatch(1);
        List<Callable<ConcurrentResult>> tasks = new ArrayList<>();
        for (int i = 0; i < workers; i++) {
            tasks.add(() -> {
                ready.countDown();
                start.await(5, TimeUnit.SECONDS);
                try {
                    action.run();
                    return new ConcurrentResult(true);
                } catch (Throwable ex) {
                    return new ConcurrentResult(false);
                }
            });
        }

        List<java.util.concurrent.Future<ConcurrentResult>> futures = tasks.stream()
                .map(executor::submit)
                .toList();
        Assertions.assertTrue(ready.await(5, TimeUnit.SECONDS));
        start.countDown();

        List<ConcurrentResult> results = new ArrayList<>();
        for (java.util.concurrent.Future<ConcurrentResult> future : futures) {
            results.add(future.get(5, TimeUnit.SECONDS));
        }
        executor.shutdownNow();
        return results;
    }

    private long successCount(List<ConcurrentResult> results) {
        return results.stream().filter(ConcurrentResult::success).count();
    }

    private long failureCount(List<ConcurrentResult> results) {
        return results.stream().filter(result -> !result.success()).count();
    }

    private record ConcurrentResult(boolean success) {
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
