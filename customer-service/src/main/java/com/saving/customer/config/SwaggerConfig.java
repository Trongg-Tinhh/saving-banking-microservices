package com.saving.customer.config;

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

    @Value("${server.port:8082}")
    private String serverPort;

    @Bean
    public OpenAPI customerServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Customer Service API")
                        .description("""
                            Customer Management Service for Saving Banking Microservices.

                            **Features:**
                            - Customer profile management (CIF-based)
                            - KYC status tracking and verification
                            - Contact information management
                            - Internal validation endpoint for other services

                            **Roles:**
                            - `ROLE_ADMIN` — Full access
                            - `ROLE_TELLER` — Create/update customers, manage KYC
                            - `ROLE_CUSTOMER` — Read own profile
                            - `ROLE_MANAGER` — Read all customers

                            **Internal endpoint (no auth required):**
                            - `GET /api/v1/customers/internal/{cif}/validate` — for other microservices
                        """)
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("Saving Banking Team")
                                .email("dev@saving-banking.com")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Local development")))
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                .components(new Components()
                        .addSecuritySchemes("Bearer Authentication",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Enter JWT token obtained from auth-service /api/v1/auth/login")));
    }
}
