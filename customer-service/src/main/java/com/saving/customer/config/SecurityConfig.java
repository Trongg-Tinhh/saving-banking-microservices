package com.saving.customer.config;

import com.saving.customer.filter.JwtAuthenticationFilter;
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
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;

    private static final String[] PUBLIC_PATHS = {
            "/api/v1/customers/internal/**",   // Internal service-to-service calls
            "/api/v1/customers/health",
            "/actuator/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/api-docs/**",
            "/v3/api-docs/**"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(ex -> ex
                    // 401 khi chưa xác thực (thay vì trả 403 mặc định)
                    .authenticationEntryPoint((request, response, authException) -> {
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                        response.getWriter().write(
                            "{\"success\":false,\"message\":\"Authentication required\"," +
                            "\"errorCode\":\"AUTH_001\"}");
                    })
                    // 403 khi đã xác thực nhưng không đủ quyền
                    .accessDeniedHandler((request, response, accessDeniedException) -> {
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                        response.getWriter().write(
                            "{\"success\":false,\"message\":\"Access denied\"," +
                            "\"errorCode\":\"AUTH_002\"}");
                    })
            )
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers(PUBLIC_PATHS).permitAll()
                    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    // GET: mọi role đã xác thực đều đọc được
                    .requestMatchers(HttpMethod.GET, "/api/v1/customers/**").authenticated()
                    // POST: chỉ TELLER/ADMIN mới tạo được khách hàng mới
                    .requestMatchers(HttpMethod.POST,   "/api/v1/customers").hasAnyAuthority("TELLER", "ADMIN")
                    // PUT KYC status: chỉ staff duyệt KYC
                    .requestMatchers(HttpMethod.PUT,    "/api/v1/customers/*/kyc/status").hasAnyAuthority("TELLER", "ADMIN", "MANAGER")
                    // PUT profile / contacts: mọi user đã xác thực (ownership kiểm tra trong controller)
                    .requestMatchers(HttpMethod.PUT,    "/api/v1/customers/**").authenticated()
                    .requestMatchers(HttpMethod.DELETE, "/api/v1/customers/**").hasAuthority("ADMIN")
                    .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
