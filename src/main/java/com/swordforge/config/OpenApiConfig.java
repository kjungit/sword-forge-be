package com.swordforge.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI swordForgeOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Sword Forge API")
                        .version("0.1.0")
                        .description("Backend API for weapon forging, saves, evolution, items, shop, and logs."))
                .components(new Components()
                        .addSecuritySchemes("basicAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("basic"))
                        .addSecuritySchemes("csrfToken", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-XSRF-TOKEN")
                                .description("Required for POST, PUT, PATCH, and DELETE requests. Fetch it from /api/v1/security/csrf and send the matching XSRF-TOKEN cookie for native clients that do not keep cookies automatically.")))
                .addSecurityItem(new SecurityRequirement()
                        .addList("basicAuth")
                        .addList("csrfToken"))
                .addServersItem(new Server()
                        .url("/")
                        .description("Current host"));
    }
}
