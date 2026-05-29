package com.saving.contract.filter;

import com.saving.contract.common.Constants;
import com.saving.contract.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.startsWith("/api/v1/contracts/internal/")
                || path.equals("/api/v1/contracts/health")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/actuator");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                // Read username, roles (list), and optional CIF from JWT
                String       username = jwtService.extractUsername(token);
                List<String> roles    = jwtService.extractRoles(token);
                String       cif      = jwtService.extractCif(token);   // null for staff

                // Store roles as-is (no ROLE_ prefix) to match hasAnyAuthority() in SecurityConfig
                List<SimpleGrantedAuthority> authorities = roles.stream()
                        .map(SimpleGrantedAuthority::new)
                        .collect(java.util.stream.Collectors.toList());

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(username, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);

                // Make CIF available to controllers for ownership checks
                if (cif != null) {
                    request.setAttribute("cif", cif);
                }

                MDC.put(Constants.MDC_USERNAME_KEY, username);
                log.info("JWT auth OK: user={} cif={} path={} roles={}",
                        username, cif, request.getRequestURI(), roles);

            } catch (Exception ex) {
                log.warn("JWT parsing failed for path {}: {}", request.getRequestURI(), ex.getMessage());
                // Do not set authentication; Spring Security will return 401
            }
        }

        filterChain.doFilter(request, response);
    }
}
