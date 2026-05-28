package com.saving.product.config;

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

    @Value("${server.port:8084}")
    private String serverPort;

    @Bean
    public OpenAPI savingProductOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Saving Product Service API")
                        .description("""
                            Saving Product Catalog Service for Saving Banking Microservices.

                            **Entities:**
                            - `SavingProduct` — product definition (payment method, limits)
                            - `SavingTerm` — available tenors per product (1M, 3M, 6M ...)
                            - `InterestRateConfig` — rate history with effective date ranges
                            - `EarlyWithdrawalPolicy` — penalty rules for early closing

                            **Key internal endpoint:**
                            `GET /api/v1/products/internal/{productCode}/terms/{termId}/rate?effectiveDate=YYYY-MM-DD`
                            Called by Saving Contract Service to lock in the rate at contract open date.

                            **Write operations require ROLE_ADMIN.**
                        """)
                        .version("v1.0.0")
                        .contact(new Contact().name("Saving Banking Team")))
                .servers(List.of(new Server().url("http://localhost:" + serverPort).description("Local")))
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                .components(new Components()
                        .addSecuritySchemes("Bearer Authentication",
                                new SecurityScheme().type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer").bearerFormat("JWT")));
    }
}
