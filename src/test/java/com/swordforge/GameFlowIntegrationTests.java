package com.swordforge;

import com.swordforge.application.evolution.EvolutionService;
import com.swordforge.application.enhance.EnhanceService;
import com.swordforge.application.item.SpecialItemService;
import com.swordforge.application.save.PlayerSaveService;
import com.swordforge.application.shop.WeaponPurchaseService;
import com.swordforge.domain.save.PlayerSaveData;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.closeTo;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class GameFlowIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PlayerSaveService playerSaveService;

    @Autowired
    private WeaponPurchaseService weaponPurchaseService;

    @Autowired
    private EnhanceService enhanceService;

    @Autowired
    private EvolutionService evolutionService;

    @Autowired
    private SpecialItemService specialItemService;

    @Test
    void healthEndpointWorks() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("up"));
    }

    @Test
    void weaponCatalogReturnsAllWeapons() throws Exception {
        mockMvc.perform(get("/api/v1/weapons"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(27));
    }

    @Test
    void specialItemCatalogReturnsAllItems() throws Exception {
        mockMvc.perform(get("/api/v1/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(6));
    }

    @Test
    void saveEndpointCreatesDefaultSave() throws Exception {
        mockMvc.perform(get("/api/v1/saves/test-user-default"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentWeaponId").value("normal_01"))
                .andExpect(jsonPath("$.data.weaponInventory.normal_01").value(1))
                .andExpect(jsonPath("$.data.materials.gold").value(20));
    }

    @Test
    void saveUpsertPersistsInventory() throws Exception {
        String body = """
                {
                  "currentWeaponId": "normal_01",
                  "materials": {"cracked_iron_piece": 12},
                  "specialItems": {},
                  "weaponInventory": {"normal_01": 1},
                  "ownedWeaponIds": ["normal_01"],
                  "unlockedWeaponShop": ["normal_01"],
                  "discoveredWeaponIds": ["normal_01"],
                  "highestReachedWeaponId": "normal_01"
                }
                """;

        mockMvc.perform(
                        put("/api/v1/saves/test-user-upsert")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.materials.cracked_iron_piece").value(12));
    }

    @Test
    void weaponEquipEndpointChangesCurrentWeaponFromStorage() throws Exception {
        playerSaveService.upsert(new PlayerSaveData(
                "equip-user",
                "normal_03",
                Map.of("gold", 20),
                Map.of(),
                Map.of("normal_01", 1, "normal_02", 1, "normal_03", 1),
                List.of("normal_01", "normal_02", "normal_03"),
                List.of("normal_01", "normal_02", "normal_03"),
                List.of("normal_01", "normal_02", "normal_03"),
                "normal_03",
                Map.of(),
                java.time.Instant.now()
        ));
        String body = """
                {
                  "userId": "equip-user",
                  "weaponId": "normal_02"
                }
                """;

        mockMvc.perform(
                        post("/api/v1/weapons/equip")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentWeaponId").value("normal_02"))
                .andExpect(jsonPath("$.data.weaponInventory.normal_03").value(1));
    }

    @Test
    void weaponEquipEndpointRejectsUnownedWeapon() throws Exception {
        String body = """
                {
                  "userId": "equip-unowned-user",
                  "weaponId": "normal_02"
                }
                """;

        mockMvc.perform(
                        post("/api/v1/weapons/equip")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void weaponLockEndpointPreventsSaleUntilUnlocked() throws Exception {
        String userId = "lock-sale-user";
        playerSaveService.upsert(new PlayerSaveData(
                userId,
                "normal_02",
                Map.of("gold", 10),
                Map.of(),
                Map.of("normal_01", 1, "normal_02", 1),
                List.of("normal_01", "normal_02"),
                List.of("normal_01", "normal_02"),
                List.of("normal_01", "normal_02"),
                "normal_02",
                Map.of(),
                java.time.Instant.now()
        ));
        String body = """
                {
                  "userId": "lock-sale-user",
                  "weaponId": "normal_02"
                }
                """;

        mockMvc.perform(
                        post("/api/v1/weapons/lock")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.lockedWeaponIds[0]").value("normal_02"));

        IllegalArgumentException locked = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> weaponPurchaseService.sell(userId, "normal_02", 1)
        );
        Assertions.assertTrue(locked.getMessage().contains("locked"));

        mockMvc.perform(
                        post("/api/v1/weapons/unlock")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.lockedWeaponIds.length()").value(0));

        WeaponPurchaseService.SaleResult result = weaponPurchaseService.sell(userId, "normal_02", 1);
        Assertions.assertEquals(14, result.remainingGold());
    }

    @Test
    void weaponLockEndpointRejectsUnownedWeapon() throws Exception {
        String body = """
                {
                  "userId": "lock-unowned-user",
                  "weaponId": "normal_02"
                }
                """;

        mockMvc.perform(
                        post("/api/v1/weapons/lock")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void specialItemGrantEndpointUpdatesSave() throws Exception {
        String body = """
                {
                  "userId": "grant-item-user",
                  "itemId": "enhance_rate_boost_5",
                  "amount": 2
                }
                """;

        mockMvc.perform(
                        post("/api/v1/items/grant")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.specialItems.enhance_rate_boost_5").value(2));
    }

    @Test
    void specialItemConsumeEndpointUpdatesSave() throws Exception {
        playerSaveService.upsert(new PlayerSaveData(
                "consume-item-user",
                "normal_01",
                Map.of(),
                Map.of("enhance_rate_boost_5", 2),
                Map.of("normal_01", 1),
                List.of("normal_01"),
                List.of("normal_01"),
                List.of("normal_01"),
                "normal_01",
                Map.of(),
                java.time.Instant.now()
        ));

        String body = """
                {
                  "userId": "consume-item-user",
                  "itemId": "enhance_rate_boost_5",
                  "amount": 1
                }
                """;

        mockMvc.perform(
                        post("/api/v1/items/consume")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.specialItems.enhance_rate_boost_5").value(1));
    }

    @Test
    void shopPurchaseConsumesMaterialsAndEquipsWeapon() {
        String userId = "purchase-user";
        playerSaveService.upsert(new PlayerSaveData(
                userId,
                "normal_01",
                Map.of("cracked_iron_piece", 50),
                Map.of(),
                Map.of("normal_01", 1),
                List.of("normal_01"),
                List.of("normal_01", "normal_02"),
                List.of("normal_01"),
                "normal_01",
                Map.of(),
                java.time.Instant.now()
        ));

        WeaponPurchaseService.PurchaseResult result = weaponPurchaseService.purchase(userId, "normal_02");

        Assertions.assertEquals("normal_02", result.equippedWeaponId());
        Assertions.assertEquals(48, result.remainingMaterials().get("cracked_iron_piece"));
    }

    @Test
    void shopSellWeaponAddsGoldAndPreservesStorage() {
        String userId = "sell-user";
        playerSaveService.upsert(new PlayerSaveData(
                userId,
                "normal_02",
                Map.of("gold", 10),
                Map.of(),
                Map.of("normal_01", 1, "normal_02", 1, "normal_03", 1),
                List.of("normal_01", "normal_02", "normal_03"),
                List.of("normal_01", "normal_02", "normal_03"),
                List.of("normal_01", "normal_02", "normal_03"),
                "normal_03",
                Map.of(),
                java.time.Instant.now()
        ));

        WeaponPurchaseService.SaleResult result = weaponPurchaseService.sell(userId, "normal_02", 1);

        Assertions.assertEquals(4, result.totalGold());
        Assertions.assertEquals(14, result.remainingGold());
        Assertions.assertEquals("normal_03", result.equippedWeaponId());
        Assertions.assertFalse(result.weaponInventory().containsKey("normal_02"));
        Assertions.assertTrue(result.weaponInventory().containsKey("normal_01"));
    }

    @Test
    void salePreviewUsesInvestedGoldFloor() {
        WeaponPurchaseService.SalePreview preview = weaponPurchaseService.salePreview("normal_10");

        Assertions.assertEquals("normal_10", preview.weaponId());
        Assertions.assertEquals(25, preview.investedGold());
        Assertions.assertEquals(50, preview.sellGold());
        Assertions.assertEquals(2.0, preview.profitMultiplier(), 0.0001);
    }

    @Test
    void sellingLastNonStarterWeaponFallsBackToStarter() {
        String userId = "fallback-sale-user";
        playerSaveService.upsert(new PlayerSaveData(
                userId,
                "rare_01",
                Map.of(),
                Map.of(),
                Map.of("rare_01", 1),
                List.of("rare_01"),
                List.of("rare_01"),
                List.of("rare_01"),
                "rare_01",
                Map.of(),
                java.time.Instant.now()
        ));

        WeaponPurchaseService.SaleResult result = weaponPurchaseService.sell(userId, "rare_01", 1);

        Assertions.assertTrue(result.willFallbackToStarter());
        Assertions.assertEquals("normal_01", result.equippedWeaponId());
        Assertions.assertEquals(50, result.totalGold());
        Assertions.assertEquals(50, result.remainingGold());
        Assertions.assertEquals(1, result.weaponInventory().get("normal_01"));
        Assertions.assertFalse(result.weaponInventory().containsKey("rare_01"));
    }

    @Test
    void sellingOnlyStarterWeaponIsRejected() {
        String userId = "only-starter-sale-user";
        playerSaveService.upsert(new PlayerSaveData(
                userId,
                "normal_01",
                Map.of(),
                Map.of(),
                Map.of("normal_01", 1),
                List.of("normal_01"),
                List.of("normal_01"),
                List.of("normal_01"),
                "normal_01",
                Map.of(),
                java.time.Instant.now()
        ));

        IllegalArgumentException ex = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> weaponPurchaseService.sell(userId, "normal_01", 1)
        );
        Assertions.assertTrue(ex.getMessage().contains("starter"));
    }

    @Test
    void itemPurchaseConsumesGoldAndAddsItem() throws Exception {
        String userId = "item-purchase-user";
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
                java.time.Instant.now()
        ));

        String body = """
                {
                  "userId": "item-purchase-user",
                  "itemId": "enhance_rate_boost_5",
                  "amount": 2
                }
                """;

        mockMvc.perform(
                        post("/api/v1/items/purchase")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalGoldCost").value(80))
                .andExpect(jsonPath("$.data.remainingGold").value(20))
                .andExpect(jsonPath("$.data.specialItems.enhance_rate_boost_5").value(2));
    }

    @Test
    void idleClaimGrantsGoldFromCurrentWeaponDamage() throws Exception {
        String userId = "idle-claim-user";
        playerSaveService.upsert(new PlayerSaveData(
                userId,
                "normal_02",
                Map.of("gold", 0),
                Map.of(),
                Map.of("normal_02", 1),
                List.of("normal_02"),
                List.of("normal_02"),
                List.of("normal_02"),
                "normal_02",
                Map.of(),
                java.time.Instant.now()
        ));
        String body = """
                {
                  "userId": "idle-claim-user"
                }
                """;

        mockMvc.perform(
                        post("/api/v1/idle/claim")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.weaponId").value("normal_02"))
                .andExpect(jsonPath("$.data.damage").isNumber())
                .andExpect(jsonPath("$.data.goldGained").isNumber())
                .andExpect(jsonPath("$.data.totalGold").isNumber())
                .andExpect(jsonPath("$.data.lastClaimedAt").exists());
    }

    @Test
    void enhancePreviewReturnsRates() {
        EnhanceService.EnhancePreview preview = enhanceService.preview("normal_01", false);
        Assertions.assertEquals("normal_02", preview.nextWeaponId());
        Assertions.assertEquals(0.97, preview.baseSuccessRate(), 0.0001);
        Assertions.assertEquals(0.97, preview.successRate(), 0.0001);
        Assertions.assertEquals(1, preview.goldCost());
        Assertions.assertTrue(preview.requiredItems().isEmpty());
        Assertions.assertTrue(preview.canBreak());
        Assertions.assertTrue(preview.canAfford());
        Assertions.assertEquals("fail_destroyed", preview.failureResult());
    }

    @Test
    void normalEnhanceIgnoresProtectionAndBoostItems() {
        String userId = "normal-gold-only-user";
        playerSaveService.upsert(new PlayerSaveData(
                userId,
                "normal_01",
                Map.of("gold", 20),
                Map.of(),
                Map.of("normal_01", 1),
                List.of("normal_01"),
                List.of("normal_01"),
                List.of("normal_01"),
                "normal_01",
                Map.of(),
                java.time.Instant.now()
        ));

        EnhanceService.EnhanceResult result = enhanceService.attempt(
                userId,
                "normal_01",
                true,
                "enhance_rate_boost_10",
                0.99
        );

        Assertions.assertEquals("fail_destroyed", result.outcome().name().toLowerCase());
        Assertions.assertFalse(result.protectionUsed());
        Assertions.assertNull(result.rateBoostItemId());
        Assertions.assertEquals(19, result.remainingGold());
        Assertions.assertTrue(result.remainingMaterials().containsKey("gold"));
    }

    @Test
    void rarePreviewReportsMissingProtectionItem() {
        String userId = "rare-missing-protection-user";
        playerSaveService.upsert(new PlayerSaveData(
                userId,
                "rare_01",
                Map.of("gold", 100),
                Map.of(),
                Map.of("normal_01", 1, "rare_01", 1),
                List.of("normal_01", "rare_01"),
                List.of("normal_01", "rare_01"),
                List.of("normal_01", "rare_01"),
                "rare_01",
                Map.of(),
                java.time.Instant.now()
        ));

        EnhanceService.EnhancePreview preview = enhanceService.preview(userId, "rare_01", true);

        Assertions.assertFalse(preview.useProtectionAvailable());
        Assertions.assertFalse(preview.canAfford());
        Assertions.assertEquals("middle_protection_ticket", preview.requiredItems().get(0).resourceId());
        Assertions.assertEquals("middle_protection_ticket", preview.missingResources().get(0).resourceId());
    }

    @Test
    void enhanceAttemptReturnsRetryContext() {
        String userId = "retry-context-user";
        playerSaveService.upsert(new PlayerSaveData(
                userId,
                "normal_01",
                Map.of("gold", 20),
                Map.of(),
                Map.of("normal_01", 1),
                List.of("normal_01"),
                List.of("normal_01"),
                List.of("normal_01"),
                "normal_01",
                Map.of(),
                java.time.Instant.now()
        ));

        EnhanceService.EnhanceResult result = enhanceService.attempt(userId, "normal_01", false, 0.0);

        Assertions.assertEquals("normal_02", result.currentWeaponId());
        Assertions.assertEquals("normal_02", result.equippedWeaponId());
        Assertions.assertEquals(19, result.remainingMaterials().get("gold"));
        Assertions.assertTrue(result.canRetry());
        Assertions.assertNotNull(result.nextPreview());
        Assertions.assertEquals("normal_03", result.nextPreview().nextWeaponId());
        Assertions.assertTrue(result.missingResources().isEmpty());
    }

    @Test
    void probabilityEndpointReturnsAdjustedEnhanceRates() throws Exception {
        String userId = "probability-user";
        playerSaveService.upsert(new PlayerSaveData(
                userId,
                "rare_07",
                Map.of("gold", 100),
                Map.of("enhance_rate_boost_10", 1),
                Map.of("normal_01", 1, "rare_07", 1),
                List.of("normal_01", "rare_07"),
                List.of("normal_01", "rare_07"),
                List.of("normal_01", "rare_07"),
                "rare_07",
                Map.of("rare", 3),
                java.time.Instant.now()
        ));

        mockMvc.perform(get("/api/v1/probabilities/enhance/{weaponId}", "rare_07")
                        .param("userId", userId)
                        .param("useProtection", "true")
                        .param("rateBoostItemId", "enhance_rate_boost_10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.baseSuccessRate").value(0.58))
                .andExpect(jsonPath("$.data.pityStack").value(3))
                .andExpect(jsonPath("$.data.pityBonus").value(0.06))
                .andExpect(jsonPath("$.data.protectionBonus").value(0.05))
                .andExpect(jsonPath("$.data.rateBoostBonus").value(0.1))
                .andExpect(jsonPath("$.data.adjustedSuccessRate").value(closeTo(0.79, 0.0001)));
    }

    @Test
    void probabilityEndpointMarksEvolutionBoundaries() throws Exception {
        mockMvc.perform(get("/api/v1/probabilities/enhance/{weaponId}", "normal_10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enhancementAvailable").value(false))
                .andExpect(jsonPath("$.data.requiresEvolution").value(true))
                .andExpect(jsonPath("$.data.goldCost").value(0));
    }

    @Test
    void probabilityEndpointIgnoresNormalGradeItems() throws Exception {
        mockMvc.perform(get("/api/v1/probabilities/enhance/{weaponId}", "normal_01")
                        .param("useProtection", "true")
                        .param("rateBoostItemId", "enhance_rate_boost_10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.protectionSelected").value(false))
                .andExpect(jsonPath("$.data.protectionBonus").value(0.0))
                .andExpect(jsonPath("$.data.rateBoostBonus").value(0.0))
                .andExpect(jsonPath("$.data.adjustedSuccessRate").value(0.97));
    }

    @Test
    void enhanceFailureDestroysWeaponAndRewardsMaterials() {
        String userId = "enhance-fail-user";
        playerSaveService.upsert(new PlayerSaveData(
                userId,
                "rare_01",
                Map.of("gold", 100),
                Map.of(),
                Map.of("normal_01", 1, "rare_01", 1),
                List.of("normal_01", "rare_01"),
                List.of("normal_01", "rare_01"),
                List.of("normal_01", "rare_01"),
                "rare_01",
                Map.of(),
                java.time.Instant.now()
        ));

        EnhanceService.EnhanceResult result = enhanceService.attempt(userId, "rare_01", false, 0.99);

        Assertions.assertEquals("fail_destroyed", result.outcome().name().toLowerCase());
        PlayerSaveData updated = playerSaveService.getOrCreate(userId);
        Assertions.assertEquals("rare_01", updated.currentWeaponId());
        Assertions.assertTrue(updated.weaponInventory().containsKey("rare_01"));
        Assertions.assertTrue(updated.materials().containsKey("blue_black_jade_fragment"));
        Assertions.assertEquals(95, updated.materials().get("gold"));
    }

    @Test
    void enhanceLogEndpointReturnsRecentLogs() throws Exception {
        String userId = "enhance-log-user";
        playerSaveService.upsert(new PlayerSaveData(
                userId,
                "rare_01",
                Map.of("gold", 100),
                Map.of(),
                Map.of("normal_01", 1, "rare_01", 1),
                List.of("normal_01", "rare_01"),
                List.of("normal_01", "rare_01"),
                List.of("normal_01", "rare_01"),
                "rare_01",
                Map.of(),
                java.time.Instant.now()
        ));

        enhanceService.attempt(userId, "rare_01", false, 0.99);

        mockMvc.perform(get("/api/v1/logs/enhance/{userId}", userId)
                        .param("outcome", "fail_destroyed")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].weaponId").value("rare_01"))
                .andExpect(jsonPath("$.data.content[0].outcome").value("fail_destroyed"));
    }

    @Test
    void enhanceProtectedFailKeepsWeaponAndConsumesTicket() {
        String userId = "enhance-protected-user";
        playerSaveService.upsert(new PlayerSaveData(
                userId,
                "rare_01",
                Map.of("gold", 100),
                Map.of("middle_protection_ticket", 1),
                Map.of("normal_01", 1, "rare_01", 1),
                List.of("normal_01", "rare_01"),
                List.of("normal_01", "rare_01"),
                List.of("normal_01", "rare_01"),
                "rare_01",
                Map.of(),
                java.time.Instant.now()
        ));

        EnhanceService.EnhanceResult result = enhanceService.attempt(userId, "rare_01", true, 0.99);

        Assertions.assertEquals("protected_fail", result.outcome().name().toLowerCase());
        PlayerSaveData updated = playerSaveService.getOrCreate(userId);
        Assertions.assertEquals("rare_01", updated.currentWeaponId());
        Assertions.assertEquals(0, updated.specialItems().getOrDefault("middle_protection_ticket", 0));
        Assertions.assertTrue(updated.weaponInventory().containsKey("rare_01"));
    }

    @Test
    void specialItemGrantAndConsumeWorks() {
        String userId = "item-user";
        specialItemService.grant(userId, "enhance_rate_boost_5", 2);
        PlayerSaveData granted = playerSaveService.getOrCreate(userId);
        Assertions.assertEquals(2, granted.specialItems().get("enhance_rate_boost_5"));

        specialItemService.consume(userId, "enhance_rate_boost_5", 1);
        PlayerSaveData consumed = playerSaveService.getOrCreate(userId);
        Assertions.assertEquals(1, consumed.specialItems().get("enhance_rate_boost_5"));
    }

    @Test
    void rewardLogEndpointReturnsRecentLogs() throws Exception {
        String userId = "reward-log-user";
        specialItemService.grant(userId, "enhance_rate_boost_5", 2);

        mockMvc.perform(get("/api/v1/logs/rewards/{userId}", userId)
                        .param("rewardKind", "special_item")
                        .param("sourceType", "item_grant"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].rewardKind").value("special_item"))
                .andExpect(jsonPath("$.data.content[0].rewardId").value("enhance_rate_boost_5"))
                .andExpect(jsonPath("$.data.content[0].amount").value(2));
    }

    @Test
    void economyLogEndpointTracksGoldSourcesAndSinks() throws Exception {
        String userId = "economy-log-user";
        playerSaveService.upsert(new PlayerSaveData(
                userId,
                "normal_01",
                Map.of("gold", 100),
                Map.of(),
                Map.of("normal_01", 1, "normal_02", 1),
                List.of("normal_01", "normal_02"),
                List.of("normal_01", "normal_02"),
                List.of("normal_01", "normal_02"),
                "normal_02",
                Map.of(),
                java.time.Instant.now()
        ));

        weaponPurchaseService.sell(userId, "normal_02", 1);
        specialItemService.purchase(userId, "enhance_rate_boost_5", 1);

        mockMvc.perform(get("/api/v1/logs/economy/{userId}", userId)
                        .param("transactionType", "weapon_sale")
                        .param("resourceId", "gold"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].amount").value(4))
                .andExpect(jsonPath("$.data.content[0].balanceAfter").value(104));

        mockMvc.perform(get("/api/v1/logs/economy/{userId}", userId)
                        .param("transactionType", "item_purchase_gold_cost")
                        .param("resourceId", "gold"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].amount").value(-40))
                .andExpect(jsonPath("$.data.content[0].balanceAfter").value(64));
    }

    @Test
    void enhanceConsumesBoostItemAndUsesItsBonus() {
        String userId = "enhance-boost-user";
        playerSaveService.upsert(new PlayerSaveData(
                userId,
                "rare_07",
                Map.of("gold", 100),
                Map.of("enhance_rate_boost_10", 1),
                Map.of("normal_01", 1, "rare_07", 1),
                List.of("normal_01", "rare_07"),
                List.of("normal_01", "rare_07"),
                List.of("normal_01", "rare_07"),
                "rare_07",
                Map.of(),
                java.time.Instant.now()
        ));

        EnhanceService.EnhanceResult result = enhanceService.attempt(userId, "rare_07", false, "enhance_rate_boost_10", 0.18);

        Assertions.assertEquals("success", result.outcome().name().toLowerCase());
        Assertions.assertEquals("enhance_rate_boost_10", result.rateBoostItemId());
        PlayerSaveData updated = playerSaveService.getOrCreate(userId);
        Assertions.assertEquals(0, updated.specialItems().getOrDefault("enhance_rate_boost_10", 0));
        Assertions.assertEquals("rare_08", updated.currentWeaponId());
    }

    @Test
    void gradeCapEnhancementRequiresEvolution() {
        String userId = "grade-cap-user";
        playerSaveService.upsert(new PlayerSaveData(
                userId,
                "rare_08",
                Map.of(),
                Map.of(),
                Map.of("normal_01", 1, "rare_08", 1),
                List.of("normal_01", "rare_08"),
                List.of("normal_01", "rare_08"),
                List.of("normal_01", "rare_08"),
                "rare_08",
                Map.of(),
                java.time.Instant.now()
        ));

        IllegalArgumentException ex = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> enhanceService.attempt(userId, "rare_08", false, 0.0)
        );
        Assertions.assertTrue(ex.getMessage().contains("requires evolution"));
    }

    @Test
    void pityStackIncreasesAfterFailureAndAddsSuccessBonus() {
        String userId = "pity-user";
        playerSaveService.upsert(new PlayerSaveData(
                userId,
                "rare_01",
                Map.of("gold", 100),
                Map.of(),
                Map.of("normal_01", 1, "rare_01", 1),
                List.of("normal_01", "rare_01"),
                List.of("normal_01", "rare_01"),
                List.of("normal_01", "rare_01"),
                "rare_01",
                Map.of(),
                java.time.Instant.now()
        ));

        EnhanceService.EnhanceResult failed = enhanceService.attempt(userId, "rare_01", false, 0.99);
        Assertions.assertEquals(1, failed.pityStackAfter());
        Assertions.assertEquals(1, playerSaveService.getOrCreate(userId).pityStacks().get("rare"));

        playerSaveService.upsert(new PlayerSaveData(
                userId,
                "rare_01",
                Map.of("gold", 100),
                Map.of(),
                Map.of("normal_01", 1, "rare_01", 1),
                List.of("normal_01", "rare_01"),
                List.of("normal_01", "rare_01"),
                List.of("normal_01", "rare_01"),
                "rare_01",
                Map.of("rare", 1),
                java.time.Instant.now()
        ));
        EnhanceService.EnhanceResult succeeded = enhanceService.attempt(userId, "rare_01", false, 0.83);
        Assertions.assertEquals("success", succeeded.outcome().name().toLowerCase());
        Assertions.assertEquals(0.02, succeeded.pityBonus(), 0.0001);
        Assertions.assertEquals(1, succeeded.pityStackAfter());
        Assertions.assertEquals(1, playerSaveService.getOrCreate(userId).pityStacks().get("rare"));
    }

    @Test
    void evolutionPreviewReturnsTargetWeapon() {
        EvolutionService.EvolutionPreview preview = evolutionService.preview("normal_10");
        Assertions.assertEquals("rare_01", preview.toWeaponId());
        Assertions.assertEquals(1, preview.requiredMaterials().get("evolution_fixed_stone"));
        Assertions.assertEquals(6, preview.requiredMaterials().get("cold_ember_powder"));
    }

    @Test
    void evolutionConsumesResourcesAndUpgradesWeapon() {
        String userId = "evolution-user";
        playerSaveService.upsert(new PlayerSaveData(
                userId,
                "normal_10",
                Map.of(
                        "cracked_iron_piece", 150,
                        "cold_ember_powder", 40,
                        "evolution_fixed_stone", 2
                ),
                Map.of(),
                Map.of("normal_01", 1, "normal_10", 1),
                List.of("normal_01", "normal_10"),
                List.of("normal_01", "normal_10"),
                List.of("normal_01", "normal_10"),
                "normal_10",
                Map.of(),
                java.time.Instant.now()
        ));

        EvolutionService.EvolutionResult result = evolutionService.evolve(userId);

        Assertions.assertEquals("rare_01", result.toWeaponId());
        PlayerSaveData updated = playerSaveService.getOrCreate(userId);
        Assertions.assertEquals("rare_01", updated.currentWeaponId());
        Assertions.assertTrue(updated.weaponInventory().containsKey("rare_01"));
    }
}
