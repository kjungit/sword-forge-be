package com.swordforge;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.security.enabled=true")
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
    void apiRequiresAuthenticationWhenSecurityIsEnabled() throws Exception {
        mockMvc.perform(get("/api/v1/weapons"))
                .andExpect(status().isUnauthorized());
    }
}
