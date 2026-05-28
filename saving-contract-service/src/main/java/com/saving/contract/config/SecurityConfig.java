package com.saving.contract.config;

import com.saving.contract.filter.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /** Paths exempt from JWT authentication. */
    private static final String[] PUBLIC_PATHS = {
            "/api/v1/contracts/internal/**",
            "/api/v1/contracts/health",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/actuator/**"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) -> {
                            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            res.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            res.getWriter().write(
                                "{\"success\":false,\"message\":\"Authentication required\",\"errorCode\":\"AUTH_001\"}");
                        })
                        .accessDeniedHandler((req, res, e) -> {
                            res.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            res.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            res.getWriter().write(
                                "{\"success\":false,\"message\":\"Access denied\",\"errorCode\":\"AUTH_002\"}");
                        })
                )
                .authorizeHttpRequests(auth -> auth
                        // Public (internal service-to-service + docs)
                        .requestMatchers(PUBLIC_PATHS).permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Open contract — any authenticated user (CUSTOMER opens for own CIF,
                        // staff opens for any CIF; ownership is verified in the controller)
                        .requestMatchers(HttpMethod.POST, "/api/v1/contracts").authenticated()

                        // Close contract — any authenticated user
                        // (CUSTOMER chỉ được đóng hợp đồng của chính mình — kiểm tra trong controller)
                        .requestMatchers(HttpMethod.POST, "/api/v1/contracts/*/close").authenticated()

                        // Maturity instruction — any authenticated user (ownership check in controller)
                        .requestMatchers(HttpMethod.PUT, "/api/v1/contracts/*/maturity-instruction").authenticated()

                        // All other authenticated requests
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
