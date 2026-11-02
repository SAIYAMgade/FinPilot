package com.finpilot.core.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI finpilotOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("FinPilot Core Financial Operations API")
                        .description("Deterministic double-entry ledger, multi-tier reconciliation engine, idempotency, and controlled AI investigation platform.")
                        .version("v1.0.0")
                        .contact(new Contact().name("FinPilot Engineering").email("engineering@finpilot.io")))
                .addSecurityItem(new SecurityRequirement().addList("BearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("BearerAuth", new SecurityScheme()
                                .name("BearerAuth")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
