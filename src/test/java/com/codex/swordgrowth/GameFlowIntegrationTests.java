package com.codex.swordgrowth;

import com.codex.swordgrowth.application.evolution.EvolutionService;
import com.codex.swordgrowth.application.enhance.EnhanceService;
import com.codex.swordgrowth.application.item.SpecialItemService;
import com.codex.swordgrowth.application.save.PlayerSaveService;
import com.codex.swordgrowth.application.shop.WeaponPurchaseService;
import com.codex.swordgrowth.domain.save.PlayerSaveData;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

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
                .andExpect(jsonPath("$.data.weaponInventory.normal_01").value(1));
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
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.materials.cracked_iron_piece").value(12));
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
                java.time.Instant.now()
        ));

        WeaponPurchaseService.PurchaseResult result = weaponPurchaseService.purchase(userId, "normal_02");

        Assertions.assertEquals("normal_02", result.equippedWeaponId());
        Assertions.assertEquals(40, result.remainingMaterials().get("cracked_iron_piece"));
    }

    @Test
    void enhancePreviewReturnsRates() {
        EnhanceService.EnhancePreview preview = enhanceService.preview("normal_01", false);
        Assertions.assertEquals("normal_02", preview.nextWeaponId());
        Assertions.assertEquals(0.9, preview.baseSuccessRate(), 0.0001);
    }

    @Test
    void enhanceFailureDestroysWeaponAndRewardsMaterials() {
        String userId = "enhance-fail-user";
        playerSaveService.upsert(new PlayerSaveData(
                userId,
                "rare_01",
                Map.of(),
                Map.of(),
                Map.of("normal_01", 1, "rare_01", 1),
                List.of("normal_01", "rare_01"),
                List.of("normal_01", "rare_01"),
                List.of("normal_01", "rare_01"),
                "rare_01",
                java.time.Instant.now()
        ));

        EnhanceService.EnhanceResult result = enhanceService.attempt(userId, "rare_01", false, 0.99);

        Assertions.assertEquals("fail_destroyed", result.outcome().name().toLowerCase());
        PlayerSaveData updated = playerSaveService.getOrCreate(userId);
        Assertions.assertEquals("normal_01", updated.currentWeaponId());
        Assertions.assertFalse(updated.weaponInventory().containsKey("rare_01"));
        Assertions.assertTrue(updated.materials().containsKey("blue_black_jade_fragment"));
    }

    @Test
    void enhanceLogEndpointReturnsRecentLogs() throws Exception {
        String userId = "enhance-log-user";
        playerSaveService.upsert(new PlayerSaveData(
                userId,
                "rare_01",
                Map.of(),
                Map.of(),
                Map.of("normal_01", 1, "rare_01", 1),
                List.of("normal_01", "rare_01"),
                List.of("normal_01", "rare_01"),
                List.of("normal_01", "rare_01"),
                "rare_01",
                java.time.Instant.now()
        ));

        enhanceService.attempt(userId, "rare_01", false, 0.99);

        mockMvc.perform(get("/api/v1/logs/enhance/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].weaponId").value("rare_01"))
                .andExpect(jsonPath("$.data[0].outcome").value("fail_destroyed"));
    }

    @Test
    void enhanceProtectedFailKeepsWeaponAndConsumesTicket() {
        String userId = "enhance-protected-user";
        playerSaveService.upsert(new PlayerSaveData(
                userId,
                "rare_01",
                Map.of(),
                Map.of("middle_protection_ticket", 1),
                Map.of("normal_01", 1, "rare_01", 1),
                List.of("normal_01", "rare_01"),
                List.of("normal_01", "rare_01"),
                List.of("normal_01", "rare_01"),
                "rare_01",
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

        mockMvc.perform(get("/api/v1/logs/rewards/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].rewardKind").value("special_item"))
                .andExpect(jsonPath("$.data[0].rewardId").value("enhance_rate_boost_5"))
                .andExpect(jsonPath("$.data[0].amount").value(2));
    }

    @Test
    void enhanceConsumesBoostItemAndUsesItsBonus() {
        String userId = "enhance-boost-user";
        playerSaveService.upsert(new PlayerSaveData(
                userId,
                "rare_08",
                Map.of(),
                Map.of("enhance_rate_boost_10", 1),
                Map.of("normal_01", 1, "rare_08", 1),
                List.of("normal_01", "rare_08"),
                List.of("normal_01", "rare_08"),
                List.of("normal_01", "rare_08"),
                "rare_08",
                java.time.Instant.now()
        ));

        EnhanceService.EnhanceResult result = enhanceService.attempt(userId, "rare_08", false, "enhance_rate_boost_10", 0.18);

        Assertions.assertEquals("success", result.outcome().name().toLowerCase());
        Assertions.assertEquals("enhance_rate_boost_10", result.rateBoostItemId());
        PlayerSaveData updated = playerSaveService.getOrCreate(userId);
        Assertions.assertEquals(0, updated.specialItems().getOrDefault("enhance_rate_boost_10", 0));
        Assertions.assertEquals("epic_01", updated.currentWeaponId());
    }

    @Test
    void evolutionPreviewReturnsTargetWeapon() {
        EvolutionService.EvolutionPreview preview = evolutionService.preview("normal_10");
        Assertions.assertEquals("rare_01", preview.toWeaponId());
        Assertions.assertEquals(1, preview.requiredMaterials().get("evolution_fixed_stone"));
        Assertions.assertEquals(100, preview.requiredMaterials().get("cracked_iron_piece"));
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
                java.time.Instant.now()
        ));

        EvolutionService.EvolutionResult result = evolutionService.evolve(userId);

        Assertions.assertEquals("rare_01", result.toWeaponId());
        PlayerSaveData updated = playerSaveService.getOrCreate(userId);
        Assertions.assertEquals("rare_01", updated.currentWeaponId());
        Assertions.assertTrue(updated.weaponInventory().containsKey("rare_01"));
    }
}
