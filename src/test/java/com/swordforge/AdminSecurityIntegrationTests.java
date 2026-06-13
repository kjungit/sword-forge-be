package com.swordforge;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "app.security.enabled=true",
        "spring.security.user.name=admin",
        "spring.security.user.password=password",
        "spring.security.user.roles=ADMIN"
})
@AutoConfigureMockMvc
class AdminSecurityIntegrationTests {

    private final MockMvc mockMvc;

    @Autowired
    AdminSecurityIntegrationTests(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    void adminCanUpsertAnySave() throws Exception {
        String body = """
                {
                  "currentWeaponId": "normal_01",
                  "materials": {"gold": 999},
                  "specialItems": {},
                  "weaponInventory": {"normal_01": 1},
                  "ownedWeaponIds": ["normal_01"],
                  "unlockedWeaponShop": ["normal_01"],
                  "discoveredWeaponIds": ["normal_01"],
                  "highestReachedWeaponId": "normal_01"
                }
                """;

        mockMvc.perform(put("/api/v1/saves/target-player")
                        .with(csrf())
                        .header("Authorization", basicAuth("admin", "password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    @Test
    void adminCanGrantItemsToAnySave() throws Exception {
        String body = """
                {
                  "userId": "target-player",
                  "itemId": "enhance_rate_boost_5",
                  "amount": 1
                }
                """;

        mockMvc.perform(post("/api/v1/items/grant")
                        .with(csrf())
                        .header("Authorization", basicAuth("admin", "password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    @Test
    void adminMutationWithoutCsrfIsForbidden() throws Exception {
        String body = """
                {
                  "userId": "target-player",
                  "itemId": "enhance_rate_boost_5",
                  "amount": 1
                }
                """;

        mockMvc.perform(post("/api/v1/items/grant")
                        .header("Authorization", basicAuth("admin", "password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    private String basicAuth(String username, String password) {
        String token = username + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(token.getBytes(StandardCharsets.UTF_8));
    }
}
