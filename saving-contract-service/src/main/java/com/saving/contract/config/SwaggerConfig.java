package com.saving.contract.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    private static final String BEARER_SCHEME = "BearerAuth";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Saving Contract Service API")
                        .description("""
                                Manages the full lifecycle of saving contracts:
                                open, query, update maturity instructions, and close (maturity or early withdrawal).
                                Internal endpoints are available for service-to-service communication.
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Saving Banking System")
                                .email("dev@saving-bank.internal")))
                .servers(List.of(
                        new Server().url("http://localhost:8085").description("Local"),
                        new Server().url("http://saving-contract-service:8085").description("Docker")))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT issued by the Auth Service")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }
}
