package com.saving.auth.config;

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

    @Value("${server.port:8081}")
    private String serverPort;

    @Bean
    public OpenAPI authServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Auth Service API")
                        .description("""
                            Authentication & Authorization Service for Saving Banking Microservices.

                            **Features:**
                            - JWT-based authentication (access + refresh token)
                            - Role-based access control (RBAC)
                            - OTP verification
                            - Session management
                            - Internal token validation endpoint for other services

                            **Test accounts:**
                            - `customer001` / `Test@123` → ROLE_CUSTOMER
                            - `teller01` / `Teller@123` → ROLE_TELLER
                            - `admin` / `Admin@123` → ROLE_ADMIN
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
                                        .description("Enter JWT token obtained from /api/v1/auth/login")));
    }
}
