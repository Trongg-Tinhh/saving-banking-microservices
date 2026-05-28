package com.saving.account.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Value("${server.port:8083}")
    private String serverPort;

    @Bean
    public OpenAPI accountServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Account Service API")
                        .description("""
                            Account Management Service for Saving Banking Microservices.

                            **Features:**
                            - Account lifecycle (PAYMENT / SAVING / LOAN)
                            - Balance management with optimistic locking (@Version)
                            - Debit / Credit operations with idempotency keys
                            - Fund hold & release (used by Saving Contract Service)
                            - Internal validation endpoint for other microservices

                            **Balance model:**
                            - `available_balance` = funds customer can use
                            - `ledger_balance`    = accounting balance
                            - `hold_amount`       = reserved but not yet settled
                            - `available = ledger - hold`

                            **Idempotency:** All debit/credit requests require a `reference` field.
                        """)
                        .version("v1.0.0")
                        .contact(new Contact().name("Saving Banking Team")))
                .servers(List.of(new Server().url("http://localhost:" + serverPort).description("Local")))
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                .components(new Components()
                        .addSecuritySchemes("Bearer Authentication",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }
}
