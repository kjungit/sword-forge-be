package com.swordforge;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "app.security.enabled=true",
        "spring.security.user.name=alice",
        "spring.security.user.password=password"
})
@AutoConfigureMockMvc
class SecurityIntegrationTests {

    private final MockMvc mockMvc;

    @Autowired
    SecurityIntegrationTests(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    void healthAndOpenApiRemainPublic() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());
    }

    @Test
    void csrfTokenEndpointRemainsPublic() throws Exception {
        mockMvc.perform(get("/api/v1/security/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.headerName").exists())
                .andExpect(jsonPath("$.data.token").isNotEmpty());
    }

    @Test
    void apiRequiresAuthenticationWhenSecurityIsEnabled() throws Exception {
        mockMvc.perform(get("/api/v1/weapons"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedPlayerCanAccessOwnSave() throws Exception {
        mockMvc.perform(get("/api/v1/saves/alice")
                        .header("Authorization", basicAuth("alice", "password")))
                .andExpect(status().isOk());
    }

    @Test
    void authenticatedPlayerCannotAccessAnotherUsersSave() throws Exception {
        mockMvc.perform(get("/api/v1/saves/bob")
                        .header("Authorization", basicAuth("alice", "password")))
                .andExpect(status().isForbidden());
    }

    @Test
    void authenticatedPlayerCannotReadAnotherUsersProbabilityState() throws Exception {
        mockMvc.perform(get("/api/v1/probabilities/enhance/normal_01")
                        .param("userId", "bob")
                        .header("Authorization", basicAuth("alice", "password")))
                .andExpect(status().isForbidden());
    }

    @Test
    void regularPlayerCannotUpsertSaveDirectly() throws Exception {
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

        mockMvc.perform(put("/api/v1/saves/alice")
                        .with(csrf())
                        .header("Authorization", basicAuth("alice", "password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void regularPlayerCannotGrantItems() throws Exception {
        String body = """
                {
                  "userId": "alice",
                  "itemId": "enhance_rate_boost_5",
                  "amount": 1
                }
                """;

        mockMvc.perform(post("/api/v1/items/grant")
                        .with(csrf())
                        .header("Authorization", basicAuth("alice", "password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    private String basicAuth(String username, String password) {
        String token = username + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(token.getBytes(StandardCharsets.UTF_8));
    }
}
